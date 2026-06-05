package com.lemarc.sofia.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ShowChart
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
import com.lemarc.sofia.ui.b1610.B1610Screen
import com.lemarc.sofia.ui.b1610.B1610ViewModel
import com.lemarc.sofia.ui.production.ProductionScreen
import com.lemarc.sofia.ui.production.ProductionViewModel
import com.lemarc.sofia.ui.graph.GraphScreen
import com.lemarc.sofia.ui.graph.GraphViewModel
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
    RealOutput("Real Output"),
    Graph("Graph"),
    Weather("Weather"),
    Remit("REMIT"),
    Settings("Settings"),
}

val timestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
        .withZone(ZoneOffset.UTC)
val shortAxisFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM-dd HH:mm")
        .withZone(ZoneOffset.UTC)

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
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Production) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == AppTab.Production,
                    onClick = { selectedTab = AppTab.Production },
                    icon = { Icon(Icons.Filled.Bolt, contentDescription = null) },
                    label = { Text(AppTab.Production.label) },
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.RealOutput,
                    onClick = { selectedTab = AppTab.RealOutput },
                    icon = { Icon(Icons.Filled.PowerSettingsNew, contentDescription = null) },
                    label = { Text(AppTab.RealOutput.label) },
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.Graph,
                    onClick = { selectedTab = AppTab.Graph },
                    icon = { Icon(Icons.Filled.ShowChart, contentDescription = null) },
                    label = { Text(AppTab.Graph.label) },
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.Remit,
                    onClick = { selectedTab = AppTab.Remit },
                    icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                    label = { Text(AppTab.Remit.label) },
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.Weather,
                    onClick = { selectedTab = AppTab.Weather },
                    icon = { Icon(Icons.Filled.Air, contentDescription = null) },
                    label = { Text(AppTab.Weather.label) },
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.Settings,
                    onClick = { selectedTab = AppTab.Settings },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(AppTab.Settings.label) },
                )
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            AppTab.Production -> ProductionScreen(
                state = productionState,
                modifier = Modifier.padding(innerPadding),
                onRefresh = { productionViewModel.refresh() },
                onSelectWindow = productionViewModel::selectWindow,
                onDismissError = productionViewModel::dismissError,
            )

            AppTab.RealOutput -> B1610Screen(
                state = b1610State,
                modifier = Modifier.padding(innerPadding),
                onRefresh = { b1610ViewModel.refresh() },
                onSelectWindow = b1610ViewModel::selectWindow,
                onDismissError = b1610ViewModel::dismissError,
            )

            AppTab.Graph -> GraphScreen(
                state = graphState,
                modifier = Modifier.padding(innerPadding),
                onRefresh = graphViewModel::refresh,
                onSelectWindow = graphViewModel::selectWindow,
                onToggleDataset = graphViewModel::toggleDataset,
                onDismissError = graphViewModel::dismissError,
            )

            AppTab.Weather -> WeatherScreen(
                state = weatherState,
                modifier = Modifier.padding(innerPadding),
                onRefresh = weatherViewModel::refresh,
                onSelectWindow = weatherViewModel::selectWindow,
                onDismissError = weatherViewModel::dismissError,
            )

            AppTab.Remit -> RemitScreen(
                state = remitState,
                modifier = Modifier.padding(innerPadding),
                onRefresh = remitViewModel::refresh,
                onDismissError = remitViewModel::dismissError,
            )

            AppTab.Settings -> SettingsScreen(
                state = settingsState,
                modifier = Modifier.padding(innerPadding),
                onToggleTestMode = settingsViewModel::setTestMode,
            )
        }
    }
}
