package com.goveye.app.ui.navigation

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeepLinkHandlerTest {

    private fun makeIntent(uri: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(uri))

    @Test
    fun `parses mp deep link with memberId`() {
        val intent = makeIntent("goveye://mp/172")
        val route = DeepLinkHandler.parseDeepLink(intent)
        assertTrue(route is MpProfileRoute)
        assertEquals(172, (route as MpProfileRoute).memberId)
    }

    @Test
    fun `parses mp deep link without memberId as DirectoryRoute`() {
        val intent = makeIntent("goveye://mp")
        val route = DeepLinkHandler.parseDeepLink(intent)
        assertEquals(DirectoryRoute, route)
    }

    @Test
    fun `parses division deep link as FeedRoute`() {
        val intent = makeIntent("goveye://division")
        val route = DeepLinkHandler.parseDeepLink(intent)
        assertEquals(FeedRoute, route)
    }

    @Test
    fun `returns null for unknown host`() {
        val intent = makeIntent("goveye://unknown")
        val route = DeepLinkHandler.parseDeepLink(intent)
        assertNull(route)
    }

    @Test
    fun `returns null for wrong scheme`() {
        val intent = makeIntent("https://mp/172")
        val route = DeepLinkHandler.parseDeepLink(intent)
        assertNull(route)
    }

    @Test
    fun `returns null for null data`() {
        val intent = Intent(Intent.ACTION_VIEW)
        val route = DeepLinkHandler.parseDeepLink(intent)
        assertNull(route)
    }

    @Test
    fun `handles non-numeric memberId gracefully`() {
        val intent = makeIntent("goveye://mp/abc")
        val route = DeepLinkHandler.parseDeepLink(intent)
        assertEquals(DirectoryRoute, route)
    }
}
