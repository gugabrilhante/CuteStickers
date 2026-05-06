package com.gustavo.brilhante.cutecats.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gustavo.brilhante.cutecats.navigation.CuteCatsNavHost
import com.gustavo.brilhante.cutecats.navigation.Screen

@Composable
fun CuteCatsApp() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Cats,
        Screen.Dogs,
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                if (screen == Screen.Cats) Icons.Default.Home else Icons.Default.List, 
                                contentDescription = null
                            ) 
                        },
                        label = { Text(screen.route.capitalize()) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        CuteCatsNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
