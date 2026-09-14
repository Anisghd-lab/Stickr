package com.stickr.app.core.provider

import android.content.Context
import android.content.pm.ProviderInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StickerContentProviderTest {

    private lateinit var provider: StickerContentProvider
    private lateinit var context: Context
    private val testAuthority = "com.stickr.app.stickercontentprovider"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        provider = StickerContentProvider()
        val info = ProviderInfo().apply {
            authority = testAuthority
        }
        provider.attachInfo(context, info)
    }

    @Test
    fun `UriMatcher resolves metadata route correctly`() {
        val uri = Uri.parse("content://$testAuthority/metadata")
        val type = provider.getType(uri)

        assertEquals("vnd.android.cursor.dir/vnd.$testAuthority.metadata", type)
    }

    @Test
    fun `UriMatcher resolves single pack metadata route correctly`() {
        val uri = Uri.parse("content://$testAuthority/metadata/42")
        val type = provider.getType(uri)

        assertEquals("vnd.android.cursor.item/vnd.$testAuthority.metadata", type)
    }

    @Test
    fun `UriMatcher resolves stickers route correctly`() {
        val uri = Uri.parse("content://$testAuthority/stickers/42")
        val type = provider.getType(uri)

        assertEquals("vnd.android.cursor.dir/vnd.$testAuthority.stickers", type)
    }

    @Test
    fun `UriMatcher resolves sticker asset route as image_webp`() {
        val uri = Uri.parse("content://$testAuthority/stickers_asset/42/sticker_1.webp")
        val type = provider.getType(uri)

        assertEquals("image/webp", type)
    }

    @Test
    fun `UriMatcher resolves tray asset route as image_png`() {
        val uri = Uri.parse("content://$testAuthority/tray_asset/42")
        val type = provider.getType(uri)

        assertEquals("image/png", type)
    }

    @Test
    fun `StickerContentProviderContract builds conforming URIs`() {
        assertEquals(
            "content://$testAuthority/metadata",
            StickerContentProviderContract.getMetadataUri(context).toString()
        )
        assertEquals(
            "content://$testAuthority/metadata/5",
            StickerContentProviderContract.getPackMetadataUri(context, "5").toString()
        )
        assertEquals(
            "content://$testAuthority/stickers/5",
            StickerContentProviderContract.getStickersUri(context, "5").toString()
        )
        assertEquals(
            "content://$testAuthority/stickers_asset/5/sticker.webp",
            StickerContentProviderContract.getStickerAssetUri(context, "5", "sticker.webp").toString()
        )
        assertEquals(
            "content://$testAuthority/tray_asset/5",
            StickerContentProviderContract.getTrayAssetUri(context, "5").toString()
        )
    }
}
