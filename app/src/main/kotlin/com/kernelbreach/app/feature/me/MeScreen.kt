package com.kernelbreach.app.feature.me

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kernelbreach.app.ui.KbScreen
import com.kernelbreach.app.ui.Overline
import com.kernelbreach.app.ui.ScreenTitle
import com.kernelbreach.core.database.repo.ThemeMode
import com.kernelbreach.core.design.components.KbCard
import com.kernelbreach.core.design.components.Pill
import com.kernelbreach.core.design.theme.KbTheme
import com.kernelbreach.core.model.ConceptStrength

@Composable
fun MeScreen(
    onOpenDev: () -> Unit,
    viewModel: MeViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val s = settings

    KbScreen {
        Spacer(Modifier.height(28.dp))
        Overline("You")
        ScreenTitle("Your progress")

        KbCard(Modifier.fillMaxWidth()) {
            Row {
                Stat("Lessons done", progress.lessonsCompleted.toString(), Modifier.weight(1f))
                Stat("Modules passed", progress.modulesPassed.toString(), Modifier.weight(1f))
            }
        }

        if (progress.strengths.isNotEmpty()) {
            Overline("Concept strength")
            progress.strengths.forEach { ms ->
                KbCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(ms.title, style = KbTheme.type.bodyStrong, color = KbTheme.colors.ink, modifier = Modifier.weight(1f))
                        Pill(ms.strength.label(), background = ms.strength.tint())
                    }
                }
            }
        }

        if (s != null) {
            Spacer(Modifier.height(8.dp))
            Overline("Accessibility")
            KbCard(Modifier.fillMaxWidth()) {
                Column {
                    ToggleRow("Larger text", s.largerText, viewModel::setLargerText)
                    ToggleRow("Reduce motion", s.reduceMotion, viewModel::setReduceMotion)
                    ToggleRow("Read aloud", s.readAloud, viewModel::setReadAloud)
                    ToggleRow("Daily refresh reminder", s.dailyReminder, viewModel::setDailyReminder)
                }
            }

            Overline("Theme")
            KbCard(Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = s.themeMode == mode
                        Pill(
                            text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                            background = if (selected) KbTheme.colors.highlighter else KbTheme.colors.paper,
                            modifier = Modifier.clickable { viewModel.setTheme(mode) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        KbCard(Modifier.fillMaxWidth().clickable { onOpenDev() }) {
            Column {
                Text("Developer: unreviewed content", style = KbTheme.type.bodyStrong, color = KbTheme.colors.ink)
                Text("All content is AI-drafted and not yet human fact-checked.", style = KbTheme.type.body, color = KbTheme.colors.muted)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = KbTheme.type.headline, color = KbTheme.colors.ink)
        Text(label, style = KbTheme.type.label, color = KbTheme.colors.muted)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = KbTheme.type.body, color = KbTheme.colors.ink, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ConceptStrength.label(): String = when (this) {
    ConceptStrength.NEW -> "New"
    ConceptStrength.OKAY -> "Okay"
    ConceptStrength.GOOD -> "Good"
    ConceptStrength.SOLID -> "Solid"
}

@Composable
private fun ConceptStrength.tint() = when (this) {
    ConceptStrength.SOLID -> KbTheme.colors.successTint
    ConceptStrength.GOOD -> KbTheme.colors.softYellow
    else -> KbTheme.colors.paper
}
