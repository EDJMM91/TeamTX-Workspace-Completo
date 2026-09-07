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
    /**
     * Vincula una cuenta Google de forma atómica con un piloto usando una transacción en Firestore,
     * asociando estrictamente el código de acceso como contraseña única del correo.
     */
    suspend fun vincularCuentaGoogle(
        correo: String,
        uidFirebase: String,
        numeroMiembro: String,
        nombrePiloto: String,
        idDispositivo: String,
        codigoAcceso: String = ""
    ): Pair<Boolean, String> {
        val correoSanitizado = correo.trim().lowercase()
        val codigoLimpio = codigoAcceso.trim().uppercase()
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
                    val codigoRegistrado = instantanea.getString("codigo_acceso") ?: instantanea.getString("accessCode") ?: ""

                    // 🛡️ BYPASS PARA DESARROLLADORES: Permitir reclamar el correo si es uno de los correos maestros
                    val esCorreoDesarrollador = correoSanitizado == "eduardo.androide.em@gmail.com" || 
                                               correoSanitizado == "eduardo.jose.marquez.matos@gmail.com"
                    
                    val esMismoMiembro = docNumero.equals(numeroMiembro, ignoreCase = true)

                    // Bloquear si el correo pertenece a OTRO miembro distinto (y no es bypass de Dev)
                    if (!esMismoMiembro && docNumero.isNotBlank() && !esCorreoDesarrollador) {
                        throw IllegalStateException(
                            "Este correo ya está vinculado al perfil de $pilotoExistente ($docNumero). Para usarlo aquí, primero debes desvincularlo desde ese perfil."
                        )
                    }

                    val codigoFinal = if (codigoLimpio.isNotBlank()) codigoLimpio else codigoRegistrado

                    // Si pertenece al mismo miembro, es correo dev o no tenía miembro registrado, renovamos vínculo
                    val datosActualizacion = hashMapOf<String, Any>(
                        "correo" to correoSanitizado,
                        "email" to correoSanitizado,
                        "uid_firebase" to uidFirebase,
                        "firebaseUid" to uidFirebase,
                        "numero_miembro" to numeroMiembro,
                        "memberNumber" to numeroMiembro,
                        "nombre_piloto" to nombrePiloto,
                        "memberName" to nombrePiloto,
                        "codigo_acceso" to codigoFinal,
                        "accessCode" to codigoFinal,
                        "id_dispositivo_activo" to idDispositivo,
                        "activeDeviceId" to idDispositivo,
                        "fecha_vinculacion_reciente" to FieldValue.serverTimestamp(),
                        "lastActiveTimestamp" to System.currentTimeMillis()
                    )

                    // Siempre guardamos en la referencia normalizada
                    transaccion.set(referenciaDocSanitizado, datosActualizacion, com.google.firebase.firestore.SetOptions.merge())

                    // Si existía bajo el id sin sanitizar, lo eliminamos para evitar duplicados
                    if (!esDocSanitizado && idDocSanitizado != correoSanitizado) {
                        transaccion.delete(referenciaDocRaw)
                    }

                    Log.i(ETIQUETA_LOG, "🔄 Vinculación actualizada para $correoSanitizado en dispositivo $idDispositivo con código $codigoFinal")
                } else {
                    // Documento nuevo: creación atómica del vínculo inicial con su código asociado
                    val datosNuevoRegistro = hashMapOf(
                        "correo" to correoSanitizado,
                        "email" to correoSanitizado,
                        "uid_firebase" to uidFirebase,
                        "firebaseUid" to uidFirebase,
                        "numero_miembro" to numeroMiembro,
                        "memberNumber" to numeroMiembro,
                        "nombre_piloto" to nombrePiloto,
                        "memberName" to nombrePiloto,
                        "codigo_acceso" to codigoLimpio,
                        "accessCode" to codigoLimpio,
                        "id_dispositivo_activo" to idDispositivo,
                        "activeDeviceId" to idDispositivo,
                        "fecha_creacion" to FieldValue.serverTimestamp(),
                        "fecha_vinculacion_reciente" to FieldValue.serverTimestamp(),
                        "linkedAt" to System.currentTimeMillis(),
                        "lastActiveTimestamp" to System.currentTimeMillis()
                    )
                    transaccion.set(referenciaDocSanitizado, datosNuevoRegistro)
                    Log.i(ETIQUETA_LOG, "✨ Nueva vinculación 1-a-1 creada para $correoSanitizado con $numeroMiembro y código $codigoLimpio")
                }
            }.await()

            Pair(true, "¡Cuenta vinculada exitosamente! Tu código queda enlazado exclusivamente a este correo.")
        } catch (error: Exception) {
            val mensajeError = error.message ?: "Error desconocido al procesar la vinculación en Firestore."
            Log.e(ETIQUETA_LOG, "❌ Error en transacción de vinculación: $mensajeError", error)
            Pair(false, mensajeError)
        }
    }

    /**
     * Consulta el estado de un correo en Firestore.
     * Retorna Triple(existe: Boolean, codigoRegistrado: String, nombrePiloto: String)
     */
    suspend fun consultarEstadoCorreo(correo: String): Triple<Boolean, String, String> {
        val correoSanitizado = correo.trim().lowercase()
        if (correoSanitizado.isBlank()) return Triple(false, "", "")

        return try {
            val baseDatos = FirebaseFirestore.getInstance()
            val idDocSanitizado = PerfilNube.sanitizarEmailDocId(correoSanitizado)
            var doc = baseDatos.collection(COLECCION_VINCULOS).document(idDocSanitizado).get().await()
            if (!doc.exists() && idDocSanitizado != correoSanitizado) {
                doc = baseDatos.collection(COLECCION_VINCULOS).document(correoSanitizado).get().await()
            }

            if (doc.exists()) {
                val codigo = doc.getString("codigo_acceso") ?: doc.getString("accessCode") ?: ""
                val nombre = doc.getString("nombre_piloto") ?: doc.getString("memberName") ?: "Piloto TX"
                Triple(true, codigo, nombre)
            } else {
                // Validación especial para correos fijos maestros
                if (correoSanitizado == "eduardo.androide.em@gmail.com") {
                    Triple(true, "DESARROLLO1", "Eduardo Marquez (EM)")
                } else if (correoSanitizado == "eduardo.jose.marquez.matos@gmail.com") {
                    Triple(true, "DESARROLLO2", "Eduardo Marquez (Matos)")
                } else {
                    Triple(false, "", "")
                }
            }
        } catch (e: Exception) {
            Log.e(ETIQUETA_LOG, "Error consultando estado de correo $correo: ${e.message}")
            Triple(false, "", "")
        }
    }

    /**
     * Valida si un código corresponde como contraseña al correo especificado.
     */
    suspend fun validarCodigoParaCorreo(correo: String, inputCode: String): Pair<Boolean, String> {
        val correoSanitizado = correo.trim().lowercase()
        val codeTrim = inputCode.trim().uppercase()

        if (correoSanitizado.isBlank() || codeTrim.isBlank()) {
            return Pair(false, "Debes ingresar correo y código de acceso.")
        }

        // 1. Verificación para cuentas fijas de desarrollador
        if (correoSanitizado == "eduardo.androide.em@gmail.com") {
            return if (codeTrim == "DESARROLLO1" || codeTrim == "TX19554402SB") {
                Pair(true, "Acceso concedido como Desarrollador 1")
            } else {
                Pair(false, "Código incorrecto para eduardo.androide.em@gmail.com.")
            }
        }

        if (correoSanitizado == "eduardo.jose.marquez.matos@gmail.com") {
            return if (codeTrim == "DESARROLLO2" || codeTrim == "19554402SB") {
                Pair(true, "Acceso concedido como Desarrollador 2")
            } else {
                Pair(false, "Código incorrecto para eduardo.jose.marquez.matos@gmail.com.")
            }
        }

        // 2. Verificación en Firestore para cualquier otra cuenta
        return try {
            val baseDatos = FirebaseFirestore.getInstance()
            val idDocSanitizado = PerfilNube.sanitizarEmailDocId(correoSanitizado)
            var doc = baseDatos.collection(COLECCION_VINCULOS).document(idDocSanitizado).get().await()
            if (!doc.exists() && idDocSanitizado != correoSanitizado) {
                doc = baseDatos.collection(COLECCION_VINCULOS).document(correoSanitizado).get().await()
            }

            if (!doc.exists()) {
                return Pair(false, "El correo no está vinculado a ninguna credencial activa.")
            }

            val codigoRegistrado = (doc.getString("codigo_acceso") ?: doc.getString("accessCode") ?: "").trim().uppercase()
            if (codigoRegistrado.isNotBlank() && codigoRegistrado == codeTrim) {
                Pair(true, "Código validado exitosamente.")
            } else if (codigoRegistrado.isBlank()) {
                // Si el vínculo no tenía código guardado, este código pasa a ser el oficial
                baseDatos.collection(COLECCION_VINCULOS).document(doc.id).update(
                    mapOf("codigo_acceso" to codeTrim, "accessCode" to codeTrim)
                )
                Pair(true, "Código validado y asignado como contraseña del correo.")
            } else {
                Pair(false, "Código incorrecto para este correo. Si lo olvidaste, solicítalo a la directiva.")
            }
        } catch (e: Exception) {
            Log.e(ETIQUETA_LOG, "Error validando código para correo: ${e.message}")
            Pair(false, "Error de conexión al validar credenciales: ${e.message}")
        }
    }

    /**
     * Permite cambiar el código de acceso por uno personalizado en Carnet TX,
     * sincronizándolo atómicamente con Firebase.
     */
    suspend fun actualizarCodigoAccesoPersonalizado(
        correo: String,
        nuevoCodigo: String,
        uidFirebase: String
    ): Pair<Boolean, String> {
        val correoSanitizado = correo.trim().lowercase()
        val nuevoCodigoTrim = nuevoCodigo.trim().uppercase()

        if (correoSanitizado.isBlank()) {
            return Pair(false, "No hay un correo vinculado activo para cambiar el código.")
        }
        if (nuevoCodigoTrim.length < 5) {
            return Pair(false, "El nuevo código debe tener al menos 5 caracteres.")
        }

        val baseDatos = FirebaseFirestore.getInstance()

        // 1. Verificar que el código NO esté asignado a otro correo
        try {
            val busqueda = baseDatos.collection(COLECCION_VINCULOS)
                .whereEqualTo("codigo_acceso", nuevoCodigoTrim)
                .get()
                .await()
            
            for (d in busqueda.documents) {
                val emailAsociado = d.getString("correo") ?: d.getString("email") ?: ""
                if (emailAsociado.isNotBlank() && !emailAsociado.equals(correoSanitizado, ignoreCase = true)) {
                    return Pair(false, "El código '$nuevoCodigoTrim' ya está en uso por otro miembro. Elige otro.")
                }
            }
        } catch (e: Exception) {
            Log.w(ETIQUETA_LOG, "Aviso verificando unicidad de código: ${e.message}")
        }

        return try {
            val idDocSanitizado = PerfilNube.sanitizarEmailDocId(correoSanitizado)
            val datosUpdate = mapOf(
                "codigo_acceso" to nuevoCodigoTrim,
                "accessCode" to nuevoCodigoTrim,
                "lastActiveTimestamp" to System.currentTimeMillis()
            )

            baseDatos.collection(COLECCION_VINCULOS).document(idDocSanitizado).update(datosUpdate).await()

            if (uidFirebase.isNotBlank()) {
                try {
                    baseDatos.collection(COLECCION_USUARIOS).document(uidFirebase).update(datosUpdate).await()
                } catch (_: Exception) {}
            }

            Log.i(ETIQUETA_LOG, "🔑 Código personalizado actualizado a '$nuevoCodigoTrim' para $correoSanitizado")
            Pair(true, "¡Código de acceso actualizado exitosamente a '$nuevoCodigoTrim'!")
        } catch (e: Exception) {
            Log.e(ETIQUETA_LOG, "Error actualizando código personalizado: ${e.message}")
            Pair(false, "Error al actualizar código en la nube: ${e.message}")
        }
    }

    /**
     * Desvincula y elimina el registro de 'vinculos_google' mediante una transacción atómica.
     * Limpia completamente usuarios/{uid} y elimina la ubicación en radar_en_vivo para no dejar rastro.
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

            // Limpieza preventiva de radar_en_vivo para evitar duplicaciones de pilotos en el mapa
            try {
                if (uidFirebase.isNotBlank()) {
                    baseDatos.collection("radar_en_vivo").document(uidFirebase).delete().await()
                }
                if (numeroMiembro.isNotBlank()) {
                    baseDatos.collection("radar_en_vivo").document(numeroMiembro).delete().await()
                }
            } catch (_: Exception) {}

            Pair(true, "✅ Cuenta desvinculada exitosamente. El correo y código han quedado completamente libres.")
        } catch (error: Exception) {
            val mensajeError = error.message ?: "Error desconocido al desvincular en Firestore."
            Log.e(ETIQUETA_LOG, "❌ Error en desvinculación: $mensajeError", error)
            Pair(false, mensajeError)
        }
    }
}
