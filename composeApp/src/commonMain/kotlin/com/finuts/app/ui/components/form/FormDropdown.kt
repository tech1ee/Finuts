package com.finuts.app.ui.components.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.finuts.app.theme.FinutsTypography

/**
 * Generic dropdown component for form fields.
 *
 * @param selected Currently selected item
 * @param options List of options to display
 * @param onSelect Callback when an option is selected
 * @param displayText Function to convert option to display string
 * @param modifier Modifier for the component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FormDropdown(
    selected: T,
    options: List<T>,
    onSelect: (T) -> Unit,
    displayText: (T) -> String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayText(selected),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            textStyle = FinutsTypography.bodyLarge,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = displayText(option),
                            style = FinutsTypography.bodyMedium
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Generic nullable dropdown for optional selections.
 *
 * @param selected Currently selected item (can be null)
 * @param options List of options to display
 * @param onSelect Callback when an option is selected (null for "None")
 * @param displayText Function to convert option to display string
 * @param noneText Text to display when nothing is selected
 * @param modifier Modifier for the component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FormNullableDropdown(
    selected: T?,
    options: List<T>,
    onSelect: (T?) -> Unit,
    displayText: (T) -> String,
    noneText: String = "None",
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected?.let { displayText(it) } ?: noneText,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            textStyle = FinutsTypography.bodyLarge,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // None option
            DropdownMenuItem(
                text = {
                    Text(
                        text = noneText,
                        style = FinutsTypography.bodyMedium
                    )
                },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            // Actual options
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = displayText(option),
                            style = FinutsTypography.bodyMedium
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
