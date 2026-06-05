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
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.data.model.GraphPoint
import com.lemarc.sofia.ui.shortAxisFormatter
import kotlin.math.max
import kotlin.math.roundToInt


@Composable
fun ProductionChart(points: List<GraphPoint>, allowNegative: Boolean, unit: String, tw: TimeWindow) {

    val axisColor = MaterialTheme.colorScheme.onSurface.toArgb()

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false

                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)

                setNoDataText("No production data available")

                axisRight.isEnabled = false

                axisLeft.apply {
                    axisMinimum = if (allowNegative) {
                        points.minOf { it.quantity }.toFloat()
                    } else {
                        0f
                    }
                    textColor = axisColor
                }

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    labelRotationAngle = -30f

                    textColor = axisColor
                }

                setViewPortOffsets(70f, 20f, 28f, 90f)
            }
        },
        update = { chart ->
            val entries = points.mapIndexed { index, point ->
                Entry(index.toFloat(), point.quantity.toFloat())
            }

            val dataSet = LineDataSet(entries, "Production").apply {
                color = android.graphics.Color.rgb(30, 136, 229)
                lineWidth = 2.5f
                setDrawCircles(false)
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = android.graphics.Color.rgb(77, 208, 225)
                fillAlpha = 48
                mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            }

            chart.data = LineData(dataSet)

            chart.axisLeft.axisMaximum =
                (max(points.maxOfOrNull { it.quantity } ?: 0.0, 10.0) * 1.15).toFloat()

            chart.xAxis.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(
                    value: Float,
                    axis: AxisBase?,
                ): String {
                    val index = value.roundToInt().coerceIn(points.indices)
                    return shortAxisFormatter(tw).format(points[index].timeFrom)
                }
            }
            chart.axisLeft.apply {
                axisMinimum = if (allowNegative) {
                    points.minOf { it.quantity }.toFloat()
                } else {
                    0f
                }
                textColor = axisColor
            }
            chart.axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(
                    value: Float,
                    axis: AxisBase?
                ): String {
                    return if (value % 1f == 0f) {
                        "${value.toInt()} $unit"
                    } else {
                        "$value $unit"
                    }
                }
            }
            chart.invalidate()
        },
    )
}