package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.ui.theme.padding
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestDateFilterBottomSheet(
    fromDate: String?,
    toDate: String?,
    onFromDateChange: (String?) -> Unit,
    onToDateChange: (String?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            ) {
                item {
                    Text(
                        text = "Date Range",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                item {
                    DateSelectorRow(
                        label = "From date",
                        date = fromDate,
                        onClick = { showFromPicker = true },
                    )
                }

                item {
                    DateSelectorRow(
                        label = "To date",
                        date = toDate,
                        onClick = { showToPicker = true },
                    )
                }
            }

            OutlinedButton(
                onClick = onClear,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(text = "Clear filters")
            }
        }
    }

    if (showFromPicker) {
        DatePickerModal(
            initialDate = fromDate,
            onConfirm = { dateStr ->
                onFromDateChange(dateStr)
                showFromPicker = false
            },
            onDismiss = { showFromPicker = false },
        )
    }

    if (showToPicker) {
        DatePickerModal(
            initialDate = toDate,
            onConfirm = { dateStr ->
                onToDateChange(dateStr)
                showToPicker = false
            },
            onDismiss = { showToPicker = false },
        )
    }
}

@Composable
private fun DateSelectorRow(
    label: String,
    date: String?,
    onClick: () -> Unit,
) {
    val displayText = date?.let {
        runCatching {
            LocalDate.parse(it).format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        }.getOrNull() ?: it
    } ?: "Select date"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyLarge,
            color = if (date != null) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (date != null) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initialDate: String?,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialMillis = initialDate?.let {
        runCatching {
            LocalDate.parse(it).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull()
    } ?: System.currentTimeMillis()

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    val dateStr = millis?.let {
                        Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .toString()
                    }
                    onConfirm(dateStr)
                },
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}
