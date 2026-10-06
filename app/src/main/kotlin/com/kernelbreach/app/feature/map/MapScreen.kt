package com.kernelbreach.app.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kernelbreach.app.ui.KbScreen
import com.kernelbreach.app.ui.LoadingBox
import com.kernelbreach.app.ui.Overline
import com.kernelbreach.app.ui.ScreenTitle
import com.kernelbreach.core.design.components.Pill
import com.kernelbreach.core.design.theme.KbTheme
import com.kernelbreach.core.design.theme.PathPalette
import com.kernelbreach.core.model.PathAccess

/**
 * The learning map. A data-driven, TalkBack-friendly vertical "metro line":
 * each path is a coloured line, stages are sections, modules are stations. The
 * current station carries a "You are here" chip.
 *
 * (The spec also envisions a Canvas metro drawing; this accessible list is the
 * primary representation and the data model behind it is ready to drive a Canvas.)
 */
@Composable
fun MapScreen(
    onOpenModule: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) {
        LoadingBox()
        return
    }

    KbScreen {
        Spacer(Modifier.height(28.dp))
        Overline("Your path")
        ScreenTitle("Learning map")

        state.paths.forEach { path ->
            val lineColor = PathPalette.parse(path.color, path.key)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(lineColor))
                Spacer(Modifier.size(8.dp))
                Text(path.name, style = KbTheme.type.title, color = KbTheme.colors.ink)
                Spacer(Modifier.size(8.dp))
                when (path.access) {
                    PathAccess.OPEN -> {}
                    PathAccess.LOCKED_CAN_OVERRIDE -> Pill("Locked · open anyway")
                    PathAccess.LOCKED -> Pill("Locked")
                }
            }
            Text(path.tagline, style = KbTheme.type.body, color = KbTheme.colors.muted)

            path.stages.forEach { stage ->
                Overline(stage.title, Modifier.padding(top = 10.dp, bottom = 2.dp))
                stage.stations.forEach { station ->
                    Station(station, lineColor, onClick = { onOpenModule(station.code) })
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Station(station: StationUi, lineColor: Color, onClick: () -> Unit) {
    val colors = KbTheme.colors
    val clickable = station.state != StationState.LOCKED
    val stateLabel = when (station.state) {
        StationState.DONE -> "done"
        StationState.CURRENT -> "current, you are here"
        StationState.AVAILABLE -> "available"
        StationState.COMING_SOON -> "coming soon"
        StationState.LOCKED -> "locked"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp)
            .clearAndSetSemantics {
                contentDescription = "${station.title}, ${station.level}, $stateLabel"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dotColor = when (station.state) {
            StationState.DONE, StationState.CURRENT -> lineColor
            StationState.LOCKED, StationState.COMING_SOON -> colors.border
            StationState.AVAILABLE -> lineColor.copy(alpha = 0.4f)
        }
        Box(Modifier.size(16.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                station.title,
                style = KbTheme.type.bodyStrong,
                color = if (station.state == StationState.LOCKED) colors.muted else colors.ink,
            )
            Text(station.level, style = KbTheme.type.overline, color = colors.muted)
        }
        when (station.state) {
            StationState.CURRENT -> Pill("You are here", background = colors.highlighter)
            StationState.DONE -> Text("Done", style = KbTheme.type.label, color = colors.success)
            StationState.COMING_SOON -> Text("Soon", style = KbTheme.type.label, color = colors.muted)
            else -> {}
        }
    }
}
