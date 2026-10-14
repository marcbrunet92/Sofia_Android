package com.lemarc.sofia.data.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant

/** Un palier du profil d'un avis REMIT : capacité disponible (MW) entre [start] et [end]. */
data class OutageStep(
    val start: Instant,
    val end: Instant,
    val capacityMw: Double,
)

private data class OutageStepDto(
    val startTime: String?,
    val endTime: String?,
    val capacity: Double?,
)

private val outageProfileType = object : TypeToken<List<OutageStepDto>>() {}.type

/** `outage_profile` (JSON `[{startTime, endTime, capacity}]`) ; liste vide si absent ou illisible. */
fun parseOutageProfile(json: String): List<OutageStep> {
    if (json.isBlank()) return emptyList()
    val items: List<OutageStepDto?> = runCatching { Gson().fromJson<List<OutageStepDto?>>(json, outageProfileType) }
        .getOrNull() ?: return emptyList()
    return items.mapNotNull { item ->
        val start = item?.startTime?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return@mapNotNull null
        val end = item.endTime?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return@mapNotNull null
        val capacity = item.capacity ?: return@mapNotNull null
        OutageStep(start, end, capacity)
    }.sortedBy { it.start }
}
