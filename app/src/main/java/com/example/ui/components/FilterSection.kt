package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PartsOfSpeech
import com.example.ui.theme.StatusCircleGreen
import com.example.ui.theme.StatusCrossRed
import com.example.ui.theme.StatusNoneGray
import com.example.ui.theme.StatusTriangleAmber
import com.example.ui.theme.StudyBluePrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSection(
    filterState: FilterState,
    onToggleExpanded: () -> Unit,
    onToggleStatus: (String) -> Unit,
    onTogglePos: (String) -> Unit,
    onToggleMultiPosOnly: (Boolean) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier,
    posOptions: List<String> = PartsOfSpeech.ALL,
    testTag: String = "filter_section"
) {
    val activeFilterCount = filterState.selectedStatuses.size +
        filterState.selectedPos.size +
        if (filterState.multiPosOnly) 1 else 0

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "フィルター",
                    tint = StudyBluePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "絞り込みフィルター",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (activeFilterCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(StudyBluePrimary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$activeFilterCount",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (activeFilterCount > 0) {
                    TextButton(
                        onClick = onResetFilters,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("解除", fontSize = 12.sp, color = StatusCrossRed)
                    }
                }

                Icon(
                    imageVector = if (filterState.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (filterState.isExpanded) "閉じる" else "開く",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = filterState.isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "ステータス",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statuses = listOf(
                            Triple("○", "○ 完璧", StatusCircleGreen),
                            Triple("△", "△ 微妙", StatusTriangleAmber),
                            Triple("×", "× 忘れた", StatusCrossRed),
                            Triple("未", "未評価", StatusNoneGray)
                        )
                        statuses.forEach { (key, label, color) ->
                            val selected = filterState.selectedStatuses.contains(key)
                            Surface(
                                onClick = { onToggleStatus(key) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) color.copy(alpha = 0.18f) else Color.Transparent,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "品詞",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = filterState.multiPosOnly,
                            onClick = { onToggleMultiPosOnly(!filterState.multiPosOnly) },
                            label = { Text("複数品詞のみ", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
                            modifier = Modifier.height(30.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudyBluePrimary.copy(alpha = 0.15f),
                                selectedLabelColor = StudyBluePrimary
                            )
                        )

                        posOptions.forEach { pos ->
                            val selected = filterState.selectedPos.contains(pos)
                            FilterChip(
                                selected = selected,
                                onClick = { onTogglePos(pos) },
                                label = { Text(pos, fontSize = 11.sp, maxLines = 1, softWrap = false) },
                                modifier = Modifier.height(30.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudyBluePrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = StudyBluePrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
