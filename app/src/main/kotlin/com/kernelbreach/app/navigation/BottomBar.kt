package com.kernelbreach.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kernelbreach.core.design.theme.KbTheme

/**
 * Custom bottom bar: Learn, Refresh, Library, You. The active tab shows a yellow
 * pill behind its icon (per the design). Each item is a 44dp+ tap target and
 * exposes its selected state to TalkBack.
 */
@Composable
fun KbBottomBar(
    currentRoute: String?,
    onSelect: (TopTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KbTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(colors.card)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopTab.entries.forEach { tab ->
            val selected = currentRoute == tab.route
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .semantics {
                        this.selected = selected
                        contentDescription = tab.label
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) colors.highlighter else androidx.compose.ui.graphics.Color.Transparent)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = colors.ink,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = tab.label,
                    style = KbTheme.type.overline,
                    color = if (selected) colors.ink else colors.muted,
                )
            }
        }
    }
}
