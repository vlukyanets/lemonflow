package io.github.vlukyanets.lemonflow.ui.about

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** FR-024: every license the app points to is bundled and readable offline. */
@RunWith(AndroidJUnit4::class)
class LicenseTextTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun read(license: LicenseText): String =
        context.assets.open(license.asset).bufferedReader().use { it.readText() }

    @Test
    fun everyLicenseIsBundledWithItsExpectedText() {
        val marker = mapOf(
            LicenseText.GPL3 to "GNU GENERAL PUBLIC LICENSE",
            LicenseText.APACHE2 to "Apache License",
            LicenseText.SQLCIPHER to "ZETETIC LLC"
        )
        LicenseText.entries.forEach { license ->
            val text = read(license)
            assertTrue(
                "${license.asset} lacks '${marker[license]}'",
                marker.getValue(license) in text
            )
        }
    }

    @Test
    fun everyComponentLicenseIsKnownById() {
        ossComponents.mapNotNull { it.license }.forEach {
            assertNotNull(LicenseText.byId(it.id))
        }
        assertEquals(null, LicenseText.byId("unknown"))
    }

    @Test
    fun reflowJoinsWrappedLinesAndKeepsParagraphs() {
        val text = "  Title line\n\n   first line\n   continues here\n\n\n second\r\n paragraph\n"
        assertEquals(
            listOf("Title line", "first line continues here", "second paragraph"),
            reflow(text)
        )
    }

    @Test
    fun reflowOfTheGplKeepsItsTitleFirst() {
        val paragraphs = reflow(read(LicenseText.GPL3))
        assertTrue(paragraphs.first().startsWith("GNU GENERAL PUBLIC LICENSE"))
        assertTrue(paragraphs.size > 100)
    }
}
