package com.kernelbreach.core.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.kernelbreach.core.design.theme.KbTheme

/**
 * The paper background with its 18dp dot grid — the signature "graph paper" look.
 * Draw this behind a screen's content.
 */
@Composable
fun DotGridBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = KbTheme.colors
    Box(modifier.background(colors.paper)) {
        Canvas(Modifier.matchParentSize()) {
            drawDotGrid(colors.dotGrid)
        }
        content()
    }
}

private fun DrawScope.drawDotGrid(color: androidx.compose.ui.graphics.Color) {
    val step = 18.dp.toPx()
    val radius = 1.dp.toPx()
    var y = 0f
    while (y < size.height) {
        var x = 0f
        while (x < size.width) {
            drawCircle(color = color, radius = radius, center = Offset(x, y))
            x += step
        }
        y += step
    }
}

/** A standard white/dark card with the Blueprint border and radius. */
@Composable
fun KbCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable () -> Unit,
) {
    val colors = KbTheme.colors
    val shapes = KbTheme.shapes
    Box(
        modifier
            .clip(RoundedCornerShape(shapes.card))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(shapes.card))
            .padding(padding),
    ) { content() }
}
