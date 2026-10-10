package com.lemarc.sofia.widget.modern

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import androidx.compose.ui.graphics.toArgb
import com.lemarc.sofia.data.model.GraphPoint
import com.lemarc.sofia.ui.theme.SofiaBlue
import com.lemarc.sofia.ui.theme.SofiaCyan
import com.lemarc.sofia.ui.theme.SofiaIndigo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

data class ChartLine(
    val points: List<GraphPoint>,
    val color: Int,
    val filled: Boolean = false,
)

/** Glance n'a pas de Canvas : on dessine en Bitmap (android.graphics) puis on l'affiche avec Image. */
object SofiaWidgetCharts {

    private fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)

    /** Jauge 270° avec dégradé cyan → bleu → indigo, comme dans l'appli. */
    fun gauge(sizePx: Int, fraction: Float): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val stroke = sizePx * 0.12f
        val inset = stroke / 2f + 1f
        val oval = RectF(inset, inset, sizePx - inset, sizePx - inset)
        val center = sizePx / 2f

        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            color = 0x2EFFFFFF
        }
        canvas.drawArc(oval, 135f, 270f, false, track)

        val sweep = 270f * fraction.coerceIn(0f, 1f)
        if (sweep > 0f) {
            val progress = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
                shader = SweepGradient(
                    center, center,
                    intArrayOf(SofiaCyan.toArgb(), SofiaBlue.toArgb(), SofiaIndigo.toArgb(), SofiaIndigo.toArgb()),
                    floatArrayOf(0f, 0.35f, 0.75f, 1f),
                )
            }
            canvas.save()
            canvas.rotate(135f, center, center) // le dégradé démarre au début de l'arc
            canvas.drawArc(oval, 0f, sweep, false, progress)
            canvas.restore()
        }
        return bitmap
    }

    /** Courbes temporelles (MW) sur [from, to], grille, libellés Y/X et point sur la dernière valeur. */
    fun lineChart(
        widthPx: Int,
        heightPx: Int,
        density: Float,
        lines: List<ChartLine>,
        from: Instant,
        to: Instant,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val plot = RectF(34f * density, 8f * density, widthPx - 8f * density, heightPx - 20f * density)

        val fromMs = from.toEpochMilli()
        val spanMs = (to.toEpochMilli() - fromMs).coerceAtLeast(1L).toFloat()
        val maxValue = lines.flatMap { it.points }.maxOfOrNull { it.quantity } ?: 0.0
        val yMax = (ceil(maxValue * 1.05 / 100.0) * 100.0).coerceAtLeast(100.0)

        fun xOf(t: Instant): Float = plot.left + ((t.toEpochMilli() - fromMs) / spanMs).coerceIn(0f, 1f) * plot.width()
        fun yOf(v: Double): Float = plot.bottom - (v / yMax).toFloat().coerceIn(0f, 1f) * plot.height()

        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = density
            color = 0x1FFFFFFF
        }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF9FB0C8.toInt()
            textSize = 10f * density
        }

        // Grille + axe Y (0, milieu, max)
        for (i in 0..2) {
            val v = yMax * i / 2.0
            val y = yOf(v)
            canvas.drawLine(plot.left, y, plot.right, y, grid)
            label.textAlign = Paint.Align.RIGHT
            canvas.drawText(v.toInt().toString(), plot.left - 6f * density, y + label.textSize / 3f, label)
        }

        // Axe X : 5 repères horaires
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
        val ticks = 4
        for (i in 0..ticks) {
            val t = Instant.ofEpochMilli(fromMs + (spanMs * i / ticks).toLong())
            label.textAlign = when (i) {
                0 -> Paint.Align.LEFT
                ticks -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            val x = when (i) {
                0 -> plot.left
                ticks -> plot.right
                else -> plot.left + plot.width() * i / ticks
            }
            canvas.drawText(timeFmt.format(t), x, heightPx - 4f * density, label)
        }

        // Courbes
        lines.forEach { line ->
            val pts = line.points.filter { it.timeFrom >= from }
            if (pts.isEmpty()) return@forEach

            val path = Path()
            pts.forEachIndexed { i, p ->
                val x = xOf(p.timeFrom)
                val y = yOf(p.quantity)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            if (line.filled) {
                val area = Path(path).apply {
                    lineTo(xOf(pts.last().timeFrom), plot.bottom)
                    lineTo(xOf(pts.first().timeFrom), plot.bottom)
                    close()
                }
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    shader = LinearGradient(
                        0f, plot.top, 0f, plot.bottom,
                        withAlpha(line.color, 0x66), withAlpha(line.color, 0x00),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawPath(area, fill)
            }

            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.2f * density
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                color = line.color
            }
            canvas.drawPath(path, stroke)

            val last = pts.last()
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = line.color }
            canvas.drawCircle(xOf(last.timeFrom), yOf(last.quantity), 3.5f * density, dot)
        }
        return bitmap
    }
}
