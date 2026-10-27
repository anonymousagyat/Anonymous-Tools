package com.anonymoustools.app.navigation

sealed class Screen(val route: String) {
    data object CaptureCenter : Screen("capture_center")
    data object GestureConfig : Screen("gesture_config")
    data object TileConfig : Screen("tile_config")
    data object VaultGallery : Screen("vault_gallery")
}
