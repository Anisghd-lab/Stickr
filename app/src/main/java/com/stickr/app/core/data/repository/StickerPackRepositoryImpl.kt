package com.stickr.app.core.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import com.stickr.app.core.database.dao.StickerPackDao
import com.stickr.app.core.database.entity.StickerItemEntity
import com.stickr.app.core.database.entity.StickerPackEntity
import com.stickr.app.core.database.entity.StickerPackWithStickers
import com.stickr.app.core.image.TrayIconHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implémentation du dépôt reliant la base Room et le système de fichiers interne.
 *
 * Assure la suppression stricte des fichiers physiques lors de la suppression de stickers ou de packs
 * pour éviter toute fuite de stockage ou fichier orphelin.
 */
@Singleton
class StickerPackRepositoryImpl @Inject constructor(
    private val packDao: StickerPackDao,
    @ApplicationContext private val context: Context
) : StickerPackRepository {

    override fun getAllPacks(): Flow<List<StickerPackWithStickers>> {
        return packDao.getAllPacksWithStickers()
    }

    override fun getPackById(packId: String): Flow<StickerPackWithStickers?> {
        return packDao.getPackWithStickersById(packId)
    }

    override suspend fun createPack(name: String, publisher: String): String = withContext(Dispatchers.IO) {
        val packId = UUID.randomUUID().toString()
        val newPack = StickerPackEntity(
            id = packId,
            name = name.trim(),
            publisher = publisher.trim(),
            trayImagePath = null
        )
        packDao.insertPack(newPack)
        packId
    }

    override suspend fun updatePack(pack: StickerPackEntity) = withContext(Dispatchers.IO) {
        packDao.updatePack(pack)
    }

    override suspend fun deletePack(packId: String) = withContext(Dispatchers.IO) {
        // 1. Récupération des stickers associés pour supprimer leurs fichiers physiques
        val packWithStickers = packDao.getPackWithStickersByIdSync(packId)
        packWithStickers?.stickers?.forEach { item ->
            deletePhysicalFile(item.imagePath)
        }

        // 2. Suppression de l'icône de plateau
        packWithStickers?.pack?.trayImagePath?.let { deletePhysicalFile(it) }
        val trayDir = File(context.filesDir, "tray")
        val defaultTrayFile = File(trayDir, "tray_${packId}.png")
        if (defaultTrayFile.exists()) {
            defaultTrayFile.delete()
        }

        // 3. Suppression en base Room (cascade vers sticker_items)
        packDao.deletePackById(packId)
    }

    override suspend fun addStickerToPack(
        packId: String,
        imagePath: String,
        emojis: String
    ): String = withContext(Dispatchers.IO) {
        val stickerId = UUID.randomUUID().toString()
        val currentStickers = packDao.getStickersForPackSync(packId)
        val nextOrderIndex = currentStickers.size

        val sticker = StickerItemEntity(
            id = stickerId,
            packId = packId,
            imagePath = imagePath,
            emojis = emojis,
            orderIndex = nextOrderIndex
        )
        packDao.insertSticker(sticker)

        // Génération automatique de l'icône de plateau si absente
        ensureTrayIcon(packId)

        stickerId
    }

    override suspend fun deleteSticker(stickerId: String) = withContext(Dispatchers.IO) {
        val sticker = packDao.getStickerById(stickerId)
        if (sticker != null) {
            deletePhysicalFile(sticker.imagePath)
            packDao.deleteStickerById(stickerId)
        }
    }

    override suspend fun updateStickerEmojis(stickerId: String, emojis: String) = withContext(Dispatchers.IO) {
        packDao.updateStickerEmojis(stickerId, emojis)
    }

    override suspend fun ensureTrayIcon(packId: String): String? = withContext(Dispatchers.IO) {
        val packWithStickers = packDao.getPackWithStickersByIdSync(packId) ?: return@withContext null
        val existingTray = packWithStickers.pack.trayImagePath

        if (!existingTray.isNullOrBlank() && File(existingTray).exists()) {
            return@withContext existingTray
        }

        // Si aucune icône n'est définie mais qu'au moins un sticker existe, on crée l'icône 96x96
        if (packWithStickers.stickers.isNotEmpty()) {
            val firstStickerFile = File(packWithStickers.stickers[0].imagePath)
            if (firstStickerFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(firstStickerFile.absolutePath)
                if (bitmap != null) {
                    val trayDir = File(context.filesDir, "tray")
                    if (!trayDir.exists()) trayDir.mkdirs()

                    val trayFile = File(trayDir, "tray_${packId}.png")
                    val pngBytes = TrayIconHelper.compressTrayIconToPng(bitmap)
                    FileOutputStream(trayFile).use { it.write(pngBytes) }

                    val updatedPack = packWithStickers.pack.copy(trayImagePath = trayFile.absolutePath)
                    packDao.updatePack(updatedPack)
                    return@withContext trayFile.absolutePath
                }
            }
        }
        null
    }

    private fun deletePhysicalFile(path: String) {
        runCatching {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
