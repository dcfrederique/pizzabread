package com.dcfrederique.pizzabread.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcfrederique.pizzabread.alarm.AlarmScheduler
import com.dcfrederique.pizzabread.data.AppState
import com.dcfrederique.pizzabread.data.BakeStore
import com.dcfrederique.pizzabread.domain.PlannedStep
import com.dcfrederique.pizzabread.domain.Planner
import com.dcfrederique.pizzabread.domain.StepKind
import com.dcfrederique.pizzabread.domain.StepStatus
import com.dcfrederique.pizzabread.domain.TimeFormat

/** De lopende bak: grote aftelling, tijdlijn met kloktijden, Gedaan / +5 min / Pauzeer. */
@Composable
fun ActiveScreen(state: AppState, now: Long, nav: Navigator, modifier: Modifier = Modifier) {
    val bake = state.bake
    var confirmStop by remember { mutableStateOf(false) }
    var selected by remember { mutableIntStateOf(-1) }
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopBar(bake?.recipe?.name ?: "Geen bak bezig", onBack = { nav.back() }) {
                if (bake != null) TextButton(onClick = { confirmStop = true }) { Text("Stoppen") }
            }
        },
    ) { padding ->
        if (bake == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Er loopt geen bak. Kies een recept om te starten.")
            }
            return@Scaffold
        }
        val plan = Planner.plan(bake, now)
        val active = plan.active
        val paused = bake.pausedAt != null

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!AlarmScheduler.canScheduleExact(context)) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Text(
                            "Exacte alarmen staan uit: je alarmen kunnen enkele minuten te laat komen. Zet ze aan via Home.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        when {
                            active == null -> {
                                Text("Klaar!", fontSize = 44.sp, fontWeight = FontWeight.Bold)
                                Text("Smakelijk.", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { BakeStore.finish(aborted = false); nav.root(Screen.Home) }) { Text("Bak afsluiten") }
                            }
                            paused -> {
                                Text("Op pauze", style = MaterialTheme.typography.titleMedium)
                                Text(active.step.name, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Text("De klok staat stil. Alarmen worden herpland als je hervat.", textAlign = TextAlign.Center)
                            }
                            active.dueAt > now -> {
                                Text("Volgende stap", style = MaterialTheme.typography.titleMedium)
                                Text(active.step.name, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                Text(TimeFormat.countdown(active.dueAt - now), fontSize = 64.sp, fontWeight = FontWeight.Bold)
                                Text("om ${TimeFormat.clock(active.dueAt, now)}", style = MaterialTheme.typography.titleMedium)
                            }
                            else -> {
                                Text(if (active.step.kind == StepKind.MANUAL) "Nu te doen" else "Nu", style = MaterialTheme.typography.titleMedium)
                                Text(active.step.name, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                if (active.step.instruction.isNotBlank()) {
                                    Text(active.step.instruction, textAlign = TextAlign.Center)
                                }
                                Text("Tik op Gedaan als je klaar bent.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (active != null) {
                            Spacer(Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = { plan.doneCount / bake.recipe.steps.size.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Klaar om ${TimeFormat.clock(plan.finishAt, now)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            if (active != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { BakeStore.update { b, t -> Planner.done(b, active.index, t) } },
                            enabled = !paused,
                            modifier = Modifier.weight(1.4f).height(60.dp),
                        ) { Text("Gedaan", fontSize = 20.sp) }
                        OutlinedButton(
                            onClick = { BakeStore.update { b, t -> Planner.snooze(b, active.index, t) } },
                            enabled = !paused,
                            modifier = Modifier.weight(1f).height(60.dp),
                        ) { Text("+5 min") }
                        OutlinedButton(
                            onClick = {
                                BakeStore.update { b, t -> if (b.pausedAt == null) Planner.pause(b, t) else Planner.resume(b, t) }
                            },
                            modifier = Modifier.weight(1.2f).height(60.dp),
                        ) { Text(if (paused) "Hervat" else "Pauzeer") }
                    }
                }
            }
            if (bake.note.isNotBlank()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Text(bake.note, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            item { Text("Tijdlijn", style = MaterialTheme.typography.titleMedium) }
            plan.steps.forEach { p ->
                item(key = p.index) {
                    TimelineRow(
                        p = p,
                        now = now,
                        expanded = selected == p.index && p.status != StepStatus.DONE && !paused,
                        onClick = { selected = if (selected == p.index) -1 else p.index },
                        onAdjust = { delta -> BakeStore.update { b, _ -> Planner.adjust(b, p.index, delta) } },
                    )
                }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    if (confirmStop && bake != null) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Bak stoppen?") },
            text = { Text("Alle alarmen van “${bake.recipe.name}” worden verwijderd.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmStop = false
                    BakeStore.finish(aborted = true)
                    nav.root(Screen.Home)
                }) { Text("Stoppen") }
            },
            dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Annuleren") } },
        )
    }
}

@Composable
private fun TimelineRow(p: PlannedStep, now: Long, expanded: Boolean, onClick: () -> Unit, onAdjust: (Int) -> Unit) {
    val color = when (p.status) {
        StepStatus.DONE -> MaterialTheme.colorScheme.outline
        StepStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        StepStatus.PLANNED -> MaterialTheme.colorScheme.onSurface
    }
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(14.dp).clip(CircleShape).background(
                    if (p.status == StepStatus.PLANNED) MaterialTheme.colorScheme.surfaceVariant else color,
                ),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                TimeFormat.clock(p.dueAt, now),
                modifier = Modifier.width(92.dp),
                color = color,
                fontWeight = if (p.status == StepStatus.ACTIVE) FontWeight.Bold else FontWeight.Normal,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    p.step.name,
                    color = color,
                    fontWeight = if (p.status == StepStatus.ACTIVE) FontWeight.Bold else FontWeight.Normal,
                )
                val status = when (p.status) {
                    StepStatus.DONE -> "klaar"
                    StepStatus.ACTIVE -> if (p.dueAt > now) "bezig" else "nu"
                    StepStatus.PLANNED -> if (p.step.kind == StepKind.MANUAL) "wacht op jou" else "gepland"
                }
                Text(status, style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
        p.preheatAt?.let {
            if (p.status != StepStatus.DONE) {
                Text(
                    "${p.step.preheatText}: ${TimeFormat.clock(it, now)}",
                    modifier = Modifier.padding(start = 118.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        if (expanded) {
            Stepper(
                value = "verschuif",
                label = "Deze stap en alles erna",
                onMinus = { onAdjust(-5) },
                onPlus = { onAdjust(5) },
                modifier = Modifier.fillMaxWidth().padding(start = 26.dp, top = 6.dp),
            )
        }
    }
}
