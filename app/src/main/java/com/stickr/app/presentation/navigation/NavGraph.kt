package com.stickr.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stickr.app.presentation.screens.editor.StickerEditorScreen
import com.stickr.app.presentation.screens.editor.StickerEditorViewModel
import com.stickr.app.presentation.screens.home.HomeScreen
import com.stickr.app.presentation.screens.home.HomeViewModel
import com.stickr.app.presentation.screens.packdetail.PackDetailScreen
import com.stickr.app.presentation.screens.packdetail.PackDetailViewModel

@Composable
fun StickrNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                onNavigateToPack = { packId ->
                    navController.navigate(Screen.PackDetail.createRoute(packId))
                },
                onNavigateToEditor = { packId ->
                    navController.navigate(Screen.Editor.createRoute(packId))
                }
            )
        }

        composable(
            route = "editor?packId={packId}&imageUri={imageUri}",
            arguments = listOf(
                navArgument("packId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument("imageUri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val viewModel: com.stickr.app.presentation.screens.editor.StickerEditorViewModel = hiltViewModel()
            val initialUri = backStackEntry.arguments?.getString("imageUri")
            com.stickr.app.presentation.screens.editor.StickerEditorScreen(
                viewModel = viewModel,
                initialImageUri = initialUri,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PackDetail.route,
            arguments = listOf(
                navArgument("packId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val packId = backStackEntry.arguments?.getLong("packId") ?: 0L
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
    }
}
