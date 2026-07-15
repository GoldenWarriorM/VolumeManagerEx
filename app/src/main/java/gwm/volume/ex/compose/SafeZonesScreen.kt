package gwm.volume.ex.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import gwm.volume.ex.data.SafeZone
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun SafeZonesScreen(
    zones: List<SafeZone>,
    onZonesChange: (List<SafeZone>) -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var drawingRect by remember { mutableStateOf<SafeZone?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val currentZones by rememberUpdatedState(zones)

    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenRatio = config.screenWidthDp.toFloat() / config.screenHeightDp.toFloat()
    val handleHitPx = with(density) { 28.dp.toPx() }

    val bgColor = Color(0xFF1A1A2E)
    val zoneColors = listOf(
        Color(0x8064B5F6), Color(0x8081C784), Color(0x80FFB74D),
        Color(0x80E57373), Color(0x80BA68C8), Color(0x804FC3F7)
    )
    val zoneBorder = Color(0xFF64B5F6)
    val selFill = Color(0x60FF9800)
    val selBorder = Color(0xFFFF9800)
    val handleColor = Color(0xFFFF9800)
    val drawPreviewFill = Color(0x60FFFFFF)
    val drawPreviewBorder = Color(0xCCFFFFFF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Define screen areas where tapping will NOT dismiss the bubble:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .aspectRatio(screenRatio)
                .onSizeChanged { canvasSize = it }
                .pointerInput(zones.size) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val cw = size.width.toFloat()
                        val ch = size.height.toFloat()
                        val xPct = (down.position.x / cw).coerceIn(0f, 1f)
                        val yPct = (down.position.y / ch).coerceIn(0f, 1f)
                        val zn = currentZones
                        val handleThresholdPct = handleHitPx / minOf(cw, ch)
                        val mnX = 20f / cw
                        val mnY = 20f / ch

                        val hitHandle = findHandle(zn, selectedIndex, xPct, yPct, handleThresholdPct)
                        val hitSelected = hitHandle == null && selectedIndex in zn.indices &&
                                isInside(zn[selectedIndex], xPct, yPct)
                        val hitAnyIndex = if (hitHandle == null && !hitSelected)
                            zn.indexOfLast { isInside(it, xPct, yPct) }
                        else -1

                        var lastXPct = xPct
                        var lastYPct = yPct
                        var dragged = false

                        if (hitHandle != null) {
                            val zi = selectedIndex
                            val eps = 0.005f
                            while (true) {
                                val event = awaitPointerEvent()
                                val c = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!c.pressed) break
                                val rawNx = (c.position.x / cw).coerceIn(mnX, 1f - mnX)
                                val rawNy = (c.position.y / ch).coerceIn(mnY, 1f - mnY)
                                val z = currentZones[zi]
                                val nx = when (hitHandle) {
                                    0 -> maxOf(mnX, minOf(rawNx, z.rightPercent - eps))
                                    1 -> maxOf(z.leftPercent + eps, minOf(rawNx, 1f - mnX))
                                    2 -> maxOf(mnX, minOf(rawNx, z.rightPercent - eps))
                                    3 -> maxOf(z.leftPercent + eps, minOf(rawNx, 1f - mnX))
                                    else -> rawNx
                                }
                                val ny = when (hitHandle) {
                                    0 -> maxOf(mnY, minOf(rawNy, z.bottomPercent - eps))
                                    1 -> maxOf(mnY, minOf(rawNy, z.bottomPercent - eps))
                                    2 -> maxOf(z.topPercent + eps, minOf(rawNy, 1f - mnY))
                                    3 -> maxOf(z.topPercent + eps, minOf(rawNy, 1f - mnY))
                                    else -> rawNy
                                }
                                if (nx != lastXPct || ny != lastYPct) dragged = true
                                val updated = when (hitHandle) {
                                    0 -> z.copy(leftPercent = nx, topPercent = ny)
                                    1 -> z.copy(rightPercent = nx, topPercent = ny)
                                    2 -> z.copy(leftPercent = nx, bottomPercent = ny)
                                    3 -> z.copy(rightPercent = nx, bottomPercent = ny)
                                    else -> z
                                }.let { sanitize(it) }
                                if (dragged) {
                                    onZonesChange(currentZones.toMutableList().apply { set(zi, updated) })
                                }
                                lastXPct = nx
                                lastYPct = ny
                                c.consume()
                            }
                        } else if (hitSelected || hitAnyIndex >= 0) {
                            if (hitAnyIndex >= 0) selectedIndex = hitAnyIndex
                            val zi = if (hitAnyIndex >= 0) hitAnyIndex else selectedIndex
                            val zn = currentZones
                            if (zi !in zn.indices) return@awaitEachGesture
                            val z0 = zn[zi]
                            val w = z0.rightPercent - z0.leftPercent
                            val h = z0.bottomPercent - z0.topPercent
                            val offX = xPct - z0.leftPercent
                            val offY = yPct - z0.topPercent
                            while (true) {
                                val event = awaitPointerEvent()
                                val c = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!c.pressed) break
                                val nx = (c.position.x / cw).coerceIn(0f, 1f)
                                val ny = (c.position.y / ch).coerceIn(0f, 1f)
                                dragged = true
                                val newLeft = maxOf(mnX, minOf(nx - offX, 1f - w - mnX))
                                val newTop = maxOf(mnY, minOf(ny - offY, 1f - h - mnY))
                                val updatedZn = currentZones
                                if (zi !in updatedZn.indices) break
                                onZonesChange(updatedZn.toMutableList().apply {
                                    set(zi, sanitize(SafeZone(
                                        newLeft, newTop,
                                        newLeft + w, newTop + h
                                    )))
                                })
                                c.consume()
                            }
                        } else {
                            selectedIndex = -1
                            var sx = xPct
                            var sy = yPct
                            var ex = xPct
                            var ey = yPct
                            while (true) {
                                val event = awaitPointerEvent()
                                val c = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!c.pressed) {
                                    if (dragged && min(abs(ex - sx), abs(ey - sy)) > 0.015f) {
                                        onZonesChange(currentZones + sanitize(SafeZone(
                                            minOf(sx, ex), minOf(sy, ey),
                                            maxOf(sx, ex), maxOf(sy, ey)
                                        )))
                                    }
                                    drawingRect = null
                                    break
                                }
                                ex = (c.position.x / cw).coerceIn(0f, 1f)
                                ey = (c.position.y / ch).coerceIn(0f, 1f)
                                if (abs(ex - sx) > 0.005f || abs(ey - sy) > 0.005f) {
                                    dragged = true
                                    drawingRect = SafeZone(
                                        minOf(sx, ex), minOf(sy, ey),
                                        maxOf(sx, ex), maxOf(sy, ey)
                                    )
                                }
                                c.consume()
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            drawRoundRect(color = bgColor, cornerRadius = CornerRadius(16f), size = size)

            for ((index, zone) in zones.withIndex()) {
                val color = zoneColors[index % zoneColors.size]
                val isSel = index == selectedIndex
                val rect = Rect(
                    zone.leftPercent * w, zone.topPercent * h,
                    zone.rightPercent * w, zone.bottomPercent * h
                )
                drawRect(color = if (isSel) selFill else color, topLeft = rect.topLeft, size = rect.size)
                drawRect(
                    color = if (isSel) selBorder else zoneBorder,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = Stroke(width = 2f)
                )
                if (isSel) {
                    val hs = 16f
                    val corners = listOf(
                        rect.topLeft, Offset(rect.right, rect.top),
                        Offset(rect.left, rect.bottom), Offset(rect.right, rect.bottom)
                    )
                    for (corner in corners) {
                        drawCircle(color = handleColor, radius = hs, center = corner)
                        drawCircle(color = Color.White, radius = 6f, center = corner)
                    }
                }
            }

            drawingRect?.let { dr ->
                val rect = Rect(
                    dr.leftPercent * w, dr.topPercent * h,
                    dr.rightPercent * w, dr.bottomPercent * h
                )
                drawRect(color = drawPreviewFill, topLeft = rect.topLeft, size = rect.size)
                drawRect(color = drawPreviewBorder, topLeft = rect.topLeft, size = rect.size, style = Stroke(width = 2f))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedIndex in zones.indices) {
                Button(
                    onClick = {
                        onZonesChange(zones.toMutableList().apply { removeAt(selectedIndex) })
                        selectedIndex = -1
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text("Delete Selected", modifier = Modifier.padding(start = 4.dp))
                }
            }
            if (zones.isNotEmpty()) {
                Button(onClick = { onZonesChange(emptyList()); selectedIndex = -1 }) {
                    Text("Clear All")
                }
            }
        }

        if (zones.isNotEmpty()) {
            zones.forEachIndexed { index, zone ->
                val color = zoneColors[index % zoneColors.size]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (index == selectedIndex)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color = color, shape = CircleShape)
                            .border(width = 1.dp, color = zoneBorder, shape = CircleShape)
                    )
                    Text(
                        text = "  Zone ${index + 1}:  L ${fmt(zone.leftPercent)}  T ${fmt(zone.topPercent)}  R ${fmt(zone.rightPercent)}  B ${fmt(zone.bottomPercent)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            onZonesChange(zones.toMutableList().apply { removeAt(index) })
                            if (selectedIndex == index) selectedIndex = -1
                            else if (selectedIndex > index) selectedIndex--
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private fun findHandle(
    zones: List<SafeZone>,
    selectedIndex: Int,
    xPct: Float,
    yPct: Float,
    thresholdPct: Float
): Int? {
    if (selectedIndex !in zones.indices) return null
    val zone = zones[selectedIndex]
    val corners = listOf(
        Offset(zone.leftPercent, zone.topPercent),
        Offset(zone.rightPercent, zone.topPercent),
        Offset(zone.leftPercent, zone.bottomPercent),
        Offset(zone.rightPercent, zone.bottomPercent)
    )
    val idx = corners.indexOfFirst { (it - Offset(xPct, yPct)).getDistance() < thresholdPct }
    return if (idx >= 0) idx else null
}

private fun isInside(zone: SafeZone, xPct: Float, yPct: Float): Boolean {
    return xPct >= zone.leftPercent && xPct <= zone.rightPercent &&
            yPct >= zone.topPercent && yPct <= zone.bottomPercent
}

private fun sanitize(z: SafeZone): SafeZone {
    val left = z.leftPercent.coerceIn(0f, 1f)
    val right = z.rightPercent.coerceIn(0f, 1f)
    val top = z.topPercent.coerceIn(0f, 1f)
    val bottom = z.bottomPercent.coerceIn(0f, 1f)
    return SafeZone(
        leftPercent = minOf(left, right),
        topPercent = minOf(top, bottom),
        rightPercent = maxOf(left, right),
        bottomPercent = maxOf(top, bottom)
    )
}

private fun fmt(v: Float): String = String.format(Locale.US, "%.0f%%", v * 100f)
