package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.RadioButton
import com.example.ui.theme.StatusCrossRed
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StudyBluePrimary

@Composable
fun CreateDeckDialog(
    existingDecks: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var deckName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = StudyBluePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("新規デッキ作成", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "単語を分類するための新しいデッキ（カテゴリ）名を入力してください。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = deckName,
                    onValueChange = {
                        deckName = it
                        error = null
                    },
                    label = { Text("デッキ名 (例: 英検準1級、旅行英語)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_deck_name_input"),
                    shape = RoundedCornerShape(10.dp),
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = deckName.trim()
                    when {
                        trimmed.isBlank() -> {
                            error = "デッキ名を入力してください"
                        }
                        existingDecks.any { it.equals(trimmed, ignoreCase = true) } -> {
                            error = "そのデッキ名はすでに存在します"
                        }
                        else -> {
                            onConfirm(trimmed)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("create_deck_confirm_button")
            ) {
                Text("作成")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("create_deck_cancel_button")
            ) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
fun RenameDeckDialog(
    currentName: String,
    existingDecks: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DriveFileRenameOutline,
                    contentDescription = null,
                    tint = StudyBluePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("デッキ名の変更", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "「${currentName}」の新しい名称を入力してください。このデッキに属するすべての単語も自動的に更新されます。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        error = null
                    },
                    label = { Text("新しいデッキ名") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_deck_name_input"),
                    shape = RoundedCornerShape(10.dp),
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = newName.trim()
                    when {
                        trimmed.isBlank() -> {
                            error = "デッキ名を入力してください"
                        }
                        trimmed.equals(currentName, ignoreCase = true) -> {
                            onDismiss()
                        }
                        existingDecks.any { it.equals(trimmed, ignoreCase = true) } -> {
                            error = "そのデッキ名はすでに存在します"
                        }
                        else -> {
                            onConfirm(trimmed)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StudyBluePrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("rename_deck_confirm_button")
            ) {
                Text("変更")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("rename_deck_cancel_button")
            ) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
fun DeleteDeckConfirmDialog(
    deckName: String,
    cardCount: Int,
    fallbackDecks: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (fallbackDeck: String, deleteCards: Boolean) -> Unit
) {
    val remainingDecks = remember(fallbackDecks, deckName) {
        fallbackDecks.filter { it != deckName }.ifEmpty { listOf("日常英会話") }
    }
    var selectedFallback by remember { mutableStateOf(remainingDecks.firstOrNull() ?: "日常英会話") }
    var deleteCardsOption by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = StatusCrossRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("デッキ「$deckName」の削除", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (cardCount == 0) {
                    Text(
                        text = "このデッキ（0語）を削除してもよろしいですか？",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "このデッキには現在 ${cardCount} 件の単語が登録されています。削除方法を選択してください：",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 1: Move cards to another deck
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteCardsOption = false },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !deleteCardsOption,
                            onClick = { deleteCardsOption = false }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("単語を別のデッキに移動して残す（推奨）", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    if (!deleteCardsOption) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "移動先デッキを選択:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            remainingDecks.forEach { d ->
                                FilterChip(
                                    selected = selectedFallback == d,
                                    onClick = { selectedFallback = d },
                                    label = { Text(d, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 2: Delete cards too
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteCardsOption = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = deleteCardsOption,
                            onClick = { deleteCardsOption = true }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "このデッキの単語（${cardCount}語）もすべて削除する",
                            fontSize = 13.sp,
                            color = StatusCrossRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedFallback, deleteCardsOption)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusCrossRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("delete_deck_confirm_button")
            ) {
                Text("削除する")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("delete_deck_cancel_button")
            ) {
                Text("キャンセル")
            }
        }
    )
}
