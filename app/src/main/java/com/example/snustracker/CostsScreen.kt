package com.example.snustracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun CostsScreen(state: AppState, today: LocalDate) {
    val c = MaterialTheme.colorScheme
    val basis = state.basis(today)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // ---------- Hochrechnung ----------
        AppCard {
            Text("Hochrechnung bei aktuellem Konsum", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))

            when {
                basis == null -> Text(
                    "Sobald du Einträge hast, rechne ich hier hoch.",
                    color = c.onSurfaceVariant, fontSize = 14.sp,
                )

                !state.ready -> Text(
                    "Dosengröße und Preis fehlen noch. Trag sie unter Einstellungen ein.",
                    color = c.onSurfaceVariant, fontSize = 14.sp,
                )

                else -> {
                    val perDay = basis.avgPerDay * state.unitPrice
                    val cansPerDay = basis.avgPerDay / state.pouches!!
                    val rows = listOf(
                        "Pro Tag" to 1.0,
                        "Pro Woche" to 7.0,
                        "Pro Monat" to 30.4375,
                        "Pro Jahr" to 365.0,
                    )
                    rows.forEachIndexed { index, (label, factor) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label)
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(money(perDay * factor), fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${num1(cansPerDay * factor)} Dosen",
                                    color = c.onSurfaceVariant, fontSize = 14.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    val window = if (basis.windowDays >= 14) {
                        "den letzten 14 Tagen"
                    } else {
                        "den ${basis.windowDays} Tag${if (basis.windowDays == 1) "" else "en"} seit deinem ersten Eintrag"
                    }
                    Text(
                        "Basis: Ø ${num1(basis.avgPerDay)} Snus pro Tag in $window, " +
                            "${money(state.unitPrice)} pro Snus.",
                        color = c.onSurfaceVariant, fontSize = 14.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---------- Bisher ausgegeben ----------
        val spent = state.days.values.sumOf { it.cost }
        val pouchesTotal = state.days.values.sumOf { it.n }
        val first = state.firstDay()
        val daysTracked = if (first == null) 0 else ChronoUnit.DAYS.between(first, today) + 1

        AppCard {
            Text("Bisher ausgegeben", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                text = money(spent),
                fontFamily = Doto,
                fontWeight = FontWeight.Black,
                fontSize = 52.sp,
            )
            Spacer(Modifier.height(10.dp))
            InfoRow("Snus insgesamt", pouchesTotal.toString())
            InfoRow("Dosen aufgebraucht", state.done.toString())
            InfoRow("Tage erfasst", daysTracked.toString())
            InfoRow("Ø Preis pro Snus", if (pouchesTotal > 0) money(spent / pouchesTotal) else "–")
        }
    }
}
