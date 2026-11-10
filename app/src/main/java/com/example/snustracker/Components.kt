package com.example.snustracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Abgerundete Karte mit dünnem Rand. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, borderColor, shape)
            .padding(18.dp),
        content = content,
    )
}

/** Schalter mit mehreren Optionen, z. B. „Heute | Gestern“. */
@Composable
fun Segment(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(c.surface)
            .border(1.dp, c.outline, CircleShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val isSelected = i == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) c.onBackground else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = if (isSelected) c.background else c.onSurfaceVariant,
                )
            }
        }
    }
}

enum class PillStyle { Filled, Outline, Danger }

/** Runder Knopf mit Text. */
@Composable
fun PillButton(text: String, style: PillStyle = PillStyle.Filled, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val shape = CircleShape
    val base = Modifier.clip(shape)
    val styled = when (style) {
        PillStyle.Filled -> base.background(c.onBackground)
        PillStyle.Outline -> base.border(1.dp, c.outline, shape)
        PillStyle.Danger -> base.border(1.dp, c.primary, shape)
    }
    val textColor = when (style) {
        PillStyle.Filled -> c.background
        PillStyle.Outline -> c.onBackground
        PillStyle.Danger -> c.primary
    }
    Box(
        modifier = styled.clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/** Zeile mit Beschriftung links und Wert rechts. */
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, fontSize = 14.sp)
    }
}
