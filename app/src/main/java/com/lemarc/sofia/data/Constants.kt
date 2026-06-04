package com.lemarc.sofia.data

import java.time.Instant

const val BASE_URL = "https://sofia.lemarc.fr/"
const val TEST_BMU = "T_HEYM11"
const val SOFIA_MAX_CAPACITY_MW = 1400.0
val SOFIA_BMUS = listOf("T_SOFOW-11", "T_SOFOW-12", "T_SOFOW-21", "T_SOFOW-22")
val HISTORY_START: Instant = Instant.parse("2026-04-01T00:00:00Z")