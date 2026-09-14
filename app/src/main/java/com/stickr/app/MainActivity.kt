package com.stickr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.stickr.app.presentation.navigation.AppNavigation
import com.stickr.app.presentation.theme.StickrTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialisation de l'API AndroidX Core Splash Screen pour un démarrage fluide
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Activation du mode Edge-to-Edge conforme aux standards Android 15 (API 35)
        enableEdgeToEdge()

        setContent {
            StickrTheme {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}
