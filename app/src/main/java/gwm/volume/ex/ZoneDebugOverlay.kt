package gwm.volume.ex

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import gwm.volume.ex.data.SafeZone

class ZoneDebugOverlay(context: Context) : View(context) {

    var zones: List<SafeZone> = emptyList()
        set(value) {
            field = value
            postInvalidate()
        }

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
        return WindowManager.LayoutParams(
            size.x,
            size.y,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSPARENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        for ((index, zone) in zones.withIndex()) {
            val left = zone.leftPercent * w
            val top = zone.topPercent * h
            val right = zone.rightPercent * w
            val bottom = zone.bottomPercent * h
            if (right <= left || bottom <= top) continue

            fillPaint.color = fillColors[index % fillColors.size]
            canvas.drawRect(left, top, right, bottom, fillPaint)
            canvas.drawRect(left, top, right, bottom, borderPaint)
        }
    }
}
