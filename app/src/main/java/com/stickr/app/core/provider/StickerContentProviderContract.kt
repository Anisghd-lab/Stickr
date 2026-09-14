package com.stickr.app.core.provider

import android.content.Context
import android.net.Uri

/**
 * Contrat d'URIs pour le [StickerContentProvider].
 */
object StickerContentProviderContract {

    fun getAuthority(context: Context): String =
        "${context.packageName}${StickerContentProvider.AUTHORITY_SUFFIX}"

    fun getMetadataUri(context: Context): Uri =
        Uri.parse("content://${getAuthority(context)}/${StickerContentProvider.METADATA_PATH}")

    fun getPackMetadataUri(context: Context, packId: String): Uri =
        Uri.parse("content://${getAuthority(context)}/${StickerContentProvider.METADATA_PATH}/$packId")

    fun getStickersUri(context: Context, packId: String): Uri =
        Uri.parse("content://${getAuthority(context)}/${StickerContentProvider.STICKERS_PATH}/$packId")

    fun getStickerAssetUri(context: Context, packId: String, fileName: String): Uri =
        Uri.parse("content://${getAuthority(context)}/${StickerContentProvider.STICKERS_ASSET_PATH}/$packId/$fileName")

    fun getTrayAssetUri(context: Context, packId: String): Uri =
        Uri.parse("content://${getAuthority(context)}/${StickerContentProvider.TRAY_ASSET_PATH}/$packId")
}
