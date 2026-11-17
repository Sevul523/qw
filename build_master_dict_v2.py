import gzip
import os
import re
import ssl
import urllib.request

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

CACHE_DIR = "/tmp/dict_cache"
os.makedirs(CACHE_DIR, exist_ok=True)


def fetch_cached(url: str, filename: str) -> bytes:
    path = os.path.join(CACHE_DIR, filename)
    if os.path.exists(path) and os.path.getsize(path) > 100:
        with open(path, "rb") as f:
            return f.read()
    print(f"Downloading {url}...")
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, context=ctx, timeout=45) as r:
        data = r.read()
    with open(path, "wb") as f:
        f.write(data)
    return data


# ==============================================================================
# 1. Curated High-Precision Overrides for Fundamental & Grammar Basic Words
# ==============================================================================
CURATED_BASIC_WORDS = {
    # Articles / Determiners / Pronouns / Conjunctions / Prepositions / Auxiliaries
    "the": ("冠詞", "その, 例の (定冠詞)"),
    "a": ("冠詞", "1つの, ある (不定冠詞)"),
    "an": ("冠詞", "1つの, ある (母音で始まる語の前)"),
    "i": ("代名詞", "私は, 私が"),
    "you": ("代名詞", "あなた(たち)は, あなた(たち)を"),
    "he": ("代名詞", "彼は, 彼が"),
    "she": ("代名詞", "彼女は, 彼女が"),
    "it": ("代名詞", "それは, それを, それが"),
    "we": ("代名詞", "私たちは, 私たちが"),
    "they": ("代名詞", "彼ら(彼女ら・それら)は"),
    "me": ("代名詞", "私を, 私に"),
    "him": ("代名詞", "彼を, 彼に"),
    "her": ("代名詞", "彼女を, 彼女に / 彼女の"),
    "us": ("代名詞", "私たちを, 私たちに"),
    "them": ("代名詞", "彼ら(それら)を, 彼らに"),
    "my": ("代名詞", "私の"),
    "your": ("代名詞", "あなた(たち)の"),
    "his": ("代名詞", "彼の, 彼のもの"),
    "its": ("代名詞", "それの, その"),
    "our": ("代名詞", "私たちの"),
    "their": ("代名詞", "彼ら(それら)の"),
    "mine": ("代名詞", "私のもの"),
    "yours": ("代名詞", "あなた(たち)のもの"),
    "hers": ("代名詞", "彼女のもの"),
    "ours": ("代名詞", "私たちのもの"),
    "theirs": ("代名詞", "彼ら(それら)のもの"),
    "myself": ("代名詞", "私自身"),
    "yourself": ("代名詞", "あなた自身"),
    "himself": ("代名詞", "彼自身"),
    "herself": ("代名詞", "彼女自身"),
    "itself": ("代名詞", "それ自身"),
    "ourselves": ("代名詞", "私たち自身"),
    "themselves": ("代名詞", "彼ら自身"),
    "this": ("代名詞", "これ, この"),
    "that": ("代名詞", "あれ, それ, あの / 〜ということ (接続詞)"),
    "these": ("代名詞", "これら, これらの"),
    "those": ("代名詞", "あれら, それら, あれらの"),
    "who": ("代名詞", "だれ, だれが / 〜する人 (関係代名詞)"),
    "whom": ("代名詞", "だれを, だれに"),
    "whose": ("代名詞", "だれの"),
    "which": ("代名詞", "どちら, どれ / 〜するところの (関係代名詞)"),
    "what": ("代名詞", "何, どんなもの / 〜すること・もの"),
    "where": ("副詞", "どこに, どこで / 〜するところ (関係副詞)"),
    "when": ("副詞", "いつ / 〜するとき (接続詞)"),
    "why": ("副詞", "なぜ, どうして"),
    "how": ("副詞", "どのように, どのくらい, どんな方法"),
    "be": ("動詞", "〜である, 〜にいる, 〜になる, 存在する"),
    "am": ("動詞", "〜である, 〜にいる (Iに対するbe動詞現在形)"),
    "is": ("動詞", "〜である, 〜にいる (3人称単数現在)"),
    "are": ("動詞", "〜である, 〜にいる (複数・youに対する現在形)"),
    "was": ("動詞", "〜であった, 〜にいた (am/isの過去形)"),
    "were": ("動詞", "〜であった, 〜にいた (areの過去形)"),
    "been": ("動詞", "〜であった (beの過去分詞)"),
    "being": ("動詞", "〜であること / 存在, 生き物 (名詞)"),
    "have": ("動詞", "持っている, ある / 食べる, 飲む / 〜させる / 完了助動詞"),
    "has": ("動詞", "持っている (haveの3人称単数現在)"),
    "had": ("動詞", "持っていた (haveの過去・過去分詞)"),
    "do": ("動詞", "する, 行う, 果たす / 疑問・否定の助動詞"),
    "does": ("動詞", "する, 行う (doの3人称単数現在)"),
    "did": ("動詞", "した, 行った (doの過去形)"),
    "done": ("動詞", "終わった, 済んだ (doの過去分詞)"),
    "will": ("助動詞", "〜だろう, 〜するつもりだ / 意志, 遺言 (名詞)"),
    "would": ("助動詞", "〜だろう, 〜したものだ (willの過去・丁寧表現)"),
    "can": ("助動詞", "〜できる, 〜してもよい, 〜でありうる / 缶 (名詞)"),
    "could": ("助動詞", "〜できた, 〜できただろう (canの過去・丁寧)"),
    "may": ("助動詞", "〜かもしれない, 〜してもよい / 5月 (名詞)"),
    "might": ("助動詞", "〜かもしれない (mayの過去) / 力, 威力 (名詞)"),
    "must": ("助動詞", "〜しなければならない, 〜にちがいない"),
    "shall": ("助動詞", "〜しましょうか, 〜するつもりだ"),
    "should": ("助動詞", "〜すべきだ, 〜のはずだ"),
    "and": ("接続詞", "〜と〜, そして"),
    "but": ("接続詞", "しかし, けれども / 〜以外に (前置詞)"),
    "or": ("接続詞", "または, あるいは, さもなければ"),
    "so": ("副詞", "だから, それで (接続詞) / そんなに, とても"),
    "because": ("接続詞", "なぜなら〜だから"),
    "if": ("接続詞", "もし〜ならば / 〜かどうか"),
    "although": ("接続詞", "〜だけれども, たとえ〜でも"),
    "though": ("接続詞", "〜だけれども / もっとも〜だが (副詞)"),
    "while": ("接続詞", "〜する間に, 一方で / しばらくの間 (名詞)"),
    "unless": ("接続詞", "〜でない限り, もし〜でなければ"),
    "until": ("前置詞", "〜まで (ずっと)"),
    "till": ("前置詞", "〜まで"),
    "since": ("前置詞", "〜以来 / 〜なので (接続詞)"),
    "before": ("前置詞", "〜の前に, 〜より先に / 以前 (副詞)"),
    "after": ("前置詞", "〜の後に, 〜の後で"),
    "during": ("前置詞", "〜の間 (ずっと)"),
    "about": ("前置詞", "〜について, 〜のまわりに / 約, およそ (副詞)"),
    "above": ("前置詞", "〜の上に, 〜より上に"),
    "across": ("前置詞", "〜を横切って, 〜の向こうに"),
    "against": ("前置詞", "〜に反対して, 〜に逆らって, 〜にもたれて"),
    "along": ("前置詞", "〜に沿って / 一緒に (副詞)"),
    "among": ("前置詞", "〜の中で, 〜の間で (3つ以上)"),
    "around": ("前置詞", "〜のまわりに / 約, およそ (副詞)"),
    "as": ("前置詞", "〜として / 〜のように, 〜するとき, 〜なので (接続詞)"),
    "at": ("前置詞", "〜で, 〜に (時刻・場所の一点)"),
    "behind": ("前置詞", "〜の後ろに, 〜に遅れて"),
    "below": ("前置詞", "〜の下に, 〜より下に"),
    "beneath": ("前置詞", "〜の下に, 〜の真下に"),
    "beside": ("前置詞", "〜のそばに, 〜の横に"),
    "besides": ("前置詞", "〜に加えて / そのうえ, さらに (副詞)"),
    "between": ("前置詞", "〜の間に (主に2つの間)"),
    "beyond": ("前置詞", "〜の向こうに, 〜を越えて"),
    "by": ("前置詞", "〜によって, 〜のそばに, 〜までに"),
    "down": ("副詞", "下へ, 下に / 〜を下って (前置詞)"),
    "for": ("前置詞", "〜のために, 〜の間, 〜に向かって"),
    "from": ("前置詞", "〜から, 〜より"),
    "in": ("前置詞", "〜の中に, 〜で (場所・時・状態)"),
    "inside": ("前置詞", "〜の内側に / 内側 (名詞・副詞)"),
    "into": ("前置詞", "〜の中へ, 〜に変化して"),
    "near": ("前置詞", "〜の近くに / 近い (形容詞)"),
    "of": ("前置詞", "〜の, 〜について, 〜のうちの"),
    "off": ("副詞", "離れて, 消えて / 〜から離れて (前置詞)"),
    "on": ("前置詞", "〜の上に (接触), 〜(曜日・日付)に / 続けて (副詞)"),
    "onto": ("前置詞", "〜の上へ"),
    "out": ("副詞", "外へ, 外に, 消えて"),
    "outside": ("前置詞", "〜の外に / 外側 (名詞・副詞)"),
    "over": ("前置詞", "〜の上に, 〜を越えて / 終わって, 〜以上 (副詞)"),
    "past": ("名詞", "過去, 昔 / 過去の (形容詞) / 〜を過ぎて (前置詞)"),
    "through": ("前置詞", "〜を通って, 〜を通して / 終えて (副詞)"),
    "to": ("前置詞", "〜へ, 〜に, 〜まで / 不定詞マーカー"),
    "toward": ("前置詞", "〜の方へ, 〜に向かって"),
    "towards": ("前置詞", "〜の方へ, 〜に向かって"),
    "under": ("前置詞", "〜の下に, 〜のもとに, 〜未満で"),
    "up": ("副詞", "上へ, 上に, 起きて / 〜を上って (前置詞)"),
    "upon": ("前置詞", "〜の上に, 〜するとすぐに"),
    "with": ("前置詞", "〜と一緒に, 〜を使って, 〜を持って"),
    "within": ("前置詞", "〜以内に, 〜の内部に"),
    "without": ("前置詞", "〜なしで, 〜せずに"),
    "not": ("副詞", "〜でない, 〜しない"),
    "no": ("副詞", "いいえ / 少しも〜ない (形容詞)"),
    "yes": ("副詞", "はい, そうです, ええ"),
    "yeah": ("副詞", "うん, はい, ええ (yesの口語)"),
    "ok": ("形容詞", "よい, 大丈夫な / よし, わかった (間投詞)"),
    "okay": ("形容詞", "よい, 大丈夫な / よし, わかった (間投詞)"),
    "hello": ("間投詞", "こんにちは, やあ, もしもし"),
    "hi": ("間投詞", "やあ, こんにちは"),
    "goodbye": ("間投詞", "さようなら / 別れ (名詞)"),
    "bye": ("間投詞", "さようなら, じゃあね"),
    "please": ("副詞", "どうぞ, お願いします / 喜ばせる (動詞)"),
    "thank": ("動詞", "感謝する, お礼を言う"),
    "thanks": ("名詞", "感謝, ありがとう"),
    "sorry": ("形容詞", "すみません, 申し訳ない, 残念な, 気の毒な"),
    # Adverbs & Connectors often mistagged as Nouns
    "always": ("副詞", "いつも, 常に, 必ず"),
    "often": ("副詞", "しばしば, たびたび, よく"),
    "usually": ("副詞", "たいてい, ふつうは, いつも"),
    "sometimes": ("副詞", "ときどき, 時々"),
    "never": ("副詞", "決して〜ない, 一度も〜ない"),
    "seldom": ("副詞", "めったに〜ない"),
    "rarely": ("副詞", "めったに〜ない, まれに"),
    "already": ("副詞", "すでに, もう"),
    "yet": ("副詞", "まだ (〜ない), もう / しかし (接続詞)"),
    "still": ("副詞", "まだ, それでも / 静かな, 静止した (形容詞)"),
    "just": ("副詞", "ちょうど, たった今, ただ〜だけ / 公正な (形容詞)"),
    "soon": ("副詞", "すぐに, まもなく"),
    "now": ("副詞", "今, 現在, すぐ"),
    "then": ("副詞", "そのとき, それから, それなら"),
    "today": ("副詞", "今日, きょうは, 現代は / 今日 (名詞)"),
    "tomorrow": ("副詞", "明日, あした / 明日 (名詞)"),
    "yesterday": ("副詞", "昨日, きのう / 昨日 (名詞)"),
    "here": ("副詞", "ここに, ここで, こちらへ"),
    "there": ("副詞", "そこに, あそこに, そこで"),
    "together": ("副詞", "一緒に, 共に"),
    "alone": ("形容詞", "ひとりで, 単独の / ただ〜だけ (副詞)"),
    "again": ("副詞", "再び, もう一度, また"),
    "also": ("副詞", "〜もまた, さらに, 同様に"),
    "too": ("副詞", "〜もまた / 〜すぎる, あまりにも"),
    "very": ("副詞", "とても, 非常に / まさにその (形容詞)"),
    "really": ("副詞", "本当に, 実際に, とても"),
    "quite": ("副詞", "かなり, すっかり, まったく"),
    "almost": ("副詞", "ほとんど, もう少しで"),
    "even": ("副詞", "〜でさえ, なおいっそう / 平らな, 偶数の (形容詞)"),
    "only": ("副詞", "ただ〜だけ, ほんの / 唯一の (形容詞)"),
    "maybe": ("副詞", "たぶん, おそらく, ひょっとすると"),
    "perhaps": ("副詞", "たぶん, おそらく"),
    "probably": ("副詞", "おそらく, たぶん"),
    "certainly": ("副詞", "確かに, もちろん, 間違いなく"),
    "however": ("副詞", "しかしながら, けれども / どんなに〜でも"),
    "therefore": ("副詞", "それゆえに, したがって"),
    "anyway": ("副詞", "とにかく, いずれにせよ"),
    "anyhow": ("副詞", "とにかく, どうにかして"),
    "later": ("副詞", "あとで, のちに / より遅い (形容詞)"),
    "early": ("形容詞", "早い, 初期の / 早く (副詞)"),
    "fast": ("形容詞", "速い, しっかりした / 速く (副詞)"),
    "well": ("副詞", "よく, 上手に, 十分に / 健康な (形容詞) / 井戸 (名詞)"),
    # Indefinite pronouns
    "everyone": ("代名詞", "みんな, だれもが, 全員"),
    "everybody": ("代名詞", "みんな, だれもが, 全員"),
    "everything": ("代名詞", "すべてのこと, 何もかも, 万事"),
    "everywhere": ("副詞", "どこでも, 至るところに"),
    "someone": ("代名詞", "だれか, ある人"),
    "somebody": ("代名詞", "だれか, ある人"),
    "something": ("代名詞", "何か, あるもの"),
    "somewhere": ("副詞", "どこかに, どこかで"),
    "anyone": ("代名詞", "だれでも, だれか"),
    "anybody": ("代名詞", "だれでも, だれか"),
    "anything": ("代名詞", "何でも, 何か"),
    "anywhere": ("副詞", "どこでも, どこかに"),
    "nobody": ("代名詞", "だれも〜ない"),
    "nothing": ("代名詞", "何も〜ない, 無"),
    "nowhere": ("副詞", "どこにも〜ない"),
    "whatever": ("代名詞", "〜するものは何でも / 一体何が"),
    "whoever": ("代名詞", "〜する人はだれでも"),
    "whenever": ("接続詞", "〜するときはいつでも"),
    "wherever": ("接続詞", "〜するところはどこでも"),
    # Core nouns/verbs/adjectives that had broken or weird GENE95 entries
    "man": ("名詞", "男, 男性 / 人間, 人類 / 人手"),
    "woman": ("名詞", "女, 女性, 婦人"),
    "men": ("名詞", "男たち, 男性たち / 人々 (manの複数形)"),
    "women": ("名詞", "女たち, 女性たち (womanの複数形)"),
    "child": ("名詞", "子供, 児童, 幼児 / 我が子"),
    "children": ("名詞", "子供たち (childの複数形)"),
    "person": ("名詞", "人, 人物, 個人"),
    "people": ("名詞", "人々, 世間の人 / 国民, 民族"),
    "point": ("名詞", "点, 得点 / 要点, 主旨 / 先端 / 指さす (動詞)"),
    "stay": ("動詞", "とどまる, 滞在する, 泊まる, 〜のままでいる / 滞在 (名詞)"),
    "ask": ("動詞", "尋ねる, 質問する, 頼む, 求める"),
    "wish": ("動詞", "願う, 望む, 祈る / 願い, 希望 (名詞)"),
    "think": ("動詞", "考える, 思う, 想像する, 信じる"),
    "keep": ("動詞", "保つ, 持ち続ける, 維持する, 飼う, 守る"),
    "let": ("動詞", "〜させる, 許す, 〜させておく"),
    "watch": ("動詞", "見る, じっと見る, 見守る / 腕時計 (名詞)"),
    "get": ("動詞", "手に入れる, 得る, 受け取る / 着く / 〜になる"),
    "make": ("動詞", "作る, 製造する / 〜させる / 〜になる"),
    "take": ("動詞", "取る, 持っていく, 連れていく / 時間がかかる / 乗る"),
    "give": ("動詞", "与える, 渡す, 差し出す / 開催する"),
    "go": ("動詞", "行く, 進む, 出かける / うまくいく"),
    "come": ("動詞", "来る, やって来る, 到着する / 〜になる"),
    "see": ("動詞", "見る, 目に入る / 会う / わかる, 理解する"),
    "know": ("動詞", "知っている, わかる, 認識している"),
    "feel": ("動詞", "感じる, 〜の気分がする / 触る / 感情"),
    "want": ("動詞", "欲しい, 〜したい, 望む, 必要とする"),
    "need": ("動詞", "必要とする, 〜する必要がある / 必要 (名詞)"),
    "try": ("動詞", "試す, やってみる, 努力する / 試み (名詞)"),
    "use": ("動詞", "使う, 使用する, 利用する / 使用, 用途 (名詞)"),
    "find": ("動詞", "見つける, 発見する, 気づく"),
    "tell": ("動詞", "話す, 伝える, 教える, 命じる, 見分ける"),
    "look": ("動詞", "見る, 目を向ける / 〜のように見える / 外見 (名詞)"),
    "work": ("動詞", "働く, 仕事をする, 機能する, 効く / 仕事, 作品 (名詞)"),
    "call": ("動詞", "呼ぶ, 電話する, 叫ぶ / 電話, 呼び声 (名詞)"),
    "play": ("動詞", "遊ぶ, 試合をする, 演奏する, 演じる / 劇, 遊び (名詞)"),
    "run": ("動詞", "走る, 経営する, 作動する, 流れる / 走ること (名詞)"),
    "move": ("動詞", "動く, 動かす, 引っ越す, 感動させる / 動き (名詞)"),
    "live": ("動詞", "住む, 生きる, 生活する / 生の, 生放送の (形容詞)"),
    "believe": ("動詞", "信じる, 信頼する, 〜だと思う"),
    "bring": ("動詞", "持ってくる, 連れてくる, もたらす"),
    "happen": ("動詞", "起こる, 発生する, たまたま〜する"),
    "write": ("動詞", "書く, 執筆する, 手紙を書く"),
    "sit": ("動詞", "座る, 腰かける"),
    "stand": ("動詞", "立つ, 我慢する, 位置する / 立場, スタンド (名詞)"),
    "lose": ("動詞", "失う, 負ける, 道に迷う"),
    "pay": ("動詞", "支払う, 割に合う / 給料, 支払い (名詞)"),
    "meet": ("動詞", "会う, 出会う, 満たす, 集合する"),
    "learn": ("動詞", "学ぶ, 習う, 知る"),
    "study": ("動詞", "勉強する, 研究する / 勉強, 研究 (名詞)"),
    "speak": ("動詞", "話す, しゃべる, 演説する"),
    "read": ("動詞", "読む, 朗読する, 解釈する"),
    "help": ("動詞", "助ける, 手伝う, 役立つ / 助け, 手助け (名詞)"),
    "show": ("動詞", "見せる, 示す, 表す / ショー, 展示 (名詞)"),
    "hear": ("動詞", "聞く, 聞こえる, 耳にする"),
    "happy": ("形容詞", "幸福な, 幸せな, うれしい, 楽しい"),
    "good": ("形容詞", "よい, 上手な, 親切な, 十分な"),
    "bad": ("形容詞", "悪い, ひどい, まずい, 腐った"),
    "angry": ("形容詞", "怒った, 腹を立てた, 激しい"),
    "true": ("形容詞", "本当の, 真実の, 正確な, 忠実な"),
    "real": ("形容詞", "本物の, 現実の, 本当の"),
    "wrong": ("形容詞", "間違った, 誤った, 不正な, 具合が悪い"),
    "right": ("形容詞", "正しい, 右の, 適切な / 権利, 右 (名詞) / ちょうど (副詞)"),
    "important": ("形容詞", "重要な, 大切な, 重大な"),
    "story": ("名詞", "話, 物語, 記事 / 建物の階, 階層"),
    "mom": ("名詞", "お母さん, ママ"),
    "mother": ("名詞", "母, お母さん"),
    "dad": ("名詞", "お父さん, パパ"),
    "father": ("名詞", "父, お父さん"),
    "friend": ("名詞", "友人, 友達, 味方"),
    "school": ("名詞", "学校, 学部, 流派"),
    "water": ("名詞", "水 / 水をやる (動詞)"),
    "company": ("名詞", "会社, 企業 / 仲間, 交際, 来客"),
    "time": ("名詞", "時間, 時, 時刻, 回数, 時代"),
    "day": ("名詞", "日, 1日, 昼間, 時代"),
    "year": ("名詞", "年, 1年間, 年度"),
    "week": ("名詞", "週, 1週間"),
    "month": ("名詞", "月, 1か月"),
    "world": ("名詞", "世界, 地球, 世間"),
    "life": ("名詞", "生命, 命, 人生, 生活"),
    "way": ("名詞", "道, 方法, やり方, 方向, 様子"),
    "thing": ("名詞", "もの, 事, 物事, 状況"),
    "place": ("名詞", "場所, 所, 順位 / 置く (動詞)"),
    "house": ("名詞", "家, 住宅, 議会 / 収容する (動詞)"),
    "home": ("名詞", "家, 家庭, 故郷 / 家へ (副詞)"),
    "room": ("名詞", "部屋, 余地, 空間"),
    "book": ("名詞", "本, 書籍, 帳簿 / 予約する (動詞)"),
    "music": ("名詞", "音楽, 楽曲, 楽譜"),
    "game": ("名詞", "ゲーム, 試合, 遊び"),
    "money": ("名詞", "お金, 金銭, 通貨"),
    "food": ("名詞", "食べ物, 食料, 食品"),
    "family": ("名詞", "家族, 家庭, 一族"),
    "student": ("名詞", "学生, 生徒, 研究者"),
    "teacher": ("名詞", "教師, 先生"),
    "country": ("名詞", "国, 国家, 田舎, 地方"),
    "city": ("名詞", "市, 都市, 都会"),
    "problem": ("名詞", "問題, 難問, トラブル"),
    "question": ("名詞", "質問, 疑問, 問題 / 質問する (動詞)"),
    "answer": ("名詞", "答え, 返事, 解答 / 答える (動詞)"),
    "idea": ("名詞", "考え, アイデア, 思いつき, 理念"),
    "fact": ("名詞", "事実, 真実, 現実"),
    "number": ("名詞", "数, 数字, 番号 / 数える (動詞)"),
    "part": ("名詞", "部分, 役割, 部品 / 分かれる (動詞)"),
    "group": ("名詞", "グループ, 集団, 群れ"),
    "name": ("名詞", "名前, 氏名, 名声 / 名づける (動詞)"),
    "word": ("名詞", "単語, 言葉, 発言, 約束"),
    "love": ("動詞", "愛する, 大好きである / 愛, 愛情 (名詞)"),
    "all": ("代名詞", "すべて, 全員 / すべての (形容詞) / すっかり (副詞)"),
    "both": ("代名詞", "両方とも, 2つとも / 両方の (形容詞)"),
    "each": ("形容詞", "おのおのの, それぞれの / 各自 (代名詞)"),
    "every": ("形容詞", "すべての, 毎〜, あらゆる"),
    "some": ("形容詞", "いくつかの, いくらかの / ある〜 (代名詞)"),
    "any": ("形容詞", "いくつかの (疑問・否定), どんな〜でも"),
    "many": ("形容詞", "多くの (数えられる名詞) / 多くの人 (代名詞)"),
    "much": ("形容詞", "多くの (量), 多量の / とても, 大いに (副詞)"),
    "more": ("形容詞", "もっと多くの / さらに, もっと (副詞)"),
    "most": ("形容詞", "ほとんどの, 最も多くの / 最も (副詞)"),
    "few": ("形容詞", "ほとんどない (否定), 少数の (a few)"),
    "little": ("形容詞", "小さい, 幼い, ほとんどない (量) / 少し (副詞)"),
    "less": ("形容詞", "より少ない / より少なく (副詞)"),
    "least": ("形容詞", "最も少ない / 少なくとも (at least)"),
    "several": ("形容詞", "いくつかの, 数人の, それぞれの"),
    "other": ("形容詞", "他の, 別の / もう一方, 他者 (代名詞)"),
    "another": ("形容詞", "もう1つの, 別の / もう1つ (代名詞)"),
    "such": ("形容詞", "そのような, こんな, 実に〜な"),
    "same": ("形容詞", "同じ, 同一の / 同じもの (代名詞)"),
    "different": ("形容詞", "異なった, 別の, さまざまな"),
    "large": ("形容詞", "大きい, 広い, 大規模な"),
    "small": ("形容詞", "小さい, 少ない, 些細な"),
    "big": ("形容詞", "大きい, 重大な, 年上の"),
    "long": ("形容詞", "長い, 久しい / 長く (副詞) / 切望する (動詞)"),
    "short": ("形容詞", "短い, 低い, 不足している"),
    "high": ("形容詞", "高い, 高度な / 高く (副詞)"),
    "low": ("形容詞", "低い, 安い, 弱い / 低く (副詞)"),
    "old": ("形容詞", "古い, 年をとった, 昔の, 〜歳の"),
    "young": ("形容詞", "若い, 幼い, 初期の"),
    "new": ("形容詞", "新しい, 最近の, 慣れていない"),
    "absolute": ("形容詞", "絶対的な, 完全な, 紛れもない"),
    "everyone": ("代名詞", "みんな, だれもが, 全員"),
    "download": ("動詞", "ダウンロードする / ダウンロード (名詞)"),
    "login": ("名詞", "ログイン, 接続 / ログインする (動詞)"),
    "email": ("名詞", "電子メール, Eメール / メールを送る (動詞)"),
    "website": ("名詞", "ウェブサイト, ホームページ"),
    "internet": ("名詞", "インターネット"),
    "smartphone": ("名詞", "スマートフォン"),
    "video": ("名詞", "ビデオ, 動画, 映像"),
    "photo": ("名詞", "写真"),
    "database": ("名詞", "データベース"),
}

