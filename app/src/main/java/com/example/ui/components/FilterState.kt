package com.example.ui.components

data class FilterState(
    val selectedStatuses: Set<String> = emptySet(), // "○", "△", "×", "未"
    val selectedPos: Set<String> = emptySet(),
    val multiPosOnly: Boolean = false,
    val isExpanded: Boolean = false
)
