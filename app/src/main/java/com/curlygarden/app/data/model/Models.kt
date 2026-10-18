package com.curlygarden.app.data.model

// Modelos alineados 1:1 con las colecciones de Firestore
// definidas en el documento maestro (seccion 4.1)

data class Couple(
    val id: String = "",
    val user1Id: String = "",
    val user2Id: String = "",
    val streakDays: Int = 0,
    val createdAt: Long = 0L
)

data class User(
    val id: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val coupleId: String = "",
    val pushToken: String = ""
)

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val seen: Boolean = false
)

data class DrawingStroke(
    val points: List<Pair<Float, Float>> = emptyList(),
    val color: String = "",
    val thickness: Float = 4f,
    val userId: String = ""
)

data class DailyChallenge(
    val date: String = "",
    val question: String = "",
    val answerUser1: String? = null,
    val answerUser2: String? = null
)

data class Idea(
    val id: String = "",
    val type: String = "idea", // "idea" | "timeCapsule" | "list"
    val title: String = "",
    val content: String = "",
    val createdBy: String = "",
    val unlockDate: Long? = null,
    val createdAt: Long = 0L
)

data class Memory(
    val id: String = "",
    val photoUrl: String = "",
    val caption: String = "",
    val date: Long = 0L,
    val albumId: String? = null,
    val createdBy: String = "",
    val createdAt: Long = 0L
)

data class Album(
    val id: String = "",
    val name: String = "",
    val coverPhotoUrl: String = "",
    val createdAt: Long = 0L
)
