package com.example.data.model

/**
 * 学習科目モード (英単語 ⇄ 古語)
 */
enum class LearningMode(
    val id: String,
    val title: String,
    val shortLabel: String,
    val description: String,
    val wordLabel: String,
    val meaningLabel: String,
    val defaultDeck: String
) {
    ENGLISH(
        id = "ENGLISH",
        title = "英単語",
        shortLabel = "英語",
        description = "日常会話・TOEIC・大学受験の英単語帳",
        wordLabel = "英単語",
        meaningLabel = "日本語訳・意味",
        defaultDeck = "日常英会話"
    ),
    CLASSICAL_JAPANESE(
        id = "CLASSICAL",
        title = "古語",
        shortLabel = "古語",
        description = "大学受験・共通テスト頻出重要古語の暗記帳",
        wordLabel = "古語",
        meaningLabel = "現代語訳・多義",
        defaultDeck = "共通テスト最重要動詞"
    );

    val isEnglish: Boolean get() = this == ENGLISH
    val isClassical: Boolean get() = this == CLASSICAL_JAPANESE
    val displayName: String get() = title
    val icon: String get() = if (this == ENGLISH) "🔤" else "📜"
}
