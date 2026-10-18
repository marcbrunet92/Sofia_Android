package com.lemarc.sofia

import androidx.annotation.StringRes
import androidx.datastore.preferences.core.booleanPreferencesKey
import java.time.Duration
import java.time.Instant

const val BASE_URL = "https://sofia.lemarc.fr/"
const val SOFIA_MAX_CAPACITY_MW = 1400.0
val SOFIA_BMUS = listOf("T_SOFOW-11", "T_SOFOW-12", "T_SOFOW-21", "T_SOFOW-22")
val HISTORY_START: Instant = Instant.parse("2026-04-01T00:00:00Z")

enum class TimeWindow(
    val label: String,
    val duration: Duration?,
    @StringRes val labelRes: Int = R.string.time_window_all,
) {
    // HOURS_6("6h", Duration.ofHours(6), R.string.time_window_6h),
    HOURS_24("24h", Duration.ofHours(24), R.string.time_window_24h),
    DAYS_7("7d", Duration.ofDays(7), R.string.time_window_7d),
    MONTH_1("1m", Duration.ofDays(30), R.string.time_window_1m),
    ALL("All", null, R.string.time_window_all),
}
