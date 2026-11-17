package com.example.data.model

data class WordKnowledge(
    val word: String,
    val definitions: List<Definition>,
    val deck: String = "日常英会話",
    val etymology: String = "",
    val collocations: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val note: String = "",
    val conjugation: String = ""
)

object WordKnowledgeBase {
    val ENTRIES: Map<String, WordKnowledge> = listOf(
        WordKnowledge(
            word = "run",
            definitions = listOf(
                Definition("動詞", "走る、作動する、経営する、立候補する"),
                Definition("名詞", "走ること、運行、連続記録")
            ),
            deck = "日常英会話",
            etymology = "古英語 rinnan（走る・流れる）に由来。川が「流れる」、機械が「動く」、会社を「動かす＝経営する」へと派生。",
            collocations = listOf("run a business (会社を経営する)", "run out of (〜を使い果たす)", "in the long run (長い目で見れば)", "run a risk (危険を冒す)"),
            synonyms = listOf("operate", "manage", "dash", "sprint"),
            antonyms = listOf("stop", "walk", "halt"),
            note = "最重要多義語。自動詞と他動詞で意味が大きく変わる。"
        ),
        WordKnowledge(
            word = "book",
            definitions = listOf(
                Definition("名詞", "本、書籍、帳簿"),
                Definition("動詞", "予約する、記録する")
            ),
            deck = "日常英会話",
            etymology = "古英語 bōc（ブナの木）に由来。昔ブナの木の皮に文字を刻んだことから。",
            collocations = listOf("book a flight (飛行機を予約する)", "book a room (部屋を予約する)", "by the book (規則通りに)", "open book (公然の秘密・わかりやすい人)"),
            synonyms = listOf("reserve", "schedule", "volume", "tome"),
            antonyms = listOf("cancel"),
            note = "旅行・ホテル・飛行機などで動詞「予約する」として頻出。"
        ),
        WordKnowledge(
            word = "present",
            definitions = listOf(
                Definition("名詞", "贈り物、プレゼント、現在"),
                Definition("形容詞", "出席している、現在の、存在する"),
                Definition("動詞", "提示する、贈呈する、発表する")
            ),
            deck = "TOEIC頻出",
            etymology = "ラテン語 praesent-（目の前にあるもの）＝ prae（前に）+ esse（ある）に由来。",
            collocations = listOf("present a report (報告書を提示する)", "at present (現在は)", "present at the meeting (会議に出席して)", "present findings (調査結果を発表する)"),
            synonyms = listOf("current", "existing", "gift", "display", "introduce"),
            antonyms = listOf("absent (欠席の)", "past (過去の)"),
            note = "名詞/形容詞は第1音節[préznt]、動詞は第2音節[prizént]にアクセントがある。"
        ),
        WordKnowledge(
            word = "light",
            definitions = listOf(
                Definition("名詞", "光、明かり、視点、信号"),
                Definition("形容詞", "明るい、軽い、薄い、手軽な"),
                Definition("動詞", "火をつける、照らす")
            ),
            deck = "日常英会話",
            etymology = "古英語 lēoht（光）に由来。重さのlightとは語源的に異なる二つの単語が同音同綴化。",
            collocations = listOf("in light of (〜を考慮して)", "bring to light (〜を明るみに出す)", "light meal (軽食)", "shed light on (〜を解明する)"),
            synonyms = listOf("illumination", "bright", "weightless", "ignite"),
            antonyms = listOf("dark (暗い)", "heavy (重い)", "extinguish (消す)"),
            note = "「軽い」と「光」の2大義。ビジネスでは in light of が頻出。"
        ),
        WordKnowledge(
            word = "address",
            definitions = listOf(
                Definition("名詞", "住所、宛名、演説、挨拶"),
                Definition("動詞", "（問題に）取り組む、宛名を書く、演説する")
            ),
            deck = "ビジネス英語",
            etymology = "ラテン語 ad-（〜へ向かって）+ directus（真っ直ぐな）に由来。「真っ直ぐ目標に向ける」意。",
            collocations = listOf("address an issue (問題に対処する)", "address the audience (聴衆に語りかける)", "keynote address (基調講演)", "address concerns (懸念に対処する)"),
            synonyms = listOf("tackle", "handle", "deal with", "speech"),
            antonyms = listOf("ignore (無視する)", "neglect"),
            note = "TOEICやビジネス英語では「取り組む、対処する」の意味で出題率No.1。"
        ),
        WordKnowledge(
            word = "novel",
            definitions = listOf(
                Definition("名詞", "長編小説"),
                Definition("形容詞", "斬新な、新奇な、これまでにない")
            ),
            deck = "大学受験",
            etymology = "ラテン語 novus（新しい）に由来。イタリア語 novella（新しい知らせ・物語）を経て英語へ。",
            collocations = listOf("novel approach (斬新なアプローチ)", "novel idea (独創的なアイデア)", "historical novel (歴史小説)", "novel Coronavirus (新型コロナウイルス)"),
            synonyms = listOf("innovative", "original", "unprecedented", "fiction"),
            antonyms = listOf("traditional (伝統的な)", "conventional", "commonplace"),
            note = "形容詞「斬新な」を見落としやすい。science news等でも頻出。"
        ),
        WordKnowledge(
            word = "consider",
            definitions = listOf(
                Definition("動詞", "熟考する、よく考える、〜とみなす、思いやる")
            ),
            deck = "日常英会話",
            etymology = "ラテン語 com-（共に）+ sidus（星）。星をじっと見つめて吉凶を占ったことから。",
            collocations = listOf("consider options (選択肢を検討する)", "take into consideration (〜を考慮に入れる)", "carefully consider (慎重に検討する)"),
            synonyms = listOf("contemplate", "deliberate", "regard", "evaluate"),
            antonyms = listOf("ignore", "overlook", "dismiss"),
            note = "後ろに動名詞（-ing）をとる代表的な動詞。"
        ),
        WordKnowledge(
            word = "decision",
            definitions = listOf(
                Definition("名詞", "決定、決断、判決、果断さ")
            ),
            deck = "ビジネス英語",
            etymology = "ラテン語 decidere（切り離す、決定する）＝ de-（離れて）+ caedere（切る）。迷いを断ち切ること。",
            collocations = listOf("make a decision (決断を下す)", "reach a decision (結論に達する)", "decision-making process (意思決定プロセス)", "tough decision (難しい決断)"),
            synonyms = listOf("choice", "conclusion", "resolution", "verdict"),
            antonyms = listOf("indecision (優柔不断)", "hesitation"),
            note = "動詞 decide の名詞形。"
        ),
        WordKnowledge(
            word = "opportunity",
            definitions = listOf(
                Definition("名詞", "機会、好機、チャンス")
            ),
            deck = "ビジネス英語",
            etymology = "ラテン語 ob-（〜に向かって）+ portus（港）。風が船を港へ向けて吹く好都合な状態から。",
            collocations = listOf("seize an opportunity (好機を捉える)", "equal opportunity (機会均等)", "business opportunity (ビジネスチャンス)", "miss an opportunity (好機を逃す)"),
            synonyms = listOf("chance", "opening", "occasion", "prospect"),
            antonyms = listOf("misfortune", "obstacle"),
            note = "chance より公式・建設的な「好機」を表す。"
        ),
        WordKnowledge(
            word = "achieve",
            definitions = listOf(
                Definition("動詞", "達成する、成し遂げる、獲得する")
            ),
            deck = "TOEIC頻出",
            etymology = "古フランス語 a chef（頭へ、頂点へ）に由来。物事をてっぺん・完結まで持っていくこと。",
            collocations = listOf("achieve a goal (目標を達成する)", "achieve success (成功を収める)", "achieve results (成果を上げる)"),
            synonyms = listOf("accomplish", "attain", "realize", "fulfill"),
            antonyms = listOf("fail (失敗する)", "abandon (断念する)"),
            note = "努力を伴って目標を達成する文脈で使われる。"
        ),
        WordKnowledge(
            word = "perspective",
            definitions = listOf(
                Definition("名詞", "観点、見解、大局観、遠近法")
            ),
            deck = "大学受験",
            etymology = "ラテン語 per-（通して）+ specere（見る）。物事を見通す力。",
            collocations = listOf("from a different perspective (異なる視点から)", "put things in perspective (大局的に捉える)", "global perspective (地球規模の視点)"),
            synonyms = listOf("viewpoint", "standpoint", "outlook", "angle"),
            antonyms = listOf("narrow-mindedness"),
            note = "学術論文や討論で不可欠な表現。"
        ),
        WordKnowledge(
            word = "crucial",
            definitions = listOf(
                Definition("形容詞", "極めて重要な、決定的な、肝心な")
            ),
            deck = "TOEIC頻出",
            etymology = "ラテン語 crux（十字架・分岐点）。道が十字に交差してどちらへ進むか決まる分かれ道から。",
            collocations = listOf("play a crucial role (決定的な役割を果たす)", "crucial factor (決定的な要因)", "crucial moment (正念場)"),
            synonyms = listOf("vital", "essential", "critical", "pivotal"),
            antonyms = listOf("trivial (些細な)", "minor", "unimportant"),
            note = "important よりも緊急度・重要度が高い。"
        ),
        WordKnowledge(
            word = "negotiate",
            definitions = listOf(
                Definition("動詞", "交渉する、取り決める、乗り越える")
            ),
            deck = "ビジネス英語",
            etymology = "ラテン語 neg-（否定）+ otium（余暇）。「暇がないこと、商売に追われること」から。",
            collocations = listOf("negotiate a deal (取引を交渉する)", "negotiate a contract (契約を交渉する)", "negotiate terms (条件を協議する)"),
            synonyms = listOf("bargain", "confer", "settle", "mediate"),
            antonyms = listOf("concede (譲歩する)", "refuse"),
            note = "ビジネスシーンで最頻出の動詞。"
        ),
        WordKnowledge(
            word = "spectacular",
            definitions = listOf(
                Definition("形容詞", "壮観な、見事な、目覚ましい"),
                Definition("名詞", "豪華絢爛なショー、大スペクタクル")
            ),
            deck = "旅行英会話",
            etymology = "ラテン語 spectare（見つめる・眺める）に由来。spectator（観客）と同語源。",
            collocations = listOf("spectacular view (素晴らしい絶景)", "spectacular success (目覚ましい大成功)", "spectacular performance (圧巻の演技)"),
            synonyms = listOf("breathtaking", "magnificent", "stunning", "impressive"),
            antonyms = listOf("dull (退屈な)", "ordinary", "mediocre"),
            note = "旅行先の景色や公演の感想を述べるときに最適。"
        ),
        WordKnowledge(
            word = "efficient",
            definitions = listOf(
                Definition("形容詞", "効率的な、能率的な、有能な")
            ),
            deck = "ビジネス英語",
            etymology = "ラテン語 ex-（外へ）+ facere（作る・為す）。結果を無駄なく生み出すこと。",
            collocations = listOf("energy efficient (省エネの)", "efficient way (効率的な方法)", "cost-effective and efficient (費用対効果が高く効率的)"),
            synonyms = listOf("effective", "productive", "streamlined", "competent"),
            antonyms = listOf("inefficient (非効率な)", "wasteful"),
            note = "effective（効果がある）と違い「無駄なく素早い」ニュアンス。"
        )
    ).associateBy { it.word.lowercase() }

    fun lookup(word: String): WordKnowledge? {
        return ENTRIES[word.trim().lowercase()]
    }
}
