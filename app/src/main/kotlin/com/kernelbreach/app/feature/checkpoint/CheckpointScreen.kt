package com.kernelbreach.app.feature.checkpoint

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kernelbreach.app.ui.KbScreen
import com.kernelbreach.app.ui.LoadingBox
import com.kernelbreach.app.ui.Overline
import com.kernelbreach.app.ui.ScreenTitle
import com.kernelbreach.core.design.components.FeedbackBanner
import com.kernelbreach.core.design.components.KbCard
import com.kernelbreach.core.design.components.PrimaryButton
import com.kernelbreach.core.design.components.SecondaryButton
import com.kernelbreach.core.design.components.SegmentBar
import com.kernelbreach.core.design.theme.KbTheme

@Composable
fun CheckpointScreen(
    moduleCode: String,
    onDone: () -> Unit,
    onClose: () -> Unit,
    viewModel: CheckpointViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(moduleCode) { viewModel.load(moduleCode) }

    if (state.loading) {
        LoadingBox()
        return
    }

    when (state.phase) {
        CheckpointPhase.QUIZ -> Quiz(state, viewModel, onClose)
        CheckpointPhase.RESULT -> Result(state, viewModel, moduleCode, onDone)
    }
}

@Composable
private fun Quiz(state: CheckpointState, vm: CheckpointViewModel, onClose: () -> Unit) {
    val q = state.current ?: return
    val colors = KbTheme.colors
    KbScreen {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close checkpoint", tint = colors.ink)
            }
            SegmentBar(
                total = state.questions.size,
                completed = state.index,
                current = state.index,
                modifier = Modifier.weight(1f),
            )
        }
        Overline("Question ${state.index + 1} of ${state.questions.size}")
        ScreenTitle(q.prompt)

        q.options.forEachIndexed { index, option ->
            val chosen = state.selected == index
            val isAnswer = index == q.correctIndex
            val border = when {
                state.revealed && isAnswer -> colors.success
                state.revealed && chosen -> colors.danger
                chosen -> colors.ink
                else -> colors.border
            }
            KbCard(
                Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (chosen || (state.revealed && isAnswer)) 2.dp else 1.dp,
                        color = border,
                        shape = RoundedCornerShape(KbTheme.shapes.card),
                    )
                    .then(if (!state.revealed) Modifier.clickable { vm.answer(index) } else Modifier),
            ) {
                Text(option, style = KbTheme.type.body, color = colors.ink)
            }
        }

        if (state.revealed) {
            FeedbackBanner(correct = state.selected == q.correctIndex)
            if (q.why.isNotBlank()) Text(q.why, style = KbTheme.type.body, color = colors.muted)
            PrimaryButton(
                text = if (state.index + 1 < state.questions.size) "Next" else "See result",
                onClick = vm::next,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Result(state: CheckpointState, vm: CheckpointViewModel, moduleCode: String, onDone: () -> Unit) {
    val colors = KbTheme.colors
    val percent = if (state.questions.isEmpty()) 0 else state.correctCount * 100 / state.questions.size
    KbScreen {
        Spacer(Modifier.height(40.dp))
        Overline("Checkpoint")
        ScreenTitle(if (state.passed) "Passed · $percent%" else "Not yet · $percent%")
        Text(
            if (state.passed) "Nice work. You've unlocked what comes next." else "You need 80% to pass. Review the misses and try again.",
            style = KbTheme.type.body,
            color = colors.muted,
        )

        val misses = state.questions.filterIndexed { i, q -> state.answers.getOrNull(i) != q.correctIndex }
        if (misses.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Overline("Review these")
            misses.forEach { q ->
                KbCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(q.prompt, style = KbTheme.type.bodyStrong, color = colors.ink)
                        Text("Answer: ${q.options[q.correctIndex]}", style = KbTheme.type.body, color = colors.success)
                        if (q.why.isNotBlank()) Text(q.why, style = KbTheme.type.body, color = colors.muted)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        if (!state.passed) {
            SecondaryButton("Retake", onClick = { vm.retake(moduleCode) })
        }
        PrimaryButton("Done", onClick = onDone)
        Spacer(Modifier.height(24.dp))
    }
}
