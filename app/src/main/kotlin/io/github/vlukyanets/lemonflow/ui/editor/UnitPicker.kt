package io.github.vlukyanets.lemonflow.ui.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.model.AmountUnit
import io.github.vlukyanets.lemonflow.ui.common.unitName

/**
 * A read-only outlined field, so it matches the Amount field next to it in shape, height
 * and label position.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnitPicker(spec: UnitSpec) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(unitName(spec.unit)),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(spec.label) },
            trailingIcon = {
                Icon(painterResource(R.drawable.ic_expand), contentDescription = null)
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag(spec.tag)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AmountUnit.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(unitName(option))) },
                    onClick = {
                        spec.onUnit(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
