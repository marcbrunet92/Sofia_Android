package com.lemarc.sofia.ui.remit

import com.lemarc.sofia.data.model.RemitNotice
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

enum class RemitPhase { Current, Upcoming }

/**
 * Un avis seul, ou le même événement déclaré sur toutes les unités du parc
 * (même type, même cause, même fin, et même début pour un avis à venir).
 */
data class RemitGroup(
    val key: String,
    val phase: RemitPhase,
    val notices: List<RemitNotice>,
    val isFarmWide: Boolean,
) {
    val first: RemitNotice get() = notices.first()
    val start: Instant? get() = notices.mapNotNull { it.eventStartTime }.minOrNull()
    val end: Instant? get() = if (notices.any { it.eventEndTime == null }) null else notices.mapNotNull { it.eventEndTime }.maxOrNull()

    /** Somme des capacités disponibles ; null si une unité n'en donne pas. */
    val availableMw: Double? get() = notices.map { it.effectiveAvailableMw() }.sumOrNull()
    val normalMw: Double? get() = notices.map { it.normalCapacityMw }.sumOrNull()
}

data class RemitSections(
    val current: List<RemitGroup> = emptyList(),
    val upcoming: List<RemitGroup> = emptyList(),
) {
    val noticeCount: Int get() = (current + upcoming).sumOf { it.notices.size }
}

fun RemitNotice.effectiveAvailableMw(): Double? {
    availableCapacityMw?.let { return it }
    val n = normalCapacityMw
    val u = unavailableCapacityMw
    return if (n != null && u != null) n - u else null
}

private fun List<Double?>.sumOrNull(): Double? = if (any { it == null }) null else sumOf { it ?: 0.0 }

/**
 * Avis en vigueur d'abord, puis avis à venir (du plus proche au plus lointain). Les avis terminés
 * (fin passée) sont écartés. Un événement présent sur toutes les [units] devient un seul groupe ;
 * une perturbation partielle du parc garde un avis par carte.
 */
fun groupRemits(notices: List<RemitNotice>, units: List<String>, now: Instant): RemitSections {
    val inScope = notices.filter { it.eventEndTime == null || !it.eventEndTime.isBefore(now) }
    val groups = inScope
        .groupBy { notice ->
            val phase = if (notice.eventStartTime?.isAfter(now) == true) RemitPhase.Upcoming else RemitPhase.Current
            val start = if (phase == RemitPhase.Upcoming) notice.eventStartTime?.truncatedTo(ChronoUnit.HOURS) else null
            listOf(
                phase,
                notice.unavailabilityType,
                notice.cause,
                notice.eventEndTime?.truncatedTo(ChronoUnit.HOURS),
                start,
            )
        }
        .flatMap { (key, grouped) ->
            val phase = key.first() as RemitPhase
            val sorted = grouped.sortedBy { it.bmuId }
            val farmWide = units.size > 1 && sorted.map { it.bmuId }.toSet().containsAll(units)
            if (farmWide) {
                listOf(RemitGroup(key.joinToString("|"), phase, sorted, isFarmWide = true))
            } else {
                sorted.map { RemitGroup("${it.id}", phase, listOf(it), isFarmWide = false) }
            }
        }
    val current = groups
        .filter { it.phase == RemitPhase.Current }
        .sortedWith(compareBy<RemitGroup>({ !it.isFarmWide }, { it.first.bmuId }, { it.end ?: Instant.MAX }))
    val upcoming = groups
        .filter { it.phase == RemitPhase.Upcoming }
        .sortedWith(compareBy<RemitGroup>({ it.start ?: Instant.MAX }, { !it.isFarmWide }, { it.first.bmuId }))
    return RemitSections(current, upcoming)
}

/** Délai avant le début d'un avis : en heures sous 24 h, sinon en jours calendaires. */
sealed interface StartsIn {
    data class Hours(val hours: Int) : StartsIn
    data class Days(val days: Int) : StartsIn
}

fun startsIn(start: Instant, now: Instant, zone: ZoneId = ZoneId.systemDefault()): StartsIn {
    val duration = Duration.between(now, start)
    if (duration < Duration.ofDays(1)) {
        return StartsIn.Hours(ceil(duration.toMinutes() / 60.0).toInt().coerceAtLeast(1))
    }
    val days = ChronoUnit.DAYS.between(now.atZone(zone).toLocalDate(), start.atZone(zone).toLocalDate())
    return StartsIn.Days(days.toInt().coerceAtLeast(1))
}
