package com.stickr.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stickr.app.data.local.entity.StickerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StickerDao {
    @Query("SELECT * FROM stickers WHERE pack_id = :packId ORDER BY `order` ASC, created_at ASC")
    fun getStickersForPack(packId: Long): Flow<List<StickerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSticker(sticker: StickerEntity): Long

    @Query("DELETE FROM stickers WHERE id = :id")
    suspend fun deleteStickerById(id: Long)

    @Query("DELETE FROM stickers WHERE pack_id = :packId")
    suspend fun deleteStickersByPackId(packId: Long)
}
