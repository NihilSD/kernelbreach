package com.kernelbreach.app.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kernelbreach.app.ui.KbScreen
import com.kernelbreach.app.ui.Overline
import com.kernelbreach.app.ui.ScreenTitle
import com.kernelbreach.core.design.components.KbCard
import com.kernelbreach.core.design.theme.KbTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: LibraryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    KbScreen {
        Spacer(Modifier.height(28.dp))
        Overline("Library")
        ScreenTitle("Look it up")
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Search terms") },
        )
        Text("${state.results.size} terms", style = KbTheme.type.label, color = KbTheme.colors.muted)

        state.results.forEach { term ->
            KbCard(Modifier.fillMaxWidth().clickable { viewModel.open(term) }) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(term.term, style = KbTheme.type.bodyStrong, color = KbTheme.colors.ink)
                    Text(
                        term.definition,
                        style = KbTheme.type.body,
                        color = KbTheme.colors.muted,
                        maxLines = 2,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    state.openTerm?.let { term ->
        ModalBottomSheet(onDismissRequest = viewModel::close, containerColor = KbTheme.colors.card) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(term.term, style = KbTheme.type.title, color = KbTheme.colors.ink)
                Text(term.definition, style = KbTheme.type.body, color = KbTheme.colors.ink)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
