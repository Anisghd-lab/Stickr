package com.stickr.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entité Room représentant un sticker individuel avec relation de clé étrangère en cascade.
 */
@Entity(
    tableName = "sticker_items",
    foreignKeys = [
        ForeignKey(
            entity = StickerPackEntity::class,
            parentColumns = ["id"],
            childColumns = ["pack_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["pack_id"])]
)
data class StickerItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "pack_id")
    val packId: String,
    @ColumnInfo(name = "image_path")
    val imagePath: String,
    @ColumnInfo(name = "emojis")
    val emojis: String = "✨",
    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0
) {
    val emojiList: List<String>
        get() = if (emojis.isBlank()) listOf("✨") else emojis.split(",").map { it.trim() }
}
