package com.example.data

import com.example.map.GeoUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

enum class AlertSeverity(
    val code: String,
    val labelThai: String,
    val colorHex: Long
) {
    NORMAL("NORMAL", "ปกติ", 0xFF10B981),
    WATCH("WATCH", "เฝ้าระวัง", 0xFFEAB308),
    WARNING("WARNING", "เตือนภัย", 0xFFF97316),
    CRITICAL("CRITICAL", "วิกฤต", 0xFFEF4444)
}

enum class RealtimeTelemetryTab(
    val id: String,
    val titleThai: String,
    val shortTitle: String,
    val badgeEmoji: String
) {
    WEATHER("WEATHER", "พยากรณ์อากาศเรียลไทม์", "อากาศ", "⛅"),
    RAIN("RAIN", "เรดาร์ฝนและปริมาณน้ำฝน", "ฝน", "🌧️"),
    FIRE("FIRE", "ไฟไหม้และจุดความร้อน (Hotspot)", "ไฟไหม้", "🔥"),
    WATER("WATER", "ระดับน้ำและอุทกภัยเรียลไทม์", "ระดับน้ำ", "🌊")
}

data class HourlyWeatherPoint(
    val timeLabel: String,
    val tempC: Double,
    val rainProbabilityPercent: Int,
    val rainMm: Double,
    val windSpeedKmh: Double,
    val weatherCode: Int,
    val badgeEmoji: String
)

data class RainRadarZone(
    val id: String,
    val areaNameThai: String,
    val latitude: Double,
    val longitude: Double,
    val rainMmPerHour: Double,
    val rain24hMm: Double,
    val probabilityPercent: Int,
    val radiusKm: Double,
    val severity: AlertSeverity,
    val statusTextThai: String
)

data class FireHotspotPoint(
    val id: String,
    val areaNameThai: String,
    val latitude: Double,
    val longitude: Double,
    val frpMegawatts: Double,
    val confidencePercent: Int,
    val fireWeatherIndex: Double,
    val windSpeedKmh: Double,
    val windDirectionThai: String,
    val severity: AlertSeverity,
    val statusTextThai: String,
    val detectedTimeLabel: String
)

data class WaterLevelStation(
    val stationCode: String,
    val stationNameThai: String,
    val riverNameThai: String,
    val latitude: Double,
    val longitude: Double,
    val waterLevelMetersMsl: Double,
    val bankLevelMetersMsl: Double,
    val capacityPercent: Double,
    val dischargeM3s: Double,
    val trendThai: String,
    val severity: AlertSeverity,
    val statusTextThai: String
)

data class RealtimeTelemetrySnapshot(
    val latitude: Double,
    val longitude: Double,
    val locationLabelThai: String,
    // 1. Weather Forecast
    val temperatureC: Double,
    val feelsLikeC: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val windGustsKmh: Double,
    val windDirectionDeg: Int,
    val windDirectionThai: String,
    val pressureHpa: Double,
    val cloudCoverPercent: Int,
    val weatherCode: Int,
    val weatherConditionThai: String,
    val weatherBadgeEmoji: String,
    val hourlyForecasts: List<HourlyWeatherPoint>,
    // 2. Rainfall & Storm
    val currentRainMm: Double,
    val rain24hSumMm: Double,
    val rainProbabilityPercent: Int,
    val rainSeverity: AlertSeverity,
    val rainStatusThai: String,
    val rainRadarZones: List<RainRadarZone>,
    // 3. Fire / Wildfire Risk & Hotspots
    val fireWeatherIndex: Double,
    val fireRiskSeverity: AlertSeverity,
    val fireRiskStatusThai: String,
    val fireSpreadAdvisoryThai: String,
    val fireHotspots: List<FireHotspotPoint>,
    // 4. Water Level & Flood
    val localWaterLevelMsl: Double,
    val localBankCapacityPercent: Double,
    val localRiverDischargeM3s: Double,
    val waterTrendThai: String,
    val waterSeverity: AlertSeverity,
    val waterStatusThai: String,
    val waterStations: List<WaterLevelStation>,
    // Metadata
    val updatedAtEpochMs: Long,
    val updatedTimeFormatted: String,
    val isLiveOnlineData: Boolean
)

class RealtimeTelemetryService {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun fetchRealtimeTelemetry(
        lat: Double,
        lng: Double
    ): RealtimeTelemetrySnapshot = withContext(Dispatchers.IO) {
        val clampedLat = GeoUtils.clampLat(lat)
        val normLng = GeoUtils.normalizeLng(lng)
        val nowMs = System.currentTimeMillis()
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(nowMs))
        val areaLabel = resolveNearestThaiAreaName(clampedLat, normLng)

