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

        // Collapse to Idle directly
        IslandStateManager.expandMedia()
        assertEquals(IslandMode.MEDIA_EXPANDED, IslandStateManager.islandMode.value)
        IslandStateManager.collapseToIdle()
        assertEquals(IslandMode.IDLE, IslandStateManager.islandMode.value)
    }
}

