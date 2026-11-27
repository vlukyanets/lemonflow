package io.github.vlukyanets.lemonflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.vlukyanets.lemonflow.ui.about.LicensesScreen

/** Placeholder entry point; the real screens come with the UI work. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                var showLicenses by rememberSaveable { mutableStateOf(false) }
                BackHandler(enabled = showLicenses) { showLicenses = false }
                if (showLicenses) {
                    LicensesScreen(onBack = { showLicenses = false })
                } else {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(text = stringResource(R.string.app_name))
                            TextButton(onClick = { showLicenses = true }) {
                                Text(text = stringResource(R.string.licenses_title))
                            }
                        }
                    }
                }
            }
        }
    }
}
