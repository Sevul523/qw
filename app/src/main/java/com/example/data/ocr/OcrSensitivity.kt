package com.example.data.ocr

/**
 * Controls optical character detection sensitivity ("どこまでを文字とみなすか"):
 * - Does NOT filter words by character count (3/4/5 letters).
 * - Adjusts ML Kit character/symbol confidence thresholds, minimum relative stroke size,
 *   image contrast/shadow enhancement passes, and ambiguous stroke/digit-to-letter rescue.
 */
enum class OcrSensitivity(
    val id: String,
    val label: String,
    val title: String,
    val shortLabel: String,
    val badgeLabel: String,
    val shortHint: String,
    val description: String,
    val classicalDescription: String,
    val minElementConfidence: Float,
    val minSymbolConfidence: Float,
    val minRelativeSizeRatio: Float,
    val runColorInkPass: Boolean,
    val runFaintShadowPass: Boolean
) {
    HIGH(
        id = "HIGH",
        label = "高",
        title = "高（薄文字・かすれも検出）",
        shortLabel = "高（薄文字も）",
        badgeLabel = "薄い文字・赤文字・影まで文字判定",
        shortHint = "薄い印字・赤シート文字・影やかすれ文字まで幅広く文字とみなします",
        description = "薄い印字・赤シート対応文字・ページの影やかすれがある文字・小さな文字まで幅広く文字とみなして検出します（文字数の長短に関わらず辞書・Weblioの実在語を抽出）。",
        classicalDescription = "薄い印字・赤シート対応文字・かすれ文字・小さな文字や字間が空いた見出し語まで幅広く文字とみなして古語を検出します。",
        minElementConfidence = 0.14f,
        minSymbolConfidence = 0.10f,
        minRelativeSizeRatio = 0.18f,
        runColorInkPass = true,
        runFaintShadowPass = true
    ),
    MEDIUM(
        id = "MEDIUM",
        label = "中",
        title = "中（標準）",
        shortLabel = "中（標準）",
        badgeLabel = "標準の文字判定（おすすめ）",
        shortHint = "通常の印字・赤シート文字を認識し、裏写りや汚れの誤検出を抑えます",
        description = "通常の印字や赤シート対応文字を標準的な基準で文字とみなし、紙面の薄い裏写りや記号ノイズの誤検出をバランスよく抑えます。",
        classicalDescription = "通常の印字や赤シート対応文字を標準的な基準で文字とみなし、細かなルビ断片や紙面汚れの誤検出を抑えて古語を抽出します。",
        minElementConfidence = 0.38f,
        minSymbolConfidence = 0.28f,
        minRelativeSizeRatio = 0.32f,
        runColorInkPass = true,
        runFaintShadowPass = false
    ),
    LOW(
        id = "LOW",
        label = "低",
        title = "低（くっきりした文字のみ）",
        shortLabel = "低（鮮明のみ）",
        badgeLabel = "鮮明な文字のみ厳格判定",
        shortHint = "輪郭が鮮明な文字のみを文字とみなし、枠線・手書き・かすれを除外します",
        description = "輪郭がはっきりした高確信度の文字のみを文字とみなし、裏写り・手書きメモ・チェック枠・不鮮明なかすれ文字を厳しく除外します（短い単語も鮮明なら抽出されます）。",
        classicalDescription = "輪郭がはっきりした高確信度の文字のみを文字とみなし、裏写り・傍線・チェック枠・小書き文字などの誤認識を厳しく除外します。",
        minElementConfidence = 0.62f,
        minSymbolConfidence = 0.50f,
        minRelativeSizeRatio = 0.48f,
        runColorInkPass = false,
        runFaintShadowPass = false
    );

    companion object {
        fun fromId(id: String?): OcrSensitivity =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: HIGH
    }
}
