package com.example.snustracker

import android.content.Context
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/* =====================================================================
   DATENMODELL
   Ein Tag wird unter seinem ISO-Datum gespeichert, z. B. "2026-11-10".
   ===================================================================== */

/** Ein Tag: wie viele Snus und was sie gekostet haben. */
data class DayEntry(val n: Int, val cost: Double)

/** Der komplette Zustand der App. Unveränderlich: Änderungen erzeugen eine Kopie. */
data class AppState(
    val pouches: Int? = null,            // Snus pro Dose
    val price: Double? = null,           // Preis pro Dose in Euro
    val days: Map<String, DayEntry> = emptyMap(),
    val inCan: Int = 0,                  // wie viele Snus in der aktuellen Dose schon benutzt wurden
    val done: Int = 0,                   // wie viele Dosen komplett aufgebraucht sind
) {
    /** Sind Dosengröße und Preis eingetragen? Erst dann kann gezählt werden. */
    val ready: Boolean get() = (pouches ?: 0) > 0 && (price ?: 0.0) > 0.0

    /** Preis pro einzelnem Snus. */
    val unitPrice: Double get() = if (ready) price!! / pouches!! else 0.0
}

/* =====================================================================
   LOGIK (reine Funktionen, leicht zu testen)
   ===================================================================== */

/** +1 Snus für einen Tag. Erhöht auch den Dosen-Zähler, wenn die Dose leer wird. */
fun AppState.addSnus(day: LocalDate): AppState {
    if (!ready) return this
    val key = day.toString()
    val old = days[key] ?: DayEntry(0, 0.0)
    val newDays = days + (key to DayEntry(old.n + 1, old.cost + unitPrice))

    var newInCan = inCan + 1
    var newDone = done
    if (newInCan >= pouches!!) {   // Dose ist leer -> Zähler hoch, neue Dose beginnt
        newDone += 1
        newInCan = 0
    }
    return copy(days = newDays, inCan = newInCan, done = newDone)
}

/** −1 Snus für einen Tag (macht ein +1 rückgängig). */
fun AppState.removeSnus(day: LocalDate): AppState {
    val key = day.toString()
    val old = days[key] ?: return this
    if (old.n < 1) return this

    // Wir ziehen den Durchschnittspreis dieses Tages ab, falls sich der Preis mal geändert hat
    val avg = old.cost / old.n
    val newDays = if (old.n == 1) days - key
    else days + (key to DayEntry(old.n - 1, max(0.0, old.cost - avg)))

    return when {
        inCan > 0 -> copy(days = newDays, inCan = inCan - 1)
        done > 0 -> copy(days = newDays, done = done - 1, inCan = max(0, (pouches ?: 1) - 1))
        else -> copy(days = newDays)
    }
}

fun AppState.withPouches(value: Int?): AppState {
    // Wenn die Dose kleiner wird, darf der Füllstand nicht darüber hinausgehen
    val newInCan = if (value != null && inCan >= value) value - 1 else inCan
    return copy(pouches = value, inCan = max(0, newInCan))
}

fun AppState.withPrice(value: Double?): AppState = copy(price = value)

/** Anzahl Snus an einem Tag (0, wenn nichts eingetragen). */
fun AppState.countOn(day: LocalDate): Int = days[day.toString()]?.n ?: 0

/** Kosten an einem Tag. */
fun AppState.costOn(day: LocalDate): Double = days[day.toString()]?.cost ?: 0.0

/** Erster Tag mit Eintrag, oder null. */
fun AppState.firstDay(): LocalDate? = days.keys.minOrNull()?.let { LocalDate.parse(it) }

/** Grundlage der Hochrechnung: Durchschnitt der letzten bis zu 14 Tage. */
data class Basis(val windowDays: Int, val avgPerDay: Double)

fun AppState.basis(today: LocalDate): Basis? {
    val first = firstDay() ?: return null
    val span = (ChronoUnit.DAYS.between(first, today) + 1).toInt()
    val w = max(1, min(14, span))
    var sum = 0
    for (i in 0 until w) sum += countOn(today.minusDays(i.toLong()))
    return Basis(w, sum.toDouble() / w)
}

/* =====================================================================
   SPEICHERN (SharedPreferences, Inhalt als JSON)
   Das Format ist dasselbe wie bei der Web-Version. Eine Sicherung von dort
   lässt sich also direkt in die App einfügen.
   ===================================================================== */

object Storage {
    private const val PREFS = "snus_tracker"
    private const val KEY = "state_v1"

    fun load(context: Context): AppState {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return AppState()
        return try { parse(raw) } catch (e: Exception) { AppState() }
    }

    fun save(context: Context, state: AppState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, toJson(state)).apply()
    }

    fun toJson(state: AppState): String {
        val settings = JSONObject()
            .put("pouches", state.pouches ?: JSONObject.NULL)
            .put("price", state.price ?: JSONObject.NULL)
        val days = JSONObject()
        state.days.forEach { (k, v) ->
            days.put(k, JSONObject().put("n", v.n).put("c", v.cost))
        }
        val can = JSONObject().put("inCan", state.inCan).put("done", state.done)
        return JSONObject()
            .put("v", 1)
            .put("settings", settings)
            .put("days", days)
            .put("can", can)
            .toString()
    }

    /** Liest JSON ein. Wirft eine Exception, wenn es keine gültige Sicherung ist. */
    fun parse(raw: String): AppState {
        val root = JSONObject(raw.trim())
        if (!root.has("days")) throw IllegalArgumentException("keine Sicherung")

        val settings = root.optJSONObject("settings")
        val pouches = settings?.optInt("pouches", 0)?.takeIf { it > 0 }
        val price = settings?.optDouble("price", 0.0)?.takeIf { it > 0.0 && !it.isNaN() }

        val dayMap = mutableMapOf<String, DayEntry>()
        val daysJson = root.getJSONObject("days")
        val dateRegex = Regex("""\d{4}-\d{2}-\d{2}""")
        for (k in daysJson.keys()) {
            if (!dateRegex.matches(k)) continue
            val d = daysJson.optJSONObject(k) ?: continue
            val n = d.optInt("n", 0)
            val c = d.optDouble("c", 0.0)
            if (n > 0) dayMap[k] = DayEntry(n, if (c.isNaN() || c < 0) 0.0 else c)
        }

        val can = root.optJSONObject("can")
        return AppState(
            pouches = pouches,
            price = price,
            days = dayMap,
            inCan = max(0, can?.optInt("inCan", 0) ?: 0),
            done = max(0, can?.optInt("done", 0) ?: 0),
        )
    }
}

/* =====================================================================
   ZAHLEN FORMATIEREN (deutsch: 1,85 €)
   ===================================================================== */

private val moneyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY)
private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.GERMANY).apply {
    maximumFractionDigits = 1
}

fun money(v: Double): String = moneyFormat.format(v)
fun num1(v: Double): String = numberFormat.format(v)
