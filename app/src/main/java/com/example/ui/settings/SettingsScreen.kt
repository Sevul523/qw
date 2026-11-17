package com.example.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.ocr.OcrSensitivity
import com.example.ui.MainViewModel
import com.example.ui.components.DeleteAllConfirmDialog
import com.example.ui.components.DeleteDeckConfirmDialog
import com.example.ui.components.ExportDialog
import com.example.ui.components.ImportDialog
import com.example.ui.components.RenameDeckDialog
import com.example.ui.theme.StatusCircleGreen
import com.example.ui.theme.StatusCrossRed
import com.example.ui.theme.StudyBluePrimary
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.TextButton

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onSpeakTest: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val learningMode by viewModel.learningMode.collectAsStateWithLifecycle()
    val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()
    val cardBackTheme by viewModel.cardBackTheme.collectAsStateWithLifecycle()
    val fontFamily by viewModel.fontFamily.collectAsStateWithLifecycle()
    val ttsEnabled by viewModel.ttsEnabled.collectAsStateWithLifecycle()
    val autoSpeak by viewModel.autoSpeakOnCardChange.collectAsStateWithLifecycle()
    val speechRate by viewModel.speechRate.collectAsStateWithLifecycle()
    val speechPitch by viewModel.speechPitch.collectAsStateWithLifecycle()
    val autoUncheckPerfectCount by viewModel.autoUncheckPerfectCount.collectAsStateWithLifecycle()
    val autoUncheckAction by viewModel.autoUncheckAction.collectAsStateWithLifecycle()
    val dictionaryPriority by viewModel.dictionaryPriority.collectAsStateWithLifecycle()
    val internalDictionaryEnabled by viewModel.internalDictionaryEnabled.collectAsStateWithLifecycle()
    val weblioAutoFillEnabled by viewModel.weblioAutoFillEnabled.collectAsStateWithLifecycle()
    val weblioAutoAddNoteLevelEnabled by viewModel.weblioAutoAddNoteLevelEnabled.collectAsStateWithLifecycle()
    val weblioAutoAddConjugationEnabled by viewModel.weblioAutoAddConjugationEnabled.collectAsStateWithLifecycle()
    val ocrExcludeRegisteredWords by viewModel.ocrExcludeRegisteredWords.collectAsStateWithLifecycle()
    val ocrAutoLookupMeaningEnabled by viewModel.ocrAutoLookupMeaningEnabled.collectAsStateWithLifecycle()
    val ocrSensitivity by viewModel.ocrSensitivity.collectAsStateWithLifecycle()
    val allCards by viewModel.allCards.collectAsStateWithLifecycle()
    val allDecks by viewModel.allDecks.collectAsStateWithLifecycle()
    val defaultDecks = viewModel.defaultDecks
    val showDeleteAllDialog by viewModel.showDeleteAllDialog.collectAsStateWithLifecycle()

    var deckToRename by remember { mutableStateOf<String?>(null) }
    var deckToDelete by remember { mutableStateOf<String?>(null) }
    var selectedDeckForImport by remember { mutableStateOf<String?>(null) }
    var selectedDeckForExport by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    var expandedCategories by remember {
        mutableStateOf(emptySet<SettingsCategory>())
    }
    val toggleCategory = { category: SettingsCategory ->
        expandedCategories = if (category in expandedCategories) {
            expandedCategories - category
        } else {
            expandedCategories + category
        }
    }

    val studyAudioSummary = if (learningMode.isClassical) {
        "自動解除: ${if (autoUncheckPerfectCount > 0) "○${autoUncheckPerfectCount}回" else "OFF"}"
    } else {
        "TTS: ${if (ttsEnabled) "ON" else "OFF"} / 自動解除: ${if (autoUncheckPerfectCount > 0) "○${autoUncheckPerfectCount}回" else "OFF"}"
    }
    val addOcrSummary = if (learningMode.isClassical) {
        "優先: ${dictionaryPriority.badgeLabel} / 内蔵: ${if (internalDictionaryEnabled) "ON" else "OFF"} / Weblio: ${if (weblioAutoFillEnabled) "ON" else "OFF"}"
    } else {
        "優先: ${dictionaryPriority.badgeLabel} / 内蔵: ${if (internalDictionaryEnabled) "ON" else "OFF"} / Weblio: ${if (weblioAutoFillEnabled) "ON" else "OFF"}"
    }
    val appearanceSummary = "テーマ: ${themeColor.title} / カード裏面: ${cardBackTheme.title}"
    val dataManagementSummary = "デッキ: ${allDecks.size}個 / 単語: ${allCards.size}枚"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Toolbar: Category Summary & Expand/Collapse All
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                Text(
                    text = "設定",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (learningMode.isClassical) "📜 古語モードの個別設定" else "🇬🇧 英単語モードの個別設定",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = StudyBluePrimary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(
                    onClick = {
                        expandedCategories = SettingsCategory.entries.toSet()
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("expand_all_categories_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = StudyBluePrimary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("すべて展開", fontSize = 11.sp, color = StudyBluePrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                }

                TextButton(
                    onClick = {
                        expandedCategories = emptySet()
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("collapse_all_categories_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.UnfoldLess,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2刀流モード切り替えカード
        Card(
            modifier = Modifier.fillMaxWidth().testTag("subject_mode_selection_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "⚔️ 学習対象モード (2刀流切り替え)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "英単語と古語の切り替え（カード・学習進度は独立保存）",
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.data.model.LearningMode.entries.forEach { mode ->
                        val isSelected = learningMode == mode
                        OutlinedButton(
                            onClick = { viewModel.setLearningMode(mode) },
                            modifier = Modifier.weight(1f).testTag("select_mode_${mode.id}"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) StudyBluePrimary.copy(alpha = 0.08f) else Color.Transparent
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = mode.icon, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = mode.displayName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (mode.isClassical) "共通テスト・入試300語" else "日常・TOEIC・英検",
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. 学習・音声設定 (Study & Audio)
        SettingsAccordionSection(
            category = SettingsCategory.STUDY_AUDIO,
            isExpanded = SettingsCategory.STUDY_AUDIO in expandedCategories,
            onToggle = { toggleCategory(SettingsCategory.STUDY_AUDIO) },
            summaryText = studyAudioSummary,
            titleOverride = if (learningMode.isClassical) "学習設定" else null,
            subtitleOverride = if (learningMode.isClassical) "連続正解時の自動チェック解除" else null,
            iconOverride = if (learningMode.isClassical) Icons.Default.CheckCircle else null
        ) {
            // TTS / Speech Section (English mode only)
            if (!learningMode.isClassical) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = StudyBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("音声発音設定 (TTS)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("音声発音機能を有効にする", fontSize = 14.sp)
                        Switch(
                            checked = ttsEnabled,
                            onCheckedChange = { viewModel.setTtsEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = StudyBluePrimary)
                        )
                    }

                    if (ttsEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("カード切り替え時に自動発音", fontSize = 14.sp)
                            Switch(
                                checked = autoSpeak,
                                onCheckedChange = { viewModel.setAutoSpeakOnCardChange(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = StudyBluePrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Speech Rate Slider (Centered at 1.0x)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "読み上げ速度: ${String.format("%.1f", speechRate)}x",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (Math.abs(speechRate - 1.0f) > 0.01f) {
                                Text(
                                    text = "標準(1.0x)に戻す",
                                    fontSize = 11.sp,
                                    color = StudyBluePrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { viewModel.setSpeechRate(1.0f) }
                                        .padding(vertical = 2.dp, horizontal = 4.dp)
                                )
                            }
                        }
                        Slider(
                            value = speechRate.coerceIn(0.5f, 1.5f),
                            onValueChange = { viewModel.setSpeechRate(it) },
                            valueRange = 0.5f..1.5f,
                            steps = 9,
                            colors = SliderDefaults.colors(thumbColor = StudyBluePrimary, activeTrackColor = StudyBluePrimary),
                            modifier = Modifier.testTag("speech_rate_slider")
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ゆっくり (0.5x)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("標準 (1.0x)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudyBluePrimary)
                            Text("はやい (1.5x)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Speech Pitch Slider (Centered at 1.0x)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "読み上げピッチ: ${String.format("%.1f", speechPitch)}x",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (Math.abs(speechPitch - 1.0f) > 0.01f) {
                                Text(
                                    text = "標準(1.0x)に戻す",
                                    fontSize = 11.sp,
                                    color = StudyBluePrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { viewModel.setSpeechPitch(1.0f) }
                                        .padding(vertical = 2.dp, horizontal = 4.dp)
                                )
                            }
                        }
                        Slider(
                            value = speechPitch.coerceIn(0.5f, 1.5f),
                            onValueChange = { viewModel.setSpeechPitch(it) },
                            valueRange = 0.5f..1.5f,
                            steps = 9,
                            colors = SliderDefaults.colors(thumbColor = StudyBluePrimary, activeTrackColor = StudyBluePrimary),
                            modifier = Modifier.testTag("speech_pitch_slider")
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("低い (0.5x)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("標準 (1.0x)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudyBluePrimary)
                            Text("高い (1.5x)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { onSpeakTest("Hello! This is a test for English flashcards.") },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("発音テスト")
                        }
                    }
                }
            }
            }

            // Auto Uncheck on Consecutive Perfect Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusCircleGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("完璧連続時の自動チェック解除", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "連続で「完璧」を選んだ単語を自動で学習チェックから外します。",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "自動解除する連続回数",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            0 to "オフ（無効）",
                            2 to "2回連続",
                            3 to "3回連続",
                            4 to "4回連続",
                            5 to "5回連続"
                        ).forEach { (count, label) ->
                            val isSelected = count == autoUncheckPerfectCount
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) StatusCircleGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(if (isSelected) StatusCircleGreen.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { viewModel.setAutoUncheckPerfectCount(count) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("auto_uncheck_count_$count")
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) StatusCircleGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (autoUncheckPerfectCount > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "解除時の動作（チェックの外し方）",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AutoUncheckAction.entries.forEach { action ->
                                val isSelected = action == autoUncheckAction
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) StatusCircleGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .background(if (isSelected) StatusCircleGreen.copy(alpha = 0.08f) else Color.Transparent)
                                        .clickable { viewModel.setAutoUncheckAction(action) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("auto_uncheck_action_${action.name}")
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { viewModel.setAutoUncheckAction(action) },
                                                colors = RadioButtonDefaults.colors(selectedColor = StatusCircleGreen),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = action.title,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) StatusCircleGreen else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = action.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 15.sp,
                                            modifier = Modifier.padding(start = 28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }

        // 2. 単語追加・OCR設定 (Word Addition & OCR)
        SettingsAccordionSection(
            category = SettingsCategory.ADD_OCR,
            isExpanded = SettingsCategory.ADD_OCR in expandedCategories,
            onToggle = { toggleCategory(SettingsCategory.ADD_OCR) },
            summaryText = addOcrSummary
        ) {
            // Dictionary Search Priority Card (内蔵辞書 vs Weblio優先順位)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = null,
                            tint = StudyBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "辞書検索の優先順位設定",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "単語追加・OCR抽出時の意味補完順序（内蔵優先はオフライン0秒）",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DictionaryPriority.entries.forEach { priority ->
                            val isSelected = priority == dictionaryPriority
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .background(if (isSelected) StudyBluePrimary.copy(alpha = 0.08f) else Color.Transparent)
                                    .clickable { viewModel.setDictionaryPriority(priority) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("dict_priority_${priority.id}")
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { viewModel.setDictionaryPriority(priority) },
                                                colors = RadioButtonDefaults.colors(selectedColor = StudyBluePrimary),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = priority.title,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        ) {
                                            Text(
                                                text = priority.badgeLabel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = priority.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 14.sp,
                                        modifier = Modifier.padding(start = 26.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Internal Dictionary Auto-fill Switch Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = StudyBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "内蔵辞書による高速自動補完",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (internalDictionaryEnabled) {
                                        if (learningMode.isClassical) "有効（重要古語900語以上対応）" else "有効（基礎5,000語優先・54万語対応）"
                                    } else {
                                        "無効（内蔵辞書を使用しない）"
                                    },
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (internalDictionaryEnabled) StudyBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Switch(
                            checked = internalDictionaryEnabled,
                            onCheckedChange = { viewModel.setInternalDictionaryEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = StudyBluePrimary
                            ),
                            modifier = Modifier.testTag("internal_dict_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (learningMode.isClassical) {
                            "端末内蔵の重要古語900語以上からオフラインで訳・品詞・活用を自動補完します。"
                        } else {
                            "端末内蔵の約54万語（基礎5,000語優先）からオフラインで意味・品詞を自動補完します。"
                        },
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Weblio Dictionary Auto-fill Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = StudyBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (learningMode.isClassical) "Weblio古語辞典の意味自動入力" else "Weblio英和辞典の意味自動入力",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (weblioAutoFillEnabled) "自動入力する" else "自動入力しない（手動のみ）",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (weblioAutoFillEnabled) StudyBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Switch(
                            checked = weblioAutoFillEnabled,
                            onCheckedChange = { viewModel.setWeblioAutoFillEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = StudyBluePrimary
                            ),
                            modifier = Modifier.testTag("weblio_autofill_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (learningMode.isClassical) {
                            "単語入力時にWeblio古語辞典から現代語訳と品詞を自動補完します。"
                        } else {
                            "単語入力時にWeblio英和辞典から意味と品詞を自動補完します。"
                        },
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Weblio Dictionary Level & Eiken Grade Note Auto-Addition Settings Card (English mode only)
            if (!learningMode.isClassical) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = StudyBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "備考へWeblioレベル・英検級を自動追加",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (weblioAutoAddNoteLevelEnabled) "自動追加する（例: Weblioレベル5 / 英検2級）" else "自動追加しない",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (weblioAutoAddNoteLevelEnabled) StudyBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Switch(
                            checked = weblioAutoAddNoteLevelEnabled,
                            onCheckedChange = { viewModel.setWeblioAutoAddNoteLevelEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = StudyBluePrimary
                            ),
                            modifier = Modifier.testTag("weblio_auto_add_note_level_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "単語追加やOCR抽出時、学習レベル(1〜30)と英検級を備考欄へ自動入力します。",
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            } else {
            // Weblio Classical Dictionary Conjugation Type Auto-Addition Settings Card (Classical mode only)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = StudyBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "備考へ活用の種類を自動追加",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (weblioAutoAddConjugationEnabled) "自動追加する（例: カ行四段活用）" else "自動追加しない",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (weblioAutoAddConjugationEnabled) StudyBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Switch(
                            checked = weblioAutoAddConjugationEnabled,
                            onCheckedChange = { viewModel.setWeblioAutoAddConjugationEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = StudyBluePrimary
                            ),
                            modifier = Modifier.testTag("weblio_auto_add_conjugation_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "古語追加やOCR抽出時、四段・下二段・シク活用などを備考欄へ自動入力します。",
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            }

            // Feature: Camera / Image OCR Exclude Registered Words Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = StudyBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "画像抽出時の既登録単語除外",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (ocrExcludeRegisteredWords) "自動除外する（未登録のみ抽出）" else "自動除外しない（全単語対象）",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (ocrExcludeRegisteredWords) StudyBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Switch(
                            checked = ocrExcludeRegisteredWords,
                            onCheckedChange = { viewModel.setOcrExcludeRegisteredWords(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = StudyBluePrimary
                            ),
                            modifier = Modifier.testTag("ocr_exclude_registered_switch_settings")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (learningMode.isClassical) {
                            "写真や古文から抽出する際、登録済みの古語を除外して未登録のみ候補にします。"
                        } else {
                            "写真や長文から抽出する際、登録済みの英単語を除外して未登録のみ候補にします。"
                        },
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Feature: Camera / Photo OCR Auto Lookup Meaning Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = StudyBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "写真撮影時の意味の自動検索",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (ocrAutoLookupMeaningEnabled) "自動で探して載せる" else "自動で載せない（手動入力）",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (ocrAutoLookupMeaningEnabled) StudyBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Switch(
                            checked = ocrAutoLookupMeaningEnabled,
                            onCheckedChange = { viewModel.setOcrAutoLookupMeaningEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = StudyBluePrimary
                            ),
                            modifier = Modifier.testTag("ocr_auto_lookup_meaning_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (learningMode.isClassical) {
                            "抽出時にWeblio古語辞典・内蔵辞書から現代語訳や品詞を確認画面へ自動入力します。"
                        } else {
                            "抽出時に辞書・Weblioから日本語の意味や品詞を確認画面へ自動入力します。"
                        },
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Feature: Camera / Photo OCR Extraction Sensitivity Settings Card (3 Levels)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = StudyBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("写真・カメラの文字認識感度", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = "現在: ${ocrSensitivity.title}（薄い文字や枠線判定の基準を調整）",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = StudyBluePrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OcrSensitivity.entries.forEach { level ->
                        val isSelected = ocrSensitivity == level
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .background(if (isSelected) StudyBluePrimary.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable { viewModel.setOcrSensitivity(level) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("ocr_sensitivity_option_${level.id.lowercase()}")
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.setOcrSensitivity(level) },
                                            colors = RadioButtonDefaults.colors(selectedColor = StudyBluePrimary),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = level.title,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) StudyBluePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = level.badgeLabel,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (learningMode.isClassical) level.classicalDescription else level.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.padding(start = 26.dp)
                                )
                            }
                        }
                        if (level != OcrSensitivity.entries.last()) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }

        }

        // 3. 外観・デザイン設定 (Appearance & Display)
        SettingsAccordionSection(
            category = SettingsCategory.APPEARANCE,
            isExpanded = SettingsCategory.APPEARANCE in expandedCategories,
            onToggle = { toggleCategory(SettingsCategory.APPEARANCE) },
            summaryText = appearanceSummary
        ) {
            // App Theme Section (Ultra-compact Icon/Theme Card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            tint = StudyBluePrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("テーマカラー設定", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "アプリ全体の背景色やアクセントカラーを変更できます。目に優しいライト調や夜間ダーク調から選べます。",
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
                        verticalArrangement = Arrangement.spacedBy(3.5.dp)
                    ) {
                        AppThemeColor.entries.forEach { theme ->
                            val isSelected = theme == themeColor
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(5.dp)
                                    )
                                    .background(if (isSelected) StudyBluePrimary.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { viewModel.setThemeColor(theme) }
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                    .testTag("theme_color_${theme.name}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.5.dp)
                                            .clip(CircleShape)
                                            .background(theme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = theme.title,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "💡 ${themeColor.title}: ${themeColor.description}",
                        fontSize = 8.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 11.5.sp
                    )
                }
            }

            // Card Back Theme Section (Ultra-compact)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ColorLens,
                            contentDescription = null,
                            tint = StudyBluePrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("単語カード裏面カラー", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "暗記学習中にカードをタップしてめくった際の裏面デザインを設定します。文字が見やすい配色を選べます。",
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
                        verticalArrangement = Arrangement.spacedBy(3.5.dp)
                    ) {
                        CardBackTheme.entries.forEach { backTheme ->
                            val isSelected = backTheme == cardBackTheme
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(5.dp)
                                    )
                                    .background(if (isSelected) StudyBluePrimary.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { viewModel.setCardBackTheme(backTheme) }
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                    .testTag("card_back_theme_${backTheme.name}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    listOf(backTheme.gradientStart, backTheme.gradientEnd)
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = backTheme.title,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "💡 ${cardBackTheme.title}: ${cardBackTheme.description}",
                        fontSize = 8.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 11.5.sp
                    )
                }
            }

            // Font Selection Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TextFields, contentDescription = null, tint = StudyBluePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("文字フォント設定", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "カードや画面全体の文字フォントを選択します。",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppFontFamily.entries.forEach { fontOption ->
                            val isSelected = fontOption == fontFamily
                            val fontFam = com.example.ui.theme.getFontFamily(fontOption)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .background(if (isSelected) StudyBluePrimary.copy(alpha = 0.08f) else Color.Transparent)
                                    .clickable { viewModel.setFontFamily(fontOption) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("font_family_${fontOption.name}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = fontOption.title,
                                                fontSize = 13.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                fontFamily = fontFam,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "選択中",
                                                    tint = StudyBluePrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = fontOption.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = fontOption.previewSample,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Normal,
                                            fontFamily = fontFam,
                                            color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }

        // 4. デッキ・データ管理 (Decks & Data Management)
        SettingsAccordionSection(
            category = SettingsCategory.DATA_MANAGEMENT,
            isExpanded = SettingsCategory.DATA_MANAGEMENT in expandedCategories,
            onToggle = { toggleCategory(SettingsCategory.DATA_MANAGEMENT) },
            summaryText = dataManagementSummary
        ) {
            // Feature ⑧: Deck Management Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = StudyBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📁 デッキ管理", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "デッキの名前変更・削除・入出力（※新規追加は「追加」タブから）",
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // List of all decks
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allDecks.forEach { deck ->
                            val cardCount = allCards.count { it.deck == deck }
                            val isDefault = deck in defaultDecks

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                                        ) {
                                            Icon(
                                                if (isDefault) Icons.Default.Folder else Icons.Default.FolderSpecial,
                                                contentDescription = null,
                                                tint = if (isDefault) MaterialTheme.colorScheme.onSurfaceVariant else StudyBluePrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = deck,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        if (isDefault) MaterialTheme.colorScheme.surfaceVariant
                                                        else StudyBluePrimary.copy(alpha = 0.15f)
                                                    )
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isDefault) "プリセット" else "カスタム",
                                                    fontSize = 9.5.sp,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isDefault) MaterialTheme.colorScheme.onSurfaceVariant else StudyBluePrimary
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${cardCount} 語",
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Action Buttons Row for each deck
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedDeckForImport = deck
                                                showImportDialog = true
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("インポート", fontSize = 11.sp, maxLines = 1, softWrap = false)
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        OutlinedButton(
                                            onClick = {
                                                selectedDeckForExport = deck
                                                showExportDialog = true
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("エクスポート", fontSize = 11.sp, maxLines = 1, softWrap = false)
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { deckToRename = deck },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "名前変更",
                                                tint = StudyBluePrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { deckToDelete = deck },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "デッキを削除",
                                                tint = StatusCrossRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Data Management (Import / Export / Delete)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("データ管理", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "現在登録されている単語: ${allCards.size} 件",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_import_button")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "インポート",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        OutlinedButton(
                            onClick = { showExportDialog = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_export_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "エクスポート",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.setDeleteAllDialogVisible(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCrossRed.copy(alpha = 0.9f)),
                        modifier = Modifier.fillMaxWidth().testTag("settings_delete_all_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("全単語データを削除")
                    }
                }
            }


            // Reset Settings (Mode-scoped)
            OutlinedButton(
                onClick = {
                    viewModel.resetSettingsToDefaults()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (learningMode.isClassical) "古語モードの設定を初期値に戻しました" else "英単語モードの設定を初期値に戻しました"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("reset_mode_settings_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (learningMode.isClassical) "古語モードの設定を初期値に戻す" else "英単語モードの設定を初期値に戻す")
            }

        }

        // App Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("アプリ情報", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(com.example.R.drawable.img_dual_wordbook_icon),
                        contentDescription = "アプリアイコン",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("英単語・古語帳 (wordbook)", fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "バージョン: v${BuildConfig.VERSION_NAME} (54万語・基本5000語優先)",
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (deckToRename != null) {
        val current = deckToRename!!
        RenameDeckDialog(
            currentName = current,
            existingDecks = allDecks,
            onDismiss = { deckToRename = null },
            onConfirm = { newName ->
                viewModel.renameCustomDeck(current, newName)
                deckToRename = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("デッキ「$current」を「$newName」に変更しました")
                }
            }
        )
    }

    if (deckToDelete != null) {
        val target = deckToDelete!!
        val cardCount = remember(target, allCards) {
            allCards.count { it.deck == target }
        }
        DeleteDeckConfirmDialog(
            deckName = target,
            cardCount = cardCount,
            fallbackDecks = allDecks,
            onDismiss = { deckToDelete = null },
            onConfirm = { fallback, deleteCards ->
                viewModel.deleteCustomDeck(target, fallback, deleteCards)
                deckToDelete = null
                coroutineScope.launch {
                    if (deleteCards) {
                        snackbarHostState.showSnackbar("デッキ「$target」と単語を削除しました")
                    } else {
                        snackbarHostState.showSnackbar("デッキ「$target」を削除し、単語を「$fallback」へ移動しました")
                    }
                }
            }
        )
    }

    if (showImportDialog) {
        ImportDialog(
            viewModel = viewModel,
            initialDeck = selectedDeckForImport,
            onDismiss = {
                showImportDialog = false
                selectedDeckForImport = null
            },
            onImportSuccess = { msg ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            viewModel = viewModel,
            initialDeck = selectedDeckForExport,
            onDismiss = {
                showExportDialog = false
                selectedDeckForExport = null
            },
            onMessage = { msg ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        )
    }

    if (showDeleteAllDialog) {
        DeleteAllConfirmDialog(
            onDismiss = { viewModel.setDeleteAllDialogVisible(false) },
            onConfirm = {
                viewModel.confirmDeleteAll()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("すべての単語を削除しました")
                }
            }
        )
    }
}
