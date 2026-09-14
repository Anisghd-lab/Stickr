package com.stickr.app.core.data.repository

import com.stickr.app.core.database.entity.StickerItemEntity
import com.stickr.app.core.database.entity.StickerPackEntity
import com.stickr.app.core.database.entity.StickerPackWithStickers
import kotlinx.coroutines.flow.Flow

/**
 * Interface de dépôt pour la gestion des packs et du système de fichiers local.
 */
interface StickerPackRepository {
    fun getAllPacks(): Flow<List<StickerPackWithStickers>>
    fun getPackById(packId: String): Flow<StickerPackWithStickers?>
    suspend fun createPack(name: String, publisher: String): String
    suspend fun updatePack(pack: StickerPackEntity)
    suspend fun deletePack(packId: String)
    suspend fun addStickerToPack(packId: String, imagePath: String, emojis: String = "✨"): String
    suspend fun deleteSticker(stickerId: String)
    suspend fun updateStickerEmojis(stickerId: String, emojis: String)
    suspend fun ensureTrayIcon(packId: String): String?
}
