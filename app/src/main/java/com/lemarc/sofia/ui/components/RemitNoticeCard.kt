package com.lemarc.sofia.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lemarc.sofia.R
import com.lemarc.sofia.data.model.RemitNotice
import com.lemarc.sofia.ui.remit.RemitGroup
import com.lemarc.sofia.ui.remit.RemitPhase
import com.lemarc.sofia.ui.remit.StartsIn
import com.lemarc.sofia.ui.remit.effectiveAvailableMw
import com.lemarc.sofia.ui.remit.startsIn
import com.lemarc.sofia.ui.theme.SofiaYellow
import com.lemarc.sofia.ui.theme.SofiaYellowText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private fun cardDate(instant: Instant): String =
    DateTimeFormatter.ofPattern("EEE d MMM HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)

private fun unitLabel(bmuId: String): String = bmuId.removePrefix("T_")

private fun RemitNotice.title(): String = cause.ifBlank { messageHeading.ifBlank { eventType } }

/** Événement déclaré sur tout le parc : une carte qui se déplie en une carte cliquable par unité. */
@Composable
fun RemitGroupCard(
    group: RemitGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenNotice: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            onClick = onToggle,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TypeChip(group.first.unavailabilityType)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(
                            if (expanded) R.string.remit_hide_units else R.string.remit_show_units,
                        ),
                    )
                }
                Text(
                    text = group.first.title(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.remit_all_units_format, group.notices.size) + " · " +
                        availabilityText(group.availableMw, group.normalMw),
                    style = MaterialTheme.typography.bodyMedium,
                )
                DatesBlock(phase = group.phase, start = group.start, end = group.end)
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                group.notices.forEach { notice ->
                    UnitNoticeCard(notice = notice, onClick = { onOpenNotice(notice.id) })
                }
            }
        }
    }
}

/** Avis d'une seule unité (perturbation partielle du parc, ou mode test). */
@Composable
fun RemitNoticeCard(
    notice: RemitNotice,
    phase: RemitPhase,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeChip(notice.unavailabilityType)
                Text(
                    text = stringResource(R.string.remit_unit_format, unitLabel(notice.bmuId)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
            Text(
                text = notice.title(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = availabilityText(notice.effectiveAvailableMw(), notice.normalCapacityMw),
                style = MaterialTheme.typography.bodyMedium,
            )
            DatesBlock(phase = phase, start = notice.eventStartTime, end = notice.eventEndTime)
        }
    }
}

/** Carte compacte d'une unité dans un groupe déplié. */
@Composable
private fun UnitNoticeCard(notice: RemitNotice, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.remit_unit_format, unitLabel(notice.bmuId)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = availabilityText(notice.effectiveAvailableMw(), notice.normalCapacityMw),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/** « ──── À venir ──── » entre les avis en vigueur et les avis à venir. */
@Composable
fun UpcomingDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(R.string.remit_section_upcoming),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun availabilityText(availableMw: Double?, normalMw: Double?): String = when {
    availableMw == null -> stringResource(R.string.remit_available_unknown)
    normalMw == null -> stringResource(R.string.remit_available_format, availableMw.roundToInt())
    else -> stringResource(R.string.remit_available_of_format, availableMw.roundToInt(), normalMw.roundToInt())
}

/** En vigueur : la fin. À venir : le début mis en avant, « dans X jours », puis la fin. */
@Composable
private fun DatesBlock(phase: RemitPhase, start: Instant?, end: Instant?) {
    val endText = end?.let { stringResource(R.string.remit_until_format, cardDate(it)) }
        ?: stringResource(R.string.remit_no_end)
    if (phase == RemitPhase.Upcoming && start != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.remit_starts_format, cardDate(start)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            val delay = when (val s = startsIn(start, Instant.now())) {
                is StartsIn.Hours -> pluralStringResource(R.plurals.remit_in_hours, s.hours, s.hours)
                is StartsIn.Days -> pluralStringResource(R.plurals.remit_in_days, s.days, s.days)
            }
            Text(
                text = delay,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SofiaYellowText,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SofiaYellow)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
    Text(
        text = endText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** « Planifié » / « Imprévu » (texte Elexon tel quel pour un autre type). */
@Composable
private fun TypeChip(unavailabilityType: String) {
    val (label, background, foreground) = when (unavailabilityType) {
        "Planned" -> Triple(stringResource(R.string.remit_type_planned), SofiaYellow, SofiaYellowText)
        "Unplanned" -> Triple(
            stringResource(R.string.remit_type_unplanned),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        else -> Triple(
            unavailabilityType,
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (label.isBlank()) return
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = foreground,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
