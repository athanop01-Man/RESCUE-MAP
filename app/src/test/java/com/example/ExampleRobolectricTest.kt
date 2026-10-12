package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.EmergencyCategory
import com.example.data.EmergencyPinEntity
import com.example.data.GeoPoint
import com.example.data.TacticalStrokeEntity
import com.example.map.GeoUtils
import com.example.map.GoogleMapLayer
import com.example.ui.FieldToolMode
import com.example.ui.TacticalMapViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name and developer credit from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        val devCredit = context.getString(R.string.developer_credit)
        assertEquals("E23WCM Tactical Map", appName)
        assertEquals("E23WCM ช่างแมน", devCredit)
    }

    @Test
    fun `verify 5 Google Maps layers and 8 emergency categories without criticality`() {
        assertEquals(5, GoogleMapLayer.entries.size)
        assertEquals(8, EmergencyCategory.entries.size)

        val roadmapUrl = GoogleMapLayer.ROADMAP.buildTileUrl(x = 100, y = 200, z = 12)
        val hybridUrl = GoogleMapLayer.HYBRID.buildTileUrl(x = 100, y = 200, z = 12)
        val satelliteUrl = GoogleMapLayer.SATELLITE.buildTileUrl(x = 100, y = 200, z = 12)
        val terrainUrl = GoogleMapLayer.TERRAIN.buildTileUrl(x = 100, y = 200, z = 12)
        val trafficUrl = GoogleMapLayer.TRAFFIC.buildTileUrl(x = 100, y = 200, z = 12)

        assertTrue(roadmapUrl.contains("lyrs=m&"))
        assertTrue(hybridUrl.contains("lyrs=y&"))
        assertTrue(satelliteUrl.contains("lyrs=s&"))
        assertTrue(terrainUrl.contains("lyrs=p&"))
        assertTrue(trafficUrl.contains("lyrs=m,traffic&"))

        // Verify EmergencyPinEntity stores reporter name and phone and has no criticality field
        val pin = EmergencyPinEntity(
            latitude = 13.756331,
            longitude = 100.501762,
            categoryCode = EmergencyCategory.MEDICAL.code,
            title = "ผู้ป่วยฉุกเฉิน",
            details = "ต้องการรถพยาบาล",
            callsign = "กู้ภัย 01",
            reporterPhone = "081-234-5678"
        )
        assertEquals("กู้ภัย 01", pin.callsign)
        assertEquals("081-234-5678", pin.reporterPhone)
        val fieldNames = EmergencyPinEntity::class.java.declaredFields.map { it.name }
        assertFalse(fieldNames.any { it.contains("critical", ignoreCase = true) })
    }

    @Test
    fun `verify ViewModel reporter profile, brush sizes, scale bar, and coordinate marking`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TacticalMapViewModel(app)

        // Update reporter profile (name + phone)
        viewModel.updateReporterProfile("กู้ภัย E23WCM 09", "089-999-8888", showConfirmation = false)
        assertEquals("กู้ภัย E23WCM 09", viewModel.reporterName.value)
        assertEquals("089-999-8888", viewModel.reporterPhone.value)

        // Verify 4 brush sizes (2px, 4px, 6px, 8px)
        listOf(2, 4, 6, 8).forEach { px ->
            viewModel.setBrushSizePx(px)
            assertEquals(px, viewModel.brushSizePx.value)
        }

        // Verify scale bar toggle
        viewModel.setShowScaleBar(false)
        assertFalse(viewModel.showScaleBar.value)
        viewModel.setShowScaleBar(true)
        assertTrue(viewModel.showScaleBar.value)

        // Verify map tap coordinate marking in NAVIGATE mode
        viewModel.setToolMode(FieldToolMode.NAVIGATE)
        val tapped = GeoPoint(13.800000, 100.550000)
        viewModel.onMapTapCoordinate(tapped)
        assertNotNull(viewModel.markedCoordinate.value)
        assertEquals(13.800000, viewModel.markedCoordinate.value!!.latitude, 0.0001)

        viewModel.clearMarkedCoordinate()
        assertNull(viewModel.markedCoordinate.value)
    }

    @Test
    fun `verify Web Mercator roundtrip, stroke caching, and scale bar calculation`() {
        val lat = 13.756331
        val lng = 100.501762
        val zoom = 14.0
        val (px, py) = GeoUtils.latLngToWorldPixel(lat, lng, zoom)
        val roundtrip = GeoUtils.worldPixelToLatLng(px, py, zoom)
        assertEquals(lat, roundtrip.latitude, 0.0001)
        assertEquals(lng, roundtrip.longitude, 0.0001)

        val scaleSpec = GeoUtils.computeScaleBar(lat = lat, zoom = zoom, densityScale = 2.5f)
        assertTrue(scaleSpec.distanceMeters > 0)
        assertTrue(scaleSpec.widthPx >= 60f)

        val p1 = GeoPoint(13.756331, 100.501762)
        val p2 = GeoPoint(13.746630, 100.534740)
        val dist = GeoUtils.haversineDistanceMeters(p1, p2)
        assertTrue(dist > 3000.0)

        val encoded = TacticalStrokeEntity.encodePoints(listOf(p1, p2))
        val stroke = TacticalStrokeEntity(
            isMeasurement = true,
            strokeWidthPx = 4,
            colorHex = 0xFFF59E0B,
            serializedPoints = encoded,
            totalDistanceMeters = dist,
            label = GeoUtils.formatDistanceThai(dist)
        )
        assertEquals(2, stroke.toGeoPoints().size)
    }

    @Test
    fun `verify realtime weather, rain, fire, and water level telemetry and overlays`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TacticalMapViewModel(app)

        assertEquals(4, com.example.data.RealtimeTelemetryTab.entries.size)
        assertTrue(viewModel.showRainRadarOverlay.value)
        assertTrue(viewModel.showFireHotspotOverlay.value)
        assertTrue(viewModel.showWaterStationOverlay.value)

        viewModel.toggleRainRadarOverlay()
        assertFalse(viewModel.showRainRadarOverlay.value)
        viewModel.toggleRainRadarOverlay()
        assertTrue(viewModel.showRainRadarOverlay.value)

        viewModel.setSelectedTelemetryTab(com.example.data.RealtimeTelemetryTab.WATER)
        assertEquals(com.example.data.RealtimeTelemetryTab.WATER, viewModel.selectedTelemetryTab.value)

        val service = com.example.data.RealtimeTelemetryService()
        val snapshot = service.fetchRealtimeTelemetry(13.756331, 100.501762)
        assertTrue(snapshot.temperatureC in -20.0..60.0)
        assertTrue(snapshot.humidityPercent in 0..100)
        assertTrue(snapshot.hourlyForecasts.isNotEmpty())
        assertTrue(snapshot.rainRadarZones.isNotEmpty())
        assertTrue(snapshot.fireHotspots.isNotEmpty())
        assertTrue(snapshot.waterStations.isNotEmpty())
    }
}

