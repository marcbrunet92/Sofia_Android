package com.lemarc.sofia.widget.modern

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
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
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.lemarc.sofia.TimeWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Courbe de production sur la fenêtre choisie à la création du widget (SofiaGraphConfigActivity) :
 * production engagée (PN, remplie) et, si la fenêtre est ≥ 7 jours, production réelle (B1610).
 *
 * La fenêtre est lue dans l'ÉTAT Glance et les données sont (re)chargées DANS la composition :
 * si une session Glance est déjà ouverte (ex. rafraîchissement lancé pendant la configuration),
 * un changement de fenêtre déclenche bien un nouveau chargement, au lieu de rester sur 24 h.
 */
class SofiaGraphGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val window = GraphWidgetPrefs.windowOf(currentState<Preferences>())
            val loaded by produceState<GraphUi?>(initialValue = null, key1 = window) {
                value = buildGraphUi(context, window)
            }
            // On ignore un résultat calculé pour une autre fenêtre (changement en cours)
            val ui = loaded?.takeIf { it.window == window }

            WidgetRoot { GraphContent(window, ui) }
        }
    }
}

private data class GraphUi(
    val window: TimeWindow,
    val data: LiveData,
    val chart: Bitmap?,
)

private suspend fun buildGraphUi(context: Context, window: TimeWindow): GraphUi {
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
        withContext(Dispatchers.Default) {
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
    }
    return GraphUi(window, data, chart)
}

@Composable
private fun GraphContent(window: TimeWindow, ui: GraphUi?) {
    val showActual = window.showsActual()
    val chart = ui?.chart
    val title = (if (ui?.data?.fromCache == true) {
        "${window.shortTitle()} · HORS-LIGNE"
    } else {
        "PRODUCTION · ${window.shortTitle()}"
    }) + if (ui?.data?.testMode == true) " · TEST" else ""

    Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WText(title, 10, WidgetPalette.Muted, bold = true)
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
        when {
            ui == null -> Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                WText("Chargement…", 12, WidgetPalette.Muted)
            }
            chart == null -> Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                WText("Pas de données", 12, WidgetPalette.Muted)
            }
            else -> Image(
                provider = ImageProvider(chart),
                contentDescription = "Production sur ${window.longLabel()}",
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

// L'état Glance (fenêtre choisie) est supprimé automatiquement avec le widget : plus de nettoyage à faire.
class SofiaGraphGlanceReceiver : ModernWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SofiaGraphGlanceWidget()
}