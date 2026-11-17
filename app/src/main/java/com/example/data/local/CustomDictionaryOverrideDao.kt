package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomDictionaryOverrideDao {

    @Query("SELECT * FROM custom_dictionary_overrides ORDER BY updatedAt DESC")
    fun getAllOverridesFlow(): Flow<List<CustomDictionaryOverride>>

    @Query("SELECT * FROM custom_dictionary_overrides")
    suspend fun getAllOverrides(): List<CustomDictionaryOverride>

    @Query("SELECT * FROM custom_dictionary_overrides WHERE wordKey = :wordKey LIMIT 1")
    suspend fun getOverrideByKey(wordKey: String): CustomDictionaryOverride?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(override: CustomDictionaryOverride)

    @Query("DELETE FROM custom_dictionary_overrides WHERE wordKey = :wordKey")
    suspend fun deleteOverrideByKey(wordKey: String)

    @Query("DELETE FROM custom_dictionary_overrides")
    suspend fun clearAllOverrides()
}
