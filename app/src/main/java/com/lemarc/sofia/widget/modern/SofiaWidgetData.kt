package com.lemarc.sofia.widget.modern

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.lemarc.sofia.BASE_URL
import com.lemarc.sofia.HISTORY_START
import com.lemarc.sofia.SOFIA_BMUS
import com.lemarc.sofia.SOFIA_MAX_CAPACITY_MW
import com.lemarc.sofia.TEST_BMU
import com.lemarc.sofia.data.api.SofiaApiService
import com.lemarc.sofia.data.local.SofiaDatabase
import com.lemarc.sofia.data.model.GraphPoint
import com.lemarc.sofia.data.model.RemitNotice
import com.lemarc.sofia.data.repository.aggregateB1610
import com.lemarc.sofia.data.repository.aggregateProduction
import com.lemarc.sofia.data.repository.toEntity
import com.lemarc.sofia.data.repository.toModel
import com.lemarc.sofia.data.settings.SettingsRepository
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// ── Modèles ──────────────────────────────────────────────────────────────────

data class LiveData(
    val testMode: Boolean,
    val committed: List<GraphPoint>, // MW (PN)
    val actual: List<GraphPoint>,    // MW (B1610 MWh/30min × 2)
    val from: Instant,
    val to: Instant,
    val fromCache: Boolean,
) {
    val currentMw: Double get() = committed.lastOrNull()?.quantity ?: 0.0
    val actualMw: Double? get() = actual.lastOrNull()?.quantity
}

data class UnitStatus(
    val bmuId: String,
    val normalMw: Double?,
    val availableMw: Double?,
    val hasNotice: Boolean,
    val cause: String?,
) {
    val label: String get() = bmuId.removePrefix("T_")
    val shortLabel: String get() = bmuId.substringAfterLast('-').removePrefix("T_")
    val fraction: Float
        get() {
            val n = normalMw
            val a = availableMw
            return if (n != null && n > 0.0 && a != null) (a / n).toFloat().coerceIn(0f, 1f) else 1f
        }
}

data class UnitsData(
    val testMode: Boolean,
    val units: List<UnitStatus>,
    val fetchedAt: Instant,
    val fromCache: Boolean,
) {
    val totalAvailable: Double get() = units.sumOf { it.availableMw ?: 0.0 }
    val totalNormal: Double get() = units.sumOf { it.normalMw ?: 0.0 }
}

// ── Chargement ───────────────────────────────────────────────────────────────

object SofiaWidgetData {