# EDICT2 inflected/slang overrides that harm normal English lookup/stemming
EDICT2_POLLUTION_BLOCKLIST = {
    "isn", "didn", "doesn", "wasn", "weren", "wouldn", "couldn", "shouldn",
    "hasn", "hadn", "haven", "aren", "ain", "doing", "talking", "getting",
    "looking", "called", "having", "wants", "hands", "guys", "girls", "kids",
    "friends", "looks", "51sts", "a.k.", "a-head", "a-si:h", "49ers",
}


def load_frequency_lists():
    g_raw = fetch_cached(
        "https://raw.githubusercontent.com/first20hours/google-10000-english/master/google-10000-english-usa-no-swears.txt",
        "google10k.txt",
    ).decode("utf-8", errors="ignore").splitlines()

    e_raw = fetch_cached(
        "https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/en/en_50k.txt",
        "en50k.txt",
    ).decode("utf-8", errors="ignore").splitlines()

    google_words = [w.strip().lower() for w in g_raw if re.match(r"^[a-z]{1,20}$", w.strip().lower())]
    en50k_words = []
    for line in e_raw:
        parts = line.strip().split()
        if parts:
            w = parts[0].lower()
            if re.match(r"^[a-z]{1,20}$", w):
                en50k_words.append(w)
    return google_words, en50k_words


