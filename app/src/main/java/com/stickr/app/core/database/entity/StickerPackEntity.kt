package com.stickr.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entité Room représentant un pack de stickers complet.
 */
@Entity(tableName = "sticker_packs")
data class StickerPackEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "publisher")
    val publisher: String,
    @ColumnInfo(name = "tray_image_path")
    val trayImagePath: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
