package com.lemarc.sofia

import androidx.datastore.preferences.core.booleanPreferencesKey
import java.time.Duration
import java.time.Instant

const val BASE_URL = "https://sofia.lemarc.fr/"
const val TEST_BMU = "T_HEYM11"
const val SOFIA_MAX_CAPACITY_MW = 1400.0
val SOFIA_BMUS = listOf("T_SOFOW-11", "T_SOFOW-12", "T_SOFOW-21", "T_SOFOW-22")
val HISTORY_START: Instant = Instant.parse("2026-04-01T00:00:00Z")

enum class TimeWindow(val label: String, val duration: Duration?) {
    HOURS_6("6h", Duration.ofHours(6)),
    HOURS_24("24h", Duration.ofHours(24)),
    HOURS_48("48h", Duration.ofHours(48)),
    DAYS_7("7d", Duration.ofDays(7)),
    ALL("All", null),
}
val TEST_MODE_KEY = booleanPreferencesKey("test_mode")
