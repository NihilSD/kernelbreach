package com.kernelbreach.app.feature.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kernelbreach.app.ui.KbScreen
import com.kernelbreach.app.ui.LoadingBox
import com.kernelbreach.app.ui.Overline
import com.kernelbreach.app.ui.ScreenTitle
import com.kernelbreach.core.design.components.KbCard
import com.kernelbreach.core.design.components.PrimaryButton
import com.kernelbreach.core.design.components.SecondaryButton
import com.kernelbreach.core.design.components.SegmentBar
import com.kernelbreach.core.design.theme.KbTheme

/**
 * Guided-script lab (v1). Each step shows its instruction and a simulator-styled
 * control; running/revealing shows the step output, then Next. The final screen
 * asks for the learner's finding and self-checks it against the expected answer.
 */
@Composable
fun LabScreen(
    labId: String,
    onFinished: () -> Unit,
    onClose: () -> Unit,
    viewModel: LabViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(labId) { viewModel.load(labId) }

    val lab = state.lab
    if (state.loading || lab == null) {
        LoadingBox()
        return
    }

    KbScreen {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close lab", tint = KbTheme.colors.ink)
            }
            Overline("Lab · ${lab.sim.wire}")
        }
        ScreenTitle(lab.title)

        KbCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Overline("Scenario")
                Text(lab.scenario, style = KbTheme.type.body, color = KbTheme.colors.ink)
                Spacer(Modifier.height(4.dp))
                Overline("Objective")
                Text(lab.objective, style = KbTheme.type.body, color = KbTheme.colors.ink)
            }
        }

        if (!state.onFinal) {
            SegmentBar(total = lab.steps.size, completed = state.stepIndex, current = state.stepIndex)
            val step = lab.steps[state.stepIndex]
            KbCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Overline("Step ${state.stepIndex + 1} of ${lab.steps.size}")
                    Text(step.instruction, style = KbTheme.type.body, color = KbTheme.colors.ink)

                    val command = LabViewModel.extractCommand(step.instruction)
                    if (!state.currentStepRevealed) {
                        SecondaryButton(
                            text = if (command != null) "Run: $command" else "Reveal output",
                            onClick = viewModel::revealCurrent,
                        )
                    } else {
                        SimulatorOutput(step.output)
                        PrimaryButton(
                            text = if (state.stepIndex + 1 < lab.steps.size) "Next step" else "Finish",
                            onClick = viewModel::next,
                        )
                    }
                }
            }
        } else {
            FinalStep(state, viewModel, lab.answer, lab.hint, onFinished)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SimulatorOutput(output: String) {
    val colors = KbTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.ink)
            .padding(12.dp),
    ) {
        Text(
            output,
            style = KbTheme.type.mono,
            color = colors.onInk,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        )
    }
}

@Composable
private fun FinalStep(
    state: LabUiState,
    vm: LabViewModel,
    answer: String,
    hint: String,
    onFinished: () -> Unit,
) {
    val colors = KbTheme.colors
    KbCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Overline("Your finding")
            Text("What did you conclude? Write it, then check against the answer.", style = KbTheme.type.body, color = colors.muted)
            OutlinedTextField(
                value = state.finding,
                onValueChange = vm::updateFinding,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.committed,
            )
            if (!state.committed) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(if (state.hintShown) "Hide hint" else "Hint", onClick = vm::toggleHint, modifier = Modifier.weight(1f))
                    PrimaryButton("Check", onClick = vm::commitFinding, modifier = Modifier.weight(1f))
                }
                if (state.hintShown && hint.isNotBlank()) {
                    Text("Hint: $hint", style = KbTheme.type.body, color = colors.muted)
                }
            } else {
                Overline("Expected answer")
                Text(answer, style = KbTheme.type.body, color = colors.ink)
                Spacer(Modifier.height(4.dp))
                PrimaryButton("Done", onClick = onFinished)
            }
        }
    }
}
