package com.example.snustracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import java.time.format.TextStyle as JavaTextStyle

private val RANGES = listOf(7, 14, 30, 90)
private val dateLabel = DateTimeFormatter.ofPattern("EEE, d. MMM", Locale.GERMAN)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(state: AppState, today: LocalDate) {
    val c = MaterialTheme.colorScheme

    var rangeIdx by rememberSaveable { mutableIntStateOf(0) }
    var metricIdx by rememberSaveable { mutableIntStateOf(0) }   // 0 = Stück, 1 = Euro
    var selectedBar by remember { mutableStateOf<Int?>(null) }

    val n = RANGES[rangeIdx]
    val isCount = metricIdx == 0
    val first = state.firstDay()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Segment(listOf("7 Tage", "14", "30", "90"), rangeIdx) {
                rangeIdx = it
                selectedBar = null
            }
            Segment(listOf("Stück", "Euro"), metricIdx) { metricIdx = it }
        }
        Spacer(Modifier.height(16.dp))

        if (first == null || first.isAfter(today)) {
            AppCard {
                Text(
                    "Noch keine Einträge. Tippe unter „Heute“ auf +1.",
                    color = c.onSurfaceVariant,
                )
            }
            return@Column
        }

        // Die letzten n Tage, ältester zuerst
        val dates = (n - 1 downTo 0).map { today.minusDays(it.toLong()) }
        val values = dates.map { d -> if (isCount) state.countOn(d).toDouble() else state.costOn(d) }

        // Statistik nur ab dem ersten Eintrag, sonst würden leere Tage vor dem Start den Schnitt drücken
        val trackedIdx = dates.indices.filter { !dates[it].isBefore(first) }
        val tracked = trackedIdx.map { values[it] }
        val avg = if (tracked.isEmpty()) 0.0 else tracked.sum() / tracked.size
        var hiIdx = trackedIdx.first()
        var loIdx = trackedIdx.first()
        for (i in trackedIdx) {
            if (values[i] >= values[hiIdx]) hiIdx = i
            if (values[i] <= values[loIdx]) loIdx = i
        }

        AppCard {
            val sel = selectedBar
            Text(
                text = if (sel == null) {
                    "Tippe auf einen Balken für Details."
                } else {
                    val d = dates[sel]
                    "${dateLabel.format(d)}: ${state.countOn(d)} Snus · ${money(state.costOn(d))}"
                },
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(8.dp))
            BarChart(
                values = values,
                dates = dates,
                avg = avg,
                isCount = isCount,
                selected = sel,
                onSelect = { selectedBar = it },
            )
        }

        Spacer(Modifier.height(14.dp))

        fun fmt(v: Double) = if (isCount) num1(v) else money(v)
        AppCard {
            InfoRow("Durchschnitt pro Tag", fmt(avg))
            InfoRow("Meiste an einem Tag", "${fmt(values[hiIdx])} · ${dateLabel.format(dates[hiIdx])}")
            InfoRow("Wenigste an einem Tag", "${fmt(values[loIdx])} · ${dateLabel.format(dates[loIdx])}")
        }
    }
}

/** Rundet auf einen „schönen“ Wert für die Achse auf (z. B. 7 -> 8, 13 -> 15). */
private fun niceCeil(v: Double): Double {
    if (v <= 0.0) return 1.0
    val p = 10.0.pow(floor(log10(v)))
    val m = v / p
    val steps = listOf(1.0, 1.5, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0)
    return (steps.firstOrNull { m <= it } ?: 10.0) * p
}

/** Text zeichnen: align = 0 mittig um x, 1 rechtsbündig bei x. y ist die Oberkante. */
private fun DrawScope.label(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    x: Float,
    y: Float,
    rightAligned: Boolean = false,
): TextLayoutResult {
    val r = measurer.measure(text, style)
    val left = if (rightAligned) x - r.size.width else x - r.size.width / 2f
    drawText(r, topLeft = Offset(left, y))
    return r
}

@Composable
private fun BarChart(
    values: List<Double>,
    dates: List<LocalDate>,
    avg: Double,
    isCount: Boolean,
    selected: Int?,
    onSelect: (Int?) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val n = values.size
    val mutedStyle = TextStyle(color = c.onSurfaceVariant, fontSize = 10.sp)
    val valueStyle = TextStyle(color = c.onBackground, fontSize = 10.sp)
    val avgStyle = TextStyle(color = c.primary, fontSize = 10.sp)
    val barColor = c.onBackground
    val todayColor = c.primary
    val lineColor = c.outline

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .pointerInput(n, selected) {
                // Antippen: herausfinden, welcher Balken getroffen wurde
                detectTapGestures { offset ->
                    val padL = 32.dp.toPx()
                    val padR = 4.dp.toPx()
                    val slot = (size.width - padL - padR) / n
                    val idx = ((offset.x - padL) / slot).toInt()
                    if (offset.x >= padL && idx in 0 until n) {
                        onSelect(if (selected == idx) null else idx)
                    }
                }
            },
    ) {
        val padL = 32.dp.toPx()
        val padR = 4.dp.toPx()
        val padT = 18.dp.toPx()
        val padB = 24.dp.toPx()
        val plotW = size.width - padL - padR
        val plotH = size.height - padT - padB
        val slot = plotW / n
        val barW = max(1.5.dp.toPx(), slot * 0.68f)

        val top = niceCeil(max(values.max(), if (isCount) 4.0 else 1.0))

        // Hilfslinien bei 0, 50 % und 100 %
        for (f in listOf(0f, 0.5f, 1f)) {
            val y = padT + plotH - plotH * f
            drawLine(lineColor, Offset(padL, y), Offset(size.width - padR, y), strokeWidth = 1.dp.toPx())
            val r = measurer.measure(num1(top * f), mutedStyle)
            drawText(r, topLeft = Offset(padL - 5.dp.toPx() - r.size.width, y - r.size.height / 2f))
        }

        // Balken, Werte und Beschriftung
        val labelStep = when {
            n <= 7 -> 1
            n <= 14 -> 2
            n <= 30 -> 5
            else -> 15
        }
        for (i in 0 until n) {
            val v = values[i]
            val h = (v / top * plotH).toFloat()
            val x = padL + slot * i + (slot - barW) / 2f
            val y = padT + plotH - h

            if (v > 0) {
                val base = if (i == n - 1) todayColor else barColor
                val color = if (selected != null && selected != i) base.copy(alpha = 0.35f) else base
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(1.5.dp.toPx()),
                )
                if (n <= 14) {
                    val text = if (isCount) v.toInt().toString() else num1(v)
                    val r = measurer.measure(text, valueStyle)
                    drawText(r, topLeft = Offset(x + barW / 2f - r.size.width / 2f, y - r.size.height - 1.dp.toPx()))
                }
            }

            if ((n - 1 - i) % labelStep == 0) {
                val d = dates[i]
                val text = if (n <= 7) {
                    d.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.GERMAN).replace(".", "")
                } else {
                    "${d.dayOfMonth}.${d.monthValue}."
                }
                label(measurer, text, mutedStyle, x + barW / 2f, size.height - padB + 6.dp.toPx())
            }
        }

        // Durchschnittslinie
        if (avg > 0) {
            val ya = padT + plotH - (avg / top * plotH).toFloat()
            drawLine(
                color = todayColor,
                start = Offset(padL, ya),
                end = Offset(size.width - padR, ya),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
            val r = measurer.measure("Ø ${num1(avg)}", avgStyle)
            drawText(r, topLeft = Offset(size.width - padR - r.size.width, ya - r.size.height - 1.dp.toPx()))
        }
    }
}
