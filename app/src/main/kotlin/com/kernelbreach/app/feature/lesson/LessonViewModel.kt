package com.kernelbreach.app.feature.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernelbreach.core.database.repo.CurriculumRepository
import com.kernelbreach.core.database.repo.ProgressRepository
import com.kernelbreach.core.database.repo.ReviewRepository
import com.kernelbreach.core.model.Confidence
import com.kernelbreach.core.model.Lesson
import com.kernelbreach.core.model.Question
import com.kernelbreach.core.model.Term
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LessonPhase { LEARN, TRY, COMPLETE }

data class LessonUiState(
    val loading: Boolean = true,
    val phase: LessonPhase = LessonPhase.LEARN,
    val lesson: Lesson? = null,
    val conceptNumber: Int = 0,
    val conceptCount: Int = 0,
    // Try phase
    val questionIndex: Int = 0,
    val selectedOption: Int? = null,
    val revealed: Boolean = false,
    val correctCount: Int = 0,
    // Term sheet
    val openTerm: Term? = null,
) {
    val currentQuestion: Question?
        get() = lesson?.check?.getOrNull(questionIndex)

    val scorePercent: Int
        get() {
            val total = lesson?.check?.size ?: return 0
            if (total == 0) return 0
            return (correctCount * 100) / total
        }
}

@HiltViewModel
class LessonViewModel @Inject constructor(
    private val curriculumRepository: CurriculumRepository,
    private val progressRepository: ProgressRepository,
    private val reviewRepository: ReviewRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LessonUiState())
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    fun load(lessonId: String) {
        viewModelScope.launch {
            curriculumRepository.ensureImported()
            val lesson = curriculumRepository.lesson(lessonId)
            val module = lesson?.let { curriculumRepository.module(it.moduleCode) }
            if (lesson != null) progressRepository.markLessonOpened(lessonId)
            _state.update {
                it.copy(
                    loading = false,
                    lesson = lesson,
                    conceptNumber = lesson?.index ?: 0,
                    conceptCount = module?.lessons?.size ?: 0,
                )
            }
        }
    }

    fun startTry() = _state.update { it.copy(phase = LessonPhase.TRY, questionIndex = 0, selectedOption = null, revealed = false) }

    fun answer(option: Int) {
        val q = _state.value.currentQuestion ?: return
        if (_state.value.revealed) return
        val correct = option == q.answerIndex
        _state.update {
            it.copy(
                selectedOption = option,
                revealed = true,
                correctCount = it.correctCount + if (correct) 1 else 0,
            )
        }
    }

    fun next() {
        val s = _state.value
        val lesson = s.lesson ?: return
        if (s.questionIndex + 1 < lesson.check.size) {
            _state.update { it.copy(questionIndex = it.questionIndex + 1, selectedOption = null, revealed = false) }
        } else {
            _state.update { it.copy(phase = LessonPhase.COMPLETE) }
        }
    }

    fun openTerm(term: String) {
        val match = _state.value.lesson?.terms?.firstOrNull { it.term.equals(term, ignoreCase = true) }
        viewModelScope.launch {
            val resolved = match ?: curriculumRepository.termDefinition(term)
            _state.update { it.copy(openTerm = resolved) }
        }
    }

    fun closeTerm() = _state.update { it.copy(openTerm = null) }

    fun finish(confidence: Confidence, onFinished: () -> Unit) {
        val lesson = _state.value.lesson ?: return onFinished()
        viewModelScope.launch {
            progressRepository.completeLesson(lesson.id, _state.value.scorePercent)
            reviewRepository.onLessonComplete(lesson, confidence)
            onFinished()
        }
    }
}
