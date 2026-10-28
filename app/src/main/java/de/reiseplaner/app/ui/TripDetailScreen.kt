@file:OptIn(ExperimentalMaterial3Api::class)

package de.reiseplaner.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.reiseplaner.app.MainViewModel
import de.reiseplaner.app.data.Category
import de.reiseplaner.app.data.Place
import de.reiseplaner.app.data.Trip
import de.reiseplaner.app.util.coordText
import de.reiseplaner.app.util.dateRangeText
import de.reiseplaner.app.util.openInMaps
import de.reiseplaner.app.util.shareTrip
import de.reiseplaner.app.util.tripStatus
import kotlinx.coroutines.launch

private enum class PlaceFilter(val label: String) {
    ALL("Alle"), OPEN("Offen"), DONE("Erledigt")
}

@Composable
fun TripDetailScreen(vm: MainViewModel, tripId: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val trip by remember(tripId) { vm.trip(tripId) }.collectAsStateWithLifecycle(initialValue = null)
    val places by remember(tripId) { vm.places(tripId) }.collectAsStateWithLifecycle(initialValue = emptyList())

    var filter by remember { mutableStateOf(PlaceFilter.ALL) }
    var showAdd by remember { mutableStateOf(false) }
    var editPlace by remember { mutableStateOf<Place?>(null) }
    var showEditTrip by remember { mutableStateOf(false) }
    var showDeleteTrip by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val t = trip
    val doneCount = places.count { it.done }
    val visible = places.filter {
        when (filter) {
            PlaceFilter.ALL -> true
            PlaceFilter.OPEN -> !it.done
            PlaceFilter.DONE -> it.done
        }
    }

    fun showUndo(message: String, onUndo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = message,
                actionLabel = "Rückgängig",
                withDismissAction = false
            )
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { if (t != null) shareTrip(ctx, t, places) }) {
                        Icon(Icons.Default.Share, contentDescription = "Reise teilen")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Mehr")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Reise bearbeiten") },
                                onClick = { menuOpen = false; showEditTrip = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Alle zurücksetzen") },
                                onClick = {
                                    menuOpen = false
                                    val before = places.filter { it.done }
                                    if (before.isEmpty()) {
                                        scope.launch { snackbar.showSnackbar("Nichts abgehakt") }
                                    } else {
                                        vm.updatePlaces(before.map { it.copy(done = false) })
                                        showUndo("${before.size} Orte zurückgesetzt") { vm.updatePlaces(before) }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Erledigte löschen") },
                                onClick = {
                                    menuOpen = false
                                    val gone = places.filter { it.done }
                                    if (gone.isEmpty()) {
                                        scope.launch { snackbar.showSnackbar("Keine erledigten Orte") }
                                    } else {
                                        vm.deletePlaces(gone)
                                        showUndo("${gone.size} Orte gelöscht") { vm.restorePlaces(gone) }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reise löschen", color = MaterialTheme.colorScheme.error) },
                                onClick = { menuOpen = false; showDeleteTrip = true }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (t != null) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Ort hinzufügen") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (t != null) {
                item { TripHeader(t, places.size, doneCount) }
            }
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlaceFilter.entries.forEach { f ->
                        val n = when (f) {
                            PlaceFilter.ALL -> places.size
                            PlaceFilter.OPEN -> places.size - doneCount
                            PlaceFilter.DONE -> doneCount
                        }
                        FilterChip(
                            selected = filter == f,
                            onClick = { filter = f },
                            label = { Text("${f.label} $n") }
                        )
                    }
                }
            }
            if (visible.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "📍",
                        title = if (places.isEmpty()) "Noch keine Orte" else "Keine Treffer",
                        text = if (places.isEmpty()) {
                            "Füge Koordinaten hinzu oder nutze deinen aktuellen Standort."
                        } else {
                            "In diesem Filter gibt es keine Einträge."
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    )
                }
            } else {
                items(visible, key = { it.id }) { place ->
                    PlaceCard(
                        place = place,
                        onToggle = { vm.setDone(place, it) },
                        onCopy = {
                            clipboard.setText(AnnotatedString(coordText(place.latitude, place.longitude)))
                            scope.launch { snackbar.showSnackbar("Koordinaten kopiert") }
                        },
                        onMap = {
                            openInMaps(
                                ctx, place.latitude, place.longitude,
                                place.name.ifBlank { Category.from(place.category).label }
                            )
                        },
                        onEdit = { editPlace = place },
                        onDelete = {
                            vm.deletePlace(place)
                            showUndo("Ort gelöscht") { vm.restorePlace(place) }
                        }
                    )
                }
            }
        }
    }

    if (showAdd) {
        PlaceDialog(
            initial = null,
            tripId = tripId,
            onDismiss = { showAdd = false },
            onSave = { place ->
                showAdd = false
                vm.savePlace(place)
            }
        )
    }
    editPlace?.let { ep ->
        PlaceDialog(
            initial = ep,
            tripId = tripId,
            onDismiss = { editPlace = null },
            onSave = { place ->
                editPlace = null
                vm.savePlace(place)
            }
        )
    }
    if (showEditTrip && t != null) {
        TripDialog(
            initial = t,
            onDismiss = { showEditTrip = false },
            onSave = { updated ->
                showEditTrip = false
                vm.saveTrip(updated)
            }
        )
    }
    if (showDeleteTrip && t != null) {
        AlertDialog(
            onDismissRequest = { showDeleteTrip = false },
            title = { Text("Reise löschen?") },
            text = { Text("„${t.name}“ und alle ${places.size} Orte werden endgültig gelöscht.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteTrip = false
                    vm.deleteTrip(t)
                    onBack()
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteTrip = false }) { Text("Abbrechen") } }
        )
    }
}

@Composable
private fun TripHeader(trip: Trip, total: Int, done: Int) {
    val status = tripStatus(trip.startDate, trip.endDate)
    val range = dateRangeText(trip.startDate, trip.endDate)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (trip.destination.isNotBlank()) {
                        Text(
                            "📍 ${trip.destination}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (range != null) {
                        Text("🗓 $range", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (status != null) StatusBadge(status)
            }
            if (total > 0) {
                LinearProgressIndicator(
                    progress = { done / total.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = DoneGreen,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                )
                Text("$done von $total Orten erledigt", style = MaterialTheme.typography.bodySmall)
            } else {
                Text(
                    "Noch keine Orte – tippe auf „Ort hinzufügen“.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (trip.notes.isNotBlank()) {
                Text(trip.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PlaceCard(
    place: Place,
    onToggle: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onMap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cat = Category.from(place.category)

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(start = 4.dp, top = 8.dp, end = 4.dp, bottom = 4.dp)) {
            Checkbox(
                checked = place.done,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(checkedColor = DoneGreen)
            )
            Column(
                modifier = Modifier.weight(1f).alpha(if (place.done) 0.55f else 1f).padding(top = 12.dp, end = 8.dp)
            ) {
                Text(
                    "${cat.emoji}  ${place.name.ifBlank { cat.label }}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (place.done) TextDecoration.LineThrough else null
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    onClick = onCopy,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        coordText(place.latitude, place.longitude),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (place.note.isNotBlank()) {
                    Text(
                        place.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onMap) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Karte")
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen")
                    }
                }
            }
        }
    }
}
