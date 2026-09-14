package com.stickr.app.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WhatsAppIntentHelperTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `official WhatsApp intent action and extra keys match specifications`() {
        assertEquals("com.whatsapp.intent.action.ENABLE_ADD_PACK", WhatsAppIntentHelper.ACTION_ENABLE_ADD_PACK)
        assertEquals("sticker_pack_id", WhatsAppIntentHelper.EXTRA_STICKER_PACK_ID)
        assertEquals("sticker_pack_authority", WhatsAppIntentHelper.EXTRA_STICKER_PACK_AUTHORITY)
        assertEquals("sticker_pack_name", WhatsAppIntentHelper.EXTRA_STICKER_PACK_NAME)
    }

    @Test
    fun `package types target consumer and business WhatsApp apps`() {
        assertEquals("com.whatsapp", WhatsAppPackage.CONSUMER.packageName)
        assertEquals("com.whatsapp.w4b", WhatsAppPackage.BUSINESS.packageName)
    }

    @Test
    fun `launchAddToWhatsAppIntent returns failure with ActivityNotFoundException when not installed`() {
        val result = WhatsAppIntentHelper.launchAddToWhatsAppIntent(
            context = context,
            packId = "pack_1",
            packName = "Test Pack"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ActivityNotFoundException)
    }
}
