package com.curlygarden.app.couple

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

/**
 * Resultado de pedir un codigo de invitacion nuevo (flujo "invitar" de la
 * seccion 2.4). expiresAt llega como ISO-8601 desde la Cloud Function
 * (ver createInviteCode: expiresAt.toISOString()).
 */
data class InviteCodeResult(
    val code: String,
    val coupleId: String,
    val expiresAtIso: String
)

sealed class CoupleActionResult {
    object Success : CoupleActionResult()
    data class Error(val message: String) : CoupleActionResult()
}

/**
 * Puente hacia las Cloud Functions createInviteCode/redeemInviteCode/
 * unlinkCouple. Toda la logica de negocio (transacciones, validacion de
 * expiracion, evitar condiciones de carrera) vive en el backend con
 * Admin SDK; este repositorio solo invoca y traduce errores a algo que la
 * UI pueda mostrar.
 */
class CoupleRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()
) {

    /**
     * Usuario 1: genera (o reutiliza, ver backend) un codigo de invitacion
     * de 6 caracteres, valido por 48 horas, para compartir por fuera de la
     * app (WhatsApp, SMS, etc., segun la seccion 2.4).
     */
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

    /**
     * Usuario 2: canjea el codigo que recibio para vincularse con la
     * pareja que lo genero. El backend normaliza el codigo (mayusculas,
     * sin espacios), asi que aqui no hace falta duplicar esa logica.
     */
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

    /**
     * Desvincula la pareja actual (ver seccion "Estado de Conexion" del
     * perfil). Solo valido si ya hay un companero vinculado (user2Id no
     * vacio); el backend marca couples/{coupleId} como archived en vez de
     * borrar mensajes/memories/drawings, y limpia coupleId de ambos
     * usuarios para que cualquiera pueda volver a vincularse desde cero.
     */
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

    /**
     * Los mensajes que manda el backend en HttpsError (ver funciones.js)
     * ya estan en espanol y pensados para mostrarse tal cual al usuario,
     * asi que se usan directamente en vez de mapear cada codigo por
     * separado.
     */
    private fun friendlyMessage(e: FirebaseFunctionsException): String {
        return e.message ?: "Ocurrio un error. Intenta de nuevo."
    }
}