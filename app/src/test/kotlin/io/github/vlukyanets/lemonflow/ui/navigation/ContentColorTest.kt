package io.github.vlukyanets.lemonflow.ui.navigation

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Screens rely on the default content color for plain text. Without a Surface at the
 * root it is black, which is invisible on the dark background.
 */
@RunWith(AndroidJUnit4::class)
class ContentColorTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun darkScreensGetOnBackgroundAsTheDefaultTextColor() = assertContentColor(dark = true)

    @Test
    fun lightScreensGetOnBackgroundAsTheDefaultTextColor() = assertContentColor(dark = false)

    private fun assertContentColor(dark: Boolean) {
        var seen = Color.Unspecified
        var expected = Color.Unspecified
        val screens = AppScreens(
            today = { _, _ ->
                seen = LocalContentColor.current
                expected = MaterialTheme.colorScheme.onBackground
                Text("today")
            },
            habits = { _, _ -> Text("habits") },
            settings = { _, _ -> Text("settings") },
            editor = { _, _ -> Text("editor") },
            archive = { Text("archive") }
        )
        compose.setContent { LemonFlowTheme(darkTheme = dark) { LemonFlowNavHost(screens) } }

        compose.runOnIdle { assertEquals(expected, seen) }
    }
}
