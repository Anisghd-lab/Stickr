package com.stickr.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.stickr.app.domain.model.Sticker

@Entity(
    tableName = "stickers",
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
data class StickerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "pack_id")
    val packId: Long,
    @ColumnInfo(name = "image_uri")
    val imageUri: String,
    @ColumnInfo(name = "emojis")
    val emojis: String = "",
    @ColumnInfo(name = "order")
    val order: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Sticker = Sticker(
        id = id,
        packId = packId,
        imageUri = imageUri,
        emojis = if (emojis.isBlank()) emptyList() else emojis.split(","),
        order = order,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(sticker: Sticker): StickerEntity = StickerEntity(
            id = sticker.id,
            packId = sticker.packId,
            imageUri = sticker.imageUri,
            emojis = sticker.emojis.joinToString(","),
            order = sticker.order,
            createdAt = sticker.createdAt
        )
    }
}
