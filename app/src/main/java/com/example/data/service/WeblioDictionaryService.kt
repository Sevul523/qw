package com.example.data.service

import com.example.data.model.Definition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.regex.Pattern

enum class WeblioVerifyStatus {
    FOUND,
    NOT_FOUND,
    TRANSIENT_ERROR
}

data class WeblioLookupResult(
    val word: String,
    val definitions: List<Definition>,
    val summaryMeaning: String,
    val level: String = "",
    val eikenGrade: String = "",
    val formattedNote: String = "",
    val conjugation: String = ""
)

object WeblioDictionaryService {
    private val memoryCache = mutableMapOf<String, WeblioLookupResult>()
    private val classicalMemoryCache = mutableMapOf<String, WeblioLookupResult>()
    private val notFoundEnglishCache = mutableSetOf<String>()
    private val notFoundClassicalCache = mutableSetOf<String>()

    /**
     * True whenever Weblio responded authoritatively (HTTP 200 or 404), proving the device is online;
     * false when network I/O failed or only transient throttling occurred.
     */
    @Volatile
    var lastNetworkReachable: Boolean = false
        private set

    internal fun isExplicitNotFoundHtml(html: String): Boolean {
        return html.contains("一致する見出し語は見つかりませんでした") ||
            html.contains("該当する言葉は見つかりませんでした") ||
            html.contains("に一致する見出し語は見つかりません")
    }

    fun getCachedWord(rawWord: String): WeblioLookupResult? {
        val key = rawWord.trim().lowercase()
        if (key.isBlank()) return null
        synchronized(memoryCache) {
            return memoryCache[key]
        }
    }

    fun getCachedClassicalWord(rawWord: String): WeblioLookupResult? {
        val key = rawWord.trim()
        if (key.isBlank()) return null
        synchronized(classicalMemoryCache) {
            return classicalMemoryCache[key]
        }
    }

    /**
     * Looks up an English word on Weblio English-Japanese Dictionary (ejje.weblio.jp).
     * Extracts all parts of speech and their meanings when multiple parts of speech exist.
     * Never negative-caches transient HTTP 429 / 5xx responses.
     */
    suspend fun lookupWord(rawWord: String): WeblioLookupResult? = withContext(Dispatchers.IO) {
        val word = rawWord.trim().lowercase()
        if (word.isBlank()) return@withContext null

        synchronized(memoryCache) {
            memoryCache[word]?.let { return@withContext it }
            if (word in notFoundEnglishCache) return@withContext null
        }

        for (attempt in 0..1) {
            try {
                val encodedWord = URLEncoder.encode(word, "UTF-8")
                val targetUrl = URL("https://ejje.weblio.jp/content/$encodedWord")
                val connection = (targetUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4500
                    readTimeout = 4500
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
                    setRequestProperty("Accept-Language", "ja,en-US;q=0.9,en;q=0.8")
                    instanceFollowRedirects = true
                }

                val code = connection.responseCode
                if (code == 200 || code == 404) {
                    lastNetworkReachable = true
                }
                if (code == 404) {
                    synchronized(memoryCache) { notFoundEnglishCache.add(word) }
                    return@withContext null
                }
                if (code == 429 || code >= 500) {
                    if (attempt == 0) {
                        delay(260)
                        continue
                    }
                    return@withContext null
                }
                if (code != 200) {
                    return@withContext null
                }

                val html = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val parsed = parseWeblioHtml(word, html)

                synchronized(memoryCache) {
                    if (parsed != null && parsed.definitions.isNotEmpty()) {
                        memoryCache[word] = parsed
                    } else if (isExplicitNotFoundHtml(html)) {
                        notFoundEnglishCache.add(word)
                    }
                }
                return@withContext parsed
            } catch (_: Exception) {
                lastNetworkReachable = false
                if (attempt == 0) {
                    delay(180)
                    continue
                }
                return@withContext null
            }
        }
        null
    }

    /**
     * Looks up a Classical Japanese word (古語) on Weblio Classical Japanese Dictionary (kobun.weblio.jp).
     * Supports multiple parts of speech and multiple conjugations, and never populates Weblioレベル or 英検級.
     */
    suspend fun lookupClassicalWord(rawWord: String): WeblioLookupResult? = withContext(Dispatchers.IO) {
        val word = rawWord.trim()
        if (word.isBlank()) return@withContext null

        synchronized(classicalMemoryCache) {
            classicalMemoryCache[word]?.let { return@withContext it }
            if (word in notFoundClassicalCache) return@withContext null
        }

        val queryVariants = buildClassicalQueryVariants(word)
        var allAuthoritativeNotFound = queryVariants.isNotEmpty()
        for (variant in queryVariants) {
            val (fetched, status) = fetchClassicalFromWeblioDetailed(variant, originalWord = word)
            if (status == WeblioVerifyStatus.FOUND && fetched != null && fetched.definitions.isNotEmpty()) {
                synchronized(classicalMemoryCache) {
                    classicalMemoryCache[word] = fetched
                    classicalMemoryCache[variant] = fetched
                }
                return@withContext fetched
            }
            if (status != WeblioVerifyStatus.NOT_FOUND) {
                allAuthoritativeNotFound = false
            }
        }
        if (allAuthoritativeNotFound) {
            synchronized(classicalMemoryCache) {
                notFoundClassicalCache.add(word)
            }
        }
        null
    }

    /**
     * Verifies whether a Classical Japanese word exists on Weblio古語辞典 (`kobun.weblio.jp`) while distinguishing
     * authoritative NOT_FOUND (HTTP 404 or explicit not-found HTML) from TRANSIENT_ERROR (HTTP 429/5xx/offline).
     */
    suspend fun verifyClassicalWordExistence(rawWord: String): WeblioVerifyStatus = withContext(Dispatchers.IO) {
        val word = rawWord.trim()
        if (word.isBlank()) return@withContext WeblioVerifyStatus.NOT_FOUND

        synchronized(classicalMemoryCache) {
            if (classicalMemoryCache.containsKey(word)) return@withContext WeblioVerifyStatus.FOUND
            if (word in notFoundClassicalCache) return@withContext WeblioVerifyStatus.NOT_FOUND
        }

        val queryVariants = buildClassicalQueryVariants(word)
        if (queryVariants.isEmpty()) return@withContext WeblioVerifyStatus.NOT_FOUND

        var sawTransientError = false
        for (variant in queryVariants) {
            synchronized(classicalMemoryCache) {
                classicalMemoryCache[variant]?.let { cached ->
                    classicalMemoryCache[word] = cached
                    return@withContext WeblioVerifyStatus.FOUND
                }
            }
            val (fetched, status) = fetchClassicalFromWeblioDetailed(variant, originalWord = word)
            when (status) {
                WeblioVerifyStatus.FOUND -> {
                    if (fetched != null && fetched.definitions.isNotEmpty()) {
                        synchronized(classicalMemoryCache) {
                            classicalMemoryCache[word] = fetched
                            classicalMemoryCache[variant] = fetched
                        }
                    }
                    return@withContext WeblioVerifyStatus.FOUND
                }
                WeblioVerifyStatus.TRANSIENT_ERROR -> {
                    sawTransientError = true
                }
                WeblioVerifyStatus.NOT_FOUND -> Unit
            }
        }

        if (!sawTransientError) {
            synchronized(classicalMemoryCache) {
                notFoundClassicalCache.add(word)
            }
            WeblioVerifyStatus.NOT_FOUND
        } else {
            WeblioVerifyStatus.TRANSIENT_ERROR
        }
    }

