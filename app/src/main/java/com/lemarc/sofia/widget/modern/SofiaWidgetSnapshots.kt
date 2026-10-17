package com.lemarc.sofia.widget.modern

import android.content.Context
import com.lemarc.sofia.data.model.GraphPoint
import com.lemarc.sofia.data.model.UouDay
import java.io.File
import java.time.Instant
import java.time.LocalDate
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

    // ── 14 prochains jours ───────────────────────────────────────────────────
    fun saveNextDays(context: Context, data: NextDaysData) {
        try {
            val days = JSONArray()
            data.days.forEach { days.put(JSONArray().put(it.date.toEpochDay()).put(it.outputUsableMw)) }
            val json = JSONObject()
                .put("at", data.fetchedAt.toEpochMilli())
                .put("days", days)
            file(context, "next_days").writeText(json.toString())
        } catch (_: Exception) {
        }
    }

    fun loadNextDays(context: Context): NextDaysData? = try {
        val f = file(context, "next_days")
        if (!f.exists()) null else {
            val o = JSONObject(f.readText())
            val arr = o.getJSONArray("days")
            NextDaysData(
                days = List(arr.length()) { i ->
                    val d = arr.getJSONArray(i)
                    UouDay(LocalDate.ofEpochDay(d.getLong(0)), d.getDouble(1))
                },
                fetchedAt = Instant.ofEpochMilli(o.getLong("at")),
                fromCache = true,
            )
        }
    } catch (_: Exception) {
        null
    }

    // ── Part de l'éolien GB ──────────────────────────────────────────────────
    fun saveGbShare(context: Context, data: GbShareData) {
        try {
            val json = JSONObject()
                .put("at", data.fetchedAt.toEpochMilli())
                .apply {
                    data.timeFrom?.let { put("timeFrom", it.toEpochMilli()) }
                    data.sofiaMw?.let { put("sofia", it) }
                    data.offshoreMw?.let { put("offshore", it) }
                    data.onshoreMw?.let { put("onshore", it) }
                }
            file(context, "gb_share").writeText(json.toString())
        } catch (_: Exception) {
        }
    }

    fun loadGbShare(context: Context): GbShareData? = try {
        val f = file(context, "gb_share")
        if (!f.exists()) null else {
            val o = JSONObject(f.readText())
            GbShareData(
                timeFrom = if (o.has("timeFrom")) Instant.ofEpochMilli(o.getLong("timeFrom")) else null,
                sofiaMw = if (o.has("sofia")) o.getDouble("sofia") else null,
                offshoreMw = if (o.has("offshore")) o.getDouble("offshore") else null,
                onshoreMw = if (o.has("onshore")) o.getDouble("onshore") else null,
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