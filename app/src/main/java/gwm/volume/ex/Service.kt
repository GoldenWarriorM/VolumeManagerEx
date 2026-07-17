package gwm.volume.ex

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityButtonController.AccessibilityButtonCallback
import android.accessibilityservice.AccessibilityService
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import gwm.volume.ex.bubble.calculateBubbleLayout
import gwm.volume.ex.compose.AppVolumeList
import gwm.volume.ex.compose.SystemVolumePanel
import gwm.volume.ex.data.BubbleAnimationStyle
import gwm.volume.ex.system.ActivityTaskManagerProxy
import gwm.volume.ex.ui.theme.VolumeManagerTheme
import org.joor.Reflect
import java.util.Objects
import kotlin.math.roundToInt

@SuppressLint("AccessibilityPolicy")
class Service : AccessibilityService() {
    companion object {
        const val ACTION_SHOW_VIEW = "gwm.volume.ex.ACTION_SHOW_VIEW"
        const val ACTION_BUBBLE_SETTINGS_CHANGED = "gwm.volume.ex.ACTION_BUBBLE_SETTINGS_CHANGED"
        const val ACTION_BUBBLE_PREVIEW_MODE = "gwm.volume.ex.ACTION_BUBBLE_PREVIEW_MODE"
        const val EXTRA_BUBBLE_PREVIEW_ENABLED = "gwm.volume.ex.EXTRA_BUBBLE_PREVIEW_ENABLED"

        private const val TAG = "VolumeManager.Service"
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
        private const val SERVICE_NAME_SEPARATOR = ":"
        private const val ACCESSIBILITY_BUTTON_TARGETS_KEY = "accessibility_button_targets"
        private const val ACCESSIBILITY_SHORTCUT_TARGET_SERVICE_KEY =
            "accessibility_shortcut_target_service"

        private const val OVERLAY_IDLE_TIMEOUT = 5000L
        private const val ANIMATION_DURATION = 220L
    }

    private val windowManager: WindowManager by lazy {
        Objects.requireNonNull(getSystemService(WindowManager::class.java)!!)
    }
    private lateinit var manager: Manager
    val activityTaskManager by lazy { ActivityTaskManagerProxy(this) }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val hideOverlayRunnable = Runnable { hideOverlay() }
    private val hideBubbleRunnable = Runnable { hideBubble() }

    private var overlayView: View? = null
    private var overlayVisible = false
    private var overlayLifecycle: LifecycleRegistry? = null

    private var bubbleView: View? = null
    private var bubbleVisible = false
    private var bubbleLifecycle: LifecycleRegistry? = null
    private var bubblePreviewModeEnabled = false
    private var lastOverlayDismissMs = 0L

    private var zoneDebugOverlay: ZoneDebugOverlay? = null

