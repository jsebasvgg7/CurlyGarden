package com.curlygarden.app.couple

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

data class InviteCodeResult(
    val code: String,
    val coupleId: String,
    val expiresAtIso: String
)

sealed class CoupleActionResult {
    object Success : CoupleActionResult()
    data class Error(val message: String) : CoupleActionResult()
}

class CoupleRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {

    suspend fun createInviteCode(): Result<InviteCodeResult> {
        return try {
            val response = functions.getHttpsCallable("createInviteCode")
                .call()
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = response.data as Map<String, Any?>

            Result.success(
                InviteCodeResult(
                    code = data["code"] as String,
                    coupleId = data["coupleId"] as String,
                    expiresAtIso = data["expiresAt"] as String
                )
            )
        } catch (e: FirebaseFunctionsException) {
            Result.failure(Exception(friendlyMessage(e)))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo generar el codigo. Intenta de nuevo."))
        }
    }

    suspend fun redeemInviteCode(code: String): CoupleActionResult {
        return try {
            functions.getHttpsCallable("redeemInviteCode")
                .call(mapOf("code" to code))
                .await()
            CoupleActionResult.Success
        } catch (e: FirebaseFunctionsException) {
            CoupleActionResult.Error(friendlyMessage(e))
        } catch (e: Exception) {
            CoupleActionResult.Error("No se pudo usar el codigo. Intenta de nuevo.")
        }
    }

    suspend fun unlinkCouple(): CoupleActionResult {
        return try {
            functions.getHttpsCallable("unlinkCouple")
                .call()
                .await()
            CoupleActionResult.Success
        } catch (e: FirebaseFunctionsException) {
            CoupleActionResult.Error(friendlyMessage(e))
        } catch (e: Exception) {
            CoupleActionResult.Error("No se pudo desvincular. Intenta de nuevo.")
        }
    }

    private fun friendlyMessage(e: FirebaseFunctionsException): String {
        return e.message ?: "Ocurrio un error. Intenta de nuevo."
    }
}