package com.stickr.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Editor : Screen("editor?packId={packId}&imageUri={imageUri}") {
        fun createRoute(packId: Long, imageUri: String? = null): String {
            return if (imageUri != null) {
                "editor?packId=$packId&imageUri=${java.net.URLEncoder.encode(imageUri, "UTF-8")}"
            } else {
                "editor?packId=$packId"
            }
        }
    }
    data object PackDetail : Screen("pack_detail/{packId}") {
        fun createRoute(packId: Long): String = "pack_detail/$packId"
    }
}
