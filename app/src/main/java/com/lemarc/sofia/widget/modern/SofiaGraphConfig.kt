package com.lemarc.sofia.widget.modern

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.lemarc.sofia.TimeWindow
import com.lemarc.sofia.ui.theme.Sofia_AndroidTheme
import kotlinx.coroutines.launch
import java.time.Duration

/** Les données réelles (B1610) ne sont affichées qu'à partir de 7 jours (« All » inclus). */
private val ActualMinWindow: Duration = Duration.ofDays(7)

fun TimeWindow.showsActual(): Boolean {
    val d = duration
    return d == null || d >= ActualMinWindow
}

fun TimeWindow.longLabel(): String = when (this) {
    TimeWindow.HOURS_6 -> "6 heures"
    TimeWindow.HOURS_24 -> "24 heures"
    TimeWindow.HOURS_48 -> "48 heures"
    TimeWindow.DAYS_7 -> "7 jours"
    TimeWindow.MONTH_1 -> "1 mois"
    TimeWindow.MONTH_3 -> "3 mois"
    TimeWindow.ALL -> "Tout l'historique"
}

fun TimeWindow.shortTitle(): String = when (this) {
    TimeWindow.HOURS_6 -> "6 H"
    TimeWindow.HOURS_24 -> "24 H"
    TimeWindow.HOURS_48 -> "48 H"
    TimeWindow.DAYS_7 -> "7 J"
    TimeWindow.MONTH_1 -> "1 MOIS"
    TimeWindow.MONTH_3 -> "3 MOIS"
    TimeWindow.ALL -> "TOUT"
}

/** Fenêtre choisie par instance de widget (clé = appWidgetId). */
object GraphWidgetPrefs {
    private const val FILE = "sofia_graph_widget"
    private fun key(appWidgetId: Int) = "window_$appWidgetId"

    fun getWindow(context: Context, appWidgetId: Int): TimeWindow {
        val name = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(key(appWidgetId), null)
        return TimeWindow.entries.firstOrNull { it.name == name } ?: TimeWindow.HOURS_24
    }

    fun setWindow(context: Context, appWidgetId: Int, window: TimeWindow) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit { putString(key(appWidgetId), window.name) }
    }

    fun clear(context: Context, appWidgetIds: IntArray) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
            appWidgetIds.forEach { remove(key(it)) }
        }
    }
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

        val initial = GraphWidgetPrefs.getWindow(this, appWidgetId)
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
        GraphWidgetPrefs.setWindow(this, appWidgetId, window)
        lifecycleScope.launch {
            // Avec une activité de configuration, le système n'envoie pas la 1re mise à jour : on la déclenche.
            runCatching {
                val glanceId = GlanceAppWidgetManager(this@SofiaGraphConfigActivity).getGlanceIdBy(appWidgetId)
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
                text = "Fenêtre de temps",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Période affichée par le widget graphique.",
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
                    Text(window.longLabel(), style = MaterialTheme.typography.bodyLarge)
                }
            }

            Text(
                text = if (selected.showsActual()) {
                    "La production réelle sera affichée avec la production engagée."
                } else {
                    "En dessous de 7 jours, seule la production engagée est affichée (pas de données réelles)."
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
                Text(if (busy) "Ajout en cours…" else "Valider")
            }
        }
    }
}