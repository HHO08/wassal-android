package com.wassal.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.wassal.app.ui.AppRoot
import com.wassal.app.ui.auth.AuthViewModel
import com.wassal.app.ui.theme.WassalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            authViewModel.onMicPermissionResult(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WassalTheme {
                AppRoot(authViewModel)
            }
        }
    }

    /** Request RECORD_AUDIO. This is the only runtime permission the app uses. */
    fun requestMicPermission() {
        micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }
}
