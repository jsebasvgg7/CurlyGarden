package com.curlygarden.app.ui.screens.notifications

enum class NotificationModule(val key: String, val label: String, val color: Long) {
    TALK("talk", "Hablar", 0xFF9BBFE0),
    DRAW("draw", "Dibujar", 0xFFBFB9FD),
    PLAY("play", "Jugar", 0xFFEBC27A),
    IMAGINE("imagine", "Imaginar", 0xFFF4B7A6),
    MEMORIES("memories", "Recuerdos", 0xFFBFB9FD)
}

enum class NotificationTab(val label: String) {
    TURN("Tu turno"),
    NEWS("Novedades")
}

enum class NotificationKind(
    val key: String,
    val module: NotificationModule,
    val tab: NotificationTab,
    val pushByDefault: Boolean,
    val label: String
) {
    TALK_MESSAGE("talk_message", NotificationModule.TALK, NotificationTab.NEWS, true, "Cartas nuevas"),
    DRAW_TOPIC_RECEIVED("draw_topic_received", NotificationModule.DRAW, NotificationTab.TURN, true, "Tema recibido"),
    DRAW_READY_TO_DRAW("draw_ready_to_draw", NotificationModule.DRAW, NotificationTab.TURN, true, "Listos para dibujar"),
    DRAW_REVEALED("draw_revealed", NotificationModule.DRAW, NotificationTab.NEWS, true, "Dibujos revelados"),
    DRAW_NEXT_ROUND("draw_next_round", NotificationModule.DRAW, NotificationTab.TURN, false, "Otra ronda"),
    PLAY_DAILY_ANSWERED("play_daily_answered", NotificationModule.PLAY, NotificationTab.TURN, true, "Reto del día respondido"),
    PLAY_SESSION_NEW("play_session_new", NotificationModule.PLAY, NotificationTab.TURN, true, "Partida nueva"),
    PLAY_GUESS_TURN("play_guess_turn", NotificationModule.PLAY, NotificationTab.TURN, true, "Te toca adivinar"),
    PLAY_RESULT("play_result", NotificationModule.PLAY, NotificationTab.NEWS, true, "Resultado de partida"),
    IMAGINE_CAPSULE_PENDING("imagine_capsule_pending", NotificationModule.IMAGINE, NotificationTab.TURN, true, "Falta tu parte en una cápsula"),
    IMAGINE_CAPSULE_READY("imagine_capsule_ready", NotificationModule.IMAGINE, NotificationTab.NEWS, true, "Cápsula lista para abrir"),
    IMAGINE_JOINED("imagine_joined", NotificationModule.IMAGINE, NotificationTab.NEWS, false, "Se unieron a algo tuyo"),
    MEMORY_NEW("memory_new", NotificationModule.MEMORIES, NotificationTab.NEWS, true, "Recuerdos nuevos");

    companion object {
        fun fromKey(key: String?): NotificationKind? = entries.firstOrNull { it.key == key }

        fun forModule(module: NotificationModule): List<NotificationKind> =
            entries.filter { it.module == module }
    }
}

const val NOTIFICATIONS_INBOX_LIMIT = 50
const val NOTIFICATIONS_FALLBACK_NAME = "El otro"

data class InboxNotification(
    val id: String,
    val kind: NotificationKind,
    val actorId: String,
    val refId: String?,
    val read: Boolean,
    val createdAt: Long
)

data class NotificationSettings(
    val pushEnabled: Boolean = true,
    val prefs: Map<String, Boolean> = emptyMap()
) {
    fun isPushEnabled(kind: NotificationKind): Boolean =
        prefs[kind.key] ?: kind.pushByDefault
}

data class NotificationsProfile(
    val uid: String = "",
    val name: String = "",
    val photoUrl: String = ""
)

data class NotificationsSession(
    val coupleId: String?,
    val mySlot: String?,
    val timezone: String,
    val myUid: String,
    val partnerUid: String?,
    val myProfile: NotificationsProfile,
    val partnerProfile: NotificationsProfile?
)
