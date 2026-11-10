package io.github.vlukyanets.lemonflow.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.ui.about.LicenseTextScreen
import io.github.vlukyanets.lemonflow.ui.about.LicensesScreen
import io.github.vlukyanets.lemonflow.ui.insights.InsightsScreen
import kotlin.reflect.KClass

private data class Tab(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    @DrawableRes val icon: Int
)

private val tabs = listOf(
    Tab(TodayRoute, TodayRoute::class, R.string.tab_today, R.drawable.ic_today),
    Tab(HabitsRoute, HabitsRoute::class, R.string.tab_habits, R.drawable.ic_habits),
    Tab(InsightsRoute, InsightsRoute::class, R.string.tab_insights, R.drawable.ic_insights),
    Tab(SettingsRoute, SettingsRoute::class, R.string.tab_settings, R.drawable.ic_settings)
)

/**
 * Screens fill the whole window; the tab bar floats over their bottom edge, and
 * [LocalBottomBarSpace] tells scrolling screens how much room to leave under it.
 *
 * The root is a Surface, not a bare Box: it sets the default content color
 * (onBackground), so text without an explicit color stays readable in the dark theme.
 */
@Composable
fun LemonFlowNavHost(
    screens: AppScreens,
    navController: NavHostController = rememberNavController()
) {
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val showBottomBar = destination?.hasRoute(HabitEditorRoute::class) != true
    var barHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val gestureInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barSpace = if (showBottomBar) {
        with(density) { barHeight.toDp() } + BAR_MARGIN * 2 + gestureInset
    } else {
        0.dp
    }
    val contentInsets = if (showBottomBar) {
        WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    } else {
        WindowInsets.systemBars
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(contentInsets)) {
                CompositionLocalProvider(LocalBottomBarSpace provides barSpace) {
                    Destinations(navController, screens)
                }
            }
            if (showBottomBar) {
                FloatingTabBar(
                    items = tabs.map {
                        TabItem(stringResource(it.label), it.icon, destination.isOn(it))
                    },
                    onSelect = { i -> navController.openTab(tabs[i]) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(
                            WindowInsets.systemBars.only(
                                WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                            )
                        )
                        .padding(start = BAR_MARGIN, end = BAR_MARGIN, bottom = BAR_MARGIN)
                        .onSizeChanged { barHeight = it.height }
                )
            }
        }
    }
}

@Composable
private fun Destinations(navController: NavHostController, screens: AppScreens) {
    NavHost(navController = navController, startDestination = TodayRoute) {
        composable<TodayRoute> {
            screens.today(
                { id -> navController.navigate(HabitEditorRoute(id)) },
                { navController.navigate(HabitEditorRoute()) }
            )
        }
        composable<HabitsRoute> {
            screens.habits(
                { id -> navController.navigate(HabitEditorRoute(id)) },
                { navController.navigate(ArchiveRoute) }
            )
        }
        composable<InsightsRoute> { InsightsScreen() }
        composable<SettingsRoute> {
            screens.settings(
                { navController.navigate(LicensesRoute) },
                { id -> navController.navigate(LicenseRoute(id)) }
            )
        }
        composable<LicensesRoute> {
            LicensesScreen(
                onBack = { navController.popBackStack() },
                openLicense = { id -> navController.navigate(LicenseRoute(id)) }
            )
        }
        composable<LicenseRoute> { backStack ->
            LicenseTextScreen(backStack.toRoute<LicenseRoute>().id, {
                navController.popBackStack()
            })
        }
        composable<ArchiveRoute> { screens.archive { navController.popBackStack() } }
        composable<HabitEditorRoute> { backStack ->
            val route = backStack.toRoute<HabitEditorRoute>()
            screens.editor(route.habitId) { navController.popBackStack() }
        }
    }
}

/** The archive belongs to the Habits tab; the license screens to Settings. */
private fun NavDestination?.isOn(tab: Tab): Boolean {
    if (this == null) return false
    val owned = when (tab.routeClass) {
        HabitsRoute::class -> hasRoute(ArchiveRoute::class)
        SettingsRoute::class -> hasRoute(LicensesRoute::class) || hasRoute(LicenseRoute::class)
        else -> false
    }
    return owned || hierarchy.any { it.hasRoute(tab.routeClass) }
}

private fun NavHostController.openTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
