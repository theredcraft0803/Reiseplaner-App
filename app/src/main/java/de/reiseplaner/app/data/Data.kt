package de.reiseplaner.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

enum class Category(val label: String, val emoji: String) {
    SIGHT("Sehenswürdigkeit", "🏛️"),
    FOOD("Essen & Trinken", "🍽️"),
    HOTEL("Unterkunft", "🏨"),
    TRANSPORT("Transport", "🚆"),
    ACTIVITY("Aktivität", "🥾"),
    OTHER("Sonstiges", "📍");

    companion object {
        fun from(name: String): Category = entries.firstOrNull { it.name == name } ?: OTHER
    }
}

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val destination: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val notes: String = ""
)

@Entity(
    tableName = "places",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val note: String = "",
    val category: String = "SIGHT",
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class TripSummary(
    @Embedded val trip: Trip,
    val total: Int,
    val doneCount: Int
)

@Dao
interface TripDao {
    @Query(
        """
        SELECT t.*,
               (SELECT COUNT(*) FROM places p WHERE p.tripId = t.id) AS total,
               (SELECT COUNT(*) FROM places p WHERE p.tripId = t.id AND p.done = 1) AS doneCount
        FROM trips t
        ORDER BY t.id DESC
        """
    )
    fun observeSummaries(): Flow<List<TripSummary>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observeTrip(id: Long): Flow<Trip?>

    @Insert
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("SELECT * FROM places WHERE tripId = :tripId ORDER BY done ASC, createdAt DESC")
    fun observePlaces(tripId: Long): Flow<List<Place>>

    @Insert
    suspend fun insertPlace(place: Place): Long

    @Insert
    suspend fun insertPlaces(places: List<Place>)

    @Update
    suspend fun updatePlace(place: Place)

    @Update
    suspend fun updatePlaces(places: List<Place>)

    @Delete
    suspend fun deletePlace(place: Place)

    @Delete
    suspend fun deletePlaces(places: List<Place>)
}

@Database(entities = [Trip::class, Place::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): TripDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reiseplaner.db"
                ).build().also { instance = it }
            }
    }
}
