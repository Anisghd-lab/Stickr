package com.stickr.app.core.model

import android.graphics.Bitmap
import java.io.File

/**
 * Validateur des contraintes techniques officielles imposées par WhatsApp pour l'exportation de stickers.
 *
 * Spécifications WhatsApp :
 * - Nombre de stickers par pack : entre 3 et 30 inclus.
 * - Nombre d'émojis par sticker : entre 1 et 3 émojis.
 * - Dimensions de l'icône de plateau (tray icon) : exactement 96x96 pixels.
 * - Poids de l'icône de plateau : strictement inférieur à 50 Ko.
 * - Dimensions de chaque sticker : exactement 512x512 pixels.
 * - Poids de chaque sticker : strictement inférieur à 100 Ko.
 * - Longueur maximale du nom du pack et de l'éditeur : 128 caractères.
 */
object WhatsAppStickerValidator {

    const val MIN_STICKERS_PER_PACK = 3
    const val MAX_STICKERS_PER_PACK = 30
    const val MIN_EMOJIS_PER_STICKER = 1
    const val MAX_EMOJIS_PER_STICKER = 3
    const val TRAY_ICON_SIZE_PX = 96
    const val MAX_TRAY_ICON_SIZE_BYTES = 50 * 1024 // 51 200 octets
    const val MAX_STICKER_SIZE_BYTES = 100 * 1024 // 102 400 octets
    const val MAX_NAME_LENGTH = 128

    /**
     * Valide la structure logique et les métadonnées d'un [WhatsAppStickerPack].
     *
     * @return [Result.success] si valide, ou [Result.failure] avec une [IllegalArgumentException] détaillée.
     */
    fun validatePack(pack: WhatsAppStickerPack): Result<Unit> {
        if (pack.identifier.isBlank()) {
            return Result.failure(IllegalArgumentException("L'identifiant du pack ne peut pas être vide."))
        }
        if (pack.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Le nom du pack ne peut pas être vide."))
        }
        if (pack.name.length > MAX_NAME_LENGTH) {
            return Result.failure(IllegalArgumentException("Le nom du pack dépasse la limite de $MAX_NAME_LENGTH caractères."))
        }
        if (pack.publisher.isBlank()) {
            return Result.failure(IllegalArgumentException("Le nom de l'éditeur ne peut pas être vide."))
        }
        if (pack.publisher.length > MAX_NAME_LENGTH) {
            return Result.failure(IllegalArgumentException("Le nom de l'éditeur dépasse la limite de $MAX_NAME_LENGTH caractères."))
        }
        if (pack.trayImageFile.isBlank()) {
            return Result.failure(IllegalArgumentException("L'icône de plateau (tray icon) est obligatoire."))
        }

        // Validation du nombre de stickers (3 à 30)
        val count = pack.stickers.size
        if (count < MIN_STICKERS_PER_PACK) {
            return Result.failure(
                IllegalArgumentException("Un pack WhatsApp doit contenir au moins $MIN_STICKERS_PER_PACK stickers ($count présent(s)).")
            )
        }
        if (count > MAX_STICKERS_PER_PACK) {
            return Result.failure(
                IllegalArgumentException("Un pack WhatsApp ne peut pas contenir plus de $MAX_STICKERS_PER_PACK stickers ($count présents).")
            )
        }

        // Validation individuelle de chaque sticker
        for ((index, sticker) in pack.stickers.withIndex()) {
            val stickerResult = validateStickerItem(sticker, index)
            if (stickerResult.isFailure) {
                return stickerResult
            }
        }

        return Result.success(Unit)
    }

    /**
     * Valide un [WhatsAppStickerItem] individuel.
     */
    fun validateStickerItem(sticker: WhatsAppStickerItem, index: Int = 0): Result<Unit> {
        if (sticker.imageFile.isBlank()) {
            return Result.failure(IllegalArgumentException("Le fichier du sticker #$index ne peut pas être vide."))
        }
        if (sticker.emojis.size < MIN_EMOJIS_PER_STICKER) {
            return Result.failure(
                IllegalArgumentException("Le sticker #$index doit être associé à au moins $MIN_EMOJIS_PER_STICKER émoji.")
            )
        }
        if (sticker.emojis.size > MAX_EMOJIS_PER_STICKER) {
            return Result.failure(
                IllegalArgumentException("Le sticker #$index ne peut pas être associé à plus de $MAX_EMOJIS_PER_STICKER émojis (${sticker.emojis.size} fournis).")
            )
        }
        return Result.success(Unit)
    }

    /**
     * Valide un Bitmap d'icône de plateau (doit être exactement 96x96 px).
     */
    fun validateTrayIconBitmap(bitmap: Bitmap): Result<Unit> {
        if (bitmap.isRecycled) {
            return Result.failure(IllegalArgumentException("L'icône de plateau est recyclée."))
        }
        if (bitmap.width != TRAY_ICON_SIZE_PX || bitmap.height != TRAY_ICON_SIZE_PX) {
            return Result.failure(
                IllegalArgumentException(
                    "L'icône de plateau doit faire exactement ${TRAY_ICON_SIZE_PX}x${TRAY_ICON_SIZE_PX} pixels (actuel: ${bitmap.width}x${bitmap.height})."
                )
            )
        }
        return Result.success(Unit)
    }

    /**
     * Valide la taille d'un fichier d'icône de plateau (< 50 Ko).
     */
    fun validateTrayIconFile(file: File): Result<Unit> {
        if (!file.exists()) {
            return Result.failure(IllegalArgumentException("Le fichier d'icône de plateau n'existe pas : ${file.name}"))
        }
        if (file.length() >= MAX_TRAY_ICON_SIZE_BYTES) {
            return Result.failure(
                IllegalArgumentException("L'icône de plateau dépasse 50 Ko (${file.length()} octets).")
            )
        }
        return Result.success(Unit)
    }
}
