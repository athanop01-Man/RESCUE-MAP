package com.example.map

import com.example.data.GeoPoint
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class ScaleBarSpec(
    val distanceMeters: Double,
    val label: String,
    val widthPx: Float
)

object GeoUtils {
    const val TILE_SIZE = 256.0
    private const val EARTH_RADIUS_METERS = 6378137.0

    fun clampLat(lat: Double): Double = lat.coerceIn(-85.05112878, 85.05112878)

    fun normalizeLng(lng: Double): Double {
        var result = (lng + 180.0) % 360.0
        if (result < 0) result += 360.0
        return result - 180.0
    }

    fun worldSizePixels(zoom: Double): Double {
        return TILE_SIZE * 2.0.pow(zoom)
    }

    fun latLngToWorldPixel(lat: Double, lng: Double, zoom: Double): Pair<Double, Double> {
        val clampedLat = clampLat(lat)
        val normLng = normalizeLng(lng)
        val worldSize = worldSizePixels(zoom)
        val x = (normLng + 180.0) / 360.0 * worldSize
        val sinLat = sin(clampedLat * PI / 180.0)
        val y = (0.5 - ln((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * PI)) * worldSize
        return Pair(x, y)
    }

    fun worldPixelToLatLng(px: Double, py: Double, zoom: Double): GeoPoint {
        val worldSize = worldSizePixels(zoom)
        val lng = normalizeLng((px / worldSize) * 360.0 - 180.0)
        val n = PI - (2.0 * PI * py) / worldSize
        val lat = (180.0 / PI) * atan(0.5 * (exp(n) - exp(-n)))
        return GeoPoint(clampLat(lat), lng)
    }

    fun screenToLatLng(
        screenX: Float,
        screenY: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        centerLat: Double,
        centerLng: Double,
        zoom: Double,
        densityScale: Float
    ): GeoPoint {
        val (centerPx, centerPy) = latLngToWorldPixel(centerLat, centerLng, zoom)
        val dxWorld = (screenX - viewportWidth / 2f) / densityScale
        val dyWorld = (screenY - viewportHeight / 2f) / densityScale
        return worldPixelToLatLng(centerPx + dxWorld, centerPy + dyWorld, zoom)
    }

    fun latLngToScreen(
        lat: Double,
        lng: Double,
        viewportWidth: Float,
        viewportHeight: Float,
        centerLat: Double,
        centerLng: Double,
        zoom: Double,
        densityScale: Float
    ): Pair<Float, Float> {
        val (centerPx, centerPy) = latLngToWorldPixel(centerLat, centerLng, zoom)
        val (targetPx, targetPy) = latLngToWorldPixel(lat, lng, zoom)
        val sx = (viewportWidth / 2f) + ((targetPx - centerPx) * densityScale).toFloat()
        val sy = (viewportHeight / 2f) + ((targetPy - centerPy) * densityScale).toFloat()
        return Pair(sx, sy)
    }

    fun metersPerWorldPixel(lat: Double, zoom: Double): Double {
        val clampedLat = clampLat(lat)
        return (cos(clampedLat * PI / 180.0) * 2.0 * PI * EARTH_RADIUS_METERS) / worldSizePixels(zoom)
    }

    fun computeScaleBar(
        lat: Double,
        zoom: Double,
        densityScale: Float,
        maxBarWidthPx: Float = 320f
    ): ScaleBarSpec {
        val metersPerScreenPx = metersPerWorldPixel(lat, zoom) / densityScale.coerceAtLeast(0.5f)
        val maxMeters = metersPerScreenPx * maxBarWidthPx

        val niceSteps = listOf(
            5.0, 10.0, 20.0, 50.0, 100.0, 200.0, 500.0,
            1000.0, 2000.0, 5000.0, 10000.0, 20000.0, 50000.0, 100000.0, 200000.0, 500000.0
        )
        val chosenMeters = niceSteps.lastOrNull { it <= maxMeters } ?: niceSteps.first()
        val barWidthPx = (chosenMeters / metersPerScreenPx).toFloat().coerceIn(60f, maxBarWidthPx)
        val label = if (chosenMeters >= 1000.0) {
            String.format(Locale.US, "%.0f กม.", chosenMeters / 1000.0)
        } else {
            String.format(Locale.US, "%.0f ม.", chosenMeters)
        }
        return ScaleBarSpec(
            distanceMeters = chosenMeters,
            label = label,
            widthPx = barWidthPx
        )
    }

    fun haversineDistanceMeters(p1: GeoPoint, p2: GeoPoint): Double {
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        val lat1 = Math.toRadians(p1.latitude)
        val lat2 = Math.toRadians(p2.latitude)

        val a = sin(dLat / 2).pow(2.0) +
                sin(dLon / 2).pow(2.0) * cos(lat1) * cos(lat2)
        val c = 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
        return EARTH_RADIUS_METERS * c
    }

    fun calculatePathDistanceMeters(points: List<GeoPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += haversineDistanceMeters(points[i], points[i + 1])
        }
        return total
    }

    fun formatDistanceThai(meters: Double): String {
        return if (meters >= 1000.0) {
            String.format(Locale.US, "%.2f กม.", meters / 1000.0)
        } else {
            String.format(Locale.US, "%.1f ม.", meters)
        }
    }

    fun formatLatLngDecimal(lat: Double, lng: Double): String {
        return String.format(Locale.US, "%.6f, %.6f", lat, lng)
    }

    fun formatDMS(lat: Double, lng: Double): String {
        fun toDms(value: Double, posHem: Char, negHem: Char): String {
            val hem = if (value >= 0) posHem else negHem
            val absVal = abs(value)
            val deg = floor(absVal).toInt()
            val minFull = (absVal - deg) * 60.0
            val min = floor(minFull).toInt()
            val sec = (minFull - min) * 60.0
            return String.format(Locale.US, "%d°%02d'%04.1f\"%c", deg, min, sec, hem)
        }
        return "${toDms(lat, 'N', 'S')}  ${toDms(lng, 'E', 'W')}"
    }
}
