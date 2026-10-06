package com.kernelbreach.app.feature.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernelbreach.core.database.repo.CurriculumRepository
import com.kernelbreach.core.database.repo.ProgressRepository
import com.kernelbreach.core.database.repo.ReviewRepository
import com.kernelbreach.core.database.repo.Settings
import com.kernelbreach.core.database.repo.SettingsRepository
import com.kernelbreach.core.database.repo.ThemeMode
import com.kernelbreach.core.model.ConceptStrength
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModuleStrength(val code: String, val title: String, val strength: ConceptStrength)

data class MeProgress(
    val lessonsCompleted: Int = 0,
    val modulesPassed: Int = 0,
    val strengths: List<ModuleStrength> = emptyList(),
    val loading: Boolean = true,
)

@HiltViewModel
class MeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val curriculumRepository: CurriculumRepository,
    private val progressRepository: ProgressRepository,
    private val reviewRepository: ReviewRepository,
) : ViewModel() {

    val settings: StateFlow<Settings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _progress = MutableStateFlow(MeProgress())
    val progress: StateFlow<MeProgress> = _progress.asStateFlow()

    init {
        viewModelScope.launch {
            curriculumRepository.ensureImported()
            val curriculum = curriculumRepository.curriculum()
            val passed = progressRepository.passedCheckpoints()
            val contentModules = curriculum.allModules.filter { it.hasContent }

            var lessonsCompleted = 0
            val strengths = mutableListOf<ModuleStrength>()
            for (module in contentModules) {
                val completed = progressRepository.completedLessonIds(module.lessons.map { it.id })
                lessonsCompleted += completed.size
                val strength = reviewRepository.conceptStrength(module.code)
                if (strength != ConceptStrength.NEW) {
                    strengths += ModuleStrength(module.code, module.title, strength)
                }
            }
            _progress.update {
                it.copy(
                    loading = false,
                    lessonsCompleted = lessonsCompleted,
                    modulesPassed = passed.size,
                    strengths = strengths,
                )
            }
        }
    }

    fun setTheme(mode: ThemeMode) = launchSetting { settingsRepository.setThemeMode(mode) }
    fun setLargerText(v: Boolean) = launchSetting { settingsRepository.setLargerText(v) }
    fun setReduceMotion(v: Boolean) = launchSetting { settingsRepository.setReduceMotion(v) }
    fun setReadAloud(v: Boolean) = launchSetting { settingsRepository.setReadAloud(v) }
    fun setDailyReminder(v: Boolean) = launchSetting { settingsRepository.setDailyReminder(v) }

    private fun launchSetting(block: suspend () -> Unit) = viewModelScope.launch { block() }
}
