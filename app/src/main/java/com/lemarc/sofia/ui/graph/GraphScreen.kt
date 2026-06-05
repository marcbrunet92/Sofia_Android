package com.lemarc.sofia.ui.graph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.data.filterPoints
import com.lemarc.sofia.ui.components.ChartSeries
import com.lemarc.sofia.ui.components.ErrorBanner
import com.lemarc.sofia.ui.components.ProductionChartMulti
import com.lemarc.sofia.ui.components.TitleBanner
import com.lemarc.sofia.ui.components.WarningBanner

@OptIn(ExperimentalMaterial3Api::class)
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
        state.selectedWindow,
        state.pnPoints,
        state.b1610PointsMwh,
        state.weatherPoints,
    ) {
        // Build series for each selected dataset
        val series = state.selectedDatasets.map { ds ->
            when (ds) {
                GraphDataset.PN -> ChartSeries(
                    points = filterPoints(state.pnPoints, state.selectedWindow),
                    allowNegative = false,
                    unit = "MW",
                    label = GraphDataset.PN.label,
                )
                GraphDataset.B1610 -> ChartSeries(
                    points = filterPoints(state.b1610PointsMwh.map { it.copy(quantity = it.quantity * 2) }, state.selectedWindow),
                    allowNegative = true,
                    unit = "MW",
                    label = GraphDataset.B1610.label,
                )
                GraphDataset.Weather -> ChartSeries(
                    points = filterPoints(state.weatherPoints, state.selectedWindow),
                    allowNegative = false,
                    unit = "m/s",
                    label = GraphDataset.Weather.label,
                )
            }
        }

        val byUnit = series.groupBy { it.unit }
        val mwSeries = byUnit["MW"].orEmpty()
        val otherUnits = byUnit.filterKeys { it != "MW" }

        if (mwSeries.isNotEmpty() && otherUnits.isNotEmpty()) {
            // MW left, first other unit (m/s) right; ignore any additional different units for now
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
                item { TitleBanner(title = "Graph") }
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(GraphDataset.Weather, GraphDataset.PN, GraphDataset.B1610).forEach { option ->
            FilterChip(
                selected = option in selected,
                onClick = { onSelect(option) },
                label = { Text(option.label) },
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
                text = "Chart",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimeWindow.entries.forEach { window ->
                    FilterChip(
                        selected = window == selectedWindow,
                        onClick = { onSelectWindow(window) },
                        label = { Text(window.label) },
                    )
                }
            }
            ProductionChartMulti(left = left, right = right)
        }
    }
}
