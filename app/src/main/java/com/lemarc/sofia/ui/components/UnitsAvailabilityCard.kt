package com.lemarc.sofia.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.R
import com.lemarc.sofia.widget.modern.UnitStatus
import com.lemarc.sofia.widget.modern.UnitsData
import com.lemarc.sofia.widget.modern.statusColor
import kotlin.math.roundToInt

/** Puissance disponible maintenant : total du parc, puis une jauge par unité. */
@Composable
fun UnitsAvailabilityCard(data: UnitsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.remit_available_now),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.mw_value_format, data.totalAvailable.roundToInt()),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (data.totalNormal > 0) {
                        Text(
                            text = stringResource(R.string.remit_out_of_format, data.totalNormal.roundToInt()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                data.units.forEach { unit -> UnitGauge(unit, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun UnitGauge(unit: UnitStatus, modifier: Modifier) {
    val color = unit.statusColor()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.remit_unit_format, unit.shortLabel),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        LinearProgressIndicator(
            progress = { unit.fraction },
            modifier = Modifier.fillMaxWidth(),
            color = color,
        )
        Text(
            text = unit.availableMw?.let { stringResource(R.string.mw_value_format, it.roundToInt()) } ?: "—",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}
