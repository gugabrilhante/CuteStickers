package com.gustavo.brilhante.cutecats.feature.mediadetails

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.gustavo.brilhante.cutecats.core.designsystem.theme.CuteStickersTheme
import com.gustavo.brilhante.cutecats.feature.stickers.domain.StickerPack
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
    ) { _ -> }

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
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
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
        onDismissStickerSheet = viewModel::DismissStickerSheet
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
    onConfirmExport: (StickerPack) -> Unit,
    onDismissStickerSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.stickerState) {
        when (val state = uiState.stickerState) {
            is StickerState.Error -> snackbarHostState.showSnackbar(state.message)
            else -> Unit
        }
    }

    LaunchedEffect(uiState.downloadState) {
        when (val state = uiState.downloadState) {
            is DownloadState.Error -> snackbarHostState.showSnackbar(state.message)
            is DownloadState.Success -> snackbarHostState.showSnackbar("Saved to gallery!")
            else -> Unit
        }
    }

    with(sharedTransitionScope) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Media Details") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
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
                    contentScale = ContentScale.Crop
                )

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
                            Text("Add to WhatsApp")
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
                            Text("Download")
                        }
                    }
                }
            }

            if (uiState.stickerState is StickerState.Success) {
                ModalBottomSheet(
                    onDismissRequest = onDismissStickerSheet,
                    sheetState = sheetState
                ) {
                    val pack = (uiState.stickerState as StickerState.Success).pack
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
    val stickerFile = remember(pack) {
        File(File(context.filesDir, "stickers/${pack.id}"), pack.stickers.first().imageFileName)
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
            text = "My stickers",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Tap to add this sticker to WhatsApp",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AsyncImage(
            model = stickerFile,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp)),
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
                text = "Add to WhatsApp",
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