LEXICALIZED_INFLECTED_KEEP = {
    "news", "always", "sometimes", "perhaps", "business", "building", "morning",
    "evening", "interesting", "exciting", "tired", "surprised", "worried",
    "married", "united", "clothes", "glasses", "thanks", "besides", "towards",
    "feelings", "savings", "customs", "works", "means", "series", "species",
    "politics", "physics", "economics", "mathematics", "electronics",
    "amazing", "boring", "confusing", "disappointed", "embarrassed",
    "frightened", "interested", "pleased", "relaxed", "satisfied", "shocked",
    "used", "born", "crowded", "broken", "closed", "known", "written", "spoken",
    "living", "missing", "following", "remaining", "existing", "growing",
    "leading", "including", "concerning", "regarding", "according", "during",
    "including", "outstanding", "underlying", "corresponding", "understanding",
    "training", "learning", "meaning", "feeling", "meeting", "beginning",
    "ending", "wedding", "shopping", "swimming", "running", "cooking",
    "reading", "writing", "speaking", "listening", "teaching", "thinking",
    "painting", "drawing", "parking", "smoking", "housing", "planning",
    "marketing", "engineering", "accounting", "programming", "banking",
    "clothing", "warning", "opening", "setting", "crossing", "finding",
    "children", "women", "men", "feet", "teeth", "people",
    "am", "is", "are", "was", "were", "been", "being", "has", "had", "does", "did", "done",
    "hers", "ours", "theirs", "yours", "its", "him", "her", "them", "us", "me",
}

