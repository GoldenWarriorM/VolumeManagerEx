package gwm.volume.ex

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GetEventReader(private val scope: CoroutineScope) {
    companion object {
        private const val TAG = "GetEventReader"
    }

    @Volatile
    private var touchPctX: Float = -1f
    @Volatile
    private var touchPctY: Float = -1f

    @Volatile
    private var running = false

    private var maxX: Float = -1f
    private var maxY: Float = -1f
    private var device: String? = null
    private var process: Process? = null
    private var readerThread: Thread? = null

    fun getTouchPct(): Pair<Float, Float>? {
        val x = touchPctX
        val y = touchPctY
        return if (x >= 0 && y >= 0) Pair(x, y) else null
    }

    fun start() {
        stop()
        running = true
        scope.launch(Dispatchers.IO) {
            detectDevice()
            val dev = device ?: return@launch
            if (maxX <= 0 || maxY <= 0) return@launch
            Log.d(TAG, "starting reader for $dev")
            startReader(dev)
        }
    }

    fun stop() {
        running = false
        readerThread?.interrupt()
        readerThread = null
        synchronized(this) {
            process?.let {
                try { it.destroy() } catch (_: Exception) {}
            }
            process = null
        }
    }

    private fun detectDevice() {
        try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "getevent", "-p"))
            val output = p.inputStream.bufferedReader().readText()
            p.waitFor()

            val sections = output.split("add device ")
            for (section in sections) {
                if (!section.contains("INPUT_PROP_DIRECT")) continue
                val m = Regex("event(\\d+)").find(section) ?: continue
                val dev = "/dev/input/event${m.groupValues[1]}"
                val xM = Regex("0035.*max (\\d+)").find(section)
                val yM = Regex("0036.*max (\\d+)").find(section)
                if (xM != null && yM != null) {
                    maxX = xM.groupValues[1].toFloat()
                    maxY = yM.groupValues[1].toFloat()
                }
                device = dev
                Log.d(TAG, "touch device: $dev max=($maxX,$maxY)")
                return
            }
            Log.w(TAG, "no touch device found")
        } catch (e: Exception) {
            Log.e(TAG, "detect failed", e)
        }
    }

    private fun startReader(dev: String) {
        readerThread = Thread {
            try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "getevent", dev))
                synchronized(this) { process = p }
                p.inputStream.bufferedReader().use { reader ->
                    for (line in reader.lines()) {
                        if (!running) break
                        parseLine(line.trim())
                    }
                }
                p.waitFor()
            } catch (e: Exception) {
                if (running) Log.e(TAG, "reader crashed", e)
            }
        }.also {
            it.isDaemon = true
            it.name = "getevent-reader"
            it.start()
        }
    }

    private fun parseLine(line: String) {
        val parts = line.split(' ', limit = 3)
        if (parts.size < 3 || parts[0] != "0003") return
        val code = parts[1]
        val hex = parts[2].trimStart('0').ifEmpty { "0" }
        val value = hex.toIntOrNull(16) ?: return
        when (code) {
            "0035" -> touchPctX = value.toFloat() / maxX
            "0036" -> touchPctY = value.toFloat() / maxY
        }
    }
}
