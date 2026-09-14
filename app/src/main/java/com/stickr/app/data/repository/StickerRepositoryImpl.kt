package com.stickr.app.data.repository

import com.stickr.app.data.local.dao.StickerDao
import com.stickr.app.data.local.dao.StickerPackDao
import com.stickr.app.data.local.entity.StickerEntity
import com.stickr.app.data.local.entity.StickerPackEntity
import com.stickr.app.domain.model.Sticker
import com.stickr.app.domain.model.StickerPack
import com.stickr.app.domain.repository.StickerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StickerRepositoryImpl @Inject constructor(
    private val packDao: StickerPackDao,
    private val stickerDao: StickerDao
) : StickerRepository {

    override fun getAllPacks(): Flow<List<StickerPack>> {
        return packDao.getAllPacksWithCount().map { list ->
            list.map { item ->
                StickerPack(
                    id = item.id,
                    name = item.name,
                    author = item.author,
                    trayImageUri = item.tray_image_uri,
                    stickerCount = item.sticker_count,
                    createdAt = item.created_at
                )
            }
        }
    }

    override fun getPackById(packId: Long): Flow<StickerPack?> {
        return packDao.getPackById(packId).map { entity ->
            entity?.toDomain()
        }
    }

    override fun getStickersForPack(packId: Long): Flow<List<Sticker>> {
        return stickerDao.getStickersForPack(packId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertPack(pack: StickerPack): Long {
        return packDao.insertPack(StickerPackEntity.fromDomain(pack))
    }

    override suspend fun deletePack(packId: Long) {
        packDao.deletePackById(packId)
    }

    override suspend fun insertSticker(sticker: Sticker): Long {
        return stickerDao.insertSticker(StickerEntity.fromDomain(sticker))
    }

    override suspend fun deleteSticker(stickerId: Long) {
        stickerDao.deleteStickerById(stickerId)
    }
}
