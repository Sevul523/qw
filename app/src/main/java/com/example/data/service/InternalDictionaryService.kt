package com.example.data.service

import android.content.Context
import com.example.data.local.CustomDictionaryOverride
import com.example.data.model.ClassicalWordKnowledgeBase
import com.example.data.model.Definition
import com.example.data.model.WordKnowledgeBase
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream

enum class DictionaryLevelFilter(val id: String, val label: String) {
    ALL("ALL", "すべて"),
    BASIC("BASIC", "基礎・中高"),
    EXAM("EXAM", "大学受験"),
    IDIOM("IDIOM", "熟語・連語"),
    PRACTICAL("PRACTICAL", "TOEIC・実用"),
    SPECIALIZED("SPECIALIZED", "専門・学術")
}

data class InternalDictionaryResult(
    val word: String,
    val definitions: List<Definition>,
    val summaryMeaning: String,
    val pos: String = "",
    val deck: String = "",
    val note: String = "",
    val conjugation: String = "",
    val isClassical: Boolean = false,
    val source: String = "内蔵辞書",
    val isCustomEdited: Boolean = false,
    val isUserCreated: Boolean = false,
    val canonicalWord: String = word,
    val priorityTier: Int = 5,
    val levelBadge: String = ""
) {
    fun toWeblioLookupResult(): WeblioLookupResult {
        return WeblioLookupResult(
            word = word,
            definitions = definitions,
            summaryMeaning = summaryMeaning,
            level = when {
                isUserCreated -> "ユーザー登録"
                isCustomEdited -> "ユーザー編集"
                isClassical -> "重要古語"
                else -> "基礎5,000語・大学受験・学術熟語網羅 (約54万語)"
            },
            eikenGrade = "",
            formattedNote = note,
            conjugation = conjugation
        )
    }
}

internal data class InternalEntry(
    val word: String,
    val lowerWord: String,
    val pos: String,
    val meanings: String,
    val deck: String,
    val note: String,
    val rank: Int,
    val tier: Int
)

object InternalDictionaryService {

    @Volatile
    private var isLoaded: Boolean = false
    private val englishMap = HashMap<String, InternalEntry>(560000)
    private val orderedEntries = ArrayList<InternalEntry>(560000)
    private val customOverrides: MutableMap<String, CustomDictionaryOverride> = ConcurrentHashMap()

    private val commonIrregularEnglish = mapOf(
        "went" to "go", "gone" to "go",
        "came" to "come",
        "saw" to "see", "seen" to "see",
        "took" to "take", "taken" to "take",
        "gave" to "give", "given" to "give",
        "made" to "make",
        "knew" to "know", "known" to "know",
        "found" to "find",
        "thought" to "think",
        "told" to "tell",
        "felt" to "feel",
        "left" to "leave",
        "brought" to "bring",
        "began" to "begin", "begun" to "begin",
        "kept" to "keep",
        "held" to "hold",
        "wrote" to "write", "written" to "write",
        "stood" to "stand",
        "heard" to "hear",
        "let" to "let",
        "meant" to "mean",
        "set" to "set",
        "met" to "meet",
        "ran" to "run",
        "paid" to "pay",
        "sat" to "sit",
        "spoke" to "speak", "spoken" to "speak",
        "lay" to "lie", "lain" to "lie",
        "led" to "lead",
        "read" to "read",
        "grew" to "grow", "grown" to "grow",
        "lost" to "lose",
        "fell" to "fall", "fallen" to "fall",
        "sent" to "send",
        "built" to "build",
        "understood" to "understand",
        "drew" to "draw", "drawn" to "draw",
        "broke" to "break", "broken" to "break",
        "spent" to "spend",
        "cut" to "cut",
        "rose" to "rise", "risen" to "rise",
        "drove" to "drive", "driven" to "drive",
        "bought" to "buy",
        "wore" to "wear", "worn" to "wear",
        "chose" to "choose", "chosen" to "choose",
        "better" to "good", "best" to "good",
        "worse" to "bad", "worst" to "bad",
        "more" to "many", "most" to "many",
        "less" to "little", "least" to "little",
        "further" to "far", "furthest" to "far",
        "children" to "child", "men" to "man", "women" to "woman", "feet" to "foot", "teeth" to "tooth", "mice" to "mouse"
    )

    private fun tierFromRank(rank: Int): Int {
        return when {
            rank <= 5000 -> 1
            rank <= 15000 -> 2
            rank <= 30000 -> 3
            rank <= 90000 -> 4
            else -> 5
        }
    }

