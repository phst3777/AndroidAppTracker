package com.example.snustracker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: TrackerViewModel) {
    val c = MaterialTheme.colorScheme
    val state = vm.state
    val context = LocalContext.current

    // Die Textfelder halten ihren eigenen Text, damit man z. B. "5," tippen kann.
    // Nach Wiederherstellen/Löschen (vm.version ändert sich) werden sie neu befüllt.
    var pouchText by remember(vm.version) { mutableStateOf(state.pouches?.toString() ?: "") }
    var priceText by remember(vm.version) {
        mutableStateOf(state.price?.toString()?.replace('.', ',') ?: "")
    }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = c.primary,
        unfocusedBorderColor = c.outline,
        cursorColor = c.primary,
        focusedTextColor = c.onBackground,
        unfocusedTextColor = c.onBackground,
        focusedLabelColor = c.primary,
        unfocusedLabelColor = c.onSurfaceVariant,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // ---------- Dose ----------
        AppCard {
            OutlinedTextField(
                value = pouchText,
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() }.take(3)
                    pouchText = clean
                    vm.setPouches(clean.toIntOrNull()?.takeIf { it > 0 })
                },
                label = { Text("Snus pro Dose") },
                placeholder = { Text("z. B. 20") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = priceText,
                onValueChange = { input ->
                    // Nur Ziffern sowie ein Komma oder Punkt erlauben
                    val clean = input.filter { it.isDigit() || it == ',' || it == '.' }.take(7)
                    priceText = clean
                    vm.setPrice(clean.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 })
                },
                label = { Text("Preis pro Dose in Euro") },
                placeholder = { Text("z. B. 5,50") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (state.ready) {
                    "Ein Snus kostet ${money(state.unitPrice)}."
                } else {
                    "Sobald beide Felder ausgefüllt sind, rechne ich den Preis pro Snus aus."
                },
                color = c.onSurfaceVariant, fontSize = 14.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Eine Preisänderung gilt nur für neue Einträge. Bisherige Ausgaben bleiben, wie sie waren.",
                color = c.onSurfaceVariant, fontSize = 14.sp,
            )
        }

        Spacer(Modifier.height(14.dp))

        // ---------- Sicherung ----------
        AppCard {
            Text("Sicherung", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Kopiere deine Daten als Text oder füge eine Sicherung aus der Zwischenablage ein. " +
                    "Auch eine Sicherung aus der Web-Version funktioniert.",
                color = c.onSurfaceVariant, fontSize = 14.sp,
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PillButton("Sicherung kopieren") {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Snus-Tracker Sicherung", vm.exportJson()))
                    Toast.makeText(context, "Sicherung kopiert", Toast.LENGTH_SHORT).show()
                }
                PillButton("Aus Zwischenablage einfügen", PillStyle.Outline) {
                    showRestoreDialog = true
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---------- Löschen ----------
        AppCard {
            Text("Alles löschen", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Entfernt alle Einträge, den Dosen-Zähler und die Einstellungen.",
                color = c.onSurfaceVariant, fontSize = 14.sp,
            )
            Spacer(Modifier.height(12.dp))
            PillButton("Alles löschen", PillStyle.Danger) { showResetDialog = true }
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Sicherung einfügen?") },
            text = { Text("Das ersetzt alle aktuellen Daten in der App durch den Inhalt der Zwischenablage.") },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreDialog = false
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = cm.primaryClip?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                    val ok = vm.restore(text)
                    Toast.makeText(
                        context,
                        if (ok) "Sicherung wiederhergestellt" else "Die Zwischenablage enthält keine gültige Sicherung",
                        Toast.LENGTH_LONG,
                    ).show()
                }) { Text("Ersetzen", color = c.primary) }
            },
            dismissButton = { TextButton(onClick = { showRestoreDialog = false }) { Text("Abbrechen") } },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Wirklich alles löschen?") },
            text = { Text("Das lässt sich nicht rückgängig machen. Kopiere vorher eine Sicherung, wenn du die Daten behalten willst.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    vm.reset()
                }) { Text("Löschen", color = c.primary) }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Abbrechen") } },
        )
    }
}
