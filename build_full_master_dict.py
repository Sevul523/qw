import gzip
import re
import ssl
import sys
import urllib.request

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# 1. Load existing base entries (from app/src/main/assets/internal_ej_dict.tsv)
existing = {}
with open("app/src/main/assets/internal_ej_dict.tsv", "r", encoding="utf-8") as f:
    for line in f:
        parts = line.strip().split("\t")
        if len(parts) >= 3:
            k = parts[0].lower().strip()
            pos = parts[1]
            meanings = parts[2]
            deck = parts[3] if len(parts) > 3 else "日常英会話"
            note = parts[4] if len(parts) > 4 else ""
            existing[k] = [parts[0].strip(), pos, meanings, deck, note]
print(f"Loaded base entries: {len(existing):,}")

# 2. Download and parse Compdic
comp_entries = {}
try:
    req = urllib.request.Request("https://ftp.edrdg.org/pub/Nihongo/compdic.gz", headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, context=ctx) as r:
        data = gzip.decompress(r.read())
    for line in data.decode("euc-jp", errors="replace").splitlines():
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
            if not clean or len(clean) < 2 or len(clean) > 80:
                continue
            if re.match(r"^[\d\W]+$", clean):
                continue
            k = clean.lower()
            if k not in comp_entries:
                pos = "熟語・連語" if (" " in clean or "-" in clean) else "IT用語"
                comp_entries[k] = [clean, pos, head, "IT・プログラミング", "情報処理・IT専門用語"]
    print(f"Parsed Compdic: {len(comp_entries):,} unique terms")
except Exception as e:
    print(f"Compdic error: {e}")

# 3. Download and parse Edict2
req = urllib.request.Request("https://ftp.edrdg.org/pub/Nihongo/edict2.gz", headers={"User-Agent": "Mozilla/5.0"})
with urllib.request.urlopen(req, context=ctx) as r:
    data = gzip.decompress(r.read())
lines = data.decode("euc-jp", errors="replace").splitlines()
print(f"Edict2 total lines: {len(lines):,}")

MED_PATTERN = re.compile(r"\b(med|anat|bio|pharm|chem|syndrome|disease|disorder|infection|artery|vein|cell|organ|bacteria|virus|muscle|bone|gland|cancer|tumor|hormone|cortex|neuron|peptide|protein|acid|enzyme|therapy|symptom|lesion|vascular|cardiac|cerebral|renal|pulmonary)\b", re.I)
BIZ_PATTERN = re.compile(r"\b(econ|fin|bus|trade|market|stock|tax|bond|audit|dividend|deficit|profit|asset|liability|depreciation|collateral|equity|monetary|fiscal)\b", re.I)
COMP_PATTERN = re.compile(r"\b(comp|comput|data|byte|server|packet|router|cache|pixel|algorithm|protocol|database|software|hardware|network|interface|kernel|compiler)\b", re.I)

edict_new = {}
for line in lines[1:]:
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
            if not sub_key or len(sub_key) < 2 or len(sub_key) > 80:
                continue
            if re.match(r"^[\d\W]+$", sub_key):
                continue
            k = sub_key.lower()
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
                deck = "日常英会話"
            note = f"EDICT2大辞典収録 ({deck})"
            if k in edict_new:
                cur_meanings = edict_new[k][2]
                if kanji_reading not in cur_meanings and len(cur_meanings.split(" / ")) < 4:
                    edict_new[k][2] = f"{cur_meanings} / {full_jp}"
            else:
                edict_new[k] = [sub_key, pos, full_jp, deck, note]
print(f"Parsed Edict2: {len(edict_new):,} entries")

# 4. Merge: existing -> clean GENE95 '~' aliases -> compdic -> edict_new
merged = dict(existing)

# Clean trailing '~' / '~ing' / '...' from multi-word idioms so OCR matches plain text phrases
alias_added = 0
for k, val in list(existing.items()):
    if k.endswith(" ~") or k.endswith(" ~ing") or k.endswith(" ..."):
        cleaned = re.sub(r"\s+(?:~ing|~|\.\.\.)$", "", val[0]).strip()
        ck = cleaned.lower()
        if " " in ck and "~" not in ck and re.match(r"^[a-z][a-z\s\-']+[a-z]$", ck):
            if ck not in merged:
                pos = val[1] if val[1] in ("句動詞", "熟語・連語", "連語", "前置詞句", "成句") else "熟語・連語"
                merged[ck] = [cleaned, pos, val[2], val[3], val[4]]
                alias_added += 1
print(f"Created {alias_added:,} clean idiom aliases from '~' entries.")

for k, val in comp_entries.items():
    if k not in merged:
        merged[k] = val
for k, val in edict_new.items():
    if k not in merged:
        merged[k] = val
print(f"Merged total before idioms & exam enrichment: {len(merged):,}")

# 5. Merge curated idioms from both idioms_5000.tsv and all_idioms.tsv
idiom_count = 0
for idiom_file in ["idioms_5000.tsv", "all_idioms.tsv"]:
    try:
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
                    idiom_count += 1
    except FileNotFoundError:
        pass
print(f"Injected {idiom_count:,} curated idioms. Total now: {len(merged):,}")

# 5.5 Guarantee core university entrance exam idioms & collocations
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

# 6. Apply all entrance exam polysemy, thematic words, syntax and confusing pair notes if available
import os
if os.path.exists("/tmp/enrich_entrance_exam_dict.py"):
    enrich_code = open("/tmp/enrich_entrance_exam_dict.py", "r", encoding="utf-8").read()
    env = {}
    exec(enrich_code, env)
    polysemous_words = env.get("polysemous_words", {})
    syntax_phrases = env.get("syntax_phrases", {})
    thematic_words = env.get("thematic_words", {})
    confusing_notes = env.get("confusing_notes", {})

    enriched = 0
    for k, (pos, meanings, deck, note) in polysemous_words.items():
        merged[k] = [k, pos, meanings, deck, note]
        enriched += 1

    for k, (pos, meanings, deck, note) in syntax_phrases.items():
        merged[k] = [k, pos, meanings, deck, note]
        enriched += 1

    for k, (pos, meanings, deck, note) in thematic_words.items():
        merged[k] = [k, pos, meanings, deck, note]
        enriched += 1

    for k, note in confusing_notes.items():
        if k in merged:
            cur_note = merged[k][4]
            if note not in cur_note:
                merged[k][4] = f"{note} / {cur_note}" if cur_note else note

    print(f"Applied {enriched} entrance exam polysemy & syntax rules.")

print(f"FINAL TOTAL DICTIONARY ENTRIES: {len(merged):,}")

# 7. Write to app/src/main/assets/internal_ej_dict.tsv
out_path = "app/src/main/assets/internal_ej_dict.tsv"
print(f"Writing to {out_path}...")
sorted_keys = sorted(merged.keys())
with open(out_path, "w", encoding="utf-8") as f:
    for k in sorted_keys:
        row = merged[k]
        w = row[0].replace("\t", " ").strip()
        p = row[1].replace("\t", " ").strip()
        m = row[2].replace("\t", " ").strip()
        d = row[3].replace("\t", " ").strip()
        n = row[4].replace("\t", " ").strip()
        f.write(f"{w}\t{p}\t{m}\t{d}\t{n}\n")

print(f"Successfully wrote {len(sorted_keys):,} entries to {out_path}!")
