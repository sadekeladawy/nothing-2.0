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
 * Visual expansion modes for Dynamic Island.
 */
enum class IslandMode {
    IDLE,
    MEDIA_COMPACT,
    NOTIFICATION,
    MEDIA_EXPANDED
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
    val timestamp: Long = System.currentTimeMillis()
)
