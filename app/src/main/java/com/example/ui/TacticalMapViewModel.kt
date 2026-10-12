package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EmergencyCategory
import com.example.data.EmergencyPinEntity
import com.example.data.GeoPoint
import com.example.data.MapNoteEntity
import com.example.data.RealtimeTelemetryService
import com.example.data.RealtimeTelemetrySnapshot
import com.example.data.RealtimeTelemetryTab
import com.example.data.TacticalDatabase
import com.example.data.TacticalRepository
import com.example.data.TacticalStrokeEntity
import com.example.map.GeoUtils
import com.example.map.GoogleMapLayer
import com.example.map.GoogleMapTileEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FieldToolMode(val titleThai: String, val subtitleThai: String) {
    NAVIGATE(
        titleThai = "สำรวจแผนที่ (Navigate)",
        subtitleThai = "แตะแผนที่เพื่อมาร์คพิกัด ปักหมุด พิมพ์ไทย หรือนำทาง"
    ),
    DRAW_SHAPE(
        titleThai = "วาดรูปทรงยุทธวิธี",
        subtitleThai = "ลากนิ้วบนแผนที่เพื่อวาดเส้นยุทธวิธีตามขนาดแปรง"
    ),
    MEASURE_DISTANCE(
        titleThai = "วัดระยะทางจริง",
        subtitleThai = "แตะจุดบนแผนที่ต่อเนื่องเพื่อคำนวณระยะทางจริง"
    )
}

data class QuickLocationPreset(
    val nameThai: String,
    val lat: Double,
    val lng: Double,
    val zoom: Double = 14.0
)

class TacticalMapViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("e23wcm_user_prefs", Context.MODE_PRIVATE)
    private val database = TacticalDatabase.getInstance(application)
    private val repository = TacticalRepository(database.tacticalDao())
    private val telemetryService = RealtimeTelemetryService()
    val tileEngine = GoogleMapTileEngine()

    val emergencyPins: StateFlow<List<EmergencyPinEntity>> = repository.allEmergencyPins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mapNotes: StateFlow<List<MapNoteEntity>> = repository.allMapNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val strokes: StateFlow<List<TacticalStrokeEntity>> = repository.allStrokes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _reporterName = MutableStateFlow(
        prefs.getString("reporter_name", "E23WCM ช่างแมน") ?: "E23WCM ช่างแมน"
    )
    val reporterName: StateFlow<String> = _reporterName.asStateFlow()

    private val _reporterPhone = MutableStateFlow(
        prefs.getString("reporter_phone", "") ?: ""
    )
    val reporterPhone: StateFlow<String> = _reporterPhone.asStateFlow()

    private val _selectedLayer = MutableStateFlow(GoogleMapLayer.ROADMAP)
    val selectedLayer: StateFlow<GoogleMapLayer> = _selectedLayer.asStateFlow()

    private val _centerLat = MutableStateFlow(13.756331)
    val centerLat: StateFlow<Double> = _centerLat.asStateFlow()

    private val _centerLng = MutableStateFlow(100.501762)
    val centerLng: StateFlow<Double> = _centerLng.asStateFlow()

    private val _zoom = MutableStateFlow(13.0)
    val zoom: StateFlow<Double> = _zoom.asStateFlow()

    private val _showScaleBar = MutableStateFlow(true)
    val showScaleBar: StateFlow<Boolean> = _showScaleBar.asStateFlow()

    // ขนาดเส้นหัวแปรงวาดรูปทรง: 2px, 4px, 6px, 8px
    private val _brushSizePx = MutableStateFlow(4)
    val brushSizePx: StateFlow<Int> = _brushSizePx.asStateFlow()

    private val _brushColorHex = MutableStateFlow(0xFFEF4444L)
    val brushColorHex: StateFlow<Long> = _brushColorHex.asStateFlow()

    private val _activeToolMode = MutableStateFlow(FieldToolMode.NAVIGATE)
    val activeToolMode: StateFlow<FieldToolMode> = _activeToolMode.asStateFlow()

    // พิกัดที่แตะมาร์คบนแผนที่
    private val _markedCoordinate = MutableStateFlow<GeoPoint?>(GeoPoint(13.756331, 100.501762))
    val markedCoordinate: StateFlow<GeoPoint?> = _markedCoordinate.asStateFlow()

    // หมุดเหตุฉุกเฉินที่กำลังเลือกดูรายละเอียด
    private val _selectedEmergencyPin = MutableStateFlow<EmergencyPinEntity?>(null)
    val selectedEmergencyPin: StateFlow<EmergencyPinEntity?> = _selectedEmergencyPin.asStateFlow()

    // จุดกำลังวัดระยะชั่วคราวก่อนบันทึก
    private val _activeMeasurePoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val activeMeasurePoints: StateFlow<List<GeoPoint>> = _activeMeasurePoints.asStateFlow()

    // ตำแหน่ง GPS ปัจจุบันของผู้ใช้
    private val _userGpsLocation = MutableStateFlow<GeoPoint?>(null)
    val userGpsLocation: StateFlow<GeoPoint?> = _userGpsLocation.asStateFlow()

    // แถบแจ้งเตือนสถานะการทำงาน
    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    // ข้อมูลเรียลไทม์: พยากรณ์อากาศ ฝน ไฟไหม้ และระดับน้ำ
    private val _realtimeTelemetry = MutableStateFlow<RealtimeTelemetrySnapshot?>(null)
    val realtimeTelemetry: StateFlow<RealtimeTelemetrySnapshot?> = _realtimeTelemetry.asStateFlow()

    private val _isLoadingTelemetry = MutableStateFlow(false)
    val isLoadingTelemetry: StateFlow<Boolean> = _isLoadingTelemetry.asStateFlow()

    private val _selectedTelemetryTab = MutableStateFlow(RealtimeTelemetryTab.WEATHER)
    val selectedTelemetryTab: StateFlow<RealtimeTelemetryTab> = _selectedTelemetryTab.asStateFlow()

    private val _showRainRadarOverlay = MutableStateFlow(true)
    val showRainRadarOverlay: StateFlow<Boolean> = _showRainRadarOverlay.asStateFlow()

    private val _showFireHotspotOverlay = MutableStateFlow(true)
    val showFireHotspotOverlay: StateFlow<Boolean> = _showFireHotspotOverlay.asStateFlow()

    private val _showWaterStationOverlay = MutableStateFlow(true)
    val showWaterStationOverlay: StateFlow<Boolean> = _showWaterStationOverlay.asStateFlow()

    val thailandPresets = listOf(
        QuickLocationPreset("กรุงเทพฯ (ศูนย์กลาง)", 13.756331, 100.501762, 13.0),
        QuickLocationPreset("เชียงใหม่ (ภาคเหนือ)", 18.788343, 98.985300, 13.0),
        QuickLocationPreset("ขอนแก่น (ภาคอีสาน)", 16.432194, 102.823621, 13.0),
        QuickLocationPreset("ชลบุรี / พัทยา (ภาคตะวันออก)", 12.923556, 100.882455, 13.0),
        QuickLocationPreset("ภูเก็ต (ภาคใต้)", 7.880448, 98.392250, 13.0)
    )

    init {
        refreshRealtimeTelemetry(
            lat = _centerLat.value,
            lng = _centerLng.value,
            silent = true
        )
    }

    fun refreshRealtimeTelemetry(
        lat: Double = _markedCoordinate.value?.latitude ?: _centerLat.value,
        lng: Double = _markedCoordinate.value?.longitude ?: _centerLng.value,
        silent: Boolean = false
    ) {
        viewModelScope.launch {
            _isLoadingTelemetry.value = true
            try {
                val snapshot = telemetryService.fetchRealtimeTelemetry(lat, lng)
                _realtimeTelemetry.value = snapshot
                if (!silent) {
                    showStatus("อัปเดตพยากรณ์อากาศ ฝน ไฟไหม้ ระดับน้ำ (${snapshot.updatedTimeFormatted} น.)")
                }
            } finally {
                _isLoadingTelemetry.value = false
            }
        }
    }

    fun setSelectedTelemetryTab(tab: RealtimeTelemetryTab) {
        _selectedTelemetryTab.value = tab
    }

    fun toggleRainRadarOverlay() {
        val next = !_showRainRadarOverlay.value
        _showRainRadarOverlay.value = next
        showStatus(if (next) "เปิดแสดงเรดาร์กลุ่มฝนเรียลไทม์บนแผนที่" else "ซ่อนเรดาร์กลุ่มฝนบนแผนที่")
    }

    fun toggleFireHotspotOverlay() {
        val next = !_showFireHotspotOverlay.value
        _showFireHotspotOverlay.value = next
        showStatus(if (next) "เปิดแสดงจุดความร้อน/เสี่ยงไฟไหม้บนแผนที่" else "ซ่อนจุดความร้อนไฟไหม้บนแผนที่")
    }

    fun toggleWaterStationOverlay() {
        val next = !_showWaterStationOverlay.value
        _showWaterStationOverlay.value = next
        showStatus(if (next) "เปิดแสดงสถานีวัดระดับน้ำเรียลไทม์บนแผนที่" else "ซ่อนสถานีวัดระดับน้ำบนแผนที่")
    }

    fun updateReporterProfile(name: String, phone: String, showConfirmation: Boolean = true) {
        val cleanName = name.trim().ifEmpty { "ผู้แจ้งเหตุภาคสนาม" }
        val cleanPhone = phone.trim()
        _reporterName.value = cleanName
        _reporterPhone.value = cleanPhone
        prefs.edit()
            .putString("reporter_name", cleanName)
            .putString("reporter_phone", cleanPhone)
            .apply()
        if (showConfirmation) {
            val phoneSuffix = if (cleanPhone.isNotEmpty()) " ($cleanPhone)" else ""
            showStatus("บันทึกชื่อผู้แจ้ง: $cleanName$phoneSuffix เรียบร้อย")
        }
    }

    fun setMapLayer(layer: GoogleMapLayer) {
        _selectedLayer.value = layer
        showStatus("สลับชั้นข้อมูลเป็น ${layer.titleThai}")
    }

    fun setShowScaleBar(enabled: Boolean) {
        _showScaleBar.value = enabled
        val msg = if (enabled) "เปิดแสดงแถบสเกลวัดระยะทางจริง" else "ซ่อนแถบสเกลวัดระยะทางจริง"
        showStatus(msg)
    }

    fun setBrushSizePx(sizePx: Int) {
        val valid = if (sizePx in listOf(2, 4, 6, 8)) sizePx else 4
        _brushSizePx.value = valid
        showStatus("ตั้งขนาดเส้นหัวแปรงเป็น ${valid}px")
    }

    fun setBrushColorHex(colorHex: Long) {
        _brushColorHex.value = colorHex
    }

    fun setToolMode(mode: FieldToolMode) {
        if (_activeToolMode.value == FieldToolMode.MEASURE_DISTANCE && mode != FieldToolMode.MEASURE_DISTANCE) {
            commitActiveMeasurementIfAny()
        }
        _activeToolMode.value = mode
        showStatus("โหมด: ${mode.titleThai}")
    }

    fun panByWorldPixels(dxWorld: Double, dyWorld: Double) {
        val z = _zoom.value
        val (curPx, curPy) = GeoUtils.latLngToWorldPixel(_centerLat.value, _centerLng.value, z)
        val updated = GeoUtils.worldPixelToLatLng(curPx + dxWorld, curPy + dyWorld, z)
        _centerLat.value = updated.latitude
        _centerLng.value = updated.longitude
    }

    fun zoomBy(deltaZoom: Double) {
        _zoom.value = (_zoom.value + deltaZoom).coerceIn(3.0, 20.0)
    }

    fun moveToLocation(lat: Double, lng: Double, targetZoom: Double? = null, markCoordinate: Boolean = true) {
        _centerLat.value = GeoUtils.clampLat(lat)
        _centerLng.value = GeoUtils.normalizeLng(lng)
        if (targetZoom != null) {
            _zoom.value = targetZoom.coerceIn(3.0, 20.0)
        }
        if (markCoordinate) {
            _markedCoordinate.value = GeoPoint(_centerLat.value, _centerLng.value)
        }
        refreshRealtimeTelemetry(_centerLat.value, _centerLng.value, silent = true)
    }

    fun onMapTapCoordinate(point: GeoPoint) {
        when (_activeToolMode.value) {
            FieldToolMode.NAVIGATE -> {
                _selectedEmergencyPin.value = null
                _markedCoordinate.value = point
                refreshRealtimeTelemetry(point.latitude, point.longitude, silent = true)
            }
            FieldToolMode.MEASURE_DISTANCE -> {
                _activeMeasurePoints.value = _activeMeasurePoints.value + point
            }
            FieldToolMode.DRAW_SHAPE -> {
                // Handled by drag gesture
            }
        }
    }

    fun clearMarkedCoordinate() {
        _markedCoordinate.value = null
    }

    fun selectEmergencyPin(pin: EmergencyPinEntity?) {
        _selectedEmergencyPin.value = pin
        if (pin != null) {
            _centerLat.value = pin.latitude
            _centerLng.value = pin.longitude
        }
    }

    fun saveFreehandStroke(points: List<GeoPoint>) {
        if (points.size < 2) return
        val dist = GeoUtils.calculatePathDistanceMeters(points)
        val stroke = TacticalStrokeEntity(
            isMeasurement = false,
            strokeWidthPx = _brushSizePx.value,
            colorHex = _brushColorHex.value,
            serializedPoints = TacticalStrokeEntity.encodePoints(points),
            totalDistanceMeters = dist,
            label = "เส้นวาด (${_brushSizePx.value}px)"
        )
        viewModelScope.launch {
            repository.addStroke(stroke)
        }
    }

    fun commitActiveMeasurementIfAny() {
        val pts = _activeMeasurePoints.value
        if (pts.size >= 2) {
            val dist = GeoUtils.calculatePathDistanceMeters(pts)
            val formatted = GeoUtils.formatDistanceThai(dist)
            val stroke = TacticalStrokeEntity(
                isMeasurement = true,
                strokeWidthPx = _brushSizePx.value,
                colorHex = 0xFFF59E0BL,
                serializedPoints = TacticalStrokeEntity.encodePoints(pts),
                totalDistanceMeters = dist,
                label = formatted
            )
            viewModelScope.launch {
                repository.addStroke(stroke)
                showStatus("บันทึกเส้นวัดระยะทาง $formatted เรียบร้อย")
            }
        }
        _activeMeasurePoints.value = emptyList()
    }

    fun undoLastMeasurePoint() {
        val current = _activeMeasurePoints.value
        if (current.isNotEmpty()) {
            _activeMeasurePoints.value = current.dropLast(1)
        }
    }

    fun addEmergencyPin(
        lat: Double,
        lng: Double,
        category: EmergencyCategory,
        title: String,
        details: String,
        reporterNameInput: String,
        reporterPhoneInput: String,
        onPinCreated: ((EmergencyPinEntity) -> Unit)? = null
    ) {
        val cleanReporter = reporterNameInput.trim().ifEmpty { _reporterName.value }
        val cleanPhone = reporterPhoneInput.trim()
        updateReporterProfile(cleanReporter, cleanPhone, showConfirmation = false)

        viewModelScope.launch {
            val cleanTitle = title.trim().ifEmpty { category.shortTitle }
            val pin = EmergencyPinEntity(
                latitude = lat,
                longitude = lng,
                categoryCode = category.code,
                title = cleanTitle,
                details = details.trim().ifEmpty { "แจ้งเหตุภาคสนามโดย $cleanReporter" },
                callsign = cleanReporter,
                reporterPhone = cleanPhone
            )
            val newId = repository.addEmergencyPin(pin)
            val savedPin = pin.copy(id = newId)
            showStatus("ปักหมุดเหตุฉุกเฉิน: $cleanTitle สำเร็จ")
            onPinCreated?.invoke(savedPin)
        }
    }

    fun deleteEmergencyPin(id: Long) {
        viewModelScope.launch {
            repository.deleteEmergencyPin(id)
            if (_selectedEmergencyPin.value?.id == id) {
                _selectedEmergencyPin.value = null
            }
            showStatus("ลบหมุดเหตุฉุกเฉินเรียบร้อยแล้ว")
        }
    }

    fun addThaiTextNote(
        lat: Double,
        lng: Double,
        text: String,
        colorHex: Long,
        fontSizeSp: Int
    ) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repository.addMapNote(
                MapNoteEntity(
                    latitude = lat,
                    longitude = lng,
                    text = clean,
                    colorHex = colorHex,
                    fontSizeSp = fontSizeSp
                )
            )
            showStatus("บันทึกข้อความไทย (Noto Sans Thai) สำเร็จ")
        }
    }

    fun deleteMapNote(id: Long) {
        viewModelScope.launch {
            repository.deleteMapNote(id)
            showStatus("ลบข้อความบนแผนที่แล้ว")
        }
    }

    fun clearTileMemoryCache() {
        val count = tileEngine.clearMemoryCache()
        showStatus("ล้างแคชแผนที่ ($count ไทล์) และโหลดใหม่จาก Google Maps แล้ว")
    }

    fun clearAllDrawingsAndNotes() {
        _activeMeasurePoints.value = emptyList()
        viewModelScope.launch {
            repository.clearAllDrawingsAndNotes()
            showStatus("ลบรูปวาด เส้นวัดระยะ และข้อความทั้งหมดเรียบร้อย")
        }
    }

    fun clearAllEmergencyPins() {
        _selectedEmergencyPin.value = null
        viewModelScope.launch {
            repository.clearAllEmergencyPins()
            showStatus("ลบหมุดเหตุฉุกเฉินทั้งหมดในฐานข้อมูลเรียบร้อย")
        }
    }

    fun updateUserGpsLocation(lat: Double, lng: Double, centerMap: Boolean = true) {
        val pt = GeoPoint(lat, lng)
        _userGpsLocation.value = pt
        if (centerMap) {
            moveToLocation(lat, lng, targetZoom = 16.0, markCoordinate = true)
            showStatus("ระบุพิกัด GPS ปัจจุบันสำเร็จ")
        }
    }

    fun showStatus(message: String) {
        viewModelScope.launch {
            _statusBanner.value = message
            delay(2800)
            if (_statusBanner.value == message) {
                _statusBanner.value = null
            }
        }
    }
}
