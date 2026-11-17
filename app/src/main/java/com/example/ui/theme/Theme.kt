package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ui.settings.AppFontFamily
import com.example.ui.settings.AppThemeColor

private val DarkColorScheme = darkColorScheme(
    primary = StudyBlueSecondary,
    onPrimary = Color.Black,
    primaryContainer = StudyBlueOnContainer,
    onPrimaryContainer = StudyBlueContainer,
    secondary = PosBadgeColor,
    background = DarkStudyCanvasBg,
    surface = DarkStudySurface,
    surfaceVariant = DarkStudySurfaceVariant,
    onBackground = DarkStudyText,
    onSurface = DarkStudyText,
    onSurfaceVariant = DarkStudyTextMuted
)

private val LightColorScheme = lightColorScheme(
    primary = StudyBluePrimary,
    onPrimary = Color.White,
    primaryContainer = StudyBlueContainer,
    onPrimaryContainer = StudyBlueOnContainer,
    secondary = PosBadgeColor,
    background = StudyCanvasBg,
    surface = StudySurfaceWhite,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = StudyTextDark,
    onSurface = StudyTextDark,
    onSurfaceVariant = StudyTextMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use intentional brand theme
    themeColor: AppThemeColor = AppThemeColor.BLUE_LIGHT,
    fontFamily: AppFontFamily = AppFontFamily.SYSTEM,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeColor == AppThemeColor.BLUE_LIGHT -> {
            LightColorScheme
        }
        themeColor.isDark -> {
            darkColorScheme(
                primary = themeColor.primary,
                onPrimary = Color.Black,
                primaryContainer = themeColor.primary.copy(alpha = 0.2f),
                onPrimaryContainer = Color.White,
                secondary = PosBadgeColor,
                background = themeColor.background,
                surface = themeColor.surface,
                surfaceVariant = themeColor.surface.copy(alpha = 0.8f),
                onBackground = DarkStudyText,
                onSurface = DarkStudyText,
                onSurfaceVariant = DarkStudyTextMuted
            )
        }
        else -> {
            lightColorScheme(
                primary = themeColor.primary,
                onPrimary = Color.White,
                primaryContainer = themeColor.primary.copy(alpha = 0.15f),
                onPrimaryContainer = themeColor.primary,
                secondary = PosBadgeColor,
                background = themeColor.background,
                surface = themeColor.surface,
                surfaceVariant = Color(0xFFF1F5F9),
                onBackground = StudyTextDark,
                onSurface = StudyTextDark,
                onSurfaceVariant = StudyTextMuted
            )
        }
    }

    val typography = getAppTypography(fontFamily)
    val selectedFont = getFontFamily(fontFamily)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.copy(fontFamily = selectedFont),
            content = content
        )
    }
}
