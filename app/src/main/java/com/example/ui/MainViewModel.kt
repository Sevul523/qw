package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CustomDictionaryOverride
import com.example.data.local.CustomDictionaryOverrideDao
import com.example.data.local.FlashcardEntity
import com.example.data.model.ClassicalPosNormalizer
import com.example.data.model.ClassicalWordKnowledgeBase
import com.example.data.model.Definition
import com.example.data.model.PartsOfSpeech
import com.example.data.ocr.OcrExtractor
import com.example.data.ocr.OcrSensitivity
import com.example.data.repository.DuplicateStrategy
import com.example.data.repository.FlashcardRepository
import com.example.data.repository.ImportResult
import com.example.ui.components.FilterState
import com.example.ui.quiz.QuizQuestion
import com.example.ui.quiz.QuizQuestionResult
import com.example.ui.quiz.QuizState
import com.example.ui.quiz.QuizType
import com.example.ui.settings.AppFontFamily
import com.example.ui.settings.AppThemeColor
import com.example.ui.settings.AutoUncheckAction
import com.example.ui.settings.CardBackTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import com.example.data.service.WeblioDictionaryService
import com.example.data.service.WeblioLookupResult
import com.example.data.service.WeblioVerifyStatus
import com.example.data.service.InternalDictionaryService
import com.example.ui.settings.DictionaryPriority
import java.io.InputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        internal fun resolvePrefKey(baseKey: String, mode: com.example.data.model.LearningMode): String {
            return if (mode.isClassical) "${baseKey}_classical" else baseKey
        }
    }

    private val db = AppDatabase.getInstance(application)
    private val repository = FlashcardRepository(db.flashcardDao())
    private val customDictionaryOverrideDao: CustomDictionaryOverrideDao = db.customDictionaryOverrideDao()
    private val prefs: SharedPreferences =
        application.getSharedPreferences("wordbook_prefs", Context.MODE_PRIVATE)

    val customDictionaryOverrides: StateFlow<List<CustomDictionaryOverride>> =
        customDictionaryOverrideDao.getAllOverridesFlow()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Learning Subject Mode (英単語 ⇄ 古語)
    val defaultEnglishDecks = listOf("日常英会話", "TOEIC頻出", "ビジネス英語", "大学受験", "旅行英会話")
    val defaultClassicalDecks = listOf("共通テスト最重要動詞", "最頻出形容詞・形容動詞", "入試必出の多義語", "古今異義語・注意語", "重要助動詞・敬語")

    private val _learningMode = MutableStateFlow(
        try {
            val saved = prefs.getString("current_learning_mode", com.example.data.model.LearningMode.ENGLISH.name)
                ?: com.example.data.model.LearningMode.ENGLISH.name
            com.example.data.model.LearningMode.valueOf(saved)
        } catch (e: Exception) {
            com.example.data.model.LearningMode.ENGLISH
        }
    )
    val learningMode: StateFlow<com.example.data.model.LearningMode> = _learningMode.asStateFlow()

    private fun modePrefKey(
        baseKey: String,
        mode: com.example.data.model.LearningMode = _learningMode.value
    ): String = resolvePrefKey(baseKey, mode)

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            repository.normalizeExistingClassicalCards()
            if (_learningMode.value == com.example.data.model.LearningMode.CLASSICAL_JAPANESE) {
                repository.seedClassicalJapaneseDataIfEmpty()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            InternalDictionaryService.ensureLoaded(application.applicationContext)
        }
        viewModelScope.launch {
            customDictionaryOverrideDao.getAllOverridesFlow().collect { overrides ->
                InternalDictionaryService.applyOverrides(overrides)
            }
        }
    }

    fun setLearningMode(mode: com.example.data.model.LearningMode) {
        if (_learningMode.value == mode) return
        _learningMode.value = mode
        prefs.edit().putString("current_learning_mode", mode.name).apply()
        if (mode == com.example.data.model.LearningMode.CLASSICAL_JAPANESE) {
            viewModelScope.launch {
                repository.seedClassicalJapaneseDataIfEmpty()
            }
        }
        reloadModeSettings(mode)
        clearAddForm()
        _addDeck.value = mode.defaultDeck
        _bulkInput.value = ""
        _currentIndex.value = 0
        _isFlipped.value = false
        _selectedListDeck.value = null
        _selectedStudyDecks.value = emptySet()
        _srsDueOnly.value = false
        _filterState.value = FilterState()
        resetListFilters()
        resetQuiz()
    }

    private fun reloadModeSettings(mode: com.example.data.model.LearningMode) {
        _deletedDefaultDecks.value = loadDeletedDefaultDecks(mode)
        _customDecks.value = loadCustomDecks(mode)
        _isShuffle.value = prefs.getBoolean(modePrefKey("is_shuffle", mode), false)
        _shuffledCardIds.value = emptyList()
        _weblioAutoFillEnabled.value = prefs.getBoolean(modePrefKey("weblio_autofill_enabled", mode), true)
        _weblioAutoAddNoteLevelEnabled.value = if (mode.isClassical) {
            false
        } else {
            prefs.getBoolean("weblio_auto_add_note_level_enabled", true)
        }
        _weblioAutoAddConjugationEnabled.value = if (mode.isClassical) {
            prefs.getBoolean(modePrefKey("weblio_auto_add_conjugation_enabled", mode), true)
        } else {
            false
        }
        _ocrExcludeRegisteredWords.value = prefs.getBoolean(modePrefKey("ocr_exclude_registered_words", mode), true)
        _ocrAutoLookupMeaningEnabled.value = prefs.getBoolean(modePrefKey("ocr_auto_lookup_meaning_enabled", mode), true)
        _ocrSensitivity.value = OcrSensitivity.fromId(
            prefs.getString(modePrefKey("ocr_sensitivity", mode), OcrSensitivity.HIGH.id)
        )
        _themeColor.value = AppThemeColor.entries.find {
            it.name == prefs.getString(modePrefKey("theme_color", mode), AppThemeColor.BLUE_LIGHT.name)
        } ?: AppThemeColor.BLUE_LIGHT
        _cardBackTheme.value = CardBackTheme.entries.find {
            it.name == prefs.getString(modePrefKey("card_back_theme", mode), CardBackTheme.BLUE_GRADIENT.name)
        } ?: CardBackTheme.BLUE_GRADIENT
        _fontFamily.value = AppFontFamily.entries.find {
            it.name == prefs.getString(modePrefKey("font_family", mode), AppFontFamily.SYSTEM.name)
        } ?: AppFontFamily.SYSTEM
        _autoUncheckPerfectCount.value = prefs.getInt(modePrefKey("auto_uncheck_perfect_count", mode), 0)
        _autoUncheckAction.value = AutoUncheckAction.entries.find {
            it.name == prefs.getString(modePrefKey("auto_uncheck_action", mode), AutoUncheckAction.DEFAULT.name)
        } ?: AutoUncheckAction.DEFAULT
        _dictionaryPriority.value = DictionaryPriority.fromId(
            prefs.getString(modePrefKey("dictionary_lookup_priority", mode), DictionaryPriority.DEFAULT.id)
        )
        _internalDictionaryEnabled.value = prefs.getBoolean(modePrefKey("internal_dictionary_enabled", mode), true)
    }

    // Tabs
    private val _currentTab = MutableStateFlow(AppTab.STUDY)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun switchTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // All Cards from DB, filtered by current subject mode
    val allCards: StateFlow<List<FlashcardEntity>> = combine(repository.getAllCards(), _learningMode) { cards, mode ->
        cards.filter { it.subject.equals(mode.id, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val registeredWordSet: StateFlow<Set<String>> = allCards.map { cards ->
        cards.map { it.word.lowercase().trim() }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun saveCustomDictionaryEntry(
        word: String,
        pos: String,
        meanings: String,
        deck: String,
        note: String,
        isClassical: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanWord = word.trim()
            if (cleanWord.isBlank()) return@launch
            val key = cleanWord.lowercase()
            val existing = customDictionaryOverrideDao.getOverrideByKey(key)
            val isUserCreated = existing?.isUserCreated ?: (
                if (isClassical) ClassicalWordKnowledgeBase.lookup(cleanWord) == null
                else InternalDictionaryService.lookupEnglish(cleanWord) == null
            )
            val override = CustomDictionaryOverride(
                wordKey = key,
                word = cleanWord,
                isClassical = isClassical,
                isDeleted = false,
                isUserCreated = isUserCreated,
                pos = pos.trim().ifEmpty { if (isClassical) "古語" else "名詞" },
                meanings = meanings.trim(),
                deck = deck.trim().ifEmpty { if (isClassical) "共通テスト最重要動詞" else "日常英会話" },
                note = note.trim(),
                conjugation = existing?.conjugation ?: "",
                updatedAt = System.currentTimeMillis()
            )
            customDictionaryOverrideDao.upsertOverride(override)
        }
    }

    fun deleteDictionaryEntry(word: String, isClassical: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanWord = word.trim()
            if (cleanWord.isBlank()) return@launch
            val key = cleanWord.lowercase()
            val existing = customDictionaryOverrideDao.getOverrideByKey(key)
            val isUserCreated = existing?.isUserCreated ?: (
                if (isClassical) ClassicalWordKnowledgeBase.lookup(cleanWord) == null
                else InternalDictionaryService.lookupEnglish(cleanWord) == null
            )
            if (isUserCreated) {
                customDictionaryOverrideDao.deleteOverrideByKey(key)
            } else {
                val override = (existing ?: CustomDictionaryOverride(
                    wordKey = key,
                    word = cleanWord,
                    isClassical = isClassical,
                    isDeleted = true,
                    isUserCreated = false
                )).copy(isDeleted = true, updatedAt = System.currentTimeMillis())
                customDictionaryOverrideDao.upsertOverride(override)
            }
        }
    }

    fun resetCustomDictionary() {
        viewModelScope.launch(Dispatchers.IO) {
            customDictionaryOverrideDao.clearAllOverrides()
            InternalDictionaryService.applyOverrides(emptyList())
        }
    }

    fun addWordFromDictionary(
        result: com.example.data.service.InternalDictionaryResult,
        targetDeckOverride: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val targetDeck = targetDeckOverride?.takeIf { it.isNotBlank() }
                ?: if (_addDeck.value.isNotBlank()) _addDeck.value
                else if (result.deck.isNotBlank()) result.deck
                else if (result.isClassical) "共通テスト最重要動詞" else "日常英会話"

            val entity = FlashcardEntity(
                word = result.word,
                definitions = result.definitions,
                deck = targetDeck,
                note = result.note,
                status = null,
                repetitionLevel = 0,
                intervalDays = 0,
                nextReviewTimestamp = 0L,
                subject = if (result.isClassical) "CLASSICAL" else "ENGLISH"
            )
            repository.insertCard(entity)
            onComplete?.invoke(true)
        }
    }

    fun populateAddFormFromDictionary(result: com.example.data.service.InternalDictionaryResult) {
        _addWord.value = result.word
        _addDefinitions.value = result.definitions
        _addNote.value = result.note
        if (result.deck.isNotBlank()) {
            _addDeck.value = result.deck
        }
        _currentTab.value = AppTab.ADD
    }

    // Feature ⑧: Decks Management
    val defaultDecks: List<String>
        get() = if (_learningMode.value.isClassical) defaultClassicalDecks else defaultEnglishDecks

    private val _deletedDefaultDecks = MutableStateFlow<Set<String>>(loadDeletedDefaultDecks(_learningMode.value))
    private val _customDecks = MutableStateFlow<List<String>>(loadCustomDecks(_learningMode.value))

    val allDecks: StateFlow<List<String>> = combine(_customDecks, repository.getAllCards(), _deletedDefaultDecks, _learningMode) { custom, rawCards, deletedDefaults, mode ->
        val defaultForMode = if (mode.isClassical) defaultClassicalDecks else defaultEnglishDecks
        val activeDefaults = defaultForMode.filter { it !in deletedDefaults }
        val cardsForMode = rawCards.filter { it.subject.equals(mode.id, ignoreCase = true) }
        val fromCards = cardsForMode.map { it.deck }.filter { it.isNotBlank() }
        val validCustom = if (mode.isClassical) {
            custom.filter { it !in defaultEnglishDecks }
        } else {
            custom.filter { it !in defaultClassicalDecks }
        }
        (activeDefaults + validCustom + fromCards).distinct().sorted()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        if (_learningMode.value.isClassical) defaultClassicalDecks else defaultEnglishDecks
    )

    private val _selectedStudyDecks = MutableStateFlow<Set<String>>(emptySet())
    val selectedStudyDecks: StateFlow<Set<String>> = _selectedStudyDecks.asStateFlow()

    val selectedStudyDeck: StateFlow<String?> = _selectedStudyDecks.map { decks ->
        if (decks.size == 1) decks.first() else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedListDeck = MutableStateFlow<String?>(null)
    val selectedListDeck: StateFlow<String?> = _selectedListDeck.asStateFlow()

    fun toggleStudyDeck(deck: String) {
        val current = _selectedStudyDecks.value
        _selectedStudyDecks.value = if (deck in current) {
            current - deck
        } else {
            current + deck
        }
        _currentIndex.value = 0
        _isFlipped.value = false
    }

    fun clearStudyDecks() {
        _selectedStudyDecks.value = emptySet()
        _currentIndex.value = 0
        _isFlipped.value = false
    }

    fun selectStudyDeck(deck: String?) {
        _selectedStudyDecks.value = if (deck == null) emptySet() else setOf(deck)
        _currentIndex.value = 0
        _isFlipped.value = false
    }

    fun selectStudyDecks(decks: Set<String>) {
        _selectedStudyDecks.value = decks
        _currentIndex.value = 0
        _isFlipped.value = false
    }

    fun selectListDeck(deck: String?) {
        _selectedListDeck.value = deck
    }

    fun addCustomDeck(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && trimmed !in _customDecks.value) {
            val updated = _customDecks.value + trimmed
            _customDecks.value = updated
            saveCustomDecks(updated)
            return true
        }
        return false
    }

    fun renameCustomDeck(oldName: String, newName: String) {
        val trimmedNew = newName.trim()
        if (trimmedNew.isBlank() || trimmedNew == oldName) return
        val current = _customDecks.value
        val updated = current.map { if (it == oldName) trimmedNew else it }
        val finalCustom = if (trimmedNew !in updated && oldName !in current) updated + trimmedNew else updated
        _customDecks.value = finalCustom
        saveCustomDecks(finalCustom)

        viewModelScope.launch {
            repository.renameDeck(oldName, trimmedNew)
            if (oldName in _selectedStudyDecks.value) {
                _selectedStudyDecks.value = (_selectedStudyDecks.value - oldName) + trimmedNew
            }
            if (_selectedListDeck.value == oldName) _selectedListDeck.value = trimmedNew
            if (_addDeck.value == oldName) _addDeck.value = trimmedNew
        }
    }

    fun deleteCustomDeck(name: String, fallbackDeck: String = _learningMode.value.defaultDeck, deleteCards: Boolean = false) {
        val updated = _customDecks.value.filter { it != name }
        _customDecks.value = updated
        saveCustomDecks(updated)

        if (name in defaultDecks) {
            val newDeleted = _deletedDefaultDecks.value + name
            _deletedDefaultDecks.value = newDeleted
            saveDeletedDefaultDecks(newDeleted)
        }

        val resolvedFallback = if (fallbackDeck == name || fallbackDeck in _deletedDefaultDecks.value) {
            allDecks.value.firstOrNull { it != name } ?: _learningMode.value.defaultDeck
        } else {
            fallbackDeck
        }

        viewModelScope.launch {
            if (deleteCards) {
                repository.deleteByDeck(name)
            } else {
                repository.renameDeck(name, resolvedFallback)
            }
            if (name in _selectedStudyDecks.value) {
                _selectedStudyDecks.value = _selectedStudyDecks.value - name
            }
            if (_selectedListDeck.value == name) _selectedListDeck.value = null
            if (_addDeck.value == name) _addDeck.value = resolvedFallback
        }
    }

    private fun loadCustomDecks(mode: com.example.data.model.LearningMode = _learningMode.value): List<String> {
        val raw = prefs.getString(modePrefKey("custom_decks", mode), "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|").filter { it.isNotBlank() }
    }

    private fun saveCustomDecks(decks: List<String>, mode: com.example.data.model.LearningMode = _learningMode.value) {
        prefs.edit().putString(modePrefKey("custom_decks", mode), decks.joinToString("|")).apply()
    }

    private fun loadDeletedDefaultDecks(mode: com.example.data.model.LearningMode = _learningMode.value): Set<String> {
        val raw = prefs.getString(modePrefKey("deleted_default_decks", mode), "") ?: ""
        if (raw.isBlank()) return emptySet()
        return raw.split("|").filter { it.isNotBlank() }.toSet()
    }

    private fun saveDeletedDefaultDecks(deleted: Set<String>, mode: com.example.data.model.LearningMode = _learningMode.value) {
        prefs.edit().putString(modePrefKey("deleted_default_decks", mode), deleted.joinToString("|")).apply()
    }

    // Feature ①: 忘却曲線 (SRS) Due Filter
    private val _srsDueOnly = MutableStateFlow(false)
    val srsDueOnly: StateFlow<Boolean> = _srsDueOnly.asStateFlow()

    val srsDueCount: StateFlow<Int> = allCards.map { cards ->
        val now = System.currentTimeMillis()
        cards.count { !it.isHidden && (it.nextReviewTimestamp <= now || it.repetitionLevel == 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun toggleSrsDueOnly() {
        _srsDueOnly.value = !_srsDueOnly.value
        _currentIndex.value = 0
        _isFlipped.value = false
    }

    // Filters
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    fun toggleFilterExpanded() {
        _filterState.value = _filterState.value.copy(isExpanded = !_filterState.value.isExpanded)
    }

    fun toggleStatusFilter(status: String) {
        val current = _filterState.value.selectedStatuses.toMutableSet()
        if (current.contains(status)) {
            current.remove(status)
        } else {
            current.add(status)
        }
        _filterState.value = _filterState.value.copy(selectedStatuses = current)
    }

    fun togglePosFilter(pos: String) {
        val current = _filterState.value.selectedPos.toMutableSet()
        if (current.contains(pos)) {
            current.remove(pos)
        } else {
            current.add(pos)
        }
        _filterState.value = _filterState.value.copy(selectedPos = current)
    }

    fun toggleMultiPosOnly(enabled: Boolean) {
        _filterState.value = _filterState.value.copy(multiPosOnly = enabled)
    }

    fun resetFilters() {
        _filterState.value = FilterState()
    }

    // Shuffle & Study Cards
    private val _isShuffle = MutableStateFlow(prefs.getBoolean(modePrefKey("is_shuffle"), false))
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _shuffledCardIds = MutableStateFlow<List<Long>>(emptyList())

    private val _studyDeckAndSrs = combine(_selectedStudyDecks, _srsDueOnly) { decks, srsOnly ->
        Pair(decks, srsOnly)
    }

    val studyCards: StateFlow<List<FlashcardEntity>> = combine(
        allCards,
        _filterState,
        _isShuffle,
        _shuffledCardIds,
        _studyDeckAndSrs
    ) { cards, filter, shuffle, shuffledIds, deckAndSrs ->
        val (selectedDecks, srsOnly) = deckAndSrs
        val now = System.currentTimeMillis()
        val filtered = cards.filter { card ->
            if (card.isHidden) return@filter false
            if (selectedDecks.isNotEmpty() && card.deck !in selectedDecks) return@filter false
            if (srsOnly && card.nextReviewTimestamp > now && card.repetitionLevel > 0) return@filter false
            isCardVisible(card, filter)
        }
        if (!shuffle) {
            filtered
        } else {
            if (shuffledIds.isEmpty()) {
                filtered
            } else {
                val map = filtered.associateBy { it.id }
                val ordered = shuffledIds.mapNotNull { map[it] }
                val unmapped = filtered.filter { it.id !in shuffledIds }
                ordered + unmapped
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleShuffle() {
        val next = !_isShuffle.value
        _isShuffle.value = next
        prefs.edit().putBoolean(modePrefKey("is_shuffle"), next).apply()
        if (next) {
            reshuffle()
        }
    }

    fun reshuffle() {
        val currentVisible = allCards.value.filter { !it.isHidden && isCardVisible(it, _filterState.value) }
        _shuffledCardIds.value = currentVisible.map { it.id }.shuffled()
        _currentIndex.value = 0
        _isFlipped.value = false
    }

    fun isCardVisible(card: FlashcardEntity, filter: FilterState): Boolean {
        if (filter.selectedStatuses.isNotEmpty()) {
            val s = card.status ?: "未"
            val matchesStatus = if (s in listOf("○", "△", "×")) {
                filter.selectedStatuses.contains(s)
            } else {
                filter.selectedStatuses.contains("未")
            }
            if (!matchesStatus) return false
        }

        if (filter.selectedPos.isNotEmpty()) {
            val cardPosSet = card.definitions.flatMap {
                it.pos.split(Regex("[・、,\\s/]+"))
            }.map { it.trim() }.filter { it.isNotBlank() }.toSet()
            val matchesPos = filter.selectedPos.any { selected ->
                selected in cardPosSet || card.definitions.any { it.pos.contains(selected) }
            }
            if (!matchesPos) return false
        }

        if (filter.multiPosOnly) {
            val distinctPosCount = card.definitions.flatMap {
                it.pos.split(Regex("[・、,\\s/]+"))
            }.map { it.trim() }.filter { it.isNotBlank() }.distinct().size
            if (distinctPosCount <= 1) return false
        }

        return true
    }

    // Flashcard Study Interaction
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isFlipped = MutableStateFlow(false)
    val isFlipped: StateFlow<Boolean> = _isFlipped.asStateFlow()

    fun flipCard() {
        _isFlipped.value = !_isFlipped.value
    }

    fun nextCard() {
        val total = studyCards.value.size
        if (total > 0) {
            _currentIndex.value = (_currentIndex.value + 1) % total
            _isFlipped.value = false
        }
    }

    fun prevCard() {
        val total = studyCards.value.size
        if (total > 0) {
            _currentIndex.value = (_currentIndex.value - 1 + total) % total
            _isFlipped.value = false
        }
    }

    fun evaluateCard(status: String) {
        val cards = studyCards.value
        val idx = _currentIndex.value
        if (idx in cards.indices) {
            val card = cards[idx]
            viewModelScope.launch {
                repository.updateSrsRating(card.id, status)
                val threshold = autoUncheckPerfectCount.value
                val wasPerfect = card.status == "○"

                if (status == "○") {
                    val newConsecutive = if (wasPerfect) card.consecutivePerfectCount + 1 else 1

                    if (threshold > 0 && wasPerfect && newConsecutive >= threshold) {
                        // Reached threshold consecutive times of perfect while already in perfect state!
                        when (autoUncheckAction.value) {
                            AutoUncheckAction.HIDE_FROM_STUDY -> {
                                repository.updateHidden(card.id, true)
                                _lastAutoUncheckMessage.value = "🎉「${card.word}」が${threshold}回連続完璧を達成！学習対象から外しました"
                            }
                            AutoUncheckAction.RESET_STATUS -> {
                                repository.updateStatus(card.id, null)
                                _lastAutoUncheckMessage.value = "🎉「${card.word}」が${threshold}回連続完璧を達成！完璧マークを解除しました"
                            }
                            AutoUncheckAction.BOTH -> {
                                repository.updateHidden(card.id, true)
                                repository.updateStatus(card.id, null)
                                _lastAutoUncheckMessage.value = "🎉「${card.word}」が${threshold}回連続完璧を達成！学習対象から外し未評価に戻しました"
                            }
                        }
                    }
                }
                nextCard()
            }
        }
    }

    fun deleteCurrentCard() {
        val cards = studyCards.value
        val idx = _currentIndex.value
        if (idx in cards.indices) {
            val card = cards[idx]
            viewModelScope.launch {
                repository.deleteCard(card)
                if (_currentIndex.value >= studyCards.value.size - 1) {
                    _currentIndex.value = maxOf(0, studyCards.value.size - 2)
                }
                _isFlipped.value = false
            }
        }
    }

    // List Screen
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(WordSortOrder.CREATED_ASC)
    val sortOrder: StateFlow<WordSortOrder> = _sortOrder.asStateFlow()

    // 非表示カードを一覧に含めるかどうか（デフォルト: true = 非表示カードも含めて表示し薄く表示/切り替え可能、false = 非表示カードを一覧から除外）
    private val _showHiddenInList = MutableStateFlow(true)
    val showHiddenInList: StateFlow<Boolean> = _showHiddenInList.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: WordSortOrder) {
        _sortOrder.value = order
    }

    fun setShowHiddenInList(show: Boolean) {
        _showHiddenInList.value = show
    }

    fun toggleShowHiddenInList() {
        _showHiddenInList.value = !_showHiddenInList.value
    }

    // List tab filters (品詞 & ステータス)
    private val _listFilterState = MutableStateFlow(FilterState())
    val listFilterState: StateFlow<FilterState> = _listFilterState.asStateFlow()

    private val _listSelectedPos = MutableStateFlow<Set<String>>(emptySet())
    val listSelectedPos: StateFlow<Set<String>> = _listSelectedPos.asStateFlow()

    private val _listSelectedStatuses = MutableStateFlow<Set<String>>(emptySet())
    val listSelectedStatuses: StateFlow<Set<String>> = _listSelectedStatuses.asStateFlow()

    fun toggleListFilterExpanded() {
        _listFilterState.value = _listFilterState.value.copy(isExpanded = !_listFilterState.value.isExpanded)
    }

    fun toggleListStatusFilter(status: String) {
        val current = _listFilterState.value.selectedStatuses.toMutableSet()
        if (current.contains(status)) {
            current.remove(status)
        } else {
            current.add(status)
        }
        _listFilterState.value = _listFilterState.value.copy(selectedStatuses = current)
        _listSelectedStatuses.value = current
    }

    fun toggleListPosFilter(pos: String) {
        val current = _listFilterState.value.selectedPos.toMutableSet()
        if (current.contains(pos)) {
            current.remove(pos)
        } else {
            current.add(pos)
        }
        _listFilterState.value = _listFilterState.value.copy(selectedPos = current)
        _listSelectedPos.value = current
    }

    fun toggleListMultiPosOnly(enabled: Boolean) {
        _listFilterState.value = _listFilterState.value.copy(multiPosOnly = enabled)
    }

    fun resetListFilters() {
        _listFilterState.value = FilterState()
        _listSelectedPos.value = emptySet()
        _listSelectedStatuses.value = emptySet()
    }

    fun toggleListPos(pos: String) = toggleListPosFilter(pos)
    fun clearListPos() {
        _listFilterState.value = _listFilterState.value.copy(selectedPos = emptySet())
        _listSelectedPos.value = emptySet()
    }

    fun toggleListStatus(status: String) = toggleListStatusFilter(status)
    fun clearListStatus() {
        _listFilterState.value = _listFilterState.value.copy(selectedStatuses = emptySet())
        _listSelectedStatuses.value = emptySet()
    }

    val listCards: StateFlow<List<FlashcardEntity>> = combine(
        allCards,
        _searchQuery,
        _sortOrder,
        _selectedListDeck,
        combine(_showHiddenInList, _listFilterState) { hidden, filter ->
            Pair(hidden, filter)
        }
    ) { cards, query, sort, deck, (showHidden, filter) ->
        val hiddenFiltered = if (showHidden) cards else cards.filter { !it.isHidden }
        val deckFiltered = if (deck == null) hiddenFiltered else hiddenFiltered.filter { it.deck == deck }
        val statusAndPosFiltered = deckFiltered.filter { isCardVisible(it, filter) }

        val filtered = if (query.isBlank()) {
            statusAndPosFiltered
        } else {
            val q = query.trim().lowercase()
            statusAndPosFiltered.filter { card ->
                card.word.lowercase().contains(q) ||
                card.definitions.any { it.meaning.lowercase().contains(q) || it.pos.lowercase().contains(q) } ||
                card.note.lowercase().contains(q)
            }
        }

        when (sort) {
            WordSortOrder.CREATED_ASC -> filtered.sortedBy { it.createdAt }
            WordSortOrder.CREATED_DESC -> filtered.sortedByDescending { it.createdAt }
            WordSortOrder.ALPHA_ASC -> filtered.sortedBy { it.word.lowercase() }
            WordSortOrder.ALPHA_DESC -> filtered.sortedByDescending { it.word.lowercase() }
            WordSortOrder.POS_ASC -> {
                val posOrder = PartsOfSpeech.ALL.withIndex().associate { it.value to it.index }
                filtered.sortedWith(
                    compareBy<FlashcardEntity> { card ->
                        val pos = card.definitions.firstOrNull { it.pos.isNotBlank() }?.pos?.trim() ?: ""
                        posOrder[pos] ?: (PartsOfSpeech.ALL.size + 1)
                    }.thenBy { it.word.lowercase() }
                )
            }
            WordSortOrder.POS_DESC -> {
                val posOrder = PartsOfSpeech.ALL.withIndex().associate { it.value to it.index }
                filtered.sortedWith(
                    compareByDescending<FlashcardEntity> { card ->
                        val pos = card.definitions.firstOrNull { it.pos.isNotBlank() }?.pos?.trim() ?: ""
                        posOrder[pos] ?: -1
                    }.thenBy { it.word.lowercase() }
                )
            }
            WordSortOrder.DECK_ASC -> {
                filtered.sortedWith(
                    compareBy<FlashcardEntity> { it.deck.lowercase() }
                        .thenBy { it.word.lowercase() }
                )
            }
            WordSortOrder.DECK_DESC -> {
                filtered.sortedWith(
                    compareByDescending<FlashcardEntity> { it.deck.lowercase() }
                        .thenBy { it.word.lowercase() }
                )
            }
            WordSortOrder.SRS_LEVEL_ASC -> {
                filtered.sortedWith(
                    compareBy<FlashcardEntity> { it.repetitionLevel }
                        .thenBy { it.nextReviewTimestamp }
                        .thenBy { it.word.lowercase() }
                )
            }
            WordSortOrder.SRS_LEVEL_DESC -> {
                filtered.sortedWith(
                    compareByDescending<FlashcardEntity> { it.repetitionLevel }
                        .thenBy { it.nextReviewTimestamp }
                        .thenBy { it.word.lowercase() }
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleCardHidden(card: FlashcardEntity) {
        viewModelScope.launch {
            repository.updateHidden(card.id, !card.isHidden)
        }
    }

    fun batchSetHidden(ids: List<Long>, isHidden: Boolean) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.updateBatchHidden(ids, isHidden)
        }
    }

    fun deleteCard(card: FlashcardEntity) {
        viewModelScope.launch {
            repository.deleteCard(card)
        }
    }

    fun batchDeleteCards(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteBatchByIds(ids)
        }
    }

    // Editing Modal
    private val _editingCard = MutableStateFlow<FlashcardEntity?>(null)
    val editingCard: StateFlow<FlashcardEntity?> = _editingCard.asStateFlow()

    fun openEditModal(card: FlashcardEntity) {
        _editingCard.value = card
    }

    fun closeEditModal() {
        _editingCard.value = null
    }

    fun saveEditedCard(card: FlashcardEntity) {
        viewModelScope.launch {
            val normalizedCard = if (card.subject.equals("CLASSICAL", ignoreCase = true)) {
                val extractedConjs = mutableListOf<String>()
                val simpleDefs = card.definitions.map { def ->
                    val (simplePos, conj) = ClassicalPosNormalizer.splitPosAndConjugation(def.pos)
                    if (conj.isNotBlank()) extractedConjs.add(conj)
                    def.copy(pos = simplePos)
                }
                val mergedNote = ClassicalPosNormalizer.mergeConjugationIntoNote(extractedConjs, card.note)
                card.copy(definitions = simpleDefs, note = mergedNote)
            } else {
                card
            }
            repository.updateCard(normalizedCard)
            _editingCard.value = null
        }
    }

    // Delete All Confirmation
    private val _showDeleteAllDialog = MutableStateFlow(false)
    val showDeleteAllDialog: StateFlow<Boolean> = _showDeleteAllDialog.asStateFlow()

    fun setDeleteAllDialogVisible(visible: Boolean) {
        _showDeleteAllDialog.value = visible
    }

    fun confirmDeleteAll() {
        viewModelScope.launch {
            repository.deleteAll()
            _showDeleteAllDialog.value = false
            _currentIndex.value = 0
        }
    }

    // Add Screen
    private val _addWord = MutableStateFlow("")
    val addWord: StateFlow<String> = _addWord.asStateFlow()

    private val _addNote = MutableStateFlow("")
    val addNote: StateFlow<String> = _addNote.asStateFlow()

    private val _addDeck = MutableStateFlow(_learningMode.value.defaultDeck)
    val addDeck: StateFlow<String> = _addDeck.asStateFlow()

    private val _addDefinitions = MutableStateFlow<List<Definition>>(listOf(Definition("", "")))
    val addDefinitions: StateFlow<List<Definition>> = _addDefinitions.asStateFlow()

    fun setAddDeck(deck: String) { _addDeck.value = deck }

    fun applySmartKnowledge(knowledge: com.example.data.model.WordKnowledge) {
        _addWord.value = knowledge.word
        _addDefinitions.value = knowledge.definitions
        _addDeck.value = knowledge.deck
        _addNote.value = knowledge.note
    }

    val duplicateCard: StateFlow<FlashcardEntity?> = combine(allCards, _addWord) { cards, word ->
        val trimmed = word.trim()
        if (trimmed.isBlank()) null
        else cards.find { it.word.equals(trimmed, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var weblioJob: Job? = null
    private val _isWeblioLoading = MutableStateFlow(false)
    val isWeblioLoading: StateFlow<Boolean> = _isWeblioLoading.asStateFlow()

    private val _lastWeblioAutofilledWord = MutableStateFlow<String?>(null)
    val lastWeblioAutofilledWord: StateFlow<String?> = _lastWeblioAutofilledWord.asStateFlow()

    private var lastAutoFilledNote: String? = null

    private fun applyClassicalAutoConjugationNote(conjugation: String) {
        if (!_weblioAutoAddConjugationEnabled.value) return
        if (conjugation.isNotBlank()) {
            if (_addNote.value.isBlank() || _addNote.value == lastAutoFilledNote) {
                _addNote.value = conjugation
                lastAutoFilledNote = conjugation
            }
        } else {
            if (lastAutoFilledNote != null && _addNote.value == lastAutoFilledNote) {
                _addNote.value = ""
                lastAutoFilledNote = null
            }
        }
    }

    fun setAddWord(word: String) {
        _addWord.value = word
        val trimmed = word.trim()
        if (_learningMode.value.isClassical) {
            if (_weblioAutoFillEnabled.value) {
                if (trimmed.isBlank()) {
                    weblioJob?.cancel()
                    _isWeblioLoading.value = false
                    _lastWeblioAutofilledWord.value = null
                    _addDefinitions.value = listOf(Definition("", ""))
                    if (_weblioAutoAddConjugationEnabled.value && lastAutoFilledNote != null && _addNote.value == lastAutoFilledNote) {
                        _addNote.value = ""
                        lastAutoFilledNote = null
                    }
                } else {
                    triggerWeblioLookupDebounced(trimmed)
                }
            } else {
                weblioJob?.cancel()
                _isWeblioLoading.value = false
            }
            return
        }
        if (_weblioAutoFillEnabled.value) {
            if (trimmed.isBlank()) {
                // When word is cleared/deleted, automatically clear the meaning and note as requested
                weblioJob?.cancel()
                _isWeblioLoading.value = false
                _lastWeblioAutofilledWord.value = null
                _addDefinitions.value = listOf(Definition("", ""))
                if (_weblioAutoAddNoteLevelEnabled.value && lastAutoFilledNote != null && _addNote.value == lastAutoFilledNote) {
                    _addNote.value = ""
                    lastAutoFilledNote = null
                }
            } else if (trimmed.length < 2 && !trimmed.equals("a", ignoreCase = true) && !trimmed.equals("i", ignoreCase = true)) {
                // When incomplete single letter (not valid 1-letter words a/I), clear outdated previous definition
                weblioJob?.cancel()
                _isWeblioLoading.value = false
                _lastWeblioAutofilledWord.value = null
                _addDefinitions.value = listOf(Definition("", ""))
                if (_weblioAutoAddNoteLevelEnabled.value && lastAutoFilledNote != null && _addNote.value == lastAutoFilledNote) {
                    _addNote.value = ""
                    lastAutoFilledNote = null
                }
            } else {
                // Update Weblio definition whenever word is updated as requested
                triggerWeblioLookupDebounced(trimmed)
            }
        }
    }

    fun triggerWeblioLookup(force: Boolean = false) {
        val word = _addWord.value.trim()
        val isClassical = _learningMode.value.isClassical
        if (word.isBlank()) {
            if (_weblioAutoFillEnabled.value || _internalDictionaryEnabled.value) {
                _addDefinitions.value = listOf(Definition("", ""))
                if (isClassical) {
                    if (_weblioAutoAddConjugationEnabled.value && lastAutoFilledNote != null && _addNote.value == lastAutoFilledNote) {
                        _addNote.value = ""
                        lastAutoFilledNote = null
                    }
                } else {
                    if (_weblioAutoAddNoteLevelEnabled.value && lastAutoFilledNote != null && _addNote.value == lastAutoFilledNote) {
                        _addNote.value = ""
                        lastAutoFilledNote = null
                    }
                }
            }
            return
        }

        if (!force && !_weblioAutoFillEnabled.value && !_internalDictionaryEnabled.value) return

        weblioJob?.cancel()
        weblioJob = viewModelScope.launch {
            _isWeblioLoading.value = true
            try {
                performDictionaryLookup(word, isClassical, force)
            } finally {
                _isWeblioLoading.value = false
            }
        }
    }

    private fun triggerWeblioLookupDebounced(word: String) {
        val trimmed = word.trim()
        if (trimmed.isBlank()) return
        val isClassical = _learningMode.value.isClassical
        if (!_weblioAutoFillEnabled.value && !_internalDictionaryEnabled.value) return

        weblioJob?.cancel()
        weblioJob = viewModelScope.launch {
            delay(250)
            _isWeblioLoading.value = true
            try {
                performDictionaryLookup(trimmed, isClassical, force = false)
            } finally {
                _isWeblioLoading.value = false
            }
        }
    }

    private suspend fun performDictionaryLookup(word: String, isClassical: Boolean, force: Boolean) {
        val internalEnabled = _internalDictionaryEnabled.value
        val weblioEnabled = _weblioAutoFillEnabled.value || force
        val priority = _dictionaryPriority.value

        if (!internalEnabled && !weblioEnabled) return

        if (internalEnabled && priority == DictionaryPriority.INTERNAL_FIRST) {
            val internal = InternalDictionaryService.lookupWord(word, isClassical)
            if (internal != null && internal.definitions.isNotEmpty()) {
                applyDictionaryResult(word, internal.toWeblioLookupResult(), isClassical, internal.deck)
                return
            }
            // Fallback to Weblio
            if (weblioEnabled) {
                val weblio = if (isClassical) {
                    WeblioDictionaryService.lookupClassicalWord(word)
                } else {
                    WeblioDictionaryService.lookupWord(word)
                }
                if (weblio != null && weblio.definitions.isNotEmpty()) {
                    applyDictionaryResult(word, weblio, isClassical, null)
                }
            }
            return
        }

        // WEBLIO_FIRST: Try Weblio first
        if (weblioEnabled) {
            val weblio = if (isClassical) {
                WeblioDictionaryService.lookupClassicalWord(word)
            } else {
                WeblioDictionaryService.lookupWord(word)
            }
            if (weblio != null && weblio.definitions.isNotEmpty()) {
                applyDictionaryResult(word, weblio, isClassical, null)
                return
            }
        }

        // Fallback to internal
        if (internalEnabled) {
            val internal = InternalDictionaryService.lookupWord(word, isClassical)
            if (internal != null && internal.definitions.isNotEmpty()) {
                applyDictionaryResult(word, internal.toWeblioLookupResult(), isClassical, internal.deck)
            }
        }
    }

    private fun applyDictionaryResult(
        requestedWord: String,
        result: WeblioLookupResult,
        isClassical: Boolean,
        suggestedDeck: String?
    ) {
        val currentWord = _addWord.value.trim()
        val matches = if (isClassical) currentWord == requestedWord else currentWord.equals(requestedWord, ignoreCase = true)
        if (!matches) return

        _addDefinitions.value = result.definitions
        _lastWeblioAutofilledWord.value = requestedWord

        if (isClassical) {
            applyClassicalAutoConjugationNote(result.conjugation)
            if (suggestedDeck != null && (_addDeck.value in defaultEnglishDecks || _addDeck.value.isBlank())) {
                _addDeck.value = suggestedDeck
            }
        } else {
            if (_weblioAutoAddNoteLevelEnabled.value && result.formattedNote.isNotBlank()) {
                if (_addNote.value.isBlank() || _addNote.value == lastAutoFilledNote) {
                    _addNote.value = result.formattedNote
                    lastAutoFilledNote = result.formattedNote
                }
            }
        }
    }

    fun setAddNote(note: String) {
        _addNote.value = note
    }

    fun addDefinitionRow() {
        _addDefinitions.value = _addDefinitions.value + Definition("", "")
    }

    fun updateDefinitionRow(index: Int, pos: String, meaning: String) {
        val list = _addDefinitions.value.toMutableList()
        if (index in list.indices) {
            list[index] = Definition(pos = pos, meaning = meaning)
            _addDefinitions.value = list
        }
    }

    fun removeDefinitionRow(index: Int) {
        val list = _addDefinitions.value.toMutableList()
        if (list.size > 1 && index in list.indices) {
            list.removeAt(index)
            _addDefinitions.value = list
        }
    }

    fun addCard(
        onDuplicateFound: (FlashcardEntity) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val word = _addWord.value.trim()
        val wordLabel = if (_learningMode.value.isClassical) "古語" else "英単語"
        if (word.isBlank()) {
            onError("${wordLabel}を入力してください")
            return
        }

        val defs = _addDefinitions.value.map { it.copy(meaning = it.meaning.trim(), pos = it.pos.trim()) }.filter { it.meaning.isNotBlank() }
        if (defs.isEmpty()) {
            onError("少なくとも1つの意味を入力してください")
            return
        }

        val existing = duplicateCard.value
        if (existing != null) {
            onDuplicateFound(existing)
            return
        }

        forceAddNewCard(onSuccess, onError)
    }

    fun forceAddNewCard(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val word = _addWord.value.trim()
        val wordLabel = if (_learningMode.value.isClassical) "古語" else "英単語"
        if (word.isBlank()) {
            onError("${wordLabel}を入力してください")
            return
        }

        val defs = _addDefinitions.value.map { it.copy(meaning = it.meaning.trim(), pos = it.pos.trim()) }.filter { it.meaning.isNotBlank() }
        if (defs.isEmpty()) {
            onError("少なくとも1つの意味を入力してください")
            return
        }

        viewModelScope.launch {
            try {
                val isClassical = _learningMode.value.isClassical
                val extractedConjs = mutableListOf<String>()
                val cleanedDefs = defs.map { def ->
                    val cleanedMeaning = def.meaning.trim().replace(Regex("[\\r\\n]+"), " ")
                    if (isClassical) {
                        val (simplePos, conj) = ClassicalPosNormalizer.splitPosAndConjugation(def.pos)
                        if (conj.isNotBlank()) extractedConjs.add(conj)
                        def.copy(pos = simplePos, meaning = cleanedMeaning)
                    } else {
                        def.copy(meaning = cleanedMeaning)
                    }
                }
                val rawNote = _addNote.value.trim().replace(Regex("[\\r\\n]+"), " ")
                val finalNote = if (isClassical) {
                    ClassicalPosNormalizer.mergeConjugationIntoNote(extractedConjs, rawNote)
                } else {
                    rawNote
                }
                repository.insertCard(
                    FlashcardEntity(
                        word = word,
                        definitions = cleanedDefs,
                        deck = _addDeck.value,
                        note = finalNote,
                        subject = _learningMode.value.id
                    )
                )
                clearAddForm()
                onSuccess()
            } catch (e: Exception) {
                onError("登録に失敗しました: ${e.localizedMessage}")
            }
        }
    }

    fun clearAddForm() {
        weblioJob?.cancel()
        _isWeblioLoading.value = false
        _lastWeblioAutofilledWord.value = null
        lastAutoFilledNote = null
        _addWord.value = ""
        _addNote.value = ""
        _addDefinitions.value = listOf(Definition("", ""))
    }

    fun mergeIntoExisting(
        existingCard: FlashcardEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val newDefs = _addDefinitions.value.filter { it.meaning.isNotBlank() }
        if (newDefs.isEmpty()) {
            onError("少なくとも1つの意味を入力してください")
            return
        }

        viewModelScope.launch {
            try {
                val mergedDefs = existingCard.definitions.toMutableList()
                for (d in newDefs) {
                    if (mergedDefs.none { it.pos.equals(d.pos, ignoreCase = true) && it.meaning.equals(d.meaning, ignoreCase = true) }) {
                        mergedDefs.add(d)
                    }
                }
                val newNote = _addNote.value.trim()
                val mergedNote = when {
                    existingCard.note.isBlank() -> newNote
                    newNote.isBlank() -> existingCard.note
                    else -> "${existingCard.note}; $newNote"
                }

                val updatedCard = existingCard.copy(
                    definitions = mergedDefs,
                    note = mergedNote
                )
                repository.updateCard(updatedCard)
                _addWord.value = ""
                _addNote.value = ""
                lastAutoFilledNote = null
                _addDefinitions.value = listOf(Definition("", ""))
                onSuccess()
            } catch (e: Exception) {
                onError("統合に失敗しました: ${e.localizedMessage}")
            }
        }
    }

    // Bulk Import
    private val _bulkInput = MutableStateFlow("")
    val bulkInput: StateFlow<String> = _bulkInput.asStateFlow()

    fun setBulkInput(text: String) {
        _bulkInput.value = text
    }

    fun checkBulkDuplicates(text: String): List<String> {
        val rows = repository.parseDelimitedText(text)
        val existingWords = allCards.value.map { it.word.lowercase() }.toSet()
        return repository.findDuplicateWordsInRows(rows, existingWords)
    }

    fun importBulk(
        strategy: DuplicateStrategy = DuplicateStrategy.MERGE_DUPLICATES,
        targetDeck: String? = null,
        onResult: (ImportResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val text = _bulkInput.value
        if (text.isBlank()) {
            onError("テキストが空です")
            return
        }
        viewModelScope.launch {
            try {
                if (targetDeck != null && targetDeck.isNotBlank()) {
                    addCustomDeck(targetDeck)
                }
                val result = repository.importBulk(text, strategy, targetDeck, subject = _learningMode.value.id)
                _bulkInput.value = ""
                onResult(result)
            } catch (e: Exception) {
                onError("一括登録に失敗しました: ${e.localizedMessage}")
            }
        }
    }

    fun importBulk(onResult: (Int, Int) -> Unit, onError: (String) -> Unit) {
        importBulk(
            strategy = DuplicateStrategy.MERGE_DUPLICATES,
            targetDeck = null,
            onResult = { res -> onResult(res.newCount, res.mergeCount) },
            onError = onError
        )
    }

    fun importText(
        text: String,
        strategy: DuplicateStrategy = DuplicateStrategy.MERGE_DUPLICATES,
        targetDeck: String? = null,
        onResult: (ImportResult) -> Unit,
        onError: (String) -> Unit
    ) {
        if (text.isBlank()) {
            onError("テキストが空です")
            return
        }
        viewModelScope.launch {
            try {
                if (targetDeck != null && targetDeck.isNotBlank()) {
                    addCustomDeck(targetDeck)
                }
                val result = repository.importBulk(text, strategy, targetDeck, subject = _learningMode.value.id)
                onResult(result)
            } catch (e: Exception) {
                onError("インポートに失敗しました: ${e.localizedMessage}")
            }
        }
    }

    fun importText(text: String, onResult: (Int, Int) -> Unit, onError: (String) -> Unit) {
        importText(
            text = text,
            strategy = DuplicateStrategy.MERGE_DUPLICATES,
            targetDeck = null,
            onResult = { res -> onResult(res.newCount, res.mergeCount) },
            onError = onError
        )
    }

    fun importXlsx(
        inputStream: InputStream,
        strategy: DuplicateStrategy = DuplicateStrategy.MERGE_DUPLICATES,
        targetDeck: String? = null,
        onResult: (ImportResult) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (targetDeck != null && targetDeck.isNotBlank()) {
                    addCustomDeck(targetDeck)
                }
                val result = repository.importXlsx(inputStream, strategy, targetDeck, subject = _learningMode.value.id)
                onResult(result)
            } catch (e: Exception) {
                onError("Excelの読み込みに失敗しました: ${e.localizedMessage}")
            }
        }
    }

    fun importXlsx(
        inputStream: InputStream,
        onResult: (Int, Int) -> Unit,
        onError: (String) -> Unit
    ) {
        importXlsx(
            inputStream = inputStream,
            strategy = DuplicateStrategy.MERGE_DUPLICATES,
            targetDeck = null,
            onResult = { res -> onResult(res.newCount, res.mergeCount) },
            onError = onError
        )
    }

    // Export
    fun exportData(onSuccess: (String) -> Unit) {
        exportDataAsCsv(null, onSuccess)
    }

    fun exportDataAsCsv(deckFilter: String? = null, onSuccess: (String) -> Unit) {
        val mode = _learningMode.value
        viewModelScope.launch {
            val all = repository.getAllOnce().filter { it.subject.equals(mode.id, ignoreCase = true) }
            val cards = if (deckFilter != null && deckFilter.isNotBlank()) {
                all.filter { it.deck == deckFilter }
            } else {
                all
            }
            val sb = StringBuilder()
            if (mode.isClassical) {
                sb.append("古語,現代語訳,品詞,備考,ステータス,非表示,デッキ\n")
            } else {
                sb.append("単語,意味,品詞,備考,ステータス,非表示,デッキ\n")
            }
            for (c in cards) {
                val defs = c.definitions.joinToString(" / ") { it.meaning }
                val pos = c.definitions.joinToString(" / ") { it.pos }
                val statusStr = c.status ?: ""
                val hiddenStr = if (c.isHidden) "1" else "0"
                sb.append("\"${escapeCsv(c.word)}\",\"${escapeCsv(defs)}\",\"${escapeCsv(pos)}\",\"${escapeCsv(c.note)}\",\"${escapeCsv(statusStr)}\",\"$hiddenStr\",\"${escapeCsv(c.deck)}\"\n")
            }
            onSuccess(sb.toString())
        }
    }

    fun exportDataAsCsv(onSuccess: (String) -> Unit) {
        exportDataAsCsv(null, onSuccess)
    }

    fun exportDataAsTsv(deckFilter: String? = null, onSuccess: (String) -> Unit) {
        val mode = _learningMode.value
        viewModelScope.launch {
            val all = repository.getAllOnce().filter { it.subject.equals(mode.id, ignoreCase = true) }
            val cards = if (deckFilter != null && deckFilter.isNotBlank()) {
                all.filter { it.deck == deckFilter }
            } else {
                all
            }
            val sb = StringBuilder()
            if (mode.isClassical) {
                sb.append("古語\t現代語訳\t品詞\t備考\tステータス\t非表示\tデッキ\n")
            } else {
                sb.append("単語\t意味\t品詞\t備考\tステータス\t非表示\tデッキ\n")
            }
            for (c in cards) {
                val defs = c.definitions.joinToString(" / ") { it.meaning }
                val pos = c.definitions.joinToString(" / ") { it.pos }
                val statusStr = c.status ?: ""
                val hiddenStr = if (c.isHidden) "1" else "0"
                sb.append("${c.word}\t${defs}\t${pos}\t${c.note}\t$statusStr\t$hiddenStr\t${c.deck}\n")
            }
            onSuccess(sb.toString())
        }
    }

    fun exportDataAsTsv(onSuccess: (String) -> Unit) {
        exportDataAsTsv(null, onSuccess)
    }

    private fun escapeCsv(str: String): String {
        return str.replace("\"", "\"\"")
    }

    // Internal Dictionary & Search Priority Settings
    private val _dictionaryPriority = MutableStateFlow(
        DictionaryPriority.fromId(prefs.getString(modePrefKey("dictionary_lookup_priority"), DictionaryPriority.DEFAULT.id))
    )
    val dictionaryPriority: StateFlow<DictionaryPriority> = _dictionaryPriority.asStateFlow()

    fun setDictionaryPriority(priority: DictionaryPriority) {
        _dictionaryPriority.value = priority
        prefs.edit().putString(modePrefKey("dictionary_lookup_priority"), priority.id).apply()
        val currentWord = _addWord.value.trim()
        if (currentWord.isNotBlank()) {
            triggerWeblioLookup(force = true)
        }
    }

    private val _internalDictionaryEnabled = MutableStateFlow(
        prefs.getBoolean(modePrefKey("internal_dictionary_enabled"), true)
    )
    val internalDictionaryEnabled: StateFlow<Boolean> = _internalDictionaryEnabled.asStateFlow()

    fun setInternalDictionaryEnabled(enabled: Boolean) {
        _internalDictionaryEnabled.value = enabled
        prefs.edit().putBoolean(modePrefKey("internal_dictionary_enabled"), enabled).apply()
        val currentWord = _addWord.value.trim()
        if (currentWord.isNotBlank()) {
            triggerWeblioLookup(force = true)
        }
    }

    // Weblio Dictionary Auto-fill (mode-scoped: English -> ejje.weblio.jp, Classical -> kobun.weblio.jp)
    private val _weblioAutoFillEnabled = MutableStateFlow(prefs.getBoolean(modePrefKey("weblio_autofill_enabled"), true))
    val weblioAutoFillEnabled: StateFlow<Boolean> = _weblioAutoFillEnabled.asStateFlow()

    fun setWeblioAutoFillEnabled(enabled: Boolean) {
        _weblioAutoFillEnabled.value = enabled
        prefs.edit().putBoolean(modePrefKey("weblio_autofill_enabled"), enabled).apply()
        if (enabled) {
            val currentWord = _addWord.value.trim()
            if (currentWord.isNotBlank()) {
                triggerWeblioLookup(force = true)
            } else {
                _addDefinitions.value = listOf(Definition("", ""))
            }
        } else {
            weblioJob?.cancel()
            _isWeblioLoading.value = false
        }
    }

    // Weblio Note Level (レベル・英検級) Auto-fill (English mode only; always false in Classical mode)
    private val _weblioAutoAddNoteLevelEnabled = MutableStateFlow(
        if (_learningMode.value.isClassical) false else prefs.getBoolean("weblio_auto_add_note_level_enabled", true)
    )
    val weblioAutoAddNoteLevelEnabled: StateFlow<Boolean> = _weblioAutoAddNoteLevelEnabled.asStateFlow()

    fun setWeblioAutoAddNoteLevelEnabled(enabled: Boolean) {
        if (_learningMode.value.isClassical) {
            _weblioAutoAddNoteLevelEnabled.value = false
            return
        }
        _weblioAutoAddNoteLevelEnabled.value = enabled
        prefs.edit().putBoolean("weblio_auto_add_note_level_enabled", enabled).apply()
        if (enabled) {
            val currentWord = _addWord.value.trim()
            if (currentWord.isNotBlank() && _addNote.value.isBlank()) {
                triggerWeblioLookup(force = true)
            }
        } else {
            if (lastAutoFilledNote != null && _addNote.value == lastAutoFilledNote) {
                _addNote.value = ""
                lastAutoFilledNote = null
            }
        }
    }

    // Weblio Classical Conjugation (活用の種類) Auto-fill into Note (Classical mode only)
    private val _weblioAutoAddConjugationEnabled = MutableStateFlow(
        if (_learningMode.value.isClassical) {
            prefs.getBoolean(modePrefKey("weblio_auto_add_conjugation_enabled"), true)
        } else {
            false
        }
    )
    val weblioAutoAddConjugationEnabled: StateFlow<Boolean> = _weblioAutoAddConjugationEnabled.asStateFlow()

    fun setWeblioAutoAddConjugationEnabled(enabled: Boolean) {
        val classicalKey = resolvePrefKey(
            "weblio_auto_add_conjugation_enabled",
            com.example.data.model.LearningMode.CLASSICAL_JAPANESE
        )
        prefs.edit().putBoolean(classicalKey, enabled).apply()
        if (!_learningMode.value.isClassical) {
            _weblioAutoAddConjugationEnabled.value = false
            return
        }
        _weblioAutoAddConjugationEnabled.value = enabled
        if (enabled) {
            val currentWord = _addWord.value.trim()
            if (currentWord.isNotBlank() && _addNote.value.isBlank()) {
                triggerWeblioLookup(force = true)
            }
        } else {
            if (lastAutoFilledNote != null && _addNote.value == lastAutoFilledNote) {
                _addNote.value = ""
                lastAutoFilledNote = null
            }
        }
    }

    // OCR Auto Exclude Already Registered Words (mode-scoped)
    private val _ocrExcludeRegisteredWords = MutableStateFlow(
        prefs.getBoolean(modePrefKey("ocr_exclude_registered_words"), true)
    )
    val ocrExcludeRegisteredWords: StateFlow<Boolean> = _ocrExcludeRegisteredWords.asStateFlow()

    fun setOcrExcludeRegisteredWords(enabled: Boolean) {
        _ocrExcludeRegisteredWords.value = enabled
        prefs.edit().putBoolean(modePrefKey("ocr_exclude_registered_words"), enabled).apply()
    }

    // OCR Auto Lookup Meaning for Photos/Camera (mode-scoped)
    private val _ocrAutoLookupMeaningEnabled = MutableStateFlow(
        prefs.getBoolean(modePrefKey("ocr_auto_lookup_meaning_enabled"), true)
    )
    val ocrAutoLookupMeaningEnabled: StateFlow<Boolean> = _ocrAutoLookupMeaningEnabled.asStateFlow()

    fun setOcrAutoLookupMeaningEnabled(enabled: Boolean) {
        _ocrAutoLookupMeaningEnabled.value = enabled
        prefs.edit().putBoolean(modePrefKey("ocr_auto_lookup_meaning_enabled"), enabled).apply()
    }

    // OCR Extraction Sensitivity (mode-scoped)
    private val _ocrSensitivity = MutableStateFlow(
        OcrSensitivity.fromId(prefs.getString(modePrefKey("ocr_sensitivity"), OcrSensitivity.HIGH.id))
    )
    val ocrSensitivity: StateFlow<OcrSensitivity> = _ocrSensitivity.asStateFlow()

    fun setOcrSensitivity(sensitivity: OcrSensitivity) {
        _ocrSensitivity.value = sensitivity
        prefs.edit().putString(modePrefKey("ocr_sensitivity"), sensitivity.id).apply()
    }

    // Settings (mode-scoped)
    private val _themeColor = MutableStateFlow(
        AppThemeColor.entries.find {
            it.name == prefs.getString(modePrefKey("theme_color"), AppThemeColor.BLUE_LIGHT.name)
        } ?: AppThemeColor.BLUE_LIGHT
    )
    val themeColor: StateFlow<AppThemeColor> = _themeColor.asStateFlow()

    private val _cardBackTheme = MutableStateFlow(
        CardBackTheme.entries.find {
            it.name == prefs.getString(modePrefKey("card_back_theme"), CardBackTheme.BLUE_GRADIENT.name)
        } ?: CardBackTheme.BLUE_GRADIENT
    )
    val cardBackTheme: StateFlow<CardBackTheme> = _cardBackTheme.asStateFlow()

    private val _fontFamily = MutableStateFlow(
        AppFontFamily.entries.find {
            it.name == prefs.getString(modePrefKey("font_family"), AppFontFamily.SYSTEM.name)
        } ?: AppFontFamily.SYSTEM
    )
    val fontFamily: StateFlow<AppFontFamily> = _fontFamily.asStateFlow()

    private val _ttsEnabled = MutableStateFlow(prefs.getBoolean("tts_enabled", true))
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled.asStateFlow()

    private val _autoSpeakOnCardChange = MutableStateFlow(prefs.getBoolean("auto_speak", false))
    val autoSpeakOnCardChange: StateFlow<Boolean> = _autoSpeakOnCardChange.asStateFlow()

    private val _speechRate = MutableStateFlow(prefs.getFloat("speech_rate", 1.0f))
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(prefs.getFloat("speech_pitch", 1.0f))
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    fun setThemeColor(color: AppThemeColor) {
        _themeColor.value = color
        prefs.edit().putString(modePrefKey("theme_color"), color.name).apply()
    }

    fun setCardBackTheme(theme: CardBackTheme) {
        _cardBackTheme.value = theme
        prefs.edit().putString(modePrefKey("card_back_theme"), theme.name).apply()
    }

    fun setFontFamily(font: AppFontFamily) {
        _fontFamily.value = font
        prefs.edit().putString(modePrefKey("font_family"), font.name).apply()
    }

    fun setTtsEnabled(enabled: Boolean) {
        _ttsEnabled.value = enabled
        prefs.edit().putBoolean("tts_enabled", enabled).apply()
    }

    fun setAutoSpeakOnCardChange(enabled: Boolean) {
        _autoSpeakOnCardChange.value = enabled
        prefs.edit().putBoolean("auto_speak", enabled).apply()
    }

    fun setSpeechRate(rate: Float) {
        val clamped = (Math.round(rate * 10f) / 10f).coerceIn(0.5f, 1.5f)
        _speechRate.value = clamped
        prefs.edit().putFloat("speech_rate", clamped).apply()
    }

    fun setSpeechPitch(pitch: Float) {
        val clamped = (Math.round(pitch * 10f) / 10f).coerceIn(0.5f, 1.5f)
        _speechPitch.value = clamped
        prefs.edit().putFloat("speech_pitch", clamped).apply()
    }

    // Auto Uncheck on Consecutive Perfect (mode-scoped)
    private val _autoUncheckPerfectCount = MutableStateFlow(
        prefs.getInt(modePrefKey("auto_uncheck_perfect_count"), 0)
    )
    val autoUncheckPerfectCount: StateFlow<Int> = _autoUncheckPerfectCount.asStateFlow()

    private val _autoUncheckAction = MutableStateFlow(
        AutoUncheckAction.entries.find {
            it.name == prefs.getString(modePrefKey("auto_uncheck_action"), AutoUncheckAction.DEFAULT.name)
        } ?: AutoUncheckAction.DEFAULT
    )
    val autoUncheckAction: StateFlow<AutoUncheckAction> = _autoUncheckAction.asStateFlow()

    private val _lastAutoUncheckMessage = MutableStateFlow<String?>(null)
    val lastAutoUncheckMessage: StateFlow<String?> = _lastAutoUncheckMessage.asStateFlow()

    fun clearAutoUncheckMessage() {
        _lastAutoUncheckMessage.value = null
    }

    fun setAutoUncheckPerfectCount(count: Int) {
        val safeCount = count.coerceAtLeast(0)
        _autoUncheckPerfectCount.value = safeCount
        prefs.edit().putInt(modePrefKey("auto_uncheck_perfect_count"), safeCount).apply()
    }

    fun setAutoUncheckAction(action: AutoUncheckAction) {
        _autoUncheckAction.value = action
        prefs.edit().putString(modePrefKey("auto_uncheck_action"), action.name).apply()
    }

    // Feature ②: Quiz & Test Engine
    private val _quizState = MutableStateFlow(QuizState())
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    fun startQuiz(
        selectedDecks: Set<String> = emptySet(),
        selectedPos: Set<String> = emptySet(),
        questionCount: Int = 10,
        optionsCount: Int = 4,
        showPosInQuestion: Boolean = true,
        showPosInOptions: Boolean = true,
        type: QuizType = QuizType.MULTIPLE_CHOICE
    ) {
        val effectiveType = if (_learningMode.value.isClassical) QuizType.MULTIPLE_CHOICE else type
        val allAvailable = allCards.value.filter { !it.isHidden }
        val pool = allAvailable.filter { card ->
            (selectedDecks.isEmpty() || selectedDecks.contains(card.deck)) &&
            (selectedPos.isEmpty() || card.definitions.any { d -> selectedPos.contains(d.pos.trim()) })
        }
        if (pool.isEmpty()) return

        val shuffledPool = pool.shuffled()
        val targetCards = shuffledPool.take(questionCount)

        val decoyTargetCount = (optionsCount - 1).coerceIn(1, 9)

        fun formatDefList(defs: List<Definition>): String {
            return if (showPosInOptions) {
                defs.joinToString(" / ") { def ->
                    if (def.pos.isNotBlank()) "[${def.pos}] ${def.meaning}" else def.meaning
                }
            } else {
                defs.joinToString(" / ") { it.meaning }
            }
        }

        val questions = targetCards.map { card ->
            // Determine the target part of speech for this card
            val targetPos = if (selectedPos.isNotEmpty()) {
                card.definitions.firstOrNull { selectedPos.contains(it.pos.trim()) }?.pos?.trim()
                    ?: card.definitions.firstOrNull { it.pos.isNotBlank() }?.pos?.trim()
                    ?: ""
            } else {
                card.definitions.firstOrNull { it.pos.isNotBlank() }?.pos?.trim() ?: ""
            }

            val targetDefs = if (targetPos.isNotBlank()) {
                card.definitions.filter { it.pos.trim() == targetPos }.ifEmpty { card.definitions }
            } else {
                card.definitions
            }

            val correctMeaning = formatDefList(targetDefs)

            // 選択肢はどのデッキを選んだかによらず、全範囲から同一の品詞のものを選定
            val allCandidates = allAvailable.filter { it.id != card.id }.shuffled()

            val (promptText, answerText, optionsList) = if (effectiveType == QuizType.MULTIPLE_CHOICE_EN) {
                // 日本語の意味が問題文、英単語が正解および選択肢
                val prompt = correctMeaning
                val answer = card.word

                val samePosEnglishDecoys = if (targetPos.isNotBlank()) {
                    allCandidates
                        .filter { c -> c.definitions.any { it.pos.trim() == targetPos } }
                        .map { it.word.trim() }
                        .filter { it.isNotBlank() && !it.equals(answer.trim(), ignoreCase = true) }
                        .distinct()
                } else {
                    emptyList()
                }

                val fallbackEnglishDecoys = allCandidates
                    .map { it.word.trim() }
                    .filter { it.isNotBlank() && !it.equals(answer.trim(), ignoreCase = true) && it !in samePosEnglishDecoys }
                    .distinct()

                val decoys = (samePosEnglishDecoys + fallbackEnglishDecoys).take(decoyTargetCount)
                val options = (decoys + answer).shuffled()
                Triple(prompt, answer, options)
            } else {
                // MULTIPLE_CHOICE (英単語が問題文、意味が正解および選択肢) または SPELLING
                val samePosDecoys = if (targetPos.isNotBlank()) {
                    allCandidates
                        .filter { c -> c.definitions.any { it.pos.trim() == targetPos } }
                        .map { c ->
                            val matchingDefs = c.definitions.filter { it.pos.trim() == targetPos }
                            formatDefList(matchingDefs)
                        }
                        .filter { it.isNotBlank() && it != correctMeaning }
                        .distinct()
                } else {
                    emptyList()
                }

                val fallbackDecoys = allCandidates
                    .map { c -> formatDefList(c.definitions) }
                    .filter { it.isNotBlank() && it != correctMeaning && it !in samePosDecoys }
                    .distinct()

                val decoys = (samePosDecoys + fallbackDecoys).take(decoyTargetCount)
                val options = (decoys + correctMeaning).shuffled()
                Triple(card.word, correctMeaning, options)
            }

            QuizQuestion(
                card = card,
                prompt = promptText,
                correctAnswer = answerText,
                options = optionsList,
                partOfSpeech = targetPos.ifBlank { card.definitions.firstOrNull()?.pos ?: "" },
                note = card.note,
                showPosInQuestion = showPosInQuestion,
                showPosInOptions = showPosInOptions
            )
        }

        _quizState.value = QuizState(
            isRunning = true,
            isFinished = false,
            quizType = effectiveType,
            selectedDecks = selectedDecks,
            selectedPos = selectedPos,
            optionsCount = optionsCount,
            showPosInQuestion = showPosInQuestion,
            showPosInOptions = showPosInOptions,
            questions = questions,
            currentIndex = 0,
            selectedAnswerIndex = null,
            typedSpellingText = "",
            isSubmitted = false,
            isAnswerCorrect = false,
            hintRevealed = false,
            results = emptyList(),
            score = 0
        )
    }

    fun startQuiz(deck: String? = null, questionCount: Int = 10, type: QuizType = QuizType.MULTIPLE_CHOICE) {
        startQuiz(
            selectedDecks = if (deck != null) setOf(deck) else emptySet(),
            selectedPos = emptySet(),
            questionCount = questionCount,
            optionsCount = 4,
            showPosInQuestion = true,
            showPosInOptions = true,
            type = type
        )
    }

    fun selectQuizAnswer(index: Int) {
        val current = _quizState.value
        if (!current.isRunning || current.isSubmitted) return
        val question = current.currentQuestion ?: return
        val selectedOption = question.options.getOrNull(index) ?: return
        val isCorrect = selectedOption == question.correctAnswer

        val newResults = current.results + QuizQuestionResult(question, selectedOption, isCorrect)
        val newScore = if (isCorrect) current.score + 1 else current.score

        _quizState.value = current.copy(
            selectedAnswerIndex = index,
            isSubmitted = true,
            isAnswerCorrect = isCorrect,
            results = newResults,
            score = newScore
        )
    }

    fun setTypedSpelling(text: String) {
        _quizState.value = _quizState.value.copy(typedSpellingText = text)
    }

    fun revealSpellingHint() {
        _quizState.value = _quizState.value.copy(hintRevealed = true)
    }

    fun submitSpellingAnswer() {
        val current = _quizState.value
        if (!current.isRunning || current.isSubmitted) return
        val question = current.currentQuestion ?: return
        val isCorrect = current.typedSpellingText.trim().equals(question.card.word.trim(), ignoreCase = true)

        val newResults = current.results + QuizQuestionResult(question, current.typedSpellingText.trim(), isCorrect)
        val newScore = if (isCorrect) current.score + 1 else current.score

        _quizState.value = current.copy(
            isSubmitted = true,
            isAnswerCorrect = isCorrect,
            results = newResults,
            score = newScore
        )
    }

    fun nextQuizQuestion() {
        val current = _quizState.value
        if (!current.isRunning) return
        if (current.currentIndex + 1 < current.questions.size) {
            _quizState.value = current.copy(
                currentIndex = current.currentIndex + 1,
                selectedAnswerIndex = null,
                typedSpellingText = "",
                isSubmitted = false,
                isAnswerCorrect = false,
                hintRevealed = false
            )
        } else {
            _quizState.value = current.copy(
                isFinished = true,
                isRunning = false
            )
        }
    }

    fun retryIncorrectQuizQuestions() {
        val current = _quizState.value
        val incorrectCards = current.results.filter { !it.isCorrect }.map { it.question.card }
        if (incorrectCards.isEmpty()) return

        val allAvailable = allCards.value.filter { !it.isHidden }
        val decoyTargetCount = (current.optionsCount - 1).coerceIn(1, 9)

        fun formatDefList(defs: List<Definition>): String {
            return if (current.showPosInOptions) {
                defs.joinToString(" / ") { def ->
                    if (def.pos.isNotBlank()) "[${def.pos}] ${def.meaning}" else def.meaning
                }
            } else {
                defs.joinToString(" / ") { it.meaning }
            }
        }

        val questions = incorrectCards.map { card ->
            val targetPos = if (current.selectedPos.isNotEmpty()) {
                card.definitions.firstOrNull { current.selectedPos.contains(it.pos.trim()) }?.pos?.trim()
                    ?: card.definitions.firstOrNull { it.pos.isNotBlank() }?.pos?.trim()
                    ?: ""
            } else {
                card.definitions.firstOrNull { it.pos.isNotBlank() }?.pos?.trim() ?: ""
            }

            val targetDefs = if (targetPos.isNotBlank()) {
                card.definitions.filter { it.pos.trim() == targetPos }.ifEmpty { card.definitions }
            } else {
                card.definitions
            }

            val correctMeaning = formatDefList(targetDefs)

            val allCandidates = allAvailable.filter { it.id != card.id }.shuffled()

            val (promptText, answerText, optionsList) = if (current.quizType == QuizType.MULTIPLE_CHOICE_EN) {
                val prompt = correctMeaning
                val answer = card.word

                val samePosEnglishDecoys = if (targetPos.isNotBlank()) {
                    allCandidates
                        .filter { c -> c.definitions.any { it.pos.trim() == targetPos } }
                        .map { it.word.trim() }
                        .filter { it.isNotBlank() && !it.equals(answer.trim(), ignoreCase = true) }
                        .distinct()
                } else {
                    emptyList()
                }

                val fallbackEnglishDecoys = allCandidates
                    .map { it.word.trim() }
                    .filter { it.isNotBlank() && !it.equals(answer.trim(), ignoreCase = true) && it !in samePosEnglishDecoys }
                    .distinct()

                val decoys = (samePosEnglishDecoys + fallbackEnglishDecoys).take(decoyTargetCount)
                val options = (decoys + answer).shuffled()
                Triple(prompt, answer, options)
            } else {
                val samePosDecoys = if (targetPos.isNotBlank()) {
                    allCandidates
                        .filter { c -> c.definitions.any { it.pos.trim() == targetPos } }
                        .map { c ->
                            val matchingDefs = c.definitions.filter { it.pos.trim() == targetPos }
                            formatDefList(matchingDefs)
                        }
                        .filter { it.isNotBlank() && it != correctMeaning }
                        .distinct()
                } else {
                    emptyList()
                }

                val fallbackDecoys = allCandidates
                    .map { c -> formatDefList(c.definitions) }
                    .filter { it.isNotBlank() && it != correctMeaning && it !in samePosDecoys }
                    .distinct()

                val decoys = (samePosDecoys + fallbackDecoys).take(decoyTargetCount)
                val options = (decoys + correctMeaning).shuffled()
                Triple(card.word, correctMeaning, options)
            }

            QuizQuestion(
                card = card,
                prompt = promptText,
                correctAnswer = answerText,
                options = optionsList,
                partOfSpeech = targetPos.ifBlank { card.definitions.firstOrNull()?.pos ?: "" },
                note = card.note,
                showPosInQuestion = current.showPosInQuestion,
                showPosInOptions = current.showPosInOptions
            )
        }

        _quizState.value = QuizState(
            isRunning = true,
            isFinished = false,
            quizType = current.quizType,
            selectedDecks = current.selectedDecks,
            selectedPos = current.selectedPos,
            optionsCount = current.optionsCount,
            showPosInQuestion = current.showPosInQuestion,
            showPosInOptions = current.showPosInOptions,
            questions = questions,
            currentIndex = 0,
            selectedAnswerIndex = null,
            typedSpellingText = "",
            isSubmitted = false,
            isAnswerCorrect = false,
            hintRevealed = false,
            results = emptyList(),
            score = 0
        )
    }

    fun resetQuiz() {
        _quizState.value = QuizState()
    }

    fun applyQuizResultsToSrs() {
        val current = _quizState.value
        viewModelScope.launch {
            for (res in current.results) {
                val rating = if (res.isCorrect) "○" else "×"
                repository.updateSrsRating(res.question.card.id, rating)
            }
        }
    }

    // Feature ④: OCR & Bulk Text Extraction
    fun processImageUriForOcr(
        context: Context,
        uri: Uri,
        sensitivity: OcrSensitivity = _ocrSensitivity.value,
        onResult: (List<String>) -> Unit
    ) {
        val isClassical = _learningMode.value.isClassical
        viewModelScope.launch {
            val res = OcrExtractor.extractTextFromImageUri(
                context = context,
                imageUri = uri,
                isClassical = isClassical,
                sensitivity = sensitivity
            )
            res.onSuccess { rawText ->
                if (isClassical) {
                    val candidates = OcrExtractor.extractClassicalWords(rawText, sensitivity)
                    if (candidates.isNotEmpty()) {
                        val refined = refineClassicalOcrCandidatesWithWeblio(candidates, sensitivity)
                        onResult(refined)
                    } else {
                        onResult(emptyList())
                    }
                } else {
                    val words = OcrExtractor.extractMeaningfulWords(
                        rawText = rawText,
                        sensitivity = sensitivity,
                        context = context
                    )
                    onResult(words)
                    // Politely pre-warm Weblio Japanese definitions in background without dropping any valid lexicon word
                    if (words.isNotEmpty() && (_ocrAutoLookupMeaningEnabled.value || _weblioAutoFillEnabled.value)) {
                        launch(Dispatchers.IO) {
                            for (batch in words.take(18).chunked(3)) {
                                batch.map { w ->
                                    async {
                                        if (com.example.data.model.WordKnowledgeBase.lookup(w) == null) {
                                            WeblioDictionaryService.lookupWord(w)
                                        }
                                    }
                                }.awaitAll()
                                delay(90)
                            }
                        }
                    }
                }
            }.onFailure {
                onResult(emptyList())
            }
        }
    }

    /**
     * Verifies Classical Japanese OCR candidates against [com.example.data.model.ClassicalWordKnowledgeBase]
     * and Weblio古語辞典 (`kobun.weblio.jp`):
     * - Resolves ~420+ core classical headwords + ~200 Kanji forms & de-inflections locally in 0ms without network calls.
     * - Verifies remaining open candidates politely against Weblio古語辞典 (`chunked(4)`).
     * - Respects [sensitivity] for how aggressively edge/checkbox artifacts are stripped during candidate rescue.
     */
    private suspend fun refineClassicalOcrCandidatesWithWeblio(
        candidates: List<String>,
        sensitivity: OcrSensitivity = _ocrSensitivity.value
    ): List<String> = coroutineScope {
        val resolvedSlots = arrayOfNulls<String>(candidates.size)
        val pendingIndices = mutableListOf<Int>()

        fun rescueVariantsFor(token: String): List<String> {
            return if (sensitivity == OcrSensitivity.LOW) {
                if (OcrExtractor.trimClassicalEdgeNoise(token) != token) {
                    emptyList()
                } else {
                    OcrExtractor.deinflectClassicalWord(token).take(6)
                }
            } else {
                buildClassicalRescueVariants(token)
            }
        }

        // Pass 1: Instant local verification & de-inflection against ClassicalWordKnowledgeBase (0ms, zero network requests)
        for (i in candidates.indices) {
            val token = candidates[i]
            val localDirect = com.example.data.model.ClassicalWordKnowledgeBase.lookup(token)
            if (localDirect != null && localDirect.definitions.isNotEmpty()) {
                resolvedSlots[i] = localDirect.word
                continue
            }

            val rescueVariants = rescueVariantsFor(token)
            val localRescued = rescueVariants.firstNotNullOfOrNull { variant ->
                com.example.data.model.ClassicalWordKnowledgeBase.lookup(variant)?.takeIf { it.definitions.isNotEmpty() }
            }
            if (localRescued != null) {
                resolvedSlots[i] = localRescued.word
                continue
            }

            pendingIndices.add(i)
        }

        // Pass 2: Polite Weblio古語辞典 verification for open candidates not in local KB
        val toQueryOnWeblio = pendingIndices.take(48)
        val overflowIndices = if (pendingIndices.size > 48) pendingIndices.drop(48) else emptyList()

        for (batch in toQueryOnWeblio.chunked(4)) {
            batch.map { idx ->
                async(Dispatchers.IO) {
                    val token = candidates[idx]
                    when (WeblioDictionaryService.verifyClassicalWordExistence(token)) {
                        WeblioVerifyStatus.FOUND -> {
                            resolvedSlots[idx] = token
                        }
                        WeblioVerifyStatus.TRANSIENT_ERROR -> {
                            if (OcrExtractor.isCleanStandaloneClassicalWord(token)) {
                                resolvedSlots[idx] = token
                            }
                        }
                        WeblioVerifyStatus.NOT_FOUND -> {
                            val rescueVariants = rescueVariantsFor(token).take(4)
                            var rescuedWord: String? = null
                            var sawTransientOnVariant = false
                            for (variant in rescueVariants) {
                                when (WeblioDictionaryService.verifyClassicalWordExistence(variant)) {
                                    WeblioVerifyStatus.FOUND -> {
                                        rescuedWord = variant
                                        break
                                    }
                                    WeblioVerifyStatus.TRANSIENT_ERROR -> {
                                        sawTransientOnVariant = true
                                    }
                                    WeblioVerifyStatus.NOT_FOUND -> Unit
                                }
                            }
                            if (rescuedWord != null) {
                                resolvedSlots[idx] = rescuedWord
                            } else if (sawTransientOnVariant && OcrExtractor.isCleanStandaloneClassicalWord(token)) {
                                resolvedSlots[idx] = token
                            }
                        }
                    }
                }
            }.awaitAll()
        }

        for (idx in overflowIndices) {
            val extra = candidates[idx]
            if (!WeblioDictionaryService.lastNetworkReachable && OcrExtractor.isCleanStandaloneClassicalWord(extra)) {
                resolvedSlots[idx] = extra
            }
        }

        resolvedSlots.filterNotNull().distinct()
    }

    internal fun buildClassicalRescueVariants(token: String): List<String> {
        val variants = LinkedHashSet<String>()
        val trimmed = OcrExtractor.trimClassicalEdgeNoise(token)
        if (trimmed != token && trimmed.length >= 2) {
            variants.add(trimmed)
        }
        // De-inflected citation candidates (e.g., 驚きて -> 驚く, 悲しく -> 悲し, 静かなる -> 静かなり)
        variants.addAll(OcrExtractor.deinflectClassicalWord(trimmed))

        if (trimmed.length >= 3 && trimmed.first() in "はがをにへともぞやでの口ロ日回レノ一") {
            val sub = trimmed.substring(1)
            variants.add(sub)
            variants.addAll(OcrExtractor.deinflectClassicalWord(sub))
        }
        if (trimmed.length >= 3 && trimmed.last() in "はがをにへともぞやで口ロ日回レノ一") {
            val sub = trimmed.dropLast(1)
            variants.add(sub)
            variants.addAll(OcrExtractor.deinflectClassicalWord(sub))
        }
        variants.remove(token)
        return variants.filter { it.length >= 2 }.take(10)
    }

    fun extractWordsFromRawText(text: String, sensitivity: OcrSensitivity = _ocrSensitivity.value): List<String> {
        return if (_learningMode.value.isClassical) {
            OcrExtractor.extractClassicalWords(text, sensitivity)
        } else {
            OcrExtractor.extractMeaningfulWords(
                rawText = text,
                sensitivity = sensitivity,
                context = getApplication()
            )
        }
    }

    fun bulkImportWords(words: List<String>, deck: String, onComplete: (Int) -> Unit) {
        val isClassical = _learningMode.value.isClassical
        viewModelScope.launch {
            val count = repository.bulkImportWords(
                words = words,
                targetDeck = deck,
                enableWeblioFallback = _weblioAutoFillEnabled.value || _ocrAutoLookupMeaningEnabled.value,
                enableWeblioNoteLevel = if (isClassical) false else _weblioAutoAddNoteLevelEnabled.value,
                enableWeblioClassicalConjugation = if (isClassical) _weblioAutoAddConjugationEnabled.value else false,
                subject = _learningMode.value.id
            )
            onComplete(count)
        }
    }

    fun insertCards(cards: List<FlashcardEntity>, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val currentSubject = _learningMode.value.id
            val prepared = cards.map { card ->
                val withSubject = if (card.subject.isBlank() || card.subject == "ENGLISH") {
                    card.copy(subject = currentSubject)
                } else {
                    card
                }
                if (withSubject.subject.equals("CLASSICAL", ignoreCase = true)) {
                    val extractedConjs = mutableListOf<String>()
                    val simpleDefs = withSubject.definitions.map { def ->
                        val (simplePos, conj) = ClassicalPosNormalizer.splitPosAndConjugation(def.pos)
                        if (conj.isNotBlank()) extractedConjs.add(conj)
                        def.copy(pos = simplePos)
                    }
                    val mergedNote = ClassicalPosNormalizer.mergeConjugationIntoNote(extractedConjs, withSubject.note)
                    withSubject.copy(definitions = simpleDefs, note = mergedNote)
                } else {
                    withSubject
                }
            }
            val count = repository.insertCards(prepared)
            onComplete(count)
        }
    }

    data class CandidateLookupDetails(
        val definitions: List<Definition>,
        val note: String = ""
    )

    suspend fun lookupCandidateDetails(word: String, forceWeblio: Boolean = false): CandidateLookupDetails {
        val cleanWord = word.trim()
        val isClassical = _learningMode.value.isClassical
        val useInternalFirst = _internalDictionaryEnabled.value && _dictionaryPriority.value == DictionaryPriority.INTERNAL_FIRST
        val allowWeblio = forceWeblio || _weblioAutoFillEnabled.value || _ocrAutoLookupMeaningEnabled.value

        if (useInternalFirst) {
            val internal = InternalDictionaryService.lookupWord(cleanWord, isClassical)
            if (internal != null && internal.definitions.isNotEmpty()) {
                val note = if (isClassical) {
                    if (_weblioAutoAddConjugationEnabled.value) internal.conjugation else ""
                } else {
                    if (_weblioAutoAddNoteLevelEnabled.value) internal.note else ""
                }
                return CandidateLookupDetails(definitions = internal.definitions, note = note)
            }
        }

        if (allowWeblio) {
            if (isClassical) {
                val weblio = WeblioDictionaryService.lookupClassicalWord(cleanWord)
                if (weblio != null && weblio.definitions.isNotEmpty()) {
                    val note = if (_weblioAutoAddConjugationEnabled.value) weblio.conjugation else ""
                    return CandidateLookupDetails(definitions = weblio.definitions, note = note)
                }
            } else {
                val weblio = WeblioDictionaryService.lookupWord(cleanWord.lowercase())
                if (weblio != null && weblio.definitions.isNotEmpty()) {
                    val note = if (_weblioAutoAddNoteLevelEnabled.value) weblio.formattedNote else ""
                    return CandidateLookupDetails(definitions = weblio.definitions, note = note)
                }
            }
        }

        if (_internalDictionaryEnabled.value && !useInternalFirst) {
            val internal = InternalDictionaryService.lookupWord(cleanWord, isClassical)
            if (internal != null && internal.definitions.isNotEmpty()) {
                val note = if (isClassical) {
                    if (_weblioAutoAddConjugationEnabled.value) internal.conjugation else ""
                } else {
                    if (_weblioAutoAddNoteLevelEnabled.value) internal.note else ""
                }
                return CandidateLookupDetails(definitions = internal.definitions, note = note)
            }
        }

        val defaultPos = if (isClassical) "動詞" else "名詞"
        return CandidateLookupDetails(definitions = listOf(Definition(pos = defaultPos, meaning = "")), note = "")
    }

    suspend fun lookupDefinitionForCandidate(word: String): List<Definition> {
        return lookupCandidateDetails(word).definitions
    }

    fun updateBatchDeck(ids: List<Long>, deck: String) {
        viewModelScope.launch {
            repository.updateBatchDeck(ids, deck)
        }
    }

    fun resetSettingsToDefaults() {
        setThemeColor(AppThemeColor.BLUE_LIGHT)
        setCardBackTheme(CardBackTheme.BLUE_GRADIENT)
        setFontFamily(AppFontFamily.DEFAULT)
        if (_isShuffle.value) {
            toggleShuffle()
        }
        setAutoUncheckPerfectCount(0)
        setAutoUncheckAction(AutoUncheckAction.DEFAULT)
        setDictionaryPriority(DictionaryPriority.DEFAULT)
        setInternalDictionaryEnabled(true)
        setWeblioAutoFillEnabled(true)
        setOcrExcludeRegisteredWords(true)
        setOcrAutoLookupMeaningEnabled(true)
        setOcrSensitivity(OcrSensitivity.HIGH)
        if (_learningMode.value.isClassical) {
            setWeblioAutoAddConjugationEnabled(true)
        } else {
            setTtsEnabled(true)
            setAutoSpeakOnCardChange(false)
            setSpeechRate(1.0f)
            setSpeechPitch(1.0f)
            setWeblioAutoAddNoteLevelEnabled(true)
        }
    }
}
