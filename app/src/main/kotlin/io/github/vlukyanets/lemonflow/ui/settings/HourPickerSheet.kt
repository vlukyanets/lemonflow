package io.github.vlukyanets.lemonflow.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.time.UserDayCalculator
import io.github.vlukyanets.lemonflow.ui.common.LemonSheet
import io.github.vlukyanets.lemonflow.ui.common.SheetHeader
import io.github.vlukyanets.lemonflow.ui.common.hourText
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow

/** "Day ends at": whole hours 00:00-06:00 as a radio group (FR-018). */
@Composable
internal fun HourPickerSheet(selected: Int, onPick: (Int) -> Unit, onClose: () -> Unit) {
    LemonSheet(onDismiss = onClose) {
        SheetHeader(
            title = stringResource(R.string.settings_day_ends),
            subtitle = stringResource(R.string.settings_hours_range),
            onClose = onClose
        )
        Column(Modifier.selectableGroup()) {
            UserDayCalculator.DAY_END_HOURS.forEach { hour ->
                HorizontalDivider(color = LemonFlow.colors.surface2)
                PickerOption(hourText(hour), hour == selected) { onPick(hour) }
            }
        }
    }
}

/** One row of a settings radio group; the selected row is tinted and ticked. */
@Composable
internal fun PickerOption(text: String, selected: Boolean, onClick: () -> Unit) {
    val color =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = color,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
