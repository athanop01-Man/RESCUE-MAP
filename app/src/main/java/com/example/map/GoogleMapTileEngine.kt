package com.example.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request

enum class GoogleMapLayer(
    val id: String,
    val titleThai: String,
    val shortTitle: String,
    val descriptionThai: String,
    val lyrsParam: String,
    val badgeText: String
) {
    ROADMAP(
        id = "ROADMAP",
        titleThai = "Google Maps (แผนที่ถนน)",
        shortTitle = "แผนที่ถนน",
        descriptionThai = "แผนที่มาตรฐานของ Google คมชัด แสดงเส้นทาง ถนน ตรอกซอกซอย และสถานที่สำคัญ",
        lyrsParam = "m",
        badgeText = "ถนน"
    ),
    HYBRID(
        id = "HYBRID",
        titleThai = "Google Maps (ดาวเทียม + เส้นทาง Hybrid)",
        shortTitle = "ไฮบริด",
        descriptionThai = "ภาพถ่ายดาวเทียมความละเอียดสูงพร้อมเส้นทางและชื่อสถานที่",
        lyrsParam = "y",
        badgeText = "ไฮบริด"
    ),
    SATELLITE(
        id = "SATELLITE",
        titleThai = "Google Maps (ดาวเทียมภาพถ่ายแท้ Satellite)",
        shortTitle = "ดาวเทียมแท้",
        descriptionThai = "ภาพถ่ายทางอากาศและภาพถ่ายดาวเทียมแท้แบบไร้ป้ายกำกับ",
        lyrsParam = "s",
        badgeText = "ดาวเทียม"
    ),
    TERRAIN(
        id = "TERRAIN",
        titleThai = "Google Maps (ภูมิประเทศและความสูง Terrain)",
        shortTitle = "ภูมิประเทศ",
        descriptionThai = "แสดงเส้นชั้นความสูง เนินเขา ความลาดชัน และลักษณะทางภูมิศาสตร์",
        lyrsParam = "p",
        badgeText = "ภูมิประเทศ"
    ),
    TRAFFIC(
        id = "TRAFFIC",
        titleThai = "Google Maps (ถนน + การจราจรสด Traffic)",
        shortTitle = "จราจรสด",
        descriptionThai = "แผนที่ถนนมาตรฐานพร้อมสภาพการจราจรแบบเรียลไทม์",
        lyrsParam = "m,traffic",
        badgeText = "จราจรสด"
    );

    fun buildTileUrl(x: Int, y: Int, z: Int, cacheBuster: Int = 0): String {
        val serverIdx = ((x + y) and 3).coerceIn(0, 3)
        val cbSuffix = if (cacheBuster > 1) "&cb=$cacheBuster" else ""
        return "https://mt$serverIdx.google.com/vt/lyrs=$lyrsParam&hl=th&x=$x&y=$y&z=$z&scale=2$cbSuffix"
    }
}

data class TileKey(
    val layerId: String,
    val z: Int,
    val x: Int,
    val y: Int,
    val generation: Int
)

class GoogleMapTileEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val okHttpClient = OkHttpClient.Builder()
        .dispatcher(
            Dispatcher().apply {
                maxRequests = 48
                maxRequestsPerHost = 16
            }
        )
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // In-memory LRU Cache storing up to 420 tiles
    private val maxTilesInMemory = 420
    private val memoryCache = object : LruCache<TileKey, ImageBitmap>(maxTilesInMemory) {
        override fun sizeOf(key: TileKey, value: ImageBitmap): Int = 1
    }

    private val inFlightRequests = ConcurrentHashMap.newKeySet<TileKey>()
    private val frameTickPending = AtomicBoolean(false)

    private val _tileLoadedTick = MutableStateFlow(0L)
    val tileLoadedTick: StateFlow<Long> = _tileLoadedTick.asStateFlow()

    private val _cachedTileCount = MutableStateFlow(0)
    val cachedTileCount: StateFlow<Int> = _cachedTileCount.asStateFlow()

    private val _cacheGeneration = MutableStateFlow(1)
    val cacheGeneration: StateFlow<Int> = _cacheGeneration.asStateFlow()

    private fun scheduleFrameInvalidation() {
        if (frameTickPending.compareAndSet(false, true)) {
            scope.launch {
                delay(16)
                frameTickPending.set(false)
                _tileLoadedTick.value = System.nanoTime()
            }
        }
    }

    fun getTileOrFetch(layer: GoogleMapLayer, z: Int, x: Int, y: Int): ImageBitmap? {
        val maxTileIndex = (1 shl z)
        if (y < 0 || y >= maxTileIndex) return null
        val wrappedX = ((x % maxTileIndex) + maxTileIndex) % maxTileIndex

        val gen = _cacheGeneration.value
        val key = TileKey(layer.id, z, wrappedX, y, gen)

        synchronized(memoryCache) {
            memoryCache.get(key)?.let { return it }
        }

        if (inFlightRequests.add(key)) {
            scope.launch {
                fetchTileFromGoogleServer(layer, z, wrappedX, y, gen, key)
            }
        }
        return null
    }

    fun getParentFallbackTile(layer: GoogleMapLayer, z: Int, x: Int, y: Int): Pair<ImageBitmap, Int>? {
        if (z <= 2) return null
        val maxTileIndex = (1 shl z)
        val wrappedX = ((x % maxTileIndex) + maxTileIndex) % maxTileIndex
        val gen = _cacheGeneration.value

        for (step in 1..3) {
            val parentZ = z - step
            if (parentZ < 1) break
            val parentX = wrappedX shr step
            val parentY = y shr step
            val parentKey = TileKey(layer.id, parentZ, parentX, parentY, gen)
            val parentBitmap = synchronized(memoryCache) { memoryCache.get(parentKey) }
            if (parentBitmap != null) {
                return Pair(parentBitmap, step)
            }
        }
        return null
    }

    private fun fetchTileFromGoogleServer(
        layer: GoogleMapLayer,
        z: Int,
        x: Int,
        y: Int,
        gen: Int,
        key: TileKey
    ) {
        try {
            val url = layer.buildTileUrl(x = x, y = y, z = z, cacheBuster = gen)
            val request = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; E23WCM Tactical Map) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                )
                .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        val decodeOptions = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                        if (bitmap != null && _cacheGeneration.value == gen) {
                            val imageBitmap = bitmap.asImageBitmap()
                            synchronized(memoryCache) {
                                memoryCache.put(key, imageBitmap)
                                _cachedTileCount.value = memoryCache.size()
                            }
                            scheduleFrameInvalidation()
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Gracefully ignore transient network errors
        } finally {
            inFlightRequests.remove(key)
        }
    }

    fun clearMemoryCache(): Int {
        val evictedCount: Int
        synchronized(memoryCache) {
            evictedCount = memoryCache.size()
            memoryCache.evictAll()
            _cachedTileCount.value = 0
        }
        inFlightRequests.clear()
        _cacheGeneration.value += 1
        _tileLoadedTick.value = System.nanoTime()
        return evictedCount
    }
}
