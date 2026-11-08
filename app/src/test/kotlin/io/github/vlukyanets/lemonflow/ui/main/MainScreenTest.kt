package io.github.vlukyanets.lemonflow.ui.main

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vlukyanets.lemonflow.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTitle() {
        composeRule.setContent { MainScreen() }

        val title = ApplicationProvider.getApplicationContext<Context>().getString(
            R.string.main_title
        )
        composeRule.onNodeWithText(title).assertExists()
    }
}
