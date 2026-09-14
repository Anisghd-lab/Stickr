package com.stickr.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stickr.app.feature.dashboard.DashboardScreen
import com.stickr.app.feature.dashboard.DashboardViewModel
import com.stickr.app.feature.packdetail.PackDetailScreen
import com.stickr.app.feature.packdetail.PackDetailViewModel
import com.stickr.app.presentation.screens.editor.StickerEditorScreen
import com.stickr.app.presentation.screens.editor.StickerEditorViewModel

/**
 * Graphe de navigation principal reliant Dashboard -> PackDetail -> StickerEditor.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        // 1. Dashboard (Liste des packs)
        composable(Screen.Dashboard.route) {
            val viewModel: DashboardViewModel = hiltViewModel()
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToPackDetail = { packId ->
                    navController.navigate(Screen.PackDetail.createRoute(packId))
                },
                onNavigateToEditor = { packId ->
                    navController.navigate(Screen.Editor.createRoute(packId))
                }
            )
        }

        // 2. Détail du Pack (Grille de stickers, export WhatsApp)
        composable(
            route = Screen.PackDetail.route,
            arguments = listOf(
                navArgument("packId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val packId = backStackEntry.arguments?.getString("packId").orEmpty()
            val viewModel: PackDetailViewModel = hiltViewModel()
            PackDetailScreen(
                viewModel = viewModel,
                packId = packId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { pId ->
                    navController.navigate(Screen.Editor.createRoute(pId))
                }
            )
        }

        // 3. Éditeur Tactile de Sticker (Photo Picker, détourage IA, bordure, enregistrement)
        composable(
            route = "editor?packId={packId}&imageUri={imageUri}",
            arguments = listOf(
                navArgument("packId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("imageUri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val viewModel: StickerEditorViewModel = hiltViewModel()
            val initialUri = backStackEntry.arguments?.getString("imageUri")
            StickerEditorScreen(
                viewModel = viewModel,
                initialImageUri = initialUri,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
