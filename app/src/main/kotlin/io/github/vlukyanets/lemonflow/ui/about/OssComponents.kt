package io.github.vlukyanets.lemonflow.ui.about

import androidx.annotation.StringRes
import io.github.vlukyanets.lemonflow.R

/**
 * A license text bundled in `assets/licenses/` (FR-024); [id] is the route argument. The
 * texts themselves stay in their official English; [title] is a resource like all copy.
 */
enum class LicenseText(val id: String, @StringRes val title: Int, val asset: String) {
    GPL3("gpl-3.0", R.string.license_title_gpl3, "licenses/GPL-3.0.txt"),
    APACHE2("apache-2.0", R.string.license_title_apache2, "licenses/Apache-2.0.txt"),
    SQLCIPHER("sqlcipher", R.string.license_title_sqlcipher, "licenses/SQLCipher.txt");

    companion object {
        fun byId(id: String): LicenseText? = entries.firstOrNull { it.id == id }
    }
}

/**
 * A bundled third-party component (Constitution VII: all copy lives in resources). Names and
 * authors are proper names, `translatable="false"`, so every locale falls back to English;
 * [partOf] names the component it ships inside, in translated wording. A null [license]
 * means public domain.
 */
class OssComponent(
    @StringRes val name: Int,
    @StringRes val authors: Int,
    val license: LicenseText?,
    @StringRes val partOf: Int? = null
)

/**
 * Everything on the release runtime classpath, grouped by project (research R11). Licenses
 * are taken from each artifact's POM. Update this list when a runtime dependency is added.
 */
val ossComponents: List<OssComponent> = listOf(
    OssComponent(R.string.oss_androidx, R.string.oss_androidx_authors, LicenseText.APACHE2),
    OssComponent(R.string.oss_kotlin, R.string.oss_kotlin_authors, LicenseText.APACHE2),
    OssComponent(R.string.oss_dagger, R.string.oss_dagger_authors, LicenseText.APACHE2),
    OssComponent(R.string.oss_guava, R.string.oss_guava_authors, LicenseText.APACHE2),
    OssComponent(R.string.oss_inject, R.string.oss_inject_authors, LicenseText.APACHE2),
    OssComponent(
        R.string.oss_annotations,
        R.string.oss_annotations_authors,
        LicenseText.APACHE2
    ),
    OssComponent(R.string.oss_sqlcipher, R.string.oss_sqlcipher_authors, LicenseText.SQLCIPHER),
    OssComponent(R.string.oss_sqlite, R.string.oss_sqlite_authors, null, R.string.oss_sqlcipher),
    OssComponent(
        R.string.oss_libtomcrypt,
        R.string.oss_libtomcrypt_authors,
        null,
        R.string.oss_sqlcipher
    ),
    OssComponent(R.string.oss_roboto, R.string.oss_roboto_authors, LicenseText.APACHE2)
)

/**
 * Turns a hard-wrapped license file into paragraphs that wrap on any screen width:
 * blank lines separate paragraphs, line breaks inside a paragraph become spaces.
 */
fun reflow(text: String): List<String> = text
    .replace("\r\n", "\n")
    .split(Regex("\n[ \t]*\n"))
    .map { paragraph -> paragraph.lines().joinToString(" ") { it.trim() }.trim() }
    .filter { it.isNotEmpty() }

/** Where the app's source code is published (FR-024). */
const val SOURCE_URL = "https://github.com/vlukyanets/lemonflow"
