package gwm.volume.ex

import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope

class GetEventReader(private val scope: CoroutineScope) {
    companion object {
        private const val TAG = "GetEventReader"
    }

    @Volatile
    private var touchPctX: Float = -1f
    @Volatile
    private var touchPctY: Float = -1f

    private var maxX: Float = -1f
    private var maxY: Float = -1f
    private var device: String? = null
    private var process: Process? = null
    private var job: Job? = null

    fun getTouchPct(): Pair<Float, Float>? {
        val x = touchPctX
        val y = touchPctY
        return if (x >= 0 && y >= 0) Pair(x, y) else null
    }

    fun start() {
        stop()
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Shizuku permission not granted")
            return
        }
        job = scope.launch(Dispatchers.IO) {
            if (detectDevice() == null) return@launch
            while (isActive) {
                try {
                    runGetEvent()
                } catch (e: Exception) {
                    Log.e(TAG, "reader crashed", e)
                    delay(3000)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        synchronized(this) {
            process?.let {
                try { it.destroy() } catch (_: Exception) {}
            }
            process = null
        }
    }

    private suspend fun detectDevice(): String? {
        device?.let { if (maxX > 0) return it }
        return withContext(Dispatchers.IO) {
            try {
                val p = Shizuku.newProcess(arrayOf("getevent", "-p"), null, null)
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
                    return@withContext dev
                }
                Log.w(TAG, "no touch device found")
                null
            } catch (e: Exception) {
                Log.e(TAG, "detect failed", e)
                null
            }
        }
    }

    private fun runGetEvent() {
        val dev = device ?: return
        val p = Shizuku.newProcess(arrayOf("getevent", dev), null, null)
        synchronized(this) { process = p }
        p.inputStream.bufferedReader().use { reader ->
            for (line in reader.lines()) {
                if (!currentCoroutineContext().isActive) break
                parseLine(line.trim())
            }
        }
    }

    private fun parseLine(line: String) {
        val parts = line.split(' ', limit = 3)
        if (parts.size < 3 || parts[0] != "0003") return
        val code = parts[1]
        val value = parts[2].trimStart('0').let {
            if (it.isEmpty()) "0" else it
        }.toIntOrNull(16) ?: return
        when (code) {
            "0035" -> touchPctX = value.toFloat() / maxX
            "0036" -> touchPctY = value.toFloat() / maxY
        }
    }
}
