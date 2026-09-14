package com.stickr.app.domain.repository

import com.stickr.app.domain.model.Sticker
import com.stickr.app.domain.model.StickerPack
import kotlinx.coroutines.flow.Flow

interface StickerRepository {
    fun getAllPacks(): Flow<List<StickerPack>>
    fun getPackById(packId: Long): Flow<StickerPack?>
    fun getStickersForPack(packId: Long): Flow<List<Sticker>>
    suspend fun insertPack(pack: StickerPack): Long
    suspend fun deletePack(packId: Long)
    suspend fun insertSticker(sticker: Sticker): Long
    suspend fun deleteSticker(stickerId: Long)
}
