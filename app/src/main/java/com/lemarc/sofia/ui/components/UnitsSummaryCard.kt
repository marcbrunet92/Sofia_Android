package com.lemarc.sofia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.R
import com.lemarc.sofia.widget.modern.UnitStatus
import com.lemarc.sofia.widget.modern.UnitsData
import com.lemarc.sofia.widget.modern.WidgetPalette
import com.lemarc.sofia.widget.modern.formatClock
import com.lemarc.sofia.widget.modern.statusColor
import kotlin.math.roundToInt

/** Même contenu que SofiaUnitsGlanceWidget (version large) : total, une tuile par unité, heure de mise à jour. */
@Composable
fun UnitsSummaryCard(
    data: UnitsData,
    modifier: Modifier = Modifier,
) {
    val mwUnit = stringResource(R.string.unit_mw)
    val total = if (data.totalNormal > 0) {
        "${data.totalAvailable.roundToInt()} / ${data.totalNormal.roundToInt()} $mwUnit"
    } else {
        "${data.totalAvailable.roundToInt()} $mwUnit"
    }
    val prefix = if (data.fromCache) {
        stringResource(R.string.widget_prefix_offline)
    } else {
        stringResource(R.string.widget_prefix_updated)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.widget_units_header),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (data.testMode) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.widget_live_test),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = WidgetPalette.Bad,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = total,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                data.units.forEach { unit -> UnitTile(unit, Modifier.weight(1f)) }
            }

            Text(
                text = prefix + formatClock(data.fetchedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UnitTile(unit: UnitStatus, modifier: Modifier = Modifier) {
    val color = unit.statusColor()
    val mw = unit.availableMw?.roundToInt()?.toString() ?: "—"

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = unit.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Text(
            text = mw,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = stringResource(R.string.unit_mw),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { unit.fraction },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
