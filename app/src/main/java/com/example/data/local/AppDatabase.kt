package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FlashcardEntity::class, CustomDictionaryOverride::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(FlashcardConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun flashcardDao(): FlashcardDao
    abstract fun customDictionaryOverrideDao(): CustomDictionaryOverrideDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE flashcards ADD COLUMN consecutivePerfectCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE flashcards ADD COLUMN deck TEXT NOT NULL DEFAULT '日常英会話'")
                db.execSQL("ALTER TABLE flashcards ADD COLUMN repetitionLevel INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE flashcards ADD COLUMN intervalDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE flashcards ADD COLUMN nextReviewTimestamp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE flashcards ADD COLUMN lastReviewedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE flashcards ADD COLUMN etymology TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE flashcards ADD COLUMN collocations TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE flashcards ADD COLUMN synonyms TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE flashcards ADD COLUMN antonyms TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE flashcards ADD COLUMN subject TEXT NOT NULL DEFAULT 'ENGLISH'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS custom_dictionary_overrides (
                        wordKey TEXT NOT NULL PRIMARY KEY,
                        word TEXT NOT NULL,
                        isClassical INTEGER NOT NULL DEFAULT 0,
                        isDeleted INTEGER NOT NULL DEFAULT 0,
                        isUserCreated INTEGER NOT NULL DEFAULT 0,
                        pos TEXT NOT NULL DEFAULT '名詞',
                        meanings TEXT NOT NULL DEFAULT '',
                        deck TEXT NOT NULL DEFAULT '日常英会話',
                        note TEXT NOT NULL DEFAULT '',
                        conjugation TEXT NOT NULL DEFAULT '',
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flashcard_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
