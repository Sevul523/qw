package com.example.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.FlashcardEntity
import com.example.ui.MainViewModel
import com.example.ui.WordSortOrder
import com.example.ui.components.DeleteDeckConfirmDialog
import com.example.ui.components.EditWordDialog
import com.example.ui.components.ExportDialog
import com.example.ui.components.ImportDialog
import com.example.ui.components.FilterSection
import com.example.ui.components.RenameDeckDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.PosBadgeColor
import com.example.ui.theme.PosBadgeContainer
import com.example.ui.theme.StatusCrossRed
import com.example.ui.theme.StudyBluePrimary

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun WordListScreen(
    viewModel: MainViewModel,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cards by viewModel.listCards.collectAsStateWithLifecycle()
    val allCards by viewModel.allCards.collectAsStateWithLifecycle()
    val allDecks by viewModel.allDecks.collectAsStateWithLifecycle()
    val selectedDeck by viewModel.selectedListDeck.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val showHiddenInList by viewModel.showHiddenInList.collectAsStateWithLifecycle()
    val listFilterState by viewModel.listFilterState.collectAsStateWithLifecycle()
    val learningMode by viewModel.learningMode.collectAsStateWithLifecycle()
    val ttsEnabled by viewModel.ttsEnabled.collectAsStateWithLifecycle()
    val showSpeakButton = ttsEnabled && !learningMode.isClassical
    val editingCard by viewModel.editingCard.collectAsStateWithLifecycle()

    // 複数選択モードの状態管理
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // ダイアログ状態
    var cardToDelete by remember { mutableStateOf<FlashcardEntity?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchMoveDeckDialog by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var targetExportDeck by remember { mutableStateOf<String?>(null) }
    var targetImportDeck by remember { mutableStateOf<String?>(null) }
    var deckToRename by remember { mutableStateOf<String?>(null) }
    var deckToDelete by remember { mutableStateOf<String?>(null) }
    var importSuccessMsg by remember { mutableStateOf<String?>(null) }

    // 選択中のアイテムIDリスト（現在のcards内に存在する選択中アイテム）
    val currentSelectedCount = selectedIds.size
    val allCurrentIds = remember(cards) { cards.map { it.id }.toSet() }
    val isAllSelected = cards.isNotEmpty() && cards.all { it.id in selectedIds }

    // 選択モードを終了するヘルパー
    fun exitSelectionMode() {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    // アイテム選択トグル
    fun toggleSelectItem(id: Long) {
        selectedIds = if (id in selectedIds) {
            selectedIds - id
        } else {
            selectedIds + id
        }
        if (selectedIds.isEmpty()) {
            isSelectionMode = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 複数選択モードのアクションバー または 通常の検索＆アクションバー
        if (isSelectionMode) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("selection_action_bar"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // 上段: 選択件数・全選択・閉じる
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { exitSelectionMode() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("exit_selection_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "選択解除",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${currentSelectedCount}件 選択中",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        TextButton(
                            onClick = {
                                selectedIds = if (isAllSelected) {
                                    emptySet()
                                } else {
                                    allCurrentIds
                                }
                            },
                            modifier = Modifier.testTag("select_all_button")
                        ) {
                            Icon(
                                imageVector = if (isAllSelected) Icons.Default.CheckBoxOutlineBlank else Icons.Default.SelectAll,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isAllSelected) "全解除" else "すべて選択", maxLines = 1, softWrap = false)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 下段: 操作ボタン（非表示・表示・削除・デッキ移動）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 一括非表示（学習・一覧から非表示）
                        Button(
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    viewModel.batchSetHidden(selectedIds.toList(), true)
                                    importSuccessMsg = "${selectedIds.size}件の単語を非表示にしました"
                                    exitSelectionMode()
                                }
                            },
                            enabled = selectedIds.isNotEmpty(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("batch_hide_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("非表示にする", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }

                        // 一括再表示
                        Button(
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    viewModel.batchSetHidden(selectedIds.toList(), false)
                                    importSuccessMsg = "${selectedIds.size}件の単語を表示に戻しました"
                                    exitSelectionMode()
                                }
                            },
                            enabled = selectedIds.isNotEmpty(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudyBluePrimary.copy(alpha = 0.15f),
                                contentColor = StudyBluePrimary
                            ),
                            modifier = Modifier.testTag("batch_unhide_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("表示に戻す", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }

                        // 一括デッキ移動
                        Button(
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    showBatchMoveDeckDialog = true
                                }
                            },
                            enabled = selectedIds.isNotEmpty(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudyBluePrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("batch_move_deck_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DriveFileMove,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("デッキ移動", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }

                        // 一括削除
                        Button(
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    showBatchDeleteConfirm = true
                                }
                            },
                            enabled = selectedIds.isNotEmpty(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusCrossRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("batch_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("消去 (${selectedIds.size})", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        } else {
            // 通常時: Search Bar & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = "単語・意味・品詞で検索",
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 13.sp,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "検索", modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "クリア", modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    singleLine = true,
                    maxLines = 1,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("word_search_field")
                )

                Spacer(modifier = Modifier.width(6.dp))

                // 複数選択モード開始ボタン
                OutlinedIconButton(
                    onClick = {
                        isSelectionMode = true
                        selectedIds = emptySet()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("start_selection_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckBox,
                        contentDescription = "複数選択",
                        modifier = Modifier.size(20.dp),
                        tint = StudyBluePrimary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedIconButton(
                    onClick = { showImportDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("list_import_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "インポート",
                        modifier = Modifier.size(20.dp),
                        tint = StudyBluePrimary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box {
                    OutlinedIconButton(
                        onClick = { sortMenuExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "並び替え",
                            modifier = Modifier.size(20.dp),
                            tint = StudyBluePrimary
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.widthIn(min = 220.dp)
                    ) {
                        WordSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(18.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (order == sortOrder) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = StudyBluePrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = order.title,
                                            fontWeight = if (order == sortOrder) FontWeight.Bold else FontWeight.Normal,
                                            color = if (order == sortOrder) StudyBluePrimary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOrder(order)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Deck Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedDeck == null,
                onClick = { viewModel.selectListDeck(null) },
                label = { Text("すべてのデッキ", fontSize = 11.5.sp, maxLines = 1, softWrap = false) },
                modifier = Modifier.height(32.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StudyBluePrimary,
                    selectedLabelColor = Color.White
                )
            )

            allDecks.forEach { deck ->
                FilterChip(
                    selected = selectedDeck == deck,
                    onClick = { viewModel.selectListDeck(deck) },
                    label = { Text(deck, fontSize = 11.5.sp, maxLines = 1, softWrap = false) },
                    modifier = Modifier.height(32.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudyBluePrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (selectedDeck != null) {
            Spacer(modifier = Modifier.height(4.dp))
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
                        Text(
                            text = "「$selectedDeck」 (${cards.size}語)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                targetImportDeck = selectedDeck
                                showImportDialog = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription = "インポート",
                                tint = StudyBluePrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                targetExportDeck = selectedDeck
                                showExportDialog = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = "エクスポート",
                                tint = StudyBluePrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        IconButton(
                            onClick = { deckToRename = selectedDeck },
                            modifier = Modifier.size(32.dp).testTag("edit_selected_deck_button")
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "名前変更",
                                tint = StudyBluePrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        IconButton(
                            onClick = { deckToDelete = selectedDeck },
                            modifier = Modifier.size(32.dp).testTag("delete_selected_deck_button")
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

        Spacer(modifier = Modifier.height(6.dp))

        // 品詞 & ステータス 絞り込みフィルター (アコーディオン)
        FilterSection(
            filterState = listFilterState,
            onToggleExpanded = { viewModel.toggleListFilterExpanded() },
            onToggleStatus = { viewModel.toggleListStatusFilter(it) },
            onTogglePos = { viewModel.toggleListPosFilter(it) },
            onToggleMultiPosOnly = { viewModel.toggleListMultiPosOnly(it) },
            onResetFilters = { viewModel.resetListFilters() },
            posOptions = if (learningMode.isClassical) com.example.data.model.PartsOfSpeech.CLASSICAL else com.example.data.model.PartsOfSpeech.ENGLISH,
            testTag = "list_filter_section"
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Count label, 非表示カードトグル, 現在の並び順表示 (改行防止) & 選択ボタン
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${cards.size}件",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )

                // 非表示カードの表示/非表示トグルチップ
                FilterChip(
                    selected = showHiddenInList,
                    onClick = { viewModel.toggleShowHiddenInList() },
                    label = {
                        Text(
                            text = if (showHiddenInList) "非表示含む" else "非表示除外",
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (showHiddenInList) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("toggle_hidden_in_list_chip")
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(start = 6.dp)
            ) {
                if (!isSelectionMode && cards.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            isSelectionMode = true
                            selectedIds = emptySet()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("enter_selection_mode_button")
                    ) {
                        Text("選択", fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                    }
                }

                // 現在の順番表示（無駄な改行が生じないよう maxLines=1, softWrap=false, タップでメニュー展開）
                Surface(
                    onClick = { sortMenuExpanded = true },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .testTag("current_sort_indicator")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "並び替えメニュー",
                            modifier = Modifier.size(14.dp),
                            tint = StudyBluePrimary
                        )
                        Text(
                            text = sortOrder.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = StudyBluePrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) {
                        "一致する単語が見つかりませんでした"
                    } else if (!showHiddenInList) {
                        "表示可能な単語がありません（非表示単語があります）"
                    } else {
                        "登録されている単語がありません"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cards, key = { it.id }) { card ->
                    val isSelected = card.id in selectedIds
                    WordItemCard(
                        card = card,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onSelectToggle = { toggleSelectItem(card.id) },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedIds = setOf(card.id)
                            } else {
                                toggleSelectItem(card.id)
                            }
                        },
                        onSpeak = { onSpeak(card.word) },
                        showSpeakButton = showSpeakButton,
                        onEdit = { viewModel.openEditModal(card) },
                        onToggleHidden = { viewModel.toggleCardHidden(card) },
                        onDelete = { cardToDelete = card }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // 複数単語一括消去確認ダイアログ
    if (showBatchDeleteConfirm) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("単語の一括消去", fontWeight = FontWeight.Bold) },
            text = { Text("選択した ${count} 件の単語を完全に消去してもよろしいですか？（この操作は取り消せません）") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.batchDeleteCards(selectedIds.toList())
                        importSuccessMsg = "${count} 件の単語を消去しました"
                        showBatchDeleteConfirm = false
                        exitSelectionMode()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCrossRed),
                    modifier = Modifier.testTag("batch_delete_confirm_button")
                ) {
                    Text("消去する")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 複数単語一括デッキ移動ダイアログ
    if (showBatchMoveDeckDialog) {
        var selectedTargetDeck by remember { mutableStateOf(allDecks.firstOrNull() ?: "日常英会話") }
        AlertDialog(
            onDismissRequest = { showBatchMoveDeckDialog = false },
            title = { Text("デッキの一括移動", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("選択した ${selectedIds.size} 件の単語の移動先デッキを選択してください:")
                    Spacer(modifier = Modifier.height(12.dp))
                    allDecks.forEach { deck ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTargetDeck = deck }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (selectedTargetDeck == deck) Icons.Default.Check else Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (selectedTargetDeck == deck) StudyBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = deck,
                                fontWeight = if (selectedTargetDeck == deck) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTargetDeck == deck) StudyBluePrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBatchDeck(selectedIds.toList(), selectedTargetDeck)
                        importSuccessMsg = "${selectedIds.size} 件の単語を「${selectedTargetDeck}」へ移動しました"
                        showBatchMoveDeckDialog = false
                        exitSelectionMode()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary),
                    modifier = Modifier.testTag("batch_move_deck_confirm_button")
                ) {
                    Text("移動する")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchMoveDeckDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // Edit Modal
    if (editingCard != null) {
        EditWordDialog(
            card = editingCard!!,
            existingDecks = allDecks,
            onDismiss = { viewModel.closeEditModal() },
            onSave = { updated -> viewModel.saveEditedCard(updated) }
        )
    }

    // Delete Single Card Dialog
    if (cardToDelete != null) {
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            title = { Text("単語の削除", fontWeight = FontWeight.Bold) },
            text = { Text("「${cardToDelete!!.word}」を削除してもよろしいですか？") },
            confirmButton = {
                Button(
                    onClick = {
                        cardToDelete?.let { viewModel.deleteCard(it) }
                        cardToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCrossRed)
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { cardToDelete = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    if (showImportDialog) {
        ImportDialog(
            viewModel = viewModel,
            initialDeck = targetImportDeck,
            onDismiss = {
                showImportDialog = false
                targetImportDeck = null
            },
            onImportSuccess = { msg ->
                importSuccessMsg = msg
            }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            viewModel = viewModel,
            initialDeck = targetExportDeck,
            onDismiss = {
                showExportDialog = false
                targetExportDeck = null
            },
            onMessage = { msg ->
                importSuccessMsg = msg
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

    if (importSuccessMsg != null) {
        AlertDialog(
            onDismissRequest = { importSuccessMsg = null },
            title = { Text("通知", fontWeight = FontWeight.Bold) },
            text = { Text(importSuccessMsg!!) },
            confirmButton = {
                Button(
                    onClick = { importSuccessMsg = null },
                    colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary)
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun WordItemCard(
    card: FlashcardEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectToggle: () -> Unit,
    onLongClick: () -> Unit,
    onSpeak: () -> Unit,
    showSpeakButton: Boolean = true,
    onEdit: () -> Unit,
    onToggleHidden: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("word_card_${card.id}")
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onSelectToggle()
                    }
                },
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                card.isHidden -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            // Row 1: Checkbox + Word + Top-right Action Icons (eliminates dedicated empty bottom row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelectToggle() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = StudyBluePrimary
                        ),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("card_checkbox_${card.id}")
                    )
                }

                Text(
                    text = card.word,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (card.isHidden) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showSpeakButton) {
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "発音",
                                tint = StudyBluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onToggleHidden,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (card.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (card.isHidden) "表示に戻す" else "学習から非表示",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "編集",
                            tint = StudyBluePrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "削除",
                            tint = StatusCrossRed,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Row 2: FlowRow for StatusBadge + Deck + SRS + Hidden (never squeezes badges)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusBadge(
                    status = card.status,
                    consecutiveCount = card.consecutivePerfectCount
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "📁 ${card.deck}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val srsLabel = when (card.repetitionLevel) {
                    0 -> "🌱Lv.0"
                    in 1..2 -> "🌿Lv.${card.repetitionLevel}"
                    in 3..4 -> "🌳Lv.${card.repetitionLevel}"
                    else -> "🌟Lv.${card.repetitionLevel}"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StudyBluePrimary.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = srsLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudyBluePrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (card.isHidden) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "非表示中",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            card.definitions.forEach { def ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (def.pos.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(PosBadgeContainer)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = def.pos,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PosBadgeColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = def.meaning.trim().replace(Regex("[\\r\\n]+"), " "),
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (card.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "備考: ${card.note.trim().replace(Regex("[\\r\\n]+"), " ")}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
