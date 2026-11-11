@file:OptIn(ExperimentalMaterial3Api::class)

package com.dcfrederique.pizzabread.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dcfrederique.pizzabread.domain.DoughCalculator
import com.dcfrederique.pizzabread.domain.DoughInput
import com.dcfrederique.pizzabread.domain.PizzaStyle
import com.dcfrederique.pizzabread.domain.Planner
import com.dcfrederique.pizzabread.domain.Templates
import com.dcfrederique.pizzabread.domain.TimeFormat
import kotlin.math.roundToInt

/** Pizzadeeg-calculator met bakkerspercentages, en daarna de deeg-timers starten. */
@Composable
fun CalculatorScreen(nav: Navigator, modifier: Modifier = Modifier) {
    var style by rememberSaveable { mutableStateOf(PizzaStyle.NEAPOLITAN) }
    var count by rememberSaveable { mutableStateOf(4) }
    var ballWeight by rememberSaveable { mutableStateOf(PizzaStyle.NEAPOLITAN.defaultBallWeight) }
    var hydrationPct by rememberSaveable { mutableStateOf((PizzaStyle.NEAPOLITAN.defaultHydration * 100).roundToInt()) }
    var dryYeast by rememberSaveable { mutableStateOf(false) }

    val input = DoughInput(style, count, ballWeight, hydrationPct / 100.0, dryYeast)
    val r = DoughCalculator.calculate(input)
    val recipe = Templates.pizzaFor(style)

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { PlainTopBar("Pizzadeeg") },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                PizzaStyle.entries.forEachIndexed { i, s ->
                    SegmentedButton(
                        selected = style == s,
                        onClick = {
                            style = s
                            ballWeight = s.defaultBallWeight
                            hydrationPct = (s.defaultHydration * 100).roundToInt()
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, PizzaStyle.entries.size),
                    ) { Text(s.label) }
                }
            }
            Text(
                if (style == PizzaStyle.NEAPOLITAN) "Luchtige rand, tipo 00, geen olie." else "Dun en krokant (tonda), met olijfolie.",
                style = MaterialTheme.typography.bodySmall,
            )

            Stepper(
                label = "Aantal pizza's",
                value = "$count",
                onMinus = { count = (count - 1).coerceAtLeast(1) },
                onPlus = { count = (count + 1).coerceAtMost(30) },
                modifier = Modifier.fillMaxWidth(),
            )
            Stepper(
                label = "Gewicht per bol",
                value = "$ballWeight g",
                onMinus = { ballWeight = (ballWeight - 10).coerceAtLeast(150) },
                onPlus = { ballWeight = (ballWeight + 10).coerceAtMost(350) },
                modifier = Modifier.fillMaxWidth(),
            )
            Column {
                val range = style.hydrationRange
                val lo = (range.start * 100).roundToInt()
                val hi = (range.endInclusive * 100).roundToInt()
                Row {
                    Text("Hydratatie", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text("$hydrationPct %", style = MaterialTheme.typography.titleMedium)
                }
                Slider(
                    value = hydrationPct.toFloat(),
                    onValueChange = { hydrationPct = it.roundToInt() },
                    valueRange = lo.toFloat()..hi.toFloat(),
                    steps = (hi - lo - 1).coerceAtLeast(0),
                )
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = !dryYeast, onClick = { dryYeast = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                    Text("Verse gist")
                }
                SegmentedButton(selected = dryYeast, onClick = { dryYeast = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                    Text("Droge gist")
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Afwegen voor ${DoughCalculator.grams(r.totalDough)} deeg", style = MaterialTheme.typography.titleMedium)
                    Ingredient("Bloem (${style.flour})", r.flour)
                    Ingredient("Water", r.water)
                    Ingredient("Zout", r.salt)
                    Ingredient(if (dryYeast) "Droge gist" else "Verse gist", r.yeast)
                    if (r.oil > 0) Ingredient("Olijfolie", r.oil)
                }
            }

            Text(
                "Timers: ${recipe.description} Klaar na ongeveer ${TimeFormat.duration(Planner.totalMinutes(recipe))}.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = { nav.go(Screen.Review(recipe, DoughCalculator.summary(input))) },
                modifier = Modifier.fillMaxWidth().height(60.dp),
            ) { Text("Verder naar deeg-timers", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Ingredient(name: String, grams: Double) {
    Row {
        Text(name, modifier = Modifier.weight(1f))
        Text(DoughCalculator.grams(grams), fontWeight = FontWeight.Bold)
    }
    HorizontalDivider()
}

