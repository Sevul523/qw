package com.example

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.add.AddWordScreen
import com.example.ui.dictionary.DictionaryScreen
import com.example.ui.list.WordListScreen
import com.example.ui.quiz.QuizScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.study.StudyScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudyBluePrimary
import java.util.Locale

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LearningMode

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private val viewModel: MainViewModel by viewModels()
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        textToSpeech = TextToSpeech(this, this)

        setContent {
            val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()
            val fontFamily by viewModel.fontFamily.collectAsStateWithLifecycle()
            MyApplicationTheme(
                darkTheme = false,
                dynamicColor = false,
                themeColor = themeColor,
                fontFamily = fontFamily
            ) {
                MainAppScreen(
                    viewModel = viewModel,
                    onSpeak = { text -> speakWord(text) }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
            }
        }
    }

    private fun speakWord(word: String) {
        if (viewModel.learningMode.value.isClassical) {
            textToSpeech?.stop()
            return
        }
        if (viewModel.ttsEnabled.value && isTtsReady && word.isNotBlank()) {
            textToSpeech?.setLanguage(Locale.US)
            textToSpeech?.setSpeechRate(viewModel.speechRate.value)
            textToSpeech?.setPitch(viewModel.speechPitch.value)
            textToSpeech?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "WordTTS_${System.currentTimeMillis()}")
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        super.onDestroy()
    }
}

@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    onSpeak: (String) -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentMode by viewModel.learningMode.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (currentMode.isClassical) "📜 古語単語帳" else "🔤 英単語帳",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Mode segmented switch
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Row(modifier = Modifier.padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
                            LearningMode.entries.forEach { mode ->
                                val isSelected = currentMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                        )
                                        .clickable { viewModel.setLearningMode(mode) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                        .testTag("mode_toggle_${mode.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = mode.icon, fontSize = 12.sp, maxLines = 1, softWrap = false)
                                        Text(
                                            text = mode.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                NavigationBarItem(
                    selected = currentTab == AppTab.STUDY,
                    onClick = { viewModel.switchTab(AppTab.STUDY) },
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "学習") },
                    label = { Text("学習", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = StudyBluePrimary),
                    modifier = Modifier.testTag("tab_study")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.QUIZ,
                    onClick = { viewModel.switchTab(AppTab.QUIZ) },
                    icon = { Icon(Icons.Default.Quiz, contentDescription = "テスト") },
                    label = { Text("テスト", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = StudyBluePrimary),
                    modifier = Modifier.testTag("tab_quiz")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.LIST,
                    onClick = { viewModel.switchTab(AppTab.LIST) },
                    icon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "一覧") },
                    label = { Text("一覧", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = StudyBluePrimary),
                    modifier = Modifier.testTag("tab_list")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.ADD,
                    onClick = { viewModel.switchTab(AppTab.ADD) },
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = "追加") },
                    label = { Text("追加", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = StudyBluePrimary),
                    modifier = Modifier.testTag("tab_add")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.DICTIONARY,
                    onClick = { viewModel.switchTab(AppTab.DICTIONARY) },
                    icon = { Icon(Icons.Default.AutoStories, contentDescription = "辞書") },
                    label = { Text("辞書", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = StudyBluePrimary),
                    modifier = Modifier.testTag("tab_dictionary")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.SETTINGS,
                    onClick = { viewModel.switchTab(AppTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "設定") },
                    label = { Text("設定", fontSize = 10.sp, maxLines = 1, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = StudyBluePrimary),
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentTab,
            label = "TabTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { tab ->
            when (tab) {
                AppTab.STUDY -> StudyScreen(
                    viewModel = viewModel,
                    onSpeak = onSpeak
                )
                AppTab.QUIZ -> QuizScreen(
                    viewModel = viewModel,
                    onSpeak = onSpeak,
                    snackbarHostState = snackbarHostState
                )
                AppTab.LIST -> WordListScreen(
                    viewModel = viewModel,
                    onSpeak = onSpeak
                )
                AppTab.ADD -> AddWordScreen(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState
                )
                AppTab.DICTIONARY -> DictionaryScreen(
                    viewModel = viewModel,
                    onSpeak = onSpeak,
                    snackbarHostState = snackbarHostState
                )
                AppTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onSpeakTest = onSpeak,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}
