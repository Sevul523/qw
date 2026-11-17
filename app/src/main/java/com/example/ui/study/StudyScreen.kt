package com.example.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import com.example.ui.components.DeleteDeckConfirmDialog
import com.example.ui.components.RenameDeckDialog
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.FilterSection
import com.example.ui.components.FlashcardView
import com.example.ui.theme.StatusCircleGreen
import com.example.ui.theme.StatusCrossRed
import com.example.ui.theme.StatusTriangleAmber
import com.example.ui.theme.StudyBluePrimary
import com.example.ui.theme.StudyTextMuted

@Composable
fun StudyScreen(
    viewModel: MainViewModel,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allCards by viewModel.allCards.collectAsStateWithLifecycle()
    val studyCards by viewModel.studyCards.collectAsStateWithLifecycle()
    val allDecks by viewModel.allDecks.collectAsStateWithLifecycle()
    val selectedDecks by viewModel.selectedStudyDecks.collectAsStateWithLifecycle()
    val srsDueOnly by viewModel.srsDueOnly.collectAsStateWithLifecycle()
    val srsDueCount by viewModel.srsDueCount.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentIndex.collectAsStateWithLifecycle()
    val isFlipped by viewModel.isFlipped.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val learningMode by viewModel.learningMode.collectAsStateWithLifecycle()
    val ttsEnabled by viewModel.ttsEnabled.collectAsStateWithLifecycle()
    val effectiveTtsEnabled = ttsEnabled && !learningMode.isClassical
    val autoSpeak by viewModel.autoSpeakOnCardChange.collectAsStateWithLifecycle()
    val cardBackTheme by viewModel.cardBackTheme.collectAsStateWithLifecycle()
    val uncheckMsg by viewModel.lastAutoUncheckMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var deckToRename by remember { mutableStateOf<String?>(null) }
    var deckToDelete by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(uncheckMsg) {
        uncheckMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearAutoUncheckMessage()
        }
    }

    val currentCard = if (studyCards.isNotEmpty()) {
        val safeIndex = currentIndex.coerceIn(0, studyCards.lastIndex)
        studyCards[safeIndex]
    } else {
        null
    }

    // Auto-speak word on card change if enabled (English mode only)
    androidx.compose.runtime.LaunchedEffect(currentCard?.id, autoSpeak, effectiveTtsEnabled) {
        if (autoSpeak && effectiveTtsEnabled && currentCard != null && currentCard.word.isNotBlank()) {
            onSpeak(currentCard.word)
        }
    }

    // Stats calculations
    val circleCount = remember(studyCards) { studyCards.count { it.status == "○" } }
    val triangleCount = remember(studyCards) { studyCards.count { it.status == "△" } }
    val crossCount = remember(studyCards) { studyCards.count { it.status == "×" } }

    Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Deck & SRS Bar (Feature ⑧ & ①)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedDecks.isEmpty(),
                    onClick = { viewModel.clearStudyDecks() },
                    label = { Text("すべてのデッキ", fontSize = 11.5.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.height(32.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudyBluePrimary,
                        selectedLabelColor = Color.White
                    )
                )

                allDecks.forEach { deck ->
                    val isSelected = selectedDecks.contains(deck)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.toggleStudyDeck(deck) },
                        label = { Text(deck, fontSize = 11.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.height(32.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudyBluePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                FilterChip(
                    selected = srsDueOnly,
                    onClick = { viewModel.toggleSrsDueOnly() },
                    label = { Text("⚡ 復習が必要 ($srsDueCount)", fontSize = 11.5.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.height(32.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StatusTriangleAmber,
                        selectedLabelColor = Color.White
                    )
                )
            }

            if (selectedDecks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = StudyBluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val deckSummary = if (selectedDecks.size == 1) {
                                "「${selectedDecks.first()}」 (${studyCards.size}語)"
                            } else {
                                "選択中: ${selectedDecks.size}デッキ (${selectedDecks.joinToString(", ")}) (${studyCards.size}語)"
                            }
                            Text(
                                text = deckSummary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (selectedDecks.size == 1) {
                            val singleDeck = selectedDecks.first()
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { deckToRename = singleDeck },
                                    modifier = Modifier.size(32.dp).testTag("study_rename_deck_button")
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "名前変更",
                                        tint = StudyBluePrimary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { deckToDelete = singleDeck },
                                    modifier = Modifier.size(32.dp).testTag("study_delete_deck_button")
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "デッキを削除",
                                        tint = StatusCrossRed,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Filter section (collapsible)
        FilterSection(
            filterState = filterState,
            onToggleExpanded = { viewModel.toggleFilterExpanded() },
            onToggleStatus = { viewModel.toggleStatusFilter(it) },
            onTogglePos = { viewModel.togglePosFilter(it) },
            onToggleMultiPosOnly = { viewModel.toggleMultiPosOnly(it) },
            onResetFilters = { viewModel.resetFilters() },
            posOptions = if (learningMode.isClassical) com.example.data.model.PartsOfSpeech.CLASSICAL else com.example.data.model.PartsOfSpeech.ENGLISH
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Stats Area
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "登録数: ${allCards.size}件 (学習対象: ${studyCards.size}件)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatPill(symbol = "○", count = circleCount, color = StatusCircleGreen)
                    StatPill(symbol = "△", count = triangleCount, color = StatusTriangleAmber)
                    StatPill(symbol = "×", count = crossCount, color = StatusCrossRed)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress & Shuffle Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card Index Tracker
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (studyCards.isNotEmpty()) {
                        "${(currentIndex.coerceIn(0, studyCards.lastIndex)) + 1} / ${studyCards.size}"
                    } else {
                        "0 / 0"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isShuffle) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StudyBluePrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ランダム",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudyBluePrimary
                        )
                    }
                }
            }

            // Shuffle Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isShuffle && studyCards.size > 1) {
                    IconButton(
                        onClick = { viewModel.reshuffle() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("reshuffle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "順番を再シャッフル",
                            tint = StudyBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                FilterChip(
                    selected = isShuffle,
                    onClick = { viewModel.toggleShuffle() },
                    label = {
                        Text(
                            text = if (isShuffle) "シャッフル ON" else "シャッフル OFF",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudyBluePrimary.copy(alpha = 0.15f),
                        selectedLabelColor = StudyBluePrimary,
                        selectedLeadingIconColor = StudyBluePrimary
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("shuffle_toggle_button")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive 3D Flashcard
        FlashcardView(
            card = currentCard,
            isFlipped = isFlipped,
            onFlip = { viewModel.flipCard() },
            onNext = { viewModel.nextCard() },
            onPrev = { viewModel.prevCard() },
            onSpeak = onSpeak,
            ttsEnabled = effectiveTtsEnabled,
            cardBackTheme = cardBackTheme
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Evaluation Controls: ○ 完璧 / △ 微妙 / × 忘れた (matches top order)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.evaluateCard("○") },
                enabled = currentCard != null,
                colors = ButtonDefaults.buttonColors(containerColor = StatusCircleGreen),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("eval_circle_button")
            ) {
                Text(
                    text = "○ 完璧",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Button(
                onClick = { viewModel.evaluateCard("△") },
                enabled = currentCard != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusTriangleAmber,
                    contentColor = Color(0xFF333333)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("eval_triangle_button")
            ) {
                Text(
                    text = "△ 微妙",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Button(
                onClick = { viewModel.evaluateCard("×") },
                enabled = currentCard != null,
                colors = ButtonDefaults.buttonColors(containerColor = StatusCrossRed),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("eval_cross_button")
            ) {
                Text(
                    text = "× 忘れた",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Navigation Controls: Prev / Next
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.prevCard() },
                enabled = studyCards.size > 1,
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("prev_card_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("前へ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, softWrap = false)
            }

            Button(
                onClick = { viewModel.nextCard() },
                enabled = studyCards.size > 1,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34495E)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("next_card_button")
            ) {
                Text("スキップ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Delete Current Card button
        if (currentCard != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showDeleteConfirmDialog = true }
                    .padding(vertical = 8.dp, horizontal = 12.dp)
                    .testTag("delete_current_card_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = StudyTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "現在表示中の単語を削除",
                        fontSize = 12.sp,
                        color = StudyTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    // Delete single card confirm dialog
    if (showDeleteConfirmDialog && currentCard != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("単語の削除", fontWeight = FontWeight.Bold) },
            text = { Text("「${currentCard.word}」を完全に削除しますか？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCurrentCard()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCrossRed)
                ) {
                    Text("削除する")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
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
            }
        )
    }
}

@Composable
private fun StatPill(
    symbol: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(color = color, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                color = if (symbol == "△") Color(0xFF333333) else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                        includeFontPadding = false
                    ),
                    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                        alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                        trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both
                    )
                )
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$count",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = color,
            style = androidx.compose.ui.text.TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                    includeFontPadding = false
                )
            )
        )
    }
}
