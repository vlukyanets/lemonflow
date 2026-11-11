package io.github.vlukyanets.lemonflow.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow

/** Upper-case date line plus a large screen title, as on every top-level design artboard. */
@Composable
fun ScreenHeader(title: String, dateLine: String?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (dateLine != null) {
            Text(
                dateLine.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = LemonFlow.colors.muted
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )
    }
}
