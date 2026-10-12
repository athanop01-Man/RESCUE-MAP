package com.example.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

enum class EmergencyCategory(
    val code: String,
    val titleThai: String,
    val shortTitle: String,
    val badgeSymbol: String,
    val defaultColorHex: Long
) {
    ACCIDENT(
        code = "ACCIDENT",
        titleThai = "อุบัติเหตุจราจร / กู้ภัยทางถนน",
        shortTitle = "อุบัติเหตุจราจร",
        badgeSymbol = "🚗",
        defaultColorHex = 0xFFEF4444
    ),
    FIRE(
        code = "FIRE",
        titleThai = "อัคคีภัย / เพลิงไหม้",
        shortTitle = "อัคคีภัย",
        badgeSymbol = "🔥",
        defaultColorHex = 0xFFF97316
    ),
    FLOOD(
        code = "FLOOD",
        titleThai = "อุทกภัย / กู้ภัยทางน้ำ",
        shortTitle = "อุทกภัย/น้ำท่วม",
        badgeSymbol = "🌊",
        defaultColorHex = 0xFF0EA5E9
    ),
    MEDICAL(
        code = "MEDICAL",
        titleThai = "ผู้ป่วยฉุกเฉิน / กู้ชีพการแพทย์",
        shortTitle = "การแพทย์ฉุกเฉิน",
        badgeSymbol = "🚑",
        defaultColorHex = 0xFFEC4899
    ),
    HAZMAT(
        code = "HAZMAT",
        titleThai = "สารเคมีรั่วไหล / วัตถุอันตราย",
        shortTitle = "สารเคมี/วัตถุอันตราย",
        badgeSymbol = "☣️",
        defaultColorHex = 0xFFEAB308
    ),
    CRIME(
        code = "CRIME",
        titleThai = "เหตุอาชญากรรม / ความมั่นคง",
        shortTitle = "ความมั่นคง/อาชญากรรม",
        badgeSymbol = "🚨",
        defaultColorHex = 0xFF8B5CF6
    ),
    COLLAPSE(
        code = "COLLAPSE",
        titleThai = "อาคารถล่ม / ดินโคลนถล่ม",
        shortTitle = "อาคารถล่ม/ดินถล่ม",
        badgeSymbol = "⚠️",
        defaultColorHex = 0xFFD97706
    ),
    COMMAND_POST(
        code = "COMMAND_POST",
        titleThai = "จุดรวมพล / ศูนย์บัญชาการ E23WCM",
        shortTitle = "ศูนย์บัญชาการ",
        badgeSymbol = "📡",
        defaultColorHex = 0xFF10B981
    );

    companion object {
        fun fromCode(code: String): EmergencyCategory {
            return entries.firstOrNull { it.code == code } ?: ACCIDENT
        }
    }
}

@Entity(tableName = "emergency_pins")
data class EmergencyPinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val categoryCode: String,
    val title: String,
    val details: String,
    val callsign: String = "E23WCM ช่างแมน",
    val reporterPhone: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "map_thai_notes")
data class MapNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val text: String,
    val colorHex: Long = 0xFF06B6D4,
    val fontSizeSp: Int = 14,
    val timestamp: Long = System.currentTimeMillis()
)

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

@Entity(tableName = "tactical_strokes")
data class TacticalStrokeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val isMeasurement: Boolean,
    val strokeWidthPx: Int,
    val colorHex: Long,
    val serializedPoints: String,
    val totalDistanceMeters: Double = 0.0,
    val label: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    @Ignore
    val cachedPoints: List<GeoPoint> = parsePoints(serializedPoints)

    fun toGeoPoints(): List<GeoPoint> = cachedPoints

    companion object {
        private fun parsePoints(raw: String): List<GeoPoint> {
            if (raw.isBlank()) return emptyList()
            return raw.split(";").mapNotNull { token ->
                val commaIdx = token.indexOf(',')
                if (commaIdx > 0 && commaIdx < token.length - 1) {
                    val lat = token.substring(0, commaIdx).toDoubleOrNull()
                    val lng = token.substring(commaIdx + 1).toDoubleOrNull()
                    if (lat != null && lng != null) GeoPoint(lat, lng) else null
                } else null
            }
        }

        fun encodePoints(points: List<GeoPoint>): String {
            return points.joinToString(";") { "${it.latitude},${it.longitude}" }
        }
    }
}
