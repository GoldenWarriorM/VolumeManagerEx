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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Define screen areas where tapping will NOT dismiss the bubble:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(370.dp)
                .onSizeChanged { canvasSize = it }
                .pointerInput(zones.size, selectedIndex) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val cw = size.width.toFloat()
                        val ch = size.height.toFloat()
                        val xPct = (down.position.x / cw).coerceIn(0f, 1f)
                        val yPct = (down.position.y / ch).coerceIn(0f, 1f)

                        val hitHandle = findHandle(zones, selectedIndex, xPct, yPct, cw)
                        val hitSelected = hitHandle == null && selectedIndex in zones.indices &&
                                isInside(zones[selectedIndex], xPct, yPct)
                        val hitAnyIndex = if (hitHandle == null && !hitSelected)
                            zones.indexOfLast { isInside(it, xPct, yPct) }
                        else -1

                        var lastXPct = xPct
                        var lastYPct = yPct
                        var dragged = false

                        if (hitHandle != null) {
                            val zi = selectedIndex
                            while (true) {
                                val event = awaitPointerEvent()
                                val c = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!c.pressed) break
                                val nx = (c.position.x / cw).coerceIn(0f, 1f)
                                val ny = (c.position.y / ch).coerceIn(0f, 1f)
                                if (nx != lastXPct || ny != lastYPct) dragged = true
                                val updated = when (hitHandle) {
                                    0 -> zones[zi].copy(leftPercent = nx, topPercent = ny)
                                    1 -> zones[zi].copy(rightPercent = nx, topPercent = ny)
                                    2 -> zones[zi].copy(leftPercent = nx, bottomPercent = ny)
                                    3 -> zones[zi].copy(rightPercent = nx, bottomPercent = ny)
                                    else -> zones[zi]
                                }.let { sanitize(it) }
                                if (dragged) onZonesChange(zones.toMutableList().apply { set(zi, updated) })
                                lastXPct = nx
                                lastYPct = ny
                                c.consume()
                            }
                        } else if (hitSelected) {
                            val zi = selectedIndex
                            while (true) {
                                val event = awaitPointerEvent()
                                val c = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!c.pressed) break
                                val nx = (c.position.x / cw).coerceIn(0f, 1f)
                                val ny = (c.position.y / ch).coerceIn(0f, 1f)
                                val dx = nx - lastXPct
                                val dy = ny - lastYPct
                                if (dx != 0f || dy != 0f) dragged = true
                                if (dragged) {
                                    val z = zones[zi]
                                    onZonesChange(zones.toMutableList().apply {
                                        set(zi, sanitize(SafeZone(
                                            z.leftPercent + dx, z.topPercent + dy,
                                            z.rightPercent + dx, z.bottomPercent + dy
                                        )))
                                    })
                                }
                                lastXPct = nx
                                lastYPct = ny
                                c.consume()
                            }
                        } else if (hitAnyIndex >= 0) {
                            selectedIndex = hitAnyIndex
                            val zi = hitAnyIndex
                            while (true) {
                                val event = awaitPointerEvent()
                                val c = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!c.pressed) break
                                val nx = (c.position.x / cw).coerceIn(0f, 1f)
                                val ny = (c.position.y / ch).coerceIn(0f, 1f)
                                val dx = nx - lastXPct
                                val dy = ny - lastYPct
                                if (dx != 0f || dy != 0f) dragged = true
                                if (dragged) {
                                    val z = zones[zi]
                                    onZonesChange(zones.toMutableList().apply {
                                        set(zi, sanitize(SafeZone(
                                            z.leftPercent + dx, z.topPercent + dy,
                                            z.rightPercent + dx, z.bottomPercent + dy
                                        )))
                                    })
                                }
                                lastXPct = nx
                                lastYPct = ny
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
                                        onZonesChange(zones + sanitize(SafeZone(
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
                    val hs = 10f
                    val corners = listOf(
                        rect.topLeft, Offset(rect.right, rect.top),
                        Offset(rect.left, rect.bottom), Offset(rect.right, rect.bottom)
                    )
                    for (corner in corners) {
                        drawCircle(color = handleColor, radius = hs, center = corner)
                        drawCircle(color = Color.White, radius = 4f, center = corner)
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
            Spacer(modifier = Modifier.height(4.dp))
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
    canvasWidthPx: Float
): Int? {
    if (selectedIndex !in zones.indices) return null
    val zone = zones[selectedIndex]
    val threshold = 14f / canvasWidthPx
    val corners = listOf(
        Offset(zone.leftPercent, zone.topPercent),
        Offset(zone.rightPercent, zone.topPercent),
        Offset(zone.leftPercent, zone.bottomPercent),
        Offset(zone.rightPercent, zone.bottomPercent)
    )
    val idx = corners.indexOfFirst { (it - Offset(xPct, yPct)).getDistance() < threshold }
    return if (idx >= 0) idx else null
}

private fun isInside(zone: SafeZone, xPct: Float, yPct: Float): Boolean {
    val pad = 0.01f
    return xPct >= zone.leftPercent - pad && xPct <= zone.rightPercent + pad &&
            yPct >= zone.topPercent - pad && yPct <= zone.bottomPercent + pad
}

private fun sanitize(z: SafeZone): SafeZone {
    return SafeZone(
        leftPercent = z.leftPercent.coerceIn(0f, 1f),
        topPercent = z.topPercent.coerceIn(0f, 1f),
        rightPercent = z.rightPercent.coerceIn(0f, 1f),
        bottomPercent = z.bottomPercent.coerceIn(0f, 1f)
    )
}

private fun fmt(v: Float): String = String.format(Locale.US, "%.0f%%", v * 100f)
