package com.gustavo.brilhante.cutecats.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gustavo.brilhante.cutecats.feature.cats.CatsRoute
import com.gustavo.brilhante.cutecats.feature.dogs.DogsRoute

sealed class Screen(val route: String) {
    object Cats : Screen("cats")
    object Dogs : Screen("dogs")
}

@Composable
fun CuteCatsNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Cats.route,
        modifier = modifier
    ) {
        composable(Screen.Cats.route) {
            CatsRoute()
        }
        composable(Screen.Dogs.route) {
            DogsRoute()
        }
    }
}
