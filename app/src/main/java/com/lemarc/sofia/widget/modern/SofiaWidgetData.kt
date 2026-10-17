package com.lemarc.sofia.widget.modern

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.lemarc.sofia.BASE_URL
import com.lemarc.sofia.HISTORY_START
import com.lemarc.sofia.SOFIA_BMUS
import com.lemarc.sofia.SOFIA_MAX_CAPACITY_MW
import com.lemarc.sofia.data.api.SofiaApiService
import com.lemarc.sofia.data.api.getB1610For
import com.lemarc.sofia.data.api.getProductionFor
import com.lemarc.sofia.data.api.getUouFor
import com.lemarc.sofia.data.local.SofiaDatabase
import com.lemarc.sofia.data.model.GraphPoint
import com.lemarc.sofia.data.model.RemitNotice
import com.lemarc.sofia.data.model.UouDay
import com.lemarc.sofia.data.repository.aggregateB1610
import com.lemarc.sofia.data.repository.aggregateProduction
import com.lemarc.sofia.data.repository.toEntity
import com.lemarc.sofia.data.repository.toModel
import com.lemarc.sofia.util.parseApiInstant
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.time.Duration.Companion.milliseconds

// ── Modèles ──────────────────────────────────────────────────────────────────

data class LiveData(
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

/** Puissance utilisable prévue par jour (UOU2T14D), pour le widget « 14 prochains jours ». */
data class NextDaysData(
    val days: List<UouDay>,
    val fetchedAt: Instant,
    val fromCache: Boolean,
)

/**
 * Production annoncée de Sofia (PN) comparée à l'éolien de Grande-Bretagne (AGWS) sur la même
 * demi-heure, la dernière publiée par AGWS.
 */
data class GbShareData(
    val timeFrom: Instant?,
    val sofiaMw: Double?,
    val offshoreMw: Double?,
    val onshoreMw: Double?,
    val fetchedAt: Instant,
    val fromCache: Boolean,
) {
    /** Part de Sofia dans l'éolien en mer GB, en %. */
    val offshoreSharePercent: Double? get() = shareOf(offshoreMw)

    /** Part de Sofia dans tout l'éolien GB (en mer + terrestre), en %. */
    val windSharePercent: Double?
        get() = shareOf(if (offshoreMw != null && onshoreMw != null) offshoreMw + onshoreMw else null)

    private fun shareOf(totalMw: Double?): Double? =
        if (sofiaMw != null && totalMw != null && totalMw > 0.0) sofiaMw / totalMw * 100 else null
}

data class UnitsData(
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
        val to = Instant.now()
        val from = window?.let { to.minus(it) } ?: HISTORY_START
        val snapshotName = "live_${window?.toMinutes() ?: "all"}_${if (includeActual) "a" else "c"}"

        val fresh = runCatching {
            withTimeout(30_000.milliseconds) {
                coroutineScope {
                    val pn = async { api.getProductionFor( from.toString(), to.toString()) }
                    val b1610 = if (includeActual) {
                        async { api.getB1610For( from.toString(), to.toString()) }
                    } else null
                    val committed = aggregateProduction(pn.await())
                    val actual = b1610
                        ?.let { call -> aggregateB1610(call.await()) }
                        ?.map { it.copy(quantity = it.quantity * 2) }
                        ?: emptyList()
                    committed to actual
                }
            }
        }.getOrNull()

        if (fresh != null) {
            LiveData( fresh.first, fresh.second, from, to, fromCache = false)
                .also { SofiaWidgetSnapshots.saveLive(app, snapshotName, it) }
        } else {
            val dao = SofiaDatabase.getDatabase(app).sofiaDao()
            val committed = dao.getProductionPoints().first()
                .filter { it.timeFrom >= from }.map { it.toModel() }
            val actual = if (includeActual) {
                dao.getB1610Points().first()
                    .filter { it.timeFrom >= from }
                    .map { it.toModel() }
                    .map { it.copy(quantity = it.quantity * 2) }
            } else emptyList()
            val room = LiveData( committed, actual, from, to, fromCache = true)
            // Cache Room vide/périmé (appli pas ouverte depuis longtemps) → dernier snapshot du widget
            if (committed.isNotEmpty()) room
            else SofiaWidgetSnapshots.loadLive(app, snapshotName) ?: room
        }
    }

    /** Prévision UOU2T14D à partir d'aujourd'hui ; en secours le dernier instantané, puis le cache de l'appli. */
    suspend fun loadNextDays(context: Context): NextDaysData = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val now = Instant.now()

        val fresh = runCatching {
            withTimeout(20_000.milliseconds) {
                api.getUouFor().map { UouDay(LocalDate.parse(it.forecast_date), it.output_usable) }
            }
        }.getOrNull()

        if (fresh != null) {
            return@withContext NextDaysData( fresh, now, fromCache = false)
                .also { SofiaWidgetSnapshots.saveNextDays(app, it) }
        }
        SofiaWidgetSnapshots.loadNextDays(app)?.let { return@withContext it }
        val cached = SofiaDatabase.getDatabase(app).sofiaDao().getUouDays().first()
            .map { UouDay(LocalDate.ofEpochDay(it.epochDay), it.outputUsableMw) }
        NextDaysData(cached, now, fromCache = true)
    }

    /**
     * Dernière demi-heure AGWS et PN de Sofia sur la même demi-heure (le B1610 arrive trop tard).
     * En secours le dernier instantané, puis le cache de l'appli.
     */
    suspend fun loadGbShare(context: Context): GbShareData = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val now = Instant.now()

        val fresh = runCatching {
            withTimeout(20_000.milliseconds) {
                val gb = api.getAgwsLatest()
                val timeFrom = parseApiInstant(gb.time_from)
                val sofiaMw = timeFrom?.let { start ->
                    val pn = api.getProductionFor(start.toString(), start.plus(Duration.ofMinutes(30)).toString())
                    aggregateProduction(pn).firstOrNull { it.timeFrom == start }?.quantity
                }
                GbShareData(timeFrom, sofiaMw, gb.wind_offshore_mw, gb.wind_onshore_mw, now, fromCache = false)
            }
        }.getOrNull()

        if (fresh != null) {
            return@withContext fresh.also { SofiaWidgetSnapshots.saveGbShare(app, it) }
        }
        SofiaWidgetSnapshots.loadGbShare(app)?.let { return@withContext it }
        val dao = SofiaDatabase.getDatabase(app).sofiaDao()
        val gb = dao.getGbContext().first()
        val sofiaMw = gb?.let { latest ->
            dao.getProductionPoints().first().lastOrNull { it.timeFrom == latest.timeFrom }?.quantity
        }
        GbShareData(gb?.timeFrom, sofiaMw, gb?.windOffshoreMw, gb?.windOnshoreMw, now, fromCache = true)
    }

    /** Disponibilité par unité d'après les avis REMIT actifs (sans avis → pleine capacité nominale). */
    suspend fun loadUnits(context: Context): UnitsData = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val bmus = SOFIA_BMUS
        val now = Instant.now()

        val fresh: List<RemitNotice>? = runCatching {
            withTimeout(20_000.milliseconds) {
                coroutineScope {
                    bmus.map { id ->
                        async { api.getRemits(bmuId = id, eventStatus = "Active", limit = 100, offset = 0) }
                    }.awaitAll().flatten().distinctBy { it.id }.map { it.toEntity().toModel() }
                }
            }
        }.getOrNull()

        if (fresh == null) {
            SofiaWidgetSnapshots.loadUnits(app)?.let { return@withContext it }
        }

        val notices = fresh ?: SofiaDatabase.getDatabase(app).sofiaDao()
            .getRemitNotices().first().map { it.toModel() }

        val units = computeUnitStatuses(notices, now)
        UnitsData( units, now, fromCache = fresh == null)
            .also { if (fresh != null) SofiaWidgetSnapshots.saveUnits(app, it) }
    }
}

/** Disponibilité par unité à l'instant [now] d'après les avis REMIT (sans avis → pleine capacité nominale). */
fun computeUnitStatuses(notices: List<RemitNotice>, now: Instant): List<UnitStatus> {
    val bmus = SOFIA_BMUS
    val nominal = SOFIA_MAX_CAPACITY_MW / SOFIA_BMUS.size
    return bmus.map { id ->
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
}

private fun RemitNotice.isInEffectAt(t: Instant): Boolean =
    (eventStartTime == null || eventStartTime <= t) && (eventEndTime == null || eventEndTime >= t)

private fun RemitNotice.availableMw(): Double? {
    availableCapacityMw?.let { return it }
    val n = normalCapacityMw
    val u = unavailableCapacityMw
    return if (n != null && u != null) n - u else null
}

object SofiaModernWidgets {
    suspend fun updateAll(context: Context) {
        SofiaLiveGlanceWidget().updateAll(context)
        SofiaUnitsGlanceWidget().updateAll(context)
        SofiaGraphGlanceWidget().updateAll(context)
        SofiaNextDaysGlanceWidget().updateAll(context)
        SofiaGbShareGlanceWidget().updateAll(context)
    }
}