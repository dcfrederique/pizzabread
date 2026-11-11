package com.dcfrederique.pizzabread.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dcfrederique.pizzabread.data.AppState
import com.dcfrederique.pizzabread.data.BakeStore
import com.dcfrederique.pizzabread.domain.Category
import com.dcfrederique.pizzabread.domain.Planner
import com.dcfrederique.pizzabread.domain.Recipe
import com.dcfrederique.pizzabread.domain.Step
import com.dcfrederique.pizzabread.domain.StepKind
import com.dcfrederique.pizzabread.domain.TimeFormat

@Composable
fun RecipesScreen(state: AppState, now: Long, nav: Navigator, modifier: Modifier = Modifier) {
    var toDelete by remember { mutableStateOf<Recipe?>(null) }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { PlainTopBar("Recepten") },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    nav.go(Screen.Edit(Recipe("", "", Category.BREAD, listOf(Step("Eerste stap", 30)))))
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nieuw recept") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.bake?.let { bake ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { nav.go(Screen.Active) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        val active = Planner.plan(bake, now).active
                        Text(
                            "Bezig: ${bake.recipe.name}" + (active?.let { " · ${it.step.name}" } ?: ""),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            for (category in Category.entries) {
                val list = state.allRecipes.filter { it.category == category }
                if (list.isEmpty()) continue
                item {
                    Text(
                        if (category == Category.BREAD) "Brood" else "Pizzadeeg",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(list, key = { it.id }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        onOpen = { nav.go(Screen.Review(recipe)) },
                        onEdit = { nav.go(Screen.Edit(recipe)) },
                        onDelete = if (recipe.builtIn) null else ({ toDelete = recipe }),
                    )
                }
            }
        }
    }

    toDelete?.let { recipe ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Recept verwijderen?") },
            text = { Text("“${recipe.name}” wordt verwijderd.") },
            confirmButton = {
                TextButton(onClick = { BakeStore.deleteRecipe(recipe.id); toDelete = null }) { Text("Verwijderen") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annuleren") } },
        )
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: (() -> Unit)?) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(recipe.name, style = MaterialTheme.typography.titleMedium)
                if (recipe.description.isNotBlank()) {
                    Text(recipe.description, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${TimeFormat.duration(Planner.totalMinutes(recipe))} · ${Planner.alarmCount(recipe)} timers" +
                        if (recipe.builtIn) "" else " · eigen recept",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = if (recipe.builtIn) "Kopiëren en aanpassen" else "Bewerken")
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Verwijderen") }
            }
        }
    }
}

/** Recept nakijken: tijden per stap aanpassen, live eindtijd, en Start rijzing. */
@Composable
fun ReviewScreen(screen: Screen.Review, state: AppState, now: Long, nav: Navigator, modifier: Modifier = Modifier) {
    val original = screen.recipe
    var steps by remember(original) { mutableStateOf(original.steps) }
    var confirmReplace by remember { mutableStateOf(false) }
    val recipe = original.copy(steps = steps)
    val changed = steps != original.steps
    val plan = Planner.plan(Planner.start(recipe, now), now)
    val startLabel = if (recipe.category == Category.PIZZA) "Start deeg-timers" else "Start rijzing"

    fun start() {
        BakeStore.start(recipe, screen.note)
        nav.showActive()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopBar(original.name, onBack = { nav.back() }) {
                IconButton(onClick = { nav.go(Screen.Edit(recipe)) }) { Icon(Icons.Default.Edit, contentDescription = "Bewerken") }
            }
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                Text(
                    "Klaar om ${TimeFormat.clock(plan.finishAt, now)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text("${Planner.alarmCount(recipe)} timers worden in één keer gezet.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                if (changed) {
                    OutlinedButton(
                        onClick = {
                            val saved = BakeStore.saveRecipe(recipe.copy(name = if (original.builtIn) "${original.name} (eigen)" else original.name))
                            nav.back()
                            nav.go(Screen.Review(saved, screen.note))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Bewaar als eigen recept") }
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = { if (state.bake != null) confirmReplace = true else start() },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) { Text(startLabel, style = MaterialTheme.typography.titleLarge) }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (screen.note.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Text(screen.note, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            items(plan.steps.size) { i ->
                val p = plan.steps[i]
                val s = p.step
                Column(Modifier.padding(vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(96.dp)) {
                            Text(TimeFormat.clock(p.dueAt, now), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (s.kind == StepKind.MANUAL) "wacht op jou · ±${TimeFormat.duration(s.workMinutes)} werk" else "op de klok",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    p.preheatAt?.let {
                        Text(
                            "${s.preheatText}: ${TimeFormat.clock(it, now)}",
                            modifier = Modifier.padding(start = 96.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    if (i > 0 || s.waitMinutes > 0) {
                        Stepper(
                            value = TimeFormat.duration(s.waitMinutes),
                            label = if (i == 0) "Na start" else "Wachten",
                            onMinus = {
                                steps = steps.toMutableList().also { l ->
                                    l[i] = s.copy(waitMinutes = (s.waitMinutes - minuteStep(s.waitMinutes, false)).coerceAtLeast(0))
                                }
                            },
                            onPlus = {
                                steps = steps.toMutableList().also { l ->
                                    l[i] = s.copy(waitMinutes = s.waitMinutes + minuteStep(s.waitMinutes, true))
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(start = 96.dp, top = 4.dp),
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }

    if (confirmReplace) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            title = { Text("Er loopt al een bak") },
            text = { Text("“${state.bake?.recipe?.name}” wordt afgebroken en alle alarmen ervan verdwijnen.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReplace = false
                    BakeStore.finish(aborted = true)
                    start()
                }) { Text("Afbreken en starten") }
            },
            dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("Annuleren") } },
        )
    }
}
