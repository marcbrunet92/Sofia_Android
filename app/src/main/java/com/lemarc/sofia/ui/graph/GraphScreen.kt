package com.lemarc.sofia.ui.graph

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.lemarc.sofia.R
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.ui.components.ChartSeries
import com.lemarc.sofia.ui.components.ErrorBanner
import com.lemarc.sofia.ui.components.ProductionChartMulti
import com.lemarc.sofia.ui.components.ProductionGaugeCompactCard
import com.lemarc.sofia.ui.components.gbSharePercent
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
    bottomInset: Dp = 0.dp,
    onRefresh: () -> Unit,
    onSelectWindow: (TimeWindow) -> Unit,
    onToggleDataset: (GraphDataset) -> Unit,
    onDismissError: () -> Unit,
) {
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

            // Écran à hauteur fixe : le graphique prend toute la place restante, rien ne défile.
            // verticalScroll est gardé pour que le tirer-pour-rafraîchir reçoive le geste ; il ne défile
            // réellement que sur les très petits écrans, où la hauteur minimale dépasse l'écran.
            else -> BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .height(max(maxHeight, MinScreenHeight + bottomInset))
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + bottomInset),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TitleBanner(title = stringResource(R.string.title_graph))
                    if (state.errorMessageRes != null) {
                        ErrorBanner(
                            text = stringResource(state.errorMessageRes),
                            onDismiss = onDismissError,
                        )
                    }
                    val latest = state.pnPoints.maxByOrNull { it.timeFrom }
                    ProductionGaugeCompactCard(
                        currentMw = latest?.quantity ?: 0.0,
                        maxCapacityMw = 1400.toDouble(),
                        dataTimestamp = latest?.timeFrom,
                        gbSharePercent = remember(state.gbContext, state.pnPoints) {
                            gbSharePercent(state.gbContext, state.pnPoints)
                        },
                    )
                    GraphChartCard(
                        left = leftRightSeries.first,
                        right = leftRightSeries.second,
                        selectedWindow = state.selectedWindow,
                        onSelectWindow = onSelectWindow,
                        selectedDataset = state.selectedDatasets,
                        onSelectDataset = onToggleDataset,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// En dessous (hors barre d'onglets), le graphique deviendrait illisible : l'écran défile alors
private val MinScreenHeight = 620.dp

@Composable
private fun DatasetSelector(
    selected: Set<GraphDataset>,
    onSelect: (GraphDataset) -> Unit,
) {
    // Chaque chip garde sa largeur naturelle, l'espace restant est réparti à parts égales
    // pour que l'ensemble occupe toute la largeur
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            listOf(GraphDataset.B1610, GraphDataset.PN, GraphDataset.Weather).forEach { option ->
                FilterChip(
                    selected = option in selected,
                    onClick = { onSelect(option) },
                    label = {
                        Text(
                            text = stringResource(option.labelRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
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
        },
    ) { measurables, constraints ->
        val gap = 8.dp.roundToPx()
        val available = constraints.maxWidth - gap * (measurables.size - 1)
        val natural = measurables.map { it.maxIntrinsicWidth(constraints.maxHeight) }
        val naturalSum = natural.sum()

        val widths = if (naturalSum <= available) {
            val extra = (available - naturalSum) / measurables.size
            natural.map { it + extra }
        } else {
            // Pas assez de place : on réduit proportionnellement
            natural.map { it * available / naturalSum }
        }.toMutableList()
        // Le reste des arrondis va au dernier chip pour remplir exactement la largeur
        widths[widths.lastIndex] += available - widths.sum()

        val placeables = measurables.mapIndexed { i, measurable ->
            measurable.measure(
                Constraints(
                    minWidth = widths[i],
                    maxWidth = widths[i],
                    maxHeight = constraints.maxHeight,
                ),
            )
        }
        val height = placeables.maxOf { it.height }

        layout(constraints.maxWidth, height) {
            var x = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(x, (height - placeable.height) / 2)
                x += placeable.width + gap
            }
        }
    }
}

@Composable
private fun GraphChartCard(
    left: List<ChartSeries>,
    right: List<ChartSeries>,
    selectedWindow: TimeWindow,
    onSelectWindow: (TimeWindow) -> Unit,
    selectedDataset: Set<GraphDataset>,
    onSelectDataset: (GraphDataset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DatasetSelector(
                selected = selectedDataset,
                onSelect = onSelectDataset,
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                items(TimeWindow.entries) { window ->
                    FilterChip(
                        selected = window == selectedWindow,
                        onClick = { onSelectWindow(window) },
                        label = { Text(stringResource(window.labelRes)) },
                    )
                }
            }
            ProductionChartMulti(
                left = left,
                right = right,
                tw = selectedWindow,
                modifier = Modifier.weight(1f),
            )
            if (selectedDataset.contains(GraphDataset.B1610)) {
                Text(stringResource(R.string.chart_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