    internal fun buildClassicalQueryVariants(word: String): List<String> {
        val cleaned = word.trim().replace("・", "").replace("‐", "")
        if (cleaned.isEmpty()) return emptyList()
        val variants = LinkedHashSet<String>()
        variants.add(cleaned)
        if (cleaned != word) variants.add(word)

        if ((cleaned.endsWith("なり") || cleaned.endsWith("たり")) && cleaned.length > 3) {
            variants.add(cleaned.dropLast(2))
        }
        val dakutenNormalized = cleaned
            .replace("ゐ", "い")
            .replace("ゑ", "え")
            .replace("を", "お")
            .replace("ぢ", "じ")
            .replace("づ", "ず")
        variants.add(dakutenNormalized)

        // Medial historical-kana -> modern-kana (e.g., あはれ -> あわれ, いとほし -> いとおし, にほふ -> におう)
        if (cleaned.length >= 2) {
            val medialModern = buildString(cleaned.length) {
                cleaned.forEachIndexed { idx, ch ->
                    val mapped = if (idx == 0) {
                        when (ch) {
                            'ゐ' -> 'い'
                            'ゑ' -> 'え'
                            'を' -> 'お'
                            else -> ch
                        }
                    } else {
                        when (ch) {
                            'は' -> 'わ'
                            'ひ', 'ゐ' -> 'い'
                            'ふ' -> 'う'
                            'へ', 'ゑ' -> 'え'
                            'ほ', 'を' -> 'お'
                            'ぢ' -> 'じ'
                            'づ' -> 'ず'
                            else -> ch
                        }
                    }
                    append(mapped)
                }
            }
            variants.add(medialModern)
            if ((medialModern.endsWith("なり") || medialModern.endsWith("たり")) && medialModern.length > 3) {
                variants.add(medialModern.dropLast(2))
            }
        }
        return variants.toList()
    }

    private suspend fun fetchClassicalFromWeblioDetailed(
        queryWord: String,
        originalWord: String
    ): Pair<WeblioLookupResult?, WeblioVerifyStatus> {
        for (attempt in 0..1) {
            try {
                val encodedWord = URLEncoder.encode(queryWord, "UTF-8")
                val targetUrl = URL("https://kobun.weblio.jp/content/$encodedWord")
                val connection = (targetUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4500
                    readTimeout = 4500
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
                    setRequestProperty("Accept-Language", "ja,en-US;q=0.9,en;q=0.8")
                    instanceFollowRedirects = true
                }

                val code = connection.responseCode
                if (code == 200 || code == 404) {
                    lastNetworkReachable = true
                }
                if (code == 404) {
                    return null to WeblioVerifyStatus.NOT_FOUND
                }
                if (code == 429 || code >= 500) {
                    if (attempt == 0) {
                        delay(260)
                        continue
                    }
                    return null to WeblioVerifyStatus.TRANSIENT_ERROR
                }
                if (code != 200) {
                    return null to WeblioVerifyStatus.TRANSIENT_ERROR
                }

                val html = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (isExplicitNotFoundHtml(html)) {
                    return null to WeblioVerifyStatus.NOT_FOUND
                }
                val parsed = parseWeblioClassicalHtml(originalWord, html)
                return if (parsed != null && parsed.definitions.isNotEmpty()) {
                    parsed to WeblioVerifyStatus.FOUND
                } else {
                    null to WeblioVerifyStatus.TRANSIENT_ERROR
                }
            } catch (_: Exception) {
                lastNetworkReachable = false
                if (attempt == 0) {
                    delay(180)
                    continue
                }
                return null to WeblioVerifyStatus.TRANSIENT_ERROR
            }
        }
        return null to WeblioVerifyStatus.TRANSIENT_ERROR
    }

