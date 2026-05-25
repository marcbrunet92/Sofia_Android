package com.lemarc.sofia.data.repository

import com.lemarc.sofia.data.api.PnEntryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SofiaProductionRepositoryTest {
    @Test
    fun aggregateProduction_sumsMatchingSettlementWindows() {
        val entries = listOf(
            PnEntryDto("T_SOFOW-11", "2026-04-01T00:00:00", "2026-04-01T00:30:00", 1, 100.0, "pn"),
            PnEntryDto("T_SOFOW-12", "2026-04-01T00:00:00", "2026-04-01T00:30:00", 1, 150.0, "pn"),
            PnEntryDto("T_SOFOW-21", "2026-04-01T00:30:00", "2026-04-01T01:00:00", 2, 200.0, "pn"),
        )

        val result = aggregateProduction(entries, testMode = false)

        assertEquals(2, result.size)
        assertEquals(250.0, result.first().levelMw, 0.0)
        assertEquals("SOFIA_TOTAL", result.first().bmuId)
        assertEquals(200.0, result.last().levelMw, 0.0)
    }

    @Test
    fun aggregateProduction_keepsTestModeIdentifier() {
        val entries = listOf(
            PnEntryDto("T_HEYM11", "2026-04-01T00:00:00", "2026-04-01T00:30:00", 1, 42.5, "pn"),
        )

        val result = aggregateProduction(entries, testMode = true)

        assertEquals(1, result.size)
        assertEquals(SofiaProductionRepository.TEST_BMU, result.single().bmuId)
        assertEquals(42.5, result.single().levelMw, 0.0)
        assertTrue(result.single().timeFrom.isBefore(result.single().timeTo))
    }
}
