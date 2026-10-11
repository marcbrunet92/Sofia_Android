package com.lemarc.sofia.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lemarc.sofia.TimeWindow

@Composable
fun ProductionChartMulti(
    left: List<ChartSeries>,
    right: List<ChartSeries> = emptyList(),
    tw: TimeWindow,
) {
    // Vico plante sur une série vide : on les écarte en amont
    val leftFiltered = remember(left) { left.filter { it.points.isNotEmpty() } }
    val rightFiltered = remember(right) { right.filter { it.points.isNotEmpty() } }

    if (leftFiltered.isEmpty() && rightFiltered.isEmpty()) {
        EmptyChartPlaceholder()
        return
    }

    ProductionChartContent(leftFiltered, rightFiltered, tw)
}