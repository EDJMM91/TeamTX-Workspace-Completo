package com.example

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Módulo de vinculación 1-a-1 de cuentas de Google con perfiles de pilotos en Firestore.
 * Utiliza transacciones atómicas sobre la colección 'vinculos_google' para impedir colisiones
 * y garantizar que un correo solo pertenezca a un único piloto.
 */
object VINCULACION {

    private const val ETIQUETA_LOG = "VINCULACION_TX"
    private const val COLECCION_VINCULOS = "vinculos_google"

    /**
     * Vincula una cuenta Google de forma atómica con un piloto usando una transacción en Firestore.
     *
     * Reglas:
     * - Si el documento ya existe: verifica que 'uid_firebase' coincida con el solicitante.
     *   Si no coincide, lanza un error bloqueante.
     *   Si coincide, actualiza 'id_dispositivo_activo' y 'fecha_vinculacion_reciente' con FieldValue.serverTimestamp().
     * - Si no existe: crea el registro completo con los datos del piloto y el 'id_dispositivo_activo'.
     */
    suspend fun vincularCuentaGoogle(
        correo: String,
        uidFirebase: String,
        numeroMiembro: String,
        nombrePiloto: String,
        idDispositivo: String
    ): Pair<Boolean, String> {
        val correoSanitizado = correo.trim().lowercase()
        if (correoSanitizado.isBlank() || uidFirebase.isBlank()) {
            return Pair(false, "Correo o identificador de usuario inválido.")
        }

        val baseDatos = FirebaseFirestore.getInstance()
        val referenciaDocumento = baseDatos.collection(COLECCION_VINCULOS).document(correoSanitizado)

        return try {
            baseDatos.runTransaction { transaccion ->
                val instantanea = transaccion.get(referenciaDocumento)

                if (instantanea.exists()) {
                    val uidExistente = instantanea.getString("uid_firebase") ?: ""
                    val pilotoExistente = instantanea.getString("nombre_piloto") ?: instantanea.getString("numero_miembro") ?: "Otro piloto"

                    if (uidExistente.isNotBlank() && uidExistente != uidFirebase) {
                        throw IllegalStateException(
                            "Este correo ya está vinculado al perfil de $pilotoExistente. Para usarlo aquí, primero debes desvincularlo desde ese dispositivo."
                        )
                    }

                    // Si coincide el UID, actualizamos dispositivo activo y fecha reciente
                    val datosActualizacion = mapOf(
                        "id_dispositivo_activo" to idDispositivo,
                        "fecha_vinculacion_reciente" to FieldValue.serverTimestamp(),
                        "numero_miembro" to numeroMiembro,
                        "nombre_piloto" to nombrePiloto
                    )
                    transaccion.update(referenciaDocumento, datosActualizacion)
                    Log.i(ETIQUETA_LOG, "🔄 Vinculación actualizada para $correoSanitizado en dispositivo $idDispositivo")
                } else {
                    // Documento nuevo: creación atómica del vínculo inicial
                    val datosNuevoRegistro = hashMapOf(
                        "correo" to correoSanitizado,
                        "uid_firebase" to uidFirebase,
                        "numero_miembro" to numeroMiembro,
                        "nombre_piloto" to nombrePiloto,
                        "id_dispositivo_activo" to idDispositivo,
                        "fecha_creacion" to FieldValue.serverTimestamp(),
                        "fecha_vinculacion_reciente" to FieldValue.serverTimestamp()
                    )
                    transaccion.set(referenciaDocumento, datosNuevoRegistro)
                    Log.i(ETIQUETA_LOG, "✨ Nueva vinculación 1-a-1 creada para $correoSanitizado con $numeroMiembro")
                }
            }.await()

            Pair(true, "¡Cuenta Google vinculada exitosamente a tu Carnet TX!")
        } catch (error: Exception) {
            val mensajeError = error.message ?: "Error desconocido al procesar la vinculación en Firestore."
            Log.e(ETIQUETA_LOG, "❌ Error en transacción de vinculación: $mensajeError", error)
            Pair(false, mensajeError)
        }
    }

    /**
     * Desvincula y elimina el registro de 'vinculos_google' mediante una transacción atómica.
     * Solo se permite el borrado si el 'uid_firebase' del documento coincide con el solicitante.
     */
    suspend fun desvincularCuentaGoogle(
        correo: String,
        uidFirebase: String
    ): Pair<Boolean, String> {
        val correoSanitizado = correo.trim().lowercase()
        if (correoSanitizado.isBlank() || uidFirebase.isBlank()) {
            return Pair(false, "No hay credenciales activas para desvincular.")
        }

        val baseDatos = FirebaseFirestore.getInstance()
        val referenciaDocumento = baseDatos.collection(COLECCION_VINCULOS).document(correoSanitizado)

        return try {
            baseDatos.runTransaction { transaccion ->
                val instantanea = transaccion.get(referenciaDocumento)

                if (!instantanea.exists()) {
                    // Si el documento ya no existe, consideramos que ya está liberado
                    return@runTransaction
                }

                val uidExistente = instantanea.getString("uid_firebase") ?: ""
                if (uidExistente.isNotBlank() && uidExistente != uidFirebase) {
                    throw IllegalStateException("No tienes autorización para desvincular este correo.")
                }

                // Borrado atómico del vínculo para liberar el correo
                transaccion.delete(referenciaDocumento)
                Log.i(ETIQUETA_LOG, "🗑️ Documento vinculos_google/$correoSanitizado eliminado exitosamente.")
            }.await()

            Pair(true, "✅ Cuenta Google desvinculada exitosamente. El correo ahora está libre.")
        } catch (error: Exception) {
            val mensajeError = error.message ?: "Error desconocido al desvincular en Firestore."
            Log.e(ETIQUETA_LOG, "❌ Error en transacción de desvinculación: $mensajeError", error)
            Pair(false, mensajeError)
        }
    }
}
