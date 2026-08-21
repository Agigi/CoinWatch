package dev.shorthouse.coinwatch.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.shorthouse.coinwatch.data.AppContainer
import dev.shorthouse.coinwatch.ui.screen.details.DetailsScreen
import dev.shorthouse.coinwatch.ui.screen.details.DetailsViewModel
import dev.shorthouse.coinwatch.ui.screen.settings.SettingsScreen
import dev.shorthouse.coinwatch.ui.screen.settings.SettingsViewModel

@Composable
fun AppNavHost(
    appContainer: AppContainer,
    navigationBarStartScreen: NavigationBarScreen = NavigationBarScreen.Market,
    navController: NavHostController = rememberNavController()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    appContainer.wssManager.resumeConnection()
                }
                Lifecycle.Event.ON_STOP -> {
                    appContainer.wssManager.pauseConnection()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.NavigationBar.route,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None }
    ) {
        composable(route = Screen.NavigationBar.route) {
            NavigationBarScaffold(
                appContainer = appContainer,
                startScreen = navigationBarStartScreen,
                onNavigateDetails = { coinId: String ->
                    navController.navigate(Screen.Details.route + "/$coinId")
                },
                onNavigateSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(
            route = Screen.Details.route + "/{coinId}",
            enterTransition = { fadeIn(animationSpec = tween(500)) },
            exitTransition = { fadeOut(animationSpec = tween(500)) }
        ) { backStackEntry ->
            val coinId = backStackEntry.arguments?.getString("coinId") ?: ""
            val viewModel = remember(coinId) {
                DetailsViewModel(
                    coinId = coinId,
                    wssManager = appContainer.wssManager,
                    favouritesPreferences = appContainer.favouritesPreferences
                )
            }
            DetailsScreen(
                viewModel = viewModel,
                onNavigateUp = { navController.navigateUp() }
            )
        }
        composable(
            route = Screen.Settings.route,
            enterTransition = { fadeIn(animationSpec = tween(500)) },
            exitTransition = { fadeOut(animationSpec = tween(500)) }
        ) {
            val viewModel = remember {
                SettingsViewModel()
            }
            SettingsScreen(
                viewModel = viewModel,
                onNavigateUp = { navController.navigateUp() }
            )
        }
    }
}
