package com.example

import com.example.data.ocr.EnglishWordValidator
import com.example.data.ocr.OcrExtractor
import com.example.data.ocr.OcrSensitivity
import com.example.data.service.InternalDictionaryService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class OcrSensitivityTest {

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
    fun defaultSensitivity_isHigh() {
        assertEquals(OcrSensitivity.HIGH, OcrSensitivity.fromId("HIGH"))
        assertEquals(OcrSensitivity.HIGH, OcrSensitivity.fromId(null))
        assertEquals(OcrSensitivity.HIGH, OcrSensitivity.fromId("unknown"))
        assertEquals(OcrSensitivity.MEDIUM, OcrSensitivity.fromId("MEDIUM"))
        assertEquals(OcrSensitivity.LOW, OcrSensitivity.fromId("LOW"))
    }

    @Test
    fun allSensitivities_doNotFilterCleanWordsByLengthOrDifficulty() {
        val sample = "The cat and dog run in clean water near a happy family. Era, aid, comprehensive, anticipate!"

        for (level in listOf(OcrSensitivity.HIGH, OcrSensitivity.MEDIUM, OcrSensitivity.LOW)) {
            val words = OcrExtractor.extractMeaningfulWords(sample, level)
            // Short 3-letter words and basic study words must NEVER be dropped just because of length or difficulty
            for (expected in listOf(
                "cat", "dog", "run", "era", "aid",
                "clean", "water", "happy", "family",
                "comprehensive", "anticipate"
            )) {
                assertTrue("Clean word '$expected' must be extracted in $level", words.contains(expected))
            }
            // Pure grammar function words ("the", "and", "in", "a") remain excluded
            assertFalse("Grammar article 'the' should remain excluded in $level", words.contains("the"))
        }
    }

    @Test
    fun englishSensitivityTiers_adjustCharacterInterpretationThresholdInsteadOfLength() {
        val mixedClaritySample = """
            cat achieve
            0124apple imp0rtant lapple ooachieve
            8ui1d 1mp0rtant dimportant clvocabulary
        """.trimIndent()

        val lowWords = OcrExtractor.extractMeaningfulWords(mixedClaritySample, OcrSensitivity.LOW)
        val mediumWords = OcrExtractor.extractMeaningfulWords(mixedClaritySample, OcrSensitivity.MEDIUM)
        val highWords = OcrExtractor.extractMeaningfulWords(mixedClaritySample, OcrSensitivity.HIGH)

        // LOW: Only crisp, uncorrupted alphabetic words are treated as characters (including 3-letter "cat")
        assertTrue("LOW keeps crisp 3-letter 'cat'", lowWords.contains("cat"))
        assertTrue("LOW keeps crisp 'achieve'", lowWords.contains("achieve"))
        assertFalse("LOW rejects digit/box-fused '0124apple'/'lapple'", lowWords.contains("apple"))
        assertFalse("LOW rejects digit-corrupted 'imp0rtant'", lowWords.contains("important"))
        assertFalse("LOW rejects multi-digit smudge '8ui1d'", lowWords.contains("build"))
        assertFalse("LOW rejects checkbox-fused 'clvocabulary'", lowWords.contains("vocabulary"))

        // MEDIUM: Rescues standard 1-digit '0'/'1' misreads and standard box strokes, rejects heavy smudges
        assertTrue("MEDIUM keeps crisp 3-letter 'cat'", mediumWords.contains("cat"))
        assertTrue("MEDIUM rescues '0124apple' / 'lapple' -> 'apple'", mediumWords.contains("apple"))
        assertTrue("MEDIUM rescues 'imp0rtant' -> 'important'", mediumWords.contains("important"))
        assertTrue("MEDIUM rescues 'ooachieve' -> 'achieve'", mediumWords.contains("achieve"))
        assertFalse("MEDIUM rejects heavily smudged '8ui1d'", mediumWords.contains("build"))
        assertFalse("MEDIUM rejects multi-box 'clvocabulary'", mediumWords.contains("vocabulary"))

        // HIGH: Aggressively rescues faint/smudged characters ('8ui1d', '1mp0rtant', 'dimportant', 'clvocabulary')
        assertTrue("HIGH keeps crisp 3-letter 'cat'", highWords.contains("cat"))
        assertTrue("HIGH rescues 'apple'", highWords.contains("apple"))
        assertTrue("HIGH rescues '1mp0rtant' / 'dimportant' -> 'important'", highWords.contains("important"))
        assertTrue("HIGH rescues '8ui1d' -> 'build'", highWords.contains("build"))
        assertTrue("HIGH rescues 'clvocabulary' -> 'vocabulary'", highWords.contains("vocabulary"))
    }

    @Test
    fun classicalSensitivityTiers_adjustCharacterBoundaryWithoutDroppingTwoCharWords() {
        // 1) Crisp 2-character classical words must be extracted even in LOW sensitivity
        val cleanShortClassical = "うし いぬ ふみ つゆ をかし"
        for (level in OcrSensitivity.entries) {
            val words = OcrExtractor.extractClassicalWords(cleanShortClassical, level)
            assertTrue("2-char classical word 'うし' must be extracted in $level", words.contains("うし"))
            assertTrue("2-char classical word 'いぬ' must be extracted in $level", words.contains("いぬ"))
            assertTrue("2-char classical word 'ふみ' must be extracted in $level", words.contains("ふみ"))
            assertTrue("2-char classical word 'つゆ' must be extracted in $level", words.contains("つゆ"))
        }

        // 2) Single OCR box prefix ('口おどろく'): rejected in LOW, rescued in MEDIUM & HIGH
        assertFalse(
            "LOW rejects checkbox-fused '口おどろく'",
            OcrExtractor.extractClassicalWords("口おどろく", OcrSensitivity.LOW).contains("おどろく")
        )
        assertTrue(
            "MEDIUM rescues single-box '口おどろく' -> 'おどろく'",
            OcrExtractor.extractClassicalWords("口おどろく", OcrSensitivity.MEDIUM).contains("おどろく")
        )
        assertTrue(
            "HIGH rescues single-box '口おどろく' -> 'おどろく'",
            OcrExtractor.extractClassicalWords("口おどろく", OcrSensitivity.HIGH).contains("おどろく")
        )

        // 3) Heavy multi-box noise ('口口おどろく') & spaced headwords ('あ は れ な り'): rescued ONLY in HIGH
        val heavyClassicalNoise = "口口おどろく　あ は れ な り"
        val medHeavy = OcrExtractor.extractClassicalWords(heavyClassicalNoise, OcrSensitivity.MEDIUM)
        val highHeavy = OcrExtractor.extractClassicalWords(heavyClassicalNoise, OcrSensitivity.HIGH)

        assertFalse("MEDIUM rejects multi-box '口口おどろく'", medHeavy.contains("おどろく"))
        assertFalse("MEDIUM does not fuse wide-spaced 'あ は れ な り'", medHeavy.contains("あはれなり"))
        assertTrue("HIGH rescues multi-box '口口おどろく' -> 'おどろく'", highHeavy.contains("おどろく"))
        assertTrue("HIGH fuses spaced headword 'あ は れ な り' -> 'あはれなり'", highHeavy.contains("あはれなり"))
    }

    @Test
    fun englishOcr_rejectsNonExistentGibberishEvenInHighSensitivity() {
        val noisyOcr = """
            xqzpt bcdfgh llliii zxvbnm qwrtyp mnbvcx
            adj. adv. vt. vi. prep. conj. iii vii xii
            serendipity ubiquitous negotiate
        """.trimIndent()

        val words = OcrExtractor.extractMeaningfulWords(noisyOcr, OcrSensitivity.HIGH)

        assertFalse("xqzpt gibberish must be rejected", words.contains("xqzpt"))
        assertFalse("bcdfgh gibberish must be rejected", words.contains("bcdfgh"))
        assertFalse("llliii gibberish must be rejected", words.contains("llliii"))
        assertFalse("zxvbnm gibberish must be rejected", words.contains("zxvbnm"))
        assertFalse("qwrtyp gibberish must be rejected", words.contains("qwrtyp"))
        assertFalse("POS tag adj must be rejected", words.contains("adj"))
        assertFalse("POS tag adv must be rejected", words.contains("adv"))
        assertFalse("POS tag prep must be rejected", words.contains("prep"))
        assertFalse("Roman numeral vii must be rejected", words.contains("vii"))

        assertTrue("Real word serendipity must be kept", words.contains("serendipity"))
        assertTrue("Real word ubiquitous must be kept", words.contains("ubiquitous"))
        assertTrue("Real word negotiate must be kept", words.contains("negotiate"))
    }

    @Test
    fun englishOcr_rescuesWordsWithAttachedSurroundingNoise() {
        // Simulates checkboxes ('o', 'v'), vertical borders ('l', 'I'), numbers ('0124'), IPA ('/əˈtʃiːv/'), POS tags ('[動]')
        val noisyWordbookPage = """
            0124 lapple [名] りんご
            0125 importantI /ɪmˈpɔːrtnt/ (形) 重要な
            0126 vachieve [他動] 達成する
            0127 osubstantiall 【形】 かなりの
            0128 lComprehensive [形] 包括的な
        """.trimIndent()

        val words = OcrExtractor.extractMeaningfulWords(noisyWordbookPage, OcrSensitivity.HIGH)

        assertTrue("lapple -> apple should be rescued", words.contains("apple"))
        assertFalse("corrupted lapple should not appear", words.contains("lapple"))

        assertTrue("importantI -> important should be rescued", words.contains("important"))
        assertFalse("corrupted importanti should not appear", words.contains("importanti"))

        assertTrue("vachieve -> achieve should be rescued", words.contains("achieve"))
        assertFalse("corrupted vachieve should not appear", words.contains("vachieve"))

        assertTrue("osubstantiall -> substantial should be rescued", words.contains("substantial"))
        assertFalse("corrupted osubstantiall should not appear", words.contains("osubstantiall"))

        assertTrue("lComprehensive -> comprehensive should be rescued", words.contains("comprehensive"))

        // Direct unit check on validator across sensitivity levels
        assertNull(EnglishWordValidator.resolveCleanEnglishWord("zxqvbn", OcrSensitivity.HIGH))
        assertEquals("negotiate", EnglishWordValidator.resolveCleanEnglishWord("lnegotiateI", OcrSensitivity.HIGH))
        assertNull(EnglishWordValidator.resolveCleanEnglishWord("lnegotiateI", OcrSensitivity.LOW))
    }

    @Test
    fun classicalOcr_stripsSurroundingBoxesTagsAndRejectsNoise() {
        val noisyClassicalPage = """
            □ 015 口おどろく 【動カ四】（目を覚ます・はっと気づく）
            ☑ 016 ロあはれなり ［形動ナリ］ ①しみじみと情趣深い
            ｜017 レをかし｜【形シク】 趣がある
            018 驚（おどろ）く 口口 一目 んっぁ あああ
        """.trimIndent()

        val extracted = OcrExtractor.extractClassicalWords(
            rawText = noisyClassicalPage,
            sensitivity = OcrSensitivity.HIGH,
            filterStopWords = true
        )

        assertTrue("口おどろく should be cleaned to おどろく", extracted.contains("おどろく"))
        assertFalse("corrupted 口おどろく should not appear", extracted.contains("口おどろく"))

        assertTrue("ロあはれなり should be cleaned to あはれなり", extracted.contains("あはれなり"))
        assertFalse("corrupted ロあはれなり should not appear", extracted.contains("ロあはれなり"))

        assertTrue("レをかし｜ should be cleaned to をかし", extracted.contains("をかし"))

        assertFalse("Box Kanji noise 口口 must be rejected", extracted.contains("口口"))
        assertFalse("Box Kanji noise 一目 must be rejected", extracted.contains("一目"))
        assertFalse("Invalid kana noise んっぁ must be rejected", extracted.contains("んっぁ"))
        assertFalse("Repeated kana noise あああ must be rejected", extracted.contains("あああ"))
        assertFalse("Bracketed POS tag 動カ四 must be stripped", extracted.any { it.contains("四段") || it.contains("ナリ") })
    }

    @Test
    fun highSensitivity_reliablyExtractsEdgeLettersParenthesesDigitsAndDerivedWords() {
        val trickyPage = """
            1. (achieve) [vocabulary] (= acquire)
            2. Beautiful, Crucial, Level, Local, Learn, Important, Issue, Object, Value, Join, Radio!
            3. OCR digit glitches: imp0rtant prob1em 1mportant va1ue
            4. Polysemous & basic study words: make take give work people know think
            5. Derived, British & specialized words: deforestation microplastics sustainability realise behaviour anthropocene
            6. Full-width & possessives: ｅｓｓｅｎｔｉａｌ world's doesn't
        """.trimIndent()

        val words = OcrExtractor.extractMeaningfulWords(trickyPage, OcrSensitivity.HIGH)

        // Parenthesized & bracketed target words must NOT be deleted
        assertTrue("(achieve) must be extracted", words.contains("achieve"))
        assertTrue("[vocabulary] must be extracted", words.contains("vocabulary"))
        assertTrue("(= acquire) must extract acquire", words.contains("acquire"))

        // Words starting/ending with l, i, o, v, j must never be truncated
        for (expected in listOf(
            "beautiful", "crucial", "level", "local", "learn",
            "important", "issue", "object", "value", "join", "radio"
        )) {
            assertTrue("$expected must be extracted intact", words.contains(expected))
        }

        // OCR '0'/'1' digit-letter misreads inside words must be rescued
        assertTrue("prob1em -> problem should be rescued", words.contains("problem"))

        // Polysemous & core study words must be picked up in HIGH sensitivity
        for (poly in listOf("make", "take", "give", "work", "people", "know", "think")) {
            assertTrue("Polysemous/study word '$poly' must be picked up in HIGH sensitivity", words.contains(poly))
        }

        // Derived, British, specialized, full-width, and possessive words
        for (adv in listOf("deforestation", "microplastics", "sustainability", "realise", "behaviour", "anthropocene", "essential", "world")) {
            assertTrue("Derived/specialized word '$adv' must be extracted in HIGH", words.contains(adv))
        }
        assertFalse("Contraction fragment 'doesn' must not appear", words.contains("doesn"))
    }

    @Test
    fun classicalOcr_highSensitivityExtractsParenthesizedCompoundsAndDeinflections() {
        val classicalExamText = """
            問１ 空欄（ おどろく ）・（ うとまし ）に入る語を考えよ。
            昔のことを思ひ出づ。山際に立ち寄るもいと心細し。
        """.trimIndent()

        val extracted = OcrExtractor.extractClassicalWords(
            rawText = classicalExamText,
            sensitivity = OcrSensitivity.HIGH,
            filterStopWords = true
        )

        assertTrue("Parenthesized おどろく must be extracted", extracted.contains("おどろく"))
        assertTrue("Parenthesized うとまし must be extracted", extracted.contains("うとまし"))
        assertTrue("Compound classical verb 思ひ出づ / おもひいづ must be extracted", extracted.any { it.contains("思ひ出") || it.contains("おもひい") })
        assertTrue("Compound classical verb 立ち寄る / たちよる must be extracted", extracted.any { it == "立ち寄る" || it == "たちよる" })
        assertTrue("Compound classical adjective 心細し / こころぼそし must be extracted", extracted.any { it == "心細し" || it == "こころぼそし" })

        // Verify classical de-inflection helper generates citation forms for Weblio/KB lookup
        assertTrue(OcrExtractor.deinflectClassicalWord("驚きて").contains("驚く"))
        assertTrue(OcrExtractor.deinflectClassicalWord("悲しく").contains("悲し"))
        assertTrue(OcrExtractor.deinflectClassicalWord("静かなる").contains("静かなり"))
    }

    @Test
    fun englishOcr_stripsCheckboxPrefixesAndRejectsNonDictionaryWordsInHighSensitivity() {
        // Simulates checkboxes □ / □□ / ☑ / ▶ misread as 'd', 'b', 'u', 'oo', 'cl', 'co', 'il', 'O', 'OO', 'D'
        val checkboxWordbookPage = """
            dimportant bachieve ucomprehend
            ooachieve clvocabulary coimportant ilimportant
            OImportant OOAchieve DApple
            aba oca ivi loo anthropocenexyz qwertyuiop
        """.trimIndent()

        val words = OcrExtractor.extractMeaningfulWords(checkboxWordbookPage, OcrSensitivity.HIGH)

        assertTrue("dimportant -> important", words.contains("important"))
        assertTrue("bachieve / ooachieve / OOAchieve -> achieve", words.contains("achieve"))
        assertTrue("ucomprehend -> comprehend", words.contains("comprehend"))
        assertTrue("clvocabulary -> vocabulary", words.contains("vocabulary"))
        assertTrue("DApple -> apple", words.contains("apple"))

        // Leading checkbox characters must NEVER remain attached
        for (corrupted in listOf(
            "dimportant", "bachieve", "ucomprehend", "ooachieve",
            "clvocabulary", "coimportant", "ilimportant", "oimportant", "dapple"
        )) {
            assertFalse("Prefix-corrupted '$corrupted' must not appear", words.contains(corrupted))
        }

        // Non-dictionary & obscure Scrabble checkbox tokens must be strictly rejected even in HIGH sensitivity
        for (bogus in listOf("aba", "oca", "ivi", "loo", "anthropocenexyz", "qwertyuiop")) {
            assertFalse("Non-dictionary token '$bogus' must be rejected", words.contains(bogus))
        }
    }

    @Test
    fun classicalOcr_extractsBracketedHeadwordsHistoricalKanaGuidesAndWordbookEntries() {
        // Simulates real 古文単語帳 formatting: 【headword】, historical kana reading guides あは（わ）れなり,
        // middle dots おどろ・く, and core high-school headwords previously missing
        val wordbookPage = """
            101 【おどろ・く】［動カ四］ ①目を覚ます ②はっと気づく
            102 ［あは（わ）れなり］【形動ナリ】 しみじみと趣深い
            103 を（お）かし 【形シク】 趣がある
            104 いとほ（お）し 【形シク】 気の毒だ
            105 うつくし かなし ありがたし さぶらふ たづぬ
        """.trimIndent()

        val extracted = OcrExtractor.extractClassicalWords(
            rawText = wordbookPage,
            sensitivity = OcrSensitivity.HIGH,
            filterStopWords = true
        )

        for (expected in listOf(
            "おどろく", "あはれなり", "をかし", "いとほし",
            "うつくし", "かなし", "ありがたし", "さぶらふ", "たづぬ"
        )) {
            assertTrue("Classical wordbook entry '$expected' must be extracted", extracted.contains(expected))
        }
    }

    @Test
    fun englishOcr_preservesThinLetterThreeLetterProductivePrefixAndHyphenatedWords() {
        val passageAndWordbook = """
            ability utility visibility initial civilization artificial critical
            significant literature limit little title
            ace apt fad ire ode roe
            unbelievable rediscover nonprofit disadvantage prehistoric outperform
            environ-
            ment coimportant ilimportant dimportant aba oca ivi
        """.trimIndent()

        val words = OcrExtractor.extractMeaningfulWords(passageAndWordbook, OcrSensitivity.HIGH)

        for (expected in listOf(
            "ability", "utility", "visibility", "initial", "civilization", "artificial", "critical",
            "significant", "literature", "limit", "little", "title",
            "ace", "apt", "fad", "ire", "ode", "roe",
            "unbelievable", "rediscover", "nonprofit", "disadvantage", "prehistoric", "outperform",
            "environment", "important"
        )) {
            assertTrue("Expected real English word '$expected' to be extracted", words.contains(expected))
        }

        for (rejected in listOf("coimportant", "ilimportant", "dimportant", "aba", "oca", "ivi", "environ", "ment")) {
            assertFalse("Noise or split fragment '$rejected' must not appear", words.contains(rejected))
        }
    }

    @Test
    fun classicalOcr_extractsSpacedHeadwordsTildeAdverbsAndExpandedWordbookEntries() {
        val classicalWordbookText = """
            201 お ど ろ く　202 あ は れ な り　203 う つ く し
            204 あなづる いまめかし おぼろけなり かたじけなし ためらふ にほふ
            205 やさし やんごとなし らうらうじ をしむ つゆ たえて をさをさ
            206 思ひ遣る 長閑なり
        """.trimIndent()

        val extracted = OcrExtractor.extractClassicalWords(
            rawText = classicalWordbookText,
            sensitivity = OcrSensitivity.HIGH,
            filterStopWords = true
        )

        for (expected in listOf(
            "おどろく", "あはれなり", "うつくし",
            "あなづる", "いまめかし", "おぼろけなり", "かたじけなし", "ためらふ", "にほふ",
            "やさし", "やんごとなし", "らうらうじ", "をしむ", "つゆ", "たえて", "をさをさ",
            "おもひやる", "のどかなり"
        )) {
            assertTrue("Expanded classical word '$expected' must be extracted (got $extracted)", extracted.contains(expected))
        }
    }

    @Test
    fun weblioNotFoundHtmlDetector_distinguishesExplicitNotFoundFromRateLimitPages() {
        assertTrue(
            com.example.data.service.WeblioDictionaryService.isExplicitNotFoundHtml(
                "<html><body>一致する見出し語は見つかりませんでした。</body></html>"
            )
        )
        assertFalse(
            com.example.data.service.WeblioDictionaryService.isExplicitNotFoundHtml(
                "<html><body>Too Many Requests - Please retry shortly</body></html>"
            )
        )
    }

    @Test
    fun englishOcr_extractsIdiomsAndCollocationsInLongReading() {
        val sampleText = """
            Students should take into account all available information and took advantage of educational opportunities.
            We are looking forward to meeting the new scholars who succeed by virtue of their relentless efforts.
            In terms of academic performance, they never give up even when facing difficulties.
        """.trimIndent()

        val extracted = OcrExtractor.extractMeaningfulWords(sampleText, OcrSensitivity.HIGH)

        // Idioms (including conjugated forms) should be detected as multi-word units
        assertTrue("Extracted words should include 'take into account': $extracted", extracted.contains("take into account"))
        assertTrue("Extracted words should include 'take advantage of': $extracted", extracted.contains("take advantage of"))
        assertTrue(
            "Extracted words should include 'look forward to' or 'looking forward to': $extracted",
            extracted.contains("look forward to") || extracted.contains("looking forward to")
        )
        assertTrue("Extracted words should include 'by virtue of': $extracted", extracted.contains("by virtue of"))
        assertTrue("Extracted words should include 'in terms of': $extracted", extracted.contains("in terms of"))
        assertTrue("Extracted words should include 'give up': $extracted", extracted.contains("give up"))

        // Regular individual words should also be included
        assertTrue("Individual words like 'educational' should still be extracted", extracted.contains("educational"))
        assertTrue("Individual words like 'information' should still be extracted", extracted.contains("information"))
    }
}
