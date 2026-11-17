package com.example

import com.example.data.local.FlashcardEntity
import com.example.data.model.Definition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateWordUnitTest {

    @Test
    fun findDuplicate_isCaseInsensitive() {
        val existingCards = listOf(
            FlashcardEntity(
                id = 1L,
                word = "Apple",
                definitions = listOf(Definition("名詞", "りんご")),
                note = "果物"
            ),
            FlashcardEntity(
                id = 2L,
                word = "Run",
                definitions = listOf(Definition("動詞", "走る")),
                note = ""
            )
        )

        val match1 = existingCards.find { it.word.equals("apple", ignoreCase = true) }
        assertNotNull(match1)
        assertEquals(1L, match1?.id)

        val match2 = existingCards.find { it.word.equals("RUN ", ignoreCase = true) == false }
        // Trimming handles whitespace
        val trimmedMatch = existingCards.find { it.word.equals("RUN ".trim(), ignoreCase = true) }
        assertNotNull(trimmedMatch)
        assertEquals(2L, trimmedMatch?.id)

        val noMatch = existingCards.find { it.word.equals("Banana", ignoreCase = true) }
        assertNull(noMatch)
    }

    @Test
    fun mergeDefinitions_combinesDefinitionsAndNotesCorrectly() {
        val existing = FlashcardEntity(
            id = 1L,
            word = "run",
            definitions = listOf(Definition("動詞", "走る")),
            note = "基本動詞"
        )

        val newDefs = listOf(
            Definition("動詞", "走る"), // identical - should not duplicate
            Definition("名詞", "走ること、ランニング"), // new definition
            Definition("動詞", "経営する、運営する") // another new meaning
        )

        val mergedDefs = existing.definitions.toMutableList()
        for (d in newDefs) {
            if (mergedDefs.none { it.pos.equals(d.pos, ignoreCase = true) && it.meaning.equals(d.meaning, ignoreCase = true) }) {
                mergedDefs.add(d)
            }
        }

        val newNote = "ビジネスでも使う"
        val mergedNote = when {
            existing.note.isBlank() -> newNote
            newNote.isBlank() -> existing.note
            else -> "${existing.note}; $newNote"
        }

        assertEquals(3, mergedDefs.size)
        assertEquals("動詞", mergedDefs[0].pos)
        assertEquals("走る", mergedDefs[0].meaning)
        assertEquals("名詞", mergedDefs[1].pos)
        assertEquals("走ること、ランニング", mergedDefs[1].meaning)
        assertEquals("動詞", mergedDefs[2].pos)
        assertEquals("経営する、運営する", mergedDefs[2].meaning)

        assertEquals("基本動詞; ビジネスでも使う", mergedNote)
    }

    open class BaseDummyDao : com.example.data.local.FlashcardDao {
        override fun getAllCards() = kotlinx.coroutines.flow.flowOf(emptyList<FlashcardEntity>())
        override suspend fun getAllOnce() = emptyList<FlashcardEntity>()
        override suspend fun getCount() = 0
        override suspend fun getById(id: Long): FlashcardEntity? = null
        override suspend fun getByWord(word: String): FlashcardEntity? = null
        override suspend fun insert(card: FlashcardEntity) = 1L
        override suspend fun insertAll(cards: List<FlashcardEntity>): List<Long> = emptyList()
        override suspend fun update(card: FlashcardEntity) {}
        override suspend fun updateStatus(id: Long, status: String?) {}
        override suspend fun updateStatusAndConsecutive(id: Long, status: String?, consecutiveCount: Int) {}
        override suspend fun updateHidden(id: Long, isHidden: Boolean) {}
        override suspend fun updateBatchHidden(ids: List<Long>, isHidden: Boolean) {}
        override suspend fun updateSrsStatus(id: Long, status: String?, consecutiveCount: Int, level: Int, interval: Int, nextReview: Long, lastReviewed: Long) {}
        override suspend fun updateDeck(id: Long, deck: String) {}
        override suspend fun updateBatchDeck(ids: List<Long>, deck: String) {}
        override suspend fun renameDeck(oldDeck: String, newDeck: String) {}
        override fun getDistinctDecks(): kotlinx.coroutines.flow.Flow<List<String>> = kotlinx.coroutines.flow.flowOf(emptyList())
        override suspend fun delete(card: FlashcardEntity) {}
        override suspend fun deleteById(id: Long) {}
        override suspend fun deleteBatchByIds(ids: List<Long>) {}
        override suspend fun deleteByDeck(deck: String) {}
        override suspend fun deleteAll() {}
    }

    @Test
    fun findDuplicateWordsInRows_detectsExistingWords() {
        val dummyDao = BaseDummyDao()
        val repo = com.example.data.repository.FlashcardRepository(dummyDao)
        val rows = listOf(
            listOf("単語", "意味", "品詞"),
            listOf("apple", "りんご", "名詞"),
            listOf("banana", "バナナ", "名詞"),
            listOf("cherry", "さくらんぼ", "名詞")
        )
        val existingWords = setOf("apple", "cherry")
        val duplicates = repo.findDuplicateWordsInRows(rows, existingWords)

        assertEquals(2, duplicates.size)
        assertTrue(duplicates.contains("apple"))
        assertTrue(duplicates.contains("cherry"))
    }

    @Test
    fun processRowsAndInsert_withSkipDuplicates_skipsOnlyDuplicates() = kotlinx.coroutines.runBlocking {
        val insertedCards = mutableListOf<FlashcardEntity>()
        val existingCards = mutableListOf(
            FlashcardEntity(id = 1L, word = "apple", definitions = listOf(Definition("名詞", "りんご")), note = "")
        )

        val dummyDao = object : BaseDummyDao() {
            override fun getAllCards() = kotlinx.coroutines.flow.flowOf(existingCards)
            override suspend fun getAllOnce() = existingCards
            override suspend fun getCount() = existingCards.size
            override suspend fun getById(id: Long): FlashcardEntity? = existingCards.find { it.id == id }
            override suspend fun getByWord(word: String): FlashcardEntity? = existingCards.find { it.word.equals(word, ignoreCase = true) }
            override suspend fun insert(card: FlashcardEntity): Long {
                insertedCards.add(card)
                return 2L
            }
            override suspend fun insertAll(cards: List<FlashcardEntity>): List<Long> {
                insertedCards.addAll(cards)
                return cards.map { 2L }
            }
        }

        val repo = com.example.data.repository.FlashcardRepository(dummyDao)
        val rows = listOf(
            listOf("apple", "りんご新訳", "名詞"), // Duplicate -> should be SKIPPED
            listOf("banana", "バナナ", "名詞")     // New -> should be INSERTED
        )

        val result = repo.processRowsAndInsert(rows, com.example.data.repository.DuplicateStrategy.SKIP_DUPLICATES)

        assertEquals(1, result.newCount)
        assertEquals(1, result.skippedCount)
        assertEquals(listOf("apple"), result.skippedWords)
        assertEquals(1, insertedCards.size)
        assertEquals("banana", insertedCards[0].word)
    }

    @Test
    fun processRowsAndInsert_withStatusAndHidden_importsStudyData() = kotlinx.coroutines.runBlocking {
        val insertedCards = mutableListOf<FlashcardEntity>()
        val dummyDao = object : BaseDummyDao() {
            override suspend fun insertAll(cards: List<FlashcardEntity>): List<Long> {
                insertedCards.addAll(cards)
                return cards.map { 1L }
            }
        }

        val repo = com.example.data.repository.FlashcardRepository(dummyDao)
        val rows = listOf(
            listOf("単語", "意味", "品詞", "備考", "ステータス", "非表示"),
            listOf("apple", "りんご", "名詞", "果物", "○", "0"),
            listOf("banana", "バナナ", "名詞", "黄色", "△ 微妙", "1"),
            listOf("cat", "猫", "名詞", "動物", "×", "0")
        )

        val result = repo.processRowsAndInsert(rows, com.example.data.repository.DuplicateStrategy.MERGE_DUPLICATES)

        assertEquals(3, result.newCount)
        assertEquals(3, insertedCards.size)

        assertEquals("apple", insertedCards[0].word)
        assertEquals("○", insertedCards[0].status)
        assertEquals(false, insertedCards[0].isHidden)

        assertEquals("banana", insertedCards[1].word)
        assertEquals("△", insertedCards[1].status)
        assertEquals(true, insertedCards[1].isHidden)

        assertEquals("cat", insertedCards[2].word)
        assertEquals("×", insertedCards[2].status)
        assertEquals(false, insertedCards[2].isHidden)
    }

    @Test
    fun processRowsAndInsert_mergeDuplicates_preservesExistingStudyData() = kotlinx.coroutines.runBlocking {
        var updatedCard: FlashcardEntity? = null
        val existingCard = FlashcardEntity(
            id = 1L,
            word = "apple",
            definitions = listOf(Definition("名詞", "りんご")),
            note = "既存メモ",
            status = "○",
            isHidden = true
        )

        val dummyDao = object : BaseDummyDao() {
            override fun getAllCards() = kotlinx.coroutines.flow.flowOf(listOf(existingCard))
            override suspend fun getAllOnce() = listOf(existingCard)
            override suspend fun getCount() = 1
            override suspend fun getById(id: Long): FlashcardEntity? = existingCard
            override suspend fun getByWord(word: String): FlashcardEntity? = existingCard
            override suspend fun update(card: FlashcardEntity) {
                updatedCard = card
            }
        }

        val repo = com.example.data.repository.FlashcardRepository(dummyDao)
        // Incoming CSV without status column
        val rows = listOf(
            listOf("単語", "意味", "品詞", "備考"),
            listOf("apple", "リンゴの木", "名詞", "新メモ")
        )

        val result = repo.processRowsAndInsert(rows, com.example.data.repository.DuplicateStrategy.MERGE_DUPLICATES)

        assertEquals(1, result.mergeCount)
        assertNotNull(updatedCard)
        assertEquals("○", updatedCard?.status) // Existing learning data preserved!
        assertEquals(true, updatedCard?.isHidden) // Existing hidden flag preserved!
        assertEquals(2, updatedCard?.definitions?.size)
    }

    @Test
    fun parseStatus_recognizesVariousFormats() {
        val repo = com.example.data.repository.FlashcardRepository(BaseDummyDao())

        assertEquals("○", repo.parseStatus("○"))
        assertEquals("○", repo.parseStatus("○ 完璧"))
        assertEquals("○", repo.parseStatus("完璧"))
        assertEquals("○", repo.parseStatus("perfect"))

        assertEquals("△", repo.parseStatus("△"))
        assertEquals("△", repo.parseStatus("△ 微妙"))
        assertEquals("△", repo.parseStatus("微妙"))

        assertEquals("×", repo.parseStatus("×"))
        assertEquals("×", repo.parseStatus("× 忘れた"))
        assertEquals("×", repo.parseStatus("忘れた"))
        assertEquals("×", repo.parseStatus("x"))

        assertNull(repo.parseStatus(""))
        assertNull(repo.parseStatus("未評価"))
        assertNull(repo.parseStatus("-"))
        assertNull(repo.parseStatus("none"))
    }
}
