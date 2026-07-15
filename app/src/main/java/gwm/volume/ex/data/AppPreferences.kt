package gwm.volume.ex.data

import kotlinx.serialization.Serializable

@Serializable
data class AppPreferences(
    var isPlayer: Boolean = false,
    var volume: Float = 1.0f,
    var hidden: Boolean = false,
    var disableVolumeButtons: Boolean = false
)

@Serializable
data class BubblePreferences(
    var enabled: Boolean = true,
    var sizeScale: Float = 1.2f,
    var horizontal: Float = 0.97f,
    var vertical: Float = 0.77f,
    var horizontalLandscape: Float = 1.0f,
    var verticalLandscape: Float = 0.99f,
    var shadowEnabled: Boolean = false,
    var closeDelayMs: Long = 3150L,
    var animationStyle: BubbleAnimationStyle = BubbleAnimationStyle.Default,
    var systemVolumeEnabled: Boolean = true,
    var appVolumeListEnabled: Boolean = true
)

@Serializable
enum class BubbleAnimationStyle {
    Default,
    SlideInLeft,
    SlideInRight,
    Scale,
    Fade,
    Rotate
}