    private fun levelBadgeForTier(tier: Int): String {
        return when (tier) {
            1 -> "基礎"
            2 -> "受験"
            3 -> "熟語"
            4 -> "実用"
            else -> "専門"
        }
    }

    private fun inferFallbackRank(word: String, deck: String, note: String): Int {
        return when {
            deck.contains("最難関") || deck.contains("大学受験") -> 10000
            deck.contains("熟語") || note.contains("連語") || word.contains(' ') -> 20000
            deck.contains("日常") -> 4000
            deck.contains("TOEIC") || deck.contains("総合") -> 60000
            else -> 200000
        }
    }

    private fun matchesLevelFilter(tier: Int, levelFilter: DictionaryLevelFilter): Boolean {
        return when (levelFilter) {
            DictionaryLevelFilter.ALL -> true
            DictionaryLevelFilter.BASIC -> tier == 1
            DictionaryLevelFilter.EXAM -> tier == 2
            DictionaryLevelFilter.IDIOM -> tier == 3
            DictionaryLevelFilter.PRACTICAL -> tier == 4
            DictionaryLevelFilter.SPECIALIZED -> tier == 5
        }
    }

    fun ensureLoaded(context: Context) {
        if (isLoaded) return
        synchronized(this) {
            if (isLoaded) return
            try {
                // 1. Prefer split chunks (internal_ej_dict_part1..partN as .tsv.gz or AAPT2-stripped .tsv)
                val partAssets = (context.assets.list("") ?: emptyArray())
                    .filter { it.startsWith("internal_ej_dict_part") && (it.endsWith(".tsv.gz") || it.endsWith(".tsv")) }
                    .sortedWith(compareBy { name ->
                        Regex("part(\\d+)").find(name)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
                    })

                if (partAssets.isNotEmpty()) {
                    val streams = partAssets.map { context.assets.open(it) }
                    try {
                        loadFromInputStreams(streams)
                        return
                    } finally {
                        streams.forEach { runCatching { it.close() } }
                    }
                }

                // 2. Fallback to single-file gzipped or plain TSV
                context.assets.open("internal_ej_dict.tsv.gz").use { raw ->
                    loadFromInputStream(raw)
                }
            } catch (_: Exception) {
                try {
                    context.assets.open("internal_ej_dict.tsv").use { stream ->
                        loadFromInputStream(stream)
                    }
                } catch (_: Exception) {
                    // If asset read fails, mark loaded and proceed with curated base
                    isLoaded = true
                }
            }
        }
    }

    fun loadFromInputStream(stream: InputStream) {
        loadFromInputStreams(listOf(stream))
    }

    fun loadFromInputStreams(streams: List<InputStream>) {
        synchronized(this) {
            englishMap.clear()
            orderedEntries.clear()

            for (stream in streams) {
                parseStreamIntoMap(stream)
            }

            orderedEntries.addAll(englishMap.values)
            orderedEntries.sortWith(
                compareBy<InternalEntry> { it.tier }
                    .thenBy { it.rank }
                    .thenBy { it.lowerWord }
            )
            isLoaded = true
        }
    }

