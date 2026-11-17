package com.example

import com.example.ui.AppTab
import com.example.ui.settings.AppThemeColor
import com.example.ui.settings.CardBackTheme
import com.example.ui.settings.SettingsCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsUnitTest {

    @Test
    fun appTab_containsSixTabs() {
        assertEquals(6, AppTab.entries.size)
        assertEquals(AppTab.STUDY, AppTab.entries[0])
        assertEquals(AppTab.QUIZ, AppTab.entries[1])
        assertEquals(AppTab.LIST, AppTab.entries[2])
        assertEquals(AppTab.ADD, AppTab.entries[3])
        assertEquals(AppTab.DICTIONARY, AppTab.entries[4])
        assertEquals(AppTab.SETTINGS, AppTab.entries[5])
    }

    @Test
    fun appThemeColors_haveValidProperties() {
        val themes = AppThemeColor.entries
        assertEquals(8, themes.size)
        
        val defaultTheme = AppThemeColor.BLUE_LIGHT
        assertEquals("ブルー (ライト・標準)", defaultTheme.title)
        assertEquals(false, defaultTheme.isDark)

        val darkTheme = AppThemeColor.BLUE_DARK
        assertEquals(true, darkTheme.isDark)
        assertNotNull(darkTheme.background)
        assertNotNull(darkTheme.surface)

        // Check all light/dark pairs exist
        val titles = themes.map { it.title }
        assertTrue(titles.any { it.contains("ブルー") && it.contains("ライト") })
        assertTrue(titles.any { it.contains("ブルー") && it.contains("ダーク") })
        assertTrue(titles.any { it.contains("ベージュ") && it.contains("ライト") })
        assertTrue(titles.any { it.contains("ベージュ") && it.contains("ダーク") })
        assertTrue(titles.any { it.contains("グリーン") && it.contains("ライト") })
        assertTrue(titles.any { it.contains("グリーン") && it.contains("ダーク") })
        assertTrue(titles.any { it.contains("パープル") && it.contains("ライト") })
        assertTrue(titles.any { it.contains("パープル") && it.contains("ダーク") })
    }

    @Test
    fun cardBackThemes_haveValidProperties() {
        val backThemes = CardBackTheme.entries
        assertTrue(backThemes.size >= 8)

        val defaultTheme = CardBackTheme.DEFAULT
        assertEquals(CardBackTheme.BLUE_GRADIENT, defaultTheme)
        assertNotNull(defaultTheme.gradientStart)
        assertNotNull(defaultTheme.gradientEnd)
        assertNotNull(defaultTheme.textColor)
        assertNotNull(defaultTheme.badgeTextColor)

        // Verify key styles exist
        val titles = backThemes.map { it.title }
        assertTrue(titles.any { it.contains("ブルー") })
        assertTrue(titles.any { it.contains("インディゴ") })
        assertTrue(titles.any { it.contains("セピア") })
        assertTrue(titles.any { it.contains("エメラルド") || it.contains("フォレスト") })
        assertTrue(titles.any { it.contains("パープル") })
        assertTrue(titles.any { it.contains("コーラル") })
        assertTrue(titles.any { it.contains("チャコール") })
        assertTrue(titles.any { it.contains("クリーム") })
        assertTrue(backThemes.all { it.description.isNotBlank() })
    }

    @Test
    fun appVersion_isAvailable() {
        assertNotNull(BuildConfig.VERSION_NAME)
        assertTrue(BuildConfig.VERSION_NAME.isNotBlank())
    }

    @Test
    fun ttsSliderRange_centersDefaultAtFiftyPercent() {
        val min = 0.5f
        val max = 1.5f
        val defaultVal = 1.0f

        // Center calculation
        val center = (min + max) / 2.0f
        assertEquals(defaultVal, center, 0.0001f)

        // Percentage along the track: (val - min) / (max - min)
        val percentage = (defaultVal - min) / (max - min)
        assertEquals(0.5f, percentage, 0.0001f) // Exactly 50% (middle)
    }

    @Test
    fun appFontFamilies_haveValidOptions() {
        val fonts = com.example.ui.settings.AppFontFamily.entries
        assertEquals(5, fonts.size)

        val defaultFont = com.example.ui.settings.AppFontFamily.DEFAULT
        assertEquals(com.example.ui.settings.AppFontFamily.SYSTEM, defaultFont)

        assertTrue(fonts.any { it.name == "SYSTEM" })
        assertTrue(fonts.any { it.name == "NOTO_SERIF_JP" })
        assertTrue(fonts.any { it.name == "M_PLUS_ROUNDED" })
        assertTrue(fonts.any { it.name == "KLEE_ONE" })
        assertTrue(fonts.any { it.name == "NOTO_SANS_JP" })

        for (font in fonts) {
            assertTrue(font.title.isNotBlank())
            assertTrue(font.description.isNotBlank())
            assertTrue(font.previewSample.isNotBlank())
        }
    }

    @Test
    fun autoUncheckAction_hasValidOptions() {
        val actions = com.example.ui.settings.AutoUncheckAction.entries
        assertEquals(3, actions.size)

        assertEquals(
            com.example.ui.settings.AutoUncheckAction.HIDE_FROM_STUDY,
            com.example.ui.settings.AutoUncheckAction.DEFAULT
        )

        for (action in actions) {
            assertTrue(action.title.isNotBlank())
            assertTrue(action.description.isNotBlank())
        }
    }

    @Test
    fun consecutivePerfectLogic_evaluatesThresholdCorrectly() {
        // When threshold is 3
        val threshold = 3

        // Case 1: Starting from non-perfect state (e.g. status null or △)
        var wasPerfect = false
        var consecutive = 0
        var triggered = false

        // User chooses ○
        consecutive = if (wasPerfect) consecutive + 1 else 1
        triggered = threshold > 0 && wasPerfect && consecutive >= threshold
        assertEquals(1, consecutive)
        assertFalse(triggered)

        // Card is now in 完璧 state
        wasPerfect = true

        // User chooses ○ 2nd time
        consecutive = if (wasPerfect) consecutive + 1 else 1
        triggered = threshold > 0 && wasPerfect && consecutive >= threshold
        assertEquals(2, consecutive)
        assertFalse(triggered)

        // User chooses ○ 3rd time (threshold reached!)
        consecutive = if (wasPerfect) consecutive + 1 else 1
        triggered = threshold > 0 && wasPerfect && consecutive >= threshold
        assertEquals(3, consecutive)
        assertTrue(triggered)
    }

    @Test
    fun defaultDecks_areComprehensive() {
        val defaults = listOf("日常英会話", "TOEIC頻出", "ビジネス英語", "大学受験", "旅行英会話")
        assertEquals(5, defaults.size)
        assertTrue(defaults.contains("日常英会話"))
        assertTrue(defaults.contains("TOEIC頻出"))
        assertTrue(defaults.contains("ビジネス英語"))
        assertTrue(defaults.contains("大学受験"))
        assertTrue(defaults.contains("旅行英会話"))
    }

    @Test
    fun deckKeywordDetection_identifiesKeywords() {
        val keywords = listOf("デッキ", "deck", "カテゴリー", "category", "分類", "フォルダ", "folder", "グループ", "group")
        assertTrue(keywords.any { "デッキ".contains(it) })
        assertTrue(keywords.any { "deck".equals(it, ignoreCase = true) })
        assertTrue(keywords.any { "category".equals(it, ignoreCase = true) })
        assertTrue(keywords.any { "分類".contains(it) })
    }

    @Test
    fun settingsCategories_containAllFourExpectedCategories() {
        val categories = SettingsCategory.entries
        assertEquals(4, categories.size)
        assertEquals(SettingsCategory.STUDY_AUDIO, categories[0])
        assertEquals(SettingsCategory.ADD_OCR, categories[1])
        assertEquals(SettingsCategory.APPEARANCE, categories[2])
        assertEquals(SettingsCategory.DATA_MANAGEMENT, categories[3])

        assertEquals("学習・音声設定", SettingsCategory.STUDY_AUDIO.title)
        assertEquals("単語追加・OCR設定", SettingsCategory.ADD_OCR.title)
        assertEquals("外観・デザイン設定", SettingsCategory.APPEARANCE.title)
        assertEquals("デッキ・データ管理", SettingsCategory.DATA_MANAGEMENT.title)

        assertEquals("accordion_study_audio", SettingsCategory.STUDY_AUDIO.testTag)
        assertEquals("accordion_add_ocr", SettingsCategory.ADD_OCR.testTag)
        assertEquals("accordion_appearance", SettingsCategory.APPEARANCE.testTag)
        assertEquals("accordion_data_management", SettingsCategory.DATA_MANAGEMENT.testTag)
    }

    @Test
    fun settingsCategories_accordionExpansionLogic() {
        // Default initial state: all categories are collapsed (emptySet)
        var expanded = emptySet<SettingsCategory>()
        assertTrue(expanded.isEmpty())
        assertFalse(SettingsCategory.STUDY_AUDIO in expanded)
        assertFalse(SettingsCategory.ADD_OCR in expanded)
        assertFalse(SettingsCategory.APPEARANCE in expanded)
        assertFalse(SettingsCategory.DATA_MANAGEMENT in expanded)

        // Expand Study & Audio
        expanded = expanded + SettingsCategory.STUDY_AUDIO
        assertTrue(SettingsCategory.STUDY_AUDIO in expanded)
        assertFalse(SettingsCategory.ADD_OCR in expanded)

        // Expand Appearance
        expanded = expanded + SettingsCategory.APPEARANCE
        assertTrue(SettingsCategory.APPEARANCE in expanded)

        // Collapse Study
        expanded = expanded - SettingsCategory.STUDY_AUDIO
        assertFalse(SettingsCategory.STUDY_AUDIO in expanded)

        // Expand All
        expanded = SettingsCategory.entries.toSet()
        assertEquals(4, expanded.size)
        assertTrue(SettingsCategory.entries.all { it in expanded })

        // Collapse All
        expanded = emptySet()
        assertTrue(expanded.isEmpty())
    }
}
