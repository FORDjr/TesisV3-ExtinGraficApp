package org.example.project.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn

@Composable
@OptIn(ExperimentalMaterial3Api::class)
actual fun PlatformDatePickerField(
    label: String,
    value: String?,
    onDateSelected: (String) -> Unit,
    modifier: Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val today = remember { kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val initialDate = remember(value) {
        value?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
    }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        OutlinedTextField(
            value = value.orEmpty(),
            onValueChange = {},
            label = { Text(label) },
            placeholder = { Text("YYYY-MM-DD") },
            leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
            trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
            singleLine = true,
            readOnly = true,
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { showPicker = true }
        )
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateState.selectedDateMillis?.let { millis ->
                            val selected = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.currentSystemDefault()).date
                            onDateSelected(selected.toString())
                        }
                        showPicker = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = dateState)
        }
    }
}
