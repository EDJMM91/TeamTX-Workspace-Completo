package com.example.data.remote

import android.util.Log
import com.aistudio.teamtxvzla.nube.AutenticacionNube
import com.aistudio.teamtxvzla.nube.EstadoAuth
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sincronización de Chat con Firebase Firestore.
 *
 * ARQUITECTURA CORREGIDA (resuelve race condition AUTH vs FIRESTORE):
 *
 * 1. Al crearse, NO adjunta listeners ni escribe nada.
 * 2. Espera a que AutenticacionNube emita EstadoAuth.AUTENTICADO.
 * 3. Solo entonces adjunta SnapshotListeners de los 4 canales.
 * 4. Si un usuario envía mensaje ANTES de estar autenticado,
 *    el mensaje se encola en `colaPendientes` y se envía cuando auth confirme.
 * 5. Si la sesión de Auth cambia (ej: de anónima a Google),
 *    re-adjunta todos los listeners automáticamente.
 */
class FirebaseChatSync(
    private val database: AppDatabase,
    private val scope: CoroutineScope
) {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "FIREBASE_SYNC"
    private val CHANNELS = listOf("GENERAL", "RODADAS", "MECANICA_AUXILIO", "DIRECTIVA")

    // Listeners activos por canal
    private val listeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()

    // Flag de inicialización (solo una vez)
    private val isInitialized = AtomicBoolean(false)

    // ═══════════════════════════════════════════════
    // COLA DE MENSAJES PENDIENTES (antes de auth)
    // ═══════════════════════════════════════════════

    /**
     * Cola thread-safe de mensajes que el usuario envió ANTES de que
     * la autenticación estuviera confirmada. Se procesan cuando auth = AUTENTICADO.
     */
    private val colaPendientes = ConcurrentLinkedQueue<ChatMessage>()

    // ═══════════════════════════════════════════════
    // ESTADO DE CONEXIÓN (para que la UI lo observe)
    // ═══════════════════════════════════════════════

    private val _conexionChat = MutableStateFlow(ConexionChat.DESCONOCIDO)
    val conexionChat: StateFlow<ConexionChat> = _conexionChat.asStateFlow()

    // ═══════════════════════════════════════════════
    // AUTH STATE LISTENER (re-conexión automática)
    // ═══════════════════════════════════════════════

    private var ultimoUidVisto: String? = null
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        synchronized(this@FirebaseChatSync) {
            if (uid != null && uid != ultimoUidVisto) {
                ultimoUidVisto = uid
                if (isInitialized.get()) {
                    Log.i(TAG, "🔄 Sesión de Auth cambió ($uid): re-adjuntando listeners")
                    reAdjuntarListeners()
                }
            } else if (uid == null) {
                ultimoUidVisto = null
                Log.w(TAG, "⚠️ Sesión de Auth perdida")
            }
        }
    }

    // ═══════════════════════════════════════════════
    // INICIALIZACIÓN (desde Repository)
    // ═══════════════════════════════════════════════

    init {
        try {
            FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        } catch (_: Exception) {}

        // Lanzar la inicialización que espera auth
        scope.launch { inicializar() }
    }

    /**
     * Punto de entrada principal. Espera auth y luego arranca todo.
     */
    private suspend fun inicializar() {
        if (isInitialized.getAndSet(true)) return

        Log.i(TAG, "⏳ Comprobando Auth para conectar chat...")
        _conexionChat.value = ConexionChat.ESPERANDO_AUTH

        // Garantizar sesión activa (reutiliza existente o espera confirmación)
        if (FirebaseAuth.getInstance().currentUser == null) {
            AutenticacionNube.esperarAuthLista()
        }

        Log.i(TAG, "✅ Auth lista. Iniciando listeners de chat...")
        _conexionChat.value = ConexionChat.CONECTANDO

        // Adjuntar listeners de los 4 canales
        for (channelId in CHANNELS) {
            setupChannelListener(channelId)
        }

        // Procesar mensajes que se encolaron antes de auth
        procesarColaPendientes()

        _conexionChat.value = ConexionChat.CONECTADO
        Log.i(TAG, "═══════════════════════════════════════")
        Log.i(TAG, "  CHAT CONECTADO - Todos los canales activos")
        Log.i(TAG, "═══════════════════════════════════════")
    }

    // ═══════════════════════════════════════════════
    // LISTENERS DE CANALES
    // ═══════════════════════════════════════════════

    /**
     * Adjunta un SnapshotListener a un canal específico de Firestore.
     * Puede ser uno de los 4 fijos o un grupo privado dinámico.
     */
    fun ensureChannelListener(channelId: String) {
        setupChannelListener(channelId)
    }

    private fun setupChannelListener(channelId: String) {
        // Evitar duplicados
        if (listeners.containsKey(channelId)) return

        scope.launch(Dispatchers.IO) {
            Log.d(TAG, "📡 Adjuntando listener de canal: $channelId")

            val registration = db.collection("chat_channels")
                .document(channelId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(200)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "❌ Error en canal $channelId: code=${error.code} ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot == null) return@addSnapshotListener

                    Log.d(TAG, "📥 Canal $channelId: ${snapshot.size()} mensajes")

                    scope.launch(Dispatchers.IO) {
                        // 1. 🗑️ Procesar eliminaciones de mensajes en tiempo real
                        for (change in snapshot.documentChanges) {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                val msgId = change.document.id.toLongOrNull()
                                if (msgId != null) {
                                    database.chatDao().deleteMessage(msgId)
                                }
                            }
                        }

                        // 2. 📥 Procesar mensajes entrantes y actualizados
                        val messages = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(ChatMessage::class.java)?.let { msg ->
                                    val photo = (msg.senderPhotoUrl ?: "").ifBlank { null }
                                        ?: doc.getString("senderPhotoUrl")
                                        ?: doc.getString("photoUrl")
                                        ?: doc.getString("photo_url")
                                        ?: doc.getString("foto_url")
                                        ?: doc.getString("avatarUrl")
                                    val photoSanitizada = com.example.util.SanitizadorImagenUrl.obtenerUrlEfectiva(photo)

                                    val audio = (msg.audioUrl ?: "").ifBlank { null }
                                        ?: doc.getString("audioUrl")
                                        ?: doc.getString("audio_url")
                                    val audioSanitizado = com.example.util.SanitizadorImagenUrl.obtenerUrlEfectiva(audio)

                                    msg.copy(
                                        id = doc.id.toLongOrNull() ?: msg.id,
                                        senderPhotoUrl = photoSanitizada,
                                        audioUrl = audioSanitizado
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error parseando msg ${doc.id}: ${e.message}")
                                null
                            }
                        }
                        if (messages.isNotEmpty()) {
                            database.chatDao().upsertMessages(messages)
                        }
                    }
                }
            listeners[channelId] = registration
        }
    }

    /**
     * Remueve todos los listeners y los vuelve a adjuntar.
     * Se llama cuando la sesión de Auth cambia.
     */
    private fun reAdjuntarListeners() {
        val existingCustomChannels = listeners.keys.toList()
        listeners.values.forEach { it.remove() }
        listeners.clear()
        for (channelId in CHANNELS) {
            setupChannelListener(channelId)
        }
        for (channelId in existingCustomChannels) {
            if (!CHANNELS.contains(channelId)) {
                setupChannelListener(channelId)
            }
        }
    }

    // ═══════════════════════════════════════════════
    // ENVÍO DE MENSAJES (con cola de pendientes)
    // ═══════════════════════════════════════════════

    /**
     * Envía un mensaje de chat con enfoque Offline-First.
     *
     * 1. Guarda de inmediato en Room con syncStatus = "PENDING" (o "SENT" si se sube al instante).
     * 2. Si hay conexión y Auth, sube a Firestore de inmediato.
     * 3. Si no hay conexión o falla la subida, queda con reloj 🕒 en Room y la cola de fondo
     *    lo despacha automáticamente apenas detecte red.
     */
    suspend fun sendMessage(message: ChatMessage) {
        val hayAuth = FirebaseAuth.getInstance().currentUser != null || AutenticacionNube.estaAutenticado()

        if (!hayAuth) {
            // Guardar local con estado PENDING
            message.syncStatus = "PENDING"
            database.chatDao().insertMessage(message)
            colaPendientes.add(message)
            _conexionChat.value = ConexionChat.ENCOLADO
            iniciarCicloReintentoCola()
            return
        }

        // Intento directo
        message.syncStatus = "PENDING"
        database.chatDao().insertMessage(message)

        scope.launch(Dispatchers.IO) {
            val enviado = enviarAFirestore(message)
            if (enviado) {
                database.chatDao().updateMessageSyncStatus(message.id, "SENT")
            } else {
                colaPendientes.add(message)
                _conexionChat.value = ConexionChat.ENCOLADO
                iniciarCicloReintentoCola()
            }
        }
    }

    private var colaReintentoJob: kotlinx.coroutines.Job? = null

    private fun iniciarCicloReintentoCola() {
        if (colaReintentoJob?.isActive == true) return
        colaReintentoJob = scope.launch(Dispatchers.IO) {
            while (true) {
                kotlinx.coroutines.delay(4000L) // Reintento cada 4 segundos
                val pendientesDb = database.chatDao().getPendingMessages()
                if (pendientesDb.isEmpty() && colaPendientes.isEmpty()) {
                    _conexionChat.value = ConexionChat.CONECTADO
                    break
                }

                val hayAuth = FirebaseAuth.getInstance().currentUser != null || AutenticacionNube.estaAutenticado()
                if (hayAuth) {
                    procesarColaPendientes()
                }
            }
        }
    }

    /**
     * Procesa todos los mensajes pendientes tanto de memoria como de Room.
     */
    fun procesarColaPendientes() {
        scope.launch(Dispatchers.IO) {
            val pendientesDb = database.chatDao().getPendingMessages()
            val listaAEnviar = (pendientesDb + colaPendientes.toList()).distinctBy { it.id }

            if (listaAEnviar.isEmpty()) return@launch

            Log.i(TAG, "📤 Despachando ${listaAEnviar.size} mensajes de la cola local...")
            var fallos = 0

            for (mensaje in listaAEnviar) {
                val exito = enviarAFirestore(mensaje)
                if (exito) {
                    database.chatDao().updateMessageSyncStatus(mensaje.id, "SENT")
                    colaPendientes.remove(mensaje)
                } else {
                    fallos++
                }
            }

            if (fallos == 0) {
                Log.i(TAG, "✅ Todos los mensajes de la cola local fueron despachados a Firestore")
                _conexionChat.value = ConexionChat.CONECTADO
            } else {
                _conexionChat.value = ConexionChat.ENCOLADO
            }
        }
    }

    /**
     * Escribe un mensaje en Firestore. Retorna true si se sincronizó correctamente.
     */
    private suspend fun enviarAFirestore(message: ChatMessage): Boolean = withContext(Dispatchers.IO) {
        try {
            val localId = message.id
            if (localId <= 0) {
                Log.w(TAG, "⚠️ ID inválido ($localId), omitiendo subida")
                return@withContext false
            }

            val docRef = db.collection("chat_channels")
                .document(message.channelId)
                .collection("messages")
                .document(localId.toString())

            val mensajeSincronizado = message.copy(syncStatus = "SENT")
            Log.d(TAG, "📤 Enviando msg a canal ${message.channelId}, ID: $localId")
            docRef.set(mensajeSincronizado, com.google.firebase.firestore.SetOptions.merge()).await()
            Log.i(TAG, "✅ Mensaje sincronizado: $localId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ FALLO enviando msg: ${e.message}")
            false
        }
    }

    /**
     * Marca una lista de mensajes como leídos tanto en Room local como en Firestore.
     */
    suspend fun markMessagesAsRead(messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        database.chatDao().upsertMessages(messages)
        val hayAuth = FirebaseAuth.getInstance().currentUser != null || AutenticacionNube.estaAutenticado()
        if (hayAuth) {
            scope.launch(Dispatchers.IO) {
                for (message in messages) {
                    try {
                        db.collection("chat_channels")
                            .document(message.channelId)
                            .collection("messages")
                            .document(message.id.toString())
                            .update("readBy", message.readBy)
                            .await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error actualizando readBy en Firestore para msg ${message.id}: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Actualiza las reacciones de un mensaje tanto en Room local como en Firestore.
     */
    suspend fun updateMessageReactions(messageId: Long, channelId: String, reactions: String) {
        val hayAuth = FirebaseAuth.getInstance().currentUser != null || AutenticacionNube.estaAutenticado()
        if (hayAuth) {
            scope.launch(Dispatchers.IO) {
                try {
                    db.collection("chat_channels")
                        .document(channelId)
                        .collection("messages")
                        .document(messageId.toString())
                        .update("reactions", reactions)
                        .await()
                    Log.d(TAG, "✅ Reacciones actualizadas en Firestore para msg $messageId: $reactions")
                } catch (e: Exception) {
                    Log.w(TAG, "Error actualizando reacciones en Firestore: ${e.message}")
                }
            }
        }
    }

    // ═══════════════════════════════════════════════
    // ELIMINACIÓN DE MENSAJES
    // ═══════════════════════════════════════════════

    suspend fun deleteMessage(messageId: Long) {
        database.chatDao().deleteMessage(messageId)
        if (AutenticacionNube.estaAutenticado()) {
            scope.launch(Dispatchers.IO) {
                for (channelId in CHANNELS) {
                    try {
                        db.collection("chat_channels")
                            .document(channelId)
                            .collection("messages")
                            .document(messageId.toString())
                            .delete()
                            .await()
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error eliminando msg $messageId: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Vacía un canal completo de chat tanto en Room local como en Firestore para todos los usuarios.
     */
    suspend fun clearChannel(channelId: String) {
        database.chatDao().deleteMessagesByChannel(channelId)
        scope.launch(Dispatchers.IO) {
            try {
                val snapshot = db.collection("chat_channels")
                    .document(channelId)
                    .collection("messages")
                    .get()
                    .await()
                if (!snapshot.isEmpty) {
                    val batch = db.batch()
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                    Log.i(TAG, "🧹 Canal $channelId vaciado en Firestore (${snapshot.size()} mensajes eliminados)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error vaciando canal $channelId en Firestore: ${e.message}")
            }
        }
    }

    // ═══════════════════════════════════════════════
    // LECTURA DE MENSAJES (siempre desde Room)
    // ═══════════════════════════════════════════════

    fun getMessagesForChannel(channelId: String): Flow<List<ChatMessage>> {
        return database.chatDao().getMessagesForChannel(channelId)
    }

    fun getAllMessages(): Flow<List<ChatMessage>> {
        return database.chatDao().getAllMessages()
    }

    // ═══════════════════════════════════════════════
    // APAGADO LIMPIO
    // ═══════════════════════════════════════════════

    suspend fun shutdown() {
        try { FirebaseAuth.getInstance().removeAuthStateListener(authStateListener) } catch (_: Exception) {}
        listeners.values.forEach { it.remove() }
        listeners.clear()
        isInitialized.set(false)
        colaPendientes.clear()
    }
}

/**
 * Estados de conexión del chat para que la UI los observe.
 */
enum class ConexionChat(val label: String) {
    DESCONOCIDO("Iniciando..."),
    ESPERANDO_AUTH("Conectando..."),
    CONECTANDO("Sincronizando..."),
    ENCOLADO("Mensajes pendientes..."),
    CONECTADO("Conectado"),
    DESCONECTADO("Sin conexión")
}
