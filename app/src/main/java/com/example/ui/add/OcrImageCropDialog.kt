package com.example.ui.add

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.ocr.OcrExtractor
import com.example.ui.theme.StudyBluePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private enum class CropDragMode {
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    LEFT_EDGE,
    RIGHT_EDGE,
    TOP_EDGE,
    BOTTOM_EDGE,
    MOVE_INSIDE
}

/**
 * Interactive region-selection dialog so users can optionally frame only the word column
 * (excluding checkboxes `□`, list numbers, or modern Japanese translations) before OCR scanning.
 */
@Composable
fun OcrImageCropDialog(
    imageUri: Uri,
    isClassical: Boolean,
    onDismiss: () -> Unit,
    onUseFullImage: () -> Unit,
    onCropConfirmed: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var bitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(imageUri) { mutableStateOf(false) }
    var isSavingCrop by remember { mutableStateOf(false) }

    // Normalized crop coordinates in [0f, 1f] relative to rendered image bounds
    var cropLeft by remember(imageUri) { mutableFloatStateOf(0.08f) }
    var cropTop by remember(imageUri) { mutableFloatStateOf(0.05f) }
    var cropRight by remember(imageUri) { mutableFloatStateOf(0.92f) }
    var cropBottom by remember(imageUri) { mutableFloatStateOf(0.95f) }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var activeDragMode by remember { mutableStateOf(CropDragMode.NONE) }

    LaunchedEffect(imageUri) {
        val loaded = withContext(Dispatchers.IO) {
            OcrExtractor.loadNormalizedBitmap(context, imageUri)
        }
        if (loaded != null) {
            bitmap = loaded
        } else {
            loadFailed = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            bitmap?.let {
                if (!it.isRecycled) {
                    it.recycle()
                }
            }
        }
    }

    fun computeImageDisplayRect(bmp: Bitmap, boxSize: IntSize): Rect {
        if (boxSize.width <= 0 || boxSize.height <= 0 || bmp.width <= 0 || bmp.height <= 0) {
            return Rect.Zero
        }
        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()
        val cw = boxSize.width.toFloat()
        val ch = boxSize.height.toFloat()
        val scale = min(cw / bw, ch / bh)
        val drawW = bw * scale
        val drawH = bh * scale
        val offsetX = (cw - drawW) / 2f
        val offsetY = (ch - drawH) / 2f
        return Rect(offsetX, offsetY, offsetX + drawW, offsetY + drawH)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Crop,
                            contentDescription = null,
                            tint = StudyBluePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "読み取り範囲を指定",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isClassical) {
                                    "チェック欄や現代語訳を避け、古語の列・箇所だけを枠で囲めます"
                                } else {
                                    "チェック欄（□）や日本語訳を避け、英単語の列だけを枠で囲めます"
                                },
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("ocr_crop_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "閉じる"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Preset Chips for 1-tap column framing
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            cropLeft = 0.14f
                            cropRight = 0.52f
                            cropTop = 0.04f
                            cropBottom = 0.96f
                        },
                        label = { Text("左側の単語列（□除外）", fontSize = 11.5.sp) }
                    )
                    AssistChip(
                        onClick = {
                            cropLeft = 0.16f
                            cropRight = 0.84f
                            cropTop = 0.06f
                            cropBottom = 0.94f
                        },
                        label = { Text("中央エリア", fontSize = 11.5.sp) }
                    )
                    AssistChip(
                        onClick = {
                            cropLeft = 0.48f
                            cropRight = 0.94f
                            cropTop = 0.04f
                            cropBottom = 0.96f
                        },
                        label = { Text("右側の列（縦書き等）", fontSize = 11.5.sp) }
                    )
                    AssistChip(
                        onClick = {
                            cropLeft = 0.02f
                            cropRight = 0.98f
                            cropTop = 0.02f
                            cropBottom = 0.98f
                        },
                        label = { Text("範囲リセット", fontSize = 11.5.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Image & Crop Canvas
                val touchSlopPx = with(density) { 28.dp.toPx() }
                val minCropSpan = 0.08f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF14181F))
                        .onSizeChanged { containerSize = it },
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = bitmap
                    when {
                        loadFailed -> {
                            Text(
                                text = "画像の読み込みに失敗しました。「画像全体を使う」をお試しください。",
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        bmp == null -> {
                            CircularProgressIndicator(color = Color.White)
                        }
                        else -> {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "選択された画像プレビュー",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("ocr_crop_canvas")
                                    .pointerInput(bmp, containerSize) {
                                        detectDragGestures(
                                            onDragStart = { startOffset ->
                                                val imgRect = computeImageDisplayRect(bmp, containerSize)
                                                if (imgRect.width <= 0f || imgRect.height <= 0f) {
                                                    activeDragMode = CropDragMode.NONE
                                                    return@detectDragGestures
                                                }
                                                val selLeft = imgRect.left + cropLeft * imgRect.width
                                                val selRight = imgRect.left + cropRight * imgRect.width
                                                val selTop = imgRect.top + cropTop * imgRect.height
                                                val selBottom = imgRect.top + cropBottom * imgRect.height

                                                val nearLeft = abs(startOffset.x - selLeft) <= touchSlopPx
                                                val nearRight = abs(startOffset.x - selRight) <= touchSlopPx
                                                val nearTop = abs(startOffset.y - selTop) <= touchSlopPx
                                                val nearBottom = abs(startOffset.y - selBottom) <= touchSlopPx
                                                val withinX = startOffset.x in (selLeft - touchSlopPx)..(selRight + touchSlopPx)
                                                val withinY = startOffset.y in (selTop - touchSlopPx)..(selBottom + touchSlopPx)

                                                activeDragMode = when {
                                                    nearLeft && nearTop -> CropDragMode.TOP_LEFT
                                                    nearRight && nearTop -> CropDragMode.TOP_RIGHT
                                                    nearLeft && nearBottom -> CropDragMode.BOTTOM_LEFT
                                                    nearRight && nearBottom -> CropDragMode.BOTTOM_RIGHT
                                                    nearLeft && withinY -> CropDragMode.LEFT_EDGE
                                                    nearRight && withinY -> CropDragMode.RIGHT_EDGE
                                                    nearTop && withinX -> CropDragMode.TOP_EDGE
                                                    nearBottom && withinX -> CropDragMode.BOTTOM_EDGE
                                                    startOffset.x in selLeft..selRight && startOffset.y in selTop..selBottom -> CropDragMode.MOVE_INSIDE
                                                    else -> CropDragMode.NONE
                                                }
                                            },
                                            onDragEnd = { activeDragMode = CropDragMode.NONE },
                                            onDragCancel = { activeDragMode = CropDragMode.NONE },
                                            onDrag = { change, dragAmount ->
                                                val imgRect = computeImageDisplayRect(bmp, containerSize)
                                                if (imgRect.width <= 0f || imgRect.height <= 0f || activeDragMode == CropDragMode.NONE) {
                                                    return@detectDragGestures
                                                }
                                                change.consume()
                                                val dx = dragAmount.x / imgRect.width
                                                val dy = dragAmount.y / imgRect.height

                                                when (activeDragMode) {
                                                    CropDragMode.TOP_LEFT -> {
                                                        cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minCropSpan)
                                                        cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minCropSpan)
                                                    }
                                                    CropDragMode.TOP_RIGHT -> {
                                                        cropRight = (cropRight + dx).coerceIn(cropLeft + minCropSpan, 1f)
                                                        cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minCropSpan)
                                                    }
                                                    CropDragMode.BOTTOM_LEFT -> {
                                                        cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minCropSpan)
                                                        cropBottom = (cropBottom + dy).coerceIn(cropTop + minCropSpan, 1f)
                                                    }
                                                    CropDragMode.BOTTOM_RIGHT -> {
                                                        cropRight = (cropRight + dx).coerceIn(cropLeft + minCropSpan, 1f)
                                                        cropBottom = (cropBottom + dy).coerceIn(cropTop + minCropSpan, 1f)
                                                    }
                                                    CropDragMode.LEFT_EDGE -> {
                                                        cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minCropSpan)
                                                    }
                                                    CropDragMode.RIGHT_EDGE -> {
                                                        cropRight = (cropRight + dx).coerceIn(cropLeft + minCropSpan, 1f)
                                                    }
                                                    CropDragMode.TOP_EDGE -> {
                                                        cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minCropSpan)
                                                    }
                                                    CropDragMode.BOTTOM_EDGE -> {
                                                        cropBottom = (cropBottom + dy).coerceIn(cropTop + minCropSpan, 1f)
                                                    }
                                                    CropDragMode.MOVE_INSIDE -> {
                                                        val w = cropRight - cropLeft
                                                        val h = cropBottom - cropTop
                                                        val newLeft = (cropLeft + dx).coerceIn(0f, 1f - w)
                                                        val newTop = (cropTop + dy).coerceIn(0f, 1f - h)
                                                        cropLeft = newLeft
                                                        cropRight = newLeft + w
                                                        cropTop = newTop
                                                        cropBottom = newTop + h
                                                    }
                                                    CropDragMode.NONE -> Unit
                                                }
                                            }
                                        )
                                    }
                            ) {
                                val bw = bmp.width.toFloat()
                                val bh = bmp.height.toFloat()
                                val cw = size.width
                                val ch = size.height
                                val scale = min(cw / bw, ch / bh)
                                val drawW = bw * scale
                                val drawH = bh * scale
                                val imgLeft = (cw - drawW) / 2f
                                val imgTop = (ch - drawH) / 2f

                                val selLeft = imgLeft + cropLeft * drawW
                                val selTop = imgTop + cropTop * drawH
                                val selRight = imgLeft + cropRight * drawW
                                val selBottom = imgTop + cropBottom * drawH
                                val selW = max(1f, selRight - selLeft)
                                val selH = max(1f, selBottom - selTop)

                                val scrimColor = Color.Black.copy(alpha = 0.56f)
                                // Top scrim
                                drawRect(scrimColor, Offset(imgLeft, imgTop), Size(drawW, max(0f, selTop - imgTop)))
                                // Bottom scrim
                                drawRect(scrimColor, Offset(imgLeft, selBottom), Size(drawW, max(0f, (imgTop + drawH) - selBottom)))
                                // Left scrim
                                drawRect(scrimColor, Offset(imgLeft, selTop), Size(max(0f, selLeft - imgLeft), selH))
                                // Right scrim
                                drawRect(scrimColor, Offset(selRight, selTop), Size(max(0f, (imgLeft + drawW) - selRight), selH))

                                // Selection frame & guide grid
                                drawRect(
                                    color = Color.White,
                                    topLeft = Offset(selLeft, selTop),
                                    size = Size(selW, selH),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                val guideColor = Color.White.copy(alpha = 0.35f)
                                drawLine(guideColor, Offset(selLeft + selW / 3f, selTop), Offset(selLeft + selW / 3f, selBottom), 1.dp.toPx())
                                drawLine(guideColor, Offset(selLeft + 2f * selW / 3f, selTop), Offset(selLeft + 2f * selW / 3f, selBottom), 1.dp.toPx())
                                drawLine(guideColor, Offset(selLeft, selTop + selH / 3f), Offset(selRight, selTop + selH / 3f), 1.dp.toPx())
                                drawLine(guideColor, Offset(selLeft, selTop + 2f * selH / 3f), Offset(selRight, selTop + 2f * selH / 3f), 1.dp.toPx())

                                // Prominent corner handles
                                val handleLen = min(22.dp.toPx(), min(selW, selH) * 0.35f)
                                val handleStroke = 4.dp.toPx()
                                val accent = Color(0xFF60A5FA)
                                // Top-Left
                                drawLine(accent, Offset(selLeft, selTop), Offset(selLeft + handleLen, selTop), handleStroke)
                                drawLine(accent, Offset(selLeft, selTop), Offset(selLeft, selTop + handleLen), handleStroke)
                                // Top-Right
                                drawLine(accent, Offset(selRight, selTop), Offset(selRight - handleLen, selTop), handleStroke)
                                drawLine(accent, Offset(selRight, selTop), Offset(selRight, selTop + handleLen), handleStroke)
                                // Bottom-Left
                                drawLine(accent, Offset(selLeft, selBottom), Offset(selLeft + handleLen, selBottom), handleStroke)
                                drawLine(accent, Offset(selLeft, selBottom), Offset(selLeft, selBottom - handleLen), handleStroke)
                                // Bottom-Right
                                drawLine(accent, Offset(selRight, selBottom), Offset(selRight - handleLen, selBottom), handleStroke)
                                drawLine(accent, Offset(selRight, selBottom), Offset(selRight, selBottom - handleLen), handleStroke)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onUseFullImage,
                        enabled = !isSavingCrop,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("ocr_crop_use_full_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("画像全体を使う", fontSize = 12.5.sp)
                    }

                    Button(
                        onClick = {
                            val bmp = bitmap ?: return@Button
                            isSavingCrop = true
                            scope.launch {
                                val croppedUri = withContext(Dispatchers.IO) {
                                    try {
                                        val x = (cropLeft * bmp.width).toInt().coerceIn(0, bmp.width - 1)
                                        val y = (cropTop * bmp.height).toInt().coerceIn(0, bmp.height - 1)
                                        val w = ((cropRight - cropLeft) * bmp.width).toInt().coerceIn(1, bmp.width - x)
                                        val h = ((cropBottom - cropTop) * bmp.height).toInt().coerceIn(1, bmp.height - y)
                                        val cropped = Bitmap.createBitmap(bmp, x, y, w, h)
                                        val outFile = File(context.cacheDir, "ocr_cropped_${System.currentTimeMillis()}.jpg")
                                        FileOutputStream(outFile).use { fos ->
                                            cropped.compress(Bitmap.CompressFormat.JPEG, 94, fos)
                                        }
                                        if (cropped !== bmp) {
                                            cropped.recycle()
                                        }
                                        outFile.deleteOnExit()
                                        try {
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                outFile
                                            )
                                        } catch (_: Exception) {
                                            Uri.fromFile(outFile)
                                        }
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
                                isSavingCrop = false
                                if (croppedUri != null) {
                                    onCropConfirmed(croppedUri)
                                } else {
                                    onUseFullImage()
                                }
                            }
                        },
                        enabled = bitmap != null && !isSavingCrop,
                        modifier = Modifier
                            .weight(1.35f)
                            .height(48.dp)
                            .testTag("ocr_crop_confirm_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSavingCrop) "切り抜き中..." else "この範囲で抽出",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}
