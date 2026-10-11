package com.lemarc.sofia.widget.modern

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width

/**
 * Courbe de production sur la fenêtre choisie à la création du widget (SofiaGraphConfigActivity) :
 * production engagée (PN, remplie) et, si la fenêtre est ≥ 7 jours, production réelle (B1610).
 * Idéal en 4x2.
 */
class SofiaGraphGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val window = GraphWidgetPrefs.getWindow(context, appWidgetId)
        val showActual = window.showsActual()

        val data = SofiaWidgetData.loadLive(
            context = context,
            window = window.duration, // null pour « All »
            includeActual = showActual,
        )
        val density = context.resources.displayMetrics.density

        val chart = if (data.committed.isEmpty() && (!showActual || data.actual.isEmpty())) {
            null
        } else {
            val lines = buildList {
                add(ChartLine(data.committed, WidgetPalette.Committed.toArgb(), filled = true))
                if (showActual) add(ChartLine(data.actual, WidgetPalette.Actual.toArgb()))
            }
            SofiaWidgetCharts.lineChart(
                widthPx = (320 * density).toInt(),
                heightPx = (140 * density).toInt(),
                density = density,
                lines = lines,
                from = data.from,
                to = data.to,
            )
        }

        provideContent {
            WidgetRoot {
                Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        WText(
                            text = "PRODUCTION · ${window.shortTitle()}" + if (data.testMode) " · TEST" else "",
                            size = 10,
                            color = WidgetPalette.Muted,
                            bold = true,
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Dot(WidgetPalette.Committed)
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        WText("Engagé", 10)
                        if (showActual) {
                            Spacer(modifier = GlanceModifier.width(10.dp))
                            Dot(WidgetPalette.Actual)
                            Spacer(modifier = GlanceModifier.width(4.dp))
                            WText("Réel", 10)
                        }
                    }
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    if (chart == null) {
                        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            WText("Pas de données", 12, WidgetPalette.Muted)
                        }
                    } else {
                        Image(
                            provider = ImageProvider(chart),
                            contentDescription = "Production sur ${window.longLabel()}",
                            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
        }
    }
}

class SofiaGraphGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SofiaGraphGlanceWidget()

    // Nettoie la fenêtre mémorisée quand le widget est retiré
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        GraphWidgetPrefs.clear(context, appWidgetIds)
    }
}