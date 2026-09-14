package com.stickr.app.presentation.navigation

/**
 * Définition des routes de navigation de l'application Stickr.
 */
sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")

    data object PackDetail : Screen("pack_detail/{packId}") {
        fun createRoute(packId: String): String = "pack_detail/$packId"
    }

    data object Editor : Screen("editor?packId={packId}&imageUri={imageUri}") {
        fun createRoute(packId: String, imageUri: String? = null): String {
            return if (imageUri != null) {
                "editor?packId=$packId&imageUri=${java.net.URLEncoder.encode(imageUri, "UTF-8")}"
            } else {
                "editor?packId=$packId"
            }
        }
    }

    // Alias rétrocompatibilité
    companion object {
        val Home = Dashboard
    }
}
