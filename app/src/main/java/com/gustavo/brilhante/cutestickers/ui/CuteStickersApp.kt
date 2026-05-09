package com.gustavo.brilhante.cutestickers.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.gustavo.brilhante.cutestickers.ui.R as UiR
import com.gustavo.brilhante.cutestickers.cats.CatsRoute
import com.gustavo.brilhante.cutestickers.common.PreferencesManager
import com.gustavo.brilhante.cutestickers.dogs.DogsRoute
import com.gustavo.brilhante.cutestickers.mediadetails.MediaDetailsRoute
import com.gustavo.brilhante.cutestickers.navigation.Navigator
import com.gustavo.brilhante.cutestickers.navigation.Screen
import com.gustavo.brilhante.cutestickers.navigation.rememberNavigationState
import com.gustavo.brilhante.cutestickers.navigation.toEntries

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CuteStickersApp(
    preferencesManager: PreferencesManager = hiltViewModel<AppViewModel>().preferencesManager
) {
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
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            val entryProvider = entryProvider<NavKey> {
                entry<Screen.Cats> {
                    CatsRoute(
                        onItemClick = { item -> navigator.navigate(Screen.MediaDetails(item.url, item.id)) },
                        onAboutClick = { navigator.navigate(Screen.About) },
                        sharedTransitionScope = LocalSharedTransitionScope.current!!,
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        preferencesManager = preferencesManager
                    )
                }
                entry<Screen.Dogs> {
                    DogsRoute(
                        onItemClick = { item -> navigator.navigate(Screen.MediaDetails(item.url, item.id)) },
                        onAboutClick = { navigator.navigate(Screen.About) },
                        sharedTransitionScope = LocalSharedTransitionScope.current!!,
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        preferencesManager = preferencesManager
                    )
                }
                entry<Screen.About> {
                    AboutScreen(
                        onBackClick = { navigator.goBack() }
                    )
                }
                entry<Screen.MediaDetails> { details ->
                    MediaDetailsRoute(
                        imageUrl = details.imageUrl,
                        mediaId = details.mediaId,
                        sharedTransitionScope = LocalSharedTransitionScope.current!!,
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                        onBackClick = { navigator.goBack() }
                    )
                }
            }

            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                        windowInsets = NavigationBarDefaults.windowInsets
                    ) {
                        items.forEach { screen ->
                            val selected = screen == navigationState.topLevelRoute
                            NavigationBarItem(
                                icon = {
                                    Text(if (screen is Screen.Cats) "😸" else "🐶")
                                },
                                label = {
                                    Text(
                                        if (screen is Screen.Cats) {
                                            stringResource(UiR.string.cats)
                                        } else {
                                            stringResource(UiR.string.dogs)
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
                val entries = navigationState.toEntries(entryProvider)
                NavDisplay(
                    modifier = Modifier.padding(innerPadding),
                    entries = entries,
                    onBack = { navigator.goBack() },
                    sharedTransitionScope = this
                )
            }
        }
    }
}
