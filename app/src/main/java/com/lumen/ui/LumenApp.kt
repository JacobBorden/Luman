package com.lumen.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lumen.viewmodel.AddMomentViewModel
import com.lumen.viewmodel.ExploreViewModel
import com.lumen.viewmodel.FeedViewModel
import com.lumen.R

private enum class LumenDestination(
    val route: String,
    val icon: ImageVector,
    @StringRes val labelRes: Int
) {
    Feed(
        route = "feed",
        icon = Icons.Outlined.GridView,
        labelRes = R.string.feed_tab
    ),
    Add(
        route = "add",
        icon = Icons.Outlined.Add,
        labelRes = R.string.add_tab
    ),
    Explore(
        route = "explore",
        icon = Icons.Outlined.AutoAwesome,
        labelRes = R.string.explore_tab
    )
}

@Composable
fun LumenApp(
    feedViewModel: FeedViewModel,
    addMomentViewModel: AddMomentViewModel,
    exploreViewModel: ExploreViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                LumenDestination.values().forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                        icon = { Icon(destination.icon, contentDescription = null) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = LumenDestination.Feed.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(LumenDestination.Feed.route) {
                FeedScreen(viewModel = feedViewModel)
            }
            composable(LumenDestination.Add.route) {
                AddMomentScreen(viewModel = addMomentViewModel)
            }
            composable(LumenDestination.Explore.route) {
                ExploreScreen(viewModel = exploreViewModel)
            }
        }
    }
}
