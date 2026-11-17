package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.DuplicateStrategy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.StatusCircleGreen
import com.example.ui.theme.StatusCrossRed
import com.example.ui.theme.StudyBluePrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getFileName(context: Context, uri: Uri): String {
    var name = ""
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
    }
    return name
}

fun isXlsxFile(context: Context, uri: Uri): Boolean {
    val fileName = getFileName(context, uri).lowercase()
    if (fileName.endsWith(".xlsx") || fileName.endsWith(".xlsm") || fileName.endsWith(".xltx")) {
        return true
    }
    val mime = context.contentResolver.getType(uri)?.lowercase() ?: ""
    if (mime.contains("spreadsheetml") || mime.contains("openxmlformats") || mime.contains("excel")) {
        return true
    }
    // Check magic bytes: PK\u0003\u0004 (0x50, 0x4B, 0x03, 0x04)
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val buf = ByteArray(4)
            val readCount = stream.read(buf)
            if (readCount >= 4 && buf[0] == 0x50.toByte() && buf[1] == 0x4B.toByte() && buf[2] == 0x03.toByte() && buf[3] == 0x04.toByte()) {
                return true
            }
        }
    } catch (_: Exception) {}
    return false
}

val EXCEL_MIME_TYPES = arrayOf(
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-excel",
    "application/x-excel",
    "application/excel",
    "application/octet-stream",
    "application/zip",
    "*/*"
)

val ALL_IMPORT_MIME_TYPES = arrayOf(
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-excel",
    "text/*",
    "text/csv",
    "text/comma-separated-values",
    "text/tab-separated-values",
    "text/plain",
    "application/octet-stream",
    "application/zip",
    "*/*"
)

