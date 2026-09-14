package com.stickr.app.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.stickr.app.core.model.WhatsAppStickerItem
import com.stickr.app.core.model.WhatsAppStickerPack
import com.stickr.app.core.model.WhatsAppStickerValidator

/**
 * Relation 1-à-N entre un StickerPackEntity et ses StickerItemEntity associés.
 */
data class StickerPackWithStickers(
    @Embedded
    val pack: StickerPackEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "pack_id"
    )
    val stickers: List<StickerItemEntity>
) {
    val stickerCount: Int
        get() = stickers.size

    val isWhatsAppReady: Boolean
        get() = stickers.size in WhatsAppStickerValidator.MIN_STICKERS_PER_PACK..WhatsAppStickerValidator.MAX_STICKERS_PER_PACK

    val missingStickersCount: Int
        get() = maxOf(0, WhatsAppStickerValidator.MIN_STICKERS_PER_PACK - stickers.size)

    /**
     * Convertit vers le modèle officiel d'exportation WhatsApp.
     */
    fun toWhatsAppModel(): WhatsAppStickerPack {
        return WhatsAppStickerPack(
            identifier = pack.id,
            name = pack.name,
            publisher = pack.publisher,
            trayImageFile = pack.trayImagePath ?: "tray_${pack.id}.png",
            stickers = stickers.map {
                WhatsAppStickerItem(
                    imageFile = it.imagePath,
                    emojis = it.emojiList
                )
            }
        )
    }
}