    private val overlayLayoutParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    private val bubbleLayoutParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun installViewOwners(
        view: AbstractComposeView,
        onLifecycleCreated: (LifecycleRegistry) -> Unit
    ) {
        val owner = object : SavedStateRegistryOwner {
            private val lifecycleRegistry = LifecycleRegistry(this)
            private val savedStateRegistryController = SavedStateRegistryController.create(this)

            init {
                savedStateRegistryController.performRestore(null)
                lifecycleRegistry.currentState = Lifecycle.State.STARTED
                onLifecycleCreated(lifecycleRegistry)
            }

            override val lifecycle: Lifecycle
                get() = lifecycleRegistry

            override val savedStateRegistry: SavedStateRegistry
                get() = savedStateRegistryController.savedStateRegistry
        }

        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)
    }

    private fun createOverlayView(): View {
        return object : AbstractComposeView(this) {
            init {
                installViewOwners(this) { lifecycle -> overlayLifecycle = lifecycle }
            }

            override fun onAttachedToWindow() {
                super.onAttachedToWindow()

                Log.i(TAG, "overlay attached manufacturer: ${Build.MANUFACTURER}")
                @Suppress("SpellCheckingInspection")
                if (windowManager.isCrossWindowBlurEnabled && isHardwareAccelerated && Build.MANUFACTURER != "realme") {
                    background =
                        Reflect.on(rootSurfaceControl).call("createBackgroundBlurDrawable").apply {
                            call("setBlurRadius", 200)
                            call("setCornerRadius", 40f)
                        }.get()
                }

                startOverlayIdleTimer()
            }

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                    hideOverlay()
                    return true
                }
                return super.onTouchEvent(event)
            }

            @Composable
            override fun Content() {
                val prefs = manager.bubblePreferences

                VolumeManagerTheme {
                    Surface(
                        color = Color(1f, 1f, 1f, 0.3f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(40f)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp, 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (prefs.systemVolumeEnabled) {
                                SystemVolumePanel(
                                    audioManager = manager.audioManager,
                                    notificationManagerProxy = manager.notificationManagerProxy,
                                    showCallVolumeAlways = false,
                                    applyVisibilityFilter = true,
                                    allowVisibilityConfig = false,
                                    isSliderVisible = manager::isSystemSliderVisible,
                                    onSliderVisibilityChange = manager::setSystemSliderVisible,
                                    onChange = ::startOverlayIdleTimer
                                )
                            }

                            if (prefs.appVolumeListEnabled) {
                                AppVolumeList(
                                    apps = manager.apps.values.filter {
                                        !it.hidden
                                    }.toMutableList(),
                                    showAll = false,
                                    onChange = ::startOverlayIdleTimer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createBubbleView(): View {
        return object : AbstractComposeView(this) {
            init {
                installViewOwners(this) { lifecycle -> bubbleLifecycle = lifecycle }
            }

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                    if (!isInSafeZone(event, this)) {
                        hideBubble()
                    }
                    return true
                }
                return super.onTouchEvent(event)
            }

            @Composable
            override fun Content() {
                val shadowEnabled = manager.bubblePreferences.shadowEnabled

                VolumeManagerTheme {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
                            shadowElevation = if (shadowEnabled) 8.dp else 0.dp,
                            tonalElevation = if (shadowEnabled) 4.dp else 0.dp,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    showOverlay()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Open volume manager"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun animateIn(view: View) {
        view.animate().cancel()
        view.alpha = 0f
        view.scaleX = 0.9f
        view.scaleY = 0.9f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(null)
            .start()
    }

    private fun animateOut(view: View, onEnd: () -> Unit) {
        view.animate().cancel()
        view.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd()
                }
            })
            .start()
    }

    private fun startOverlayIdleTimer() {
        mainHandler.removeCallbacks(hideOverlayRunnable)
        mainHandler.postDelayed(hideOverlayRunnable, OVERLAY_IDLE_TIMEOUT)
    }

    private fun startBubbleIdleTimer() {
        mainHandler.removeCallbacks(hideBubbleRunnable)
        val delayMs = manager.bubblePreferences.closeDelayMs.coerceIn(300L, 15000L)
        mainHandler.postDelayed(hideBubbleRunnable, delayMs)
    }

    private fun showOverlay() {
        hideBubble()

        if (overlayView == null) {
            overlayView = createOverlayView()
            windowManager.addView(overlayView, overlayLayoutParams)
        }

        if (!overlayVisible) {
            overlayVisible = true
            animateOverlayIn(overlayView!!)
        }

        startOverlayIdleTimer()
    }

    private fun hideOverlay() {
        if (!overlayVisible) {
            return
        }

        overlayVisible = false
        lastOverlayDismissMs = System.currentTimeMillis()
        mainHandler.removeCallbacks(hideOverlayRunnable)

        val target = overlayView ?: return

        target.background = null

        animateOverlayOut(target) {
            if (overlayVisible) {
                return@animateOverlayOut
            }

            try {
                overlayLifecycle?.currentState = Lifecycle.State.DESTROYED
                windowManager.removeView(target)
            } catch (_: Exception) {
            } finally {
                overlayView = null
                overlayLifecycle = null
                if (bubblePreviewModeEnabled) {
                    showBubble()
                }
            }
        }
    }

    private fun animateOverlayIn(view: View) {
        view.animate().cancel()
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f
        view.rotation = 0f
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(null)
            .start()
    }

    private fun animateOverlayOut(view: View, onEnd: () -> Unit) {
        view.animate().cancel()
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f
        view.rotation = 0f
        view.alpha = 1f
        view.animate()
            .alpha(0f)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd()
                }
            })
            .start()
    }

    private fun updateBubbleLayout() {
        val preferences = manager.bubblePreferences
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val isLandscape = width > height
        val layout = calculateBubbleLayout(
            widthPx = width,
            heightPx = height,
            density = resources.displayMetrics.density,
            sizeScale = preferences.sizeScale,
            horizontal = if (isLandscape) preferences.horizontalLandscape else preferences.horizontal,
            vertical = if (isLandscape) preferences.verticalLandscape else preferences.vertical
        )
        bubbleLayoutParams.width = layout.sizePx
        bubbleLayoutParams.height = layout.sizePx
        bubbleLayoutParams.x =
            layout.xPx.coerceIn(0, (width - layout.sizePx).coerceAtLeast(0))
        bubbleLayoutParams.y =
            layout.yPx.coerceIn(0, (height - layout.sizePx).coerceAtLeast(0))

        val target = bubbleView
        if (target != null) {
            windowManager.updateViewLayout(target, bubbleLayoutParams)
        }
    }

    private fun showBubble() {
        if (!manager.bubblePreferences.enabled) {
            hideBubble()
            return
        }

        if (manager.audioManager.activePlaybackConfigurations.isEmpty()) {
            hideBubble()
            return
        }

        if (overlayVisible) {
            startOverlayIdleTimer()
            return
        }

        if (System.currentTimeMillis() - lastOverlayDismissMs < 300L) {
            return
        }

        updateBubbleLayout()

        if (bubbleView == null) {
            bubbleView = createBubbleView()
            windowManager.addView(bubbleView, bubbleLayoutParams)
        }

        if (!bubbleVisible) {
            bubbleVisible = true
            animateBubbleIn(bubbleView!!)
        }

        startBubbleIdleTimer()
    }

    private fun animateBubbleIn(view: View) {
        view.animate().cancel()
        view.translationX = 0f
        view.rotation = 0f

        when (manager.bubblePreferences.animationStyle) {
            BubbleAnimationStyle.Default -> {
                view.alpha = 0f
                view.scaleX = 0.9f
                view.scaleY = 0.9f
                view.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setListener(null)
                    .start()
            }

            BubbleAnimationStyle.SlideInLeft -> {
                view.alpha = 0f
                view.scaleX = 1f
                view.scaleY = 1f
                view.translationX = -80f * resources.displayMetrics.density
                view.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setListener(null)
                    .start()
            }

            BubbleAnimationStyle.SlideInRight -> {
                view.alpha = 0f
                view.scaleX = 1f
                view.scaleY = 1f
                view.translationX = 80f * resources.displayMetrics.density
                view.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setListener(null)
                    .start()
            }

            BubbleAnimationStyle.Scale -> {
                view.alpha = 1f
                view.scaleX = 0.6f
                view.scaleY = 0.6f
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setListener(null)
                    .start()
            }

            BubbleAnimationStyle.Fade -> {
                view.alpha = 0f
                view.scaleX = 1f
                view.scaleY = 1f
                view.animate()
                    .alpha(1f)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setListener(null)
                    .start()
            }

            BubbleAnimationStyle.Rotate -> {
                view.alpha = 0f
                view.scaleX = 1f
                view.scaleY = 1f
                view.rotation = -35f
                view.animate()
                    .alpha(1f)
                    .rotation(0f)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setListener(null)
                    .start()
            }
        }
    }

    private fun removeAccessibilityTargetFromSetting(settingKey: String, serviceName: String) {
        val value = Settings.Secure.getString(contentResolver, settingKey) ?: return
        if (value.isBlank()) {
            return
        }

        val updated = value.split(SERVICE_NAME_SEPARATOR)
            .filter { it.isNotBlank() && it != serviceName }
            .joinToString(SERVICE_NAME_SEPARATOR)

        if (updated != value) {
            Settings.Secure.putString(contentResolver, settingKey, updated)
        }
    }

    private fun disableAccessibilityShortcuts(serviceName: String) {
        removeAccessibilityTargetFromSetting(ACCESSIBILITY_BUTTON_TARGETS_KEY, serviceName)
        removeAccessibilityTargetFromSetting(
            ACCESSIBILITY_SHORTCUT_TARGET_SERVICE_KEY,
            serviceName
        )
    }

    private fun hideBubble() {
        if (!bubbleVisible) {
            return
        }

        bubbleVisible = false
        mainHandler.removeCallbacks(hideBubbleRunnable)

        val target = bubbleView ?: return
        animateBubbleOut(target) {
            if (bubbleVisible) {
                return@animateBubbleOut
            }

            try {
                bubbleLifecycle?.currentState = Lifecycle.State.DESTROYED
                windowManager.removeView(target)
            } catch (_: Exception) {
            } finally {
                bubbleView = null
                bubbleLifecycle = null
            }
        }
    }

    private fun animateBubbleOut(view: View, onEnd: () -> Unit) {
        view.animate().cancel()

        val animation = view.animate()
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd()
                }
            })

        when (manager.bubblePreferences.animationStyle) {
            BubbleAnimationStyle.Default -> {
                animation.alpha(0f).scaleX(0.9f).scaleY(0.9f)
            }

            BubbleAnimationStyle.SlideInLeft -> {
                animation.alpha(0f).translationX(-80f * resources.displayMetrics.density)
            }

            BubbleAnimationStyle.SlideInRight -> {
                animation.alpha(0f).translationX(80f * resources.displayMetrics.density)
            }

            BubbleAnimationStyle.Scale -> {
                animation.scaleX(0.6f).scaleY(0.6f)
            }

            BubbleAnimationStyle.Fade -> {
                animation.alpha(0f)
            }

            BubbleAnimationStyle.Rotate -> {
                animation.alpha(0f).rotation(-35f)
            }
        }

        animation.start()
    }

    private fun isInSafeZone(event: MotionEvent, view: View): Boolean {
        val display = windowManager.defaultDisplay
        val realSize = android.graphics.Point()
        display.getRealSize(realSize)
        val width = realSize.x.toFloat()
        val height = realSize.y.toFloat()
        val isLandscape = width > height
        val zones = if (isLandscape) manager.bubblePreferences.safeZonesLandscape else manager.bubblePreferences.safeZones
        Log.d("SafeZone", "display: %dx%d rot=%d isLandscape=%s zoneCount=%d"
            .format(realSize.x, realSize.y, display.rotation, isLandscape, zones.size))
        if (zones.isEmpty()) {
            Log.d("SafeZone", "no zones defined for this orientation")
            return false
        }

        val rawX = event.rawX
        val rawY = event.rawY
        val screenX: Float
        val screenY: Float
        if (rawX == 0f && rawY == 0f) {
            if (event.x == 0f && event.y == 0f) {
                val tapPct = manager.getEventReader.getTouchPct()
                if (tapPct != null) {
                    val (exPct, eyPct) = tapPct
                    val (fxPct, fyPct) = when (display.rotation) {
                        1 -> Pair(1f - eyPct, exPct)      // ROTATION_90
                        2 -> Pair(1f - exPct, 1f - eyPct) // ROTATION_180
                        3 -> Pair(eyPct, 1f - exPct)      // ROTATION_270
                        else -> Pair(exPct, eyPct)        // ROTATION_0
                    }
                    Log.d("SafeZone", "getevent: eventRaw=(%.4f,%.4f) rot%d -> display=(%.4f,%.4f)"
                        .format(exPct, eyPct, display.rotation, fxPct, fyPct))
                    val hit = zones.any { zone ->
                        val inside = fxPct >= zone.leftPercent && fxPct <= zone.rightPercent &&
                                fyPct >= zone.topPercent && fyPct <= zone.bottomPercent
                        Log.d("SafeZone", "  zone(%.3f-%.3f, %.3f-%.3f): (%.4f,%.4f) %s"
                            .format(zone.leftPercent, zone.rightPercent, zone.topPercent, zone.bottomPercent,
                                fxPct, fyPct, if (inside) "IN ZONE ✓" else "outside"))
                        inside
                    }
                    Log.d("SafeZone", "getevent result: %s".format(if (hit) "IN ZONE (keep)" else "OUTSIDE (hide)"))
                    return hit
                }
                Log.d("SafeZone", "getevent: no touch data, assuming in zone")
                return true
            }
            Log.d("SafeZone", "raw=(0,0) but event=(%.0f,%.0f) — using view location"
                .format(event.x, event.y))
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            screenX = event.x + location[0]
            screenY = event.y + location[1]
        } else {
            screenX = rawX
            screenY = rawY
        }

        val xPct = screenX / width
        val yPct = screenY / height
        Log.d("SafeZone", "motionEvent: raw=(%.0f,%.0f) screen=(%.0f,%.0f) pct=(%.4f,%.4f) win=(%dx%d)"
            .format(rawX, rawY, screenX, screenY, xPct, yPct, realSize.x, realSize.y))

        val hit = zones.any { zone ->
            val inside = xPct >= zone.leftPercent && xPct <= zone.rightPercent &&
                    yPct >= zone.topPercent && yPct <= zone.bottomPercent
            Log.d("SafeZone", "  zone(%.3f-%.3f, %.3f-%.3f): (%.4f,%.4f) %s"
                .format(zone.leftPercent, zone.rightPercent, zone.topPercent, zone.bottomPercent,
                    xPct, yPct, if (inside) "IN ZONE ✓" else "outside"))
            inside
        }
        Log.d("SafeZone", "motionEvent result: %s".format(if (hit) "IN ZONE (keep)" else "OUTSIDE (hide)"))
        return hit
    }

    private fun shouldIgnoreForegroundTaskVolumeKeys(): Boolean {
        val task = activityTaskManager.getForegroundTask()
        if (task != null) {
            val app = manager.apps[task.app]
            if (app != null && app.disableVolumeButtons) {
                return true
            }
        }
        return false
    }

    private val accessibilityButtonCallback = object : AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController?) {
            if (manager.shizukuStatus == Manager.ShizukuStatus.Connected) {
                showOverlay()
            }
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_SHOW_VIEW -> showOverlay()
                ACTION_BUBBLE_SETTINGS_CHANGED -> {
                    updateZoneDebugOverlay()
                    if (!manager.bubblePreferences.enabled) {
                        hideBubble()
                    } else {
                        updateBubbleLayout()
                        if (bubblePreviewModeEnabled && !overlayVisible) {
                            showBubble()
                        }
                    }
                }
                ACTION_BUBBLE_PREVIEW_MODE -> {
                    bubblePreviewModeEnabled =
                        intent.getBooleanExtra(EXTRA_BUBBLE_PREVIEW_ENABLED, false)
                    if (!bubblePreviewModeEnabled) {
                        hideBubble()
                    }
                }
                VOLUME_CHANGED_ACTION -> Unit
            }
        }
    }

    override fun onServiceConnected() {
        Log.i(TAG, "onServiceConnected")

        val application = super.getApplication() as MyApplication
        manager = application.manager
        val serviceName = ComponentName(this, Service::class.java).flattenToString()
        disableAccessibilityShortcuts(serviceName)

        accessibilityButtonController.registerAccessibilityButtonCallback(accessibilityButtonCallback)

        val filter = IntentFilter().apply {
            addAction(ACTION_SHOW_VIEW)
            addAction(ACTION_BUBBLE_SETTINGS_CHANGED)
            addAction(ACTION_BUBBLE_PREVIEW_MODE)
            addAction(VOLUME_CHANGED_ACTION)
        }
        registerReceiver(broadcastReceiver, filter, RECEIVER_NOT_EXPORTED)

        Log.i(TAG, "onServiceConnected done ${serviceInfo.capabilities.toString(2)}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}

    override fun onInterrupt() {
        Log.i(TAG, "onInterrupt")
    }

    private fun updateZoneDebugOverlay() {
        val prefs = manager.bubblePreferences
        if (prefs.debugZonesOverlay) {
            val display = windowManager.defaultDisplay
            val realSize = Point()
            display.getRealSize(realSize)
            val zones = if (realSize.x > realSize.y) prefs.safeZonesLandscape else prefs.safeZones
            if (zoneDebugOverlay == null) {
                val overlay = ZoneDebugOverlay(this)
                try {
                    windowManager.addView(overlay, overlay.getWindowLayoutParams())
                    zoneDebugOverlay = overlay
                } catch (e: Exception) {
                    Log.e(TAG, "failed to add zone debug overlay", e)
                    return
                }
            } else {
                val overlay = zoneDebugOverlay!!
                overlay.layoutParams = overlay.getWindowLayoutParams()
                try { windowManager.updateViewLayout(overlay, overlay.layoutParams) } catch (_: Exception) {}
            }
            zoneDebugOverlay?.zones = zones
        } else {
            zoneDebugOverlay?.let {
                try { windowManager.removeView(it) } catch (_: Exception) {}
            }
            zoneDebugOverlay = null
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        updateZoneDebugOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.i(TAG, "onDestroy")
        Toast.makeText(this, "Accessibility service died!", Toast.LENGTH_SHORT).show()

        mainHandler.removeCallbacks(hideOverlayRunnable)
        mainHandler.removeCallbacks(hideBubbleRunnable)
        hideOverlay()
        hideBubble()
        zoneDebugOverlay?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        zoneDebugOverlay = null

        try {
            accessibilityButtonController.unregisterAccessibilityButtonCallback(accessibilityButtonCallback)
        } catch (_: Exception) {
        }
        unregisterReceiver(broadcastReceiver)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP && event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }

        if (manager.shizukuStatus != Manager.ShizukuStatus.Connected) {
            return false
        }

        if (shouldIgnoreForegroundTaskVolumeKeys()) {
            return false
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            showBubble()
        }

        // Let the system handle the key so Android's default slider stays visible.
        return false
    }
}
