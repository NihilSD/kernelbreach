package com.kernelbreach.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernelbreach.core.database.repo.CurriculumRepository
import com.kernelbreach.core.model.Term
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryState(
    val query: String = "",
    val results: List<Term> = emptyList(),
    val openTerm: Term? = null,
    val loading: Boolean = true,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val curriculumRepository: CurriculumRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            curriculumRepository.ensureImported()
            _state.update { it.copy(results = curriculumRepository.allTerms(), loading = false) }
        }
        viewModelScope.launch {
            queryFlow.debounce(180).collect { q ->
                val results = if (q.isBlank()) {
                    curriculumRepository.allTerms()
                } else {
                    curriculumRepository.searchTerms(q)
                }
                _state.update { it.copy(results = results) }
            }
        }
    }

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
        queryFlow.value = q
    }

    fun open(term: Term) = _state.update { it.copy(openTerm = term) }
    fun close() = _state.update { it.copy(openTerm = null) }
}
