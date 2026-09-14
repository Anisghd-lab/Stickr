package com.stickr.app.core.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.stickr.app.core.provider.StickerContentProviderContract

/**
 * Types de clients WhatsApp supportés.
 */
enum class WhatsAppPackage(val packageName: String, val displayName: String) {
    CONSUMER("com.whatsapp", "WhatsApp"),
    BUSINESS("com.whatsapp.w4b", "WhatsApp Business")
}

/**
 * Utilitaire pour interagir avec l'application WhatsApp via l'API officielle Intent.
 *
 * Utilise l'action `com.whatsapp.intent.action.ENABLE_ADD_PACK` avec les extras requis
 * pour déclencher la boîte de dialogue d'ajout de pack de stickers dans WhatsApp.
 */
object WhatsAppIntentHelper {

    const val ACTION_ENABLE_ADD_PACK = "com.whatsapp.intent.action.ENABLE_ADD_PACK"
    const val EXTRA_STICKER_PACK_ID = "sticker_pack_id"
    const val EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority"
    const val EXTRA_STICKER_PACK_NAME = "sticker_pack_name"

    /**
     * Vérifie si un client WhatsApp spécifique est installé sur l'appareil.
     */
    fun isWhatsAppInstalled(
        context: Context,
        packageType: WhatsAppPackage = WhatsAppPackage.CONSUMER
    ): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageType.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageType.packageName, PackageManager.GET_ACTIVITIES)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Vérifie si au moins une version de WhatsApp (Standard ou Business) est installée.
     */
    fun isAnyWhatsAppInstalled(context: Context): Boolean {
        return isWhatsAppInstalled(context, WhatsAppPackage.CONSUMER) ||
                isWhatsAppInstalled(context, WhatsAppPackage.BUSINESS)
    }

    /**
     * Détermine la version de WhatsApp disponible par défaut (priorité à Consumer).
     */
    fun getAvailableWhatsAppPackage(context: Context): WhatsAppPackage? {
        return when {
            isWhatsAppInstalled(context, WhatsAppPackage.CONSUMER) -> WhatsAppPackage.CONSUMER
            isWhatsAppInstalled(context, WhatsAppPackage.BUSINESS) -> WhatsAppPackage.BUSINESS
            else -> null
        }
    }

    /**
     * Déclenche l'Intent officiel WhatsApp pour ajouter un pack de stickers.
     *
     * @param context Contexte d'application ou d'activité.
     * @param packId Identifiant unique du pack de stickers.
     * @param packName Nom du pack affiché dans la confirmation WhatsApp.
     * @param packageType Version ciblée (Consumer ou Business).
     * @return [Result.success] si l'Intent a été lancé, ou [Result.failure] avec l'exception levée.
     */
    fun launchAddToWhatsAppIntent(
        context: Context,
        packId: String,
        packName: String,
        packageType: WhatsAppPackage = WhatsAppPackage.CONSUMER
    ): Result<Unit> = runCatching {
        if (!isWhatsAppInstalled(context, packageType)) {
            throw ActivityNotFoundException("${packageType.displayName} n'est pas installé sur cet appareil.")
        }

        val authority = StickerContentProviderContract.getAuthority(context)

        val intent = Intent().apply {
            action = ACTION_ENABLE_ADD_PACK
            putExtra(EXTRA_STICKER_PACK_ID, packId)
            putExtra(EXTRA_STICKER_PACK_AUTHORITY, authority)
            putExtra(EXTRA_STICKER_PACK_NAME, packName)
            setPackage(packageType.packageName)

            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        context.startActivity(intent)
    }
}
