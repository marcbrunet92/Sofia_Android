package com.lemarc.sofia

import androidx.annotation.StringRes
import androidx.datastore.preferences.core.booleanPreferencesKey
import java.time.Duration
import java.time.Instant

const val BASE_URL = "https://sofia.lemarc.fr/"
const val TEST_BMU = "T_HEYM11"
const val SOFIA_MAX_CAPACITY_MW = 1400.0
val SOFIA_BMUS = listOf("T_SOFOW-11", "T_SOFOW-12", "T_SOFOW-21", "T_SOFOW-22")
val HISTORY_START: Instant = Instant.parse("2026-04-01T00:00:00Z")

enum class TimeWindow(
    val label: String,
    val duration: Duration?,
    @StringRes val labelRes: Int = R.string.time_window_all,
) {
    HOURS_6("6h", Duration.ofHours(6), R.string.time_window_6h),
    HOURS_24("24h", Duration.ofHours(24), R.string.time_window_24h),
    HOURS_48("48h", Duration.ofHours(48), R.string.time_window_48h),
    DAYS_7("7d", Duration.ofDays(7), R.string.time_window_7d),
    MONTH_1("1m", Duration.ofDays(30), R.string.time_window_1m),
    MONTH_3("3m", Duration.ofDays(90), R.string.time_window_3m),
    ALL("All", null, R.string.time_window_all),
}
val TEST_MODE_KEY = booleanPreferencesKey("test_mode")