SLANG_CONTRACTIONS_SKIP = {
    "didn", "doesn", "wasn", "weren", "wouldn", "couldn", "shouldn", "hasn",
    "hadn", "haven", "aren", "isn", "ain", "wanna", "gonna", "gotta", "kinda",
    "sorta", "uh", "um", "hmm", "mmm", "ah", "oh", "hey", "huh", "wow", "yay",
    "ya", "yep", "nope", "yo", "ow", "aw", "shh", "blah", "haha", "hehe",
    "lol", "omg", "btw", "fyi", "tbh", "idk", "imo", "imho",
    "ben", "tom", "jack", "john", "sam", "mike", "david", "paul", "mark",
    "mary", "sarah", "anna", "bob", "joe", "jim", "tim", "dan", "alex",
    "chris", "kate", "jane", "lisa", "nick", "tony", "steve", "peter", "frank",
    "george", "harry", "henry", "charles", "james", "william", "robert",
    "michael", "richard", "thomas", "daniel", "matthew", "kevin", "brian",
    "eric", "jason", "jeff", "ryan", "adam", "scott", "gary", "larry",
    "ray", "roy", "lee", "max", "leo", "amy", "emma", "emily", "laura",
    "rachel", "kelly", "julie", "susan", "karen", "nancy", "helen", "betty",
    "jan", "feb", "mar", "apr", "jun", "jul", "aug", "sep", "oct", "nov", "dec",
    "ii", "iii", "iv", "vi", "vii", "viii", "ix", "xi", "xii", "html", "http",
    "https", "www", "com", "org", "net", "edu", "gov", "php", "xml", "css",
    "sql", "rss", "url", "pdf", "jpg", "png", "gif", "zip", "exe", "iso",
    "usb", "cpu", "ram", "dvd", "tv", "pc", "id", "ip", "os", "ui", "api",
    "etc", "vs", "mr", "mrs", "ms", "dr", "jr", "sr", "st", "inc", "ltd",
    "corp", "llc", "co", "est", "pst", "gmt", "utc",
}


