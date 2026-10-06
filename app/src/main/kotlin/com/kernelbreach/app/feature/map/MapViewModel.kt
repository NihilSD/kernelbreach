package com.kernelbreach.app.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kernelbreach.core.database.repo.CurriculumRepository
import com.kernelbreach.core.database.repo.ProgressRepository
import com.kernelbreach.core.database.repo.SettingsRepository
import com.kernelbreach.core.model.PathAccess
import com.kernelbreach.core.model.UnlockRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StationState { DONE, CURRENT, AVAILABLE, COMING_SOON, LOCKED }

data class StationUi(
    val code: String,
    val title: String,
    val level: String,
    val state: StationState,
)

data class StageUi(val title: String, val stations: List<StationUi>)

data class PathUi(
    val key: String,
    val name: String,
    val color: String,
    val tagline: String,
    val access: PathAccess,
    val isTrunk: Boolean,
    val stages: List<StageUi>,
)

data class MapState(
    val loading: Boolean = true,
    val paths: List<PathUi> = emptyList(),
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val curriculumRepository: CurriculumRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            curriculumRepository.ensureImported()
            val curriculum = curriculumRepository.curriculum()
            val rules = UnlockRules(curriculum)
            val passed = progressRepository.passedCheckpoints()
            val settings = settingsRepository.settings.first()
            val placement = settings.placementPassed

            var currentMarked = false
            val pathUis = curriculum.paths.map { path ->
                val access = rules.pathAccess(path.key, passed, placement)
                val stageUis = path.stages.map { stage ->
                    val stations = stage.modules.map { module ->
                        val state = when {
                            !module.hasContent -> StationState.COMING_SOON
                            module.code in passed -> StationState.DONE
                            access != PathAccess.OPEN -> StationState.LOCKED
                            !currentMarked -> {
                                currentMarked = true
                                StationState.CURRENT
                            }

                            else -> StationState.AVAILABLE
                        }
                        StationUi(module.code, module.title, module.level.displayName, state)
                    }
                    StageUi(stage.title, stations)
                }
                PathUi(
                    key = path.key,
                    name = path.name,
                    color = path.color,
                    tagline = path.tagline,
                    access = access,
                    isTrunk = curriculum.trunkPaths.any { it.key == path.key },
                    stages = stageUis,
                )
            }
            _state.update { it.copy(loading = false, paths = pathUis) }
        }
    }
}
