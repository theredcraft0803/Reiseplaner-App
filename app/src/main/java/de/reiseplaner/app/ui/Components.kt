@file:OptIn(ExperimentalMaterial3Api::class)

package de.reiseplaner.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.reiseplaner.app.util.formatDate

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun StatusBadge(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DateField(label: String, value: Long?, onChange: (Long?) -> Unit) {
    var open by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value?.let { formatDate(it) } ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            Row {
                if (value != null) {
                    IconButton(onClick = { onChange(null) }) {
                        Icon(Icons.Default.Close, contentDescription = "Datum entfernen")
                    }
                }
                IconButton(onClick = { open = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Datum wählen")
                }
            }
        }
    )

    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(state.selectedDateMillis)
                    open = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Abbrechen") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
