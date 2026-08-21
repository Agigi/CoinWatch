package dev.shorthouse.coinwatch.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.shorthouse.coinwatch.data.AppContainer
import dev.shorthouse.coinwatch.ui.screen.favourites.FavouritesScreen
import dev.shorthouse.coinwatch.ui.screen.favourites.FavouritesViewModel
import dev.shorthouse.coinwatch.ui.screen.market.MarketScreen
import dev.shorthouse.coinwatch.ui.screen.market.MarketViewModel
import dev.shorthouse.coinwatch.ui.screen.search.SearchScreen
import dev.shorthouse.coinwatch.ui.screen.search.SearchViewModel
import kotlinx.collections.immutable.persistentListOf

@Composable
fun NavigationBarScaffold(
    appContainer: AppContainer,
    startScreen: NavigationBarScreen,
    onNavigateDetails: (String) -> Unit,
    onNavigateSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    val navigationBarScreens = remember {
        persistentListOf(
            NavigationBarScreen.Market,
            NavigationBarScreen.Favourites,
            NavigationBarScreen.Search
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showNavigationBar = navigationBarScreens.any { navigationBarScreen ->
        currentDestination?.hierarchy?.any { destination ->
            destination.route == navigationBarScreen.route
        } == true
    }

    Scaffold(
        bottomBar = {
            if (showNavigationBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    windowInsets = WindowInsets(0, 0, 0, 0)
                ) {
                    navigationBarScreens.forEach { screen ->
                        AddNavigationBarItem(
                            screen = screen,
                            currentDestination = currentDestination,
                            navController = navController
                        )
                    }
                }
            }
        },
        content = { paddingValues ->
            NavigationBarNavHost(
                appContainer = appContainer,
                navController = navController,
                startScreen = startScreen,
                onNavigateDetails = onNavigateDetails,
                onNavigateSettings = onNavigateSettings,
                modifier = Modifier.padding(paddingValues)
            )
        },
        modifier = modifier
    )
}

@Composable
private fun NavigationBarNavHost(
    appContainer: AppContainer,
    navController: NavHostController,
    startScreen: NavigationBarScreen,
    onNavigateDetails: (String) -> Unit,
    onNavigateSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startScreen.route,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
        modifier = modifier
    ) {
        composable(route = NavigationBarScreen.Market.route) {
            val viewModel = remember {
                MarketViewModel(
                    wssManager = appContainer.wssManager,
                    favouritesPreferences = appContainer.favouritesPreferences
                )
            }
            MarketScreen(
                viewModel = viewModel,
                onNavigateDetails = onNavigateDetails,
                onNavigateSettings = onNavigateSettings
            )
        }
        composable(route = NavigationBarScreen.Favourites.route) {
            val viewModel = remember {
                FavouritesViewModel(
                    wssManager = appContainer.wssManager,
                    favouritesPreferences = appContainer.favouritesPreferences
                )
            }
            FavouritesScreen(
                viewModel = viewModel,
                onNavigateDetails = onNavigateDetails,
                onNavigateSettings = onNavigateSettings
            )
        }
        composable(route = NavigationBarScreen.Search.route) {
            val viewModel = remember {
                SearchViewModel(
                    symbolSearchApi = appContainer.symbolSearchApi
                )
            }
            SearchScreen(
                viewModel = viewModel,
                onNavigateDetails = onNavigateDetails,
                onNavigateSettings = onNavigateSettings
            )
        }
    }
}

@Composable
private fun RowScope.AddNavigationBarItem(
    screen: NavigationBarScreen,
    currentDestination: NavDestination?,
    navController: NavHostController,
) {
    val selected = currentDestination?.hierarchy?.any { destination ->
        destination.route == screen.route
    } == true

    NavigationBarItem(
        label = {
            Text(
                text = stringResource(screen.nameResourceId),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        icon = {
            val icon = when (screen) {
                NavigationBarScreen.Market -> Icons.Rounded.BarChart
                NavigationBarScreen.Favourites -> Icons.Rounded.Favorite
                NavigationBarScreen.Pulse -> Icons.Rounded.WifiTethering
                NavigationBarScreen.Search -> Icons.Rounded.Search
            }

            Icon(
                imageVector = icon,
                contentDescription = stringResource(screen.nameResourceId)
            )
        },
        selected = selected,
        onClick = {
            navController.navigate(screen.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.background,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
