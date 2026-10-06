package com.kernelbreach.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kernelbreach.app.navigation.KernelBreachApp
import com.kernelbreach.core.database.repo.ThemeMode
import com.kernelbreach.core.design.theme.KernelBreachTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val rootViewModel: RootViewModel = hiltViewModel()
            val settings by rootViewModel.settings.collectAsStateWithLifecycle()

            val forceDark = when (settings?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> null // follow system
            }

            KernelBreachTheme(forceDark = forceDark) {
                KernelBreachApp(
                    onboardingDone = settings?.onboardingDone == true,
                    settingsLoaded = settings != null,
                )
            }
        }
    }
}
