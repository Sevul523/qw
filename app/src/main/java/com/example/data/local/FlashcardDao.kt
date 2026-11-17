package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY createdAt ASC")
    fun getAllCards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<FlashcardEntity>

    @Query("SELECT COUNT(*) FROM flashcards")
    suspend fun getCount(): Int

    @Query("SELECT * FROM flashcards WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FlashcardEntity?

    @Query("SELECT * FROM flashcards WHERE LOWER(word) = LOWER(:word) LIMIT 1")
    suspend fun getByWord(word: String): FlashcardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<FlashcardEntity>): List<Long>

    @Update
    suspend fun update(card: FlashcardEntity)

    @Query("UPDATE flashcards SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String?)

    @Query("UPDATE flashcards SET status = :status, consecutivePerfectCount = :consecutiveCount WHERE id = :id")
    suspend fun updateStatusAndConsecutive(id: Long, status: String?, consecutiveCount: Int)

    @Query("UPDATE flashcards SET isHidden = :isHidden WHERE id = :id")
    suspend fun updateHidden(id: Long, isHidden: Boolean)

    @Query("UPDATE flashcards SET isHidden = :isHidden WHERE id IN (:ids)")
    suspend fun updateBatchHidden(ids: List<Long>, isHidden: Boolean)

    @Query("UPDATE flashcards SET status = :status, consecutivePerfectCount = :consecutiveCount, repetitionLevel = :level, intervalDays = :interval, nextReviewTimestamp = :nextReview, lastReviewedAt = :lastReviewed WHERE id = :id")
    suspend fun updateSrsStatus(id: Long, status: String?, consecutiveCount: Int, level: Int, interval: Int, nextReview: Long, lastReviewed: Long)

    @Query("UPDATE flashcards SET deck = :deck WHERE id = :id")
    suspend fun updateDeck(id: Long, deck: String)

    @Query("UPDATE flashcards SET deck = :deck WHERE id IN (:ids)")
    suspend fun updateBatchDeck(ids: List<Long>, deck: String)

    @Query("UPDATE flashcards SET deck = :newDeck WHERE deck = :oldDeck")
    suspend fun renameDeck(oldDeck: String, newDeck: String)

    @Query("SELECT DISTINCT deck FROM flashcards WHERE deck IS NOT NULL AND deck != ''")
    fun getDistinctDecks(): Flow<List<String>>

    @Query("DELETE FROM flashcards WHERE deck = :deck")
    suspend fun deleteByDeck(deck: String)

    @Delete
    suspend fun delete(card: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM flashcards WHERE id IN (:ids)")
    suspend fun deleteBatchByIds(ids: List<Long>)

    @Query("DELETE FROM flashcards")
    suspend fun deleteAll()
}
