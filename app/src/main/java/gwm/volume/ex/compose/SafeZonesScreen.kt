package gwm.volume.ex.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import gwm.volume.ex.data.SafeZone
import java.util.Locale

@Composable
fun SafeZonesScreen(
    zones: List<SafeZone>,
    onZonesChange: (List<SafeZone>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "When you tap outside the bubble, it hides. Safe zones let you define areas on screen where tapping will NOT dismiss the bubble.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (zones.isEmpty()) {
            Text(
                text = "No safe zones defined. Add one below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        for ((index, zone) in zones.withIndex()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Zone ${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            val updated = zones.toMutableList()
                            updated.removeAt(index)
                            onZonesChange(updated)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete zone")
                        }
                    }
                    Text(
                        text = "L: ${formatPct(zone.leftPercent)}  T: ${formatPct(zone.topPercent)}  R: ${formatPct(zone.rightPercent)}  B: ${formatPct(zone.bottomPercent)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        HorizontalDivider()

        AddZoneSection(onAdd = { zone ->
            onZonesChange(zones + zone)
        })
    }
}

@Composable
private fun AddZoneSection(onAdd: (SafeZone) -> Unit) {
    var leftText by remember { mutableStateOf("") }
    var topText by remember { mutableStateOf("") }
    var rightText by remember { mutableStateOf("") }
    var bottomText by remember { mutableStateOf("") }

    var leftFocused by remember { mutableStateOf(false) }
    var topFocused by remember { mutableStateOf(false) }
    var rightFocused by remember { mutableStateOf(false) }
    var bottomFocused by remember { mutableStateOf(false) }

    fun parsePct(text: String): Float? {
        val normalized = text.replace(',', '.')
        val v = normalized.toFloatOrNull() ?: return null
        return (v / 100f).coerceIn(0f, 1f)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Add Zone", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = leftText,
                    onValueChange = { leftText = it },
                    modifier = Modifier
                        .width(96.dp)
                        .onFocusChanged { leftFocused = it.isFocused },
                    singleLine = true,
                    label = { Text("Left %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = topText,
                    onValueChange = { topText = it },
                    modifier = Modifier
                        .width(96.dp)
                        .onFocusChanged { topFocused = it.isFocused },
                    singleLine = true,
                    label = { Text("Top %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = rightText,
                    onValueChange = { rightText = it },
                    modifier = Modifier
                        .width(96.dp)
                        .onFocusChanged { rightFocused = it.isFocused },
                    singleLine = true,
                    label = { Text("Right %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = bottomText,
                    onValueChange = { bottomText = it },
                    modifier = Modifier
                        .width(96.dp)
                        .onFocusChanged { bottomFocused = it.isFocused },
                    singleLine = true,
                    label = { Text("Bottom %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Button(
                onClick = {
                    val left = parsePct(leftText)
                    val top = parsePct(topText)
                    val right = parsePct(rightText)
                    val bottom = parsePct(bottomText)
                    if (left != null && top != null && right != null && bottom != null) {
                        onAdd(SafeZone(left, top, right, bottom))
                        leftText = ""
                        topText = ""
                        rightText = ""
                        bottomText = ""
                    }
                },
                enabled = listOf(leftText, topText, rightText, bottomText).all { it.isNotBlank() }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add Zone", modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

private fun formatPct(value: Float): String {
    return String.format(Locale.US, "%.0f%%", value * 100f)
}
