package com.dcfrederique.pizzabread.data

import com.dcfrederique.pizzabread.domain.Bake
import com.dcfrederique.pizzabread.domain.Category
import com.dcfrederique.pizzabread.domain.Recipe
import com.dcfrederique.pizzabread.domain.RecentBake
import com.dcfrederique.pizzabread.domain.Step
import com.dcfrederique.pizzabread.domain.StepKind
import org.json.JSONArray
import org.json.JSONObject

/** Opslag als JSON met de ingebouwde org.json van Android. */
object JsonCodec {

    fun step(s: Step) = JSONObject()
        .put("name", s.name)
        .put("wait", s.waitMinutes)
        .put("kind", s.kind.name)
        .put("work", s.workMinutes)
        .put("instruction", s.instruction)
        .put("preheat", s.preheatMinutes)
        .put("preheatText", s.preheatText)

    fun step(o: JSONObject) = Step(
        name = o.getString("name"),
        waitMinutes = o.getInt("wait"),
        kind = StepKind.valueOf(o.optString("kind", StepKind.AUTO.name)),
        workMinutes = o.optInt("work", 0),
        instruction = o.optString("instruction", ""),
        preheatMinutes = o.optInt("preheat", 0),
        preheatText = o.optString("preheatText", "Oven voorverwarmen"),
    )

    fun recipe(r: Recipe) = JSONObject()
        .put("id", r.id)
        .put("name", r.name)
        .put("category", r.category.name)
        .put("builtIn", r.builtIn)
        .put("description", r.description)
        .put("steps", JSONArray().apply { r.steps.forEach { put(step(it)) } })

    fun recipe(o: JSONObject) = Recipe(
        id = o.getString("id"),
        name = o.getString("name"),
        category = Category.valueOf(o.optString("category", Category.BREAD.name)),
        builtIn = o.optBoolean("builtIn", false),
        description = o.optString("description", ""),
        steps = o.getJSONArray("steps").objects().map { step(it) },
    )

    fun bake(b: Bake) = JSONObject()
        .put("recipe", recipe(b.recipe))
        .put("startedAt", b.startedAt)
        .put("completedAt", JSONArray().apply { b.completedAt.forEach { put(it ?: JSONObject.NULL) } })
        .put("delayMs", JSONArray().apply { b.delayMs.forEach { put(it) } })
        .put("pausedAt", b.pausedAt ?: JSONObject.NULL)
        .put("note", b.note)

    fun bake(o: JSONObject): Bake {
        val recipe = recipe(o.getJSONObject("recipe"))
        val n = recipe.steps.size
        val completed = o.getJSONArray("completedAt")
        val delays = o.getJSONArray("delayMs")
        return Bake(
            recipe = recipe,
            startedAt = o.getLong("startedAt"),
            completedAt = List(n) { i -> if (i < completed.length() && !completed.isNull(i)) completed.getLong(i) else null },
            delayMs = List(n) { i -> if (i < delays.length()) delays.getLong(i) else 0L },
            pausedAt = if (o.isNull("pausedAt")) null else o.getLong("pausedAt"),
            note = o.optString("note", ""),
        )
    }

    fun recent(r: RecentBake) = JSONObject()
        .put("recipe", recipe(r.recipe))
        .put("finishedAt", r.finishedAt)
        .put("aborted", r.aborted)
        .put("note", r.note)

    fun recent(o: JSONObject) = RecentBake(
        recipe = recipe(o.getJSONObject("recipe")),
        finishedAt = o.getLong("finishedAt"),
        aborted = o.optBoolean("aborted", false),
        note = o.optString("note", ""),
    )

    fun <T> list(items: List<T>, encode: (T) -> JSONObject): String =
        JSONArray().apply { items.forEach { put(encode(it)) } }.toString()

    fun <T> list(json: String?, decode: (JSONObject) -> T): List<T> =
        if (json.isNullOrBlank()) emptyList()
        else runCatching { JSONArray(json).objects().map(decode) }.getOrDefault(emptyList())

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
}
