package gwm.volume.ex

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.util.Log
import android.view.View
import android.view.WindowManager
import gwm.volume.ex.data.SafeZone

class ZoneDebugOverlay(context: Context) : View(context) {

    var zones: List<SafeZone> = emptyList()
        set(value) {
            field = value
            zoneW = -1f
            zoneH = -1f
            postInvalidate()
        }

    private var zoneW = -1f
    private var zoneH = -1f

    private val fillColors = intArrayOf(
        0x8064B5F6.toInt(), 0x8081C784.toInt(), 0x80FFB74D.toInt(),
        0x80E57373.toInt(), 0x80BA68C8.toInt(), 0x804FC3F7.toInt()
    )
    private val borderColor = 0xFF64B5F6.toInt()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = borderColor
    }

    fun getWindowLayoutParams(): WindowManager.LayoutParams {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val size = Point()
        wm.defaultDisplay.getRealSize(size)
        Log.d("ZoneDebugOverlay", "getWindowLayoutParams: display=%dx%d".format(size.x, size.y))
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSPARENT
        )
    }

    private val debugPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF0000.toInt()
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x80FF0000.toInt()
        strokeWidth = 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        if (w != zoneW || h != zoneH) {
            val displaySize = Point()
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .defaultDisplay.getRealSize(displaySize)
            Log.d("ZoneDebugOverlay", "view=%dx%d display=%dx%d zones=%d"
                .format(width, height, displaySize.x, displaySize.y, zones.size))
            zoneW = w
            zoneH = h
        }

        canvas.drawRect(1f, 1f, w - 1f, h - 1f, debugPaint)
        canvas.drawLine(w / 2 - 20, h / 2, w / 2 + 20, h / 2, crossPaint)
        canvas.drawLine(w / 2, h / 2 - 20, w / 2, h / 2 + 20, crossPaint)
        canvas.drawCircle(w / 2, h / 2, 5f, crossPaint)

        for ((index, zone) in zones.withIndex()) {
            val left = zone.leftPercent * w
            val top = zone.topPercent * h
            val right = zone.rightPercent * w
            val bottom = zone.bottomPercent * h

            fillPaint.color = fillColors[index % fillColors.size]
            canvas.drawRect(left, top, right, bottom, fillPaint)
            canvas.drawRect(left, top, right, bottom, borderPaint)
        }
    }
}
