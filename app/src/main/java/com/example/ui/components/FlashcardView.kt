package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FlashcardEntity
import com.example.ui.settings.CardBackTheme
import com.example.ui.theme.PosBadgeColor
import com.example.ui.theme.PosBadgeContainer
import com.example.ui.theme.StatusCircleGreen
import com.example.ui.theme.StatusCircleGreenLight
import com.example.ui.theme.StatusCrossRed
import com.example.ui.theme.StatusCrossRedLight
import com.example.ui.theme.StatusNoneGray
import com.example.ui.theme.StatusNoneGrayLight
import com.example.ui.theme.StatusTriangleAmber
import com.example.ui.theme.StatusTriangleAmberLight
import com.example.ui.theme.StudyBluePrimary
import com.example.ui.theme.StudyBlueSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlashcardView(
    card: FlashcardEntity?,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSpeak: (String) -> Unit,
    ttsEnabled: Boolean = true,
    cardBackTheme: CardBackTheme = CardBackTheme.DEFAULT,
    modifier: Modifier = Modifier
) {
    // Keep displayed back face card frozen during flip animations to prevent
    // premature reveals of the next word's meaning before the card returns to the front face.
    var displayedBackCard by remember { mutableStateOf(card) }

    // 3D rotation animation
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        finishedListener = { finalValue ->
            if (finalValue == 0f) {
                // Animation to front face has completed; safely sync back face content to the new card
                displayedBackCard = card
            }
        },
        label = "CardFlipAnimation"
    )

    // Synchronize displayedBackCard when idle on front face or when card is cleared
    LaunchedEffect(card, isFlipped, rotation == 0f) {
        if (card == null) {
            displayedBackCard = null
        } else if (!isFlipped && rotation == 0f) {
            displayedBackCard = card
        }
    }

    // Track horizontal swipe distance
    var totalDrag by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .pointerInput(card?.id) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                    onDragEnd = {
                        if (totalDrag < -60f) {
                            onNext()
                        } else if (totalDrag > 60f) {
                            onPrev()
                        }
                    }
                )
            }
            .clickable(onClick = onFlip)
            .testTag("flashcard_container"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        if (rotation <= 90f) {
            // Front Face (Normal orientation)
            CardFrontFace(
                card = card,
                onSpeak = onSpeak,
                ttsEnabled = ttsEnabled
            )
        } else {
            // Back Face (Mirrored 180 to be readable)
            CardBackFace(
                card = displayedBackCard,
                cardBackTheme = cardBackTheme,
                modifier = Modifier.graphicsLayer { rotationY = 180f }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardFrontFace(
    card: FlashcardEntity?,
    onSpeak: (String) -> Unit,
    ttsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .border(
                width = 2.5.dp,
                color = StudyBluePrimary.copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top row: Audio TTS + Deck + SRS + Status Badge
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
                if (card != null && card.word.isNotBlank() && ttsEnabled) {
                    IconButton(
                        onClick = { onSpeak(card.word) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = StudyBluePrimary.copy(alpha = 0.12f),
                                shape = CircleShape
                            )
                            .testTag("pronounce_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "発音を聞く",
                            tint = StudyBluePrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                if (card != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "📁 ${card.deck}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (card != null) {
                    val srsLabel = when (card.repetitionLevel) {
                        0 -> "🌱Lv.0"
                        in 1..2 -> "🌿Lv.${card.repetitionLevel}"
                        in 3..4 -> "🌳Lv.${card.repetitionLevel}"
                        else -> "🌟Lv.${card.repetitionLevel}"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(StudyBluePrimary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = srsLabel,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudyBluePrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                StatusBadge(
                    status = card?.status,
                    consecutiveCount = card?.consecutivePerfectCount ?: 0
                )
            }
        }

        // Center: Word & Metadata in Box with weight(1f)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (card == null) {
                    Text(
                        text = "条件に合う単語がありません",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "フィルターを調整するか単語を追加してください",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = card.word,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp,
                        modifier = Modifier.testTag("card_word_front")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // POS chips
                    val allPos = remember(card.definitions) {
                        card.definitions.mapNotNull { it.pos.takeIf { p -> p.isNotBlank() } }
                            .flatMap { it.split(Regex("[・、,]+")) }
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .distinct()
                    }

                    if (allPos.isNotEmpty() || card.note.isNotBlank()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allPos.forEach { posTag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PosBadgeContainer)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = posTag,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PosBadgeColor,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            if (card.note.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = card.note.trim().replace(Regex("[\\r\\n]+"), " "),
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        // Bottom hint
        Text(
            text = "タップで裏返す / 左右にスワイプ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun CardBackFace(
    card: FlashcardEntity?,
    cardBackTheme: CardBackTheme = CardBackTheme.DEFAULT,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        cardBackTheme.gradientStart,
                        cardBackTheme.gradientEnd
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            if (card == null || card.definitions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "意味が登録されていません",
                        color = cardBackTheme.textColor.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = card.word,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = cardBackTheme.textColor.copy(alpha = 0.95f),
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                card.definitions.forEachIndexed { index, def ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        if (def.pos.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cardBackTheme.badgeBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = def.pos,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = cardBackTheme.badgeTextColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = def.meaning.trim().replace(Regex("[\\r\\n]+"), " "),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = cardBackTheme.textColor,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (card.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (cardBackTheme.isLightBack) Color.Black.copy(alpha = 0.08f)
                                else Color.White.copy(alpha = 0.18f)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "備考: ${card.note.trim().replace(Regex("[\\r\\n]+"), " ")}",
                            fontSize = 12.sp,
                            color = cardBackTheme.textColor.copy(alpha = 0.9f)
                        )
                    }
                }

                // ① 忘却曲線・復習スケジュール
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (cardBackTheme.isLightBack) Color.Black.copy(alpha = 0.04f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val nextLabel = when {
                        card.intervalDays == 0 -> "今日復習"
                        card.intervalDays == 1 -> "明日復習 (1日後)"
                        else -> "${card.intervalDays}日後に復習"
                    }
                    Text(
                        text = "忘却曲線レベル: Lv.${card.repetitionLevel}",
                        fontSize = 11.sp,
                        color = cardBackTheme.textColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = nextLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = cardBackTheme.textColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String?,
    consecutiveCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val (label, bgColor, textColor) = when (status) {
        "○" -> {
            val countText = if (consecutiveCount > 1) " (${consecutiveCount}連続)" else ""
            Triple("○ 完璧$countText", StatusCircleGreenLight, StatusCircleGreen)
        }
        "△" -> Triple("△ 微妙", StatusTriangleAmberLight, StatusTriangleAmber)
        "×" -> Triple("× 忘れた", StatusCrossRedLight, StatusCrossRed)
        else -> Triple("未評価", StatusNoneGrayLight, StatusNoneGray)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            softWrap = false
        )
    }
}
