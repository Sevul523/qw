package com.example.data.model

/**
 * 大学受験・共通テスト頻出の重要古語マスターデータ
 * 各単語に正確な品詞・活用型、現代語訳（多義語①②③）、語源・重要ニュアンス・古今異義語の注意点を収録
 */
object ClassicalWordKnowledgeBase {

    private val RAW_ENTRIES: List<WordKnowledge> = listOf(
        // === 1. 共通テスト最重要動詞 ===
        WordKnowledge(
            word = "おどろく",
            definitions = listOf(
                Definition("動詞 カ行四段", "目を覚ます、起きる"),
                Definition("動詞 カ行四段", "（はっと）気づく"),
                Definition("動詞 カ行四段", "驚く、びっくりする")
            ),
            deck = "共通テスト最重要動詞",
            etymology = "現代語の「びっくりする」よりも、「ハッと目が覚める・気づく」の意味で入試最頻出。",
            collocations = listOf("秋来ぬと目にはさやかに見えねども 風の音にぞおどろかれぬる", "朝におどろきて"),
            synonyms = listOf("目を覚ます", "気づく"),
            antonyms = listOf("寝入る", "まどろむ"),
            note = "【最重要古今異義語】①目を覚ます ②ハッと気づく の2大現代語訳を必ず押さえる。"
        ),
        WordKnowledge(
            word = "ののしる",
            definitions = listOf(
                Definition("動詞 ラ行四段", "大声で騒ぐ、大声をあげる"),
                Definition("動詞 ラ行四段", "評判になる、噂される"),
                Definition("動詞 ラ行四段", "勢威を振るう、羽振りを利かす")
            ),
            deck = "共通テスト最重要動詞",
            etymology = "現代語の「悪口を言う」ではなく、大声で騒ぎ立てたり世間で大評判になることを表す。",
            collocations = listOf("世にののしる (世間で大評判になる)", "ののしり合ふ (大騒ぎする)"),
            synonyms = listOf("大騒ぎする", "評判になる"),
            antonyms = listOf("静まる"),
            note = "【超頻出】「大声で騒ぐ」から転じてプラスの意味「評判になる・栄える」でも用いられる。"
        ),
        WordKnowledge(
            word = "ねんず",
            definitions = listOf(
                Definition("動詞 サ行変格", "（心の中で）我慢する、じっと耐える"),
                Definition("動詞 サ行変格", "（神仏に）祈る、念じる")
            ),
            deck = "共通テスト最重要動詞",
            etymology = "「念ず（念じる）」から。痛さや辛さを心の中でじっとこらえる意味が試験で問われる。",
            collocations = listOf("痛きをねんじて (痛いのを我慢して)", "心にねんず"),
            synonyms = listOf("我慢する", "耐え忍ぶ"),
            antonyms = listOf("耐えかねる"),
            note = "入試では圧倒的に「我慢する・耐える」の意味で出題される。"
        ),
        WordKnowledge(
            word = "おぼゆ",
            definitions = listOf(
                Definition("動詞 ヤ行下二段", "（自然と）思われる、感じられる"),
                Definition("動詞 ヤ行下二段", "思い出される、思い出す"),
                Definition("動詞 ヤ行下二段", "似ている、面影がある")
            ),
            deck = "共通テスト最重要動詞",
            etymology = "「思ふ」に自発の接尾語「ゆ」が付いた形。自然と心に浮かぶニュアンス。",
            collocations = listOf("昔おぼえて (昔が思い出されて)", "親におぼえず (親に似ていない)"),
            synonyms = listOf("思われる", "似る"),
            antonyms = listOf("忘る"),
            note = "自発「思われる」と「似ている」の訳出の切り替えがポイント。"
        ),
        WordKnowledge(
            word = "まもる",
            definitions = listOf(
                Definition("動詞 ラ行四段", "じっと見つめる、見守る"),
                Definition("動詞 ラ行四段", "世話をする、大切にする")
            ),
            deck = "共通テスト最重要動詞",
            etymology = "「目（ま）＋見る（もる）」が語源。視線をじっと注ぐこと。",
            collocations = listOf("月をまもりて (月をじっと見つめて)"),
            synonyms = listOf("見つめる", "凝視する"),
            antonyms = listOf("見捨つ"),
            note = "現代語の「防衛・守護」ではなく「じっと見つめる」の意。"
        ),
        WordKnowledge(
            word = "ながむ",
            definitions = listOf(
                Definition("動詞 マ行下二段", "物思いに沈む、ぼんやりと遠くを見る"),
                Definition("動詞 マ行下二段 (詠む)", "（和歌を）口ずさむ、詠む")
            ),
            deck = "共通テスト最重要動詞",
            etymology = "「長思（ながおも）ひ」に由来。ボーッと物思いに沈みながら遠くを見つめる情景。",
            collocations = listOf("雨の降る日、つれづれとながめて", "歌をながむ"),
            synonyms = listOf("物思いに沈む", "口ずさむ"),
            antonyms = listOf("決断す"),
            note = "「眺む」＝物思いに沈む、「詠む」＝詩歌を吟じる。文脈で漢字を判断。"
        ),
        WordKnowledge(
            word = "まどふ",
            definitions = listOf(
                Definition("動詞 ハ行四段", "迷う、途方に暮れる"),
                Definition("動詞 ハ行四段", "あわてる、うろたえる"),
                Definition("動詞 接尾語", "ひどく〜する（例: 泣きまどふ）")
            ),
            deck = "共通テスト最重要動詞",
            etymology = "進むべき方向や判断を失ってパニックになること。",
            collocations = listOf("心惑ひて (心が混乱して)", "泣きまどふ (ひどく激しく泣く)"),
            synonyms = listOf("迷う", "狼狽する"),
            antonyms = listOf("落ち着く"),
            note = "動詞の連用形に付いて「ひどく〜する」という強調接尾語としても頻出。"
        ),
        WordKnowledge(
            word = "わぶ",
            definitions = listOf(
                Definition("動詞 バ行上二段", "やりきれなく思う、つらく思う"),
                Definition("動詞 バ行上二段", "思い悩む、困り果てる"),
                Definition("動詞 接尾語", "〜しかねる、〜しづらくなる")
            ),
            deck = "共通テスト最重要動詞",
            etymology = "「侘び・寂び」の語源。自分の力ではどうにもならず悲観し、途方に暮れる心情。",
            collocations = listOf("世をわびて (世の中をつらく思って)", "住みわびぬ (住みづらくなった)"),
            synonyms = listOf("嘆く", "困惑する"),
            antonyms = listOf("喜ぶ"),
            note = "「〜わぶ」で「〜するのに困る、〜しかねる」の補助動詞用法も重要。"
        ),
        WordKnowledge(
            word = "ありく",
            definitions = listOf(
                Definition("動詞 カ行四段", "歩き回る、あちこち移動する"),
                Definition("動詞 接尾語", "〜して回る、〜し続ける")
            ),
            deck = "共通テスト最重要動詞",
            etymology = "現代語の「歩く」の古形だが、単に歩くだけでなく広範囲を移動・徘徊することを指す。",
            collocations = listOf("野山をありく", "尋ねありく (探し回る)"),
            synonyms = listOf("歩き回る", "徘徊する"),
            antonyms = listOf("留まる"),
            note = "「〜ありく」で「あちこち〜して回る」の意。"
        ),
        WordKnowledge(
            word = "いぬ",
            definitions = listOf(
                Definition("動詞 ナ行変格", "行ってしまう、去る"),
                Definition("動詞 ナ行変格", "（時が）過ぎ去る、経過する")
            ),
            deck = "共通テスト最重要動詞",
            etymology = "古文に2つしかない「ナ行変格活用動詞（死ぬ・往ぬ/去ぬ）」の一つ。往生・去留。",
            collocations = listOf("立ち去りていぬ", "年月もいぬ"),
            synonyms = listOf("去る", "過ぎ行く"),
            antonyms = listOf("来（く）"),
            note = "活用表: な・に・ぬ・ぬる・ぬれ・ね。完了の助動詞「ぬ」との識別が入試頻出。"
        ),

        // === 2. 最頻出形容詞・形容動詞 ===
        WordKnowledge(
            word = "をかし",
            definitions = listOf(
                Definition("形容詞 シク活用", "すばらしい、趣（おもむき）がある"),
                Definition("形容詞 シク活用", "美しい、かわいい"),
                Definition("形容詞 シク活用", "滑稽だ、おもしろい")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "枕草子の美意識の根本。知性的・客観的な「おもしろさ・美しさ」を表す。",
            collocations = listOf("春はあけぼの…いとをかし", "をかしげなり (かわいらしい様子だ)"),
            synonyms = listOf("風情がある", "趣深い"),
            antonyms = listOf("わろし", "すさまじ"),
            note = "【古典文学の二大美意識】客観的・知的な「をかし」に対し、主観的・情緒的な「あはれ」。"
        ),
        WordKnowledge(
            word = "あはれなり",
            definitions = listOf(
                Definition("形容動詞 ナリ活用", "しみじみと心にしみる、情趣が深い"),
                Definition("形容動詞 ナリ活用", "悲しい、気の毒だ、切ない"),
                Definition("形容動詞 ナリ活用", "いとおしい、ありがたい、感銘深い")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "源氏物語の美意識「もののあはれ」。「ああ」という感嘆詞に由来し、心揺さぶられる感動。",
            collocations = listOf("あはれなること (心にしみる話)", "あはれと思ふ (いとおしく・かわいそうに思う)"),
            synonyms = listOf("情趣深い", "哀れだ"),
            antonyms = listOf("無感動なり"),
            note = "プラス評価（素敵だ・ありがたい）にもマイナス評価（悲しい・気の毒だ）にも使われる最重要語。"
        ),
        WordKnowledge(
            word = "ゆかし",
            definitions = listOf(
                Definition("形容詞 ク活用", "見たい、聞きたい、知りたい"),
                Definition("形容詞 ク活用", "心ひかれる、懐かしい")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "「行（ゆ）く＋欲（し）」＝そこへ行って自分の目で確かめたいという知的好奇心・探究心。",
            collocations = listOf("物語のゆかしさに (物語を読みたくて)", "ゆかしく思ふ"),
            synonyms = listOf("見たい", "知りたい"),
            antonyms = listOf("いとわし"),
            note = "文脈に応じて「見たい」「聞きたい」「知りたい」を的確に補って訳す。"
        ),
        WordKnowledge(
            word = "いみじ",
            definitions = listOf(
                Definition("形容詞 シク活用", "たいそう、非常に（連用形「いみじく」）"),
                Definition("形容詞 シク活用", "すばらしい、見事だ（良い意味）"),
                Definition("形容詞 シク活用", "ひどい、恐ろしい（悪い意味）")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "「忌（い）み＋路（じ）」＝度を越していて普通ではない状態。極端さを表す。",
            collocations = listOf("いみじく泣く (ひどく激しく泣く)", "いみじき人 (大変身分・能力が高い人)"),
            synonyms = listOf("甚だしい", "すばらしい"),
            antonyms = listOf("並一通りなり"),
            note = "現代の若者言葉「ヤバい」に相当。良い意味にも悪い意味にも振れる多義語。"
        ),
        WordKnowledge(
            word = "ありがたし",
            definitions = listOf(
                Definition("形容詞 ク活用", "めったにない、珍しい"),
                Definition("形容詞 ク活用", "（めったにないほど）すばらしい、尊い"),
                Definition("形容詞 ク活用", "生きづらい、生きにくい")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "「有り難し（存在することが難しい）」の文字通り、滅多に出会えない奇跡的な珍しさを表す。",
            collocations = listOf("ありがたきもの (めったにない珍しいもの)", "世にありがたき人"),
            synonyms = listOf("稀有なり", "尊し"),
            antonyms = listOf("ありがちなり", "世常なり"),
            note = "現代語の感謝「ありがとう」の原点だが、古文では「めったにない」の直訳が原則。"
        ),
        WordKnowledge(
            word = "めづらし",
            definitions = listOf(
                Definition("形容詞 シク活用", "すばらしい、好ましい"),
                Definition("形容詞 シク活用", "めったにない、目新しい"),
                Definition("形容詞 シク活用", "新鮮で愛らしい")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "「愛（め）づ」の形容詞形。単に珍しいだけでなく「称賛すべき愛らしさ・素晴らしさ」を含む。",
            collocations = listOf("めづらしき花 (愛らしく素晴らしい花)"),
            synonyms = listOf("愛づ", "賞玩す"),
            antonyms = listOf("見慣る"),
            note = "「珍奇」ではなく「称賛・好ましさ」のニュアンス。"
        ),
        WordKnowledge(
            word = "あたらし",
            definitions = listOf(
                Definition("形容詞 シク活用", "もったいない、惜しい"),
                Definition("形容詞 シク活用", "惜しむべきだ、すばらしい")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "「可惜（あたら）し」。失うのが惜しまれるほど価値があること。現代語の「新しい」とは別語。",
            collocations = listOf("あたらしき人を失ひつ (惜しい人物を亡くしてしまった)"),
            synonyms = listOf("もったいない", "惜し"),
            antonyms = listOf("惜しからず"),
            note = "【最頻出古今異義語】現代の「新しい（New）」ではなく「もったいない（惜しい）」と即答できるようにする。"
        ),
        WordKnowledge(
            word = "かなし",
            definitions = listOf(
                Definition("形容詞 シク活用", "いとおしい、かわいい"),
                Definition("形容詞 シク活用", "切ない、心にしみる"),
                Definition("形容詞 シク活用", "悲しい、嘆かわしい")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "「愛し（かなし）」「悲し（かなし）」。胸が締め付けられる感情。古文では「愛しい」の意味が多い。",
            collocations = listOf("かなしと思ふ (いとおしく大切に思う)", "かなしき子 (愛しい我が子)"),
            synonyms = listOf("いとおしい", "愛す"),
            antonyms = listOf("にくし"),
            note = "「愛し」＝いとおしい・かわいい の意味で出題されるケースが非常に多い。"
        ),
        WordKnowledge(
            word = "うつくし",
            definitions = listOf(
                Definition("形容詞 シク活用", "かわいらしい、愛らしい"),
                Definition("形容詞 シク活用", "立派だ、見事だ"),
                Definition("形容詞 シク活用", "美しい")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "幼い子どもや小動物などの無垢で小さく「かわいい」対象に向けられる愛着の情。",
            collocations = listOf("なほいとうつくしうてゐたり (依然としてとてもかわいらしい姿で座っている)"),
            synonyms = listOf("愛らしい", "可憐なり"),
            antonyms = listOf("憎らし"),
            note = "竹取物語の「いとうつくしうてゐたり」が典型。「かわいい」が基本訳。"
        ),
        WordKnowledge(
            word = "こころにくし",
            definitions = listOf(
                Definition("形容詞 ク活用", "奥ゆかしい、上品である"),
                Definition("形容詞 ク活用", "心が惹かれる、気品がある")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "「心憎し」。憎らしいほど完璧で奥ゆかしく、底知れない魅力があること。",
            collocations = listOf("こころにくき人 (奥ゆかしく魅力的な人)"),
            synonyms = listOf("奥ゆかしい", "雅なり"),
            antonyms = listOf("あさまし", "浅はかなり"),
            note = "「憎い」ではなく、最高のプラス評価「奥ゆかしい・上品だ」。"
        ),
        WordKnowledge(
            word = "おぼつかなし",
            definitions = listOf(
                Definition("形容詞 ク活用", "ぼんやりしている、はっきりしない"),
                Definition("形容詞 ク活用", "気がかりだ、不安だ"),
                Definition("形容詞 ク活用", "待ち遠しい、じれったい")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "先が見通せず、様子が定かでないため生じる胸騒ぎ・不安感。",
            collocations = listOf("行く末おぼつかなし (将来が気がかりだ・不安だ)"),
            synonyms = listOf("不安なり", "不確か便り"),
            antonyms = listOf("心安し", "確かなり"),
            note = "「はっきりしない」→「不安だ・気がかりだ」への派生が試験の狙い目。"
        ),
        WordKnowledge(
            word = "さうざうし",
            definitions = listOf(
                Definition("形容詞 シク活用", "物足りない、寂しい"),
                Definition("形容詞 シク活用", "心寂しい、満たされない")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "何かあるべきものが欠けていて、心がぽっかり空いたような虚無感。",
            collocations = listOf("つれづれなるままに、さうざうしくて"),
            synonyms = listOf("物足りない", "寂し"),
            antonyms = listOf("満足なり", "にぎはふ"),
            note = "現代語の「騒々しい（うるさい）」とは無関係。「物足りない」の意。"
        ),
        WordKnowledge(
            word = "むつかし",
            definitions = listOf(
                Definition("形容詞 シク活用", "不快だ、うっとうしい"),
                Definition("形容詞 シク活用", "恐ろしい、気味が悪い"),
                Definition("形容詞 シク活用", "面倒だ")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "現代語の「難解だ（Difficult）」ではなく、気分が害されて不快・不気味なこと。",
            collocations = listOf("むつかしき所 (気味が悪い場所・化け物が出そうな所)"),
            synonyms = listOf("不気味なり", "わずらはし"),
            antonyms = listOf("心地よし"),
            note = "「難問」の意味ではなく、「不快・気味が悪い」を表す。"
        ),
        WordKnowledge(
            word = "すさまじ",
            definitions = listOf(
                Definition("形容詞 シク活用", "興ざめだ、おもしろくない"),
                Definition("形容詞 シク活用", "殺風景だ、寒々としている")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "「進む」の否定。「をかし」の正反対で、期待が外れてテンションが下がる状態。",
            collocations = listOf("すさまじきもの (興ざめなもの・がっかりするもの)"),
            synonyms = listOf("興ざめなり", "白ける"),
            antonyms = listOf("をかし", "おもしろし"),
            note = "枕草子の「すさまじきもの」章段が著名。「興ざめだ」と訳す。"
        ),
        WordKnowledge(
            word = "あさまし",
            definitions = listOf(
                Definition("形容詞 シク活用", "驚きあきれるほどだ、思いがけない"),
                Definition("形容詞 シク活用", "ひどい、情けない、興ざめだ")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "「あさむ（驚く・呆然とする）」より。予期せぬ事態に呆気にとられるさま。",
            collocations = listOf("あさましきこと (呆れるようなひどいこと)"),
            synonyms = listOf("呆然とす", "嘆かわし"),
            antonyms = listOf("心安し"),
            note = "単なる驚きではなく「あきれる・情けない」というマイナス感情を含む。"
        ),
        WordKnowledge(
            word = "いたづらなり",
            definitions = listOf(
                Definition("形容動詞 ナリ活用", "無駄だ、役に立たない"),
                Definition("形容動詞 ナリ活用", "むなしい、手持ち無沙汰だ")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "「徒（いたづ）ら」＝努力や存在が空転して何も実を結ばないこと。",
            collocations = listOf("いたづらになりぬ (無駄になってしまった／亡くなってしまった)"),
            synonyms = listOf("無益なり", "徒労なり"),
            antonyms = listOf("甲斐あり"),
            note = "「いたづらになりぬ」で「死ぬ（空しくなる）」の婉曲表現としても頻出。"
        ),
        WordKnowledge(
            word = "あてなり",
            definitions = listOf(
                Definition("形容動詞 ナリ活用", "高貴だ、身分が高い"),
                Definition("形容動詞 ナリ活用", "上品だ、気品がある")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "「貴（あて）」＝生まれながらの上品さと身分の高さを兼ね備えた優雅さ。",
            collocations = listOf("あてなる人 (気品ある高貴な人)"),
            synonyms = listOf("高貴なり", "優雅なり"),
            antonyms = listOf("いやし (下品だ・身分が低い)"),
            note = "対義語の「いやし（卑し・賤し＝身分が低い、下品）」との対比が頻出。"
        ),
        WordKnowledge(
            word = "あやし",
            definitions = listOf(
                Definition("形容詞 シク活用 (奇し/怪し)", "不思議だ、奇妙だ"),
                Definition("形容詞 シク活用 (賤し)", "身分が低い、卑しい"),
                Definition("形容詞 シク活用 (賤し)", "みすぼらしい、粗末だ")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "漢字によって「怪し・奇し（不思議だ）」と「賤し（身分が低い・粗末だ）」に大別される。",
            collocations = listOf("あやしき家 (粗末な家)", "あやしきこと (不思議な出来事)"),
            synonyms = listOf("奇異なり", "みすぼらし"),
            antonyms = listOf("普通なり", "あてなり"),
            note = "文脈で「不思議」か「みすぼらしい（身分が低い）」かを見極める入試定番語。"
        ),
        WordKnowledge(
            word = "つれづれなり",
            definitions = listOf(
                Definition("形容動詞 ナリ活用", "（することがなく）手持ち無沙汰だ、退屈だ"),
                Definition("形容動詞 ナリ活用", "物寂しい、しんみりしている")
            ),
            deck = "最頻出形容詞・形容動詞",
            etymology = "連れ（同伴者）がおらず一人で所在のない時間を持て余すさま。『徒然草』の由来。",
            collocations = listOf("つれづれなるままに (退屈にまかせて、所在ないままに)"),
            synonyms = listOf("所在なし", "退屈なり"),
            antonyms = listOf("多忙なり"),
            note = "兼好法師の『徒然草』冒頭文で誰もが知る古語。退屈で所在ないさま。"
        ),

        // === 3. 入試必出の多義語・重要名詞 ===
        WordKnowledge(
            word = "けしき",
            definitions = listOf(
                Definition("名詞 (気色)", "様子、ありさま、気配"),
                Definition("名詞 (気色)", "機嫌、顔色、意向"),
                Definition("名詞 (気色)", "情景、景色")
            ),
            deck = "入試必出の多義語",
            etymology = "心や自然の「気」が外側に現れ出た「色（兆候・表れ）」。",
            collocations = listOf("人のけしきを見る (人の機嫌・顔色をうかがう)", "春のけしき (春の気配)"),
            synonyms = listOf("様子", "気配", "意向"),
            antonyms = listOf(),
            note = "「人のけしき」なら人の表情・機嫌、「空のけしき」なら天候・様子。"
        ),
        WordKnowledge(
            word = "かげ",
            definitions = listOf(
                Definition("名詞 (影/景)", "光（月光・日光・灯火）"),
                Definition("名詞 (影/景)", "姿、面影、肖像"),
                Definition("名詞 (陰)", "物陰、庇護、おかげ")
            ),
            deck = "入試必出の多義語",
            etymology = "古文の「かげ」の第一義は「光」。月の光＝月の影。暗がりを表す「陰」とは区別。",
            collocations = listOf("月のかげ (月の光)", "鏡のかげ (鏡に映る姿)"),
            synonyms = listOf("光", "姿"),
            antonyms = listOf("闇"),
            note = "【超重要】「月影」は現代の月の影ではなく「月光（月の光）」のこと。"
        ),
        WordKnowledge(
            word = "たより",
            definitions = listOf(
                Definition("名詞 (便り/頼り)", "手段、便利な方法、都合のよい機会"),
                Definition("名詞 (頼り)", "頼れる人、縁故、後ろ盾"),
                Definition("名詞 (便り)", "手紙、便り、知らせ")
            ),
            deck = "入試必出の多義語",
            etymology = "「寄りかかる、便とする」ことから。頼れる機会やコネクション、手段。",
            collocations = listOf("風のたより (風の知らせ／噂)", "たよりにして (頼れる縁故にして)"),
            synonyms = listOf("都合", "機会", "便り"),
            antonyms = listOf("頼りなし"),
            note = "「手紙」の意味だけでなく「機会・手段・後ろ盾（コネ）」の訳出が試される。"
        ),
        WordKnowledge(
            word = "ついで",
            definitions = listOf(
                Definition("名詞 (序/次)", "順序、順番"),
                Definition("名詞 (序/次)", "機会、都合のよい時")
            ),
            deck = "入試必出の多義語",
            etymology = "「次（つ）ぎ出で」＝順序よく次々と出てくること。",
            collocations = listOf("座のついで (座席の順序)", "事のついでに (話の機会に)"),
            synonyms = listOf("順番", "機会"),
            antonyms = listOf("不順"),
            note = "現代語の「ついでに（ついで買い等）」から「順序・機会」の正確な古義を復元する。"
        ),
        WordKnowledge(
            word = "ざえ",
            definitions = listOf(
                Definition("名詞 (才)", "学問、漢学の教養、漢詩文の才能"),
                Definition("名詞 (才)", "芸能、才芸")
            ),
            deck = "入試必出の多義語",
            etymology = "貴族男子の必須教養であった「漢才（からざえ・漢学の学識）」。",
            collocations = listOf("ざえある人 (漢学の教養がある人物)", "和魂漢才"),
            synonyms = listOf("学才", "学問"),
            antonyms = listOf("無学"),
            note = "単なる才能ではなく、特に「漢学・学問の学識」を指すことが多い。"
        ),
        WordKnowledge(
            word = "ときめく",
            definitions = listOf(
                Definition("動詞 カ行四段 (時めく)", "時に合って栄える、時流に乗る"),
                Definition("動詞 カ行四段 (時めく)", "（帝や権力者から）寵愛を受ける")
            ),
            deck = "入試必出の多義語",
            etymology = "「その時」に愛され栄えること。源氏物語桐壺冒頭「いとやむごとなき際にはあらぬが、すぐれてときめき給ふありけり」。",
            collocations = listOf("世にときめく (世間で大いに栄える)", "すぐれてときめく (並外れて寵愛される)"),
            synonyms = listOf("栄える", "寵愛さる"),
            antonyms = listOf("衰ふ"),
            note = "胸がドキドキする意味ではなく「寵愛を受けて栄える」の意味。"
        ),

        // === 4. 古今異義語・注意語 ===
        WordKnowledge(
            word = "やさし",
            definitions = listOf(
                Definition("形容詞 シク活用", "身も細るほど恥ずかしい、肩身が狭い"),
                Definition("形容詞 シク活用", "優美だ、上品だ、風流だ"),
                Definition("形容詞 シク活用", "けなげだ、感心だ")
            ),
            deck = "古今異義語・注意語",
            etymology = "「身を痩（や）す」から。自分の存在が小さく感じられる恥ずかしさから、他者への優雅さ・上品さへ派生。",
            collocations = listOf("やさしき心 (けなげで風流な心)", "恥づかしくやさし"),
            synonyms = listOf("優美なり", "けなげなり"),
            antonyms = listOf("無骨なり"),
            note = "現代語の「優しい（Kind）」ではなく「恥ずかしい」「優美だ・けなげだ」の意。"
        ),
        WordKnowledge(
            word = "らうたし",
            definitions = listOf(
                Definition("形容詞 ク活用", "かわいらしい、いとしい"),
                Definition("形容詞 ク活用", "愛おしい、手放しで庇護したい")
            ),
            deck = "古今異義語・注意語",
            etymology = "「労（らう）痛（いた）し」＝労を尽くして甲斐甲斐しく世話してあげたいほどかわいい。",
            collocations = listOf("らうたげなり (かわいらしい様子だ)"),
            synonyms = listOf("かわいらし", "愛らし"),
            antonyms = listOf("にくし"),
            note = "「労多し」ではなく、小さく無邪気なものを愛でる「かわいらしい」。"
        ),
        WordKnowledge(
            word = "びんなし",
            definitions = listOf(
                Definition("形容詞 ク活用 (便無し)", "都合が悪い、不都合だ"),
                Definition("形容詞 ク活用 (便無し)", "感心しない、けしからん、不届きだ")
            ),
            deck = "古今異義語・注意語",
            etymology = "「便（びん＝都合・便法）＋無し」。物事の筋道が通らず不都合で具合が悪いさま。",
            collocations = listOf("びんなきこと (不都合なこと／けしからぬこと)"),
            synonyms = listOf("不都合なり", "不当なり"),
            antonyms = listOf("便りよし"),
            note = "「都合が悪い」から「けしからん・とんでもない」という非難のニュアンスに注意。"
        ),
        WordKnowledge(
            word = "ほい",
            definitions = listOf(
                Definition("名詞 (本意)", "本来の志、かねてからの願い"),
                Definition("名詞 (本意)", "本懐、真意")
            ),
            deck = "古今異義語・注意語",
            etymology = "出家を望む気持ちや、長年抱いてきた本来の願望のこと。",
            collocations = listOf("ほいなきこと (本来の願いと違って不本意なこと)", "ほいを遂ぐ (本来の宿願を果たす)"),
            synonyms = listOf("本懐", "宿願"),
            antonyms = listOf("不本意"),
            note = "派生語の形容詞「ほいなし（本意なし＝不本意だ、残念だ）」も極めて頻出。"
        ),

        // === 5. 重要助動詞・敬語動詞 ===
        WordKnowledge(
            word = "給ふ",
            definitions = listOf(
                Definition("動詞・補助動詞 ハ行四段 (尊敬)", "〜なさる、お〜になる（尊格動作）"),
                Definition("動詞・補助動詞 ハ行下二段 (謙譲)", "〜ていただきます、〜申し上げます（会話文・手紙で自省）")
            ),
            deck = "重要助動詞・敬語",
            etymology = "四段活用は尊敬語（〜なさる）。下二段活用は会話文で自己の動作を卑下する謙譲語（ハ行下二段・給へ/給へ/給ふ/給ふる/給ふれ/給へよ）。",
            collocations = listOf("仰せ給ふ (おっしゃる)", "思ひ給ふ (思っております)"),
            synonyms = listOf("お〜になる", "〜申し上げる"),
            antonyms = listOf(),
            note = "四段活用（尊敬＝〜なさる）と下二段活用（謙譲＝〜ます、おります）の識別が入試必出。"
        ),
        WordKnowledge(
            word = "奉る",
            definitions = listOf(
                Definition("動詞 ラ行四段", "（貴人に）差し上げる、献上する（謙譲）"),
                Definition("動詞 ラ行四段", "お〜申し上げる（謙譲・補助動詞）"),
                Definition("動詞 ラ行四段", "（貴人が）お召しになる、召し上がる、お乗りになる（尊敬）")
            ),
            deck = "重要助動詞・敬語",
            etymology = "相手を高める謙譲語が基本。しかし「着る・飲む・食べる・乗る」の尊敬語としても機能する多機能語。",
            collocations = listOf("御文を奉る (お手紙を差し上げる)", "薬奉る (お薬を召し上がる)"),
            synonyms = listOf("差し上げる", "召し上がる"),
            antonyms = listOf("賜ふ"),
            note = "「差し上げる（謙譲）」と「召し上がる・お召しになる（尊敬）」の逆転現象を識別する。"
        ),
        WordKnowledge(
            word = "参る",
            definitions = listOf(
                Definition("動詞 ラ行四段", "（神社や貴所へ）参上する、伺う（謙譲）"),
                Definition("動詞 ラ行四段", "（貴人が）召し上がる（尊敬）"),
                Definition("動詞 ラ行四段", "〜して差し上げる（謙譲）")
            ),
            deck = "重要助動詞・敬語",
            etymology = "「まゐる」。貴人のもとへ赴く謙譲と、貴人の飲食の尊敬語。",
            collocations = listOf("宮に参る (宮中に参上する)", "大御酒参る (お酒を召し上がる)"),
            synonyms = listOf("参上す", "召し上がる"),
            antonyms = listOf("退出づ"),
            note = "「参上する（謙譲）」と「召し上がる（尊敬）」の文脈判定が頻出。"
        ),
        WordKnowledge(
            word = "侍り",
            definitions = listOf(
                Definition("動詞 ラ行変格 (謙譲)", "（貴人のお側に）お仕えする、お控えする"),
                Definition("動詞・補助動詞 ラ行変格 (丁寧)", "あります、おります、ございます、〜です/ます")
            ),
            deck = "重要助動詞・敬語",
            etymology = "「侍（はべ）り」。お側に寄り添って控える謙譲語から、丁寧語「あります・ございます」へと発展。",
            collocations = listOf("御前に侍りて (御前にお控えして)", "〜と侍り (〜でございます)"),
            synonyms = listOf("仕ふ", "あります"),
            antonyms = listOf(),
            note = "ラ行変格活用（ら・り・り・る・れ・れ）。謙譲（お仕えする）と丁寧（ございます）の使い分け。"
        ),
        WordKnowledge(
            word = "候ふ",
            definitions = listOf(
                Definition("動詞 ハ行四段 (謙譲)", "（貴人のお側に）お仕えする、伺候する"),
                Definition("動詞・補助動詞 ハ行四段 (丁寧)", "あります、おります、〜でございます")
            ),
            deck = "重要助動詞・敬語",
            etymology = "「さぶらふ」から「さうらふ」。侍りと同様に謙譲（お仕えする）と丁寧（ございます）を持つ。",
            collocations = listOf("さ候ふ (さようでございます)", "御前に候ふ"),
            synonyms = listOf("侍り", "お仕えす"),
            antonyms = listOf(),
            note = "中世・軍記物語等で多用される重要敬語。「侍り」と同等の機能を持つ。"
        )
    )

    val ENTRIES: List<WordKnowledge> by lazy {
        RAW_ENTRIES.map { raw ->
            val conjugations = mutableListOf<String>()
            val simplifiedDefs = raw.definitions.map { def ->
                val (simplePos, conj) = ClassicalPosNormalizer.splitPosAndConjugation(def.pos)
                if (conj.isNotBlank()) {
                    conjugations.add(conj)
                }
                def.copy(pos = simplePos)
            }
            val conjLabel = conjugations.distinct().joinToString("・")
            val mergedNote = ClassicalPosNormalizer.mergeConjugationIntoNote(conjugations, raw.note)
            raw.copy(
                definitions = simplifiedDefs,
                note = mergedNote,
                conjugation = conjLabel
            )
        }
    }

    val ALL_ENTRIES: List<WordKnowledge> by lazy {
        val existingWords = ENTRIES.map { it.word.lowercase() }.toSet()
        val extended = ClassicalExtendedVocabulary.getExtendedWordKnowledgeList().filter { !existingWords.contains(it.word.lowercase()) }
        ENTRIES + extended
    }

    /**
     * 古文本文・単語帳で頻出する漢字表記見出しと内蔵マスター見出し語の対応表
     */
    val KANJI_HEADWORD_ALIASES: Map<String, String> = mapOf(
        "驚く" to "おどろく",
        "罵る" to "ののしる",
        "念ず" to "ねんず",
        "覚ゆ" to "おぼゆ",
        "目守る" to "まもる",
        "守る" to "まもる",
        "眺む" to "ながむ",
        "詠む" to "ながむ",
        "惑ふ" to "まどふ",
        "侘ぶ" to "わぶ",
        "哀れなり" to "あはれなり",
        "可笑し" to "をかし",
        "招し" to "をかし",
        "甚じ" to "いみじ",
        "愛し" to "かなし",
        "悲し" to "かなし",
        "美し" to "うつくし",
        "面白し" to "おもしろし",
        "有り難し" to "ありがたし",
        "難有り" to "ありがたし",
        "床し" to "ゆかし",
        "懐かし" to "なつかし",
        "疎まし" to "うとまし",
        "羨まし" to "うらやまし",
        "後ろめたし" to "うしろめたし",
        "後目痛し" to "うしろめたし",
        "目安し" to "めやすし",
        "心細し" to "こころぼそし",
        "心苦し" to "こころぐるし",
        "心もとなし" to "こころもとなし",
        "心許なし" to "こころもとなし",
        "心にくし" to "こころにくし",
        "心憂し" to "こころうし",
        "物憂し" to "ものうし",
        "果敢なし" to "はかなし",
        "儚し" to "はかなし",
        "傍ら痛し" to "かたはらいたし",
        "味気なし" to "あぢきなし",
        "端なし" to "はしたなし",
        "便なし" to "びんなし",
        "便無し" to "びんなし",
        "本意なし" to "ほいなし",
        "甲斐なし" to "かひなし",
        "由なし" to "よしなし",
        "理なし" to "わりなし",
        "割りなし" to "わりなし",
        "口惜し" to "くちをし",
        "浅まし" to "あさまし",
        "凄まじ" to "すさまじ",
        "目覚まし" to "めざまし",
        "恥づかし" to "はづかし",
        "恥ずかし" to "はづかし",
        "麗し" to "うるはし",
        "大人し" to "おとなし",
        "著し" to "しるし",
        "拙し" to "つたなし",
        "無礼し" to "なめし",
        "憎し" to "にくし",
        "憂し" to "うし",
        "苦し" to "くるし",
        "安し" to "やすし",
        "易し" to "やすし",
        "畏し" to "かしこし",
        "賢し" to "かしこし",
        "難し" to "かたし",
        "繁し" to "しげし",
        "遠し" to "とほし",
        "近し" to "ちかし",
        "深し" to "ふかし",
        "侘し" to "わびし",
        "清らなり" to "きよらなり",
        "清げなり" to "きよげなり",
        "貴なり" to "あてなり",
        "徒らなり" to "いたづらなり",
        "顕なり" to "あらはなり",
        "優なり" to "いうなり",
        "愚かなり" to "おろかなり",
        "疎かなり" to "おろそかなり",
        "仮初めなり" to "かりそめなり",
        "希有なり" to "けうなり",
        "更なり" to "さらなり",
        "切なり" to "せちなり",
        "懇ろなり" to "ねむごろなり",
        "密かなり" to "みそかなり",
        "無下なり" to "むげなり",
        "徒然なり" to "つれづれなり",
        "等閑なり" to "なほざりなり",
        "静かなり" to "しづかなり",
        "穏やかなり" to "おだやかなり",
        "頼む" to "たのむ",
        "忍ぶ" to "しのぶ",
        "契る" to "ちぎる",
        "背く" to "そむく",
        "訪ふ" to "とぶらふ",
        "弔ふ" to "とぶらふ",
        "悩む" to "なやむ",
        "怠る" to "おこたる",
        "慣らふ" to "ならふ",
        "習ふ" to "ならふ",
        "渡る" to "わたる",
        "歩く" to "ありく",
        "住む" to "すむ",
        "澄む" to "すむ",
        "飽く" to "あく",
        "去ぬ" to "いぬ",
        "往ぬ" to "いぬ",
        "分く" to "わく",
        "理る" to "ことわる",
        "具す" to "ぐす",
        "愛づ" to "めづ",
        "賞づ" to "めづ",
        "憂ふ" to "うれふ",
        "愁ふ" to "うれふ",
        "逢ふ" to "あふ",
        "合ふ" to "あふ",
        "会ふ" to "あふ",
        "忌む" to "いむ",
        "仰す" to "おほす",
        "聞こゆ" to "きこゆ",
        "潮垂る" to "しほたる",
        "時めく" to "ときめく",
        "咎む" to "とがむ",
        "宣ふ" to "のたまふ",
        "尋ぬ" to "たづぬ",
        "詣づ" to "まうづ",
        "賜はる" to "たまはる",
        "承る" to "うけたまはる",
        "思す" to "おぼす",
        "思し召す" to "おぼしめす",
        "思ひ出づ" to "おもひいづ",
        "思い出づ" to "おもひいづ",
        "立ち寄る" to "たちよる",
        "うち見る" to "うちみる",
        "かき暮らす" to "かきくらす",
        "叶ふ" to "かなふ",
        "適ふ" to "かなふ",
        "慕ふ" to "したふ",
        "厭ふ" to "いとふ",
        "恨む" to "うらむ",
        "嘆く" to "なげく",
        "喜ぶ" to "よろこぶ",
        "遊ぶ" to "あそぶ",
        "出づ" to "いづ",
        "帰る" to "かへる",
        "至る" to "いたる",
        "寄る" to "よる",
        "泣く" to "なく",
        "鳴く" to "なく",
        "笑む" to "ゑむ",
        "笑ふ" to "わらふ",
        "問ふ" to "とふ",
        "答ふ" to "こたふ",
        "思ふ" to "おもふ",
        "忘る" to "わする",
        "微睡む" to "まどろむ",
        "驚かす" to "おどろかす",
        "催す" to "もよほす",
        "理" to "ことわり",
        "甲斐" to "かひ",
        "心ばへ" to "こころばへ",
        "志" to "こころざし",
        "気配" to "けはひ",
        "有様" to "ありさま",
        "容貌" to "かたち",
        "姿" to "すがた",
        "涙" to "なみだ",
        "文" to "ふみ",
        "物語" to "ものがたり",
        "現" to "うつつ",
        "都" to "みやこ",
        "田舎" to "ゐなか",
        "山里" to "やまざと",
        "曙" to "あけぼの",
        "夕暮れ" to "ゆふぐれ",
        "暁" to "あかつき",
        "宵" to "よひ",
        "数多" to "あまた",
        "古" to "いにしへ",
        "侮る" to "あなづる",
        "今めかし" to "いまめかし",
        "公" to "おほやけ",
        "朧けなり" to "おぼろけなり",
        "思ひ遣る" to "おもひやる",
        "思ひ侘ぶ" to "おもひわぶ",
        "忝し" to "かたじけなし",
        "語らふ" to "かたらふ",
        "悔し" to "くやし",
        "心安し" to "こころやすし",
        "言痛し" to "こちたし",
        "事々し" to "ことごとし",
        "異なり" to "ことなり",
        "定む" to "さだむ",
        "悟る" to "さとる",
        "覚る" to "さとる",
        "寂し" to "さびし",
        "騒ぐ" to "さわぐ",
        "優る" to "すぐる",
        "勝る" to "まさる",
        "増さる" to "まさる",
        "術なし" to "すべなし",
        "類なし" to "たぐひなし",
        "頼もし" to "たのもし",
        "尊し" to "たふとし",
        "貴し" to "たふとし",
        "躊躇ふ" to "ためらふ",
        "慎まし" to "つつまし",
        "努む" to "つとむ",
        "勤む" to "つとむ",
        "乏し" to "ともし",
        "慰む" to "なぐさむ",
        "準ふ" to "なずらふ",
        "泥む" to "なづむ",
        "匂ふ" to "にほふ",
        "妬し" to "ねたし",
        "長閑なり" to "のどかなり",
        "長閑けし" to "のどけし",
        "遥かなり" to "はるかなり",
        "不便なり" to "ふびんなり",
        "振る舞ふ" to "ふるまふ",
        "仄かなり" to "ほのかなり",
        "微笑む" to "ほほゑむ",
        "紛る" to "まぎる",
        "誠なり" to "まことなり",
        "真なり" to "まことなり",
        "目映ゆし" to "まばゆし",
        "眩し" to "まばゆし",
        "参らす" to "まゐらす",
        "見苦し" to "みぐるし",
        "乱る" to "みだる",
        "自ら" to "みづから",
        "睦まじ" to "むつまじ",
        "空し" to "むなし",
        "虚し" to "むなし",
        "宜なり" to "むべなり",
        "持て囃す" to "もてはやす",
        "求む" to "もとむ",
        "物狂ほし" to "ものぐるほし",
        "優し" to "やさし",
        "止む事無し" to "やごとなし",
        "世の常" to "よのつね",
        "宜し" to "よろし",
        "悪し" to "あし",
        "良し" to "よし",
        "善し" to "よし",
        "居る" to "ゐる",
        "率る" to "ゐる",
        "幼し" to "をさなし",
        "惜し" to "をし",
        "惜しむ" to "をしむ"
    )

    private val lookupMap: Map<String, WordKnowledge> by lazy {
        val map = mutableMapOf<String, WordKnowledge>()
        for (item in ALL_ENTRIES) {
            val rawKey = item.word.trim().lowercase()
            map[rawKey] = item
            val key = rawKey.substringBefore("〜").trim()
            if (key.length >= 2 && !map.containsKey(key)) {
                map[key] = item.copy(word = key)
            }
            // また活用語尾などの正規化（例: "あはれなり" -> "あはれ", "をかし" -> "おかし"）
            val kanaClean = key.removeSuffix("なり").removeSuffix("たり")
            if (kanaClean != key && kanaClean.length >= 2 && !map.containsKey(kanaClean)) {
                map[kanaClean] = item
            }
            // 現代仮名遣い対応 (あはれ -> あわれ, をかし -> おかし, はづかし -> はずかし, あぢきなし -> あじきなし)
            val modern = toModernKana(key)
            if (modern != key && !map.containsKey(modern)) {
                map[modern] = if (rawKey.contains("〜") && key.length >= 2) item.copy(word = key) else item
            }
        }
        for ((kanjiForm, kanaHead) in KANJI_HEADWORD_ALIASES) {
            val entry = map[kanaHead]
            if (entry != null && !map.containsKey(kanjiForm)) {
                map[kanjiForm] = entry
            }
        }
        map
    }

    private fun toModernKana(s: String): String {
        if (s.isEmpty()) return s
        val body = if (s.length > 1) {
            s[0] + s.substring(1)
                .replace("は", "わ")
                .replace("ひ", "い")
                .replace("ふ", "う")
                .replace("へ", "え")
                .replace("ほ", "お")
        } else {
            s
        }
        return body
            .replace("ゐ", "い")
            .replace("ゑ", "え")
            .replace("を", "お")
            .replace("ぢ", "じ")
            .replace("づ", "ず")
    }

    fun lookup(rawWord: String): WordKnowledge? {
        val word = rawWord.trim().replace("・", "").lowercase()
        if (word.isEmpty()) return null
        lookupMap[word]?.let { return it }
        val modern = toModernKana(word)
        lookupMap[modern]?.let { return it }
        val stripped = word.removeSuffix("なり").removeSuffix("たり").removeSuffix("す")
        if (stripped != word && stripped.length >= 2) {
            lookupMap[stripped]?.let { return it }
        }
        return null
    }

    /**
     * 古語初期サンプルのカードEntity群を生成
     */
    fun createInitialClassicalCards(): List<com.example.data.local.FlashcardEntity> {
        return ALL_ENTRIES.mapIndexed { index, entry ->
            com.example.data.local.FlashcardEntity(
                word = entry.word,
                definitions = entry.definitions,
                deck = entry.deck,
                note = entry.note,
                status = if (index < 2) "○" else if (index == 2) "△" else null,
                repetitionLevel = if (index < 2) 2 else if (index == 2) 1 else 0,
                intervalDays = if (index < 2) 3 else if (index == 2) 1 else 0,
                nextReviewTimestamp = if (index < 2) System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000L else if (index == 2) System.currentTimeMillis() else 0L,
                etymology = entry.etymology,
                collocations = entry.collocations,
                synonyms = entry.synonyms,
                antonyms = entry.antonyms,
                subject = "CLASSICAL"
            )
        }
    }
}
