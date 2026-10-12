package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.EmergencyCategory
import com.example.data.FireHotspotPoint
import com.example.data.HourlyWeatherPoint
import com.example.data.RainRadarZone
import com.example.data.RealtimeTelemetryService
import com.example.data.RealtimeTelemetrySnapshot
import com.example.data.RealtimeTelemetryTab
import com.example.data.WaterLevelStation
import com.example.map.GeoUtils
import com.example.ui.theme.NotoSansThaiFontFamily
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalBorderDark
import com.example.ui.theme.TacticalCardDark
import com.example.ui.theme.TacticalCoralRed
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TacticalSurfaceDark

@Composable
fun RealtimeTelemetryQuickBar(
    snapshot: RealtimeTelemetrySnapshot?,
    isLoading: Boolean,
    showRainOverlay: Boolean,
    showFireOverlay: Boolean,
    showWaterOverlay: Boolean,
    onOpenTab: (RealtimeTelemetryTab) -> Unit,
    onToggleRainOverlay: () -> Unit,
    onToggleFireOverlay: () -> Unit,
    onToggleWaterOverlay: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = Color(0xE6081223),
        border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("realtime_telemetry_quick_bar")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Top Row: 4 Real-Time Telemetry Pills (อากาศ • ฝน • ไฟไหม้ • ระดับน้ำ) + Refresh
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Live Pulse Badge
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = Color(0xFF064E3B),
                    border = BorderStroke(1.dp, TacticalEmerald),
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .clickable { onRefresh() }
                        .testTag("realtime_live_refresh_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isLoading) TacticalAmber else TacticalEmerald)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLoading) "กำลังโหลด..." else "LIVE",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 1. Weather Pill
                val tempText = if (snapshot != null) {
                    "${snapshot.weatherBadgeEmoji} ${RealtimeTelemetryService.format1Dec(snapshot.temperatureC)}°C • ${snapshot.weatherConditionThai}"
                } else {
                    "⛅ พยากรณ์อากาศ"
                }
                TelemetryMetricPill(
                    text = tempText,
                    accentColor = TacticalCyan,
                    tag = "realtime_pill_WEATHER",
                    onClick = { onOpenTab(RealtimeTelemetryTab.WEATHER) }
                )

                // 2. Rain Pill
                val rainColor = snapshot?.rainSeverity?.let { Color(it.colorHex) } ?: Color(0xFF38BDF8)
                val rainText = if (snapshot != null) {
                    "🌧️ ฝน ${RealtimeTelemetryService.format1Dec(snapshot.currentRainMm)} มม. (${snapshot.rainProbabilityPercent}%)"
                } else {
                    "🌧️ เรดาร์ฝน"
                }
                TelemetryMetricPill(
                    text = rainText,
                    accentColor = rainColor,
                    tag = "realtime_pill_RAIN",
                    onClick = { onOpenTab(RealtimeTelemetryTab.RAIN) }
                )

                // 3. Fire Pill
                val fireColor = snapshot?.fireRiskSeverity?.let { Color(it.colorHex) } ?: Color(0xFFF97316)
                val fireText = if (snapshot != null) {
                    "🔥 ไฟไหม้ FWI ${RealtimeTelemetryService.format1Dec(snapshot.fireWeatherIndex)} (${snapshot.fireHotspots.size} จุด)"
                } else {
                    "🔥 ไฟไหม้"
                }
                TelemetryMetricPill(
                    text = fireText,
                    accentColor = fireColor,
                    tag = "realtime_pill_FIRE",
                    onClick = { onOpenTab(RealtimeTelemetryTab.FIRE) }
                )

                // 4. Water Level Pill
                val waterColor = snapshot?.waterSeverity?.let { Color(it.colorHex) } ?: Color(0xFF0EA5E9)
                val waterText = if (snapshot != null) {
                    "🌊 ระดับน้ำ ${RealtimeTelemetryService.format1Dec(snapshot.localBankCapacityPercent)}% (${RealtimeTelemetryService.format2Dec(snapshot.localWaterLevelMsl)} ม.)"
                } else {
                    "🌊 ระดับน้ำ"
                }
                TelemetryMetricPill(
                    text = waterText,
                    accentColor = waterColor,
                    tag = "realtime_pill_WATER",
                    onClick = { onOpenTab(RealtimeTelemetryTab.WATER) }
                )
            }

            // Bottom Row: Map Overlay Layer Toggles (แสดงเรดาร์ฝน / จุดไฟไหม้ / สถานีระดับน้ำ บนแผนที่)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "แสดงบนแผนที่:",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = NotoSansThaiFontFamily,
                    fontWeight = FontWeight.Medium
                )

                OverlayToggleChip(
                    label = "🌧️ เรดาร์ฝน",
                    active = showRainOverlay,
                    activeColor = Color(0xFF0EA5E9),
                    tag = "toggle_rain_overlay_chip",
                    onClick = onToggleRainOverlay
                )

                OverlayToggleChip(
                    label = "🔥 จุดไฟไหม้",
                    active = showFireOverlay,
                    activeColor = Color(0xFFF97316),
                    tag = "toggle_fire_overlay_chip",
                    onClick = onToggleFireOverlay
                )

                OverlayToggleChip(
                    label = "🌊 สถานีระดับน้ำ",
                    active = showWaterOverlay,
                    activeColor = Color(0xFF10B981),
                    tag = "toggle_water_overlay_chip",
                    onClick = onToggleWaterOverlay
                )
            }
        }
    }
}

