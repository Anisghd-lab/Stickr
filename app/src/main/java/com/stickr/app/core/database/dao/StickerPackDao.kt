package com.stickr.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.stickr.app.core.database.entity.StickerItemEntity
import com.stickr.app.core.database.entity.StickerPackEntity
import com.stickr.app.core.database.entity.StickerPackWithStickers
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object pour la gestion réactive et transactionnelle des packs et stickers.
 */
@Dao
interface StickerPackDao {

    // --- Requêtes réactives (Flow) pour l'UI Compose ---

    @Transaction
    @Query("SELECT * FROM sticker_packs ORDER BY created_at DESC")
    fun getAllPacksWithStickers(): Flow<List<StickerPackWithStickers>>

    @Transaction
    @Query("SELECT * FROM sticker_packs WHERE id = :packId")
    fun getPackWithStickersById(packId: String): Flow<StickerPackWithStickers?>

    // --- Requêtes synchrones pour le StickerContentProvider (WhatsApp IPC) ---

    @Transaction
    @Query("SELECT * FROM sticker_packs ORDER BY created_at DESC")
    fun getAllPacksWithStickersSync(): List<StickerPackWithStickers>

    @Transaction
    @Query("SELECT * FROM sticker_packs WHERE id = :packId")
    fun getPackWithStickersByIdSync(packId: String): StickerPackWithStickers?

    @Query("SELECT * FROM sticker_items WHERE pack_id = :packId ORDER BY order_index ASC")
    fun getStickersForPackSync(packId: String): List<StickerItemEntity>

    // --- Opérations d'écriture sur les packs ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPack(pack: StickerPackEntity)

    @Update
    suspend fun updatePack(pack: StickerPackEntity)

    @Query("DELETE FROM sticker_packs WHERE id = :packId")
    suspend fun deletePackById(packId: String)

    // --- Opérations d'écriture sur les stickers ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSticker(sticker: StickerItemEntity)

    @Query("UPDATE sticker_items SET emojis = :emojis WHERE id = :stickerId")
    suspend fun updateStickerEmojis(stickerId: String, emojis: String)

    @Query("DELETE FROM sticker_items WHERE id = :stickerId")
    suspend fun deleteStickerById(stickerId: String)

    @Query("SELECT * FROM sticker_items WHERE id = :stickerId LIMIT 1")
    suspend fun getStickerById(stickerId: String): StickerItemEntity?

    @Update
    suspend fun updateStickers(stickers: List<StickerItemEntity>)

    @Transaction
    suspend fun reorderStickers(orderedStickers: List<StickerItemEntity>) {
        val updatedList = orderedStickers.mapIndexed { index, item ->
            item.copy(orderIndex = index)
        }
        updateStickers(updatedList)
    }
}
