package com.lemarc.sofia.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.TimeWindow

@Composable
fun ProductionChartMulti(
    left: List<ChartSeries>,
    right: List<ChartSeries> = emptyList(),
    tw: TimeWindow,
    modifier: Modifier = Modifier.height(280.dp),
) {
    // Vico plante sur une série vide : on les écarte en amont
    val leftFiltered = remember(left) { left.filter { it.points.isNotEmpty() } }
    val rightFiltered = remember(right) { right.filter { it.points.isNotEmpty() } }

    if (leftFiltered.isEmpty() && rightFiltered.isEmpty()) {
        EmptyChartPlaceholder(modifier)
        return
    }

    ProductionChartContent(leftFiltered, rightFiltered, tw, modifier)
}
