package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.Definition
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class FlashcardConverters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val type = Types.newParameterizedType(List::class.java, Definition::class.java)
    private val adapter = moshi.adapter<List<Definition>>(type)

    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

    @TypeConverter
    fun fromDefinitionsList(definitions: List<Definition>?): String {
        if (definitions.isNullOrEmpty()) return "[]"
        return adapter.toJson(definitions)
    }

    @TypeConverter
    fun toDefinitionsList(json: String?): List<Definition> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringList(strings: List<String>?): String {
        if (strings.isNullOrEmpty()) return "[]"
        return stringListAdapter.toJson(strings)
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            stringListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