    internal fun parseWeblioClassicalHtml(word: String, html: String): WeblioLookupResult? {
        if (html.isBlank()) return null
        if (isExplicitNotFoundHtml(html)) {
            return null
        }

        // Remove scripts, styles, comments, and ruby reading tags (<rt>, <rp>) before extracting text
        val sanitizedHtml = html
            .replace(Regex("<script.*?</script>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("<style.*?</style>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<rt[^>]*>.*?</rt>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("<rp[^>]*>.*?</rp>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")

        // Path 1: Structured 学研全訳古語辞典 (<div class="Gkzkj"> / <div class=Gkzkj>) with multi-POS & multi-entry support
        val gkzkjResult = extractFromGkzkjBlocks(word, sanitizedHtml)
        if (gkzkjResult != null && gkzkjResult.definitions.isNotEmpty()) {
            return gkzkjResult
        }

        // Path 2: General article blocks (Gkgjt, Wkkdj, Sgkdj, NetDicBody, kiji - quoted or unquoted class attributes)
        val articleBlocks = mutableListOf<String>()
        val blockPatterns = listOf(
            "<div\\s+class=(?:[\"'][^\"']*(?:Gkgjt|Wkkdj|Sgkdj|NetDicBody)[^\"']*[\"']|(?:Gkgjt|Wkkdj|Sgkdj|NetDicBody)(?=[\\s>]))[^>]*>(.*?)</div>\\s*</div>",
            "<div\\s+class=(?:[\"']kiji[\"']|kiji(?=[\\s>]))[^>]*>(.*?)</div>\\s*(?:<!--|<div\\s+class=(?:[\"']?(?:kijiFoot|wrdb|addInfo)))",
            "<div\\s+class=(?:[\"']kiji[\"']|kiji(?=[\\s>]))[^>]*>(.*?)</div>"
        )
        for (pat in blockPatterns) {
            val matcher = Pattern.compile(pat, Pattern.DOTALL or Pattern.CASE_INSENSITIVE).matcher(sanitizedHtml)
            while (matcher.find()) {
                val candidate = matcher.group(1) ?: ""
                if (candidate.isNotBlank()) {
                    articleBlocks.add(candidate)
                }
            }
            if (articleBlocks.isNotEmpty()) break
        }

        val plainArticle = articleBlocks.joinToString("\n") { block ->
            decodeHtmlEntities(
                block
                    .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                    .replace(Regex("</?(?:ruby|rb|span|a|b|strong|em|i|u|font)[^>]*>", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("</(p|div|li|dt|dd|tr|h\\d)>", RegexOption.IGNORE_CASE), "\n")
                    .replace(Regex("<[^>]+>"), " ")
            )
        }

        // Extract meta description as supplementary/fallback source
        var metaDescription = ""
        val metaMatcher = Pattern.compile(
            "<meta\\s+name=[\"']?description[\"']?\\s+content=[\"'](.*?)[\"']",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        ).matcher(sanitizedHtml)
        if (metaMatcher.find()) {
            metaDescription = decodeHtmlEntities(metaMatcher.group(1) ?: "")
        } else {
            val metaReverseMatcher = Pattern.compile(
                "<meta\\s+content=[\"'](.*?)[\"']\\s+name=[\"']?description[\"']?",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
            ).matcher(sanitizedHtml)
            if (metaReverseMatcher.find()) {
                metaDescription = decodeHtmlEntities(metaReverseMatcher.group(1) ?: "")
            }
        }

        // Try multi-POS section extraction on article text first
        val multiFromArticle = extractMultiPosClassicalFromPlainText(plainArticle)
        if (multiFromArticle != null && multiFromArticle.first.isNotEmpty()) {
            val (defs, conjs) = multiFromArticle
            val conjNote = conjs.distinct().filter { it.isNotBlank() }.joinToString("・")
            return WeblioLookupResult(
                word = word,
                definitions = defs,
                summaryMeaning = defs.joinToString(" / ") { it.meaning },
                level = "",
                eikenGrade = "",
                formattedNote = conjNote,
                conjugation = conjNote
            )
        }

        // Fallback single-POS or meta-description extraction (also checks multi-POS in meta)
        val combinedForPos = "$plainArticle\n$metaDescription"
        val (extractedPos, extractedConjugation) = extractClassicalPosAndConjugationFromText(combinedForPos)

        val definitionsFromArticle = extractClassicalSenses(plainArticle, extractedPos)
        val (definitions, finalConjugation) = if (definitionsFromArticle.isNotEmpty()) {
            definitionsFromArticle to extractedConjugation
        } else {
            val multiFromMeta = extractMultiPosClassicalFromPlainText(stripClassicalMetaPrefix(metaDescription))
            if (multiFromMeta != null && multiFromMeta.first.isNotEmpty()) {
                val conjJoined = multiFromMeta.second.distinct().filter { it.isNotBlank() }.joinToString("・")
                    .ifBlank { extractedConjugation }
                multiFromMeta.first to conjJoined
            } else {
                extractClassicalSensesFromMeta(metaDescription, extractedPos) to extractedConjugation
            }
        }

        if (definitions.isEmpty()) {
            return null
        }

        val summary = definitions.joinToString(" / ") { it.meaning }
        return WeblioLookupResult(
            word = word,
            definitions = definitions,
            summaryMeaning = summary,
            level = "",
            eikenGrade = "",
            formattedNote = finalConjugation,
            conjugation = finalConjugation
        )
    }

    private data class GkzkjEntryBlock(
        val headerText: String,
        val bodyHtml: String
    )

    private fun extractFromGkzkjBlocks(word: String, sanitizedHtml: String): WeblioLookupResult? {
        val entryPattern = Pattern.compile(
            "(?:<h3[^>]*class=(?:[\"'][^\"']*ttl1[^\"']*[\"']|ttl1(?=[\\s>]))[^>]*>(.*?)</h3>\\s*)?" +
                "<div[^>]*class=(?:[\"']Gkzkj[\"']|Gkzkj(?=[\\s>]))[^>]*>(.*?)" +
                "(?=<h3[^>]*class=(?:[\"'][^\"']*ttl1[^\"']*[\"']|ttl1(?=[\\s>]))|<hr\\s+class=(?:[\"']?dot)|<div[^>]*class=(?:[\"']?division)|$)",
            Pattern.DOTALL or Pattern.CASE_INSENSITIVE
        )
        val matcher = entryPattern.matcher(sanitizedHtml)
        val rawBlocks = mutableListOf<GkzkjEntryBlock>()
        while (matcher.find()) {
            val h3 = decodeHtmlEntities((matcher.group(1) ?: "").replace(Regex("<[^>]+>"), "")).trim()
            val body = matcher.group(2) ?: ""
            if (body.isNotBlank()) {
                rawBlocks.add(GkzkjEntryBlock(headerText = h3, bodyHtml = body))
            }
        }
        if (rawBlocks.isEmpty()) return null

        // Prioritize primary headwords (without compound morpheme hyphens '－') over rare poetic compounds
        val orderedBlocks = rawBlocks.sortedBy { if (it.headerText.contains("－")) 1 else 0 }

        val subSplitRegex = Regex("(?=<div[^>]*class=(?:[\"']gkzkjSub[\"']|gkzkjSub(?=[\\s>]))[^>]*>)", RegexOption.IGNORE_CASE)
        val subHeaderPattern = Pattern.compile(
            "<div[^>]*class=(?:[\"']gkzkjSub[\"']|gkzkjSub(?=[\\s>]))[^>]*>(.*?)</div>",
            Pattern.DOTALL or Pattern.CASE_INSENSITIVE
        )
        // Match exact class="lvlB" / class=lvlB and NEVER class="lvlBGBG" (which contains citations/examples/translations)
        val lvlBPattern = Pattern.compile(
            "<p[^>]*class=(?:\"lvlB\"|'lvlB'|lvlB(?=[\\s>]))[^>]*>(.*?)</p>",
            Pattern.DOTALL or Pattern.CASE_INSENSITIVE
        )

        val posBuckets = LinkedHashMap<String, MutableList<String>>()
        val conjugations = mutableListOf<String>()

        for (entry in orderedBlocks) {
            val subSections = entry.bodyHtml.split(subSplitRegex)
            for (sub in subSections) {
                val subMatcher = subHeaderPattern.matcher(sub)
                if (!subMatcher.find()) continue
                val rawPosText = decodeHtmlEntities((subMatcher.group(1) ?: "").replace(Regex("<[^>]+>"), " "))
                    .replace(Regex("^\\s*[\\[［][一二三四五六七八九十0-9０-９]+[\\]］]\\s*"), "")
                    .trim()
                val normalized = normalizeClassicalPosAndConjugation(rawPosText) ?: continue
                val (simplePos, conj) = refineAdjectiveOrAdjectivalConjugation(
                    normalized,
                    decodeHtmlEntities(sub.replace(Regex("<[^>]+>"), " "))
                )
                if (conj.isNotBlank() && conj !in conjugations) {
                    conjugations.add(conj)
                }

                val lvlMatcher = lvlBPattern.matcher(sub)
                val subMeanings = mutableListOf<String>()
                while (lvlMatcher.find() && subMeanings.size < 3) {
                    val rawSense = decodeHtmlEntities((lvlMatcher.group(1) ?: "").replace(Regex("<[^>]+>"), ""))
                        .trim()
                    if (rawSense.startsWith("活用") || rawSense.startsWith("《接続》") || rawSense.startsWith("分類")) {
                        continue
                    }
                    val cleaned = cleanClassicalSenseText(rawSense)
                    if (cleaned.isNotBlank() && cleaned !in subMeanings) {
                        subMeanings.add(cleaned)
                    }
                }

                if (subMeanings.isNotEmpty()) {
                    val bucket = posBuckets.getOrPut(simplePos) { mutableListOf() }
                    for (m in subMeanings) {
                        if (m !in bucket && bucket.size < 4) {
                            bucket.add(m)
                        }
                    }
                }
            }
        }

        if (posBuckets.isEmpty()) return null

        val definitions = if (posBuckets.size == 1) {
            val (onlyPos, senses) = posBuckets.entries.first()
            senses.take(3).map { Definition(pos = onlyPos, meaning = it) }
        } else {
            posBuckets.entries.map { (pos, senses) ->
                Definition(pos = pos, meaning = senses.take(3).joinToString(" / "))
            }
        }

        val conjNote = conjugations.joinToString("・")
        return WeblioLookupResult(
            word = word,
            definitions = definitions,
            summaryMeaning = definitions.joinToString(" / ") { it.meaning },
            level = "",
            eikenGrade = "",
            formattedNote = conjNote,
            conjugation = conjNote
        )
    }

    /**
     * Splits plain text containing 2+ explicit POS headers (e.g., `[一]感動詞 ... [二]名詞 ...` or `【感動詞】... 【名詞】...`)
     * into per-POS definitions and collects all conjugations. Returns null if fewer than 2 POS sections exist.
     */
    private fun extractMultiPosClassicalFromPlainText(text: String): Pair<List<Definition>, List<String>>? {
        if (text.isBlank()) return null

        val headerRegex = Regex(
            "(?:[\\[［(（][一二三四五1-5１-５][\\]］)）]\\s*([ぁ-んァ-ヶ一-龯]{1,18}?)(?=[\\s　①②③④⑤活用《「『]|(?=[ぁ-んァ-ヶ一-龯]))|" +
                "(?:[【［\\[](?:文|古)[】］\\]]\\s*)?[【［\\[]([^】］\\]]{1,24})[】］\\]])"
        )

        data class PosHit(val start: Int, val bodyStart: Int, val pos: String, val conj: String)

        val hits = mutableListOf<PosHit>()
        for (m in headerRegex.findAll(text)) {
            val numberedCandidate = m.groupValues[1]
            val bracketedCandidate = m.groupValues[2]

            if (bracketedCandidate.isNotBlank()) {
                val norm = normalizeClassicalPosAndConjugation(bracketedCandidate) ?: continue
                hits.add(PosHit(m.range.first, m.range.last + 1, norm.first, norm.second))
            } else if (numberedCandidate.isNotBlank()) {
                // Match longest valid POS prefix at start of numbered section (e.g. "[一]感動詞ああ。" -> "感動詞")
                val prefixMatch = Regex(
                    "^(形容動詞(?:ナリ活用|タリ活用)?|形容詞(?:ク活用|シク活用)?|助動詞|(?:自|他)?動詞[ァ-ヴー一-龯]{0,8}(?:四段|上一段|下一段|上二段|下二段|カ行変格|サ行変格|ナ行変格|ラ行変格|カ変|サ変|ナ変|ラ変)(?:活用)?|連体詞|接続詞|感動詞|間投詞|副詞|代名詞|名詞|副助詞|係助詞|接続助詞|終助詞|格助詞|助詞)"
                ).find(text.substring(m.range.first).replace(Regex("^[\\[［(（][一二三四五1-5１-５][\\]］)）]\\s*"), ""))
                if (prefixMatch != null) {
                    val norm = normalizeClassicalPosAndConjugation(prefixMatch.value)
                    if (norm != null) {
                        val numPrefixLen = Regex("^[\\[［(（][一二三四五1-5１-５][\\]］)）]\\s*")
                            .find(text.substring(m.range.first))?.value?.length ?: 0
                        val bodyStart = m.range.first + numPrefixLen + prefixMatch.value.length
                        hits.add(PosHit(m.range.first, bodyStart, norm.first, norm.second))
                    }
                }
            }
        }

        if (hits.size < 2) return null

        val posBuckets = LinkedHashMap<String, MutableList<String>>()
        val conjugations = mutableListOf<String>()

        for (i in hits.indices) {
            val current = hits[i]
            val end = if (i + 1 < hits.size) hits[i + 1].start else text.length
            if (current.bodyStart >= end) continue
            val sectionBody = text.substring(current.bodyStart, end)
            val (refinedPos, refinedConj) = refineAdjectiveOrAdjectivalConjugation(
                current.pos to current.conj,
                sectionBody
            )
            if (refinedConj.isNotBlank() && refinedConj !in conjugations) {
                conjugations.add(refinedConj)
            }
            val senses = extractClassicalSenses(sectionBody, refinedPos)
            if (senses.isNotEmpty()) {
                val bucket = posBuckets.getOrPut(refinedPos) { mutableListOf() }
                for (s in senses) {
                    if (s.meaning.isNotBlank() && s.meaning !in bucket && bucket.size < 4) {
                        bucket.add(s.meaning)
                    }
                }
            }
        }

        if (posBuckets.isEmpty()) return null

        val definitions = if (posBuckets.size == 1) {
            val (onlyPos, senses) = posBuckets.entries.first()
            senses.take(3).map { Definition(pos = onlyPos, meaning = it) }
        } else {
            posBuckets.entries.map { (pos, senses) ->
                Definition(pos = pos, meaning = senses.take(3).joinToString(" / "))
            }
        }
        return definitions to conjugations
    }

    internal fun extractClassicalPosFromText(text: String): String {
        return extractClassicalPosAndConjugationFromText(text).first
    }

    internal fun extractClassicalPosAndConjugationFromText(text: String): Pair<String, String> {
        if (text.isBlank()) return "その他" to ""

        // Priority 1: Explicit classical grammar marker ［文］［動マ下二］ / 【文】【形シク】 (e.g., Digital Daijisen)
        val classicalMarkerMatcher = Pattern.compile(
            "[【［\\[](?:文|古)[】］\\]]\\s*[【［\\[]([^】］\\]]{1,24})[】］\\]]"
        ).matcher(text)
        while (classicalMarkerMatcher.find()) {
            val tag = classicalMarkerMatcher.group(1)?.trim() ?: ""
            val normalized = normalizeClassicalPosAndConjugation(tag)
            if (normalized != null) {
                return refineAdjectiveOrAdjectivalConjugation(normalized, text)
            }
        }

        // Priority 2: Bracketed POS tags: 【形容動詞ナリ活用】, ［動カ四］, ［形シク］, etc.
        val bracketMatcher = Pattern.compile("[【［\\[]([^】］\\]]{1,24})[】］\\]]").matcher(text)
        while (bracketMatcher.find()) {
            val tag = bracketMatcher.group(1)?.trim() ?: ""
            val normalized = normalizeClassicalPosAndConjugation(tag)
            if (normalized != null) {
                return refineAdjectiveOrAdjectivalConjugation(normalized, text)
            }
        }

        // Priority 3: Unbracketed standard dictionary POS lines (e.g., "自動詞カ行四段活用", "形容動詞ナリ活用")
        val inlinePosRegex = Regex(
            "(形容動詞(?:ナリ活用|タリ活用)?|形容詞(?:ク活用|シク活用)?|感動詞|間投詞|助動詞|(?:自|他)?動詞[ァ-ヴー一-龯]{0,8}(?:四段|上一段|下一段|上二段|下二段|カ行変格|サ行変格|ナ行変格|ラ行変格|カ変|サ変|ナ変|ラ変)(?:活用)?|連体詞|接続詞|副詞|代名詞|名詞)"
        )
        val inlineMatch = inlinePosRegex.find(text)
        if (inlineMatch != null) {
            normalizeClassicalPosAndConjugation(inlineMatch.value)?.let {
                return refineAdjectiveOrAdjectivalConjugation(it, text)
            }
        }

        return "その他" to ""
    }

    private fun refineAdjectiveOrAdjectivalConjugation(
        pair: Pair<String, String>,
        fullText: String
    ): Pair<String, String> {
        val (pos, conj) = pair
        if (pos == "形容詞" && conj == "ク活用") {
            if (Regex("[【［\\[]シク[】］\\]]").containsMatchIn(fullText) || fullText.contains("｛しく")) {
                return "形容詞" to "シク活用"
            }
        } else if (pos == "形容動詞" && conj == "ナリ活用") {
            if (Regex("[【［\\[]タリ[】］\\]]").containsMatchIn(fullText) || fullText.contains("｛たら／たり")) {
                return "形容動詞" to "タリ活用"
            }
        }
        return pair
    }

    internal fun normalizeClassicalPos(rawTag: String): String? {
        return normalizeClassicalPosAndConjugation(rawTag)?.first
    }

    internal fun normalizeClassicalPosAndConjugation(rawTag: String): Pair<String, String>? {
        val tag = rawTag.trim().replace(" ", "").replace("　", "")
        if (tag.isBlank()) return null
        // Skip non-POS bracket labels
        if (tag in setOf("訳", "出典", "参考", "語源", "活用", "用例", "類語", "対義語", "注意", "意味", "解説", "文", "古")) {
            return null
        }

        val verbRowTypeRegex = Regex(
            "([アカガサザタダナハバマヤラワ])?行?(上一段|上一|下一段|下一|上二段|上二|下二段|下二|五[（(]四[）)]|四段|四)"
        )

        // Note: 感動詞 and 助動詞 and 形容動詞 MUST be checked BEFORE 動詞 because they contain the substring "動詞" / "動"!
        return when {
            tag.contains("感動詞") || tag.contains("間投詞") || tag == "感" -> "感動詞" to ""
            tag.contains("助動詞") || tag == "助動" -> "助動詞" to ""
            tag.contains("形容動詞") || tag.startsWith("形動") -> {
                val conj = if (tag.contains("タリ")) "タリ活用" else "ナリ活用"
                "形容動詞" to conj
            }
            tag.contains("形容詞") || tag == "形" || tag.startsWith("形ク") || tag.startsWith("形シク") -> {
                val conj = when {
                    tag.contains("シク") -> "シク活用"
                    tag.contains("カリ") || tag.contains("補助") -> "カリ活用"
                    else -> "ク活用"
                }
                "形容詞" to conj
            }
            tag.contains("動詞") || tag.startsWith("動") || tag.startsWith("自動") || tag.startsWith("他動") -> {
                val conj = when {
                    tag.contains("カ変") || tag.contains("カ行変格") -> "カ行変格活用"
                    tag.contains("サ変") || tag.contains("サ行変格") -> "サ行変格活用"
                    tag.contains("ナ変") || tag.contains("ナ行変格") -> "ナ行変格活用"
                    tag.contains("ラ変") || tag.contains("ラ行変格") -> "ラ行変格活用"
                    else -> {
                        val m = verbRowTypeRegex.find(tag)
                        if (m != null) {
                            val row = m.groupValues[1].let { if (it.isNotBlank()) "${it}行" else "" }
                            val type = when (val rawType = m.groupValues[2]) {
                                "上一", "上一段" -> "上一段活用"
                                "下一", "下一段" -> "下一段活用"
                                "上二", "上二段" -> "上二段活用"
                                "下二", "下二段" -> "下二段活用"
                                "四", "四段" -> "四段活用"
                                else -> if (rawType.startsWith("五")) "四段活用" else "${rawType}活用"
                            }
                            "$row$type"
                        } else {
                            ""
                        }
                    }
                }
                "動詞" to conj
            }
            tag.contains("連体詞") || tag == "連体" -> "連体詞" to ""
            tag.contains("接続詞") || tag == "接" -> "接続詞" to ""
            tag.contains("副詞") || tag == "副" -> "副詞" to ""
            tag.contains("名詞") || tag.contains("代名詞") || tag == "名" || tag == "代" -> "名詞" to ""
            tag.contains("連語") || tag.contains("枕詞") || tag.contains("接頭") || tag.contains("接尾") || tag.contains("助詞") -> "その他" to ""
            else -> null
        }
    }

    private fun extractClassicalSenses(articleText: String, pos: String): List<Definition> {
        if (articleText.isBlank()) return emptyList()

        // Strip conjugation table / header noise if present before meanings
        var body = articleText
        if (body.contains("意味")) {
            val afterImi = body.substringAfter("意味")
            if (afterImi.isNotBlank() && afterImi.length >= 4) {
                val prefixLen = body.indexOf("意味")
                if (prefixLen in 0..120) {
                    body = afterImi
                }
            }
        }

        // Case A: Circled numbers ① ② ③ ...
        val circledRegex = Regex("[①②③④⑤]")
        if (circledRegex.containsMatchIn(body)) {
            val parts = body.split(circledRegex).drop(1)
            val defs = parts.mapNotNull { part ->
                val cleaned = cleanClassicalSenseText(part)
                if (cleaned.isNotBlank()) Definition(pos = pos, meaning = cleaned) else null
            }.take(3)
            if (defs.isNotEmpty()) return defs
        }

        // Case B: Numbered senses (1) (2) or １ ２ at line/segment boundaries
        val parenNumRegex = Regex("(?:^|\\n)\\s*(?:\\(\\d+\\)|（[０-９\\d]+）|[１-５1-5][\\.．\\s])")
        if (parenNumRegex.containsMatchIn(body)) {
            val parts = body.split(parenNumRegex).drop(1)
            val defs = parts.mapNotNull { part ->
                val cleaned = cleanClassicalSenseText(part)
                if (cleaned.isNotBlank()) Definition(pos = pos, meaning = cleaned) else null
            }.take(3)
            if (defs.isNotEmpty()) return defs
        }

        // Case C: Single paragraph explanation after POS bracket
        var single = body
        if (single.contains("】")) {
            single = single.substringAfter("】")
        } else if (single.contains("］")) {
            single = single.substringAfter("］")
        }
        val cleanedSingle = cleanClassicalSenseText(single)
        return if (cleanedSingle.isNotBlank()) {
            listOf(Definition(pos = pos, meaning = cleanedSingle))
        } else {
            emptyList()
        }
    }

    private fun stripClassicalMetaPrefix(meta: String): String {
        if (meta.isBlank()) return ""
        var content = meta
            .substringBefore(" - Weblio古語辞典")
            .substringBefore("｜Weblio古語辞典")
            .substringBefore("- 古文辞書なら「Weblio古語辞典」")
            .substringBefore(" - Weblio辞書")
            .trim()

        for (marker in listOf("の意味は", "の意味。", "とは。意味や解説、類語。", "【意味】")) {
            if (content.contains(marker)) {
                content = content.substringAfter(marker).trim()
            }
        }
        content = content.replace(Regex("^読み方[：:][^\\s。・]+\\s*"), "").trim()
        if (content.startsWith("・")) content = content.trimStart('・', ' ')
        return content
    }

    private fun extractClassicalSensesFromMeta(meta: String, pos: String): List<Definition> {
        var content = stripClassicalMetaPrefix(meta)
        if (content.isBlank()) return emptyList()

        // Strip leading POS tag in meta if present
        content = content
            .replace(Regex("^[【［][^】］]+[】］]\\s*"), "")
            .replace(
                Regex("^(?:形容動詞(?:ナリ活用|タリ活用)?|形容詞(?:ク活用|シク活用)?|感動詞|間投詞|助動詞|(?:自|他)?動詞[ァ-ヴー一-龯]{0,8}(?:四段|上一段|下一段|上二段|下二段|カ行変格|サ行変格|ナ行変格|ラ行変格|カ変|サ変|ナ変|ラ変)(?:活用)?|連体詞|接続詞|副詞|代名詞|名詞|副助詞|係助詞|接続助詞|終助詞|格助詞|助詞)\\s*"),
                ""
            )

        val circledRegex = Regex("[①②③④⑤]")
        if (circledRegex.containsMatchIn(content)) {
            val defs = content.split(circledRegex).drop(1).mapNotNull { part ->
                val cleaned = cleanClassicalSenseText(part)
                if (cleaned.isNotBlank()) Definition(pos = pos, meaning = cleaned) else null
            }.take(3)
            if (defs.isNotEmpty()) return defs
        }

        val cleaned = cleanClassicalSenseText(content)
        return if (cleaned.isNotBlank()) {
            listOf(Definition(pos = pos, meaning = cleaned))
        } else {
            emptyList()
        }
    }

    private fun cleanClassicalSenseText(raw: String): String {
        var text = raw
        // Cut off example citations, translations of citations, etymology, conjugation tables, etc.
        val cutMarkers = listOf(
            "出典", "[訳]", "［訳］", "【訳】", "用例", "〔出典〕", "参考", "語源", "類語",
            "[活用]", "［活用］", "【活用】", "活用｛", "「", "『", "▽", "◆", "⇒",
            "Weblio古語辞典", "学研全訳古語辞典"
        )
        for (marker in cutMarkers) {
            val idx = text.indexOf(marker)
            if (idx > 0) {
                text = text.substring(0, idx)
            } else if (idx == 0 && marker != "「" && marker != "『") {
                return ""
            }
        }

        text = text
            .replace(Regex("\\{[^{}]*\\}"), "")
            .replace(Regex("｛[^｛｝]*｝"), "")
            .replace(Regex("《接続》[^。]*。?"), "")
            .replace(Regex("^[・：:\\-―\\s]+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('。', '、', '・', '…', '.')
            .trim()

        if (text.length > 80) {
            text = text.take(80).substringBeforeLast("。").trim()
        }
        return text
    }

    internal fun decodeHtmlEntities(input: String): String {
        if (input.isEmpty()) return input
        // Replace &amp; first so double-encoded entities such as &amp;#9312; decode to ①
        var s = input.replace("&amp;", "&")

        s = Regex("&#(\\d{1,7});").replace(s) { m ->
            val code = m.groupValues[1].toIntOrNull()
            if (code != null && Character.isValidCodePoint(code)) {
                String(Character.toChars(code))
            } else {
                m.value
            }
        }
        s = Regex("&#[xX]([0-9a-fA-F]{1,6});").replace(s) { m ->
            val code = m.groupValues[1].toIntOrNull(16)
            if (code != null && Character.isValidCodePoint(code)) {
                String(Character.toChars(code))
            } else {
                m.value
            }
        }

        return s
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
    }

    internal fun parseWeblioHtml(word: String, html: String): WeblioLookupResult? {
        if (html.isBlank()) return null
        if (html.contains("一致する見出し語は見つかりませんでした") || html.contains("該当する言葉は見つかりませんでした")) {
            return null
        }

        val sanitizedHtml = html
            .replace(Regex("<script.*?</script>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("<style.*?</style>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

        // 1. Extract Learning Level and Eiken Grade (Weblioレベル・英検級)
        var level = ""
        var eikenGrade = ""

        val levelBlockMatcher = Pattern.compile(
            "class=(?:[\"'][^\"']*learningLevel[^\"']*[\"']|learningLevel(?=[\\s>]))[^>]*>(.*?)</div>",
            Pattern.DOTALL or Pattern.CASE_INSENSITIVE
        ).matcher(sanitizedHtml)

        if (levelBlockMatcher.find()) {
            val levelBlock = decodeHtmlEntities(levelBlockMatcher.group(1) ?: "")

            val levelMatcher = Pattern.compile("レベル[：:]\\s*(レベル\\s*\\d+|\\d+)").matcher(levelBlock)
            if (levelMatcher.find()) {
                val rawLevel = levelMatcher.group(1)?.replace("\\s+".toRegex(), "") ?: ""
                level = if (rawLevel.startsWith("レベル")) rawLevel else "レベル$rawLevel"
            }

            val eikenMatcher = Pattern.compile("英検[：:]\\s*([^<]+)").matcher(levelBlock)
            if (eikenMatcher.find()) {
                val eikenRaw = eikenMatcher.group(1)?.trim() ?: ""
                val gradeMatcher = Pattern.compile("(1級|準1級|2級|準2級|3級|4級|5級)").matcher(eikenRaw)
                eikenGrade = if (gradeMatcher.find()) {
                    "英検" + gradeMatcher.group(1)
                } else {
                    if (eikenRaw.startsWith("英検")) eikenRaw else "英検: $eikenRaw"
                }
            }
        }

        val noteParts = mutableListOf<String>()
        if (level.isNotBlank()) {
            noteParts.add("Weblio$level")
        }
        if (eikenGrade.isNotBlank()) {
            noteParts.add(eikenGrade)
        }
        val formattedNote = noteParts.joinToString(" / ")

        // 2. Primary Multi-POS Extraction from Kejje (研究社 新英和中辞典) or other ttl2 dictionary blocks
        val structuredDefinitions = extractMultiPosEnglishDefinitions(word, sanitizedHtml)
        if (structuredDefinitions.isNotEmpty()) {
            val summary = structuredDefinitions.joinToString(" / ") { def ->
                if (structuredDefinitions.size > 1) "[${def.pos}] ${def.meaning}" else def.meaning
            }
            return WeblioLookupResult(
                word = word,
                definitions = structuredDefinitions,
                summaryMeaning = summary,
                level = level,
                eikenGrade = eikenGrade,
                formattedNote = formattedNote
            )
        }

        // 3. Fallback: Extract from content-explanation / quickDefinition explanation / meta description
        var meaningText = ""
        val explanationPatterns = listOf(
            "class=(?:[\"'][^\"']*content-explanation[^\"']*[\"']|content-explanation(?=[\\s>]))[^>]*>(.*?)</(?:td|div|p)>",
            "<span\\s+class=(?:[\"']explanation[\"']|explanation(?=[\\s>]))[^>]*>(.*?)</span>"
        )
        for (pat in explanationPatterns) {
            val explanationMatcher = Pattern.compile(pat, Pattern.DOTALL or Pattern.CASE_INSENSITIVE).matcher(sanitizedHtml)
            if (explanationMatcher.find()) {
                var raw = decodeHtmlEntities((explanationMatcher.group(1) ?: "").replace(Regex("<[^>]+>"), " "))
                for (cut in listOf("主な例文", "音節", "発音記号", "コア", "覚え方", "大学入試")) {
                    if (raw.contains(cut)) {
                        raw = raw.substringBefore(cut)
                    }
                }
                meaningText = raw.split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ").trim()
                if (meaningText.isNotBlank()) break
            }
        }

        var metaRaw = ""
        val metaMatcher = Pattern.compile(
            "<meta\\s+name=[\"']?description[\"']?\\s+content=[\"'](.*?)[\"']",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        ).matcher(sanitizedHtml)
        if (metaMatcher.find()) {
            metaRaw = decodeHtmlEntities(metaMatcher.group(1) ?: "")
        }

        if (meaningText.isBlank() && metaRaw.isNotBlank()) {
            if (metaRaw.contains("の意味・翻訳・日本語")) {
                val after = metaRaw.substringAfter("の意味・翻訳・日本語").trimStart('-', ' ')
                val clean = after.substringBefore("｜").substringBefore(" - ").substringBefore("Weblio").trim()
                if (clean.isNotBlank()) meaningText = clean
            } else if (metaRaw.contains("【") && metaRaw.contains("】")) {
                val after = metaRaw.substringAfter("【").let { "【$it" }
                val clean = after.substringBefore(" - ").substringBefore("Weblio").trim()
                if (clean.isNotBlank()) meaningText = clean
            }
        }

        // Check if meaningText or metaRaw contains inline multi-POS markers like "【名】本、書籍【動】予約する"
        val inlineMultiDefs = extractInlineEnglishPosDefinitions(meaningText.ifBlank { metaRaw })
        if (inlineMultiDefs.size >= 2) {
            val summary = inlineMultiDefs.joinToString(" / ") { "[${it.pos}] ${it.meaning}" }
            return WeblioLookupResult(
                word = word,
                definitions = inlineMultiDefs,
                summaryMeaning = summary,
                level = level,
                eikenGrade = eikenGrade,
                formattedNote = formattedNote
            )
        } else if (inlineMultiDefs.size == 1 && meaningText.startsWith("【")) {
            return WeblioLookupResult(
                word = word,
                definitions = inlineMultiDefs,
                summaryMeaning = inlineMultiDefs.first().meaning,
                level = level,
                eikenGrade = eikenGrade,
                formattedNote = formattedNote
            )
        }

        if (meaningText.isBlank()) {
            return null
        }

        // Single-POS fallback determination
        val posCandidates = extractPosListFromIndex(sanitizedHtml)
        val primaryPos = posCandidates.firstOrNull() ?: run {
            val bracketPosMatcher = Pattern.compile("【([^】]{1,12})】").matcher(sanitizedHtml)
            var found = ""
            while (bracketPosMatcher.find()) {
                val normalized = normalizeEnglishPos(bracketPosMatcher.group(1) ?: "")
                if (normalized != null) {
                    found = normalized
                    break
                }
            }
            found.ifBlank { "名詞" }
        }

        val definition = Definition(pos = primaryPos, meaning = meaningText)
        return WeblioLookupResult(
            word = word,
            definitions = listOf(definition),
            summaryMeaning = meaningText,
            level = level,
            eikenGrade = eikenGrade,
            formattedNote = formattedNote
        )
    }

    private fun extractMultiPosEnglishDefinitions(word: String, sanitizedHtml: String): List<Definition> {
        val posGlossMap = LinkedHashMap<String, MutableList<String>>()

        // Prefer Kejje (研究社 新英和中辞典) blocks first
        val kejjePattern = Pattern.compile(
            "<div[^>]*class=(?:[\"']Kejje[\"']|Kejje(?=[\\s>]))[^>]*>(.*?)" +
                "(?=<div[^>]*class=(?:[\"']?(?:Kejje\\b|division2|subDivision))|<hr\\s+class=(?:[\"']?dot)|$)",
            Pattern.DOTALL or Pattern.CASE_INSENSITIVE
        )
        val kejjeMatcher = kejjePattern.matcher(sanitizedHtml)
        val kejjeBlocks = mutableListOf<String>()
        while (kejjeMatcher.find()) {
            val kblock = kejjeMatcher.group(1) ?: ""
            if (kblock.isNotBlank()) {
                val osMatcher = Pattern.compile(
                    "<b[^>]*class=(?:[\"']KejjeOs[\"']|KejjeOs(?=[\\s>]))[^>]*>(.*?)</b>",
                    Pattern.DOTALL or Pattern.CASE_INSENSITIVE
                ).matcher(kblock)
                if (osMatcher.find()) {
                    val hw = decodeHtmlEntities((osMatcher.group(1) ?: "").replace(Regex("<[^>]+>"), ""))
                        .replace("・", "")
                        .trim()
                    val cleanTarget = word.trim()
                    if (!cleanTarget.endsWith(".") && hw.endsWith(".")) {
                        continue // Skip abbreviations like "Can." / "can." when looking up "can"
                    }
                    if (hw.isNotBlank() && !hw.equals(cleanTarget, ignoreCase = true)) {
                        continue
                    }
                }
                kejjeBlocks.add(kblock)
            }
        }

        if (kejjeBlocks.isNotEmpty()) {
            for (kblock in kejjeBlocks) {
                collectTtl2SectionsIntoMap(kblock, posGlossMap)
            }
        } else {
            // Fallback to other dictionary blocks that use <h4 class="ttl2"> (e.g., Nwnej, Wiktionary) before example lists
            val beforeExamples = sanitizedHtml.substringBefore("を含む例文一覧")
            collectTtl2SectionsIntoMap(beforeExamples, posGlossMap)
        }

        return posGlossMap.entries
            .filter { it.value.isNotEmpty() }
            .map { (pos, glosses) ->
                Definition(pos = pos, meaning = glosses.joinToString("、"))
            }
    }

    private fun collectTtl2SectionsIntoMap(blockHtml: String, posGlossMap: LinkedHashMap<String, MutableList<String>>) {
        val withoutExamples = blockHtml
            .replace(Regex("<span[^>]*class=(?:[\"']?smpYrLabel[\"']?)[^>]*>.*?</span>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("<table[^>]*class=(?:[\"']?[^\"'>]*Yr[^\"'>]*[\"']?)[^>]*>.*?</table>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("<div[^>]*class=(?:[\"']?[^\"'>]*Yr[^\"'>]*[\"']?)[^>]*>.*?</div>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")

        val sections = withoutExamples.split(Regex("(?=<h4[^>]*class=(?:[\"'][^\"']*ttl2[^\"']*[\"']|ttl2(?=[\\s>]))[^>]*>)", RegexOption.IGNORE_CASE))
        val ttl2Pattern = Pattern.compile(
            "^<h4[^>]*class=(?:[\"'][^\"']*ttl2[^\"']*[\"']|ttl2(?=[\\s>]))[^>]*>(.*?)</h4>(.*)$",
            Pattern.DOTALL or Pattern.CASE_INSENSITIVE
        )

        for (sec in sections) {
            val hm = ttl2Pattern.matcher(sec.trim())
            if (!hm.find()) continue
            val headerPlain = decodeHtmlEntities((hm.group(1) ?: "").replace(Regex("<[^>]+>"), " ")).trim()
            if (headerPlain.contains("語源")) {
                break
            }
            val pos = normalizeEnglishPos(headerPlain) ?: continue

            var bodyHtml = hm.group(2) ?: ""
            for (cut in listOf("としての「", "イディオムやフレーズ", "主な例文", "印欧語根")) {
                if (bodyHtml.contains(cut)) {
                    bodyHtml = bodyHtml.substringBefore(cut)
                }
            }

            val rawLines = bodyHtml.split(Regex("<(?:br|/dt|/dd|/div|/p|/li)[^>]*>", RegexOption.IGNORE_CASE))
            val sectionGlosses = mutableListOf<String>()
            for (rawLine in rawLines) {
                val plainLine = rawLine.replace(Regex("<[^>]+>"), "").trim()
                if (plainLine.isBlank()) continue
                for (gloss in cleanEnglishSenseLine(plainLine)) {
                    if (gloss !in sectionGlosses && sectionGlosses.size < 4) {
                        sectionGlosses.add(gloss)
                    }
                }
                if (sectionGlosses.size >= 4) break
            }

            if (sectionGlosses.isNotEmpty()) {
                val bucket = posGlossMap.getOrPut(pos) { mutableListOf() }
                for (g in sectionGlosses) {
                    if (g !in bucket && bucket.size < 6) {
                        bucket.add(g)
                    }
                }
            }
        }
    }

    internal fun normalizeEnglishPos(rawTag: String): String? {
        val t = rawTag.trim()
        if (t.isBlank()) return null
        if (listOf("語源", "発音", "変化", "分節", "類語", "解説", "用法").any { t.contains(it) }) {
            return null
        }
        return when {
            t.contains("助動") -> "助動詞"
            t.contains("動") -> "動詞"
            t.contains("代名") || t == "代" -> "代名詞"
            t.contains("名") -> "名詞"
            t.contains("形") -> "形容詞"
            t.contains("副") -> "副詞"
            t.contains("前") -> "前置詞"
            t.contains("接頭") || t.contains("接尾") -> "その他"
            t.contains("接") -> "接続詞"
            t.contains("疑問") -> "疑問詞"
            t.contains("間") || t.contains("感") -> "間投詞"
            t.contains("冠") -> "冠詞"
            else -> null
        }
    }

    private fun cleanEnglishSenseLine(rawLine: String): List<String> {
        var t = decodeHtmlEntities(rawLine)
        for (cut in listOf("としての「", "イディオムやフレーズ", "主な例文", "印欧語根")) {
            if (t.contains(cut)) {
                t = t.substringBefore(cut)
            }
        }

        t = t
            .replace(Regex("《[^》]*》"), "")
            .replace(Regex("【[^】]*】"), "")
            .replace(Regex("〔[^〕]*〕"), "")

        // Repeatedly strip nested [...] and ［...］ usage notes
        repeat(3) {
            t = t.replace(Regex("\\[[^\\[\\]]*\\]"), "")
            t = t.replace(Regex("［[^［］]*］"), "")
        }

        t = t
            .replace(Regex("〈[a-zA-Z0-9,\\s，;；'’-]+〉"), "")
            .replace(Regex("\\((?:cf\\.|→|⇔|比較|複数|単数|過去|現在|通例|主に)[^)]*\\)"), "")
            .replace(Regex("\\([a-zA-Z・\\s/;ˈˌəɪʊæɑɔʌɛː`'’-]+\\)"), "")
            .replace(Regex("[⇒→＝=][^.;。]*"), "")

        // Strip leading sense identifiers (e.g. "A 1 a ", "2 b ") only at clause start so numbers inside glosses (e.g. "缶1杯") stay intact
        t = t.replace(Regex("^\\s*(?:[A-D]\\s+)?(?:\\d{1,2}\\s+)?(?:[a-f]\\s+)?"), "")
        t = t.replace(Regex("(?<=[.;。])\\s*(?:[A-D]\\s+)?(?:\\d{1,2}\\s+)?(?:[a-f]\\s+)?"), " ")

        t = t
            .replace('，', '、')
            .replace(',', '、')
            .replace(';', '、')
            .replace('；', '、')
            .replace(Regex("\\s+"), " ")
            .trim()

        val parts = t.split(Regex("[.。、]+"))
        val result = mutableListOf<String>()
        val jpCharRegex = Regex("[ぁ-んァ-ヶ一-龯]")
        val longAsciiWordRegex = Regex("[a-zA-Z]{3,}")

        for (rawPart in parts) {
            val p = rawPart
                .replace(Regex("^\\s*(?:\\d{1,2}|[a-fA-D])\\s+"), "")
                .trim(' ', '.', '、', ':', '：', '・')
            if (p.isBlank() || p == "現…" || p == "…") continue
            if (jpCharRegex.containsMatchIn(p) && !longAsciiWordRegex.containsMatchIn(p)) {
                if (p !in result) {
                    result.add(p)
                }
            }
        }
        return result
    }

    private fun extractInlineEnglishPosDefinitions(text: String): List<Definition> {
        if (text.isBlank()) return emptyList()
        val markerRegex = Regex("[【［\\[]([^】］\\]]{1,10})[】］\\]]")
        data class InlineHit(val start: Int, val bodyStart: Int, val pos: String)

        val hits = mutableListOf<InlineHit>()
        for (m in markerRegex.findAll(text)) {
            val pos = normalizeEnglishPos(m.groupValues[1]) ?: continue
            hits.add(InlineHit(m.range.first, m.range.last + 1, pos))
        }
        if (hits.isEmpty()) return emptyList()

        val posMap = LinkedHashMap<String, MutableList<String>>()
        for (i in hits.indices) {
            val h = hits[i]
            val end = if (i + 1 < hits.size) hits[i + 1].start else text.length
            if (h.bodyStart >= end) continue
            val segment = text.substring(h.bodyStart, end)
                .substringBefore(" - ")
                .substringBefore("｜")
                .substringBefore("Weblio")
                .trim(' ', '、', ',', ';', '；', '.', '。', ':')
            if (segment.isNotBlank() && Regex("[ぁ-んァ-ヶ一-龯]").containsMatchIn(segment)) {
                val bucket = posMap.getOrPut(h.pos) { mutableListOf() }
                if (segment !in bucket) bucket.add(segment)
            }
        }

        return posMap.entries.map { (pos, list) ->
            Definition(pos = pos, meaning = list.joinToString("、"))
        }
    }

    private fun extractPosListFromIndex(sanitizedHtml: String): List<String> {
        val result = mutableListOf<String>()
        val posIndexMatcher = Pattern.compile(
            "<div\\s+class=(?:[\"']partOfSpeechIndex[\"']|partOfSpeechIndex(?=[\\s>]))[^>]*>(.*?)</div>",
            Pattern.DOTALL or Pattern.CASE_INSENSITIVE
        ).matcher(sanitizedHtml)
        if (posIndexMatcher.find()) {
            val block = decodeHtmlEntities(posIndexMatcher.group(1) ?: "")
            val titleMatcher = Pattern.compile("title=[\"']([^\"']+)[\"']").matcher(block)
            while (titleMatcher.find()) {
                val title = titleMatcher.group(1) ?: ""
                val pos = normalizeEnglishPos(title)
                if (pos != null && pos !in result) {
                    result.add(pos)
                }
            }
        }
        return result
    }
}
