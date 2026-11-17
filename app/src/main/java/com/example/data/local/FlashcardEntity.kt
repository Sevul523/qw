package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Definition

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,
    val definitions: List<Definition> = emptyList(),
    val note: String = "",
    val status: String? = null, // "○", "△", "×", or null/"未"
    val isHidden: Boolean = false,
    val consecutivePerfectCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),

    // ⑧ デッキ（フォルダ・分類）
    val deck: String = "日常英会話",

    // ① 忘却曲線（SRS: 間隔反復学習）
    val repetitionLevel: Int = 0, // 0: 未習得, 1: 1日後, 2: 3日後, 3: 7日後, 4: 14日後, 5: 30日後, 6+: 習得済み
    val intervalDays: Int = 0,
    val nextReviewTimestamp: Long = 0L, // 次回復習予定日時(ミリ秒), 0=いつでも復習可能
    val lastReviewedAt: Long = 0L,

    // ⑤ 語源・コロケーション・類義語・対義語
    val etymology: String = "",
    val collocations: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),

    // 科目分類: "ENGLISH" (英単語) または "CLASSICAL" (古語)
    val subject: String = "ENGLISH"
)
