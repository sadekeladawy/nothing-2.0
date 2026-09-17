package com.example.ui.island

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val mode by IslandStateManager.islandMode.collectAsState()
    val mediaData by IslandStateManager.mediaData.collectAsState()
    val notificationData by IslandStateManager.activeNotification.collectAsState()
    val theme by IslandStateManager.islandTheme.collectAsState()

    // Authentic iPhone dynamic island spring physics
    val bouncySpring = spring<androidx.compose.ui.unit.IntSize>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Dynamic Corner Radius depending on state
    val cornerRadius = when (mode) {
        IslandMode.MEDIA_EXPANDED -> 36.dp
        IslandMode.NOTIFICATION -> 26.dp
        else -> 22.dp
    }

    val capsuleShape = RoundedCornerShape(cornerRadius)

    // Capsule styling according to selected visual theme
    val isLucid = theme == IslandTheme.LUCID

    val backgroundBrush = if (isLucid) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x991E2230),
                Color(0x80131622)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0A0A0A),
                Color(0xFF000000)
            )
        )
    }

    val borderBrush = if (isLucid) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x66FFFFFF),
                Color(0x22FFFFFF),
                Color(0x0DFFFFFF)
            )
        )
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
            .testTag("dynamic_island_container"),
        contentAlignment = Alignment.TopCenter
    ) {
        // Shadow/glow aura
        Box(
            modifier = Modifier
                .wrapContentSize()
                .shadow(
                    elevation = if (isLucid) 12.dp else 8.dp,
                    shape = capsuleShape,
                    ambientColor = if (isLucid) Color(0x663B82F6) else Color(0xAA000000),
                    spotColor = if (isLucid) Color(0x6660A5FA) else Color(0xFF000000)
                )
        ) {
            // Main Glass/Capsule Body
            Box(
                modifier = Modifier
                    .clip(capsuleShape)
                    .then(
                        if (isLucid) {
                            Modifier.blur(1.dp) // Subtle Compose-level soft diffusion
                        } else {
                            Modifier
                        }
                    )
                    .background(brush = backgroundBrush)
                    .border(
                        width = if (isLucid) 1.2.dp else 0.8.dp,
                        brush = borderBrush,
                        shape = capsuleShape
                    )
                    .animateContentSize(animationSpec = bouncySpring)
            ) {
                // Specular liquid highlight for Lucid theme
                if (isLucid) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0x1FFFFFFF), Color.Transparent),
                                    radius = 350f
                                )
                            )
                    )
                }

                // Content transition between states
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        fadeIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) togetherWith fadeOut(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessHigh
                            )
                        )
                    },
                    label = "island_content_transition"
                ) { currentMode ->
                    when (currentMode) {
                        IslandMode.IDLE -> {
                            IdleCapsule(
                                onClick = {
                                    // Quick demo tap: trigger Spotify simulation if idle
                                    if (isInteractive) {
                                        IslandStateManager.simulateSpotifyPlaying(true)
                                    }
                                }
                            )
                        }
                        IslandMode.MEDIA_COMPACT -> {
                            CompactMediaCapsule(
                                media = mediaData,
                                onClick = {
                                    if (isInteractive) {
                                        IslandStateManager.expandMedia()
                                    }
                                }
                            )
                        }
                        IslandMode.NOTIFICATION -> {
                            NotificationCapsule(
                                notification = notificationData,
                                onClick = {
                                    if (isInteractive) {
                                        try {
                                            notificationData?.contentIntent?.send()
                                        } catch (_: Exception) {
                                        }
                                        IslandStateManager.dismissNotificationBanner()
                                    }
                                },
                                onDismiss = {
                                    if (isInteractive) {
                                        IslandStateManager.dismissNotificationBanner()
                                    }
                                }
                            )
                        }
                        IslandMode.MEDIA_EXPANDED -> {
                            ExpandedMediaIsland(
                                media = mediaData,
                                onCollapse = {
                                    if (isInteractive) {
                                        IslandStateManager.collapseToPill()
                                    }
                                },
                                onCardClick = {
                                    if (isInteractive) {
                                        try {
                                            mediaData?.launchIntent?.send()
                                        } catch (_: Exception) {
                                        }
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
            .width(116.dp)
            .height(34.dp)
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
 */
@Composable
private fun CompactMediaCapsule(
    media: MediaData?,
    onClick: () -> Unit
) {
    val isPlaying = media?.isPlaying == true

    Row(
        modifier = Modifier
            .width(210.dp)
            .height(38.dp)
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(90.dp)
            )
        }

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
 */
@Composable
private fun NotificationCapsule(
    notification: NotificationData?,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(310.dp)
            .height(52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon / Avatar
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

        Spacer(modifier = Modifier.width(10.dp))

        // Message text
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = notification?.title ?: "Notification",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = notification?.text ?: "",
                color = Color(0xFFD1D5DB),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Dismiss cross button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = Color(0x99FFFFFF),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * 4. Expanded Media Player State: Full dynamic island media card.
 */
@Composable
private fun ExpandedMediaIsland(
    media: MediaData?,
    onCollapse: () -> Unit,
    onCardClick: () -> Unit
) {
    val isPlaying = media?.isPlaying == true

    Column(
        modifier = Modifier
            .width(330.dp)
            .wrapContentHeight()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: Album Art, Track Info, and Collapse Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCardClick
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
                        contentDescription = "Spotify",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Track & Artist Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media?.title ?: "Unknown Track",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = media?.artist ?: "Unknown Artist",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Collapse action
            IconButton(
                onClick = onCollapse,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Collapse Capsule",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Progress Bar
        Column(modifier = Modifier.fillMaxWidth()) {
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

        // Transport Controls Row: Previous, Play/Pause, Next, Sound Visualizer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Equalizer icon/bars on bottom-left
            AudioVisualizer(
                isPlaying = isPlaying,
                barCount = 4,
                barWidth = 3.dp,
                maxHeight = 16.dp,
                minHeight = 4.dp
            )

            // Center playback buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = { IslandStateManager.skipPrevious() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Play / Pause big button with spring scale feedback
                val playButtonScale by animateFloatAsState(
                    targetValue = if (isPlaying) 1.05f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "play_button_scale"
                )

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .scale(playButtonScale)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { IslandStateManager.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = { IslandStateManager.skipNext() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Launch app hint (e.g. Spotify)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x22FFFFFF))
                    .clickable(onClick = onCardClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "OPEN",
                    color = Color(0xFF60A5FA),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
