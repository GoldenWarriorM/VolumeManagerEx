package gwm.volume.ex.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import gwm.volume.ex.data.BubbleAnimationStyle
import java.util.Locale

@Composable
fun BubbleSettingsCard(
    sizeScale: Float,
    horizontal: Float,
    vertical: Float,
    shadowEnabled: Boolean,
    closeDelayMs: Long,
    animationStyle: BubbleAnimationStyle,
    systemVolumeEnabled: Boolean,
    appVolumeListEnabled: Boolean,
    systemSliderVisibility: Map<String, Boolean>,
    excludedPackages: Set<String>,
    excludedPackageLabels: Map<String, String>,
    onSizeScaleChange: (Float) -> Unit,
    onPositionChange: (Float, Float) -> Unit,
    onShadowEnabledChange: (Boolean) -> Unit,
    onCloseDelayChange: (Long) -> Unit,
    onAnimationStyleChange: (BubbleAnimationStyle) -> Unit,
    onSystemVolumeEnabledChange: (Boolean) -> Unit,
    onAppVolumeListEnabledChange: (Boolean) -> Unit,
    onSliderVisibilityChange: (String, Boolean) -> Unit,
    onRemoveExcludedPackage: (String) -> Unit
) {
    val animationOptions = listOf(
        BubbleAnimationStyle.Default to "Default",
        BubbleAnimationStyle.SlideInLeft to "Slide in from left",
        BubbleAnimationStyle.SlideInRight to "Slide in from right",
        BubbleAnimationStyle.Scale to "Scale",
        BubbleAnimationStyle.Fade to "Fade (other)",
        BubbleAnimationStyle.Rotate to "Rotate (other)"
    )

    val streamLabels = mapOf(
        "call" to "Call",
        "media" to "Media",
        "ring" to "Ring",
        "alarm" to "Alarm",
        "notification" to "Notification"
    )

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Quick Bubble", style = MaterialTheme.typography.titleLarge)
                Text(
                    "The real floating bubble is shown live while this page is open, so changes reflect directly on screen.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bubble shadow", modifier = Modifier.weight(1f))
                    Switch(
                        checked = shadowEnabled,
                        onCheckedChange = onShadowEnabledChange
                    )
                }

                NumericDurationSetting(
                    title = "Bubble close delay",
                    valueMs = closeDelayMs,
                    rangeSeconds = 0.3f..15f,
                    onValueChange = onCloseDelayChange
                )

                Text("Bubble popup animation", style = MaterialTheme.typography.titleMedium)
                for ((style, label) in animationOptions) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = animationStyle == style,
                                onClick = { onAnimationStyleChange(style) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = animationStyle == style,
                            onClick = null
                        )
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                NumericPercentSetting(
                    title = "Bubble size",
                    value = sizeScale,
                    range = 0.7f..1.8f,
                    onValueChange = onSizeScaleChange
                )

                NumericPercentSetting(
                    title = "Horizontal position",
                    value = horizontal,
                    range = 0f..1f,
                    onValueChange = { onPositionChange(it, vertical) }
                )

                NumericPercentSetting(
                    title = "Vertical position",
                    value = vertical,
                    range = 0f..1f,
                    onValueChange = { onPositionChange(horizontal, it) }
                )
            }
        }

        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Overlay Panel", style = MaterialTheme.typography.titleLarge)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show system volume sliders", modifier = Modifier.weight(1f))
                    Switch(
                        checked = systemVolumeEnabled,
                        onCheckedChange = onSystemVolumeEnabledChange
                    )
                }

                if (systemVolumeEnabled) {
                    for ((id, label) in streamLabels) {
                        Row(
                            modifier = Modifier.padding(start = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, modifier = Modifier.weight(1f))
                            Switch(
                                checked = systemSliderVisibility[id] ?: true,
                                onCheckedChange = { onSliderVisibilityChange(id, it) }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show app volume list", modifier = Modifier.weight(1f))
                    Switch(
                        checked = appVolumeListEnabled,
                        onCheckedChange = onAppVolumeListEnabledChange
                    )
                }
            }
        }

        if (excludedPackages.isNotEmpty()) {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Excluded Apps", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "These apps are hidden from the overlay volume list.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    for (pkg in excludedPackages.sorted()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = excludedPackageLabels[pkg] ?: pkg,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onRemoveExcludedPackage(pkg) }) {
                                Icon(
                                    imageVector = Icons.Default.RemoveCircle,
                                    contentDescription = "Remove from exclusions"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumericPercentSetting(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    var text by remember { mutableStateOf(formatDecimal(value * 100f, 4)) }
    var editingText by remember { mutableStateOf(false) }

    LaunchedEffect(value, editingText) {
        if (!editingText) {
            text = formatDecimal(value * 100f, 4)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    val normalizedInput = input.replace(',', '.')
                    text = normalizedInput
                    val parsed = normalizedInput.toFloatOrNull() ?: return@OutlinedTextField
                    onValueChange((parsed / 100f).coerceIn(range.start, range.endInclusive))
                },
                modifier = Modifier
                    .width(124.dp)
                    .onFocusChanged { editingText = it.isFocused },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                trailingIcon = { Text("%") }
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}

@Composable
private fun NumericDurationSetting(
    title: String,
    valueMs: Long,
    rangeSeconds: ClosedFloatingPointRange<Float>,
    onValueChange: (Long) -> Unit
) {
    val valueSeconds = (valueMs / 1000f).coerceIn(rangeSeconds.start, rangeSeconds.endInclusive)
    var text by remember { mutableStateOf(formatDecimal(valueSeconds, 3)) }
    var editingText by remember { mutableStateOf(false) }

    LaunchedEffect(valueMs, editingText) {
        if (!editingText) {
            text = formatDecimal(valueSeconds, 3)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    val normalizedInput = input.replace(',', '.')
                    text = normalizedInput
                    val parsed = normalizedInput.toFloatOrNull() ?: return@OutlinedTextField
                    onValueChange((parsed * 1000f).toLong())
                },
                modifier = Modifier
                    .width(124.dp)
                    .onFocusChanged { editingText = it.isFocused },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                trailingIcon = { Text("s") }
            )
        }

        Slider(
            value = valueSeconds,
            onValueChange = { onValueChange((it * 1000f).toLong()) },
            valueRange = rangeSeconds
        )
    }
}

private fun formatDecimal(value: Float, precision: Int): String {
    val fixed = String.format(Locale.US, "%.${precision}f", value)
    return fixed.trimEnd('0').trimEnd('.')
}
