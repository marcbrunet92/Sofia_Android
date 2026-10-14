package com.lemarc.sofia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.R
import com.lemarc.sofia.data.model.GbContext
import com.lemarc.sofia.data.model.GraphPoint
import com.lemarc.sofia.data.model.UouDay
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Barre d'un jour disponible / jour à 0 MW (mêmes couleurs que la maquette). */
val AvailableGreen = Color(0xFF1D9E75)
val UnavailableRed = Color(0xFFD85A30)

private val BAR_AREA_HEIGHT = 44.dp

@Composable
private fun InfoCardContainer(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

@Composable
private fun CardTitle(title: String, subtitle: String? = null) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * « 14 prochains jours » : puissance utilisable prévue (UOU2T14D), une barre par jour.
 * [capacityMw] sert d'échelle (1 400 MW pour Sofia) ; null = la plus grande valeur (mode test).
 */
@Composable
fun NextDaysCard(days: List<UouDay>, capacityMw: Double?) {
    InfoCardContainer {
        CardTitle(stringResource(R.string.next_days_title), stringResource(R.string.next_days_subtitle))
        val today = LocalDate.now()
        val shown = days.filter { !it.date.isBefore(today) }.take(14)
        if (shown.isEmpty()) {
            Text(
                text = stringResource(R.string.next_days_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@InfoCardContainer
        }
        val scale = (capacityMw ?: shown.maxOf { it.outputUsableMw }).coerceAtLeast(1.0)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_AREA_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            shown.forEach { day ->
                val fraction = (day.outputUsableMw / scale).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (fraction > 0f) BAR_AREA_HEIGHT * fraction.coerceAtLeast(0.07f) else 2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (fraction > 0f) AvailableGreen else UnavailableRed),
                )
            }
        }
        val formatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOfNotNull(shown.first(), shown.last().takeIf { shown.size > 1 }).forEach { day ->
                Text(
                    text = stringResource(
                        R.string.next_days_label_format,
                        formatter.format(day.date),
                        day.outputUsableMw.roundToInt(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Contexte national (AGWS) : éolien en mer de Grande-Bretagne et part de Sofia (PN ÷ éolien en mer). */
@Composable
fun GbContextCard(context: GbContext?, pnPoints: List<GraphPoint>) {
    InfoCardContainer {
        CardTitle(stringResource(R.string.gb_context_title))
        if (context == null) {
            Text(
                text = stringResource(R.string.gb_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@InfoCardContainer
        }
        val zone = ZoneId.systemDefault()
        val sameDay = context.timeFrom.atZone(zone).toLocalDate() == LocalDate.now(zone)
        val time = DateTimeFormatter.ofPattern(if (sameDay) "HH:mm" else "dd/MM HH:mm", Locale.getDefault())
            .withZone(zone)
            .format(context.timeFrom)
        val offshore = context.windOffshoreMw
        val sofiaMw = pnPoints.lastOrNull { it.timeFrom == context.timeFrom }?.quantity
        val share = if (offshore != null && offshore > 0.0 && sofiaMw != null) sofiaMw / offshore * 100 else null

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                label = stringResource(R.string.gb_offshore_label),
                value = offshore?.let { stringResource(R.string.gb_gw_format, gigawatts(it)) } ?: "—",
                caption = stringResource(R.string.gb_source_format, time),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.gb_share_label),
                value = share?.let { String.format(Locale.getDefault(), "%.1f %%", it) } ?: "—",
                caption = stringResource(R.string.gb_share_caption),
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = stringResource(
                R.string.gb_others_format,
                context.windOnshoreMw?.let(::gigawatts) ?: "—",
                context.solarMw?.let(::gigawatts) ?: "—",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun gigawatts(mw: Double): String = String.format(Locale.getDefault(), "%.1f", mw / 1000)

@Composable
private fun StatTile(label: String, value: String, caption: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        Text(text = caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Rôle de chaque jeu de données, en une phrase. */
@Composable
fun DatasetsInfoCard() {
    InfoCardContainer {
        CardTitle(stringResource(R.string.data_info_title))
        listOf(
            "PN" to R.string.data_info_pn,
            "B1610" to R.string.data_info_b1610,
            "MEL" to R.string.data_info_mel,
            "UOU2T14D" to R.string.data_info_uou,
            "AGWS" to R.string.data_info_agws,
            "REMIT" to R.string.data_info_remit,
            "Open-Meteo" to R.string.data_info_wind,
        ).forEach { (name, description) ->
            Column {
                Text(text = name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
