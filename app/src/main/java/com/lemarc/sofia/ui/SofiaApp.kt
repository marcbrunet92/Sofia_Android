package com.lemarc.sofia.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.lemarc.sofia.ui.graph.GraphScreen
import com.lemarc.sofia.ui.graph.GraphViewModel
import com.lemarc.sofia.ui.navigation.Route
import com.lemarc.sofia.ui.remit.RemitDetailScreen
import com.lemarc.sofia.ui.remit.RemitScreen
import com.lemarc.sofia.ui.remit.RemitViewModel
import com.lemarc.sofia.ui.settings.SettingsScreen
import com.lemarc.sofia.ui.settings.SettingsViewModel
import com.lemarc.sofia.ui.sofia.SofiaScreen
import com.lemarc.sofia.ui.sofia.SofiaViewModel
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private enum class AppTab(val label: String) {
    Graph("Graph"),
    Remit("Alert"),
    Settings("Settings"),
    Sofia("Sofia"),
}

val timestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd-MM-yy HH:mm")
        .withZone(ZoneId.systemDefault())
@Composable
fun SofiaApp(
    graphViewModel: GraphViewModel,
    remitViewModel: RemitViewModel,
    settingsViewModel: SettingsViewModel,
    sofiaViewModel: SofiaViewModel,
) {
    val graphState by graphViewModel.uiState.collectAsStateWithLifecycle()
    val remitState by remitViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val sofiaState by sofiaViewModel.state.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val tabs = listOf(
        AppTab.Graph to Route.Graph,
        AppTab.Remit to Route.Remit,
        AppTab.Sofia to Route.Sofia,
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
                                AppTab.Graph -> Icons.AutoMirrored.Filled.ShowChart
                                AppTab.Remit -> Icons.Filled.Warning
                                AppTab.Settings -> Icons.Filled.Settings
                                AppTab.Sofia -> Icons.Filled.Home
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
            startDestination = Route.Graph,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Route.Graph> {
                GraphScreen(
                    state = graphState,
                    onRefresh = graphViewModel::refresh,
                    onSelectWindow = graphViewModel::selectWindow,
                    onToggleDataset = graphViewModel::toggleDataset,
                    onDismissError = graphViewModel::dismissError,
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
            composable<Route.Sofia> {
                SofiaScreen(
                    state = sofiaState,
                    onDismissError = sofiaViewModel::onDismissError,
                )
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
