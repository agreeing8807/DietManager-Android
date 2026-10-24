package com.example.diet.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.diet.ui.analysis.AnalysisScreen
import com.example.diet.ui.dashboard.DashboardScreen
import com.example.diet.ui.meal.MealScreen
import com.example.diet.ui.onboarding.OnboardingScreen
import com.example.diet.ui.settings.ApiListScreen
import com.example.diet.ui.settings.SettingsScreen
import com.example.diet.ui.water.ReminderScreen
import com.example.diet.ui.water.WaterScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val MEAL = "meal"
    const val WATER = "water"
    const val ANALYSIS = "analysis"
    const val SETTINGS = "settings"
    const val REMINDER = "reminder"
    const val ONBOARDING = "onboarding"
    const val API_LIST = "api_list"
}

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD, "首页", Icons.Filled.Home),
    BottomNavItem(Routes.MEAL, "记录", Icons.Filled.Restaurant),
    BottomNavItem(Routes.WATER, "喝水", Icons.Filled.LocalDrink),
    BottomNavItem(Routes.ANALYSIS, "分析", Icons.Filled.Analytics),
    BottomNavItem(Routes.SETTINGS, "设置", Icons.Filled.Settings)
)

@Composable
fun DietNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    // 首次启动检测：已完成引导进首页，否则进引导页
    val startDestination = remember {
        if (context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getBoolean("onboarded", false)
        ) Routes.DASHBOARD else Routes.ONBOARDING
    }
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.ONBOARDING) { OnboardingScreen(navController) }
            composable(Routes.DASHBOARD) { DashboardScreen(navController) }
            composable(Routes.MEAL) { MealScreen() }
            composable(Routes.WATER) { WaterScreen(navController) }
            composable(Routes.ANALYSIS) { AnalysisScreen() }
            composable(Routes.SETTINGS) { SettingsScreen(navController) }
            composable(Routes.API_LIST) { ApiListScreen(navController) }
            composable(Routes.REMINDER) { ReminderScreen(navController) }
        }
    }
}
