package com.stickr.app.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.stickr.app.core.database.dao.StickerPackDao
import com.stickr.app.core.database.entity.StickerItemEntity
import com.stickr.app.core.database.entity.StickerPackEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StickerPackDaoTest {

    private lateinit var db: StickrDatabase
    private lateinit var dao: StickerPackDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, StickrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.stickerPackDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetPackWithStickers() = runBlocking {
        val pack = StickerPackEntity(
            id = "pack_1",
            name = "Memes Animaux",
            publisher = "Stickr Dev"
        )
        dao.insertPack(pack)

        val sticker1 = StickerItemEntity(
            id = "s_1",
            packId = "pack_1",
            imagePath = "/files/s1.webp",
            emojis = "🐶",
            orderIndex = 0
        )
        val sticker2 = StickerItemEntity(
            id = "s_2",
            packId = "pack_1",
            imagePath = "/files/s2.webp",
            emojis = "🐱",
            orderIndex = 1
        )
        dao.insertSticker(sticker1)
        dao.insertSticker(sticker2)

        val packWithStickers = dao.getPackWithStickersByIdSync("pack_1")

        assertNotNull(packWithStickers)
        assertEquals("Memes Animaux", packWithStickers!!.pack.name)
        assertEquals(2, packWithStickers.stickerCount)
        assertEquals("🐶", packWithStickers.stickers[0].emojis)
    }

    @Test
    fun deletePackCascadesToStickerItems() = runBlocking {
        val pack = StickerPackEntity(id = "pack_2", name = "Test", publisher = "User")
        dao.insertPack(pack)

        val sticker = StickerItemEntity(id = "s_3", packId = "pack_2", imagePath = "/files/s3.webp")
        dao.insertSticker(sticker)

        // Vérifier la présence
        assertEquals(1, dao.getStickersForPackSync("pack_2").size)

        // Supprimer le pack
        dao.deletePackById("pack_2")

        // La suppression en cascade doit avoir nettoyé les stickers
        assertNull(dao.getPackWithStickersByIdSync("pack_2"))
        assertTrue(dao.getStickersForPackSync("pack_2").isEmpty())
    }

    @Test
    fun updateStickerEmojis() = runBlocking {
        val pack = StickerPackEntity(id = "pack_3", name = "Emojis", publisher = "User")
        dao.insertPack(pack)

        val sticker = StickerItemEntity(id = "s_4", packId = "pack_3", imagePath = "/files/s4.webp", emojis = "✨")
        dao.insertSticker(sticker)

        dao.updateStickerEmojis("s_4", "🔥,😎")

        val updated = dao.getStickerById("s_4")
        assertNotNull(updated)
        assertEquals("🔥,😎", updated!!.emojis)
    }

    @Test
    fun deleteStickerById() = runBlocking {
        val pack = StickerPackEntity(id = "pack_4", name = "Single Delete", publisher = "User")
        dao.insertPack(pack)

        val sticker = StickerItemEntity(id = "s_5", packId = "pack_4", imagePath = "/files/s5.webp")
        dao.insertSticker(sticker)

        dao.deleteStickerById("s_5")

        val item = dao.getStickerById("s_5")
        assertNull(item)
    }
}
