package com.lemarc.sofia.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.R
import com.lemarc.sofia.data.model.OutageStep
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Profil d'un avis REMIT : capacité disponible en paliers (zone colorée), capacité normale en
 * pointillés et trait « maintenant » quand il tombe dans la période.
 */
@Composable
fun OutageProfileChart(steps: List<OutageStep>, normalMw: Double?, modifier: Modifier = Modifier) {
    if (steps.isEmpty()) return
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val nowColor = MaterialTheme.colorScheme.error
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val textMeasurer = rememberTextMeasurer()
    val nowLabel = stringResource(R.string.remit_profile_now)
    val normalLabel = normalMw?.let { stringResource(R.string.remit_profile_normal_format, it.roundToInt()) }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()).withZone(ZoneId.systemDefault())
    }

    val start = steps.minOf { it.start }
    val end = steps.maxOf { it.end }
    val spanMs = max(end.toEpochMilli() - start.toEpochMilli(), 60_000L).toDouble()
    val topMw = max(steps.maxOf { it.capacityMw }, normalMw ?: 0.0).coerceAtLeast(1.0) * 1.15
    val now = Instant.now()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        val left = 40.dp.toPx()
        val top = 6.dp.toPx()
        val bottom = size.height - 20.dp.toPx()
        val right = size.width - 4.dp.toPx()
        fun x(t: Instant) = left + ((t.toEpochMilli() - start.toEpochMilli()) / spanMs * (right - left)).toFloat()
        fun y(mw: Double) = bottom - (mw / topMw * (bottom - top)).toFloat()

        // Axe 0 MW
        drawLine(gridColor, Offset(left, bottom), Offset(right, bottom), strokeWidth = 1.dp.toPx())
        drawText(textMeasurer, "0", Offset(4.dp.toPx(), bottom - 8.dp.toPx()), labelStyle)

        // Paliers de capacité disponible
        val line = Path()
        steps.forEachIndexed { index, step ->
            if (index == 0) line.moveTo(x(step.start), y(step.capacityMw)) else line.lineTo(x(step.start), y(step.capacityMw))
            line.lineTo(x(step.end), y(step.capacityMw))
        }
        val area = Path().apply {
            addPath(line)
            lineTo(x(steps.last().end), bottom)
            lineTo(x(steps.first().start), bottom)
            close()
        }
        drawPath(area, lineColor.copy(alpha = 0.2f))
        drawPath(line, lineColor, style = Stroke(width = 2.dp.toPx()))

        // Capacité normale
        if (normalMw != null && normalLabel != null) {
            val yNormal = y(normalMw)
            drawLine(
                gridColor,
                Offset(left, yNormal),
                Offset(right, yNormal),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
            val measured = textMeasurer.measure(normalLabel, labelStyle)
            drawText(measured, topLeft = Offset(right - measured.size.width, yNormal - measured.size.height))
            drawText(textMeasurer, "${normalMw.roundToInt()}", Offset(4.dp.toPx(), yNormal - 8.dp.toPx()), labelStyle)
        }

        // Maintenant
        if (now.isAfter(start) && now.isBefore(end)) {
            val xNow = x(now)
            drawLine(
                nowColor,
                Offset(xNow, top),
                Offset(xNow, bottom),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
            )
            drawText(textMeasurer, nowLabel, Offset(xNow + 3.dp.toPx(), top), labelStyle.copy(color = nowColor))
        }

        // Dates de début et de fin
        val startLabel = textMeasurer.measure(dateFormatter.format(start), labelStyle)
        drawText(startLabel, topLeft = Offset(left, bottom + 4.dp.toPx()))
        val endLabel = textMeasurer.measure(dateFormatter.format(end), labelStyle)
        drawText(endLabel, topLeft = Offset(right - endLabel.size.width, bottom + 4.dp.toPx()))
    }
}
