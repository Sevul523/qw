package com.example

import com.example.data.service.WeblioDictionaryService
import com.example.data.service.WeblioLookupResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeblioNoteLevelTest {

    @Test
    fun weblioLookupResult_formattedNoteFormat() {
        val resultWithBoth = WeblioLookupResult(
            word = "apple",
            definitions = emptyList(),
            summaryMeaning = "リンゴ",
            level = "レベル1",
            eikenGrade = "英検3級",
            formattedNote = "Weblioレベル1 / 英検3級"
        )
        assertEquals("Weblioレベル1 / 英検3級", resultWithBoth.formattedNote)
        assertEquals("レベル1", resultWithBoth.level)
        assertEquals("英検3級", resultWithBoth.eikenGrade)

        val resultLevelOnly = WeblioLookupResult(
            word = "serendipity",
            definitions = emptyList(),
            summaryMeaning = "思いがけない幸運",
            level = "レベル13",
            eikenGrade = "",
            formattedNote = "Weblioレベル13"
        )
        assertEquals("Weblioレベル13", resultLevelOnly.formattedNote)
        assertTrue(resultLevelOnly.eikenGrade.isEmpty())

        val resultEikenOnly = WeblioLookupResult(
            word = "test",
            definitions = emptyList(),
            summaryMeaning = "テスト",
            level = "",
            eikenGrade = "英検2級",
            formattedNote = "英検2級"
        )
        assertEquals("英検2級", resultEikenOnly.formattedNote)
    }

    @Test
    fun weblioNoteLevel_autoFillLogicBehavior() {
        var autoAddNoteLevelEnabled = true
        var addNote = ""
        var lastAutoFilledNote: String? = null

        // 1. When enabled and lookup returns formattedNote
        val sampleNote = "Weblioレベル5 / 英検2級"
        if (autoAddNoteLevelEnabled && sampleNote.isNotBlank()) {
            if (addNote.isBlank() || addNote == lastAutoFilledNote) {
                addNote = sampleNote
                lastAutoFilledNote = sampleNote
            }
        }
        assertEquals("Weblioレベル5 / 英検2級", addNote)
        assertEquals("Weblioレベル5 / 英検2級", lastAutoFilledNote)

        // 2. When word is cleared, auto-filled note should be cleared
        if (autoAddNoteLevelEnabled && lastAutoFilledNote != null && addNote == lastAutoFilledNote) {
            addNote = ""
            lastAutoFilledNote = null
        }
        assertEquals("", addNote)
        assertEquals(null, lastAutoFilledNote)

        // 3. When user writes a custom note manually, it should NOT be overwritten or auto-cleared
        addNote = "学校の小テスト範囲"
        lastAutoFilledNote = null

        val anotherNote = "Weblioレベル10 / 英検準1級"
        if (autoAddNoteLevelEnabled && anotherNote.isNotBlank()) {
            if (addNote.isBlank() || addNote == lastAutoFilledNote) {
                addNote = anotherNote
                lastAutoFilledNote = anotherNote
            }
        }
        // Custom note must remain untouched!
        assertEquals("学校の小テスト範囲", addNote)
        assertEquals(null, lastAutoFilledNote)

        // 4. When setting is toggled OFF
        autoAddNoteLevelEnabled = false
        addNote = ""
        lastAutoFilledNote = null

        if (autoAddNoteLevelEnabled && sampleNote.isNotBlank()) {
            if (addNote.isBlank() || addNote == lastAutoFilledNote) {
                addNote = sampleNote
                lastAutoFilledNote = sampleNote
            }
        }
        // Disabled: note remains blank!
        assertEquals("", addNote)
        assertEquals(null, lastAutoFilledNote)
    }

    @Test
    fun eikenGradePatterns_recognition() {
        val sampleGrades = listOf("1級", "準1級", "2級", "準2級", "3級", "4級", "5級")
        for (grade in sampleGrades) {
            val text = "英検：${grade}以上合格に覚えておきたい単語"
            val pattern = java.util.regex.Pattern.compile("(1級|準1級|2級|準2級|3級|4級|5級)")
            val matcher = pattern.matcher(text)
            assertTrue("Should find $grade in $text", matcher.find())
            assertEquals(grade, matcher.group(1))
        }
    }

    @Test
    fun parseWeblioHtml_extractsMultiplePartsOfSpeech() {
        val sampleMultiPosHtml = """
            <html>
            <body>
                <div class="learningLevel">
                    <span>レベル：レベル1</span>
                    <span>英検：3級以上合格に覚えておきたい単語</span>
                </div>
                <div class="Kejje">
                    <b class="KejjeOs">pres・ent</b>
                    <div class="level0">
                        <h4 class="ttl2"><span class="txtBox1">【形容詞】</span></h4>
                    </div>
                    <dt class="subTtlNum"><span class="KejjeSubTtlNum">1</span> 【限定用法の形容詞】 現在の，今の，当面の.<br class="clr"></dt>
                    <span class="smpYrLabel">用例</span>
                    <table class="KejjeYr"><tr><td>one's present address 現住所.</td></tr></table>
                    <div class="level0">
                        <h4 class="ttl2"><span class="txtBox1">【名詞】</span></h4>
                    </div>
                    <dt class="subTtlNum"><span class="KejjeSubTtlNum">1</span> [the present] 現今，現在.<br class="clr"></dt>
                    <div class="level0">
                        <h4 class="ttl2"><span class="txtBox1">【語源】</span></h4>
                    </div>
                    <div>ラテン語から</div>
                </div>
                <div class="Kejje">
                    <b class="KejjeOs">pres・ent</b>
                    <div class="level0">
                        <h4 class="ttl2"><span class="txtBox1">【名詞】</span>【可算名詞】</h4>
                    </div>
                    <div class="level0">贈り物，プレゼント; みやげ.<br class="clr"></div>
                </div>
                <div class="Kejje">
                    <b class="KejjeOs">pre・sent</b>
                    <div class="level0">
                        <h4 class="ttl2"><span class="txtBox1">【動詞】</span>【他動詞】</h4>
                    </div>
                    <dt class="subTtlNum"><span class="KejjeSubTtlNum">1</span> a 〈ものを〉贈呈する，贈る.<br class="clr"></dt>
                </div>
            </body>
            </html>
        """.trimIndent()

        val result = WeblioDictionaryService.parseWeblioHtml("present", sampleMultiPosHtml)
        org.junit.Assert.assertNotNull(result)
        assertEquals(3, result!!.definitions.size)

        assertEquals("形容詞", result.definitions[0].pos)
        assertTrue(result.definitions[0].meaning.contains("現在の"))

        assertEquals("名詞", result.definitions[1].pos)
        assertTrue(result.definitions[1].meaning.contains("現在"))
        assertTrue(result.definitions[1].meaning.contains("贈り物"))

        assertEquals("動詞", result.definitions[2].pos)
        assertTrue(result.definitions[2].meaning.contains("贈呈する"))
        assertFalse("Example sentences should not leak into meaning", result.definitions[0].meaning.contains("現住所"))

        assertEquals("Weblioレベル1 / 英検3級", result.formattedNote)
    }

    @Test
    fun parseWeblioHtml_supportsInlineMultiPosFallback() {
        val fallbackHtml = """
            <html>
            <head>
                <meta name="description" content="bookの意味・翻訳・日本語 - 【名詞】本、書籍【動詞】予約する、記帳する｜Weblio英和・和英辞書" />
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val result = WeblioDictionaryService.parseWeblioHtml("book", fallbackHtml)
        org.junit.Assert.assertNotNull(result)
        assertEquals(2, result!!.definitions.size)
        assertEquals("名詞", result.definitions[0].pos)
        assertEquals("本、書籍", result.definitions[0].meaning)
        assertEquals("動詞", result.definitions[1].pos)
        assertEquals("予約する、記帳する", result.definitions[1].meaning)
    }
}
