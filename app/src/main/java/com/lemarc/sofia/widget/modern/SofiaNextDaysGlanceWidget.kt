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
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
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
import com.lemarc.sofia.R
import com.lemarc.sofia.SOFIA_MAX_CAPACITY_MW
import com.lemarc.sofia.data.model.UouDay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val CompactSize = DpSize(110.dp, 110.dp)
private val WideSize = DpSize(250.dp, 110.dp)

/**
 * 14 prochains jours : puissance utilisable prévue (UOU2T14D), une barre par jour.
 * 2x2 = barres + aujourd'hui, 4x2 = barres, aujourd'hui, premier et dernier jour.
 */
class SofiaNextDaysGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(CompactSize, WideSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = SofiaWidgetData.loadNextDays(context)
        val today = LocalDate.now()
        val days = data.days.filter { !it.date.isBefore(today) }.take(14)
        // Échelle : 1 400 MW pour Sofia ; en mode test, la plus grande valeur
        val scale = if (data.testMode) days.maxOfOrNull { it.outputUsableMw } ?: 1.0 else SOFIA_MAX_CAPACITY_MW
        val density = context.resources.displayMetrics.density
        val bars = days.takeIf { it.isNotEmpty() }?.let {
            SofiaWidgetCharts.dayBars((280 * density).toInt(), (60 * density).toInt(), density, it.map { d -> d.outputUsableMw }, scale)
        }

        provideContent {
            val wide = LocalSize.current.width >= WideSize.width
            WidgetRoot { NextDaysContent(data, days, bars, wide) }
        }
    }
}

private val dayFormatter: DateTimeFormatter get() = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

@Composable
private fun NextDaysContent(data: NextDaysData, days: List<UouDay>, bars: Bitmap?, wide: Boolean) {
    val context = LocalContext.current
    val today = days.firstOrNull()
    Column(modifier = GlanceModifier.fillMaxSize().padding(if (wide) 12.dp else 10.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WText(
                context.getString(if (wide) R.string.widget_next_days_header else R.string.widget_next_days_header_short),
                10,
                WidgetPalette.Muted,
                bold = true,
            )
            if (data.testMode) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                WText(context.getString(R.string.widget_live_test), 9, WidgetPalette.Bad, bold = true)
            }
        }
        if (today != null) {
            WText(
                context.getString(R.string.widget_next_days_today_format, today.outputUsableMw.roundToInt()),
                if (wide) 16 else 13,
                bold = true,
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        if (bars == null) {
            Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.Center) {
                WText(context.getString(R.string.widget_graph_no_data), 12, WidgetPalette.Muted)
            }
        } else {
            Image(
                provider = ImageProvider(bars),
                contentDescription = context.getString(R.string.widget_next_days_description),
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                contentScale = ContentScale.FillBounds,
            )
        }
        if (days.size > 1) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                WText(dayLabel(context, days.first(), wide), 9, WidgetPalette.Muted)
                Spacer(modifier = GlanceModifier.defaultWeight())
                WText(dayLabel(context, days.last(), wide), 9, WidgetPalette.Muted)
            }
        }
        if (wide) {
            WText(
                text = (if (data.fromCache) context.getString(R.string.widget_prefix_offline) else context.getString(R.string.widget_prefix_updated)) + formatClock(data.fetchedAt),
                size = 9,
                color = WidgetPalette.Muted,
            )
        }
    }
}

/** « 15 oct. · 401 MW » en grand format, « 15 oct. » en petit. */
private fun dayLabel(context: Context, day: UouDay, wide: Boolean): String {
    val date = dayFormatter.format(day.date)
    return if (wide) context.getString(R.string.next_days_label_format, date, day.outputUsableMw.roundToInt()) else date
}

class SofiaNextDaysGlanceReceiver : ModernWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SofiaNextDaysGlanceWidget()
}
