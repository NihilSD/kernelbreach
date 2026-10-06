package com.kernelbreach.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kernelbreach.core.design.components.DotGridBackground
import com.kernelbreach.core.design.components.KbCard
import com.kernelbreach.core.design.theme.KbTheme

/** Standard screen: dot-grid paper background + scrollable, padded column. */
@Composable
fun KbScreen(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val shapes = KbTheme.shapes
    DotGridBackground(Modifier.fillMaxSize()) {
        val base = Modifier
            .fillMaxSize()
            .padding(horizontal = shapes.screenPadding)
        Column(
            modifier = if (scrollable) base.verticalScroll(rememberScrollState()) else base,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            content()
        }
    }
}

@Composable
fun Overline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = KbTheme.type.overline,
        color = KbTheme.colors.muted,
        modifier = modifier,
    )
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = KbTheme.type.headline, color = KbTheme.colors.ink, modifier = modifier)
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = KbTheme.colors.ink)
    }
}

@Composable
fun ComingSoonCard(message: String, modifier: Modifier = Modifier) {
    KbCard(modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Coming soon", style = KbTheme.type.title, color = KbTheme.colors.ink)
            Text(message, style = KbTheme.type.body, color = KbTheme.colors.muted)
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    KbCard(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = KbTheme.type.title, color = KbTheme.colors.ink, textAlign = TextAlign.Center)
            Text(body, style = KbTheme.type.body, color = KbTheme.colors.muted, textAlign = TextAlign.Center)
        }
    }
}
