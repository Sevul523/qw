package com.example.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import com.example.data.model.ClassicalWordKnowledgeBase
import com.example.data.service.InternalDictionaryService
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

object OcrExtractor {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val japaneseRecognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }

    /**
     * 基本ストップワード（感度: 高・中・低 すべてで除外）
     * 純粋な機能語（冠詞・代名詞・助動詞・基本前置詞・接続詞）のみに限定し、
     * 高感度モードでは多義語や基本動詞・名詞（make, take, give, work, people, time 等）も確実に拾えるようにする。
     */
    private val BASIC_STOP_WORDS = setOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "her", "she",
        "or", "an", "will", "my", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "which", "me", "when",
        "can", "no", "him", "into", "your", "some", "could", "them",
        "than", "then", "now", "its", "also", "how", "our", "because",
        "any", "these", "us", "are", "was", "were", "been", "has", "had",
        "did", "does", "am", "is"
    )

    /**
     * 古語OCR抽出時の基本除外ストップワード（現代語機能語・問題文メタ語彙・品詞枠ノイズ）
     */
    private val CLASSICAL_BASIC_STOP_WORDS = setOf(
        "これ", "それ", "あれ", "どれ", "ここ", "そこ", "あそこ", "どこ", "この", "その", "あの", "どの",
        "こと", "もの", "ため", "よう", "とき", "ところ", "なか", "うえ", "した", "まえ",
        "から", "より", "まで", "など", "にて", "して", "とも", "ども", "けれど", "ながら", "つつ",
        "する", "した", "され", "ある", "いる", "なる", "ない", "なく", "ます", "です", "だっ",
        "といふ", "という", "といった", "における", "について", "によって", "として",
        // 単語帳・問題集のメタ見出し・品詞枠ノイズ
        "意味", "現代語訳", "口語訳", "品詞", "活用", "語源", "例文", "用例", "出典", "参考", "類語", "対義語",
        "問題", "解答", "解説", "傍線", "傍線部", "次の", "文章", "古文", "単語", "古語", "重要", "頻出",
        "動詞", "形容詞", "形容動詞", "名詞", "副詞", "助動詞", "助詞", "連体詞", "接続詞", "感動詞",
        "四段", "上一段", "下一段", "上二段", "下二段", "カ変", "サ変", "ナ変", "ラ変",
        "ク活用", "シク活用", "ナリ活用", "タリ活用", "自動詞", "他動詞", "敬語", "尊敬", "謙譲", "丁寧",
        "チェック", "ポイント", "レベル", "ページ", "章", "節", "番", "問"
    )

    /**
     * 古語OCR抽出の中・低感度で除外する現代語説明ノイズ
     */
    private val CLASSICAL_MODERN_EXPLANATION_NOISE = setOf(
        "気持ち", "様子", "状態", "場合", "理由", "言葉", "人物", "時代", "世界", "自然", "自分", "相手",
        "現代", "現代語", "ニュアンス", "プラス", "マイナス", "ポイント", "チェック", "テスト", "共通テスト",
        "入試", "大学", "受験", "選択", "選択肢", "適切", "最も", "選び", "答え", "なさい", "ページ", "章",
        "あり", "たり", "なり", "けり", "ほど", "うち"
    )

    /**
     * 四角いチェックボックス `□`、罫線 `｜` `―`、チェックマーク `✓`、箇条書き記号などが
     * 日本語OCRで誤認識されやすい語頭・語尾ノイズ文字（辞書検索ステージでのトリミング用）
     */
    private val CLASSICAL_EDGE_NOISE_CHARS = setOf(
        '口', 'ロ', '日', '目', '田', '回', '品', 'レ', 'ノ', 'メ', 'ト', 'イ', 'ハ', 'ニ', '丨', '｜', '|',
        '-', '―', '─', '━', '│', '┃', '┌', '┐', '└', '┘', '├', '┤', '┬', '┴', '┼',
        '□', '■', '▫', '▪', '○', '●', '◎', '◇', '◆', '△', '▲', '▽', '▼', '☆', '★',
        '✓', '✔', '☑', '☒', '✗', '✘', '※', '〒', '→', '←', '↑', '↓', '⇒', '⇔'
    )

    private val classicalPatternIndex: List<Pair<String, String>> by lazy {
        val pairs = mutableMapOf<String, String>()
        val entriesByWord = ClassicalWordKnowledgeBase.ALL_ENTRIES.associateBy { it.word.trim() }

        fun registerHeadwordForms(surfaceHead: String, canonicalHead: String, posJoined: String) {
            if (surfaceHead.length < 2 || surfaceHead.contains("〜")) return
            pairs.putIfAbsent(surfaceHead, canonicalHead)

            // ナリ・タリ活用形容動詞の語幹・連体・連用マッチ
            if ((surfaceHead.endsWith("なり") || surfaceHead.endsWith("たり")) && surfaceHead.length >= 3) {
                val stem = surfaceHead.dropLast(2)
                if (stem.length >= 2) {
                    pairs.putIfAbsent(stem, canonicalHead)
                }
                if (surfaceHead.endsWith("なり")) {
                    pairs.putIfAbsent("${stem}なる", canonicalHead)
                    pairs.putIfAbsent("${stem}なれ", canonicalHead)
                    pairs.putIfAbsent("${stem}なら", canonicalHead)
                    if (stem.length >= 2) pairs.putIfAbsent("${stem}に", canonicalHead)
                } else {
                    pairs.putIfAbsent("${stem}たる", canonicalHead)
                    pairs.putIfAbsent("${stem}たれ", canonicalHead)
                    pairs.putIfAbsent("${stem}たら", canonicalHead)
                    if (stem.length >= 2) pairs.putIfAbsent("${stem}と", canonicalHead)
                }
            }
            // 形容詞の連用・連体・已然形マッチ（漢字語幹1文字＋し 例: 悲し・美し・憂し も対応）
            val startsWithKanji = surfaceHead.first() in '\u4E00'..'\u9FFF'
            val minAdjLen = if (startsWithKanji) 2 else 3
            if (posJoined.contains("形容詞") && surfaceHead.endsWith("し") && surfaceHead.length >= minAdjLen) {
                val stem = if (posJoined.contains("シク")) surfaceHead else surfaceHead.dropLast(1)
                for (suffix in listOf("く", "う", "き", "けれ", "かり", "かる", "から")) {
                    val form = stem + suffix
                    if (form.length >= 2) pairs.putIfAbsent(form, canonicalHead)
                }
            }
            // 四段・二段・変格動詞の主要活用形マッチ（漢字語幹1文字＋送り仮名 例: 驚く・頼む・忍ぶ も対応）
            val minVerbLen = if (startsWithKanji) 2 else 3
            if (posJoined.contains("動詞") && !posJoined.contains("形容動詞") && !posJoined.contains("助動詞") && surfaceHead.length >= minVerbLen) {
                val stem = surfaceHead.dropLast(1)
                val lastChar = surfaceHead.last()
                val endings = when (lastChar) {
                    'く' -> listOf("か", "き", "け")
                    'ぐ' -> listOf("が", "ぎ", "げ")
                    'す' -> listOf("さ", "し", "せ")
                    'つ' -> listOf("た", "ち", "て", "つる", "つれ")
                    'づ' -> listOf("で", "づる", "づれ")
                    'ぬ' -> listOf("な", "に", "ね", "ぬる", "ぬれ")
                    'ぶ' -> listOf("ば", "び", "べ", "ぶる", "ぶれ")
                    'む' -> listOf("ま", "み", "め", "むる", "むれ")
                    'る' -> listOf("ら", "り", "れ", "るる", "るれ")
                    'ふ' -> listOf("は", "ひ", "へ", "ふる", "ふれ")
                    'ゆ' -> listOf("え", "ゆる", "ゆれ")
                    'ず' -> listOf("ぜ", "じ", "ずる", "ずれ")
                    else -> emptyList()
                }
                for (end in endings) {
                    val form = stem + end
                    if (form.length >= 2) pairs.putIfAbsent(form, canonicalHead)
                }
            }
        }

        for (entry in ClassicalWordKnowledgeBase.ALL_ENTRIES) {
            val rawHead = entry.word.trim()
            val head = rawHead.substringBefore("〜").trim()
            if (head.length < 2) continue
            pairs[head] = head
            val posJoined = entry.definitions.joinToString(" ") { it.pos } + " " + entry.conjugation
            registerHeadwordForms(head, head, posJoined)
        }

        // Also index Kanji classical headwords & their inflections (e.g., 驚く/驚きて, 悲し/悲しく, 美し/美しき, 立ち寄る, 心細し)
        for ((kanjiSurface, kanaCanonical) in ClassicalWordKnowledgeBase.KANJI_HEADWORD_ALIASES) {
            if (kanjiSurface.length < 2) continue
            val targetEntry = entriesByWord[kanaCanonical] ?: continue
            val posJoined = targetEntry.definitions.joinToString(" ") { it.pos } + " " + targetEntry.conjugation
            registerHeadwordForms(kanjiSurface, kanaCanonical, posJoined)
        }

        pairs.entries.map { it.key to it.value }.sortedByDescending { it.first.length }
    }

    /**
     * ML Kit の認識結果オブジェクト [Text] から、[sensitivity]（「どこまでを文字とみなすか」）の基準に従って
     * 文字確信度・相対文字サイズ・罫線形状・端の記号ノイズを判定し、テキストを再構成する。
     */
    internal fun sanitizeVisionText(
        visionText: Text,
        isClassical: Boolean,
        sensitivity: OcrSensitivity = OcrSensitivity.HIGH
    ): String {
        val blocks = visionText.textBlocks
        if (blocks.isEmpty()) return visionText.text

        val lineThicknesses = blocks
            .flatMap { it.lines }
            .mapNotNull { line ->
                line.boundingBox?.let { box ->
                    min(box.width(), box.height()).takeIf { it > 0 }
                }
            }
            .sorted()

        val medianLineThickness = if (lineThicknesses.isNotEmpty()) {
            lineThicknesses[lineThicknesses.size / 2].toFloat()
        } else {
            0f
        }

        val cleanedLines = mutableListOf<String>()

        for (block in blocks) {
            for (line in block.lines) {
                val rawLineTrimmed = line.text.trim()
                if (rawLineTrimmed.isEmpty()) continue

                val lineConf = line.confidence
                if (lineConf > 0f && lineConf < sensitivity.minElementConfidence * 0.85f) {
                    continue
                }

                val lineBox = line.boundingBox
                val lineThickness = if (lineBox != null) {
                    min(lineBox.width(), lineBox.height()).toFloat()
                } else {
                    medianLineThickness
                }

                // Filter out lines smaller than sensitivity's relative character size floor
                if (medianLineThickness > 0f && lineThickness > 0f) {
                    val maxTinyChars = if (sensitivity == OcrSensitivity.LOW) 6 else 2
                    if (lineThickness < medianLineThickness * sensitivity.minRelativeSizeRatio &&
                        rawLineTrimmed.length <= maxTinyChars
                    ) {
                        continue
                    }
                }

                val isVerticalLine = lineBox != null && lineBox.height() > lineBox.width() * 1.6f

                val keptElements = mutableListOf<Pair<Rect?, String>>()
                for (element in line.elements) {
                    val cleanedElement = sanitizeVisionElement(element, lineThickness, isClassical, sensitivity)
                    if (cleanedElement.isNotBlank()) {
                        keptElements.add(element.boundingBox to cleanedElement)
                    }
                }

                if (keptElements.isNotEmpty()) {
                    if (!isClassical) {
                        cleanedLines.add(keptElements.joinToString(" ") { it.second })
                    } else {
                        // In Japanese OCR, join adjacent elements on the same line WITHOUT spaces so words like
                        // "驚"+"く" or "あはれ"+"なり" stay intact, while inserting a space across column/word gaps.
                        val sb = StringBuilder()
                        for (i in keptElements.indices) {
                            val (currBox, currText) = keptElements[i]
                            if (i > 0) {
                                val prevBox = keptElements[i - 1].first
                                val hasColumnGap = if (prevBox != null && currBox != null && lineThickness > 0f) {
                                    val gap = if (isVerticalLine) {
                                        currBox.top - prevBox.bottom
                                    } else {
                                        currBox.left - prevBox.right
                                    }
                                    gap > lineThickness * 0.65f
                                } else {
                                    false
                                }
                                if (hasColumnGap) sb.append(' ')
                            }
                            sb.append(currText)
                        }
                        cleanedLines.add(sb.toString())
                    }
                }
            }
        }

        return cleanedLines.joinToString("\n")
    }

    private fun sanitizeVisionElement(
        element: Text.Element,
        lineThickness: Float,
        isClassical: Boolean,
        sensitivity: OcrSensitivity
    ): String {
        val rawElemText = element.text.trim()
        if (rawElemText.isEmpty()) return ""

        // 1. ML Kit character recognition confidence check ("どこまでを文字とみなすか")
        val elemConf = element.confidence
        if (elemConf > 0f && elemConf < sensitivity.minElementConfidence) {
            return ""
        }

        // 2. Relative stroke size & ruled-line / checkbox aspect ratio check
        val elemBox = element.boundingBox
        val ruleAspectLimit = when (sensitivity) {
            OcrSensitivity.LOW -> 3.8f
            OcrSensitivity.MEDIUM -> 4.6f
            OcrSensitivity.HIGH -> 5.4f
        }
        if (lineThickness > 0f && elemBox != null) {
            val eh = elemBox.height().toFloat()
            val ew = elemBox.width().toFloat()
            val elemThickness = min(eh, ew)
            if (elemThickness < lineThickness * sensitivity.minRelativeSizeRatio &&
                (rawElemText.length == 1 || sensitivity == OcrSensitivity.LOW)
            ) {
                return ""
            }
            // Isolated vertical table divider misread as '|', '｜', '丨', 'l', 'I', '1'
            if (rawElemText.length == 1 && eh > ew * ruleAspectLimit && rawElemText[0] in "|｜丨!！1lI") {
                return ""
            }
            // Isolated horizontal divider misread as '-', '_', '―', '─', '━'
            if (rawElemText.length <= 2 && ew > eh * ruleAspectLimit && rawElemText.all { it in "-_―─━~" }) {
                return ""
            }
        }

        // 3. Symbol-level confidence & edge non-character trimming
        val symbols = element.symbols
        if (symbols.size >= 2) {
            var startIdx = 0
            var endIdx = symbols.lastIndex

            while (startIdx < endIdx && isEdgeSymbolFilteredOut(symbols[startIdx], sensitivity)) {
                startIdx++
            }
            while (endIdx > startIdx && isEdgeSymbolFilteredOut(symbols[endIdx], sensitivity)) {
                endIdx--
            }

            // In LOW sensitivity ("くっきりした文字のみ"), reject elements whose internal glyphs are low-confidence smudges
            if (sensitivity == OcrSensitivity.LOW) {
                val hasBlurryInternalGlyph = (startIdx..endIdx).any { idx ->
                    val sc = symbols[idx].confidence
                    sc > 0f && sc < sensitivity.minSymbolConfidence * 0.85f
                }
                if (hasBlurryInternalGlyph) return ""
            }

            if (!isClassical && sensitivity != OcrSensitivity.LOW && elemBox != null && elemBox.height() > 0) {
                val elemHeight = elemBox.height().toFloat()
                for (leadCount in 1..2) {
                    val splitAfter = startIdx + leadCount - 1
                    if (splitAfter < endIdx && (endIdx - splitAfter) >= 3) {
                        val leadChars = symbols.subList(startIdx, splitAfter + 1).joinToString("") { it.text }
                        val prevBox = symbols[splitAfter].boundingBox
                        val nextBox = symbols[splitAfter + 1].boundingBox
                        if (leadChars.isNotEmpty() &&
                            leadChars.all { it in "liocvxjdbpuqLIOCVXJDBPUQ01" } &&
                            prevBox != null &&
                            nextBox != null &&
                            (nextBox.left - prevBox.right).toFloat() > elemHeight * 0.72f
                        ) {
                            val remainder = symbols.subList(splitAfter + 1, endIdx + 1).joinToString("") { it.text }
                            if (EnglishWordValidator.isValidEnglishWord(remainder)) {
                                return remainder
                            }
                        }
                    }
                }
            }

            if (startIdx > 0 || endIdx < symbols.lastIndex) {
                return symbols.subList(startIdx, endIdx + 1).joinToString("") { it.text }
            }
        }

        return rawElemText
    }

    private fun isEdgeSymbolFilteredOut(symbol: Text.Symbol, sensitivity: OcrSensitivity): Boolean {
        val ch = symbol.text.firstOrNull() ?: return true
        if (ch in "[](){}【】［］（）「」『』<>＜＞|｜丨/\\!！?？_~・•●○□■▫▪✓✔☑☒✗✘★☆※") {
            return true
        }
        val symConf = symbol.confidence
        if (symConf > 0f && symConf < sensitivity.minSymbolConfidence) {
            // Trim low-confidence ambiguous edge strokes (checkbox corners, smudges, rules)
            if (sensitivity == OcrSensitivity.LOW || ch in "liocvxjdbpuqLIOCVXJDBPUQ01口ロ日回品レノメトイハニ") {
                return true
            }
        }
        return false
    }

    suspend fun extractTextFromImageUri(
        context: Context,
        imageUri: Uri,
        isClassical: Boolean = false,
        sensitivity: OcrSensitivity = OcrSensitivity.HIGH
    ): Result<String> {
        return try {
            if (!isClassical) {
                EnglishWordValidator.ensureInitialized(context)
            }
            val activeRecognizer = if (isClassical) japaneseRecognizer else recognizer

            // Decode upright, safely-scaled bitmap (upscales tiny cropped screenshots & prevents OOM on 50MP photos)
            val normalizedBitmap = loadNormalizedBitmap(context, imageUri)
            if (normalizedBitmap != null) {
                val passTexts = mutableListOf<String>()

                // Pass 1: Standard upright bitmap (all sensitivities)
                val pass1Text = runRecognizerPass(
                    activeRecognizer = activeRecognizer,
                    inputImage = InputImage.fromBitmap(normalizedBitmap, 0),
                    isClassical = isClassical,
                    sensitivity = sensitivity
                )
                if (pass1Text.isNotBlank()) passTexts.add(pass1Text)

                // Pass 2 (MEDIUM & HIGH): Colored memorization-sheet ink (赤シート対応文字) & global contrast stretch
                if (sensitivity.runColorInkPass) {
                    val enhancedBitmap = createInkEnhancedBitmap(normalizedBitmap)
                    try {
                        val pass2Text = runRecognizerPass(
                            activeRecognizer = activeRecognizer,
                            inputImage = InputImage.fromBitmap(enhancedBitmap, 0),
                            isClassical = isClassical,
                            sensitivity = sensitivity
                        )
                        if (pass2Text.isNotBlank()) passTexts.add(pass2Text)
                    } finally {
                        if (enhancedBitmap !== normalizedBitmap) {
                            enhancedBitmap.recycle()
                        }
                    }
                }

                // Pass 3 (HIGH only): Local adaptive shadow & faint-stroke recovery for washed-out print & book-spine shadows
                if (sensitivity.runFaintShadowPass) {
                    val faintShadowBitmap = createFaintShadowAdaptiveBitmap(normalizedBitmap)
                    try {
                        val pass3Text = runRecognizerPass(
                            activeRecognizer = activeRecognizer,
                            inputImage = InputImage.fromBitmap(faintShadowBitmap, 0),
                            isClassical = isClassical,
                            sensitivity = sensitivity
                        )
                        if (pass3Text.isNotBlank()) passTexts.add(pass3Text)
                    } finally {
                        if (faintShadowBitmap !== normalizedBitmap) {
                            faintShadowBitmap.recycle()
                        }
                    }
                }

                normalizedBitmap.recycle()
                Result.success(passTexts.joinToString("\n"))
            } else {
                val inputImage = InputImage.fromFilePath(context, imageUri)
                val text = runRecognizerPass(activeRecognizer, inputImage, isClassical, sensitivity)
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun runRecognizerPass(
        activeRecognizer: TextRecognizer,
        inputImage: InputImage,
        isClassical: Boolean,
        sensitivity: OcrSensitivity
    ): String = suspendCancellableCoroutine { continuation ->
        activeRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                if (continuation.isActive) {
                    val cleaned = sanitizeVisionText(
                        visionText = visionText,
                        isClassical = isClassical,
                        sensitivity = sensitivity
                    )
                    continuation.resume(cleaned)
                }
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resumeWith(Result.failure(exception))
                }
            }
    }

    /**
     * Loads a properly EXIF-rotated Bitmap from [imageUri]:
     * - Downsamples ultra-large >2600px camera images cleanly to avoid OOM while preserving sharp character detail.
     * - Upscales small cropped screenshots (< 720px max side or < 360px min side) by 2x so ML Kit reliably reads cropped single words.
     */
    internal fun loadNormalizedBitmap(context: Context, imageUri: Uri): Bitmap? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(imageUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val rawW = boundsOpts.outWidth
            val rawH = boundsOpts.outHeight
            if (rawW <= 0 || rawH <= 0) return null

            var sampleSize = 1
            val maxTargetDim = 2560
            while ((rawW / sampleSize) > maxTargetDim * 1.4 && (rawH / sampleSize) > maxTargetDim * 1.4) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            var decoded = context.contentResolver.openInputStream(imageUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            val rotationMatrix = Matrix()
            context.contentResolver.openInputStream(imageUri)?.use { exifStream ->
                val exif = ExifInterface(exifStream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotationMatrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotationMatrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotationMatrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> rotationMatrix.preScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> rotationMatrix.preScale(1f, -1f)
                }
            }
            if (!rotationMatrix.isIdentity) {
                val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, rotationMatrix, true)
                if (rotated !== decoded) decoded.recycle()
                decoded = rotated
            }

            // Upscale small cropped images so character height >= 24px for ML Kit
            val minSide = min(decoded.width, decoded.height)
            val maxSide = max(decoded.width, decoded.height)
            if ((minSide < 380 || maxSide < 720) && maxSide <= 1200) {
                val scale = if (minSide < 180) 2.5f else 2.0f
                val targetW = (decoded.width * scale).toInt().coerceAtLeast(1)
                val targetH = (decoded.height * scale).toInt().coerceAtLeast(1)
                val upscaled = Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
                if (upscaled !== decoded) decoded.recycle()
                decoded = upscaled
            }

            decoded
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fast (< 3ms) subsampled check for red/orange/pink memorization ink (赤シート対応の赤・オレンジ文字) on wordbook pages.
     */
    private fun hasColoredMemorizationInk(bitmap: Bitmap): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 16 || h < 16) return false
        val stepX = max(1, w / 96)
        val stepY = max(1, h / 96)
        var totalSamples = 0
        var redOrangeSamples = 0

        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val px = bitmap.getPixel(x, y)
                val r = Color.red(px)
                val g = Color.green(px)
                val b = Color.blue(px)
                totalSamples++
                // Red / orange / vermilion / pink ink on light paper: high R, significantly lower G & B
                if (r >= 135 && (r - g) >= 42 && (r - b) >= 38) {
                    redOrangeSamples++
                }
                x += stepX
            }
            y += stepY
        }
        return totalSamples > 0 && redOrangeSamples * 500 >= totalSamples // >= 0.2% red/orange pixels
    }

    /**
     * Converts colored (especially red/orange赤シート文字) and low-contrast text into crisp dark grayscale
     * using `min(R, G, B)` channel projection + mild contrast stretch.
     */
    private fun createInkEnhancedBitmap(source: Bitmap): Bitmap {
        val maxDim = max(source.width, source.height)
        val working = if (maxDim > 1800) {
            val ratio = 1800f / maxDim.toFloat()
            Bitmap.createScaledBitmap(
                source,
                (source.width * ratio).toInt().coerceAtLeast(1),
                (source.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }

        val w = working.width
        val h = working.height
        val pixels = IntArray(w * h)
        working.getPixels(pixels, 0, w, 0, 0, w, h)

        val histogram = IntArray(256)
        for (i in pixels.indices) {
            val px = pixels[i]
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            // If pixel is yellow-ish highlighter (both R and G high, only B low), use average of R & G so highlighter stays white!
            val v = if (r > 175 && g > 165 && (r - g) < 35 && b < 130) {
                (r + g) shr 1
            } else {
                min(r, min(g, b))
            }
            pixels[i] = v
            histogram[v]++
        }

        val total = pixels.size
        var lowCut = 20
        var highCut = 235
        var acc = 0
        for (i in 0..255) {
            acc += histogram[i]
            if (acc >= total / 50) { // 2nd percentile
                lowCut = i
                break
            }
        }
        acc = 0
        for (i in 255 downTo 0) {
            acc += histogram[i]
            if (acc >= total / 10) { // 90th percentile (paper background)
                highCut = i
                break
            }
        }
        val span = max(40, highCut - lowCut)

        for (i in pixels.indices) {
            val v = pixels[i]
            val stretched = (((v - lowCut) * 255) / span).coerceIn(0, 255)
            pixels[i] = (0xFF shl 24) or (stretched shl 16) or (stretched shl 8) or stretched
        }

        working.setPixels(pixels, 0, w, 0, 0, w, h)
        return working
    }

    /**
     * Local adaptive integral-image normalization used in HIGH sensitivity (`runFaintShadowPass = true`):
     * Recovers faint pencil/light-colored strokes and characters inside dark book-gutter/camera shadows
     * while keeping smooth paper regions clean white.
     */
    private fun createFaintShadowAdaptiveBitmap(source: Bitmap): Bitmap {
        val maxDim = max(source.width, source.height)
        val working = if (maxDim > 1600) {
            val ratio = 1600f / maxDim.toFloat()
            Bitmap.createScaledBitmap(
                source,
                (source.width * ratio).toInt().coerceAtLeast(1),
                (source.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }

        val w = working.width
        val h = working.height
        val pixels = IntArray(w * h)
        working.getPixels(pixels, 0, w, 0, 0, w, h)

        val lum = IntArray(w * h)
        val stride = w + 1
        val integral = IntArray(stride * (h + 1))

        for (y in 0 until h) {
            var rowSum = 0
            val rowOffset = y * w
            val prevIntegralOffset = y * stride
            val currIntegralOffset = (y + 1) * stride
            for (x in 0 until w) {
                val px = pixels[rowOffset + x]
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                val v = if (r > 175 && g > 165 && (r - g) < 35 && b < 130) {
                    (r + g) shr 1
                } else {
                    min(r, min(g, b))
                }
                lum[rowOffset + x] = v
                rowSum += v
                integral[currIntegralOffset + x + 1] = integral[prevIntegralOffset + x + 1] + rowSum
            }
        }

        val radius = max(12, min(w, h) / 28)
        for (y in 0 until h) {
            val y0 = max(0, y - radius)
            val y1 = min(h, y + radius + 1)
            val rowOffset = y * w
            val iTop = y0 * stride
            val iBottom = y1 * stride
            for (x in 0 until w) {
                val x0 = max(0, x - radius)
                val x1 = min(w, x + radius + 1)
                val area = (y1 - y0) * (x1 - x0)
                val sum = integral[iBottom + x1] - integral[iTop + x1] - integral[iBottom + x0] + integral[iTop + x0]
                val localMean = sum / area
                val v = lum[rowOffset + x]
                val diff = max(0, localMean - v - 5)
                val enhanced = if (diff == 0) {
                    255
                } else {
                    (255 - (diff * 980) / max(localMean, 92)).coerceIn(0, 255)
                }
                pixels[rowOffset + x] = (0xFF shl 24) or (enhanced shl 16) or (enhanced shl 8) or enhanced
            }
        }

        working.setPixels(pixels, 0, w, 0, 0, w, h)
        return working
    }

    /**
     * 全角英数字を半角へ正規化する。
     */
    private fun normalizeFullWidthAscii(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                in 'Ａ'..'Ｚ' -> sb.append((ch.code - 'Ａ'.code + 'A'.code).toChar())
                in 'ａ'..'ｚ' -> sb.append((ch.code - 'ａ'.code + 'a'.code).toChar())
                in '０'..'９' -> sb.append((ch.code - '０'.code + '0'.code).toChar())
                '’', '‘', 'ʼ' -> sb.append('\'')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * 単語の周囲にある発音記号 `[...]` `/.../`、日本語品詞記号 `[名]` `(動)`、英語品詞略語 `(vt.)` `[U]`、URL等を除去する。
     * ※ `(achieve)` や `[vocabulary]` のような括弧内の英単語自体は絶対に削除しない。
     * ※ `prob1em` や `imp0rtant` のような単語内部の数字誤認もスペースで分断せず `EnglishWordValidator` で復元する。
     */
    internal fun stripSurroundingAnnotationsForEnglish(rawText: String): String {
        return normalizeFullWidthAscii(rawText)
            // Join line-break hyphenated words (e.g. "environ-\nment" -> "environment") before tokenizing
            .replace(Regex("(?<=[a-zA-Z]{2})[-‐‑]\\s*\\r?\\n\\s*(?=[a-z]{2})"), "")
            // Replace URLs and email addresses so domain fragments don't turn into bogus words
            .replace(Regex("https?://\\S+|www\\.\\S+|\\S+@\\S+"), " ")
            // Remove phonetic transcriptions in /.../ or [...] containing IPA symbols or stress marks
            .replace(Regex("/[^/\\n]{1,32}[əɪʊɔæɑɜɛʃʒθðŋˈˌː][^/\\n]{0,32}/"), " ")
            .replace(Regex("\\[[^\\]\\n]{1,32}[əɪʊɔæɑɜɛʃʒθðŋˈˌː][^\\]\\n]{0,32}\\]"), " ")
            // Remove bracketed Japanese POS/grammar tags like [名], [動], (形), 【他動】 (must contain at least 1 Japanese char)
            .replace(Regex("[\\[［【\\(（][ぁ-んァ-ヶ一-龯・/\\s]{1,8}[\\]］】\\)）]"), " ")
            // Remove explicit short English POS / countability markers in brackets like (n.), (vt.), (adj.), [U], [C], [pl.]
            .replace(
                Regex(
                    "[\\[\\(]\\s*(?:n|v|vt|vi|adj|adv|prep|conj|pron|aux|pl|sing|U|C|sb|sth)\\.?\\s*[\\]\\)]",
                    RegexOption.IGNORE_CASE
                ),
                " "
            )
            // Remove common English contractions so fragments like "doesn" or "didn" aren't treated as standalone tokens
            .replace(
                Regex(
                    "\\b(?:don't|doesn't|didn't|can't|won't|isn't|aren't|wasn't|weren't|haven't|hasn't|hadn't|wouldn't|couldn't|shouldn't|mustn't|it's|that's|there's|what's|who's|let's|i'm|you're|we're|they're|i've|you've|we've|they've|i'll|you'll|we'll|they'll|i'd|you'd|we'd|they'd)\\b",
                    RegexOption.IGNORE_CASE
                ),
                " "
            )
            // Strip possessive "'s" cleanly so "world's" -> "world"
            .replace(Regex("(?<=[a-zA-Z])'s\\b", RegexOption.IGNORE_CASE), "")
    }

    /**
     * Checks whether text inside `【...】` / `［...］` / `[...]` / `〔...〕` in a classical wordbook or exam
     * is a POS/conjugation/meta label (which should be removed) vs. an actual classical headword
     * like `【おどろく】`, `［あはれなり］`, `【心細し】` (which MUST be preserved!).
     */
    private fun isClassicalBracketedMetaTag(innerRaw: String): Boolean {
        val inner = innerRaw.trim().replace(" ", "").replace("　", "")
        if (inner.isEmpty()) return true
        // POS / Conjugation / Dictionary metadata labels
        if (CLASSICAL_BRACKET_META_REGEX.matches(inner)) return true
        if (CLASSICAL_CONJUGATION_TAG_REGEX.containsMatchIn(inner)) return true
        return false
    }

    private val CLASSICAL_BRACKET_META_REGEX = Regex(
        "^(?:[自他]?動.*|[クシ]?形.*|形動.*|名|名詞|代|代名詞|副|副詞|連体|連体詞|接|接続|接続詞|感|感動|感動詞|間投|間投詞|助動|助動詞|[格係副接続終間]?助|助詞|連語|枕|枕詞|掛詞|敬|敬語|尊|尊敬|尊敬語|謙|謙譲|謙譲語|丁|丁寧|丁寧語|文|古|出典|訳|現代語訳|口語訳|意|意味|語源|例|例文|用例|類|類語|対|対義語|参|参考|注|注意|要|重要|頻出|古今|古今異義.*|ポイント|チェック|問.*|設問.*|解答|解説|[一二三四五六七八九十]+)$"
    )

    private val CLASSICAL_CONJUGATION_TAG_REGEX = Regex(
        "(?:四段|上一段|下一段|上二段|下二段|上一|下一|上二|下二|カ変|サ変|ナ変|ラ変|変格|ク活|シク活|ナリ活|タリ活)"
    )

    internal fun stripSurroundingAnnotationsForClassical(
        rawText: String,
        sensitivity: OcrSensitivity = OcrSensitivity.HIGH
    ): String {
        val baseCleaned = rawText
            // Remove alphanumeric / Latin / numbers / page numbers / circled numbers
            .replace(Regex("[0-9０-９a-zA-Zａ-ｚＡ-Ｚ①-⑳⑴-⒇]+"), " ")
            // Remove ONLY POS/grammar/meta tags inside 【...】, ［...］, [...], 〔...〕 while preserving headwords like 【おどろく】!
            .replace(Regex("[【［\\[〔]([^】］\\]〕\\n]{1,22})[】］\\]〕]")) { match ->
                val inner = match.groupValues[1]
                if (isClassicalBracketedMetaTag(inner)) " " else " $inner "
            }
            // Fuse inline historical-kana pronunciation guides inside headwords: あは（わ）れなり -> あはれなり, を（お）かし -> をかし
            .replace(Regex("(?<=[はひふへほゐゑをぢづむ])[（(]\\s*[わいうえおじずんワイウエオジズンー]{1,2}\\s*[）)]"), "")
            // Fuse inline furigana sandwiched between Kanji and okurigana: 驚(おどろ)く -> 驚く
            .replace(Regex("(?<=[一-龯々])[（(][ぁ-んァ-ヶゐゑー・\\s]{1,10}[）)](?=[ぁ-んゐゑ])"), "")
            // Fuse dictionary okurigana/morpheme dots inside headwords: おどろ・く -> おどろく, あはれ・なり -> あはれなり
            .replace(Regex("(?<=[ぁ-んゐゑ一-龯々])[・‐](?=[ぁ-んゐゑ]{1,4}(?=[\\s、。，．【［\\[〔（(「『]|$))"), "")
            // Remove explicit modern glosses starting with ＝ / 訳 / 意 or multi-sense modern glosses with '・'
            .replace(Regex("[（(]\\s*(?:[＝=訳意例対類]|[^）)\\n]*[・。]|[^）)\\n]*(?:ます|です|する|こと\\s*[）)]))[^）)\\n]{0,28}[）)]"), " ")
            // Replace explicit UI / checkbox / bullet / line symbols with space
            .replace(Regex("[□■▫▪○●◎◇◆△▲▽▼☆★✓✔☑☒✗✘※〒→←↑↓⇒⇔―─━│┃┌┐└┘├┤┬┴┼|｜丨_~]+"), " ")

        if (sensitivity == OcrSensitivity.HIGH) {
            return baseCleaned
                // Strip inline small/regular Katakana modern-reading annotations inside hiragana headwords: あはワれなり -> あはれなり
                .replace(Regex("(?<=[はひふへほゐゑをぢづ])[ワイウエオジズンァィゥェォ](?=[ぁ-んゐゑ])"), "")
                // Fuse justified/spaced single-hiragana headwords in wordbooks: "お ど ろ く" -> "おどろく"
                .replace(Regex("(?<![ぁ-んゐゑ一-龯々])([ぁ-んゐゑ](?:[ \\u3000\\t]+[ぁ-んゐゑ]){1,8})(?![ぁ-んゐゑ一-龯々])")) { match ->
                    match.groupValues[1].replace(Regex("[ \\u3000\\t]+"), "")
                }
        }
        return baseCleaned
    }

    /**
     * 古語モード用: 文字数ではなく「どこまでを文字とみなすか」（[sensitivity]）に応じて古語候補を抽出。
     * - 2文字古語も全感度で対象とし、文字数による切り捨てを行わない。
     * - LOW: 枠線やチェックボックス誤認文字（`口`・`ロ`・`レ`等）が癒着していない鮮明な文字のみを古語として認識。
     * - MEDIUM: 標準的な1文字の端記号・チェックボックス混入を除去して古語を認識。
     * - HIGH: 字間が空いた見出し語・複数文字の枠線混入・かすれまで積極的に文字として復元。
     */
    fun extractClassicalWords(
        rawText: String,
        sensitivity: OcrSensitivity = OcrSensitivity.HIGH,
        filterStopWords: Boolean = true
    ): List<String> {
        if (rawText.isBlank()) return emptyList()

        val cleanedText = stripSurroundingAnnotationsForClassical(rawText, sensitivity)

        val found = mutableListOf<Pair<Int, String>>()
        val seen = mutableSetOf<String>()
        val occupied = BooleanArray(cleanedText.length)

        // Step 1: Scan for known Classical Japanese headwords & inflections (longest patterns first)
        for ((pattern, canonical) in classicalPatternIndex) {
            var searchFrom = 0
            while (searchFrom < cleanedText.length) {
                val idx = cleanedText.indexOf(pattern, searchFrom)
                if (idx < 0) break
                val endExclusive = idx + pattern.length

                val overlapsExisting = (idx until endExclusive).any { occupied[it] }
                val validBoundary = isValidClassicalMatchBoundary(cleanedText, idx, endExclusive, pattern, sensitivity)

                if (!overlapsExisting && validBoundary) {
                    for (k in idx until endExclusive) {
                        occupied[k] = true
                    }
                    if (seen.add(canonical)) {
                        found.add(idx to canonical)
                    }
                }
                searchFrom = idx + 1
            }
        }
        found.sortBy { it.first }
        val result = found.map { it.second }.toMutableList()

        // Step 2: Extract open Classical Japanese headwords & passage vocabulary candidates (always >= 2 chars regardless of sensitivity!)
        val minTokenLen = 2

        val segments = cleanedText.split(
            Regex("[\\s、。，．・…―「」『』（）()\\[\\]【】［］｛｝{}<>＜＞/／:：;；!！?？\"'“”‘’]+")
        )

        val tokenRegex = Regex(
            "[一-龯々][きぎしちびみりひゐゑえけせてでねべめれ]{1,2}[一-龯々][ぁ-んゐゑ]{0,3}|" +
                "(?:うち|かき|さし|ひき|おし|とり|もの|あひ)[一-龯々]{1,2}[ぁ-んゐゑ]{1,4}|" +
                "[一-龯々]{1,3}[ぁ-んゐゑ]{1,6}|" +
                "[ぁ-んゐゑ]{2,11}|" +
                "[一-龯々]{2,4}"
        )

        for (seg in segments) {
            if (seg.isBlank()) continue
            val edgeTrimmedSeg = when (sensitivity) {
                OcrSensitivity.LOW -> {
                    // In LOW sensitivity, reject segments that have checkbox/symbol Kanji fused directly onto the word
                    val s = seg.trim()
                    if (isKanjiCheckboxPrefixNoise(s) || isKanjiCheckboxSuffixNoise(s) ||
                        s.first() in "ロレノメトイハニ" || s.last() in "ロレノメトイハニ"
                    ) {
                        ""
                    } else {
                        s
                    }
                }
                OcrSensitivity.MEDIUM -> trimClassicalEdgeNoiseSingle(seg)
                OcrSensitivity.HIGH -> trimClassicalEdgeNoise(seg)
            }
            if (edgeTrimmedSeg.isBlank()) continue

            val subClauses = edgeTrimmedSeg.split(
                Regex(
                    "(?<=[ぁ-んゐゑ一-龯])(?:には|とは|では|をば|から|より|にて|して|ども|けれど|ながら|たまひ|たまふ|たまへ|給ひ|給ふ|給へ|にけり|てけり|たりき)(?=[ぁ-んゐゑ一-龯])|" +
                        "(?<=[一-龯々])[はがをにへともぞやの](?=[一-龯々ぁ-んゐゑ])|" +
                        "(?<=[一-龯々][ぁ-んゐゑ]{1,2}[一-龯々][ぁ-んゐゑ]{1,2})[もはぞやをにがとてばど](?=[ぁ-んゐゑ一-龯々])|" +
                        "(?<=[一-龯々][うくぐすつづぬふぶむゆるしき])[もはぞやをにがとてばど](?=[ぁ-んゐゑ一-龯々])"
                )
            )
            for (clause in subClauses) {
                for (match in tokenRegex.findAll(clause)) {
                    val candidate = cleanOpenClassicalCandidate(match.value, sensitivity)
                    if (!isPlausibleClassicalCandidate(candidate, minTokenLen)) continue

                    if (filterStopWords && CLASSICAL_BASIC_STOP_WORDS.contains(candidate)) continue
                    if (filterStopWords && CLASSICAL_MODERN_EXPLANATION_NOISE.contains(candidate)) continue

                    // Resolve to canonical ClassicalWordKnowledgeBase entry if either direct or de-inflected form matches
                    val kbMatch = ClassicalWordKnowledgeBase.lookup(candidate)
                        ?: deinflectClassicalWord(candidate).firstNotNullOfOrNull { ClassicalWordKnowledgeBase.lookup(it) }

                    val finalWord = kbMatch?.word ?: candidate
                    if (filterStopWords && CLASSICAL_BASIC_STOP_WORDS.contains(finalWord)) continue
                    if (filterStopWords && CLASSICAL_MODERN_EXPLANATION_NOISE.contains(finalWord)) continue

                    if (seen.add(finalWord)) {
                        result.add(finalWord)
                    }
                }
            }
        }

        return result.take(150)
    }

    /**
     * Generates plausible dictionary citation (終止形) candidates from an inflected classical Japanese token
     * (e.g., `驚きて` -> `驚く`, `悲しく` / `悲しき` -> `悲し`, `恥づかしき` -> `恥づかし`, `静かなる` -> `静かなり`).
     */
    internal fun deinflectClassicalWord(token: String): List<String> {
        val out = LinkedHashSet<String>()
        var core = trimClassicalEdgeNoise(token)
        if (core.length < 2) return emptyList()

        // Strip trailing conjunctive/auxiliary endings first
        val auxSuffixes = listOf(
            "けり", "ける", "けれ", "たり", "たる", "たれ", "てき", "てむ", "てけり",
            "にけり", "にき", "たりき", "たまふ", "給ふ", "ばや", "なむ", "こそ",
            "て", "で", "ば", "ど", "ども", "ぬ", "つ", "き", "ず", "む"
        )
        for (sfx in auxSuffixes) {
            if (core.length > sfx.length + 1 && core.endsWith(sfx) && !core.endsWith("あはれなり") && !core.endsWith("つとめて")) {
                val stripped = core.dropLast(sfx.length)
                out.add(stripped)
                addStemToCitationCandidates(stripped, out)
            }
        }

        addStemToCitationCandidates(core, out)
        out.remove(token)
        return out.filter { it.length >= 2 }
    }

    private fun addStemToCitationCandidates(form: String, out: MutableSet<String>) {
        if (form.length < 2) return
        // Adjectival verbs (ナリ活用 / タリ活用)
        if (form.length >= 3 && (form.endsWith("なる") || form.endsWith("なれ") || form.endsWith("なら"))) {
            out.add(form.dropLast(2) + "なり")
        }
        if (form.length >= 3 && form.endsWith("に")) {
            out.add(form.dropLast(1) + "なり")
        }
        val startsWithKanji = form.first() in '\u4E00'..'\u9FFF'
        val minStemLen = if (startsWithKanji) 1 else 2
        // Adjectives (シク活用 & ク活用)
        for (adjSfx in listOf("しく", "しき", "しけれ", "しかり", "しかる")) {
            if (form.length >= adjSfx.length + minStemLen && form.endsWith(adjSfx)) {
                out.add(form.dropLast(adjSfx.length) + "し")
            }
        }
        for (adjSfx in listOf("く", "き", "けれ", "かり", "かる")) {
            if (form.length >= adjSfx.length + minStemLen && form.endsWith(adjSfx)) {
                out.add(form.dropLast(adjSfx.length) + "し")
            }
        }
        // Yodan / Nidan verb stems -> Citation (終止形)
        val stem = form.dropLast(1)
        if (stem.isNotEmpty()) {
            when (form.last()) {
                'か', 'き', 'け' -> out.add("${stem}く")
                'が', 'ぎ', 'げ' -> out.add("${stem}ぐ")
                'さ', 'し', 'せ' -> out.add("${stem}す")
                'た', 'ち', 'て' -> {
                    out.add("${stem}つ")
                }
                'で' -> out.add("${stem}づ")
                'な', 'に', 'ね' -> out.add("${stem}ぬ")
                'ば', 'び', 'べ' -> out.add("${stem}ぶ")
                'ま', 'み', 'め' -> out.add("${stem}む")
                'ら', 'り', 'れ' -> out.add("${stem}る")
                'は', 'ひ', 'へ' -> out.add("${stem}ふ")
                'え' -> out.add("${stem}ゆ")
            }
        }
        for ((end, cit) in listOf("つる" to "つ", "づる" to "づ", "ぬる" to "ぬ", "ぶる" to "ぶ", "むる" to "む", "るる" to "る", "ふる" to "ふ", "ゆる" to "ゆ")) {
            if (form.length > end.length + 1 && form.endsWith(end)) {
                out.add(form.dropLast(end.length) + cit)
            }
        }
    }

    internal fun isCleanStandaloneClassicalWord(candidate: String): Boolean {
        if (!isPlausibleClassicalCandidate(candidate, minTokenLen = 2)) return false
        if (candidate in CLASSICAL_BASIC_STOP_WORDS) return false
        // Reject tokens with attached checkbox Kanji noise
        if (isKanjiCheckboxPrefixNoise(candidate) || isKanjiCheckboxSuffixNoise(candidate)) return false
        // Pure 2-char hiragana not in KB and in non-word particle list should not stay standalone
        if (candidate.length == 2 && candidate.all { it in 'ぁ'..'ん' } && candidate in TWO_CHAR_NON_WORD_HIRAGANA) {
            return false
        }
        return true
    }

    private fun isValidClassicalMatchBoundary(
        text: String,
        start: Int,
        endExclusive: Int,
        pattern: String,
        sensitivity: OcrSensitivity
    ): Boolean {
        val prev = if (start > 0) text[start - 1] else ' '
        val next = if (endExclusive < text.length) text[endExclusive] else ' '
        if (sensitivity == OcrSensitivity.LOW) {
            if (prev in "口ロ日回品レノメトイハニ" || next in "口ロ日回品レノメトイハニ") {
                return false
            }
        } else if (sensitivity == OcrSensitivity.MEDIUM) {
            val prev2 = if (start > 1) text[start - 2] else ' '
            if (prev in "口ロ日回品レノメトイハニ" && prev2 in "口ロ日回品レノメトイハニ") {
                return false
            }
        }
        if (pattern.length == 2 && pattern.all { it in 'ぁ'..'ん' || it in "ゐゑ" }) {
            val prevOk = prev !in 'ぁ'..'ん' || prev in "をにはがもとぞやの"
            val nextOk = next !in '一'..'\u9FFF' && (next !in 'ぁ'..'ん' || next in "てたりけりぬべしずむらむめりかなやよぞもはにをがと")
            if (!prevOk || !nextOk) return false
        }
        return true
    }

    private fun trimClassicalEdgeNoiseSingle(raw: String): String {
        var s = raw.trim()
        if (s.isNotEmpty() && (s.first() in CLASSICAL_EDGE_NOISE_CHARS || isKanjiCheckboxPrefixNoise(s))) {
            if (s.first() !in "口日目田回品" || isKanjiCheckboxPrefixNoise(s)) {
                s = s.substring(1).trimStart()
            }
        }
        if (s.isNotEmpty() && (s.last() in CLASSICAL_EDGE_NOISE_CHARS || isKanjiCheckboxSuffixNoise(s))) {
            if (s.last() !in "口日目田回品" || isKanjiCheckboxSuffixNoise(s)) {
                s = s.dropLast(1).trimEnd()
            }
        }
        // If still starting/ending with checkbox noise (e.g. 2+ checkbox chars in MEDIUM), reject as too noisy
        if (s.isNotEmpty() && (isKanjiCheckboxPrefixNoise(s) || isKanjiCheckboxSuffixNoise(s) || s.first() in "ロレノメトイハニ")) {
            return ""
        }
        return s
    }

    internal fun trimClassicalEdgeNoise(raw: String): String {
        var s = raw.trim()
        while (s.isNotEmpty() && (s.first() in CLASSICAL_EDGE_NOISE_CHARS || isKanjiCheckboxPrefixNoise(s))) {
            if (s.first() in "口日目田回品" && !isKanjiCheckboxPrefixNoise(s)) break
            s = s.substring(1).trimStart()
        }
        while (s.isNotEmpty() && (s.last() in CLASSICAL_EDGE_NOISE_CHARS || isKanjiCheckboxSuffixNoise(s))) {
            if (s.last() in "口日目田回品" && !isKanjiCheckboxSuffixNoise(s)) break
            s = s.dropLast(1).trimEnd()
        }
        return s
    }

    private fun isKanjiCheckboxPrefixNoise(s: String): Boolean {
        if (s.length < 3) return false
        val first = s[0]
        if (first !in "口ロ日回") return false
        if (s.startsWith("口惜し") || s.startsWith("口ずさ") || s.startsWith("口まね")) return false
        val restAfterBoxes = s.trimStart { it in "口ロ日回" }
        return restAfterBoxes.length >= 2 && restAfterBoxes.all { it in 'ぁ'..'ん' || it in "ゐゑ" }
    }

    private fun isKanjiCheckboxSuffixNoise(s: String): Boolean {
        if (s.length < 3) return false
        val last = s.last()
        if (last !in "口ロ日回") return false
        val bodyBeforeBoxes = s.trimEnd { it in "口ロ日回" }
        return bodyBeforeBoxes.length >= 2 && bodyBeforeBoxes.all { it in 'ぁ'..'ん' || it in "ゐゑ" }
    }

    private fun cleanOpenClassicalCandidate(raw: String, sensitivity: OcrSensitivity): String {
        var token = when (sensitivity) {
            OcrSensitivity.LOW -> raw.trim()
            OcrSensitivity.MEDIUM -> trimClassicalEdgeNoiseSingle(raw)
            OcrSensitivity.HIGH -> trimClassicalEdgeNoise(raw)
        }
        if (token.isEmpty()) return ""

        if (sensitivity != OcrSensitivity.LOW) {
            // Strip leading single-hiragana case particles when attached to Kanji (e.g., "を驚く" -> "驚く", "は山際" -> "山際")
            if (token.length >= 3 && token[0] in "はがをにへとやもでの" && token[1] in '\u4E00'..'\u9FFF') {
                token = token.substring(1)
            }

            // If stripping 1 leading particle from a hiragana token reveals a known ClassicalWordKnowledgeBase entry
            if (token.length >= 4 && token[0] in "はがをにへともぞや" && ClassicalWordKnowledgeBase.lookup(token) == null) {
                val withoutLeadingParticle = token.substring(1)
                val kb = ClassicalWordKnowledgeBase.lookup(withoutLeadingParticle)
                if (kb != null) {
                    return kb.word
                }
            }
        }

        for (suffix in listOf("けり", "ける", "けれ", "たり", "たる", "たれ", "つつ", "てき", "てむ", "には", "をば", "こそ", "なむ", "ばや", "てよ")) {
            if (token.length > suffix.length + 2 &&
                token.endsWith(suffix) &&
                ClassicalWordKnowledgeBase.lookup(token) == null &&
                !token.endsWith("あはれなり") &&
                !token.endsWith("つれづれなり")
            ) {
                token = token.dropLast(suffix.length)
                break
            }
        }
        if (token.length >= 4 && token.last() in "はがをにへともぞやで" && ClassicalWordKnowledgeBase.lookup(token) == null) {
            token = token.dropLast(1)
        }
        return token
    }

    private fun isPlausibleClassicalCandidate(candidate: String, minTokenLen: Int): Boolean {
        if (candidate.length < minTokenLen || candidate.length > 10) return false
        if (Regex("(.)\\1\\1").containsMatchIn(candidate)) return false
        if (candidate.length == 2 && candidate[0] == candidate[1] && candidate != "人々" && candidate != "代々" && candidate != "時々" && candidate != "折々") {
            return false
        }
        if (candidate.first() in "んっゃゅょぁぃぅぇぉゎー") return false
        if (candidate.length == 2 && candidate.all { it in 'ぁ'..'ん' }) {
            if (ClassicalWordKnowledgeBase.lookup(candidate) == null && candidate in TWO_CHAR_NON_WORD_HIRAGANA) {
                return false
            }
        }
        if (candidate.all { it in "口日目田回品一二三四五六七八九十百千万頁行問番" } && ClassicalWordKnowledgeBase.lookup(candidate) == null) {
            return false
        }
        return true
    }

    private val TWO_CHAR_NON_WORD_HIRAGANA = setOf(
        "にて", "して", "たる", "なる", "なれ", "ける", "けれ",
        "べき", "べく", "には", "とは", "では", "をば", "かも", "かな", "こそ", "なむ",
        "のみ", "など", "より", "から", "まで", "ほど", "ゆゑ", "ため",
        "こと", "もの", "とき", "ひと", "これ", "それ", "あれ", "どれ", "この", "その",
        "あの", "どの", "ここ", "そこ", "どこ", "おり", "いる",
        "ある", "なる", "する", "ずて", "いは", "いひ"
    )

    /**
     * 文字数ではなく「どこまでを文字とみなすか」（[sensitivity]）に応じた英単語抽出。
     * - 3文字の短い実在語（`cat`, `run`, `aid`, `era` 等）や基礎単語も全感度で一切切り捨てない。
     * - LOW: 数字誤認やチェックボックス接触のない純粋・鮮明な英字列のみを文字とみなして辞書照合。
     * - MEDIUM: 1文字程度の数字誤認（`0`/`1`）や標準的な枠線接触（`liocvxj`）を補正して辞書照合。
     * - HIGH: かすれ・複数数字誤認（`0`/`1`/`5`/`8`）やチェックボックス接触（最大3文字）まで積極的に文字復元して辞書照合。
     */
    /**
     * テキスト中から2〜6語の連続する熟語・成句・句動詞・コロケーションを
     * [InternalDictionaryService] と照合して自動検出する。
     */
    fun extractEnglishIdioms(
        rawText: String,
        context: Context? = null
    ): List<String> {
        if (rawText.isBlank()) return emptyList()
        if (context != null) {
            InternalDictionaryService.ensureLoaded(context)
        }
        val cleanedText = stripSurroundingAnnotationsForEnglish(rawText)

        // 句読点・改行・記号等で文・節単位に分割
        val segments = cleanedText.split(Regex("[\r\n\t.,，;:!?\"'()\\[\\]{}]+"))
        val detected = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        for (segment in segments) {
            val rawTokens = segment.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (rawTokens.size < 2) continue

            val cleanTokens = rawTokens.map { token ->
                token.filter { it in 'a'..'z' || it in 'A'..'Z' || it == '-' }.lowercase(Locale.ROOT)
            }.filter { it.isNotEmpty() }

            if (cleanTokens.size < 2) continue

            var i = 0
            while (i < cleanTokens.size) {
                var matchedLen = 0
                var matchedIdiom: String? = null

                val maxWindow = min(6, cleanTokens.size - i)
                for (w in maxWindow downTo 2) {
                    val candidate = cleanTokens.subList(i, i + w).joinToString(" ")
                    val lookup = InternalDictionaryService.lookupEnglish(candidate)
                    if (lookup != null && (candidate.contains(" ") || lookup.pos in setOf("句動詞", "熟語・連語", "連語", "前置詞句", "成句", "重要構文", "ビジネス連語"))) {
                        matchedLen = w
                        matchedIdiom = lookup.canonicalWord
                        break
                    }
                }

                if (matchedIdiom != null && matchedLen >= 2) {
                    val key = matchedIdiom.lowercase(Locale.ROOT)
                    if (seen.add(key)) {
                        detected.add(matchedIdiom)
                    }
                    i += matchedLen
                } else {
                    i++
                }
            }
        }
        return detected
    }

    /**
     * 文字数ではなく「どこまでを文字とみなすか」（[sensitivity]）に応じた英単語および熟語・連語の抽出。
     * - テキスト中に存在する2〜6語の熟語・句動詞・コロケーションを自動検出し先頭に配置。
     * - 3文字の短い実在語（`cat`, `run`, `aid`, `era` 等）や基礎単語も全感度で一切切り捨てない。
     * - LOW: 数字誤認やチェックボックス接触のない純粋・鮮明な英字列のみを文字とみなして辞書照合。
     * - MEDIUM: 1文字程度の数字誤認（`0`/`1`）や標準的な枠線接触（`liocvxj`）を補正して辞書照合。
     * - HIGH: かすれ・複数数字誤認（`0`/`1`/`5`/`8`）やチェックボックス接触（最大3文字）まで積極的に文字復元して辞書照合。
     */
    fun extractMeaningfulWords(
        rawText: String,
        sensitivity: OcrSensitivity = OcrSensitivity.HIGH,
        filterStopWords: Boolean = true,
        context: Context? = null
    ): List<String> {
        if (rawText.isBlank()) return emptyList()
        if (context != null) {
            EnglishWordValidator.ensureInitialized(context)
        }

        val wordList = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        // 1. 熟語・成句・句動詞を先行検出
        val detectedIdioms = extractEnglishIdioms(rawText, context)
        for (idm in detectedIdioms) {
            if (seen.add(idm.lowercase(Locale.ROOT))) {
                wordList.add(idm)
            }
        }

        // 2. 単語単位の抽出
        val cleanedText = stripSurroundingAnnotationsForEnglish(rawText)
        val minWordLen = 3

        val tokenRegex = Regex("[a-zA-Z0-9]{$minWordLen,28}")
        val matches = tokenRegex.findAll(cleanedText)

        for (match in matches) {
            val rawToken = match.value
            if (rawToken.count { it in 'a'..'z' || it in 'A'..'Z' } < 2) continue

            val resolvedWord = EnglishWordValidator.resolveCleanEnglishWord(
                rawToken = rawToken,
                sensitivity = sensitivity,
                context = context
            ) ?: continue

            val word = resolvedWord.lowercase(Locale.ROOT)
            if (word.length < minWordLen) continue
            if (seen.contains(word)) continue

            // 純粋な文法機能語（冠詞・代名詞・be動詞等）と品詞略語のみ共通で除外（文字数や単語難易度での除外は行わない）
            if (filterStopWords && BASIC_STOP_WORDS.contains(word)) continue
            if (EnglishWordValidator.isNoiseAbbreviation(word)) continue

            seen.add(word)
            wordList.add(word)
        }
        return wordList
    }
}
