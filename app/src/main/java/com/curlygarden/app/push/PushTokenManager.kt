package com.curlygarden.app.push

import com.curlygarden.app.ui.screens.notifications.NotificationsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val UNREGISTER_TIMEOUT_MS = 3000L

class PushTokenManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
    private val repository: NotificationsRepository = NotificationsRepository()
) {

    suspend fun register() {
        if (auth.currentUser == null) return
        repository.addPushToken(messaging.token.await())
    }

    suspend fun unregister() {
        withTimeoutOrNull(UNREGISTER_TIMEOUT_MS) {
            val token = attempt { messaging.token.await() } ?: return@withTimeoutOrNull
            attempt { repository.removePushToken(token) }
            attempt { messaging.deleteToken().await() }
        }
    }

    private suspend fun <T> attempt(block: suspend () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
}
