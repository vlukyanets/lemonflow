package io.github.vlukyanets.lemonflow.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vlukyanets.lemonflow.ui.assertAllClickablesAccessible
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LemonFlowNavHostTest {
    @get:Rule
    val compose = createComposeRule()

    private val stubs = AppScreens(
        today = { _, _ -> Text("today-stub") },
        habits = { _, _ -> Text("habits-stub") },
        settings = { openLicenses, _ ->
            Column {
                Text("settings-stub")
                Button(onClick = openLicenses) { Text("licenses-link") }
            }
        },
        editor = { _, _ -> Text("editor-stub") },
        archive = { Text("archive-stub") }
    )

    @Before
    fun setUp() {
        compose.setContent { LemonFlowTheme { LemonFlowNavHost(stubs) } }
    }

    @Test
    fun startsOnToday() {
        compose.onNodeWithText("today-stub").assertIsDisplayed()
        compose.onNodeWithText("Today").assertIsSelected()
    }

    @Test
    fun eachTabOpensItsScreen() {
        listOf(
            "Habits" to "habits-stub",
            "Settings" to "settings-stub",
            "Today" to "today-stub"
        ).forEach { (tab, stub) ->
            compose.onNodeWithText(tab).performClick()
            compose.onNodeWithText(stub).assertIsDisplayed()
            compose.onNodeWithText(tab).assertIsSelected()
        }
    }

    @Test
    fun insightsIsAPlaceholderWithoutActions() {
        compose.onNodeWithText("Insights").performClick()

        compose.onNodeWithText("Coming soon").assertIsDisplayed()
        compose.onNodeWithText(
            "Trends and patterns of your habits will appear here."
        ).assertIsDisplayed()
        // Only the four tabs are clickable: no data, no buttons (FR-020).
        assertEquals(4, compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
    }

    @Test
    fun licensesBelongToTheSettingsTabAndOpenALicense() {
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("licenses-link").performClick()

        compose.onNodeWithText("Open source licenses").assertIsDisplayed()
        compose.onNodeWithText("Settings").assertIsSelected()

        // The list is lazy: items below the fold don't exist until scrolled to.
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("SQLCipher for Android"))
        compose.onNodeWithText("SQLCipher for Android").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("ZETETIC LLC", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("SQLCipher License (BSD-style)").assertIsDisplayed()
        compose.onNodeWithText("Settings").assertIsSelected()
    }

    @Test
    fun bottomBarIsAccessible() {
        compose.assertAllClickablesAccessible()
    }
}
