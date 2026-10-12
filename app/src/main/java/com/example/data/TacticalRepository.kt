package com.example.data

import kotlinx.coroutines.flow.Flow

class TacticalRepository(private val dao: TacticalDao) {
    val allEmergencyPins: Flow<List<EmergencyPinEntity>> = dao.getAllEmergencyPins()
    val allMapNotes: Flow<List<MapNoteEntity>> = dao.getAllMapNotes()
    val allStrokes: Flow<List<TacticalStrokeEntity>> = dao.getAllStrokes()

    suspend fun addEmergencyPin(pin: EmergencyPinEntity): Long {
        return dao.insertEmergencyPin(pin)
    }

    suspend fun deleteEmergencyPin(id: Long) {
        dao.deleteEmergencyPinById(id)
    }

    suspend fun clearAllEmergencyPins() {
        dao.clearAllEmergencyPins()
    }

    suspend fun addMapNote(note: MapNoteEntity): Long {
        return dao.insertMapNote(note)
    }

    suspend fun deleteMapNote(id: Long) {
        dao.deleteMapNoteById(id)
    }

    suspend fun addStroke(stroke: TacticalStrokeEntity): Long {
        return dao.insertStroke(stroke)
    }

    suspend fun deleteStroke(id: Long) {
        dao.deleteStrokeById(id)
    }

    suspend fun clearAllDrawingsAndNotes() {
        dao.clearAllStrokes()
        dao.clearAllMapNotes()
    }
}
