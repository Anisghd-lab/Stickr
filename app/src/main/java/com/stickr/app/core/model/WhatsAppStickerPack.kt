package com.stickr.app.core.model

/**
 * Modèle de données complet pour un pack de stickers conforme au protocole WhatsApp.
 *
 * @param identifier Identifiant unique du pack (alphanumérique).
 * @param name Nom du pack affiché dans le sélecteur WhatsApp (max 128 caractères).
 * @param publisher Auteur ou créateur du pack (max 128 caractères).
 * @param trayImageFile Nom ou chemin de l'icône de plateau 96x96 px (PNG/WebP < 50 Ko).
 * @param publisherEmail Email optionnel du créateur.
 * @param publisherWebsite Site Web optionnel du créateur.
 * @param privacyPolicyWebsite Lien vers la politique de confidentialité.
 * @param licenseAgreementWebsite Lien vers le contrat de licence.
 * @param animatedStickerPack True si le pack contient des stickers animés (WebP animé).
 * @param imageDataVersion Version des données (pour forcer le rafraîchissement du cache WhatsApp).
 * @param avoidCache True pour empêcher WhatsApp de mettre en cache les images.
 * @param stickers Liste des stickers du pack (doit contenir entre 3 et 30 éléments).
 */
data class WhatsAppStickerPack(
    val identifier: String,
    val name: String,
    val publisher: String,
    val trayImageFile: String,
    val publisherEmail: String? = null,
    val publisherWebsite: String? = null,
    val privacyPolicyWebsite: String? = null,
    val licenseAgreementWebsite: String? = null,
    val animatedStickerPack: Boolean = false,
    val imageDataVersion: String = "1",
    val avoidCache: Boolean = false,
    val stickers: List<WhatsAppStickerItem> = emptyList()
) {
    val stickerCount: Int
        get() = stickers.size

    val isExportableToWhatsApp: Boolean
        get() = stickers.size in WhatsAppStickerValidator.MIN_STICKERS_PER_PACK..WhatsAppStickerValidator.MAX_STICKERS_PER_PACK
}
