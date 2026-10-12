package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.GeoPoint
import com.example.data.RealtimeTelemetryTab
import com.example.map.GeoUtils
import com.example.map.GoogleMapLayer
import com.example.ui.components.ApkDownloadCenterDialog
import com.example.ui.components.EmergencyPinCreationDialog
import com.example.ui.components.GoogleMapCanvas
import com.example.ui.components.MapScaleBarOverlay
import com.example.ui.components.RealtimeTelemetryDialog
import com.example.ui.components.RealtimeTelemetryQuickBar
import com.example.ui.components.SelectedEmergencyPinDetailDialog
import com.example.ui.components.SettingsAndPreferencesDialog
import com.example.ui.components.ThaiTextNoteDialog
import com.example.ui.theme.NotoSansThaiFontFamily
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalBorderDark
import com.example.ui.theme.TacticalCardDark
import com.example.ui.theme.TacticalCoralRed
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TacticalSurfaceDark
import com.example.util.ApkExporter
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun TacticalMapScreen(
    viewModel: TacticalMapViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val reporterName by viewModel.reporterName.collectAsState()
    val reporterPhone by viewModel.reporterPhone.collectAsState()
    val selectedLayer by viewModel.selectedLayer.collectAsState()
    val centerLat by viewModel.centerLat.collectAsState()
    val centerLng by viewModel.centerLng.collectAsState()
    val zoom by viewModel.zoom.collectAsState()
    val showScaleBar by viewModel.showScaleBar.collectAsState()
    val brushSizePx by viewModel.brushSizePx.collectAsState()
    val brushColorHex by viewModel.brushColorHex.collectAsState()
    val activeToolMode by viewModel.activeToolMode.collectAsState()
    val markedCoordinate by viewModel.markedCoordinate.collectAsState()
    val selectedEmergencyPin by viewModel.selectedEmergencyPin.collectAsState()
    val activeMeasurePoints by viewModel.activeMeasurePoints.collectAsState()
    val userGpsLocation by viewModel.userGpsLocation.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsState()

    val emergencyPins by viewModel.emergencyPins.collectAsState()
    val mapNotes by viewModel.mapNotes.collectAsState()
    val strokes by viewModel.strokes.collectAsState()
    val cachedTileCount by viewModel.tileEngine.cachedTileCount.collectAsState()

    val realtimeTelemetry by viewModel.realtimeTelemetry.collectAsState()
    val isLoadingTelemetry by viewModel.isLoadingTelemetry.collectAsState()
    val selectedTelemetryTab by viewModel.selectedTelemetryTab.collectAsState()
    val showRainRadarOverlay by viewModel.showRainRadarOverlay.collectAsState()
    val showFireHotspotOverlay by viewModel.showFireHotspotOverlay.collectAsState()
    val showWaterStationOverlay by viewModel.showWaterStationOverlay.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showApkDownloadDialog by remember { mutableStateOf(false) }
    var lastApkExportStatus by remember { mutableStateOf<String?>(null) }
    var showEmergencyPinDialog by remember { mutableStateOf(false) }
    var showThaiNoteDialog by remember { mutableStateOf(false) }
    var showLocationPresetMenu by remember { mutableStateOf(false) }
    var showRealtimeTelemetryDialog by remember { mutableStateOf(false) }

    // Periodic 60-second real-time auto-refresh for Weather, Rain, Fire, and Water Level
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            viewModel.refreshRealtimeTelemetry(silent = true)
        }
    }

    BackHandler(
        enabled = showRealtimeTelemetryDialog || showSettingsDialog || showApkDownloadDialog || showEmergencyPinDialog || showThaiNoteDialog || selectedEmergencyPin != null
    ) {
        when {
            showRealtimeTelemetryDialog -> showRealtimeTelemetryDialog = false
            showApkDownloadDialog -> showApkDownloadDialog = false
            showSettingsDialog -> showSettingsDialog = false
            showEmergencyPinDialog -> showEmergencyPinDialog = false
            showThaiNoteDialog -> showThaiNoteDialog = false
            selectedEmergencyPin != null -> viewModel.selectEmergencyPin(null)
        }
    }

    val saveApkDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val res = ApkExporter.saveApkToUri(context, uri)
                res.onSuccess { msg ->
                    lastApkExportStatus = msg
                    viewModel.showStatus(msg)
                }.onFailure { err ->
                    viewModel.showStatus("เกิดข้อผิดพลาด: ${err.message}")
                }
            }
        }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        viewModel.updateUserGpsLocation(location.latitude, location.longitude, true)
                    } else {
                        viewModel.showStatus("กำลังค้นหาสัญญาณดาวเทียม GPS...")
                    }
                }
        } else {
            viewModel.showStatus("ไม่ได้รับอนุญาตเข้าถึงพิกัด GPS")
        }
    }

    fun requestCurrentGpsLocation() {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        viewModel.updateUserGpsLocation(location.latitude, location.longitude, true)
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                viewModel.updateUserGpsLocation(lastLoc.latitude, lastLoc.longitude, true)
                            } else {
                                viewModel.showStatus("ไม่พบสัญญาณ GPS ล่าสุดบนอุปกรณ์")
                            }
                        }
                    }
                }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full-Screen Smooth Google Maps Online Tile Engine Canvas
        GoogleMapCanvas(
            tileEngine = viewModel.tileEngine,
            selectedLayer = selectedLayer,
            centerLat = centerLat,
            centerLng = centerLng,
            zoom = zoom,
            toolMode = activeToolMode,
            brushSizePx = brushSizePx,
            brushColorHex = brushColorHex,
            markedCoordinate = markedCoordinate,
            userGpsLocation = userGpsLocation,
            emergencyPins = emergencyPins,
            mapNotes = mapNotes,
            strokes = strokes,
            activeMeasurePoints = activeMeasurePoints,
            onPanByWorldPixels = { dx, dy -> viewModel.panByWorldPixels(dx, dy) },
            onZoomBy = { dz -> viewModel.zoomBy(dz) },
            onMapTap = { pt -> viewModel.onMapTapCoordinate(pt) },
            onSelectPin = { pin -> viewModel.selectEmergencyPin(pin) },
            onDeleteNote = { noteId -> viewModel.deleteMapNote(noteId) },
            onSaveFreehandStroke = { pts -> viewModel.saveFreehandStroke(pts) },
            realtimeTelemetry = realtimeTelemetry,
            showRainRadarOverlay = showRainRadarOverlay,
            showFireHotspotOverlay = showFireHotspotOverlay,
            showWaterStationOverlay = showWaterStationOverlay,
            onOpenTelemetryTab = { tab ->
                viewModel.setSelectedTelemetryTab(tab)
                showRealtimeTelemetryDialog = true
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Compact Top Bar + 5-Layer Google Maps Quick Switcher
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xE80A1120),
                border = BorderStroke(1.dp, TacticalBorderDark),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0E3A4C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = TacticalCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Google Maps • $reporterName",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = NotoSansThaiFontFamily,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val phoneBadge = if (reporterPhone.isNotBlank()) " • 📞 $reporterPhone" else ""
                            Text(
                                text = "${selectedLayer.shortTitle} • หมุด ${emergencyPins.size}$phoneBadge",
                                color = TacticalCyan,
                                fontSize = 10.sp,
                                fontFamily = NotoSansThaiFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Compact APK Save Button on Top Bar
                        Surface(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showApkDownloadDialog = true }
                                .testTag("topbar_apk_download_button"),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF064E3B),
                            border = BorderStroke(1.dp, TacticalEmerald)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "บันทึกไฟล์ APK พร้อมดาวน์โหลด",
                                    tint = TacticalEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "APK",
                                    color = Color.White,
                                    fontFamily = NotoSansThaiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Settings Gear Button (⚙️)
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0E3A4C))
                                .testTag("settings_gear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "ตั้งค่าระบบและแผนที่",
                                tint = TacticalCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Compact 5 Google Maps Layer Selector Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                GoogleMapLayer.entries.forEach { layer ->
                    val isSelected = selectedLayer == layer
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { viewModel.setMapLayer(layer) }
                            .testTag("quick_layer_${layer.id}"),
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSelected) TacticalCyan else Color(0xD90A1120),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) Color.White else TacticalBorderDark
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF031B24) else TacticalCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = layer.shortTitle,
                                color = if (isSelected) Color(0xFF031B24) else Color.White,
                                fontSize = 11.sp,
                                fontFamily = NotoSansThaiFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Real-Time Weather • Rain • Fire • Water Level Quick Bar & Overlay Switcher
            RealtimeTelemetryQuickBar(
                snapshot = realtimeTelemetry,
                isLoading = isLoadingTelemetry,
                showRainOverlay = showRainRadarOverlay,
                showFireOverlay = showFireHotspotOverlay,
                showWaterOverlay = showWaterStationOverlay,
                onOpenTab = { tab ->
                    viewModel.setSelectedTelemetryTab(tab)
                    showRealtimeTelemetryDialog = true
                },
                onToggleRainOverlay = { viewModel.toggleRainRadarOverlay() },
                onToggleFireOverlay = { viewModel.toggleFireHotspotOverlay() },
                onToggleWaterOverlay = { viewModel.toggleWaterStationOverlay() },
                onRefresh = { viewModel.refreshRealtimeTelemetry(silent = false) }
            )

            // Compact Status Notification Banner
            AnimatedVisibility(
                visible = statusBanner != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                statusBanner?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xEE0E3A4C),
                        border = BorderStroke(1.dp, TacticalCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // 3. Compact Right-side Map Controls (Zoom +, Zoom -, GPS Location, Preset Jump)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = Color(0xE00A1120),
                border = BorderStroke(1.dp, TacticalBorderDark)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { viewModel.zoomBy(1.0) },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("zoom_in_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "ขยายแผนที่",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.0f", zoom),
                        color = TacticalCyan,
                        fontSize = 10.sp,
                        fontFamily = NotoSansThaiFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { viewModel.zoomBy(-1.0) },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("zoom_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "ย่อแผนที่",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Box {
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = Color(0xE00A1120),
                    border = BorderStroke(1.dp, TacticalAmber)
                ) {
                    IconButton(
                        onClick = { showLocationPresetMenu = true },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("preset_locations_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "ลัดไปยังภูมิภาค",
                            tint = TacticalAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = showLocationPresetMenu,
                    onDismissRequest = { showLocationPresetMenu = false },
                    modifier = Modifier.background(TacticalSurfaceDark)
                ) {
                    viewModel.thailandPresets.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = preset.nameThai,
                                    color = Color.White,
                                    fontFamily = NotoSansThaiFontFamily,
                                    fontSize = 12.sp
                                )
                            },
                            onClick = {
                                showLocationPresetMenu = false
                                viewModel.moveToLocation(preset.lat, preset.lng, preset.zoom, true)
                            }
                        )
                    }
                }
            }

            // My Location Button (จุดตำแหน่งของฉัน)
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = Color(0xE00A1120),
                border = BorderStroke(1.dp, TacticalCyan)
            ) {
                IconButton(
                    onClick = { requestCurrentGpsLocation() },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("gps_location_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "จุดตำแหน่งของฉัน (GPS ปัจจุบัน)",
                        tint = TacticalCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Real-Time Weather, Rain, Fire & Water Level Center Button
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = Color(0xE00A1120),
                border = BorderStroke(1.dp, TacticalEmerald)
            ) {
                IconButton(
                    onClick = { showRealtimeTelemetryDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("realtime_telemetry_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Thunderstorm,
                        contentDescription = "พยากรณ์อากาศ ฝน ไฟไหม้ ระดับน้ำ เรียลไทม์",
                        tint = TacticalEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 4. Compact Bottom HUD: Scale Bar (Bottom-Left) + Field Mode Switcher + Compact Coordinate Mark Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                if (showScaleBar) {
                    MapScaleBarOverlay(
                        latitude = centerLat,
                        zoom = zoom
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (activeToolMode == FieldToolMode.MEASURE_DISTANCE && activeMeasurePoints.isNotEmpty()) {
                    val liveDist = GeoUtils.calculatePathDistanceMeters(activeMeasurePoints)
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xEE0F172A),
                        border = BorderStroke(1.dp, TacticalAmber)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "วัดระยะ: ${GeoUtils.formatDistanceThai(liveDist)}",
                                color = TacticalAmber,
                                fontSize = 11.sp,
                                fontFamily = NotoSansThaiFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { viewModel.undoLastMeasurePoint() },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Undo,
                                    contentDescription = "ย้อนกลับจุดล่าสุด",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.commitActiveMeasurementIfAny() },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "บันทึกเส้นวัดระยะ",
                                    tint = TacticalEmerald,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Compact Field Mode Selector Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xE80A1120),
                border = BorderStroke(1.dp, TacticalBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FieldToolMode.entries.forEach { mode ->
                        val selected = activeToolMode == mode
                        val icon = when (mode) {
                            FieldToolMode.NAVIGATE -> Icons.Default.Explore
                            FieldToolMode.DRAW_SHAPE -> Icons.Default.Brush
                            FieldToolMode.MEASURE_DISTANCE -> Icons.Default.Straighten
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { viewModel.setToolMode(mode) }
                                .testTag("tool_mode_${mode.name}"),
                            shape = RoundedCornerShape(9.dp),
                            color = if (selected) Color(0xFF0E3A4C) else Color.Transparent,
                            border = if (selected) BorderStroke(1.dp, TacticalCyan) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (selected) TacticalCyan else Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (mode) {
                                        FieldToolMode.NAVIGATE -> "สำรวจ/มาร์คพิกัด"
                                        FieldToolMode.DRAW_SHAPE -> "วาดเส้น (${brushSizePx}px)"
                                        FieldToolMode.MEASURE_DISTANCE -> "วัดระยะทาง"
                                    },
                                    color = if (selected) Color.White else Color(0xFFCBD5E1),
                                    fontSize = 10.sp,
                                    fontFamily = NotoSansThaiFontFamily,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Compact Coordinate Mark Box (แสดงเมื่อแตะแผนที่ พร้อมปุ่มแชร์ขนาดพอดีหน้าจอในรูปแบบ 2x2)
            AnimatedVisibility(
                visible = markedCoordinate != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                val activeCoord = markedCoordinate ?: GeoPoint(centerLat, centerLng)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xF00A1120),
                    border = BorderStroke(1.2.dp, TacticalCyan),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("marked_coordinate_box")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = TacticalCoralRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "พิกัด: ${GeoUtils.formatLatLngDecimal(activeCoord.latitude, activeCoord.longitude)}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = NotoSansThaiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val coordStr = GeoUtils.formatLatLngDecimal(activeCoord.latitude, activeCoord.longitude)
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Coordinate", coordStr))
                                        viewModel.showStatus("คัดลอกพิกัด $coordStr แล้ว")
                                    },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(TacticalCardDark)
                                        .testTag("copy_coordinate_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "คัดลอกพิกัด",
                                        tint = TacticalCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.clearMarkedCoordinate() },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(TacticalCardDark)
                                        .testTag("close_coordinate_box_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "ซ่อนกล่องพิกัด",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Adaptive Action Bar with Compact Circular Share Button
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isCompact = maxWidth < 370.dp
                            val btnHeight = if (isCompact) 32.dp else 35.dp
                            val labelFontSize = if (isCompact) 10.sp else 11.sp
                            val iconSize = if (isCompact) 12.dp else 13.dp

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { showEmergencyPinDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(btnHeight)
                                        .testTag("btn_mark_pin_emergency"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TacticalCoralRed,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(iconSize)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isCompact) "ปักหมุด" else "ปักหมุดฉุกเฉิน",
                                        fontFamily = NotoSansThaiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = labelFontSize,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showThaiNoteDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(btnHeight)
                                        .testTag("btn_mark_thai_text"),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, TacticalCyan),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color(0xFF0E3A4C),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = TacticalCyan,
                                        modifier = Modifier.size(iconSize)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isCompact) "ข้อความไทย" else "พิมพ์ข้อความไทย",
                                        fontFamily = NotoSansThaiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = labelFontSize,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = {
                                        ApkExporter.startGoogleMapsTurnByTurn(
                                            context = context,
                                            lat = activeCoord.latitude,
                                            lng = activeCoord.longitude
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(btnHeight)
                                        .testTag("btn_mark_open_google_maps"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TacticalEmerald,
                                        contentColor = Color(0xFF031B24)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = null,
                                        modifier = Modifier.size(iconSize)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isCompact) "นำทาง" else "Google Maps",
                                        fontFamily = NotoSansThaiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = labelFontSize,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Settings & Preferences Dialog (⚙️)
        if (showSettingsDialog) {
            SettingsAndPreferencesDialog(
                reporterName = reporterName,
                reporterPhone = reporterPhone,
                selectedLayer = selectedLayer,
                showScaleBar = showScaleBar,
                brushSizePx = brushSizePx,
                brushColorHex = brushColorHex,
                cachedTileCount = cachedTileCount,
                strokesAndNotesCount = strokes.size + mapNotes.size,
                emergencyPinsCount = emergencyPins.size,
                onSaveReporterProfile = { name, phone ->
                    viewModel.updateReporterProfile(name, phone, showConfirmation = true)
                },
                onSelectLayer = { layer -> viewModel.setMapLayer(layer) },
                onToggleScaleBar = { enabled -> viewModel.setShowScaleBar(enabled) },
                onSelectBrushSizePx = { px -> viewModel.setBrushSizePx(px) },
                onSelectBrushColor = { hex -> viewModel.setBrushColorHex(hex) },
                onClearTileCache = { viewModel.clearTileMemoryCache() },
                onClearDrawingsAndNotes = { viewModel.clearAllDrawingsAndNotes() },
                onClearEmergencyPins = { viewModel.clearAllEmergencyPins() },
                onDownloadApkToDevice = {
                    coroutineScope.launch {
                        val res = ApkExporter.saveApkToSystemDownloads(context)
                        res.onSuccess { msg -> viewModel.showStatus(msg) }
                            .onFailure {
                                saveApkDocumentLauncher.launch(ApkExporter.EXPORT_APK_FILENAME)
                            }
                    }
                },
                onSaveApkAsDocument = {
                    saveApkDocumentLauncher.launch(ApkExporter.EXPORT_APK_FILENAME)
                },
                onDismiss = { showSettingsDialog = false }
            )
        }

        // Emergency Pin Creation Dialog (8 Categories, editable reporter name & phone)
        if (showEmergencyPinDialog) {
            val target = markedCoordinate ?: GeoPoint(centerLat, centerLng)
            EmergencyPinCreationDialog(
                coordinate = target,
                initialReporterName = reporterName,
                initialReporterPhone = reporterPhone,
                onConfirmPin = { category, title, details, repName, repPhone ->
                    viewModel.addEmergencyPin(
                        lat = target.latitude,
                        lng = target.longitude,
                        category = category,
                        title = title,
                        details = details,
                        reporterNameInput = repName,
                        reporterPhoneInput = repPhone
                    )
                    showEmergencyPinDialog = false
                },
                onDismiss = { showEmergencyPinDialog = false }
            )
        }

        // Thai Text Note Dialog (Noto Sans Thai)
        if (showThaiNoteDialog) {
            val target = markedCoordinate ?: GeoPoint(centerLat, centerLng)
            ThaiTextNoteDialog(
                coordinate = target,
                onConfirmNote = { text, colorHex, fontSizeSp ->
                    viewModel.addThaiTextNote(
                        lat = target.latitude,
                        lng = target.longitude,
                        text = text,
                        colorHex = colorHex,
                        fontSizeSp = fontSizeSp
                    )
                    showThaiNoteDialog = false
                },
                onDismiss = { showThaiNoteDialog = false }
            )
        }

        // Selected Emergency Pin Detail Modal
        selectedEmergencyPin?.let { pin ->
            SelectedEmergencyPinDetailDialog(
                pin = pin,
                onNavigateGoogleMaps = { lat, lng, label ->
                    viewModel.selectEmergencyPin(null)
                    ApkExporter.openInGoogleMapsNavigation(context, lat, lng, label)
                },
                onCallReporterPhone = { phone ->
                    ApkExporter.dialPhoneNumber(context, phone)
                },
                onDeletePin = { id ->
                    viewModel.deleteEmergencyPin(id)
                },
                onDismiss = { viewModel.selectEmergencyPin(null) }
            )
        }

        // Dedicated APK Save & Download Center Modal (without share button)
        if (showApkDownloadDialog) {
            ApkDownloadCenterDialog(
                lastExportMessage = lastApkExportStatus,
                onDownloadToDownloadsFolder = {
                    coroutineScope.launch {
                        val res = ApkExporter.saveApkToSystemDownloads(context)
                        res.onSuccess { msg ->
                            lastApkExportStatus = msg
                            viewModel.showStatus(msg)
                        }.onFailure {
                            saveApkDocumentLauncher.launch(ApkExporter.EXPORT_APK_FILENAME)
                        }
                    }
                },
                onSaveApkChooseFolder = {
                    saveApkDocumentLauncher.launch(ApkExporter.EXPORT_APK_FILENAME)
                },
                onDismiss = { showApkDownloadDialog = false }
            )
        }

        // Real-Time Weather Forecast, Rain Radar, Fire Hotspot & Water Level Command Center Modal
        if (showRealtimeTelemetryDialog) {
            RealtimeTelemetryDialog(
                snapshot = realtimeTelemetry,
                isLoading = isLoadingTelemetry,
                selectedTab = selectedTelemetryTab,
                showRainOverlay = showRainRadarOverlay,
                showFireOverlay = showFireHotspotOverlay,
                showWaterOverlay = showWaterStationOverlay,
                onSelectTab = { tab -> viewModel.setSelectedTelemetryTab(tab) },
                onToggleRainOverlay = { viewModel.toggleRainRadarOverlay() },
                onToggleFireOverlay = { viewModel.toggleFireHotspotOverlay() },
                onToggleWaterOverlay = { viewModel.toggleWaterStationOverlay() },
                onRefresh = { viewModel.refreshRealtimeTelemetry(silent = false) },
                onFocusMapCoordinate = { lat, lng, targetZoom ->
                    viewModel.moveToLocation(lat, lng, targetZoom = targetZoom, markCoordinate = true)
                },
                onQuickPinHazard = { lat, lng, category, title, details ->
                    viewModel.moveToLocation(lat, lng, targetZoom = 14.5, markCoordinate = true)
                    viewModel.addEmergencyPin(
                        lat = lat,
                        lng = lng,
                        category = category,
                        title = title,
                        details = details,
                        reporterNameInput = reporterName,
                        reporterPhoneInput = reporterPhone
                    )
                },
                onDismiss = { showRealtimeTelemetryDialog = false }
            )
        }
    }
}
