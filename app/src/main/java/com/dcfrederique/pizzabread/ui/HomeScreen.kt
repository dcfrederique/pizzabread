package com.dcfrederique.pizzabread.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcfrederique.pizzabread.data.AppState
import com.dcfrederique.pizzabread.domain.Bake
import com.dcfrederique.pizzabread.domain.Planner
import com.dcfrederique.pizzabread.domain.TimeFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(state: AppState, now: Long, nav: Navigator, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("DoughCalc", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item { PermissionCard() }
        state.bake?.let { bake ->
            item { ActiveBakeCard(bake, now) { nav.go(Screen.Active) } }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigTile("Brood", "Desem of gist, met alle timers", Modifier.weight(1f)) { nav.root(Screen.Recipes) }
                BigTile("Pizzadeeg", "Calculator voor Napolitaans en Romeins", Modifier.weight(1f)) { nav.root(Screen.Calculator) }
            }
        }
        if (state.recent.isNotEmpty()) {
            item { Text("Opnieuw bakken", style = MaterialTheme.typography.titleMedium) }
            items(state.recent) { r ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { nav.go(Screen.Review(r.recipe, r.note)) },
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(r.recipe.name, style = MaterialTheme.typography.titleSmall)
                        val date = DateTimeFormatter.ofPattern("d/M").format(Instant.ofEpochMilli(r.finishedAt).atZone(ZoneId.systemDefault()))
                        Text(
                            (if (r.aborted) "Afgebroken op $date" else "Gebakken op $date") +
                                " · ${TimeFormat.duration(Planner.totalMinutes(r.recipe))}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (r.note.isNotBlank()) Text(r.note, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveBakeCard(bake: Bake, now: Long, onClick: () -> Unit) {
    val plan = Planner.plan(bake, now)
    val active = plan.active
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(bake.recipe.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            when {
                active == null -> Text("Klaar! Tik om af te sluiten.", style = MaterialTheme.typography.titleLarge)
                bake.pausedAt != null -> Text("Op pauze · volgende: ${active.step.name}", style = MaterialTheme.typography.titleLarge)
                active.dueAt > now -> {
                    Text("Volgende: ${active.step.name}", style = MaterialTheme.typography.bodyLarge)
                    Text(TimeFormat.countdown(active.dueAt - now), fontSize = 44.sp, fontWeight = FontWeight.Bold)
                }
                else -> Text("Nu: ${active.step.name}", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { plan.doneCount / bake.recipe.steps.size.coerceAtLeast(1).toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text("Klaar om ${TimeFormat.clock(plan.finishAt, now)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BigTile(title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(132.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
