package com.kernelbreach.app.feature.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernelbreach.core.database.repo.CurriculumRepository
import com.kernelbreach.core.database.repo.ProgressRepository
import com.kernelbreach.core.model.LabPlacement
import com.kernelbreach.core.model.LessonStatus
import com.kernelbreach.core.model.ModuleItem
import com.kernelbreach.core.model.UnlockRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ModuleRow {
    data class LessonRow(
        val id: String,
        val number: Int,
        val title: String,
        val status: LessonStatus,
    ) : ModuleRow

    data class LabRow(
        val id: String,
        val title: String,
        val sim: String,
        val unlocked: Boolean,
        val completed: Boolean,
    ) : ModuleRow
}

data class ModuleState(
    val loading: Boolean = true,
    val exists: Boolean = true,
    val hasContent: Boolean = true,
    val title: String = "",
    val topics: String = "",
    val level: String = "",
    val canDo: List<String> = emptyList(),
    val rows: List<ModuleRow> = emptyList(),
    val completedLessons: Int = 0,
    val lessonCount: Int = 0,
    val checkpointUnlocked: Boolean = false,
    val checkpointPassed: Boolean = false,
    val checkpointFocus: String? = null,
)

@HiltViewModel
class ModuleViewModel @Inject constructor(
    private val curriculumRepository: CurriculumRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ModuleState())
    val state: StateFlow<ModuleState> = _state.asStateFlow()

    fun load(code: String) {
        viewModelScope.launch {
            curriculumRepository.ensureImported()
            val module = curriculumRepository.module(code)
            if (module == null) {
                _state.update { it.copy(loading = false, exists = false) }
                return@launch
            }
            if (!module.hasContent) {
                _state.update {
                    it.copy(
                        loading = false,
                        hasContent = false,
                        title = module.title,
                        topics = module.topics,
                        level = module.level.displayName,
                    )
                }
                return@launch
            }

            val completedIds = progressRepository.completedLessonIds(module.lessons.map { it.id })
            val completedIndices = module.lessons.withIndex()
                .filter { it.value.id in completedIds }.map { it.index }.toSet()
            val completedLessonNumbers = completedIndices.map { it + 1 }.toSet()
            val inProgress = emptySet<Int>()

            val sequence = LabPlacement.sequence(module.lessons, module.labs)
            val rows = sequence.map { item ->
                when (item) {
                    is ModuleItem.LessonItem -> {
                        val idx = item.lesson.index - 1
                        ModuleRow.LessonRow(
                            id = item.lesson.id,
                            number = item.lesson.index,
                            title = item.lesson.title,
                            status = UnlockRules.lessonStatus(idx, completedIndices, inProgress),
                        )
                    }

                    is ModuleItem.LabItem -> {
                        val progress = progressRepository.labProgress(item.lab.id)
                        ModuleRow.LabRow(
                            id = item.lab.id,
                            title = item.lab.title,
                            sim = item.lab.sim.wire,
                            unlocked = UnlockRules.isLabUnlocked(item.afterLessonNumber, completedLessonNumbers),
                            completed = progress?.completed == true,
                        )
                    }
                }
            }

            _state.update {
                it.copy(
                    loading = false,
                    exists = true,
                    hasContent = true,
                    title = module.title,
                    topics = module.topics,
                    level = module.level.displayName,
                    canDo = module.canDo,
                    rows = rows,
                    completedLessons = completedIndices.size,
                    lessonCount = module.lessons.size,
                    checkpointUnlocked = UnlockRules.isCheckpointUnlocked(module.lessons.size, completedIndices.size),
                    checkpointPassed = code in progressRepository.passedCheckpoints(),
                    checkpointFocus = module.checkpointFocus,
                )
            }
        }
    }
}
