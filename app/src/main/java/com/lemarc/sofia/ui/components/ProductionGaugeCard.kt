package com.lemarc.sofia.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.R
import com.lemarc.sofia.ui.theme.SofiaBlue
import com.lemarc.sofia.ui.theme.SofiaCyan
import com.lemarc.sofia.ui.theme.SofiaIndigo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun ProductionGaugeCard(
    currentMw: Double,
    maxCapacityMw: Double,
    dataTimestamp: Instant?,
) {
    val safeMaxCapacity = maxCapacityMw.coerceAtLeast(1.0)
    val progress = (currentMw / safeMaxCapacity).toFloat().coerceIn(0f, 1f)
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress,
        label = "productionGaugeProgress",
    )
    val animatedMw by androidx.compose.animation.core.animateFloatAsState(
        targetValue = currentMw.toFloat(),
        label = "productionGaugeValue",
    )
    val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(240.dp)) {
                val stroke = 24.dp.toPx()
                val diameter = size.minDimension - stroke
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(diameter, diameter),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(SofiaCyan, SofiaBlue, SofiaIndigo, SofiaCyan),
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = androidx.compose.ui.geometry.Size(diameter, diameter),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.mw_value_format, animatedMw.roundToInt()),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.gauge_capacity_format,
                        (animatedProgress * 100).roundToInt(),
                        safeMaxCapacity.roundToInt(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.gauge_data_timestamp_format, formatDataTimestamp(dataTimestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** « Aujourd'hui, 14:30 » / « Today, 14:30 » si la donnée date d'aujourd'hui, sinon « 17/10 14:30 ». */
@Composable
private fun formatDataTimestamp(timestamp: Instant?): String {
    if (timestamp == null) return "—"
    val zone = ZoneId.systemDefault()
    val locale = LocalLocale.current.platformLocale
    val dateTime = timestamp.atZone(zone)
    return if (dateTime.toLocalDate() == LocalDate.now(zone)) {
        val time = DateTimeFormatter.ofPattern("HH:mm", locale).format(dateTime)
        stringResource(R.string.timestamp_today_format, time)
    } else {
        DateTimeFormatter.ofPattern("dd/MM HH:mm", locale).format(dateTime)
    }
}