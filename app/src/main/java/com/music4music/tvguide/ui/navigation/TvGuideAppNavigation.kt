package com.music4music.tvguide.ui.navigation

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
import com.music4music.tvguide.ui.channels.ChannelListScreen
import com.music4music.tvguide.ui.detail.ProgramDetailScreen
import com.music4music.tvguide.ui.guide.GuideScreen
import com.music4music.tvguide.ui.now.NowPlayingScreen
import com.music4music.tvguide.ui.settings.SettingsScreen
import com.music4music.tvguide.ui.theme.AccentRed

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    data object Now : Screen("now", R.string.tab_programs, Icons.Default.Tv)
    data object Guide : Screen("guide", R.string.tab_guide, Icons.Default.Schedule)
    data object Calendar : Screen("calendar", R.string.tab_calendar, Icons.Default.CalendarMonth)
    data object Channels : Screen("channels", R.string.tab_channels, Icons.AutoMirrored.Filled.List)
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
        Screen.Channels,
        Screen.Calendar
    )

    val showBottomBar = bottomNavItems.any { screen ->
        currentRoute == screen.route || (screen == Screen.Guide && currentRoute?.startsWith("guide") == true)
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route ||
                            (screen == Screen.Guide && currentRoute?.startsWith("guide") == true)
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
                            label = { Text(text = stringResource(screen.titleRes)) },
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
        NavHost(
            navController = navController,
            startDestination = Screen.Now.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Now.route) {
                NowPlayingScreen(
                    onProgramClick = { programId ->
                        navController.navigate("program/$programId")
                    },
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
            composable("categories") {
                GuideScreen(
                    onProgramClick = { programId ->
                        navController.navigate("program/$programId")
                    },
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
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            composable(Screen.Channels.route) {
                ChannelListScreen(
                    onChannelClick = { channelId ->
                        navController.navigate("guide?channelId=$channelId") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
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
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