def is_redundant_inflection(word: str, known_set: set) -> bool:
    if word in LEXICALIZED_INFLECTED_KEEP or word in CURATED_BASIC_WORDS:
        return False
    if len(word) <= 3:
        return False
    # Plural / 3rd person -s/-es/-ies
    if word.endswith("ies") and len(word) > 4 and (word[:-3] + "y") in known_set:
        return True
    if word.endswith("es") and len(word) > 3 and (word[:-2] in known_set or word[:-1] in known_set):
        return True
    if word.endswith("s") and not word.endswith("ss") and not word.endswith("us") and not word.endswith("is") and word[:-1] in known_set:
        return True
    # Past / participle -ed/-ied
    if word.endswith("ied") and len(word) > 4 and (word[:-3] + "y") in known_set:
        return True
    if word.endswith("ed") and len(word) > 4:
        if word[:-2] in known_set or word[:-1] in known_set:
            return True
        if len(word) > 5 and word[-3] == word[-4] and word[:-3] in known_set:
            return True
    # -ing
    if word.endswith("ing") and len(word) > 5:
        if word[:-3] in known_set or (word[:-3] + "e") in known_set:
            return True
        if len(word) > 6 and word[-4] == word[-5] and word[:-4] in known_set:
            return True
    return False


def clean_gene95_meaning(word: str, pos: str, meanings: str, all_entries: dict) -> tuple[str, str]:
    m = meanings.strip()

    # 1. Strip "=word " duplicate headword prefixes (e.g. "story -> =story (家・建物の)階...")
    m = re.sub(rf"^={re.escape(word)}\b[\s,]*", "", m, flags=re.IGNORECASE).strip()

    # 2. Resolve pure "=target" aliases to real Japanese meanings
    eq_match = re.match(r"^=([a-zA-Z\-']{2,30})(?:\s*\d+)?(?:\s*[,/;]\s*(.+))?$", m)
    if eq_match:
        target = eq_match.group(1).lower()
        extra = eq_match.group(2) or ""
        if target in CURATED_BASIC_WORDS:
            t_pos, t_mean = CURATED_BASIC_WORDS[target]
            m = f"{t_mean}" + (f" / {extra}" if extra else "")
            if pos in ("名詞", "英単語"):
                pos = t_pos
        elif target in all_entries:
            t_entry = all_entries[target]
            t_m = t_entry[2]
            if not t_m.startswith("="):
                m = f"{t_m} (={target})" if not extra else f"{extra} / {t_m}"
                if pos == "英単語" and t_entry[1] != "英単語":
                    pos = t_entry[1]

    # 3. Repair known chopped leading characters from GENE95 parser
    chop_fixes = [
        (r"^がった先", "とがった先"),
        (r"^どまる([,;/])", r"とどまる\1"),
        (r"^どまる$", "とどまる"),
        (r"^ついて([,、])", r"〜について\1"),
        (r"^ついて質問する", "〜について質問する"),
        (r"^であればよいのにと思う", "〜であればよいのにと思う"),
        (r"^であればよかったのにと思う", "〜であればよかったのにと思う"),
    ]
    for pat, repl in chop_fixes:
        m = re.sub(pat, repl, m)

    # 4. Strip noisy example sentences / GENE95 inline meta
    m = re.sub(r"\s*/\s*[A-Z][a-zA-Z0-9,\s'\.\-]{25,}\.?$", "", m).strip()
    m = m.replace("■", " ").strip()
    m = re.sub(r"\s{2,}", " ", m)

    # 5. Fix obvious POS errors based on meaning patterns when current POS is "名詞" or "英単語"
    if pos in ("名詞", "英単語") and word not in CURATED_BASIC_WORDS:
        first_seg = m.split("/")[0].split(",")[0].split(";")[0].strip()
        if re.search(r"(?:する|いる|ある|なる|れる|られる|せる|させる|める|てる|ける|げる|べる|ねる|む|く|ぐ|す|つ|ぶ|う)$", first_seg):
            if not re.search(r"(?:こと|もの|人|物|場所|状態|行為|名詞)$", first_seg):
                pos = "動詞"
        elif re.search(r"(?:ない|しい|たい|やすい|にくい|っぽい|的な|な)$", first_seg) or (
            re.search(r"[あ-ん]い$", first_seg) and not re.search(r"(?:思い|願い|戦い|笑い|祝い|違い|出会い|間違い|匂い|臭い|勢い|迷い|問い|救い|払い|扱い|住まい|振舞い|言い合い|話し合い|付き合い|見合い|知り合い|親しみ|悲しみ|苦しみ|楽しみ|喜び)$", first_seg)
        ):
            pos = "形容詞"

    return pos, m


