package com.example.ui.island

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GlassBlurEffect
import com.example.model.IslandMode
import com.example.model.IslandTheme
import com.example.model.MediaData
import com.example.model.NotificationData
import com.example.state.IslandStateManager
import com.example.ui.components.AudioVisualizer

/**
 * The Dynamic Island overlay composable with authentic iPhone spring physics
 * and smooth shape morphing.
 */
@Composable
fun DynamicIsland(
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true
) {
    val context = LocalContext.current
    val mode by IslandStateManager.islandMode.collectAsState()
    val mediaData by IslandStateManager.mediaData.collectAsState()
    val notificationData by IslandStateManager.activeNotification.collectAsState()
    val theme by IslandStateManager.islandTheme.collectAsState()
    val blurEffect by IslandStateManager.blurEffect.collectAsState()

    // Explicit Transition to coordinate bounds morphing independently of content layout
    val transition = updateTransition(targetState = mode, label = "dynamic_island_transition")

    // Spring specification for authentic Apple fluid physics: dampingRatio = 0.6f, stiffness = 300f
    val springSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.6f,
        stiffness = 300f
    )

    val capsuleWidth by transition.animateDp(
        transitionSpec = { springSpec },
        label = "capsule_width"
    ) { targetMode ->
        when (targetMode) {
            IslandMode.IDLE -> 116.dp
            IslandMode.MEDIA_COMPACT -> 216.dp
            IslandMode.NOTIFICATION -> 312.dp
            IslandMode.NOTIFICATION_EXPANDED -> 344.dp
            IslandMode.MEDIA_EXPANDED -> 344.dp
        }
    }

    val capsuleHeight by transition.animateDp(
        transitionSpec = { springSpec },
        label = "capsule_height"
    ) { targetMode ->
        when (targetMode) {
            IslandMode.IDLE -> 34.dp
            IslandMode.MEDIA_COMPACT -> 38.dp
            IslandMode.NOTIFICATION -> 52.dp
            IslandMode.NOTIFICATION_EXPANDED -> if ((notificationData?.badgeCount ?: 1) > 1) 184.dp else 156.dp
            IslandMode.MEDIA_EXPANDED -> 192.dp
        }
    }

    val cornerRadius by transition.animateDp(
        transitionSpec = {
            spring(
                dampingRatio = 0.6f,
                stiffness = 300f
            )
        },
        label = "capsule_corner_radius"
    ) { targetMode ->
        when (targetMode) {
            IslandMode.MEDIA_EXPANDED -> 36.dp
            IslandMode.NOTIFICATION_EXPANDED -> 30.dp
            IslandMode.NOTIFICATION -> 26.dp
            IslandMode.MEDIA_COMPACT -> 20.dp
            IslandMode.IDLE -> 17.dp
        }
    }

    val capsuleShape = RoundedCornerShape(cornerRadius)

    // Capsule styling according to selected visual theme & glassmorphism blur effect
    val isLucid = theme == IslandTheme.LUCID
    val isDeepBlur = blurEffect == GlassBlurEffect.DEEP

    val backgroundBrush = if (isLucid) {
        if (isDeepBlur) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xD91B2030), // Deep frosted glass backdrop
                    Color(0xC4111422)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x801E2230), // Light crisp glass translucency
                    Color(0x66131622)
                )
            )
        }
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0A0A0A),
                Color(0xFF000000)
            )
        )
    }

    val borderBrush = if (isLucid) {
        if (isDeepBlur) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x8CFFFFFF),
                    Color(0x38FFFFFF),
                    Color(0x14FFFFFF)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0x66FFFFFF),
                    Color(0x22FFFFFF),
                    Color(0x0DFFFFFF)
                )
            )
        }
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x33FFFFFF),
                Color(0x1AFFFFFF)
            )
        )
    }

    Box(
        modifier = modifier
            .wrapContentSize()
            .background(Color.Transparent)
            .testTag("dynamic_island_container"),
        contentAlignment = Alignment.TopCenter
    ) {
        // Shadow/glow aura
        Box(
            modifier = Modifier
                .wrapContentSize()
                .background(Color.Transparent)
                .shadow(
                    elevation = if (isLucid) (if (isDeepBlur) 14.dp else 10.dp) else 8.dp,
                    shape = capsuleShape,
                    ambientColor = if (isLucid) Color(0x663B82F6) else Color(0xAA000000),
                    spotColor = if (isLucid) Color(0x6660A5FA) else Color(0xFF000000)
                )
        ) {
            // Main Glass/Capsule Container: explicitly sized by updateTransition spring to eliminate WindowManager layout thrashing
            Box(
                modifier = Modifier
                    .size(width = capsuleWidth, height = capsuleHeight)
                    .clip(capsuleShape)
                    .border(
                        width = if (isLucid) (if (isDeepBlur) 1.5.dp else 1.0.dp) else 0.8.dp,
                        brush = borderBrush,
                        shape = capsuleShape
                    )
            ) {
                // Inner Glass Surface / Backdrop: STRICTLY and ONLY blur this backdrop Box, so the rest of the screen remains 100% clear!
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(capsuleShape)
                        .then(
                            if (isLucid) {
                                Modifier.blur(
                                    radius = blurEffect.blurRadiusDp.dp,
                                    edgeTreatment = BlurredEdgeTreatment.Rectangle
                                )
                            } else {
                                Modifier
                            }
                        )
                        .background(brush = backgroundBrush)
                )

                // Specular liquid highlight for Lucid theme
                if (isLucid) {
                    val specularAlpha = if (isDeepBlur) 0.28f else 0.16f
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(capsuleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = specularAlpha), Color.Transparent),
                                    radius = if (isDeepBlur) 420f else 350f
                                )
                            )
                    )
                }

                // Staggered crossfade: inner content transitions smoothly once capsule morph starts
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = 180,
                                delayMillis = 80,
                                easing = LinearOutSlowInEasing
                            )
                        ) togetherWith fadeOut(
                            animationSpec = tween(
                                durationMillis = 80,
                                easing = FastOutLinearInEasing
                            )
                        )
                    },
                    contentAlignment = Alignment.Center,
                    label = "island_content_transition"
                ) { currentMode ->
                    when (currentMode) {
                        IslandMode.IDLE -> {
                            IdleCapsule(
                                onClick = {
                                    if (isInteractive) {
                                        IslandStateManager.onCapsuleTapped()
                                    }
                                }
                            )
                        }
                        IslandMode.MEDIA_COMPACT -> {
                            CompactMediaCapsule(
                                media = mediaData,
                                onClick = {
                                    if (isInteractive) {
                                        IslandStateManager.onCapsuleTapped()
                                    }
                                }
                            )
                        }
                        IslandMode.NOTIFICATION -> {
                            NotificationCapsule(
                                notification = notificationData,
                                onClick = {
                                    if (isInteractive) {
                                        IslandStateManager.onCapsuleTapped()
                                    }
                                }
                            )
                        }
                        IslandMode.NOTIFICATION_EXPANDED -> {
                            ExpandedNotificationIsland(
                                notification = notificationData,
                                onOpenApp = {
                                    if (isInteractive) {
                                        launchSourceApp(
                                            context = context,
                                            pendingIntent = notificationData?.contentIntent,
                                            packageName = notificationData?.packageName
                                        )
                                        IslandStateManager.collapseToIdle()
                                    }
                                }
                            )
                        }
                        IslandMode.MEDIA_EXPANDED -> {
                            ExpandedMediaIsland(
                                media = mediaData,
                                onOpenApp = {
                                    if (isInteractive) {
                                        launchSourceApp(
                                            context = context,
                                            pendingIntent = mediaData?.launchIntent,
                                            packageName = mediaData?.packageName
                                        )
                                        IslandStateManager.collapseToIdle()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 1. Idle State: Compact camera cutout representation.
 */
@Composable
private fun IdleCapsule(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left camera lens dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF14171E))
                .border(1.dp, Color(0xFF282D3D), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A0C10))
            )
        }

        // Right sensor / faint dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0x33FFFFFF))
        )
    }
}

