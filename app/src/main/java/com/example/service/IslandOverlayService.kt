package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.model.IslandMode
import com.example.state.IslandStateManager
import com.example.ui.island.DynamicIsland
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Foreground service that hosts the floating Dynamic Island overlay via WindowManager.
 *
 * CRITICAL REQUIREMENTS:
 * - LayoutParams strictly WRAP_CONTENT for both width & height (NEVER fullscreen).
 * - FLAG_NOT_FOCUSABLE and FLAG_WATCH_OUTSIDE_TOUCH for seamless pass-through outside capsule.
 */
class IslandOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "IslandOverlayService onCreate")

        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Overlay permission not granted. Stopping service.")
            stopSelf()
            return
        }

        createNotificationChannel()
        startForegroundWithNotification()

        setupOverlay()
        observeSettings()

        IslandStateManager.setOverlayRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (windowManager == null) {
            stopSelf()
            return
        }

        // Initialize LifecycleOwner for Compose inside Service
        lifecycleOwner = OverlayLifecycleOwner().apply {
            onCreate()
            onStart()
        }

        // Strict WindowManager Constraints per instructions:
        // LayoutParams MUST strictly be WRAP_CONTENT for both width and height.
        // FLAG_NOT_FOCUSABLE and FLAG_WATCH_OUTSIDE_TOUCH so touches outside pass through.
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val initialY = dpToPx(IslandStateManager.yOffsetDp.value)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = initialY
        }

        this.layoutParams = params

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)

            setContent {
                MyApplicationTheme(darkTheme = true) {
                    DynamicIsland(isInteractive = true)
                }
            }

            // Outside touch listener: when user touches outside the expanded island, collapse to pill!
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    if (IslandStateManager.islandMode.value == IslandMode.MEDIA_EXPANDED) {
                        IslandStateManager.collapseToPill()
                        return@setOnTouchListener true
                    }
                }
                false
            }
        }

        try {
            windowManager?.addView(composeView, params)
            Log.d(TAG, "Dynamic Island overlay view added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add WindowManager view", e)
            stopSelf()
        }
    }

    private fun observeSettings() {
        // Adjust Y-position dynamically when user tweaks slider in companion app
        IslandStateManager.yOffsetDp
            .onEach { offsetDp ->
                layoutParams?.let { params ->
                    val newY = dpToPx(offsetDp)
                    if (params.y != newY && composeView?.isAttachedToWindow == true) {
                        params.y = newY
                        windowManager?.updateViewLayout(composeView, params)
                    }
                }
            }
            .launchIn(serviceScope)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, IslandOverlayService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_content_title))
            .setContentText(getString(R.string.notification_content_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(0, "Stop Overlay", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "IslandOverlayService onDestroy")

        IslandStateManager.setOverlayRunning(false)
        serviceScope.cancel()

        try {
            if (composeView != null && composeView?.isAttachedToWindow == true) {
                windowManager?.removeViewImmediate(composeView)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay view", e)
        }

        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        composeView = null
    }

    companion object {
        private const val TAG = "IslandOverlayService"
        private const val CHANNEL_ID = "dynamic_island_overlay_channel"
        private const val NOTIFICATION_ID = 2024
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_OVERLAY"

        fun start(context: Context) {
            val intent = Intent(context, IslandOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, IslandOverlayService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
