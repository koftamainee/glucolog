package com.koftamainee.glucolog.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.koftamainee.glucolog.di.AppContainer
import com.koftamainee.glucolog.domain.DateKeys
import com.koftamainee.glucolog.ui.day.DayScreen
import com.koftamainee.glucolog.ui.day.DayViewModel
import com.koftamainee.glucolog.ui.importexport.ImportExportScreen
import com.koftamainee.glucolog.ui.importexport.ImportExportViewModel
import com.koftamainee.glucolog.ui.roam.RoamScreen
import com.koftamainee.glucolog.ui.roam.RoamViewModel
import com.koftamainee.glucolog.ui.settings.ChartSettingsScreen
import com.koftamainee.glucolog.ui.settings.ChartSettingsViewModel
import com.koftamainee.glucolog.ui.xdrip.XdripSetupScreen
import com.koftamainee.glucolog.ui.xdrip.XdripSetupViewModel

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = "day",
                modifier = Modifier.fillMaxSize(),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                composable("day") {
                    val vm: DayViewModel = viewModel(factory = DayViewModel.factory(container))
                    DayScreen(
                        viewModel = vm,
                        onOpenRoam = { date ->
                            navController.navigate("roam/${DateKeys.key(date)}")
                        },
                    )
                }
                composable(
                    route = "roam/{date}",
                    arguments = listOf(navArgument("date") { type = NavType.StringType }),
                ) {
                    val vm: RoamViewModel = viewModel(factory = RoamViewModel.factory(container))
                    RoamScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("io") {
                    val vm: ImportExportViewModel = viewModel(factory = ImportExportViewModel.factory(container))
                    ImportExportScreen(
                        viewModel = vm,
                        onOpenXdrip = { navController.navigate("xdrip") },
                        onOpenChartSettings = { navController.navigate("chart-settings") },
                    )
                }
                composable("chart-settings") {
                    val vm: ChartSettingsViewModel = viewModel(factory = ChartSettingsViewModel.factory(container))
                    ChartSettingsScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("xdrip") {
                    val vm: XdripSetupViewModel = viewModel(factory = XdripSetupViewModel.factory(container))
                    XdripSetupScreen(
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        if (currentRoute != "roam/{date}") {
            BottomNav(navController = navController)
        }
    }
}

@Composable
private fun BottomNav(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val tabs = listOf(
        BottomTab("day", "День", Icons.Filled.DateRange),
        BottomTab("io", "Настройки", Icons.AutoMirrored.Filled.List),
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { navController.navigateToTab(tab.route) },
                icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)
