package dev.shorthouse.coinwatch

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dev.shorthouse.coinwatch.data.AppContainer
import dev.shorthouse.coinwatch.navigation.AppNavHost
import dev.shorthouse.coinwatch.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.dark(
                scrim = Color.TRANSPARENT,
            )
        )

        val appContainer = AppContainer(applicationContext)

        setContent {
            val navController = rememberNavController()

            AppTheme {
                val viewModel = remember { MainActivityViewModel() }
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                if (!uiState.isLoading) {
                    AppNavHost(
                        appContainer = appContainer,
                        navController = navController,
                        navigationBarStartScreen = uiState.startScreen
                    )
                }
            }
        }
    }
}