@Composable
private fun TelemetryMetricPill(
    text: String,
    accentColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0F1D33),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.75f))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = NotoSansThaiFontFamily,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun OverlayToggleChip(
    label: String,
    active: Boolean,
    activeColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(7.dp),
        color = if (active) activeColor.copy(alpha = 0.25f) else Color(0xFF0B1526),
        border = BorderStroke(
            width = 1.dp,
            color = if (active) activeColor else Color(0xFF334155)
        )
    ) {
        Text(
            text = if (active) "$label ✓" else label,
            color = if (active) Color.White else Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontFamily = NotoSansThaiFontFamily,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun RealtimeTelemetryDialog(
    snapshot: RealtimeTelemetrySnapshot?,
    isLoading: Boolean,
    selectedTab: RealtimeTelemetryTab,
    showRainOverlay: Boolean,
    showFireOverlay: Boolean,
    showWaterOverlay: Boolean,
    onSelectTab: (RealtimeTelemetryTab) -> Unit,
    onToggleRainOverlay: () -> Unit,
    onToggleFireOverlay: () -> Unit,
    onToggleWaterOverlay: () -> Unit,
    onRefresh: () -> Unit,
    onFocusMapCoordinate: (Double, Double, Double) -> Unit,
    onQuickPinHazard: (Double, Double, EmergencyCategory, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .testTag("realtime_telemetry_dialog"),
            shape = RoundedCornerShape(18.dp),
            color = TacticalSurfaceDark,
            border = BorderStroke(1.5.dp, TacticalCyan),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ศูนย์ติดตามสภาพอากาศ ฝน ไฟไหม้ ระดับน้ำ (เรียลไทม์)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subText = if (snapshot != null) {
                            "${snapshot.locationLabelThai} • อัปเดต ${snapshot.updatedTimeFormatted} น."
                        } else {
                            "กำลังดึงข้อมูลอุตุนิยมวิทยาและอุทกวิทยาเรียลไทม์..."
                        }
                        Text(
                            text = subText,
                            color = TacticalCyan,
                            fontSize = 11.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0E3A4C))
                                .testTag("btn_refresh_realtime_telemetry")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = TacticalCyan,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "อัปเดตข้อมูลเรียลไทม์",
                                    tint = TacticalCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TacticalCardDark)
                                .testTag("btn_close_realtime_telemetry")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "ปิดหน้าต่าง",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 4 Category Tabs (อากาศ • ฝน • ไฟไหม้ • ระดับน้ำ)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    RealtimeTelemetryTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectTab(tab) }
                                .testTag("tab_realtime_${tab.id}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) TacticalCyan else TacticalCardDark,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (selected) Color.White else TacticalBorderDark
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = tab.badgeEmoji, fontSize = 14.sp)
                                Text(
                                    text = tab.shortTitle,
                                    color = if (selected) Color(0xFF031B24) else Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = NotoSansThaiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Map Overlay Switcher Bar inside Dialog
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0B1528),
                    border = BorderStroke(1.dp, TacticalBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ชั้นข้อมูลสดบนแผนที่:",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                        OverlayToggleChip(
                            label = "🌧️ เรดาร์ฝน",
                            active = showRainOverlay,
                            activeColor = Color(0xFF0EA5E9),
                            tag = "dialog_toggle_rain_overlay",
                            onClick = onToggleRainOverlay
                        )
                        OverlayToggleChip(
                            label = "🔥 จุดไฟไหม้",
                            active = showFireOverlay,
                            activeColor = Color(0xFFF97316),
                            tag = "dialog_toggle_fire_overlay",
                            onClick = onToggleFireOverlay
                        )
                        OverlayToggleChip(
                            label = "🌊 ระดับน้ำ",
                            active = showWaterOverlay,
                            activeColor = Color(0xFF10B981),
                            tag = "dialog_toggle_water_overlay",
                            onClick = onToggleWaterOverlay
                        )
                    }
                }

                HorizontalDivider(color = TacticalBorderDark)

                // Content Body
                if (snapshot == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = TacticalCyan)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "กำลังเชื่อมต่อข้อมูลพยากรณ์อากาศ ฝน ไฟไหม้ และระดับน้ำเรียลไทม์...",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = NotoSansThaiFontFamily
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (selectedTab) {
                            RealtimeTelemetryTab.WEATHER -> WeatherForecastTabContent(snapshot)
                            RealtimeTelemetryTab.RAIN -> RainRadarTabContent(
                                snapshot = snapshot,
                                onFocusZone = { zone ->
                                    if (!showRainOverlay) onToggleRainOverlay()
                                    onFocusMapCoordinate(zone.latitude, zone.longitude, 13.5)
                                    onDismiss()
                                },
                                onPinFloodRisk = { zone ->
                                    onQuickPinHazard(
                                        zone.latitude,
                                        zone.longitude,
                                        EmergencyCategory.FLOOD,
                                        "เฝ้าระวังฝนตกหนัก: ${zone.areaNameThai}",
                                        "${zone.statusTextThai} • ปริมาณฝน ${RealtimeTelemetryService.format1Dec(zone.rainMmPerHour)} มม./ชม. (สะสม 24 ชม. ${RealtimeTelemetryService.format1Dec(zone.rain24hMm)} มม.)"
                                    )
                                    onDismiss()
                                }
                            )
                            RealtimeTelemetryTab.FIRE -> FireHotspotTabContent(
                                snapshot = snapshot,
                                onFocusHotspot = { hs ->
                                    if (!showFireOverlay) onToggleFireOverlay()
                                    onFocusMapCoordinate(hs.latitude, hs.longitude, 14.5)
                                    onDismiss()
                                },
                                onPinFireHazard = { hs ->
                                    onQuickPinHazard(
                                        hs.latitude,
                                        hs.longitude,
                                        EmergencyCategory.FIRE,
                                        "จุดความร้อน/เสี่ยงไฟไหม้: ${hs.areaNameThai}",
                                        "${hs.statusTextThai} • ค่าพลังงานความร้อน FRP ${RealtimeTelemetryService.format1Dec(hs.frpMegawatts)} MW • ดัชนี FWI ${RealtimeTelemetryService.format1Dec(hs.fireWeatherIndex)}"
                                    )
                                    onDismiss()
                                }
                            )
                            RealtimeTelemetryTab.WATER -> WaterLevelTabContent(
                                snapshot = snapshot,
                                onFocusStation = { st ->
                                    if (!showWaterOverlay) onToggleWaterOverlay()
                                    onFocusMapCoordinate(st.latitude, st.longitude, 14.0)
                                    onDismiss()
                                },
                                onPinWaterAlert = { st ->
                                    onQuickPinHazard(
                                        st.latitude,
                                        st.longitude,
                                        EmergencyCategory.FLOOD,
                                        "ระดับน้ำ ${st.stationNameThai}",
                                        "${st.riverNameThai} • ${st.statusTextThai} • ระดับน้ำ ${RealtimeTelemetryService.format2Dec(st.waterLevelMetersMsl)} ม.รทก. (อัตราไหล ${RealtimeTelemetryService.format1Dec(st.dischargeM3s)} ลบ.ม./วินาที)"
                                    )
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherForecastTabContent(snapshot: RealtimeTelemetrySnapshot) {
    // Primary Weather Card
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0E2238),
        border = BorderStroke(1.dp, TacticalCyan),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = snapshot.weatherBadgeEmoji, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${RealtimeTelemetryService.format1Dec(snapshot.temperatureC)}°C",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = snapshot.weatherConditionThai,
                            color = TacticalCyan,
                            fontSize = 13.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "รู้สึกจริง ${RealtimeTelemetryService.format1Dec(snapshot.feelsLikeC)}°C",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        fontFamily = NotoSansThaiFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "พิกัด ${GeoUtils.formatLatLngDecimal(snapshot.latitude, snapshot.longitude)}",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontFamily = NotoSansThaiFontFamily
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF1E3A5F))

            // 2x3 Grid of Meteorological Telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniBox(
                    label = "ความชื้นสัมพัทธ์",
                    value = "${snapshot.humidityPercent}%",
                    sub = "เมฆปกคลุม ${snapshot.cloudCoverPercent}%",
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "ความเร็วลม / ทิศลม",
                    value = "${RealtimeTelemetryService.format1Dec(snapshot.windSpeedKmh)} กม./ชม.",
                    sub = "ทิศ${snapshot.windDirectionThai} (กระโชก ${RealtimeTelemetryService.format1Dec(snapshot.windGustsKmh)})",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniBox(
                    label = "ความกดอากาศ",
                    value = "${RealtimeTelemetryService.format1Dec(snapshot.pressureHpa)} hPa",
                    sub = if (snapshot.pressureHpa < 1006.0) "ความกดอากาศต่ำ (เฝ้าระวังพายุ)" else "ความกดอากาศปกติ",
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "โอกาสเกิดฝน / ฝนสะสม",
                    value = "${snapshot.rainProbabilityPercent}%",
                    sub = "สะสม 24 ชม. ${RealtimeTelemetryService.format1Dec(snapshot.rain24hSumMm)} มม.",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Hourly Forecast Section
    Text(
        text = "พยากรณ์อากาศรายชั่วโมงล่วงหน้า (Hourly Forecast)",
        color = Color.White,
        fontSize = 13.sp,
        fontFamily = NotoSansThaiFontFamily,
        fontWeight = FontWeight.Bold
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        snapshot.hourlyForecasts.forEach { hr ->
            HourlyForecastCard(hr)
        }
    }
}

@Composable
private fun HourlyForecastCard(hr: HourlyWeatherPoint) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = TacticalCardDark,
        border = BorderStroke(1.dp, TacticalBorderDark),
        modifier = Modifier.width(88.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "${hr.timeLabel} น.",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                fontFamily = NotoSansThaiFontFamily,
                fontWeight = FontWeight.Bold
            )
            Text(text = hr.badgeEmoji, fontSize = 18.sp)
            Text(
                text = "${RealtimeTelemetryService.format1Dec(hr.tempC)}°C",
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = NotoSansThaiFontFamily,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "💧 ${hr.rainProbabilityPercent}%",
                color = Color(0xFF38BDF8),
                fontSize = 10.sp,
                fontFamily = NotoSansThaiFontFamily,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${RealtimeTelemetryService.format1Dec(hr.rainMm)} มม.",
                color = Color(0xFF94A3B8),
                fontSize = 9.sp,
                fontFamily = NotoSansThaiFontFamily
            )
        }
    }
}

