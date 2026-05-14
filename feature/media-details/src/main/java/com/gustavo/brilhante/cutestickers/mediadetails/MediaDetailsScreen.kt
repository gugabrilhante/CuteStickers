package com.gustavo.brilhante.cutestickers.mediadetails

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.gustavo.brilhante.cutestickers.ui.R as UiR
import com.gustavo.brilhante.cutestickers.designsystem.theme.CuteStickersTheme
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerPack
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MediaDetailsRoute(
    imageUrl: String,
    mediaId: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit,
    viewModel: MediaDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(imageUrl, mediaId) {
        viewModel.init(imageUrl, mediaId)
    }

    val stickerImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onExportResult(result.resultCode, result.data)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MediaDetailsEvent.LaunchIntent -> stickerImportLauncher.launch(event.intent)
            }
        }
    }

    val downloadPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onDownloadMedia()
        else viewModel.onDownloadPermissionDenied()
    }

    val onDownloadClick = {
        val needsPermission = (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED)
        if (needsPermission) {
            downloadPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.onDownloadMedia()
        }
    }

    MediaDetailsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onBackClick = onBackClick,
        onAddToWhatsApp = viewModel::onAddToWhatsApp,
        onDownload = onDownloadClick,
        onConfirmExport = viewModel::onConfirmExport,
        onDismissStickerSheet = viewModel::dismissStickerSheet,
        onToggleCrop = viewModel::onToggleCrop
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MediaDetailsScreen(
    uiState: MediaDetailsUiState,
    snackbarHostState: SnackbarHostState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBackClick: () -> Unit,
    onAddToWhatsApp: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
    onConfirmExport: (StickerPack) -> Unit = {},
    onDismissStickerSheet: () -> Unit = {},
    onToggleCrop: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val savedToGalleryMessage = stringResource(UiR.string.saved_to_gallery)

    LaunchedEffect(uiState.stickerState) {
        when (val state = uiState.stickerState) {
            is StickerState.Error -> snackbarHostState.showSnackbar(state.message)
            else -> Unit
        }
    }

    LaunchedEffect(uiState.downloadState) {
        when (val state = uiState.downloadState) {
            is DownloadState.Error -> snackbarHostState.showSnackbar(state.message)
            is DownloadState.Success -> snackbarHostState.showSnackbar(savedToGalleryMessage)
            else -> Unit
        }
    }

    with(sharedTransitionScope) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(UiR.string.media_details),
                            modifier = Modifier.testTag("media_details_title"),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(UiR.string.back)
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Box {
                    AsyncImage(
                        model = uiState.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .sharedElement(
                                rememberSharedContentState(key = "image-${uiState.mediaId}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                            .clip(RoundedCornerShape(0.dp))
                            .testTag("hero_image"),
                        contentScale = if (uiState.isCropped) ContentScale.Crop else ContentScale.Fit
                    )
                    
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.isCropped,
                            onClick = onToggleCrop,
                            label = { Text(stringResource(UiR.string.crop)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Black.copy(alpha = 0.4f),
                                labelColor = Color.White,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )

                        if (uiState.mediaType is com.gustavo.brilhante.cutestickers.model.MediaType.Animated) {
                            Text(
                                text = stringResource(UiR.string.gif),
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAddToWhatsApp,
                        enabled = uiState.stickerState !is StickerState.Loading,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_sticker_button")
                    ) {
                        if (uiState.stickerState is StickerState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(UiR.string.add_to_whatsapp))
                        }
                    }

                    OutlinedButton(
                        onClick = onDownload,
                        enabled = uiState.downloadState !is DownloadState.Loading,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_button")
                    ) {
                        if (uiState.downloadState is DownloadState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(UiR.string.download))
                        }
                    }
                }
            }

            if (uiState.stickerState is StickerState.Success) {
                ModalBottomSheet(
                    onDismissRequest = onDismissStickerSheet,
                    sheetState = sheetState
                ) {
                    val pack = uiState.stickerState.pack
                    StickerPreviewContent(
                        pack = pack,
                        onConfirm = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                onConfirmExport(pack)
                                onDismissStickerSheet()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StickerPreviewContent(
    pack: StickerPack,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    // For preview, we show only the LATEST sticker added to the pack
    val stickerFile = remember(pack) {
        val lastStickerFileName = pack.stickers.last().imageFileName
        File(File(context.filesDir, "stickers/${pack.id}"), lastStickerFileName)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(UiR.string.my_stickers),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(UiR.string.tap_to_add_to_whatsapp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AsyncImage(
            model = stickerFile,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .testTag("sticker_preview_image"),
            contentScale = ContentScale.Fit
        )

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(UiR.string.add_to_whatsapp),
                modifier = Modifier.padding(vertical = 4.dp),
                fontSize = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true)
@Composable
fun MediaDetailsScreenPreview() {
    CuteStickersTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                MediaDetailsScreen(
                    uiState = MediaDetailsUiState(
                        imageUrl = "https://example.com/item.gif",
                        mediaId = "1"
                    ),
                    snackbarHostState = remember { SnackbarHostState() },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this,
                    onBackClick = {},
                    onAddToWhatsApp = {},
                    onDownload = {},
                    onConfirmExport = {},
                    onDismissStickerSheet = {}
                )
            }
        }
    }
}
