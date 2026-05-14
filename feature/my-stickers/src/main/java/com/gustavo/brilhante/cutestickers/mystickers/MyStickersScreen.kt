package com.gustavo.brilhante.cutestickers.mystickers

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.net.Uri
import com.gustavo.brilhante.cutestickers.model.MediaItem
import com.gustavo.brilhante.cutestickers.mystickers.R as MyR
import com.gustavo.brilhante.cutestickers.ui.R as UiR
import com.gustavo.brilhante.cutestickers.ui.MediaCard

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MyStickersRoute(
    onItemClick: (MediaItem) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: MyStickersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { pendingCropUri = it }
    }

    if (uiState is MyStickersUiState.Success) {
        val error = (uiState as MyStickersUiState.Success).importError
        LaunchedEffect(error) {
            if (error != null) {
                snackbarHostState.showSnackbar(error)
                viewModel.clearImportError()
            }
        }
    }

    val successState = uiState as? MyStickersUiState.Success
    val isInSelectionMode = successState?.selectedIds?.isNotEmpty() == true

    MyStickersScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onItemClick = { item ->
            if (isInSelectionMode) viewModel.toggleSelection(item.id) else onItemClick(item)
        },
        onImportClick = {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onItemLongClick = { item -> viewModel.toggleSelection(item.id) },
        onDeleteSelected = viewModel::deleteSelected,
        onClearSelection = viewModel::clearSelection,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        modifier = modifier
    )

    pendingCropUri?.let { uri ->
        ImageCropScreen(
            sourceUri = uri,
            onCropComplete = { croppedUri ->
                pendingCropUri = null
                viewModel.importFromGallery(croppedUri.toString())
            },
            onDismiss = { pendingCropUri = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MyStickersScreen(
    uiState: MyStickersUiState,
    snackbarHostState: SnackbarHostState,
    onItemClick: (MediaItem) -> Unit,
    onImportClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    onItemLongClick: (MediaItem) -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onClearSelection: () -> Unit = {}
) {
    val successState = uiState as? MyStickersUiState.Success
    val selectedIds = successState?.selectedIds ?: emptySet()
    val isInSelectionMode = selectedIds.isNotEmpty()

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isInSelectionMode) stringResource(UiR.string.selected_count, selectedIds.size)
                        else stringResource(MyR.string.my_stickers)
                    )
                },
                navigationIcon = {
                    if (isInSelectionMode) {
                        IconButton(
                            onClick = onClearSelection,
                            modifier = Modifier.testTag("clear_selection_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(UiR.string.clear_selection))
                        }
                    }
                },
                actions = {},
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.offset(y = 20.dp)
            ) {
                AnimatedVisibility(
                    visible = isInSelectionMode,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = onDeleteSelected,
                        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        text = { Text(stringResource(UiR.string.delete)) },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.testTag("delete_selected_fab")
                    )
                }

                ExtendedFloatingActionButton(
                    onClick = onImportClick,
                    icon = {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = stringResource(MyR.string.import_from_gallery)
                        )
                    },
                    text = { Text(stringResource(MyR.string.add)) },
                    modifier = Modifier.testTag("import_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            when (uiState) {
                is MyStickersUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is MyStickersUiState.Empty -> {
                    EmptyMyStickersContent(
                        onImportClick = onImportClick,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is MyStickersUiState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            end = 8.dp,
                            top = 8.dp,
                            bottom = innerPadding.calculateBottomPadding() + 60.dp
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("my_stickers_grid")
                    ) {
                        items(uiState.items, key = { it.id }) { item ->
                            MediaCard(
                                item = item,
                                onItemClick = onItemClick,
                                onItemLongClick = onItemLongClick,
                                isSelected = item.id in uiState.selectedIds,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                badgeText = stringResource(MyR.string.tap_to_send),
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    if (uiState.isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .testTag("import_progress")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMyStickersContent(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⭐",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(MyR.string.my_stickers),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("empty_title")
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(MyR.string.empty_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onImportClick,
            modifier = Modifier.testTag("import_button")
        ) {
            Text(stringResource(MyR.string.import_from_gallery))
        }
    }
}
