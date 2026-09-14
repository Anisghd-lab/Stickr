package com.stickr.app.core.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.content.pm.ProviderInfo
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.stickr.app.core.image.TrayIconHelper
import com.stickr.app.data.local.AppDatabase
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

/**
 * Fournisseur de contenu (ContentProvider) officiel pour l'intégration des stickers WhatsApp.
 *
 * Le client WhatsApp communique directement avec ce Provider via Binder IPC pour :
 * 1. Lire la liste des packs de stickers disponibles (`/metadata`)
 * 2. Lire les métadonnées d'un pack spécifique (`/metadata/*`)
 * 3. Lister les stickers d'un pack (`/stickers/*`)
 * 4. Obtenir le flux binaire de chaque sticker WebP (`/stickers_asset/*/*`)
 * 5. Obtenir l'icône de plateau 96x96 px (`/tray_asset/*`)
 */
class StickerContentProvider : ContentProvider() {

    private var authority: String = ""
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    override fun attachInfo(context: Context?, info: ProviderInfo?) {
        val auth = info?.authority ?: "${context?.packageName}$AUTHORITY_SUFFIX"
        authority = auth
        setupUriMatcher(auth)
        super.attachInfo(context, info)
    }

    override fun onCreate(): Boolean {
        if (authority.isBlank()) {
            context?.let { ctx ->
                authority = "${ctx.packageName}$AUTHORITY_SUFFIX"
                setupUriMatcher(authority)
            }
        }
        return true
    }

    internal fun setupUriMatcher(auth: String) {
        uriMatcher.addURI(auth, METADATA_PATH, CODE_METADATA)
        uriMatcher.addURI(auth, "$METADATA_PATH/*", CODE_METADATA_PACK)
        uriMatcher.addURI(auth, "$STICKERS_PATH/*", CODE_STICKERS)
        uriMatcher.addURI(auth, "$STICKERS_ASSET_PATH/*/*", CODE_STICKERS_ASSET)
        uriMatcher.addURI(auth, "$TRAY_ASSET_PATH/*", CODE_TRAY_ASSET)
        uriMatcher.addURI(auth, "$TRAY_ASSET_PATH/*/*", CODE_TRAY_ASSET)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val match = uriMatcher.match(uri)
        val ctx = context ?: return null
        val database = AppDatabase.getInstance(ctx)

        return when (match) {
            CODE_METADATA -> {
                queryAllPacks(database)
            }
            CODE_METADATA_PACK -> {
                val packId = uri.lastPathSegment?.toLongOrNull() ?: return null
                querySinglePack(database, packId)
            }
            CODE_STICKERS -> {
                val packId = uri.lastPathSegment?.toLongOrNull() ?: return null
                queryStickersForPack(database, packId)
            }
            else -> {
                Log.w(TAG, "Requête non reconnue pour l'URI : $uri (code=$match)")
                null
            }
        }
    }

    private fun queryAllPacks(database: AppDatabase): Cursor {
        val cursor = MatrixCursor(STICKER_PACK_COLUMNS)
        val packs = database.stickerPackDao().getPacksSync()

        for (pack in packs) {
            cursor.addRow(
                arrayOf(
                    pack.id.toString(),                         // sticker_pack_identifier
                    pack.name,                                  // sticker_pack_name
                    pack.author,                                // sticker_pack_publisher
                    getTrayIconFileName(pack.id),               // sticker_pack_icon
                    "",                                         // android_play_store_link
                    "",                                         // ios_app_store_link
                    "",                                         // publisher_email
                    "",                                         // publisher_website
                    "",                                         // privacy_policy_website
                    "",                                         // license_agreement_website
                    "1",                                        // image_data_version
                    0,                                          // avoid_cache
                    0                                           // animated_sticker_pack
                )
            )
        }
        return cursor
    }

    private fun querySinglePack(database: AppDatabase, packId: Long): Cursor {
        val cursor = MatrixCursor(STICKER_PACK_COLUMNS)
        val pack = database.stickerPackDao().getPackByIdSync(packId) ?: return cursor

        cursor.addRow(
            arrayOf(
                pack.id.toString(),
                pack.name,
                pack.author,
                getTrayIconFileName(pack.id),
                "",
                "",
                "",
                "",
                "",
                "",
                "1",
                0,
                0
            )
        )
        return cursor
    }

