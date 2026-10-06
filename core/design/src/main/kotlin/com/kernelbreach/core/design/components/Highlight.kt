package com.kernelbreach.core.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import com.kernelbreach.core.design.theme.KbTheme

/**
 * A short key-idea phrase with the highlighter painted across its lower ~48%,
 * matching the design's yellow marker.
 */
@Composable
fun HighlightPhrase(text: String, modifier: Modifier = Modifier) {
    val colors = KbTheme.colors
    Box(modifier) {
        Text(
            text = text,
            style = KbTheme.type.bodyStrong,
            color = colors.ink,
            modifier = Modifier
                .drawBehind {
                    val top = size.height * 0.52f
                    drawRect(
                        color = colors.highlighter,
                        topLeft = Offset(0f, top),
                        size = Size(size.width, size.height - top),
                    )
                }
                .padding(horizontal = 1.dp),
        )
    }
}

/**
 * Body text where known glossary [terms] are rendered tappable (dotted-underline
 * style in the design; underline + highlight tint here) and invoke [onTermClick].
 *
 * The JSON body carries no highlight markup in v1, so terms are matched by their
 * literal first occurrence (case-insensitive, whole word). This keeps the Learn
 * screen faithful without hand-authoring spans.
 */
@Composable
fun GlossaryBodyText(
    text: String,
    terms: List<String>,
    onTermClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KbTheme.colors
    val annotated = rememberGlossaryAnnotatedString(text, terms, colors.highlighter, colors.mutedStrong, onTermClick)
    Text(text = annotated, style = KbTheme.type.body, color = colors.ink, modifier = modifier)
}

@Composable
private fun rememberGlossaryAnnotatedString(
    text: String,
    terms: List<String>,
    highlight: androidx.compose.ui.graphics.Color,
    underline: androidx.compose.ui.graphics.Color,
    onTermClick: (String) -> Unit,
): AnnotatedString {
    // Find first, non-overlapping occurrence of each term, longest terms first so
    // multi-word terms win over their constituent words.
    data class Hit(val start: Int, val end: Int, val term: String)

    val lower = text.lowercase()
    val hits = mutableListOf<Hit>()
    terms.sortedByDescending { it.length }.forEach { term ->
        if (term.isBlank()) return@forEach
        val idx = lower.indexOf(term.lowercase())
        if (idx >= 0) {
            val end = idx + term.length
            val overlaps = hits.any { idx < it.end && end > it.start }
            if (!overlaps) hits += Hit(idx, end, text.substring(idx, end))
        }
    }
    hits.sortBy { it.start }

    return buildAnnotatedString {
        var cursor = 0
        for (hit in hits) {
            if (hit.start > cursor) append(text.substring(cursor, hit.start))
            withLink(
                LinkAnnotation.Clickable(
                    tag = hit.term,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            background = highlight,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ),
                    linkInteractionListener = { onTermClick(hit.term) },
                ),
            ) {
                append(hit.term)
            }
            cursor = hit.end
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
}

/** A simple inline mono chip used for commands/values. */
@Composable
fun MonoChip(text: String, modifier: Modifier = Modifier) {
    val colors = KbTheme.colors
    Box(
        modifier
            .padding(2.dp),
    ) {
        Text(text, style = KbTheme.type.mono, color = colors.ink)
    }
}

@Suppress("unused")
private fun AnnotatedString.Builder.appendStyled(text: String, style: SpanStyle) {
    withStyle(style) { append(text) }
}
