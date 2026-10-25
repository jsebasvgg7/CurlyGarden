package com.curlygarden.app.ui.screens.draw

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.curlygarden.app.ui.screens.memories.MemoryPhoto
import com.curlygarden.app.ui.theme.CurlyColors
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val RASTER_WIDTH = 900
private const val RASTER_HEIGHT = 1200

internal suspend fun rasterizeDrawing(strokes: List<DrawStroke>): MemoryPhoto =
    withContext(Dispatchers.Default) {
        val image = ImageBitmap(RASTER_WIDTH, RASTER_HEIGHT)
        val canvas = Canvas(image)
        val size = Size(RASTER_WIDTH.toFloat(), RASTER_HEIGHT.toFloat())
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
            drawRect(CurlyColors.DarkSurface)
            drawNormalizedStrokes(strokes)
        }
        val output = ByteArrayOutputStream()
        image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
        MemoryPhoto(
            bytes = output.toByteArray(),
            mimeType = "image/png",
            aspectRatio = RASTER_WIDTH.toFloat() / RASTER_HEIGHT.toFloat()
        )
    }
