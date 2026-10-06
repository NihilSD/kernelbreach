package com.kernelbreach.app.feature.refresh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernelbreach.core.database.repo.CurriculumRepository
import com.kernelbreach.core.database.repo.ReviewRepository
import com.kernelbreach.core.model.ReviewGrade
import com.kernelbreach.core.srs.ReviewItem
import com.kernelbreach.core.srs.ReviewKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewCard(
    val itemId: String,
    val overline: String,
    val prompt: String,
    val answer: String,
)

data class RefreshState(
    val loading: Boolean = true,
    val cards: List<ReviewCard> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val done: Boolean = false,
) {
    val current: ReviewCard? get() = cards.getOrNull(index)
}

@HiltViewModel
class RefreshViewModel @Inject constructor(
    private val curriculumRepository: CurriculumRepository,
    private val reviewRepository: ReviewRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RefreshState())
    val state: StateFlow<RefreshState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            curriculumRepository.ensureImported()
            val due = reviewRepository.dueItems()
            val cards = due.mapNotNull { resolve(it) }
            _state.update { it.copy(loading = false, cards = cards, done = cards.isEmpty()) }
        }
    }

    private suspend fun resolve(item: ReviewItem): ReviewCard? = when (item.kind) {
        ReviewKind.TERM -> {
            val termName = item.id.substringAfter("#term#", "").ifEmpty { null }
            val term = termName?.let { curriculumRepository.termDefinition(it) }
            term?.let { ReviewCard(item.id, "Term", "What does \"${it.term}\" mean?", it.definition) }
        }

        ReviewKind.LESSON -> {
            val lesson = curriculumRepository.lesson(item.id)
            lesson?.let {
                ReviewCard(item.id, "Recall", "Key idea: ${it.title}", it.recap.joinToString("\n• ", prefix = "• "))
            }
        }

        ReviewKind.QUESTION -> {
            // id form: MODULE#CP<n>
            val module = curriculumRepository.module(item.moduleCode)
            val cpIndex = item.id.substringAfter("#CP", "").toIntOrNull()
            val q = cpIndex?.let { module?.checkpoint?.getOrNull(it) }
            q?.let { ReviewCard(item.id, "Question", it.prompt, it.answer) }
        }
    }

    fun reveal() = _state.update { it.copy(revealed = true) }

    fun grade(grade: ReviewGrade) {
        val card = _state.value.current ?: return
        viewModelScope.launch {
            reviewRepository.grade(card.itemId, grade)
            val nextIndex = _state.value.index + 1
            if (nextIndex < _state.value.cards.size) {
                _state.update { it.copy(index = nextIndex, revealed = false) }
            } else {
                _state.update { it.copy(done = true) }
            }
        }
    }
}
