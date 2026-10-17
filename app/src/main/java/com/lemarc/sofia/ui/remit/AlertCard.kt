package com.lemarc.sofia.ui.remit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.R
import com.lemarc.sofia.ui.theme.SofiaYellow
import com.lemarc.sofia.ui.theme.SofiaYellowText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val PlannedAmber = Color(0xFFF2A900)

/** « mer. 22 oct. 18:00 », dans le fuseau de l'appareil. */
fun alertDate(instant: Instant): String =
    DateTimeFormatter.ofPattern("EEE d MMM HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)

/** Une alerte : une unité, ou le même événement sur tout le parc. */
@Composable
fun AlertCard(group: RemitGroup, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 7.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(typeColor(group)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = scopeText(group) + " · " + availabilityText(group.availableMw, group.normalMw),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = listOfNotNull(typeLabel(group), whenText(group)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val start = group.start
            if (group.phase == RemitPhase.Upcoming && start != null) {
                StartsInBadge(start)
            }
        }
    }
}

@Composable
private fun StartsInBadge(start: Instant) {
    val text = when (val s = startsIn(start, Instant.now())) {
        is StartsIn.Hours -> pluralStringResource(R.plurals.remit_in_hours, s.hours, s.hours)
        is StartsIn.Days -> pluralStringResource(R.plurals.remit_in_days, s.days, s.days)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = SofiaYellowText,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SofiaYellow)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** En cours : la fin. À venir : le début. */
@Composable
private fun whenText(group: RemitGroup): String {
    val start = group.start
    if (group.phase == RemitPhase.Upcoming && start != null) {
        return stringResource(R.string.remit_starts_format, alertDate(start))
    }
    return group.end?.let { stringResource(R.string.remit_until_format, alertDate(it)) }
        ?: stringResource(R.string.remit_no_end)
}

@Composable
fun scopeText(group: RemitGroup): String =
    if (group.isFarmWide) {
        stringResource(R.string.remit_all_units)
    } else {
        stringResource(R.string.remit_unit_format, unitNumber(group.first.bmuId))
    }

/** « Planifié » / « Imprévu » (texte d'origine pour un autre type), null si absent. */
@Composable
fun typeLabel(group: RemitGroup): String? = when (group.first.unavailabilityType) {
    "Planned" -> stringResource(R.string.remit_type_planned)
    "Unplanned" -> stringResource(R.string.remit_type_unplanned)
    else -> group.first.unavailabilityType.takeIf { it.isNotBlank() }
}

@Composable
fun typeColor(group: RemitGroup): Color = when (group.first.unavailabilityType) {
    "Planned" -> PlannedAmber
    "Unplanned" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.outline
}

@Composable
fun availabilityText(availableMw: Double?, normalMw: Double?): String = when {
    availableMw == null -> stringResource(R.string.remit_available_unknown)
    normalMw == null -> stringResource(R.string.remit_available_format, availableMw.roundToInt())
    else -> stringResource(R.string.remit_available_of_format, availableMw.roundToInt(), normalMw.roundToInt())
}
