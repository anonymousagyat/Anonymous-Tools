package com.anonymoustools.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anonymoustools.app.features.gesture.GestureConfigScreen
import com.anonymoustools.app.features.home.CaptureCenterScreen
import com.anonymoustools.app.features.tile.TileConfigScreen
import com.anonymoustools.app.features.vault.VaultGalleryScreen
import com.anonymoustools.app.ui.theme.CreamBackground

@Composable
fun AppNavGraph(
    startDestination: String = Screen.CaptureCenter.route,
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground),
        containerColor = CreamBackground,
        contentWindowInsets = WindowInsets.systemBars
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable(Screen.CaptureCenter.route) {
                CaptureCenterScreen(
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            composable(Screen.GestureConfig.route) {
                GestureConfigScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.TileConfig.route) {
                TileConfigScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.VaultGallery.route) {
                VaultGalleryScreen(
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.CaptureCenter.route) {
                                popUpTo(Screen.CaptureCenter.route) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}
