package io.github.vlukyanets.lemonflow.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.ui.navigation.LocalBottomBarSpace
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow

/** Every bundled third-party component and its license (FR-024). */
@Composable
fun LicensesScreen(onBack: () -> Unit, openLicense: (String) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 16.dp + LocalBottomBarSpace.current
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { BackButton(onBack) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.settings_oss),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    stringResource(R.string.licenses_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LemonFlow.colors.muted
                )
            }
        }
        items(ossComponents, key = { it.name }) { component ->
            ComponentCard(component) { license -> openLicense(license.id) }
        }
    }
}

@Composable
internal fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(
            painterResource(R.drawable.ic_back),
            contentDescription = stringResource(R.string.back)
        )
    }
}

@Composable
private fun ComponentCard(component: OssComponent, onOpen: (LicenseText) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val license = component.license
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .border(1.dp, LemonFlow.colors.line, shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (license != null) {
                    Modifier.clickable(role = Role.Button) { onOpen(license) }
                } else {
                    Modifier
                }
            )
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val name = stringResource(component.name)
            Text(
                component.partOf?.let {
                    stringResource(R.string.oss_part_of, name, stringResource(it))
                }
                    ?: name,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                stringResource(component.authors),
                style = MaterialTheme.typography.bodySmall,
                color = LemonFlow.colors.muted
            )
            Text(
                license?.let { stringResource(it.title) }
                    ?: stringResource(R.string.license_public_domain),
                style = MaterialTheme.typography.labelMedium,
                color = if (license != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    LemonFlow.colors.muted
                }
            )
        }
        if (license != null) {
            Icon(
                painterResource(R.drawable.ic_chevron),
                contentDescription = null,
                tint = LemonFlow.colors.muted
            )
        }
    }
}
