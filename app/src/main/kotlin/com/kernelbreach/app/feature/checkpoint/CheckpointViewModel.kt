package com.kernelbreach.app.feature.checkpoint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernelbreach.core.database.repo.CurriculumRepository
import com.kernelbreach.core.database.repo.ProgressRepository
import com.kernelbreach.core.database.repo.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import javax.inject.Inject

data class CpQuestion(
    val originalIndex: Int,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val why: String,
)

enum class CheckpointPhase { QUIZ, RESULT }

data class CheckpointState(
    val loading: Boolean = true,
    val moduleCode: String = "",
    val questions: List<CpQuestion> = emptyList(),
    val phase: CheckpointPhase = CheckpointPhase.QUIZ,
    val index: Int = 0,
    val selected: Int? = null,
    val revealed: Boolean = false,
    val answers: List<Int> = emptyList(), // chosen option per question, in shuffled order
    val correctCount: Int = 0,
) {
    val current: CpQuestion? get() = questions.getOrNull(index)
    val passed: Boolean get() = questions.isNotEmpty() && correctCount * 100 / questions.size >= PASS_PERCENT

    companion object {
        const val PASS_PERCENT = 80
    }
}

@HiltViewModel
class CheckpointViewModel @Inject constructor(
    private val curriculumRepository: CurriculumRepository,
    private val progressRepository: ProgressRepository,
    private val reviewRepository: ReviewRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CheckpointState())
    val state: StateFlow<CheckpointState> = _state.asStateFlow()

    fun load(moduleCode: String) {
        viewModelScope.launch {
            curriculumRepository.ensureImported()
            val module = curriculumRepository.module(moduleCode) ?: return@launch
            // Stored seed so the shuffle is stable across recompositions/rotations.
            val seed = System.nanoTime()
            val rnd = Random(seed)
            val shuffled = module.checkpoint.mapIndexed { i, q -> i to q }
                .shuffled(rnd)
                .map { (origIndex, q) ->
                    val optionOrder = q.options.indices.shuffled(rnd)
                    val options = optionOrder.map { q.options[it] }
                    CpQuestion(
                        originalIndex = origIndex,
                        prompt = q.prompt,
                        options = options,
                        correctIndex = optionOrder.indexOf(q.answerIndex),
                        why = q.explanation,
                    )
                }
            _state.update { it.copy(loading = false, moduleCode = moduleCode, questions = shuffled) }
        }
    }

    fun answer(option: Int) {
        if (_state.value.revealed) return
        val q = _state.value.current ?: return
        val correct = option == q.correctIndex
        _state.update {
            it.copy(
                selected = option,
                revealed = true,
                answers = it.answers + option,
                correctCount = it.correctCount + if (correct) 1 else 0,
            )
        }
    }

    fun next() {
        val s = _state.value
        if (s.index + 1 < s.questions.size) {
            _state.update { it.copy(index = it.index + 1, selected = null, revealed = false) }
        } else {
            finishAttempt()
        }
    }

    private fun finishAttempt() {
        val s = _state.value
        viewModelScope.launch {
            progressRepository.recordCheckpoint(
                moduleCode = s.moduleCode,
                score = s.correctCount,
                total = s.questions.size,
                passed = s.passed,
                answers = s.answers,
            )
            // Feed misses into review items.
            s.questions.forEachIndexed { i, q ->
                if (s.answers.getOrNull(i) != q.correctIndex) {
                    reviewRepository.onCheckpointMiss(s.moduleCode, "${s.moduleCode}#CP${q.originalIndex}")
                }
            }
            _state.update { it.copy(phase = CheckpointPhase.RESULT) }
        }
    }

    fun retake(moduleCode: String) {
        _state.value = CheckpointState()
        load(moduleCode)
    }
}
