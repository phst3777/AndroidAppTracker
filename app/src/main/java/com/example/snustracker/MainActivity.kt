package com.example.snustracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    // "Heute" muss sich ändern, wenn die App über Mitternacht im Hintergrund lag
    private var today by mutableStateOf(LocalDate.now())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()   // App zeichnet hinter Status- und Navigationsleiste
        setContent {
            SnusTheme {
                val vm: TrackerViewModel = viewModel()
                SnusApp(vm, today)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        today = LocalDate.now()
    }
}

private val TAB_LABELS = listOf("Heute", "Verlauf", "Kosten", "Einstellungen")

@Composable
fun SnusApp(vm: TrackerViewModel, today: LocalDate) {
    val c = MaterialTheme.colorScheme
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = c.background,
        contentColor = c.onBackground,
        bottomBar = { TabBar(selected = tab, onSelect = { tab = it }) },
    ) { padding ->
        // padding enthält schon den Abstand zur Statusleiste und zur unteren Leiste
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> TodayScreen(
                    state = vm.state,
                    today = today,
                    onAdd = vm::add,
                    onRemove = vm::remove,
                    onOpenSettings = { tab = 3 },
                )
                1 -> HistoryScreen(vm.state, today)
                2 -> CostsScreen(vm.state, today)
                else -> SettingsScreen(vm)
            }
        }
    }
}

/** Untere Leiste. Der aktive Tab hat einen roten Punkt, wie bei Nothing OS. */
@Composable
private fun TabBar(selected: Int, onSelect: (Int) -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier
            .background(c.surface)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = c.outline)
        Row(Modifier.fillMaxWidth()) {
            TAB_LABELS.forEachIndexed { i, label ->
                val isSelected = i == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(i) }
                        .padding(top = 10.dp, bottom = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (isSelected) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(c.primary)
                        )
                    } else {
                        Spacer(Modifier.height(6.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) c.onBackground else c.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
