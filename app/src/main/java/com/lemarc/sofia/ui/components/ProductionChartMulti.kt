package com.lemarc.sofia.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.data.model.GraphPoint
import com.lemarc.sofia.ui.shortAxisFormatter
import kotlin.math.roundToInt

data class ChartSeries(
    val points: List<GraphPoint>,
    val allowNegative: Boolean,
    val unit: String,
    val label: String = "",
)

@Composable
fun ProductionChartMulti(
    left: List<ChartSeries>,
    right: List<ChartSeries> = emptyList(),
    tw: TimeWindow
) {
    val axisColor = MaterialTheme.colorScheme.onSurface.toArgb()

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = true

                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)

                setNoDataText("No data available")

                axisRight.isEnabled = right.isNotEmpty()

                axisLeft.apply {
                    textColor = axisColor
                }
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    labelRotationAngle = -30f
                    textColor = axisColor
                }
                setViewPortOffsets(70f, 20f, 48f, 90f)
            }
        },
        update = { chart ->
            val dataSets = mutableListOf<LineDataSet>()

            fun colorForLabel(label: String, index: Int): Int {
                return when (label) {
                    "PN" -> android.graphics.Color.rgb(30, 136, 229) // blue
                    "B1610" -> android.graphics.Color.rgb(255, 152, 0) // amber
                    "Weather" -> android.graphics.Color.rgb(76, 175, 80) // green
                    else -> {
                        // fallback palette
                        val palette = listOf(
                            android.graphics.Color.rgb(3, 169, 244), // light blue
                            android.graphics.Color.rgb(0, 150, 136), // teal
                            android.graphics.Color.rgb(244, 67, 54), // red
                            android.graphics.Color.rgb(156, 39, 176), // purple
                            android.graphics.Color.rgb(63, 81, 181), // indigo
                        )
                        palette[index % palette.size]
                    }
                }
            }

            // LEFT AXIS SERIES
            if (left.isNotEmpty()) {
                left.forEachIndexed { idx, series ->
                    val entries = series.points.mapIndexed { index, point ->
                        Entry(index.toFloat(), point.quantity.toFloat())
                    }
                    val ds = LineDataSet(entries, series.label.ifEmpty { "Series L${idx + 1}" }).apply {
                        color = colorForLabel(series.label, idx)
                        lineWidth = 2.2f
                        setDrawCircles(false)
                        setDrawValues(false)
                        setDrawFilled(idx == 0) // fill only first to reduce clutter
                        fillColor = android.graphics.Color.argb(64, 33, 150, 243)
                        fillAlpha = 48
                        mode = LineDataSet.Mode.HORIZONTAL_BEZIER
                        axisDependency = YAxis.AxisDependency.LEFT
                    }
                    dataSets += ds
                }

                val allPoints = left.flatMap { it.points }
                chart.axisLeft.apply {
                    axisMinimum = if (allPoints.isNotEmpty()) {
                        minOf(0f, allPoints.minOf { it.quantity }.toFloat()) -2f
                    } else {
                        0f
                    }
                    textColor = axisColor
                }
                val leftUnit = left.first().unit
                chart.axisLeft.valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String =
                        if (value % 1f == 0f) "${value.toInt()} $leftUnit" else "$value $leftUnit"
                }
            }

            // RIGHT AXIS SERIES
            if (right.isNotEmpty()) {
                chart.axisRight.isEnabled = true
                right.forEachIndexed { idx, series ->
                    val entries = series.points.mapIndexed { index, point ->
                        Entry(index.toFloat(), point.quantity.toFloat())
                    }
                    val ds = LineDataSet(entries, series.label.ifEmpty { "Series R${idx + 1}" }).apply {
                        color = colorForLabel(series.label, idx)
                        lineWidth = 2.2f
                        setDrawCircles(false)
                        setDrawValues(false)
                        setDrawFilled(false)
                        mode = LineDataSet.Mode.HORIZONTAL_BEZIER
                        axisDependency = YAxis.AxisDependency.RIGHT
                    }
                    dataSets += ds
                }

                val allPoints = right.flatMap { it.points }
                chart.axisRight.apply {
                    isEnabled = true
                    axisMinimum = if (allPoints.isNotEmpty()) {
                        minOf(0f, allPoints.minOf { it.quantity }.toFloat()) - 2f
                    } else {
                        0f
                    }
                    textColor = axisColor
                }
                val rightUnit = right.first().unit
                chart.axisRight.valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String =
                        if (value % 1f == 0f) "${value.toInt()} $rightUnit" else "$value $rightUnit"
                }
            } else {
                chart.axisRight.isEnabled = false
            }

            chart.data = LineData(dataSets as List<LineDataSet>)

            val referencePoints = when {
                left.isNotEmpty() -> left.first().points
                right.isNotEmpty() -> right.first().points
                else -> emptyList()
            }
            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    val index = value.roundToInt().coerceIn(referencePoints.indices)
                    return if (referencePoints.isNotEmpty()) shortAxisFormatter(tw).format(referencePoints[index].timeFrom) else ""
                }
            }
            chart.invalidate()
        },
    )
}
