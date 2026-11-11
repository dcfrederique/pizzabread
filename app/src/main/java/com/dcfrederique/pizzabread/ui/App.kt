package com.dcfrederique.pizzabread.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcfrederique.pizzabread.data.BakeStore
import com.dcfrederique.pizzabread.domain.Recipe
import kotlinx.coroutines.delay

sealed interface Screen {
    data object Home : Screen
    data object Recipes : Screen
    data object Calculator : Screen
    data object Active : Screen
    data class Review(val recipe: Recipe, val note: String = "") : Screen
    data class Edit(val recipe: Recipe) : Screen
}

private enum class Tab(val label: String, val icon: ImageVector, val screen: Screen) {
    HOME("Home", Icons.Default.Home, Screen.Home),
    RECIPES("Recepten", Icons.AutoMirrored.Filled.List, Screen.Recipes),
    CALCULATOR("Calculator", Icons.Default.Star, Screen.Calculator),
}

/** Eenvoudige navigatie: een stapel schermen, met Home · Recepten · Calculator als basis. */
class Navigator {
    val stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = stack.last()
    fun go(screen: Screen) { stack.add(screen) }
    fun back(): Boolean = if (stack.size > 1) { stack.removeAt(stack.lastIndex); true } else false
    fun root(screen: Screen) { stack.clear(); stack.add(screen) }
    /** Na Start: terug naar Home en meteen het actieve scherm open. */
    fun showActive() { stack.clear(); stack.add(Screen.Home); stack.add(Screen.Active) }
}

@Composable
fun App() {
    val nav = remember { Navigator() }
    val state by BakeStore.state.collectAsStateWithLifecycle()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000L - value % 1000L)
        }
    }

    BackHandler(enabled = nav.stack.size > 1) { nav.back() }

    val current = nav.current
    val rootTab = Tab.entries.firstOrNull { it.screen == nav.stack.first() }
    val showBottomBar = current is Screen.Home || current is Screen.Recipes || current is Screen.Calculator

    Scaffold(
        // Elk scherm regelt zelf de statusbalk; zo komt er geen dubbele ruimte bovenaan.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = rootTab == tab,
                            onClick = { nav.root(tab.screen) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (current) {
            Screen.Home -> HomeScreen(state, now, nav, modifier)
            Screen.Recipes -> RecipesScreen(state, now, nav, modifier)
            Screen.Calculator -> CalculatorScreen(nav, modifier)
            Screen.Active -> ActiveScreen(state, now, nav, modifier)
            is Screen.Review -> ReviewScreen(current, state, now, nav, modifier)
            is Screen.Edit -> EditScreen(current.recipe, nav, modifier)
        }
    }
}
