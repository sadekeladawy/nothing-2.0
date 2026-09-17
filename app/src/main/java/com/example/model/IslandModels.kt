package com.example.model

import android.app.PendingIntent
import android.graphics.Bitmap

/**
 * Visual styles for the Dynamic Island capsule.
 */
enum class IslandTheme(val displayName: String, val description: String) {
    CLASSIC_BLACK(
        displayName = "Classic Black",
        description = "Deep OLED pitch-black capsule with sleek metallic border."
    ),
    LUCID(
        displayName = "Lucid (Liquid Glass)",
        description = "Translucent glassmorphic pill with Compose blur & specular reflection."
    )
}

/**
 * Glassmorphism blur radius presets for the Dynamic Island overlay.
 */
enum class GlassBlurEffect(
    val displayName: String,
    val description: String,
    val blurRadiusDp: Float
) {
    LIGHT(
        displayName = "Light",
        description = "Crisp, subtle frosted glass effect with 6 dp blur radius.",
        blurRadiusDp = 6f
    ),
    DEEP(
        displayName = "Deep",
        description = "Rich, velvety frosted glass effect with 20 dp blur radius.",
        blurRadiusDp = 20f
    )
}

/**
 * Visual expansion modes for Dynamic Island.
 */
enum class IslandMode {
    IDLE,
    MEDIA_COMPACT,
    MEDIA_EXPANDED,
    NOTIFICATION,
    NOTIFICATION_EXPANDED
}

/**
 * Explicit active state representation for Dynamic Island routing.
 * Ensures the capsule knows whether it is currently displaying Media or Notification,
 * and whether it is compact or expanded.
 */
sealed class IslandActiveState {
    object Idle : IslandActiveState()
    data class ShowingMedia(val isExpanded: Boolean = false) : IslandActiveState()
    data class ShowingNotification(val isExpanded: Boolean = false) : IslandActiveState()
}

/**
 * Information regarding active media playback (e.g. Spotify, YouTube Music).
 */
data class MediaData(
    val title: String,
    val artist: String,
    val albumArt: Bitmap? = null,
    val albumArtUri: String? = null,
    val isPlaying: Boolean = false,
    val packageName: String? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val launchIntent: PendingIntent? = null,
    val onPlayPause: (() -> Unit)? = null,
    val onSkipNext: (() -> Unit)? = null,
    val onSkipPrevious: (() -> Unit)? = null
)

/**
 * Intercepted system notification representation.
 */
data class NotificationData(
    val id: String,
    val title: String,
    val text: String,
    val packageName: String,
    val appIcon: Bitmap? = null,
    val contentIntent: PendingIntent? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val badgeCount: Int = 1,
    val batchedNotifications: List<NotificationData> = emptyList()
)
