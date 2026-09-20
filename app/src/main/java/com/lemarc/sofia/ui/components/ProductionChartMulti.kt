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
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.data.model.GraphPoint
import com.lemarc.sofia.ui.shortAxisFormatter
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

data class ChartSeries(
    val points: List<GraphPoint>,
    val allowNegative: Boolean,
    val unit: String,
    val label: String = "",
)

private val palette = listOf(
    android.graphics.Color.rgb(30, 136, 229),  // bleu
    android.graphics.Color.rgb(255, 152, 0),   // ambre
    android.graphics.Color.rgb(76, 175, 80),   // vert
    android.graphics.Color.rgb(244, 67, 54),   // rouge
)

private fun colorFor(index: Int) = androidx.compose.ui.graphics.Color(palette[index % palette.size])

// x = timestamp en millisecondes epoch, pour que chaque quantity reste liée à son propre instant
private fun xOf(point: GraphPoint): Double = point.timeFrom.toEpochMilli().toDouble()

@Composable
fun ProductionChartMulti(
    left: List<ChartSeries>,
    right: List<ChartSeries> = emptyList(),
    tw: TimeWindow,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

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

    val leftLayer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(
            List(left.size) { i ->
                LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(Fill(colorFor(i))))
            },
        ),
        verticalAxisPosition = Axis.Position.Vertical.Start,
    )

    val rightLayer = if (right.isNotEmpty()) {
        rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(
                List(right.size) { i ->
                    LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(Fill(colorFor(left.size + i))))
                },
            ),
            verticalAxisPosition = Axis.Position.Vertical.End,
        )
    } else null

    val leftUnit = left.firstOrNull()?.unit.orEmpty()
    val rightUnit = right.firstOrNull()?.unit.orEmpty()

    CartesianChartHost(
        modifier = Modifier.fillMaxWidth().height(280.dp),
        chart = rememberCartesianChart(
            leftLayer,
            *listOfNotNull(rightLayer).toTypedArray(),
            startAxis = VerticalAxis.rememberStart(
                title = { leftUnit },
                titleComponent = rememberTextComponent(
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                    ),
                ),
                titlePosition = BaseAxis.TitlePosition.End,
                valueFormatter = { _, value, _ -> "${value.roundToInt()}" },
            ),
            endAxis = if (rightLayer != null) {
                VerticalAxis.rememberEnd(
                    title = { rightUnit },
                    titleComponent = rememberTextComponent(
                        style = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                        ),
                    ),
                    titlePosition = BaseAxis.TitlePosition.End,
                    valueFormatter = { _, value, _ -> "${value.roundToInt()}" },
                )
            } else null,
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = { _, value, _ ->
                    shortAxisFormatter(tw).format(Instant.ofEpochMilli(value.toLong()))
                },
            ),
        ),
        modelProducer = modelProducer,
    )
}