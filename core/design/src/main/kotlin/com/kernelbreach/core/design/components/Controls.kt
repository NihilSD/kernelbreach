package com.kernelbreach.core.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kernelbreach.core.design.theme.KbTheme

/**
 * Segmented progress bar: one segment per exercise/lesson. Never a bare
 * percentage (per the design). [completed] segments are filled with [fillColor],
 * the current one is half-filled, the rest are empty.
 */
@Composable
fun SegmentBar(
    total: Int,
    completed: Int,
    modifier: Modifier = Modifier,
    current: Int = -1,
    fillColor: Color = KbTheme.colors.success,
) {
    val colors = KbTheme.colors
    Row(
        modifier.semantics {
            contentDescription = "$completed of $total done"
        },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(total.coerceAtLeast(1)) { i ->
            val color = when {
                i < completed -> fillColor
                i == current -> fillColor.copy(alpha = 0.45f)
                else -> colors.border
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}

/** The single dark primary button per screen (54–56dp tall). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = KbTheme.colors
    val shapes = KbTheme.shapes
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = shapes.primaryButtonHeight),
        shape = RoundedCornerShape(shapes.button),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.ink,
            contentColor = colors.onInk,
            disabledContainerColor = colors.border,
            disabledContentColor = colors.muted,
        ),
    ) {
        Text(text, style = KbTheme.type.bodyStrong)
    }
}

/** A lighter secondary action. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = KbTheme.colors
    val shapes = KbTheme.shapes
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = shapes.primaryButtonHeight),
        shape = RoundedCornerShape(shapes.button),
        border = BorderStroke(1.dp, colors.border),
    ) {
        Text(text, style = KbTheme.type.bodyStrong, color = colors.ink)
    }
}

/** A fully-rounded pill, used for tags and the bottom-bar active indicator. */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = KbTheme.colors.softYellow,
    textColor: Color = KbTheme.colors.ink,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, style = KbTheme.type.overline, color = textColor)
    }
}

/**
 * Answer feedback that never relies on colour alone: an icon + the word
 * "Correct" / "Not quite", on the appropriate tint.
 */
@Composable
fun FeedbackBanner(correct: Boolean, modifier: Modifier = Modifier) {
    val colors = KbTheme.colors
    val bg = if (correct) colors.successTint else colors.danger.copy(alpha = 0.12f)
    val fg = if (correct) colors.success else colors.danger
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (correct) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (correct) "Correct" else "Not quite",
            style = KbTheme.type.bodyStrong,
            color = fg,
        )
    }
}
