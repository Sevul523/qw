package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusCrossRed

@Composable
fun DeleteAllConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusCrossRed)
        },
        title = {
            Text("全単語データの削除", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Text(
                "登録されているすべての単語カードを削除します。この操作は取り消せません。本当によろしいですか？",
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = StatusCrossRed),
                modifier = Modifier.testTag("confirm_delete_all_button")
            ) {
                Text("全削除する")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_all_button")
            ) {
                Text("キャンセル")
            }
        }
    )
}