/**
 * 2. Media Compact State: Shows album art thumbnail and animated visualizer.
 * Fixed required width and clipToBounds prevent letter-by-letter typing jitter.
 */
@Composable
private fun CompactMediaCapsule(
    media: MediaData?,
    onClick: () -> Unit
) {
    val isPlaying = media?.isPlaying == true

    Row(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Album Art or Music disc
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f, fill = false)
                .clipToBounds()
        ) {
            if (media?.albumArt != null) {
                Image(
                    bitmap = media.albumArt.asImageBitmap(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0x44FFFFFF), CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFF22C55E),
                                    Color(0xFF3B82F6),
                                    Color(0xFFA855F7),
                                    Color(0xFF22C55E)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music Playing",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = media?.title ?: "Music",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 110.dp)
                    .clipToBounds()
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Right: Animated Sound Wave Visualizer
        AudioVisualizer(
            isPlaying = isPlaying,
            barCount = 4,
            barWidth = 2.5.dp,
            maxHeight = 15.dp,
            minHeight = 4.dp
        )
    }
}

/**
 * 3. Notification State: Bouncy banner expanding to reveal sender and snippet.
 * No explicit close button: tapping anywhere fires the notification intent and collapses to idle.
 * Tapping outside collapses via FLAG_WATCH_OUTSIDE_TOUCH.
 */
