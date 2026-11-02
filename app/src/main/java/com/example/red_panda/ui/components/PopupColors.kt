package com.example.red_panda.ui.components

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable

@Composable
internal fun popupButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
)

@Composable
internal fun popupTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = MaterialTheme.colorScheme.onSurface,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedBorderColor = MaterialTheme.colorScheme.onSurface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    cursorColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun popupDatePickerColors() = DatePickerDefaults.colors(
    containerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    headlineContentColor = MaterialTheme.colorScheme.onSurface,
    weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    yearContentColor = MaterialTheme.colorScheme.onSurface,
    currentYearContentColor = MaterialTheme.colorScheme.onSurface,
    selectedYearContainerColor = MaterialTheme.colorScheme.primary,
    selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
    dayContentColor = MaterialTheme.colorScheme.onSurface,
    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
    todayContentColor = MaterialTheme.colorScheme.onSurface,
    todayDateBorderColor = MaterialTheme.colorScheme.onSurface
)