    private val api: SofiaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SofiaApiService::class.java)
    }

    /**
     * Récupère seulement la fenêtre demandée (24 h) au lieu de tout l'historique, et lit le cache Room
     * en secours. On n'ÉCRIT volontairement pas en base : refreshProductionPoints() efface tout, ça
     * tronquerait l'historique de l'appli.
     */
    suspend fun loadLive(
        context: Context,
        window: Duration? = Duration.ofHours(24), // null = tout l'historique
        includeActual: Boolean = true,            // false = on ne charge pas du tout le B1610
    ): LiveData = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val testMode = SettingsRepository(app).testMode.first()
        val to = Instant.now()
        val from = window?.let { to.minus(it) } ?: HISTORY_START
        val bmus = if (testMode) listOf(TEST_BMU) else SOFIA_BMUS
        val snapshotName = "live_${window?.toMinutes() ?: "all"}_${if (includeActual) "a" else "c"}"

        val fresh = runCatching {
            withTimeout(30_000) {
                coroutineScope {
                    val pn = bmus.map { id -> async { api.getProduction(id, from.toString(), to.toString()) } }
                    val b1610 = if (includeActual) {
                        bmus.map { id -> async { api.getB1610(id, from.toString(), to.toString()) } }
                    } else null
                    val committed = aggregateProduction(pn.awaitAll().flatten(), testMode)
                    val actual = b1610
                        ?.let { calls -> aggregateB1610(calls.awaitAll().flatten(), testMode) }
                        ?.map { it.copy(quantity = it.quantity * 2) }
                        ?: emptyList()
                    committed to actual
                }
            }
        }.getOrNull()

        if (fresh != null) {
            LiveData(testMode, fresh.first, fresh.second, from, to, fromCache = false)
                .also { SofiaWidgetSnapshots.saveLive(app, snapshotName, it) }
        } else {
            val dao = SofiaDatabase.getDatabase(app).sofiaDao()
            val committed = dao.getProductionPoints(testMode).first()
                .filter { it.timeFrom >= from }.map { it.toModel() }
            val actual = if (includeActual) {
                dao.getB1610Points(testMode).first()
                    .filter { it.timeFrom >= from }
                    .map { it.toModel() }
                    .map { it.copy(quantity = it.quantity * 2) }
            } else emptyList()
            val room = LiveData(testMode, committed, actual, from, to, fromCache = true)
            // Cache Room vide/périmé (appli pas ouverte depuis longtemps) → dernier snapshot du widget
            if (committed.isNotEmpty()) room
            else SofiaWidgetSnapshots.loadLive(app, snapshotName)?.takeIf { it.testMode == testMode } ?: room
        }
    }

    /** Disponibilité par unité d'après les avis REMIT actifs (sans avis → pleine capacité nominale). */
    suspend fun loadUnits(context: Context): UnitsData = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val testMode = SettingsRepository(app).testMode.first()
        val bmus = if (testMode) listOf(TEST_BMU) else SOFIA_BMUS
        val now = Instant.now()

        val fresh: List<RemitNotice>? = runCatching {
            withTimeout(20_000) {
                coroutineScope {
                    bmus.map { id ->
                        async { api.getRemits(bmuId = id, eventStatus = "Active", limit = 100, offset = 0) }
                    }.awaitAll().flatten().distinctBy { it.id }.map { it.toEntity(testMode).toModel() }
                }
            }
        }.getOrNull()

        if (fresh == null) {
            SofiaWidgetSnapshots.loadUnits(app)?.takeIf { it.testMode == testMode }?.let { return@withContext it }
        }

        val notices = fresh ?: SofiaDatabase.getDatabase(app).sofiaDao()
            .getRemitNotices(testMode).first().map { it.toModel() }

        // En mode test (T_HEYM11) la capacité nominale d'une unité est inconnue sans avis.
        val nominal = if (testMode) null else SOFIA_MAX_CAPACITY_MW / SOFIA_BMUS.size

        val units = bmus.map { id ->
            val active = notices.filter { it.bmuId == id && it.isInEffectAt(now) }
            val normal = active.firstNotNullOfOrNull { it.normalCapacityMw } ?: nominal
            val available = active.mapNotNull { it.availableMw() }.minOrNull() ?: normal
            UnitStatus(
                bmuId = id,
                normalMw = normal,
                availableMw = available,
                hasNotice = active.isNotEmpty(),
                cause = active.firstOrNull()?.cause?.takeIf { it.isNotBlank() },
            )
        }
        UnitsData(testMode, units, now, fromCache = fresh == null)
            .also { if (fresh != null) SofiaWidgetSnapshots.saveUnits(app, it) }
    }
}

private fun RemitNotice.isInEffectAt(t: Instant): Boolean =
    (eventStartTime == null || eventStartTime <= t) && (eventEndTime == null || eventEndTime >= t)

private fun RemitNotice.availableMw(): Double? {
    availableCapacityMw?.let { return it }
    val n = normalCapacityMw
    val u = unavailableCapacityMw
    return if (n != null && u != null) n - u else null
}

/** À appeler si tu veux forcer le rafraîchissement des 3 nouveaux widgets (ex. changement du mode test). */
object SofiaModernWidgets {
    suspend fun updateAll(context: Context) {
        SofiaLiveGlanceWidget().updateAll(context)
        SofiaUnitsGlanceWidget().updateAll(context)
        SofiaGraphGlanceWidget().updateAll(context)
    }
}