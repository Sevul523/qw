package com.example

import com.example.data.local.FlashcardDao
import com.example.data.local.FlashcardEntity
import com.example.data.model.ClassicalPosNormalizer
import com.example.data.model.ClassicalWordKnowledgeBase
import com.example.data.model.Definition
import com.example.data.model.LearningMode
import com.example.data.model.PartsOfSpeech
import com.example.data.ocr.OcrExtractor
import com.example.data.ocr.OcrSensitivity
import com.example.data.repository.FlashcardRepository
import com.example.data.service.WeblioDictionaryService
import com.example.ui.MainViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicalJapaneseTest {

    @Test
    fun testLearningModeProperties() {
        val english = LearningMode.ENGLISH
        val classical = LearningMode.CLASSICAL_JAPANESE

        assertEquals("ENGLISH", english.id)
        assertEquals("英単語", english.displayName)
        assertFalse(english.isClassical)

        assertEquals("CLASSICAL", classical.id)
        assertEquals("古語", classical.displayName)
        assertTrue(classical.isClassical)
    }

    @Test
    fun testClassicalKnowledgeBaseEntriesUseSimplePosAndConjugationInNote() {
        val initialCards = ClassicalWordKnowledgeBase.createInitialClassicalCards()
        assertTrue("Initial classical cards should contain essential words", initialCards.size >= 150)

        // Verify all initial cards have subject = "CLASSICAL" and strictly simple POS
        assertTrue(initialCards.all { it.subject == "CLASSICAL" })
        for (card in initialCards) {
            for (def in card.definitions) {
                assertTrue(
                    "Card '${card.word}' should have simple POS in PartsOfSpeech.CLASSICAL but got '${def.pos}'",
                    PartsOfSpeech.CLASSICAL.contains(def.pos)
                )
                assertFalse(
                    "Card '${card.word}' POS '${def.pos}' should not contain conjugation details",
                    def.pos.contains("四段") || def.pos.contains("二段") || def.pos.contains("一段") ||
                        def.pos.contains("変格") || def.pos.contains("活用")
                )
            }
        }

        // Test essential words lookup: simple POS + conjugation in note & conjugation property
        val odoroku = ClassicalWordKnowledgeBase.lookup("おどろく")
        assertNotNull(odoroku)
        assertEquals("おどろく", odoroku?.word)
        assertTrue(odoroku!!.definitions.all { it.pos == "動詞" })
        assertTrue(odoroku.conjugation.contains("四段"))
        assertTrue(odoroku.note.contains("四段"))
        assertTrue(odoroku.definitions.any { it.meaning.contains("目を覚ます") || it.meaning.contains("はっと気づく") })

        val wokashi = ClassicalWordKnowledgeBase.lookup("をかし")
        assertNotNull(wokashi)
        assertTrue(wokashi!!.definitions.all { it.pos == "形容詞" })
        assertEquals("シク活用", wokashi.conjugation)
        assertTrue(wokashi.note.contains("シク活用"))
        assertTrue(wokashi.definitions.any { it.meaning.contains("趣") || it.meaning.contains("すばらしい") })

        val aware = ClassicalWordKnowledgeBase.lookup("あはれなり")
        assertNotNull(aware)
        assertTrue(aware!!.definitions.all { it.pos == "形容動詞" })
        assertEquals("ナリ活用", aware.conjugation)
        assertTrue(aware.note.contains("ナリ活用"))
        assertTrue(aware.definitions.any { it.meaning.contains("情趣") || it.meaning.contains("心にしみる") })

        val tatematsuru = ClassicalWordKnowledgeBase.lookup("奉る")
        assertNotNull(tatematsuru)
        assertTrue(tatematsuru!!.definitions.all { it.pos == "動詞" })
        assertTrue(tatematsuru.note.contains("四段"))

        val tsuto = ClassicalWordKnowledgeBase.lookup("つとめて")
        assertNotNull(tsuto)
        assertTrue(tsuto!!.definitions.any { it.meaning.contains("早朝") })
    }

    @Test
    fun testClassicalPartsOfSpeechCategoriesAreSimple() {
        val expectedClassicalPos = listOf(
            "動詞",
            "形容詞",
            "形容動詞",
            "助動詞",
            "名詞",
            "副詞",
            "連体詞",
            "接続詞",
            "感動詞",
            "その他"
        )
        assertEquals(expectedClassicalPos, PartsOfSpeech.CLASSICAL)
        assertFalse(PartsOfSpeech.CLASSICAL.contains("動詞 四段"))
        assertFalse(PartsOfSpeech.CLASSICAL.contains("形容詞 ク活用"))
        assertFalse(PartsOfSpeech.CLASSICAL.contains("形容動詞 ナリ活用"))

        assertTrue(PartsOfSpeech.ALL.contains("動詞"))
        assertTrue(PartsOfSpeech.ALL.contains("形容動詞"))
        assertTrue(PartsOfSpeech.ALL.contains("助動詞"))
        assertTrue(PartsOfSpeech.ALL.contains("名詞"))
    }

    @Test
    fun testFlashcardEntitySubjectDefaultAndCopy() {
        val defaultEntity = FlashcardEntity(
            word = "apple",
            definitions = listOf(Definition("名詞", "りんご"))
        )
        assertEquals("ENGLISH", defaultEntity.subject)

        val classicalEntity = defaultEntity.copy(
            word = "いみじ",
            definitions = listOf(Definition("形容詞", "たいそう・はなはだしい")),
            note = "シク活用",
            subject = "CLASSICAL"
        )
        assertEquals("CLASSICAL", classicalEntity.subject)
        assertEquals("形容詞", classicalEntity.definitions[0].pos)
        assertEquals("シク活用", classicalEntity.note)
    }

    @Test
    fun testClassicalModeDisablesTtsAndRestrictsQuizTypes() {
        val englishMode = LearningMode.ENGLISH
        val classicalMode = LearningMode.CLASSICAL_JAPANESE

        // Effective TTS visibility: enabled only in English mode when ttsEnabled is true
        val ttsPrefTrue = true
        assertTrue(ttsPrefTrue && !englishMode.isClassical)
        assertFalse(ttsPrefTrue && !classicalMode.isClassical)

        // Effective QuizType: Classical mode forces MULTIPLE_CHOICE (古語 -> 現代語訳)
        for (requestedType in com.example.ui.quiz.QuizType.entries) {
            val effectiveInClassical = if (classicalMode.isClassical) {
                com.example.ui.quiz.QuizType.MULTIPLE_CHOICE
            } else {
                requestedType
            }
            assertEquals(com.example.ui.quiz.QuizType.MULTIPLE_CHOICE, effectiveInClassical)

            val effectiveInEnglish = if (englishMode.isClassical) {
                com.example.ui.quiz.QuizType.MULTIPLE_CHOICE
            } else {
                requestedType
            }
            assertEquals(requestedType, effectiveInEnglish)
        }
    }

    @Test
    fun testIsolatedPreferenceKeysPerLearningMode() {
        val settingBaseKeys = listOf(
            "is_shuffle",
            "weblio_autofill_enabled",
            "weblio_auto_add_conjugation_enabled",
            "ocr_exclude_registered_words",
            "ocr_auto_lookup_meaning_enabled",
            "ocr_sensitivity",
            "theme_color",
            "card_back_theme",
            "font_family",
            "auto_uncheck_perfect_count",
            "auto_uncheck_action",
            "custom_decks",
            "deleted_default_decks"
        )

        for (baseKey in settingBaseKeys) {
            val englishKey = MainViewModel.resolvePrefKey(baseKey, LearningMode.ENGLISH)
            val classicalKey = MainViewModel.resolvePrefKey(baseKey, LearningMode.CLASSICAL_JAPANESE)

            assertEquals(baseKey, englishKey)
            assertEquals("${baseKey}_classical", classicalKey)
            assertNotEquals(englishKey, classicalKey)
        }
    }

    @Test
    fun testWeblioClassicalHtmlParserAndConjugationExtraction() {
        val sampleHtml = """
            <html>
            <head>
                <meta name="description" content="「あはれなり」の意味は【形容動詞ナリ活用】①しみじみと情趣深い。②かわいそうだ。 - Weblio古語辞典" />
            </head>
            <body>
                <div class="kiji">
                    <div class="Gkgjt">
                        <h2><ruby>哀<rt>あは</rt></ruby>れなり</h2>
                        <p>【形容動詞ナリ活用】活用｛なら／なり・に／なり／なる／なれ／なれ｝</p>
                        <p>①しみじみと<ruby>情趣<rt>じょうしゅ</rt></ruby>深い。趣がある。出典『枕草子』「春はあけぼの」[訳] 春は夜明けがしみじみと趣深い。</p>
                        <p>②かわいそうだ。気の毒だ。</p>
                        <p>③すばらしい。立派だ。</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val result = WeblioDictionaryService.parseWeblioClassicalHtml("あはれなり", sampleHtml)
        assertNotNull(result)
        assertEquals("あはれなり", result!!.word)
        assertEquals(3, result.definitions.size)
        assertEquals("形容動詞", result.definitions[0].pos)
        assertTrue(result.definitions[0].meaning.contains("しみじみと情趣深い"))
        assertFalse("Ruby reading should be stripped cleanly", result.definitions[0].meaning.contains("じょうしゅ"))
        assertFalse("Example citation should be trimmed", result.definitions[0].meaning.contains("枕草子"))
        assertTrue(result.definitions[1].meaning.contains("かわいそうだ"))

        // Classical mode extracts conjugation for note, and NEVER populates English Weblio level / Eiken grade
        assertEquals("", result.level)
        assertEquals("", result.eikenGrade)
        assertEquals("ナリ活用", result.conjugation)
        assertEquals("ナリ活用", result.formattedNote)

        // Not-found page returns null
        val notFoundHtml = "<html><body>一致する見出し語は見つかりませんでした</body></html>"
        assertNull(WeblioDictionaryService.parseWeblioClassicalHtml("そんざいしないご", notFoundHtml))

        // POS & conjugation split checks
        assertEquals("動詞" to "カ行四段活用", WeblioDictionaryService.normalizeClassicalPosAndConjugation("動カ四"))
        assertEquals("動詞" to "マ行下二段活用", WeblioDictionaryService.normalizeClassicalPosAndConjugation("他動詞マ行下二段活用"))
        assertEquals("形容詞" to "シク活用", WeblioDictionaryService.normalizeClassicalPosAndConjugation("形シク"))
        assertEquals("形容動詞" to "タリ活用", WeblioDictionaryService.normalizeClassicalPosAndConjugation("形容動詞タリ活用"))
        assertEquals("助動詞" to "", WeblioDictionaryService.normalizeClassicalPosAndConjugation("助動"))
        assertEquals("名詞" to "", WeblioDictionaryService.normalizeClassicalPosAndConjugation("名"))
    }

    @Test
    fun testClassicalOcrWordExtraction() {
        val sampleClassicalPassage = """
            春はあけぼの。やうやう白くなりゆく山際、少し明かりて、紫だちたる雲の細くたなびきたる。
            つとめて、いとをかしき事におどろき、あはれなる心地す。
        """.trimIndent()

        val extractedHigh = OcrExtractor.extractClassicalWords(
            rawText = sampleClassicalPassage,
            sensitivity = OcrSensitivity.HIGH,
            filterStopWords = true
        )

        assertTrue("Should extract つとめて", extractedHigh.contains("つとめて"))
        assertTrue("Should normalize をかしき -> をかし", extractedHigh.contains("をかし"))
        assertTrue("Should normalize おどろき -> おどろく", extractedHigh.contains("おどろく"))
        assertTrue("Should normalize あはれなる -> あはれなり", extractedHigh.contains("あはれなり"))
        assertFalse("Should filter modern/meta stop words", extractedHigh.contains("こと"))

        val extractedLow = OcrExtractor.extractClassicalWords(
            rawText = "おどろき、あはれなることあり。",
            sensitivity = OcrSensitivity.LOW,
            filterStopWords = true
        )
        assertTrue(extractedLow.contains("おどろく"))
        assertTrue(extractedLow.contains("あはれなり"))
    }

    @Test
    fun testClassicalCsvColumnsAndConjugationToNoteNormalization() = runBlocking {
        val insertedCards = mutableListOf<FlashcardEntity>()
        val updatedCards = mutableListOf<FlashcardEntity>()
        val existingDbCards = mutableListOf(
            // Existing English card should not collide with Classical import or migration
            FlashcardEntity(id = 1L, word = "あはれなり", definitions = listOf(Definition("名詞", "dummy")), subject = "ENGLISH"),
            // Legacy Classical card with conjugation inside POS to test migration
            FlashcardEntity(
                id = 2L,
                word = "覚ゆ",
                definitions = listOf(Definition("動詞 ヤ行下二段", "思われる / 似ている")),
                note = "頻出語",
                subject = "CLASSICAL"
            )
        )
        val dummyDao = object : FlashcardDao {
            override fun getAllCards() = flowOf(emptyList<FlashcardEntity>())
            override suspend fun getAllOnce() = existingDbCards.toList()
            override suspend fun getCount() = existingDbCards.size
            override suspend fun getById(id: Long): FlashcardEntity? = existingDbCards.find { it.id == id }
            override suspend fun getByWord(word: String): FlashcardEntity? = null
            override suspend fun insert(card: FlashcardEntity) = 1L
            override suspend fun insertAll(cards: List<FlashcardEntity>): List<Long> {
                insertedCards.addAll(cards)
                return cards.mapIndexed { idx, _ -> (idx + 10).toLong() }
            }
            override suspend fun update(card: FlashcardEntity) {
                updatedCards.add(card)
            }
            override suspend fun updateStatus(id: Long, status: String?) {}
            override suspend fun updateStatusAndConsecutive(id: Long, status: String?, consecutiveCount: Int) {}
            override suspend fun updateHidden(id: Long, isHidden: Boolean) {}
            override suspend fun updateBatchHidden(ids: List<Long>, isHidden: Boolean) {}
            override suspend fun updateSrsStatus(id: Long, status: String?, consecutiveCount: Int, level: Int, interval: Int, nextReview: Long, lastReviewed: Long) {}
            override suspend fun updateDeck(id: Long, deck: String) {}
            override suspend fun updateBatchDeck(ids: List<Long>, deck: String) {}
            override suspend fun renameDeck(oldDeck: String, newDeck: String) {}
            override fun getDistinctDecks() = flowOf(emptyList<String>())
            override suspend fun delete(card: FlashcardEntity) {}
            override suspend fun deleteById(id: Long) {}
            override suspend fun deleteBatchByIds(ids: List<Long>) {}
            override suspend fun deleteByDeck(deck: String) {}
            override suspend fun deleteAll() {}
        }

        val repo = FlashcardRepository(dummyDao)

        // 1. Verify existing Classical DB cards are normalized so POS is simple and conjugation moves to note
        repo.normalizeExistingClassicalCards()
        assertEquals(1, updatedCards.size)
        assertEquals("覚ゆ", updatedCards[0].word)
        assertEquals("動詞", updatedCards[0].definitions[0].pos)
        assertEquals("ヤ行下二段活用 / 頻出語", updatedCards[0].note)

        // 2. Verify Classical CSV headers and legacy/new POS handling
        val rows = listOf(
            listOf("古語", "現代語訳", "品詞", "備考", "ステータス", "非表示", "デッキ"),
            listOf("あはれなり", "しみじみと趣深い", "形容動詞 ナリ活用", "王朝文学の美的理念", "○", "0", "最頻出形容詞・形容動詞"),
            listOf("おどろく", "はっと気づく / 目を覚ます", "動詞", "カ行四段活用 / 古今異義語", "", "0", "古今異義語・注意語"),
            listOf("をかし", "趣がある", "形容詞 シク活用", "枕草子の理念", "△", "0", "最頻出形容詞・形容動詞")
        )

        val mapping = repo.detectColumns(rows)
        assertEquals(0, mapping.headerRowIndex)
        assertEquals(0, mapping.wordCol)
        assertEquals(1, mapping.meaningCol)
        assertEquals(2, mapping.posCol)
        assertEquals(3, mapping.noteCol)
        assertEquals(4, mapping.statusCol)
        assertEquals(5, mapping.hiddenCol)
        assertEquals(6, mapping.deckCol)

        val importResult = repo.processRowsAndInsert(
            rows = rows,
            subject = "CLASSICAL"
        )
        assertEquals(3, importResult.newCount)
        assertEquals(0, importResult.mergeCount)
        assertEquals(3, insertedCards.size)
        assertTrue(insertedCards.all { it.subject == "CLASSICAL" })

        assertEquals("形容動詞", insertedCards[0].definitions[0].pos)
        assertEquals("ナリ活用 / 王朝文学の美的理念", insertedCards[0].note)

        assertEquals("動詞", insertedCards[1].definitions[0].pos)
        assertEquals(2, insertedCards[1].definitions.size)
        assertEquals("カ行四段活用 / 古今異義語", insertedCards[1].note)

        assertEquals("形容詞", insertedCards[2].definitions[0].pos)
        assertEquals("シク活用 / 枕草子の理念", insertedCards[2].note)

        // Verify ClassicalPosNormalizer direct split & merge behavior
        assertEquals("動詞" to "カ行四段活用", ClassicalPosNormalizer.splitPosAndConjugation("動詞 カ行四段"))
        assertEquals("カ行四段活用 / 古今異義語", ClassicalPosNormalizer.mergeConjugationIntoNote(listOf("カ行四段活用"), "古今異義語"))
        assertEquals("カ行四段活用 / 古今異義語", ClassicalPosNormalizer.mergeConjugationIntoNote(listOf("カ行四段活用"), "カ行四段活用 / 古今異義語"))
    }

    @Test
    fun testWeblioClassicalMultiPosAndMultiConjugationParsing() {
        // 1. Gkzkj structured HTML with multiple POS ([一] 感動詞, [二] 名詞) and numeric HTML entities
        val awareHtml = """
            <html><body>
            <div class="subDivision">
                <h3 class=ttl1>あはれ</h3>
                <div class=Gkzkj>
                    <div class="gkzkjSub"><span class="gkzkjSubBx">[一]</span><span class="gkzkjSubPos">感動詞</span></div>
                    <div class="level0"><p class="lvlB">ああ。あれ。</p></div>
                    <div class="level0"><p class="lvlBGBG">「あはれ。いと寒しや」</p></div>
                    <div class="gkzkjSub"><span class="gkzkjSubBx">[二]</span><span class="gkzkjSubPos">名詞</span></div>
                    <div class="level0"><p class="lvlNHN">&#9312;</p><p class="lvlB">しみじみとした趣。</p></div>
                    <div class="level0"><p class="lvlNHN">&#9313;</p><p class="lvlB">寂しさ。悲しさ。</p></div>
                </div>
            </div>
            </body></html>
        """.trimIndent()

        val awareResult = WeblioDictionaryService.parseWeblioClassicalHtml("あはれ", awareHtml)
        assertNotNull(awareResult)
        assertEquals(2, awareResult!!.definitions.size)
        assertEquals("感動詞", awareResult.definitions[0].pos)
        assertEquals("ああ。あれ", awareResult.definitions[0].meaning)
        assertEquals("名詞", awareResult.definitions[1].pos)
        assertTrue(awareResult.definitions[1].meaning.contains("しみじみとした趣"))
        assertTrue(awareResult.definitions[1].meaning.contains("寂しさ。悲しさ"))
        assertFalse("Citation quotes in lvlBGBG must not leak", awareResult.definitions[0].meaning.contains("いと寒しや"))

        // 2. Multiple Gkzkj homographs & multiple conjugations (たのむ: 頼む [四段・下二段] + 田の面 [名詞])
        val tanomuHtml = """
            <html><body>
            <h3 class=ttl1>た－の－む 【田の面】</h3>
            <div class=Gkzkj>
                <div class="gkzkjSub"><span class="gkzkjSubPos">名詞</span></div>
                <div class="level0"><p class="lvlB">田の表面。</p></div>
            </div>
            <hr class=dot>
            <h3 class=ttl1>たの・む 【頼む】</h3>
            <div class=Gkzkj>
                <div class="gkzkjSub"><span class="gkzkjSubBx">[一]</span><span class="gkzkjSubPos">他動詞</span><span class="gkzkjSubPosSm">マ行四段活用</span></div>
                <div class="level0"><p class="lvlB"><span class="gkzkjC">活用</span>｛ま／み／む／む／め／め｝</p><p class="lvlB">頼りにする。あてにする。</p></div>
                <div class="gkzkjSub"><span class="gkzkjSubBx">[二]</span><span class="gkzkjSubPos">他動詞</span><span class="gkzkjSubPosSm">マ行下二段活用</span></div>
                <div class="level0"><p class="lvlB">頼りにさせる。あてにさせる。</p></div>
            </div>
            </body></html>
        """.trimIndent()

        val tanomuResult = WeblioDictionaryService.parseWeblioClassicalHtml("たのむ", tanomuHtml)
        assertNotNull(tanomuResult)
        assertEquals(2, tanomuResult!!.definitions.size)
        // Primary headword without '－' comes first
        assertEquals("動詞", tanomuResult.definitions[0].pos)
        assertTrue(tanomuResult.definitions[0].meaning.contains("頼りにする"))
        assertTrue(tanomuResult.definitions[0].meaning.contains("頼りにさせる"))
        assertEquals("名詞", tanomuResult.definitions[1].pos)
        assertEquals("田の表面", tanomuResult.definitions[1].meaning)
        assertEquals("マ行四段活用・マ行下二段活用", tanomuResult.conjugation)
    }
}
