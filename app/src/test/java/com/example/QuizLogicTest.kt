package com.example

import com.example.data.local.FlashcardEntity
import com.example.data.model.Definition
import com.example.ui.quiz.QuizType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizLogicTest {

    @Test
    fun quizType_containsThreeOptions() {
        assertEquals(3, QuizType.entries.size)
        assertEquals(QuizType.MULTIPLE_CHOICE, QuizType.entries[0])
        assertEquals(QuizType.MULTIPLE_CHOICE_EN, QuizType.entries[1])
        assertEquals(QuizType.SPELLING, QuizType.entries[2])
    }

    @Test
    fun multipleChoiceEn_decoySelection_samePosFromAllDecks() {
        val targetCard = FlashcardEntity(
            id = 1,
            word = "run",
            definitions = listOf(Definition(pos = "動詞", meaning = "走る")),
            deck = "Deck A"
        )

        val otherCards = listOf(
            FlashcardEntity(
                id = 2,
                word = "walk",
                definitions = listOf(Definition(pos = "動詞", meaning = "歩く")),
                deck = "Deck B"
            ),
            FlashcardEntity(
                id = 3,
                word = "apple",
                definitions = listOf(Definition(pos = "名詞", meaning = "りんご")),
                deck = "Deck A"
            ),
            FlashcardEntity(
                id = 4,
                word = "swim",
                definitions = listOf(Definition(pos = "動詞", meaning = "泳ぐ")),
                deck = "Deck C"
            ),
            FlashcardEntity(
                id = 5,
                word = "jump",
                definitions = listOf(Definition(pos = "動詞", meaning = "跳ぶ")),
                deck = "Deck B"
            )
        )

        val targetPos = "動詞"
        val allCandidates = otherCards.filter { it.id != targetCard.id }

        val samePosEnglishDecoys = allCandidates
            .filter { c -> c.definitions.any { it.pos.trim() == targetPos } }
            .map { it.word.trim() }
            .filter { it.isNotBlank() && !it.equals(targetCard.word.trim(), ignoreCase = true) }
            .distinct()

        // Decoys should be walk, swim, jump (all verbs across all decks B and C)
        assertEquals(3, samePosEnglishDecoys.size)
        assertTrue(samePosEnglishDecoys.contains("walk"))
        assertTrue(samePosEnglishDecoys.contains("swim"))
        assertTrue(samePosEnglishDecoys.contains("jump"))
        assertFalse(samePosEnglishDecoys.contains("apple"))
        assertFalse(samePosEnglishDecoys.contains("run"))
    }

    @Test
    fun multipleChoiceEn_promptAndAnswer_areReversedCorrectly() {
        val card = FlashcardEntity(
            id = 1,
            word = "achieve",
            definitions = listOf(Definition(pos = "動詞", meaning = "達成する")),
            deck = "TOEIC"
        )

        val prompt = card.definitions.joinToString(" / ") { it.meaning }
        val answer = card.word

        assertEquals("達成する", prompt)
        assertEquals("achieve", answer)
    }
}
