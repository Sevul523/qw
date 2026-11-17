package com.example.ui

enum class WordSortOrder(val title: String) {
    CREATED_ASC("登録順 (古い順)"),
    CREATED_DESC("登録順 (新しい順)"),
    ALPHA_ASC("アルファベット順 (A-Z)"),
    ALPHA_DESC("アルファベット順 (Z-A)"),
    POS_ASC("品詞順 (昇順)"),
    POS_DESC("品詞順 (降順)"),
    DECK_ASC("デッキ順 (昇順)"),
    DECK_DESC("デッキ順 (降順)"),
    SRS_LEVEL_ASC("忘却曲線レベル順 (昇順)"),
    SRS_LEVEL_DESC("忘却曲線レベル順 (降順)")
}
