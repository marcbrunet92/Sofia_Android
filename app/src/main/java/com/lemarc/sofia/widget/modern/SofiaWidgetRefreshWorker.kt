package com.lemarc.sofia.widget.modern

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Rafraîchit les 3 widgets toutes les ~30 min, appli fermée, dès qu'il y a du réseau.
 * Plus fiable que updatePeriodMillis seul (que le système peut espacer fortement).
 * Se désactive tout seul quand il n'y a plus aucun widget.
 */
class SofiaWidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(applicationContext)
        val hasWidgets = listOf(
            SofiaLiveGlanceWidget::class.java,
            SofiaUnitsGlanceWidget::class.java,
            SofiaGraphGlanceWidget::class.java,
        ).any { manager.getGlanceIds(it).isNotEmpty() }

        if (!hasWidgets) {
            cancel(applicationContext)
            return Result.success()
        }
        SofiaModernWidgets.updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val NAME = "sofia_modern_widgets_refresh"

        /** Idempotent (KEEP) : peut être appelé autant de fois que nécessaire. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SofiaWidgetRefreshWorker>(30, TimeUnit.MINUTES)
                .setInitialDelay(30, TimeUnit.MINUTES) // pas d'exécution immédiate pendant la configuration
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}

/** Base commune des receivers : programme le rafraîchissement périodique. */
abstract class ModernWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SofiaWidgetRefreshWorker.schedule(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SofiaWidgetRefreshWorker.schedule(context) // se « répare » après un redémarrage / force-stop
    }
}