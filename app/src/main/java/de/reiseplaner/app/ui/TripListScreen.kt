@file:OptIn(ExperimentalMaterial3Api::class)

package de.reiseplaner.app.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.reiseplaner.app.MainViewModel
import de.reiseplaner.app.data.TripSummary
import de.reiseplaner.app.util.dateRangeText
import de.reiseplaner.app.util.tripStatus

@Composable
fun TripListScreen(vm: MainViewModel, onOpen: (Long) -> Unit) {
    val trips by vm.summaries.collectAsStateWithLifecycle()
    var showNew by remember { mutableStateOf(false) }

    val totalPlaces = trips.sumOf { it.total }
    val totalDone = trips.sumOf { it.doneCount }
    val subtitle = "${trips.size} ${if (trips.size == 1) "Reise" else "Reisen"} · " +
        "$totalPlaces Orte · $totalDone erledigt"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Reiseplaner", fontWeight = FontWeight.Bold)
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNew = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Neue Reise") }
            )
        }
    ) { padding ->
        if (trips.isEmpty()) {
            EmptyState(
                emoji = "✈️",
                title = "Noch keine Reise",
                text = "Lege deine erste Reise an und sammle die Orte, die du besuchen willst.",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trips, key = { it.trip.id }) { summary ->
                    TripCard(summary, onClick = { onOpen(summary.trip.id) })
                }
            }
        }
    }

    if (showNew) {
        TripDialog(
            initial = null,
            onDismiss = { showNew = false },
            onSave = { trip ->
                showNew = false
                vm.saveTrip(trip) { id -> onOpen(id) }
            }
        )
    }
}

@Composable
private fun TripCard(summary: TripSummary, onClick: () -> Unit) {
    val trip = summary.trip
    val status = tripStatus(trip.startDate, trip.endDate)
    val range = dateRangeText(trip.startDate, trip.endDate)

    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) { Text("✈️", style = MaterialTheme.typography.titleMedium) }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        trip.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (trip.destination.isNotBlank()) {
                        Text(
                            "📍 ${trip.destination}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (status != null) {
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(status)
                }
            }

            if (range != null) {
                Text(
                    "🗓 $range",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (summary.total > 0) {
                LinearProgressIndicator(
                    progress = { summary.doneCount / summary.total.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = DoneGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    "${summary.doneCount} von ${summary.total} Orten erledigt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "Noch keine Orte",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
