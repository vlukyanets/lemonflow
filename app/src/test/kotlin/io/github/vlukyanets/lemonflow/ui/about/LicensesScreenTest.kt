package io.github.vlukyanets.lemonflow.ui.about

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vlukyanets.lemonflow.ui.TALL_PHONE
import io.github.vlukyanets.lemonflow.ui.assertAllClickablesAccessible
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * FR-024 with Constitution VII: proper names fall back to English in every locale, the
 * descriptive copy is translated, and license texts say they are the English original.
 */
@RunWith(AndroidJUnit4::class)
class LicensesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var opened: String? = null

    private fun open() {
        compose.setContent {
            LemonFlowTheme { LicensesScreen(onBack = {}, openLicense = { opened = it }) }
        }
    }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun englishListsComponentsAndOpensALicense() {
        open()
        compose.onNodeWithText("SQLite (part of SQLCipher for Android)").assertIsDisplayed()
        compose.onNodeWithText("SQLCipher License (BSD-style)").performClick()
        assertEquals("sqlcipher", opened)
        compose.assertAllClickablesAccessible()
    }

    @Test
    @Config(qualifiers = "ru-$TALL_PHONE")
    fun russianKeepsProperNamesAndTranslatesTheRest() {
        open()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Roboto font"))
        // Proper names: no Russian resource, so the English one is used.
        compose.onNodeWithText("Dagger and Hilt").assertIsDisplayed()
        compose.onNodeWithText("The Dagger Authors").assertIsDisplayed()
        // Descriptive copy: translated.
        compose.onNodeWithText("SQLite (входит в SQLCipher for Android)").assertIsDisplayed()
        compose.onNodeWithText("Лицензия SQLCipher (в стиле BSD)").assertIsDisplayed()
        // SQLite and LibTomCrypt.
        compose.onAllNodesWithText("Общественное достояние").assertCountEquals(2)
    }

    @Test
    @Config(qualifiers = "uk")
    fun aLicenseTextSaysItIsTheEnglishOriginal() {
        compose.setContent { LemonFlowTheme { LicenseTextScreen("apache-2.0", onBack = {}) } }
        compose.onNodeWithText("Apache License 2.0").assertIsDisplayed()
        compose.onNodeWithText("Офіційний текст англійською мовою.").assertIsDisplayed()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("TERMS AND CONDITIONS", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