@Composable
fun ImportDialog(
    viewModel: MainViewModel,
    initialDeck: String? = null,
    onDismiss: () -> Unit,
    onImportSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val learningMode by viewModel.learningMode.collectAsStateWithLifecycle()
    val allDecks by viewModel.allDecks.collectAsStateWithLifecycle()
    var selectedTargetDeck by remember(initialDeck) { mutableStateOf<String?>(initialDeck) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var pasteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            errorMessage = null
            try {
                val isXlsx = isXlsxFile(context, uri)
                val inputStream = context.contentResolver.openInputStream(uri)

                if (inputStream == null) {
                    errorMessage = "ファイルを開くことができませんでした"
                    isProcessing = false
                } else {
                    if (isXlsx) {
                        viewModel.importXlsx(
                            inputStream = inputStream,
                            strategy = DuplicateStrategy.MERGE_DUPLICATES,
                            targetDeck = selectedTargetDeck,
                            onResult = { res ->
                                isProcessing = false
                                val msg = if (res.mergeCount > 0) {
                                    "Excelから${res.newCount}件追加、${res.mergeCount}件統合しました！"
                                } else {
                                    "Excelから${res.newCount}件追加しました！"
                                }
                                onImportSuccess(msg)
                                onDismiss()
                            },
                            onError = { err ->
                                isProcessing = false
                                errorMessage = err
                            }
                        )
                    } else {
                        val text = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        viewModel.importText(
                            text = text,
                            strategy = DuplicateStrategy.MERGE_DUPLICATES,
                            targetDeck = selectedTargetDeck,
                            onResult = { res ->
                                isProcessing = false
                                val msg = if (res.mergeCount > 0) {
                                    "${res.newCount}件追加、${res.mergeCount}件統合しました！"
                                } else {
                                    "${res.newCount}件追加しました！"
                                }
                                onImportSuccess(msg)
                                onDismiss()
                            },
                            onError = { err ->
                                isProcessing = false
                                errorMessage = err
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                isProcessing = false
                errorMessage = "読込エラー: ${e.localizedMessage}"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (learningMode.isClassical) "古語データの一括インポート" else "単語データの一括インポート",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Deck Target Selection
                Column(modifier = Modifier.fillMaxWidth()) {
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
                            selected = selectedTargetDeck == null,
                            onClick = { selectedTargetDeck = null },
                            label = { Text("自動（ファイル優先）", fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudyBluePrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                        allDecks.forEach { deck ->
                            FilterChip(
                                selected = selectedTargetDeck == deck,
                                onClick = { selectedTargetDeck = deck },
                                label = { Text(deck, fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudyBluePrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Excel / ファイル") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("テキスト貼り付け") }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "📗 Excel (.xlsx) 直接インポート対応",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E7E34)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (learningMode.isClassical) {
                                        "• 列構成: 古語, 現代語訳, 品詞, 備考, ステータス(○/△/×), デッキ\n" +
                                            "• デッキ（カテゴリー）列も自動判別して振り分けられます\n" +
                                            "• 学習データやステータスもそのまま引き継がれます\n" +
                                            "• No.列(1, 2, 3...)やヘッダー行も自動判別\n" +
                                            "• Excelの振り仮名(ルビ)も自動除去\n" +
                                            "• CSV / TSVファイルにも対応（例: あはれなり,しみじみと趣深い,形容動詞,ナリ活用）"
                                    } else {
                                        "• 列構成: 単語, 意味, 品詞, 備考, ステータス(○/△/×), デッキ\n" +
                                            "• デッキ（カテゴリー）列も自動判別して振り分けられます\n" +
                                            "• 学習データやステータスもそのまま引き継がれます\n" +
                                            "• No.列(1, 2, 3...)やヘッダー行も自動判別\n" +
                                            "• Excelの振り仮名(ルビ)も自動除去\n" +
                                            "• CSV / TSVファイルにも対応"
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                filePickerLauncher.launch(EXCEL_MIME_TYPES)
                            },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E7E34)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("select_excel_file_button")
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Excel解析中...")
                            } else {
                                Icon(Icons.Default.FileUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Excel (.xlsx) を直接インポート", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch(ALL_IMPORT_MIME_TYPES)
                            },
                            enabled = !isProcessing,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_any_file_button")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CSV / TSV / その他のファイルを選択")
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = "📁 インポート先デッキ: ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedTargetDeck?.let { "「$it」" } ?: "自動（ファイル優先）",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudyBluePrimary
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedTargetDeck == null,
                                onClick = { selectedTargetDeck = null },
                                label = { Text("自動（ファイル優先）", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudyBluePrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                            allDecks.forEach { deck ->
                                FilterChip(
                                    selected = selectedTargetDeck == deck,
                                    onClick = { selectedTargetDeck = deck },
                                    label = { Text(deck, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StudyBluePrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (learningMode.isClassical) {
                                "古語、現代語訳、品詞(任意)、備考(任意)、ステータス(任意)、デッキ(任意)をカンマまたはタブ区切りで入力してください。"
                            } else {
                                "単語、意味、品詞(任意)、備考(任意)、ステータス(任意)、デッキ(任意)をタブまたはカンマ区切りで入力してください。"
                            },
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pasteText,
                            onValueChange = { pasteText = it },
                            placeholder = {
                                Text(
                                    if (learningMode.isClassical) {
                                        "例:\nあはれなり,しみじみと趣深い,形容動詞,ナリ活用 / 王朝文学の美的理念,○,最頻出形容詞・形容動詞\nおどろく,目を覚ます,動詞,カ行四段活用 / 古今異義語,△,共通テスト最重要動詞"
                                    } else {
                                        "例: apple\tりんご\t名詞\t果物\t○\t日常英会話"
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .testTag("import_paste_input"),
                            maxLines = 8
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (pasteText.isNotBlank()) {
                                    isProcessing = true
                                    errorMessage = null
                                    viewModel.importText(
                                        text = pasteText,
                                        strategy = DuplicateStrategy.MERGE_DUPLICATES,
                                        targetDeck = selectedTargetDeck,
                                        onResult = { res ->
                                            isProcessing = false
                                            val msg = if (res.mergeCount > 0) {
                                                "${res.newCount}件追加、${res.mergeCount}件統合しました！"
                                            } else {
                                                "${res.newCount}件追加しました！"
                                            }
                                            onImportSuccess(msg)
                                            onDismiss()
                                        },
                                        onError = { err ->
                                            isProcessing = false
                                            errorMessage = err
                                        }
                                    )
                                }
                            },
                            enabled = pasteText.isNotBlank() && !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary),
                            modifier = Modifier.fillMaxWidth().testTag("import_paste_submit_button")
                        ) {
                            Text(
                                text = if (selectedTargetDeck != null) "「$selectedTargetDeck」にインポート" else "貼り付けたデータをインポート",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = StatusCrossRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

@Composable
fun ExportDialog(
    viewModel: MainViewModel,
    initialDeck: String? = null,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val learningMode by viewModel.learningMode.collectAsStateWithLifecycle()
    val allCards by viewModel.allCards.collectAsStateWithLifecycle()
    val allDecks by viewModel.allDecks.collectAsStateWithLifecycle()
    var selectedExportDeck by remember(initialDeck) { mutableStateOf<String?>(initialDeck) }
    var isCsvFormat by remember { mutableStateOf(true) }
    var exportedContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(if (isCsvFormat) "text/csv" else "text/plain")
    ) { uri: Uri? ->
        if (uri != null && exportedContent.isNotBlank()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(exportedContent.toByteArray(Charsets.UTF_8))
                }
                onMessage("ファイルを保存しました")
                onDismiss()
            } catch (e: Exception) {
                onMessage("保存に失敗しました: ${e.localizedMessage}")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (learningMode.isClassical) "古語データのエクスポート" else "単語データのエクスポート",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (learningMode.isClassical) {
                        "古語データ、学習ステータス(○/△/×)、非表示設定、デッキ（分類）情報を含めてCSV形式またはTSV形式で書き出します。"
                    } else {
                        "単語データ、学習ステータス(○/△/×)、非表示設定、デッキ（分類）情報を含めてCSV形式またはTSV形式で書き出します。"
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Deck selector for export
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "📁 エクスポート対象デッキ:",
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
                            selected = selectedExportDeck == null,
                            onClick = { selectedExportDeck = null },
                            label = { Text("すべてのデッキ (${allCards.size})", fontSize = 11.sp) }
                        )
                        allDecks.forEach { deck ->
                            val count = allCards.count { it.deck == deck }
                            FilterChip(
                                selected = selectedExportDeck == deck,
                                onClick = { selectedExportDeck = deck },
                                label = { Text("$deck ($count)", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { isCsvFormat = true },
                        colors = if (isCsvFormat) ButtonDefaults.outlinedButtonColors(containerColor = StudyBluePrimary.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CSV形式 (,)")
                    }
                    OutlinedButton(
                        onClick = { isCsvFormat = false },
                        colors = if (!isCsvFormat) ButtonDefaults.outlinedButtonColors(containerColor = StudyBluePrimary.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("TSV形式 (タブ)")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isLoading = true
                        val callback = { text: String ->
                            exportedContent = text
                            isLoading = false
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            val ext = if (isCsvFormat) "csv" else "txt"
                            val deckSuffix = if (selectedExportDeck != null) "_${selectedExportDeck}" else ""
                            createDocumentLauncher.launch("wordbook${deckSuffix}_$timeStamp.$ext")
                        }
                        if (isCsvFormat) {
                            viewModel.exportDataAsCsv(deckFilter = selectedExportDeck, onSuccess = callback)
                        } else {
                            viewModel.exportDataAsTsv(deckFilter = selectedExportDeck, onSuccess = callback)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary),
                    modifier = Modifier.fillMaxWidth().testTag("export_save_file_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ファイルとして書き出し保存")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val callback = { text: String ->
                            clipboardManager.setText(AnnotatedString(text))
                            onMessage("クリップボードにコピーしました")
                            onDismiss()
                        }
                        if (isCsvFormat) {
                            viewModel.exportDataAsCsv(deckFilter = selectedExportDeck, onSuccess = callback)
                        } else {
                            viewModel.exportDataAsTsv(deckFilter = selectedExportDeck, onSuccess = callback)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("export_copy_clipboard_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("クリップボードにコピー")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}
