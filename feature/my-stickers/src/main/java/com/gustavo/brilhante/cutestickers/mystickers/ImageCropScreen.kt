package com.gustavo.brilhante.cutestickers.mystickers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gustavo.brilhante.cutestickers.mystickers.R as MyR
import com.gustavo.brilhante.cutestickers.ui.R as UiR

enum class CropMode { ORIGINAL, AUTO_CUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropScreen(
    viewModel: ImageCropViewModel,
    onCropComplete: (android.net.Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.cropResult.collect {
            onCropComplete(it)
        }
    }

    // Pure display interaction state
    var displayScale by remember { mutableFloatStateOf(1f) }
    var displayOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("crop_screen")
    ) {
        val ready = uiState as? ImageCropUiState.Ready
        val bmp = ready?.displayBitmap
        val rotation = ready?.rotation ?: 0f

        if (bmp != null) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val boxW = constraints.maxWidth.toFloat()
                val boxH = constraints.maxHeight.toFloat()
                val cropSizePx = minOf(boxW, boxH) * 0.82f

                val fitScale = run {
                    val rotatedW = if (rotation % 180 == 0f) bmp.width else bmp.height
                    val rotatedH = if (rotation % 180 == 0f) bmp.height else bmp.width
                    maxOf(cropSizePx / rotatedW, cropSizePx / rotatedH)
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(bmp, fitScale, rotation) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (displayScale * zoom).coerceIn(0.5f, 8f)
                                val totalScale = newScale * fitScale
                                val rotatedW = if (rotation % 180 == 0f) bmp.width else bmp.height
                                val rotatedH = if (rotation % 180 == 0f) bmp.height else bmp.width
                                val imgW = rotatedW * totalScale
                                val imgH = rotatedH * totalScale
                                val maxOffX = maxOf(0f, (imgW - cropSizePx) / 2f + cropSizePx * 0.3f)
                                val maxOffY = maxOf(0f, (imgH - cropSizePx) / 2f + cropSizePx * 0.3f)
                                displayScale = newScale
                                displayOffset = Offset(
                                    (displayOffset.x + pan.x).coerceIn(-maxOffX, maxOffX),
                                    (displayOffset.y + pan.y).coerceIn(-maxOffY, maxOffY)
                                )
                            }
                        }
                ) {
                    if (ready.mode == CropMode.AUTO_CUT) {
                        val sz = 24.dp.toPx()
                        val numCols = (boxW / sz).toInt() + 2
                        val numRows = (boxH / sz).toInt() + 2
                        for (row in 0 until numRows) {
                            for (col in 0 until numCols) {
                                drawRect(
                                    color = if ((row + col) % 2 == 0) Color(0xFF888888) else Color(0xFF555555),
                                    topLeft = Offset(col * sz, row * sz),
                                    size = Size(sz, sz)
                                )
                            }
                        }
                    }

                    val totalScale = displayScale * fitScale
                    val rotatedW = if (rotation % 180 == 0f) bmp.width else bmp.height
                    val rotatedH = if (rotation % 180 == 0f) bmp.height else bmp.width
                    val imgW = rotatedW * totalScale
                    val imgH = rotatedH * totalScale
                    val imgLeft = (boxW - imgW) / 2f + displayOffset.x
                    val imgTop = (boxH - imgH) / 2f + displayOffset.y

                    drawContext.canvas.save()
                    drawContext.canvas.translate(imgLeft + imgW / 2f, imgTop + imgH / 2f)
                    drawContext.canvas.rotate(rotation)

                    val drawW = bmp.width * totalScale
                    val drawH = bmp.height * totalScale
                    drawImage(
                        image = bmp.asImageBitmap(),
                        dstOffset = IntOffset((-drawW / 2f).toInt(), (-drawH / 2f).toInt()),
                        dstSize = IntSize(drawW.toInt(), drawH.toInt())
                    )
                    drawContext.canvas.restore()

                    val cropLeft = (boxW - cropSizePx) / 2f
                    val cropTop = (boxH - cropSizePx) / 2f
                    val overlay = Color.Black.copy(alpha = 0.55f)
                    drawRect(overlay, Offset.Zero, Size(boxW, cropTop))
                    drawRect(overlay, Offset(0f, cropTop + cropSizePx), Size(boxW, boxH - cropTop - cropSizePx))
                    drawRect(overlay, Offset(0f, cropTop), Size(cropLeft, cropSizePx))
                    drawRect(overlay, Offset(cropLeft + cropSizePx, cropTop), Size(boxW - cropLeft - cropSizePx, cropSizePx))
                    drawRect(
                        Color.White,
                        Offset(cropLeft, cropTop),
                        Size(cropSizePx, cropSizePx),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = ready.mode == CropMode.ORIGINAL,
                            onClick = { viewModel.onEvent(ImageCropUiEvent.SetMode(CropMode.ORIGINAL)) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            enabled = !ready.isAutoCutting,
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Color.White.copy(alpha = 0.15f),
                                activeContentColor = Color.White,
                                inactiveContentColor = Color.White.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(stringResource(MyR.string.original_photo))
                        }
                        SegmentedButton(
                            selected = ready.mode == CropMode.AUTO_CUT,
                            onClick = { viewModel.onEvent(ImageCropUiEvent.SetMode(CropMode.AUTO_CUT)) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            enabled = !ready.isAutoCutting,
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Color.White.copy(alpha = 0.15f),
                                activeContentColor = Color.White,
                                inactiveContentColor = Color.White.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(stringResource(MyR.string.auto_cut))
                        }
                    }
                    if (ready.autoCutError) {
                        Text(
                            text = stringResource(MyR.string.auto_cut_failed),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                if (ready.isAutoCutting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(UiR.string.cancel), color = Color.White)
                    }
                    OutlinedButton(onClick = {
                        viewModel.onEvent(ImageCropUiEvent.Rotate)
                        displayOffset = Offset.Zero
                    }) {
                        Text(stringResource(UiR.string.rotate), color = Color.White)
                    }
                    Button(
                        onClick = {
                            viewModel.onEvent(ImageCropUiEvent.ConfirmCrop(
                                displayScale = displayScale,
                                displayOffset = displayOffset,
                                boxSize = Size(boxW, boxH),
                                cropSizePx = cropSizePx,
                                fitScale = fitScale
                            ))
                        },
                        enabled = !ready.isCropping && !ready.isAutoCutting,
                        modifier = Modifier.testTag("crop_confirm_button")
                    ) {
                        if (ready.isCropping) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text(stringResource(UiR.string.crop))
                        }
                    }
                }
            }
        } else {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }
    }
}

internal fun computeCropRegion(
    cropLeft: Float,
    cropTop: Float,
    cropSizePx: Float,
    imgLeft: Float,
    imgTop: Float,
    totalScale: Float,
    bmpWidth: Int,
    bmpHeight: Int
): Triple<Int, Int, Int> {
    val bmpX = ((cropLeft - imgLeft) / totalScale).toInt().coerceIn(0, bmpWidth - 1)
    val bmpY = ((cropTop - imgTop) / totalScale).toInt().coerceIn(0, bmpHeight - 1)
    val bmpSize = (cropSizePx / totalScale).toInt()
        .coerceIn(1, minOf(bmpWidth - bmpX, bmpHeight - bmpY))
    return Triple(bmpX, bmpY, bmpSize)
}
