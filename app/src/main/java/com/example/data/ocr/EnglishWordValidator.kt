package com.example.data.ocr

import android.content.Context
import com.example.data.model.WordKnowledgeBase
import java.io.File
import java.util.Locale

/**
 * Offline English word lexicon & morphological validator used by OCR extraction.
 * - Loads ~61,000 standard English base words from `assets/en_lexicon.txt` (both on Android device and JVM unit tests).
 * - Strictly restricts output to real dictionary words (zero synthetic non-dictionary fallback), even in `HIGH` sensitivity.
 * - Resolves regular English inflections, safe 4+ letter compound prefixes, suffixes, British/American spelling variants,
 *   and OCR digit/letter confusions (`0`->`o`, `1`->`l`/`i`).
 * - Rescues valid English words when surrounding visual noise (checkboxes `□`/`☑`, vertical rules, bullets, list numbers, brackets)
 *   gets OCR-misread as 1–3 leading or 1–2 trailing characters attached to a real word (e.g., `dimportant` -> `important`,
 *   `ooachieve` -> `achieve`, `clvocabulary` -> `vocabulary`, `OImportant` -> `important`, `lapple` -> `apple`, `importantI` -> `important`).
 */
object EnglishWordValidator {

    @Volatile
    private var loadedWords: Set<String>? = null

    private val COMMON_IRREGULAR_AND_CORE_WORDS = setOf(
        "better", "best", "worse", "worst", "further", "furthest", "farther", "farthest",
        "elder", "eldest", "less", "least", "more", "most",
        "went", "gone", "saw", "seen", "came", "taken", "took", "gave", "given",
        "found", "thought", "told", "became", "left", "felt", "brought", "began", "begun",
        "kept", "held", "wrote", "written", "stood", "heard", "meant", "met", "paid",
        "sat", "spoke", "spoken", "lain", "led", "read", "grew", "grown", "lost", "fell",
        "fallen", "sent", "built", "understood", "drew", "drawn", "broke", "broken",
        "spent", "cut", "rose", "risen", "drove", "driven", "bought", "wore", "worn",
        "chose", "chosen", "sought", "threw", "thrown", "caught", "dealt", "aught",
        "fought", "taught", "ate", "eaten", "sang", "sung", "swam", "swum", "rang", "rung",
        "sank", "sunk", "drank", "drunk", "flew", "flown", "rode", "ridden", "shook",
        "shaken", "stole", "stolen", "froze", "frozen", "woke", "woken", "hid", "hidden",
        "bit", "bitten", "blew", "blown", "forgave", "forgiven", "forgot", "forgotten",
        "children", "men", "women", "teeth", "feet", "mice", "geese", "oxen", "analyses",
        "criteria", "phenomena", "indices", "appendices", "matrices", "hypotheses", "theses",
        "crises", "diagnoses", "oases", "syntheses", "emphases", "curricula", "data", "media",
        // Common British & modern textbook/exam terms that also exist in Weblio
        "colour", "colours", "coloured", "behaviour", "behaviours", "favour", "favours", "favourite",
        "honour", "honours", "honourable", "labour", "labours", "neighbour", "neighbours", "neighbourhood",
        "rumour", "rumours", "humour", "centre", "centres", "theatre", "theatres", "metre", "metres",
        "kilometre", "kilometres", "litre", "litres", "fibre", "fibres", "defence", "offence", "licence",
        "practise", "practised", "practising", "realise", "realised", "realising", "organise", "organised",
        "organisation", "recognise", "recognised", "analyse", "analysed", "travelling", "travelled",
        "traveller", "cancelled", "cancelling", "labelled", "labelling", "modelled", "modelling",
        "pandemic", "cybersecurity", "telework", "microplastic", "microplastics", "deforestation",
        "sustainability", "sustainable", "biodiversity", "globalization", "globalisation", "smartphone",
        "anthropocene", "ecosystem", "ecosystems", "renewable", "renewables", "decarbonization",
        "desalination", "telemedicine", "biofuel", "biofuels", "upcycling", "crowdfunding", "overtourism"
    )

