package com.example.ui.companion

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurCircular
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.model.GlassBlurEffect
import com.example.model.IslandMode
import com.example.model.IslandTheme
import com.example.service.IslandOverlayService
import com.example.state.IslandStateManager
import com.example.ui.island.DynamicIsland

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val theme by IslandStateManager.islandTheme.collectAsState()
    val blurEffect by IslandStateManager.blurEffect.collectAsState()
    val xOffsetDp by IslandStateManager.xOffsetDp.collectAsState()
    val yOffsetDp by IslandStateManager.yOffsetDp.collectAsState()
    val isOverlayRunning by IslandStateManager.isOverlayRunning.collectAsState()
    val islandMode by IslandStateManager.islandMode.collectAsState()
    val autoHideInFullScreenVideo by IslandStateManager.autoHideInFullScreenVideo.collectAsState()
    val isFullScreenVideoActive by IslandStateManager.isFullScreenVideoActive.collectAsState()
    val shouldHideIsland by IslandStateManager.shouldHideIsland.collectAsState()

    // Permission tracking state refreshed on app resume
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasNotificationAccess by remember {
        mutableStateOf(
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        )
    }

    val postNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
                hasNotificationAccess = NotificationManagerCompat.getEnabledListenerPackages(context)
                    .contains(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0C0E14)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Dynamic Island Studio",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0C0E14)
                )
            )

            // 1. Live Interactive Island Canvas / Preview Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141722))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE CAPSULE PREVIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9CA3AF),
                            letterSpacing = 1.2.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isOverlayRunning) Color(0x3322C55E) else Color(0x336B7280)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isOverlayRunning) "OVERLAY ACTIVE" else "STANDBY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverlayRunning) Color(0xFF4ADE80) else Color(0xFF9CA3AF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone screen header simulation area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1E2232), Color(0xFF161926))
                                )
                            )
                            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(18.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                // Tapping outside capsule in preview collapses it back (mirrors WindowManager ACTION_OUTSIDE)
                                if (islandMode == IslandMode.MEDIA_EXPANDED || islandMode == IslandMode.NOTIFICATION_EXPANDED) {
                                    IslandStateManager.collapseToPill()
                                } else if (islandMode == IslandMode.NOTIFICATION) {
                                    IslandStateManager.dismissNotificationBanner()
                                }
                            },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Simulated Punch hole indicator line/dot
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(width = 16.dp, height = 16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F121C))
                                .border(1.dp, Color(0x44FFFFFF), CircleShape)
                        )

                        // The actual animated Dynamic Island with live X & Y offsets applied
                        Box(
                            modifier = Modifier
                                .offset(x = xOffsetDp.dp, y = (yOffsetDp + 6).dp)
                        ) {
                            DynamicIsland(isInteractive = true)
                        }

                        // Status notification when island is auto-hidden for full-screen video
                        if (shouldHideIsland) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xE60F172A))
                                    .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Island Hidden (Full-Screen Video Mode Active)",
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tip: Tap the capsule above to expand media, or swipe controls.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // 2. Master Overlay Service Toggle Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOverlayRunning) Color(0xFF13231B) else Color(0xFF161924)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isOverlayRunning) Color(0x6622C55E) else Color(0x1FFFFFFF)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dynamic Island Overlay",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isOverlayRunning) {
                                "Floating over all applications seamlessly"
                            } else {
                                "Start service to display floating capsule"
                            },
                            fontSize = 12.sp,
                            color = if (isOverlayRunning) Color(0xFF86EFAC) else Color(0xFF9CA3AF)
                        )
                    }

                    Switch(
                        checked = isOverlayRunning,
                        onCheckedChange = { start ->
                            if (start) {
                                if (Settings.canDrawOverlays(context)) {
                                    IslandOverlayService.start(context)
                                } else {
                                    openOverlaySettings(context)
                                }
                            } else {
                                IslandOverlayService.stop(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF22C55E)
                        ),
                        modifier = Modifier.testTag("overlay_toggle_switch")
                    )
                }
            }

            // 3. Permissions Section
            Text(
                text = "SYSTEM PERMISSIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 6.dp)
            )

            // Draw Over Other Apps Card
            PermissionStatusCard(
                title = "Draw Over Other Apps",
                description = "Required to display the Dynamic Island over apps.",
                isGranted = hasOverlayPermission,
                actionLabel = "Grant Permission",
                onAction = { openOverlaySettings(context) },
                icon = Icons.Default.Layers
            )

            // Notification & Media Access Card
            PermissionStatusCard(
                title = "Notification & Media Access",
                description = "Intercepts notifications & Spotify media sessions.",
                isGranted = hasNotificationAccess,
                actionLabel = "Enable Access",
                onAction = { openNotificationListenerSettings(context) },
                icon = Icons.Default.Notifications
            )

            // 4. Visual Themes Section (Requirement 3)
            Text(
                text = "VISUAL THEME",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Classic Black Style
                ThemeSelectionCard(
                    title = "Classic Black",
                    subtitle = "Solid OLED black",
                    isSelected = theme == IslandTheme.CLASSIC_BLACK,
                    icon = Icons.Default.DarkMode,
                    modifier = Modifier.weight(1f),
                    onSelect = { IslandStateManager.setTheme(IslandTheme.CLASSIC_BLACK) }
                )

                // Lucid (Liquid Glass) Style
                ThemeSelectionCard(
                    title = "Lucid Glass",
                    subtitle = "Liquid translucent",
                    isSelected = theme == IslandTheme.LUCID,
                    icon = Icons.Default.Tune,
                    modifier = Modifier.weight(1f),
                    onSelect = { IslandStateManager.setTheme(IslandTheme.LUCID) }
                )
            }

            // 4b. Glassmorphism Blur Radius Setting (Light vs Deep)
            Text(
                text = "GLASSMORPHISM BLUR RADIUS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Light Blur Effect Option
                BlurSelectionCard(
                    title = "Light Blur",
                    badge = "6 dp",
                    subtitle = "Crisp, subtle frosted translucency",
                    isSelected = blurEffect == GlassBlurEffect.LIGHT,
                    icon = Icons.Default.BlurOn,
                    modifier = Modifier.weight(1f),
                    onSelect = {
                        IslandStateManager.setBlurEffect(GlassBlurEffect.LIGHT)
                        if (theme != IslandTheme.LUCID) {
                            IslandStateManager.setTheme(IslandTheme.LUCID)
                        }
                    }
                )

                // Deep Blur Effect Option
                BlurSelectionCard(
                    title = "Deep Blur",
                    badge = "20 dp",
                    subtitle = "Rich, velvety heavy diffusion",
                    isSelected = blurEffect == GlassBlurEffect.DEEP,
                    icon = Icons.Default.BlurCircular,
                    modifier = Modifier.weight(1f),
                    onSelect = {
                        IslandStateManager.setBlurEffect(GlassBlurEffect.DEEP)
                        if (theme != IslandTheme.LUCID) {
                            IslandStateManager.setTheme(IslandTheme.LUCID)
                        }
                    }
                )
            }

            // 5. Island Alignment Calibration (X & Y Axis)
            Text(
                text = "CAMERA CUTOUT ALIGNMENT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 6.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161924))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Horizontal X-Offset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Horizontal Offset (X-Axis)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        val xLabel = when {
                            xOffsetDp == 0 -> "0 dp (Centered)"
                            xOffsetDp > 0 -> "+$xOffsetDp dp (Right)"
                            else -> "$xOffsetDp dp (Left)"
                        }
                        Text(
                            text = xLabel,
                            color = Color(0xFF60A5FA),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = xOffsetDp.toFloat(),
                        onValueChange = { IslandStateManager.setXOffsetDp(it.toInt()) },
                        valueRange = -60f..60f,
                        steps = 120,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF3B82F6),
                            activeTrackColor = Color(0xFF3B82F6),
                            inactiveTrackColor = Color(0xFF282F45)
                        ),
                        modifier = Modifier.testTag("x_offset_slider")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Vertical Y-Offset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vertical Offset (Y-Axis)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$yOffsetDp dp",
                            color = Color(0xFF60A5FA),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = yOffsetDp.toFloat(),
                        onValueChange = { IslandStateManager.setYOffsetDp(it.toInt()) },
                        valueRange = 0f..60f,
                        steps = 60,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF3B82F6),
                            activeTrackColor = Color(0xFF3B82F6),
                            inactiveTrackColor = Color(0xFF282F45)
                        ),
                        modifier = Modifier.testTag("y_offset_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Moves physical overlay via WindowManager.updateViewLayout in real time.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(
                            onClick = { IslandStateManager.resetOffsets() },
                            modifier = Modifier.testTag("reset_offsets_button")
                        ) {
                            Text(
                                text = "Reset",
                                color = Color(0xFF60A5FA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 6. Full-Screen Video Auto-Hide Setting
            Text(
                text = "FULL-SCREEN VIDEO PLAYBACK",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 6.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161924))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Full-Screen Video Setting",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Hide in Full-Screen Video",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Automatically hides the Dynamic Island during full-screen video playback to ensure it does not obstruct media content.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = autoHideInFullScreenVideo,
                        onCheckedChange = { enabled ->
                            IslandStateManager.setAutoHideInFullScreenVideo(enabled, context)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF3B82F6)
                        ),
                        modifier = Modifier.testTag("switch_auto_hide_video")
                    )
                }
            }

            // 7. Interactive State Simulator (Requirement 5)
            Text(
                text = "TEST SIMULATOR",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 6.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161924))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Trigger Island Behaviors",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Test how the capsule smoothly morphs and animates across states.",
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { IslandStateManager.simulateSpotifyPlaying(true) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_simulate_music"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color(0xFF22C55E)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Music", fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = { IslandStateManager.simulateNotification() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_simulate_notification"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color(0xFF38BDF8)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Notify", fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = { IslandStateManager.simulateIdle() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_simulate_idle"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF1E293B),
                                contentColor = Color(0xFF9CA3AF)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Idle", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FilledTonalButton(
                        onClick = { IslandStateManager.toggleFullScreenVideoSimulation() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_simulate_fullscreen_video"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isFullScreenVideoActive) Color(0xFF1E3A8A) else Color(0xFF1E293B),
                            contentColor = if (isFullScreenVideoActive) Color(0xFF60A5FA) else Color(0xFFCBD5E1)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFullScreenVideoActive) "Video Playing (Active: Tap to Exit)" else "Simulate Full-Screen Video Playback",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    icon: ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161924))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) Color(0x2222C55E) else Color(0x22EF4444)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF22C55E) else Color(0xFFF87171),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )
            }

            if (!isGranted) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Grant", fontSize = 12.sp)
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x2222C55E))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Active",
                        color = Color(0xFF4ADE80),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFF3B82F6) else Color(0x1AFFFFFF),
                shape = RoundedCornerShape(18.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF161924)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFF2563EB) else Color(0x22FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun BlurSelectionCard(
    title: String,
    badge: String,
    subtitle: String,
    isSelected: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFF38BDF8) else Color(0x1AFFFFFF),
                shape = RoundedCornerShape(18.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF0F243A) else Color(0xFF161924)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF0284C7) else Color(0x22FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0x3338BDF8) else Color(0x1AFFFFFF))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF7DD3FC) else Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

private fun openOverlaySettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

private fun openNotificationListenerSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