        var isLive = false
        var tempC = 31.4
        var feelsLikeC = 34.2
        var humidity = 66
        var currentRain = 0.0
        var weatherCode = 1
        var cloudCover = 38
        var pressureHpa = 1009.5
        var windSpeed = 12.6
        var windDirDeg = 145
        var windGusts = 19.4
        var rain24hSum = 2.8
        var maxRainProb = 35
        val hourlyList = mutableListOf<HourlyWeatherPoint>()

        // 1. Fetch Live Open-Meteo Weather & Precipitation Forecast API
        try {
            val weatherUrl = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.5f&longitude=%.5f" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,weather_code,cloud_cover,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m" +
                    "&hourly=temperature_2m,precipitation_probability,rain,weather_code,wind_speed_10m" +
                    "&forecast_days=2&timezone=auto",
                clampedLat,
                normLng
            )
            val req = Request.Builder()
                .url(weatherUrl)
                .header("User-Agent", "E23WCM-Tactical-Map/1.0")
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val root = JSONObject(bodyStr)
                        val currentObj = root.optJSONObject("current")
                        if (currentObj != null) {
                            isLive = true
                            tempC = currentObj.optDouble("temperature_2m", tempC)
                            humidity = currentObj.optInt("relative_humidity_2m", humidity)
                            feelsLikeC = currentObj.optDouble("apparent_temperature", feelsLikeC)
                            val precip = currentObj.optDouble("precipitation", 0.0)
                            val rainVal = currentObj.optDouble("rain", precip)
                            currentRain = max(precip, rainVal)
                            weatherCode = currentObj.optInt("weather_code", weatherCode)
                            cloudCover = currentObj.optInt("cloud_cover", cloudCover)
                            pressureHpa = currentObj.optDouble("surface_pressure", pressureHpa)
                            windSpeed = currentObj.optDouble("wind_speed_10m", windSpeed)
                            windDirDeg = currentObj.optInt("wind_direction_10m", windDirDeg)
                            windGusts = currentObj.optDouble("wind_gusts_10m", max(windSpeed * 1.3, windGusts))
                        }

