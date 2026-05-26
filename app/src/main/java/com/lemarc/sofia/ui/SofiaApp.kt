package com.lemarc.sofia.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Settings
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
import com.lemarc.sofia.data.model.ProductionPoint
import com.lemarc.sofia.ui.production.ProductionScreen
import com.lemarc.sofia.ui.production.ProductionViewModel
import com.lemarc.sofia.ui.production.TimeWindow
import com.lemarc.sofia.ui.settings.SettingsScreen
import com.lemarc.sofia.ui.settings.SettingsViewModel
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private enum class AppTab(val label: String) {
    Production("Production"),
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
    settingsViewModel: SettingsViewModel,
) {
    val productionState by productionViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
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

            AppTab.Settings -> SettingsScreen(
                state = settingsState,
                modifier = Modifier.padding(innerPadding),
                onToggleTestMode = settingsViewModel::setTestMode,
            )
        }
    }
}



fun filterPoints(points: List<ProductionPoint>, window: TimeWindow): List<ProductionPoint> {
    val duration = window.duration ?: return points
    val lastTimestamp = points.lastOrNull()?.timeTo ?: return emptyList()
    val threshold = lastTimestamp.minus(duration)
    return points.filter { it.timeTo >= threshold }
}
