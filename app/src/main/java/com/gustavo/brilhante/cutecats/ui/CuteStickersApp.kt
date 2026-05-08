package com.gustavo.brilhante.cutecats.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.compose.ui.res.stringResource
import com.gustavo.brilhante.cutecats.R
import com.gustavo.brilhante.cutecats.feature.cats.CatsRoute
import com.gustavo.brilhante.cutecats.feature.dogs.DogsRoute
import com.gustavo.brilhante.cutecats.feature.mediadetails.MediaDetailsRoute
import com.gustavo.brilhante.cutecats.navigation.Navigator
import com.gustavo.brilhante.cutecats.navigation.Screen
import com.gustavo.brilhante.cutecats.navigation.rememberNavigationState
import com.gustavo.brilhante.cutecats.navigation.toEntries

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CuteStickersApp() {
    val items = listOf(
        Screen.Cats,
        Screen.Dogs,
    )
    val navigationState = rememberNavigationState(
        startRoute = Screen.Cats,
        topLevelRoutes = items.toSet()
    )
    val navigator = remember { Navigator(navigationState) }

    SharedTransitionLayout {
        val entryProvider = entryProvider<NavKey> {
            entry<Screen.Cats> {
                AnimatedVisibility(
                    visible = true,
                    enter = EnterTransition.None,
                    exit = ExitTransition.None
                ) {
                    CatsRoute(
                        onItemClick = { item -> navigator.navigate(Screen.MediaDetails(item.url, item.id)) },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                }
            }
            entry<Screen.Dogs> {
                AnimatedVisibility(
                    visible = true,
                    enter = EnterTransition.None,
                    exit = ExitTransition.None
                ) {
                    DogsRoute(
                        onItemClick = { item -> navigator.navigate(Screen.MediaDetails(item.url, item.id)) },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                }
            }
            entry<Screen.MediaDetails> { details ->
                AnimatedVisibility(
                    visible = true,
                    enter = EnterTransition.None,
                    exit = ExitTransition.None
                ) {
                    MediaDetailsRoute(
                        imageUrl = details.imageUrl,
                        mediaId = details.mediaId,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this,
                        onBackClick = { navigator.goBack() }
                    )
                }
            }
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    items.forEach { screen ->
                        val selected = screen == navigationState.topLevelRoute
                        NavigationBarItem(
                            icon = {
                                Text(if (screen is Screen.Cats) "😸" else "🐶")
                            },
                            label = { 
                                Text(
                                    if (screen is Screen.Cats) {
                                        stringResource(R.string.cats)
                                    } else {
                                        stringResource(R.string.dogs)
                                    }
                                )
                            },
                            selected = selected,
                            onClick = {
                                navigator.navigate(screen)
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavDisplay(
                modifier = Modifier.padding(innerPadding),
                entries = navigationState.toEntries(entryProvider),
                onBack = { navigator.goBack() }
            )
        }
    }
}
