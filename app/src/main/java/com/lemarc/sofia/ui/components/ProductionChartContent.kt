package com.lemarc.sofia.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.data.model.GraphPoint
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import java.time.Instant
import kotlin.math.roundToInt
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Format de l'axe X choisi d'après la durée RÉELLEMENT visible (et non d'après le chip sélectionné),
 * donc il suit aussi les pincements de zoom.
 */
private enum class AxisTimeFormat(val pattern: String) {
    HOURS("HH:mm"),         // ≤ 12 h
    DAY_HOURS("EEE HH:mm"), // ≤ 3 j ET dans les 3 derniers jours de données
    DATES("dd/MM"),         // ≤ 1 an
    MONTHS("MM/yy");        // > 1 an

    companion object {
        private val RECENT_MS = Duration.ofDays(3).toMillis()

        fun forVisibleRange(spanMs: Double, startMs: Double, dataEndMs: Double): AxisTimeFormat {
            // Petite tolérance : le calcul de la fenêtre visible est approximatif (en pixels)
            val tolerance = spanMs * 0.05
            val isRecent = startMs >= dataEndMs - RECENT_MS - tolerance
            return when {
                spanMs <= Duration.ofHours(12).toMillis() -> HOURS
                spanMs <= RECENT_MS && isRecent -> DAY_HOURS
                spanMs <= Duration.ofDays(365).toMillis() -> DATES
                else -> MONTHS
            }
        }
    }
}

data class ChartSeries(
    val points: List<GraphPoint>,
    val allowNegative: Boolean,
    val unit: String,
    val label: String = "",
    val color: Int, // Color.rgb(76, 175, 80),
    /** Zone sous la courbe colorée (MEL : plafond de production). */
    val filled: Boolean = false,
) {
    constructor(
        points: List<GraphPoint>,
        allowNegative: Boolean,
        unit: String,
        label: Int,
        color: Int,
        filled: Boolean = false,
    ) : this(points, allowNegative, unit, label.toString(), color, filled)
}

private fun colorFor(color: Int) = androidx.compose.ui.graphics.Color(color)

private fun areaFillFor(series: ChartSeries): LineCartesianLayer.AreaFill? =
    if (series.filled) LineCartesianLayer.AreaFill.single(Fill(colorFor(series.color).copy(alpha = 0.2f))) else null

// x = timestamp en millisecondes epoch, pour que chaque quantity reste liée à son propre instant
private fun xOf(point: GraphPoint): Double = point.timeFrom.toEpochMilli().toDouble()

private fun zoomFor(tw: TimeWindow): Zoom =
    tw.duration?.let { Zoom.x(it.toMillis().toDouble()) } ?: Zoom.Content // "All" : tout voir

@Composable
fun ProductionChartContent(
    left: List<ChartSeries>,
    right: List<ChartSeries>,
    tw: TimeWindow,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    // Zoom/scroll INITIAUX = fenêtre sélectionnée : Vico les applique dès que le premier modèle arrive.
    // Avant, l'effet sur `tw` s'exécutait alors que le modèle était encore vide (retour sur l'onglet,
    // item recréé par la LazyColumn...), donc le graphique restait sur « All » alors que le chip disait autre chose.
    val scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End)
    val zoomState = rememberVicoZoomState(zoomEnabled = true, initialZoom = zoomFor(tw))

    // Toutes les données vont dans le modèle — pas de filtrage par fenêtre temporelle
    LaunchedEffect(left, right) {
        modelProducer.runTransaction {
            if (left.isNotEmpty()) {
                lineModel {
                    left.forEach { series ->
                        series(x = series.points.map { xOf(it) }, y = series.points.map { it.quantity })
                    }
                }
            }
            if (right.isNotEmpty()) {
                lineModel {
                    right.forEach { series ->
                        series(x = series.points.map { xOf(it) }, y = series.points.map { it.quantity })
                    }
                }
            }
        }
    }

    // `tw` ne change que le niveau de zoom + la position de scroll, jamais les données.
    // La 1re exécution est ignorée : l'état initial ci-dessus s'en charge déjà.
    var skipFirstWindow by remember { mutableStateOf(true) }
    LaunchedEffect(tw) {
        if (skipFirstWindow) {
            skipFirstWindow = false
            return@LaunchedEffect
        }
        scrollState.animateScroll(Scroll.Absolute.End)
        zoomState.animateZoom(zoomFor(tw))
        scrollState.animateScroll(Scroll.Absolute.End)
    }

    // ── Fenêtre visible estimée ──────────────────────────────────────────────
    // contenu total (px) = viewport + maxValue ; la fenêtre visible couvre [value, value + viewport]
    val dataRange = remember(left, right) {
        val filled = (left + right).filter { it.points.isNotEmpty() }
        if (filled.isEmpty()) 0.0 to 0.0
        else filled.minOf { xOf(it.points.first()) } to filled.maxOf { xOf(it.points.last()) }
    }
    var viewportPx by remember { mutableFloatStateOf(0f) }
    val visibleFormat by remember(dataRange) {
        derivedStateOf {
            val (dataStartMs, dataEndMs) = dataRange
            val totalSpanMs = dataEndMs - dataStartMs
            val viewport = viewportPx
            if (viewport <= 0f || totalSpanMs <= 0.0) {
                AxisTimeFormat.forVisibleRange(totalSpanMs, dataStartMs, dataEndMs)
            } else {
                val maxScroll = scrollState.maxValue.coerceAtLeast(0f)
                val contentPx = viewport + maxScroll
                val spanMs = totalSpanMs * viewport / contentPx
                val startMs = dataStartMs + totalSpanMs * scrollState.value.coerceIn(0f, maxScroll) / contentPx
                AxisTimeFormat.forVisibleRange(spanMs, startMs, dataEndMs)
            }
        }
    }
    val formatters = remember {
        AxisTimeFormat.entries.associateWith {
            DateTimeFormatter.ofPattern(it.pattern, Locale.getDefault()).withZone(ZoneId.systemDefault())
        }
    }

    val leftLayer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(
            List(left.size) { i ->
                LineCartesianLayer.rememberLine(
                    fill = LineCartesianLayer.LineFill.single(Fill(colorFor(left[i].color))),
                    areaFill = areaFillFor(left[i]),
                )
            },
        ),
        verticalAxisPosition = Axis.Position.Vertical.Start,
    )

    val rightLayer = if (right.isNotEmpty()) {
        rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(
                List(right.size) { i ->
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(Fill(colorFor(right[i].color))),
                        areaFill = areaFillFor(right[i]),
                    )
                },
            ),
            verticalAxisPosition = Axis.Position.Vertical.End,
        )
    } else null

    val leftUnit = left.firstOrNull()?.unit.orEmpty()
    val rightUnit = right.firstOrNull()?.unit.orEmpty()
    val axisTitleStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)

    CartesianChartHost(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .onSizeChanged { viewportPx = it.width.toFloat() },
        scrollState = scrollState,
        zoomState = zoomState,
        chart = rememberCartesianChart(
            leftLayer,
            *listOfNotNull(rightLayer).toTypedArray(),
            startAxis = VerticalAxis.rememberStart(
                title = { leftUnit },
                titleComponent = rememberTextComponent(style = axisTitleStyle),
                titlePosition = BaseAxis.TitlePosition.End,
                valueFormatter = { _, value, _ -> "${value.roundToInt()}" },
            ),
            endAxis = if (rightLayer != null) {
                VerticalAxis.rememberEnd(
                    title = { rightUnit },
                    titleComponent = rememberTextComponent(style = axisTitleStyle),
                    titlePosition = BaseAxis.TitlePosition.End,
                    valueFormatter = { _, value, _ -> "${value.roundToInt()}" },
                )
            } else null,
            bottomAxis = HorizontalAxis.rememberBottom(
                // Lu à chaque dessin : le format suit le zoom courant (pincement ou chip)
                valueFormatter = { _, value, _ ->
                    formatters.getValue(visibleFormat).format(Instant.ofEpochMilli(value.toLong()))
                },
                labelRotationDegrees = -20f,
            ),
        ),
        modelProducer = modelProducer,
    )
}