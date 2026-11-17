package com.example.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.FlashcardEntity
import com.example.data.model.Definition
import com.example.data.model.PartsOfSpeech
import com.example.ui.MainViewModel
import com.example.ui.components.PosDropdownSelector
import com.example.ui.theme.StatusCircleGreen
import com.example.ui.theme.StatusCrossRed
import com.example.ui.theme.StudyBluePrimary
import kotlinx.coroutines.launch

data class EditableCandidate(
    val id: String,
    val word: String,
    val definitions: List<Definition>,
    val note: String,
    val deck: String,
    val isIdiom: Boolean = false,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrConfirmEditDialog(
    words: List<String>,
    initialDeck: String,
    allDecks: List<String>,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onConfirmAdd: (List<FlashcardEntity>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val learningMode by viewModel.learningMode.collectAsStateWithLifecycle()
    val ocrAutoLookupMeaningEnabled by viewModel.ocrAutoLookupMeaningEnabled.collectAsStateWithLifecycle()
    val defaultPos = if (learningMode.isClassical) "動詞" else "名詞"
    val posOptions = if (learningMode.isClassical) PartsOfSpeech.CLASSICAL else PartsOfSpeech.ENGLISH

    var targetDeckAll by remember { mutableStateOf(initialDeck) }
    var isBatchLoading by remember { mutableStateOf(false) }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }

    val candidates = remember {
        mutableStateListOf<EditableCandidate>().apply {
            words.forEachIndexed { index, w ->
                val isIdiom = w.contains(" ") || (!learningMode.isClassical && com.example.data.service.InternalDictionaryService.isIdiomPhrase(w))
                val candidatePos = if (isIdiom && !learningMode.isClassical) "句動詞" else defaultPos
                val candidateDeck = if (isIdiom && !learningMode.isClassical && allDecks.any { it.contains("熟語") }) {
                    allDecks.firstOrNull { it.contains("熟語") } ?: initialDeck
                } else {
                    initialDeck
                }
                add(
                    EditableCandidate(
                        id = "$w-$index",
                        word = w,
                        definitions = listOf(Definition(pos = candidatePos, meaning = "")),
                        note = "",
                        deck = candidateDeck,
                        isIdiom = isIdiom,
                        isLoading = ocrAutoLookupMeaningEnabled
                    )
                )
            }
        }
    }

    fun batchFetchMeanings(forceWeblio: Boolean = false) {
        coroutineScope.launch {
            isBatchLoading = true
            for (i in candidates.indices) {
                val candidate = candidates[i]
                candidates[i] = candidate.copy(isLoading = true)
                val details = viewModel.lookupCandidateDetails(candidate.word, forceWeblio = forceWeblio)
                val nextNote = if (candidate.note.isBlank() && details.note.isNotBlank()) {
                    details.note
                } else {
                    candidate.note
                }
                if (details.definitions.isNotEmpty()) {
                    val resolvedDefs = details.definitions.map { def ->
                        def.copy(pos = def.pos.ifBlank { defaultPos })
                    }
                    candidates[i] = candidate.copy(
                        definitions = resolvedDefs,
                        note = nextNote,
                        isLoading = false
                    )
                } else {
                    candidates[i] = candidate.copy(
                        note = nextNote,
                        isLoading = false
                    )
                }
            }
            isBatchLoading = false
        }
    }

    // Auto-fetch definitions from Weblio / KnowledgeBase ONLY IF setting is ON
    LaunchedEffect(words, ocrAutoLookupMeaningEnabled) {
        if (!ocrAutoLookupMeaningEnabled) {
            isBatchLoading = false
            for (i in candidates.indices) {
                candidates[i] = candidates[i].copy(isLoading = false)
            }
            return@LaunchedEffect
        }
        batchFetchMeanings(forceWeblio = false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .fillMaxHeight(0.92f)
            .testTag("ocr_confirm_edit_dialog")
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudyBluePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = StudyBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (learningMode.isClassical) "抽出古語の編集と確認" else "抽出単語の編集と確認",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (learningMode.isClassical) {
                                    "${candidates.size}件の古語を確認・修正して追加できます"
                                } else {
                                    "${candidates.size}件の単語を確認・修正して追加できます"
                                },
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_ocr_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "閉じる", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Target Deck Selection Header
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "一括登録先デッキ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            allDecks.forEach { deck ->
                                FilterChip(
                                    selected = targetDeckAll == deck,
                                    onClick = {
                                        targetDeckAll = deck
                                        for (i in candidates.indices) {
                                            candidates[i] = candidates[i].copy(deck = deck)
                                        }
                                    },
                                    label = { Text(deck, fontSize = 11.5.sp, maxLines = 1, softWrap = false) },
                                    modifier = Modifier.height(30.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StudyBluePrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Setting Indicator & On-demand fetch option
                if (isBatchLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = StudyBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (learningMode.isClassical) {
                                "Weblio古語辞典から品詞・現代語訳を自動検索中..."
                            } else {
                                "Weblio / 辞書から品詞・意味を自動検索中..."
                            },
                            fontSize = 11.5.sp,
                            color = StudyBluePrimary
                        )
                    }
                } else if (!ocrAutoLookupMeaningEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "※ 意味自動検索: OFF（設定で変更可）",
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        )
                        TextButton(
                            onClick = { batchFetchMeanings(forceWeblio = true) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.testTag("ocr_batch_fetch_meanings_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (learningMode.isClassical) "Weblio古語辞典から取得" else "Weblioから一括取得",
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = StudyBluePrimary
                            )
                        }
                    }
                }

                val idiomCount = candidates.count { it.isIdiom }
                val wordCount = candidates.size - idiomCount
                if (idiomCount > 0 && !learningMode.isClassical) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedTypeFilter == "ALL",
                            onClick = { selectedTypeFilter = "ALL" },
                            label = { Text("すべて (${candidates.size})", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                            modifier = Modifier.height(30.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudyBluePrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedTypeFilter == "IDIOM",
                            onClick = { selectedTypeFilter = "IDIOM" },
                            label = { Text("★ 熟語・連語 ($idiomCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
                            modifier = Modifier.height(30.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFD97706),
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedTypeFilter == "WORD",
                            onClick = { selectedTypeFilter = "WORD" },
                            label = { Text("単語 ($wordCount)", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                            modifier = Modifier.height(30.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudyBluePrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val displayedEntries = candidates.withIndex().filter { (_, item) ->
                    when (selectedTypeFilter) {
                        "IDIOM" -> item.isIdiom
                        "WORD" -> !item.isIdiom
                        else -> true
                    }
                }

                // Editable Word List
                if (candidates.isEmpty() || displayedEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (candidates.isEmpty()) "追加対象の単語がありません" else "該当する候補がありません",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displayedEntries, key = { it.value.id }) { entry ->
                            val index = entry.index
                            val item = entry.value
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Row 1: Word index + Weblio refresh + Delete button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "#${index + 1}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StudyBluePrimary
                                            )
                                            if (item.isIdiom) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFFD97706).copy(alpha = 0.18f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "★ 熟語・連語",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        color = Color(0xFFD97706)
                                                    )
                                                }
                                            }
                                            if (item.definitions.size > 1) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(StudyBluePrimary.copy(alpha = 0.14f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "${item.definitions.size}品詞",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        color = StudyBluePrimary
                                                    )
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        candidates[index] = item.copy(isLoading = true)
                                                        val details = viewModel.lookupCandidateDetails(item.word, forceWeblio = true)
                                                        val nextNote = if (item.note.isBlank() && details.note.isNotBlank()) {
                                                            details.note
                                                        } else {
                                                            item.note
                                                        }
                                                        if (details.definitions.isNotEmpty()) {
                                                            val resolvedDefs = details.definitions.map { def ->
                                                                def.copy(pos = def.pos.ifBlank { defaultPos })
                                                            }
                                                            candidates[index] = item.copy(
                                                                definitions = resolvedDefs,
                                                                note = nextNote,
                                                                isLoading = false
                                                            )
                                                        } else {
                                                            candidates[index] = item.copy(
                                                                note = nextNote,
                                                                isLoading = false
                                                            )
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp).testTag("refresh_candidate_${item.word}")
                                            ) {
                                                if (item.isLoading) {
                                                    CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                                                } else {
                                                    Icon(
                                                        Icons.Default.Refresh,
                                                        contentDescription = "Weblioから再検索",
                                                        tint = StudyBluePrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = { candidates.removeAt(index) },
                                                modifier = Modifier.size(32.dp).testTag("delete_candidate_${item.word}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "削除",
                                                    tint = StatusCrossRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Word Input
                                    OutlinedTextField(
                                        value = item.word,
                                        onValueChange = { newWord ->
                                            candidates[index] = item.copy(word = newWord)
                                        },
                                        label = { Text(if (learningMode.isClassical) "古語" else "英単語", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Multi-POS & Meaning Rows
                                    item.definitions.forEachIndexed { defIdx, def ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            PosDropdownSelector(
                                                selectedPos = def.pos,
                                                onPosSelected = { newPos ->
                                                    val updated = item.definitions.toMutableList()
                                                    if (defIdx in updated.indices) {
                                                        updated[defIdx] = def.copy(pos = newPos)
                                                        candidates[index] = item.copy(definitions = updated)
                                                    }
                                                },
                                                options = posOptions,
                                                modifier = Modifier.weight(0.38f)
                                            )

                                            Spacer(modifier = Modifier.width(6.dp))

                                            OutlinedTextField(
                                                value = def.meaning,
                                                onValueChange = { newMeaning ->
                                                    val updated = item.definitions.toMutableList()
                                                    if (defIdx in updated.indices) {
                                                        updated[defIdx] = def.copy(meaning = newMeaning)
                                                        candidates[index] = item.copy(definitions = updated)
                                                    }
                                                },
                                                placeholder = {
                                                    Text(
                                                        if (learningMode.isClassical) "現代語訳" else "日本語の意味",
                                                        fontSize = 11.5.sp
                                                    )
                                                },
                                                singleLine = true,
                                                modifier = Modifier.weight(0.62f),
                                                shape = RoundedCornerShape(8.dp)
                                            )

                                            if (item.definitions.size > 1) {
                                                IconButton(
                                                    onClick = {
                                                        val updated = item.definitions.toMutableList()
                                                        if (defIdx in updated.indices) {
                                                            updated.removeAt(defIdx)
                                                            candidates[index] = item.copy(definitions = updated)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .align(Alignment.CenterVertically)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "この品詞・意味を削除",
                                                        tint = StatusCrossRed,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            val updated = item.definitions + Definition(pos = defaultPos, meaning = "")
                                            candidates[index] = item.copy(definitions = updated)
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.testTag("add_pos_row_${item.word}")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = StudyBluePrimary)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (learningMode.isClassical) "別の品詞・訳を追加" else "別の品詞・意味を追加",
                                            fontSize = 11.sp,
                                            color = StudyBluePrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Note Input (Optional)
                                    OutlinedTextField(
                                        value = item.note,
                                        onValueChange = { newNote ->
                                            candidates[index] = item.copy(note = newNote)
                                        },
                                        label = {
                                            Text(
                                                if (learningMode.isClassical) "備考・活用・用例（任意）" else "メモ・例文（任意）",
                                                fontSize = 11.sp
                                            )
                                        },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Confirmation Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("戻る", fontSize = 13.sp, maxLines = 1, softWrap = false)
                    }

                    val canSubmit = candidates.isNotEmpty() && candidates.all { it.word.isNotBlank() }

                    Button(
                        onClick = {
                            val entities = candidates.map { c ->
                                val cleanedDefs = c.definitions
                                    .map { d ->
                                        Definition(
                                            pos = d.pos.trim().ifBlank { defaultPos },
                                            meaning = d.meaning.trim()
                                        )
                                    }
                                    .filter { it.meaning.isNotBlank() }
                                    .ifEmpty {
                                        val fallbackPos = c.definitions.firstOrNull()?.pos?.trim()?.ifBlank { defaultPos } ?: defaultPos
                                        listOf(Definition(pos = fallbackPos, meaning = ""))
                                    }
                                FlashcardEntity(
                                    word = c.word.trim(),
                                    definitions = cleanedDefs,
                                    deck = c.deck.trim().ifBlank { targetDeckAll },
                                    note = c.note.trim(),
                                    subject = learningMode.id
                                )
                            }
                            onConfirmAdd(entities)
                        },
                        enabled = canSubmit,
                        modifier = Modifier
                            .weight(2f)
                            .height(44.dp)
                            .testTag("ocr_final_confirm_add_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCircleGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "この内容で ${candidates.size} 件を追加",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
