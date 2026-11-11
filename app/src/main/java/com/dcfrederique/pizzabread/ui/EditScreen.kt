@file:OptIn(ExperimentalMaterial3Api::class)

package com.dcfrederique.pizzabread.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dcfrederique.pizzabread.data.BakeStore
import com.dcfrederique.pizzabread.domain.Category
import com.dcfrederique.pizzabread.domain.Recipe
import com.dcfrederique.pizzabread.domain.Step
import com.dcfrederique.pizzabread.domain.StepKind
import com.dcfrederique.pizzabread.domain.TimeFormat

/** Recept bewerken: naam, stappen toevoegen, verwijderen en herordenen. Sjablonen worden een kopie. */
@Composable
fun EditScreen(initial: Recipe, nav: Navigator, modifier: Modifier = Modifier) {
    var name by remember(initial) {
        mutableStateOf(if (initial.builtIn) "${initial.name} (eigen)" else initial.name)
    }
    var category by remember(initial) { mutableStateOf(initial.category) }
    var steps by remember(initial) { mutableStateOf(initial.steps) }

    fun update(i: Int, change: (Step) -> Step) {
        steps = steps.toMutableList().also { it[i] = change(it[i]) }
    }

    val canSave = name.isNotBlank() && steps.isNotEmpty() && steps.all { it.name.isNotBlank() }

    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopBar(if (initial.id.isBlank()) "Nieuw recept" else "Recept bewerken", onBack = { nav.back() }) {
                TextButton(
                    enabled = canSave,
                    onClick = {
                        BakeStore.saveRecipe(
                            initial.copy(name = name.trim(), category = category, steps = steps, description = initial.description),
                        )
                        nav.root(Screen.Recipes)
                    },
                ) { Text("Bewaar") }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Naam") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Category.entries.forEachIndexed { i, c ->
                        SegmentedButton(
                            selected = category == c,
                            onClick = { category = c },
                            shape = SegmentedButtonDefaults.itemShape(i, Category.entries.size),
                        ) { Text(if (c == Category.BREAD) "Brood" else "Pizzadeeg") }
                    }
                }
            }
            items(steps.size) { i ->
                val s = steps[i]
                Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Stap ${i + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            IconButton(enabled = i > 0, onClick = {
                                steps = steps.toMutableList().also { l -> l.add(i - 1, l.removeAt(i)) }
                            }) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Omhoog") }
                            IconButton(enabled = i < steps.lastIndex, onClick = {
                                steps = steps.toMutableList().also { l -> l.add(i + 1, l.removeAt(i)) }
                            }) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Omlaag") }
                            IconButton(enabled = steps.size > 1, onClick = {
                                steps = steps.toMutableList().also { l -> l.removeAt(i) }
                            }) { Icon(Icons.Default.Delete, contentDescription = "Verwijderen") }
                        }
                        OutlinedTextField(
                            value = s.name,
                            onValueChange = { v -> update(i) { it.copy(name = v) } },
                            label = { Text("Wat doe je dan?") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Stepper(
                            label = if (i == 0) "Na start" else "Na vorige stap",
                            value = TimeFormat.duration(s.waitMinutes),
                            onMinus = { update(i) { it.copy(waitMinutes = (it.waitMinutes - minuteStep(it.waitMinutes, false)).coerceAtLeast(0)) } },
                            onPlus = { update(i) { it.copy(waitMinutes = it.waitMinutes + minuteStep(it.waitMinutes, true)) } },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = s.kind == StepKind.AUTO,
                                onClick = { update(i) { it.copy(kind = StepKind.AUTO) } },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                            ) { Text("Op de klok") }
                            SegmentedButton(
                                selected = s.kind == StepKind.MANUAL,
                                onClick = { update(i) { it.copy(kind = StepKind.MANUAL, workMinutes = it.workMinutes.coerceAtLeast(5)) } },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                            ) { Text("Wacht op mij") }
                        }
                        if (s.kind == StepKind.MANUAL) {
                            Stepper(
                                label = "Werk (schatting)",
                                value = TimeFormat.duration(s.workMinutes),
                                onMinus = { update(i) { it.copy(workMinutes = (it.workMinutes - 5).coerceAtLeast(0)) } },
                                onPlus = { update(i) { it.copy(workMinutes = it.workMinutes + 5) } },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Stepper(
                            label = "Oven aan vooraf",
                            value = if (s.preheatMinutes == 0) "nee" else TimeFormat.duration(s.preheatMinutes),
                            onMinus = { update(i) { it.copy(preheatMinutes = (it.preheatMinutes - 15).coerceAtLeast(0)) } },
                            onPlus = { update(i) { it.copy(preheatMinutes = it.preheatMinutes + 15) } },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (s.preheatMinutes > 0) {
                            OutlinedTextField(
                                value = s.preheatText,
                                onValueChange = { v -> update(i) { it.copy(preheatText = v) } },
                                label = { Text("Tekst oven-alarm") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        OutlinedTextField(
                            value = s.instruction,
                            onValueChange = { v -> update(i) { it.copy(instruction = v) } },
                            label = { Text("Instructie op het alarm") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { steps = steps + Step("Nieuwe stap", 30) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Stap toevoegen")
                }
            }
        }
    }
}
