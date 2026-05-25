package com.lemarc.sofia.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.progressindicator.CircularProgressIndicator
import androidx.glance.appwidget.progressindicator.LinearProgressIndicator
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.defaultWeight
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lemarc.sofia.data.repository.SofiaProductionRepository
import com.lemarc.sofia.data.settings.SettingsRepository
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

private val widgetTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
        .withZone(ZoneOffset.UTC)

private data class WidgetSnapshot(
    val currentMw: Double,
    val maxCapacityMw: Double,
    val percent: Int,
    val latestDataTimestamp: Instant?,
    val testMode: Boolean,
    val hasError: Boolean,
)

private suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot {
    val testMode = SettingsRepository(context).testMode.first()
    return runCatching {
        val snapshot = SofiaProductionRepository.create().fetchProduction(testMode)
        val safeCapacity = snapshot.maxCapacityMw.coerceAtLeast(1.0)
        val percent = ((snapshot.currentMw / safeCapacity) * 100).roundToInt().coerceIn(0, 100)
        WidgetSnapshot(
            currentMw = snapshot.currentMw,
            maxCapacityMw = snapshot.maxCapacityMw,
            percent = percent,
            latestDataTimestamp = snapshot.latestDataTimestamp,
            testMode = testMode,
            hasError = false,
        )
    }.getOrElse {
        WidgetSnapshot(
            currentMw = 0.0,
            maxCapacityMw = SofiaProductionRepository.SOFIA_MAX_CAPACITY_MW,
            percent = 0,
            latestDataTimestamp = null,
            testMode = testMode,
            hasError = true,
        )
    }
}

private class SofiaWidget(
    private val medium: Boolean,
) : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val snapshot = loadWidgetSnapshot(context)
        provideContent {
            SofiaWidgetContent(snapshot = snapshot, medium = medium)
        }
    }
}

class SofiaSmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SofiaWidget(medium = false)
}

class SofiaMediumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SofiaWidget(medium = true)
}

@androidx.glance.Composable
private fun SofiaWidgetContent(
    snapshot: WidgetSnapshot,
    medium: Boolean,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF10182A))
            .padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        if (snapshot.testMode) {
            Row(
                modifier = GlanceModifier
                    .background(Color(0xFFFBC02D))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = "TEST",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF2B1D00)),
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
        }

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier.width(72.dp).height(72.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = snapshot.percent / 100f,
                    modifier = GlanceModifier.fillMaxSize(),
                )
                Text(
                    text = "${snapshot.percent}%",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Spacer(modifier = GlanceModifier.width(10.dp))
            Column(
                modifier = GlanceModifier.defaultWeight(),
            ) {
                Text(
                    text = "${snapshot.currentMw.roundToInt()} MW",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = "${snapshot.percent}% capacity",
                    style = TextStyle(color = ColorProvider(Color(0xFFBFD4FF))),
                )
                if (medium) {
                    Text(
                        text = "Max ${snapshot.maxCapacityMw.roundToInt()} MW",
                        style = TextStyle(color = ColorProvider(Color(0xFFBFD4FF))),
                    )
                }
            }
        }

        if (medium) {
            Spacer(modifier = GlanceModifier.height(10.dp))
            LinearProgressIndicator(
                progress = snapshot.percent / 100f,
                modifier = GlanceModifier.fillMaxWidth(),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "Last update: ${
                    snapshot.latestDataTimestamp?.let(widgetTimestampFormatter::format) ?: "—"
                }",
                style = TextStyle(color = ColorProvider(Color(0xFFBFD4FF))),
            )
            Text(
                text = if (snapshot.hasError) "Status: data unavailable" else "Status: OK",
                style = TextStyle(
                    color = ColorProvider(if (snapshot.hasError) Color(0xFFFF8A80) else Color(0xFF9FF7A7)),
                ),
            )
            Text(
                text = "Mode: ${if (snapshot.testMode) "TEST (T_HEYM11)" else "NORMAL (SOFIA aggregate)"}",
                style = TextStyle(color = ColorProvider(Color(0xFFBFD4FF))),
            )
        } else if (snapshot.hasError) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = "No data",
                style = TextStyle(color = ColorProvider(Color(0xFFFF8A80))),
            )
        }
    }
}
