package com.lemarc.sofia.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.R
import com.lemarc.sofia.ui.theme.SofiaBlue
import com.lemarc.sofia.ui.theme.SofiaCyan
import com.lemarc.sofia.ui.theme.SofiaIndigo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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
                .padding(20.dp)
                // L'arc (270°) est ouvert en bas : ~32 dp vides sous le dessin dans le carré de 240 dp.
                // On décale de la moitié pour que la jauge paraisse centrée dans la carte.
                .offset(y = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(240.dp)) {
                drawGaugeArcs(progress = animatedProgress, strokePx = 24.dp.toPx(), trackColor = color)
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

/**
 * Variante compacte (écran Graph sans défilement) : jauge à gauche, part de Sofia dans
 * l'éolien GB à droite (anciennement GbContextCard).
 */
@Composable
fun ProductionGaugeCompactCard(
    currentMw: Double,
    maxCapacityMw: Double,
    dataTimestamp: Instant?,
    gbSharePercent: Double?,
) {
    val safeMaxCapacity = maxCapacityMw.coerceAtLeast(1.0)
    val progress = (currentMw / safeMaxCapacity).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "compactGaugeProgress")
    val animatedMw by animateFloatAsState(targetValue = currentMw.toFloat(), label = "compactGaugeValue")
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    // Même taille pour « XX MW » et « XX % » ; plus grand, « 1400 MW » ne tient plus dans l'arc
    val valueStyle = MaterialTheme.typography.headlineSmall
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        // Deux moitiés de même largeur : jauge (production) | part de l'éolien GB.
        // IntrinsicSize.Min donne au séparateur la hauteur du contenu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawGaugeArcs(progress = animatedProgress, strokePx = 12.dp.toPx(), trackColor = trackColor)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.mw_value_format, animatedMw.roundToInt()),
                            style = valueStyle,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(R.string.gauge_data_timestamp_format, formatDataTimestamp(dataTimestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            VerticalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = formatGbShare(gbSharePercent),
                    style = valueStyle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.gb_share_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Arc de fond (270°, ouvert en bas) puis arc de progression en dégradé. */
private fun DrawScope.drawGaugeArcs(progress: Float, strokePx: Float, trackColor: Color) {
    val diameter = size.minDimension - strokePx
    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
    val arcSize = Size(diameter, diameter)
    val style = Stroke(width = strokePx, cap = StrokeCap.Round)
    drawArc(
        color = trackColor,
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = style,
    )
    drawArc(
        brush = Brush.sweepGradient(colors = listOf(SofiaCyan, SofiaBlue, SofiaIndigo, SofiaCyan)),
        startAngle = 135f,
        sweepAngle = 270f * progress,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = style,
    )
}

/** « Aujourd'hui, 14:30 » / « Today, 14:30 » si la donnée date d'aujourd'hui, sinon « 17/10 14:30 ». */
@Composable
private fun formatDataTimestamp(timestamp: Instant?): String {
    if (timestamp == null) return "—"
    val zone = ZoneId.of("Europe/Paris")
    val locale = LocalLocale.current.platformLocale
    val dateTime = timestamp.atZone(zone)
    return if (dateTime.toLocalDate() == LocalDate.now(zone)) {
        val time = DateTimeFormatter.ofPattern("h a", Locale.ENGLISH).format(dateTime)
        stringResource(R.string.timestamp_today_format, time)
    } else {
        DateTimeFormatter.ofPattern("dd/MM HH:mm", locale).format(dateTime)
    }
}