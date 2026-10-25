package com.curlygarden.app.ui.screens.memories

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

const val MEMORIES_ALBUM_NAME_MAX = 40
const val MEMORIES_CAPTION_MAX = 140
const val MEMORIES_MAX_PHOTO_BYTES = 10L * 1024 * 1024
const val MEMORIES_COLLAGE_MAX = 4

class PhotoTooLargeException : Exception("La foto supera los 10 MB")

val MemoriesAlbumColors = listOf(
    Color(0xFFF4B7A6),
    Color(0xFF9BBFE0),
    Color(0xFFBFB9FD),
    Color(0xFFEBC27A)
)

fun albumColor(index: Int): Color =
    MemoriesAlbumColors[index.coerceIn(0, MemoriesAlbumColors.lastIndex)]

fun albumEdgeColor(index: Int): Color = lerp(albumColor(index), Color.Black, 0.3f)

data class MemoriesProfile(
    val uid: String = "",
    val name: String = "",
    val photoUrl: String = ""
)

data class MemoriesSession(
    val coupleId: String,
    val myUid: String,
    val partnerUid: String?,
    val myProfile: MemoriesProfile,
    val partnerProfile: MemoriesProfile?
)

class MemoryPhoto(
    val bytes: ByteArray,
    val mimeType: String,
    val aspectRatio: Float
)

data class Memory(
    val id: String,
    val photoUrl: String,
    val caption: String,
    val dateMillis: Long,
    val aspectRatio: Float,
    val albumId: String?,
    val createdBy: String,
    val hidden: Boolean,
    val createdAt: Long
)

data class Album(
    val id: String,
    val name: String,
    val colorIndex: Int,
    val coverPhotoUrl: String?,
    val createdBy: String,
    val createdAt: Long
)

data class MemoriesData(
    val memories: List<Memory> = emptyList(),
    val albums: List<Album> = emptyList()
) {
    fun memoriesFor(albumId: String?, hidden: Boolean): List<Memory> =
        memories.filter { it.hidden == hidden && (albumId == null || it.albumId == albumId) }

    fun countFor(albumId: String?): Int = memoriesFor(albumId, hidden = false).size

    fun hiddenCount(albumId: String?): Int = memoriesFor(albumId, hidden = true).size

    fun collageFor(albumId: String): List<Memory> =
        memoriesFor(albumId, hidden = false).take(MEMORIES_COLLAGE_MAX)

    fun album(id: String): Album? = albums.firstOrNull { it.id == id }

    fun memory(id: String): Memory? = memories.firstOrNull { it.id == id }
}