    /**
     * Curated genuine 3-letter English vocabulary words so obscure 3-letter Scrabble tokens
     * misread from checkboxes (`aba`, `aal`, `oca`, `ova`, `ava`, `ivi`, `loo`, `ilo`, `ulu`, `xis`)
     * are strictly rejected even in HIGH sensitivity.
     */
    private val CORE_THREE_LETTER_WORDS = setOf(
        "act", "add", "age", "ago", "aid", "aim", "air", "all", "and", "ant", "any", "ape", "arc", "are",
        "ark", "arm", "art", "ash", "ask", "ate", "awe", "axe", "bad", "bag", "ban", "bar", "bat", "bay",
        "bed", "bee", "beg", "bet", "bid", "big", "bin", "bit", "bow", "box", "boy", "bud", "bug", "bun",
        "bus", "but", "buy", "cab", "can", "cap", "car", "cat", "cob", "cod", "cog", "cop", "cot", "cow",
        "cry", "cub", "cue", "cup", "cut", "dad", "dam", "day", "den", "dew", "did", "die", "dig", "dim",
        "dip", "dog", "dot", "dry", "due", "dug", "dye", "ear", "eat", "ebb", "eel", "egg", "ego", "elm",
        "end", "era", "eve", "eye", "fan", "far", "fat", "fax", "fed", "fee", "few", "fig", "fin", "fir",
        "fit", "fix", "flu", "fly", "foe", "fog", "for", "fox", "fry", "fun", "fur", "gap", "gas", "gem",
        "get", "god", "got", "gum", "gun", "gut", "guy", "gym", "had", "ham", "has", "hat", "hay", "hem",
        "hen", "her", "hid", "him", "hip", "his", "hit", "hog", "hop", "hot", "how", "hub", "hue", "hug",
        "hum", "hut", "ice", "icy", "ill", "ink", "inn", "ion", "its", "ivy", "jam", "jar", "jaw", "jay",
        "jet", "job", "jog", "joy", "jug", "key", "kid", "kin", "kit", "lab", "lad", "lag", "lap", "law",
        "lax", "lay", "led", "leg", "let", "lid", "lie", "lip", "lit", "log", "lot", "low", "mad", "man",
        "map", "mar", "mat", "max", "may", "men", "met", "mid", "mix", "mob", "mom", "mop", "mud", "mug",
        "nap", "net", "new", "nod", "nor", "not", "now", "nun", "nut", "oak", "oar", "oat", "odd", "off",
        "oil", "old", "one", "opt", "orb", "ore", "our", "out", "owe", "owl", "own", "pad", "pal", "pan",
        "par", "pat", "paw", "pay", "pea", "peg", "pen", "pet", "pie", "pig", "pin", "pit", "pod", "pop",
        "pot", "pro", "pub", "pun", "pup", "put", "rag", "ram", "ran", "rap", "rat", "raw", "ray", "red",
        "rib", "rid", "rig", "rim", "rip", "rob", "rod", "rot", "row", "rub", "rug", "rum", "run", "rye",
        "sad", "sag", "sap", "sat", "saw", "say", "sea", "see", "set", "sew", "she", "shy", "sin", "sip",
        "sir", "sit", "six", "ski", "sky", "sly", "sob", "sod", "son", "sow", "soy", "spa", "spy", "sub",
        "sue", "sum", "sun", "tab", "tag", "tan", "tap", "tar", "tax", "tea", "ten", "the", "tie", "tin",
        "tip", "toe", "ton", "too", "top", "tow", "toy", "try", "tub", "tug", "two", "urn", "use", "van",
        "vat", "vet", "vex", "via", "vie", "vow", "wag", "war", "was", "wax", "way", "web", "wed", "wet",
        "who", "why", "wig", "win", "wit", "woe", "wok", "won", "woo", "wow", "yam", "yes", "yet", "you",
        "zap", "zen", "zip", "zoo"
    )

    /**
     * Dictionary/textbook grammar & POS tags or Roman numerals that frequently appear right next to
     * words in printed wordbooks and should never be extracted as English vocabulary.
     */
    private val NON_VOCABULARY_NOISE_TOKENS = setOf(
        "vt", "vi", "adj", "adv", "prep", "conj", "pron", "interj", "aux", "abbr", "phr",
        "pl", "sing", "sb", "sth", "esp", "etc", "fig", "vol", "chap", "sec", "cf", "eg", "ie",
        "ii", "iii", "iv", "vi", "vii", "viii", "ix", "xi", "xii", "xiii", "xiv", "xv",
        "xvi", "xvii", "xviii", "xix", "xx", "xxi", "www", "http", "https", "html", "com", "jp"
    )

    /**
     * Characters commonly produced when checkboxes (`□`, `□□`, `☑`, `☒`), bullets (`•`, `○`),
     * brackets (`[`, `]`, `(`), or vertical borders (`|`) are misread by Latin OCR.
     * Note: 1-char leading strip tries ANY letter when the raw token is not a valid word;
     * 2–3 char leading strip restricts to these checkbox/border noise characters.
     */
    private const val STANDARD_BOX_NOISE_CHARS = "liocvxj"
    private const val EXTENDED_CHECKBOX_NOISE_CHARS = "liocvxjdbpuqst"
    private const val TRAILING_NOISE_CHARS = "liocvxjdbpuq"
    private const val MIN_ENGLISH_WORD_CHARS = 3

