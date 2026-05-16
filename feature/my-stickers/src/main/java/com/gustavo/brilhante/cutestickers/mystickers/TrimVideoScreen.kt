@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@file:kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.gustavo.brilhante.cutestickers.mystickers

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.sqrt

private fun Offset.distance(other: Offset): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt(dx * dx + dy * dy)
}

@Composable
fun TrimVideoScreen(
    viewModel: TrimVideoViewModel,
    onTrimComplete: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Initial video load
    LaunchedEffect(uiState.videoUri) {
        uiState.videoUri?.let { uri ->
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = isPlaying
        }
    }

    // Handle trim changes
    LaunchedEffect(uiState.startTimeMs, uiState.endTimeMs) {
        exoPlayer.seekTo(uiState.startTimeMs)
    }

    // Looping and position tracking
    LaunchedEffect(exoPlayer, uiState.startTimeMs, uiState.endTimeMs) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            if (currentPosition >= uiState.endTimeMs || currentPosition < uiState.startTimeMs) {
                exoPlayer.seekTo(uiState.startTimeMs)
            }
            delay(16)
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Trim & Crop", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        if (uiState.isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            IconButton(
                                onClick = { viewModel.processVideo(onTrimComplete) },
                                enabled = uiState.isDurationValid
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Done")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onGloballyPositioned { containerSize = it.size },
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (containerSize != IntSize.Zero && uiState.videoWidth > 0) {
                        CropOverlay(
                            videoWidth = uiState.videoWidth,
                            videoHeight = uiState.videoHeight,
                            containerWidth = containerSize.width,
                            containerHeight = containerSize.height,
                            cropRect = uiState.cropRect,
                            isSquare = uiState.isSquareCrop,
                            onCropChanged = viewModel::onCropChanged
                        )
                    }

                    // Play/Pause overlay
                    IconButton(
                        onClick = { 
                            isPlaying = !isPlaying
                            exoPlayer.playWhenReady = isPlaying
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toolbar Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Time Indicator
                    val currentRelPos = (currentPosition - uiState.startTimeMs).coerceAtLeast(0)
                    val totalSelected = (uiState.endTimeMs - uiState.startTimeMs).coerceAtLeast(0)
                    
                    val currentSec = (currentRelPos / 1000).toInt()
                    val totalSec = (totalSelected / 1000).toInt()
                    
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d / %02d:%02d", currentSec / 60, currentSec % 60, totalSec / 60, totalSec % 60),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = { viewModel.toggleSquareCrop() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isSquareCrop) MaterialTheme.colorScheme.primary else Color.DarkGray,
                            contentColor = if (uiState.isSquareCrop) MaterialTheme.colorScheme.onPrimary else Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("1:1", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                VideoTrimmer(
                    durationMs = uiState.durationMs,
                    startTimeMs = uiState.startTimeMs,
                    endTimeMs = uiState.endTimeMs,
                    currentPositionMs = currentPosition,
                    thumbnails = uiState.thumbnails,
                    onTrimChanged = viewModel::onTrimChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                if (!uiState.isDurationValid) {
                    Text(
                        text = "Duration must be less than ${AnimatedStickerProcessor.MAX_DURATION_MS / 1000} seconds",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp),
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun CropOverlay(
    videoWidth: Int,
    videoHeight: Int,
    containerWidth: Int,
    containerHeight: Int,
    cropRect: android.graphics.RectF,
    isSquare: Boolean = false,
    onCropChanged: (android.graphics.RectF) -> Unit
) {
    // Calculate actual video display size in the container (Fit center)
    val containerRatio = containerWidth.toFloat() / containerHeight
    val videoRatio = videoWidth.toFloat() / videoHeight
    
    val (displayW, displayH) = if (videoRatio > containerRatio) {
        containerWidth.toFloat() to containerWidth.toFloat() / videoRatio
    } else {
        containerHeight.toFloat() * videoRatio to containerHeight.toFloat()
    }

    val leftOffset = (containerWidth - displayW) / 2
    val topOffset = (containerHeight - displayH) / 2

    val currentCropRect by rememberUpdatedState(cropRect)
    val currentOnCropChanged by rememberUpdatedState(onCropChanged)
    
    var dragHandle by remember { mutableStateOf<DragHandle?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(displayW, displayH, isSquare) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val rect = Rect(
                            offset = Offset(leftOffset + displayW * currentCropRect.left, topOffset + displayH * currentCropRect.top),
                            size = Size(displayW * currentCropRect.width(), displayH * currentCropRect.height())
                        )
                        val handleSize = 40.dp.toPx()
                        dragHandle = when {
                            Offset(rect.left, rect.top).distance(offset) < handleSize -> DragHandle.TopLeft
                            Offset(rect.right, rect.top).distance(offset) < handleSize -> DragHandle.TopRight
                            Offset(rect.left, rect.bottom).distance(offset) < handleSize -> DragHandle.BottomLeft
                            Offset(rect.right, rect.bottom).distance(offset) < handleSize -> DragHandle.BottomRight
                            rect.contains(offset) -> DragHandle.Center
                            else -> null
                        }
                    },
                    onDrag = { change, dragAmount ->
                        val handle = dragHandle ?: return@detectDragGestures
                        change.consume()
                        
                        val deltaX = dragAmount.x / displayW
                        val deltaY = dragAmount.y / displayH
                        val newRect = android.graphics.RectF(currentCropRect)
                        val minSize = 0.1f
                        val vWidth = videoWidth.toFloat()
                        val vHeight = videoHeight.toFloat()

                        when (handle) {
                            DragHandle.TopLeft -> {
                                if (isSquare) {
                                    val dX = deltaX
                                    val dY = dX * vWidth / vHeight
                                    newRect.left = (newRect.left + dX).coerceIn(0f, newRect.right - minSize)
                                    newRect.top = (newRect.top + dY).coerceIn(0f, newRect.bottom - minSize)
                                    
                                    // Re-adjust if top went out of bounds
                                    if (newRect.top == 0f || newRect.top >= newRect.bottom - minSize) {
                                        val actualDY = newRect.top - currentCropRect.top
                                        val actualDX = actualDY * vHeight / vWidth
                                        newRect.left = currentCropRect.left + actualDX
                                    }
                                } else {
                                    newRect.left = (newRect.left + deltaX).coerceIn(0f, newRect.right - minSize)
                                    newRect.top = (newRect.top + deltaY).coerceIn(0f, newRect.bottom - minSize)
                                }
                            }
                            DragHandle.TopRight -> {
                                if (isSquare) {
                                    val dX = deltaX
                                    val dY = -dX * vWidth / vHeight
                                    newRect.right = (newRect.right + dX).coerceIn(newRect.left + minSize, 1f)
                                    newRect.top = (newRect.top + dY).coerceIn(0f, newRect.bottom - minSize)
                                    
                                    if (newRect.top == 0f || newRect.top >= newRect.bottom - minSize) {
                                        val actualDY = newRect.top - currentCropRect.top
                                        val actualDX = -actualDY * vHeight / vWidth
                                        newRect.right = currentCropRect.right + actualDX
                                    }
                                } else {
                                    newRect.right = (newRect.right + deltaX).coerceIn(newRect.left + minSize, 1f)
                                    newRect.top = (newRect.top + deltaY).coerceIn(0f, newRect.bottom - minSize)
                                }
                            }
                            DragHandle.BottomLeft -> {
                                if (isSquare) {
                                    val dX = deltaX
                                    val dY = -dX * vWidth / vHeight
                                    newRect.left = (newRect.left + dX).coerceIn(0f, newRect.right - minSize)
                                    newRect.bottom = (newRect.bottom + dY).coerceIn(newRect.top + minSize, 1f)
                                    
                                    if (newRect.bottom == 1f || newRect.bottom <= newRect.top + minSize) {
                                        val actualDY = newRect.bottom - currentCropRect.bottom
                                        val actualDX = -actualDY * vHeight / vWidth
                                        newRect.left = currentCropRect.left + actualDX
                                    }
                                } else {
                                    newRect.left = (newRect.left + deltaX).coerceIn(0f, newRect.right - minSize)
                                    newRect.bottom = (newRect.bottom + deltaY).coerceIn(newRect.top + minSize, 1f)
                                }
                            }
                            DragHandle.BottomRight -> {
                                if (isSquare) {
                                    val dX = deltaX
                                    val dY = dX * vWidth / vHeight
                                    newRect.right = (newRect.right + dX).coerceIn(newRect.left + minSize, 1f)
                                    newRect.bottom = (newRect.bottom + dY).coerceIn(newRect.top + minSize, 1f)
                                    
                                    if (newRect.bottom == 1f || newRect.bottom <= newRect.top + minSize) {
                                        val actualDY = newRect.bottom - currentCropRect.bottom
                                        val actualDX = actualDY * vHeight / vWidth
                                        newRect.right = currentCropRect.right + actualDX
                                    }
                                } else {
                                    newRect.right = (newRect.right + deltaX).coerceIn(newRect.left + minSize, 1f)
                                    newRect.bottom = (newRect.bottom + deltaY).coerceIn(newRect.top + minSize, 1f)
                                }
                            }
                            DragHandle.Center -> {
                                val width = newRect.width()
                                val height = newRect.height()
                                newRect.left = (newRect.left + deltaX).coerceIn(0f, 1f - width)
                                newRect.right = newRect.left + width
                                newRect.top = (newRect.top + deltaY).coerceIn(0f, 1f - height)
                                newRect.bottom = newRect.top + height
                            }
                        }
                        currentOnCropChanged(newRect)
                    },
                    onDragEnd = { dragHandle = null }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rect = Rect(
                offset = Offset(leftOffset + displayW * cropRect.left, topOffset + displayH * cropRect.top),
                size = Size(displayW * cropRect.width(), displayH * cropRect.height())
            )

            // Dim everything outside the crop area
            val path = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
                addRect(rect)
            }
            drawPath(path, Color.Black.copy(alpha = 0.5f), style = androidx.compose.ui.graphics.drawscope.Fill)
            
            // Draw crop border
            drawRect(
                color = Color.White,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw handles
            val handleRadius = 6.dp.toPx()
            drawCircle(Color.White, handleRadius, rect.topLeft)
            drawCircle(Color.White, handleRadius, Offset(rect.right, rect.top))
            drawCircle(Color.White, handleRadius, Offset(rect.left, rect.bottom))
            drawCircle(Color.White, handleRadius, rect.bottomRight)
        }
    }
}

enum class DragHandle {
    TopLeft, TopRight, BottomLeft, BottomRight, Center
}

@Composable
fun VideoTrimmer(
    durationMs: Long,
    startTimeMs: Long,
    endTimeMs: Long,
    currentPositionMs: Long,
    thumbnails: List<android.graphics.Bitmap>,
    onTrimChanged: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        ) {
            // Thumbnails
            Row(modifier = Modifier.fillMaxSize()) {
                if (thumbnails.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Gray))
                } else {
                    thumbnails.forEach { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Selection Overlay
            val startRatio = if (durationMs > 0) startTimeMs.toFloat() / durationMs else 0f
            val endRatio = if (durationMs > 0) endTimeMs.toFloat() / durationMs else 1f
            
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // Dimmed areas
                Box(modifier = Modifier
                    .fillMaxHeight()
                    .width(maxWidth * startRatio)
                    .background(Color.Black.copy(alpha = 0.6f)))
                
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(maxWidth * (1f - endRatio))
                        .background(Color.Black.copy(alpha = 0.6f))
                )
                
                // Border for selection
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(maxWidth * (endRatio - startRatio))
                        .offset(x = maxWidth * startRatio)
                        .background(Color.Transparent)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                )
            }
            
            // RangeSlider on top
            RangeSlider(
                value = startTimeMs.toFloat()..endTimeMs.toFloat(),
                onValueChange = { range ->
                    onTrimChanged(range.start.toLong(), range.endInclusive.toLong())
                },
                valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxSize(),
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    thumbColor = Color.White
                )
            )
            
            // Cursor
            val cursorRatio = if (durationMs > 0) currentPositionMs.toFloat() / durationMs else 0f
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val xOffset = maxWidth * cursorRatio
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .offset(x = xOffset)
                        .background(Color.Yellow)
                )
            }
        }
    }
}