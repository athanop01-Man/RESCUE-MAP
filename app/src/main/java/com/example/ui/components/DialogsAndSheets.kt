package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.EmergencyCategory
import com.example.data.EmergencyPinEntity
import com.example.data.GeoPoint
import com.example.map.GeoUtils
import com.example.map.GoogleMapLayer
import com.example.ui.theme.NotoSansThaiFontFamily
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TacticalBorderDark
import com.example.ui.theme.TacticalCardDark
import com.example.ui.theme.TacticalCoralRed
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TacticalEmerald
import com.example.ui.theme.TacticalNavyDark
import com.example.ui.theme.TacticalSurfaceDark
import com.example.util.ApkExporter

@Composable
fun SettingsAndPreferencesDialog(
    reporterName: String,
    reporterPhone: String,
    selectedLayer: GoogleMapLayer,
    showScaleBar: Boolean,
    brushSizePx: Int,
    brushColorHex: Long,
    cachedTileCount: Int,
    strokesAndNotesCount: Int,
    emergencyPinsCount: Int,
    onSaveReporterProfile: (String, String) -> Unit,
    onSelectLayer: (GoogleMapLayer) -> Unit,
    onToggleScaleBar: (Boolean) -> Unit,
    onSelectBrushSizePx: (Int) -> Unit,
    onSelectBrushColor: (Long) -> Unit,
    onClearTileCache: () -> Unit,
    onClearDrawingsAndNotes: () -> Unit,
    onClearEmergencyPins: () -> Unit,
    onDownloadApkToDevice: () -> Unit,
    onSaveApkAsDocument: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val apkSizeText = remember { ApkExporter.getInstalledApkSizeFormatted(context) }
    val scrollState = rememberScrollState()

    var editReporterName by remember(reporterName) { mutableStateOf(reporterName) }
    var editReporterPhone by remember(reporterPhone) { mutableStateOf(reporterPhone) }

    val brushColors = listOf(
        0xFFEF4444L to "แดง",
        0xFF06B6D4L to "ฟ้า",
        0xFFF59E0BL to "ส้ม",
        0xFF10B981L to "เขียว",
        0xFFFFFFFFL to "ขาว"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
                .heightIn(max = 620.dp)
                .testTag("settings_dialog"),
            shape = RoundedCornerShape(18.dp),
            color = TacticalSurfaceDark,
            border = BorderStroke(1.2.dp, TacticalBorderDark)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Compact Header with tactical banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_tactical_header_1791124047533),
                        contentDescription = "แบนเนอร์ระบบแผนที่ยุทธวิธี",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xAA0A1120), Color(0xF0111C33))
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚙️ ตั้งค่าระบบและแผนที่",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontFamily = NotoSansThaiFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "100% Google Maps Engine (ออนไลน์ 5 ชั้นข้อมูล)",
                                color = TacticalCyan,
                                fontSize = 10.sp,
                                fontFamily = NotoSansThaiFontFamily
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0x880F172A), CircleShape)
                                .testTag("close_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "ปิดหน้าต่างตั้งค่า",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Section 0: ข้อมูลผู้ใช้งาน / ผู้แจ้งเหตุ & เบอร์โทรศัพท์ติดต่อ
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TacticalCardDark),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TacticalCyan)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = TacticalCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ข้อมูลผู้ใช้งาน / ผู้แจ้งเหตุภาคสนาม",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontFamily = NotoSansThaiFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedTextField(
                                value = editReporterName,
                                onValueChange = { editReporterName = it },
                                label = {
                                    Text(
                                        "ชื่อผู้ใช้งาน / ผู้แจ้งเหตุ (นามเรียกขาน)",
                                        fontFamily = NotoSansThaiFontFamily,
                                        fontSize = 11.sp
                                    )
                                },
                                placeholder = {
                                    Text("เช่น E23WCM ช่างแมน / กู้ภัย 01", fontFamily = NotoSansThaiFontFamily, fontSize = 11.sp)
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TacticalCyan,
                                    unfocusedBorderColor = Color(0xFF475569),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_input_reporter_name")
                            )

                            OutlinedTextField(
                                value = editReporterPhone,
                                onValueChange = { editReporterPhone = it },
                                label = {
                                    Text(
                                        "เบอร์โทรศัพท์ติดต่อผู้แจ้ง",
                                        fontFamily = NotoSansThaiFontFamily,
                                        fontSize = 11.sp
                                    )
                                },
                                placeholder = {
                                    Text("เช่น 081-234-5678", fontFamily = NotoSansThaiFontFamily, fontSize = 11.sp)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = TacticalEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TacticalEmerald,
                                    unfocusedBorderColor = Color(0xFF475569),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_input_reporter_phone")
                            )

                            Button(
                                onClick = {
                                    onSaveReporterProfile(editReporterName, editReporterPhone)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .testTag("btn_save_reporter_profile"),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TacticalCyan,
                                    contentColor = Color(0xFF031B24)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "บันทึกชื่อผู้ใช้งานและเบอร์โทรศัพท์",
                                    fontFamily = NotoSansThaiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Section 1: 5 Google Maps Layers (Compact)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TacticalCardDark),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TacticalBorderDark)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = TacticalCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "รูปแบบแผนที่ Google Maps (5 รูปแบบ)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontFamily = NotoSansThaiFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            GoogleMapLayer.entries.forEach { layer ->
                                val isSelected = layer == selectedLayer
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .clickable { onSelectLayer(layer) }
                                        .testTag("layer_option_${layer.id}"),
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (isSelected) Color(0xFF0E3A4C) else Color(0xFF0F172A),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) TacticalCyan else Color(0xFF1E293B)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onSelectLayer(layer) },
                                            modifier = Modifier.size(20.dp),
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = TacticalCyan,
                                                unselectedColor = Color(0xFF64748B)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = layer.titleThai,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontFamily = NotoSansThaiFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = layer.descriptionThai,
                                                color = if (isSelected) Color(0xFFA5F3FC) else Color(0xFF94A3B8),
                                                fontSize = 10.sp,
                                                fontFamily = NotoSansThaiFontFamily,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Display & Brush Tools (Scale Bar + 2px, 4px, 6px, 8px)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TacticalCardDark),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TacticalBorderDark)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Straighten,
                                    contentDescription = null,
                                    tint = TacticalAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "การแสดงผลและเครื่องมือ",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontFamily = NotoSansThaiFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(Color(0xFF0F172A))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "แถบสเกลวัดระยะทางจริง (Scale Bar)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontFamily = NotoSansThaiFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "แสดงที่มุมซ้ายล่างของแผนที่",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        fontFamily = NotoSansThaiFontFamily
                                    )
                                }
                                Switch(
                                    checked = showScaleBar,
                                    onCheckedChange = onToggleScaleBar,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = TacticalCyan
                                    ),
                                    modifier = Modifier.testTag("switch_scale_bar")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ขนาดเส้นหัวแปรงวาดรูปทรง (2px, 4px, 6px, 8px):",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = NotoSansThaiFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(2, 4, 6, 8).forEach { sizePx ->
                                    val selected = brushSizePx == sizePx
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onSelectBrushSizePx(sizePx) }
                                            .testTag("brush_size_${sizePx}px"),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) Color(0xFF0E3A4C) else Color(0xFF0F172A),
                                        border = BorderStroke(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) TacticalCyan else Color(0xFF334155)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "${sizePx}px",
                                                color = if (selected) TacticalCyan else Color.White,
                                                fontSize = 11.sp,
                                                fontFamily = NotoSansThaiFontFamily,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(24.dp)
                                                    .height(sizePx.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color(brushColorHex))
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "สีเส้นวาด:",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    fontFamily = NotoSansThaiFontFamily
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    brushColors.forEach { (hex, name) ->
                                        val isChosen = brushColorHex == hex
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(Color(hex))
                                                .border(
                                                    width = if (isChosen) 2.5.dp else 1.dp,
                                                    color = if (isChosen) TacticalCyan else Color.Black,
                                                    shape = CircleShape
                                                )
                                                .clickable { onSelectBrushColor(hex) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isChosen) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = name,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 3: Data & Cache Management (Compact)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TacticalCardDark),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TacticalBorderDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Cached,
                                    contentDescription = null,
                                    tint = TacticalEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "การจัดการข้อมูลและแคช",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontFamily = NotoSansThaiFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = onClearTileCache,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_clear_tile_cache"),
                                shape = RoundedCornerShape(9.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                border = BorderStroke(1.dp, TacticalCyan),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF0F172A),
                                    contentColor = TacticalCyan
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Cached, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ล้างแคชแผนที่ในหน่วยความจำ ($cachedTileCount ไทล์)",
                                        fontFamily = NotoSansThaiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "รีเฟรชการโหลดไทล์ใหม่จาก Google Maps",
                                        fontFamily = NotoSansThaiFontFamily,
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onClearDrawingsAndNotes,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_clear_drawings"),
                                shape = RoundedCornerShape(9.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                border = BorderStroke(1.dp, TacticalAmber),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF0F172A),
                                    contentColor = TacticalAmber
                                )
                            ) {
                                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ลบรูปวาดและเส้นวัดทั้งหมด ($strokesAndNotesCount รายการ)",
                                        fontFamily = NotoSansThaiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "เคลียร์รูปวาด เส้นวัด และข้อความที่จดบันทึก",
                                        fontFamily = NotoSansThaiFontFamily,
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onClearEmergencyPins,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_clear_emergency_pins"),
                                shape = RoundedCornerShape(9.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                border = BorderStroke(1.dp, TacticalCoralRed),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF0F172A),
                                    contentColor = TacticalCoralRed
                                )
                            ) {
                                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ลบหมุดเหตุฉุกเฉินทั้งหมด ($emergencyPinsCount หมุด)",
                                        fontFamily = NotoSansThaiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "เคลียร์จุดเกิดเหตุทั้งหมดในฐานข้อมูล",
                                        fontFamily = NotoSansThaiFontFamily,
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    // Section 4: Save & Download APK File (Without Share button)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E293B)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TacticalCyan)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = TacticalCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "บันทึกไฟล์ APK พร้อมดาวน์โหลด ($apkSizeText)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontFamily = NotoSansThaiFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = ApkExporter.EXPORT_APK_FILENAME,
                                        color = Color(0xFFA5F3FC),
                                        fontSize = 10.sp,
                                        fontFamily = NotoSansThaiFontFamily
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = onDownloadApkToDevice,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("btn_download_apk_direct"),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TacticalCyan,
                                        contentColor = Color(0xFF031B24)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ดาวน์โหลดลงเครื่อง",
                                        fontFamily = NotoSansThaiFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = onSaveApkAsDocument,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("btn_save_apk_as"),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    border = BorderStroke(1.dp, TacticalCyan),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "เลือกโฟลเดอร์บันทึก",
                                        fontFamily = NotoSansThaiFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = TacticalBorderDark)

                    // Developer Footer Credit at the bottom of Settings Menu
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("developer_credit_card"),
                        shape = RoundedCornerShape(12.dp),
                        color = TacticalNavyDark,
                        border = BorderStroke(1.dp, TacticalAmber)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TacticalAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ผู้พัฒนา: E23WCM ช่างแมน",
                                color = TacticalAmber,
                                fontSize = 14.sp,
                                fontFamily = NotoSansThaiFontFamily,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmergencyPinCreationDialog(
    coordinate: GeoPoint,
    initialReporterName: String,
    initialReporterPhone: String,
    onConfirmPin: (
        category: EmergencyCategory,
        title: String,
        details: String,
        reporterName: String,
        reporterPhone: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(EmergencyCategory.ACCIDENT) }
    var customTitle by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var reporterNameInput by remember(initialReporterName) { mutableStateOf(initialReporterName) }
    var reporterPhoneInput by remember(initialReporterPhone) { mutableStateOf(initialReporterPhone) }
    val scrollState = rememberScrollState()
    val accentColor = Color(selectedCategory.defaultColorHex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 400.dp)
                .heightIn(max = 600.dp)
                .testTag("emergency_pin_dialog"),
            shape = RoundedCornerShape(18.dp),
            color = TacticalSurfaceDark,
            border = BorderStroke(1.2.dp, accentColor)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "ปักหมุดเหตุฉุกเฉิน (8 หมวดหมู่)",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontFamily = NotoSansThaiFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "พิกัด: ${GeoUtils.formatLatLngDecimal(coordinate.latitude, coordinate.longitude)}",
                                color = TacticalCyan,
                                fontSize = 10.sp,
                                fontFamily = NotoSansThaiFontFamily
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "ปิด",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxItemsInEachRow = 2
                ) {
                    EmergencyCategory.entries.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedCategory = cat }
                                .testTag("category_${cat.code}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF1E293B) else Color(0xFF0A1120),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(cat.defaultColorHex) else Color(0xFF334155)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = cat.badgeSymbol, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = cat.shortTitle,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = NotoSansThaiFontFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = {
                        Text("ชื่อจุดเกิดเหตุ (เว้นว่างใช้ชื่อหมวดหมู่)", fontFamily = NotoSansThaiFontFamily, fontSize = 11.sp)
                    },
                    placeholder = {
                        Text(selectedCategory.shortTitle, fontFamily = NotoSansThaiFontFamily, fontSize = 12.sp)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalCyan,
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_emergency_title")
                )

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = {
                        Text("รายละเอียดสถานการณ์ (Noto Sans Thai)", fontFamily = NotoSansThaiFontFamily, fontSize = 11.sp)
                    },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalCyan,
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_emergency_details")
                )

                // ชื่อผู้ใช้งาน/ผู้แจ้งเหตุ & เบอร์โทรศัพท์ติดต่อ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = reporterNameInput,
                        onValueChange = { reporterNameInput = it },
                        label = {
                            Text("ชื่อผู้แจ้งเหตุ", fontFamily = NotoSansThaiFontFamily, fontSize = 10.sp)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalAmber,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_reporter_name")
                    )

                    OutlinedTextField(
                        value = reporterPhoneInput,
                        onValueChange = { reporterPhoneInput = it },
                        label = {
                            Text("เบอร์โทรศัพท์", fontFamily = NotoSansThaiFontFamily, fontSize = 10.sp)
                        },
                        placeholder = {
                            Text("08x-xxx-xxxx", fontFamily = NotoSansThaiFontFamily, fontSize = 10.sp)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalEmerald,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_reporter_phone")
                    )
                }

                // Bottom action row (without share button)
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isCompactDialog = maxWidth < 310.dp
                    val btnHeight = if (isCompactDialog) 34.dp else 36.dp
                    val btnFontSize = if (isCompactDialog) 11.sp else 12.sp

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(0.8f)
                                .height(btnHeight),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(9.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text(
                                text = "ยกเลิก",
                                fontFamily = NotoSansThaiFontFamily,
                                fontSize = btnFontSize,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = {
                                onConfirmPin(
                                    selectedCategory,
                                    customTitle,
                                    details,
                                    reporterNameInput,
                                    reporterPhoneInput
                                )
                            },
                            modifier = Modifier
                                .weight(1.4f)
                                .height(btnHeight)
                                .testTag("confirm_emergency_pin_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            shape = RoundedCornerShape(9.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "ปักหมุดลงแผนที่",
                                fontFamily = NotoSansThaiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = btnFontSize,
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

@Composable
fun ThaiTextNoteDialog(
    coordinate: GeoPoint,
    onConfirmNote: (String, Long, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var noteText by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableLongStateOf(0xFF06B6D4L) }
    var selectedFontSizeSp by remember { mutableIntStateOf(14) }

    val colorChoices = listOf(
        0xFF06B6D4L to "ฟ้า",
        0xFFF59E0BL to "เหลือง",
        0xFFEF4444L to "แดง",
        0xFF10B981L to "เขียว",
        0xFFA855F7L to "ม่วง"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 380.dp)
                .testTag("thai_text_note_dialog"),
            shape = RoundedCornerShape(18.dp),
            color = TacticalSurfaceDark,
            border = BorderStroke(1.2.dp, Color(selectedColorHex))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(selectedColorHex),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "พิมพ์ข้อความไทย (Noto Sans Thai)",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = NotoSansThaiFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "พิกัด: ${GeoUtils.formatLatLngDecimal(coordinate.latitude, coordinate.longitude)}",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontFamily = NotoSansThaiFontFamily
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "ปิด", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = {
                        Text("ข้อความภาษาไทยบนแผนที่", fontFamily = NotoSansThaiFontFamily, fontSize = 11.sp)
                    },
                    placeholder = {
                        Text("เช่น จุดสกัดกั้น / เส้นทางสำรอง", fontFamily = NotoSansThaiFontFamily, fontSize = 12.sp)
                    },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(selectedColorHex),
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_thai_note_text")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "สีป้าย:",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontFamily = NotoSansThaiFontFamily
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorChoices.forEach { (hex, _) ->
                            val chosen = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(
                                        width = if (chosen) 2.5.dp else 1.dp,
                                        color = if (chosen) Color.White else Color.Black,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ขนาด:",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontFamily = NotoSansThaiFontFamily
                    )
                    listOf(12, 14, 16, 18).forEach { spSize ->
                        val chosen = selectedFontSizeSp == spSize
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(7.dp))
                                .clickable { selectedFontSizeSp = spSize },
                            color = if (chosen) Color(0xFF0E3A4C) else Color(0xFF0A1120),
                            border = BorderStroke(1.dp, if (chosen) TacticalCyan else Color(0xFF334155))
                        ) {
                            Text(
                                text = "${spSize}sp",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = NotoSansThaiFontFamily,
                                fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(9.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("ยกเลิก", fontFamily = NotoSansThaiFontFamily, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            if (noteText.isNotBlank()) {
                                onConfirmNote(noteText, selectedColorHex, selectedFontSizeSp)
                            }
                        },
                        enabled = noteText.isNotBlank(),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(36.dp)
                            .testTag("confirm_thai_note_button"),
                        shape = RoundedCornerShape(9.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(selectedColorHex),
                            contentColor = Color(0xFF031B24)
                        )
                    ) {
                        Text(
                            text = "วางข้อความ",
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedEmergencyPinDetailDialog(
    pin: EmergencyPinEntity,
    onNavigateGoogleMaps: (Double, Double, String) -> Unit,
    onCallReporterPhone: (String) -> Unit,
    onDeletePin: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val category = EmergencyCategory.fromCode(pin.categoryCode)
    val pinColor = Color(category.defaultColorHex)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 380.dp),
            shape = RoundedCornerShape(18.dp),
            color = TacticalSurfaceDark,
            border = BorderStroke(1.5.dp, pinColor)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(text = category.badgeSymbol, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = pin.title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontFamily = NotoSansThaiFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = category.titleThai,
                                color = pinColor,
                                fontSize = 11.sp,
                                fontFamily = NotoSansThaiFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "ปิด", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0A1120),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "พิกัด: ${GeoUtils.formatLatLngDecimal(pin.latitude, pin.longitude)}",
                            color = TacticalCyan,
                            fontSize = 12.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = pin.details,
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            fontFamily = NotoSansThaiFontFamily
                        )
                        HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "👤 ผู้แจ้ง: ${pin.callsign.ifBlank { "ไม่ระบุ" }}",
                                    color = TacticalAmber,
                                    fontSize = 11.sp,
                                    fontFamily = NotoSansThaiFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (pin.reporterPhone.isNotBlank()) {
                                    Text(
                                        text = "📞 โทร: ${pin.reporterPhone}",
                                        color = TacticalEmerald,
                                        fontSize = 11.sp,
                                        fontFamily = NotoSansThaiFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (pin.reporterPhone.isNotBlank()) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onCallReporterPhone(pin.reporterPhone) }
                                        .testTag("btn_call_reporter"),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF064E3B),
                                    border = BorderStroke(1.dp, TacticalEmerald)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "โทรติดต่อผู้แจ้ง",
                                            tint = TacticalEmerald,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "โทรติดต่อ",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontFamily = NotoSansThaiFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action row (without share button): ลบหมุด | นำทาง Google Maps
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isCompact = maxWidth < 310.dp
                    val btnHeight = if (isCompact) 34.dp else 36.dp
                    val btnFontSize = if (isCompact) 10.sp else 11.sp
                    val btnIconSize = if (isCompact) 13.dp else 14.dp

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onDeletePin(pin.id) },
                            modifier = Modifier
                                .weight(0.8f)
                                .height(btnHeight),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            border = BorderStroke(1.dp, TacticalCoralRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalCoralRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(btnIconSize)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "ลบหมุด",
                                fontFamily = NotoSansThaiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = btnFontSize,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = { onNavigateGoogleMaps(pin.latitude, pin.longitude, pin.title) },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(btnHeight),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TacticalEmerald,
                                contentColor = Color(0xFF031B24)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(btnIconSize)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "นำทาง Google Maps",
                                fontFamily = NotoSansThaiFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = btnFontSize,
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

@Composable
fun ApkDownloadCenterDialog(
    lastExportMessage: String?,
    onDownloadToDownloadsFolder: () -> Unit,
    onSaveApkChooseFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val apkSizeText = remember { ApkExporter.getInstalledApkSizeFormatted(context) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 380.dp)
                .testTag("apk_download_center_dialog"),
            shape = RoundedCornerShape(18.dp),
            color = TacticalSurfaceDark,
            border = BorderStroke(1.2.dp, TacticalEmerald)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(TacticalEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = TacticalEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "บันทึกไฟล์ APK พร้อมดาวน์โหลด",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = NotoSansThaiFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "พัฒนาโดย E23WCM ช่างแมน",
                                color = TacticalEmerald,
                                fontSize = 10.sp,
                                fontFamily = NotoSansThaiFontFamily
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "ปิด",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TacticalNavyDark,
                    border = BorderStroke(1.dp, TacticalBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "ไฟล์: ${ApkExporter.EXPORT_APK_FILENAME}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = NotoSansThaiFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ขนาดไฟล์: $apkSizeText • พร้อมติดตั้งใช้งานทันที",
                            color = TacticalCyan,
                            fontSize = 11.sp,
                            fontFamily = NotoSansThaiFontFamily
                        )
                    }
                }

                if (lastExportMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF064E3B),
                        border = BorderStroke(1.dp, TacticalEmerald),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = TacticalEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = lastExportMessage,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = NotoSansThaiFontFamily
                            )
                        }
                    }
                }

                Button(
                    onClick = onDownloadToDownloadsFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("dialog_btn_download_apk_now"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TacticalEmerald,
                        contentColor = Color(0xFF031B24)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "บันทึกไฟล์ .APK ลงโฟลเดอร์ Downloads",
                        fontFamily = NotoSansThaiFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onSaveApkChooseFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("dialog_btn_choose_apk_folder"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TacticalCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(
                        text = "เลือกโฟลเดอร์จัดเก็บไฟล์ .APK ด้วยตนเอง",
                        fontFamily = NotoSansThaiFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
