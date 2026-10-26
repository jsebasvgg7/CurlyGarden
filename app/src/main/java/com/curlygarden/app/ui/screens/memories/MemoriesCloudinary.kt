package com.curlygarden.app.ui.screens.memories

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val THUMB_WIDTH = 480

internal class CloudinaryUpload(
    val url: String,
    val publicId: String,
    val bytes: Long
)

internal fun memoryThumbUrl(url: String, width: Int = THUMB_WIDTH): String {
    val marker = "/image/upload/"
    val index = url.indexOf(marker)
    if (index < 0 || !url.contains("res.cloudinary.com")) return url
    val cut = index + marker.length
    return url.substring(0, cut) + "w_$width,c_limit,f_auto,q_auto/" + url.substring(cut)
}

internal suspend fun uploadPhotoToCloudinary(
    functions: FirebaseFunctions,
    photo: MemoryPhoto
): CloudinaryUpload {
    val ticket = try {
        functions.getHttpsCallable("signCloudinaryUpload")
            .call(mapOf("bytes" to photo.bytes.size))
            .await()
            .data as Map<*, *>
    } catch (error: FirebaseFunctionsException) {
        if (error.code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED) {
            throw StorageQuotaExceededException()
        }
        throw error
    }

    val cloudName = ticket["cloudName"] as String
    val fields = linkedMapOf(
        "api_key" to ticket["apiKey"].toString(),
        "timestamp" to ticket["timestamp"].toString(),
        "folder" to ticket["folder"].toString(),
        "public_id" to ticket["publicId"].toString(),
        "signature" to ticket["signature"].toString()
    )

    return withContext(Dispatchers.IO) {
        val boundary = "----curly${UUID.randomUUID()}"
        val connection = URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setChunkedStreamingMode(0)

            DataOutputStream(connection.outputStream).use { out ->
                fields.forEach { (name, value) ->
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    out.write(value.toByteArray(Charsets.UTF_8))
                    out.writeBytes("\r\n")
                }
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"photo\"\r\n")
                out.writeBytes("Content-Type: ${photo.mimeType}\r\n\r\n")
                out.write(photo.bytes)
                out.writeBytes("\r\n--$boundary--\r\n")
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("Cloudinary $code: $detail")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            CloudinaryUpload(
                url = json.getString("secure_url"),
                publicId = json.getString("public_id"),
                bytes = json.optLong("bytes", photo.bytes.size.toLong())
            )
        } finally {
            connection.disconnect()
        }
    }
}
