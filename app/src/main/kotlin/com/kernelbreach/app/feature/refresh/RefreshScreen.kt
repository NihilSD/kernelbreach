package com.kernelbreach.app.feature.refresh

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kernelbreach.app.ui.EmptyState
import com.kernelbreach.app.ui.KbScreen
import com.kernelbreach.app.ui.LoadingBox
import com.kernelbreach.app.ui.Overline
import com.kernelbreach.app.ui.ScreenTitle
import com.kernelbreach.core.design.components.KbCard
import com.kernelbreach.core.design.components.PrimaryButton
import com.kernelbreach.core.design.components.SecondaryButton
import com.kernelbreach.core.design.components.SegmentBar
import com.kernelbreach.core.design.theme.KbTheme
import com.kernelbreach.core.model.ReviewGrade

@Composable
fun RefreshScreen(viewModel: RefreshViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) {
        LoadingBox()
        return
    }

    KbScreen {
        Spacer(Modifier.height(28.dp))
        Overline("Refresh")
        ScreenTitle("Bring it back")

        if (state.done || state.current == null) {
            EmptyState(
                title = "Nothing due right now",
                body = "Review items appear here as your spaced-repetition schedule brings them up. Finish a lesson to start the clock.",
            )
            Spacer(Modifier.height(24.dp))
            return@KbScreen
        }

        val card = state.current!!
        SegmentBar(total = state.cards.size, completed = state.index, current = state.index)
        KbCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Overline(card.overline)
                Text(card.prompt, style = KbTheme.type.title, color = KbTheme.colors.ink)
                if (state.revealed) {
                    Spacer(Modifier.height(4.dp))
                    Overline("Answer")
                    Text(card.answer, style = KbTheme.type.body, color = KbTheme.colors.ink)
                }
            }
        }

        if (!state.revealed) {
            PrimaryButton("Show answer", onClick = viewModel::reveal)
        } else {
            Overline("How did that go?")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Missed it", onClick = { viewModel.grade(ReviewGrade.MISSED) }, modifier = Modifier.weight(1f))
                SecondaryButton("Slow", onClick = { viewModel.grade(ReviewGrade.SLOW) }, modifier = Modifier.weight(1f))
                PrimaryButton("Easy", onClick = { viewModel.grade(ReviewGrade.EASY) }, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
