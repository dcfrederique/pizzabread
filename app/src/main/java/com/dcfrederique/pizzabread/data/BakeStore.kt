package com.dcfrederique.pizzabread.data

import android.content.Context
import android.content.SharedPreferences
import com.dcfrederique.pizzabread.alarm.AlarmScheduler
import com.dcfrederique.pizzabread.alarm.Notifications
import com.dcfrederique.pizzabread.domain.Bake
import com.dcfrederique.pizzabread.domain.Planner
import com.dcfrederique.pizzabread.domain.Recipe
import com.dcfrederique.pizzabread.domain.RecentBake
import com.dcfrederique.pizzabread.domain.Templates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class AppState(
    val customRecipes: List<Recipe> = emptyList(),
    val bake: Bake? = null,
    val recent: List<RecentBake> = emptyList(),
) {
    val allRecipes: List<Recipe> get() = Templates.all + customRecipes
    fun recipe(id: String): Recipe? = allRecipes.firstOrNull { it.id == id }
}

/**
 * De enige bron van waarheid voor de app, de alarmen en de meldingen. Elke wijziging aan
 * de bak wordt meteen bewaard en alle alarmen worden opnieuw berekend.
 */
object BakeStore {
    private const val PREFS = "brood"
    private const val KEY_RECIPES = "recipes"
    private const val KEY_BAKE = "bake"
    private const val KEY_RECENT = "recent"
    private const val MAX_RECENT = 6

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _state.value = AppState(
            customRecipes = JsonCodec.list(prefs.getString(KEY_RECIPES, null), JsonCodec::recipe),
            bake = prefs.getString(KEY_BAKE, null)?.let { runCatching { JsonCodec.bake(org.json.JSONObject(it)) }.getOrNull() },
            recent = JsonCodec.list(prefs.getString(KEY_RECENT, null), JsonCodec::recent),
        )
    }

    val current: AppState get() = _state.value

    // ---- Recepten ----

    fun saveRecipe(recipe: Recipe): Recipe {
        val toSave = if (recipe.builtIn || recipe.id.isBlank()) recipe.copy(id = "own-" + UUID.randomUUID(), builtIn = false) else recipe
        val list = current.customRecipes.filterNot { it.id == toSave.id } + toSave
        prefs.edit().putString(KEY_RECIPES, JsonCodec.list(list, JsonCodec::recipe)).apply()
        _state.value = current.copy(customRecipes = list)
        return toSave
    }

    fun deleteRecipe(id: String) {
        val list = current.customRecipes.filterNot { it.id == id }
        prefs.edit().putString(KEY_RECIPES, JsonCodec.list(list, JsonCodec::recipe)).apply()
        _state.value = current.copy(customRecipes = list)
    }

    // ---- Lopende bak ----

    fun start(recipe: Recipe, note: String = "") {
        updateBake(Planner.start(recipe, System.currentTimeMillis(), note))
    }

    /** Pas de lopende bak aan met een functie uit [Planner]. */
    fun update(change: (Bake, Long) -> Bake) {
        val bake = current.bake ?: return
        updateBake(change(bake, System.currentTimeMillis()))
    }

    /** Bak afsluiten (klaar of afgebroken) en in "Opnieuw bakken" zetten. */
    fun finish(aborted: Boolean) {
        val bake = current.bake ?: return
        val entry = RecentBake(bake.recipe, System.currentTimeMillis(), aborted, bake.note)
        val recent = (listOf(entry) + current.recent.filterNot { it.recipe.id == bake.recipe.id }).take(MAX_RECENT)
        prefs.edit().putString(KEY_RECENT, JsonCodec.list(recent, JsonCodec::recent)).apply()
        _state.value = current.copy(recent = recent)
        updateBake(null)
    }

    private fun updateBake(bake: Bake?) {
        prefs.edit().apply {
            if (bake == null) remove(KEY_BAKE) else putString(KEY_BAKE, JsonCodec.bake(bake).toString())
        }.commit()
        _state.value = current.copy(bake = bake)
        resync()
    }

    /** Alarmen en vaste melding opnieuw opbouwen vanuit de bewaarde stand. */
    fun resync(after: Long = System.currentTimeMillis()) {
        val bake = current.bake
        AlarmScheduler.schedule(appContext, bake, after)
        Notifications.updateOngoing(appContext, bake)
    }
}
