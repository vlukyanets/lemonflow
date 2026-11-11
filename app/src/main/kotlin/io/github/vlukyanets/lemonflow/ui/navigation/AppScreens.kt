package io.github.vlukyanets.lemonflow.ui.navigation

import androidx.compose.runtime.Composable

/** Screen content per destination; the activity passes Hilt-backed screens, tests pass stubs. */
class AppScreens(
    val today: @Composable (openEditor: (String) -> Unit, addHabit: () -> Unit) -> Unit,
    val habits: @Composable (openEditor: (String?) -> Unit, openArchive: () -> Unit) -> Unit,
    val settings: @Composable (openLicenses: () -> Unit, openLicense: (String) -> Unit) -> Unit,
    val editor: @Composable (habitId: String?, close: () -> Unit) -> Unit,
    val archive: @Composable (close: () -> Unit) -> Unit
)
