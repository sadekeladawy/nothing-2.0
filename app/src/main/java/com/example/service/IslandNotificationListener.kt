package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.model.MediaData
import com.example.model.NotificationData
import com.example.state.IslandStateManager

/**
 * Service to intercept incoming notifications and observe active MediaSessions
 * (Spotify, YouTube Music, Podcasting apps, etc.).
 */
class IslandNotificationListener : NotificationListenerService() {

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeMediaController: MediaController? = null
    private var sessionsChangedListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // 5-second notification batching state
    private val notificationBatch = mutableListOf<NotificationData>()
    private var lastNotificationTimestamp = 0L
    private val batchLock = Any()
    private val BATCH_WINDOW_MS = 5000L

    private val mediaControllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            val currentState = state?.state
            if (state == null || currentState == PlaybackState.STATE_STOPPED || currentState == PlaybackState.STATE_NONE) {
                Log.d(TAG, "Playback state changed to stopped/none ($currentState). Reverting to Idle.")
                cleanupMediaController()
                IslandStateManager.clearMediaData()
                return
            }
            updateMediaFromController(activeMediaController)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            updateMediaFromController(activeMediaController)
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            Log.d(TAG, "MediaSession destroyed. Immediately reverting capsule state to Idle.")
            cleanupMediaController()
            IslandStateManager.clearMediaData()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        IslandStateManager.setNotificationListenerConnected(true)
        Log.d(TAG, "Notification listener connected successfully")

        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val componentName = ComponentName(this, IslandNotificationListener::class.java)

            sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                handleSessionsChanged(controllers)
            }

            mediaSessionManager?.addOnActiveSessionsChangedListener(
                sessionsChangedListener!!,
                componentName,
                mainHandler
            )

            // Initial query
            val currentControllers = mediaSessionManager?.getActiveSessions(componentName)
            handleSessionsChanged(currentControllers)
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException while setting up MediaSessionManager: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaSessionManager", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        IslandStateManager.setNotificationListenerConnected(false)
        cleanupMediaController()
        sessionsChangedListener?.let {
            try {
                mediaSessionManager?.removeOnActiveSessionsChangedListener(it)
            } catch (_: Exception) {
            }
        }
    }

    private fun handleSessionsChanged(controllers: List<MediaController>?) {
        Log.d(TAG, "Active sessions changed: ${controllers?.size ?: 0} controller(s) reported")

        // 1. If the list of active controllers becomes empty, reset the UI state to Idle immediately
        if (controllers.isNullOrEmpty()) {
            Log.d(TAG, "Active controllers list is empty. Resetting capsule state to Idle.")
            cleanupMediaController()
            IslandStateManager.clearMediaData()
            return
        }

        // 2. If the specific controller we were tracking is removed, reset the UI state to Idle
        val current = activeMediaController
        if (current != null && controllers.none { it.sessionToken == current.sessionToken }) {
            Log.d(TAG, "Tracked controller ${current.packageName} was removed from active sessions. Resetting to Idle.")
            cleanupMediaController()
            IslandStateManager.clearMediaData()
            return
        }

        // 3. Find if any controller is actively playing
        val preferred = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }

        if (preferred == null) {
            // Check if current tracked controller is still playing or active
            val currentPlaybackState = activeMediaController?.playbackState?.state
            if (currentPlaybackState == null ||
                currentPlaybackState == PlaybackState.STATE_STOPPED ||
                currentPlaybackState == PlaybackState.STATE_NONE
            ) {
                Log.d(TAG, "No controllers actively playing and current controller not playing. Resetting UI state to Idle.")
                cleanupMediaController()
                IslandStateManager.clearMediaData()
                return
            }
        }

        val targetController = preferred ?: activeMediaController ?: controllers.firstOrNull {
            val s = it.playbackState?.state
            s != PlaybackState.STATE_STOPPED && s != PlaybackState.STATE_NONE
        }

        if (targetController == null) {
            cleanupMediaController()
            IslandStateManager.clearMediaData()
            return
        }

        if (targetController != activeMediaController) {
            cleanupMediaController()
            activeMediaController = targetController
            try {
                targetController.registerCallback(mediaControllerCallback, mainHandler)
            } catch (e: Exception) {
                Log.e(TAG, "Error registering MediaController callback", e)
            }
        }

        updateMediaFromController(targetController)
    }

    private fun cleanupMediaController() {
        try {
            activeMediaController?.unregisterCallback(mediaControllerCallback)
        } catch (_: Exception) {
        }
        activeMediaController = null
    }

    private fun updateMediaFromController(controller: MediaController?) {
        if (controller == null) {
            IslandStateManager.clearMediaData()
            return
        }

        val playbackState = controller.playbackState
        val state = playbackState?.state

        // If the playback state is stopped or none, revert immediately to Idle
        if (state == null || state == PlaybackState.STATE_STOPPED || state == PlaybackState.STATE_NONE) {
            Log.d(TAG, "updateMediaFromController: PlaybackState is null, STOPPED or NONE ($state). Reverting to Idle.")
            cleanupMediaController()
            IslandStateManager.clearMediaData()
            return
        }

        val metadata = controller.metadata
        val isPlaying = state == PlaybackState.STATE_PLAYING
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Media Playing"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""

        val albumArt: Bitmap? = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = playbackState?.position ?: 0L
        val launchIntent = controller.sessionActivity

        val mediaData = MediaData(
            title = title,
            artist = artist,
            albumArt = albumArt,
            isPlaying = isPlaying,
            packageName = controller.packageName,
            durationMs = duration,
            positionMs = position,
            launchIntent = launchIntent,
            onPlayPause = {
                val curState = controller.playbackState?.state
                if (curState == PlaybackState.STATE_PLAYING) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            },
            onSkipNext = {
                controller.transportControls.skipToNext()
            },
            onSkipPrevious = {
                controller.transportControls.skipToPrevious()
            }
        )

        IslandStateManager.updateMediaData(mediaData)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // Ignore our own foreground service notifications
        if (sbn.packageName == packageName) return

        val notification = sbn.notification ?: return

        // Extract title and text
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        if (title.isNullOrBlank() && text.isNullOrBlank()) return

        // Extract icon or avatar
        var iconBitmap: Bitmap? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val largeIcon = notification.getLargeIcon()
                if (largeIcon != null) {
                    val drawable = largeIcon.loadDrawable(this)
                    iconBitmap = drawableToBitmap(drawable)
                }
            }
            if (iconBitmap == null) {
                val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                val appIcon = packageManager.getApplicationIcon(appInfo)
                iconBitmap = drawableToBitmap(appIcon)
            }
        } catch (_: Exception) {
        }

        val notificationData = NotificationData(
            id = sbn.key ?: System.currentTimeMillis().toString(),
            title = title ?: "Notification",
            text = text ?: "",
            packageName = sbn.packageName,
            appIcon = iconBitmap,
            contentIntent = notification.contentIntent,
            timestamp = sbn.postTime
        )

        // 5-second notification batching logic:
        // If multiple notifications arrive within 5 seconds, batch them and show a numerical badge
        // indicator on the Dynamic Island instead of force-expanding for every single alert.
        val now = System.currentTimeMillis()
        val isBatched: Boolean
        val batchedData: NotificationData

        synchronized(batchLock) {
            val elapsed = now - lastNotificationTimestamp
            if (notificationBatch.isNotEmpty() && elapsed <= BATCH_WINDOW_MS) {
                // Arrived within 5 seconds: batch incoming notification
                notificationBatch.add(notificationData)
                isBatched = true
                batchedData = notificationData.copy(
                    badgeCount = notificationBatch.size,
                    batchedNotifications = notificationBatch.toList()
                )
                lastNotificationTimestamp = now
                Log.d(TAG, "Batched notification (${notificationBatch.size} in 5s): ${notificationData.title}")
            } else {
                // First notification or > 5s: start fresh batch
                notificationBatch.clear()
                notificationBatch.add(notificationData)
                isBatched = false
                batchedData = notificationData.copy(
                    badgeCount = 1,
                    batchedNotifications = listOf(notificationData)
                )
                lastNotificationTimestamp = now
                Log.d(TAG, "First notification alert: ${notificationData.title}")
            }
        }

        if (isBatched) {
            IslandStateManager.postBatchedNotification(batchedData)
        } else {
            IslandStateManager.postNotification(batchedData)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return

        // 1. Check if media notification for current package was removed (e.g. user swiped away Spotify)
        val currentPkg = activeMediaController?.packageName
        if (currentPkg != null && sbn.packageName == currentPkg) {
            Log.d(TAG, "Notification for active media package $currentPkg was removed. Checking active playback.")
            val state = activeMediaController?.playbackState?.state
            if (state != PlaybackState.STATE_PLAYING) {
                Log.d(TAG, "Media notification removed and playback is not active. Reverting to Idle.")
                cleanupMediaController()
                IslandStateManager.clearMediaData()
            }
        }

        // 2. Remove from notification batch tracking
        synchronized(batchLock) {
            notificationBatch.removeAll { it.id == sbn.key }
        }
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null) return null
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 64
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 64
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    companion object {
        private const val TAG = "IslandNotifListener"
    }
}
