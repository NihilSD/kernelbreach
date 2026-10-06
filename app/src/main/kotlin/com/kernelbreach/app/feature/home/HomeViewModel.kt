package com.kernelbreach.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernelbreach.core.database.repo.CurriculumRepository
import com.kernelbreach.core.database.repo.ProgressRepository
import com.kernelbreach.core.database.repo.ReviewRepository
import com.kernelbreach.core.database.repo.SettingsRepository
import com.kernelbreach.core.model.Curriculum
import com.kernelbreach.core.model.Lesson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContinueTarget(
    val lessonId: String,
    val lessonTitle: String,
    val moduleCode: String,
    val moduleTitle: String,
    val lessonNumber: Int,
    val lessonCount: Int,
)

data class HomeState(
    val loading: Boolean = true,
    val greetingPathName: String? = null,
    val continueTarget: ContinueTarget? = null,
    val dueReviewCount: Int = 0,
    val quickCheckCount: Int = 0,
    val estimatedMinutes: Int = 0,
    val allCaughtUp: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val curriculumRepository: CurriculumRepository,
    private val progressRepository: ProgressRepository,
    private val reviewRepository: ReviewRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            curriculumRepository.ensureImported()
            val curriculum = curriculumRepository.curriculum()
            val settings = settingsRepository.settings.first()
            val startKey = settings.chosenGoal ?: curriculum.paths.firstOrNull()?.key

            val target = findContinueTarget(curriculum, startKey)
            val plan = reviewRepository.dailyPlan(
                continueLessonId = target?.lessonId,
                recentQuickCheckIds = emptyList(),
            )

            _state.update {
                it.copy(
                    loading = false,
                    greetingPathName = curriculum.path(startKey ?: "")?.name,
                    continueTarget = target,
                    dueReviewCount = plan.dueReviews.size,
                    quickCheckCount = plan.quickCheckIds.size,
                    estimatedMinutes = plan.estimatedMinutes,
                    allCaughtUp = target == null && plan.dueReviews.isEmpty(),
                )
            }
        }
    }

    /** First unlocked, incomplete lesson in the chosen path (then any other path). */
    private suspend fun findContinueTarget(curriculum: Curriculum, startKey: String?): ContinueTarget? {
        val orderedPaths = curriculum.paths.sortedByDescending { it.key == startKey }
        for (path in orderedPaths) {
            for (module in path.modules) {
                if (!module.hasContent) continue
                val ids = module.lessons.map { it.id }
                val completed = progressRepository.completedLessonIds(ids)
                val next: Lesson? = module.lessons.firstOrNull { it.id !in completed }
                if (next != null) {
                    return ContinueTarget(
                        lessonId = next.id,
                        lessonTitle = next.title,
                        moduleCode = module.code,
                        moduleTitle = module.title,
                        lessonNumber = next.index,
                        lessonCount = module.lessons.size,
                    )
                }
            }
        }
        return null
    }
}
