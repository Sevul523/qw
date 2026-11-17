package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ユーザーによる内蔵辞書の個別カスタマイズ（編集・新規追加・削除）を永続化するRoomエンティティ。
 * プリセットの内蔵辞書データ（TSVおよびClassicalExtendedVocabulary）に対してオーバーレイ合成されます。
 */
@Entity(tableName = "custom_dictionary_overrides")
data class CustomDictionaryOverride(
    @PrimaryKey
    val wordKey: String, // 検索用の正規化キー（小文字・トリム済み）
    val word: String, // 画面表示用の単語・表記（大文字小文字や仮名を保持）
    val isClassical: Boolean = false,
    val isDeleted: Boolean = false, // trueの場合は内蔵辞書から削除（非表示化）
    val isUserCreated: Boolean = false, // trueの場合はユーザーが完全新規に追加した単語
    val pos: String = "名詞",
    val meanings: String = "",
    val deck: String = "日常英会話",
    val note: String = "",
    val conjugation: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
