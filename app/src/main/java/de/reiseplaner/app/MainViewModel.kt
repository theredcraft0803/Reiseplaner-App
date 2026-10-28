package de.reiseplaner.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.reiseplaner.app.data.AppDatabase
import de.reiseplaner.app.data.Place
import de.reiseplaner.app.data.Trip
import de.reiseplaner.app.data.TripSummary
import de.reiseplaner.app.util.isPast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).dao()

    val summaries: StateFlow<List<TripSummary>> = dao.observeSummaries()
        .map { sortTrips(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun trip(id: Long): Flow<Trip?> = dao.observeTrip(id)
    fun places(tripId: Long): Flow<List<Place>> = dao.observePlaces(tripId)

    fun saveTrip(trip: Trip, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            if (trip.id == 0L) {
                onDone(dao.insertTrip(trip))
            } else {
                dao.updateTrip(trip)
                onDone(trip.id)
            }
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch { dao.deleteTrip(trip) }
    }

    fun savePlace(place: Place) {
        viewModelScope.launch {
            if (place.id == 0L) dao.insertPlace(place) else dao.updatePlace(place)
        }
    }

    fun setDone(place: Place, done: Boolean) {
        viewModelScope.launch { dao.updatePlace(place.copy(done = done)) }
    }

    fun deletePlace(place: Place) {
        viewModelScope.launch { dao.deletePlace(place) }
    }

    fun restorePlace(place: Place) {
        viewModelScope.launch { dao.insertPlace(place) }
    }

    fun deletePlaces(places: List<Place>) {
        viewModelScope.launch { dao.deletePlaces(places) }
    }

    fun restorePlaces(places: List<Place>) {
        viewModelScope.launch { dao.insertPlaces(places) }
    }

    fun updatePlaces(places: List<Place>) {
        viewModelScope.launch { dao.updatePlaces(places) }
    }

    /** Anstehende/laufende Reisen zuerst (aufsteigend), dann vergangene (neueste zuerst), dann ohne Datum. */
    private fun sortTrips(list: List<TripSummary>): List<TripSummary> {
        fun group(s: TripSummary): Int = when {
            s.trip.startDate == null && s.trip.endDate == null -> 2
            isPast(s.trip.startDate, s.trip.endDate) -> 1
            else -> 0
        }
        return list.sortedWith(
            compareBy<TripSummary>(
                { group(it) },
                {
                    val key = it.trip.startDate ?: it.trip.endDate ?: 0L
                    if (group(it) == 1) -key else key
                }
            )
        )
    }
}
