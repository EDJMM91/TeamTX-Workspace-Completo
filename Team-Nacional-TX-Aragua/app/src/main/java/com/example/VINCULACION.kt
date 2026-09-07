package com.example

import android.util.Log
import com.example.data.remote.PerfilNube
import com.google.firebase.auth.FirebaseAuth
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
    private const val COLECCION_USUARIOS = "usuarios"

    /**
     * Vincula una cuenta Google de forma atómica con un piloto usando una transacción en Firestore.
     *
     * Reglas:
     * - Si el documento ya existe: verifica si 'numero_miembro' pertenece a otro piloto.
     *   Si pertenece a otro piloto diferente, bloquea la vinculación.
     *   Si pertenece al mismo piloto (mismo número) o el UID coincide, actualiza el registro con
     *   el nuevo UID y el 'id_dispositivo_activo'.
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

        // Asegurar sesión de Firebase Auth activa para evitar PERMISSION_DENIED por reglas de seguridad
        if (FirebaseAuth.getInstance().currentUser == null) {
            try {
                FirebaseAuth.getInstance().signInAnonymously().await()
            } catch (e: Exception) {
                Log.w(ETIQUETA_LOG, "Aviso al verificar Auth previa a vincular: ${e.message}")
            }
        }

        val baseDatos = FirebaseFirestore.getInstance()
        val idDocSanitizado = PerfilNube.sanitizarEmailDocId(correoSanitizado)
        val referenciaDocSanitizado = baseDatos.collection(COLECCION_VINCULOS).document(idDocSanitizado)
        val referenciaDocRaw = baseDatos.collection(COLECCION_VINCULOS).document(correoSanitizado)

        return try {
            baseDatos.runTransaction { transaccion ->
                var instantanea = transaccion.get(referenciaDocSanitizado)
                var esDocSanitizado = true

                if (!instantanea.exists()) {
                    instantanea = transaccion.get(referenciaDocRaw)
                    esDocSanitizado = false
                }

                if (instantanea.exists()) {
                    val uidExistente = instantanea.getString("uid_firebase") ?: ""
                    val docNumero = instantanea.getString("numero_miembro") ?: ""
                    val pilotoExistente = instantanea.getString("nombre_piloto") ?: docNumero.ifBlank { "Otro piloto" }

                    // Bloquear si el correo pertenece a OTRO miembro distinto
                    if (docNumero.isNotBlank() && !docNumero.equals(numeroMiembro, ignoreCase = true)) {
                        throw IllegalStateException(
                            "Este correo ya está vinculado al perfil de $pilotoExistente ($docNumero). Para usarlo aquí, primero debes desvincularlo desde ese perfil."
                        )
                    }

                    // Si pertenece al mismo miembro o no tenía miembro registrado, renovamos vínculo
                    val datosActualizacion = hashMapOf<String, Any>(
                        "correo" to correoSanitizado,
                        "uid_firebase" to uidFirebase,
                        "numero_miembro" to numeroMiembro,
                        "nombre_piloto" to nombrePiloto,
                        "id_dispositivo_activo" to idDispositivo,
                        "fecha_vinculacion_reciente" to FieldValue.serverTimestamp()
                    )

                    // Siempre guardamos en la referencia normalizada
                    transaccion.set(referenciaDocSanitizado, datosActualizacion, com.google.firebase.firestore.SetOptions.merge())

                    // Si existía bajo el id sin sanitizar, lo eliminamos para evitar duplicados
                    if (!esDocSanitizado && idDocSanitizado != correoSanitizado) {
                        transaccion.delete(referenciaDocRaw)
                    }

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
                    transaccion.set(referenciaDocSanitizado, datosNuevoRegistro)
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
     * Permite el borrado si el solicitante es el dueño del perfil, coincide el UID,
     * coincide el número de miembro o si la sesión de Auth en el celular coincide con el correo.
     */
    suspend fun desvincularCuentaGoogle(
        correo: String,
        uidFirebase: String,
        numeroMiembro: String = ""
    ): Pair<Boolean, String> {
        val correoSanitizado = correo.trim().lowercase()
        if (correoSanitizado.isBlank()) {
            return Pair(false, "No hay credenciales activas para desvincular.")
        }

        // Asegurar sesión de Firebase Auth activa para satisfacer reglas de Firestore (request.auth != null)
        if (FirebaseAuth.getInstance().currentUser == null) {
            try {
                FirebaseAuth.getInstance().signInAnonymously().await()
            } catch (e: Exception) {
                Log.w(ETIQUETA_LOG, "Aviso al asegurar sesión anónima previa a desvincular: ${e.message}")
            }
        }

        val baseDatos = FirebaseFirestore.getInstance()
        val idDocSanitizado = PerfilNube.sanitizarEmailDocId(correoSanitizado)
        val referenciaDocSanitizado = baseDatos.collection(COLECCION_VINCULOS).document(idDocSanitizado)
        val referenciaDocRaw = baseDatos.collection(COLECCION_VINCULOS).document(correoSanitizado)

        return try {
            baseDatos.runTransaction { transaccion ->
                val instantaneaSanitizada = transaccion.get(referenciaDocSanitizado)
                val instantaneaRaw = if (idDocSanitizado != correoSanitizado) transaccion.get(referenciaDocRaw) else null

                val existeSanitizado = instantaneaSanitizada.exists()
                val existeRaw = instantaneaRaw?.exists() == true

                if (!existeSanitizado && !existeRaw) {
                    // Si ya no existe, el correo ya está libre
                    Log.i(ETIQUETA_LOG, "ℹ️ El documento vinculos_google no existía. Correo ya libre: $correoSanitizado")
                    return@runTransaction
                }

                val instantaneaActiva = if (existeSanitizado) instantaneaSanitizada else instantaneaRaw!!
                val docNumero = instantaneaActiva.getString("numero_miembro") ?: ""
                val docUid = instantaneaActiva.getString("uid_firebase") ?: ""

                // Validar autorización de desvinculación
                val usuarioAuth = FirebaseAuth.getInstance().currentUser
                val esMismoMiembro = numeroMiembro.isNotBlank() && docNumero.equals(numeroMiembro, ignoreCase = true)
                val esMismoUid = uidFirebase.isNotBlank() && docUid == uidFirebase
                val esMismoEmailAuth = usuarioAuth?.email?.equals(correoSanitizado, ignoreCase = true) == true
                val esDueno = esMismoMiembro || esMismoUid || esMismoEmailAuth || docNumero.isBlank() || docUid.isBlank()

                if (!esDueno) {
                    throw IllegalStateException("No tienes autorización para desvincular este correo ya que pertenece a $docNumero.")
                }

                // Borrar documento normalizado
                if (existeSanitizado) {
                    transaccion.delete(referenciaDocSanitizado)
                }
                // Borrar documento en formato raw si existía
                if (existeRaw) {
                    transaccion.delete(referenciaDocRaw)
                }

                Log.i(ETIQUETA_LOG, "🗑️ Vinculación eliminada para $correoSanitizado. Correo 100% liberado.")
            }.await()

            // Limpieza preventiva de usuarios/{uid} para evitar bloqueos por dispositivo residual
            try {
                if (uidFirebase.isNotBlank()) {
                    baseDatos.collection(COLECCION_USUARIOS).document(uidFirebase).delete().await()
                }
            } catch (_: Exception) {}

            Pair(true, "✅ Cuenta Google desvinculada exitosamente. El correo ahora está libre.")
        } catch (error: Exception) {
            val mensajeError = error.message ?: "Error desconocido al desvincular en Firestore."
            Log.e(ETIQUETA_LOG, "❌ Error en desvinculación: $mensajeError", error)
            Pair(false, mensajeError)
        }
    }
}