def main():
    # 1. Load existing base dictionary
    existing = {}
    base_path = "app/src/main/assets/internal_ej_dict.tsv"
    if os.path.exists(base_path):
        with open(base_path, "r", encoding="utf-8") as f:
            for line in f:
                parts = line.rstrip("\r\n").split("\t")
                if len(parts) >= 3:
                    w = parts[0].strip()
                    k = w.lower()
                    pos = parts[1].strip()
                    meanings = parts[2].strip()
                    deck = parts[3].strip() if len(parts) > 3 else "日常英会話"
                    note = parts[4].strip() if len(parts) > 4 else ""
                    if k in EDICT2_POLLUTION_BLOCKLIST:
                        continue
                    existing[k] = [w, pos, meanings, deck, note]
    print(f"Loaded existing base entries: {len(existing):,}")

    # Clean junk out of 日常英会話 (move valid tech/slang out to tier 3/4, remove blatant noise)
    cleaned_existing = {}
    removed_junk = 0
    demoted_junk = 0
    for k, val in existing.items():
        w, pos, m, deck, note = val
        if re.search(r"\+\d+$", k) or k in ("a.k.", "51sts", "a-head", "a-si:h"):
            removed_junk += 1
            continue
        if deck == "日常英会話" and not re.match(r"^[a-z][a-z\s\-']*$", k):
            val[3] = "総合英和語彙"
            demoted_junk += 1
        cleaned_existing[k] = val
    existing = cleaned_existing
    print(f"Cleaned existing: removed {removed_junk} junk entries, demoted {demoted_junk} non-basic entries from 日常英会話.")

    # 2. Load frequency lists & construct verified 5,000 basic English words
    google_words, en50k_words = load_frequency_lists()
    google_rank = {w: idx + 1 for idx, w in enumerate(google_words)}
    en50k_rank = {w: idx + 1 for idx, w in enumerate(en50k_words)}

    # Build combined ordered pool of real words
    basic_5000_rank = {}
    known_lemmas = set(CURATED_BASIC_WORDS.keys())
    rank_counter = 1

    # Prioritize curated core words first (sorted by corpus frequency)
    curated_sorted = sorted(
        CURATED_BASIC_WORDS.keys(),
        key=lambda w: min(en50k_rank.get(w, 99999), google_rank.get(w, 99999)),
    )
    for w in curated_sorted:
        basic_5000_rank[w] = rank_counter
        rank_counter += 1

    # Interleave en50k (spoken/school everyday) + google10k (written/general)
    combined_stream = []
    max_len = max(len(en50k_words), len(google_words))
    for i in range(max_len):
        if i < len(en50k_words):
            combined_stream.append(en50k_words[i])
        if i < len(google_words):
            combined_stream.append(google_words[i])

    for w in combined_stream:
        if rank_counter > 5000:
            break
        if w in basic_5000_rank or w in SLANG_CONTRACTIONS_SKIP or w in EDICT2_POLLUTION_BLOCKLIST:
            continue
        if len(w) < 2:
            continue
        # Only admit words that exist in existing/EDICT2 and are not redundant inflections
        if w not in existing:
            continue
        if is_redundant_inflection(w, known_lemmas | set(existing.keys())):
            continue
        # Reject words whose existing entry is pure garbage/slang/abbreviation
        ex_m = existing[w][2]
        if any(bad in ex_m for bad in ("ass-kisser", "売春婦", "LSD常用者", "国際腎臓学会")):
            continue
        basic_5000_rank[w] = rank_counter
        known_lemmas.add(w)
        rank_counter += 1

    print(f"Curated & verified Basic 5,000 essential words: {len(basic_5000_rank):,}")

    # 3. Download & parse Compdic + Edict2
    comp_entries = {}
    try:
        comp_data = gzip.decompress(
            fetch_cached("https://ftp.edrdg.org/pub/Nihongo/compdic.gz", "compdic.gz")
        )
        for line in comp_data.decode("euc-jp", errors="replace").splitlines():
            parts = line.split(" /")
            if len(parts) < 2:
                continue
            head = parts[0].split(" [")[0].replace(";", ", ").strip()
            for g in parts[1:]:
                g = g.strip().strip("/")
                if not g:
                    continue
                clean = re.sub(r"^\([^\)]+\)\s*", "", g).strip()
                clean = re.sub(r"\([^\)]*\)", "", clean).strip()
                if not clean or len(clean) < 2 or len(clean) > 80 or re.match(r"^[\d\W]+$", clean):
                    continue
                k = clean.lower()
                if k in EDICT2_POLLUTION_BLOCKLIST:
                    continue
                if k not in comp_entries:
                    pos = "熟語・連語" if (" " in clean or "-" in clean) else "IT用語"
                    comp_entries[k] = [clean, pos, head, "IT・プログラミング", "情報処理・IT専門用語"]
        print(f"Parsed Compdic: {len(comp_entries):,}")
    except Exception as e:
        print(f"Compdic warning: {e}")

    edict_new = {}
    edict_data = gzip.decompress(
        fetch_cached("https://ftp.edrdg.org/pub/Nihongo/edict2.gz", "edict2.gz")
    )
    edict_lines = edict_data.decode("euc-jp", errors="replace").splitlines()
    MED_PATTERN = re.compile(r"\b(med|anat|bio|pharm|chem|syndrome|disease|disorder|infection|artery|vein|cell|organ|bacteria|virus|muscle|bone|gland|cancer|tumor|hormone|cortex|neuron|peptide|protein|acid|enzyme|therapy|symptom|lesion|vascular|cardiac|cerebral|renal|pulmonary)\b", re.I)
    BIZ_PATTERN = re.compile(r"\b(econ|fin|bus|trade|market|stock|tax|bond|audit|dividend|deficit|profit|asset|liability|depreciation|collateral|equity|monetary|fiscal)\b", re.I)
    COMP_PATTERN = re.compile(r"\b(comp|comput|data|byte|server|packet|router|cache|pixel|algorithm|protocol|database|software|hardware|network|interface|kernel|compiler)\b", re.I)

    for line in edict_lines[1:]:
        parts = line.split(" /")
        if len(parts) < 2:
            continue
        head = parts[0]
        kanji_reading = head.split(" [")[0].replace(";", ", ").strip()
        reading_match = re.search(r"\[(.*?)\]", head)
        reading = f" ({reading_match.group(1)})" if reading_match else ""
        full_jp = f"{kanji_reading}{reading}"
        raw_text = line
        for g in parts[1:]:
            g = g.strip().strip("/")
            if not g or g.startswith("EntL"):
                continue
            pos = "英単語"
            if any(tag in g for tag in ["(v5", "(v1", "(vt)", "(vi)", "(v-"]):
                pos = "動詞"
            elif any(tag in g for tag in ["(adj-", "(adj)"]):
                pos = "形容詞"
            elif "(adv" in g:
                pos = "副詞"
            elif any(tag in g for tag in ["(exp)", "(id)"]):
                pos = "熟語・連語"
            elif "(n)" in g or "(n," in g:
                pos = "名詞"
            clean = re.sub(r"^\([^\)]+\)\s*", "", g)
            clean = re.sub(r"^\(\d+\)\s*", "", clean)
            clean = re.sub(r"\(See [^\)]+\)", "", clean).strip()
            for sub in clean.split("/"):
                sub = sub.strip()
                sub_key = re.sub(r"\([^\)]*\)", "", sub).strip()
                if not sub_key or len(sub_key) < 2 or len(sub_key) > 80 or re.match(r"^[\d\W]+$", sub_key):
                    continue
                k = sub_key.lower()
                if k in EDICT2_POLLUTION_BLOCKLIST:
                    continue
                if " " in sub_key or "-" in sub_key or pos == "熟語・連語":
                    deck = "重要イディオム・連語"
                    pos = "熟語・連語"
                elif MED_PATTERN.search(raw_text) or MED_PATTERN.search(sub_key):
                    deck = "医学・自然科学"
                elif BIZ_PATTERN.search(raw_text) or BIZ_PATTERN.search(sub_key):
                    deck = "TOEIC・ビジネス"
                elif COMP_PATTERN.search(raw_text) or COMP_PATTERN.search(sub_key):
                    deck = "IT・プログラミング"
                elif len(sub_key) >= 10 or any(sub_key.endswith(s) for s in ["ology", "ation", "istic", "ous", "ible", "able"]):
                    deck = "TOEFL・留学・GRE"
                else:
                    deck = "専門・一般語彙"
                note = f"EDICT2大辞典収録 ({deck})"
                if k in edict_new:
                    cur_meanings = edict_new[k][2]
                    if kanji_reading not in cur_meanings and len(cur_meanings.split(" / ")) < 4:
                        edict_new[k][2] = f"{cur_meanings} / {full_jp}"
                else:
                    edict_new[k] = [sub_key, pos, full_jp, deck, note]
    print(f"Parsed Edict2: {len(edict_new):,}")

    # 4. Merge all sources
    merged = dict(existing)

    # Clean trailing '~' / '~ing' / '...' from multi-word idioms
    for k, val in list(existing.items()):
        if k.endswith(" ~") or k.endswith(" ~ing") or k.endswith(" ..."):
            cleaned = re.sub(r"\s+(?:~ing|~|\.\.\.)$", "", val[0]).strip()
            ck = cleaned.lower()
            if " " in ck and "~" not in ck and re.match(r"^[a-z][a-z\s\-']+[a-z]$", ck):
                if ck not in merged:
                    pos = val[1] if val[1] in ("句動詞", "熟語・連語", "連語", "前置詞句", "成句") else "熟語・連語"
                    merged[ck] = [cleaned, pos, val[2], "大学入試重要熟語", val[4]]

    for k, val in comp_entries.items():
        if k not in merged:
            merged[k] = val
    for k, val in edict_new.items():
        if k not in merged:
            merged[k] = val

    # Merge curated idioms
    for idiom_file in ["idioms_5000.tsv", "all_idioms.tsv"]:
        if os.path.exists(idiom_file):
            with open(idiom_file, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue
                    parts = line.split("\t")
                    if len(parts) >= 3:
                        k = parts[0].strip().lower()
                        p = parts[1].strip()
                        m = parts[2].strip()
                        d = parts[3].strip() if len(parts) > 3 else "大学入試重要熟語"
                        n = parts[4].strip() if len(parts) > 4 else ""
                        merged[k] = [parts[0].strip(), p, m, d, n]

    essential_idioms = {
        "take into account": ("句動詞", "〜を考慮に入れる", "大学入試重要熟語", "重要連語: take A into account / take into account A"),
        "take advantage of": ("句動詞", "〜を利用する, 〜に付け込む", "大学入試重要熟語", "重要連語: make good use of / exploit"),
        "take into consideration": ("句動詞", "〜を考慮に入れる", "大学入試重要熟語", "同義: take into account"),
        "make use of": ("句動詞", "〜を利用する, 活用する", "大学入試重要熟語", "make good/full use of でよく使用"),
        "make sense of": ("句動詞", "〜を理解する, 意味をつかむ", "大学入試重要熟語", "複雑な内容を理解するときに頻出"),
        "keep pace with": ("句動詞", "〜に遅れずについていく", "大学入試重要熟語", "技術革新・社会変化の文脈で頻出"),
        "lose sight of": ("句動詞", "〜を見失う, 忘れる", "大学入試重要熟語", "対義: catch sight of"),
        "catch sight of": ("句動詞", "〜を見かける", "大学入試重要熟語", "対義: lose sight of"),
        "give rise to": ("句動詞", "〜を引き起こす, 生じさせる", "大学入試重要熟語", "原因→結果の最重要動詞句 (= cause, bring about)"),
        "shed light on": ("句動詞", "〜を解明する, 光をあてる", "大学入試重要熟語", "論文・評論読解で超頻出"),
        "play a role in": ("連語", "〜において役割を果たす", "大学入試重要熟語", "play an important/vital role in"),
        "have an impact on": ("連語", "〜に影響を与える", "大学入試重要熟語", "同義: have an effect/influence on"),
        "pave the way for": ("連語", "〜への道を開く, 下地を作る", "大学入試重要熟語", "歴史・科学技術長文で頻出"),
        "at the expense of": ("前置詞句", "〜を犠牲にして", "大学入試重要熟語", "同義: at the cost of, at the sacrifice of"),
        "in light of": ("前置詞句", "〜を考慮して, 〜に照らして", "大学入試重要熟語", "同義: in view of"),
        "on the grounds that": ("重要構文", "〜という理由で", "大学入試重要熟語", "論説文の根拠提示で頻出"),
        "for the sake of": ("前置詞句", "〜のために", "大学入試重要熟語", "目的・利益の対象を示す"),
        "with regard to": ("前置詞句", "〜に関して", "大学入試重要熟語", "同義: regarding, concerning, with respect to"),
        "with respect to": ("前置詞句", "〜に関して, 〜の点では", "大学入試重要熟語", "同義: in terms of, regarding"),
        "in accordance with": ("前置詞句", "〜に従って, 〜と一致して", "大学入試重要熟語", "規約・法則との合致"),
        "in contrast to": ("前置詞句", "〜と対照的に", "大学入試重要熟語", "対比構造の重要マーカー"),
        "on the contrary": ("成句", "それどころか, 逆に", "大学入試重要熟語", "前文の否定内容を受けて逆を強調"),
        "by no means": ("成句", "決して〜ない", "大学入試重要熟語", "部分否定ではない完全否定 (= not at all)"),
        "more often than not": ("成句", "たいてい, しばしば", "大学入試重要熟語", "頻度表現 (= usually, frequently)"),
        "to say nothing of": ("成句", "〜は言うまでもなく", "大学入試重要熟語", "同義: not to mention, let alone"),
        "needless to say": ("成句", "言うまでもなく", "大学入試重要熟語", "文頭・挿入句で頻出"),
        "as a whole": ("成句", "全体として", "大学入試重要熟語", "同義: on the whole"),
    }
    for k, (p, m, d, n) in essential_idioms.items():
        merged[k] = [k, p, m, d, n]

    # 5. Audit & clean all Basic 5,000 + Exam entries, and inject curated basic overrides
    for w, (c_pos, c_mean) in CURATED_BASIC_WORDS.items():
        cur_deck = "日常英会話"
        cur_note = "基礎・中高必須重要語 (総点検済み)"
        if w in merged:
            ex = merged[w]
            if ex[3] in ("大学受験・難関大", "最難関大・医学部") and "【第1義】" in ex[2]:
                # Keep already-rich multi-sense exam definition, just ensure clean POS if broken
                continue
            if ex[4] and "内蔵" not in ex[4] and "EDICT2" not in ex[4]:
                cur_note = ex[4]
        merged[w] = [w, c_pos, c_mean, cur_deck, cur_note]

    # Run automated cleanup pass across all non-EDICT2 entries (and Basic 5000)
    repaired_count = 0
    for k, val in list(merged.items()):
        if k in basic_5000_rank or val[3] in ("日常英会話", "大学受験", "大学受験・難関大", "最難関大・医学部", "総合英和語彙", "TOEIC頻出", "ビジネス英語"):
            old_pos, old_m = val[1], val[2]
            new_pos, new_m = clean_gene95_meaning(k, old_pos, old_m, merged)
            if new_pos != old_pos or new_m != old_m:
                val[1] = new_pos
                val[2] = new_m
                repaired_count += 1
            if k in basic_5000_rank and val[3] not in ("大学受験・難関大", "最難関大・医学部"):
                val[3] = "日常英会話"
                if not val[4] or "EDICT2" in val[4]:
                    val[4] = "基礎・中高〜受験必須語 (頻度上位5000)"
    print(f"Repaired {repaired_count:,} entries (POS fixes, =alias resolution, chopped prefixes).")

    # 6. Assign clean sorting & priority rank (column 6)
    # Tier 1 (1..5000): Audited Basic 5,000 English words
    # Tier 2 (5001..15000): University Entrance Exam vocab (大学受験 / 難関大 / 医学部 / 重要構文)
    # Tier 2B (15001..30000): Curated Idioms & Collocations
    # Tier 3 (30001..90000): TOEIC / Business / General English (総合英和語彙 / 学術・実用英語)
    # Tier 4 (90001+): Specialized / Technical / EDICT2 / Compdic
    EXAM_DECKS = {"大学受験", "大学受験・難関大", "最難関大・医学部", "大学入試重要構文"}
    CURATED_IDIOM_DECKS = {"大学入試重要熟語", "学術・論文連語", "TOEIC・ビジネス連語"}
    PRACTICAL_DECKS = {"TOEIC頻出", "ビジネス英語", "総合英和語彙", "学術・実用英語"}

    ranked_rows = []
    tier_counts = {1: 0, 2: 0, 3: 0, 4: 0, 5: 0}

    for k, val in merged.items():
        w, pos, m, deck, note = val
        if k in basic_5000_rank:
            rank = basic_5000_rank[k]
            tier_counts[1] += 1
        elif deck in EXAM_DECKS:
            # Prefer shorter/more frequent words first within Exam tier
            f_hint = min(en50k_rank.get(k, 40000), google_rank.get(k, 40000))
            rank = 5001 + min(9999, f_hint // 5)
            tier_counts[2] += 1
        elif deck in CURATED_IDIOM_DECKS:
            rank = 15001 + min(14999, len(k) * 100)
            tier_counts[3] += 1
        elif deck in PRACTICAL_DECKS:
            f_hint = min(en50k_rank.get(k, 50000), google_rank.get(k, 50000))
            rank = 30001 + min(59999, f_hint)
            tier_counts[4] += 1
        else:
            rank = 90001 + min(9998, len(k) * 50)
            tier_counts[5] += 1

        ranked_rows.append((rank, k, w, pos, m, deck, note))

    ranked_rows.sort(key=lambda r: (r[0], r[1]))
    print(f"Tier breakdown -> Basic5k:{tier_counts[1]:,}, Exam:{tier_counts[2]:,}, Idioms:{tier_counts[3]:,}, Practical:{tier_counts[4]:,}, Specialized:{tier_counts[5]:,}")
    print(f"TOTAL ENTRIES: {len(ranked_rows):,}")

    # 7. Write GZIP compressed TSV asset
    gz_path = "app/src/main/assets/internal_ej_dict.tsv.gz"
    with gzip.open(gz_path, "wt", encoding="utf-8", compresslevel=9) as f:
        for rank, _k, w, pos, m, deck, note in ranked_rows:
            cw = w.replace("\t", " ").strip()
            cp = pos.replace("\t", " ").strip()
            cm = m.replace("\t", " ").strip()
            cd = deck.replace("\t", " ").strip()
            cn = note.replace("\t", " ").strip()
            if cw and cm:
                f.write(f"{cw}\t{cp}\t{cm}\t{cd}\t{cn}\t{rank}\n")

    size_mb = os.path.getsize(gz_path) / (1024 * 1024)
    print(f"Successfully wrote {len(ranked_rows):,} entries to {gz_path} ({size_mb:.2f} MB)!")


if __name__ == "__main__":
    main()
