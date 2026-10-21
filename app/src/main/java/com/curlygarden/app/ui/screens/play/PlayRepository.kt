package com.curlygarden.app.ui.screens.play

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class PlayRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun resolveSession(): PlaySession? = null

    fun observeDaily(coupleId: String, dateKey: String): Flow<DailyChallenge?> = emptyFlow()

    fun observeSessions(coupleId: String): Flow<List<GameSession>> = emptyFlow()

    fun observeStats(coupleId: String, uid: String): Flow<PlayStats> = emptyFlow()

    suspend fun loadDailyResponses(
        session: PlaySession,
        dateKey: String,
        includePartner: Boolean
    ): DailyResponses = DailyResponses()

    suspend fun submitDaily(
        session: PlaySession,
        dateKey: String,
        type: DailyType,
        bankIndex: Int,
        text: String?
    ) {
    }

    suspend fun createSession(
        session: PlaySession,
        type: PlayGameType,
        questionIds: List<Int>
    ) {
    }

    suspend fun submitAnswers(
        session: PlaySession,
        sessionId: String,
        picks: List<Int>
    ) {
    }

    suspend fun submitGuesses(
        session: PlaySession,
        sessionId: String,
        picks: List<Int>
    ) {
    }

    suspend fun loadReveal(
        session: PlaySession,
        sessionId: String
    ): GameReveal = GameReveal()

    suspend fun awardGuessPoints(
        session: PlaySession,
        sessionId: String,
        points: Int
    ) {
    }
}
