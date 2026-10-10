package com.lemarc.sofia.widget.modern

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import kotlin.math.roundToInt

private val CompactSize = DpSize(110.dp, 110.dp)
private val WideSize = DpSize(250.dp, 110.dp)

/** Disponibilité (REMIT) par unité : 2x2 = grille de 4 tuiles, 4x2 = 4 tuiles + total. */
class SofiaUnitsGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(CompactSize, WideSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = SofiaWidgetData.loadUnits(context)
        provideContent {
            val wide = LocalSize.current.width >= WideSize.width
            WidgetRoot { if (wide) WideLayout(data) else CompactLayout(data) }
        }
    }
}

@Composable
private fun WideLayout(data: UnitsData) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WText("DISPONIBILITÉ PAR UNITÉ", 10, WidgetPalette.Muted, bold = true)
            Spacer(modifier = GlanceModifier.defaultWeight())
            val total = if (data.totalNormal > 0) {
                "${data.totalAvailable.roundToInt()} / ${data.totalNormal.roundToInt()} MW"
            } else {
                "${data.totalAvailable.roundToInt()} MW"
            }
            WText(total, 11, bold = true)
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            data.units.forEachIndexed { i, unit ->
                if (i > 0) Spacer(modifier = GlanceModifier.width(6.dp))
                UnitTile(unit, GlanceModifier.defaultWeight().fillMaxHeight(), compact = false)
            }
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        WText(
            text = (if (data.fromCache) "Hors-ligne · " else "Mis à jour ") + formatClock(data.fetchedAt),
            size = 9,
            color = WidgetPalette.Muted,
        )
    }
}

@Composable
private fun CompactLayout(data: UnitsData) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(8.dp)) {
        data.units.chunked(2).forEachIndexed { r, rowUnits ->
            if (r > 0) Spacer(modifier = GlanceModifier.height(6.dp))
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                rowUnits.forEachIndexed { i, unit ->
                    if (i > 0) Spacer(modifier = GlanceModifier.width(6.dp))
                    UnitTile(unit, GlanceModifier.defaultWeight().fillMaxHeight(), compact = true)
                }
            }
        }
    }
}

@Composable
private fun UnitTile(unit: UnitStatus, modifier: GlanceModifier, compact: Boolean) {
    val color = unit.statusColor()
    val mw = unit.availableMw?.roundToInt()?.toString() ?: "—"
    Column(
        modifier = modifier
            .background(solid(WidgetPalette.Tile))
            .cornerRadius(14.dp)
            .padding(horizontal = 8.dp, vertical = if (compact) 4.dp else 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(color, sizeDp = 6)
            Spacer(modifier = GlanceModifier.width(4.dp))
            WText(if (compact) unit.shortLabel else unit.label, if (compact) 10 else 9, WidgetPalette.Muted, bold = true)
        }
        if (compact) {
            WText("$mw MW", 13, color, bold = true)
        } else {
            WText(mw, 18, color, bold = true)
            WText("MW", 9, WidgetPalette.Muted)
            Spacer(modifier = GlanceModifier.height(4.dp))
            LinearProgressIndicator(
                progress = unit.fraction,
                modifier = GlanceModifier.fillMaxWidth(),
                color = solid(color),
                backgroundColor = solid(WidgetPalette.Track),
            )
        }
    }
}

class SofiaUnitsGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SofiaUnitsGlanceWidget()
}