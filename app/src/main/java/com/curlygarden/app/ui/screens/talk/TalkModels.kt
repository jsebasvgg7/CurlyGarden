package com.curlygarden.app.ui.screens.talk

const val TALK_MAX_CHARS = 140
const val TALK_PAGE_SIZE = 30L

data class TalkProfile(
    val uid: String = "",
    val name: String = "",
    val photoUrl: String = ""
)

data class TalkMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val seen: Boolean = false,
    val replyTo: String? = null,
    val replyPreview: String? = null
)

data class TalkSession(
    val coupleId: String,
    val myUid: String,
    val partnerUid: String?,
    val myProfile: TalkProfile,
    val partnerProfile: TalkProfile?
)

sealed interface TalkFeed {
    data object Loading : TalkFeed
    data class Error(val message: String) : TalkFeed
    data class Ready(val messages: List<TalkMessage>, val hasMore: Boolean) : TalkFeed
}
