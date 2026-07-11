package com.pawmino.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pawmino.app.util.DateTimeUtil
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@Composable
fun PawTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    error: String? = null,
    supporting: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = when {
            error != null -> ({ Text(error, color = MaterialTheme.colorScheme.error) })
            supporting != null -> ({ Text(supporting) })
            else -> null
        },
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    optional: Boolean = true,
    error: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }
    val display = if (value.isBlank()) "" else DateTimeUtil.displayDate(value)

    OutlinedTextField(
        value = display,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        isError = error != null,
        supportingText = if (error != null) ({ Text(error, color = MaterialTheme.colorScheme.error) }) else null,
        trailingIcon = {
            Row {
                if (optional && value.isNotBlank()) {
                    IconButton(onClick = { onChange("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear date")
                    }
                }
                IconButton(onClick = { showDialog = true }) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = "Pick date")
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    )

    if (showDialog) {
        val initialMillis = DateTimeUtil.parseDateOrNull(value)
            ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onChange(date.format(DateTimeUtil.STORAGE_DATE))
                    }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    prefer24: Boolean,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }
    val display = if (value.isBlank()) "" else DateTimeUtil.displayTime(value, prefer24)

    OutlinedTextField(
        value = display,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        isError = error != null,
        supportingText = if (error != null) ({ Text(error, color = MaterialTheme.colorScheme.error) }) else null,
        trailingIcon = {
            Row {
                if (value.isNotBlank()) {
                    IconButton(onClick = { onChange("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Clear time")
                    }
                }
                IconButton(onClick = { showDialog = true }) {
                    Icon(Icons.Outlined.Schedule, contentDescription = "Pick time")
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    )

    if (showDialog) {
        val existing = DateTimeUtil.parseTimeOrNull(value) ?: LocalTime.of(8, 0)
        val timeState = rememberTimePickerState(
            initialHour = existing.hour,
            initialMinute = existing.minute,
            is24Hour = prefer24
        )
        Dialog(onDismissRequest = { showDialog = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                TimePicker(state = timeState)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        val t = LocalTime.of(timeState.hour, timeState.minute)
                        onChange(t.format(DateTimeUtil.STORAGE_TIME))
                        showDialog = false
                    }) { Text("OK") }
                }
            }
        }
    }
}
