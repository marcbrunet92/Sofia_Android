package com.lemarc.sofia.widget.modern

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.compose.ui.graphics.toArgb

/** Courbe des dernières 24 h : production engagée (PN, remplie) vs réelle (B1610). Idéal en 4x2. */
class SofiaGraphGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = SofiaWidgetData.loadLive(context)
        val density = context.resources.displayMetrics.density

        val chart = if (data.committed.isEmpty() && data.actual.isEmpty()) {
            null
        } else {
            SofiaWidgetCharts.lineChart(
                widthPx = (320 * density).toInt(),
                heightPx = (140 * density).toInt(),
                density = density,
                lines = listOf(
                    ChartLine(data.committed, WidgetPalette.Committed.toArgb(), filled = true),
                    ChartLine(data.actual, WidgetPalette.Actual.toArgb()),
                ),
                from = data.from,
                to = data.to,
            )
        }

        provideContent {
            WidgetRoot {
                Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        WText(if (data.testMode) "24 H · TEST" else "DERNIÈRES 24 H", 10, WidgetPalette.Muted, bold = true)
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Dot(WidgetPalette.Committed)
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        WText("Engagé", 10)
                        Spacer(modifier = GlanceModifier.width(10.dp))
                        Dot(WidgetPalette.Actual)
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        WText("Réel", 10)
                    }
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    if (chart == null) {
                        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            WText("Pas de données", 12, WidgetPalette.Muted)
                        }
                    } else {
                        Image(
                            provider = ImageProvider(chart),
                            contentDescription = "Production des dernières 24 heures",
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
}
