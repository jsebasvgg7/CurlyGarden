package com.curlygarden.app.ui.screens.play

import com.curlygarden.app.data.streak.DEFAULT_TIMEZONE
import com.curlygarden.app.data.streak.dateKeyFor
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.ceil

const val POINTS_DAILY = 10
const val POINTS_THIS_OR_THAT = 10
const val POINTS_GUESS = 5
const val POINTS_ELEVEN = 5
const val POINTS_PER_LEVEL = 100
const val ELEVEN_HOUR = 23
const val ELEVEN_MINUTE = 11
const val ELEVEN_WINDOW_SECONDS = 60
const val PLAY_PICKS_PER_GAME = 5
const val PLAY_ANSWER_MAX = 140

enum class DailyType(val value: String) {
    QUESTION("question"),
    ACTION("action");

    companion object {
        fun from(value: String?): DailyType =
            values().firstOrNull { it.value == value } ?: QUESTION
    }
}

enum class PlayGameType(val value: String) {
    THIS_OR_THAT("thisOrThat"),
    KNOW_ME("knowMe");

    companion object {
        fun from(value: String?): PlayGameType =
            values().firstOrNull { it.value == value } ?: THIS_OR_THAT
    }
}

enum class SessionStatus(val value: String) {
    ANSWERING("answering"),
    GUESSING("guessing"),
    REVEALED("revealed");

    companion object {
        fun from(value: String?): SessionStatus =
            values().firstOrNull { it.value == value } ?: ANSWERING
    }
}

enum class DailyCardState {
    PENDING,
    WAITING,
    COMPLETED
}

enum class GameCardState {
    NEW_GAME,
    WAITING,
    YOUR_TURN_TO_GUESS,
    SEE_RESULT
}

data class PlayProfile(
    val uid: String,
    val name: String,
    val photoUrl: String
)

data class PlaySession(
    val coupleId: String,
    val myUid: String,
    val partnerUid: String?,
    val timezone: String,
    val myProfile: PlayProfile,
    val partnerProfile: PlayProfile?
)

data class DailyChallenge(
    val dateKey: String,
    val type: DailyType,
    val bankIndex: Int,
    val isCompleted: Boolean,
    val answeredBy: List<String>
)

data class DailyResponse(
    val authorUid: String,
    val text: String?
)

data class DailyResponses(
    val mine: DailyResponse? = null,
    val partner: DailyResponse? = null
)

data class GameSession(
    val id: String,
    val type: PlayGameType,
    val questionIds: List<Int>,
    val status: SessionStatus,
    val answeredBy: List<String>,
    val guessedBy: List<String>,
    val scored: List<String>,
    val createdBy: String,
    val createdAtMillis: Long
)

data class GameReveal(
    val answers: Map<String, List<Int>> = emptyMap(),
    val guesses: Map<String, List<Int>> = emptyMap()
)

data class PlayStats(
    val points: Int = 0
) {
    val level: Int
        get() = points / POINTS_PER_LEVEL + 1

    val progress: Int
        get() = points % POINTS_PER_LEVEL
}

enum class ElevenPhase {
    BEFORE,
    OPEN,
    PRESSED,
    MISSED
}

data class ElevenStats(
    val count: Int = 0,
    val lastDay: String = "",
    val pressedAtMillis: Long = 0L
)

data class ElevenClock(
    val phase: ElevenPhase = ElevenPhase.BEFORE,
    val secondsLeft: Int = 0,
    val progress: Float = 0f
) {
    val isCountingDown: Boolean
        get() = phase == ElevenPhase.OPEN ||
            (phase == ElevenPhase.BEFORE && secondsLeft in 1..ELEVEN_WINDOW_SECONDS)
}

data class ElevenPodiumEntry(
    val profile: PlayProfile,
    val count: Int,
    val lastPressedMillis: Long
)

private fun elevenZone(timezone: String): ZoneId =
    runCatching { ZoneId.of(timezone) }.getOrElse { ZoneId.of(DEFAULT_TIMEZONE) }

fun computeElevenClock(timezone: String, lastDay: String?): ElevenClock {
    val now = ZonedDateTime.now(elevenZone(timezone))
    val today = now.toLocalDate()
    if (lastDay == dateKeyFor(today)) return ElevenClock(ElevenPhase.PRESSED)

    val start = today.atTime(ELEVEN_HOUR, ELEVEN_MINUTE).atZone(now.zone)
    val nowMillis = now.toInstant().toEpochMilli()
    val startMillis = start.toInstant().toEpochMilli()
    val endMillis = startMillis + ELEVEN_WINDOW_SECONDS * 1000L

    return when {
        nowMillis < startMillis -> {
            val seconds = ceil((startMillis - nowMillis) / 1000.0).toInt()
            val progress = if (seconds <= ELEVEN_WINDOW_SECONDS) {
                seconds / ELEVEN_WINDOW_SECONDS.toFloat()
            } else {
                0f
            }
            ElevenClock(ElevenPhase.BEFORE, seconds, progress)
        }
        nowMillis < endMillis -> {
            val remaining = endMillis - nowMillis
            ElevenClock(
                phase = ElevenPhase.OPEN,
                secondsLeft = ceil(remaining / 1000.0).toInt().coerceIn(0, ELEVEN_WINDOW_SECONDS),
                progress = remaining / (ELEVEN_WINDOW_SECONDS * 1000f)
            )
        }
        else -> ElevenClock(ElevenPhase.MISSED)
    }
}
