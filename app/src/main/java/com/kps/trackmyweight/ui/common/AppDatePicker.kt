package com.kps.trackmyweight.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    yearRange: IntRange = 1900..2100,
) {
    var showDialog by remember { mutableStateOf(false) }
    val display = value?.let { "${it.dayOfMonth.pad2()}/${it.monthNumber.pad2()}/${it.year}" }.orEmpty()

    OutlinedTextField(
        value = display,
        onValueChange = { /* read-only */ },
        readOnly = true,
        enabled = false,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )

    if (showDialog) {
        val initialMillis = value?.atStartOfDayIn(TimeZone.currentSystemDefault())?.toEpochMilliseconds()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            yearRange = yearRange,
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        val ld = Instant.fromEpochMilliseconds(ms)
                            .toLocalDateTime(TimeZone.UTC).date
                        onValueChange(ld)
                    }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Annuler") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun Int.pad2(): String = toString().padStart(2, '0')
