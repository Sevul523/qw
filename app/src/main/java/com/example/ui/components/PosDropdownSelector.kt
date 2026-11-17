package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PartsOfSpeech

@Composable
fun PosDropdownSelector(
    selectedPos: String,
    onPosSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<String> = PartsOfSpeech.ALL
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                )
                .background(MaterialTheme.colorScheme.surface)
                .clickable { expanded = true }
                .padding(horizontal = 10.dp)
                .testTag("pos_dropdown_selector"),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = if (selectedPos.isBlank()) "-- 品詞 --" else selectedPos,
                fontSize = 13.sp,
                fontWeight = if (selectedPos.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
                color = if (selectedPos.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("-- 品詞なし --") },
                onClick = {
                    onPosSelected("")
                    expanded = false
                }
            )
            options.forEach { pos ->
                DropdownMenuItem(
                    text = { Text(pos) },
                    onClick = {
                        onPosSelected(pos)
                        expanded = false
                    }
                )
            }
        }
    }
}
