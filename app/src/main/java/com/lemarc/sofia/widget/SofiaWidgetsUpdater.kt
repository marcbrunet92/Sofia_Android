package com.lemarc.sofia.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lemarc.sofia.MainActivity
import com.lemarc.sofia.R
import com.lemarc.sofia.data.model.ProductionSnapshot
import com.lemarc.sofia.data.repository.SofiaProductionRepository
import com.lemarc.sofia.data.settings.SettingsRepository
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

object SofiaWidgetsUpdater {
    private val timestampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC)

    suspend fun updateAll(context: Context) {
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext)
        val smallIds = appWidgetManager.getAppWidgetIds(
            ComponentName(appContext, SofiaSmallWidgetProvider::class.java),
        )
        val mediumIds = appWidgetManager.getAppWidgetIds(
            ComponentName(appContext, SofiaMediumWidgetProvider::class.java),
        )
        if (smallIds.isEmpty() && mediumIds.isEmpty()) return

        val testMode = SettingsRepository(appContext).testMode.first()
        val fetchedAt = Instant.now()
        val snapshot = runCatching {
            SofiaProductionRepository.create().fetchProduction(testMode)
        }.getOrNull()

        smallIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(
                appWidgetId,
                buildSmallRemoteViews(
                    context = appContext,
                    snapshot = snapshot,
                    testMode = testMode,
                ),
            )
        }
        mediumIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(
                appWidgetId,
                buildMediumRemoteViews(
                    context = appContext,
                    snapshot = snapshot,
                    testMode = testMode,
                    fetchedAt = fetchedAt,
                ),
            )
        }
    }

    private fun buildSmallRemoteViews(
        context: Context,
        snapshot: ProductionSnapshot?,
        testMode: Boolean,
    ): RemoteViews {
        val safeMaxCapacity = snapshot?.maxCapacityMw?.coerceAtLeast(1.0) ?: 1.0
        val currentMw = snapshot?.currentMw ?: 0.0
        val capacityPercent = ((currentMw / safeMaxCapacity) * 100).roundToInt().coerceIn(0, 100)
        return RemoteViews(context.packageName, R.layout.widget_sofia_small).apply {
            setOnClickPendingIntent(R.id.widget_small_root, openAppPendingIntent(context))
            setProgressBar(R.id.widget_small_ring, 100, capacityPercent, false)
            setTextViewText(R.id.widget_small_mw, context.getString(R.string.widget_mw_value, currentMw.roundToInt()))
            setTextViewText(
                R.id.widget_small_capacity,
                context.getString(R.string.widget_capacity_value, capacityPercent),
            )
            setViewVisibility(
                R.id.widget_small_test_badge,
                if (testMode) android.view.View.VISIBLE else android.view.View.GONE,
            )
        }
    }

    private fun buildMediumRemoteViews(
        context: Context,
        snapshot: ProductionSnapshot?,
        testMode: Boolean,
        fetchedAt: Instant,
    ): RemoteViews {
        val safeMaxCapacity = snapshot?.maxCapacityMw?.coerceAtLeast(1.0) ?: 1.0
        val currentMw = snapshot?.currentMw ?: 0.0
        val capacityPercent = ((currentMw / safeMaxCapacity) * 100).roundToInt().coerceIn(0, 100)
        val lastApiUpdate = snapshot?.latestDataTimestamp?.let(timestampFormatter::format) ?: "—"
        val refreshedAt = timestampFormatter.format(fetchedAt)
        return RemoteViews(context.packageName, R.layout.widget_sofia_medium).apply {
            setOnClickPendingIntent(R.id.widget_medium_root, openAppPendingIntent(context))
            setProgressBar(R.id.widget_medium_ring, 100, capacityPercent, false)
            setProgressBar(R.id.widget_medium_bar, 100, capacityPercent, false)
            setTextViewText(R.id.widget_medium_mw, context.getString(R.string.widget_mw_value, currentMw.roundToInt()))
            setTextViewText(
                R.id.widget_medium_capacity,
                context.getString(R.string.widget_capacity_of_max_value, capacityPercent, safeMaxCapacity.roundToInt()),
            )
            setTextViewText(
                R.id.widget_medium_details,
                context.getString(
                    R.string.widget_details_value,
                    if (testMode) context.getString(R.string.widget_mode_test) else context.getString(R.string.widget_mode_live),
                    lastApiUpdate,
                ),
            )
            setTextViewText(
                R.id.widget_medium_last_update,
                context.getString(R.string.widget_last_refresh_value, refreshedAt),
            )
            setViewVisibility(
                R.id.widget_medium_test_badge,
                if (testMode) android.view.View.VISIBLE else android.view.View.GONE,
            )
        }
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
