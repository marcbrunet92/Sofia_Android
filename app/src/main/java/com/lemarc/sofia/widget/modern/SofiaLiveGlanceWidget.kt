package com.lemarc.sofia.widget.modern

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
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
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.TextAlign
import com.lemarc.sofia.SOFIA_MAX_CAPACITY_MW
import kotlin.math.roundToInt

private val CompactSize = DpSize(110.dp, 110.dp)
private val WideSize = DpSize(250.dp, 110.dp)

/** Production en direct : jauge + MW. 2x2 = jauge seule, 4x2 = jauge + détails. */
class SofiaLiveGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(CompactSize, WideSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = SofiaWidgetData.loadLive(context)
        val density = context.resources.displayMetrics.density
        val fraction = (data.currentMw / SOFIA_MAX_CAPACITY_MW).toFloat()
        val gauge = SofiaWidgetCharts.gauge((120 * density).toInt(), fraction)

        provideContent {
            val wide = LocalSize.current.width >= WideSize.width
            WidgetRoot {
                if (wide) WideLayout(data, gauge) else CompactLayout(data, gauge)
            }
        }
    }
}

private fun LiveData.percent(): Int =
    (currentMw / SOFIA_MAX_CAPACITY_MW * 100).roundToInt().coerceIn(0, 100)

@Composable
private fun CompactLayout(data: LiveData, gauge: Bitmap) {
    Box(
        modifier = GlanceModifier.fillMaxSize().padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(gauge),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WText("${data.currentMw.roundToInt()}", 22, bold = true, align = TextAlign.Center)
            WText("MW", 11, WidgetPalette.Muted, align = TextAlign.Center)
            if (data.testMode) WText("TEST", 9, WidgetPalette.Bad, bold = true)
        }
    }
}

@Composable
private fun WideLayout(data: LiveData, gauge: Bitmap) {
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = GlanceModifier.size(92.dp), contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(gauge),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            WText("${data.percent()}%", 18, bold = true)
        }
        Spacer(modifier = GlanceModifier.width(14.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WText("SOFIA OFFSHORE", 10, WidgetPalette.Muted, bold = true)
                if (data.testMode) {
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    WText("TEST", 10, WidgetPalette.Bad, bold = true)
                }
            }
            WText("${data.currentMw.roundToInt()} MW", 28, bold = true)
            WText("sur ${SOFIA_MAX_CAPACITY_MW.roundToInt()} MW installés", 11, WidgetPalette.Muted)
            Spacer(modifier = GlanceModifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(WidgetPalette.Actual)
                Spacer(modifier = GlanceModifier.width(6.dp))
                WText(
                    text = data.actualMw?.let { "Réel ${it.roundToInt()} MW" } ?: "Réel —",
                    size = 11,
                )
            }
            Spacer(modifier = GlanceModifier.height(2.dp))
            WText(
                text = (if (data.fromCache) "Hors-ligne · " else "Mis à jour ") + formatClock(data.to),
                size = 10,
                color = WidgetPalette.Muted,
            )
        }
    }
}

class SofiaLiveGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SofiaLiveGlanceWidget()
}