    private fun parseStreamIntoMap(stream: InputStream) {
        val buffered = BufferedInputStream(stream, 65536)
        buffered.mark(2)
        val b1 = buffered.read()
        val b2 = buffered.read()
        buffered.reset()
        val effectiveStream: InputStream = if (b1 == 0x1f && b2 == 0x8b) {
            GZIPInputStream(buffered, 65536)
        } else {
            buffered
        }

        effectiveStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (line in lines) {
                if (line.isBlank()) continue
                val parts = line.split('\t')
                if (parts.size >= 3) {
                    val rawWord = parts[0].trim().replace(Regex("\\s{2,}"), " ")
                    val lowerWord = rawWord.lowercase()
                    if (lowerWord.isEmpty()) continue
                    val pos = parts[1].trim().intern()
                    val meanings = parts[2].trim()
                        .replace(Regex("\\s{2,}"), " ")
                        .trimEnd(' ', '/', ';', ',')
                    val deck = if (parts.size > 3) parts[3].trim().intern() else "日常英会話"
                    val note = if (parts.size > 4) {
                        val n = parts[4].trim().replace(Regex("\\s{2,}"), " ")
                        if (n.length < 35) n.intern() else n
                    } else ""
                    val parsedRank = if (parts.size > 5) parts[5].trim().toIntOrNull() else null
                    val rank = parsedRank ?: inferFallbackRank(lowerWord, deck, note)
                    val tier = tierFromRank(rank)

                    val entry = InternalEntry(
                        word = rawWord,
                        lowerWord = lowerWord,
                        pos = pos,
                        meanings = meanings,
                        deck = deck,
                        note = note,
                        rank = rank,
                        tier = tier
                    )
                    englishMap[lowerWord] = entry
                }
            }
        }
    }

    fun isLoaded(): Boolean = isLoaded

    // === Custom Dictionary Overrides Management ===

    fun applyOverrides(overrides: List<CustomDictionaryOverride>) {
        customOverrides.clear()
        for (o in overrides) {
            customOverrides[o.wordKey.lowercase().trim()] = o
        }
    }

    fun getCustomOverride(word: String): CustomDictionaryOverride? {
        return customOverrides[word.lowercase().trim()]
    }

    private fun formatCustomOverrideResult(override: CustomDictionaryOverride): InternalDictionaryResult {
        val rawParts = override.meanings.split(Regex(" / | /|/ |/|; |;"))
            .map { it.trim().trim(';', ',') }
            .filter { it.isNotEmpty() }

        val definitions = if (rawParts.size > 1) {
            rawParts.take(4).map { Definition(pos = override.pos, meaning = it) }
        } else {
            listOf(Definition(pos = override.pos, meaning = override.meanings))
        }
        val summary = rawParts.take(3).joinToString(", ")
        val sourceLabel = when {
            override.isUserCreated -> "ユーザー登録"
            else -> "ユーザー編集"
        }

        return InternalDictionaryResult(
            word = override.word,
            definitions = definitions,
            summaryMeaning = summary,
            pos = override.pos,
            deck = override.deck,
            note = override.note,
            conjugation = override.conjugation,
            isClassical = override.isClassical,
            source = sourceLabel,
            isCustomEdited = !override.isUserCreated,
            isUserCreated = override.isUserCreated,
            priorityTier = 1,
            levelBadge = sourceLabel
        )
    }

    fun getVocabularyCount(): Pair<Int, Int> {
        val baseEng = if (englishMap.isNotEmpty()) englishMap.size else 540000
        val baseClass = ClassicalWordKnowledgeBase.ALL_ENTRIES.size

        var customEngAdd = 0
        var customEngDel = 0
        var customClassAdd = 0
        var customClassDel = 0

        for (o in customOverrides.values) {
            if (o.isClassical) {
                if (o.isDeleted) customClassDel++
                else if (o.isUserCreated) customClassAdd++
            } else {
                if (o.isDeleted) customEngDel++
                else if (o.isUserCreated) customEngAdd++
            }
        }

        val totalEng = (baseEng + customEngAdd - customEngDel).coerceAtLeast(0)
        val totalClass = (baseClass + customClassAdd - customClassDel).coerceAtLeast(0)
        return Pair(totalEng, totalClass)
    }

    fun lookupWord(rawWord: String, isClassical: Boolean): InternalDictionaryResult? {
        return if (isClassical) {
            lookupClassical(rawWord)
        } else {
            lookupEnglish(rawWord)
        }
    }

    fun lookupClassical(rawWord: String): InternalDictionaryResult? {
        val clean = rawWord.trim().lowercase()
        if (clean.isBlank()) return null

        // 1. Check user custom overrides
        val custom = customOverrides[clean]
        if (custom != null && custom.isClassical) {
            if (custom.isDeleted) return null
            return formatCustomOverrideResult(custom)
        }

        // 2. Base classical dictionary lookup
        val entry = ClassicalWordKnowledgeBase.lookup(rawWord) ?: return null
        val summary = entry.definitions.joinToString("; ") { it.meaning }
        val primaryPos = entry.definitions.firstOrNull()?.pos ?: "古語"
        val noteContent = buildString {
            if (entry.note.isNotBlank()) append(entry.note)
            if (entry.etymology.isNotBlank()) {
                if (isNotEmpty()) append(" / ")
                append("【語源】").append(entry.etymology)
            }
            if (entry.conjugation.isNotBlank()) {
                if (isNotEmpty()) append(" / ")
                append("【活用】").append(entry.conjugation)
            }
        }
        return InternalDictionaryResult(
            word = entry.word,
            definitions = entry.definitions,
            summaryMeaning = summary,
            pos = primaryPos,
            deck = entry.deck,
            note = noteContent,
            conjugation = entry.conjugation,
            isClassical = true,
            source = "内蔵古語辞書 (${ClassicalWordKnowledgeBase.ALL_ENTRIES.size}語)",
            priorityTier = 1,
            levelBadge = "古語"
        )
    }

    fun lookupEnglish(rawWord: String): InternalDictionaryResult? {
        val clean = rawWord.trim().lowercase()
        if (clean.isBlank()) return null

        // 1. Check user custom overrides
        val custom = customOverrides[clean]
        if (custom != null && !custom.isClassical) {
            if (custom.isDeleted) return null
            return formatCustomOverrideResult(custom)
        }

        // 2. Exact match in 540,000+ words dictionary
        val direct = englishMap[clean]
        if (direct != null) {
            return formatEntryResult(direct, direct.word)
        }

        // 2.5 Multi-word phrase inflection match (e.g. "took advantage of" -> "take advantage of", "looking forward to" -> "look forward to")
        if (clean.contains(" ")) {
            val spaceIdx = clean.indexOf(' ')
            val firstWord = clean.substring(0, spaceIdx)
            val rest = clean.substring(spaceIdx)

            val baseFirstIrreg = commonIrregularEnglish[firstWord]
            if (baseFirstIrreg != null) {
                val candidatePhrase = baseFirstIrreg + rest
                val match = englishMap[candidatePhrase]
                if (match != null) {
                    return formatEntryResult(
                        match,
                        clean,
                        additionalNote = "【原形】$candidatePhrase"
                    )
                }
            }

            val firstStems = generateEnglishStems(firstWord)
            for (fStem in firstStems) {
                val candidatePhrase = fStem + rest
                val match = englishMap[candidatePhrase]
                if (match != null) {
                    return formatEntryResult(
                        match,
                        clean,
                        additionalNote = "【原形】$candidatePhrase"
                    )
                }
            }
        }

        // 3. Irregular inflection match
        val irregularBase = commonIrregularEnglish[clean]
        if (irregularBase != null) {
            val customIrreg = customOverrides[irregularBase]
            if (customIrreg != null && !customIrreg.isClassical && !customIrreg.isDeleted) {
                return formatCustomOverrideResult(customIrreg)
            }
            val baseEntry = englishMap[irregularBase]
            if (baseEntry != null) {
                return formatEntryResult(
                    baseEntry,
                    clean,
                    additionalNote = "【原形】$irregularBase (${baseEntry.meanings})"
                )
            }
        }

        // 4. Regular inflection stem stripping
        val stems = generateEnglishStems(clean)
        for (stem in stems) {
            val customStem = customOverrides[stem]
            if (customStem != null && !customStem.isClassical && !customStem.isDeleted) {
                return formatCustomOverrideResult(customStem)
            }
            val stemEntry = englishMap[stem]
            if (stemEntry != null) {
                return formatEntryResult(
                    stemEntry,
                    clean,
                    additionalNote = "【原形】$stem (${stemEntry.meanings})"
                )
            }
        }

        // 5. Fallback to rich curated WordKnowledgeBase entries
        val rich = WordKnowledgeBase.lookup(clean)
        if (rich != null && rich.definitions.isNotEmpty()) {
            return InternalDictionaryResult(
                word = rich.word,
                definitions = rich.definitions,
                summaryMeaning = rich.definitions.joinToString(", ") { it.meaning },
                pos = rich.definitions.firstOrNull()?.pos ?: "英単語",
                deck = rich.deck,
                note = rich.note,
                isClassical = false,
                source = "内蔵英単語辞書",
                priorityTier = 1,
                levelBadge = "基礎"
            )
        }

        return null
    }

    private fun generateEnglishStems(word: String): List<String> {
        val list = mutableListOf<String>()
        val len = word.length

        // Plural / 3rd person -s, -es, -ies
        if (word.endsWith("ies") && len > 4) {
            list.add(word.dropLast(3) + "y") // studies -> study
        }
        if (word.endsWith("es") && len > 3) {
            list.add(word.dropLast(2)) // watches -> watch
            list.add(word.dropLast(1)) // leaves -> leave
        }
        if (word.endsWith("s") && !word.endsWith("ss") && len > 3) {
            list.add(word.dropLast(1)) // cats -> cat
        }

        // Past / Participle -ed, -ied
        if (word.endsWith("ied") && len > 4) {
            list.add(word.dropLast(3) + "y") // carried -> carry
        }
        if (word.endsWith("ed") && len > 3) {
            list.add(word.dropLast(2)) // walked -> walk
            list.add(word.dropLast(1)) // liked -> like
            // doubled consonant: stopped -> stop, planned -> plan
            if (len > 4 && word[len - 3] == word[len - 4]) {
                list.add(word.dropLast(3))
            }
        }

        // Participle -ing
        if (word.endsWith("ing") && len > 4) {
            list.add(word.dropLast(3)) // walking -> walk
            list.add(word.dropLast(3) + "e") // making -> make
            // doubled consonant: running -> run, swimming -> swim
            if (len > 5 && word[len - 4] == word[len - 5]) {
                list.add(word.dropLast(4))
            }
        }

        // Adverb -ly
        if (word.endsWith("ly") && len > 4) {
            list.add(word.dropLast(2)) // quickly -> quick
            list.add(word.dropLast(2) + "e") // truly -> true (custom), safely -> safe
            if (word.endsWith("ily")) {
                list.add(word.dropLast(3) + "y") // happily -> happy
            }
        }

        // Comparative / Superlative -er, -est
        if (word.endsWith("ier") && len > 4) {
            list.add(word.dropLast(3) + "y") // happier -> happy
        }
        if (word.endsWith("er") && len > 3) {
            list.add(word.dropLast(2)) // faster -> fast
            list.add(word.dropLast(1)) // nicer -> nice
        }
        if (word.endsWith("iest") && len > 5) {
            list.add(word.dropLast(4) + "y") // happiest -> happy
        }
        if (word.endsWith("est") && len > 4) {
            list.add(word.dropLast(3)) // fastest -> fast
            list.add(word.dropLast(2)) // nicest -> nice
        }

        return list.distinct()
    }

    private fun formatEntryResult(
        entry: InternalEntry,
        requestedWord: String,
        additionalNote: String = ""
    ): InternalDictionaryResult {
        // Parse meanings by / or ; delimiter
        val rawParts = entry.meanings.split(Regex(" / | /|/ |/|; |;"))
            .map { it.trim().trim(';', ',') }
            .filter { it.isNotEmpty() }

        val definitions = if (rawParts.size > 1) {
            rawParts.take(4).map { Definition(pos = entry.pos, meaning = it) }
        } else {
            listOf(Definition(pos = entry.pos, meaning = entry.meanings))
        }

        val summary = rawParts.take(3).joinToString(", ")
        val finalNote = buildString {
            if (entry.note.isNotBlank()) append(entry.note)
            if (additionalNote.isNotBlank()) {
                if (isNotEmpty()) append(" / ")
                append(additionalNote)
            }
        }

        return InternalDictionaryResult(
            word = requestedWord,
            definitions = definitions,
            summaryMeaning = summary,
            pos = entry.pos,
            deck = entry.deck,
            note = finalNote,
            isClassical = false,
            source = "基礎・大学受験・学術網羅辞書 (内蔵約54万語)",
            canonicalWord = entry.word,
            priorityTier = entry.tier,
            levelBadge = levelBadgeForTier(entry.tier)
        )
    }

    /**
     * Checks if a phrase is recognized as an English idiom, phrasal verb, or multi-word expression.
     */
    fun isIdiomPhrase(phrase: String): Boolean {
        val clean = phrase.trim().lowercase()
        if (clean.isBlank()) return false
        val res = lookupEnglish(clean) ?: return false
        return clean.contains(" ") || res.pos in setOf("句動詞", "熟語・連語", "連語", "前置詞句", "成句", "重要構文", "ビジネス連語")
    }

    fun getAllEnglishEntries(): List<InternalDictionaryResult> {
        val result = mutableListOf<InternalDictionaryResult>()
        // 1. Add non-deleted English custom overrides
        for (o in customOverrides.values) {
            if (!o.isClassical && !o.isDeleted) {
                result.add(formatCustomOverrideResult(o))
            }
        }
        // 2. Add base entries in priority order not overridden or deleted
        synchronized(this) {
            for (entry in orderedEntries) {
                if (!customOverrides.containsKey(entry.lowerWord)) {
                    result.add(formatEntryResult(entry, entry.word))
                }
            }
        }
        return result
    }

    fun getAllClassicalEntries(): List<InternalDictionaryResult> {
        val result = mutableListOf<InternalDictionaryResult>()
        // 1. Add non-deleted Classical custom overrides
        for (o in customOverrides.values) {
            if (o.isClassical && !o.isDeleted) {
                result.add(formatCustomOverrideResult(o))
            }
        }
        // 2. Add base entries not overridden or deleted
        for (entry in ClassicalWordKnowledgeBase.ALL_ENTRIES) {
            val key = entry.word.lowercase().trim()
            if (!customOverrides.containsKey(key)) {
                lookupClassical(entry.word)?.let { result.add(it) }
            }
        }
        return result
    }

    fun search(
        query: String,
        isClassical: Boolean,
        levelFilter: DictionaryLevelFilter = DictionaryLevelFilter.ALL,
        limit: Int = 150
    ): List<InternalDictionaryResult> {
        val clean = query.trim().lowercase()
        if (isClassical) {
            val allClassical = getAllClassicalEntries()
            if (clean.isBlank()) return allClassical.take(limit)
            val exact = mutableListOf<InternalDictionaryResult>()
            val starts = mutableListOf<InternalDictionaryResult>()
            val containsWord = mutableListOf<InternalDictionaryResult>()
            val containsMeaning = mutableListOf<InternalDictionaryResult>()

            for (item in allClassical) {
                val w = item.word.lowercase()
                val summary = item.summaryMeaning.lowercase()
                when {
                    w == clean -> exact.add(item)
                    w.startsWith(clean) -> starts.add(item)
                    w.contains(clean) -> containsWord.add(item)
                    summary.contains(clean) || item.definitions.any { it.meaning.contains(clean, ignoreCase = true) } -> containsMeaning.add(item)
                }
            }
            return (exact + starts + containsWord + containsMeaning).distinctBy { it.word.lowercase() }.take(limit)
        } else {
            // English search (tier-prioritized via orderedEntries)
            val exact = mutableListOf<InternalDictionaryResult>()
            val starts = mutableListOf<InternalDictionaryResult>()
            val containsWord = mutableListOf<InternalDictionaryResult>()
            val containsMeaning = mutableListOf<InternalDictionaryResult>()

            // Check custom overrides first (visible under ALL or BASIC filter)
            if (levelFilter == DictionaryLevelFilter.ALL || levelFilter == DictionaryLevelFilter.BASIC) {
                for (o in customOverrides.values) {
                    if (o.isClassical || o.isDeleted) continue
                    val res = formatCustomOverrideResult(o)
                    val w = o.word.lowercase()
                    val meaning = o.meanings.lowercase()
                    when {
                        clean.isBlank() || w == clean -> exact.add(res)
                        w.startsWith(clean) -> starts.add(res)
                        w.contains(clean) -> containsWord.add(res)
                        meaning.contains(clean) -> containsMeaning.add(res)
                    }
                }
            }

            if (clean.isBlank()) {
                val list = ArrayList<InternalDictionaryResult>(limit)
                list.addAll(exact)
                synchronized(this) {
                    for (entry in orderedEntries) {
                        if (!matchesLevelFilter(entry.tier, levelFilter)) continue
                        if (customOverrides.containsKey(entry.lowerWord)) continue
                        list.add(formatEntryResult(entry, entry.word))
                        if (list.size >= limit) break
                    }
                }
                return list.take(limit)
            }

            synchronized(this) {
                lookupEnglish(clean)?.let {
                    if (matchesLevelFilter(it.priorityTier, levelFilter) &&
                        exact.none { e -> e.word.equals(it.word, ignoreCase = true) }
                    ) {
                        exact.add(it)
                    }
                }

                for (entry in orderedEntries) {
                    if (!matchesLevelFilter(entry.tier, levelFilter)) continue
                    val key = entry.lowerWord
                    if (customOverrides.containsKey(key)) continue
                    if (key == clean) continue

                    if (key.startsWith(clean)) {
                        if (starts.size < limit) starts.add(formatEntryResult(entry, entry.word))
                    } else if (key.contains(clean)) {
                        if (containsWord.size < limit) containsWord.add(formatEntryResult(entry, entry.word))
                    } else if (entry.meanings.contains(clean, ignoreCase = true)) {
                        if (containsMeaning.size < limit) containsMeaning.add(formatEntryResult(entry, entry.word))
                    }

                    // Fast early-out once all buckets are filled (orderedEntries guarantees lower tiers are already collected)
                    if (starts.size >= limit && containsWord.size >= limit && containsMeaning.size >= limit) {
                        break
                    }
                }
            }
            return (exact + starts + containsWord + containsMeaning)
                .distinctBy { it.word.lowercase() }
                .take(limit)
        }
    }
}

