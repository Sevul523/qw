package com.example.data.repository

import com.example.data.local.FlashcardDao
import com.example.data.local.FlashcardEntity
import com.example.data.model.ClassicalPosNormalizer
import com.example.data.model.ClassicalWordKnowledgeBase
import com.example.data.model.Definition
import com.example.data.model.PartsOfSpeech
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

enum class DuplicateStrategy {
    SKIP_DUPLICATES,     // その重複している単語だけを登録しない（スキップ）
    MERGE_DUPLICATES,    // 重複している単語は既存カードに意味を統合
    FORCE_ADD_ALL        // 重複していても別カードとして全て新規登録
}

data class ImportResult(
    val newCount: Int,
    val mergeCount: Int,
    val skippedCount: Int = 0,
    val skippedWords: List<String> = emptyList()
)

class FlashcardRepository(private val flashcardDao: FlashcardDao) {

    fun getAllCards(): Flow<List<FlashcardEntity>> = flashcardDao.getAllCards()

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        if (flashcardDao.getCount() == 0) {
            val sampleCards = listOf(
                FlashcardEntity(
                    word = "run",
                    definitions = listOf(
                        Definition("動詞", "走る、作動する、経営する、立候補する"),
                        Definition("名詞", "走ること、運行、連続記録")
                    ),
                    deck = "日常英会話",
                    note = "最重要基本多義語",
                    status = "○",
                    repetitionLevel = 3,
                    intervalDays = 7,
                    nextReviewTimestamp = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000L
                ),
                FlashcardEntity(
                    word = "book",
                    definitions = listOf(
                        Definition("名詞", "本、書籍、帳簿"),
                        Definition("動詞", "予約する、記録する")
                    ),
                    deck = "日常英会話",
                    note = "日常会話・TOEIC必須",
                    status = "○",
                    repetitionLevel = 2,
                    intervalDays = 3,
                    nextReviewTimestamp = System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000L
                ),
                FlashcardEntity(
                    word = "present",
                    definitions = listOf(
                        Definition("名詞", "贈り物、現在"),
                        Definition("形容詞", "出席している、現在の"),
                        Definition("動詞", "提示する、贈呈する、発表する")
                    ),
                    deck = "TOEIC頻出",
                    note = "名詞/形容詞は前、動詞は後ろにアクセント",
                    status = "△",
                    repetitionLevel = 1,
                    intervalDays = 1,
                    nextReviewTimestamp = System.currentTimeMillis() // 期限到来
                ),
                FlashcardEntity(
                    word = "light",
                    definitions = listOf(
                        Definition("名詞", "光、明かり、視点"),
                        Definition("形容詞", "明るい、軽い、わずかな"),
                        Definition("動詞", "火をつける、照らす")
                    ),
                    deck = "日常英会話",
                    note = "",
                    status = "×",
                    repetitionLevel = 0,
                    intervalDays = 0,
                    nextReviewTimestamp = System.currentTimeMillis() // 期限到来
                ),
                FlashcardEntity(
                    word = "address",
                    definitions = listOf(
                        Definition("名詞", "住所、演説、挨拶"),
                        Definition("動詞", "（問題に）取り組む、話しかける")
                    ),
                    deck = "ビジネス英語",
                    note = "TOEIC頻出動詞",
                    status = null,
                    repetitionLevel = 0,
                    intervalDays = 0,
                    nextReviewTimestamp = 0L
                ),
                FlashcardEntity(
                    word = "novel",
                    definitions = listOf(
                        Definition("名詞", "長編小説"),
                        Definition("形容詞", "斬新な、新奇な")
                    ),
                    deck = "大学受験",
                    note = "形容詞の意味を見落としやすい",
                    status = null,
                    repetitionLevel = 0,
                    intervalDays = 0,
                    nextReviewTimestamp = 0L
                )
            )
            flashcardDao.insertAll(sampleCards)
        }
    }

    suspend fun seedClassicalJapaneseDataIfEmpty() = withContext(Dispatchers.IO) {
        val all = flashcardDao.getAllOnce()
        val classicalCards = all.filter { it.subject.equals("CLASSICAL", ignoreCase = true) }
        if (classicalCards.isEmpty()) {
            val sampleCards = ClassicalWordKnowledgeBase.createInitialClassicalCards()
            flashcardDao.insertAll(sampleCards)
        } else {
            normalizeExistingClassicalCardsInternal(classicalCards)
        }
    }

    /**
     * 既存の古語カードに「動詞 カ行四段」「形容詞 シク活用」「形容動詞 ナリ活用」などの旧品詞が含まれている場合、
     * 品詞を「動詞」「形容詞」「形容動詞」等にシンプル化し、活用の種類を備考欄へ自動移行する。
     */
    suspend fun normalizeExistingClassicalCards() = withContext(Dispatchers.IO) {
        val classicalCards = flashcardDao.getAllOnce().filter {
            it.subject.equals("CLASSICAL", ignoreCase = true)
        }
        if (classicalCards.isNotEmpty()) {
            normalizeExistingClassicalCardsInternal(classicalCards)
        }
    }

    private suspend fun normalizeExistingClassicalCardsInternal(classicalCards: List<FlashcardEntity>) {
        for (card in classicalCards) {
            val extractedConjs = mutableListOf<String>()
            val normalizedDefs = card.definitions.map { def ->
                val (simplePos, conj) = ClassicalPosNormalizer.splitPosAndConjugation(def.pos)
                if (conj.isNotBlank()) {
                    extractedConjs.add(conj)
                }
                def.copy(pos = simplePos)
            }
            if (extractedConjs.isEmpty() && normalizedDefs.any { it.pos in listOf("動詞", "形容詞", "形容動詞") }) {
                val kbConj = ClassicalWordKnowledgeBase.lookup(card.word)?.conjugation.orEmpty()
                if (kbConj.isNotBlank()) {
                    extractedConjs.add(kbConj)
                }
            }
            val updatedNote = ClassicalPosNormalizer.mergeConjugationIntoNote(extractedConjs, card.note)
            if (normalizedDefs != card.definitions || updatedNote != card.note) {
                flashcardDao.update(
                    card.copy(
                        definitions = normalizedDefs,
                        note = updatedNote
                    )
                )
            }
        }
    }

    suspend fun getAllOnce(): List<FlashcardEntity> = withContext(Dispatchers.IO) {
        flashcardDao.getAllOnce()
    }

    suspend fun getCardCount(): Int = withContext(Dispatchers.IO) {
        flashcardDao.getCount()
    }

    suspend fun getCardById(id: Long): FlashcardEntity? = withContext(Dispatchers.IO) {
        flashcardDao.getById(id)
    }

    suspend fun findCardByWord(word: String): FlashcardEntity? = withContext(Dispatchers.IO) {
        flashcardDao.getByWord(word.trim())
    }

    suspend fun insertCard(card: FlashcardEntity): Long = withContext(Dispatchers.IO) {
        flashcardDao.insert(card)
    }

    suspend fun updateCard(card: FlashcardEntity) = withContext(Dispatchers.IO) {
        flashcardDao.update(card)
    }

    suspend fun updateStatus(id: Long, status: String?) = withContext(Dispatchers.IO) {
        flashcardDao.updateStatus(id, status)
    }

    suspend fun updateStatusAndConsecutive(id: Long, status: String?, consecutiveCount: Int) = withContext(Dispatchers.IO) {
        flashcardDao.updateStatusAndConsecutive(id, status, consecutiveCount)
    }

    suspend fun updateHidden(id: Long, isHidden: Boolean) = withContext(Dispatchers.IO) {
        flashcardDao.updateHidden(id, isHidden)
    }

    suspend fun updateBatchHidden(ids: List<Long>, isHidden: Boolean) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) {
            flashcardDao.updateBatchHidden(ids, isHidden)
        }
    }

    fun getDistinctDecks(): Flow<List<String>> = flashcardDao.getDistinctDecks()

    suspend fun updateDeck(id: Long, deck: String) = withContext(Dispatchers.IO) {
        flashcardDao.updateDeck(id, deck)
    }

    suspend fun updateBatchDeck(ids: List<Long>, deck: String) = withContext(Dispatchers.IO) {
        flashcardDao.updateBatchDeck(ids, deck)
    }

    suspend fun updateSrsRating(id: Long, rating: String) = withContext(Dispatchers.IO) {
        val card = flashcardDao.getById(id) ?: return@withContext
        val now = System.currentTimeMillis()
        val newConsecutive = if (rating == "○") card.consecutivePerfectCount + 1 else 0

        val (newLevel, intervalDays) = when (rating) {
            "○" -> {
                val nextLevel = (card.repetitionLevel + 1).coerceAtMost(10)
                val interval = when (nextLevel) {
                    1 -> 1
                    2 -> 3
                    3 -> 7
                    4 -> 14
                    5 -> 30
                    6 -> 60
                    else -> ((card.intervalDays.takeIf { it > 0 } ?: 60) * 2).coerceAtMost(180)
                }
                nextLevel to interval
            }
            "△" -> {
                val nextLevel = maxOf(1, card.repetitionLevel)
                nextLevel to 1 // 復習は明日
            }
            else -> { // "×"
                0 to 0 // 今日即座に復習対象
            }
        }

        val nextReview = if (intervalDays == 0) {
            now
        } else {
            now + intervalDays * 24L * 60 * 60 * 1000L
        }

        flashcardDao.updateSrsStatus(
            id = id,
            status = rating,
            consecutiveCount = newConsecutive,
            level = newLevel,
            interval = intervalDays,
            nextReview = nextReview,
            lastReviewed = now
        )
    }

    suspend fun bulkImportWords(
        words: List<String>,
        targetDeck: String = "日常英会話",
        enableWeblioFallback: Boolean = false,
        enableWeblioNoteLevel: Boolean = false,
        enableWeblioClassicalConjugation: Boolean = false,
        subject: String = "ENGLISH"
    ): Int = withContext(Dispatchers.IO) {
        var addedCount = 0
        val existingCards = flashcardDao.getAllOnce()
            .filter { it.subject.equals(subject, ignoreCase = true) }
            .associateBy { it.word.lowercase() }
        val toInsert = mutableListOf<FlashcardEntity>()

        for (rawWord in words) {
            val cleanWord = rawWord.trim().lowercase()
            if (cleanWord.isBlank() || existingCards.containsKey(cleanWord)) continue

            val newEntity = if (subject.equals("CLASSICAL", ignoreCase = true)) {
                val weblioClassical = if (enableWeblioFallback) {
                    com.example.data.service.WeblioDictionaryService.lookupClassicalWord(rawWord.trim())
                } else {
                    null
                }
                val classicalKnowledge = ClassicalWordKnowledgeBase.lookup(cleanWord)
                when {
                    weblioClassical != null && weblioClassical.definitions.isNotEmpty() -> {
                        FlashcardEntity(
                            word = rawWord.trim(),
                            definitions = weblioClassical.definitions,
                            deck = targetDeck,
                            note = if (enableWeblioClassicalConjugation) weblioClassical.conjugation else "",
                            subject = subject
                        )
                    }
                    classicalKnowledge != null -> {
                        val fallbackNote = if (enableWeblioClassicalConjugation) {
                            classicalKnowledge.conjugation
                        } else {
                            ""
                        }
                        FlashcardEntity(
                            word = classicalKnowledge.word,
                            definitions = classicalKnowledge.definitions,
                            deck = targetDeck,
                            note = fallbackNote,
                            subject = subject
                        )
                    }
                    else -> {
                        FlashcardEntity(
                            word = rawWord.trim(),
                            definitions = listOf(Definition("その他", "（未設定の意味）")),
                            deck = targetDeck,
                            note = "",
                            subject = subject
                        )
                    }
                }
            } else {
                val weblio = if (enableWeblioFallback) {
                    com.example.data.service.WeblioDictionaryService.lookupWord(cleanWord)
                } else {
                    null
                }
                val knowledge = com.example.data.model.WordKnowledgeBase.lookup(cleanWord)
                when {
                    weblio != null && weblio.definitions.isNotEmpty() -> {
                        FlashcardEntity(
                            word = rawWord.trim(),
                            definitions = weblio.definitions,
                            deck = targetDeck,
                            note = if (enableWeblioNoteLevel) weblio.formattedNote else "",
                            subject = subject
                        )
                    }
                    knowledge != null -> {
                        FlashcardEntity(
                            word = knowledge.word,
                            definitions = knowledge.definitions,
                            deck = targetDeck,
                            note = knowledge.note,
                            subject = subject
                        )
                    }
                    else -> {
                        FlashcardEntity(
                            word = rawWord.trim(),
                            definitions = listOf(Definition("名詞", "（未設定の意味）")),
                            deck = targetDeck,
                            subject = subject
                        )
                    }
                }
            }
            toInsert.add(newEntity)
            addedCount++
        }

        if (toInsert.isNotEmpty()) {
            flashcardDao.insertAll(toInsert)
        }
        addedCount
    }

    suspend fun insertCards(cards: List<FlashcardEntity>): Int = withContext(Dispatchers.IO) {
        if (cards.isEmpty()) return@withContext 0
        val inserted = flashcardDao.insertAll(cards)
        inserted.size
    }

    suspend fun deleteCard(card: FlashcardEntity) = withContext(Dispatchers.IO) {
        flashcardDao.delete(card)
    }

    suspend fun deleteBatchByIds(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) {
            flashcardDao.deleteBatchByIds(ids)
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        flashcardDao.deleteAll()
    }

    /**
     * Sanitizes input strings from CSV, TSV, or XLSX:
     * - Strips furigana / reading annotations enclosed in parentheses: (ふりがな), （ふりがな）, [ふりがな]
     * - Normalizes full-width/half-width brackets and whitespace
     */
    fun sanitizeFurigana(text: String): String {
        return text
            .replace(Regex("[\\(（\\[［][^()（）\\[\\]［］]*[\\)）\\]］]"), "")
            .trim()
    }

    /**
     * Parses delimited text (CSV, TSV, etc.) into rows of cells.
     * Supports quoted fields, multiline cells, and comma/tab/semicolon separators.
     */
    fun parseDelimitedText(text: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        if (text.isBlank()) return result

        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: ""
        val isTab = firstLine.contains('\t')
        val isSemi = !isTab && firstLine.contains(';') && !firstLine.contains(',')
        val delimiter = when {
            isTab -> '\t'
            isSemi -> ';'
            else -> ','
        }

        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < text.length && text[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    currentRow.add(currentField.toString().trim())
                    currentField.clear()
                }
                (c == '\r' || c == '\n') && !inQuotes -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') {
                        i++
                    }
                    currentRow.add(currentField.toString().trim())
                    currentField.clear()
                    if (currentRow.any { it.isNotEmpty() }) {
                        rows.add(currentRow.toList())
                    }
                    currentRow.clear()
                }
                else -> {
                    currentField.append(c)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotEmpty() }) {
                rows.add(currentRow.toList())
            }
        }

        return rows
    }

    suspend fun importBulk(
        text: String,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.MERGE_DUPLICATES,
        targetDeck: String? = null,
        subject: String = "ENGLISH"
    ): ImportResult = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ImportResult(0, 0, 0)

        val rows = parseDelimitedText(text)
        processRowsAndInsert(rows, duplicateStrategy, targetDeck, subject)
    }

    suspend fun renameDeck(oldDeck: String, newDeck: String) = withContext(Dispatchers.IO) {
        flashcardDao.renameDeck(oldDeck, newDeck)
    }

    suspend fun deleteByDeck(deck: String) = withContext(Dispatchers.IO) {
        flashcardDao.deleteByDeck(deck)
    }

    data class ColumnMapping(
        val headerRowIndex: Int, // -1 if no header row found
        val wordCol: Int,
        val meaningCol: Int,
        val posCol: Int,
        val noteCol: Int,
        val statusCol: Int = -1,
        val hiddenCol: Int = -1,
        val deckCol: Int = -1
    )

    /**
     * Imports from XLSX InputStream directly, extracting shared strings without phonetic/furigana runs (<rPh>).
     */
    suspend fun importXlsx(
        inputStream: InputStream,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.MERGE_DUPLICATES,
        targetDeck: String? = null,
        subject: String = "ENGLISH"
    ): ImportResult = withContext(Dispatchers.IO) {
        val rows = parseXlsxStream(inputStream)
        if (rows.isEmpty()) {
            throw IllegalArgumentException("Excelファイルからデータを読み取れませんでした。空のシートか無効な形式です。")
        }
        processRowsAndInsert(rows, duplicateStrategy, targetDeck, subject)
    }

    fun parseXlsxStream(inputStream: InputStream): List<List<String>> {
        val sharedStrings = mutableListOf<String>()
        val sheetMap = mutableMapOf<String, ByteArray>()

        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val normalizedName = entry.name.replace('\\', '/')
                when {
                    normalizedName.equals("xl/sharedStrings.xml", ignoreCase = true) -> {
                        sharedStrings.addAll(parseSharedStringsXml(zis.readBytes()))
                    }
                    normalizedName.startsWith("xl/worksheets/", ignoreCase = true) &&
                        normalizedName.endsWith(".xml", ignoreCase = true) -> {
                        sheetMap[normalizedName] = zis.readBytes()
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (sheetMap.isEmpty()) return emptyList()

        // Sort sheets to prefer sheet1.xml first, then sheet2.xml, etc.
        val sortedSheets = sheetMap.entries.sortedWith { a, b ->
            when {
                a.key.contains("sheet1.xml", ignoreCase = true) -> -1
                b.key.contains("sheet1.xml", ignoreCase = true) -> 1
                else -> a.key.compareTo(b.key)
            }
        }

        val allRows = mutableListOf<List<String>>()
        for ((_, sheetBytes) in sortedSheets) {
            val sheetRows = parseSheetXml(sheetBytes, sharedStrings)
            if (sheetRows.isNotEmpty()) {
                allRows.addAll(sheetRows)
            }
        }

        return allRows
    }

    fun parseSharedStringsXml(xmlBytes: ByteArray): List<String> {
        val list = mutableListOf<String>()
        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                try {
                    setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
                    setFeature("http://xml.org/sax/features/external-general-entities", false)
                    setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                } catch (_: Exception) {}
            }
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(xmlBytes.inputStream())

            // Find all <si> tags regardless of prefix
            val allElements = doc.getElementsByTagName("*")
            for (i in 0 until allElements.length) {
                val el = allElements.item(i) as? Element ?: continue
                val local = el.tagName.substringAfter(':').lowercase()
                if (local == "si") {
                    val sb = StringBuilder()
                    val childNodes = el.childNodes
                    for (j in 0 until childNodes.length) {
                        val child = childNodes.item(j)
                        if (child.nodeType == Node.ELEMENT_NODE) {
                            val childEl = child as Element
                            val childTag = childEl.tagName.substringAfter(':').lowercase()
                            // Skip phonetic/furigana runs
                            if (childTag == "rph" || childTag == "phoneticpr") continue

                            when (childTag) {
                                "t" -> sb.append(childEl.textContent)
                                "r" -> {
                                    val tNodes = childEl.getElementsByTagName("*")
                                    for (k in 0 until tNodes.length) {
                                        val tEl = tNodes.item(k) as? Element ?: continue
                                        val tTag = tEl.tagName.substringAfter(':').lowercase()
                                        if (tTag == "t") {
                                            sb.append(tEl.textContent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    list.add(sb.toString())
                }
            }
        } catch (e: Exception) {
            // Fallback or log if needed
        }
        return list
    }

    fun parseSheetXml(xmlBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val result = mutableListOf<List<String>>()
        try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                try {
                    setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
                    setFeature("http://xml.org/sax/features/external-general-entities", false)
                    setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                } catch (_: Exception) {}
            }
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(xmlBytes.inputStream())

            val allElements = doc.getElementsByTagName("*")
            for (i in 0 until allElements.length) {
                val rowEl = allElements.item(i) as? Element ?: continue
                val rowTag = rowEl.tagName.substringAfter(':').lowercase()
                if (rowTag != "row") continue

                val rowValues = mutableMapOf<Int, String>()
                var maxCol = -1
                var sequentialCol = 0

                val childNodes = rowEl.childNodes
                for (j in 0 until childNodes.length) {
                    val cEl = childNodes.item(j) as? Element ?: continue
                    val cTag = cEl.tagName.substringAfter(':').lowercase()
                    if (cTag != "c") continue

                    val rAttr = cEl.getAttribute("r") // e.g., "A1", "B2", "$A$1"
                    val parsedCol = if (rAttr.isNotBlank()) columnLetterToIndex(rAttr) else -1
                    val colIndex = if (parsedCol >= 0) {
                        sequentialCol = parsedCol + 1
                        parsedCol
                    } else {
                        sequentialCol++
                    }

                    val type = cEl.getAttribute("t").lowercase()
                    var cellValue = ""

                    // Look for <v> or <is>
                    val cellChildren = cEl.childNodes
                    for (k in 0 until cellChildren.length) {
                        val subEl = cellChildren.item(k) as? Element ?: continue
                        val subTag = subEl.tagName.substringAfter(':').lowercase()
                        when (subTag) {
                            "v" -> {
                                val rawV = subEl.textContent
                                cellValue = when (type) {
                                    "s" -> {
                                        val sIdx = rawV.toIntOrNull()
                                        if (sIdx != null && sIdx in sharedStrings.indices) {
                                            sharedStrings[sIdx]
                                        } else {
                                            rawV
                                        }
                                    }
                                    "b" -> if (rawV == "1") "TRUE" else "FALSE"
                                    else -> {
                                        // If integer with .0, strip .0 (e.g. "1.0" -> "1")
                                        if (rawV.endsWith(".0") && rawV.substringBefore(".0").all { it.isDigit() }) {
                                            rawV.substringBefore(".0")
                                        } else {
                                            rawV
                                        }
                                    }
                                }
                            }
                            "is" -> {
                                // Inline string: can contain <t> or <r><t>
                                val sb = StringBuilder()
                                val isElements = subEl.getElementsByTagName("*")
                                for (m in 0 until isElements.length) {
                                    val item = isElements.item(m) as? Element ?: continue
                                    val itemTag = item.tagName.substringAfter(':').lowercase()
                                    if (itemTag == "t") {
                                        sb.append(item.textContent)
                                    }
                                }
                                if (sb.isNotEmpty()) {
                                    cellValue = sb.toString()
                                }
                            }
                        }
                    }

                    if (colIndex >= 0) {
                        rowValues[colIndex] = cellValue
                        if (colIndex > maxCol) maxCol = colIndex
                    }
                }

                if (maxCol >= 0) {
                    val rowList = ArrayList<String>(maxCol + 1)
                    for (col in 0..maxCol) {
                        rowList.add(rowValues[col] ?: "")
                    }
                    // Only add if row has at least one non-blank cell
                    if (rowList.any { it.isNotBlank() }) {
                        result.add(rowList)
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    fun columnLetterToIndex(cellRef: String): Int {
        val clean = cellRef.trim().trimStart('$')
        var col = 0
        var foundChar = false
        for (c in clean.uppercase()) {
            if (c in 'A'..'Z') {
                foundChar = true
                col = col * 26 + (c - 'A' + 1)
            } else {
                break
            }
        }
        return if (foundChar) col - 1 else -1
    }

    /**
     * Determines column indices for Word, Meaning, POS, Note, Status, Hidden based on header row or data heuristic.
     */
    fun detectColumns(rows: List<List<String>>): ColumnMapping {
        val wordKeywords = listOf("単語", "古語", "古文単語", "word", "英語", "english", "見出し", "見出し語", "vocabulary", "vocab", "term", "front")
        val meaningKeywords = listOf("意味", "現代語訳", "口語訳", "meaning", "日本語", "japanese", "訳", "和訳", "意味・訳", "definition", "back", "内容")
        val posKeywords = listOf("品詞", "pos", "part of speech", "品詞名")
        val noteKeywords = listOf("備考", "note", "メモ", "例文", "example", "remarks", "解説", "補足", "詳細", "comment")
        val statusKeywords = listOf("ステータス", "status", "学習状況", "学習データ", "評価", "習熟度", "判定", "進捗", "理解度", "マーク", "記号", "state", "学習")
        val hiddenKeywords = listOf("非表示", "hidden", "除外", "非表示フラグ", "is_hidden")
        val deckKeywords = listOf("デッキ", "deck", "カテゴリー", "category", "分類", "フォルダ", "folder", "グループ", "group")
        val numberKeywords = listOf("no", "no.", "番号", "id", "連番", "ナンバリング")

        // Search the first 5 rows for a header
        for (i in 0 until minOf(5, rows.size)) {
            val row = rows[i].map { it.trim().lowercase() }
            val hasWordHeader = row.any { cell -> wordKeywords.any { kw -> cell == kw || cell.startsWith(kw) } }
            val hasMeaningHeader = row.any { cell -> meaningKeywords.any { kw -> cell == kw || cell.startsWith(kw) } }

            if (hasWordHeader || hasMeaningHeader) {
                var wordCol = -1
                var meaningCol = -1
                var posCol = -1
                var noteCol = -1
                var statusCol = -1
                var hiddenCol = -1
                var deckCol = -1

                for (cIdx in row.indices) {
                    val cell = row[cIdx]
                    if (cell.isBlank()) continue
                    when {
                        wordCol == -1 && wordKeywords.any { kw -> cell == kw || cell.startsWith(kw) } -> wordCol = cIdx
                        meaningCol == -1 && meaningKeywords.any { kw -> cell == kw || cell.startsWith(kw) } -> meaningCol = cIdx
                        posCol == -1 && posKeywords.any { kw -> cell == kw || cell.startsWith(kw) } -> posCol = cIdx
                        noteCol == -1 && noteKeywords.any { kw -> cell == kw || cell.startsWith(kw) } -> noteCol = cIdx
                        statusCol == -1 && statusKeywords.any { kw -> cell == kw || cell.startsWith(kw) } -> statusCol = cIdx
                        hiddenCol == -1 && hiddenKeywords.any { kw -> cell == kw || cell.startsWith(kw) } -> hiddenCol = cIdx
                        deckCol == -1 && deckKeywords.any { kw -> cell == kw || cell.startsWith(kw) } -> deckCol = cIdx
                    }
                }

                // If wordCol or meaningCol found
                if (wordCol != -1 && meaningCol != -1) {
                    val fallbackPos = if (posCol != -1) posCol else if (statusCol != 2 && noteCol != 2 && deckCol != 2) 2 else -1
                    val fallbackNote = if (noteCol != -1) noteCol else if (statusCol != 3 && fallbackPos != 3 && deckCol != 3) 3 else -1
                    return ColumnMapping(
                        headerRowIndex = i,
                        wordCol = wordCol,
                        meaningCol = meaningCol,
                        posCol = fallbackPos,
                        noteCol = fallbackNote,
                        statusCol = statusCol,
                        hiddenCol = hiddenCol,
                        deckCol = deckCol
                    )
                }
            }
        }

        // No header row found. Check first few data rows for heuristics.
        val sampleRow = rows.firstOrNull { it.any { cell -> cell.isNotBlank() } } ?: emptyList()
        val firstCell = sampleRow.getOrNull(0)?.trim() ?: ""
        val secondCell = sampleRow.getOrNull(1)?.trim() ?: ""

        // If first cell is pure digits (like "1", "2") and second cell has letters/Japanese
        val isFirstCellNumber = firstCell.isNotBlank() && firstCell.all { it.isDigit() }
        val isSecondCellText = secondCell.isNotBlank() && !secondCell.all { it.isDigit() }

        val offset = if (isFirstCellNumber && isSecondCellText) 1 else 0

        return ColumnMapping(
            headerRowIndex = -1,
            wordCol = offset + 0,
            meaningCol = offset + 1,
            posCol = offset + 2,
            noteCol = offset + 3,
            statusCol = offset + 4,
            hiddenCol = offset + 5,
            deckCol = if (sampleRow.size >= offset + 7) offset + 6 else -1
        )
    }

    /**
     * Parses learning status strings (○, △, ×, 完璧, 微妙, 忘れた, etc.) into standard "○", "△", "×" or null.
     */
    fun parseStatus(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed == "-" || trimmed == "未" || trimmed == "未評価" ||
            trimmed.equals("none", ignoreCase = true) || trimmed.equals("null", ignoreCase = true)) {
            return null
        }
        return when {
            trimmed.contains("○") || trimmed.contains("〇") || trimmed.contains("完璧") ||
                trimmed.equals("perfect", ignoreCase = true) || trimmed.equals("o", ignoreCase = true) -> "○"
            trimmed.contains("△") || trimmed.contains("微妙") || trimmed.contains("三角") -> "△"
            trimmed.contains("×") || trimmed.contains("✕") || trimmed.contains("忘れた") ||
                trimmed.contains("バツ") || trimmed.contains("ばつ") || trimmed.equals("x", ignoreCase = true) -> "×"
            else -> null
        }
    }

    /**
     * Parses hidden flag (1, true, yes, はい, 非表示).
     */
    fun parseHidden(raw: String): Boolean {
        val trimmed = raw.trim().lowercase()
        return trimmed == "1" || trimmed == "true" || trimmed == "yes" || trimmed == "はい" || trimmed == "非表示"
    }

    fun findDuplicateWordsInRows(rows: List<List<String>>, existingWords: Set<String>): List<String> {
        val mapping = detectColumns(rows)
        val startRow = mapping.headerRowIndex + 1
        val found = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        for (rowIndex in startRow until rows.size) {
            val parts = rows[rowIndex]
            if (parts.size <= mapping.wordCol) continue
            val word = sanitizeFurigana(parts[mapping.wordCol].trim())
            if (word.isBlank()) continue
            val wLower = word.lowercase()
            if (wLower in listOf("単語", "古語", "古文単語", "word", "front", "見出し語", "英語") || wLower.startsWith("単語")) continue
            if (existingWords.contains(wLower) && seen.add(wLower)) {
                found.add(word)
            }
        }
        return found
    }

    suspend fun processRowsAndInsert(
        rows: List<List<String>>,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.MERGE_DUPLICATES,
        targetDeck: String? = null,
        subject: String = "ENGLISH"
    ): ImportResult {
        if (rows.isEmpty()) return ImportResult(0, 0, 0)

        val mapping = detectColumns(rows)
        val startRow = mapping.headerRowIndex + 1

        val existingCards = flashcardDao.getAllOnce()
            .filter { it.subject.equals(subject, ignoreCase = true) }
            .toMutableList()
        val existingMap = existingCards.associateBy { it.word.lowercase() }.toMutableMap()

        var newCount = 0
        var mergeCount = 0
        var skippedCount = 0
        val skippedWords = mutableListOf<String>()

        val preparedCards = mutableListOf<FlashcardEntity>()

        for (rowIndex in startRow until rows.size) {
            val parts = rows[rowIndex]
            if (parts.size <= maxOf(mapping.wordCol, mapping.meaningCol)) continue

            val rawWord = parts.getOrNull(mapping.wordCol)?.trim() ?: ""
            val rawMeaning = parts.getOrNull(mapping.meaningCol)?.trim() ?: ""
            if (rawWord.isBlank() || rawMeaning.isBlank()) continue

            val word = sanitizeFurigana(rawWord)
            val meaning = sanitizeFurigana(rawMeaning)

            val wLower = word.lowercase()
            val mLower = meaning.lowercase()

            // Skip accidental repeated header rows
            if (wLower in listOf("単語", "古語", "古文単語", "word", "front", "見出し語", "英語") || wLower.startsWith("単語")) {
                if (mLower in listOf("意味", "現代語訳", "口語訳", "meaning", "back", "訳", "日本語", "日本語訳") || mLower.startsWith("意味")) {
                    continue
                }
            }

            val isClassicalSubject = subject.equals("CLASSICAL", ignoreCase = true)
            var rawPosField = ""
            var pos = ""
            var note = ""
            var status: String? = null
            var isHidden = false
            var deck = ""

            if (mapping.posCol != -1 && mapping.posCol < parts.size) {
                rawPosField = sanitizeFurigana(parts[mapping.posCol].trim())
                pos = if (isClassicalSubject) rawPosField else sanitizePos(rawPosField)
            }

            if (mapping.noteCol != -1 && mapping.noteCol < parts.size) {
                note = sanitizeFurigana(parts[mapping.noteCol].trim())
            }

            if (mapping.statusCol != -1 && mapping.statusCol < parts.size) {
                status = parseStatus(parts[mapping.statusCol])
            }

            if (mapping.hiddenCol != -1 && mapping.hiddenCol < parts.size) {
                isHidden = parseHidden(parts[mapping.hiddenCol])
            }

            if (targetDeck != null && targetDeck.isNotBlank()) {
                deck = targetDeck.trim()
            } else if (mapping.deckCol != -1 && mapping.deckCol < parts.size && parts[mapping.deckCol].isNotBlank()) {
                deck = parts[mapping.deckCol].trim()
            }
            if (deck.isBlank()) {
                deck = if (isClassicalSubject) "共通テスト最重要動詞" else "日常英会話"
            }

            // Fallback: If posCol was not explicitly set or empty, check if parts has 3 columns where col 2 might be POS, Note, or Status
            if (parts.size == 3 && mapping.wordCol == 0 && mapping.meaningCol == 1) {
                val val3 = parts[2].trim()
                val parsedStatus = parseStatus(val3)
                if (parsedStatus != null) {
                    status = parsedStatus
                    pos = ""
                    note = ""
                } else {
                    val cleanedVal3 = sanitizeFurigana(val3)
                    if (isClassicalSubject && ClassicalPosNormalizer.isRecognizedPosOrConjugatedPos(cleanedVal3)) {
                        pos = cleanedVal3
                    } else {
                        val sanitizedVal3 = sanitizePos(val3)
                        if (isStrictPartOfSpeech(sanitizedVal3)) {
                            pos = sanitizedVal3
                        } else {
                            pos = ""
                            note = cleanedVal3
                        }
                    }
                }
            } else if (parts.size == 4 && mapping.wordCol == 0 && mapping.meaningCol == 1 && status == null) {
                // If 4th column is a status
                val val4 = parts[3].trim()
                val parsedStatus4 = parseStatus(val4)
                if (parsedStatus4 != null) {
                    status = parsedStatus4
                    if (note.isNotBlank() && parseStatus(note) != null) {
                        note = ""
                    }
                }
            }

            // If status is still null, check whether pos or note accidentally captured a status symbol
            if (status == null) {
                val statusFromPos = parseStatus(pos)
                if (statusFromPos != null) {
                    status = statusFromPos
                    pos = ""
                } else {
                    val statusFromNote = parseStatus(note)
                    if (statusFromNote != null) {
                        status = statusFromNote
                        note = ""
                    }
                }
            }

            // Check if multiple meanings / parts of speech were delimited by " / " or newlines (e.g. from an export or cell break)
            val meaningsList = meaning.split(Regex("(\\s*/\\s*|\\r?\\n+)")).map { it.trim() }.filter { it.isNotBlank() }
            val posList = pos.split(Regex("(\\s*/\\s*|\\r?\\n+)")).map { it.trim() }
            val extractedConjugations = mutableListOf<String>()

            fun resolveRowPos(rawItemPos: String): String {
                if (!isClassicalSubject) return sanitizePos(rawItemPos)
                val (simplePos, conj) = ClassicalPosNormalizer.splitPosAndConjugation(rawItemPos)
                if (conj.isNotBlank()) {
                    extractedConjugations.add(conj)
                }
                return sanitizePos(simplePos)
            }

            val initialDefs = if (meaningsList.size > 1 && (posList.size == meaningsList.size || posList.size <= 1)) {
                meaningsList.mapIndexed { idx, m ->
                    val p = posList.getOrNull(idx) ?: posList.firstOrNull() ?: ""
                    Definition(pos = resolveRowPos(p), meaning = m.replace(Regex("[\\r\\n]+"), " ").trim())
                }
            } else {
                listOf(Definition(pos = resolveRowPos(pos), meaning = meaning.replace(Regex("[\\r\\n]+"), " ").trim()))
            }

            if (isClassicalSubject && extractedConjugations.isNotEmpty()) {
                note = ClassicalPosNormalizer.mergeConjugationIntoNote(extractedConjugations, note)
            }

            if (word.isNotBlank() && meaning.isNotBlank()) {
                val existingInDb = existingMap[wLower]

                if (existingInDb != null && duplicateStrategy != DuplicateStrategy.FORCE_ADD_ALL) {
                    when (duplicateStrategy) {
                        DuplicateStrategy.SKIP_DUPLICATES -> {
                            // その重複している単語だけを登録しない
                            skippedCount++
                            if (!skippedWords.contains(word)) {
                                skippedWords.add(word)
                            }
                        }
                        DuplicateStrategy.MERGE_DUPLICATES -> {
                            // 既存の単語カードに意味を統合し、学習データ(ステータス・非表示)も維持・更新
                            val updatedDefs = existingInDb.definitions.toMutableList()
                            for (newDef in initialDefs) {
                                if (updatedDefs.none { it.pos.equals(newDef.pos, ignoreCase = true) && it.meaning.equals(newDef.meaning, ignoreCase = true) }) {
                                    updatedDefs.add(newDef)
                                }
                            }
                            val combinedNote = when {
                                existingInDb.note.isBlank() -> note
                                note.isBlank() -> existingInDb.note
                                existingInDb.note.contains(note) -> existingInDb.note
                                else -> "${existingInDb.note}; $note"
                            }
                            val finalStatus = status ?: existingInDb.status
                            val finalHidden = isHidden || existingInDb.isHidden

                            val finalDeck = if (targetDeck != null && targetDeck.isNotBlank()) targetDeck else if (deck.isNotBlank()) deck else existingInDb.deck
                            val updatedCard = existingInDb.copy(
                                definitions = updatedDefs,
                                deck = finalDeck,
                                note = combinedNote,
                                status = finalStatus,
                                isHidden = finalHidden
                            )
                            flashcardDao.update(updatedCard)
                            existingMap[wLower] = updatedCard
                            mergeCount++
                        }
                        DuplicateStrategy.FORCE_ADD_ALL -> {}
                    }
                } else {
                    // Check if already in preparedCards within this import batch
                    val batchIdx = preparedCards.indexOfLast { it.word.equals(word, ignoreCase = true) }
                    if (batchIdx >= 0) {
                        val lastCard = preparedCards[batchIdx]
                        val updatedDefs = lastCard.definitions.toMutableList()
                        for (newDef in initialDefs) {
                            if (updatedDefs.none { it.pos.equals(newDef.pos, ignoreCase = true) && it.meaning.equals(newDef.meaning, ignoreCase = true) }) {
                                updatedDefs.add(newDef)
                            }
                        }
                        val combinedNote = when {
                            lastCard.note.isBlank() -> note
                            note.isBlank() -> lastCard.note
                            lastCard.note.contains(note) -> lastCard.note
                            else -> "${lastCard.note}; $note"
                        }
                        val finalStatus = status ?: lastCard.status
                        val finalHidden = isHidden || lastCard.isHidden
                        val finalDeck = if (targetDeck != null && targetDeck.isNotBlank()) targetDeck else if (deck.isNotBlank()) deck else lastCard.deck
                        preparedCards[batchIdx] = lastCard.copy(
                            definitions = updatedDefs,
                            deck = finalDeck,
                            note = combinedNote,
                            status = finalStatus,
                            isHidden = finalHidden
                        )
                        mergeCount++
                    } else {
                        val newCard = FlashcardEntity(
                            word = word,
                            definitions = initialDefs,
                            deck = deck,
                            note = note,
                            status = status,
                            isHidden = isHidden,
                            subject = subject
                        )
                        preparedCards.add(newCard)
                        existingMap[wLower] = newCard
                        newCount++
                    }
                }
            }
        }

        if (preparedCards.isNotEmpty()) {
            flashcardDao.insertAll(preparedCards)
        }
        return ImportResult(newCount, mergeCount, skippedCount, skippedWords)
    }

    /**
     * Sanitizes Part of Speech string by stripping furigana, trailing phonetic letters, etc.
     * e.g. "その他タ" -> "その他"
     * "動詞(どうし)" -> "動詞"
     */
    fun sanitizePos(raw: String): String {
        val cleaned = sanitizeFurigana(raw).replace("　", " ").replace(Regex("\\s+"), " ").trim()
        if (cleaned.isBlank()) return ""
        if (cleaned in PartsOfSpeech.ALL) return cleaned
        // Check longer known POS entries first so "動詞 四段" / "形容詞 シク活用" are not truncated to "動詞" / "形容詞"
        val sortedKnown = PartsOfSpeech.ALL.sortedByDescending { it.length }
        for (known in sortedKnown) {
            if (cleaned.startsWith(known)) {
                return known
            }
        }
        return cleaned
    }

    private fun isStrictPartOfSpeech(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return false
        if (trimmed in PartsOfSpeech.ALL) return true
        val commaSplits = trimmed.split(Regex("[・、,/]+")).map { it.trim() }.filter { it.isNotBlank() }
        if (commaSplits.size > 1 && commaSplits.all { it in PartsOfSpeech.ALL }) return true
        val spaceSplits = trimmed.split(Regex("[・、,\\s]+")).filter { it.isNotBlank() }
        if (spaceSplits.isEmpty()) return false
        return spaceSplits.all { it in PartsOfSpeech.ALL }
    }
}
