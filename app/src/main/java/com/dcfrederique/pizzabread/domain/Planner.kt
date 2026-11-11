package com.dcfrederique.pizzabread.domain

import kotlin.math.max
import kotlin.math.min

enum class StepStatus { DONE, ACTIVE, PLANNED }

data class PlannedStep(
    val index: Int,
    val step: Step,
    /** Wanneer de stap aan de beurt is (hier gaat het alarm af). */
    val dueAt: Long,
    /** Wanneer de stap klaar is; voor handwerk een schatting zolang je niet op Gedaan tikte. */
    val endAt: Long,
    val status: StepStatus,
) {
    val preheatAt: Long? get() = if (step.preheatMinutes > 0) dueAt - step.preheatMinutes * MINUTE_MS else null
}

enum class EventType { STEP, REMINDER, PREHEAT, HEADS_UP }

data class AlarmEvent(
    val at: Long,
    val type: EventType,
    val stepIndex: Int,
    val title: String,
    val text: String,
) {
    /** Alleen stap-alarmen en herinneringen maken echt lawaai; een vooraankondiging niet. */
    val rings: Boolean get() = type != EventType.HEADS_UP
}

data class Plan(val steps: List<PlannedStep>) {
    val active: PlannedStep? get() = steps.firstOrNull { it.status == StepStatus.ACTIVE }
    val finishAt: Long get() = steps.lastOrNull()?.endAt ?: 0L
    val doneCount: Int get() = steps.count { it.status == StepStatus.DONE }
    val isFinished: Boolean get() = steps.all { it.status == StepStatus.DONE }
}

object Planner {
    const val HEADS_UP_MINUTES = 5
    const val REMINDER_MINUTES = 5
    const val SNOOZE_MINUTES = 5

    /**
     * Rekent de volledige tijdlijn uit. Elke stap hangt aan het einde van de vorige, dus
     * een wijziging bij één stap schuift alles erna automatisch mee.
     */
    fun plan(bake: Bake, now: Long): Plan {
        val clock = bake.pausedAt ?: now
        var anchor = bake.startedAt
        var activeFound = false
        val result = bake.recipe.steps.mapIndexed { i, step ->
            val due = anchor + step.waitMinutes * MINUTE_MS + bake.delayMs.getOrElse(i) { 0L }
            val completed = bake.completedAt.getOrNull(i)
            val end: Long
            val done: Boolean
            when (step.kind) {
                StepKind.MANUAL -> {
                    end = completed ?: (max(due, clock) + step.workMinutes * MINUTE_MS)
                    done = completed != null
                }
                StepKind.AUTO -> {
                    // Eerder op Gedaan getikt: de rest schuift naar voren. Later afgezet: planning blijft.
                    end = if (completed != null) min(completed, due) else due
                    done = completed != null || due <= clock
                }
            }
            anchor = end
            val status = when {
                done && !activeFound -> StepStatus.DONE
                !activeFound -> { activeFound = true; StepStatus.ACTIVE }
                else -> StepStatus.PLANNED
            }
            PlannedStep(i, step, due, end, status)
        }
        return Plan(result)
    }

    /** Alle toekomstige alarmen na [after], gesorteerd. Leeg als de bak op pauze staat. */
    fun events(bake: Bake, now: Long, after: Long = now): List<AlarmEvent> {
        if (bake.pausedAt != null) return emptyList()
        val plan = plan(bake, now)
        val total = bake.recipe.steps.size
        val out = mutableListOf<AlarmEvent>()
        for (p in plan.steps) {
            if (p.status == StepStatus.DONE) continue
            val s = p.step
            val label = stepLabel(bake.recipe.steps, p.index)
            if (p.dueAt > after) {
                val text = if (s.kind == StepKind.MANUAL) if ("Gedaan" in s.instruction) s.instruction else (s.instruction + " Tik op Gedaan als je klaar bent.").trim() else s.instruction
                out += AlarmEvent(p.dueAt, EventType.STEP, p.index, label, text)
            } else if (s.kind == StepKind.MANUAL) {
                // Handwerk dat nog niet afgevinkt is: herinneren zodra de geschatte werktijd
                // om is, daarna elke 5 minuten tot je op Gedaan tikt.
                val period = REMINDER_MINUTES * MINUTE_MS
                val base = p.dueAt + s.workMinutes * MINUTE_MS
                val k = if (after < base) 1 else (after - base) / period + 1
                out += AlarmEvent(base + k * period, EventType.REMINDER, p.index, label, "Tik op Gedaan als je klaar bent.")
            }
            p.preheatAt?.let { at ->
                if (at > after) out += AlarmEvent(at, EventType.PREHEAT, p.index, s.preheatText, "Over ${s.preheatMinutes} min: ${s.name}")
            }
            val headsUpUseful = (s.kind == StepKind.MANUAL || s.preheatMinutes > 0) && s.waitMinutes >= 2 * HEADS_UP_MINUTES
            if (headsUpUseful) {
                val at = p.dueAt - HEADS_UP_MINUTES * MINUTE_MS
                if (at > after) out += AlarmEvent(at, EventType.HEADS_UP, p.index, "Over $HEADS_UP_MINUTES min: ${s.name}", "")
            }
        }
        if (total == 0) return emptyList()
        return out.sortedWith(compareBy({ it.at }, { it.type.ordinal }))
    }

