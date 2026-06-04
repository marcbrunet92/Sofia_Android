package com.lemarc.sofia.ui.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.lemarc.sofia.data.model.ProductionPoint
import com.lemarc.sofia.data.model.WeatherPoint
import com.lemarc.sofia.ui.components.ErrorBanner
import com.lemarc.sofia.ui.components.TimestampCard
import com.lemarc.sofia.ui.components.TitleBanner
import com.lemarc.sofia.ui.filterPoints
import com.lemarc.sofia.ui.production.TimeWindow
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    state: WeatherUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onSelectWindow: (TimeWindow) -> Unit,
    onDismissError: () -> Unit,
) {
    val listState = rememberLazyListState()

    val filteredWeather = remember(state.weatherPoints, state.selectedWindow) {
        filterWeatherPoints(state.weatherPoints, state.selectedWindow)
    }
    val filteredProduction = remember(state.productionPoints, state.selectedWindow) {
        filterPoints(state.productionPoints, state.selectedWindow)
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && state.weatherPoints.isEmpty() -> Box(
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
                item { TitleBanner() }

                if (state.errorMessage != null) {
                    item {
                        ErrorBanner(
                            text = state.errorMessage,
                            onDismiss = onDismissError,
                        )
                    }
                }

                if (state.latestWindSpeed != null) {
                    item {
                        WindSpeedCard(windSpeed = state.latestWindSpeed)
                    }
                }

                item {
                    WindProductionChartCard(
                        weatherPoints = filteredWeather,
                        productionPoints = filteredProduction,
                        selectedWindow = state.selectedWindow,
                        onSelectWindow = onSelectWindow,
                    )
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
private fun WindSpeedCard(windSpeed: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Current Wind Speed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "%.1f m/s".format(windSpeed),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun WindProductionChartCard(
    weatherPoints: List<WeatherPoint>,
    productionPoints: List<ProductionPoint>,
    selectedWindow: TimeWindow,
    onSelectWindow: (TimeWindow) -> Unit,
) {
    val lineColor = MaterialTheme.colorScheme.primary.toArgb()
    val barColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f).toArgb()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Wind & Production",
                style = MaterialTheme.typography.titleMedium,
            )

            // Window selector — reuse the same row you have in ChartCard
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimeWindow.entries.forEach { window ->
                    val selected = window == selectedWindow
                    androidx.compose.material3.FilterChip(
                        selected = selected,
                        onClick = { onSelectWindow(window) },
                        label = { Text(window.label) },
                    )
                }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                factory = { context ->
                    CombinedChart(context).apply {
                        description.isEnabled = false
                        setDrawGridBackground(false)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)

                        // Left axis → production (MW)
                        axisLeft.apply {
                            axisMinimum = 0f
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float) = "${value.toInt()} MW"
                            }
                        }

                        // Right axis → wind speed (m/s)
                        axisRight.apply {
                            isEnabled = true
                            gridLineWidth = 0f
                            axisMinimum = 0f
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float) = "%.1f m/s".format(value)
                            }
                        }

                        xAxis.apply {
                            position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                            setLabelCount(4, true)
                            granularity = 1f
                        }

                        legend.apply {
                            isEnabled = true
                            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                        }

                        drawOrder = arrayOf(
                            CombinedChart.DrawOrder.BAR,
                            CombinedChart.DrawOrder.LINE,
                        )
                    }
                },
                update = { chart ->
                    if (weatherPoints.isEmpty() && productionPoints.isEmpty()) return@AndroidView

                    // Align both series on a shared epoch-second X axis
                    val combinedData = CombinedData()

                    if (productionPoints.isNotEmpty()) {
                        val barEntries = productionPoints.map { pt ->
                            BarEntry(pt.timeFrom.epochSecond.toFloat(), pt.levelMw.toFloat())
                        }
                        val barDataSet = BarDataSet(barEntries, "Production (MW)").apply {
                            color = barColor
                            setDrawValues(false)
                            axisDependency = YAxis.AxisDependency.LEFT
                        }
                        combinedData.setData(BarData(barDataSet).apply { barWidth = 1800f })
                    }

                    if (weatherPoints.isNotEmpty()) {
                        val lineEntries = weatherPoints.map { pt ->
                            Entry(pt.timeFrom.epochSecond.toFloat(), pt.windSpeed.toFloat())
                        }
                        val lineDataSet = LineDataSet(lineEntries, "Wind speed (m/s)").apply {
                            color = lineColor
                            lineWidth = 2f
                            setDrawCircles(false)
                            setDrawValues(false)
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                            axisDependency = YAxis.AxisDependency.RIGHT
                        }
                        combinedData.setData(LineData(lineDataSet))
                    }

                    // Format X labels as HH:mm or dd/MM depending on window
                    val formatter = if ((selectedWindow.duration?.toHours() ?: Long.MAX_VALUE) <= 48)
                        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC)
                    else
                        DateTimeFormatter.ofPattern("dd/MM").withZone(ZoneOffset.UTC)

                    chart.xAxis.valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String =
                            formatter.format(Instant.ofEpochSecond(value.toLong()))
                    }

                    chart.data = combinedData
                    chart.invalidate()
                },
            )
        }
    }
}

/** Mirror of [filterPoints] for weather, using [WeatherPoint.timeFrom]. */
private fun filterWeatherPoints(
    points: List<WeatherPoint>,
    window: TimeWindow,
): List<WeatherPoint> {
    val duration = window.duration ?: return points
    val cutoff = Instant.now().minus(duration)
    return points.filter { it.timeFrom >= cutoff }
}