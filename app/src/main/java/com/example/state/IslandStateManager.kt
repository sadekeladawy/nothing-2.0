package com.example.state

import android.app.PendingIntent
import com.example.model.GlassBlurEffect
import com.example.model.IslandActiveState
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
     */
    fun updateMediaData(media: MediaData?) {
        _mediaData.value = media
        // If notification is active, preserve it without interrupting
        if (_activeState.value is IslandActiveState.ShowingNotification) {
            return
        }
        if (media != null && media.isPlaying) {
            if (_activeState.value is IslandActiveState.Idle) {
                _activeState.value = IslandActiveState.ShowingMedia(isExpanded = false)
                _islandMode.value = IslandMode.MEDIA_COMPACT
            }
        } else if (media == null) {
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

    /**
     * Updates notification when batched within the 5-second window.
     * Shows a numerical badge indicator without force-expanding for every single alert.
     */
    fun postBatchedNotification(notification: NotificationData) {
        _activeNotification.value = notification

        if (_activeState.value !is IslandActiveState.ShowingNotification) {
            _activeState.value = IslandActiveState.ShowingNotification(isExpanded = false)
            _islandMode.value = IslandMode.NOTIFICATION
        } else {
            // If already showing, preserve current compact/expanded status without force-expanding
            val currentState = _activeState.value as IslandActiveState.ShowingNotification
            if (currentState.isExpanded) {
                _islandMode.value = IslandMode.NOTIFICATION_EXPANDED
            } else {
                _islandMode.value = IslandMode.NOTIFICATION
            }
        }

        notificationDismissJob?.cancel()
        notificationDismissJob = scope.launch {
            delay(5000)
            if (_activeState.value == IslandActiveState.ShowingNotification(isExpanded = false)) {
                dismissNotificationBanner()
            }
        }
    }

    fun dismissNotificationBanner() {
        notificationDismissJob?.cancel()
        simBatch.clear()
        _activeNotification.value = null
        if (_mediaData.value != null && _mediaData.value?.isPlaying == true) {
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
                if (_mediaData.value != null) {
                    _activeState.value = IslandActiveState.ShowingMedia(isExpanded = false)
                    _islandMode.value = IslandMode.MEDIA_COMPACT
                } else {
                    _activeState.value = IslandActiveState.Idle
                    _islandMode.value = IslandMode.IDLE
                }
            }
            is IslandActiveState.Idle -> {
                _activeState.value = IslandActiveState.Idle
                _islandMode.value = IslandMode.IDLE
            }
        }
    }

    /**
     * Collapses the island back directly to the idle state (e.g. after tapping to open the source app).
     */
    fun collapseToIdle() {
        notificationDismissJob?.cancel()
        _activeNotification.value = null
        _activeState.value = IslandActiveState.Idle
        _islandMode.value = IslandMode.IDLE
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

    private val simBatch = mutableListOf<NotificationData>()
    private var lastSimTimestamp = 0L

    fun simulateNotification(
        title: String = "Sarah Jenkins",
        message: String = "Hey! Let's grab lunch at 12:30? 🍜",
        pkg: String = "com.whatsapp"
    ) {
        val now = System.currentTimeMillis()
        val elapsed = now - lastSimTimestamp
        val sampleTitles = listOf("Sarah Jenkins", "Alex Rivera", "Slack • Design", "Emma Watson", "Uber Eats")
        val sampleMessages = listOf(
            "Hey! Let's grab lunch at 12:30? 🍜",
            "Can you review the latest Figma prototype?",
            "Meeting starting in 5 minutes 📅",
            "Are you free this weekend?",
            "Your courier is arriving in 2 mins 🚴"
        )
        val samplePkgs = listOf("com.whatsapp", "com.slack", "com.slack", "com.whatsapp", "com.ubercab.eats")

        if (simBatch.isNotEmpty() && elapsed <= 5000L) {
            val nextIdx = simBatch.size % sampleTitles.size
            val nextItem = NotificationData(
                id = System.currentTimeMillis().toString(),
                title = sampleTitles[nextIdx],
                text = sampleMessages[nextIdx],
                packageName = samplePkgs[nextIdx],
                appIcon = null,
                contentIntent = null,
                timestamp = now
            )
            simBatch.add(nextItem)
            val batched = nextItem.copy(
                badgeCount = simBatch.size,
                batchedNotifications = simBatch.toList()
            )
            lastSimTimestamp = now
            postBatchedNotification(batched)
        } else {
            simBatch.clear()
            val firstItem = NotificationData(
                id = System.currentTimeMillis().toString(),
                title = title,
                text = message,
                packageName = pkg,
                appIcon = null,
                contentIntent = null,
                timestamp = now
            )
            simBatch.add(firstItem)
            lastSimTimestamp = now
            postNotification(
                firstItem.copy(badgeCount = 1, batchedNotifications = listOf(firstItem))
            )
        }
    }

    fun simulateIdle() {
        notificationDismissJob?.cancel()
        simBatch.clear()
        _activeNotification.value = null
        _mediaData.value = null
        _activeState.value = IslandActiveState.Idle
        _islandMode.value = IslandMode.IDLE
    }
}
