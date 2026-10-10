package com.lemarc.sofia.widget.modern

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.lemarc.sofia.MainActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * ColorProvider(Color) est restreint (RestrictedApi) dans Glance ; la variante publique est
 * androidx.glance.color.ColorProvider(day, night). Même couleur jour/nuit = couleur fixe.
 */
fun solid(color: Color) = androidx.glance.color.ColorProvider(day = color, night = color)

/** Palette sombre fixe, alignée sur le thème de l'appli. */
object WidgetPalette {
    val Surface = Color(0xFF141B2B)
    val Tile = Color(0xFF1F2A3F)
    val OnSurface = Color.White
    val Muted = Color(0xFF9FB0C8)
    val Track = Color(0xFF2C3B52)
    val Committed = Color(0xFF1E88E5)
    val Actual = Color(0xFFFF9800)
    val Good = Color(0xFF4CAF50)
    val Warn = Color(0xFFFFB300)
    val Bad = Color(0xFFEF5350)
}

fun UnitStatus.statusColor(): Color = when {
    fraction >= 0.999f -> WidgetPalette.Good
    fraction > 0f -> WidgetPalette.Warn
    else -> WidgetPalette.Bad
}

fun formatClock(instant: Instant): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(instant)

/** Carte arrondie plein widget ; un tap ouvre l'appli. */
@Composable
fun WidgetRoot(content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(solid(WidgetPalette.Surface))
            .cornerRadius(22.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) { content() }
}

@Composable
fun WText(
    text: String,
    size: Int,
    color: Color = WidgetPalette.OnSurface,
    bold: Boolean = false,
    modifier: GlanceModifier = GlanceModifier,
    align: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        maxLines = 1,
        style = TextStyle(
            color = solid(color),
            fontSize = size.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            textAlign = align,
        ),
    )
}

@Composable
fun Dot(color: Color, sizeDp: Int = 8) {
    Box(
        modifier = GlanceModifier
            .size(sizeDp.dp)
            .background(solid(color))
            .cornerRadius((sizeDp / 2).dp),
    ) {}
}