    private fun queryStickersForPack(database: AppDatabase, packId: Long): Cursor {
        val cursor = MatrixCursor(STICKER_COLUMNS)
        val stickers = database.stickerDao().getStickersForPackSync(packId)

        for (sticker in stickers) {
            val file = File(sticker.imageUri)
            val fileName = file.name
            val emojis = if (sticker.emojis.isNotBlank()) sticker.emojis else "✨"

            cursor.addRow(
                arrayOf(
                    fileName,
                    emojis
                )
            )
        }
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            CODE_METADATA -> "vnd.android.cursor.dir/vnd.$authority.$METADATA_PATH"
            CODE_METADATA_PACK -> "vnd.android.cursor.item/vnd.$authority.$METADATA_PATH"
            CODE_STICKERS -> "vnd.android.cursor.dir/vnd.$authority.$STICKERS_PATH"
            CODE_STICKERS_ASSET -> "image/webp"
            CODE_TRAY_ASSET -> "image/png"
            else -> null
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val match = uriMatcher.match(uri)
        val ctx = context ?: throw FileNotFoundException("Contexte null")

        return when (match) {
            CODE_STICKERS_ASSET -> {
                // Format attendu: /stickers_asset/{packId}/{fileName}
                val segments = uri.pathSegments
                if (segments.size < 3) throw FileNotFoundException("URI de sticker invalide : $uri")
                val fileName = segments[2]

                // Sécurité anti path-traversal
                require(!fileName.contains("..") && !fileName.contains("/")) {
                    "Tentative de path traversal détectée : $fileName"
                }

                val stickersDir = File(ctx.filesDir, "stickers")
                val targetFile = File(stickersDir, fileName)

                if (!targetFile.exists() || !targetFile.canonicalPath.startsWith(stickersDir.canonicalPath)) {
                    throw FileNotFoundException("Fichier de sticker introuvable : $fileName")
                }

                ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
            }
            CODE_TRAY_ASSET -> {
                // Format attendu: /tray_asset/{packId} ou /tray_asset/{packId}/{fileName}
                val segments = uri.pathSegments
                if (segments.size < 2) throw FileNotFoundException("URI de tray icon invalide : $uri")
                val packId = segments[1].toLongOrNull()
                    ?: throw FileNotFoundException("packId invalide pour tray_asset : ${segments[1]}")

                val trayFile = getOrCreateTrayIconFile(ctx, packId)
                ParcelFileDescriptor.open(trayFile, ParcelFileDescriptor.MODE_READ_ONLY)
            }
            else -> {
                throw FileNotFoundException("URI non supportée pour openFile : $uri")
            }
        }
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        val pfd = openFile(uri, mode) ?: return null
        return AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
    }

    private fun getOrCreateTrayIconFile(context: Context, packId: Long): File {
        val trayDir = File(context.filesDir, "tray")
        if (!trayDir.exists()) trayDir.mkdirs()

        val trayFile = File(trayDir, "tray_${packId}.png")
        if (trayFile.exists() && trayFile.length() > 0) {
            return trayFile
        }

        // Si le fichier n'existe pas encore, on le génère depuis le premier sticker du pack ou trayImageUri
        val db = AppDatabase.getInstance(context)
        val pack = db.stickerPackDao().getPackByIdSync(packId)
        val stickers = db.stickerDao().getStickersForPackSync(packId)

        val sourceUri = when {
            pack != null && pack.trayImageUri.isNotBlank() && File(pack.trayImageUri).exists() -> pack.trayImageUri
            stickers.isNotEmpty() && File(stickers[0].imageUri).exists() -> stickers[0].imageUri
            else -> null
        }

        val bitmap = if (sourceUri != null) {
            BitmapFactory.decodeFile(sourceUri)
        } else {
            // Créer une icône de plateau 96x96 par défaut avec une couleur unie
            android.graphics.Bitmap.createBitmap(
                TrayIconHelper.TRAY_SIZE,
                TrayIconHelper.TRAY_SIZE,
                android.graphics.Bitmap.Config.ARGB_8888
            )
        }

        val trayBytes = TrayIconHelper.compressTrayIconToPng(bitmap)
        FileOutputStream(trayFile).use { it.write(trayBytes) }

        return trayFile
    }

    private fun getTrayIconFileName(packId: Long): String = "tray_${packId}.png"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        private const val TAG = "StickerProvider"

        const val AUTHORITY_SUFFIX = ".stickercontentprovider"
        const val METADATA_PATH = "metadata"
        const val STICKERS_PATH = "stickers"
        const val STICKERS_ASSET_PATH = "stickers_asset"
        const val TRAY_ASSET_PATH = "tray_asset"

        const val CODE_METADATA = 1
        const val CODE_METADATA_PACK = 2
        const val CODE_STICKERS = 3
        const val CODE_STICKERS_ASSET = 4
        const val CODE_TRAY_ASSET = 5

        // Colonnes officielles requises par WhatsApp
        val STICKER_PACK_COLUMNS = arrayOf(
            "sticker_pack_identifier",
            "sticker_pack_name",
            "sticker_pack_publisher",
            "sticker_pack_icon",
            "android_play_store_link",
            "ios_app_store_link",
            "publisher_email",
            "publisher_website",
            "privacy_policy_website",
            "license_agreement_website",
            "image_data_version",
            "avoid_cache",
            "animated_sticker_pack"
        )

        val STICKER_COLUMNS = arrayOf(
            "sticker_file_name",
            "sticker_emoji"
        )
    }
}
