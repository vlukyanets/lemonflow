package io.github.vlukyanets.lemonflow.ui.settings

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vlukyanets.lemonflow.domain.model.AppLanguage
import io.github.vlukyanets.lemonflow.testing.FakeLanguageSetting
import io.github.vlukyanets.lemonflow.testing.FakeSettingsRepository
import io.github.vlukyanets.lemonflow.ui.TALL_PHONE
import io.github.vlukyanets.lemonflow.ui.assertAllClickablesAccessible
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TALL_PHONE)
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val settings = FakeSettingsRepository()
    private val languages = FakeLanguageSetting()

    private fun open() {
        val viewModel = SettingsViewModel(settings, languages)
        compose.setContent { LemonFlowTheme { SettingsScreen(viewModel) } }
    }

    private fun radio() = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @Test
    fun rowShowsTheCurrentDayEnd() {
        open()
        compose.onNodeWithText("Day ends at").assertIsDisplayed()
        compose.onNodeWithText("03:00").assertIsDisplayed()
        compose.onNodeWithText("Marks after midnight count for the previous day until this time.")
            .assertIsDisplayed()
    }

    @Test
    fun pickerListsZeroToSix() {
        open()
        compose.onNodeWithText("Day ends at").performClick()

        compose.onNodeWithText("Whole hours, 00:00", substring = true).assertIsDisplayed()
        compose.onAllNodes(radio()).assertCountEquals(7)
        (0..6).forEach { hour ->
            compose.onNode(radio() and hasText("0$hour:00")).assertIsDisplayed()
        }
        compose.onNode(radio() and hasText("03:00")).assertIsSelected()
    }

    @Test
    fun pickingAValueUpdatesTheRowAndClosesTheSheet() {
        open()
        compose.onNodeWithText("Day ends at").performClick()

        compose.onNode(radio() and hasText("05:00")).performClick()

        assertEquals(5, settings.hour.value)
        compose.onAllNodes(hasText("Whole hours", substring = true)).assertCountEquals(0)
        compose.onNodeWithText("05:00").assertIsDisplayed()
    }

    @Test
    fun accessibleWithSheetClosedAndOpen() {
        open()
        compose.assertAllClickablesAccessible()

        compose.onNodeWithText("Day ends at").performClick()
        compose.assertAllClickablesAccessible()
    }

    @Test
    fun languageRowShowsTheCurrentChoice() {
        languages.language = AppLanguage.UKRAINIAN
        open()

        compose.onNodeWithText("Language").assertIsDisplayed()
        compose.onNodeWithText("Українська").assertIsDisplayed()
    }

    @Test
    fun languageSheetListsEveryLanguageByItsOwnName() {
        open()
        compose.onNodeWithText("Language").performClick()

        compose.onAllNodes(radio()).assertCountEquals(4)
        listOf("System default", "English", "Русский", "Українська").forEach {
            compose.onNode(radio() and hasText(it)).assertIsDisplayed()
        }
        compose.onNode(radio() and hasText("System default")).assertIsSelected()
        compose.onNodeWithText("Only for LemonFlow", substring = true).assertIsDisplayed()
    }

    @Test
    fun pickingALanguageAppliesItAndClosesTheSheet() {
        open()
        compose.onNodeWithText("Language").performClick()

        compose.onNode(radio() and hasText("Русский")).performClick()

        assertEquals(AppLanguage.RUSSIAN, languages.language)
        compose.onAllNodes(radio()).assertCountEquals(0)
        compose.onNodeWithText("Русский").assertIsDisplayed()
    }

    @Test
    fun pickingTheCurrentLanguageChangesNothing() {
        open()
        compose.onNodeWithText("Language").performClick()

        compose.onNode(radio() and hasText("System default")).performClick()

        assertEquals(0, languages.setCalls)
        compose.onAllNodes(radio()).assertCountEquals(0)
    }

    @Test
    fun aboutShowsTheAppVersionAndAuthor() {
        open()

        compose.onNodeWithText("About").assertIsDisplayed()
        compose.onNodeWithText("LemonFlow").assertIsDisplayed()
        compose.onNodeWithText("Version", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Made by Valentyn Lukianets").assertIsDisplayed()
    }

    @Test
    fun aboutLinksOpenTheLicenses() {
        var openedList = 0
        var opened: String? = null
        val viewModel = SettingsViewModel(settings, languages)
        compose.setContent {
            LemonFlowTheme {
                SettingsScreen(viewModel, { openedList++ }, { opened = it })
            }
        }

        compose.onNodeWithText("Source code").assertIsDisplayed()
        compose.onNodeWithText("License").performClick()
        assertEquals("gpl-3.0", opened)

        compose.onNodeWithText("Open source licenses").performClick()
        assertEquals(1, openedList)
    }

    @Test
    fun aLanguageChangedOutsideTheAppShowsAfterARefresh() {
        val viewModel = SettingsViewModel(settings, languages)
        compose.setContent { LemonFlowTheme { SettingsScreen(viewModel) } }
        compose.onNodeWithText("System default").assertIsDisplayed()

        // E.g. the system's per-app language screen; the activity is then recreated.
        languages.language = AppLanguage.UKRAINIAN
        compose.runOnIdle { viewModel.refreshLanguage() }

        compose.onNodeWithText("Українська").assertIsDisplayed()
    }

    @Test
    fun languageSheetIsAccessible() {
        open()
        compose.onNodeWithText("Language").performClick()
        compose.assertAllClickablesAccessible()
    }
}