@Composable
private fun RainRadarTabContent(
    snapshot: RealtimeTelemetrySnapshot,
    onFocusZone: (RainRadarZone) -> Unit,
    onPinFloodRisk: (RainRadarZone) -> Unit
) {
    val sevColor = Color(snapshot.rainSeverity.colorHex)
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = Color(0xFF0C1F35),
        border = BorderStroke(1.2.dp, sevColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌧️ สถานะเรดาร์น้ำฝน: ${snapshot.rainSeverity.labelThai}",
                    color = sevColor,
                    fontSize = 14.sp,
                    fontFamily = NotoSansThaiFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "โอกาสฝน ${snapshot.rainProbabilityPercent}%",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = NotoSansThaiFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = snapshot.rainStatusThai,
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = NotoSansThaiFontFamily
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniBox(
                    label = "ฝนตกขณะนี้",
                    value = "${RealtimeTelemetryService.format1Dec(snapshot.currentRainMm)} มม./ชม.",
                    sub = "เรดาร์ตรวจวัดสด",
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "ฝนสะสม 24 ชั่วโมง",
                    value = "${RealtimeTelemetryService.format1Dec(snapshot.rain24hSumMm)} มม.",
                    sub = if (snapshot.rain24hSumMm >= 35.0) "เฝ้าระวังน้ำท่วมขัง" else "ปริมาณน้ำฝนสะสมปกติ",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    Text(
        text = "กลุ่มฝนและเรดาร์ตรวจวัดรอบพิกัด (${snapshot.rainRadarZones.size} โซน)",
        color = Color.White,
        fontSize = 13.sp,
        fontFamily = NotoSansThaiFontFamily,
        fontWeight = FontWeight.Bold
    )

    snapshot.rainRadarZones.forEach { zone ->
        val zColor = Color(zone.severity.colorHex)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = TacticalCardDark,
            border = BorderStroke(1.dp, zColor.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌧️ ${zone.areaNameThai}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = NotoSansThaiFontFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${RealtimeTelemetryService.format1Dec(zone.rainMmPerHour)} มม./ชม.",
                        color = zColor,
                        fontSize = 12.sp,
                        fontFamily = NotoSansThaiFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${zone.statusTextThai} • สะสม 24 ชม. ${RealtimeTelemetryService.format1Dec(zone.rain24hMm)} มม. • รัศมี ${RealtimeTelemetryService.format1Dec(zone.radiusKm)} กม.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontFamily = NotoSansThaiFontFamily
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { onFocusZone(zone) },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TacticalCyan),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = TacticalCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ดูเรดาร์บนแผนที่",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { onPinFloodRisk(zone) },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ปักหมุดเฝ้าระวังฝน",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FireHotspotTabContent(
    snapshot: RealtimeTelemetrySnapshot,
    onFocusHotspot: (FireHotspotPoint) -> Unit,
    onPinFireHazard: (FireHotspotPoint) -> Unit
) {
    val fireColor = Color(snapshot.fireRiskSeverity.colorHex)
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = Color(0xFF23120B),
        border = BorderStroke(1.2.dp, fireColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔥 ดัชนีความเสี่ยงไฟไหม้: ${snapshot.fireRiskSeverity.labelThai}",
                    color = fireColor,
                    fontSize = 14.sp,
                    fontFamily = NotoSansThaiFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "FWI ${RealtimeTelemetryService.format1Dec(snapshot.fireWeatherIndex)} / 100",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = NotoSansThaiFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = snapshot.fireRiskStatusThai,
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = NotoSansThaiFontFamily
            )
            Text(
                text = "ทิศทางลมพัดลามไฟ: ${snapshot.fireSpreadAdvisoryThai}",
                color = TacticalAmber,
                fontSize = 11.sp,
                fontFamily = NotoSansThaiFontFamily,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    Text(
        text = "จุดความร้อน (Hotspot) และพื้นที่เฝ้าระวังไฟไหม้ (${snapshot.fireHotspots.size} จุด)",
        color = Color.White,
        fontSize = 13.sp,
        fontFamily = NotoSansThaiFontFamily,
        fontWeight = FontWeight.Bold
    )

    snapshot.fireHotspots.forEach { hs ->
        val hsColor = Color(hs.severity.colorHex)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = TacticalCardDark,
            border = BorderStroke(1.dp, hsColor.copy(alpha = 0.75f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥 ${hs.areaNameThai}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = NotoSansThaiFontFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "FRP ${RealtimeTelemetryService.format1Dec(hs.frpMegawatts)} MW",
                        color = hsColor,
                        fontSize = 11.sp,
                        fontFamily = NotoSansThaiFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${hs.statusTextThai} • ความเชื่อมั่น ${hs.confidencePercent}% • FWI ${RealtimeTelemetryService.format1Dec(hs.fireWeatherIndex)}",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontFamily = NotoSansThaiFontFamily
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { onFocusHotspot(hs) },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TacticalAmber),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = TacticalAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ชี้เป้าบนแผนที่",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { onPinFireHazard(hs) },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCoralRed),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ปักหมุดอัคคีภัย",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterLevelTabContent(
    snapshot: RealtimeTelemetrySnapshot,
    onFocusStation: (WaterLevelStation) -> Unit,
    onPinWaterAlert: (WaterLevelStation) -> Unit
) {
    val waterColor = Color(snapshot.waterSeverity.colorHex)
    Surface(
        shape = RoundedCornerShape(13.dp),
        color = Color(0xFF0A2239),
        border = BorderStroke(1.2.dp, waterColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌊 สถานการณ์ระดับน้ำ: ${snapshot.waterSeverity.labelThai}",
                    color = waterColor,
                    fontSize = 14.sp,
                    fontFamily = NotoSansThaiFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${RealtimeTelemetryService.format1Dec(snapshot.localBankCapacityPercent)}% ความจุลำน้ำ",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = NotoSansThaiFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = snapshot.waterStatusThai,
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = NotoSansThaiFontFamily
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniBox(
                    label = "ระดับน้ำพื้นที่ (ม.รทก.)",
                    value = "${RealtimeTelemetryService.format2Dec(snapshot.localWaterLevelMsl)} ม.",
                    sub = snapshot.waterTrendThai,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "อัตราการไหลของน้ำ",
                    value = "${RealtimeTelemetryService.format1Dec(snapshot.localRiverDischargeM3s)} ลบ.ม./วิ",
                    sub = "ข้อมูลอุทกวิทยา GloFAS",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    Text(
        text = "สถานีวัดระดับน้ำสายหลักและพื้นที่ใกล้เคียง (${snapshot.waterStations.size} สถานี)",
        color = Color.White,
        fontSize = 13.sp,
        fontFamily = NotoSansThaiFontFamily,
        fontWeight = FontWeight.Bold
    )

    snapshot.waterStations.forEach { st ->
        val stColor = Color(st.severity.colorHex)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = TacticalCardDark,
            border = BorderStroke(1.dp, stColor.copy(alpha = 0.75f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🌊 ${st.stationNameThai}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${st.riverNameThai} • รหัส ${st.stationCode} • ${st.trendThai}",
                            color = TacticalCyan,
                            fontSize = 10.sp,
                            fontFamily = NotoSansThaiFontFamily
                        )
                    }
                    Text(
                        text = "${RealtimeTelemetryService.format1Dec(st.capacityPercent)}%",
                        color = stColor,
                        fontSize = 14.sp,
                        fontFamily = NotoSansThaiFontFamily,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Water Level Capacity Bar
                val fillFraction = (st.capacityPercent / 100.0).toFloat().coerceIn(0.05f, 1.0f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF091326))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fillFraction)
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(stColor)
                    )
                }

                Text(
                    text = "${st.statusTextThai} • ระดับน้ำ ${RealtimeTelemetryService.format2Dec(st.waterLevelMetersMsl)} / ตลิ่ง ${RealtimeTelemetryService.format2Dec(st.bankLevelMetersMsl)} ม.รทก. • ไหล ${RealtimeTelemetryService.format1Dec(st.dischargeM3s)} ลบ.ม./วิ",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontFamily = NotoSansThaiFontFamily
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { onFocusStation(st) },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TacticalCyan),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = TacticalCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ดูสถานีบนแผนที่",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { onPinWaterAlert(st) },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ปักหมุดระดับน้ำ",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniBox(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF091526),
        border = BorderStroke(1.dp, Color(0xFF1E3A5F)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontFamily = NotoSansThaiFontFamily
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = NotoSansThaiFontFamily,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub,
                color = TacticalCyan,
                fontSize = 10.sp,
                fontFamily = NotoSansThaiFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
