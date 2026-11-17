package com.example.ui.add

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import com.example.data.ocr.OcrSensitivity
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import com.example.data.local.FlashcardEntity
import com.example.data.model.ClassicalWordKnowledgeBase
import com.example.data.model.WordKnowledgeBase
import com.example.data.service.WeblioDictionaryService
import com.example.ui.MainViewModel
import com.example.ui.theme.StatusCircleGreen
import com.example.ui.theme.StatusCrossRed
import com.example.ui.theme.StudyBluePrimary
import kotlinx.coroutines.launch
import java.io.File

enum class ExtractSource(val label: String) {
    CAMERA_CAPTURE("📷 カメラで撮影"),
    PHOTO_UPLOAD("🖼️ 写真から選択"),
    TEXT_INPUT("📝 長文テキスト貼り付け")
}

enum class CandidateFilterTab(val label: String) {
    ALL("すべて"),
    IDIOMS("熟語・連語"),
    UNREGISTERED("未登録のみ"),
    ALREADY_REGISTERED("登録済み")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OcrExtractSection(
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val learningMode by viewModel.learningMode.collectAsStateWithLifecycle()
    val allCards by viewModel.allCards.collectAsStateWithLifecycle()
    val allDecks by viewModel.allDecks.collectAsStateWithLifecycle()

    // Settings moved to Settings tab as requested: read mode-scoped preferences
    val ocrExcludeRegisteredWords by viewModel.ocrExcludeRegisteredWords.collectAsStateWithLifecycle()
    val ocrAutoLookupMeaningEnabled by viewModel.ocrAutoLookupMeaningEnabled.collectAsStateWithLifecycle()
    val ocrSensitivity by viewModel.ocrSensitivity.collectAsStateWithLifecycle()

    var source by remember { mutableStateOf(ExtractSource.CAMERA_CAPTURE) }
    var rawTextToExtract by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var candidateWords by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedWords by remember { mutableStateOf<Set<String>>(emptySet()) }
    var targetDeck by remember(learningMode) { mutableStateOf(allDecks.firstOrNull() ?: learningMode.defaultDeck) }
    var filterTab by remember { mutableStateOf(CandidateFilterTab.ALL) }

    // Dialog state for Confirmation & Editing before adding
    var showConfirmEditDialog by remember { mutableStateOf(false) }

    // Camera capture Uri holder, original uncropped Uri, active scanned Uri, and optional crop dialog state
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var originalScannedUri by remember { mutableStateOf<Uri?>(null) }
    var lastScannedUri by remember { mutableStateOf<Uri?>(null) }
    var cropBeforeScan by remember { mutableStateOf(false) }
    var cropDialogUri by remember { mutableStateOf<Uri?>(null) }

    val wordUnitLabel = if (learningMode.isClassical) "古語" else "英単語"

    fun cancelAndResetExtraction() {
        candidateWords = emptyList()
        selectedWords = emptySet()
        statusMessage = null
        rawTextToExtract = ""
        originalScannedUri = null
        lastScannedUri = null
        cropDialogUri = null
        filterTab = CandidateFilterTab.ALL
    }

    LaunchedEffect(learningMode) {
        cancelAndResetExtraction()
        targetDeck = allDecks.firstOrNull() ?: learningMode.defaultDeck
    }

    LaunchedEffect(allDecks) {
        if (allDecks.isNotEmpty() && targetDeck !in allDecks) {
            targetDeck = allDecks.first()
        }
    }

    fun changeSensitivity(newSensitivity: OcrSensitivity) {
        viewModel.setOcrSensitivity(newSensitivity)
        if (source == ExtractSource.TEXT_INPUT && rawTextToExtract.isNotBlank()) {
            val words = viewModel.extractWordsFromRawText(rawTextToExtract, newSensitivity)
            candidateWords = words
            val existingWords = allCards.map { it.word.lowercase() }.toSet()
            selectedWords = if (ocrExcludeRegisteredWords) {
                words.filter { it.lowercase() !in existingWords }.toSet()
            } else {
                words.toSet()
            }
            statusMessage = if (words.isEmpty()) {
                "${wordUnitLabel}が検出されませんでした。（感度: ${newSensitivity.title}）"
            } else {
                "${words.size}語の${wordUnitLabel}を検出しました！（感度: ${newSensitivity.title}）"
            }
        } else if (source != ExtractSource.TEXT_INPUT && lastScannedUri != null) {
            isProcessing = true
            statusMessage = if (learningMode.isClassical) {
                "感度（${newSensitivity.title}）でWeblio古語辞典を参照しながら再抽出中..."
            } else {
                "感度（${newSensitivity.title}）で再抽出中..."
            }
            viewModel.processImageUriForOcr(context, lastScannedUri!!, newSensitivity) { words ->
                isProcessing = false
                candidateWords = words
                val existingWords = allCards.map { it.word.lowercase() }.toSet()
                selectedWords = if (ocrExcludeRegisteredWords) {
                    words.filter { it.lowercase() !in existingWords }.toSet()
                } else {
                    words.toSet()
                }
                statusMessage = if (words.isEmpty()) {
                    "${wordUnitLabel}が検出されませんでした。"
                } else if (learningMode.isClassical) {
                    "${words.size}語の古語を検出しました！（Weblio古語辞典を参照・感度: ${newSensitivity.title}）"
                } else {
                    "${words.size}語の英単語を検出しました！（感度: ${newSensitivity.title}）"
                }
            }
        }
    }

    fun runOcrScanForUri(targetUri: Uri, isCroppedRegion: Boolean = false) {
        lastScannedUri = targetUri
        isProcessing = true
        statusMessage = if (learningMode.isClassical) {
            if (isCroppedRegion) "指定範囲をスキャンしてWeblio古語辞典と照合中..." else "画像をスキャンしてWeblio古語辞典と照合中..."
        } else {
            if (isCroppedRegion) "指定範囲をスキャンしてWeblio辞書と照合中..." else "画像をスキャンして実在する英単語を抽出中..."
        }
        viewModel.processImageUriForOcr(context, targetUri, ocrSensitivity) { words ->
            isProcessing = false
            candidateWords = words
            val existingWords = allCards.map { it.word.lowercase() }.toSet()
            selectedWords = if (ocrExcludeRegisteredWords) {
                words.filter { it.lowercase() !in existingWords }.toSet()
            } else {
                words.toSet()
            }
            val regionSuffix = if (isCroppedRegion) "（範囲指定）" else ""
            statusMessage = if (words.isEmpty()) {
                "${wordUnitLabel}が検出されませんでした。範囲を広げるか別の画像をお試しください。"
            } else if (learningMode.isClassical) {
                "${words.size}語の古語を検出しました！$regionSuffix（辞書・Weblio古語辞典に実在する語のみ）"
            } else {
                "${words.size}語の英単語を検出しました！$regionSuffix（辞書・Weblioに実在する語のみ）"
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val captured = tempCameraUri
        if (success && captured != null) {
            originalScannedUri = captured
            if (cropBeforeScan) {
                cropDialogUri = captured
            } else {
                runOcrScanForUri(captured, isCroppedRegion = false)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val tempFile = File.createTempFile("camera_ocr_", ".jpg", context.cacheDir).apply {
                    createNewFile()
                    deleteOnExit()
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                statusMessage = "カメラの起動に失敗しました: ${e.localizedMessage}"
            }
        } else {
            statusMessage = "カメラの利用権限が許可されていません。"
        }
    }

    fun startCameraCapture() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val tempFile = File.createTempFile("camera_ocr_", ".jpg", context.cacheDir).apply {
                    createNewFile()
                    deleteOnExit()
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
                tempCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                statusMessage = "カメラの起動に失敗しました: ${e.localizedMessage}"
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Zero-permission Android Photo Picker for Google Play compliance
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            originalScannedUri = uri
            if (cropBeforeScan) {
                cropDialogUri = uri
            } else {
                runOcrScanForUri(uri, isCroppedRegion = false)
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Section Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudyBluePrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = StudyBluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (learningMode.isClassical) "📷 古文・写真から古語を抽出" else "📷 英語長文・写真から単語を抽出",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (learningMode.isClassical) {
                            "写真・古文からWeblio古語辞典を参照して古語を抽出"
                        } else {
                            "カメラ撮影・写真・長文から単語を抽出して確認・追加"
                        },
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Source Switch Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ExtractSource.entries.forEach { src ->
                    val chipLabel = when (src) {
                        ExtractSource.CAMERA_CAPTURE -> "📷 カメラで撮影"
                        ExtractSource.PHOTO_UPLOAD -> "🖼️ 写真から選択"
                        ExtractSource.TEXT_INPUT -> if (learningMode.isClassical) "📝 古文テキスト貼付" else "📝 長文テキスト貼付"
                    }
                    FilterChip(
                        selected = source == src,
                        onClick = { source = src },
                        label = { Text(chipLabel, fontSize = 11.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.height(30.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudyBluePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 文字認識感度セレクター（3段階: どこまでを文字とみなすか）
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = StudyBluePrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "文字認識感度",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OcrSensitivity.entries.forEach { level ->
                                FilterChip(
                                    selected = ocrSensitivity == level,
                                    onClick = { changeSensitivity(level) },
                                    label = {
                                        Text(
                                            text = level.shortLabel,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            fontWeight = if (ocrSensitivity == level) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StudyBluePrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .testTag("ocr_sensitivity_chip_${level.id.lowercase()}")
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ocrSensitivity.shortHint,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Optional Region-Selection Toggle & Re-crop Button (for Camera / Photo modes)
            if (source != ExtractSource.TEXT_INPUT) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = cropBeforeScan,
                        onClick = { cropBeforeScan = !cropBeforeScan },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "撮影・選択後に範囲指定（任意）",
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudyBluePrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("ocr_crop_before_scan_chip")
                    )

                    if (originalScannedUri != null && !isProcessing) {
                        TextButton(
                            onClick = { cropDialogUri = originalScannedUri },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("ocr_rescan_with_crop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = null,
                                tint = StudyBluePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "範囲指定して再読取",
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = FontWeight.Bold,
                                color = StudyBluePrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Source Input Actions
            when (source) {
                ExtractSource.CAMERA_CAPTURE -> {
                    Button(
                        onClick = { startCameraCapture() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ocr_camera_capture_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (learningMode.isClassical) "カメラで古文・古語単語帳を撮影" else "カメラで英語の長文・教科書を撮影",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                ExtractSource.PHOTO_UPLOAD -> {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ocr_pick_image_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("写真・スクリーンショットを選択してスキャン", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                ExtractSource.TEXT_INPUT -> {
                    OutlinedTextField(
                        value = rawTextToExtract,
                        onValueChange = { rawTextToExtract = it },
                        label = { Text(if (learningMode.isClassical) "古文テキストを貼り付け" else "英文テキストを貼り付け") },
                        placeholder = {
                            Text(
                                if (learningMode.isClassical) {
                                    "枕草子・源氏物語などの古文本文や単語リストをここに貼り付け..."
                                } else {
                                    "英語ニュース記事やエッセイ、メールの長文をここに貼り付け..."
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (rawTextToExtract.isNotBlank()) {
                                val words = viewModel.extractWordsFromRawText(rawTextToExtract, ocrSensitivity)
                                candidateWords = words
                                val existingWords = allCards.map { it.word.lowercase() }.toSet()
                                selectedWords = if (ocrExcludeRegisteredWords) {
                                    words.filter { it.lowercase() !in existingWords }.toSet()
                                } else {
                                    words.toSet()
                                }
                                statusMessage = if (words.isEmpty()) {
                                    "${wordUnitLabel}が見つかりませんでした。"
                                } else {
                                    "${words.size}語の${wordUnitLabel}を抽出しました！"
                                }
                            }
                        },
                        enabled = rawTextToExtract.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (learningMode.isClassical) "貼り付けた古文から古語を抽出する" else "貼り付けた長文から単語を抽出する",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }

            // Processing Indicator
            if (isProcessing) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = StudyBluePrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (learningMode.isClassical) "古文から古語を認識・Weblio古語辞典を参照中..." else "長文から単語を認識・抽出中...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status message
            if (statusMessage != null && !isProcessing) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = statusMessage!!,
                    fontSize = 12.sp,
                    color = if (candidateWords.isNotEmpty()) StudyBluePrimary else StatusCrossRed,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Extracted Results Section
            if (candidateWords.isNotEmpty()) {
                val existingWordMap = remember(allCards) { allCards.associateBy { it.word.lowercase() } }
                val unregisteredWords = remember(candidateWords, existingWordMap) {
                    candidateWords.filter { !existingWordMap.containsKey(it.lowercase()) }
                }
                val registeredWords = remember(candidateWords, existingWordMap) {
                    candidateWords.filter { existingWordMap.containsKey(it.lowercase()) }
                }
                val idiomWords = remember(candidateWords, learningMode) {
                    candidateWords.filter { it.contains(" ") || (!learningMode.isClassical && com.example.data.service.InternalDictionaryService.isIdiomPhrase(it)) }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Header with Cancel Button: "単語の選択で単語の追加のキャンセルもできるようにしてください"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "抽出結果 (${candidateWords.size}語 検出)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(
                        onClick = { cancelAndResetExtraction() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("ocr_cancel_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = StatusCrossRed,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "抽出をキャンセル",
                            fontSize = 12.sp,
                            color = StatusCrossRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Information banner indicating setting state from Settings Tab
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (ocrExcludeRegisteredWords) StudyBluePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (ocrExcludeRegisteredWords) StudyBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = buildString {
                                if (ocrExcludeRegisteredWords) {
                                    append("既登録除外(${unregisteredWords.size}件未登録)")
                                } else {
                                    append("全単語選択")
                                }
                                append(" ・感度:${ocrSensitivity.shortLabel}・自動検索:")
                                append(if (ocrAutoLookupMeaningEnabled) "ON" else "OFF")
                            },
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (ocrExcludeRegisteredWords) StudyBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Tabs (All / Idioms / Unregistered / Registered)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterTab == CandidateFilterTab.ALL,
                        onClick = { filterTab = CandidateFilterTab.ALL },
                        label = { Text("すべて (${candidateWords.size})", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.height(30.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudyBluePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    if (idiomWords.isNotEmpty()) {
                        FilterChip(
                            selected = filterTab == CandidateFilterTab.IDIOMS,
                            onClick = { filterTab = CandidateFilterTab.IDIOMS },
                            label = { Text("★ 熟語 (${idiomWords.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
                            modifier = Modifier.height(30.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFD97706),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    FilterChip(
                        selected = filterTab == CandidateFilterTab.UNREGISTERED,
                        onClick = { filterTab = CandidateFilterTab.UNREGISTERED },
                        label = { Text("未登録 (${unregisteredWords.size})", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.height(30.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusCircleGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = filterTab == CandidateFilterTab.ALREADY_REGISTERED,
                        onClick = { filterTab = CandidateFilterTab.ALREADY_REGISTERED },
                        label = { Text("登録済 (${registeredWords.size})", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.height(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Selection Header & Quick actions (including cancel)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "選択中: ${selectedWords.size} / ${candidateWords.size} 語",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(
                            onClick = { selectedWords = unregisteredWords.toSet() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("未登録全選択", fontSize = 11.sp, color = StudyBluePrimary, maxLines = 1, softWrap = false)
                        }
                        TextButton(
                            onClick = { selectedWords = candidateWords.toSet() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("全選択", fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                        TextButton(
                            onClick = { selectedWords = emptySet() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("選択解除", fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }

                // Filtered Candidates List
                val displayedWords = when (filterTab) {
                    CandidateFilterTab.ALL -> candidateWords
                    CandidateFilterTab.IDIOMS -> idiomWords
                    CandidateFilterTab.UNREGISTERED -> unregisteredWords
                    CandidateFilterTab.ALREADY_REGISTERED -> registeredWords
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(6.dp)
                ) {
                    if (displayedWords.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("該当する単語はありません", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            displayedWords.forEach { word ->
                                val isChecked = selectedWords.contains(word)
                                val isAlreadyRegistered = existingWordMap.containsKey(word.lowercase())
                                val registeredCard = existingWordMap[word.lowercase()]
                                val previewDefinitions = remember(word, learningMode, candidateWords) {
                                    if (learningMode.isClassical) {
                                        com.example.data.service.InternalDictionaryService.lookupClassical(word)?.definitions
                                            ?: WeblioDictionaryService.getCachedClassicalWord(word)?.definitions
                                    } else {
                                        com.example.data.service.InternalDictionaryService.lookupEnglish(word)?.definitions
                                            ?: WeblioDictionaryService.getCachedWord(word)?.definitions
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedWords = if (isChecked) selectedWords - word else selectedWords + word
                                        }
                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedWords = if (checked) selectedWords + word else selectedWords - word
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = StudyBluePrimary),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = word,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            val isIdiom = word.contains(" ") || (!learningMode.isClassical && com.example.data.service.InternalDictionaryService.isIdiomPhrase(word))
                                            if (isIdiom) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFD97706).copy(alpha = 0.18f))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "★ 熟語",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        color = Color(0xFFD97706)
                                                    )
                                                }
                                            }

                                            if (isAlreadyRegistered) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "登録済(${registeredCard?.deck ?: ""})",
                                                        fontSize = 9.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(StatusCircleGreen.copy(alpha = 0.15f))
                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "未登録",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        color = StatusCircleGreen
                                                    )
                                                }
                                            }
                                        }

                                        if (ocrAutoLookupMeaningEnabled && !previewDefinitions.isNullOrEmpty()) {
                                            Text(
                                                text = previewDefinitions.joinToString(" / ") { "${it.pos}: ${it.meaning}" },
                                                fontSize = 11.sp,
                                                color = StudyBluePrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Target Deck Selection
                Text(
                    text = "追加先デッキ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    allDecks.forEach { deck ->
                        FilterChip(
                            selected = targetDeck == deck,
                            onClick = { targetDeck = deck },
                            label = { Text(deck, fontSize = 11.5.sp, maxLines = 1, softWrap = false) },
                            modifier = Modifier.height(30.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudyBluePrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions: Cancel and Proceed to Edit & Confirm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { cancelAndResetExtraction() },
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("ocr_cancel_bottom_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = StatusCrossRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("キャンセル", fontSize = 12.sp, color = StatusCrossRed, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = { showConfirmEditDialog = true },
                        enabled = selectedWords.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .weight(2f)
                            .height(44.dp)
                            .testTag("ocr_open_edit_confirm_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "編集・確認 (${selectedWords.size}語)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // Interactive Region Crop Dialog (optional before scan or 1-tap re-scan)
    val activeCropSourceUri = cropDialogUri
    if (activeCropSourceUri != null) {
        OcrImageCropDialog(
            imageUri = activeCropSourceUri,
            isClassical = learningMode.isClassical,
            onDismiss = { cropDialogUri = null },
            onUseFullImage = {
                cropDialogUri = null
                runOcrScanForUri(activeCropSourceUri, isCroppedRegion = false)
            },
            onCropConfirmed = { croppedUri ->
                cropDialogUri = null
                runOcrScanForUri(croppedUri, isCroppedRegion = true)
            }
        )
    }

    // Confirmation & Edit Modal Dialog
    if (showConfirmEditDialog && selectedWords.isNotEmpty()) {
        OcrConfirmEditDialog(
            words = selectedWords.toList(),
            initialDeck = targetDeck,
            allDecks = allDecks,
            viewModel = viewModel,
            onDismiss = { showConfirmEditDialog = false },
            onConfirmAdd = { reviewedEntities ->
                showConfirmEditDialog = false
                viewModel.insertCards(reviewedEntities) { count ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("🎉 ${count}件の単語を「${targetDeck}」に追加しました！")
                    }
                    cancelAndResetExtraction()
                }
            }
        )
    }
}
