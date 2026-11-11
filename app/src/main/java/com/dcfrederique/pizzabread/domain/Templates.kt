package com.dcfrederique.pizzabread.domain

import com.dcfrederique.pizzabread.domain.StepKind.AUTO
import com.dcfrederique.pizzabread.domain.StepKind.MANUAL

/** Ingebouwde sjablonen. Tijden zijn startwaarden voor een keuken van ongeveer 24 °C. */
object Templates {

    private fun stretchAndFolds(count: Int, every: Int) = (1..count).map {
        Step(
            name = "Stretch & fold $it van $count",
            waitMinutes = every,
            kind = AUTO,
            instruction = "Trek het deeg aan vier kanten omhoog en vouw het dicht.",
        )
    }

    /** Uit de oven ± 7 u 15 na Start rijzing, aansnijden na 8 u 15. */
    val sourdoughSameDay = Recipe(
        id = "tpl-desem-dag",
        name = "Desembrood, zelfde dag",
        category = Category.BREAD,
        builtIn = true,
        description = "Bulk 4 u 30 met 4 stretch & folds, narijzen 1 u 30 op het aanrecht.",
        steps = stretchAndFolds(4, 30) + listOf(
            Step("Einde bulkrijs: voorvormen", 150, MANUAL, workMinutes = 5,
                instruction = "Deeg op het werkblad, losjes rond vormen. Tik Gedaan zodra het rust."),
            Step("Eindvormen, in rijsmand", 15, MANUAL, workMinutes = 10,
                instruction = "Na de bankrust strak opvormen en met de naad boven in de rijsmand."),
            Step("In de oven, met deksel", 90, AUTO, preheatMinutes = 45,
                preheatText = "Oven voorverwarmen, 250 °C met pot",
                instruction = "Insnijden en met deksel in de pot, 250 °C."),
            Step("Deksel eraf, 230 °C", 20, AUTO, instruction = "Deksel eraf en de oven op 230 °C."),
            Step("Uit de oven", 25, AUTO, instruction = "Brood uit de oven, op een rooster laten afkoelen."),
            Step("Afgekoeld, aansnijden", 60, AUTO, instruction = "Smakelijk!"),
        ),
    )

    val sourdoughOvernight = Recipe(
        id = "tpl-desem-nacht",
        name = "Desembrood, nacht in de koelkast",
        category = Category.BREAD,
        builtIn = true,
        description = "Zelfde bulk en vormen, daarna 12 u narijzen in de koelkast.",
        steps = stretchAndFolds(4, 30) + listOf(
            Step("Einde bulkrijs: voorvormen", 150, MANUAL, workMinutes = 5,
                instruction = "Deeg op het werkblad, losjes rond vormen. Tik Gedaan zodra het rust."),
            Step("Eindvormen, in de koelkast", 15, MANUAL, workMinutes = 10,
                instruction = "Strak opvormen, in de rijsmand en afgedekt in de koelkast."),
            Step("Uit de koelkast, in de oven", 720, AUTO, preheatMinutes = 45,
                preheatText = "Oven voorverwarmen, 250 °C met pot",
                instruction = "Recht uit de koelkast insnijden, met deksel in de pot."),
            Step("Deksel eraf, 230 °C", 20, AUTO, instruction = "Deksel eraf en de oven op 230 °C."),
            Step("Uit de oven", 25, AUTO, instruction = "Brood uit de oven, op een rooster laten afkoelen."),
            Step("Afgekoeld, aansnijden", 60, AUTO, instruction = "Smakelijk!"),
        ),
    )

    val quickYeast = Recipe(
        id = "tpl-gist-snel",
        name = "Snel gistbrood",
        category = Category.BREAD,
        builtIn = true,
        description = "Klaar in ongeveer 3 uur.",
        steps = listOf(
            Step("Kneden", 0, MANUAL, workMinutes = 10, instruction = "Kneed tot het deeg glad en elastisch is."),
            Step("Vormen", 60, MANUAL, workMinutes = 10, instruction = "Deeg is verdubbeld: vormen en in de vorm leggen."),
            Step("In de oven", 45, AUTO, preheatMinutes = 30,
                preheatText = "Oven voorverwarmen, 220 °C",
                instruction = "Brood in de oven, 220 °C."),
            Step("Uit de oven", 35, AUTO, instruction = "Brood uit de oven, op een rooster laten afkoelen."),
        ),
    )

    val pizzaNeapolitan = Recipe(
        id = "tpl-pizza-napoli",
        name = "Pizzadeeg Napolitaans",
        category = Category.PIZZA,
        builtIn = true,
        description = "Bulk 2 u, 18 u koelkast, 2 u op temperatuur.",
        steps = listOf(
            Step("Kneden", 0, MANUAL, workMinutes = 15, instruction = "Kneed tot het deeg glad is."),
            Step("Bollen draaien, in de koelkast", 120, MANUAL, workMinutes = 10,
                instruction = "Verdeel in bollen, in een afgedekte bak in de koelkast."),
            Step("Bollen uit de koelkast", 18 * 60, AUTO, instruction = "Laat de bollen op temperatuur komen."),
            Step("Pizza's bakken", 120, AUTO, preheatMinutes = 45,
                preheatText = "Oven of pizzaoven voorverwarmen",
                instruction = "Uitduwen, beleggen en bakken: 60–90 s in de pizzaoven."),
        ),
    )

    val pizzaRoman = Recipe(
        id = "tpl-pizza-romana",
        name = "Pizzadeeg Romeins (tonda)",
        category = Category.PIZZA,
        builtIn = true,
        description = "Bulk 1 u, 24 u koelkast, 1 u op temperatuur.",
        steps = listOf(
            Step("Kneden", 0, MANUAL, workMinutes = 10, instruction = "Kneed, voeg de olie op het einde toe."),
            Step("Bollen draaien, in de koelkast", 60, MANUAL, workMinutes = 10,
                instruction = "Verdeel in bollen, in een afgedekte bak in de koelkast."),
            Step("Bollen uit de koelkast", 24 * 60, AUTO, instruction = "Laat de bollen op temperatuur komen."),
            Step("Pizza's bakken", 60, AUTO, preheatMinutes = 45,
                preheatText = "Oven op maximum met steen",
                instruction = "Heel dun uitrollen, beleggen en bakken."),
        ),
    )

    val bread = listOf(sourdoughSameDay, sourdoughOvernight, quickYeast)
    val pizza = listOf(pizzaNeapolitan, pizzaRoman)
    val all = bread + pizza

    fun pizzaFor(style: PizzaStyle): Recipe = when (style) {
        PizzaStyle.NEAPOLITAN -> pizzaNeapolitan
        PizzaStyle.ROMAN -> pizzaRoman
    }
}
