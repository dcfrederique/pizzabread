package com.dcfrederique.pizzabread.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerTest {
    private val t0 = 1_700_000_000_000L
    private fun min(m: Int) = m * MINUTE_MS
    private fun hm(h: Int, m: Int) = min(h * 60 + m)

    private val recipe = Templates.sourdoughSameDay

    @Test
    fun `desem zelfde dag volgt de tijden uit de analyse`() {
        val plan = Planner.plan(Planner.start(recipe, t0), t0)
        val due = plan.steps.map { it.dueAt - t0 }
        assertEquals(listOf(min(30), min(60), min(90), min(120)), due.take(4))
        assertEquals(hm(4, 30), due[4]) // einde bulk
        assertEquals(hm(4, 50), plan.steps[5].dueAt - t0) // eindvormen na bankrust (schatting)
        assertEquals(hm(6, 30), plan.steps[6].dueAt - t0) // in de oven
        assertEquals(hm(5, 45), plan.steps[6].preheatAt!! - t0) // oven voorverwarmen
        assertEquals(hm(6, 50), plan.steps[7].dueAt - t0)
        assertEquals(hm(7, 15), plan.steps[8].dueAt - t0) // uit de oven
        assertEquals(hm(8, 15), plan.finishAt - t0)
        assertEquals(StepStatus.ACTIVE, plan.steps[0].status)
    }

    @Test
    fun `start plant alle alarmen meteen`() {
        val bake = Planner.start(recipe, t0)
        val events = Planner.events(bake, t0)
        assertEquals(recipe.steps.size, events.count { it.type == EventType.STEP })
        assertEquals(1, events.count { it.type == EventType.PREHEAT })
        assertEquals(t0 + min(30), events.first().at)
    }

    @Test
    fun `op de klok stappen schuiven niet als je het alarm later afzet`() {
        var bake = Planner.start(recipe, t0)
        bake = Planner.done(bake, 0, t0 + min(33))
        assertEquals(t0 + min(60), Planner.plan(bake, t0 + min(33)).steps[1].dueAt)
    }

    @Test
    fun `eerder gedaan schuift de rest naar voren`() {
        var bake = Planner.start(recipe, t0)
        bake = Planner.done(bake, 0, t0 + min(25))
        assertEquals(t0 + min(55), Planner.plan(bake, t0 + min(25)).steps[1].dueAt)
    }

    @Test
    fun `handwerk wacht op Gedaan en de planning schuift mee`() {
        var bake = Planner.start(recipe, t0)
        val late = t0 + hm(4, 50) // 20 min na einde bulk nog niet voorgevormd
        var plan = Planner.plan(bake, late)
        assertEquals(4, plan.active!!.index)
        // eindvormen: nu + 5 min werk + 15 min bankrust
        assertEquals(late + min(20), plan.steps[5].dueAt)
        bake = Planner.done(bake, 4, late)
        plan = Planner.plan(bake, late)
        assertEquals(5, plan.active!!.index)
        assertEquals(late + min(15), plan.steps[5].dueAt)
    }

    @Test
    fun `handwerk dat nog openstaat krijgt herinneringen`() {
        val bake = Planner.start(recipe, t0)
        val now = t0 + hm(4, 32)
        val first = Planner.events(bake, now).first()
        assertEquals(EventType.REMINDER, first.type)
        // 5 min geschatte werktijd + 5 min
        assertEquals(t0 + hm(4, 40), first.at)
    }

    @Test
    fun `nog 5 min schuift de stap en alles erna`() {
        var bake = Planner.start(recipe, t0)
        val ringing = t0 + min(31)
        bake = Planner.snooze(bake, 0, ringing)
        val plan = Planner.plan(bake, ringing)
        assertEquals(0, plan.active!!.index)
        assertEquals(t0 + min(36), plan.steps[0].dueAt)
        assertEquals(t0 + min(66), plan.steps[1].dueAt)
    }

    @Test
    fun `pauze stopt de klok en hervatten schuift alles op`() {
        var bake = Planner.start(recipe, t0)
        bake = Planner.pause(bake, t0 + min(10))
        assertTrue(Planner.events(bake, t0 + min(40)).isEmpty())
        // tijdens de pauze is stap 1 niet stiekem voorbij gegaan
        assertEquals(0, Planner.plan(bake, t0 + min(40)).active!!.index)
        bake = Planner.resume(bake, t0 + min(40))
        assertNull(bake.pausedAt)
        assertEquals(t0 + min(60), Planner.plan(bake, t0 + min(40)).steps[0].dueAt)
    }

    @Test
    fun `duur aanpassen kan niet vóór de vorige stap`() {
        var bake = Planner.start(recipe, t0)
        bake = Planner.adjust(bake, 0, -60)
        assertEquals(t0, Planner.plan(bake, t0).steps[0].dueAt)
    }

    @Test
    fun `alarmen op hetzelfde moment worden samengevoegd`() {
        val r = Recipe("x", "x", Category.BREAD, listOf(
            Step("A", 30), Step("B", 30, preheatMinutes = 30, preheatText = "Oven aan"),
        ))
        val group = Planner.nextGroup(Planner.start(r, t0), t0)
        assertEquals(listOf("A", "Oven aan"), group.map { it.title })
    }

    @Test
    fun `alarm voor een al afgevinkte stap verandert niets`() {
        var bake = Planner.start(recipe, t0)
        val after = t0 + min(31) // stap 0 is voorbij, stap 1 is actief
        val same = Planner.done(bake, 0, after)
        assertEquals(bake, same)
        bake = Planner.done(bake, 1, after) // stap 1 eerder gedaan
        assertEquals(after + min(30), Planner.plan(bake, after).steps[2].dueAt)
    }

    @Test
    fun `bak is klaar na de laatste stap`() {
        val bake = Planner.start(recipe, t0)
        var b = bake
        b = Planner.done(b, 4, t0 + hm(4, 30))
        b = Planner.done(b, 5, t0 + hm(4, 50))
        assertTrue(Planner.plan(b, t0 + hm(9, 0)).isFinished)
    }
}