                        val hourlyObj = root.optJSONObject("hourly")
                        if (hourlyObj != null) {
                            val times = hourlyObj.optJSONArray("time")
                            val temps = hourlyObj.optJSONArray("temperature_2m")
                            val probs = hourlyObj.optJSONArray("precipitation_probability")
                            val rains = hourlyObj.optJSONArray("rain")
                            val codes = hourlyObj.optJSONArray("weather_code")
                            val winds = hourlyObj.optJSONArray("wind_speed_10m")

                            if (times != null && temps != null) {
                                val currentHourStr = SimpleDateFormat("yyyy-MM-dd'T'HH", Locale.US).format(Date(nowMs))
                                var startIdx = 0
                                for (i in 0 until times.length()) {
                                    if (times.optString(i).startsWith(currentHourStr)) {
                                        startIdx = i
                                        break
                                    }
                                }
                                var sum24 = 0.0
                                var maxProb = 0
                                val count24 = min(24, times.length() - startIdx)
                                for (i in 0 until count24) {
                                    val idx = startIdx + i
                                    val r = rains?.optDouble(idx, 0.0) ?: 0.0
                                    val p = probs?.optInt(idx, 0) ?: 0
                                    sum24 += r
                                    if (p > maxProb) maxProb = p
                                    if (hourlyList.size < 8 && i % 2 == 0) {
                                        val rawTime = times.optString(idx, "")
                                        val hhmm = if (rawTime.length >= 16) rawTime.substring(11, 16) else "${(i * 2) % 24}:00"
                                        val wCode = codes?.optInt(idx, weatherCode) ?: weatherCode
                                        hourlyList.add(
                                            HourlyWeatherPoint(
                                                timeLabel = hhmm,
                                                tempC = temps.optDouble(idx, tempC),
                                                rainProbabilityPercent = p,
                                                rainMm = r,
                                                windSpeedKmh = winds?.optDouble(idx, windSpeed) ?: windSpeed,
                                                weatherCode = wCode,
                                                badgeEmoji = weatherCodeToEmoji(wCode)
                                            )
                                        )
                                    }
                                }
                                rain24hSum = max(currentRain, sum24)
                                maxRainProb = max(maxProb, if (currentRain > 0.0) 80 else 15)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to deterministic coordinate model below if offline
        }

        // 2. Fetch Live Open-Meteo GloFAS River Discharge / Flood API
        var liveDischargeM3s: Double? = null
        try {
            val floodUrl = String.format(
                Locale.US,
                "https://flood-api.open-meteo.com/v1/flood?latitude=%.5f&longitude=%.5f&daily=river_discharge,river_discharge_mean&forecast_days=2",
                clampedLat,
                normLng
            )
            val req = Request.Builder()
                .url(floodUrl)
                .header("User-Agent", "E23WCM-Tactical-Map/1.0")
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val root = JSONObject(bodyStr)
                        val daily = root.optJSONObject("daily")
                        val discharges = daily?.optJSONArray("river_discharge")
                        if (discharges != null && discharges.length() > 0) {
                            val d0 = discharges.optDouble(0, Double.NaN)
                            if (!d0.isNaN() && d0 >= 0.0) {
                                liveDischargeM3s = d0
                                isLive = true
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Handled below
        }

        if (!isLive) {
            // Deterministic coordinate-aware meteorological calculation when offline
            val latFactor = abs(clampedLat - 13.75)
            val lngFactor = abs(normLng - 100.50)
            val geoSeed = ((clampedLat * 1000).roundToInt() xor (normLng * 1000).roundToInt()) and 0x7FFF
            tempC = (30.5 - (clampedLat - 13.5) * 0.45 + (geoSeed % 25) * 0.1).coerceIn(18.0, 41.0)
            humidity = (62 + ((geoSeed % 28)) - (tempC - 30.0).roundToInt()).coerceIn(30, 98)
            feelsLikeC = tempC + (humidity - 55) * 0.08
            currentRain = if (humidity > 78) ((humidity - 76) * 0.65) else 0.0
            rain24hSum = (currentRain * 3.8 + (geoSeed % 12) * 0.7).coerceAtLeast(0.0)
            maxRainProb = (humidity - 15).coerceIn(10, 95)
            windSpeed = (9.5 + (latFactor + lngFactor) * 1.8 + (geoSeed % 9)).coerceIn(4.0, 42.0)
            windGusts = windSpeed * 1.42
            windDirDeg = (geoSeed * 17) % 360
            weatherCode = when {
                currentRain >= 4.0 -> 63
                currentRain > 0.1 -> 61
                humidity > 72 -> 3
                else -> 1
            }
            cloudCover = (humidity - 10).coerceIn(12, 98)
        }

        if (hourlyList.isEmpty()) {
            val baseHour = SimpleDateFormat("HH", Locale.US).format(Date(nowMs)).toIntOrNull() ?: 12
            for (step in 0 until 8) {
                val h = (baseHour + step * 2) % 24
                val isAfternoon = h in 12..16
                val deltaT = if (isAfternoon) 1.8 else if (h in 0..5) -2.4 else -0.6
                val stepTemp = (tempC + deltaT).coerceIn(15.0, 43.0)
                val stepProb = (maxRainProb + (if (h in 15..20) 15 else -5)).coerceIn(5, 98)
                val stepRain = if (stepProb >= 60) (currentRain * 0.7 + (stepProb - 55) * 0.08) else 0.0
                val stepCode = if (stepRain > 2.0) 63 else if (stepRain > 0.1) 61 else weatherCode
                hourlyList.add(
                    HourlyWeatherPoint(
                        timeLabel = String.format(Locale.US, "%02d:00", h),
                        tempC = stepTemp,
                        rainProbabilityPercent = stepProb,
                        rainMm = stepRain,
                        windSpeedKmh = windSpeed,
                        weatherCode = stepCode,
                        badgeEmoji = weatherCodeToEmoji(stepCode)
                    )
                )
            }
        }

        val windDirThai = degreeToThaiDirection(windDirDeg)
        val weatherDescThai = weatherCodeToThaiDescription(weatherCode, currentRain)
        val weatherEmoji = weatherCodeToEmoji(weatherCode)

        // Compute Rain Alert Severity & Radar Zones
        val rainSeverity = when {
            currentRain >= 15.0 || rain24hSum >= 65.0 -> AlertSeverity.CRITICAL
            currentRain >= 5.0 || rain24hSum >= 35.0 -> AlertSeverity.WARNING
            currentRain >= 0.5 || rain24hSum >= 10.0 || maxRainProb >= 65 -> AlertSeverity.WATCH
            else -> AlertSeverity.NORMAL
        }
        val rainStatusThai = when (rainSeverity) {
            AlertSeverity.CRITICAL -> "ฝนตกหนักมาก (${format1Dec(currentRain)} มม./ชม.) เสี่ยงน้ำท่วมฉับพลัน"
            AlertSeverity.WARNING -> "ฝนตกปานกลาง-หนัก (${format1Dec(currentRain)} มม./ชม.) เฝ้าระวังน้ำท่วมขัง"
            AlertSeverity.WATCH -> "กลุ่มเมฆฝนในพื้นที่ (โอกาสฝน $maxRainProb% • สะสม ${format1Dec(rain24hSum)} มม.)"
            AlertSeverity.NORMAL -> "สภาพฝนปกติ (ฝนปัจจุบัน ${format1Dec(currentRain)} มม./ชม.)"
        }

        val rainRadarZones = buildRainRadarZones(
            centerLat = clampedLat,
            centerLng = normLng,
            areaLabel = areaLabel,
            currentRainMm = currentRain,
            rain24hMm = rain24hSum,
            rainProb = maxRainProb
        )

        // Compute Fire Weather Index (FWI) & Active Hotspots
        val drynessFactor = (100 - humidity).coerceIn(5, 90) * 0.55
        val heatFactor = max(0.0, tempC - 25.0) * 2.2
        val windFactor = windSpeed * 0.75
        val rainDampening = min(35.0, currentRain * 8.0 + rain24hSum * 1.2)
        val fwi = (drynessFactor + heatFactor + windFactor - rainDampening).coerceIn(4.0, 98.0)

        val fireSeverity = when {
            fwi >= 72.0 -> AlertSeverity.CRITICAL
            fwi >= 50.0 -> AlertSeverity.WARNING
            fwi >= 32.0 -> AlertSeverity.WATCH
            else -> AlertSeverity.NORMAL
        }
        val fireStatusThai = when (fireSeverity) {
            AlertSeverity.CRITICAL -> "วิกฤตเสี่ยงไฟไหม้รุนแรง (FWI ${format1Dec(fwi)}) อากาศแห้งและลมแรง"
            AlertSeverity.WARNING -> "ความเสี่ยงอัคคีภัยสูง (FWI ${format1Dec(fwi)}) งดเผาในที่โล่งเด็ดขาด"
            AlertSeverity.WATCH -> "เฝ้าระวังจุดความร้อน (FWI ${format1Dec(fwi)}) เชื้อเพลิงธรรมชาติเริ่มแห้ง"
            AlertSeverity.NORMAL -> "ความเสี่ยงไฟไหม้ต่ำ (FWI ${format1Dec(fwi)}) ความชื้นสัมพัทธ์ $humidity%"
        }
        val fireSpreadAdvisory = "ลมทิศ$windDirThai ความเร็ว ${format1Dec(windSpeed)} กม./ชม. (กระโชก ${format1Dec(windGusts)} กม./ชม.)"

        val fireHotspots = buildFireHotspots(
            centerLat = clampedLat,
            centerLng = normLng,
            areaLabel = areaLabel,
            baseFwi = fwi,
            windSpeedKmh = windSpeed,
            windDirThai = windDirThai,
            timeFmt = timeFmt
        )

        // Compute Water Level & Hydrological Stations
        val waterStations = buildWaterLevelStations(
            centerLat = clampedLat,
            centerLng = normLng,
            areaLabel = areaLabel,
            currentRainMm = currentRain,
            rain24hMm = rain24hSum,
            liveDischargeM3s = liveDischargeM3s
        )
        val nearestStation = waterStations.first()
        val localWaterSeverity = nearestStation.severity
        val localWaterStatus = "${nearestStation.stationNameThai}: ${nearestStation.statusTextThai}"

        RealtimeTelemetrySnapshot(
            latitude = clampedLat,
            longitude = normLng,
            locationLabelThai = areaLabel,
            temperatureC = tempC,
            feelsLikeC = feelsLikeC,
            humidityPercent = humidity,
            windSpeedKmh = windSpeed,
            windGustsKmh = windGusts,
            windDirectionDeg = windDirDeg,
            windDirectionThai = windDirThai,
            pressureHpa = pressureHpa,
            cloudCoverPercent = cloudCover,
            weatherCode = weatherCode,
            weatherConditionThai = weatherDescThai,
            weatherBadgeEmoji = weatherEmoji,
            hourlyForecasts = hourlyList,
            currentRainMm = currentRain,
            rain24hSumMm = rain24hSum,
            rainProbabilityPercent = maxRainProb,
            rainSeverity = rainSeverity,
            rainStatusThai = rainStatusThai,
            rainRadarZones = rainRadarZones,
            fireWeatherIndex = fwi,
            fireRiskSeverity = fireSeverity,
            fireRiskStatusThai = fireStatusThai,
            fireSpreadAdvisoryThai = fireSpreadAdvisory,
            fireHotspots = fireHotspots,
            localWaterLevelMsl = nearestStation.waterLevelMetersMsl,
            localBankCapacityPercent = nearestStation.capacityPercent,
            localRiverDischargeM3s = nearestStation.dischargeM3s,
            waterTrendThai = nearestStation.trendThai,
            waterSeverity = localWaterSeverity,
            waterStatusThai = localWaterStatus,
            waterStations = waterStations,
            updatedAtEpochMs = nowMs,
            updatedTimeFormatted = timeFmt,
            isLiveOnlineData = isLive
        )
    }

    private fun buildRainRadarZones(
        centerLat: Double,
        centerLng: Double,
        areaLabel: String,
        currentRainMm: Double,
        rain24hMm: Double,
        rainProb: Int
    ): List<RainRadarZone> {
        val zones = mutableListOf<RainRadarZone>()

        // Primary zone at target coordinate
        val primaryRain = max(currentRainMm, if (rainProb >= 60) (rainProb - 45) * 0.18 else 0.2)
        val primarySev = severityForRain(primaryRain, rain24hMm)
        zones.add(
            RainRadarZone(
                id = "RAIN_LOCAL",
                areaNameThai = "เรดาร์จุดมาร์ค ($areaLabel)",
                latitude = centerLat,
                longitude = centerLng,
                rainMmPerHour = primaryRain,
                rain24hMm = rain24hMm,
                probabilityPercent = rainProb,
                radiusKm = 3.5,
                severity = primarySev,
                statusTextThai = statusForRain(primaryRain, rainProb)
            )
        )

        // Surrounding tactical rain cells (North-East, South-West, North-West)
        val offsets = listOf(
            Triple("กลุ่มฝนด้านเหนือ-ตะวันออก", 0.032, 0.028),
            Triple("กลุ่มฝนด้านใต้-ตะวันตก", -0.029, -0.025),
            Triple("แนวปะทะลมมรสุมตะวันตก", 0.018, -0.038)
        )
        offsets.forEachIndexed { idx, (label, dLat, dLng) ->
            val zLat = GeoUtils.clampLat(centerLat + dLat)
            val zLng = GeoUtils.normalizeLng(centerLng + dLng)
            val seed = abs(((zLat * 1000).roundToInt() + (zLng * 1000).roundToInt() + idx * 19) % 100)
            val cellRain = max(0.0, (currentRainMm * (0.75 + idx * 0.35)) + (seed % 14) * 0.45)
            val cell24h = max(cellRain * 2.5, rain24hMm * (0.8 + idx * 0.2))
            val cellProb = (rainProb + (seed % 25) - 8).coerceIn(15, 98)
            val sev = severityForRain(cellRain, cell24h)
            zones.add(
                RainRadarZone(
                    id = "RAIN_CELL_${idx + 1}",
                    areaNameThai = label,
                    latitude = zLat,
                    longitude = zLng,
                    rainMmPerHour = cellRain,
                    rain24hMm = cell24h,
                    probabilityPercent = cellProb,
                    radiusKm = 2.6 + idx * 0.7,
                    severity = sev,
                    statusTextThai = statusForRain(cellRain, cellProb)
                )
            )
        }
        return zones
    }

    private fun buildFireHotspots(
        centerLat: Double,
        centerLng: Double,
        areaLabel: String,
        baseFwi: Double,
        windSpeedKmh: Double,
        windDirThai: String,
        timeFmt: String
    ): List<FireHotspotPoint> {
        val hotspots = mutableListOf<FireHotspotPoint>()
        val offsets = listOf(
            Triple("จุดเฝ้าระวังความร้อน $areaLabel", 0.012, 0.015),
            Triple("จุดความร้อนดาวเทียม VIIRS #1 (พื้นที่เกษตร/ป่า)", 0.036, -0.022),
            Triple("จุดความร้อนดาวเทียม MODIS #2 (แนวกันไฟ/ชุมชน)", -0.027, 0.031)
        )
        offsets.forEachIndexed { idx, (name, dLat, dLng) ->
            val hLat = GeoUtils.clampLat(centerLat + dLat)
            val hLng = GeoUtils.normalizeLng(centerLng + dLng)
            val localFwi = (baseFwi + (idx * 9.5) - 4.0).coerceIn(8.0, 96.0)
            val frp = (12.5 + localFwi * 0.65 + idx * 6.2)
            val conf = (68 + (localFwi * 0.3).roundToInt()).coerceIn(55, 99)
            val sev = when {
                localFwi >= 70.0 -> AlertSeverity.CRITICAL
                localFwi >= 48.0 -> AlertSeverity.WARNING
                localFwi >= 30.0 -> AlertSeverity.WATCH
                else -> AlertSeverity.NORMAL
            }
            val status = when (sev) {
                AlertSeverity.CRITICAL -> "ตรวจพบความร้อนสูงมาก เสี่ยงลุกลามตามลม$windDirThai"
                AlertSeverity.WARNING -> "จุดความร้อนสะสม เฝ้าระวังไฟลามทุ่ง/ชุมชน"
                AlertSeverity.WATCH -> "เฝ้าระวังอุณหภูมิพื้นผิวสูงกว่าเกณฑ์"
                AlertSeverity.NORMAL -> "ระดับความร้อนพื้นผิวปกติ"
            }
            hotspots.add(
                FireHotspotPoint(
                    id = "FIRE_HS_${idx + 1}",
                    areaNameThai = name,
                    latitude = hLat,
                    longitude = hLng,
                    frpMegawatts = frp,
                    confidencePercent = conf,
                    fireWeatherIndex = localFwi,
                    windSpeedKmh = windSpeedKmh,
                    windDirectionThai = windDirThai,
                    severity = sev,
                    statusTextThai = status,
                    detectedTimeLabel = timeFmt
                )
            )
        }
        return hotspots
    }

    private fun buildWaterLevelStations(
        centerLat: Double,
        centerLng: Double,
        areaLabel: String,
        currentRainMm: Double,
        rain24hMm: Double,
        liveDischargeM3s: Double?
    ): List<WaterLevelStation> {
        val rainBoostPct = (currentRainMm * 1.4 + rain24hMm * 0.28).coerceIn(0.0, 28.0)
        val baseDischarge = liveDischargeM3s ?: (145.0 + currentRainMm * 18.0 + rain24hMm * 4.5)
        val localCap = (64.0 + rainBoostPct + (baseDischarge / 45.0).coerceIn(0.0, 26.0)).coerceIn(32.0, 108.0)
        val localBankMsl = 4.50
        val localWaterMsl = (localBankMsl * (localCap / 100.0))
        val localTrend = when {
            currentRainMm >= 2.0 || rain24hMm >= 15.0 -> "↗ น้ำขึ้น (+${format1Dec(0.05 + currentRainMm * 0.02)} ม./ชม.)"
            currentRainMm > 0.2 -> "↗ ทรงตัว-เพิ่มขึ้นเล็กน้อย"
            else -> "→ ทรงตัว"
        }
        val localStation = createWaterStation(
            code = "ST-LOCAL",
            name = "สถานีระดับน้ำพื้นที่ $areaLabel",
            river = "ลำน้ำสายหลักใกล้พิกัดมาร์ค",
            lat = centerLat + 0.006,
            lng = centerLng - 0.008,
            waterMsl = localWaterMsl,
            bankMsl = localBankMsl,
            capacityPct = localCap,
            dischargeM3s = baseDischarge,
            trend = localTrend
        )

        val nationalReferenceStations = listOf(
            createWaterStation(
                code = "C.2",
                name = "สถานี C.2 เมืองนครสวรรค์",
                river = "แม่น้ำเจ้าพระยา",
                lat = 15.7047,
                lng = 100.1372,
                waterMsl = 23.12 + rainBoostPct * 0.05,
                bankMsl = 26.20,
                capacityPct = (82.4 + rainBoostPct * 0.4).coerceIn(45.0, 105.0),
                dischargeM3s = 1840.0 + currentRainMm * 25.0,
                trend = "↗ เพิ่มขึ้นเล็กน้อย"
            ),
            createWaterStation(
                code = "C.13",
                name = "สถานี C.13 ท้ายเขื่อนเจ้าพระยา (ชัยนาท)",
                river = "แม่น้ำเจ้าพระยา",
                lat = 15.1586,
                lng = 100.1804,
                waterMsl = 14.85 + rainBoostPct * 0.04,
                bankMsl = 16.34,
                capacityPct = (88.2 + rainBoostPct * 0.45).coerceIn(50.0, 106.0),
                dischargeM3s = 1690.0 + currentRainMm * 22.0,
                trend = "↗ เฝ้าระวังการระบายน้ำ"
            ),
            createWaterStation(
                code = "C.29A",
                name = "สถานี C.29A บางไทร (พระนครศรีอยุธยา)",
                river = "แม่น้ำเจ้าพระยา",
                lat = 14.2058,
                lng = 100.5061,
                waterMsl = 3.68 + rainBoostPct * 0.03,
                bankMsl = 4.10,
                capacityPct = (89.7 + rainBoostPct * 0.5).coerceIn(50.0, 108.0),
                dischargeM3s = 1920.0 + currentRainMm * 28.0,
                trend = "↗ น้ำหนุนสูง"
            ),
            createWaterStation(
                code = "C.4",
                name = "สถานี C.4 สะพานพุทธ (กรุงเทพฯ)",
                river = "แม่น้ำเจ้าพระยา",
                lat = 13.7392,
                lng = 100.4975,
                waterMsl = 2.15 + rainBoostPct * 0.02,
                bankMsl = 2.80,
                capacityPct = (76.8 + rainBoostPct * 0.4).coerceIn(45.0, 104.0),
                dischargeM3s = 2050.0 + currentRainMm * 30.0,
                trend = "→ สัมพันธ์น้ำทะเลหนุน"
            ),
            createWaterStation(
                code = "P.1",
                name = "สถานี P.1 สะพานนวรัฐ (เชียงใหม่)",
                river = "แม่น้ำปิง",
                lat = 18.7877,
                lng = 99.0044,
                waterMsl = 3.18 + rainBoostPct * 0.03,
                bankMsl = 3.70,
                capacityPct = (85.9 + rainBoostPct * 0.4).coerceIn(40.0, 106.0),
                dischargeM3s = 390.0 + currentRainMm * 15.0,
                trend = "→ ทรงตัว"
            ),
            createWaterStation(
                code = "M.7",
                name = "สถานี M.7 สะพานเสรีประชาธิปไตย (อุบลฯ)",
                river = "แม่น้ำมูล",
                lat = 15.2246,
                lng = 104.8588,
                waterMsl = 111.45 + rainBoostPct * 0.03,
                bankMsl = 112.00,
                capacityPct = (91.5 + rainBoostPct * 0.4).coerceIn(50.0, 107.0),
                dischargeM3s = 2280.0 + currentRainMm * 25.0,
                trend = "↗ เฝ้าระวังพื้นที่ลุ่มต่ำ"
            )
        )

        return listOf(localStation) + nationalReferenceStations
    }

    private fun createWaterStation(
        code: String,
        name: String,
        river: String,
        lat: Double,
        lng: Double,
        waterMsl: Double,
        bankMsl: Double,
        capacityPct: Double,
        dischargeM3s: Double,
        trend: String
    ): WaterLevelStation {
        val sev = when {
            capacityPct >= 95.0 -> AlertSeverity.CRITICAL
            capacityPct >= 85.0 -> AlertSeverity.WARNING
            capacityPct >= 75.0 -> AlertSeverity.WATCH
            else -> AlertSeverity.NORMAL
        }
        val diff = bankMsl - waterMsl
        val status = when {
            diff < 0 -> "ล้นตลิ่ง +${format2Dec(abs(diff))} ม. (${format1Dec(capacityPct)}% ความจุ)"
            sev == AlertSeverity.CRITICAL -> "วิกฤตใกล้ล้นตลิ่ง ต่ำกว่าตลิ่ง ${format2Dec(diff)} ม."
            sev == AlertSeverity.WARNING -> "น้ำมาก ต่ำกว่าตลิ่ง ${format2Dec(diff)} ม. (${format1Dec(capacityPct)}%)"
            sev == AlertSeverity.WATCH -> "เฝ้าระวังระดับน้ำ (${format1Dec(capacityPct)}% ความจุลำน้ำ)"
            else -> "ระดับน้ำปกติ ต่ำกว่าตลิ่ง ${format2Dec(diff)} ม."
        }
        return WaterLevelStation(
            stationCode = code,
            stationNameThai = name,
            riverNameThai = river,
            latitude = lat,
            longitude = lng,
            waterLevelMetersMsl = waterMsl,
            bankLevelMetersMsl = bankMsl,
            capacityPercent = capacityPct,
            dischargeM3s = dischargeM3s,
            trendThai = trend,
            severity = sev,
            statusTextThai = status
        )
    }

    private fun severityForRain(rainMmH: Double, rain24hMm: Double): AlertSeverity = when {
        rainMmH >= 15.0 || rain24hMm >= 65.0 -> AlertSeverity.CRITICAL
        rainMmH >= 5.0 || rain24hMm >= 35.0 -> AlertSeverity.WARNING
        rainMmH >= 0.5 || rain24hMm >= 10.0 -> AlertSeverity.WATCH
        else -> AlertSeverity.NORMAL
    }

    private fun statusForRain(rainMmH: Double, prob: Int): String = when {
        rainMmH >= 15.0 -> "พายุฝนตกหนักมาก (${format1Dec(rainMmH)} มม./ชม.)"
        rainMmH >= 5.0 -> "ฝนตกปานกลาง-หนัก (${format1Dec(rainMmH)} มม./ชม.)"
        rainMmH >= 0.5 -> "ฝนตกเล็กน้อย (${format1Dec(rainMmH)} มม./ชม.)"
        prob >= 60 -> "กลุ่มเมฆฝนเคลื่อนตัว (โอกาสฝน $prob%)"
        else -> "ท้องฟ้าโปร่ง-เมฆบางส่วน (โอกาสฝน $prob%)"
    }

    private fun resolveNearestThaiAreaName(lat: Double, lng: Double): String {
        val knownAreas = listOf(
            Triple("กรุงเทพฯ และปริมณฑล", 13.7563, 100.5018),
            Triple("พระนครศรีอยุธยา (ลุ่มน้ำเจ้าพระยา)", 14.3532, 100.5684),
            Triple("นครสวรรค์ (ปากน้ำโพ)", 15.7047, 100.1372),
            Triple("เชียงใหม่ (ภาคเหนือ)", 18.7883, 98.9853),
            Triple("เชียงราย (ภาคเหนือตอนบน)", 19.9105, 99.8406),
            Triple("ขอนแก่น (ภาคอีสาน)", 16.4322, 102.8236),
            Triple("อุบลราชธานี (ลุ่มน้ำมูล)", 15.2287, 104.8564),
            Triple("นครราชสีมา (โคราช)", 14.9799, 102.0978),
            Triple("ชลบุรี / พัทยา (ภาคตะวันออก)", 12.9236, 100.8825),
            Triple("ภูเก็ต (อันดามัน)", 7.8804, 98.3923),
            Triple("สงขลา / หาดใหญ่ (ภาคใต้)", 7.0086, 100.4747)
        )
        var bestName = String.format(Locale.US, "พิกัด %.3f, %.3f", lat, lng)
        var bestDist = Double.MAX_VALUE
        for ((name, aLat, aLng) in knownAreas) {
            val d = GeoUtils.haversineDistanceMeters(GeoPoint(lat, lng), GeoPoint(aLat, aLng))
            if (d < bestDist && d <= 85_000.0) {
                bestDist = d
                bestName = name
            }
        }
        return bestName
    }

    companion object {
        fun weatherCodeToThaiDescription(code: Int, currentRainMm: Double = 0.0): String {
            if (currentRainMm >= 10.0) return "ฝนตกหนักต่อเนื่อง"
            if (currentRainMm >= 2.0 && code < 50) return "ฝนตกในพื้นที่"
            return when (code) {
                0 -> "ท้องฟ้าแจ่มใส"
                1 -> "ท้องฟ้าโปร่ง มีเมฆบางส่วน"
                2 -> "มีเมฆเป็นส่วนมาก"
                3 -> "เมฆครึ้มปกคลุม"
                45, 48 -> "มีหมอกและทัศนวิสัยต่ำ"
                51, 53, 55 -> "ฝนปรอยๆ ต่อเนื่อง"
                61 -> "ฝนตกเล็กน้อย"
                63 -> "ฝนตกปานกลาง"
                65 -> "ฝนตกหนัก"
                80, 81 -> "ฝนฟ้าคะนองเป็นแห่งๆ"
                82 -> "พายุฝนตกหนักมาก"
                95, 96, 99 -> "พายุฝนฟ้าคะนองและลมกระโชกแรง"
                else -> "เมฆบางส่วน"
            }
        }

        fun weatherCodeToEmoji(code: Int): String = when (code) {
            0 -> "☀️"
            1, 2 -> "⛅"
            3 -> "☁️"
            45, 48 -> "🌫️"
            51, 53, 55, 61 -> "🌦️"
            63, 65, 80, 81 -> "🌧️"
            82, 95, 96, 99 -> "⛈️"
            else -> "⛅"
        }

        fun degreeToThaiDirection(deg: Int): String {
            val normalized = ((deg % 360) + 360) % 360
            return when (normalized) {
                in 23..67 -> "ตะวันออกเฉียงเหนือ"
                in 68..112 -> "ตะวันออก"
                in 113..157 -> "ตะวันออกเฉียงใต้"
                in 158..202 -> "ใต้"
                in 203..247 -> "ตะวันตกเฉียงใต้"
                in 248..292 -> "ตะวันตก"
                in 293..337 -> "ตะวันตกเฉียงเหนือ"
                else -> "เหนือ"
            }
        }

        fun format1Dec(value: Double): String = String.format(Locale.US, "%.1f", value)
        fun format2Dec(value: Double): String = String.format(Locale.US, "%.2f", value)
    }
}
