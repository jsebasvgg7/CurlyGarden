package com.curlygarden.app.ui.screens.imagine

import android.net.Uri

const val IMAGINE_TITLE_MAX = 80
const val IMAGINE_IDEA_MAX = 1000
const val IMAGINE_GOAL_CONTENT_MAX = 500
const val IMAGINE_GOAL_DATE_MAX = 60
const val IMAGINE_LIST_MAX_ITEMS = 100
const val IMAGINE_LIST_ITEM_MAX = 200
const val IMAGINE_CAPSULE_TEXT_MAX = 1000
const val IMAGINE_MAX_ATTACHMENT_BYTES = 20L * 1024 * 1024

class AttachmentTooLargeException : Exception("El archivo supera los 20 MB")

enum class ImagineCategory(val collection: String) {
    LISTS("imagineLists"),
    CAPSULES("imagineCapsules"),
    GOALS("imagineGoals"),
    IDEAS("imagineIdeas")
}

enum class ImagineStatus(val value: String) {
    ACTIVE("activa"),
    FINISHED("finalizada");

    companion object {
        fun from(value: String?): ImagineStatus =
            values().firstOrNull { it.value == value } ?: ACTIVE
    }
}

enum class AttachmentType(val value: String) {
    IMAGE("image"),
    FILE("file");

    companion object {
        fun from(value: String?): AttachmentType? =
            values().firstOrNull { it.value == value }
    }
}

enum class CapsuleMode(val value: String) {
    INDIVIDUAL("individual"),
    SHARED("shared");

    companion object {
        fun from(value: String?): CapsuleMode =
            values().firstOrNull { it.value == value } ?: INDIVIDUAL
    }
}

enum class EntryAction { EDIT, JOIN, NONE }

enum class StatusFilter { ALL, ACTIVE, FINISHED }

enum class ShareFilter { ALL, INDIVIDUAL, SHARED }

data class ImagineProfile(
    val uid: String = "",
    val name: String = "",
    val photoUrl: String = ""
)

data class ImagineSession(
    val coupleId: String,
    val myUid: String,
    val partnerUid: String?,
    val myProfile: ImagineProfile,
    val partnerProfile: ImagineProfile?
)

data class ImagineAttachment(
    val uri: Uri,
    val fileName: String,
    val mimeType: String?,
    val sizeBytes: Long
)

interface ImagineItem {
    val id: String
    val title: String
    val createdBy: String
    val likedBy: List<String>
    val createdAt: Long
}

interface ImagineShareable : ImagineItem {
    val sharedWith: List<String>
    val locked: Boolean
}

data class ListItem(
    val text: String,
    val done: Boolean = false
)

data class ImagineIdea(
    override val id: String,
    override val title: String,
    val content: String,
    override val createdBy: String,
    override val sharedWith: List<String>,
    override val locked: Boolean,
    override val likedBy: List<String>,
    val attachmentUrl: String?,
    val attachmentType: AttachmentType?,
    override val createdAt: Long
) : ImagineShareable

data class ImagineList(
    override val id: String,
    override val title: String,
    val items: List<ListItem>,
    override val createdBy: String,
    override val sharedWith: List<String>,
    override val locked: Boolean,
    override val likedBy: List<String>,
    val status: ImagineStatus,
    override val createdAt: Long
) : ImagineShareable

data class ImagineGoal(
    override val id: String,
    override val title: String,
    val content: String,
    val targetDateText: String,
    override val createdBy: String,
    override val sharedWith: List<String>,
    override val locked: Boolean,
    override val likedBy: List<String>,
    val status: ImagineStatus,
    override val createdAt: Long
) : ImagineShareable

data class ImagineCapsule(
    override val id: String,
    override val title: String,
    val mode: CapsuleMode,
    val unlockDate: Long,
    val contributors: List<String>,
    override val createdBy: String,
    override val likedBy: List<String>,
    override val createdAt: Long
) : ImagineItem

data class CapsuleEntry(
    val authorUid: String,
    val text: String,
    val attachmentUrl: String?,
    val attachmentType: AttachmentType?,
    val createdAt: Long
)

data class CapsuleEntries(
    val capsuleId: String,
    val mine: CapsuleEntry?,
    val partner: CapsuleEntry?
)

data class ImagineData(
    val ideas: List<ImagineIdea> = emptyList(),
    val lists: List<ImagineList> = emptyList(),
    val goals: List<ImagineGoal> = emptyList(),
    val capsules: List<ImagineCapsule> = emptyList()
) {
    val total: Int
        get() = ideas.size + lists.size + goals.size + capsules.size

    fun countFor(category: ImagineCategory): Int = when (category) {
        ImagineCategory.LISTS -> lists.size
        ImagineCategory.CAPSULES -> capsules.size
        ImagineCategory.GOALS -> goals.size
        ImagineCategory.IDEAS -> ideas.size
    }
}

fun ImagineShareable.isShared(): Boolean = sharedWith.isNotEmpty()

fun ImagineShareable.canEdit(uid: String): Boolean =
    createdBy == uid || (uid in sharedWith && !locked)

fun ImagineShareable.canJoin(uid: String): Boolean =
    createdBy != uid && sharedWith.isEmpty() && !locked

fun ImagineShareable.actionFor(uid: String): EntryAction = when {
    canEdit(uid) -> EntryAction.EDIT
    canJoin(uid) -> EntryAction.JOIN
    else -> EntryAction.NONE
}

fun ImagineShareable.participants(): List<String> = (listOf(createdBy) + sharedWith).distinct()

fun ImagineItem.isLikedBy(uid: String): Boolean = uid in likedBy

fun ImagineCapsule.isUnlocked(now: Long = System.currentTimeMillis()): Boolean = now >= unlockDate

fun ImagineCapsule.canContribute(uid: String, now: Long = System.currentTimeMillis()): Boolean =
    mode == CapsuleMode.SHARED && now < unlockDate && uid !in contributors

fun ImagineList.isComplete(): Boolean = items.isNotEmpty() && items.all { it.done }

fun List<ImagineList>.filterLists(filter: StatusFilter): List<ImagineList> = when (filter) {
    StatusFilter.ALL -> this
    StatusFilter.ACTIVE -> filter { it.status == ImagineStatus.ACTIVE }
    StatusFilter.FINISHED -> filter { it.status == ImagineStatus.FINISHED }
}

fun List<ImagineGoal>.filterGoals(filter: StatusFilter): List<ImagineGoal> = when (filter) {
    StatusFilter.ALL -> this
    StatusFilter.ACTIVE -> filter { it.status == ImagineStatus.ACTIVE }
    StatusFilter.FINISHED -> filter { it.status == ImagineStatus.FINISHED }
}

fun List<ImagineIdea>.filterIdeas(filter: ShareFilter): List<ImagineIdea> = when (filter) {
    ShareFilter.ALL -> this
    ShareFilter.INDIVIDUAL -> filter { !it.isShared() }
    ShareFilter.SHARED -> filter { it.isShared() }
}

fun List<ImagineCapsule>.filterCapsules(filter: ShareFilter): List<ImagineCapsule> = when (filter) {
    ShareFilter.ALL -> this
    ShareFilter.INDIVIDUAL -> filter { it.mode == CapsuleMode.INDIVIDUAL }
    ShareFilter.SHARED -> filter { it.mode == CapsuleMode.SHARED }
}
