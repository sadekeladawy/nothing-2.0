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

    private val mediaControllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            super.onPlaybackStateChanged(state)
            updateMediaFromController(activeMediaController)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            super.onMetadataChanged(metadata)
            updateMediaFromController(activeMediaController)
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            activeMediaController = null
            IslandStateManager.updateMediaData(null)
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
                componentName
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
        // Pick the first controller that is currently playing, or fallback to the first active one
        val preferred = controllers?.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers?.firstOrNull()

        if (preferred != activeMediaController) {
            cleanupMediaController()
            activeMediaController = preferred
            preferred?.registerCallback(mediaControllerCallback)
        }

        updateMediaFromController(preferred)
    }

    private fun cleanupMediaController() {
        activeMediaController?.unregisterCallback(mediaControllerCallback)
        activeMediaController = null
    }

    private fun updateMediaFromController(controller: MediaController?) {
        if (controller == null) {
            // No media controller active, do not overwrite if simulator is driving it
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
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

        // Ignore ongoing background service notifications (e.g. download in progress, pedometer) unless flagged
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

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

        IslandStateManager.postNotification(notificationData)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // If current active notification matches, we can let the timer or user handle dismissal
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
