package com.gustavo.brilhante.cutestickers.mystickers

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.gustavo.brilhante.cutestickers.ui.R as UiR
import kotlinx.coroutines.launch

@Composable
fun ImageCropScreen(
    sourceUri: Uri,
    onCropComplete: (Uri) -> Unit,
    onDismiss: () -> Unit,
    processor: CropImageProcessor
) {
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayScale by remember { mutableFloatStateOf(1f) }
    var displayOffset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var isCropping by remember { mutableStateOf(false) }

    LaunchedEffect(sourceUri) {
        bitmap = processor.loadBitmapWithExifCorrection(sourceUri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("crop_screen")
    ) {
        val bmp = bitmap

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
                        rotation = (rotation + 90f) % 360f
                        displayOffset = Offset.Zero
                    }) {
                        Text(stringResource(UiR.string.rotate), color = Color.White)
                    }
                    Button(
                        onClick = {
                            if (isCropping) return@Button
                            isCropping = true
                            scope.launch {
                                val totalScale = displayScale * fitScale
                                val rotatedW = if (rotation % 180 == 0f) bmp.width else bmp.height
                                val rotatedH = if (rotation % 180 == 0f) bmp.height else bmp.width
                                val imgW = rotatedW * totalScale
                                val imgH = rotatedH * totalScale
                                val imgLeft = (boxW - imgW) / 2f + displayOffset.x
                                val imgTop = (boxH - imgH) / 2f + displayOffset.y
                                val cropLeft = (boxW - cropSizePx) / 2f
                                val cropTop = (boxH - cropSizePx) / 2f

                                val (bmpX, bmpY, bmpSize) = computeCropRegion(
                                    cropLeft, cropTop, cropSizePx,
                                    imgLeft, imgTop, totalScale,
                                    rotatedW, rotatedH
                                )

                                val resultUri = processor.saveCroppedBitmap(bmp, bmpX, bmpY, bmpSize, rotation)
                                onCropComplete(resultUri)
                            }
                        },
                        enabled = !isCropping,
                        modifier = Modifier.testTag("crop_confirm_button")
                    ) {
                        if (isCropping) {
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
