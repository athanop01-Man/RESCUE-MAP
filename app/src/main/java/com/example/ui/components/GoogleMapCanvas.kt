package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EmergencyCategory
import com.example.data.EmergencyPinEntity
import com.example.data.GeoPoint
import com.example.data.MapNoteEntity
import com.example.data.RealtimeTelemetryService
import com.example.data.RealtimeTelemetrySnapshot
import com.example.data.RealtimeTelemetryTab
import com.example.data.TacticalStrokeEntity
import com.example.map.GeoUtils
import com.example.map.GoogleMapLayer
import com.example.map.GoogleMapTileEngine
import com.example.ui.FieldToolMode
import com.example.ui.theme.NotoSansThaiFontFamily
import com.example.ui.theme.TacticalCyan
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun GoogleMapCanvas(
    tileEngine: GoogleMapTileEngine,
    selectedLayer: GoogleMapLayer,
    centerLat: Double,
    centerLng: Double,
    zoom: Double,
    toolMode: FieldToolMode,
    brushSizePx: Int,
    brushColorHex: Long,
    markedCoordinate: GeoPoint?,
    userGpsLocation: GeoPoint?,
    emergencyPins: List<EmergencyPinEntity>,
    mapNotes: List<MapNoteEntity>,
    strokes: List<TacticalStrokeEntity>,
    activeMeasurePoints: List<GeoPoint>,
    onPanByWorldPixels: (Double, Double) -> Unit,
    onZoomBy: (Double) -> Unit,
    onMapTap: (GeoPoint) -> Unit,
    onSelectPin: (EmergencyPinEntity) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onSaveFreehandStroke: (List<GeoPoint>) -> Unit,
    realtimeTelemetry: RealtimeTelemetrySnapshot? = null,
    showRainRadarOverlay: Boolean = false,
    showFireHotspotOverlay: Boolean = false,
    showWaterStationOverlay: Boolean = false,
    onOpenTelemetryTab: ((RealtimeTelemetryTab) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val tileLoadedTick by tileEngine.tileLoadedTick.collectAsState()
    val cacheGeneration by tileEngine.cacheGeneration.collectAsState()

    var viewportWidth by remember { mutableFloatStateOf(1080f) }
    var viewportHeight by remember { mutableFloatStateOf(1920f) }

    val activeFreehandPoints = remember { mutableStateListOf<GeoPoint>() }

    // Keep latest values via rememberUpdatedState so pointerInput coroutines NEVER restart mid-gesture!
    val currentCenterLat by rememberUpdatedState(centerLat)
    val currentCenterLng by rememberUpdatedState(centerLng)
    val currentZoom by rememberUpdatedState(zoom)
    val currentPanCallback by rememberUpdatedState(onPanByWorldPixels)
    val currentZoomCallback by rememberUpdatedState(onZoomBy)
    val currentTapCallback by rememberUpdatedState(onMapTap)
    val currentSaveStrokeCallback by rememberUpdatedState(onSaveFreehandStroke)

    val intZoom = floor(zoom).toInt().coerceIn(2, 19)
    val fractionalScale = 2.0.pow(zoom - intZoom).toFloat()
    val effectiveScale = density * fractionalScale

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF091326))
            .onSizeChanged { size ->
                if (size.width > 0 && size.height > 0) {
                    viewportWidth = size.width.toFloat()
                    viewportHeight = size.height.toFloat()
                }
            }
            .testTag("google_map_canvas")
            // Smooth continuous pan & pinch-zoom without gesture coroutine restarts
            .pointerInput(toolMode) {
                if (toolMode != FieldToolMode.DRAW_SHAPE) {
                    detectTransformGestures(panZoomLock = false) { _, pan, gestureZoom, _ ->
                        if (gestureZoom != 1f && gestureZoom > 0f) {
                            val zoomDelta = ln(gestureZoom.toDouble()) / ln(2.0)
                            currentZoomCallback(zoomDelta)
                        }
                        if (pan.x != 0f || pan.y != 0f) {
                            val dxWorld = -pan.x.toDouble() / density
                            val dyWorld = -pan.y.toDouble() / density
                            currentPanCallback(dxWorld, dyWorld)
                        }
                    }
                } else {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            activeFreehandPoints.clear()
                            val pt = GeoUtils.screenToLatLng(
                                screenX = startOffset.x,
                                screenY = startOffset.y,
                                viewportWidth = viewportWidth,
                                viewportHeight = viewportHeight,
                                centerLat = currentCenterLat,
                                centerLng = currentCenterLng,
                                zoom = currentZoom,
                                densityScale = density
                            )
                            activeFreehandPoints.add(pt)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val pt = GeoUtils.screenToLatLng(
                                screenX = change.position.x,
                                screenY = change.position.y,
                                viewportWidth = viewportWidth,
                                viewportHeight = viewportHeight,
                                centerLat = currentCenterLat,
                                centerLng = currentCenterLng,
                                zoom = currentZoom,
                                densityScale = density
                            )
                            activeFreehandPoints.add(pt)
                        },
                        onDragEnd = {
                            if (activeFreehandPoints.size >= 2) {
                                currentSaveStrokeCallback(activeFreehandPoints.toList())
                            }
                            activeFreehandPoints.clear()
                        },
                        onDragCancel = {
                            activeFreehandPoints.clear()
                        }
                    )
                }
            }
            // Tap & Double-tap gesture (keyed only on toolMode so it never lags)
            .pointerInput(toolMode) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        val tappedGeo = GeoUtils.screenToLatLng(
                            screenX = tapOffset.x,
                            screenY = tapOffset.y,
                            viewportWidth = viewportWidth,
                            viewportHeight = viewportHeight,
                            centerLat = currentCenterLat,
                            centerLng = currentCenterLng,
                            zoom = currentZoom,
                            densityScale = density
                        )
                        currentTapCallback(tappedGeo)
                        currentZoomCallback(1.0)
                    },
                    onTap = { tapOffset ->
                        val tappedGeo = GeoUtils.screenToLatLng(
                            screenX = tapOffset.x,
                            screenY = tapOffset.y,
                            viewportWidth = viewportWidth,
                            viewportHeight = viewportHeight,
                            centerLat = currentCenterLat,
                            centerLng = currentCenterLng,
                            zoom = currentZoom,
                            densityScale = density
                        )
                        currentTapCallback(tappedGeo)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tickRead = tileLoadedTick + cacheGeneration
            if (tickRead < 0L) return@Canvas

            val canvasW = size.width
            val canvasH = size.height

            val (centerPxIntZoom, centerPyIntZoom) = GeoUtils.latLngToWorldPixel(
                centerLat,
                centerLng,
                intZoom.toDouble()
            )

            val scaledTileSize = (GeoUtils.TILE_SIZE * effectiveScale).toFloat()
            val halfWWorld = (canvasW / 2f) / effectiveScale
            val halfHWorld = (canvasH / 2f) / effectiveScale

            val minTileX = floor((centerPxIntZoom - halfWWorld) / GeoUtils.TILE_SIZE).toInt()
            val maxTileX = ceil((centerPxIntZoom + halfWWorld) / GeoUtils.TILE_SIZE).toInt()
            val minTileY = floor((centerPyIntZoom - halfHWorld) / GeoUtils.TILE_SIZE).toInt()
            val maxTileY = ceil((centerPyIntZoom + halfHWorld) / GeoUtils.TILE_SIZE).toInt()

            val maxTileIndex = 1 shl intZoom

            // 1. Draw real-time Google Maps Tiles
            for (ty in minTileY..maxTileY) {
                if (ty < 0 || ty >= maxTileIndex) continue
                for (tx in minTileX..maxTileX) {
                    val tileLeftWorld = tx * GeoUtils.TILE_SIZE
                    val tileTopWorld = ty * GeoUtils.TILE_SIZE

                    val screenX = (canvasW / 2f) + ((tileLeftWorld - centerPxIntZoom) * effectiveScale).toFloat()
                    val screenY = (canvasH / 2f) + ((tileTopWorld - centerPyIntZoom) * effectiveScale).toFloat()

                    val dstOffset = IntOffset(screenX.roundToInt(), screenY.roundToInt())
                    val dstSize = IntSize(ceil(scaledTileSize).toInt() + 1, ceil(scaledTileSize).toInt() + 1)

                    val tileBitmap = tileEngine.getTileOrFetch(selectedLayer, intZoom, tx, ty)
                    if (tileBitmap != null) {
                        drawImage(
                            image = tileBitmap,
                            dstOffset = dstOffset,
                            dstSize = dstSize
                        )
                    } else {
                        val fallback = tileEngine.getParentFallbackTile(selectedLayer, intZoom, tx, ty)
                        if (fallback != null) {
                            val (parentBmp, step) = fallback
                            val subDiv = 1 shl step
                            val wrappedX = ((tx % maxTileIndex) + maxTileIndex) % maxTileIndex
                            val subX = wrappedX % subDiv
                            val subY = ty % subDiv
                            val srcChunkW = parentBmp.width / subDiv
                            val srcChunkH = parentBmp.height / subDiv
                            drawImage(
                                image = parentBmp,
                                srcOffset = IntOffset(subX * srcChunkW, subY * srcChunkH),
                                srcSize = IntSize(srcChunkW, srcChunkH),
                                dstOffset = dstOffset,
                                dstSize = dstSize
                            )
                        } else {
                            drawRect(
                                color = Color(0xFF0F1D36),
                                topLeft = Offset(screenX, screenY),
                                size = androidx.compose.ui.geometry.Size(scaledTileSize, scaledTileSize)
                            )
                            drawRect(
                                color = Color(0xFF1E325C).copy(alpha = 0.35f),
                                topLeft = Offset(screenX, screenY),
                                size = androidx.compose.ui.geometry.Size(scaledTileSize, scaledTileSize),
                                style = Stroke(width = 1f)
                            )
                        }
                    }
                }
            }

            // 1B. Draw Real-Time Rain Radar Rings on Map
            if (showRainRadarOverlay && realtimeTelemetry != null) {
                realtimeTelemetry.rainRadarZones.forEach { zone ->
                    val (rx, ry) = GeoUtils.latLngToScreen(
                        lat = zone.latitude,
                        lng = zone.longitude,
                        viewportWidth = canvasW,
                        viewportHeight = canvasH,
                        centerLat = centerLat,
                        centerLng = centerLng,
                        zoom = zoom,
                        densityScale = density
                    )
                    if (rx in -240f..(canvasW + 240f) && ry in -240f..(canvasH + 240f)) {
                        val radarColor = Color(zone.severity.colorHex)
                        val radiusPx = (34f * density * (zoom / 13.0).toFloat().coerceIn(0.65f, 1.8f))
                        drawCircle(
                            color = Color(0xFF0EA5E9).copy(alpha = 0.16f),
                            radius = radiusPx,
                            center = Offset(rx, ry)
                        )
                        drawCircle(
                            color = radarColor.copy(alpha = 0.24f),
                            radius = radiusPx * 0.62f,
                            center = Offset(rx, ry)
                        )
                        drawCircle(
                            color = radarColor.copy(alpha = 0.85f),
                            radius = radiusPx,
                            center = Offset(rx, ry),
                            style = Stroke(
                                width = 1.6f * density,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                        )
                    }
                }
            }

            // 1C. Draw Real-Time Fire Hotspot Thermal Zones on Map
            if (showFireHotspotOverlay && realtimeTelemetry != null) {
                realtimeTelemetry.fireHotspots.forEach { hs ->
                    val (fx, fy) = GeoUtils.latLngToScreen(
                        lat = hs.latitude,
                        lng = hs.longitude,
                        viewportWidth = canvasW,
                        viewportHeight = canvasH,
                        centerLat = centerLat,
                        centerLng = centerLng,
                        zoom = zoom,
                        densityScale = density
                    )
                    if (fx in -200f..(canvasW + 200f) && fy in -200f..(canvasH + 200f)) {
                        val fireColor = Color(hs.severity.colorHex)
                        val radiusPx = (24f * density * (zoom / 13.0).toFloat().coerceIn(0.7f, 1.6f))
                        drawCircle(
                            color = Color(0xFFEF4444).copy(alpha = 0.22f),
                            radius = radiusPx,
                            center = Offset(fx, fy)
                        )
                        drawCircle(
                            color = fireColor,
                            radius = radiusPx,
                            center = Offset(fx, fy),
                            style = Stroke(width = 1.8f * density)
                        )
                        drawCircle(
                            color = Color(0xFFF97316),
                            radius = 5f * density,
                            center = Offset(fx, fy)
                        )
                    }
                }
            }

            // 2. Draw saved Tactical Strokes & Distance Measurement lines
            strokes.forEach { strokeEntity ->
                val pts = strokeEntity.toGeoPoints()
                if (pts.size >= 2) {
                    val path = Path()
                    pts.forEachIndexed { idx, gp ->
                        val (sx, sy) = GeoUtils.latLngToScreen(
                            lat = gp.latitude,
                            lng = gp.longitude,
                            viewportWidth = canvasW,
                            viewportHeight = canvasH,
                            centerLat = centerLat,
                            centerLng = centerLng,
                            zoom = zoom,
                            densityScale = density
                        )
                        if (idx == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
                    }

                    val strokeColor = Color(strokeEntity.colorHex)
                    val widthOnScreen = (strokeEntity.strokeWidthPx * density).coerceAtLeast(2f)

                    drawPath(
                        path = path,
                        color = Color.Black.copy(alpha = 0.5f),
                        style = Stroke(
                            width = widthOnScreen + 3f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(
                            width = widthOnScreen,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = if (strokeEntity.isMeasurement) {
                                PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
                            } else null
                        )
                    )

                    if (strokeEntity.isMeasurement) {
                        pts.forEach { gp ->
                            val (vx, vy) = GeoUtils.latLngToScreen(
                                gp.latitude, gp.longitude, canvasW, canvasH,
                                centerLat, centerLng, zoom, density
                            )
                            drawCircle(color = Color.Black, radius = 5f * density, center = Offset(vx, vy))
                            drawCircle(color = strokeColor, radius = 3.5f * density, center = Offset(vx, vy))
                        }
                    }
                }
            }

            // 3. Draw active Freehand Stroke
            if (activeFreehandPoints.size >= 2) {
                val activePath = Path()
                activeFreehandPoints.forEachIndexed { idx, gp ->
                    val (sx, sy) = GeoUtils.latLngToScreen(
                        gp.latitude, gp.longitude, canvasW, canvasH,
                        centerLat, centerLng, zoom, density
                    )
                    if (idx == 0) activePath.moveTo(sx, sy) else activePath.lineTo(sx, sy)
                }
                val activeWidth = (brushSizePx * density).coerceAtLeast(2f)
                drawPath(
                    path = activePath,
                    color = Color.Black.copy(alpha = 0.55f),
                    style = Stroke(width = activeWidth + 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = activePath,
                    color = Color(brushColorHex),
                    style = Stroke(width = activeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 4. Draw active Distance Measurement path
            if (activeMeasurePoints.isNotEmpty()) {
                val measurePath = Path()
                activeMeasurePoints.forEachIndexed { idx, gp ->
                    val (sx, sy) = GeoUtils.latLngToScreen(
                        gp.latitude, gp.longitude, canvasW, canvasH,
                        centerLat, centerLng, zoom, density
                    )
                    if (idx == 0) measurePath.moveTo(sx, sy) else measurePath.lineTo(sx, sy)
                }
                if (activeMeasurePoints.size >= 2) {
                    drawPath(
                        path = measurePath,
                        color = Color.Black.copy(alpha = 0.65f),
                        style = Stroke(width = (brushSizePx + 3) * density, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = measurePath,
                        color = Color(0xFFF59E0B),
                        style = Stroke(
                            width = brushSizePx * density,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
                        )
                    )
                }
                activeMeasurePoints.forEach { gp ->
                    val (sx, sy) = GeoUtils.latLngToScreen(
                        gp.latitude, gp.longitude, canvasW, canvasH,
                        centerLat, centerLng, zoom, density
                    )
                    drawCircle(color = Color.White, radius = 6f * density, center = Offset(sx, sy))
                    drawCircle(color = Color(0xFFF59E0B), radius = 4f * density, center = Offset(sx, sy))
                }
            }

            // 5. Draw User GPS Location if available
            userGpsLocation?.let { gps ->
                val (gx, gy) = GeoUtils.latLngToScreen(
                    gps.latitude, gps.longitude, canvasW, canvasH,
                    centerLat, centerLng, zoom, density
                )
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.25f),
                    radius = 18f * density,
                    center = Offset(gx, gy)
                )
                drawCircle(color = Color.White, radius = 7f * density, center = Offset(gx, gy))
                drawCircle(color = Color(0xFF0284C7), radius = 5f * density, center = Offset(gx, gy))
            }

            // 6. Draw Marked Coordinate Target Crosshair on Map
            markedCoordinate?.let { mark ->
                val (mx, my) = GeoUtils.latLngToScreen(
                    mark.latitude, mark.longitude, canvasW, canvasH,
                    centerLat, centerLng, zoom, density
                )
                val ringRadius = 15f * density
                drawCircle(
                    color = TacticalCyan.copy(alpha = 0.2f),
                    radius = ringRadius * 1.4f,
                    center = Offset(mx, my)
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.7f),
                    radius = ringRadius,
                    center = Offset(mx, my),
                    style = Stroke(width = 3f * density)
                )
                drawCircle(
                    color = TacticalCyan,
                    radius = ringRadius,
                    center = Offset(mx, my),
                    style = Stroke(width = 1.8f * density)
                )
                val crossLen = 21f * density
                drawLine(
                    color = TacticalCyan,
                    start = Offset(mx - crossLen, my),
                    end = Offset(mx + crossLen, my),
                    strokeWidth = 1.8f * density
                )
                drawLine(
                    color = TacticalCyan,
                    start = Offset(mx, my - crossLen),
                    end = Offset(mx, my + crossLen),
                    strokeWidth = 1.8f * density
                )
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = 4f * density,
                    center = Offset(mx, my)
                )
            }
        }

        // Overlay 1: Measurement Distance Labels on Map
        strokes.filter { it.isMeasurement && it.label.isNotEmpty() }.forEach { mStroke ->
            val pts = mStroke.toGeoPoints()
            val lastPt = pts.lastOrNull()
            if (lastPt != null) {
                val (sx, sy) = GeoUtils.latLngToScreen(
                    lastPt.latitude, lastPt.longitude,
                    viewportWidth, viewportHeight,
                    centerLat, centerLng, zoom, density
                )
                if (sx in -80f..(viewportWidth + 80f) && sy in -80f..(viewportHeight + 80f)) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset((sx - 40 * density).roundToInt(), (sy - 28 * density).roundToInt()) }
                            .background(Color(0xDD0F172A), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = mStroke.label,
                            color = Color(0xFFFDE68A),
                            fontSize = 10.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Overlay 2: Compact Thai Text Notes (Noto Sans Thai) on Map
        mapNotes.forEach { note ->
            val (sx, sy) = GeoUtils.latLngToScreen(
                note.latitude, note.longitude,
                viewportWidth, viewportHeight,
                centerLat, centerLng, zoom, density
            )
            if (sx in -160f..(viewportWidth + 160f) && sy in -100f..(viewportHeight + 100f)) {
                val noteColor = Color(note.colorHex)
                Surface(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (sx - 16 * density).roundToInt(),
                                (sy - 16 * density).roundToInt()
                            )
                        }
                        .shadow(3.dp, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xE60B1325),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, noteColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(noteColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = note.text,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = note.fontSizeSp.sp,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "ลบข้อความ",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onDeleteNote(note.id) }
                        )
                    }
                }
            }
        }

        // Overlay 3: Compact Emergency Pins (8 Categories, without criticality level)
        emergencyPins.forEach { pin ->
            val (sx, sy) = GeoUtils.latLngToScreen(
                pin.latitude, pin.longitude,
                viewportWidth, viewportHeight,
                centerLat, centerLng, zoom, density
            )
            if (sx in -120f..(viewportWidth + 120f) && sy in -120f..(viewportHeight + 120f)) {
                val category = EmergencyCategory.fromCode(pin.categoryCode)
                val pinColor = Color(category.defaultColorHex)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (sx - 56 * density).roundToInt(),
                                (sy - 42 * density).roundToInt()
                            )
                        }
                        .width(112.dp)
                        .clickable { onSelectPin(pin) }
                        .testTag("emergency_pin_${pin.id}")
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xEE0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, pinColor),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = category.badgeSymbol, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = pin.title,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = NotoSansThaiFontFamily,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(pinColor)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }
        }

        // Overlay 4: Real-Time Rain Radar Zone Labels
        if (showRainRadarOverlay && realtimeTelemetry != null) {
            realtimeTelemetry.rainRadarZones.forEach { zone ->
                val (sx, sy) = GeoUtils.latLngToScreen(
                    zone.latitude, zone.longitude,
                    viewportWidth, viewportHeight,
                    centerLat, centerLng, zoom, density
                )
                if (sx in -100f..(viewportWidth + 100f) && sy in -100f..(viewportHeight + 100f)) {
                    val zColor = Color(zone.severity.colorHex)
                    Surface(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (sx - 44 * density).roundToInt(),
                                    (sy - 14 * density).roundToInt()
                                )
                            }
                            .clickable { onOpenTelemetryTab?.invoke(RealtimeTelemetryTab.RAIN) }
                            .testTag("map_rain_zone_${zone.id}"),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xE0081A2E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, zColor)
                    ) {
                        Text(
                            text = "🌧️ ${RealtimeTelemetryService.format1Dec(zone.rainMmPerHour)} มม. (${zone.probabilityPercent}%)",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Overlay 5: Real-Time Fire Hotspot Markers
        if (showFireHotspotOverlay && realtimeTelemetry != null) {
            realtimeTelemetry.fireHotspots.forEach { hs ->
                val (sx, sy) = GeoUtils.latLngToScreen(
                    hs.latitude, hs.longitude,
                    viewportWidth, viewportHeight,
                    centerLat, centerLng, zoom, density
                )
                if (sx in -100f..(viewportWidth + 100f) && sy in -100f..(viewportHeight + 100f)) {
                    val fColor = Color(hs.severity.colorHex)
                    Surface(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (sx - 42 * density).roundToInt(),
                                    (sy - 26 * density).roundToInt()
                                )
                            }
                            .clickable { onOpenTelemetryTab?.invoke(RealtimeTelemetryTab.FIRE) }
                            .testTag("map_fire_hotspot_${hs.id}"),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xE6261008),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, fColor)
                    ) {
                        Text(
                            text = "🔥 FWI ${RealtimeTelemetryService.format1Dec(hs.fireWeatherIndex)}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Overlay 6: Real-Time Water Level Station Gauges
        if (showWaterStationOverlay && realtimeTelemetry != null) {
            realtimeTelemetry.waterStations.forEach { st ->
                val (sx, sy) = GeoUtils.latLngToScreen(
                    st.latitude, st.longitude,
                    viewportWidth, viewportHeight,
                    centerLat, centerLng, zoom, density
                )
                if (sx in -110f..(viewportWidth + 110f) && sy in -110f..(viewportHeight + 110f)) {
                    val wColor = Color(st.severity.colorHex)
                    Surface(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (sx - 48 * density).roundToInt(),
                                    (sy - 18 * density).roundToInt()
                                )
                            }
                            .clickable { onOpenTelemetryTab?.invoke(RealtimeTelemetryTab.WATER) }
                            .testTag("map_water_station_${st.stationCode}"),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xE6071F32),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, wColor)
                    ) {
                        Text(
                            text = "🌊 ${st.stationCode} • ${RealtimeTelemetryService.format1Dec(st.capacityPercent)}%",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MapScaleBarOverlay(
    latitude: Double,
    zoom: Double,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val spec = remember(latitude, zoom, density) {
        GeoUtils.computeScaleBar(
            lat = latitude,
            zoom = zoom,
            densityScale = density,
            maxBarWidthPx = 110f * density
        )
    }
    val barWidthDp = with(LocalDensity.current) { spec.widthPx.toDp() }

    Surface(
        modifier = modifier.testTag("scale_bar_overlay"),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xD90A1120),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(barWidthDp.coerceAtLeast(52.dp))
            ) {
                Text(
                    text = "0",
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    fontFamily = NotoSansThaiFontFamily
                )
                Text(
                    text = spec.label,
                    color = TacticalCyan,
                    fontSize = 10.sp,
                    fontFamily = NotoSansThaiFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(barWidthDp.coerceAtLeast(52.dp))
                    .height(5.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawLine(
                        color = TacticalCyan,
                        start = Offset(0f, 0f),
                        end = Offset(0f, h),
                        strokeWidth = 2f * density
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, h),
                        end = Offset(w / 2f, h),
                        strokeWidth = 2f * density
                    )
                    drawLine(
                        color = TacticalCyan,
                        start = Offset(w / 2f, h),
                        end = Offset(w, h),
                        strokeWidth = 2f * density
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(w / 2f, h * 0.35f),
                        end = Offset(w / 2f, h),
                        strokeWidth = 1.5f * density
                    )
                    drawLine(
                        color = TacticalCyan,
                        start = Offset(w, 0f),
                        end = Offset(w, h),
                        strokeWidth = 2f * density
                    )
                }
            }
        }
    }
}
