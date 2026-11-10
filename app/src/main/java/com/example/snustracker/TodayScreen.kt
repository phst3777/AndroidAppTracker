package com.example.snustracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    state: AppState,
    today: LocalDate,
    onAdd: (LocalDate) -> Unit,
    onRemove: (LocalDate) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    // 0 = heute, 1 = gestern (so kann man vergessene Snus nachtragen)
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val day = if (selected == 0) today else today.minusDays(1)
    val count = state.countOn(day)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Hinweis, solange Dosengröße/Preis fehlen
        if (!state.ready) {
            AppCard(borderColor = c.primary) {
                Text("Trag zuerst ein, wie viele Snus in einer Dose sind und was eine Dose kostet.")
                Spacer(Modifier.height(12.dp))
                PillButton("Einstellungen öffnen", onClick = onOpenSettings)
            }
            Spacer(Modifier.height(16.dp))
        }

        Segment(listOf("Heute", "Gestern"), selected) { selected = it }

        // Große Zahl im Dot-Matrix-Look
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = count.toString(),
                fontFamily = Doto,
                fontWeight = FontWeight.Black,
                fontSize = 150.sp,
                lineHeight = 150.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (selected == 0) "Snus heute" else "Snus gestern",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = money(state.costOn(day)) + if (selected == 0) " heute" else " gestern",
                fontSize = 15.sp,
                color = c.onSurfaceVariant,
            )
        }

        // +1 Knopf und Rückgängig
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(if (state.ready) c.primary else c.surfaceVariant)
                    .clickable(enabled = state.ready) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAdd(day)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+1",
                    fontFamily = Doto,
                    fontWeight = FontWeight.Black,
                    fontSize = 44.sp,
                    color = if (state.ready) c.onPrimary else c.onSurfaceVariant,
                )
            }
            PillButton(
                text = "−1 rückgängig",
                style = PillStyle.Outline,
                onClick = { if (count > 0) onRemove(day) },
            )
        }

        // Aktuelle Dose als Punktereihe
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Aktuelle Dose", fontWeight = FontWeight.SemiBold)
                val p = state.pouches
                Text(
                    text = if (p != null) "${state.inCan} von $p" else "–",
                    color = c.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
            val pouches = state.pouches
            if (pouches == null) {
                Spacer(Modifier.height(8.dp))
                Text("Die Dosengröße fehlt noch.", color = c.onSurfaceVariant, fontSize = 14.sp)
            } else {
                val used = minOf(state.inCan, pouches)
                Spacer(Modifier.height(14.dp))
                if (pouches <= 60) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        repeat(pouches) { i ->
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (i < used) c.onBackground else c.surfaceVariant)
                            )
                        }
                    }
                } else {
                    // Sehr große Dosen: statt Punkten ein Balken
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(c.surfaceVariant)
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(used.toFloat() / pouches)
                                .background(c.onBackground)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Noch ${pouches - used} Snus, dann zählt die Dose als aufgebraucht.",
                    color = c.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Zähler für aufgebrauchte Dosen
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Dosen aufgebraucht", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Zählt hoch, sobald eine Dose leer ist.",
                        color = c.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
                Text(
                    text = state.done.toString(),
                    fontFamily = Doto,
                    fontWeight = FontWeight.Black,
                    fontSize = 56.sp,
                )
            }
        }
    }
}
