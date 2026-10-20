package com.curlygarden.app.ui.screens.imagine

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun ImagineAttachment.type(): AttachmentType =
    if (mimeType?.startsWith("image/") == true) AttachmentType.IMAGE else AttachmentType.FILE

fun Context.readImagineAttachment(uri: Uri): ImagineAttachment? {
    var name: String? = null
    var size = -1L

    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex)
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }

    if (size < 0) {
        size = contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
    }
    if (size < 0) return null

    return ImagineAttachment(
        uri = uri,
        fileName = name?.takeIf { it.isNotBlank() } ?: "archivo",
        mimeType = contentResolver.getType(uri),
        sizeBytes = size
    )
}
