package com.example.ui.settings

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.DarkStudyCanvasBg
import com.example.ui.theme.DarkStudySurface
import com.example.ui.theme.DarkStudySurfaceVariant
import com.example.ui.theme.DarkStudyText
import com.example.ui.theme.PosBadgeColor
import com.example.ui.theme.PosBadgeContainer
import com.example.ui.theme.StudyBluePrimary
import com.example.ui.theme.StudyBlueSecondary
import com.example.ui.theme.StudyCanvasBg
import com.example.ui.theme.StudySurfaceWhite
import com.example.ui.theme.StudyTextDark

enum class AppThemeColor(
    val title: String,
    val description: String,
    val primary: Color,
    val background: Color,
    val surface: Color,
    val isDark: Boolean
) {
    BLUE_LIGHT(
        "ブルー (ライト・標準)",
        "清潔感のある純白の背景と鮮やかなブルー",
        StudyBluePrimary,
        StudyCanvasBg,
        StudySurfaceWhite,
        false
    ),
    BLUE_DARK(
        "ブルー (ダーク)",
        "夜間学習に適した深いネイビーと鮮やかなブルー",
        StudyBlueSecondary,
        DarkStudyCanvasBg,
        DarkStudySurface,
        true
    ),
    BEIGE_LIGHT(
        "ベージュ (ライト)",
        "紙の本のように目に優しい温かみのある生成り色",
        Color(0xFF8D6E63),
        Color(0xFFFDFBF7),
        Color(0xFFFFFFFF),
        false
    ),
    BEIGE_DARK(
        "ベージュ (ダーク)",
        "落ち着いたセピア調のダークテーマ",
        Color(0xFFD7CCC8),
        Color(0xFF2D2623),
        Color(0xFF3E3430),
        true
    ),
    GREEN_LIGHT(
        "グリーン (ライト)",
        "集中力を高める落ち着いた淡いミントグリーン",
        Color(0xFF059669),
        Color(0xFFF0FDF4),
        Color(0xFFFFFFFF),
        false
    ),
    GREEN_DARK(
        "グリーン (ダーク)",
        "目に優しい深緑のダークテーマ",
        Color(0xFF34D399),
        Color(0xFF064E3B),
        Color(0xFF065F46),
        true
    ),
    PURPLE_LIGHT(
        "パープル (ライト)",
        "優しく上品なリラックスパープル",
        Color(0xFF7C3AED),
        Color(0xFFFAF5FF),
        Color(0xFFFFFFFF),
        false
    ),
    PURPLE_DARK(
        "パープル (ダーク)",
        "神秘的で目に優しいダークパープル",
        Color(0xFFA78BFA),
        Color(0xFF2E1065),
        Color(0xFF3B0764),
        true
    );

    companion object {
        val DEFAULT_LIGHT = BLUE_LIGHT
        val AMOLED_BLACK = BLUE_DARK
    }
}

enum class CardBackTheme(
    val title: String,
    val description: String,
    val gradientStart: Color,
    val gradientEnd: Color,
    val textColor: Color,
    val badgeBg: Color,
    val badgeTextColor: Color,
    val isLightBack: Boolean = false
) {
    BLUE_GRADIENT(
        "ブルー グラデーション",
        "知性と集中力を高める定番のブルースタイル",
        Color(0xFF1E3A8A),
        Color(0xFF3B82F6),
        Color.White,
        Color.White.copy(alpha = 0.2f),
        Color.White,
        false
    ),
    INDIGO_GRADIENT(
        "インディゴ",
        "落ち着いてじっくり取り組める深みのある藍色",
        Color(0xFF312E81),
        Color(0xFF4F46E5),
        Color.White,
        Color.White.copy(alpha = 0.2f),
        Color.White,
        false
    ),
    SEPIA(
        "セピア",
        "古書や洋書を読んでいるような温かいクラシック調",
        Color(0xFF451A03),
        Color(0xFF78350F),
        Color(0xFFFEF3C7),
        Color(0xFFFEF3C7).copy(alpha = 0.2f),
        Color(0xFFFEF3C7),
        false
    ),
    EMERALD(
        "エメラルド",
        "目の疲れを和らげリラックスして覚えられる深緑",
        Color(0xFF064E3B),
        Color(0xFF059669),
        Color.White,
        Color.White.copy(alpha = 0.2f),
        Color.White,
        false
    ),
    PURPLE_GRADIENT(
        "パープル",
        "上品で洗練されたクリエイティブな紫のグラデーション",
        Color(0xFF581C87),
        Color(0xFF9333EA),
        Color.White,
        Color.White.copy(alpha = 0.2f),
        Color.White,
        false
    ),
    CORAL(
        "コーラル",
        "やる気を刺激しテンポよく学習が進む華やかな赤・ピンク",
        Color(0xFF991B1B),
        Color(0xFFE11D48),
        Color.White,
        Color.White.copy(alpha = 0.2f),
        Color.White,
        false
    ),
    CHARCOAL(
        "チャコール",
        "コントラストが高く文字が鮮明に際立つシックなダーク",
        Color(0xFF111827),
        Color(0xFF374151),
        Color.White,
        Color.White.copy(alpha = 0.2f),
        Color.White,
        false
    ),
    CREAM(
        "クリーム (ライト)",
        "明るいカード面で柔らかな光を届けるライトスタイル",
        Color(0xFFFFFBEB),
        Color(0xFFFEF3C7),
        Color(0xFF78350F),
        Color(0xFF78350F).copy(alpha = 0.15f),
        Color(0xFF78350F),
        true
    );

    companion object {
        val DEFAULT = BLUE_GRADIENT
    }
}

