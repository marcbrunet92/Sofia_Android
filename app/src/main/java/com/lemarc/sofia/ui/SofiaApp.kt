package com.lemarc.sofia.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.lemarc.sofia.data.model.ProductionPoint
import com.lemarc.sofia.data.repository.SofiaProductionRepository
import com.lemarc.sofia.ui.production.ProductionUiState
import com.lemarc.sofia.ui.production.ProductionViewModel
import com.lemarc.sofia.ui.production.TimeWindow
import com.lemarc.sofia.ui.settings.SettingsUiState
import com.lemarc.sofia.ui.settings.SettingsViewModel
import com.lemarc.sofia.ui.theme.SofiaBlue
import com.lemarc.sofia.ui.theme.SofiaCyan
import com.lemarc.sofia.ui.theme.SofiaErrorContainer
import com.lemarc.sofia.ui.theme.SofiaIndigo
import com.lemarc.sofia.ui.theme.SofiaYellow
import com.lemarc.sofia.ui.theme.SofiaYellowText
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

private enum class AppTab(val label: String) {
    Production("Production"),
    Settings("Settings"),
}

private val timestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
        .withZone(ZoneOffset.UTC)
private val shortAxisFormatter: DateTimeFormatter =
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductionScreen(
    state: ProductionUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onSelectWindow: (TimeWindow) -> Unit,
    onDismissError: () -> Unit,
) {
    val listState = rememberLazyListState()
    val displayedPoints = remember(state.points, state.selectedWindow) {
        filterPoints(state.points, state.selectedWindow)
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && state.points.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (state.testMode) {
                    item {
                        WarningBanner(text = "TEST MODE — displaying T_HEYM11 only")
                    }
                }
                if (state.errorMessage != null) {
                    item {
                        ErrorBanner(
                            text = state.errorMessage,
                            onDismiss = onDismissError,
                        )
                    }
                }
                item {
                    ProductionGaugeCard(
                        currentMw = state.currentMw,
                        maxCapacityMw = state.maxCapacityMw,
                    )
                }
                item {
                    Text(
                        text = "${state.currentMw.roundToInt()} MW",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (displayedPoints.isEmpty()) {
                    item {
                        EmptyStateCard()
                    }
                } else {
                    item {
                        ChartCard(
                            points = displayedPoints,
                            selectedWindow = state.selectedWindow,
                            onSelectWindow = onSelectWindow,
                        )
                    }
                }
                item {
                    TimestampCard(
                        latestDataTimestamp = state.latestDataTimestamp,
                        lastFetchTimestamp = state.lastFetchTimestamp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductionGaugeCard(
    currentMw: Double,
    maxCapacityMw: Double,
) {
    val safeMaxCapacity = maxCapacityMw.coerceAtLeast(1.0)
    val progress = (currentMw / safeMaxCapacity).toFloat().coerceIn(0f, 1f)
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress,
        label = "productionGaugeProgress",
    )
    val animatedMw by androidx.compose.animation.core.animateFloatAsState(
        targetValue = currentMw.toFloat(),
        label = "productionGaugeValue",
    )
    val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(240.dp)) {
                val stroke = 24.dp.toPx()
                val diameter = size.minDimension - stroke
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(diameter, diameter),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(SofiaCyan, SofiaBlue, SofiaIndigo, SofiaCyan),
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(diameter, diameter),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${animatedMw.roundToInt()} MW",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${(animatedProgress * 100).roundToInt()}% of ${safeMaxCapacity.roundToInt()} MW",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChartCard(
    points: List<ProductionPoint>,
    selectedWindow: TimeWindow,
    onSelectWindow: (TimeWindow) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Production history",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimeWindow.entries.forEach { window ->
                    FilterChip(
                        selected = window == selectedWindow,
                        onClick = { onSelectWindow(window) },
                        label = { Text(window.label) },
                    )
                }
            }
            ProductionChart(points = points)
            Text(
                text = "Scroll and pinch to inspect the chart in detail.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProductionChart(points: List<ProductionPoint>) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setNoDataText("No production data available")
                axisRight.isEnabled = false
                axisLeft.axisMinimum = 0f
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.labelRotationAngle = -30f
                setViewPortOffsets(70f, 20f, 28f, 90f)
            }
        },
        update = { chart ->
            val entries = points.mapIndexed { index, point ->
                Entry(index.toFloat(), point.levelMw.toFloat())
            }
            val dataSet = LineDataSet(entries, "Production").apply {
                color = android.graphics.Color.rgb(30, 136, 229)
                lineWidth = 2.5f
                setDrawCircles(false)
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = android.graphics.Color.rgb(77, 208, 225)
                fillAlpha = 48
                mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            }
            chart.data = LineData(dataSet)
            chart.axisLeft.axisMaximum = (max(points.maxOfOrNull { it.levelMw } ?: 0.0, 10.0) * 1.15).toFloat()
            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                    val index = value.roundToInt().coerceIn(points.indices)
                    return shortAxisFormatter.format(points[index].timeFrom)
                }
            }
            chart.invalidate()
        },
    )
}

@Composable
private fun TimestampCard(
    latestDataTimestamp: Instant?,
    lastFetchTimestamp: Instant?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Timestamps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TimestampRow(label = "Latest API data", value = latestDataTimestamp)
            HorizontalDivider()
            TimestampRow(label = "Last refresh", value = lastFetchTimestamp)
        }
    }
}

@Composable
private fun TimestampRow(label: String, value: Instant?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value?.let(timestampFormatter::format) ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "🌬️", fontSize = 48.sp)
            Text(
                text = "No production data available yet.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Pull to refresh and try again once the API returns data.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WarningBanner(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SofiaYellow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            color = SofiaYellowText,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ErrorBanner(
    text: String,
    onDismiss: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SofiaErrorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss error",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    modifier: Modifier = Modifier,
    onToggleTestMode: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Test mode") },
                    supportingContent = {
                        Text("Use T_HEYM11 instead of the live Sofia aggregate. This setting is persisted locally.")
                    },
                    trailingContent = {
                        Switch(
                            checked = state.testMode,
                            onCheckedChange = onToggleTestMode,
                        )
                    },
                )
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Open web visualisation") },
                    supportingContent = { Text("Launch the Sofia HTML report in your default browser.") },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://sofia.lemarc.fr/visual/pn")),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("https://sofia.lemarc.fr/visual/pn")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Info",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AssistChip(onClick = {}, enabled = false, label = { Text("API: ${SofiaProductionRepository.BASE_URL}") })
                    Text(
                        text = "Normal mode BMUs: ${SofiaProductionRepository.SOFIA_BMUS.joinToString()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Test mode BMU: ${SofiaProductionRepository.TEST_BMU}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Normal max capacity: ${SofiaProductionRepository.SOFIA_MAX_CAPACITY_MW.roundToInt()} MW",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Test mode capacity: derived from the T_HEYM11 production history in the downloaded dataset.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun filterPoints(points: List<ProductionPoint>, window: TimeWindow): List<ProductionPoint> {
    val duration = window.duration ?: return points
    val lastTimestamp = points.lastOrNull()?.timeTo ?: return emptyList()
    val threshold = lastTimestamp.minus(duration)
    return points.filter { it.timeTo >= threshold }
}
