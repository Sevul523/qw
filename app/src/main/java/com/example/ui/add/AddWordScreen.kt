package com.example.ui.add

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.FlashcardEntity
import com.example.data.repository.DuplicateStrategy
import com.example.data.repository.ImportResult
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.CreateDeckDialog
import com.example.ui.components.EXCEL_MIME_TYPES
import com.example.ui.components.ExportDialog
import com.example.ui.components.ImportDialog
import com.example.ui.components.PosDropdownSelector
import com.example.ui.components.getFileName
import com.example.ui.components.isXlsxFile
import com.example.ui.theme.StatusCrossRed
import com.example.ui.theme.StudyBluePrimary
import kotlinx.coroutines.launch

@Composable
fun AddWordScreen(
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val learningMode by viewModel.learningMode.collectAsStateWithLifecycle()
    val addWord by viewModel.addWord.collectAsStateWithLifecycle()
    val addNote by viewModel.addNote.collectAsStateWithLifecycle()
    val addDeck by viewModel.addDeck.collectAsStateWithLifecycle()
    val allDecks by viewModel.allDecks.collectAsStateWithLifecycle()
    val addDefinitions by viewModel.addDefinitions.collectAsStateWithLifecycle()
    val duplicateCard by viewModel.duplicateCard.collectAsStateWithLifecycle()
    val bulkInput by viewModel.bulkInput.collectAsStateWithLifecycle()
    val allCards by viewModel.allCards.collectAsStateWithLifecycle()
    val weblioAutoFillEnabled by viewModel.weblioAutoFillEnabled.collectAsStateWithLifecycle()
    val weblioAutoAddNoteLevelEnabled by viewModel.weblioAutoAddNoteLevelEnabled.collectAsStateWithLifecycle()
    val weblioAutoAddConjugationEnabled by viewModel.weblioAutoAddConjugationEnabled.collectAsStateWithLifecycle()
    val isWeblioLoading by viewModel.isWeblioLoading.collectAsStateWithLifecycle()
    val lastWeblioAutofilledWord by viewModel.lastWeblioAutofilledWord.collectAsStateWithLifecycle()

    var showCreateDeckInAdd by remember { mutableStateOf(false) }
    var excelTargetDeck by remember { mutableStateOf<String?>(null) }
    var bulkTargetDeck by remember(addDeck) { mutableStateOf<String?>(addDeck) }
    var singleAddError by remember { mutableStateOf<String?>(null) }
    var duplicateToConfirm by remember { mutableStateOf<FlashcardEntity?>(null) }
    var bulkDuplicatesToConfirm by remember { mutableStateOf<List<String>?>(null) }
    var bulkError by remember { mutableStateOf<String?>(null) }
    var excelError by remember { mutableStateOf<String?>(null) }
    var isExcelImporting by remember { mutableStateOf(false) }
    var excelSuccessResult by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Direct Excel (.xlsx) File Picker
    val excelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isExcelImporting = true
            excelError = null
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    excelError = "ファイルを開くことができませんでした。"
                    isExcelImporting = false
                } else {
                    viewModel.importXlsx(
                        inputStream = inputStream,
                        strategy = DuplicateStrategy.MERGE_DUPLICATES,
                        targetDeck = excelTargetDeck,
                        onResult = { res ->
                            isExcelImporting = false
                            excelSuccessResult = Pair(res.newCount, res.mergeCount)
                        },
                        onError = { err ->
                            isExcelImporting = false
                            excelError = err
                        }
                    )
                }
            } catch (e: Exception) {
                isExcelImporting = false
                excelError = "Excelの読込に失敗しました: ${e.localizedMessage}"
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val isXlsx = isXlsxFile(context, uri)
                if (isXlsx) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        bulkError = "ファイルを開くことができませんでした。"
                    } else {
                        isExcelImporting = true
                        viewModel.importXlsx(
                            inputStream = inputStream,
                            onResult = { newCount, mergeCount ->
                                isExcelImporting = false
                                excelSuccessResult = Pair(newCount, mergeCount)
                            },
                            onError = { err ->
                                isExcelImporting = false
                                bulkError = err
                            }
                        )
                    }
                } else {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val content = inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                    if (content.isNotBlank()) {
                        viewModel.setBulkInput(content)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("ファイルを読み込みました。「一括追加する」を押して登録してください。")
                        }
                    } else {
                        bulkError = "選択されたファイルが空です。"
                    }
                }
            } catch (e: Exception) {
                bulkError = "ファイルの読込に失敗しました: ${e.localizedMessage}"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dedicated Deck Creation Card (New deck addition is only possible from Add tab)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = StudyBluePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "新しいデッキの追加",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "単語帳を新規作成 (現在: ${allDecks.size}個)",
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = { showCreateDeckInAdd = true },
                    colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_create_new_deck_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("新規作成", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }

        // Section 1: Single Word Add
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "✍️ 1件ずつ追加",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (singleAddError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = singleAddError!!,
                        color = StatusCrossRed,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = addWord,
                    onValueChange = {
                        viewModel.setAddWord(it)
                        singleAddError = null
                    },
                    label = { Text(if (learningMode.isClassical) "古語 (例: おどろく, をかし)" else "英単語 (例: run, apple)") },
                    trailingIcon = if (addWord.isNotEmpty()) {
                        {
                            IconButton(onClick = {
                                viewModel.setAddWord("")
                                singleAddError = null
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "単語を消去",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth().testTag("add_word_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                if (weblioAutoFillEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            if (isWeblioLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = StudyBluePrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (learningMode.isClassical) "Weblio古語辞典から検索中..." else "Weblio英和辞典から検索中...",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = StudyBluePrimary
                                )
                            } else if (lastWeblioAutofilledWord != null && addWord.trim().equals(lastWeblioAutofilledWord, ignoreCase = true)) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = StudyBluePrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (learningMode.isClassical) "Weblio古語辞典から入力済" else "Weblio英和辞典から入力済",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = StudyBluePrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = if (learningMode.isClassical) "📖 Weblio古語辞典自動入力: ON" else "📖 Weblio自動入力: ON",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (addWord.isNotBlank() && !isWeblioLoading) {
                            TextButton(
                                onClick = { viewModel.triggerWeblioLookup(force = true) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.testTag("weblio_lookup_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (learningMode.isClassical) "Weblioから再取得" else "Weblioから再取得",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (learningMode.isClassical) "📝 Weblio古語辞典自動入力: OFF" else "📝 Weblio自動入力: OFF（設定で変更可）",
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        )
                        TextButton(
                            onClick = { viewModel.setWeblioAutoFillEnabled(true) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.testTag("enable_weblio_quick_button")
                        ) {
                            Text("ONにする", fontSize = 11.sp, color = StudyBluePrimary, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }

                if (duplicateCard != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF3CD))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("duplicate_word_inline_warning")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFF856404),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "⚠️ すでに登録済みです: ${duplicateCard!!.definitions.joinToString(" / ") { "[${it.pos.ifBlank { "なし" }}] ${it.meaning}" }}",
                                fontSize = 12.sp,
                                color = Color(0xFF856404),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Definition Rows
                addDefinitions.forEachIndexed { index, def ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        PosDropdownSelector(
                            selectedPos = def.pos,
                            onPosSelected = { newPos ->
                                viewModel.updateDefinitionRow(index, newPos, def.meaning)
                            },
                            options = if (learningMode.isClassical) com.example.data.model.PartsOfSpeech.CLASSICAL else com.example.data.model.PartsOfSpeech.ENGLISH,
                            modifier = Modifier.weight(0.42f)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        OutlinedTextField(
                            value = def.meaning,
                            onValueChange = { newMeaning ->
                                viewModel.updateDefinitionRow(index, def.pos, newMeaning)
                                singleAddError = null
                            },
                            placeholder = { Text(if (learningMode.isClassical) "現代語訳" else "意味") },
                            modifier = Modifier.weight(0.58f).testTag("add_meaning_input_$index"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (addDefinitions.size > 1) {
                            IconButton(
                                onClick = { viewModel.removeDefinitionRow(index) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .align(Alignment.CenterVertically)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "削除",
                                    tint = StatusCrossRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = { viewModel.addDefinitionRow() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (learningMode.isClassical) "➕ 別の品詞・訳を追加" else "➕ 別の品詞・意味を追加", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = addNote,
                    onValueChange = { viewModel.setAddNote(it) },
                    label = { Text("備考 (任意)") },
                    placeholder = {
                        Text(
                            text = if (learningMode.isClassical) {
                                if (weblioAutoAddConjugationEnabled) "活用の種類が自動入力されます" else "活用・用例・メモなど"
                            } else {
                                if (weblioAutoAddNoteLevelEnabled) "Weblioレベル・英検級が自動入力" else "メモや例文など"
                            },
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_note_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (learningMode.isClassical) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            if (weblioAutoAddConjugationEnabled && addNote.isNotBlank() && addNote.contains("活用")) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = StudyBluePrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "活用の種類を自動反映中",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = StudyBluePrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = if (weblioAutoAddConjugationEnabled) "📜 備考へ活用を自動追加: ON" else "📜 備考へ活用を自動追加: OFF",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (weblioAutoAddConjugationEnabled) StudyBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(
                            onClick = { viewModel.setWeblioAutoAddConjugationEnabled(!weblioAutoAddConjugationEnabled) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.testTag("toggle_weblio_conjugation_button")
                        ) {
                            Text(
                                text = if (weblioAutoAddConjugationEnabled) "OFFにする" else "ONにする",
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = StudyBluePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            if (weblioAutoAddNoteLevelEnabled && addNote.isNotBlank() && (addNote.contains("Weblio") || addNote.contains("英検"))) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = StudyBluePrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "レベル・英検級を自動反映中",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = StudyBluePrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = if (weblioAutoAddNoteLevelEnabled) "🏷️ 備考へレベル・英検級追加: ON" else "🏷️ 備考へレベル・英検級追加: OFF",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (weblioAutoAddNoteLevelEnabled) StudyBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(
                            onClick = { viewModel.setWeblioAutoAddNoteLevelEnabled(!weblioAutoAddNoteLevelEnabled) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.testTag("toggle_weblio_note_level_button")
                        ) {
                            Text(
                                text = if (weblioAutoAddNoteLevelEnabled) "OFFにする" else "ONにする",
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = StudyBluePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Target Deck Selection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = StudyBluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "登録先デッキ",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    allDecks.forEach { d ->
                        FilterChip(
                            selected = addDeck == d,
                            onClick = { viewModel.setAddDeck(d) },
                            label = { Text(d, fontSize = 11.5.sp, maxLines = 1, softWrap = false) },
                            modifier = Modifier.height(30.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudyBluePrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        viewModel.addCard(
                            onDuplicateFound = { existing ->
                                duplicateToConfirm = existing
                            },
                            onSuccess = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("単語を追加しました！")
                                }
                                viewModel.switchTab(AppTab.STUDY)
                            },
                            onError = { singleAddError = it }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_add_card_button")
                ) {
                    Text(if (learningMode.isClassical) "この古語を登録" else "この単語を登録", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        // Section: Camera / Photo / Passage Word Extraction (OCR & Confirm)
        OcrExtractSection(
            viewModel = viewModel,
            snackbarHostState = snackbarHostState
        )

        // Section 2: Direct Excel (.xlsx) Import
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF1E7E34),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Excel (.xlsx) を直接インポート",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF1E7E34)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (learningMode.isClassical) {
                        "A列:古語, B列:現代語訳, C列:品詞, D列:備考（自動処理対応）"
                    } else {
                        "A列:単語, B列:意味, C列:品詞, D列:備考（自動処理対応）"
                    },
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (excelError != null) {
                    Text(
                        text = excelError!!,
                        color = StatusCrossRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Deck selector for Excel import
                Text(
                    text = "📁 インポート先デッキ:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = excelTargetDeck == null,
                        onClick = { excelTargetDeck = null },
                        label = { Text("自動（ファイル優先）", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.height(30.dp)
                    )
                    allDecks.forEach { d ->
                        FilterChip(
                            selected = excelTargetDeck == d,
                            onClick = { excelTargetDeck = d },
                            label = { Text(d, fontSize = 11.sp, maxLines = 1, softWrap = false) },
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        excelPickerLauncher.launch(EXCEL_MIME_TYPES)
                    },
                    enabled = !isExcelImporting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E7E34)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("direct_excel_import_button")
                ) {
                    if (isExcelImporting) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Excelファイルを解析中...", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                    } else {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Excel (.xlsx) を選択してインポート",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Section 3: Bulk Import from CSV / Text Paste
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 CSV / テキスト貼り付けで一括追加",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD35400)
                    )
                }

                Text(
                    text = if (learningMode.isClassical) {
                        "形式: 古語, 現代語訳, 品詞(任意), 備考(任意)（カンマまたはタブ区切り）"
                    } else {
                        "形式: 単語, 意味, 品詞(任意), 備考(任意)（カンマまたはタブ区切り）"
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )

                // Actions: Import Dialog & Pick File
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("open_import_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Color(0xFFD35400)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "インポート画面",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD35400),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "text/*",
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "text/tab-separated-values",
                                    "text/plain",
                                    "*/*"
                                )
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("pick_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Color(0xFFD35400)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ファイル読込",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD35400),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (bulkError != null) {
                    Text(
                        text = bulkError!!,
                        color = StatusCrossRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                OutlinedTextField(
                    value = bulkInput,
                    onValueChange = {
                        viewModel.setBulkInput(it)
                        bulkError = null
                    },
                    placeholder = {
                        Text(
                            if (learningMode.isClassical) {
                                "あはれなり,しみじみと趣深い,形容動詞,ナリ活用\nをかし,趣がある・風流だ,形容詞,シク活用"
                            } else {
                                "apple,りんご,名詞,果物\nrun,走る,動詞,"
                            },
                            fontSize = 11.5.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("bulk_import_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Deck selector for Bulk import
                Text(
                    text = "📁 インポート先デッキ:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = bulkTargetDeck == null,
                        onClick = { bulkTargetDeck = null },
                        label = { Text("自動（ファイル優先）", fontSize = 11.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.height(30.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudyBluePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    allDecks.forEach { d ->
                        FilterChip(
                            selected = bulkTargetDeck == d,
                            onClick = { bulkTargetDeck = d },
                            label = { Text(d, fontSize = 11.5.sp, maxLines = 1, softWrap = false) },
                            modifier = Modifier.height(30.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudyBluePrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val input = bulkInput.trim()
                        if (input.isBlank()) {
                            bulkError = "インポートするテキストを入力してください"
                            return@Button
                        }
                        val duplicates = viewModel.checkBulkDuplicates(input)
                        if (duplicates.isNotEmpty()) {
                            bulkDuplicatesToConfirm = duplicates
                        } else {
                            viewModel.importBulk(
                                strategy = DuplicateStrategy.MERGE_DUPLICATES,
                                targetDeck = bulkTargetDeck,
                                onResult = { res ->
                                    coroutineScope.launch {
                                        val msg = if (res.mergeCount > 0) {
                                            "${res.newCount}件追加、${res.mergeCount}件を統合しました！"
                                        } else {
                                            "${res.newCount}件の単語を追加しました！"
                                        }
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                    viewModel.switchTab(AppTab.STUDY)
                                },
                                onError = { bulkError = it }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD35400)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_bulk_import_button")
                ) {
                    Text(
                        text = if (bulkTargetDeck != null) "「$bulkTargetDeck」に一括追加" else "貼り付けたデータを一括追加",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }
        }

        // Section 4: Export Quick Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "📤 単語帳データをエクスポート",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "CSVまたはテキストで保存・共有",
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showExportDialog = true },
                    enabled = allCards.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("open_export_dialog_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("出力", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }
    }

    if (excelSuccessResult != null) {
        val (newCount, mergeCount) = excelSuccessResult!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { excelSuccessResult = null },
            title = {
                Text("🎉 Excelインポート完了", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = if (mergeCount > 0) {
                            "Excelから ${newCount}件 の新しい単語を追加し、${mergeCount}件 の同一単語（品詞・意味）を統合しました！"
                        } else {
                            "Excelから ${newCount}件 の単語を追加しました！"
                        },
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        excelSuccessResult = null
                        viewModel.switchTab(AppTab.STUDY)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary)
                ) {
                    Text("学習画面へ")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        excelSuccessResult = null
                        viewModel.switchTab(AppTab.LIST)
                    }
                ) {
                    Text("一覧で確認")
                }
            }
        )
    }

    if (showImportDialog) {
        ImportDialog(
            viewModel = viewModel,
            initialDeck = bulkTargetDeck,
            onDismiss = { showImportDialog = false },
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
            onDismiss = { showExportDialog = false },
            onMessage = { msg ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        )
    }

    if (showCreateDeckInAdd) {
        CreateDeckDialog(
            existingDecks = allDecks,
            onDismiss = { showCreateDeckInAdd = false },
            onConfirm = { newDeck ->
                val added = viewModel.addCustomDeck(newDeck)
                if (added) {
                    viewModel.setAddDeck(newDeck)
                    excelTargetDeck = newDeck
                    bulkTargetDeck = newDeck
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("🎉 新しいデッキ「$newDeck」を作成しました！")
                    }
                }
                showCreateDeckInAdd = false
            }
        )
    }

    if (duplicateToConfirm != null) {
        val existing = duplicateToConfirm!!
        val currentDefs = addDefinitions.filter { it.meaning.isNotBlank() }
        val currentNote = addNote.trim()

        AlertDialog(
            onDismissRequest = { duplicateToConfirm = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE67E22),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "すでに登録されている単語です",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "「${existing.word}」はすでに単語帳に登録されています。どのように登録しますか？",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Registered content box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "【現在の登録内容】",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            existing.definitions.forEach { d ->
                                Text(
                                    text = "• [${d.pos.ifBlank { "品詞なし" }}] ${d.meaning}",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            if (existing.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "備考: ${existing.note}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // New content to add box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StudyBluePrimary.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "【新しく追加する内容】",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudyBluePrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            currentDefs.forEach { d ->
                                Text(
                                    text = "• [${d.pos.ifBlank { "品詞なし" }}] ${d.meaning}",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (currentNote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "備考: $currentNote",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            val target = duplicateToConfirm!!
                            duplicateToConfirm = null
                            viewModel.mergeIntoExisting(
                                existingCard = target,
                                onSuccess = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("既存の「${target.word}」に意味を統合しました！")
                                    }
                                    viewModel.switchTab(AppTab.STUDY)
                                },
                                onError = { singleAddError = it }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_merge_duplicate_button")
                    ) {
                        Text("意味を統合して更新", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            duplicateToConfirm = null
                            viewModel.forceAddNewCard(
                                onSuccess = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("別のカードとして登録しました！")
                                    }
                                    viewModel.switchTab(AppTab.STUDY)
                                },
                                onError = { singleAddError = it }
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_add_separate_button")
                    ) {
                        Text("別カードとして新規追加")
                    }

                    Button(
                        onClick = {
                            val wordToCancel = duplicateToConfirm?.word ?: ""
                            duplicateToConfirm = null
                            viewModel.clearAddForm()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("「$wordToCancel」は登録しませんでした（キャンセル）")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cancel_duplicate_dialog_button")
                    ) {
                        Text(
                            text = "この重複単語は登録しない（キャンセル）",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = { duplicateToConfirm = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("return_to_edit_button")
                    ) {
                        Text("入力内容を修正する", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )
    }

    if (bulkDuplicatesToConfirm != null) {
        val duplicates = bulkDuplicatesToConfirm!!
        AlertDialog(
            onDismissRequest = { bulkDuplicatesToConfirm = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE67E22),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("⚠️ すでに登録されている単語があります", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "貼り付けたデータの中に、すでに登録済みの単語が ${duplicates.size}件 含まれています: " +
                                duplicates.take(6).joinToString(", ") +
                                (if (duplicates.size > 6) " など..." else ""),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "重複している単語をどのように処理しますか？",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            bulkDuplicatesToConfirm = null
                            viewModel.importBulk(
                                strategy = DuplicateStrategy.SKIP_DUPLICATES,
                                targetDeck = bulkTargetDeck,
                                onResult = { res ->
                                    coroutineScope.launch {
                                        val msg = "${res.newCount}件の新規単語を追加しました（重複した${res.skippedCount}件は登録しませんでした）"
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                    viewModel.switchTab(AppTab.STUDY)
                                },
                                onError = { bulkError = it }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bulk_skip_duplicates_button")
                    ) {
                        Text("重複単語は登録しない（スキップ）", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            bulkDuplicatesToConfirm = null
                            viewModel.importBulk(
                                strategy = DuplicateStrategy.MERGE_DUPLICATES,
                                targetDeck = bulkTargetDeck,
                                onResult = { res ->
                                    coroutineScope.launch {
                                        val msg = "${res.newCount}件追加、重複${res.mergeCount}件を統合しました！"
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                    viewModel.switchTab(AppTab.STUDY)
                                },
                                onError = { bulkError = it }
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bulk_merge_duplicates_button")
                    ) {
                        Text("重複単語の意味を統合して追加")
                    }

                    OutlinedButton(
                        onClick = {
                            bulkDuplicatesToConfirm = null
                            viewModel.importBulk(
                                strategy = DuplicateStrategy.FORCE_ADD_ALL,
                                targetDeck = bulkTargetDeck,
                                onResult = { res ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("${res.newCount}件の単語をすべて追加しました！")
                                    }
                                    viewModel.switchTab(AppTab.STUDY)
                                },
                                onError = { bulkError = it }
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bulk_force_add_button")
                    ) {
                        Text("重複単語も別カードとして新規追加")
                    }

                    TextButton(
                        onClick = { bulkDuplicatesToConfirm = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bulk_cancel_all_button")
                    ) {
                        Text("すべて中止（キャンセル）", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )
    }
}
