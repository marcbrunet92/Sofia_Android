package com.lemarc.sofia.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.ui.navigation.Route
import com.lemarc.sofia.ui.b1610.B1610Screen
import com.lemarc.sofia.ui.b1610.B1610ViewModel
import com.lemarc.sofia.ui.production.ProductionScreen
import com.lemarc.sofia.ui.production.ProductionCombinedScreen
import com.lemarc.sofia.ui.production.ProductionViewModel
import com.lemarc.sofia.ui.graph.GraphScreen
import com.lemarc.sofia.ui.graph.GraphViewModel
import com.lemarc.sofia.ui.remit.RemitDetailScreen
import com.lemarc.sofia.ui.remit.RemitScreen
import com.lemarc.sofia.ui.remit.RemitViewModel
import com.lemarc.sofia.ui.settings.SettingsScreen
import com.lemarc.sofia.ui.settings.SettingsViewModel
import com.lemarc.sofia.ui.weather.WeatherScreen
import com.lemarc.sofia.ui.weather.WeatherViewModel
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private enum class AppTab(val label: String) {
    Production("Production"),
    Graph("Graph"),
    Weather("Weather"),
    Remit("REMIT"),
    Settings("Settings"),
}

val timestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
        .withZone(ZoneOffset.UTC)
fun shortAxisFormatter(tw: TimeWindow): DateTimeFormatter {
    val pattern = when (tw) {
        TimeWindow.HOURS_6  -> "HH:mm"
        TimeWindow.HOURS_24 -> "MM-dd HH:mm"
        TimeWindow.HOURS_48 -> "MM-dd HH:mm"
        TimeWindow.DAYS_7   -> "MM-dd"
        TimeWindow.ALL      -> "MM-dd"
    }

    return DateTimeFormatter.ofPattern(pattern)
        .withZone(ZoneOffset.UTC)
}
@Composable
fun SofiaApp(
    productionViewModel: ProductionViewModel,
    b1610ViewModel: B1610ViewModel,
    graphViewModel: GraphViewModel,
    remitViewModel: RemitViewModel,
    settingsViewModel: SettingsViewModel,
    weatherViewModel: WeatherViewModel,
) {
    val productionState by productionViewModel.uiState.collectAsStateWithLifecycle()
    val b1610State by b1610ViewModel.uiState.collectAsStateWithLifecycle()
    val graphState by graphViewModel.uiState.collectAsStateWithLifecycle()
    val remitState by remitViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val weatherState by weatherViewModel.uiState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val tabs = listOf(
        AppTab.Production to Route.Production,
        AppTab.Graph to Route.Graph,
        AppTab.Remit to Route.Remit,
        AppTab.Weather to Route.Weather,
        AppTab.Settings to Route.Settings,
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { (tab, route) ->
                    val isSelected = currentDestination?.hierarchy?.any { it.hasRoute(route::class) } == true
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            val icon = when (tab) {
                                AppTab.Production -> Icons.Filled.Bolt
                                AppTab.Graph -> Icons.AutoMirrored.Filled.ShowChart
                                AppTab.Remit -> Icons.Filled.Warning
                                AppTab.Weather -> Icons.Filled.Air
                                AppTab.Settings -> Icons.Filled.Settings
                            }
                            Icon(icon, contentDescription = null)
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Production,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Route.Production> {
                ProductionCombinedScreen(
                    productionState = productionState,
                    b1610State = b1610State,
                    onRefreshProduction = { productionViewModel.refresh() },
                    onSelectWindowProduction = productionViewModel::selectWindow,
                    onDismissErrorProduction = productionViewModel::dismissError,
                    onRefreshB1610 = { b1610ViewModel.refresh() },
                    onSelectWindowB1610 = b1610ViewModel::selectWindow,
                    onDismissErrorB1610 = b1610ViewModel::dismissError,
                )
            }
            composable<Route.Graph> {
                GraphScreen(
                    state = graphState,
                    onRefresh = graphViewModel::refresh,
                    onSelectWindow = graphViewModel::selectWindow,
                    onToggleDataset = graphViewModel::toggleDataset,
                    onDismissError = graphViewModel::dismissError,
                )
            }
            composable<Route.Weather> {
                WeatherScreen(
                    state = weatherState,
                    onRefresh = weatherViewModel::refresh,
                    onSelectWindow = weatherViewModel::selectWindow,
                    onDismissError = weatherViewModel::dismissError,
                )
            }
            composable<Route.Remit> {
                RemitScreen(
                    state = remitState,
                    onRefresh = remitViewModel::refresh,
                    onDismissError = remitViewModel::dismissError,
                    onNavigateToDetail = { id -> navController.navigate(Route.RemitDetail(id)) }
                )
            }
            composable<Route.RemitDetail> { backStackEntry ->
                val route: Route.RemitDetail = backStackEntry.toRoute()
                val notice = remitState.remits.firstOrNull { it.id == route.id }
                if (notice != null) {
                    RemitDetailScreen(
                        notice = notice,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable<Route.Settings> {
                SettingsScreen(
                    state = settingsState,
                    onToggleTestMode = settingsViewModel::setTestMode,
                )
            }
        }
    }
}
