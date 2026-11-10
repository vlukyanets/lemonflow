package io.github.vlukyanets.lemonflow.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.ui.common.ScreenHeader
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow

/** Placeholder until statistics are specified (FR-020): no data, no actions. */
@Composable
fun InsightsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 24.dp)
    ) {
        ScreenHeader(title = stringResource(R.string.tab_insights), dateLine = null)
        Column(
            Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
        ) {
            val shape = RoundedCornerShape(22.dp)
            Box(
                Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.surface, shape)
                    .border(1.dp, LemonFlow.colors.line, shape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_insights),
                    contentDescription = null,
                    tint = LemonFlow.colors.muted,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                stringResource(R.string.insights_coming_soon),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                stringResource(R.string.insights_body),
                style = MaterialTheme.typography.bodyMedium,
                color = LemonFlow.colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 260.dp)
            )
        }
    }
}
