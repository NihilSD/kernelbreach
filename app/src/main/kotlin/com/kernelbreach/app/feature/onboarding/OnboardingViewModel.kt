package com.kernelbreach.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernelbreach.core.database.repo.CurriculumRepository
import com.kernelbreach.core.database.repo.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Goal(
    val key: String,
    val name: String,
    val tagline: String,
    val recommended: Boolean,
)

data class OnboardingState(
    val goals: List<Goal> = emptyList(),
    val selectedKey: String? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val curriculumRepository: CurriculumRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            curriculumRepository.ensureImported()
            val paths = curriculumRepository.curriculum().paths
            val goals = paths.mapIndexed { index, p ->
                Goal(key = p.key, name = p.name, tagline = p.tagline, recommended = index == 0)
            }
            _state.update { it.copy(goals = goals, selectedKey = goals.firstOrNull()?.key) }
        }
    }

    fun select(key: String) = _state.update { it.copy(selectedKey = key) }

    fun confirm(onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value.selectedKey?.let { settingsRepository.setChosenGoal(it) }
            settingsRepository.setOnboardingDone(true)
            onDone()
        }
    }
}
