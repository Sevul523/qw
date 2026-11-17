package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.FlashcardEntity
import com.example.data.model.ClassicalPosNormalizer
import com.example.data.model.Definition
import com.example.data.model.PartsOfSpeech
import com.example.data.service.WeblioDictionaryService
import com.example.ui.theme.StatusCrossRed
import com.example.ui.theme.StudyBluePrimary
import kotlinx.coroutines.launch

@Composable
fun EditWordDialog(
    card: FlashcardEntity,
    existingDecks: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (FlashcardEntity) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isClassical = card.subject.equals("CLASSICAL", ignoreCase = true)
    var word by remember { mutableStateOf(card.word) }
    var note by remember { mutableStateOf(card.note) }
    var deck by remember { mutableStateOf(card.deck) }
    var isFetchingWeblio by remember { mutableStateOf(false) }

    val definitions = remember {
        mutableStateListOf<Definition>().apply {
            if (card.definitions.isEmpty()) {
                add(Definition(pos = "", meaning = ""))
            } else {
                addAll(card.definitions)
            }
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun handleSave() {
        val extractedConjs = mutableListOf<String>()
        val validDefs = definitions.map { def ->
            val cleanedMeaning = def.meaning.trim().replace(Regex("[\\r\\n]+"), " ")
            if (isClassical) {
                val (simplePos, conj) = ClassicalPosNormalizer.splitPosAndConjugation(def.pos.trim())
                if (conj.isNotBlank()) extractedConjs.add(conj)
                def.copy(meaning = cleanedMeaning, pos = simplePos)
            } else {
                def.copy(meaning = cleanedMeaning, pos = def.pos.trim())
            }
        }.filter { it.meaning.isNotBlank() }

        if (word.isBlank() || validDefs.isEmpty()) {
            errorMessage = "単語と意味を入力してください！"
            return
        }

        val rawNote = note.trim().replace(Regex("[\\r\\n]+"), " ")
        val finalNote = if (isClassical) {
            ClassicalPosNormalizer.mergeConjugationIntoNote(extractedConjs, rawNote)
        } else {
            rawNote
        }

        onSave(
            card.copy(
                word = word.trim(),
                definitions = validDefs,
                deck = deck.trim().ifBlank { if (isClassical) "共通テスト最重要動詞" else "日常英会話" },
                note = finalNote
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .heightIn(max = 540.dp)
            .padding(vertical = 12.dp),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✏️ 単語の編集",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_edit_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "閉じる",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = StatusCrossRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Word Input
                OutlinedTextField(
                    value = word,
                    onValueChange = {
                        word = it
                        errorMessage = null
                    },
                    label = { Text("単語") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_word_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Multiple Meanings & POS Header + Weblio Fetch Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "意味・品詞",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (word.isNotBlank()) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    isFetchingWeblio = true
                                    try {
                                        val internal = com.example.data.service.InternalDictionaryService.lookupWord(word.trim(), isClassical)
                                        val result = if (internal != null && internal.definitions.isNotEmpty()) {
                                            internal.toWeblioLookupResult()
                                        } else if (isClassical) {
                                            WeblioDictionaryService.lookupClassicalWord(word.trim())
                                        } else {
                                            WeblioDictionaryService.lookupWord(word.trim())
                                        }
                                        if (result != null && result.definitions.isNotEmpty()) {
                                            definitions.clear()
                                            definitions.addAll(result.definitions)
                                            errorMessage = null
                                            if (isClassical && result.conjugation.isNotBlank()) {
                                                note = ClassicalPosNormalizer.mergeConjugationIntoNote(
                                                    result.conjugation.split("・"),
                                                    note
                                                )
                                            }
                                        } else {
                                            errorMessage = "辞書で該当する語が見つかりませんでした"
                                        }
                                    } finally {
                                        isFetchingWeblio = false
                                    }
                                }
                            },
                            enabled = !isFetchingWeblio,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.testTag("edit_weblio_fetch_button")
                        ) {
                            if (isFetchingWeblio) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = StudyBluePrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("取得中...", fontSize = 11.sp, color = StudyBluePrimary)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = StudyBluePrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isClassical) "Weblio古語辞典から取得" else "Weblioから取得",
                                    fontSize = 11.sp,
                                    color = StudyBluePrimary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                definitions.forEachIndexed { index, def ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        PosDropdownSelector(
                            selectedPos = def.pos,
                            onPosSelected = { newPos: String ->
                                definitions[index] = def.copy(pos = newPos)
                            },
                            options = if (isClassical) PartsOfSpeech.CLASSICAL else PartsOfSpeech.ENGLISH,
                            modifier = Modifier.weight(0.42f)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        OutlinedTextField(
                            value = def.meaning,
                            onValueChange = { newMeaning: String ->
                                definitions[index] = def.copy(meaning = newMeaning)
                                errorMessage = null
                            },
                            placeholder = { Text(if (isClassical) "現代語訳" else "意味") },
                            modifier = Modifier
                                .weight(0.58f)
                                .testTag("edit_meaning_input_$index"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (definitions.size > 1) {
                            IconButton(
                                onClick = { definitions.removeAt(index) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .align(Alignment.CenterVertically)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "この意味を削除",
                                    tint = StatusCrossRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = { definitions.add(Definition(pos = "", meaning = "")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isClassical) "別の品詞・訳を追加" else "別の品詞・意味を追加", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = deck,
                    onValueChange = { deck = it },
                    label = { Text("デッキ (分類)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                if (existingDecks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        existingDecks.forEach { d ->
                            SuggestionChip(
                                onClick = { deck = d },
                                label = { Text(d, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isClassical) "備考・活用 (任意)" else "備考 (任意)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_note_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { handleSave() },
                colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_edit_button")
            ) {
                Text("保存する", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_edit_button")
            ) {
                Text("キャンセル")
            }
        }
    )
}
