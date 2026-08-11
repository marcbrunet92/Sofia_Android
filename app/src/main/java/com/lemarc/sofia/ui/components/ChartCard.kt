package com.lemarc.sofia.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.data.model.GraphPoint

@Composable
fun ChartCard(
    points: List<GraphPoint>,
    selectedWindow: TimeWindow,
    onSelectWindow: (TimeWindow) -> Unit,
    allowNegative: Boolean = false,
    unit: String,
    title: String = "Production history"
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
                text = title,
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
            ProductionChart(points = points, allowNegative = allowNegative, unit = unit, tw = selectedWindow)
        }
    }
}