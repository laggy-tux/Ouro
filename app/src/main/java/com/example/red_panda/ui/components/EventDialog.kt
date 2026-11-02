package com.example.red_panda.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventDialog(
    initialTitle: String = "",
    initialDate: String = "",
    initialLocation: String = "",
    initialNote: String = "",
    initialPlaceTag: String = "",
    initialPersonTag: String = "",
    isEditMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        date: String,
        location: String,
        note: String,
        placeTag: String,
        personTag: String,
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var dateText by remember {
        mutableStateOf(
            if (initialDate.isNotBlank()) initialDate
            else SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date())
        )
    }
    var location by remember { mutableStateOf(initialLocation) }
    var note by remember { mutableStateOf(initialNote) }
    var placeTag by remember { mutableStateOf(initialPlaceTag) }
    var personTag by remember { mutableStateOf(initialPersonTag) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dialogMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.9f

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = popupDatePickerColors(),
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        dateText = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("Select", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(state = datePickerState, colors = popupDatePickerColors())
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 480.dp)
                .heightIn(max = dialogMaxHeight)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = dialogMaxHeight)
                    .padding(24.dp)
            ) {
                Text(
                    text = if (isEditMode) "Edit Event" else "New Event",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Event Title") },
                        singleLine = true,
                        colors = popupTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Date") },
                        singleLine = true,
                        colors = popupTextFieldColors(),
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select Date")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = location,
                        onValueChange = {
                            location = it
                            if (placeTag.isEmpty()) placeTag = it
                        },
                        label = { Text("Location (e.g. Big Sur, CA)") },
                        singleLine = true,
                        colors = popupTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = placeTag,
                        onValueChange = { placeTag = it },
                        label = { Text("Place Tag") },
                        singleLine = true,
                        colors = popupTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = personTag,
                        onValueChange = { personTag = it },
                        label = { Text("People Tag") },
                        singleLine = true,
                        colors = popupTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note / Memory (optional)") },
                        minLines = 2,
                        maxLines = 4,
                        colors = popupTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = title.isNotBlank(),
                        colors = popupButtonColors(),
                        onClick = {
                            onConfirm(
                                title.trim(),
                                dateText.trim(),
                                location.trim(),
                                note.trim(),
                                placeTag.trim(),
                                personTag.trim()
                            )
                        }
                    ) {
                        Text(if (isEditMode) "Save" else "Create")
                    }
                }
            }
        }
    }
}
