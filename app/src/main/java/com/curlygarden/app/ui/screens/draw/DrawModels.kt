package com.curlygarden.app.ui.screens.draw

enum class DrawRoundStatus {
    LOADING,
    WAITING_TOPICS,
    DRAWING,
    REVEALED
}

data class DrawStroke(
    val points: List<Float> = emptyList(),
    val color: Long = 0xFF7F77DDL,
    val width: Float = 0.02f,
    val eraser: Boolean = false,
    val fill: Boolean = false
)

data class DrawSlots(
    val mine: String,
    val partner: String
)

data class DrawRoundData(
    val exists: Boolean = false,
    val status: DrawRoundStatus = DrawRoundStatus.WAITING_TOPICS,
    val topicISent: String? = null,
    val topicForMe: String? = null,
    val partnerHasSentTopic: Boolean = false,
    val myStrokes: List<DrawStroke> = emptyList(),
    val partnerStrokes: List<DrawStroke> = emptyList(),
    val myReadyToReveal: Boolean = false,
    val partnerReadyToReveal: Boolean = false,
    val myReadyForNext: Boolean = false,
    val partnerReadyForNext: Boolean = false
)

data class DrawHistoryEntry(
    val id: String = "",
    val topicForMe: String? = null,
    val topicISent: String? = null,
    val myStrokes: List<DrawStroke> = emptyList(),
    val partnerStrokes: List<DrawStroke> = emptyList(),
    val savedToMemoriesByMe: Boolean = false,
    val createdAt: Long = 0L
)

object DrawTopics {
    val random = listOf(
        "un gato dormido",
        "una casa en un árbol",
        "un helado derritiéndose",
        "un robot enamorado",
        "una montaña con nieve",
        "un pulpo bailando",
        "una taza de café con vapor",
        "un dinosaurio con lentes",
        "un barco pirata",
        "una flor gigante",
        "un astronauta perdido",
        "un dragón amigable",
        "un pingüino con bufanda",
        "una bicicleta voladora",
        "un caracol con casa de hongo"
    )
}