package com.music4music.tvguide.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.material3.MaterialTheme
import com.music4music.tvguide.R
import com.music4music.tvguide.ui.calendar.CalendarScreen
import com.music4music.tvguide.ui.categories.CategoriesScreen
import com.music4music.tvguide.ui.channels.ChannelListScreen
import com.music4music.tvguide.ui.detail.ProgramDetailScreen
import com.music4music.tvguide.ui.series.SeriesEpisodesScreen
import com.music4music.tvguide.ui.guide.GuideScreen
import com.music4music.tvguide.ui.now.NowPlayingScreen
import com.music4music.tvguide.ui.settings.SettingsScreen
import com.music4music.tvguide.ui.theme.AccentRed

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    data object Now : Screen("now", R.string.tab_programs, Icons.Default.Tv)
    data object Guide : Screen("guide", R.string.tab_guide, Icons.Default.Schedule)
    data object Categories : Screen("categories", R.string.tab_categories, Icons.Default.Category)
    data object Channels : Screen("channels", R.string.tab_channels, Icons.AutoMirrored.Filled.List)
    data object Calendar : Screen("calendar", R.string.tab_calendar, Icons.Default.CalendarMonth)
    data object Settings : Screen("settings", R.string.tab_settings, Icons.Default.Settings)
}

@Composable
fun TvGuideAppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Now,
        Screen.Guide,
        Screen.Categories,
        Screen.Channels,
        Screen.Calendar
    )

    val showBottomBar = bottomNavItems.any { screen ->
        currentRoute == screen.route ||
            (screen == Screen.Guide && currentRoute?.startsWith("guide") == true) ||
            (screen == Screen.Categories && currentRoute?.startsWith("categories") == true)
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxSize()
        ) {
            if (showBottomBar) {
                androidx.compose.material3.NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route ||
                            (screen == Screen.Guide && currentRoute?.startsWith("guide") == true) ||
                            (screen == Screen.Categories && currentRoute?.startsWith("categories") == true)
                        androidx.compose.material3.NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = stringResource(screen.titleRes)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(screen.titleRes),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            alwaysShowLabel = false,
                            colors = androidx.compose.material3.NavigationRailItemDefaults.colors(
                                selectedIconColor = AccentRed,
                                selectedTextColor = AccentRed,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                }
            }
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AppNavHost(navController = navController, modifier = Modifier.fillMaxSize())
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        bottomNavItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route ||
                                (screen == Screen.Guide && currentRoute?.startsWith("guide") == true) ||
                                (screen == Screen.Categories && currentRoute?.startsWith("categories") == true)
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = stringResource(screen.titleRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(screen.titleRes),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AccentRed,
                                    selectedTextColor = AccentRed,
                                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            AppNavHost(navController = navController, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun AppNavHost(
    navController: androidx.navigation.NavHostController,
    modifier: Modifier = Modifier
) {
    val navigateToChannelGuide: (String) -> Unit = { channelId ->
        navController.navigate("guide?channelId=$channelId") {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Now.route,
        modifier = modifier
    ) {
            composable(Screen.Now.route) {
                NowPlayingScreen(
                    onProgramClick = { programId ->
                        navController.navigate("program/$programId")
                    },
                    onChannelClick = navigateToChannelGuide,
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            composable(
                route = "guide?channelId={channelId}",
                arguments = listOf(
                    navArgument("channelId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val channelId = backStackEntry.arguments?.getString("channelId")
                GuideScreen(
                    onProgramClick = { programId ->
                        navController.navigate("program/$programId")
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    initialChannelId = channelId
                )
            }
            composable(Screen.Categories.route) {
                CategoriesScreen(
                    onProgramClick = { programId ->
                        navController.navigate("program/$programId")
                    },
                    onChannelClick = navigateToChannelGuide,
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    onProgramClick = { programId ->
                        navController.navigate("program/$programId")
                    },
                    onChannelClick = navigateToChannelGuide,
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            composable(Screen.Channels.route) {
                ChannelListScreen(
                    onChannelClick = navigateToChannelGuide,
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = "program/{programId}",
                arguments = listOf(
                    navArgument("programId") { type = NavType.LongType }
                )
            ) {
                ProgramDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onChannelClick = navigateToChannelGuide,
                    onProgramClick = { id -> navController.navigate("program/$id") },
                    onSeasonClick = { id, season ->
                        navController.navigate("series_episodes/$id?season=$season")
                    }
                )
            }
            composable(
                route = "series_episodes/{programId}?season={season}",
                arguments = listOf(
                    navArgument("programId") { type = NavType.LongType },
                    navArgument("season") {
                        type = NavType.IntType
                        defaultValue = 1
                    }
                )
            ) {
                SeriesEpisodesScreen(
                    onBackClick = { navController.popBackStack() },
                    onProgramClick = { id -> navController.navigate("program/$id") }
                )
            }
        }
    }

