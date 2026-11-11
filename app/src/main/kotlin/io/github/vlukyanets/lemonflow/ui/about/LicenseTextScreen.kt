package io.github.vlukyanets.lemonflow.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.ui.navigation.LocalBottomBarSpace
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The full text of one bundled license, read from assets off the main thread (FR-024). */
@Composable
fun LicenseTextScreen(id: String, onBack: () -> Unit, io: CoroutineDispatcher = Dispatchers.IO) {
    val license = LicenseText.byId(id)
    val context = LocalContext.current
    val paragraphs by produceState(emptyList<String>(), license) {
        value = license?.let {
            withContext(io) {
                reflow(
                    context.assets.open(it.asset).bufferedReader().use { reader ->
                        reader.readText()
                    }
                )
            }
        }.orEmpty()
    }
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 16.dp + LocalBottomBarSpace.current
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { BackButton(onBack) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    license?.let { stringResource(it.title) }.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() }
                )
                // License texts are legal documents: only the English original is official.
                Text(
                    stringResource(R.string.license_text_official),
                    style = MaterialTheme.typography.bodySmall,
                    color = LemonFlow.colors.muted
                )
            }
        }
        items(paragraphs) { paragraph ->
            Text(paragraph, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
