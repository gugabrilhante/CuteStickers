package com.gustavo.brilhante.cutestickers.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.gustavo.brilhante.cutestickers.ui.R as UiR
import com.gustavo.brilhante.cutestickers.model.MediaItem

sealed interface DiscoverUiState {
    data object Loading : DiscoverUiState
    data class Success(
        val items: List<MediaItem>,
        override val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        override val isOffline: Boolean = false
    ) : DiscoverUiState
    data class Error(
        val message: String,
        val isNoInternet: Boolean = false,
        override val isRefreshing: Boolean = false
    ) : DiscoverUiState

    val isRefreshing: Boolean
        get() = false

    val isOffline: Boolean
        get() = false
}

sealed interface DiscoverUiEvent {
    data object Refresh : DiscoverUiEvent
    data object LoadMore : DiscoverUiEvent
    data class OnItemClick(val item: MediaItem) : DiscoverUiEvent
    data class OnItemLongClick(val item: MediaItem) : DiscoverUiEvent
    data object ClearSelection : DiscoverUiEvent
    data object SaveSelection : DiscoverUiEvent
    data object AboutClick : DiscoverUiEvent
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DiscoverScreen(
    uiState: DiscoverUiState,
    onEvent: (DiscoverUiEvent) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    badgeText: String,
    onboardingMessage: String,
    okText: String,
    showOnboarding: Boolean,
    onOnboardingDismissed: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    selectedIds: Set<String> = emptySet(),
    offlinePlaceholderRes: Int = 0
) {
    val gridState = rememberLazyGridState()

    BackHandler(enabled = selectedIds.isNotEmpty()) {
        onEvent(DiscoverUiEvent.ClearSelection)
    }

    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItemsCount = gridState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= totalItemsCount - 4 && totalItemsCount > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && uiState is DiscoverUiState.Success && !uiState.isLoadingMore) {
            onEvent(DiscoverUiEvent.LoadMore)
        }
    }

    if (showOnboarding) {
        AlertDialog(
            onDismissRequest = onOnboardingDismissed,
            confirmButton = {
                TextButton(onClick = onOnboardingDismissed) {
                    Text(okText)
                }
            },
            title = { Text(title) },
            text = { Text(onboardingMessage) }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(title) },
                    actions = {
                        IconButton(onClick = { onEvent(DiscoverUiEvent.AboutClick) }) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = stringResource(UiR.string.about))
                        }
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background
                    )
                )
                AnimatedVisibility(
                    visible = uiState.isOffline,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    OfflineBanner(
                        text = stringResource(UiR.string.offline_banner),
                        modifier = Modifier.testTag("offline_banner")
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedIds.isNotEmpty() && !uiState.isRefreshing,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { onEvent(DiscoverUiEvent.SaveSelection) },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    text = { Text(stringResource(UiR.string.save_selection)) },
                    modifier = Modifier
                        .testTag("save_selection_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            when (uiState) {
                is DiscoverUiState.Loading -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            end = 8.dp,
                            top = 8.dp,
                            bottom = innerPadding.calculateBottomPadding() + 60.dp
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(10) {
                            ShimmerItem()
                        }
                    }
                }
                is DiscoverUiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { onEvent(DiscoverUiEvent.Refresh) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = gridState,
                            contentPadding = PaddingValues(
                                start = 8.dp,
                                end = 8.dp,
                                top = 8.dp,
                                bottom = innerPadding.calculateBottomPadding() + 80.dp
                            ),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.items, key = { it.id }) { item ->
                                MediaCard(
                                    item = item,
                                    onItemClick = { onEvent(DiscoverUiEvent.OnItemClick(it)) },
                                    onItemLongClick = { onEvent(DiscoverUiEvent.OnItemLongClick(it)) },
                                    isSelected = item.id in selectedIds,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    badgeText = badgeText,
                                    offlinePlaceholderRes = offlinePlaceholderRes,
                                    modifier = Modifier.animateItem()
                                )
                            }
                            if (uiState.isLoadingMore) {
                                items(2) {
                                    ShimmerItem()
                                }
                            }
                        }
                    }
                }
                is DiscoverUiState.Error -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { onEvent(DiscoverUiEvent.Refresh) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isNoInternet) Icons.Default.CloudOff else Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.isNoInternet) {
                                    stringResource(UiR.string.error_no_internet)
                                } else {
                                    stringResource(UiR.string.error_message)
                                },
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { onEvent(DiscoverUiEvent.Refresh) }) {
                                Text(text = stringResource(UiR.string.retry))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MediaCard(
    item: MediaItem,
    onItemClick: (MediaItem) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    badgeText: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onItemLongClick: (MediaItem) -> Unit = {},
    offlinePlaceholderRes: Int = 0
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "scale_animation"
    )

    var isLoaded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(scale)
            .testTag("media_card"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        with(sharedTransitionScope) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = isLoaded,
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongClick(item) }
                    )
            ) {
                SubcomposeAsyncImage(
                    model = item.url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedElement(
                            rememberSharedContentState(key = "image-${item.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                    loading = {
                        ShimmerBox()
                    },
                    onSuccess = {
                        isLoaded = true
                    },
                    error = {
                        isLoaded = false
                        OfflineImagePlaceholder(offlinePlaceholderRes)
                    }
                )

                if (isLoaded) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                            .padding(top = 16.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (item.type is com.gustavo.brilhante.cutestickers.model.MediaType.Animated) {
                        Text(
                            text = stringResource(UiR.string.gif),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerItem() {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ShimmerBox()
    }
}

@Composable
fun OfflineBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun OfflineImagePlaceholder(placeholderRes: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (placeholderRes != 0) {
            Icon(
                painter = painterResource(placeholderRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun ShimmerBox() {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
    )
}
