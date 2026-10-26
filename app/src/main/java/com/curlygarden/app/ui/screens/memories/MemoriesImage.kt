package com.curlygarden.app.ui.screens.memories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PHOTO_MAX_SIDE = 1280
private const val PHOTO_QUALITY = 80

internal suspend fun readMemoryPhoto(context: Context, uri: Uri): MemoryPhoto =
    withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Imagen no válida")

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= PHOTO_MAX_SIDE) sample *= 2

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
        val source = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: throw IOException("Imagen no válida")

        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix().apply { applyOrientation(orientation) }
        val scale = min(1f, PHOTO_MAX_SIDE.toFloat() / max(source.width, source.height))
        if (scale < 1f) matrix.postScale(scale, scale)

        val result = if (matrix.isIdentity) {
            source
        } else {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }
        if (result !== source) source.recycle()

        val output = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, output)
        val photo = MemoryPhoto(
            bytes = output.toByteArray(),
            mimeType = "image/jpeg",
            aspectRatio = result.width.toFloat() / result.height.toFloat()
        )
        result.recycle()
        photo
    }

private fun Matrix.applyOrientation(orientation: Int) {
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            postRotate(90f)
            postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            postRotate(270f)
            postScale(-1f, 1f)
        }
    }
}
