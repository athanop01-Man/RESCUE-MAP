package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.EmergencyCategory
import com.example.data.EmergencyPinEntity
import com.example.map.GeoUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ApkExporter {
    const val EXPORT_APK_FILENAME = "E23WCM_Tactical_Map_v1.0.apk"

    fun getInstalledApkFile(context: Context): File {
        return File(context.applicationInfo.sourceDir)
    }

    fun getInstalledApkSizeFormatted(context: Context): String {
        val file = getInstalledApkFile(context)
        val sizeBytes = if (file.exists()) file.length() else 0L
        val mb = sizeBytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.2f MB", mb)
    }

    suspend fun saveApkToUri(context: Context, targetUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceApk = getInstalledApkFile(context)
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                FileInputStream(sourceApk).use { inputStream ->
                    inputStream.copyTo(outputStream, bufferSize = 64 * 1024)
                }
            } ?: error("ไม่สามารถเปิดปลายทางสำหรับบันทึกไฟล์ APK ได้")
            "บันทึกไฟล์ $EXPORT_APK_FILENAME สำเร็จพร้อมติดตั้ง"
        }
    }

    suspend fun saveApkToSystemDownloads(context: Context): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceApk = getInstalledApkFile(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, EXPORT_APK_FILENAME)
                    put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: error("ไม่สามารถสร้างไฟล์ในโฟลเดอร์ Downloads ได้")

                resolver.openOutputStream(uri)?.use { output ->
                    FileInputStream(sourceApk).use { input ->
                        input.copyTo(output, bufferSize = 64 * 1024)
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                "บันทึกไฟล์ $EXPORT_APK_FILENAME ลงโฟลเดอร์ Downloads สำเร็จ"
            } else {
                val exportDir = File(context.cacheDir, "apk").apply { mkdirs() }
                val destFile = File(exportDir, EXPORT_APK_FILENAME)
                FileInputStream(sourceApk).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output, bufferSize = 64 * 1024)
                    }
                }
                "บันทึกไฟล์ APK สำเร็จที่ ${destFile.name}"
            }
        }
    }

    fun openInGoogleMapsNavigation(context: Context, lat: Double, lng: Double, label: String = "พิกัดเป้าหมาย") {
        val encodedLabel = Uri.encode(label)
        val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedLabel)")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
                return
            }
        } catch (_: Exception) {
        }

        try {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (_: Exception) {
        }
    }

    fun startGoogleMapsTurnByTurn(context: Context, lat: Double, lng: Double) {
        val navUri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
        val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            if (navIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(navIntent)
                return
            }
        } catch (_: Exception) {
        }
        openInGoogleMapsNavigation(context, lat, lng, "นำทางพิกัดภาคสนาม")
    }

    fun shareCoordinateToOtherApps(
        context: Context,
        lat: Double,
        lng: Double,
        reporterName: String,
        reporterPhone: String
    ) {
        val decimalCoord = GeoUtils.formatLatLngDecimal(lat, lng)
        val dmsCoord = GeoUtils.formatDMS(lat, lng)
        val mapsUrl = "https://maps.google.com/?q=${String.format(Locale.US, "%.6f,%.6f", lat, lng)}"
        val phoneLine = if (reporterPhone.isNotBlank()) "\n📞 เบอร์โทรติดต่อ: $reporterPhone" else ""
        val nameLine = if (reporterName.isNotBlank()) "\n👤 ผู้แจ้ง/ผู้ใช้งาน: $reporterName" else ""

        val message = buildString {
            append("📍 แจ้งพิกัดภาคสนาม (E23WCM Tactical Map)\n")
            append("พิกัด (Lat, Lng): $decimalCoord\n")
            append("ระบบ DMS: $dmsCoord")
            append(nameLine)
            append(phoneLine)
            append("\n🗺️ เปิดนำทางใน Google Maps:\n$mapsUrl")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "แจ้งพิกัดภาคสนาม: $decimalCoord")
            putExtra(Intent.EXTRA_TEXT, message)
        }
        val chooser = Intent.createChooser(sendIntent, "แชร์ข้อมูลพิกัดไปยังแอปพลิเคชันอื่น")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(chooser) }
    }

    fun shareEmergencyPinToOtherApps(
        context: Context,
        pin: EmergencyPinEntity
    ) {
        val category = EmergencyCategory.fromCode(pin.categoryCode)
        val decimalCoord = GeoUtils.formatLatLngDecimal(pin.latitude, pin.longitude)
        val dmsCoord = GeoUtils.formatDMS(pin.latitude, pin.longitude)
        val mapsUrl = "https://maps.google.com/?q=${String.format(Locale.US, "%.6f,%.6f", pin.latitude, pin.longitude)}"
        val timeFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm น.", Locale.getDefault()).format(Date(pin.timestamp))

        val message = buildString {
            append("${category.badgeSymbol} แจ้งเหตุฉุกเฉิน: ${pin.title}\n")
            append("หมวดหมู่: ${category.titleThai}\n")
            if (pin.details.isNotBlank()) {
                append("รายละเอียด: ${pin.details}\n")
            }
            append("📍 พิกัด: $decimalCoord ($dmsCoord)\n")
            append("👤 ผู้แจ้ง: ${pin.callsign.ifBlank { "ไม่ระบุ" }}\n")
            if (pin.reporterPhone.isNotBlank()) {
                append("📞 เบอร์โทรศัพท์: ${pin.reporterPhone}\n")
            }
            append("🕒 เวลาแจ้งเหตุ: $timeFormatted\n")
            append("🗺️ นำทางด้วย Google Maps:\n$mapsUrl")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "แจ้งเหตุฉุกเฉิน: ${pin.title}")
            putExtra(Intent.EXTRA_TEXT, message)
        }
        val chooser = Intent.createChooser(sendIntent, "แชร์ข้อมูลจุดเกิดเหตุไปยังแอปอื่น")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(chooser) }
    }

    fun dialPhoneNumber(context: Context, phone: String) {
        if (phone.isBlank()) return
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone.trim())}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(dialIntent) }
    }
}
