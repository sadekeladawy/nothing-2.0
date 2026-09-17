package com.example.state

import android.app.PendingIntent
import com.example.model.IslandMode
import com.example.model.IslandTheme
import com.example.model.MediaData
import com.example.model.NotificationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Central State Manager for Dynamic Island overlay and companion app.
 * Thread-safe and shared across processes/components.
 */
object IslandStateManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var notificationDismissJob: Job? = null

    private val _islandMode = MutableStateFlow(IslandMode.IDLE)
    val islandMode: StateFlow<IslandMode> = _islandMode.asStateFlow()

    private val _mediaData = MutableStateFlow<MediaData?>(null)
    val mediaData: StateFlow<MediaData?> = _mediaData.asStateFlow()

    private val _activeNotification = MutableStateFlow<NotificationData?>(null)
    val activeNotification: StateFlow<NotificationData?> = _activeNotification.asStateFlow()

    private val _islandTheme = MutableStateFlow(IslandTheme.CLASSIC_BLACK)
    val islandTheme: StateFlow<IslandTheme> = _islandTheme.asStateFlow()

    // Vertical Y-offset from the top of the screen (in dp) to align with front camera notch/punch-hole
    private val _yOffsetDp = MutableStateFlow(12)
    val yOffsetDp: StateFlow<Int> = _yOffsetDp.asStateFlow()

    private val _isOverlayRunning = MutableStateFlow(false)
    val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

    private val _isNotificationListenerConnected = MutableStateFlow(false)
    val isNotificationListenerConnected: StateFlow<Boolean> = _isNotificationListenerConnected.asStateFlow()

    fun setTheme(theme: IslandTheme) {
        _islandTheme.value = theme
    }

    fun setYOffsetDp(offset: Int) {
        _yOffsetDp.value = offset.coerceIn(0, 80)
    }

    fun setOverlayRunning(running: Boolean) {
        _isOverlayRunning.value = running
    }

    fun setNotificationListenerConnected(connected: Boolean) {
        _isNotificationListenerConnected.value = connected
    }

    /**
     * Updates active media session info.
     */
    fun updateMediaData(media: MediaData?) {
        _mediaData.value = media
        // If we are in IDLE and new media starts playing, transition smoothly to compact media
        if (_islandMode.value == IslandMode.IDLE && media != null && media.isPlaying) {
            _islandMode.value = IslandMode.MEDIA_COMPACT
        } else if (_islandMode.value in listOf(IslandMode.MEDIA_COMPACT, IslandMode.MEDIA_EXPANDED) && media == null) {
            _islandMode.value = IslandMode.IDLE
        }
    }

    /**
     * Intercepted notification posted.
     */
    fun postNotification(notification: NotificationData) {
        _activeNotification.value = notification
        _islandMode.value = IslandMode.NOTIFICATION

        notificationDismissJob?.cancel()
        notificationDismissJob = scope.launch {
            // Auto dismiss notification banner after 4.5 seconds
            delay(4500)
            if (_islandMode.value == IslandMode.NOTIFICATION) {
                dismissNotificationBanner()
            }
        }
    }

    fun dismissNotificationBanner() {
        notificationDismissJob?.cancel()
        _activeNotification.value = null
        if (_mediaData.value != null && _mediaData.value?.isPlaying == true) {
            _islandMode.value = IslandMode.MEDIA_COMPACT
        } else {
            _islandMode.value = IslandMode.IDLE
        }
    }

    /**
     * Expands media pill into the full player card.
     */
    fun expandMedia() {
        if (_mediaData.value != null) {
            _islandMode.value = IslandMode.MEDIA_EXPANDED
        }
    }

    /**
     * Collapses back to compact pill or idle.
     */
    fun collapseToPill() {
        if (_mediaData.value != null) {
            _islandMode.value = IslandMode.MEDIA_COMPACT
        } else {
            _islandMode.value = IslandMode.IDLE
        }
    }

    fun togglePlayPause() {
        val current = _mediaData.value ?: return
        current.onPlayPause?.invoke() ?: run {
            // Simulated toggle
            _mediaData.value = current.copy(isPlaying = !current.isPlaying)
        }
    }

    fun skipNext() {
        _mediaData.value?.onSkipNext?.invoke()
    }

    fun skipPrevious() {
        _mediaData.value?.onSkipPrevious?.invoke()
    }

    // --- Testing & Simulation Utilities ---

    fun simulateSpotifyPlaying(isPlaying: Boolean = true) {
        _mediaData.value = MediaData(
            title = "Blinding Lights",
            artist = "The Weeknd • After Hours",
            albumArt = null,
            albumArtUri = null,
            isPlaying = isPlaying,
            packageName = "com.spotify.music",
            durationMs = 200_000L,
            positionMs = 64_000L,
            launchIntent = null,
            onPlayPause = {
                val cur = _mediaData.value
                if (cur != null) {
                    _mediaData.value = cur.copy(isPlaying = !cur.isPlaying)
                }
            },
            onSkipNext = {
                _mediaData.value = _mediaData.value?.copy(
                    title = "Starboy",
                    artist = "The Weeknd, Daft Punk"
                )
            },
            onSkipPrevious = {
                _mediaData.value = _mediaData.value?.copy(
                    title = "Save Your Tears",
                    artist = "The Weeknd • After Hours"
                )
            }
        )
        _islandMode.value = IslandMode.MEDIA_COMPACT
    }

    fun simulateNotification(
        title: String = "Sarah Jenkins",
        message: String = "Hey! Let's grab lunch at 12:30? 🍜",
        pkg: String = "com.whatsapp"
    ) {
        postNotification(
            NotificationData(
                id = System.currentTimeMillis().toString(),
                title = title,
                text = message,
                packageName = pkg,
                appIcon = null,
                contentIntent = null
            )
        )
    }

    fun simulateIdle() {
        notificationDismissJob?.cancel()
        _activeNotification.value = null
        _mediaData.value = null
        _islandMode.value = IslandMode.IDLE
    }
}
