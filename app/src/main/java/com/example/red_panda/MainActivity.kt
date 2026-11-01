package com.example.red_panda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.example.red_panda.data.repository.SettingsRepositoryImpl
import com.example.red_panda.data.repository.ThemeSetting
import com.example.red_panda.ui.navigation.NavGraph
import com.example.red_panda.ui.theme.RedpandaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val startupThemeSetting = mutableStateOf<ThemeSetting?>(null)
    private var startupThemeLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !startupThemeLoaded }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsRepository = SettingsRepositoryImpl(applicationContext)
        lifecycleScope.launch {
            settingsRepository.themeSetting.collect { setting ->
                startupThemeSetting.value = setting
            }
        }
        setContent {
            val themeSetting = startupThemeSetting.value
            val systemInDark = isSystemInDarkTheme()
            val darkTheme = when (themeSetting ?: ThemeSetting.SYSTEM) {
                ThemeSetting.SYSTEM -> systemInDark
                ThemeSetting.DARK -> true
                ThemeSetting.LIGHT -> false
            }

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkTheme }
                )
                onDispose {}
            }

            RedpandaTheme(themeSetting = themeSetting ?: ThemeSetting.SYSTEM) {
                if (themeSetting == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (systemInDark) Color.Black else Color(0xFFF2F4F8))
                    )
                } else {
                    SideEffect { startupThemeLoaded = true }
                    NavGraph(settingsRepository = settingsRepository)
                }
            }
        }
    }
}
