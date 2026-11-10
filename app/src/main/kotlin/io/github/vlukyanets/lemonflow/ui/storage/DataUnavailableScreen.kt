package io.github.vlukyanets.lemonflow.ui.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow

/**
 * Shows [ready] once the encrypted store is open; until then a blank screen, and the calm
 * data-unavailable screen if it cannot be opened (FR-015). Nothing is erased without consent.
 *
 * The root is a Surface: it sets the default content color (onBackground) for every screen
 * below, so text without an explicit color is never black on the dark background.
 */
@Composable
fun StartupHost(viewModel: StartupViewModel, ready: @Composable () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (state) {
            StartupState.Ready -> ready()
            StartupState.Unavailable -> DataUnavailableScreen(onStartFresh = viewModel::startFresh)
            StartupState.Loading -> Box(Modifier.fillMaxSize())
        }
    }
}

/** The data cannot be decrypted: explain calmly, offer a single "Start fresh" (FR-015). */
@Composable
fun DataUnavailableScreen(onStartFresh: () -> Unit) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 40.dp)
    ) {
        Explanation(Modifier.weight(1f))
        Button(
            onClick = { confirming = true },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) { Text(stringResource(R.string.start_fresh)) }
    }
    if (confirming) {
        ConfirmStartFresh(
            onConfirm = {
                confirming = false
                onStartFresh()
            },
            onDismiss = { confirming = false }
        )
    }
}

@Composable
private fun Explanation(modifier: Modifier) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        val shape = RoundedCornerShape(22.dp)
        Box(
            Modifier
                .size(72.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, LemonFlow.colors.line, shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.ic_lock),
                contentDescription = null,
                tint = LemonFlow.colors.muted,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            stringResource(R.string.data_unavailable_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            stringResource(R.string.data_unavailable_body),
            style = MaterialTheme.typography.bodyLarge,
            color = LemonFlow.colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp)
        )
    }
}

/** States that the old data cannot be recovered before anything is erased (FR-015). */
@Composable
private fun ConfirmStartFresh(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.start_fresh_title)) },
        text = { Text(stringResource(R.string.start_fresh_body)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LemonFlow.colors.danger,
                    contentColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(stringResource(R.string.start_fresh)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
