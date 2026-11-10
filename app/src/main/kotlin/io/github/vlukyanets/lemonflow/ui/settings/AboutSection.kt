package io.github.vlukyanets.lemonflow.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.ui.about.LicenseText
import io.github.vlukyanets.lemonflow.ui.about.SOURCE_URL
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow

/**
 * App name, version and author (FR-023), then the app's license, its source code and the
 * third-party licenses (FR-024).
 */
@Composable
internal fun AboutSection(openLicenses: () -> Unit, openLicense: (String) -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.settings_about),
            style = MaterialTheme.typography.labelMedium,
            color = LemonFlow.colors.muted,
            modifier = Modifier.padding(horizontal = 4.dp).semantics { heading() }
        )
        AppCard()
        SettingRow(
            icon = R.drawable.ic_document,
            title = stringResource(R.string.settings_license),
            value = stringResource(R.string.settings_license_value),
            onClick = { openLicense(LicenseText.GPL3.id) }
        )
        SettingRow(
            icon = R.drawable.ic_code,
            title = stringResource(R.string.settings_source),
            value = stringResource(R.string.settings_source_value),
            onClick = {
                try {
                    uriHandler.openUri(SOURCE_URL)
                } catch (ignored: IllegalArgumentException) {
                    // No app can open web links; the row still names where the code is.
                }
            }
        )
        SettingRow(
            icon = R.drawable.ic_book,
            title = stringResource(R.string.settings_oss),
            value = "",
            onClick = openLicenses
        )
    }
}

@Composable
private fun AppCard() {
    val context = LocalContext.current
    val version = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, LemonFlow.colors.line, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.settings_version, version),
            style = MaterialTheme.typography.bodyMedium,
            color = LemonFlow.colors.muted
        )
        Text(
            stringResource(R.string.settings_made_by, stringResource(R.string.author_name)),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
