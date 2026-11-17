package com.example

import com.example.data.local.FlashcardDao
import com.example.data.local.FlashcardEntity
import com.example.data.repository.FlashcardRepository
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class XlsxImportUnitTest {

    private fun createDummyRepository(): FlashcardRepository {
        val dummyDao = object : FlashcardDao {
            override fun getAllCards() = flowOf(emptyList<FlashcardEntity>())
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
            override fun getDistinctDecks() = flowOf(emptyList<String>())
            override suspend fun delete(card: FlashcardEntity) {}
            override suspend fun deleteById(id: Long) {}
            override suspend fun deleteBatchByIds(ids: List<Long>) {}
            override suspend fun deleteByDeck(deck: String) {}
            override suspend fun deleteAll() {}
        }
        return FlashcardRepository(dummyDao)
    }

    @Test
    fun columnLetterToIndex_calculatesCorrectly() {
        val repo = createDummyRepository()
        assertEquals(0, repo.columnLetterToIndex("A1"))
        assertEquals(1, repo.columnLetterToIndex("B1"))
        assertEquals(2, repo.columnLetterToIndex("C1"))
        assertEquals(25, repo.columnLetterToIndex("Z1"))
        assertEquals(26, repo.columnLetterToIndex("AA1"))
        assertEquals(27, repo.columnLetterToIndex("AB1"))
        // Absolute reference with $
        assertEquals(0, repo.columnLetterToIndex("\$A\$1"))
        assertEquals(1, repo.columnLetterToIndex("\$B\$5"))
    }

    @Test
    fun detectColumns_withStandardHeaders() {
        val repo = createDummyRepository()
        val rows = listOf(
            listOf("単語", "意味", "品詞", "備考"),
            listOf("apple", "りんご", "名詞", "果物")
        )
        val mapping = repo.detectColumns(rows)
        assertEquals(0, mapping.headerRowIndex)
        assertEquals(0, mapping.wordCol)
        assertEquals(1, mapping.meaningCol)
        assertEquals(2, mapping.posCol)
        assertEquals(3, mapping.noteCol)
    }

    @Test
    fun detectColumns_withNumberColumnHeader() {
        val repo = createDummyRepository()
        val rows = listOf(
            listOf("No.", "英語", "日本語訳", "品詞", "例文"),
            listOf("1", "run", "走る", "動詞", "He runs fast.")
        )
        val mapping = repo.detectColumns(rows)
        assertEquals(0, mapping.headerRowIndex)
        assertEquals(1, mapping.wordCol)
        assertEquals(2, mapping.meaningCol)
        assertEquals(3, mapping.posCol)
        assertEquals(4, mapping.noteCol)
    }

    @Test
    fun detectColumns_withEnglishHeaders() {
        val repo = createDummyRepository()
        val rows = listOf(
            listOf("Word", "Meaning", "POS", "Note"),
            listOf("book", "本", "名詞", "")
        )
        val mapping = repo.detectColumns(rows)
        assertEquals(0, mapping.headerRowIndex)
        assertEquals(0, mapping.wordCol)
        assertEquals(1, mapping.meaningCol)
    }

    @Test
    fun detectColumns_withoutHeader_numericFirstColumn() {
        val repo = createDummyRepository()
        val rows = listOf(
            listOf("1", "cat", "猫", "名詞"),
            listOf("2", "dog", "犬", "名詞")
        )
        val mapping = repo.detectColumns(rows)
        assertEquals(-1, mapping.headerRowIndex)
        assertEquals(1, mapping.wordCol)
        assertEquals(2, mapping.meaningCol)
    }

    @Test
    fun parseSharedStringsXml_stripsFurigana() {
        val repo = createDummyRepository()
        val xml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="2" uniqueCount="2">
                <si>
                    <t>走る</t>
                    <rPh sb="0" eb="2"><t>はしる</t></rPh>
                    <phoneticPr fontId="1"/>
                </si>
                <si>
                    <r>
                        <rPr><b/></rPr>
                        <t>学校</t>
                    </r>
                    <rPh sb="0" eb="2"><t>がっこう</t></rPh>
                </si>
            </sst>
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val strings = repo.parseSharedStringsXml(xml)
        assertEquals(2, strings.size)
        assertEquals("走る", strings[0])
        assertEquals("学校", strings[1])
    }

    @Test
    fun parseSheetXml_extractsCellValues() {
        val repo = createDummyRepository()
        val sharedStrings = listOf("apple", "りんご", "名詞", "赤くて甘い")
        val sheetXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <sheetData>
                    <row r="1">
                        <c r="A1" t="s"><v>0</v></c>
                        <c r="B1" t="s"><v>1</v></c>
                        <c r="C1" t="s"><v>2</v></c>
                        <c r="D1" t="s"><v>3</v></c>
                    </row>
                    <row r="2">
                        <c r="A2" t="inlineStr"><is><t>banana</t></is></c>
                        <c r="B2" t="inlineStr"><is><t>バナナ</t></is></c>
                        <c r="C2" t="inlineStr"><is><t>名詞</t></is></c>
                        <c r="D2"><v>1.0</v></c>
                    </row>
                </sheetData>
            </worksheet>
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val rows = repo.parseSheetXml(sheetXml, sharedStrings)
        assertEquals(2, rows.size)

        // Row 1
        assertEquals("apple", rows[0][0])
        assertEquals("りんご", rows[0][1])
        assertEquals("名詞", rows[0][2])
        assertEquals("赤くて甘い", rows[0][3])

        // Row 2
        assertEquals("banana", rows[1][0])
        assertEquals("バナナ", rows[1][1])
        assertEquals("名詞", rows[1][2])
        assertEquals("1", rows[1][3]) // 1.0 stripped to 1
    }

    @Test
    fun parseXlsxStream_parsesSyntheticXlsx() {
        val repo = createDummyRepository()

        // Create an in-memory ZIP simulating a .xlsx file
        val sharedStringsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <si><t>単語</t></si>
                <si><t>意味</t></si>
                <si><t>品詞</t></si>
                <si><t>computer</t></si>
                <si><t>コンピューター</t></si>
                <si><t>名詞</t></si>
            </sst>
        """.trimIndent()

        val sheet1Xml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <sheetData>
                    <row r="1">
                        <c r="A1" t="s"><v>0</v></c>
                        <c r="B1" t="s"><v>1</v></c>
                        <c r="C1" t="s"><v>2</v></c>
                    </row>
                    <row r="2">
                        <c r="A2" t="s"><v>3</v></c>
                        <c r="B2" t="s"><v>4</v></c>
                        <c r="C2" t="s"><v>5</v></c>
                    </row>
                </sheetData>
            </worksheet>
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zos.write(sharedStringsXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zos.write(sheet1Xml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val zipBytes = baos.toByteArray()
        val parsedRows = repo.parseXlsxStream(ByteArrayInputStream(zipBytes))

        assertEquals(2, parsedRows.size)
        assertEquals(listOf("単語", "意味", "品詞"), parsedRows[0])
        assertEquals(listOf("computer", "コンピューター", "名詞"), parsedRows[1])
    }

    @Test
    fun sanitizeFurigana_removesReadingAnnotations() {
        val repo = createDummyRepository()
        assertEquals("走る", repo.sanitizeFurigana("走る(はしる)"))
        assertEquals("食べる", repo.sanitizeFurigana("食べる（たべる）"))
        assertEquals("学校", repo.sanitizeFurigana("学校[がっこう]"))
        assertEquals("本", repo.sanitizeFurigana("本［ほん］"))
    }
}
