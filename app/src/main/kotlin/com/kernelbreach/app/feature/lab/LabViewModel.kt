package com.kernelbreach.app.feature.lab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernelbreach.core.database.repo.CurriculumRepository
import com.kernelbreach.core.database.repo.ProgressRepository
import com.kernelbreach.core.model.Lab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LabUiState(
    val loading: Boolean = true,
    val lab: Lab? = null,
    val stepIndex: Int = 0,
    val revealedUpTo: Int = -1, // highest step index whose output is revealed
    val onFinal: Boolean = false,
    val hintShown: Boolean = false,
    val finding: String = "",
    val committed: Boolean = false,
) {
    val currentStepRevealed: Boolean get() = revealedUpTo >= stepIndex
}

@HiltViewModel
class LabViewModel @Inject constructor(
    private val curriculumRepository: CurriculumRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LabUiState())
    val state: StateFlow<LabUiState> = _state.asStateFlow()

    fun load(labId: String) {
        viewModelScope.launch {
            curriculumRepository.ensureImported()
            val lab = curriculumRepository.lab(labId)
            val progress = progressRepository.labProgress(labId)
            val resumeIndex = progress?.stepIndex?.coerceIn(0, (lab?.steps?.size ?: 1) - 1) ?: 0
            _state.update {
                it.copy(
                    loading = false,
                    lab = lab,
                    stepIndex = resumeIndex,
                    revealedUpTo = resumeIndex - 1,
                )
            }
        }
    }

    fun revealCurrent() {
        _state.update { it.copy(revealedUpTo = maxOf(it.revealedUpTo, it.stepIndex)) }
    }

    fun next() {
        val s = _state.value
        val lab = s.lab ?: return
        if (s.stepIndex + 1 < lab.steps.size) {
            val newIndex = s.stepIndex + 1
            _state.update { it.copy(stepIndex = newIndex) }
            viewModelScope.launch { progressRepository.saveLabProgress(lab.id, newIndex, completed = false) }
        } else {
            _state.update { it.copy(onFinal = true) }
        }
    }

    fun updateFinding(text: String) = _state.update { it.copy(finding = text) }
    fun toggleHint() = _state.update { it.copy(hintShown = !it.hintShown) }

    fun commitFinding() {
        val lab = _state.value.lab ?: return
        _state.update { it.copy(committed = true) }
        viewModelScope.launch {
            progressRepository.saveLabProgress(lab.id, lab.steps.size - 1, completed = true)
        }
    }

    companion object {
        /** Pull a runnable command out of a step instruction (backticks or "Run:"). */
        fun extractCommand(instruction: String): String? {
            val backtick = Regex("`([^`]+)`").find(instruction)?.groupValues?.getOrNull(1)
            if (!backtick.isNullOrBlank()) return backtick.trim()
            val runPrefix = Regex("Run:\\s*(.+)$", RegexOption.IGNORE_CASE).find(instruction)?.groupValues?.getOrNull(1)
            return runPrefix?.trim()
        }
    }
}
