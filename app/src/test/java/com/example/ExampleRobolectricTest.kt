package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.IslandMode
import com.example.model.IslandTheme
import com.example.state.IslandStateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Dynamic Island", appName)
    }

    @Test
    fun `test island state manager transitions`() {
        // Reset to idle
        IslandStateManager.simulateIdle()
        assertEquals(IslandMode.IDLE, IslandStateManager.islandMode.value)

        // Simulate Spotify playing
        IslandStateManager.simulateSpotifyPlaying(true)
        assertEquals(IslandMode.MEDIA_COMPACT, IslandStateManager.islandMode.value)
        assertNotNull(IslandStateManager.mediaData.value)
        assertTrue(IslandStateManager.mediaData.value?.isPlaying == true)

        // Expand media
        IslandStateManager.expandMedia()
        assertEquals(IslandMode.MEDIA_EXPANDED, IslandStateManager.islandMode.value)

        // Collapse to pill
        IslandStateManager.collapseToPill()
        assertEquals(IslandMode.MEDIA_COMPACT, IslandStateManager.islandMode.value)

        // Post notification
        IslandStateManager.simulateNotification("Team", "Meeting started")
        assertEquals(IslandMode.NOTIFICATION, IslandStateManager.islandMode.value)
        assertNotNull(IslandStateManager.activeNotification.value)

        // Dismiss notification
        IslandStateManager.dismissNotificationBanner()
        assertEquals(IslandMode.MEDIA_COMPACT, IslandStateManager.islandMode.value)

        // Change Theme
        IslandStateManager.setTheme(IslandTheme.LUCID)
        assertEquals(IslandTheme.LUCID, IslandStateManager.islandTheme.value)

        // Change Y Offset
        IslandStateManager.setYOffsetDp(24)
        assertEquals(24, IslandStateManager.yOffsetDp.value)

        // Change X Offset
        IslandStateManager.setXOffsetDp(18)
        assertEquals(18, IslandStateManager.xOffsetDp.value)

        // Reset Offsets
        IslandStateManager.resetOffsets()
        assertEquals(0, IslandStateManager.xOffsetDp.value)
        assertEquals(12, IslandStateManager.yOffsetDp.value)

        // Expanding media and collapsing returns to MEDIA_COMPACT because media is still active
        IslandStateManager.expandMedia()
        assertEquals(IslandMode.MEDIA_EXPANDED, IslandStateManager.islandMode.value)
        IslandStateManager.collapseOnOutsideTap()
        assertEquals(IslandMode.MEDIA_COMPACT, IslandStateManager.islandMode.value)

        // Clearing media reverts to Idle
        IslandStateManager.clearMediaData()
        assertEquals(IslandMode.IDLE, IslandStateManager.islandMode.value)
    }

    @Test
    fun `test full-screen video auto-hide configuration and behavior`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Initialize preferences
        IslandStateManager.initPreferences(context)

        // Enable auto-hide
        IslandStateManager.setAutoHideInFullScreenVideo(true, context)
        assertTrue(IslandStateManager.autoHideInFullScreenVideo.value)

        // When no full-screen video is active, island should not be hidden
        IslandStateManager.setFullScreenVideoActive(false)
        assertEquals(false, IslandStateManager.shouldHideIsland.value)

        // When full-screen video starts, island should automatically be hidden
        IslandStateManager.setFullScreenVideoActive(true)
        assertTrue(IslandStateManager.isFullScreenVideoActive.value)
        assertTrue(IslandStateManager.shouldHideIsland.value)

        // When user disables the auto-hide setting, island should not be hidden even during video
        IslandStateManager.setAutoHideInFullScreenVideo(false, context)
        assertEquals(false, IslandStateManager.autoHideInFullScreenVideo.value)
        assertEquals(false, IslandStateManager.shouldHideIsland.value)

        // Test persistence: re-initialize from context
        IslandStateManager.initPreferences(context)
        assertEquals(false, IslandStateManager.autoHideInFullScreenVideo.value)

        // Re-enable and verify persistence
        IslandStateManager.setAutoHideInFullScreenVideo(true, context)
        assertTrue(IslandStateManager.autoHideInFullScreenVideo.value)
        assertTrue(IslandStateManager.shouldHideIsland.value)

        // When video ends, island becomes visible again
        IslandStateManager.setFullScreenVideoActive(false)
        assertEquals(false, IslandStateManager.shouldHideIsland.value)
    }

    @Test
    fun `test media player remains active when paused`() {
        IslandStateManager.simulateIdle()
        assertEquals(IslandMode.IDLE, IslandStateManager.islandMode.value)

        // Start media playing
        IslandStateManager.simulateSpotifyPlaying(isPlaying = true)
        assertEquals(IslandMode.MEDIA_COMPACT, IslandStateManager.islandMode.value)
        assertTrue(IslandStateManager.mediaData.value?.isPlaying == true)

        // Expand media player
        IslandStateManager.expandMedia()
        assertEquals(IslandMode.MEDIA_EXPANDED, IslandStateManager.islandMode.value)

        // User interacts with Pause button: toggles play/pause
        IslandStateManager.togglePlayPause()
        // Media data is still present and active, now paused
        assertNotNull(IslandStateManager.mediaData.value)
        assertEquals(false, IslandStateManager.mediaData.value?.isPlaying)
        // Island does NOT disappear or drop to Idle! It stays expanded
        assertEquals(IslandMode.MEDIA_EXPANDED, IslandStateManager.islandMode.value)

        // Collapsing to pill keeps the media pill active (shows play icon / paused visualizer)
        IslandStateManager.collapseToPill()
        assertEquals(IslandMode.MEDIA_COMPACT, IslandStateManager.islandMode.value)
        assertNotNull(IslandStateManager.mediaData.value)
        assertEquals(false, IslandStateManager.mediaData.value?.isPlaying)

        // Only explicit clearMediaData (e.g. STATE_STOPPED / session destroyed) clears the media
        IslandStateManager.clearMediaData()
        assertEquals(IslandMode.IDLE, IslandStateManager.islandMode.value)
        assertEquals(null, IslandStateManager.mediaData.value)
    }

    @Test
    fun `test outside tap collapses notification to idle when no media active`() {
        IslandStateManager.simulateIdle()
        assertEquals(IslandMode.IDLE, IslandStateManager.islandMode.value)

        // Post notification
        IslandStateManager.simulateNotification("Alert", "Important message")
        assertEquals(IslandMode.NOTIFICATION, IslandStateManager.islandMode.value)

        // Outside tap on notification capsule collapses directly to Idle when no media is active
        IslandStateManager.collapseOnOutsideTap()
        assertEquals(IslandMode.IDLE, IslandStateManager.islandMode.value)
        assertEquals(null, IslandStateManager.activeNotification.value)
    }

    @Test
    fun `test outside tap on expanded media collapses back to compact media pill when playing or paused`() {
        IslandStateManager.simulateIdle()
        assertEquals(IslandMode.IDLE, IslandStateManager.islandMode.value)

        // 1. Music Playing
        IslandStateManager.simulateSpotifyPlaying(isPlaying = true)
        assertEquals(IslandMode.MEDIA_COMPACT, IslandStateManager.islandMode.value)

        // Expand media player
        IslandStateManager.expandMedia()
        assertEquals(IslandMode.MEDIA_EXPANDED, IslandStateManager.islandMode.value)

        // Tapping outside MUST return to compact media pill, NOT idle!
        IslandStateManager.collapseOnOutsideTap()
        assertEquals(IslandMode.MEDIA_COMPACT, IslandStateManager.islandMode.value)
        assertNotNull(IslandStateManager.mediaData.value)

        // 2. Music Paused
        IslandStateManager.expandMedia()
        assertEquals(IslandMode.MEDIA_EXPANDED, IslandStateManager.islandMode.value)
        IslandStateManager.togglePlayPause()
        assertEquals(false, IslandStateManager.mediaData.value?.isPlaying)

        // Tapping outside while paused MUST STILL return to compact media pill!
        IslandStateManager.collapseOnOutsideTap()
        assertEquals(IslandMode.MEDIA_COMPACT, IslandStateManager.islandMode.value)
        assertNotNull(IslandStateManager.mediaData.value)
        assertEquals(false, IslandStateManager.mediaData.value?.isPlaying)

        // 3. Only when NO media is active does outside tap go to Idle
        IslandStateManager.clearMediaData()
        assertEquals(IslandMode.IDLE, IslandStateManager.islandMode.value)
        IslandStateManager.collapseOnOutsideTap()
        assertEquals(IslandMode.IDLE, IslandStateManager.islandMode.value)
    }

    @Test
    fun `test outside tap on notification restores compact media when music is active`() {
        IslandStateManager.simulateIdle()
        IslandStateManager.simulateSpotifyPlaying(isPlaying = true)
        assertEquals(IslandMode.MEDIA_COMPACT, IslandStateManager.islandMode.value)

        IslandStateManager.simulateNotification("WhatsApp", "New message")
        assertEquals(IslandMode.NOTIFICATION, IslandStateManager.islandMode.value)

        // Tapping outside dismisses notification and restores active media pill
        IslandStateManager.collapseOnOutsideTap()
        assertEquals(IslandMode.MEDIA_COMPACT, IslandStateManager.islandMode.value)
        assertEquals(null, IslandStateManager.activeNotification.value)
    }
}

