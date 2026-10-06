package com.kernelbreach.app.feature.dev

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kernelbreach.app.ui.KbScreen
import com.kernelbreach.app.ui.Overline
import com.kernelbreach.app.ui.ScreenTitle
import com.kernelbreach.core.database.repo.CurriculumRepository
import com.kernelbreach.core.design.components.KbCard
import com.kernelbreach.core.design.theme.KbTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UnreviewedItem(val code: String, val title: String)

@HiltViewModel
class UnreviewedViewModel @Inject constructor(
    private val curriculumRepository: CurriculumRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<UnreviewedItem>>(emptyList())
    val items: StateFlow<List<UnreviewedItem>> = _items.asStateFlow()

    init {
        viewModelScope.launch {
            curriculumRepository.ensureImported()
            val list = curriculumRepository.curriculum().allModules
                .filter { it.hasContent && !it.reviewed }
                .map { UnreviewedItem(it.code, it.title) }
            _items.update { list }
        }
    }
}

@Composable
fun UnreviewedScreen(
    onBack: () -> Unit,
    viewModel: UnreviewedViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    KbScreen {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = KbTheme.colors.ink)
            }
            Overline("Developer")
        }
        ScreenTitle("Unreviewed modules")
        Text(
            "${items.size} modules are AI-drafted and not yet human fact-checked.",
            style = KbTheme.type.body,
            color = KbTheme.colors.muted,
        )
        items.forEach { item ->
            KbCard(Modifier.fillMaxWidth()) {
                Column {
                    Text(item.code, style = KbTheme.type.overline, color = KbTheme.colors.muted)
                    Text(item.title, style = KbTheme.type.bodyStrong, color = KbTheme.colors.ink)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
