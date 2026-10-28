package de.reiseplaner.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.reiseplaner.app.data.Category
import de.reiseplaner.app.data.Place
import de.reiseplaner.app.data.Trip
import de.reiseplaner.app.util.COORD_ERROR
import de.reiseplaner.app.util.CoordParser
import de.reiseplaner.app.util.coordText
import de.reiseplaner.app.util.fetchLocation

@Composable
fun TripDialog(
    initial: Trip?,
    onDismiss: () -> Unit,
    onSave: (Trip) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var destination by remember { mutableStateOf(initial?.destination ?: "") }
    var start by remember { mutableStateOf(initial?.startDate) }
    var end by remember { mutableStateOf(initial?.endDate) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Neue Reise" else "Reise bearbeiten") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Name der Reise") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Reiseziel (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                DateField("Start (optional)", start) { start = it; error = null }
                DateField("Ende (optional)", end) { end = it; error = null }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notizen (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val s = start
                val e = end
                when {
                    name.isBlank() -> error = "Bitte einen Namen eingeben."
                    s != null && e != null && e < s -> error = "Das Ende liegt vor dem Start."
                    else -> onSave(
                        (initial ?: Trip(name = "")).copy(
                            name = name.trim(),
                            destination = destination.trim(),
                            startDate = s,
                            endDate = e,
                            notes = notes.trim()
                        )
                    )
                }
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
fun PlaceDialog(
    initial: Place?,
    tripId: Long,
    onDismiss: () -> Unit,
    onSave: (Place) -> Unit
) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var coords by remember {
        mutableStateOf(initial?.let { coordText(it.latitude, it.longitude) } ?: "")
    }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var category by remember {
        mutableStateOf(Category.from(initial?.category ?: Category.SIGHT.name))
    }
    var error by remember { mutableStateOf<String?>(null) }
    var locating by remember { mutableStateOf(false) }

    fun locate() {
        locating = true
        error = null
        fetchLocation(ctx) { loc ->
            locating = false
            if (loc != null) {
                coords = coordText(loc.latitude, loc.longitude)
            } else {
                error = "Standort nicht verfügbar. Ist die Ortung aktiviert?"
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) locate() else error = "Standortberechtigung wurde nicht erteilt."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Ort hinzufügen" else "Ort bearbeiten") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = coords,
                    onValueChange = { coords = it; error = null },
                    label = { Text("Koordinaten") },
                    placeholder = { Text("48.7758, 9.1829") },
                    supportingText = { Text("Breite, Länge – oder Google-Maps-Link mit @…") },
                    isError = error != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
                            locate()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    enabled = !locating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (locating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (locating) "Suche Standort …" else "Aktuellen Standort verwenden")
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notiz (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Kategorie", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Category.entries.forEach { c ->
                        FilterChip(
                            selected = c == category,
                            onClick = { category = c },
                            label = { Text("${c.emoji} ${c.label}") }
                        )
                    }
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = CoordParser.parse(coords)
                if (parsed == null) {
                    error = COORD_ERROR
                } else {
                    onSave(
                        (initial ?: Place(tripId = tripId, name = "", latitude = 0.0, longitude = 0.0)).copy(
                            name = name.trim(),
                            latitude = parsed.first,
                            longitude = parsed.second,
                            note = note.trim(),
                            category = category.name
                        )
                    )
                }
            }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
