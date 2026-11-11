package io.github.vlukyanets.lemonflow.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.ui.common.ScreenHeader
import io.github.vlukyanets.lemonflow.ui.common.hourText
import io.github.vlukyanets.lemonflow.ui.navigation.LocalBottomBarSpace
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    openLicenses: () -> Unit = {},
    openLicense: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val hour = state.dayEndHour
    val locales = LocalConfiguration.current.locales
    LaunchedEffect(locales) { viewModel.refreshLanguage() }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 24.dp,
                bottom = 16.dp + LocalBottomBarSpace.current
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ScreenHeader(stringResource(R.string.tab_settings), dateLine = null)
        if (hour != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingRow(
                    icon = R.drawable.ic_clock,
                    title = stringResource(R.string.settings_day_ends),
                    value = hourText(hour),
                    onClick = { viewModel.open(SettingsSheet.DAY_END) }
                )
                Text(
                    stringResource(R.string.settings_day_ends_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LemonFlow.colors.muted,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
        SettingRow(
            icon = R.drawable.ic_language,
            title = stringResource(R.string.settings_language),
            value = stringResource(languageName(state.language)),
            onClick = { viewModel.open(SettingsSheet.LANGUAGE) }
        )
        AboutSection(openLicenses, openLicense)
    }
    when (state.sheet) {
        SettingsSheet.DAY_END -> if (hour != null) {
            HourPickerSheet(hour, onPick = viewModel::pickHour, onClose = viewModel::close)
        }

        SettingsSheet.LANGUAGE -> LanguagePickerSheet(
            selected = state.language,
            onPick = viewModel::pickLanguage,
            onClose = viewModel::close
        )

        null -> Unit
    }
}

@Composable
internal fun SettingRow(@DrawableRes icon: Int, title: String, value: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .border(1.dp, LemonFlow.colors.line, shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = LemonFlow.colors.muted,
            modifier = Modifier.size(20.dp)
        )
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Icon(
            painterResource(R.drawable.ic_chevron),
            contentDescription = null,
            tint = LemonFlow.colors.muted
        )
    }
}
