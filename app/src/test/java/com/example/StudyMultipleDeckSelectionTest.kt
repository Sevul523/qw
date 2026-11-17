package com.example

import com.example.data.local.FlashcardEntity
import com.example.data.model.Definition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyMultipleDeckSelectionTest {

    private val sampleCards = listOf(
        FlashcardEntity(id = 1, word = "apple", definitions = listOf(Definition(pos = "名詞", meaning = "りんご")), deck = "日常英会話"),
        FlashcardEntity(id = 2, word = "negotiate", definitions = listOf(Definition(pos = "動詞", meaning = "交渉する")), deck = "ビジネス"),
        FlashcardEntity(id = 3, word = "strategy", definitions = listOf(Definition(pos = "名詞", meaning = "戦略")), deck = "TOEIC"),
        FlashcardEntity(id = 4, word = "dog", definitions = listOf(Definition(pos = "名詞", meaning = "犬")), deck = "日常英会話"),
        FlashcardEntity(id = 5, word = "hidden_word", definitions = listOf(Definition(pos = "名詞", meaning = "隠し単語")), deck = "日常英会話", isHidden = true)
    )

    @Test
    fun emptyDeckSelection_includesAllNonHiddenCards() {
        val selectedDecks = emptySet<String>()
        val filtered = sampleCards.filter { card ->
            if (card.isHidden) return@filter false
            if (selectedDecks.isNotEmpty() && card.deck !in selectedDecks) return@filter false
            true
        }

        assertEquals(4, filtered.size)
        assertTrue(filtered.any { it.word == "apple" })
        assertTrue(filtered.any { it.word == "negotiate" })
        assertTrue(filtered.any { it.word == "strategy" })
        assertTrue(filtered.any { it.word == "dog" })
        assertFalse(filtered.any { it.word == "hidden_word" })
    }

    @Test
    fun singleDeckSelection_includesOnlyMatchingCards() {
        val selectedDecks = setOf("ビジネス")
        val filtered = sampleCards.filter { card ->
            if (card.isHidden) return@filter false
            if (selectedDecks.isNotEmpty() && card.deck !in selectedDecks) return@filter false
            true
        }

        assertEquals(1, filtered.size)
        assertEquals("negotiate", filtered.first().word)
    }

    @Test
    fun multipleDeckSelection_includesCardsFromAllSelectedDecks() {
        val selectedDecks = setOf("日常英会話", "TOEIC")
        val filtered = sampleCards.filter { card ->
            if (card.isHidden) return@filter false
            if (selectedDecks.isNotEmpty() && card.deck !in selectedDecks) return@filter false
            true
        }

        assertEquals(3, filtered.size)
        val words = filtered.map { it.word }.toSet()
        assertEquals(setOf("apple", "dog", "strategy"), words)
    }

    @Test
    fun deckToggleLogic() {
        var selected = emptySet<String>()

        // Select Daily
        selected = if ("日常英会話" in selected) selected - "日常英会話" else selected + "日常英会話"
        assertEquals(setOf("日常英会話"), selected)

        // Select Business as well (multiple selection)
        selected = if ("ビジネス" in selected) selected - "ビジネス" else selected + "ビジネス"
        assertEquals(setOf("日常英会話", "ビジネス"), selected)

        // Deselect Daily
        selected = if ("日常英会話" in selected) selected - "日常英会話" else selected + "日常英会話"
        assertEquals(setOf("ビジネス"), selected)

        // Clear all (back to all decks)
        selected = emptySet()
        assertTrue(selected.isEmpty())
    }
}
