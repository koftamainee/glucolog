package com.koftamainee.glucolog.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.koftamainee.glucolog.di.AppContainer
import com.koftamainee.glucolog.ui.day.DayScreen
import com.koftamainee.glucolog.ui.day.DayViewModel

@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "day") {
        composable("day") {
            val vm: DayViewModel = viewModel(factory = DayViewModel.factory(container))
            DayScreen(viewModel = vm)
        }
    }
}
