package com.example.state

import android.app.PendingIntent
import android.content.Context
import com.example.model.GlassBlurEffect
import com.example.model.IslandActiveState
import com.example.model.IslandMode
import com.example.model.IslandTheme
import com.example.model.MediaData
import com.example.model.NotificationData
import com.example.service.IslandNotificationListener
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

    // Explicit active state representation (Idle, ShowingMedia, ShowingNotification)
    private val _activeState = MutableStateFlow<IslandActiveState>(IslandActiveState.Idle)
    val activeState: StateFlow<IslandActiveState> = _activeState.asStateFlow()

    private val _islandMode = MutableStateFlow(IslandMode.IDLE)
    val islandMode: StateFlow<IslandMode> = _islandMode.asStateFlow()

    private val _mediaData = MutableStateFlow<MediaData?>(null)
    val mediaData: StateFlow<MediaData?> = _mediaData.asStateFlow()

    private val _activeNotification = MutableStateFlow<NotificationData?>(null)
    val activeNotification: StateFlow<NotificationData?> = _activeNotification.asStateFlow()

    private val _islandTheme = MutableStateFlow(IslandTheme.CLASSIC_BLACK)
    val islandTheme: StateFlow<IslandTheme> = _islandTheme.asStateFlow()

    // Glassmorphism blur effect preset for the overlay (Light vs Deep)
    private val _blurEffect = MutableStateFlow(GlassBlurEffect.LIGHT)
    val blurEffect: StateFlow<GlassBlurEffect> = _blurEffect.asStateFlow()

    // Horizontal X-offset from screen center (in dp) for off-center or punch-hole alignment
    private val _xOffsetDp = MutableStateFlow(0)
    val xOffsetDp: StateFlow<Int> = _xOffsetDp.asStateFlow()

    // Vertical Y-offset from the top of the screen (in dp) to align with front camera notch/punch-hole
    private val _yOffsetDp = MutableStateFlow(12)
    val yOffsetDp: StateFlow<Int> = _yOffsetDp.asStateFlow()

    private val _isOverlayRunning = MutableStateFlow(false)
    val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

    private val _isNotificationListenerConnected = MutableStateFlow(false)
    val isNotificationListenerConnected: StateFlow<Boolean> = _isNotificationListenerConnected.asStateFlow()

    // Full-screen video configuration and state
    private const val PREFS_NAME = "dynamic_island_preferences"
    private const val KEY_AUTO_HIDE_FS_VIDEO = "auto_hide_in_fullscreen_video"

    // Configuration setting: Automatically hide the Dynamic Island during full-screen video playback
    private val _autoHideInFullScreenVideo = MutableStateFlow(true)
    val autoHideInFullScreenVideo: StateFlow<Boolean> = _autoHideInFullScreenVideo.asStateFlow()

    // Runtime state: whether full-screen video/media playback is currently active
    private val _isFullScreenVideoActive = MutableStateFlow(false)
    val isFullScreenVideoActive: StateFlow<Boolean> = _isFullScreenVideoActive.asStateFlow()

    // Evaluated flag: whether the island should currently be hidden to not obstruct video content
    private val _shouldHideIsland = MutableStateFlow(false)
    val shouldHideIsland: StateFlow<Boolean> = _shouldHideIsland.asStateFlow()

    private fun recomputeShouldHide() {
        _shouldHideIsland.value = _autoHideInFullScreenVideo.value && _isFullScreenVideoActive.value
    }

    fun setAutoHideInFullScreenVideo(enabled: Boolean, context: Context? = null) {
        _autoHideInFullScreenVideo.value = enabled
        recomputeShouldHide()
        context?.let { ctx ->
            try {
                ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_AUTO_HIDE_FS_VIDEO, enabled)
                    .apply()
            } catch (_: Exception) {
            }
        }
    }

    fun setFullScreenVideoActive(active: Boolean) {
        _isFullScreenVideoActive.value = active
        recomputeShouldHide()
    }

    fun toggleFullScreenVideoSimulation() {
        setFullScreenVideoActive(!_isFullScreenVideoActive.value)
    }

    fun initPreferences(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val autoHide = prefs.getBoolean(KEY_AUTO_HIDE_FS_VIDEO, true)
            _autoHideInFullScreenVideo.value = autoHide
            recomputeShouldHide()
        } catch (_: Exception) {
        }
    }

    fun setTheme(theme: IslandTheme) {
        _islandTheme.value = theme
    }

    fun setBlurEffect(effect: GlassBlurEffect) {
        _blurEffect.value = effect
    }

    fun setXOffsetDp(offset: Int) {
        _xOffsetDp.value = offset.coerceIn(-120, 120)
    }

    fun setYOffsetDp(offset: Int) {
        _yOffsetDp.value = offset.coerceIn(0, 100)
    }

    fun resetOffsets() {
        _xOffsetDp.value = 0
        _yOffsetDp.value = 12
    }

    fun setOverlayRunning(running: Boolean) {
        _isOverlayRunning.value = running
    }

    fun setNotificationListenerConnected(connected: Boolean) {
        _isNotificationListenerConnected.value = connected
    }

    /**
     * Updates active media session info.
     * PlaybackState.STATE_PAUSED keeps the media state active (just updating the UI).
     * Only clearing media data reverts to Idle.
     */
    fun updateMediaData(media: MediaData?) {
        _mediaData.value = media
        // If notification is active, preserve it without interrupting
        if (_activeState.value is IslandActiveState.ShowingNotification) {
            return
        }
        if (media != null) {
            if (_activeState.value is IslandActiveState.Idle) {
                _activeState.value = IslandActiveState.ShowingMedia(isExpanded = false)
                _islandMode.value = IslandMode.MEDIA_COMPACT
            }
        } else {
            if (_activeState.value is IslandActiveState.ShowingMedia) {
                _activeState.value = IslandActiveState.Idle
                _islandMode.value = IslandMode.IDLE
            }
        }
    }

    /**
     * Clears active media session data and immediately reverts the capsule state back to Idle.
     */
    fun clearMediaData() {
        _mediaData.value = null
        if (_activeState.value !is IslandActiveState.ShowingNotification) {
            _activeState.value = IslandActiveState.Idle
            _islandMode.value = IslandMode.IDLE
        }
    }

    fun clearMedia() {
        clearMediaData()
    }

    /**
     * Intercepted notification posted.
     */
    fun postNotification(notification: NotificationData) {
        _activeNotification.value = notification
        _activeState.value = IslandActiveState.ShowingNotification(isExpanded = false)
        _islandMode.value = IslandMode.NOTIFICATION

        notificationDismissJob?.cancel()
        notificationDismissJob = scope.launch {
            // Auto dismiss notification banner after 4.5 seconds if not expanded
            delay(4500)
            if (_activeState.value == IslandActiveState.ShowingNotification(isExpanded = false)) {
                dismissNotificationBanner()
            }
        }
    }

    fun dismissNotificationBanner() {
        notificationDismissJob?.cancel()
        _activeNotification.value = null
        val currentMedia = _mediaData.value
        if (currentMedia != null) {
            _activeState.value = IslandActiveState.ShowingMedia(isExpanded = false)
            _islandMode.value = IslandMode.MEDIA_COMPACT
        } else {
            _activeState.value = IslandActiveState.Idle
            _islandMode.value = IslandMode.IDLE
        }
    }

    /**
     * Centralized capsule tap router.
     * Checks active state:
     * - If active data is a Notification, expands into Notification UI.
     * - If active data is Media, expands into Media UI.
     */
    fun onCapsuleTapped() {
        when (val state = _activeState.value) {
            is IslandActiveState.ShowingNotification -> {
                if (!state.isExpanded) {
                    expandNotification()
                }
            }
            is IslandActiveState.ShowingMedia -> {
                if (!state.isExpanded) {
                    expandMedia()
                }
            }
            is IslandActiveState.Idle -> {
                if (_mediaData.value != null) {
                    expandMedia()
                }
            }
        }
    }

    /**
     * Expands notification banner into detailed notification card.
     */
    fun expandNotification() {
        if (_activeNotification.value != null) {
            notificationDismissJob?.cancel() // Cancel auto-dismiss while expanded
            _activeState.value = IslandActiveState.ShowingNotification(isExpanded = true)
            _islandMode.value = IslandMode.NOTIFICATION_EXPANDED
        }
    }

    /**
     * Expands media pill into the full player card.
     */
    fun expandMedia() {
        if (_mediaData.value != null) {
            _activeState.value = IslandActiveState.ShowingMedia(isExpanded = true)
            _islandMode.value = IslandMode.MEDIA_EXPANDED
        }
    }

    /**
     * Checks if a media session is STILL active (either Playing or Paused).
     * Verifies both the MediaController / MediaSessionManager and local MediaData.
     */
    fun isMediaSessionActive(): Boolean {
        // 1. Check system MediaController via IslandNotificationListener
        if (IslandNotificationListener.isMediaActive()) {
            return true
        }
        // 2. Check current tracked media data (active whether playing or paused)
        return _mediaData.value != null
    }

    /**
     * Collapses expanded media player back to compact pill if media is still active (playing or paused),
     * or to idle if no media is active.
     */
    fun collapseExpandedMedia() {
        if (isMediaSessionActive()) {
            _activeState.value = IslandActiveState.ShowingMedia(isExpanded = false)
            _islandMode.value = IslandMode.MEDIA_COMPACT
        } else {
            _activeState.value = IslandActiveState.Idle
            _islandMode.value = IslandMode.IDLE
        }
    }

    /**
     * Handles ACTION_OUTSIDE outside-tap events on the Dynamic Island.
     * Instead of unconditionally resetting to Idle, verifies the current media status:
     * - If a media session is STILL active (either Playing or Paused): transitions back to the compact media pill.
     * - Only if NO media is active: transitions back to the Idle state.
     */
    fun collapseOnOutsideTap() {
        notificationDismissJob?.cancel()
        _activeNotification.value = null

        if (isMediaSessionActive()) {
            _activeState.value = IslandActiveState.ShowingMedia(isExpanded = false)
            _islandMode.value = IslandMode.MEDIA_COMPACT
        } else {
            _activeState.value = IslandActiveState.Idle
            _islandMode.value = IslandMode.IDLE
        }
    }

    /**
     * Collapses back to compact pill or idle.
     */
    fun collapseToPill() {
        when (val state = _activeState.value) {
            is IslandActiveState.ShowingNotification -> {
                if (state.isExpanded) {
                    _activeState.value = IslandActiveState.ShowingNotification(isExpanded = false)
                    _islandMode.value = IslandMode.NOTIFICATION
                } else {
                    dismissNotificationBanner()
                }
            }
            is IslandActiveState.ShowingMedia -> {
                collapseExpandedMedia()
            }
            is IslandActiveState.Idle -> {
                if (isMediaSessionActive()) {
                    _activeState.value = IslandActiveState.ShowingMedia(isExpanded = false)
                    _islandMode.value = IslandMode.MEDIA_COMPACT
                } else {
                    _activeState.value = IslandActiveState.Idle
                    _islandMode.value = IslandMode.IDLE
                }
            }
        }
    }

    /**
     * Collapses the island back directly:
     * Verifies if media is active (playing or paused) and returns to compact media pill,
     * otherwise transitions to idle.
     */
    fun collapseToIdle() {
        collapseOnOutsideTap()
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
        if (_activeState.value !is IslandActiveState.ShowingNotification) {
            _activeState.value = IslandActiveState.ShowingMedia(isExpanded = false)
            _islandMode.value = IslandMode.MEDIA_COMPACT
        }
    }

    fun simulateNotification(
        title: String = "Sarah Jenkins",
        message: String = "Hey! Let's grab lunch at 12:30? 🍜",
        pkg: String = "com.whatsapp"
    ) {
        val notif = NotificationData(
            id = System.currentTimeMillis().toString(),
            title = title,
            text = message,
            packageName = pkg,
            appIcon = null,
            contentIntent = null,
            timestamp = System.currentTimeMillis()
        )
        postNotification(notif)
    }

    fun simulateIdle() {
        notificationDismissJob?.cancel()
        _activeNotification.value = null
        _mediaData.value = null
        _isFullScreenVideoActive.value = false
        recomputeShouldHide()
        _activeState.value = IslandActiveState.Idle
        _islandMode.value = IslandMode.IDLE
    }
}