@Composable
private fun NotificationCapsule(
    notification: NotificationData?,
    onClick: () -> Unit
) {
    val badgeCount = notification?.badgeCount ?: 1

    Row(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon / Avatar with optional badge dot if > 1
        Box(
            modifier = Modifier.wrapContentSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            if (notification?.appIcon != null) {
                Image(
                    bitmap = notification.appIcon.asImageBitmap(),
                    contentDescription = "Notification Icon",
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF3B82F6), Color(0xFF6366F1))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notification",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (badgeCount > 1) {
                Box(
                    modifier = Modifier
                        .offset(x = 3.dp, y = (-2).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .border(1.5.dp, Color(0xFF0F172A), CircleShape)
                        .testTag("avatar_badge_indicator"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 9) "9+" else "$badgeCount",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Message text
        Column(
            modifier = Modifier
                .weight(1f)
                .clipToBounds(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = notification?.title ?: "Notification",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = notification?.text ?: "",
                color = Color(0xFFD1D5DB),
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Numerical badge indicator pill when multiple notifications arrive within 5 seconds
        if (badgeCount > 1) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                        )
                    )
                    .border(0.8.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("notification_badge_indicator"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$badgeCount",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * 4. Expanded Notification State: Detailed card revealing full sender, snippet, and action.
 * Uses AnimatedVisibility and delayed entrance to ensure the capsule smoothly morphs before content renders.
 */
@Composable
private fun ExpandedNotificationIsland(
    notification: NotificationData?,
    onOpenApp: () -> Unit
) {
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(70)
        contentVisible = true
    }

    AnimatedVisibility(
        visible = contentVisible,
        enter = fadeIn(animationSpec = tween(160, easing = LinearOutSlowInEasing)) +
                scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)),
        exit = fadeOut(animationSpec = tween(80))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Consumed: Card stays open when tapping inside */ }
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: App icon, Title / Sender name, and package badge / timestamp (tapping launches app)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenApp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (notification?.appIcon != null) {
                        Image(
                            bitmap = notification.appIcon.asImageBitmap(),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notification",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = notification?.title ?: "Notification",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = notification?.packageName?.substringAfterLast('.')
                                ?.replaceFirstChar { it.uppercase() } ?: "System",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                val badgeCount = notification?.badgeCount ?: 1
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (badgeCount > 1) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEF4444))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                .testTag("expanded_badge_indicator")
                        ) {
                            Text(
                                text = "$badgeCount alerts",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Time / status pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1FFFFFFF))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "now",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Body message text
            Text(
                text = notification?.text ?: "",
                color = Color(0xFFE2E8F0),
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = if (notification != null && notification.batchedNotifications.size > 1) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            )

            // Batched prior notifications list (if multiple arrived within 5 seconds)
            if (notification != null && notification.batchedNotifications.size > 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    notification.batchedNotifications.dropLast(1).takeLast(2).reversed().forEach { prev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x14FFFFFF))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${prev.title}: ${prev.text}",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Bottom action affordance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2563EB))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onOpenApp
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Open App",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 5. Expanded Media Player State: Full dynamic island media card.
 * - Uses AnimatedVisibility to smoothly reveal content after capsule bounds expand.
 * - Entire capsule area is clickable to launch the source app and collapse to idle.
 * - Close button and Open button removed for pure iOS feel.
 * - Tap outside dismisses via WindowManager FLAG_WATCH_OUTSIDE_TOUCH.
 */
@Composable
private fun ExpandedMediaIsland(
    media: MediaData?,
    onOpenApp: () -> Unit
) {
    val isPlaying = media?.isPlaying == true
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(70)
        contentVisible = true
    }

    AnimatedVisibility(
        visible = contentVisible,
        enter = fadeIn(animationSpec = tween(160, easing = LinearOutSlowInEasing)) +
                scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)),
        exit = fadeOut(animationSpec = tween(80))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Consumed: Island stays open when interacting with it */ }
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Row: Album Art and Track Info (Tapping explicitly on header launches the app)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenApp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Artwork
                if (media?.albumArt != null) {
                    Image(
                        bitmap = media.albumArt.asImageBitmap(),
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1DB954), Color(0xFF191414))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Music",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Track & Artist Column (clipToBounds + softWrap = false avoids any letter-by-letter jumping)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clipToBounds()
                ) {
                    Text(
                        text = media?.title ?: "Unknown Track",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = media?.artist ?: "Unknown Artist",
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Subtle music source indicator badge (no close button!)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Consume clicks */ }
                    )
            ) {
                val progress = if (media != null && media.durationMs > 0) {
                    (media.positionMs.toFloat() / media.durationMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0.35f
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF22C55E),
                    trackColor = Color(0x33FFFFFF)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(if (media != null) media.positionMs else 64_000L),
                        color = Color(0xFF6B7280),
                        fontSize = 10.sp
                    )
                    Text(
                        text = formatTime(if (media != null) media.durationMs else 200_000L),
                        color = Color(0xFF6B7280),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transport Controls Row: Waveform on left, centered buttons, audio route on right (No Open button!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Consume clicks */ }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Equalizer wave visualizer on bottom-left
                AudioVisualizer(
                    isPlaying = isPlaying,
                    barCount = 4,
                    barWidth = 3.dp,
                    maxHeight = 16.dp,
                    minHeight = 4.dp
                )

                // Center playback controls: Consume clicks completely to keep the island open
                Row(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Consume clicks */ }
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { IslandStateManager.skipPrevious() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Play / Pause big button with iOS bounce spring scale feedback
                    val playButtonScale by animateFloatAsState(
                        targetValue = if (isPlaying) 1.05f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = 0.6f,
                            stiffness = 300f
                        ),
                        label = "play_button_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .scale(playButtonScale)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { IslandStateManager.togglePlayPause() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { IslandStateManager.skipNext() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Audio route output indicator icon (e.g. AirPlay / Audio output)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Consume clicks */ }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Audio Output",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Fires the source app's PendingIntent or opens its launch intent via PackageManager.
 */
private fun launchSourceApp(
    context: Context,
    pendingIntent: PendingIntent?,
    packageName: String?
) {
    try {
        if (pendingIntent != null) {
            pendingIntent.send()
            return
        }
    } catch (_: Exception) {
    }

    if (!packageName.isNullOrBlank()) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (_: Exception) {
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
