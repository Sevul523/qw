package com.example.ui.quiz

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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.FlashcardEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.StatusCircleGreen
import com.example.ui.theme.StatusCrossRed
import com.example.ui.theme.StatusTriangleAmber
import com.example.ui.theme.StudyBluePrimary
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun QuizScreen(
    viewModel: MainViewModel,
    onSpeak: (String) -> Unit,
    snackbarHostState: SnackbarHostState? = null
) {
    val quizState by viewModel.quizState.collectAsStateWithLifecycle()
    val allDecks by viewModel.allDecks.collectAsStateWithLifecycle()
    val allCards by viewModel.allCards.collectAsStateWithLifecycle()
    val learningMode by viewModel.learningMode.collectAsStateWithLifecycle()
    val ttsEnabled by viewModel.ttsEnabled.collectAsStateWithLifecycle()
    val isClassical = learningMode.isClassical
    val showTts = ttsEnabled && !isClassical

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            quizState.isFinished -> {
                QuizResultView(
                    state = quizState,
                    onRetryIncorrect = { viewModel.retryIncorrectQuizQuestions() },
                    onRestart = { viewModel.resetQuiz() },
                    onApplyToSrs = {
                        viewModel.applyQuizResultsToSrs()
                        snackbarHostState?.let { host ->
                            kotlinx.coroutines.GlobalScope.launch {
                                host.showSnackbar("テスト結果を忘却曲線(SRS)の復習状況に反映しました！")
                            }
                        }
                    },
                    onSpeak = onSpeak,
                    showTts = showTts
                )
            }
            quizState.isRunning -> {
                QuizRunningView(
                    state = quizState,
                    onSelectAnswer = { viewModel.selectQuizAnswer(it) },
                    onTypeSpelling = { viewModel.setTypedSpelling(it) },
                    onRevealHint = { viewModel.revealSpellingHint() },
                    onSubmitSpelling = { viewModel.submitSpellingAnswer() },
                    onNext = { viewModel.nextQuizQuestion() },
                    onQuit = { viewModel.resetQuiz() },
                    onSpeak = onSpeak,
                    showTts = showTts
                )
            }
            else -> {
                QuizSetupView(
                    allDecks = allDecks,
                    allCards = allCards,
                    isClassical = isClassical,
                    onStartQuiz = { decks, pos, count, options, showQPos, showOptPos, type ->
                        viewModel.startQuiz(
                            selectedDecks = decks,
                            selectedPos = pos,
                            questionCount = count,
                            optionsCount = options,
                            showPosInQuestion = showQPos,
                            showPosInOptions = showOptPos,
                            type = if (isClassical) QuizType.MULTIPLE_CHOICE else type
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun QuizSetupView(
    allDecks: List<String>,
    allCards: List<FlashcardEntity>,
    isClassical: Boolean = false,
    onStartQuiz: (
        selectedDecks: Set<String>,
        selectedPos: Set<String>,
        questionCount: Int,
        optionsCount: Int,
        showPosInQuestion: Boolean,
        showPosInOptions: Boolean,
        type: QuizType
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(QuizType.MULTIPLE_CHOICE) }
    var selectedDecks by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedPos by remember { mutableStateOf<Set<String>>(emptySet()) }
    var optionsCount by remember { mutableIntStateOf(4) }
    var questionCount by remember { mutableIntStateOf(10) }
    var showPosInQuestion by remember { mutableStateOf(true) }
    var showPosInOptions by remember { mutableStateOf(true) }

    LaunchedEffect(isClassical) {
        if (isClassical) {
            selectedType = QuizType.MULTIPLE_CHOICE
        }
        selectedDecks = emptySet()
        selectedPos = emptySet()
    }

    val stepOffset = if (isClassical) 0 else 1

    // Collect all available parts of speech from the registered cards
    val availablePosList = remember(allCards, isClassical) {
        val standardPos = if (isClassical) {
            com.example.data.model.PartsOfSpeech.CLASSICAL
        } else {
            listOf("名詞", "動詞", "形容詞", "副詞", "熟語", "前置詞", "接続詞", "代名詞", "間投詞", "その他")
        }
        val fromCards = allCards.flatMap { it.definitions }
            .map { it.pos.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        (fromCards + standardPos).distinct()
    }

    // Filter available pool to show user immediate count feedback
    val filteredCardsCount = remember(allCards, selectedDecks, selectedPos) {
        allCards.count { card ->
            !card.isHidden &&
            (selectedDecks.isEmpty() || selectedDecks.contains(card.deck)) &&
            (selectedPos.isEmpty() || card.definitions.any { d -> selectedPos.contains(d.pos.trim()) })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudyBluePrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Quiz,
                    contentDescription = null,
                    tint = StudyBluePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isClassical) "古語テスト & クイズ" else "英単語テスト & クイズ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isClassical) "古語 → 現代語訳（出題条件を設定）" else "出題形式・問題数・デッキ・品詞をカスタマイズ",
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Quiz Mode Selection Card (English mode only; Classical mode uses 古語→現代語訳 multiple choice only)
        if (!isClassical) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "1. テスト形式の選択",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuizTypeOptionCard(
                            title = "${optionsCount}択 (英→日)",
                            description = "英語→日本語",
                            icon = Icons.Default.Quiz,
                            isSelected = selectedType == QuizType.MULTIPLE_CHOICE,
                            onClick = { selectedType = QuizType.MULTIPLE_CHOICE },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )

                        QuizTypeOptionCard(
                            title = "${optionsCount}択 (日→英)",
                            description = "日本語→英語",
                            icon = Icons.Default.Translate,
                            isSelected = selectedType == QuizType.MULTIPLE_CHOICE_EN,
                            onClick = { selectedType = QuizType.MULTIPLE_CHOICE_EN },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )

                        QuizTypeOptionCard(
                            title = "スペル入力",
                            description = "日本語→綴り",
                            icon = Icons.Default.Spellcheck,
                            isSelected = selectedType == QuizType.SPELLING,
                            onClick = { selectedType = QuizType.SPELLING },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }

        // Options Count Card (Shown for Multiple Choice modes)
        if (isClassical || selectedType == QuizType.MULTIPLE_CHOICE || selectedType == QuizType.MULTIPLE_CHOICE_EN) {
            if (!isClassical) {
                Spacer(modifier = Modifier.height(10.dp))
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "${1 + stepOffset}. 選択肢の個数（2〜10個）",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "選択肢数を自由に設定できます",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Stepper (- / +)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = { if (optionsCount > 2) optionsCount-- },
                                enabled = optionsCount > 2,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "選択肢を減らす", modifier = Modifier.size(18.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StudyBluePrimary.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${optionsCount}択",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = StudyBluePrimary
                                )
                            }

                            IconButton(
                                onClick = { if (optionsCount < 10) optionsCount++ },
                                enabled = optionsCount < 10,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "選択肢を増やす", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = optionsCount.toFloat(),
                        onValueChange = { optionsCount = it.roundToInt() },
                        valueRange = 2f..10f,
                        steps = 7,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(2, 3, 4, 5, 6, 8, 10).forEach { count ->
                            FilterChip(
                                selected = optionsCount == count,
                                onClick = { optionsCount = count },
                                label = { Text("${count}択", fontSize = 11.5.sp, maxLines = 1, softWrap = false) },
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
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Question Count Card (1 ~ 50 in 1-increment steps)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "${2 + stepOffset}. 出題問題数（1〜50問）",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "1問刻み設定（対象: ${filteredCardsCount}件）",
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Stepper (- / +)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = { if (questionCount > 1) questionCount-- },
                            enabled = questionCount > 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "問題数を減らす", modifier = Modifier.size(18.dp))
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudyBluePrimary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${questionCount}問",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = StudyBluePrimary
                            )
                        }

                        IconButton(
                            onClick = { if (questionCount < 50) questionCount++ },
                            enabled = questionCount < 50,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "問題数を増やす", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Slider(
                    value = questionCount.toFloat(),
                    onValueChange = { questionCount = it.roundToInt() },
                    valueRange = 1f..50f,
                    steps = 48,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 3, 5, 10, 15, 20, 30, 50).forEach { count ->
                        FilterChip(
                            selected = questionCount == count,
                            onClick = { questionCount = count },
                            label = { Text("${count}問", fontSize = 11.5.sp, maxLines = 1, softWrap = false) },
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

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Deck Selection Card (Multiple selection supported!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${3 + stepOffset}. 出題デッキ（複数可）",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Text(
                        text = if (selectedDecks.isEmpty()) "全デッキ" else "${selectedDecks.size}個選択",
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = StudyBluePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedDecks.isEmpty(),
                        onClick = { selectedDecks = emptySet() },
                        label = { Text("すべてのデッキ", fontSize = 11.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.height(30.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudyBluePrimary,
                            selectedLabelColor = Color.White
                        )
                    )

                    allDecks.forEach { deck ->
                        val isSelected = selectedDecks.contains(deck)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDecks = if (isSelected) {
                                    selectedDecks - deck
                                } else {
                                    selectedDecks + deck
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

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Part of Speech Selection Card (Multiple selection supported!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${4 + stepOffset}. 出題品詞（複数可）",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Text(
                        text = if (selectedPos.isEmpty()) "全品詞" else "${selectedPos.size}個選択",
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = StudyBluePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedPos.isEmpty(),
                        onClick = { selectedPos = emptySet() },
                        label = { Text("すべての品詞", fontSize = 11.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.height(30.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudyBluePrimary,
                            selectedLabelColor = Color.White
                        )
                    )

                    availablePosList.forEach { pos ->
                        val isSelected = selectedPos.contains(pos)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPos = if (isSelected) {
                                    selectedPos - pos
                                } else {
                                    selectedPos + pos
                                }
                            },
                            label = { Text(pos, fontSize = 11.5.sp, maxLines = 1, softWrap = false) },
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

        Spacer(modifier = Modifier.height(10.dp))

        // 6. POS Visibility Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "${5 + stepOffset}. 品詞の表示・非表示",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Toggle 1: Question POS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showPosInQuestion = !showPosInQuestion }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "問題文に品詞を表示",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (showPosInQuestion) {
                                "出題時に [動詞] などのタグを表示"
                            } else {
                                "出題時の品詞タグを隠す（難易度UP）"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showPosInQuestion,
                        onCheckedChange = { showPosInQuestion = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StudyBluePrimary)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Toggle 2: Options POS
                if (isClassical || selectedType == QuizType.MULTIPLE_CHOICE) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showPosInOptions = !showPosInOptions }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "選択肢に品詞を表示",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (showPosInOptions) {
                                    if (isClassical) "例: [動詞] 目を覚ます" else "例: [動詞] 走る"
                                } else {
                                    if (isClassical) "例: 目を覚ます（品詞なしですっきり表示）" else "例: 走る（品詞なしですっきり表示）"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showPosInOptions,
                            onCheckedChange = { showPosInOptions = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StudyBluePrimary)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Start Quiz Button
        Button(
            onClick = {
                onStartQuiz(
                    selectedDecks,
                    selectedPos,
                    questionCount,
                    optionsCount,
                    showPosInQuestion,
                    showPosInOptions,
                    selectedType
                )
            },
            enabled = filteredCardsCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_quiz_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (filteredCardsCount > 0) "テストを開始する（${minOf(questionCount, filteredCardsCount)}問）" else "条件に合う単語がありません",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (filteredCardsCount == 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "※ 選択したデッキ・品詞に該当する単語がありません。選択条件を緩めてください。",
                fontSize = 12.sp,
                color = StatusCrossRed
            )
        }
    }
}

@Composable
private fun QuizTypeOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) StudyBluePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) StudyBluePrimary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 10.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QuizRunningView(
    state: QuizState,
    onSelectAnswer: (Int) -> Unit,
    onTypeSpelling: (String) -> Unit,
    onRevealHint: () -> Unit,
    onSubmitSpelling: () -> Unit,
    onNext: () -> Unit,
    onQuit: () -> Unit,
    onSpeak: (String) -> Unit,
    showTts: Boolean = true
) {
    val question = state.currentQuestion ?: return

    LaunchedEffect(question.card.word, showTts) {
        if (showTts && state.quizType == QuizType.MULTIPLE_CHOICE) {
            onSpeak(question.card.word)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Status Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onQuit) {
                Icon(Icons.Default.Close, contentDescription = "テスト中断")
            }

            Text(
                text = "第 ${state.currentIndex + 1} / ${state.questions.size} 問",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusCircleGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${state.score}点",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = StatusCircleGreen
                )
            }
        }

        // Progress Bar
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = StudyBluePrimary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Question Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Deck Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudyBluePrimary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "📁 ${question.card.deck}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StudyBluePrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (state.quizType) {
                    QuizType.MULTIPLE_CHOICE -> {
                        // English Word Prompt -> Japanese Options
                        Text(
                            text = question.card.word,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (showTts) {
                            Spacer(modifier = Modifier.height(8.dp))

                            IconButton(
                                onClick = { onSpeak(question.card.word) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(StudyBluePrimary.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.VolumeUp,
                                    contentDescription = "発音",
                                    tint = StudyBluePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        if (question.showPosInQuestion && question.partOfSpeech.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "[${question.partOfSpeech}]",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    QuizType.MULTIPLE_CHOICE_EN -> {
                        // Japanese Meaning Prompt -> English Options
                        Text(
                            text = question.prompt,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        if (question.showPosInQuestion && question.partOfSpeech.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "[${question.partOfSpeech}]",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    QuizType.SPELLING -> {
                        // Spelling Test: Japanese meaning prompt
                        Text(
                            text = question.correctAnswer,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        if (question.showPosInQuestion && question.partOfSpeech.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "[${question.partOfSpeech}]",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Masked spelling view
                        val targetWord = question.card.word
                        val maskedText = if (state.hintRevealed) {
                            "${targetWord.take(2)} " + "・ ".repeat((targetWord.length - 2).coerceAtLeast(1))
                        } else {
                            "・ ".repeat(targetWord.length)
                        }

                        Text(
                            text = maskedText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp,
                            color = StudyBluePrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Answer Interface
        if (state.quizType == QuizType.MULTIPLE_CHOICE || state.quizType == QuizType.MULTIPLE_CHOICE_EN) {
            // Options (Japanese or English)
            question.options.forEachIndexed { index, option ->
                val isSelected = state.selectedAnswerIndex == index
                val isCorrectAnswer = option == question.correctAnswer

                val containerColor = when {
                    !state.isSubmitted -> MaterialTheme.colorScheme.surface
                    isCorrectAnswer -> StatusCircleGreen.copy(alpha = 0.18f)
                    isSelected && !state.isAnswerCorrect -> StatusCrossRed.copy(alpha = 0.18f)
                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                }

                val borderColor = when {
                    !state.isSubmitted -> MaterialTheme.colorScheme.outlineVariant
                    isCorrectAnswer -> StatusCircleGreen
                    isSelected && !state.isAnswerCorrect -> StatusCrossRed
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                }

                Surface(
                    onClick = {
                        if (!state.isSubmitted) onSelectAnswer(index)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .border(
                            width = if (isSelected || (state.isSubmitted && isCorrectAnswer)) 2.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(14.dp)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    color = containerColor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        state.isSubmitted && isCorrectAnswer -> StatusCircleGreen
                                        state.isSubmitted && isSelected && !state.isAnswerCorrect -> StatusCrossRed
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isSubmitted) {
                                if (isCorrectAnswer) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                } else if (isSelected) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                } else {
                                    Text("${index + 1}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Text("${index + 1}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = option,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected || (state.isSubmitted && isCorrectAnswer)) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        // If English options mode, show TTS button for options
                        if (showTts && state.quizType == QuizType.MULTIPLE_CHOICE_EN) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { onSpeak(option) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.VolumeUp,
                                    contentDescription = "発音",
                                    tint = StudyBluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Spelling Text Input
            OutlinedTextField(
                value = state.typedSpellingText,
                onValueChange = { onTypeSpelling(it) },
                label = { Text("英単語のスペルを入力") },
                singleLine = true,
                enabled = !state.isSubmitted,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmitSpelling() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spelling_input_field"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!state.hintRevealed && !state.isSubmitted) {
                    OutlinedButton(
                        onClick = onRevealHint,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ヒント", fontSize = 13.sp)
                    }
                }

                if (!state.isSubmitted) {
                    Button(
                        onClick = onSubmitSpelling,
                        enabled = state.typedSpellingText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("回答する", fontSize = 13.sp)
                    }
                }
            }
        }

        // Post-Submit Feedback Banner & Next Button
        AnimatedVisibility(
            visible = state.isSubmitted,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.isAnswerCorrect) StatusCircleGreen.copy(alpha = 0.15f)
                        else StatusCrossRed.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (state.isAnswerCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (state.isAnswerCorrect) StatusCircleGreen else StatusCrossRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.isAnswerCorrect) "正解です！ 🎉" else "不正解",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (state.isAnswerCorrect) StatusCircleGreen else StatusCrossRed
                            )
                            if (!state.isAnswerCorrect) {
                                val detailText = if (state.quizType == QuizType.MULTIPLE_CHOICE_EN) {
                                    "正解: ${question.correctAnswer} (${question.prompt})"
                                } else {
                                    "正解: ${question.card.word} (${question.correctAnswer})"
                                }
                                Text(
                                    text = detailText,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("quiz_next_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary)
                ) {
                    Text(
                        text = if (state.currentIndex + 1 < state.questions.size) "次の問題へ ➔" else "結果を見る 🏆",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizResultView(
    state: QuizState,
    onRetryIncorrect: () -> Unit,
    onRestart: () -> Unit,
    onApplyToSrs: () -> Unit,
    onSpeak: (String) -> Unit,
    showTts: Boolean = true
) {
    var srsApplied by remember { mutableStateOf(false) }
    val incorrectCount = state.results.count { !it.isCorrect }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Result Trophy Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.accuracyPercentage >= 80) StatusCircleGreen.copy(alpha = 0.15f)
                            else StatusTriangleAmber.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = if (state.accuracyPercentage >= 80) StatusCircleGreen else StatusTriangleAmber,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "テスト結果",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${state.score} / ${state.results.size} 問 正解",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = StudyBluePrimary
                )

                Text(
                    text = "正答率 ${state.accuracyPercentage}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when {
                        state.accuracyPercentage == 100 -> "満点おめでとうございます！完璧です！🌟"
                        state.accuracyPercentage >= 80 -> "素晴らしい成績です！着実に定着しています！👍"
                        state.accuracyPercentage >= 50 -> "あと少し！間違えた単語を復習しましょう！💪"
                        else -> "忘却曲線に沿って繰り返し学習しましょう！継続が力になります！🌱"
                    },
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SRS Sync Button
        Button(
            onClick = {
                onApplyToSrs()
                srsApplied = true
            },
            enabled = !srsApplied,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (srsApplied) StatusCircleGreen else StudyBluePrimary
            )
        ) {
            Icon(
                if (srsApplied) Icons.Default.Check else Icons.Default.Sync,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (srsApplied) "忘却曲線(SRS)の復習状況に反映済み ✓" else "テスト結果を忘却曲線(SRS)に反映する",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (incorrectCount > 0) {
                OutlinedButton(
                    onClick = onRetryIncorrect,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("誤答のみ再テスト", fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }

            OutlinedButton(
                onClick = onRestart,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("テスト終了", fontSize = 12.sp, maxLines = 1, softWrap = false)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Breakdown List
        Text(
            text = "解答一覧（復習リスト）",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        state.results.forEach { res ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (res.isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (res.isCorrect) StatusCircleGreen else StatusCrossRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = res.question.card.word,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (showTts) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onSpeak(res.question.card.word) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = StudyBluePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        val definitionText = if (state.quizType == QuizType.MULTIPLE_CHOICE_EN) {
                            res.question.prompt
                        } else {
                            res.question.correctAnswer
                        }
                        Text(
                            text = definitionText,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