enum class AppFontFamily(
    val title: String,
    val description: String,
    val previewSample: String = "英単語・漢字・ひらがな English 123"
) {
    SYSTEM(
        "システム標準",
        "端末の標準フォント（漢字・平仮名・英数字対応）",
        "英単語・漢字・ひらがな English 123"
    ),
    NOTO_SERIF_JP(
        "明朝体 (Noto Serif JP)",
        "漢字・平仮名・英字すべてが上品で格調高い明朝体",
        "英単語・漢字・ひらがな English 123"
    ),
    M_PLUS_ROUNDED(
        "丸ゴシック (M PLUS Rounded)",
        "漢字・平仮名・英字すべてが丸みを帯びて親しみやすい",
        "英単語・漢字・ひらがな English 123"
    ),
    KLEE_ONE(
        "教科書・手書き風 (Klee One)",
        "硬筆の手書きや教科書のように端正で美しい文字",
        "英単語・漢字・ひらがな English 123"
    ),
    NOTO_SANS_JP(
        "すっきり角ゴシック (Noto Sans JP)",
        "高い視認性で現代的かつ均整のとれたモダンゴシック",
        "英単語・漢字・ひらがな English 123"
    );

    companion object {
        val DEFAULT = SYSTEM
    }
}

enum class AutoUncheckAction(
    val title: String,
    val description: String
) {
    HIDE_FROM_STUDY(
        "学習対象から外す（非表示）",
        "習得済みとして暗記カードから除外します（単語一覧から確認可能）"
    ),
    RESET_STATUS(
        "完璧マークを外す（未評価に戻す）",
        "完璧状態のマークを解除し、未評価の状態に戻します"
    ),
    BOTH(
        "両方（非表示にし、未評価に戻す）",
        "学習カードから除外し、ステータスも未評価にリセットします"
    );

    companion object {
        val DEFAULT = HIDE_FROM_STUDY
    }
}

enum class DictionaryPriority(
    val id: String,
    val title: String,
    val badgeLabel: String,
    val description: String
) {
    INTERNAL_FIRST(
        id = "INTERNAL_FIRST",
        title = "内蔵辞書を優先（推奨）",
        badgeLabel = "超高速・オフライン",
        description = "内蔵の英単語(1万語)・古語(500語)辞書を最優先で検索。見つからない場合のみWeblio辞典へ自動フォールバックします。"
    ),
    WEBLIO_FIRST(
        id = "WEBLIO_FIRST",
        title = "Weblio辞典を優先",
        badgeLabel = "オンライン詳細",
        description = "Weblioオンライン辞典を最優先で検索。オフライン時や検索失敗時に内蔵辞書で自動補完します。"
    );

    companion object {
        val DEFAULT = INTERNAL_FIRST

        fun fromId(id: String?): DictionaryPriority {
            return entries.firstOrNull { it.id == id } ?: DEFAULT
        }
    }
}
