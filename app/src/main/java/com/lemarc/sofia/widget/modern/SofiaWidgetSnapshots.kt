package com.lemarc.sofia.widget.modern

import android.content.Context
import com.lemarc.sofia.data.model.GraphPoint
import java.io.File
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

/**
 * Dernier jeu de données récupéré avec succès par un widget. Si un rafraîchissement échoue (hors-ligne,
 * appli jamais ouverte donc cache Room vide…), le widget réaffiche ces données au lieu de se vider.
 */
object SofiaWidgetSnapshots {

    private fun file(context: Context, name: String) = File(context.filesDir, "widget_$name.json")

    // ── Live / graphique ─────────────────────────────────────────────────────
    fun saveLive(context: Context, name: String, data: LiveData) {
        try {
            val json = JSONObject()
                .put("testMode", data.testMode)
                .put("from", data.from.toEpochMilli())
                .put("to", data.to.toEpochMilli())
                .put("committed", points(data.committed))
                .put("actual", points(data.actual))
            file(context, name).writeText(json.toString())
        } catch (_: Exception) {
        }
    }

    fun loadLive(context: Context, name: String): LiveData? = try {
        val f = file(context, name)
        if (!f.exists()) null else {
            val o = JSONObject(f.readText())
            LiveData(
                testMode = o.getBoolean("testMode"),
                committed = readPoints(o.getJSONArray("committed")),
                actual = readPoints(o.getJSONArray("actual")),
                from = Instant.ofEpochMilli(o.getLong("from")),
                to = Instant.ofEpochMilli(o.getLong("to")),
                fromCache = true,
            )
        }
    } catch (_: Exception) {
        null
    }

    // ── Disponibilité par unité ──────────────────────────────────────────────
    fun saveUnits(context: Context, data: UnitsData) {
        try {
            val units = JSONArray()
            data.units.forEach { u ->
                units.put(
                    JSONObject().put("bmu", u.bmuId).put("notice", u.hasNotice).apply {
                        u.normalMw?.let { put("normal", it) }
                        u.availableMw?.let { put("available", it) }
                        u.cause?.let { put("cause", it) }
                    },
                )
            }
            val json = JSONObject()
                .put("testMode", data.testMode)
                .put("at", data.fetchedAt.toEpochMilli())
                .put("units", units)
            file(context, "units").writeText(json.toString())
        } catch (_: Exception) {
        }
    }

    fun loadUnits(context: Context): UnitsData? = try {
        val f = file(context, "units")
        if (!f.exists()) null else {
            val o = JSONObject(f.readText())
            val arr = o.getJSONArray("units")
            UnitsData(
                testMode = o.getBoolean("testMode"),
                units = List(arr.length()) { i ->
                    val u = arr.getJSONObject(i)
                    UnitStatus(
                        bmuId = u.getString("bmu"),
                        normalMw = if (u.has("normal")) u.getDouble("normal") else null,
                        availableMw = if (u.has("available")) u.getDouble("available") else null,
                        hasNotice = u.getBoolean("notice"),
                        cause = if (u.has("cause")) u.getString("cause") else null,
                    )
                },
                fetchedAt = Instant.ofEpochMilli(o.getLong("at")),
                fromCache = true,
            )
        }
    } catch (_: Exception) {
        null
    }

    // ── Points ───────────────────────────────────────────────────────────────
    private fun points(list: List<GraphPoint>): JSONArray {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONArray().put(it.timeFrom.toEpochMilli()).put(it.timeTo.toEpochMilli()).put(it.quantity))
        }
        return arr
    }

    private fun readPoints(arr: JSONArray): List<GraphPoint> = List(arr.length()) { i ->
        val p = arr.getJSONArray(i)
        GraphPoint(
            id = "SNAPSHOT",
            timeFrom = Instant.ofEpochMilli(p.getLong(0)),
            timeTo = Instant.ofEpochMilli(p.getLong(1)),
            quantity = p.getDouble(2),
        )
    }
}