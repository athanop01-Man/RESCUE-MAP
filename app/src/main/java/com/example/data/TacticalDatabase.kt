package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface TacticalDao {
    @Query("SELECT * FROM emergency_pins ORDER BY timestamp DESC")
    fun getAllEmergencyPins(): Flow<List<EmergencyPinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyPin(pin: EmergencyPinEntity): Long

    @Query("DELETE FROM emergency_pins WHERE id = :id")
    suspend fun deleteEmergencyPinById(id: Long)

    @Query("DELETE FROM emergency_pins")
    suspend fun clearAllEmergencyPins()

    @Query("SELECT * FROM map_thai_notes ORDER BY timestamp DESC")
    fun getAllMapNotes(): Flow<List<MapNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapNote(note: MapNoteEntity): Long

    @Query("DELETE FROM map_thai_notes WHERE id = :id")
    suspend fun deleteMapNoteById(id: Long)

    @Query("DELETE FROM map_thai_notes")
    suspend fun clearAllMapNotes()

    @Query("SELECT * FROM tactical_strokes ORDER BY timestamp ASC")
    fun getAllStrokes(): Flow<List<TacticalStrokeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStroke(stroke: TacticalStrokeEntity): Long

    @Query("DELETE FROM tactical_strokes WHERE id = :id")
    suspend fun deleteStrokeById(id: Long)

    @Query("DELETE FROM tactical_strokes")
    suspend fun clearAllStrokes()
}

@Database(
    entities = [
        EmergencyPinEntity::class,
        MapNoteEntity::class,
        TacticalStrokeEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TacticalDatabase : RoomDatabase() {
    abstract fun tacticalDao(): TacticalDao

    companion object {
        @Volatile
        private var INSTANCE: TacticalDatabase? = null

        fun getInstance(context: Context): TacticalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TacticalDatabase::class.java,
                    "e23wcm_tactical_map.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
