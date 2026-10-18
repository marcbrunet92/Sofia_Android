package com.lemarc.sofia.widget.modern

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.lemarc.sofia.R
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.ui.theme.Sofia_AndroidTheme
import java.time.Duration
import kotlinx.coroutines.launch

/** Les données réelles (B1610) ne sont affichées qu'à partir de 7 jours (« All » inclus). */
private val ActualMinWindow: Duration = Duration.ofDays(7)

fun TimeWindow.showsActual(): Boolean {
    val d = duration
    return d == null || d >= ActualMinWindow
}

@StringRes
fun TimeWindow.longLabelRes(): Int = when (this) {
    TimeWindow.HOURS_24 -> R.string.time_window_long_24h
    TimeWindow.DAYS_7 -> R.string.time_window_long_7d
    TimeWindow.MONTH_1 -> R.string.time_window_long_1m
    TimeWindow.ALL -> R.string.time_window_long_all
}

@StringRes
fun TimeWindow.shortTitleRes(): Int = when (this) {
    TimeWindow.HOURS_24 -> R.string.time_window_short_24h
    TimeWindow.DAYS_7 -> R.string.time_window_short_7d
    TimeWindow.MONTH_1 -> R.string.time_window_short_1m
    TimeWindow.ALL -> R.string.time_window_short_all
}

fun TimeWindow.longLabel(context: Context): String = context.getString(longLabelRes())

fun TimeWindow.shortTitle(context: Context): String = context.getString(shortTitleRes())

/** Fenêtre choisie, stockée dans l'état Glance de chaque instance de widget. */
object GraphWidgetPrefs {
    val WINDOW_KEY = stringPreferencesKey("graph_window")

    fun windowOf(prefs: Preferences): TimeWindow =
        TimeWindow.entries.firstOrNull { it.name == prefs[WINDOW_KEY] } ?: TimeWindow.HOURS_24
}

/**
 * Lancée par le launcher quand on dépose le widget (et via « reconfigurer » en appui long).
 * Tant qu'on n'a pas renvoyé RESULT_OK, le widget n'est pas ajouté.
 */
class SofiaGraphConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appWidgetId = intent?.extras
            ?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Par défaut : annulé (retour arrière = pas de widget)
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val initial = TimeWindow.HOURS_24
        setContent {
            Sofia_AndroidTheme {
                var busy by remember { mutableStateOf(false) }
                ConfigScreen(
                    initial = initial,
                    busy = busy,
                    onConfirm = { window ->
                        busy = true
                        confirm(window)
                    },
                )
            }
        }
    }

    private fun confirm(window: TimeWindow) {
        lifecycleScope.launch {
            // 1) on enregistre le choix dans l'état Glance du widget, 2) on déclenche la mise à jour.
            // (Avec une activité de configuration, le système n'envoie pas la 1re mise à jour.)
            runCatching {
                val glanceId = GlanceAppWidgetManager(this@SofiaGraphConfigActivity).getGlanceIdBy(appWidgetId)
                updateAppWidgetState(this@SofiaGraphConfigActivity, glanceId) { prefs ->
                    prefs[GraphWidgetPrefs.WINDOW_KEY] = window.name
                }
                SofiaGraphGlanceWidget().update(this@SofiaGraphConfigActivity, glanceId)
            }
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}

@Composable
private fun ConfigScreen(
    initial: TimeWindow,
    busy: Boolean,
    onConfirm: (TimeWindow) -> Unit,
) {
    var selected by remember { mutableStateOf(initial) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.widget_config_time_window_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.widget_config_time_window_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            TimeWindow.entries.forEach { window ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = window == selected,
                            onClick = { selected = window },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = window == selected, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(window.longLabelRes()), style = MaterialTheme.typography.bodyLarge)
                }
            }

            Text(
                text = if (selected.showsActual()) {
                    stringResource(R.string.widget_config_actual_included)
                } else {
                    stringResource(R.string.widget_config_actual_excluded)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            )

            Button(
                onClick = { onConfirm(selected) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (busy) stringResource(R.string.widget_config_adding) else stringResource(R.string.widget_config_validate))
            }
        }
    }
}