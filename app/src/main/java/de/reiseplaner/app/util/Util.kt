package de.reiseplaner.app.util

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import de.reiseplaner.app.data.Category
import de.reiseplaner.app.data.Place
import de.reiseplaner.app.data.Trip
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

const val COORD_ERROR =
    "Ungültige Koordinaten. Beispiel: 48.7758, 9.1829 (Breite −90…90, Länge −180…180)."

object CoordParser {
    private val plain = Regex("""^(-?\d+(?:[.,]\d+)?)\s*[,;\s]\s*(-?\d+(?:[.,]\d+)?)$""")
    private val atUrl = Regex("""@(-?\d+\.\d+),(-?\d+\.\d+)""")

    /** Akzeptiert "48.7758, 9.1829", "48,7758 9,1829" oder einen Google-Maps-Link mit @lat,lon. */
    fun parse(input: String): Pair<Double, Double>? {
        val s = input.replace("°", "").replace("(", "").replace(")", "").trim()
        val m = plain.find(s) ?: atUrl.find(s) ?: return null
        val lat = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val lon = m.groupValues[2].replace(',', '.').toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return lat to lon
    }
}

fun formatCoord(v: Double): String =
    String.format(Locale.US, "%.6f", v).trimEnd('0').trimEnd('.')

fun coordText(lat: Double, lon: Double): String = "${formatCoord(lat)}, ${formatCoord(lon)}"

private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** Der Material3-DatePicker liefert Millisekunden um 00:00 UTC. */
fun utcDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()

fun formatDate(ms: Long): String = utcDate(ms).format(dateFmt)

fun dateRangeText(start: Long?, end: Long?): String? = when {
    start != null && end != null -> "${formatDate(start)} – ${formatDate(end)}"
    start != null -> "ab ${formatDate(start)}"
    end != null -> "bis ${formatDate(end)}"
    else -> null
}

fun tripStatus(start: Long?, end: Long?): String? {
    val today = LocalDate.now()
    val s = start?.let { utcDate(it) }
    val e = end?.let { utcDate(it) }
    return when {
        s != null && today.isBefore(s) -> {
            val d = ChronoUnit.DAYS.between(today, s)
            if (d == 1L) "Morgen" else "in $d Tagen"
        }
        s != null && e != null && !today.isAfter(e) -> "Unterwegs"
        (e ?: s)?.let { today.isAfter(it) } == true -> "Beendet"
        else -> null
    }
}

fun isPast(start: Long?, end: Long?): Boolean {
    val ref = end ?: start ?: return false
    return LocalDate.now().isAfter(utcDate(ref))
}

fun openInMaps(ctx: Context, lat: Double, lon: Double, label: String) {
    val q = "${formatCoord(lat)},${formatCoord(lon)}"
    val geo = Uri.parse("geo:$q?q=$q(${Uri.encode(label.ifBlank { "Ort" })})")
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, geo))
    } catch (e: ActivityNotFoundException) {
        runCatching {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$q"))
            )
        }
    }
}

fun shareTrip(ctx: Context, trip: Trip, places: List<Place>) {
    val sb = StringBuilder()
    sb.append("✈️ ").append(trip.name).append('\n')
    val meta = listOfNotNull(
        trip.destination.ifBlank { null },
        dateRangeText(trip.startDate, trip.endDate)
    )
    if (meta.isNotEmpty()) sb.append(meta.joinToString(" · ")).append('\n')
    if (places.isNotEmpty()) {
        sb.append('\n')
        places.forEach { p ->
            val c = Category.from(p.category)
            sb.append(if (p.done) "☑ " else "☐ ")
                .append(c.emoji).append(' ')
                .append(p.name.ifBlank { c.label }).append('\n')
            sb.append("   https://www.google.com/maps/search/?api=1&query=")
                .append(formatCoord(p.latitude)).append(',').append(formatCoord(p.longitude)).append('\n')
            if (p.note.isNotBlank()) sb.append("   ").append(p.note).append('\n')
        }
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, trip.name)
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    ctx.startActivity(Intent.createChooser(send, "Reise teilen"))
}

@SuppressLint("MissingPermission")
fun fetchLocation(ctx: Context, onResult: (Location?) -> Unit) {
    val client = LocationServices.getFusedLocationProviderClient(ctx)
    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
        .addOnSuccessListener { onResult(it) }
        .addOnFailureListener { onResult(null) }
}
