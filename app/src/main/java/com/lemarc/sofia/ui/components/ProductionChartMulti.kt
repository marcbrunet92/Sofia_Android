package com.lemarc.sofia.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
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

private fun timeFormatterFor(tw: TimeWindow): DateTimeFormatter {
    val d = tw.duration
    val pattern = when {
        d == null -> "dd/MM/yy"                       // "All"
        d <= Duration.ofHours(12) -> "HH:mm"
        d <= Duration.ofDays(3) -> "EEE HH:mm"
        d <= Duration.ofDays(90) -> "dd/MM"
        else -> "MM/yy"
    }
    return DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        .withZone(ZoneId.systemDefault())
}

data class ChartSeries(
    val points: List<GraphPoint>,
    val allowNegative: Boolean,
    val unit: String,
    val label: String = "",
    val color: Int // Color.rgb(76, 175, 80),
)
private fun colorFor(color: Int) = androidx.compose.ui.graphics.Color(color)
// x = timestamp en millisecondes epoch, pour que chaque quantity reste liée à son propre instant
private fun xOf(point: GraphPoint): Double = point.timeFrom.toEpochMilli().toDouble()

@Composable
fun ProductionChartMulti(
    left: List<ChartSeries>,
    right: List<ChartSeries> = emptyList(),
    tw: TimeWindow,
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val scrollState = rememberVicoScrollState()
    val zoomState = rememberVicoZoomState(zoomEnabled = true)

    // Toutes les données vont dans le modèle — plus de filtrage par fenêtre temporelle
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

    // `tw` ne change que le niveau de zoom + la position de scroll, jamais les données
    LaunchedEffect(tw) {
        val duration = tw.duration
        if (duration != null) {
            scrollState.animateScroll(Scroll.Absolute.End)
            zoomState.animateZoom(Zoom.x(duration.toMillis().toDouble()))
            scrollState.animateScroll(Scroll.Absolute.End)
        } else {
            zoomState.animateZoom(Zoom.Content) // "All" : tout voir
        }
    }

    val leftLayer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(
            List(left.size) { i ->
                LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(Fill(colorFor(left[i].color))))
            },
        ),
        verticalAxisPosition = Axis.Position.Vertical.Start,
    )

    val rightLayer = if (right.isNotEmpty()) {
        rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(
                List(right.size) { i ->
                    LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(Fill(colorFor(right[i].color))))
                },
            ),
            verticalAxisPosition = Axis.Position.Vertical.End,
        )
    } else null

    val leftUnit = left.firstOrNull()?.unit.orEmpty()
    val rightUnit = right.firstOrNull()?.unit.orEmpty()
    val axisTitleStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
    val timeFormatter = remember(tw) { timeFormatterFor(tw) }

    CartesianChartHost(
        modifier = Modifier.fillMaxWidth().height(280.dp),
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
                valueFormatter = { _, value, _ ->
                    timeFormatter.format(Instant.ofEpochMilli(value.toLong()))
                },
                labelRotationDegrees = -20f,
            ),
        ),
        modelProducer = modelProducer,
    )
}