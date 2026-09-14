package com.stickr.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.stickr.app.data.local.entity.StickerPackEntity
import kotlinx.coroutines.flow.Flow

data class StickerPackWithCount(
    val id: Long,
    val name: String,
    val author: String,
    val tray_image_uri: String,
    val created_at: Long,
    val sticker_count: Int
)

@Dao
interface StickerPackDao {
    @Query("""
        SELECT p.id, p.name, p.author, p.tray_image_uri, p.created_at, COUNT(s.id) as sticker_count 
        FROM sticker_packs p 
        LEFT JOIN stickers s ON p.id = s.pack_id 
        GROUP BY p.id 
        ORDER BY p.created_at DESC
    """)
    fun getAllPacksWithCount(): Flow<List<StickerPackWithCount>>

    @Query("SELECT * FROM sticker_packs WHERE id = :id")
    fun getPackById(id: Long): Flow<StickerPackEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPack(pack: StickerPackEntity): Long

    @Query("DELETE FROM sticker_packs WHERE id = :id")
    suspend fun deletePackById(id: Long)
}
