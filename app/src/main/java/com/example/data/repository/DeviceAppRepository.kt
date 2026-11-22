package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.data.local.LibraryDao
import com.example.data.local.LibraryEntry
import com.example.data.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class DeviceAppRepository(
    context: Context,
    private val libraryDao: LibraryDao
) {
    private data class CachedIconVisual(
        val bitmap: ImageBitmap,
        val accentColorArgb: Long?
    )

    private val appContext = context.applicationContext
    private val iconCache = ConcurrentHashMap<String, CachedIconVisual>()

    val libraryEntries: Flow<List<LibraryEntry>> = libraryDao.observeAllEntries()

    suspend fun scanLaunchableApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = appContext.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        val seenPackages = HashSet<String>()
        val apps = ArrayList<InstalledAppInfo>(resolved.size)

        for (resolveInfo in resolved) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val packageName = activityInfo.packageName ?: continue
            if (packageName == appContext.packageName || !seenPackages.add(packageName)) {
                continue
            }

            val appInfo = activityInfo.applicationInfo
            val isGame = isApplicationGame(appInfo)
            val label = resolveInfo.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() }
                ?: pm.getApplicationLabel(appInfo).toString()

            val cachedVisual = iconCache[packageName] ?: loadIconVisualSafely(pm, packageName)?.also {
                iconCache[packageName] = it
            }

            apps.add(
                InstalledAppInfo(
                    packageName = packageName,
                    name = label,
                    isDetectedGame = isGame,
                    iconBitmap = cachedVisual?.bitmap,
                    accentColorArgb = cachedVisual?.accentColorArgb
                )
            )
        }

        apps.sortedWith(
            compareByDescending<InstalledAppInfo> { it.isDetectedGame }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        )
    }

    suspend fun togglePin(packageName: String, pinned: Boolean) {
        val existing = libraryDao.getEntry(packageName)
        val updated = (existing ?: LibraryEntry(packageName = packageName)).copy(
            isPinned = pinned,
            isHidden = if (pinned) false else (existing?.isHidden ?: false)
        )
        libraryDao.upsert(updated)
    }

    suspend fun toggleFavorite(packageName: String, favorite: Boolean) {
        val existing = libraryDao.getEntry(packageName)
        val updated = (existing ?: LibraryEntry(packageName = packageName)).copy(
            isFavorite = favorite,
            isHidden = if (favorite) false else (existing?.isHidden ?: false)
        )
        libraryDao.upsert(updated)
    }

    suspend fun setHidden(packageName: String, hidden: Boolean) {
        val existing = libraryDao.getEntry(packageName)
        val updated = (existing ?: LibraryEntry(packageName = packageName)).copy(
            isHidden = hidden,
            isPinned = if (hidden) false else (existing?.isPinned ?: false),
            isFavorite = if (hidden) false else (existing?.isFavorite ?: false)
        )
        libraryDao.upsert(updated)
    }

    suspend fun launchPackage(packageName: String): Boolean {
        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            appContext.startActivity(launchIntent)
            withContext(Dispatchers.IO) {
                val existing = libraryDao.getEntry(packageName)
                val updated = (existing ?: LibraryEntry(packageName = packageName)).copy(
                    lastLaunchedAt = System.currentTimeMillis(),
                    launchCount = (existing?.launchCount ?: 0) + 1
                )
                libraryDao.upsert(updated)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun clearTransientCaches(): Long {
        iconCache.clear()
        return deleteDirectoryContents(appContext.cacheDir)
    }

    private fun deleteDirectoryContents(dir: java.io.File?): Long {
        if (dir == null || !dir.exists() || !dir.isDirectory) return 0L
        var reclaimedBytes = 0L
        val children = dir.listFiles() ?: return 0L
        for (child in children) {
            reclaimedBytes += if (child.isDirectory) {
                val nested = deleteDirectoryContents(child)
                child.delete()
                nested
            } else {
                val size = child.length().coerceAtLeast(0L)
                if (child.delete()) size else 0L
            }
        }
        return reclaimedBytes
    }

    @Suppress("DEPRECATION")
    private fun isApplicationGame(appInfo: ApplicationInfo): Boolean {
        val hasGameFlag = (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
        val hasGameCategory = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            appInfo.category == ApplicationInfo.CATEGORY_GAME
        return hasGameFlag || hasGameCategory
    }

    private fun loadIconVisualSafely(pm: PackageManager, packageName: String): CachedIconVisual? {
        return try {
            val drawable = pm.getApplicationIcon(packageName)
            val bitmap = drawable.toBitmap(width = 144, height = 144)
            val accent = extractVibrantAccentColor(bitmap)
            CachedIconVisual(
                bitmap = bitmap.asImageBitmap(),
                accentColorArgb = accent
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun extractVibrantAccentColor(bitmap: Bitmap): Long? {
        val bucketCount = 12
        val bucketScores = FloatArray(bucketCount)
        val bucketHueSum = FloatArray(bucketCount)
        val bucketCounts = IntArray(bucketCount)
        val hsv = FloatArray(3)

        val step = 4
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = AndroidColor.alpha(pixel)
                if (alpha < 140) continue

                AndroidColor.colorToHSV(pixel, hsv)
                val sat = hsv[1]
                val value = hsv[2]
                if (sat < 0.22f || value < 0.22f) continue

                val bucket = ((hsv[0] / 30f).toInt()).coerceIn(0, bucketCount - 1)
                val weight = sat * value
                bucketScores[bucket] += weight
                bucketHueSum[bucket] += hsv[0]
                bucketCounts[bucket]++
            }
        }

        var bestBucket = -1
        var bestScore = 0f
        for (i in 0 until bucketCount) {
            if (bucketCounts[i] > 0 && bucketScores[i] > bestScore) {
                bestScore = bucketScores[i]
                bestBucket = i
            }
        }
        if (bestBucket < 0) return null

        val avgHue = (bucketHueSum[bestBucket] / bucketCounts[bestBucket]).coerceIn(0f, 360f)
        val neonColor = AndroidColor.HSVToColor(
            255,
            floatArrayOf(avgHue, 0.85f, 0.98f)
        )
        return neonColor.toLong() and 0xFFFFFFFFL
    }
}