    /**
     * Obscure 3-letter Scrabble tokens commonly produced when checkboxes (`□`, `□□`, `☑`) or vertical rules
     * are misread by OCR (`aba`, `aal`, `oca`, `ova`, `ava`, `ivi`, `loo`, `ilo`, `ulu`, `xis`, etc.).
     * Real 3-letter words in `en_lexicon.txt` are accepted EXCEPT these checkbox-noise tokens.
     */
    private val THREE_LETTER_CHECKBOX_SCRABBLE_NOISE = setOf(
        "aba", "aal", "aas", "oca", "ova", "ava", "ivi", "loo", "ilo", "ulu", "xis",
        "cwm", "vug", "zax", "obi", "udo", "uta", "upo", "oxo", "ooh", "abo", "ahu",
        "ain", "ais", "ait", "ala", "alb", "als", "ama", "ami", "amu", "ana", "ani",
        "apo", "arb", "arf", "ars", "avo", "awa", "awn", "azo", "baa", "bal", "bap",
        "bas", "bis", "boa", "bod", "bop", "bos", "cee", "cel", "cep", "cis", "col",
        "cor", "cos", "coz", "cru", "cwm", "agp", "dak", "dal", "dap", "daw", "deb",
        "dee", "del", "dev", "dey", "dib", "dif", "din", "dit", "dol", "dom", "dor",
        "dos", "dow", "dup", "eau", "edh", "efs", "ehs", "eme", "ems", "eng", "ens",
        "ern", "ers", "ess", "eta", "eth", "fae", "fas", "feh", "fem", "fen", "fer",
        "fet", "feu", "fez", "fid", "fil", "fob", "fon", "fop", "fou", "foy", "fub",
        "fud", "fug", "gad", "gae", "gal", "gam", "gan", "gar", "gat", "gau", "ged",
        "gee", "ghi", "gib", "gid", "gie", "gin", "gip", "git", "goa", "gob", "gor",
        "gos", "gox", "goy", "gul", "guv", "hae", "haj", "hao", "hap", "haw", "heh",
        "hes", "het", "hic", "hie", "hin", "hm", "hoa", "hob", "hod", "hoi", "hoy",
        "hun", "hup", "ich", "iff", "igg", "ilk", "ins", "ism", "iwis", "jib", "jin",
        "joe", "jow", "jun", "jus", "jut", "kab", "kae", "kaf", "kas", "kat", "kea",
        "kef", "keg", "ken", "kep", "kex", "khi", "kif", "kip", "kir", "kis", "koa",
        "kob", "koi", "kop", "kor", "kos", "kue", "kyu", "lac", "lam", "lar", "las",
        "lat", "lav", "lea", "lek", "leu", "lev", "lex", "lez", "lin", "lis", "lob",
        "lum", "lvs", "lye", "mac", "mae", "mam", "mas", "maw", "meg", "mel", "mem",
        "mho", "mib", "mic", "mig", "mil", "mim", "mir", "mis", "moa", "moc", "mod",
        "mog", "mol", "mon", "mor", "mos", "mot", "mow", "mun", "mus", "mut", "myc",
        "nae", "nah", "nam", "nan", "naw", "nay", "neb", "nee", "neh", "nib", "nil",
        "nim", "nip", "nit", "nix", "nob", "nog", "noh", "nom", "noo", "nos", "nth",
        "nub", "nus", "oba", "obe", "ocas", "oda", "odea", "ods", "oes", "ohs", "oka",
        "oke", "ole", "oms", "ono", "ons", "oot", "ope", "ops", "ora", "orc", "ors",
        "ort", "ose", "oud", "ouk", "oup", "owt", "oxes", "oye", "oys", "pac", "pah",
        "pam", "pap", "pas", "pax", "pec", "ped", "peh", "pes", "phi", "pht", "pia",
        "pic", "pis", "piu", "pix", "plu", "poh", "poi", "pol", "pom", "pul", "pur",
        "pus", "pya", "pye", "pyx", "qat", "qis", "qua", "rad", "rah", "rai", "raj",
        "ras", "rax", "reb", "rec", "ree", "ref", "reg", "rei", "rem", "rep", "res",
        "ret", "rev", "rex", "rho", "ria", "rif", "rin", "roc", "rom", "rya", "sab",
        "sac", "sae", "sal", "sau", "seg", "sei", "sel", "sen", "ser", "sha", "shh",
        "sib", "sic", "sim", "ska", "sol", "som", "sop", "sos", "sot", "sou",
        "sox", "sri", "suk", "suq", "syn", "tae", "taj", "tam", "tao", "tas",
        "tat", "tau", "tav", "taw", "tec", "ted", "tee", "teg", "tel", "tes", "tet",
        "tew", "tho", "tig", "til", "tis", "tit", "tod", "tom", "tor", "tui", "tun",
        "tup", "tut", "tux", "twa", "tye", "udo", "ugh", "uke", "umm", "ump", "uns",
        "urb", "urd", "urp", "urs", "uta", "ute", "uts", "vac", "vas", "vau", "vav",
        "vaw", "vee", "vig", "vim", "vis", "voe", "vox", "vug", "wab", "wad", "wae",
        "wan", "wap", "wat", "waw", "wen", "wha", "wis", "wiz", "wog", "wop", "wos",
        "wot", "wud", "wye", "wyn", "xis", "yae", "yah", "yap",
        "yar", "yaw", "yay", "yea", "yeh", "yen", "yep", "yeuk", "yew", "yid", "yin",
        "yip", "yob", "yod", "yok", "yom", "yon", "yow", "yuk", "yum", "yup", "zag",
        "zas", "zax", "zed", "zek", "zep", "zig", "zin", "zit", "zoa", "zol", "zuz"
    )

    /**
     * Productive prefixes that DO NOT collide with checkbox/border OCR characters (`co`, `il`, `im`, `in`, `ir`, `de`, `en`, `em`, `ex`
     * remain strictly excluded so checkbox+word fusions like `coimportant` / `ilimportant` never match synthetically!).
     */
    private val SAFE_COMPOUND_PREFIXES = listOf(
        "over", "under", "inter", "intra", "counter", "super", "hyper", "ultra", "anti", "auto",
        "fore", "micro", "macro", "mini", "multi", "para", "poly", "pseudo", "semi", "tele",
        "trans", "cyber", "socio", "neuro", "thermo", "electro", "agro", "agri",
        "un", "re", "dis", "non", "mis", "pre", "post", "out", "sub", "bio", "eco", "geo", "mid", "neo"
    )