    /**
     * De eerstvolgende groep alarmen: alles binnen dezelfde minuut als het eerste alarm.
     * Zo worden twee stappen op hetzelfde moment één alarm ("Stretch & fold 4 + oven aan").
     */
    fun nextGroup(bake: Bake, now: Long, after: Long = now): List<AlarmEvent> {
        val all = events(bake, now, after)
        val first = all.firstOrNull() ?: return emptyList()
        return all.filter { it.at - first.at < MINUTE_MS }
    }

    fun stepLabel(steps: List<Step>, index: Int): String = steps[index].name

    // ---- Acties ----

    /** Gedaan voor stap [index]. Een stap die al klaar is, blijft ongewijzigd. */
    fun done(bake: Bake, index: Int, now: Long): Bake {
        val plan = plan(bake, now)
        val p = plan.steps.getOrNull(index) ?: return bake
        if (p.status == StepStatus.DONE) return bake
        // Stappen die nog vóór deze stap openstaan, worden mee afgevinkt.
        val completed = bake.completedAt.toMutableList()
        for (i in 0..index) if (plan.steps[i].status != StepStatus.DONE) completed[i] = now
        return bake.copy(completedAt = completed)
    }

    /** Gedaan voor de huidige stap. */
    fun doneActive(bake: Bake, now: Long): Bake {
        val active = plan(bake, now).active ?: return bake
        return done(bake, active.index, now)
    }

    /**
     * "Nog 5 min": de stap gaat 5 minuten na nu (of na het geplande tijdstip, als dat later
     * is) opnieuw af. Alles erna schuift mee.
     */
    fun snooze(bake: Bake, index: Int, now: Long, minutes: Int = SNOOZE_MINUTES): Bake {
        val p = plan(bake, now).steps.getOrNull(index) ?: return bake
        if (bake.completedAt.getOrNull(index) != null) return bake
        val newDue = max(p.dueAt, now) + minutes * MINUTE_MS
        return shift(bake, index, newDue - p.dueAt)
    }

    /** Verschuif stap [index] (en dus alles erna) met [deltaMinutes]; nooit vóór de vorige stap. */
    fun adjust(bake: Bake, index: Int, deltaMinutes: Int): Bake {
        val step = bake.recipe.steps.getOrNull(index) ?: return bake
        val current = bake.delayMs[index]
        val minDelay = -step.waitMinutes * MINUTE_MS
        val newDelay = max(minDelay, current + deltaMinutes * MINUTE_MS)
        return shift(bake, index, newDelay - current)
    }

    fun pause(bake: Bake, now: Long): Bake = if (bake.pausedAt != null) bake else bake.copy(pausedAt = now)

    fun resume(bake: Bake, now: Long): Bake {
        val pausedAt = bake.pausedAt ?: return bake
        val active = plan(bake, now).active
        val resumed = bake.copy(pausedAt = null)
        return if (active == null) resumed else shift(resumed, active.index, max(0L, now - pausedAt))
    }

    private fun shift(bake: Bake, index: Int, deltaMs: Long): Bake {
        val delays = bake.delayMs.toMutableList()
        delays[index] = delays[index] + deltaMs
        return bake.copy(delayMs = delays)
    }

    /** Start een bak. */
    fun start(recipe: Recipe, now: Long, note: String = ""): Bake = Bake(recipe = recipe, startedAt = now, note = note)

    /** Totale geplande duur van een recept, met de schattingen voor handwerk. */
    fun totalMinutes(recipe: Recipe): Int = recipe.steps.sumOf { it.waitMinutes + if (it.kind == StepKind.MANUAL) it.workMinutes else 0 }

    /** Aantal alarmen dat Start rijzing in één keer plant. */
    fun alarmCount(recipe: Recipe): Int = recipe.steps.size + recipe.steps.count { it.preheatMinutes > 0 }
}
