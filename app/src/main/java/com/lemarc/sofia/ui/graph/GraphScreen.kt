package com.lemarc.sofia.ui.graph

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.R
import com.lemarc.sofia.SOFIA_MAX_CAPACITY_MW
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.ui.components.ChartSeries
import com.lemarc.sofia.ui.components.GbContextCard
import com.lemarc.sofia.ui.components.NextDaysCard
import com.lemarc.sofia.ui.components.ErrorBanner
import com.lemarc.sofia.ui.components.ProductionChartMulti
import com.lemarc.sofia.ui.components.ProductionGaugeCard
import com.lemarc.sofia.ui.components.TitleBanner
import androidx.compose.ui.graphics.Color as ComposeColor

private val GraphDataset.color: Int
    get() = when (this) {
        GraphDataset.PN -> Color.rgb(30, 136, 229)
        GraphDataset.B1610 -> Color.rgb(255, 152, 0)
        GraphDataset.Weather -> Color.rgb(76, 175, 80)
        GraphDataset.MEL -> Color.rgb(29, 158, 117)
    }

// Ordre de dessin : le MEL (zone colorée) passe sous les courbes de production
private val drawOrder = listOf(GraphDataset.MEL, GraphDataset.PN, GraphDataset.B1610, GraphDataset.Weather)

@Composable
fun GraphScreen(
    state: GraphUiState,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onSelectWindow: (TimeWindow) -> Unit,
    onToggleDataset: (GraphDataset) -> Unit,
    onDismissError: () -> Unit,
) {
    val listState = rememberLazyListState()

    val leftRightSeries = remember(
        state.selectedDatasets,
        state.pnPoints,
        state.b1610PointsMwh,
        state.weatherPoints,
        state.melPoints,
        // state.selectedWindow n'est plus une clé ici : il ne change plus les données affichées
    ) {
        val series = state.selectedDatasets.sortedBy { drawOrder.indexOf(it) }.map { ds ->
            when (ds) {
                GraphDataset.PN -> ChartSeries(
                    points = state.pnPoints, // toutes les données, plus de filterPoints
                    allowNegative = false,
                    unit = "MW",
                    label = GraphDataset.PN.labelRes,
                    color = GraphDataset.PN.color,
                )
                GraphDataset.B1610 -> ChartSeries(
                    points = state.b1610PointsMwh.map { it.copy(quantity = it.quantity * 2) },
                    allowNegative = true,
                    unit = "MW",
                    label = GraphDataset.B1610.labelRes,
                    color = GraphDataset.B1610.color,
                )
                GraphDataset.Weather -> ChartSeries(
                    points = state.weatherPoints,
                    allowNegative = false,
                    unit = "km/h",
                    label = GraphDataset.Weather.labelRes,
                    color = GraphDataset.Weather.color,
                )
                GraphDataset.MEL -> ChartSeries(
                    points = state.melPoints,
                    allowNegative = false,
                    unit = "MW",
                    label = GraphDataset.MEL.labelRes,
                    color = GraphDataset.MEL.color,
                    filled = true,
                )
            }
        }

        val byUnit = series.groupBy { it.unit }
        val mwSeries = byUnit["MW"].orEmpty()
        val otherUnits = byUnit.filterKeys { it != "MW" }

        if (mwSeries.isNotEmpty() && otherUnits.isNotEmpty()) {
            val rightSeries = otherUnits.values.flatten()
            mwSeries to rightSeries
        } else if (otherUnits.isNotEmpty()) {
            // Only non-MW units selected, put them on left
            otherUnits.values.flatten() to emptyList()
        } else {
            // Only MW or nothing
            mwSeries to emptyList()
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && state.pnPoints.isEmpty() && state.b1610PointsMwh.isEmpty() && state.weatherPoints.isEmpty() ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { TitleBanner(title = stringResource(R.string.title_graph)) }
                if (state.errorMessageRes != null) {
                    item {
                        ErrorBanner(
                            text = stringResource(state.errorMessageRes),
                            onDismiss = onDismissError,
                        )
                    }
                }
                item {
                    val latest = state.pnPoints.maxByOrNull { it.timeFrom }
                    ProductionGaugeCard(
                        currentMw = latest?.quantity ?: 0.0,
                        maxCapacityMw = 1400.toDouble(),
                        dataTimestamp = latest?.timeFrom,
                    )
                }
                item {
                    DatasetSelector(
                        selected = state.selectedDatasets,
                        onSelect = onToggleDataset,
                    )
                }

                item {
                    GraphChartCard(
                        left = leftRightSeries.first,
                        right = leftRightSeries.second,
                        selectedWindow = state.selectedWindow,
                        onSelectWindow = onSelectWindow,
                    )
                }
                item {
                    NextDaysCard(
                        days = state.uouDays,
                        capacityMw = SOFIA_MAX_CAPACITY_MW,
                    )
                }
                item { GbContextCard(context = state.gbContext, pnPoints = state.pnPoints) }
            }
        }
    }
}

@Composable
private fun DatasetSelector(
    selected: Set<GraphDataset>,
    onSelect: (GraphDataset) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(GraphDataset.B1610, GraphDataset.PN, GraphDataset.Weather).forEach { option ->
            FilterChip(
                selected = option in selected,
                onClick = { onSelect(option) },
                label = { Text(stringResource(option.labelRes)) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ComposeColor(option.color)),
                    )
                },
            )
        }
    }
}

@Composable
private fun GraphChartCard(
    left: List<ChartSeries>,
    right: List<ChartSeries>,
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
                text = stringResource(R.string.chart_title),
                style = MaterialTheme.typography.titleMedium,
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(TimeWindow.entries) { window ->
                    FilterChip(
                        selected = window == selectedWindow,
                        onClick = { onSelectWindow(window) },
                        label = { Text(stringResource(window.labelRes)) },
                    )
                }
            }
            ProductionChartMulti(left = left, right = right, tw = selectedWindow)
        }
    }
}
