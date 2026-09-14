package com.stickr.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stickr.app.domain.model.StickerPack

@Entity(tableName = "sticker_packs")
data class StickerPackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "author")
    val author: String,
    @ColumnInfo(name = "tray_image_uri")
    val trayImageUri: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(stickerCount: Int = 0): StickerPack = StickerPack(
        id = id,
        name = name,
        author = author,
        trayImageUri = trayImageUri,
        stickerCount = stickerCount,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(pack: StickerPack): StickerPackEntity = StickerPackEntity(
            id = pack.id,
            name = pack.name,
            author = pack.author,
            trayImageUri = pack.trayImageUri,
            createdAt = pack.createdAt
        )
    }
}