    fun ensureInitialized(context: Context?) {
        if (loadedWords != null) return
        synchronized(this) {
            if (loadedWords != null) return
            val set = HashSet<String>(65536)
            set.addAll(WordKnowledgeBase.ENTRIES.keys)
            set.addAll(COMMON_IRREGULAR_AND_CORE_WORDS)

            var loadedFromStream = false
            if (context != null) {
                try {
                    context.assets.open("en_lexicon.txt").bufferedReader(Charsets.UTF_8).useLines { lines ->
                        lines.forEach { line ->
                            val w = line.trim()
                            if (w.isNotEmpty()) set.add(w)
                        }
                    }
                    loadedFromStream = set.size > 1000
                } catch (_: Exception) {
                }
            }

            if (!loadedFromStream) {
                val candidatePaths = listOf(
                    File("app/src/main/assets/en_lexicon.txt"),
                    File("src/main/assets/en_lexicon.txt"),
                    File("../app/src/main/assets/en_lexicon.txt")
                )
                val assetFile = candidatePaths.firstOrNull { it.exists() && it.isFile }
                if (assetFile != null) {
                    try {
                        assetFile.bufferedReader(Charsets.UTF_8).useLines { lines ->
                            lines.forEach { line ->
                                val w = line.trim()
                                if (w.isNotEmpty()) set.add(w)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            loadedWords = set
        }
    }

    private fun lexicon(): Set<String> {
        if (loadedWords == null) {
            ensureInitialized(null)
        }
        return loadedWords ?: emptySet()
    }

    fun isNoiseAbbreviation(word: String): Boolean {
        return word.lowercase(Locale.ROOT) in NON_VOCABULARY_NOISE_TOKENS
    }

    /**
     * Checks whether [rawWord] is a real English word in the lexicon (or a valid inflection /
     * British-American variant / 4+ letter compound prefix of a real word in the lexicon).
     */
    fun isValidEnglishWord(rawWord: String, context: Context? = null): Boolean {
        if (context != null && loadedWords == null) {
            ensureInitialized(context)
        }
        val word = rawWord.trim().lowercase(Locale.ROOT)
        if (word.length < 3 || word.length > 26) return false
        if (!word.all { it in 'a'..'z' }) return false
        if (word in NON_VOCABULARY_NOISE_TOKENS) return false
        if (!hasPlausibleEnglishOrthography(word)) return false

        val dict = lexicon()

        // Guard 3-letter words against obscure Scrabble tokens & pure checkbox/stroke clusters while keeping ALL real 3-letter vocabulary
        if (word.length == 3) {
            if (word in CORE_THREE_LETTER_WORDS || WordKnowledgeBase.ENTRIES.containsKey(word)) {
                return true
            }
            if (word in THREE_LETTER_CHECKBOX_SCRABBLE_NOISE) {
                return false
            }
            if (word.all { it in "liocvxj" }) {
                return false
            }
            return if (dict.size >= 500) {
                word in dict || isInflectedValidWord(word, dict)
            } else {
                word in COMMON_IRREGULAR_AND_CORE_WORDS
            }
        }

        if (dict.size < 500) {
            return WordKnowledgeBase.ENTRIES.containsKey(word) || word in COMMON_IRREGULAR_AND_CORE_WORDS
        }

        if (word in dict) return true
        return isInflectedValidWord(word, dict)
    }

    /**
     * Validates or rescues a clean dictionary English word from an OCR token according to
     * optical character recognition [sensitivity] ("どこまでを文字とみなすか" — NEVER filtering by word length):
     * - [OcrSensitivity.LOW]: Only pure, uncorrupted alphabetic words directly matching the dictionary are accepted.
     * - [OcrSensitivity.MEDIUM]: Rescues 1-char `0`/`1` confusion and standard 1–2 char box/line edge strokes (`liocvxj`).
     * - [OcrSensitivity.HIGH]: Aggressively rescues degraded strokes (`0`/`1`/`5`/`8` confusions) and 1–3 char checkbox/symbol fusions.
     */
    fun resolveCleanEnglishWord(
        rawToken: String,
        sensitivity: OcrSensitivity = OcrSensitivity.HIGH,
        context: Context? = null
    ): String? {
        if (context != null && loadedWords == null) {
            ensureInitialized(context)
        }
        val trimmed = rawToken.trim()
        if (trimmed.isEmpty()) return null

        if (sensitivity == OcrSensitivity.LOW) {
            // Strict clean-character mode: must already be pure letters without fused checkbox capitalization or digits
            if (!trimmed.all { it in 'a'..'z' || it in 'A'..'Z' }) return null
            if (detectLeadingLowerBeforeTitleCase(trimmed) != null) return null
            if (detectLeadingUpperBeforeTitleCase(trimmed) != null) return null
            if (stripObviousMixedCaseEdgeArtifact(trimmed) != trimmed) return null
            val lower = trimmed.lowercase(Locale.ROOT)
            return if (lower.length >= MIN_ENGLISH_WORD_CHARS && isValidEnglishWord(lower, context)) lower else null
        }

        // 1) Strip pure leading/trailing list numbers or checkbox '0'/'1' ("0124apple" -> "apple", "0important" -> "important")
        val digitTrimmed = trimmed.trim { it.isDigit() }
        if (digitTrimmed.length >= MIN_ENGLISH_WORD_CHARS && digitTrimmed.all { it in 'a'..'z' || it in 'A'..'Z' }) {
            val fromDigitTrimmed = resolvePureAlphaToken(digitTrimmed, sensitivity, context = context)
            if (fromDigitTrimmed != null) return fromDigitTrimmed
        }

        // 2) If token had internal digit-letter confusions (MEDIUM: up to 1 '0'/'1'; HIGH: up to 3 '0'/'1'/'5'/'8')
        if (trimmed.any { it.isDigit() }) {
            for (ocrCandidate in generateDigitToLetterCandidates(trimmed, sensitivity)) {
                if (ocrCandidate.length >= MIN_ENGLISH_WORD_CHARS) {
                    val rescued = resolvePureAlphaToken(ocrCandidate, sensitivity, context = context)
                    if (rescued != null) return rescued
                }
            }
            return null
        }

        return resolvePureAlphaToken(trimmed, sensitivity, context = context)
    }

    private fun resolvePureAlphaToken(
        alphaToken: String,
        sensitivity: OcrSensitivity,
        context: Context?
    ): String? {
        if (alphaToken.isEmpty() || !alphaToken.all { it in 'a'..'z' || it in 'A'..'Z' }) return null

        val rawLower = alphaToken.lowercase(Locale.ROOT)
        val isHigh = sensitivity == OcrSensitivity.HIGH

        // Pre-Step A: Leading lowercase checkbox/border chars before a TitleCase word
        val leadingLowerBeforeUpper = detectLeadingLowerBeforeTitleCase(alphaToken)
        if (leadingLowerBeforeUpper != null) {
            val prefixLen = alphaToken.length - leadingLowerBeforeUpper.length
            val prefixPart = alphaToken.take(prefixLen).lowercase(Locale.ROOT)
            val allowedInMode = isHigh || (prefixLen <= 2 && prefixPart.all { it in STANDARD_BOX_NOISE_CHARS })
            if (allowedInMode) {
                val subLower = leadingLowerBeforeUpper.lowercase(Locale.ROOT)
                if (subLower.length >= MIN_ENGLISH_WORD_CHARS && isValidEnglishWord(subLower, context)) {
                    return subLower
                }
            }
        }

        // Pre-Step B: Leading UpperCase checkbox/symbol letters before TitleCase word ("OImportant", "OOAchieve", "DApple")
        val multiUpperStripped = detectLeadingUpperBeforeTitleCase(alphaToken)
        if (multiUpperStripped != null) {
            val strippedPrefix = alphaToken.take(alphaToken.length - multiUpperStripped.length).lowercase(Locale.ROOT)
            val allowedInMode = isHigh || (strippedPrefix.length <= 2 && strippedPrefix.all { it in STANDARD_BOX_NOISE_CHARS })
            if (allowedInMode) {
                val subLower = multiUpperStripped.lowercase(Locale.ROOT)
                val lastUpperChar = multiUpperStripped.first()
                val isLikelySansSerifLMisread = lastUpperChar in "IL" && isValidEnglishWord(rawLower, context)
                if (!isLikelySansSerifLMisread && subLower.length >= MIN_ENGLISH_WORD_CHARS && isValidEnglishWord(subLower, context)) {
                    return subLower
                }
            }
        }

        // Step 1: Exact original word check FIRST so real words ("cat", "run", "beautiful", "level", "clean") are NEVER truncated!
        if (rawLower.length >= MIN_ENGLISH_WORD_CHARS && isValidEnglishWord(rawLower, context)) {
            return rawLower
        }

        // Step 2: Mixed-case trailing artifact strip (e.g., "importantI" -> "important", "appleL" -> "apple")
        val caseTrimmed = stripObviousMixedCaseEdgeArtifact(alphaToken)
        val caseTrimmedLower = caseTrimmed.lowercase(Locale.ROOT)
        if (caseTrimmedLower != rawLower &&
            caseTrimmedLower.length >= MIN_ENGLISH_WORD_CHARS &&
            isValidEnglishWord(caseTrimmedLower, context)
        ) {
            return caseTrimmedLower
        }

        // Step 3: Rescue valid word when surrounding checkbox/border/symbol noise characters are attached
        val rescueMinLen = 4

        if (!isHigh) {
            // MEDIUM: Only rescue standard 1–2 char stroke/box edge chars ('l','i','o','c','v','x','j')
            if (rawLower.length >= rescueMinLen + 1 && rawLower.first() in STANDARD_BOX_NOISE_CHARS) {
                val sub = rawLower.substring(1)
                if (isValidEnglishWord(sub, context)) return sub
            }
            if (rawLower.length >= rescueMinLen + 1 && rawLower.last() in STANDARD_BOX_NOISE_CHARS) {
                val sub = rawLower.dropLast(1)
                if (isValidEnglishWord(sub, context)) return sub
            }
            if (rawLower.length >= rescueMinLen + 2 &&
                rawLower.first() in STANDARD_BOX_NOISE_CHARS &&
                rawLower.last() in STANDARD_BOX_NOISE_CHARS
            ) {
                val sub = rawLower.substring(1, rawLower.length - 1)
                if (isValidEnglishWord(sub, context)) return sub
            }
            if (rawLower.length >= rescueMinLen + 2 &&
                rawLower[0] == rawLower[1] &&
                rawLower[0] in "liov"
            ) {
                val sub = rawLower.substring(2)
                if (isValidEnglishWord(sub, context)) return sub
            }
            return null
        }

        // HIGH: Aggressive character rescue for smudged/faint/touching checkboxes, bullets, and borders
        // 3a. Strip ANY 1 leading letter when rawLower is NOT a real word ("dimportant" -> "important", "bachieve" -> "achieve")
        if (rawLower.length >= rescueMinLen + 1) {
            val sub = rawLower.substring(1)
            if (isValidEnglishWord(sub, context)) return sub
        }

        // 3b. Strip 1 trailing OCR noise char ("applel" -> "apple", "importanti" -> "important")
        if (rawLower.length >= rescueMinLen + 1 && rawLower.last() in TRAILING_NOISE_CHARS) {
            val sub = rawLower.dropLast(1)
            if (isValidEnglishWord(sub, context)) return sub
        }

        // 3c. Strip both 1 leading letter and 1 trailing noise char ("lapplel" -> "apple", "dimportantl" -> "important")
        if (rawLower.length >= rescueMinLen + 2 && rawLower.last() in TRAILING_NOISE_CHARS) {
            val sub = rawLower.substring(1, rawLower.length - 1)
            if (isValidEnglishWord(sub, context)) return sub
        }

        // 3d. Strip 2 or 3 leading checkbox/border noise chars ("ooachieve", "clvocabulary", "coimportant", "ilimportant", "oooimportant")
        for (k in 2..3) {
            if (rawLower.length >= rescueMinLen + k && rawLower.take(k).all { it in EXTENDED_CHECKBOX_NOISE_CHARS }) {
                val sub = rawLower.substring(k)
                if (isValidEnglishWord(sub, context)) return sub
            }
        }

        // 3e. Strip 2 trailing noise chars ("applell" -> "apple", "importantii" -> "important")
        if (rawLower.length >= rescueMinLen + 2 &&
            rawLower[rawLower.length - 1] in TRAILING_NOISE_CHARS &&
            rawLower[rawLower.length - 2] in TRAILING_NOISE_CHARS
        ) {
            val sub = rawLower.dropLast(2)
            if (isValidEnglishWord(sub, context)) return sub
        }

        // 3f. Strip 2 leading checkbox noise chars + 1 trailing noise char ("ooimportantl" -> "important")
        if (rawLower.length >= rescueMinLen + 3 &&
            rawLower.take(2).all { it in EXTENDED_CHECKBOX_NOISE_CHARS } &&
            rawLower.last() in TRAILING_NOISE_CHARS
        ) {
            val sub = rawLower.substring(2, rawLower.length - 1)
            if (isValidEnglishWord(sub, context)) return sub
        }

        return null
    }

    private fun detectLeadingLowerBeforeTitleCase(token: String): String? {
        if (token.length < 5) return null
        var lowerCount = 0
        while (lowerCount < token.length && lowerCount < 3 && token[lowerCount].isLowerCase()) {
            lowerCount++
        }
        if (lowerCount in 1..3 &&
            lowerCount < token.length &&
            token[lowerCount].isUpperCase() &&
            token.length - lowerCount >= 4 &&
            token.substring(lowerCount + 1).dropLastWhile { it in "ILOVXJ" }.all { it.isLowerCase() }
        ) {
            val tail = token.substring(lowerCount)
            return stripObviousMixedCaseEdgeArtifact(tail)
        }
        return null
    }

    private fun detectLeadingUpperBeforeTitleCase(token: String): String? {
        if (token.length < 5) return null
        var upperCount = 0
        while (upperCount < token.length && upperCount < 4 && token[upperCount].isUpperCase()) {
            upperCount++
        }
        if (upperCount in 2..4 &&
            token.length - (upperCount - 1) >= 4 &&
            token.substring(upperCount).dropLastWhile { it in "ILOVXJ" }.let { it.length >= 3 && it.all { c -> c.isLowerCase() } }
        ) {
            val tail = token.substring(upperCount - 1)
            return stripObviousMixedCaseEdgeArtifact(tail)
        }
        return null
    }

    private fun generateDigitToLetterCandidates(
        token: String,
        sensitivity: OcrSensitivity
    ): List<String> {
        val isHigh = sensitivity == OcrSensitivity.HIGH
        val maxInternalDigits = if (isHigh) 3 else 1
        val allowedDigits = if (isHigh) "0158" else "01"

        val baseForms = LinkedHashSet<String>()
        val strippedEdges = token.trim { it.isDigit() }
        if (strippedEdges.isNotEmpty() && strippedEdges.count { it.isDigit() } <= maxInternalDigits) {
            baseForms.add(strippedEdges)
        }
        if (token.length in 4..24 && token.count { it.isDigit() } <= maxInternalDigits) {
            baseForms.add(token)
        }

        val results = LinkedHashSet<String>()
        for (form in baseForms) {
            if (form.any { it.isDigit() && it !in allowedDigits }) continue
            val withL = buildString(form.length) {
                for (ch in form) {
                    when (ch) {
                        '0' -> append('o')
                        '1' -> append('l')
                        '5' -> append('s')
                        '8' -> append('b')
                        else -> append(ch)
                    }
                }
            }
            val withI = buildString(form.length) {
                for (ch in form) {
                    when (ch) {
                        '0' -> append('o')
                        '1' -> append('i')
                        '5' -> append('s')
                        '8' -> append('b')
                        else -> append(ch)
                    }
                }
            }
            if (withL.all { it in 'a'..'z' || it in 'A'..'Z' }) results.add(withL)
            if (withI.all { it in 'a'..'z' || it in 'A'..'Z' }) results.add(withI)
        }
        return results.toList()
    }

    private fun stripObviousMixedCaseEdgeArtifact(token: String): String {
        var current = token
        if (current.length >= 4 &&
            current.last() in "ILOVXJ" &&
            current.dropLast(1).count { it.isLowerCase() } >= 3
        ) {
            current = current.dropLast(1)
        }
        if (current.length >= 5 &&
            current[0] in EXTENDED_CHECKBOX_NOISE_CHARS &&
            current[1].isUpperCase() &&
            current.substring(2).all { it.isLowerCase() }
        ) {
            current = current.substring(1)
        }
        return current
    }

    private fun isInflectedValidWord(word: String, dict: Set<String>): Boolean {
        // Check British -> American spelling normalization
        for (usVariant in britishToAmericanVariants(word)) {
            if (usVariant in dict || isSuffixInflectionInDict(usVariant, dict)) return true
        }

        // Check safe 4+ letter compound prefixes
        for (pfx in SAFE_COMPOUND_PREFIXES) {
            if (word.startsWith(pfx) && word.length >= pfx.length + 4) {
                val rest = word.substring(pfx.length)
                if (rest in dict || isSuffixInflectionInDict(rest, dict)) {
                    return true
                }
                for (usRest in britishToAmericanVariants(rest)) {
                    if (usRest in dict || isSuffixInflectionInDict(usRest, dict)) return true
                }
            }
        }
        return isSuffixInflectionInDict(word, dict)
    }

    private fun britishToAmericanVariants(word: String): List<String> {
        val out = mutableListOf<String>()
        if (word.contains("our") && word.length >= 5) {
            out.add(word.replaceFirst("our", "or"))
        }
        if (word.endsWith("ise") && word.length >= 5) out.add(word.dropLast(3) + "ize")
        if (word.endsWith("ised") && word.length >= 6) out.add(word.dropLast(4) + "ized")
        if (word.endsWith("ises") && word.length >= 6) out.add(word.dropLast(4) + "izes")
        if (word.endsWith("ising") && word.length >= 7) out.add(word.dropLast(5) + "izing")
        if (word.endsWith("isation") && word.length >= 8) out.add(word.dropLast(7) + "ization")
        if (word.endsWith("isations") && word.length >= 9) out.add(word.dropLast(8) + "izations")
        if (word.endsWith("re") && word.length >= 5 && !word.endsWith("are") && !word.endsWith("ere") && !word.endsWith("ire") && !word.endsWith("ore") && !word.endsWith("ure")) {
            out.add(word.dropLast(2) + "er")
        }
        return out
    }

    private fun isSuffixInflectionInDict(word: String, dict: Set<String>): Boolean {
        // Plural -ies -> -y
        if (word.endsWith("ies") && word.length >= 5) {
            val baseY = word.dropLast(3) + "y"
            if (baseY in dict || isDerivationalSuffixInDict(baseY, dict)) return true
        }
        // Plural -ves -> -f / -fe
        if (word.endsWith("ves") && word.length >= 5) {
            val stem = word.dropLast(3)
            if ((stem + "f") in dict || (stem + "fe") in dict) return true
        }
        // Plural / 3rd-person -es -> base or base+e
        if (word.endsWith("es") && word.length >= 4) {
            val withoutEs = word.dropLast(2)
            val withoutS = word.dropLast(1)
            if (withoutS in dict || isDerivationalSuffixInDict(withoutS, dict)) return true
            if (withoutEs.length >= 3 && (withoutEs in dict || isDerivationalSuffixInDict(withoutEs, dict))) return true
        }
        // Plural / 3rd-person -s -> base
        if (word.endsWith("s") && !word.endsWith("ss") && word.length >= 4) {
            val stem = word.dropLast(1)
            if (stem in dict || isDerivationalSuffixInDict(stem, dict)) return true
        }
        // Past -ied -> -y
        if (word.endsWith("ied") && word.length >= 5) {
            if ((word.dropLast(3) + "y") in dict) return true
        }
        // Past -ed -> base, base+e, or doubled consonant
        if (word.endsWith("ed") && word.length >= 4) {
            val stem = word.dropLast(2)
            if (stem.length >= 3 && (stem in dict || isDerivationalSuffixInDict(stem, dict))) return true
            if ((stem + "e") in dict || isDerivationalSuffixInDict(stem + "e", dict)) return true
            if (stem.length >= 4 && stem[stem.length - 1] == stem[stem.length - 2]) {
                val undoubled = stem.dropLast(1)
                if (undoubled.length >= 3 && undoubled in dict) return true
            }
        }
        // Progressive -ing -> base, base+e, or doubled consonant
        if (word.endsWith("ing") && word.length >= 5) {
            val stem = word.dropLast(3)
            if (stem.length >= 3 && (stem in dict || isDerivationalSuffixInDict(stem, dict))) return true
            if ((stem + "e") in dict || isDerivationalSuffixInDict(stem + "e", dict)) return true
            if (stem.length >= 3 && stem[stem.length - 1] == stem.getOrNull(stem.length - 2)) {
                val undoubled = stem.dropLast(1)
                if (undoubled.length >= 3 && undoubled in dict) return true
            }
            if (stem.endsWith("y") && stem.length >= 2 && (stem.dropLast(1) + "ie") in dict) {
                return true
            }
        }
        // Comparative / superlative -ier, -iest, -er, -est
        if (word.endsWith("ier") && word.length >= 5) {
            if ((word.dropLast(3) + "y") in dict) return true
        }
        if (word.endsWith("iest") && word.length >= 6) {
            if ((word.dropLast(4) + "y") in dict) return true
        }
        if (word.endsWith("er") && word.length >= 4) {
            val stem = word.dropLast(2)
            if (stem.length >= 3 && stem in dict) return true
            if ((stem + "e") in dict) return true
            if (stem.length >= 3 && stem[stem.length - 1] == stem.getOrNull(stem.length - 2)) {
                val undoubled = stem.dropLast(1)
                if (undoubled.length >= 3 && undoubled in dict) return true
            }
        }
        if (word.endsWith("est") && word.length >= 5) {
            val stem = word.dropLast(3)
            if (stem.length >= 3 && stem in dict) return true
            if ((stem + "e") in dict) return true
            if (stem.length >= 3 && stem[stem.length - 1] == stem.getOrNull(stem.length - 2)) {
                val undoubled = stem.dropLast(1)
                if (undoubled.length >= 3 && undoubled in dict) return true
            }
        }
        return isDerivationalSuffixInDict(word, dict)
    }

    private fun isDerivationalSuffixInDict(word: String, dict: Set<String>): Boolean {
        // -ily / -ally / -ly
        if (word.endsWith("ily") && word.length >= 5 && (word.dropLast(3) + "y") in dict) return true
        if (word.endsWith("ally") && word.length >= 6) {
            val stem = word.dropLast(4)
            if (stem.length >= 3 && (stem in dict || (stem + "ic") in dict || (stem + "al") in dict)) return true
        }
        if (word.endsWith("ly") && word.length >= 5) {
            val stem = word.dropLast(2)
            if (stem in dict || (stem + "le") in dict || isDerivationalCoreInDict(stem, dict)) return true
        }
        // -iness / -ness
        if (word.endsWith("iness") && word.length >= 7 && (word.dropLast(5) + "y") in dict) return true
        if (word.endsWith("ness") && word.length >= 6) {
            val stem = word.dropLast(4)
            if (stem in dict || isDerivationalCoreInDict(stem, dict)) return true
        }
        return isDerivationalCoreInDict(word, dict)
    }

    private fun isDerivationalCoreInDict(word: String, dict: Set<String>): Boolean {
        // -ment
        if (word.endsWith("ment") && word.length >= 6) {
            val stem = word.dropLast(4)
            if (stem in dict || (stem + "e") in dict) return true
        }
        // -ability / -ibility
        if ((word.endsWith("ability") || word.endsWith("ibility")) && word.length >= 8) {
            val stem = word.dropLast(7)
            if (stem.length >= 3 && (stem in dict || (stem + "e") in dict || (stem + "able") in dict || (stem + "ible") in dict)) return true
        }
        // -able / -ible / -ably / -ibly
        if ((word.endsWith("able") || word.endsWith("ible")) && word.length >= 6) {
            val stem = word.dropLast(4)
            if (stem.length >= 3 && (stem in dict || (stem + "e") in dict || (stem + "ate") in dict)) return true
        }
        if ((word.endsWith("ably") || word.endsWith("ibly")) && word.length >= 6) {
            val stem = word.dropLast(4)
            if (stem.length >= 3 && (stem in dict || (stem + "e") in dict || (stem + "able") in dict || (stem + "ible") in dict)) return true
        }
        // -ization / -ation / -ition
        if (word.endsWith("ization") && word.length >= 8) {
            val stem = word.dropLast(7)
            if (stem.length >= 3 && (stem in dict || (stem + "e") in dict || (stem + "ize") in dict || (stem + "al") in dict)) return true
        }
        if (word.endsWith("ation") && word.length >= 7) {
            val stem = word.dropLast(5)
            if (stem.length >= 3 && (stem in dict || (stem + "e") in dict || (stem + "ate") in dict)) return true
        }
        if (word.endsWith("ition") && word.length >= 7) {
            val stem = word.dropLast(5)
            if (stem.length >= 3 && (stem in dict || (stem + "e") in dict)) return true
        }
        // -ity
        if (word.endsWith("ity") && word.length >= 6) {
            val stem = word.dropLast(3)
            if (stem.length >= 3 && (stem in dict || (stem + "e") in dict)) return true
        }
        // -ize
        if (word.endsWith("ize") && word.length >= 6) {
            val stem = word.dropLast(3)
            if (stem.length >= 3 && (stem in dict || (stem + "e") in dict || (stem + "y") in dict)) return true
        }
        // -ful / -less / -ous / -ive / -al / -ical / -ist / -ism / -ship / -hood / -ward
        val simpleSuffixes = listOf("ful", "less", "ous", "ive", "ical", "al", "ist", "ism", "ship", "hood", "ward")
        for (sfx in simpleSuffixes) {
            if (word.endsWith(sfx) && word.length >= sfx.length + 4) {
                val stem = word.dropLast(sfx.length)
                if (stem in dict || (stem + "e") in dict || (stem + "y") in dict) return true
            }
        }
        return false
    }

    /**
     * Basic orthographic sanity check applied to ALL sensitivity levels (including HIGH).
     */
    fun hasPlausibleEnglishOrthography(word: String): Boolean {
        if (word.length < 3 || word.length > 26) return false
        if (!word.any { it in "aeiouy" }) return false
        if (THREE_IDENTICAL_CHARS_REGEX.containsMatchIn(word)) return false
        if (word.contains('q') && !word.contains("qu") && word !in setOf("iraq", "iraqi", "qatar", "qatari", "faq", "faqs", "tranq")) {
            return false
        }
        if (SIX_CONSONANTS_REGEX.containsMatchIn(word)) return false
        return true
    }

    private val THREE_IDENTICAL_CHARS_REGEX = Regex("(.)\\1\\1")
    private val SIX_CONSONANTS_REGEX = Regex("[bcdfghjklmnpqrstvwxz]{6,}")
}
