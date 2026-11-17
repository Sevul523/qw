package com.example

import com.example.data.local.FlashcardEntity
import com.example.data.model.Definition
import com.example.data.model.PartsOfSpeech
import com.example.ui.WordSortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class WordSortOrderTest {

    private val sampleCards = listOf(
        FlashcardEntity(
            id = 1,
            word = "apple",
            definitions = listOf(Definition(pos = "名詞", meaning = "りんご")),
            deck = "日常英会話",
            repetitionLevel = 2,
            createdAt = 1000L
        ),
        FlashcardEntity(
            id = 2,
            word = "run",
            definitions = listOf(Definition(pos = "動詞", meaning = "走る")),
            deck = "日常英会話",
            repetitionLevel = 0,
            createdAt = 2000L
        ),
        FlashcardEntity(
            id = 3,
            word = "beautiful",
            definitions = listOf(Definition(pos = "形容詞", meaning = "美しい")),
            deck = "TOEIC",
            repetitionLevel = 5,
            createdAt = 1500L
        ),
        FlashcardEntity(
            id = 4,
            word = "negotiate",
            definitions = listOf(Definition(pos = "動詞", meaning = "交渉する")),
            deck = "ビジネス",
            repetitionLevel = 1,
            createdAt = 500L
        ),
        FlashcardEntity(
            id = 5,
            word = "book",
            definitions = listOf(Definition(pos = "名詞", meaning = "本")),
            deck = "ビジネス",
            repetitionLevel = 0,
            createdAt = 3000L
        )
    )

    @Test
    fun testWordSortOrderEntries() {
        assertEquals(10, WordSortOrder.entries.size)
        assertEquals("登録順 (古い順)", WordSortOrder.CREATED_ASC.title)
        assertEquals("登録順 (新しい順)", WordSortOrder.CREATED_DESC.title)
        assertEquals("アルファベット順 (A-Z)", WordSortOrder.ALPHA_ASC.title)
        assertEquals("アルファベット順 (Z-A)", WordSortOrder.ALPHA_DESC.title)
        assertEquals("品詞順 (昇順)", WordSortOrder.POS_ASC.title)
        assertEquals("品詞順 (降順)", WordSortOrder.POS_DESC.title)
        assertEquals("デッキ順 (昇順)", WordSortOrder.DECK_ASC.title)
        assertEquals("デッキ順 (降順)", WordSortOrder.DECK_DESC.title)
        assertEquals("忘却曲線レベル順 (昇順)", WordSortOrder.SRS_LEVEL_ASC.title)
        assertEquals("忘却曲線レベル順 (降順)", WordSortOrder.SRS_LEVEL_DESC.title)
    }

    @Test
    fun testSortByPosAsc() {
        val posOrder = PartsOfSpeech.ALL.withIndex().associate { it.value to it.index }
        val sorted = sampleCards.sortedWith(
            compareBy<FlashcardEntity> { card ->
                val pos = card.definitions.firstOrNull { it.pos.isNotBlank() }?.pos?.trim() ?: ""
                posOrder[pos] ?: (PartsOfSpeech.ALL.size + 1)
            }.thenBy { it.word.lowercase() }
        )

        // PartsOfSpeech.ALL starts with 動詞, then 名詞, then 形容詞
        // 動詞: negotiate, run
        // 名詞: apple, book
        // 形容詞: beautiful
        val words = sorted.map { it.word }
        assertEquals(listOf("negotiate", "run", "apple", "book", "beautiful"), words)
    }

    @Test
    fun testSortByPosDesc() {
        val posOrder = PartsOfSpeech.ALL.withIndex().associate { it.value to it.index }
        val sorted = sampleCards.sortedWith(
            compareByDescending<FlashcardEntity> { card ->
                val pos = card.definitions.firstOrNull { it.pos.isNotBlank() }?.pos?.trim() ?: ""
                posOrder[pos] ?: -1
            }.thenBy { it.word.lowercase() }
        )

        val words = sorted.map { it.word }
        assertEquals(listOf("beautiful", "apple", "book", "negotiate", "run"), words)
    }

    @Test
    fun testSortByDeckAsc() {
        val sorted = sampleCards.sortedWith(
            compareBy<FlashcardEntity> { it.deck.lowercase() }
                .thenBy { it.word.lowercase() }
        )

        // Decks: TOEIC, ビジネス, 日常英会話
        val words = sorted.map { it.word }
        assertEquals(listOf("beautiful", "book", "negotiate", "apple", "run"), words)
    }

    @Test
    fun testSortByDeckDesc() {
        val sorted = sampleCards.sortedWith(
            compareByDescending<FlashcardEntity> { it.deck.lowercase() }
                .thenBy { it.word.lowercase() }
        )

        val words = sorted.map { it.word }
        assertEquals(listOf("apple", "run", "book", "negotiate", "beautiful"), words)
    }

    @Test
    fun testSortBySrsLevelAsc() {
        val sorted = sampleCards.sortedWith(
            compareBy<FlashcardEntity> { it.repetitionLevel }
                .thenBy { it.nextReviewTimestamp }
                .thenBy { it.word.lowercase() }
        )

        // Repetition levels:
        // 0: book, run
        // 1: negotiate
        // 2: apple
        // 5: beautiful
        val words = sorted.map { it.word }
        assertEquals(listOf("book", "run", "negotiate", "apple", "beautiful"), words)
    }

    @Test
    fun testSortBySrsLevelDesc() {
        val sorted = sampleCards.sortedWith(
            compareByDescending<FlashcardEntity> { it.repetitionLevel }
                .thenBy { it.nextReviewTimestamp }
                .thenBy { it.word.lowercase() }
        )

        val words = sorted.map { it.word }
        assertEquals(listOf("beautiful", "apple", "negotiate", "book", "run"), words)
    }
}
