package com.kernelbreach.app.feature.module

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
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
import com.kernelbreach.app.ui.ComingSoonCard
import com.kernelbreach.app.ui.KbScreen
import com.kernelbreach.app.ui.LoadingBox
import com.kernelbreach.app.ui.Overline
import com.kernelbreach.app.ui.ScreenTitle
import com.kernelbreach.core.design.components.KbCard
import com.kernelbreach.core.design.components.SegmentBar
import com.kernelbreach.core.design.theme.KbTheme
import com.kernelbreach.core.model.LessonStatus

@Composable
fun ModuleScreen(
    moduleCode: String,
    onOpenLesson: (String) -> Unit,
    onOpenLab: (String) -> Unit,
    onOpenCheckpoint: () -> Unit,
    onBack: () -> Unit,
    viewModel: ModuleViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(moduleCode) { viewModel.load(moduleCode) }

    if (state.loading) {
        LoadingBox()
        return
    }

    KbScreen {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = KbTheme.colors.ink)
            }
            Overline("$moduleCode · ${state.level}")
        }
        ScreenTitle(state.title)
        if (state.topics.isNotBlank()) {
            Text(state.topics, style = KbTheme.type.body, color = KbTheme.colors.muted)
        }

        if (!state.hasContent) {
            ComingSoonCard("This module's lessons are being written. Check back soon.")
            Spacer(Modifier.height(24.dp))
            return@KbScreen
        }

        if (state.canDo.isNotEmpty()) {
            KbCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Overline("By the end you can")
                    state.canDo.forEach { outcome ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.Check, null, tint = KbTheme.colors.success, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(outcome, style = KbTheme.type.body, color = KbTheme.colors.ink)
                        }
                    }
                }
            }
        }

        SegmentBar(total = state.lessonCount, completed = state.completedLessons)
        Text(
            "${state.completedLessons} of ${state.lessonCount} lessons done",
            style = KbTheme.type.label,
            color = KbTheme.colors.muted,
        )

        state.rows.forEach { row ->
            when (row) {
                is ModuleRow.LessonRow -> LessonRowItem(row, onOpenLesson)
                is ModuleRow.LabRow -> LabRowItem(row, onOpenLab)
            }
        }

        CheckpointRow(
            unlocked = state.checkpointUnlocked,
            passed = state.checkpointPassed,
            focus = state.checkpointFocus,
            onClick = onOpenCheckpoint,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LessonRowItem(row: ModuleRow.LessonRow, onOpen: (String) -> Unit) {
    val colors = KbTheme.colors
    val locked = row.status == LessonStatus.LOCKED
    KbCard(
        Modifier
            .fillMaxWidth()
            .then(if (!locked) Modifier.clickable { onOpen(row.id) } else Modifier),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Overline("Lesson ${row.number}")
                Text(row.title, style = KbTheme.type.bodyStrong, color = if (locked) colors.muted else colors.ink)
            }
            when (row.status) {
                LessonStatus.COMPLETED -> Icon(Icons.Filled.Check, "Completed", tint = colors.success)
                LessonStatus.LOCKED -> Icon(Icons.Filled.Lock, "Locked", tint = colors.muted)
                else -> {}
            }
        }
    }
}

@Composable
private fun LabRowItem(row: ModuleRow.LabRow, onOpen: (String) -> Unit) {
    val colors = KbTheme.colors
    KbCard(
        Modifier
            .fillMaxWidth()
            .then(if (row.unlocked) Modifier.clickable { onOpen(row.id) } else Modifier),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Science, null, tint = if (row.unlocked) colors.link else colors.muted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Overline("Lab · ${row.sim}")
                Text(row.title, style = KbTheme.type.bodyStrong, color = if (row.unlocked) colors.ink else colors.muted)
            }
            if (row.completed) Icon(Icons.Filled.Check, "Completed", tint = colors.success)
            else if (!row.unlocked) Icon(Icons.Filled.Lock, "Locked", tint = colors.muted)
        }
    }
}

@Composable
private fun CheckpointRow(unlocked: Boolean, passed: Boolean, focus: String?, onClick: () -> Unit) {
    val colors = KbTheme.colors
    KbCard(
        Modifier
            .fillMaxWidth()
            .then(if (unlocked) Modifier.clickable { onClick() } else Modifier),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Overline("Checkpoint · 10 questions")
                Spacer(Modifier.weight(1f))
                when {
                    passed -> Icon(Icons.Filled.Check, "Passed", tint = colors.success)
                    !unlocked -> Icon(Icons.Filled.Lock, "Locked", tint = colors.muted)
                    else -> {}
                }
            }
            Text(
                if (unlocked) "Score 80% to pass." else "Finish every lesson to unlock.",
                style = KbTheme.type.body,
                color = colors.muted,
            )
            if (!focus.isNullOrBlank()) Text("Focus: $focus", style = KbTheme.type.label, color = colors.muted)
        }
    }
}
