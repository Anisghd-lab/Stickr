package com.stickr.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

/**
 * Point d'entrée NavGraph vers [AppNavigation].
 */
@Composable
fun StickrNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    AppNavigation(navController = navController, modifier = modifier)
}
