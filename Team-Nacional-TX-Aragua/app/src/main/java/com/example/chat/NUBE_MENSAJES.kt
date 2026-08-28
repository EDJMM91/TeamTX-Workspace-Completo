package com.example.chat

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * ARCHIVO: NUBE_MENSAJES.kt
 * 
 * Objeto independiente encargado de la comunicación con Firebase Firestore.
 * Maneja la lectura en tiempo real mediante flujos (Flow) y el envío
 * de mensajes hacia la base de datos en la nube.
 * Soporta: texto, imagen, audio, sticker (WebP).
 */
object NubeMensajes {

    private const val COLECCION = "mensajes_chat"

    /**
     * Escucha en tiempo real todos los mensajes ordenados por hora.
     * Retorna un Flow con la lista actualizada para actualizar la interfaz automáticamente.
     */
    fun escucharMensajes(): Flow<List<Mensaje>> = callbackFlow {
        try {
            val baseDatos = FirebaseFirestore.getInstance()
            val consulta = baseDatos.collection(COLECCION)
                .orderBy("hora", Query.Direction.ASCENDING)
                .limit(100)

            val suscripcion = consulta.addSnapshotListener { instantanea, error ->
                if (error != null) {
                    Log.e("NubeMensajes", "Error al escuchar mensajes: ${error.message}")
                    return@addSnapshotListener
                }

                if (instantanea != null) {
                    val lista = instantanea.documents.mapNotNull { documento ->
                        val id = documento.id
                        val texto = documento.getString("texto") ?: ""
                        val emisor = documento.getString("emisor") ?: "Piloto TX"
                        val apodoEmisor = documento.getString("apodoEmisor") ?: ""
                        val hora = documento.getLong("hora") ?: System.currentTimeMillis()
                        val tipoStr = documento.getString("tipo") ?: "texto"
                        val tipo = TipoMensaje.values().find { it.etiqueta == tipoStr } ?: TipoMensaje.TEXTO
                        val urlMultimedia = documento.getString("urlMultimedia") ?: ""
                        val nombreArchivo = documento.getString("nombreArchivo") ?: ""
                        val channelId = documento.getString("channelId") ?: "GENERAL"
                        val senderMemberId = documento.getLong("senderMemberId") ?: 0
                        val senderMemberNumber = documento.getString("senderMemberNumber") ?: ""
                        val senderRole = documento.getString("senderRole") ?: "MIEMBRO_ACTIVO"
                        val senderCustomRoleTitle = documento.getString("senderCustomRoleTitle")
                        val senderInitials = documento.getString("senderInitials") ?: "TX"
                        val isRadioCallout = documento.getBoolean("isRadioCallout") ?: false
                        Mensaje(
                            id = id,
                            texto = texto,
                            emisor = emisor,
                            apodoEmisor = apodoEmisor,
                            hora = hora,
                            tipo = tipo,
                            urlMultimedia = urlMultimedia,
                            nombreArchivo = nombreArchivo,
                            channelId = channelId,
                            senderMemberId = senderMemberId,
                            senderMemberNumber = senderMemberNumber,
                            senderRole = senderRole,
                            senderCustomRoleTitle = senderCustomRoleTitle,
                            senderInitials = senderInitials,
                            isRadioCallout = isRadioCallout
                        )
                    }
                    trySend(lista)
                }
            }

            awaitClose {
                suscripcion.remove()
            }
        } catch (e: Exception) {
            Log.e("NubeMensajes", "Firebase no disponible temporalmente: ${e.message}")
            trySend(emptyList())
            awaitClose { }
        }
    }

    /**
     * Envía un nuevo mensaje de texto a la base de datos de Firebase.
     */
    fun enviarMensaje(
        texto: String,
        emisor: String,
        apodoEmisor: String = "",
        channelId: String = "GENERAL",
        senderMemberId: Long = 0,
        senderMemberNumber: String = "",
        senderRole: String = "MIEMBRO_ACTIVO",
        senderCustomRoleTitle: String? = null,
        senderInitials: String = "TX",
        isRadioCallout: Boolean = false,
        alCompletar: (Boolean) -> Unit = {}
    ) {
        if (texto.isBlank()) return

        try {
            val baseDatos = FirebaseFirestore.getInstance()
            val mapaMensaje = hashMapOf(
                "texto" to texto.trim(),
                "emisor" to emisor,
                "apodoEmisor" to apodoEmisor,
                "hora" to System.currentTimeMillis(),
                "tipo" to TipoMensaje.TEXTO.etiqueta,
                "urlMultimedia" to "",
                "nombreArchivo" to "",
                "channelId" to channelId,
                "senderMemberId" to senderMemberId,
                "senderMemberNumber" to senderMemberNumber,
                "senderRole" to senderRole,
                "senderCustomRoleTitle" to senderCustomRoleTitle,
                "senderInitials" to senderInitials,
                "isRadioCallout" to isRadioCallout
            )

            baseDatos.collection(COLECCION)
                .add(mapaMensaje)
                .addOnSuccessListener {
                    alCompletar(true)
                }
                .addOnFailureListener { error ->
                    Log.e("NubeMensajes", "Error al guardar mensaje: ${error.message}")
                    alCompletar(false)
                }
        } catch (e: Exception) {
            Log.e("NubeMensajes", "Excepción al enviar mensaje: ${e.message}")
            alCompletar(false)
        }
    }

    /**
     * Envía un mensaje multimedia (imagen, audio, sticker) a Firebase.
     */
    fun enviarMensajeMultimedia(
        tipo: TipoMensaje,
        urlMultimedia: String,
        nombreArchivo: String,
        emisor: String,
        apodoEmisor: String = "",
        texto: String = "",
        channelId: String = "GENERAL",
        senderMemberId: Long = 0,
        senderMemberNumber: String = "",
        senderRole: String = "MIEMBRO_ACTIVO",
        senderCustomRoleTitle: String? = null,
        senderInitials: String = "TX",
        isRadioCallout: Boolean = false,
        alCompletar: (Boolean) -> Unit = {}
    ) {
        if (urlMultimedia.isBlank()) return

        try {
            val baseDatos = FirebaseFirestore.getInstance()
            val mapaMensaje = hashMapOf(
                "texto" to texto.trim(),
                "emisor" to emisor,
                "apodoEmisor" to apodoEmisor,
                "hora" to System.currentTimeMillis(),
                "tipo" to tipo.etiqueta,
                "urlMultimedia" to urlMultimedia,
                "nombreArchivo" to nombreArchivo,
                "channelId" to channelId,
                "senderMemberId" to senderMemberId,
                "senderMemberNumber" to senderMemberNumber,
                "senderRole" to senderRole,
                "senderCustomRoleTitle" to senderCustomRoleTitle,
                "senderInitials" to senderInitials,
                "isRadioCallout" to isRadioCallout
            )

            baseDatos.collection(COLECCION)
                .add(mapaMensaje)
                .addOnSuccessListener {
                    alCompletar(true)
                }
                .addOnFailureListener { error ->
                    Log.e("NubeMensajes", "Error al guardar multimedia: ${error.message}")
                    alCompletar(false)
                }
        } catch (e: Exception) {
            Log.e("NubeMensajes", "Excepción al enviar multimedia: ${e.message}")
            alCompletar(false)
        }
    }
}
