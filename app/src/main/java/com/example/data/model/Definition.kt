package com.example.data.model

data class Definition(
    val pos: String = "",
    val meaning: String = ""
)

object PartsOfSpeech {
    val ENGLISH = listOf(
        "動詞",
        "名詞",
        "形容詞",
        "副詞",
        "前置詞",
        "接続詞",
        "代名詞",
        "助動詞",
        "疑問詞",
        "間投詞",
        "冠詞",
        "その他"
    )

    val CLASSICAL = listOf(
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

    val ALL = (ENGLISH + CLASSICAL).distinct()
}

/**
 * 古語の品詞をシンプルな基本品詞（動詞・形容詞・形容動詞など）に正規化し、
 * 活用の種類（カ行四段活用・シク活用・ナリ活用など）を備考欄へ分離・統合するヘルパー。
 */
object ClassicalPosNormalizer {

    private val multiVerbConjRegex = Regex(
        "([アカガサザタダナハバマヤラワ]行)?(四段|上一段|下一段|上二段|下二段)[/・](四段|上一段|下一段|上二段|下二段)"
    )

    private val singleVerbConjRegex = Regex(
        "([アカガサザタダナハバマヤラワ])?行?(上一段|上一|下一段|下一|上二段|上二|下二段|下二|五[（(]四[）)]|四段|四)"
    )

    /**
     * 生の品詞文字列（例: "動詞 カ行四段", "形容詞 シク活用", "形容動詞 ナリ活用", "名詞 (気色)"）を
     * Pair(シンプル品詞, 活用の種類) に分割する。
     */
    fun splitPosAndConjugation(rawPos: String): Pair<String, String> {
        val cleaned = rawPos
            .replace("　", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.isBlank()) return "" to ""

        // 助動詞は「動詞」より先に判定する
        if (cleaned.startsWith("助動詞") || cleaned == "助動") {
            return "助動詞" to ""
        }

        // 形容動詞（ナリ活用・タリ活用）
        if (cleaned.startsWith("形容動詞") || cleaned.startsWith("形動")) {
            val conj = when {
                cleaned.contains("タリ") -> "タリ活用"
                cleaned.contains("ナリ") -> "ナリ活用"
                else -> ""
            }
            return "形容動詞" to conj
        }

        // 形容詞（ク活用・シク活用・カリ活用）
        if (cleaned.startsWith("形容詞") || cleaned == "形" || cleaned.startsWith("形ク") || cleaned.startsWith("形シク")) {
            val conj = when {
                cleaned.contains("シク") -> "シク活用"
                cleaned.contains("ク活用") || cleaned.startsWith("形ク") -> "ク活用"
                cleaned.contains("補助活用") || cleaned.contains("カリ") -> "カリ活用"
                else -> ""
            }
            return "形容詞" to conj
        }

        // 動詞（補助動詞・自動詞・他動詞を含む）
        if (cleaned.contains("動詞") || cleaned.startsWith("動") || cleaned.startsWith("自動") || cleaned.startsWith("他動")) {
            val compact = cleaned.replace(" ", "")
            val multiMatch = multiVerbConjRegex.find(compact)
            if (multiMatch != null) {
                val row = multiMatch.groupValues[1]
                val first = multiMatch.groupValues[2]
                val second = multiMatch.groupValues[3]
                return "動詞" to "${row}${first}・${second}活用"
            }

            val conj = when {
                compact.contains("カ変") || compact.contains("カ行変格") -> "カ行変格活用"
                compact.contains("サ変") || compact.contains("サ行変格") -> "サ行変格活用"
                compact.contains("ナ変") || compact.contains("ナ行変格") -> "ナ行変格活用"
                compact.contains("ラ変") || compact.contains("ラ行変格") -> "ラ行変格活用"
                else -> {
                    val m = singleVerbConjRegex.find(compact)
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
            return "動詞" to conj
        }

        // その他の品詞（複合表記 "副詞/接続詞" や "名詞 (気色)" などもシンプル品詞に正規化）
        val primaryToken = cleaned
            .substringBefore("(")
            .substringBefore("（")
            .substringBefore("/")
            .substringBefore("・")
            .trim()

        val simplePos = when {
            primaryToken in PartsOfSpeech.CLASSICAL -> primaryToken
            primaryToken == "代名詞" -> "名詞"
            primaryToken == "間投詞" -> "感動詞"
            primaryToken in PartsOfSpeech.ENGLISH -> primaryToken
            else -> {
                PartsOfSpeech.CLASSICAL.firstOrNull { cleaned.startsWith(it) } ?: "その他"
            }
        }
        return simplePos to ""
    }

    /**
     * 3列CSVなどで第3列が品詞（旧形式の「動詞 四段」「形容動詞 ナリ活用」等を含む）かどうかを判定する。
     */
    fun isRecognizedPosOrConjugatedPos(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return false
        if (trimmed in PartsOfSpeech.ALL) return true
        val (simplePos, conj) = splitPosAndConjugation(trimmed)
        if (simplePos in PartsOfSpeech.ALL && conj.isNotBlank()) return true
        return false
    }

    /**
     * 抽出した活用の種類リストを既存の備考（note）の先頭に重複なく統合する。
     */
    fun mergeConjugationIntoNote(conjugations: List<String>, existingNote: String): String {
        val distinctConjs = conjugations.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (distinctConjs.isEmpty()) return existingNote.trim()

        val trimmedNote = existingNote.trim()
        val missingConjs = distinctConjs.filter { conj ->
            !trimmedNote.contains(conj)
        }
        if (missingConjs.isEmpty()) return trimmedNote

        val conjText = missingConjs.joinToString("・")
        return if (trimmedNote.isBlank()) {
            conjText
        } else {
            "$conjText / $trimmedNote"
        }
    }
}
