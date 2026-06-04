package com.lemarc.sofia.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.data.model.TopProductionPoint
import com.lemarc.sofia.data.model.TopProductionWindows
import com.lemarc.sofia.ui.timestampFormatter
import kotlin.math.roundToInt

@Composable
fun ProductionRecordCard(records: TopProductionWindows, unit: String = "MW") {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Production records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            RecordRow(label = "All-time", point = records.allTime, unit = unit)
            RecordRow(label = "Last 7 days", point = records.last7Days, unit = unit)
            RecordRow(label = "Last 30 days", point = records.last30Days, unit = unit)
            RecordRow(label = "Last 90 days", point = records.last90Days, unit = unit)
        }
    }
}

@Composable
private fun RecordRow(
    label: String,
    point: TopProductionPoint,
    unit: String = "MW",
) {
    val dateValue = point.maxDate?.let(timestampFormatter::format) ?: "—"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "${point.maxMw.roundToInt()} $unit • $dateValue",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
