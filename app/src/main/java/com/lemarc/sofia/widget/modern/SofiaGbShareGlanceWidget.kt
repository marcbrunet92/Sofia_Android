package com.lemarc.sofia.widget.modern

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import com.lemarc.sofia.R
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val CompactSize = DpSize(110.dp, 110.dp)
private val WideSize = DpSize(250.dp, 110.dp)

/**
 * Part de Sofia dans l'éolien de Grande-Bretagne : PN de Sofia ÷ AGWS sur la dernière demi-heure publiée.
 * 2x2 = part de l'éolien en mer, 4x2 = + valeurs en MW/GW et part de tout l'éolien.
 */
class SofiaGbShareGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(CompactSize, WideSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = SofiaWidgetData.loadGbShare(context)
        provideContent {
            val wide = LocalSize.current.width >= WideSize.width
            WidgetRoot { if (wide) GbShareWide(data) else GbShareCompact(data) }
        }
    }
}

private fun percent(value: Double?): String = value?.let { String.format(Locale.getDefault(), "%.1f %%", it) } ?: "—"

private fun gigawatts(mw: Double?): String = mw?.let { String.format(Locale.getDefault(), "%.1f", it / 1000) } ?: "—"

/** Heure de la demi-heure AGWS : « HH:mm » aujourd'hui, « dd/MM HH:mm » sinon. */
private fun agwsTime(data: GbShareData): String {
    val time = data.timeFrom ?: return "—"
    val zone = ZoneId.systemDefault()
    val pattern = if (time.atZone(zone).toLocalDate() == LocalDate.now(zone)) "HH:mm" else "dd/MM HH:mm"
    return DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).withZone(zone).format(time)
}

@Composable
private fun Header() {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        WText(context.getString(R.string.widget_gb_header), 10, WidgetPalette.Muted, bold = true)
    }
}

@Composable
private fun GbShareCompact(data: GbShareData) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Header()
        Spacer(modifier = GlanceModifier.height(4.dp))
        WText(percent(data.offshoreSharePercent), 26, bold = true)
        WText(context.getString(R.string.widget_gb_of_offshore), 10, WidgetPalette.Muted)
        Spacer(modifier = GlanceModifier.defaultWeight())
        WText(context.getString(R.string.gb_source_format, agwsTime(data)), 9, WidgetPalette.Muted)
    }
}

@Composable
private fun GbShareWide(data: GbShareData) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Header()
            Spacer(modifier = GlanceModifier.height(4.dp))
            WText(percent(data.offshoreSharePercent), 30, bold = true)
            WText(context.getString(R.string.widget_gb_of_offshore), 10, WidgetPalette.Muted)
            Spacer(modifier = GlanceModifier.height(4.dp))
            WText(
                context.getString(R.string.widget_gb_of_all_wind_format, percent(data.windSharePercent)),
                10,
            )
        }
        Spacer(modifier = GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(WidgetPalette.Committed)
                Spacer(modifier = GlanceModifier.width(6.dp))
                WText(
                    context.getString(R.string.widget_gb_sofia_format, data.sofiaMw?.roundToInt()?.toString() ?: "—"),
                    11,
                )
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            WText(context.getString(R.string.widget_gb_offshore_format, gigawatts(data.offshoreMw)), 11)
            WText(context.getString(R.string.widget_gb_onshore_format, gigawatts(data.onshoreMw)), 11, WidgetPalette.Muted)
            Spacer(modifier = GlanceModifier.height(6.dp))
            WText(context.getString(R.string.gb_source_format, agwsTime(data)), 9, WidgetPalette.Muted)
            WText(
                text = (if (data.fromCache) context.getString(R.string.widget_prefix_offline) else context.getString(R.string.widget_prefix_updated)) + formatClock(data.fetchedAt),
                size = 9,
                color = WidgetPalette.Muted,
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
    }
}

class SofiaGbShareGlanceReceiver : ModernWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SofiaGbShareGlanceWidget()
}
