package com.kernelbreach.app.feature.onboarding

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kernelbreach.app.ui.KbScreen
import com.kernelbreach.app.ui.Overline
import com.kernelbreach.app.ui.ScreenTitle
import com.kernelbreach.core.design.components.KbCard
import com.kernelbreach.core.design.components.PrimaryButton
import com.kernelbreach.core.design.theme.KbTheme
import androidx.compose.material3.Text

/**
 * Goal picker. Chooses a path to start with (Rookie is recommended for everyone)
 * and marks onboarding complete.
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    KbScreen {
        Spacer(Modifier.height(36.dp))
        Overline("Welcome")
        ScreenTitle("What do you want to learn?")
        Text(
            "Pick where to start. New to security? Begin with the Rookie path — you can change this any time.",
            style = KbTheme.type.body,
            color = KbTheme.colors.muted,
        )
        Spacer(Modifier.height(4.dp))

        state.goals.forEach { goal ->
            GoalCard(
                title = goal.name,
                subtitle = goal.tagline,
                selected = goal.key == state.selectedKey,
                recommended = goal.recommended,
                onClick = { viewModel.select(goal.key) },
            )
        }

        Spacer(Modifier.height(8.dp))
        PrimaryButton(
            text = "Start learning",
            enabled = state.selectedKey != null,
            onClick = { viewModel.confirm(onDone) },
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GoalCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    recommended: Boolean,
    onClick: () -> Unit,
) {
    val colors = KbTheme.colors
    KbCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KbTheme.shapes.card))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.ink else colors.border,
                shape = RoundedCornerShape(KbTheme.shapes.card),
            ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (recommended) Overline("Recommended")
            Text(title, style = KbTheme.type.title, color = colors.ink)
            Text(subtitle, style = KbTheme.type.body, color = colors.muted)
        }
    }
}
