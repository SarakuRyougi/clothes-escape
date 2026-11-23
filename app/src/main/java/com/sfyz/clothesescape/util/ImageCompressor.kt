package com.sfyz.clothesescape.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageCompressor {

    private const val MAX_EDGE = 1600
    private const val QUALITY = 85

    fun photoDir(context: Context): File = File(context.filesDir, "photos").apply { mkdirs() }

    fun newPhotoFile(context: Context): File =
        File(photoDir(context), "p_${System.currentTimeMillis()}.jpg")

    /** Compresses content:// or file:// into an app-private JPEG. Returns the absolute path. */
    fun compress(context: Context, source: Uri, target: File): String? {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            }
            val decoded = resolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            val scaled = scaleDown(decoded)
            target.parentFile?.mkdirs()
            FileOutputStream(target).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            }
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
            target.absolutePath
        } catch (t: Throwable) {
            null
        }
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= MAX_EDGE && h / 2 >= MAX_EDGE) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }

    private fun scaleDown(src: Bitmap): Bitmap {
        val maxSide = maxOf(src.width, src.height)
        if (maxSide <= MAX_EDGE) return src
        val ratio = MAX_EDGE.toFloat() / maxSide
        return Bitmap.createScaledBitmap(
            src,
            (src.width * ratio).toInt().coerceAtLeast(1),
            (src.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    @Suppress("unused")
    private fun rotate(src: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return src
        val m = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }
}
