package com.dcfrederique.pizzabread.domain

import kotlin.math.roundToInt

enum class PizzaStyle(
    val label: String,
    val flour: String,
    val defaultBallWeight: Int,
    val defaultHydration: Double,
    val hydrationRange: ClosedFloatingPointRange<Double>,
    val salt: Double,
    val freshYeast: Double,
    val oil: Double,
) {
    NEAPOLITAN("Napolitaans", "tipo 00", 260, 0.63, 0.60..0.65, 0.03, 0.0015, 0.0),
    ROMAN("Romeins", "tipo 0 of 00", 190, 0.58, 0.55..0.60, 0.025, 0.005, 0.04),
}

data class DoughInput(
    val style: PizzaStyle,
    val count: Int,
    val ballWeight: Int = style.defaultBallWeight,
    val hydration: Double = style.defaultHydration,
    val dryYeast: Boolean = false,
)

data class DoughResult(
    val totalDough: Double,
    val flour: Double,
    val water: Double,
    val salt: Double,
    val yeast: Double,
    val oil: Double,
)

/**
 * Rekent met bakkerspercentages: bloem = totaal deeggewicht ÷ (1 + hydratatie + zout + gist + olie).
 * De andere ingrediënten zijn bloem × hun percentage. Droge gist = verse gist × 0,4.
 */
object DoughCalculator {
    const val DRY_YEAST_FACTOR = 0.4

    fun calculate(input: DoughInput): DoughResult {
        val s = input.style
        val yeastPct = s.freshYeast * if (input.dryYeast) DRY_YEAST_FACTOR else 1.0
        val total = input.count * input.ballWeight.toDouble()
        val flour = total / (1 + input.hydration + s.salt + yeastPct + s.oil)
        return DoughResult(
            totalDough = total,
            flour = flour,
            water = flour * input.hydration,
            salt = flour * s.salt,
            yeast = flour * yeastPct,
            oil = flour * s.oil,
        )
    }

    /** Gram afgerond: hele grammen boven 20 g, anders één decimaal. */
    fun grams(value: Double): String =
        if (value >= 20) "${value.roundToInt()} g"
        else "${"%.1f".format(java.util.Locale.ROOT, value).replace('.', ',')} g"

    fun summary(input: DoughInput): String {
        val r = calculate(input)
        val yeastName = if (input.dryYeast) "droge gist" else "verse gist"
        return buildString {
            append("${input.count} × ${input.ballWeight} g, ${(input.hydration * 100).roundToInt()} % hydratatie\n")
            append("${grams(r.flour)} bloem (${input.style.flour}), ${grams(r.water)} water, ")
            append("${grams(r.salt)} zout, ${grams(r.yeast)} $yeastName")
            if (r.oil > 0) append(", ${grams(r.oil)} olijfolie")
        }
    }
}
