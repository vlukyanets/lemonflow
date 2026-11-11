package io.github.vlukyanets.lemonflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.vlukyanets.lemonflow.ui.editor.HabitEditorScreen
import io.github.vlukyanets.lemonflow.ui.habits.ArchiveScreen
import io.github.vlukyanets.lemonflow.ui.habits.HabitsScreen
import io.github.vlukyanets.lemonflow.ui.navigation.AppScreens
import io.github.vlukyanets.lemonflow.ui.navigation.LemonFlowNavHost
import io.github.vlukyanets.lemonflow.ui.settings.SettingsScreen
import io.github.vlukyanets.lemonflow.ui.storage.StartupHost
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlowTheme
import io.github.vlukyanets.lemonflow.ui.today.TodayScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LemonFlowTheme {
                // The graph only appears once the encrypted store is open (FR-015).
                StartupHost(hiltViewModel()) { LemonFlowNavHost(appScreens()) }
            }
        }
    }
}

/** Hilt-backed screens for each destination. */
private fun appScreens(): AppScreens = AppScreens(
    today = { openEditor, addHabit -> TodayScreen(hiltViewModel(), openEditor, addHabit) },
    habits = { openEditor, openArchive -> HabitsScreen(hiltViewModel(), openEditor, openArchive) },
    settings = { openLicenses, openLicense ->
        SettingsScreen(hiltViewModel(), openLicenses, openLicense)
    },
    editor = { _, close -> HabitEditorScreen(hiltViewModel(), close) },
    archive = { back -> ArchiveScreen(hiltViewModel(), back) }
)
