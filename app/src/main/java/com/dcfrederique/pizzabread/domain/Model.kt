package com.dcfrederique.pizzabread.domain

/**
 * Hoe een stap eindigt.
 *
 * [AUTO] ("op de klok"): de stap is een moment op de klok, zoals een stretch & fold of
 * "deksel eraf". De volgende wachttijd telt vanaf het geplande tijdstip, ook als je
 * het alarm pas later afzet.
 *
 * [MANUAL] ("wacht op mij"): handwerk zoals kneden of vormen. De volgende wachttijd telt
 * pas vanaf het moment dat je op Gedaan tikt. Tot dan rekent de planning met
 * [Step.workMinutes] als schatting.
 */
enum class StepKind { AUTO, MANUAL }

enum class Category { BREAD, PIZZA }

data class Step(
    /** Wat je op dat moment doet, bv. "Stretch & fold 1". */
    val name: String,
    /** Wachttijd in minuten tussen het einde van de vorige stap en deze stap. */
    val waitMinutes: Int,
    val kind: StepKind = StepKind.AUTO,
    /** Geschatte duur van het handwerk (alleen voor [StepKind.MANUAL]). */
    val workMinutes: Int = 0,
    /** Korte instructie die op het alarm staat. */
    val instruction: String = "",
    /** > 0: extra alarm om de oven zoveel minuten vóór deze stap aan te zetten. */
    val preheatMinutes: Int = 0,
    val preheatText: String = "Oven voorverwarmen",
)

data class Recipe(
    val id: String,
    val name: String,
    val category: Category,
    val steps: List<Step>,
    val builtIn: Boolean = false,
    val description: String = "",
)

/**
 * Een lopende bak. Alle tijden zijn absolute tijdstippen (epoch millis), zodat de app
 * altijd de juiste stand kan tonen, ook na een herstart.
 */
data class Bake(
    val recipe: Recipe,
    val startedAt: Long,
    /** Per stap: wanneer je op Gedaan tikte, of null. */
    val completedAt: List<Long?> = List(recipe.steps.size) { null },
    /** Per stap: extra verschuiving in ms (uitstellen, pauze, duur aanpassen). */
    val delayMs: List<Long> = List(recipe.steps.size) { 0L },
    /** Niet-null zolang de bak op pauze staat. */
    val pausedAt: Long? = null,
    /** Vrije notitie, bv. de hoeveelheden uit de pizzacalculator. */
    val note: String = "",
)

/** Een afgewerkte of afgebroken bak, voor "Opnieuw bakken". */
data class RecentBake(
    val recipe: Recipe,
    val finishedAt: Long,
    val aborted: Boolean,
    val note: String = "",
)

const val MINUTE_MS = 60_000L
