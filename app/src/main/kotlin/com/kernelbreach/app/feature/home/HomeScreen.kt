package com.kernelbreach.app.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

@Composable
fun HomeScreen(
    onOpenMap: () -> Unit,
    onContinueLesson: (String) -> Unit,
    onOpenModule: (String) -> Unit,
    onStartRefresh: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.loading) {
        LoadingBox()
        return
    }

    KbScreen {
        Spacer(Modifier.height(28.dp))
        Overline("Today's plan")
        ScreenTitle("Keep going")
        state.estimatedMinutes.takeIf { it > 0 }?.let {
            Text("About $it min today", style = KbTheme.type.body, color = KbTheme.colors.muted)
        }

        val target = state.continueTarget
        if (target != null) {
            KbCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Overline(target.moduleTitle)
                    Text(target.lessonTitle, style = KbTheme.type.title, color = KbTheme.colors.ink)
                    SegmentBar(
                        total = target.lessonCount,
                        completed = target.lessonNumber - 1,
                        current = target.lessonNumber - 1,
                    )
                    Text(
                        "Lesson ${target.lessonNumber} of ${target.lessonCount}",
                        style = KbTheme.type.label,
                        color = KbTheme.colors.muted,
                    )
                    PrimaryButton("Continue", onClick = { onContinueLesson(target.lessonId) })
                }
            }
        } else if (state.allCaughtUp) {
            EmptyState(
                title = "You're all caught up",
                body = "No lessons waiting right now. Review something you've learned, or explore the map.",
            )
        }

        if (state.dueReviewCount > 0 || state.quickCheckCount > 0) {
            KbCard(Modifier.fillMaxWidth().clickable { onStartRefresh() }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Overline("Refresh")
                    Text(
                        "${state.dueReviewCount} to review" +
                            if (state.quickCheckCount > 0) " · ${state.quickCheckCount} quick checks" else "",
                        style = KbTheme.type.bodyStrong,
                        color = KbTheme.colors.ink,
                    )
                    Text("Keep what you've learned fresh.", style = KbTheme.type.body, color = KbTheme.colors.muted)
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        SecondaryButton("Open the map", onClick = onOpenMap)
        Spacer(Modifier.height(24.dp))
    }
}
