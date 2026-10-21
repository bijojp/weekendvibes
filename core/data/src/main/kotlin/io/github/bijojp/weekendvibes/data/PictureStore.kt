package io.github.bijojp.weekendvibes.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.max

/**
 * Picture files in the app's private storage, as in ThingSpot's PhotoStore. Imported pictures are
 * made upright, scaled to at most [MAX_EDGE] px and re-encoded as JPEG, which also drops all
 * metadata (no GPS location).
 */
class PictureStore(context: Context) {
    private val appContext = context.applicationContext
    internal val directory = File(appContext.filesDir, "pictures")
    private val downloads = File(appContext.cacheDir, "downloads")

    fun file(name: String): File = File(directory, name)

    /** Copies the image at [source] (the photo picker's result) into private storage and returns its file name. */
    suspend fun import(source: Uri): String = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Not an image" }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight) }
        val decoded = resolver.openInputStream(source).use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Couldn't decode image")
        val rotation = runCatching {
            resolver.openInputStream(source)?.use { ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)

        val upright = scaleAndRotate(decoded, rotation)
        val name = "${UUID.randomUUID()}.jpg"
        directory.mkdirs()
        file(name).outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        if (upright !== decoded) upright.recycle()
        decoded.recycle()
        name
    }

    /** Stores a downloaded image (an event page's picture) the same way. */
    suspend fun import(bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val temp = File(downloads.apply { mkdirs() }, UUID.randomUUID().toString())
        try {
            temp.writeBytes(bytes)
            import(Uri.fromFile(temp))
        } finally {
            temp.delete()
        }
    }

    /**
     * Deletes picture files that nothing refers to. Recent files are left alone, because an
     * editor may hold a picture the user hasn't saved yet.
     */
    suspend fun sweep(referenced: Set<String>, graceMillis: Long = 60 * 60 * 1000L) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - graceMillis
        directory.listFiles()?.forEach { file ->
            if (file.name !in referenced && file.lastModified() < cutoff) file.delete()
        }
        downloads.listFiles()?.forEach { file -> if (file.lastModified() < cutoff) file.delete() }
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= MAX_EDGE) sample *= 2
        return sample
    }

    private fun scaleAndRotate(bitmap: Bitmap, rotation: Int): Bitmap {
        val scale = MAX_EDGE.toFloat() / max(bitmap.width, bitmap.height)
        if (scale >= 1f && rotation == 0) return bitmap
        val matrix = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            if (rotation != 0) postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private companion object {
        const val MAX_EDGE = 1080
        const val JPEG_QUALITY = 85
    }
}
