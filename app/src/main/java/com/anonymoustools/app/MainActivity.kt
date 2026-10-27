package com.anonymoustools.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.anonymoustools.app.navigation.AppNavGraph
import com.anonymoustools.app.navigation.Screen
import com.anonymoustools.app.ui.theme.AnonymousToolsTheme
import com.anonymoustools.app.ui.theme.CreamBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val targetScreen = intent.getStringExtra("target_screen")
        val startDest = if (targetScreen == "vault") {
            Screen.VaultGallery.route
        } else {
            Screen.CaptureCenter.route
        }

        setContent {
            AnonymousToolsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CreamBackground
                ) {
                    AppNavGraph(startDestination = startDest)
                }
            }
        }
    }
}
