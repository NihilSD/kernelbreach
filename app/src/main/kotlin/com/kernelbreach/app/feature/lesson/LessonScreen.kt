package com.kernelbreach.app.feature.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kernelbreach.app.ui.KbScreen
import com.kernelbreach.app.ui.LoadingBox
import com.kernelbreach.app.ui.Overline
import com.kernelbreach.app.ui.ScreenTitle
import com.kernelbreach.core.design.components.FeedbackBanner
import com.kernelbreach.core.design.components.GlossaryBodyText
import com.kernelbreach.core.design.components.KbCard
import com.kernelbreach.core.design.components.Pill
import com.kernelbreach.core.design.components.PrimaryButton
import com.kernelbreach.core.design.components.SegmentBar
import com.kernelbreach.core.design.theme.KbTheme
import com.kernelbreach.core.model.Confidence

@Composable
fun LessonScreen(
    lessonId: String,
    onFinished: () -> Unit,
    onClose: () -> Unit,
    viewModel: LessonViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(lessonId) { viewModel.load(lessonId) }

    if (state.loading || state.lesson == null) {
        LoadingBox()
        return
    }

    when (state.phase) {
        LessonPhase.LEARN -> LearnPhase(state, viewModel, onClose)
        LessonPhase.TRY -> TryPhase(state, viewModel, onClose)
        LessonPhase.COMPLETE -> CompletePhase(state, viewModel, onFinished)
    }

    state.openTerm?.let { term ->
        TermSheet(term.term, term.definition, onDismiss = viewModel::closeTerm)
    }
}

@Composable
private fun LearnPhase(state: LessonUiState, vm: LessonViewModel, onClose: () -> Unit) {
    val lesson = state.lesson!!
    KbScreen {
        Spacer(Modifier.height(16.dp))
        TopRow(onClose) {
            SegmentBar(total = 3, completed = 0, current = 0, modifier = Modifier.weight(1f))
        }
        Overline("Concept ${state.conceptNumber} of ${state.conceptCount}")
        ScreenTitle(lesson.title)
        if (lesson.why.isNotBlank()) {
            Text(lesson.why, style = KbTheme.type.body, color = KbTheme.colors.muted)
        }

        lesson.sections.forEach { section ->
            KbCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (section.heading.isNotBlank()) {
                        Text(section.heading, style = KbTheme.type.bodyStrong, color = KbTheme.colors.ink)
                    }
                    section.paragraphs.forEach { p ->
                        GlossaryBodyText(
                            text = p,
                            terms = lesson.terms.map { it.term },
                            onTermClick = vm::openTerm,
                        )
                    }
                }
            }
        }

        lesson.snippet?.let { snippet ->
            KbCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Overline(snippet.label)
                    Text(
                        snippet.text,
                        style = KbTheme.type.mono,
                        color = KbTheme.colors.ink,
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    )
                }
            }
        }

        lesson.example?.takeIf { it.isNotBlank() }?.let { example ->
            KbCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Overline("Worked example")
                    Text(example, style = KbTheme.type.body, color = KbTheme.colors.ink)
                }
            }
        }

        if (lesson.terms.isNotEmpty()) {
            Overline("Key terms")
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                lesson.terms.forEach { t ->
                    Pill(t.term, modifier = Modifier.clickable { vm.openTerm(t.term) })
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        PrimaryButton("Try it", onClick = vm::startTry)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TryPhase(state: LessonUiState, vm: LessonViewModel, onClose: () -> Unit) {
    val q = state.currentQuestion ?: return
    KbScreen {
        Spacer(Modifier.height(16.dp))
        TopRow(onClose) {
            SegmentBar(
                total = state.lesson!!.check.size,
                completed = state.questionIndex,
                current = state.questionIndex,
                modifier = Modifier.weight(1f),
            )
        }
        Overline("Question ${state.questionIndex + 1} of ${state.lesson.check.size}")
        ScreenTitle(q.prompt)

        q.options.forEachIndexed { index, option ->
            val chosen = state.selectedOption == index
            val isAnswer = index == q.answerIndex
            val colors = KbTheme.colors
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
            FeedbackBanner(correct = state.selectedOption == q.answerIndex)
            if (q.explanation.isNotBlank()) {
                Text(q.explanation, style = KbTheme.type.body, color = KbTheme.colors.muted)
            }
            Spacer(Modifier.height(4.dp))
            PrimaryButton(
                text = if (state.questionIndex + 1 < state.lesson!!.check.size) "Next" else "See results",
                onClick = vm::next,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CompletePhase(state: LessonUiState, vm: LessonViewModel, onFinished: () -> Unit) {
    val lesson = state.lesson!!
    KbScreen {
        Spacer(Modifier.height(40.dp))
        Overline("Lesson complete")
        ScreenTitle("You scored ${state.scorePercent}%")
        SegmentBar(total = lesson.check.size, completed = state.correctCount)
        Text(
            "${state.correctCount} of ${lesson.check.size} correct",
            style = KbTheme.type.label,
            color = KbTheme.colors.muted,
        )

        if (lesson.terms.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Overline("You can now explain")
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                lesson.terms.forEach { t -> Pill(t.term) }
            }
        }

        Spacer(Modifier.height(16.dp))
        Overline("How confident do you feel?")
        Confidence.entries.forEach { c ->
            KbCard(
                Modifier.fillMaxWidth().clickable { vm.finish(c, onFinished) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.wire, style = KbTheme.type.bodyStrong, color = KbTheme.colors.ink)
                    Spacer(Modifier.weight(1f))
                    Text("review in ${c.initialIntervalDays}d", style = KbTheme.type.label, color = KbTheme.colors.muted)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TopRow(onClose: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Close lesson", tint = KbTheme.colors.ink)
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermSheet(term: String, definition: String, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = KbTheme.colors.card) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(term, style = KbTheme.type.title, color = KbTheme.colors.ink)
            Text(definition, style = KbTheme.type.body, color = KbTheme.colors.ink)
            Spacer(Modifier.height(16.dp))
        }
    }
}
