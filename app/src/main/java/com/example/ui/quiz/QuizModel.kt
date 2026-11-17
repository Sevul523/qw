package com.example.ui.quiz

import com.example.data.local.FlashcardEntity

enum class QuizType(val label: String) {
    MULTIPLE_CHOICE("意味当て（英→日）"),
    MULTIPLE_CHOICE_EN("英単語当て（日→英）"),
    SPELLING("スペル入力")
}

data class QuizQuestion(
    val card: FlashcardEntity,
    val prompt: String,
    val correctAnswer: String,
    val options: List<String> = emptyList(),
    val partOfSpeech: String = "",
    val note: String = "",
    val showPosInQuestion: Boolean = true,
    val showPosInOptions: Boolean = true
)

data class QuizQuestionResult(
    val question: QuizQuestion,
    val userAnswer: String,
    val isCorrect: Boolean
)

data class QuizState(
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val quizType: QuizType = QuizType.MULTIPLE_CHOICE,
    val selectedDecks: Set<String> = emptySet(),
    val selectedPos: Set<String> = emptySet(),
    val optionsCount: Int = 4,
    val showPosInQuestion: Boolean = true,
    val showPosInOptions: Boolean = true,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswerIndex: Int? = null,
    val typedSpellingText: String = "",
    val isSubmitted: Boolean = false,
    val isAnswerCorrect: Boolean = false,
    val hintRevealed: Boolean = false,
    val results: List<QuizQuestionResult> = emptyList(),
    val score: Int = 0
) {
    val selectedDeck: String?
        get() = selectedDecks.singleOrNull()

    val currentQuestion: QuizQuestion?
        get() = questions.getOrNull(currentIndex)

    val progress: Float
        get() = if (questions.isEmpty()) 0f else (currentIndex + 1).toFloat() / questions.size.toFloat()

    val accuracyPercentage: Int
        get() = if (results.isEmpty()) 0 else (score * 100) / results.size
}
