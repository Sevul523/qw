package com.example.ui.dictionary

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.service.DictionaryLevelFilter
import com.example.data.service.InternalDictionaryResult
import com.example.data.service.InternalDictionaryService
import com.example.ui.MainViewModel
import com.example.ui.theme.StatusCircleGreen
import com.example.ui.theme.StudyBluePrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DictionaryScreen(
    viewModel: MainViewModel,
    onSpeak: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val learningMode by viewModel.learningMode.collectAsStateWithLifecycle()
    val isClassicalMode = learningMode.isClassical
    val registeredWords by viewModel.registeredWordSet.collectAsStateWithLifecycle()
    val allDecks by viewModel.allDecks.collectAsStateWithLifecycle()
    val currentAddDeck by viewModel.addDeck.collectAsStateWithLifecycle()
    val customOverrides by viewModel.customDictionaryOverrides.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedLevelFilter by remember { mutableStateOf(DictionaryLevelFilter.ALL) }
    var selectedFilterPos by remember { mutableStateOf<String?>(null) }
    var onlyUnregistered by remember { mutableStateOf(false) }
    var onlyCustomized by remember { mutableStateOf(false) }

    // Reset mode-specific filters automatically when LearningMode toggles between English and Classical
    LaunchedEffect(isClassicalMode) {
        selectedFilterPos = null
        if (isClassicalMode) {
            selectedLevelFilter = DictionaryLevelFilter.ALL
        }
    }

    var selectedTargetDeck by remember(currentAddDeck, allDecks, isClassicalMode) {
        mutableStateOf(
            if (currentAddDeck.isNotBlank() && currentAddDeck in allDecks) currentAddDeck
            else allDecks.firstOrNull() ?: if (isClassicalMode) "共通テスト最重要動詞" else "日常英会話"
        )
    }
    var showDeckDropdown by remember { mutableStateOf(false) }
    var justAddedWords by remember { mutableStateOf(setOf<String>()) }

    // Dialog States
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<InternalDictionaryResult?>(null) }
    var entryToDelete by remember { mutableStateOf<InternalDictionaryResult?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val searchResults by remember(searchQuery, isClassicalMode, selectedLevelFilter, customOverrides) {
        derivedStateOf {
            InternalDictionaryService.search(
                query = searchQuery,
                isClassical = isClassicalMode,
                levelFilter = if (isClassicalMode) DictionaryLevelFilter.ALL else selectedLevelFilter,
                limit = 200
            )
        }
    }

    val filteredResults by remember(
        searchResults,
        selectedFilterPos,
        onlyUnregistered,
        onlyCustomized,
        registeredWords,
        justAddedWords
    ) {
        derivedStateOf {
            searchResults.filter { item ->
                val normalizedKey = item.word.lowercase().trim()
                val isRegistered = normalizedKey in registeredWords || normalizedKey in justAddedWords
                if (onlyUnregistered && isRegistered) return@filter false
                if (onlyCustomized && !item.isCustomEdited && !item.isUserCreated) return@filter false

                if (selectedFilterPos != null) {
                    val p = item.pos
                    when (selectedFilterPos) {
                        "名詞" -> p.contains("名")
                        "動詞" -> p.contains("動")
                        "形容詞" -> p.contains("形")
                        "副詞" -> p.contains("副")
                        "助動詞" -> p.contains("助動詞")
                        "助詞" -> p.contains("助詞")
                        "枕詞" -> p.contains("枕詞")
                        "連語" -> p.contains("連語") || p.contains("熟語")
                        else -> p.contains(selectedFilterPos!!)
                    }
                } else {
                    true
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("dictionary_screen")
    ) {
        // Compact Single-Row Header + Inline Actions (no vertical bloat or squeezed badges)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = StudyBluePrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = StudyBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isClassicalMode) "内蔵古語辞典" else "内蔵英和・熟語辞書",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isClassicalMode) {
                            "古語 900語以上・古語モード連動"
                        } else {
                            "基礎5千語優先・約54万語・英単語連動"
                        },
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { showAddCustomDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("add_custom_dict_word_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "新規登録",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                IconButton(
                    onClick = { showResetConfirmDialog = true },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("reset_custom_dict_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "初期状態にリセット",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Search Bar (Single-line placeholder)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dict_search_input"),
            placeholder = {
                Text(
                    text = if (isClassicalMode) {
                        "古語・現代語訳で検索 (例: をかし)"
                    } else {
                        "英単語・熟語・日本語で検索"
                    },
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = StudyBluePrimary, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "検索クリア", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Level Filter Chips (English mode only)
        if (!isClassicalMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DictionaryLevelFilter.entries.forEach { level ->
                    FilterChip(
                        selected = selectedLevelFilter == level,
                        onClick = { selectedLevelFilter = level },
                        label = {
                            Text(
                                text = level.label,
                                fontSize = 11.sp,
                                fontWeight = if (selectedLevelFilter == level) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("dict_level_filter_${level.id}")
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // POS Filter Chips
        val posFilters = if (isClassicalMode) {
            listOf("動詞", "形容詞", "形容動詞", "助動詞", "助詞", "枕詞", "名詞", "副詞", "連語")
        } else {
            listOf("名詞", "動詞", "形容詞", "副詞", "熟語・連語")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilterPos == null,
                onClick = { selectedFilterPos = null },
                label = { Text("すべての品詞", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                modifier = Modifier.height(30.dp)
            )
            posFilters.forEach { pos ->
                FilterChip(
                    selected = selectedFilterPos == pos,
                    onClick = {
                        selectedFilterPos = if (selectedFilterPos == pos) null else pos
                    },
                    label = { Text(pos, fontSize = 11.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.height(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Checkboxes & Target Deck Selector Row (wrapped gracefully on narrow screens, zero text-squashing)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onlyUnregistered = !onlyUnregistered }
                ) {
                    Checkbox(
                        checked = onlyUnregistered,
                        onCheckedChange = { onlyUnregistered = it },
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "未登録のみ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onlyCustomized = !onlyCustomized }
                ) {
                    Checkbox(
                        checked = onlyCustomized,
                        onCheckedChange = { onlyCustomized = it },
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "編集済のみ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Target deck selector dropdown
            Box(modifier = Modifier.weight(1f, fill = false).padding(start = 6.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { showDeckDropdown = true }
                ) {
                    Text(
                        text = "追加先: $selectedTargetDeck",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    )
                }

                DropdownMenu(
                    expanded = showDeckDropdown,
                    onDismissRequest = { showDeckDropdown = false }
                ) {
                    allDecks.forEach { deck ->
                        DropdownMenuItem(
                            text = { Text(deck, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = {
                                selectedTargetDeck = deck
                                showDeckDropdown = false
                            }
                        )
                    }
                }
            }
        }

        // Result count indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "表示: ${filteredResults.size}語 / 内蔵: ${if (isClassicalMode) "約916語" else "約54万語"}",
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
            if (searchQuery.isNotBlank()) {
                Text(
                    text = "「$searchQuery」",
                    fontSize = 10.5.sp,
                    color = StudyBluePrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Dictionary Results List
        if (filteredResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "該当する単語がありません" else "「$searchQuery」に一致する単語はありません",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (selectedLevelFilter != DictionaryLevelFilter.ALL || selectedFilterPos != null || onlyUnregistered || onlyCustomized) {
                            OutlinedButton(
                                onClick = {
                                    selectedLevelFilter = DictionaryLevelFilter.ALL
                                    selectedFilterPos = null
                                    onlyUnregistered = false
                                    onlyCustomized = false
                                }
                            ) {
                                Text("フィルター解除", fontSize = 12.sp, maxLines = 1, softWrap = false)
                            }
                        }
                        Button(
                            onClick = { showAddCustomDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("内蔵辞書に追加", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("dict_results_list"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(filteredResults, key = { it.word + "_" + it.isClassical }) { result ->
                    val normalizedKey = result.word.lowercase().trim()
                    val isAlreadyRegistered = normalizedKey in registeredWords || normalizedKey in justAddedWords

                    DictionaryEntryCard(
                        result = result,
                        isRegistered = isAlreadyRegistered,
                        onSpeak = { onSpeak(result.word) },
                        onAddToDeck = {
                            viewModel.addWordFromDictionary(
                                result = result,
                                targetDeckOverride = selectedTargetDeck
                            ) { success ->
                                if (success) {
                                    justAddedWords = justAddedWords + normalizedKey
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("「${result.word}」をデッキ「$selectedTargetDeck」に追加しました")
                                    }
                                }
                            }
                        },
                        onPopulateAddForm = {
                            viewModel.populateAddFormFromDictionary(result)
                        },
                        onEdit = {
                            entryToEdit = result
                        },
                        onDelete = {
                            entryToDelete = result
                        }
                    )
                }
            }
        }
    }

    // Add New Custom Entry Dialog
    if (showAddCustomDialog) {
        DictionaryEditDialog(
            title = if (isClassicalMode) "内蔵辞書に新しい古語を登録" else "内蔵辞書に新しい英単語を登録",
            initialWord = searchQuery.trim(),
            initialPos = if (isClassicalMode) "古語" else "名詞",
            initialMeaning = "",
            initialDeck = selectedTargetDeck,
            initialNote = "",
            onDismiss = { showAddCustomDialog = false },
            onSave = { word, pos, meanings, deck, note ->
                viewModel.saveCustomDictionaryEntry(
                    word = word,
                    pos = pos,
                    meanings = meanings,
                    deck = deck,
                    note = note,
                    isClassical = isClassicalMode
                )
                showAddCustomDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("「$word」を内蔵辞書に登録しました")
                }
            }
        )
    }

    // Edit Existing Entry Dialog
    entryToEdit?.let { item ->
        DictionaryEditDialog(
            title = "内蔵辞書の単語を編集",
            initialWord = item.word,
            initialPos = item.pos,
            initialMeaning = item.summaryMeaning,
            initialDeck = item.deck.ifEmpty { selectedTargetDeck },
            initialNote = item.note,
            onDismiss = { entryToEdit = null },
            onSave = { word, pos, meanings, deck, note ->
                viewModel.saveCustomDictionaryEntry(
                    word = word,
                    pos = pos,
                    meanings = meanings,
                    deck = deck,
                    note = note,
                    isClassical = item.isClassical
                )
                entryToEdit = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("「$word」の辞書内容を更新しました")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    entryToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("内蔵辞書から削除") },
            text = {
                Text(
                    text = "「${item.word}」を内蔵辞書から削除（非表示）にしますか？\n\n※ 削除しても「初期状態にリセット」でいつでも復元可能です。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDictionaryEntry(item.word, item.isClassical)
                        val removedWord = item.word
                        entryToDelete = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("「$removedWord」を内蔵辞書から削除しました")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("削除する", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("内蔵辞書の編集をリセット") },
            text = {
                Text(
                    text = "これまでユーザーが編集・追加・削除した辞書データをすべてリセットし、初期状態（英和約54万語・古語900語以上）に戻しますか？\n\n※ 単語帳に登録済みのカードは影響を受けずそのまま残ります。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetCustomDictionary()
                        showResetConfirmDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("内蔵辞書を初期状態にリセットしました")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("初期化する", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictionaryEntryCard(
    result: InternalDictionaryResult,
    isRegistered: Boolean,
    onSpeak: () -> Unit,
    onAddToDeck: () -> Unit,
    onPopulateAddForm: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            // Row 1: Headword + TTS + Edit/Delete (clean separation keeps badges from ever wrapping vertically)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = result.word,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!result.isClassical) {
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "発音",
                                tint = StudyBluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "内蔵辞書を編集",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "内蔵辞書から削除",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Row 2: FlowRow of compact single-line badges (never wraps mid-badge)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!result.isClassical && result.levelBadge.isNotBlank() && !result.isUserCreated && !result.isCustomEdited) {
                    val (badgeBg, badgeFg) = when (result.priorityTier) {
                        1 -> Color(0xFF2E7D32).copy(alpha = 0.14f) to Color(0xFF1B5E20)
                        2 -> StudyBluePrimary.copy(alpha = 0.14f) to StudyBluePrimary
                        3 -> Color(0xFFEF6C00).copy(alpha = 0.14f) to Color(0xFFE65100)
                        4 -> Color(0xFF00838F).copy(alpha = 0.14f) to Color(0xFF006064)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.14f) to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeBg,
                        contentColor = badgeFg
                    ) {
                        Text(
                            text = result.levelBadge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                if (result.pos.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = StudyBluePrimary.copy(alpha = 0.12f),
                        contentColor = StudyBluePrimary
                    ) {
                        Text(
                            text = result.pos,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                if (result.isUserCreated) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = StatusCircleGreen.copy(alpha = 0.14f),
                        contentColor = StatusCircleGreen
                    ) {
                        Text(
                            text = "⭐ ユーザー登録",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                } else if (result.isCustomEdited) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF57C00).copy(alpha = 0.14f),
                        contentColor = Color(0xFFE65100)
                    ) {
                        Text(
                            text = "✏️ 編集済",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                if (result.deck.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = result.deck,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Definitions (Concise, bounded vertical footprint)
            val displayedDefs = result.definitions.take(3)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                displayedDefs.forEachIndexed { index, def ->
                    val cleanedMeaning = def.meaning.trim().replace(Regex("[\\r\\n]+"), " ")
                    Row(verticalAlignment = Alignment.Top) {
                        if (displayedDefs.size > 1) {
                            Text(
                                text = "${index + 1}. ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudyBluePrimary
                            )
                        }
                        Text(
                            text = cleanedMeaning,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Note / Etymology / Conjugation (compact bounded strip)
            val cleanedNote = result.note.trim().replace(Regex("[\\r\\n]+"), " ")
            if (cleanedNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = cleanedNote,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons Row (never wraps button labels)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPopulateAddForm,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("フォーム適用", fontSize = 11.sp, maxLines = 1, softWrap = false)
                }

                Spacer(modifier = Modifier.width(6.dp))

                if (isRegistered) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusCircleGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, StatusCircleGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = StatusCircleGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "登録済み",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusCircleGreen,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onAddToDeck,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudyBluePrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("dict_add_button_${result.word}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "単語帳に追加",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DictionaryEditDialog(
    title: String,
    initialWord: String,
    initialPos: String,
    initialMeaning: String,
    initialDeck: String,
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (word: String, pos: String, meaning: String, deck: String, note: String) -> Unit
) {
    var word by remember { mutableStateOf(initialWord) }
    var pos by remember { mutableStateOf(initialPos) }
    var meaning by remember { mutableStateOf(initialMeaning) }
    var deck by remember { mutableStateOf(initialDeck) }
    var note by remember { mutableStateOf(initialNote) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = word,
                    onValueChange = { word = it },
                    label = { Text("単語・見出し語 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pos,
                    onValueChange = { pos = it },
                    label = { Text("品詞 (例: 名詞, 動詞, 形容詞, 助動詞)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it },
                    label = { Text("日本語の意味・訳 * (セミコロン区切り可)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                OutlinedTextField(
                    value = deck,
                    onValueChange = { deck = it },
                    label = { Text("分類・デッキ名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("補足解説・語源メモ") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.5.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (word.isBlank()) {
                        errorMessage = "単語を入力してください"
                        return@Button
                    }
                    if (meaning.isBlank()) {
                        errorMessage = "意味・訳を入力してください"
                        return@Button
                    }
                    onSave(word.trim(), pos.trim(), meaning.trim(), deck.trim(), note.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary)
            ) {
                Text("保存する", color = Color.White, maxLines = 1, softWrap = false)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル", maxLines = 1, softWrap = false)
            }
        }
    )
}
