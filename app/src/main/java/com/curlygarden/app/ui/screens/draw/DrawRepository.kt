package com.curlygarden.app.ui.screens.draw

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.curlygarden.app.data.streak.DEFAULT_TIMEZONE
import com.curlygarden.app.data.streak.todayKeyFor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

class DrawRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun randomTopic(): String = DrawTopics.random.random()

    suspend fun resolveSlots(coupleId: String): DrawSlots {
        val uid = auth.currentUser?.uid
        val coupleDoc = firestore.collection("couples").document(coupleId).get().await()
        val user1Id = coupleDoc.getString("user1Id")
        return if (uid != null && uid == user1Id) {
            DrawSlots(mine = "user1", partner = "user2")
        } else {
            DrawSlots(mine = "user2", partner = "user1")
        }
    }

    private fun roundRef(coupleId: String): DocumentReference =
        firestore.collection("couples").document(coupleId)
            .collection("drawRound").document("current")

    private fun canvasRef(coupleId: String, slot: String): DocumentReference =
        roundRef(coupleId).collection("canvases").document(slot)

    fun observeRound(coupleId: String, slots: DrawSlots): Flow<DrawRoundData> =
        combine(
            documentFlow(roundRef(coupleId)),
            documentFlow(canvasRef(coupleId, slots.mine)),
            documentFlow(canvasRef(coupleId, slots.partner))
        ) { round, myCanvas, partnerCanvas ->
            buildRoundData(round, myCanvas, partnerCanvas, slots)
        }

    private fun documentFlow(ref: DocumentReference): Flow<DocumentSnapshot?> = callbackFlow {
        val listener = ref.addSnapshotListener { snapshot, _ -> trySend(snapshot) }
        awaitClose { listener.remove() }
    }

    private fun buildRoundData(
        round: DocumentSnapshot?,
        myCanvas: DocumentSnapshot?,
        partnerCanvas: DocumentSnapshot?,
        slots: DrawSlots
    ): DrawRoundData {
        if (round == null || !round.exists()) return DrawRoundData(exists = false)

        val status = when (round.getString("status")) {
            "drawing" -> DrawRoundStatus.DRAWING
            "revealed" -> DrawRoundStatus.REVEALED
            else -> DrawRoundStatus.WAITING_TOPICS
        }

        val topics = round.get("topics") as? Map<*, *>
        val ready = round.get("readyToReveal") as? Map<*, *>
        val next = round.get("readyForNext") as? Map<*, *>
        val bothTopicsSent = topics?.get("user1") != null && topics?.get("user2") != null

        return DrawRoundData(
            exists = true,
            status = status,
            topicISent = topics?.get(slots.partner) as? String,
            topicForMe = if (bothTopicsSent) topics?.get(slots.mine) as? String else null,
            partnerHasSentTopic = topics?.get(slots.mine) != null,
            myStrokes = parseStrokes(myCanvas?.get("strokes")),
            partnerStrokes = if (status == DrawRoundStatus.REVEALED) {
                parseStrokes(partnerCanvas?.get("strokes"))
            } else {
                emptyList()
            },
            myReadyToReveal = (ready?.get(slots.mine) as? Boolean) ?: false,
            partnerReadyToReveal = (ready?.get(slots.partner) as? Boolean) ?: false,
            myReadyForNext = (next?.get(slots.mine) as? Boolean) ?: false,
            partnerReadyForNext = (next?.get(slots.partner) as? Boolean) ?: false
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseStrokes(raw: Any?): List<DrawStroke> {
        val list = raw as? List<Map<String, Any?>> ?: return emptyList()
        return list.map { entry ->
            DrawStroke(
                points = (entry["points"] as? List<*>)
                    ?.mapNotNull { (it as? Number)?.toFloat() }
                    ?: emptyList(),
                color = (entry["color"] as? Number)?.toLong() ?: 0xFF7F77DDL,
                width = (entry["width"] as? Number)?.toFloat() ?: 0.02f
            )
        }
    }

    private fun strokesToMap(strokes: List<DrawStroke>): List<Map<String, Any>> =
        strokes.map {
            mapOf(
                "points" to it.points,
                "color" to it.color,
                "width" to it.width
            )
        }

    suspend fun sendTopic(coupleId: String, slots: DrawSlots, topicForPartner: String) {
        val ref = roundRef(coupleId)
        val trimmed = topicForPartner.trim()
        if (trimmed.isEmpty()) return

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val existingTopics = (snapshot.get("topics") as? Map<*, *>)
                ?.entries
                ?.mapNotNull { entry ->
                    val key = entry.key as? String
                    val value = entry.value as? String
                    if (key != null && value != null) key to value else null
                }
                ?.toMap()
                .orEmpty()

            val alreadyDrawing = snapshot.getString("status") == "drawing" ||
                    snapshot.getString("status") == "revealed"
            if (alreadyDrawing) return@runTransaction null

            val newTopics = existingTopics + (slots.partner to trimmed)
            val bothSent = newTopics["user1"] != null && newTopics["user2"] != null
            val newStatus = if (bothSent) "drawing" else "waiting_topics"

            if (!snapshot.exists()) {
                transaction.set(
                    ref,
                    mapOf(
                        "status" to newStatus,
                        "topics" to newTopics,
                        "readyToReveal" to emptyMap<String, Boolean>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                transaction.set(
                    ref,
                    mapOf(
                        "status" to newStatus,
                        "topics" to newTopics,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }
            null
        }.await()
    }

    suspend fun saveMyStrokes(coupleId: String, slots: DrawSlots, strokes: List<DrawStroke>) {
        canvasRef(coupleId, slots.mine).set(
            mapOf(
                "strokes" to strokesToMap(strokes),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun markReadyToReveal(coupleId: String, slots: DrawSlots) {
        val ref = roundRef(coupleId)
        val coupleRef = firestore.collection("couples").document(coupleId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            if (!snapshot.exists()) return@runTransaction null

            // Leida junto con el resto (antes de cualquier escritura de esta
            // transaccion) solo si hace falta: se usa unicamente cuando la
            // ronda termina en "revealed", pero Firestore exige que todas
            // las lecturas de la transaccion ocurran antes que las escrituras,
            // asi que se lee siempre, se use o no.
            val coupleSnapshot = transaction.get(coupleRef)

            val existingReady = (snapshot.get("readyToReveal") as? Map<*, *>)
                ?.entries
                ?.mapNotNull { entry ->
                    val key = entry.key as? String
                    val value = entry.value as? Boolean
                    if (key != null && value != null) key to value else null
                }
                ?.toMap()
                .orEmpty()

            val newReady = existingReady + (slots.mine to true)
            val bothReady = newReady["user1"] == true && newReady["user2"] == true

            transaction.set(
                ref,
                mapOf(
                    "readyToReveal" to newReady,
                    "status" to if (bothReady) "revealed" else "drawing",
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )

            // Racha: Draw es una actividad bilateral (ver documento maestro,
            // seccion Racha), asi que el dia solo cuenta cuando ambos
            // terminaron, es decir, exactamente cuando bothReady pasa a true.
            if (bothReady) {
                val timezone = coupleSnapshot.getString("timezone") ?: DEFAULT_TIMEZONE
                val todayKey = todayKeyFor(timezone)
                // set+merge (no update) porque streakLog puede no existir
                // todavia como campo en couples/{coupleId} (parejas que
                // nunca completaron un Draw); update con dot-notation sobre
                // un campo mapa inexistente falla en Firestore.
                transaction.set(
                    coupleRef,
                    mapOf("streakLog" to mapOf(todayKey to true)),
                    SetOptions.merge()
                )
            }
            null
        }.await()
    }

    suspend fun confirmNextRound(coupleId: String, slots: DrawSlots) {
        val ref = roundRef(coupleId)
        val couple = firestore.collection("couples").document(coupleId)
        val historyRef = couple.collection("drawHistory").document()
        val myCanvas = canvasRef(coupleId, slots.mine)
        val partnerCanvas = canvasRef(coupleId, slots.partner)

        firestore.runTransaction { transaction ->
            val round = transaction.get(ref)
            if (!round.exists()) return@runTransaction null
            if (round.getString("status") != "revealed") return@runTransaction null

            val existing = (round.get("readyForNext") as? Map<*, *>)
                ?.entries
                ?.mapNotNull { entry ->
                    val key = entry.key as? String
                    val value = entry.value as? Boolean
                    if (key != null && value != null) key to value else null
                }
                ?.toMap()
                .orEmpty()

            val updated = existing + (slots.mine to true)
            val bothReady = updated["user1"] == true && updated["user2"] == true

            if (!bothReady) {
                transaction.set(
                    ref,
                    mapOf(
                        "readyForNext" to updated,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                return@runTransaction null
            }

            val topics = round.get("topics") as? Map<*, *>
            val mineSnap = transaction.get(myCanvas)
            val partnerSnap = transaction.get(partnerCanvas)
            val user1Strokes = if (slots.mine == "user1") mineSnap.get("strokes") else partnerSnap.get("strokes")
            val user2Strokes = if (slots.mine == "user2") mineSnap.get("strokes") else partnerSnap.get("strokes")

            transaction.set(
                historyRef,
                mapOf(
                    "user1Topic" to topics?.get("user1"),
                    "user2Topic" to topics?.get("user2"),
                    "user1Strokes" to (user1Strokes ?: emptyList<Any>()),
                    "user2Strokes" to (user2Strokes ?: emptyList<Any>()),
                    "savedToMemories" to emptyMap<String, Boolean>(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            transaction.delete(myCanvas)
            transaction.delete(partnerCanvas)
            transaction.delete(ref)
            null
        }.await()
    }

    fun observeHistory(coupleId: String, slots: DrawSlots): Flow<List<DrawHistoryEntry>> =
        callbackFlow {
            val listener = firestore.collection("couples").document(coupleId)
                .collection("drawHistory")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    val entries = snapshot?.documents?.map { doc ->
                        DrawHistoryEntry(
                            id = doc.id,
                            topicForMe = doc.getString("${slots.mine}Topic"),
                            topicISent = doc.getString("${slots.partner}Topic"),
                            myStrokes = parseStrokes(doc.get("${slots.mine}Strokes")),
                            partnerStrokes = parseStrokes(doc.get("${slots.partner}Strokes")),
                            savedToMemoriesByMe = (doc.get("savedToMemories") as? Map<*, *>)
                                ?.get(slots.mine) as? Boolean ?: false,
                            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                        )
                    } ?: emptyList()
                    trySend(entries)
                }
            awaitClose { listener.remove() }
        }

    suspend fun setSavedToMemories(
        coupleId: String,
        slots: DrawSlots,
        entryId: String,
        saved: Boolean
    ) {
        firestore.collection("couples").document(coupleId)
            .collection("drawHistory").document(entryId)
            .set(
                mapOf("savedToMemories" to mapOf(slots.mine to saved)),
                SetOptions.merge()
            )
            .await()
    }
}