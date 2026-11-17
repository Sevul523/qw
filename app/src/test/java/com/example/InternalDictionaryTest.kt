package com.example

import com.example.data.model.ClassicalWordKnowledgeBase
import com.example.data.service.DictionaryLevelFilter
import com.example.data.service.InternalDictionaryService
import com.example.ui.settings.DictionaryPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class InternalDictionaryTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun setUp() {
            if (InternalDictionaryService.isLoaded()) return
            val assetsDir = File("src/main/assets")
            val partFiles = assetsDir.listFiles { _, name ->
                name.startsWith("internal_ej_dict_part") && name.endsWith(".tsv.gz")
            }?.sortedBy { f ->
                Regex("part(\\d+)").find(f.name)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
            }.orEmpty()

            if (partFiles.isNotEmpty()) {
                val streams = partFiles.map { FileInputStream(it) }
                try {
                    InternalDictionaryService.loadFromInputStreams(streams)
                } finally {
                    streams.forEach { runCatching { it.close() } }
                }
            } else {
                val gzFile = File(assetsDir, "internal_ej_dict.tsv.gz")
                val tsvFile = File(assetsDir, "internal_ej_dict.tsv")
                val target = if (gzFile.exists()) gzFile else tsvFile
                if (target.exists()) {
                    FileInputStream(target).use { stream ->
                        InternalDictionaryService.loadFromInputStream(stream)
                    }
                }
            }
        }
    }

    @Test
    fun testEnglishVocabularyCountReachesFiveHundredThousand() {
        val (engCount, _) = InternalDictionaryService.getVocabularyCount()
        assertTrue("Internal English dictionary must contain at least 535,000 words: actual=$engCount", engCount >= 535000)
    }

    @Test
    fun testIdiomAndPhrasalVerbLookups() {
        val essentialIdioms = listOf(
            "take into account",
            "take advantage of",
            "look forward to",
            "by virtue of",
            "in terms of",
            "put up with",
            "catch up with",
            "keep in mind",
            "as a matter of fact"
        )
        for (phrase in essentialIdioms) {
            val res = InternalDictionaryService.lookupEnglish(phrase)
            assertNotNull("Idiom '$phrase' must be found in internal dictionary", res)
            assertTrue("Idiom '$phrase' must have definitions", res!!.definitions.isNotEmpty())
            assertTrue("isIdiomPhrase('$phrase') should be true", InternalDictionaryService.isIdiomPhrase(phrase))
        }

        // Test phrase verb lemmatization / inflection
        val conjugated = InternalDictionaryService.lookupEnglish("took advantage of")
        assertNotNull("Conjugated idiom 'took advantage of' should resolve", conjugated)

        val gerund = InternalDictionaryService.lookupEnglish("looking forward to")
        assertNotNull("Gerund idiom 'looking forward to' should resolve", gerund)
    }

    @Test
    fun testUniversityEntranceExamLongReadingVocabulary() {
        // 1. Polysemous words with numbered definitions
        val addressRes = InternalDictionaryService.lookupEnglish("address")
        assertNotNull("Word 'address' must be found", addressRes)
        assertTrue("address must have polysemy numbering", addressRes!!.summaryMeaning.contains("【第1義】") && addressRes.summaryMeaning.contains("【第2義】"))

        val disciplineRes = InternalDictionaryService.lookupEnglish("discipline")
        assertNotNull("Word 'discipline' must be found", disciplineRes)
        assertTrue("discipline must include academic field meaning", disciplineRes!!.summaryMeaning.contains("学問分野") || disciplineRes.summaryMeaning.contains("【第2義】"))

        // 2. Syntax / Discourse marker phrase
        val syntaxRes = InternalDictionaryService.lookupEnglish("not so much a as b")
        assertNotNull("Phrase 'not so much a as b' must be found", syntaxRes)
        assertTrue(syntaxRes!!.summaryMeaning.contains("AというよりむしろB"))

        // 3. Academic / Medical / Tech keywords
        val anthropoceneRes = InternalDictionaryService.lookupEnglish("anthropocene")
        assertNotNull("Word 'anthropocene' must be found", anthropoceneRes)
        assertTrue(anthropoceneRes!!.summaryMeaning.contains("人新世"))

        val homeostasisRes = InternalDictionaryService.lookupEnglish("homeostasis")
        assertNotNull("Word 'homeostasis' must be found", homeostasisRes)
        assertTrue(homeostasisRes!!.summaryMeaning.contains("恒常性") || homeostasisRes.summaryMeaning.contains("ホメオスタシス"))

        // 4. Confusing derivatives notes
        val respectableRes = InternalDictionaryService.lookupEnglish("respectable")
        assertNotNull("Word 'respectable' must be found", respectableRes)
        assertTrue(respectableRes!!.note.contains("respectful") || respectableRes.note.contains("派生語"))
    }

    @Test
    fun testClassicalVocabularyCountReachesEightHundred() {
        val count = ClassicalWordKnowledgeBase.ALL_ENTRIES.size
        assertTrue("Internal Classical Japanese vocabulary must contain at least 800 words: actual=$count", count >= 800)
    }

    @Test
    fun testExactEnglishLookups() {
        val testWords = listOf("apple", "school", "water", "study", "run", "friend", "happy", "world")
        for (w in testWords) {
            val result = InternalDictionaryService.lookupEnglish(w)
            assertNotNull("Word '$w' should be found in internal dictionary", result)
            assertTrue("Word '$w' should have definitions", result!!.definitions.isNotEmpty())
            assertTrue("Word '$w' definitions should have meaning text", result.definitions.first().meaning.isNotBlank())
        }
    }

    @Test
    fun testEnglishInflectionStemming() {
        // Plural
        val pluralResult = InternalDictionaryService.lookupEnglish("cats")
        assertNotNull("Plural 'cats' should be resolved", pluralResult)
        assertTrue(pluralResult!!.definitions.first().meaning.isNotBlank())

        val iesResult = InternalDictionaryService.lookupEnglish("cities")
        assertNotNull("Plural 'cities' should be resolved", iesResult)

        // Past tense
        val edResult = InternalDictionaryService.lookupEnglish("walked")
        assertNotNull("Past 'walked' should be resolved", edResult)

        val iedResult = InternalDictionaryService.lookupEnglish("studied")
        assertNotNull("Past 'studied' should be resolved", iedResult)

        // Irregulars
        val wentResult = InternalDictionaryService.lookupEnglish("went")
        assertNotNull("Irregular 'went' should be resolved to go", wentResult)

        val betterResult = InternalDictionaryService.lookupEnglish("better")
        assertNotNull("Comparative 'better' should be resolved", betterResult)
    }

    @Test
    fun testClassicalJapaneseLookups() {
        val sampleClassical = listOf(
            "おどろく", "ののしる", "ねんず", "おぼゆ", "まもる", "らうたし", "うつくし", "あはれなり", "をかし",
            "き", "けり", "つ", "ぬ", "たり (完了)", "らむ", "めり", "まし", "まじ", "べし",
            "ばや", "てしがな", "なむ (願望)", "だに", "すら", "さへ",
            "あかねさす", "あしひきの", "ぬばたまの", "ちはやぶる", "きさらぎ", "やよひ", "あけぼの", "みゆき"
        )
        for (w in sampleClassical) {
            val result = InternalDictionaryService.lookupClassical(w)
            assertNotNull("Classical word '$w' should be found in internal KB", result)
            assertTrue("Classical word '$w' should have definitions", result!!.definitions.isNotEmpty())
            assertTrue("Classical word '$w' should be marked as classical", result.isClassical)
        }
    }

    @Test
    fun testDictionaryPriorityValues() {
        assertEquals(DictionaryPriority.INTERNAL_FIRST, DictionaryPriority.fromId("INTERNAL_FIRST"))
        assertEquals(DictionaryPriority.WEBLIO_FIRST, DictionaryPriority.fromId("WEBLIO_FIRST"))
        assertEquals(DictionaryPriority.INTERNAL_FIRST, DictionaryPriority.fromId(null))
        assertEquals(DictionaryPriority.INTERNAL_FIRST, DictionaryPriority.fromId("UNKNOWN"))

        assertEquals("超高速・オフライン", DictionaryPriority.INTERNAL_FIRST.badgeLabel)
        assertEquals("オンライン詳細", DictionaryPriority.WEBLIO_FIRST.badgeLabel)
    }

    @Test
    fun testToWeblioLookupResultConversion() {
        val internal = InternalDictionaryService.lookupEnglish("study")
        assertNotNull(internal)
        val weblioResult = internal!!.toWeblioLookupResult()
        assertEquals(internal.word, weblioResult.word)
        assertEquals(internal.definitions, weblioResult.definitions)
        assertEquals(internal.summaryMeaning, weblioResult.summaryMeaning)
    }

    @Test
    fun testDictionarySearchFunctionality() {
        // English prefix search
        val appResults = InternalDictionaryService.search("app", isClassical = false)
        assertTrue("Prefix search for 'app' should return results", appResults.isNotEmpty())
        assertTrue(appResults.any { it.word.startsWith("app") })

        // English meaning search
        val schoolResults = InternalDictionaryService.search("学校", isClassical = false)
        assertTrue("Meaning search for '学校' should return results", schoolResults.isNotEmpty())
        assertTrue(schoolResults.any { it.word == "school" || it.summaryMeaning.contains("学校") })

        // Classical Japanese search by headword
        val odoroResults = InternalDictionaryService.search("おど", isClassical = true)
        assertTrue("Classical search for 'おど' should return results", odoroResults.isNotEmpty())
        assertTrue(odoroResults.any { it.word == "おどろく" })

        // Classical Japanese search by meaning
        val awakenResults = InternalDictionaryService.search("目を覚ます", isClassical = true)
        assertTrue("Classical search for '目を覚ます' should return results", awakenResults.isNotEmpty())
        assertTrue(awakenResults.any { it.word == "おどろく" })

        // Empty search returns initial items
        val allEng = InternalDictionaryService.search("", isClassical = false, limit = 50)
        assertEquals(50, allEng.size)

        val allClassical = InternalDictionaryService.search("", isClassical = true, limit = 50)
        assertEquals(50, allClassical.size)
    }

    @Test
    fun testGetAllEntries() {
        val engEntries = InternalDictionaryService.getAllEnglishEntries()
        assertTrue("getAllEnglishEntries should return >= 120000 words: actual=${engEntries.size}", engEntries.size >= 120000)

        val classicalEntries = InternalDictionaryService.getAllClassicalEntries()
        assertTrue("getAllClassicalEntries should return >= 800 words: actual=${classicalEntries.size}", classicalEntries.size >= 800)
    }

    @Test
    fun testCustomDictionaryOverrides() {
        val newWord = com.example.data.local.CustomDictionaryOverride(
            wordKey = "quantumcore",
            word = "QuantumCore",
            isClassical = false,
            isDeleted = false,
            isUserCreated = true,
            pos = "名詞",
            meanings = "量子コア; 次世代処理装置",
            deck = "科学技術",
            note = "ユーザーによるカスタム登録単語"
        )
        val editedApple = com.example.data.local.CustomDictionaryOverride(
            wordKey = "apple",
            word = "apple",
            isClassical = false,
            isDeleted = false,
            isUserCreated = false,
            pos = "名詞",
            meanings = "リンゴ（特選果物）; アップル社",
            deck = "フルーツ",
            note = "編集済みメモ"
        )
        val deletedWater = com.example.data.local.CustomDictionaryOverride(
            wordKey = "water",
            word = "water",
            isClassical = false,
            isDeleted = true,
            isUserCreated = false
        )

        InternalDictionaryService.applyOverrides(listOf(newWord, editedApple, deletedWater))

        // Check new word is found
        val newLookup = InternalDictionaryService.lookupEnglish("quantumcore")
        assertNotNull("Newly registered word should be found", newLookup)
        assertEquals("QuantumCore", newLookup!!.word)
        assertTrue(newLookup.summaryMeaning.contains("量子コア"))
        assertTrue(newLookup.isUserCreated)

        // Check edited word has updated meaning
        val editedLookup = InternalDictionaryService.lookupEnglish("apple")
        assertNotNull(editedLookup)
        assertTrue(editedLookup!!.summaryMeaning.contains("リンゴ（特選果物）"))
        assertTrue(editedLookup.isCustomEdited)

        // Check deleted word is not found
        val deletedLookup = InternalDictionaryService.lookupEnglish("water")
        org.junit.Assert.assertNull("Deleted word 'water' should not be found", deletedLookup)

        // Clean up overrides
        InternalDictionaryService.applyOverrides(emptyList())

        // Water should be found again after reset
        val restoredWater = InternalDictionaryService.lookupEnglish("water")
        assertNotNull("Word 'water' should be restored after reset", restoredWater)
    }

    @Test
    fun testBasicVocabularyAuditAndAccuracy() {
        val man = InternalDictionaryService.lookupEnglish("man")
        assertNotNull("'man' must be found", man)
        assertTrue("'man' meaning must include 男 or 人間, got=${man!!.summaryMeaning}", man.summaryMeaning.contains("男") || man.summaryMeaning.contains("人間"))
        assertFalse("'man' primary meaning must not start with マン島", man.summaryMeaning.startsWith("マン島"))
        assertEquals("基礎", man.levelBadge)
        assertEquals(1, man.priorityTier)

        val expectedPosAndKeywords = listOf(
            Triple("always", "副詞", "いつも"),
            Triple("because", "接続詞", "なぜなら"),
            Triple("think", "動詞", "考える"),
            Triple("point", "名詞", "点"),
            Triple("stay", "動詞", "とどまる"),
            Triple("ask", "動詞", "尋ねる"),
            Triple("wish", "動詞", "願う")
        )
        for ((w, expectedPos, expectedMeaning) in expectedPosAndKeywords) {
            val res = InternalDictionaryService.lookupEnglish(w)
            assertNotNull("Basic word '$w' must exist", res)
            assertEquals("Basic word '$w' POS mismatch", expectedPos, res!!.pos)
            assertTrue("Basic word '$w' meaning '${res.summaryMeaning}' should contain '$expectedMeaning'", res.summaryMeaning.contains(expectedMeaning))
            assertEquals("Basic word '$w' should be Tier 1", 1, res.priorityTier)
        }
    }

    @Test
    fun testBasicWordsPrioritizedAndLevelFilterWorks() {
        // Default blank search shows Tier 1 basic words (no numeric junk like 1950s)
        val topResults = InternalDictionaryService.search("", isClassical = false, limit = 50)
        assertEquals(50, topResults.size)
        assertTrue("All top 50 default results should be Tier 1 basic words", topResults.all { it.priorityTier == 1 })
        assertFalse("Junk like 1950s must not appear in top 50 basic results", topResults.any { it.word == "1950s" || it.word == "51sts" })

        // Query search prioritizes exact & basic words first
        val getResults = InternalDictionaryService.search("get", isClassical = false, limit = 30)
        assertTrue(getResults.isNotEmpty())
        assertEquals("Exact match 'get' must be ranked #1", "get", getResults.first().word.lowercase())
        assertTrue("Top search results for 'get' should include Tier 1/2 entries early", getResults.take(5).all { it.priorityTier <= 3 })

        // Level filter BASIC strictly returns Tier 1 entries
        val basicOnly = InternalDictionaryService.search("app", isClassical = false, levelFilter = DictionaryLevelFilter.BASIC, limit = 50)
        assertTrue("BASIC filter for 'app' should return results", basicOnly.isNotEmpty())
        assertTrue("BASIC filter must only return Tier 1", basicOnly.all { it.priorityTier == 1 })

        // Level filter SPECIALIZED strictly returns Tier 5 entries
        val specOnly = InternalDictionaryService.search("neuro", isClassical = false, levelFilter = DictionaryLevelFilter.SPECIALIZED, limit = 30)
        assertTrue("SPECIALIZED filter for 'neuro' should return results", specOnly.isNotEmpty())
        assertTrue("SPECIALIZED filter must only return Tier 5", specOnly.all { it.priorityTier == 5 })
    }
}
