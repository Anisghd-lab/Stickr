package com.stickr.app.core.model

/**
 * Représente un sticker individuel au sein d'un pack WhatsApp.
 *
 * @param imageFile Chemin relatif, absolu ou nom du fichier WebP (ex: "sticker_1.webp").
 * @param emojis Liste de 1 à 3 émojis associés au sticker (requis par WhatsApp pour la recherche).
 */
data class WhatsAppStickerItem(
    val imageFile: String,
    val emojis: List<String> = listOf("✨")
) {
    init {
        require(imageFile.isNotBlank()) { "imageFile ne peut pas être vide." }
    }

    /**
     * Retourne la chaîne d'émojis formatée pour le ContentProvider (séparée par des virgules).
     */
    val emojiString: String
        get() = emojis.take(3).joinToString(",")
}
