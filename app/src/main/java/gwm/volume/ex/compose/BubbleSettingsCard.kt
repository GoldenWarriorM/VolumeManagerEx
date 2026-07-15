package gwm.volume.ex.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
    bubbleEnabled: Boolean,
    settingsModified: Boolean = true,
    sizeScale: Float,
    horizontal: Float,
    vertical: Float,
    shadowEnabled: Boolean,
    closeDelayMs: Long,
    animationStyle: BubbleAnimationStyle,
    systemVolumeEnabled: Boolean,
    appVolumeListEnabled: Boolean,
    systemSliderVisibility: Map<String, Boolean>,
    onSizeScaleChange: (Float) -> Unit,
    onPositionChange: (Float, Float) -> Unit,
    onShadowEnabledChange: (Boolean) -> Unit,
    onCloseDelayChange: (Long) -> Unit,
    onAnimationStyleChange: (BubbleAnimationStyle) -> Unit,
    onBubbleEnabledChange: (Boolean) -> Unit,
    onSystemVolumeEnabledChange: (Boolean) -> Unit,
    onAppVolumeListEnabledChange: (Boolean) -> Unit,
    onSliderVisibilityChange: (String, Boolean) -> Unit,
    onOpenHiddenApps: () -> Unit
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
                Text("Volume Bubble", style = MaterialTheme.typography.titleLarge)

                if (settingsModified) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enabled", modifier = Modifier.weight(1f))
                        Switch(
                            checked = bubbleEnabled,
                            onCheckedChange = onBubbleEnabledChange
                        )
                    }
                }

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
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.requiredSize(32.dp).padding(end = 8.dp)
                    )
                    Text("Show system volume sliders", modifier = Modifier.weight(1f))
                    Switch(
                        checked = systemVolumeEnabled,
                        onCheckedChange = onSystemVolumeEnabledChange
                    )
                }

                val streamIcons = mapOf(
                    "call" to Icons.Default.PhoneInTalk,
                    "media" to Icons.AutoMirrored.Filled.VolumeUp,
                    "ring" to Icons.Default.RingVolume,
                    "alarm" to Icons.Default.Alarm,
                    "notification" to Icons.Default.NotificationsNone
                )

                if (systemVolumeEnabled) {
                    for ((id, label) in streamLabels) {
                        Row(
                            modifier = Modifier.padding(start = 32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = streamIcons[id] ?: Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.requiredSize(32.dp).padding(end = 8.dp)
                            )
                            Text(label, modifier = Modifier.weight(1f))
                            Switch(
                                checked = systemSliderVisibility[id] ?: true,
                                onCheckedChange = { onSliderVisibilityChange(id, it) }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.requiredSize(32.dp).padding(end = 8.dp)
                    )
                    Text("Show app volume list", modifier = Modifier.weight(1f))
                    Switch(
                        checked = appVolumeListEnabled,
                        onCheckedChange = onAppVolumeListEnabledChange
                    )
                }
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
                    Text(
                        text = "Hidden Apps",
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = onOpenHiddenApps) {
                        Text("Manage")
                    }
                }
                Text(
                    text = "Hidden apps are hidden from the overlay volume list.",
                    style = MaterialTheme.typography.bodyMedium
                )
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
