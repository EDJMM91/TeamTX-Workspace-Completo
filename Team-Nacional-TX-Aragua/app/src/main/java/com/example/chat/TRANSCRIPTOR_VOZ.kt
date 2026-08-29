package com.example.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * MÓDULO INDEPENDIENTE: TRANSCRIPTOR DE VOZ A TEXTO
 * ═══════════════════════════════════════════════════════════════════════════
 * Este módulo encapsula la API nativa de Android (SpeechRecognizer) para
 * convertir la voz del usuario en texto en tiempo real.
 *
 * Características principales:
 * 1. Conversión de voz a texto en vivo (español por defecto).
 * 2. Soporte para resultados parciales (mientras la persona va hablando).
 * 3. Estados reactivos (StateFlow) para integración fluida con Jetpack Compose.
 * 4. Callbacks directos para uso en actividades o componentes tradicionales.
 * 5. Manejo detallado de errores en español comprensible.
 */
object TranscriptorVoz {

    private const val ETIQUETA_LOG = "TranscriptorVoz"

    // Instancia del reconocedor de voz nativo de Android
    private var reconocedorDeVoz: SpeechRecognizer? = null

    // Intención que configura los parámetros del reconocimiento de voz
    private var intencionDeVoz: Intent? = null

    // Contexto de la aplicación
    private var contextoAplicacion: Context? = null

    // ─────────────────────────────────────────────────────────────────────────
    // ESTADOS REACTIVOS (Observables desde la interfaz / Compose)
    // ─────────────────────────────────────────────────────────────────────────

    // Indica si el micrófono está escuchando activamente
    private val _estaEscuchando = MutableStateFlow(false)
    val estaEscuchando: StateFlow<Boolean> = _estaEscuchando.asStateFlow()

    // Almacena el texto final transcrito
    private val _textoTranscrito = MutableStateFlow("")
    val textoTranscrito: StateFlow<String> = _textoTranscrito.asStateFlow()

    // Almacena el texto parcial mientras el usuario sigue hablando
    private val _textoParcial = MutableStateFlow("")
    val textoParcial: StateFlow<String> = _textoParcial.asStateFlow()

    // Almacena el último mensaje de error si ocurre alguno
    private val _mensajeDeError = MutableStateFlow<String?>(null)
    val mensajeDeError: StateFlow<String?> = _mensajeDeError.asStateFlow()

    // Nivel de volumen / potencia de la voz recibida (útil para animar ondas)
    private val _nivelDeVoz = MutableStateFlow(0f)
    val nivelDeVoz: StateFlow<Float> = _nivelDeVoz.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // CALLBACKS OPCIONALES PERSONALIZADOS
    // ─────────────────────────────────────────────────────────────────────────
    private var alRecibirTextoFinalCallback: ((String) -> Unit)? = null
    private var alRecibirTextoParcialCallback: ((String) -> Unit)? = null
    private var alOcurrirErrorCallback: ((String) -> Unit)? = null
    private var alCambiarEstadoEscuchaCallback: ((Boolean) -> Unit)? = null

    private val manejadorHiloPrincipal = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * Verifica si el servicio de reconocimiento de voz está disponible en el dispositivo.
     */
    fun esReconocimientoDisponible(contexto: Context): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(contexto)
    }

    /**
     * Inicializa el reconocedor de voz con el contexto proporcionado en el Hilo Principal.
     */
    fun inicializar(contexto: Context) {
        contextoAplicacion = contexto.applicationContext

        manejadorHiloPrincipal.post {
            try {
                if (reconocedorDeVoz == null) {
                    reconocedorDeVoz = SpeechRecognizer.createSpeechRecognizer(contextoAplicacion)
                    reconocedorDeVoz?.setRecognitionListener(crearOyenteDeReconocimiento())
                }
                configurarIntencionDeVoz("es-ES")
            } catch (e: Exception) {
                Log.e(ETIQUETA_LOG, "Error al inicializar reconocedor en hilo principal: ${e.message}")
            }
        }
    }

    /**
     * Configura la intención del sistema con idioma español y resultados parciales.
     */
    private fun configurarIntencionDeVoz(codigoIdioma: String) {
        val idiomaActual = try {
            val loc = Locale.getDefault().toString()
            if (loc.startsWith("es")) loc else codigoIdioma
        } catch (e: Exception) {
            codigoIdioma
        }

        intencionDeVoz = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, idiomaActual)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, idiomaActual)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, idiomaActual)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, contextoAplicacion?.packageName)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora para transcribir tu mensaje...")
        }
    }

    /**
     * Inicia la captura de audio y transcripción a texto (ejecutado estrictamente en Main Looper).
     */
    fun iniciarEscucha(
        contexto: Context,
        codigoIdioma: String = "es-ES",
        alObtenerTexto: ((String) -> Unit)? = null,
        alObtenerParcial: ((String) -> Unit)? = null,
        alRecibirError: ((String) -> Unit)? = null,
        alCambiarEstado: ((Boolean) -> Unit)? = null
    ) {
        contextoAplicacion = contexto.applicationContext
        alRecibirTextoFinalCallback = alObtenerTexto
        alRecibirTextoParcialCallback = alObtenerParcial
        alOcurrirErrorCallback = alRecibirError
        alCambiarEstadoEscuchaCallback = alCambiarEstado

        _textoTranscrito.value = ""
        _textoParcial.value = ""
        _mensajeDeError.value = null
        _nivelDeVoz.value = 0f

        manejadorHiloPrincipal.post {
            try {
                // Recrear de forma fresca para evitar estados colgados (ERROR_CLIENT / ERROR_RECOGNIZER_BUSY)
                reconocedorDeVoz?.destroy()
                reconocedorDeVoz = SpeechRecognizer.createSpeechRecognizer(contextoAplicacion)
                reconocedorDeVoz?.setRecognitionListener(crearOyenteDeReconocimiento())

                configurarIntencionDeVoz(codigoIdioma)

                reconocedorDeVoz?.startListening(intencionDeVoz)
                _estaEscuchando.value = true
                alCambiarEstadoEscuchaCallback?.invoke(true)
                Log.d(ETIQUETA_LOG, "🎙️ Escucha nativa iniciada en MainThread exitosamente")
            } catch (excepcion: Exception) {
                val errorMsg = "Error al iniciar la escucha: ${excepcion.localizedMessage ?: "desconocido"}"
                Log.e(ETIQUETA_LOG, errorMsg, excepcion)
                _mensajeDeError.value = errorMsg
                _estaEscuchando.value = false
                alOcurrirErrorCallback?.invoke(errorMsg)
                alCambiarEstadoEscuchaCallback?.invoke(false)
            }
        }
    }

    /**
     * Detiene la escucha activa y procesa el audio capturado.
     */
    fun detenerEscucha() {
        manejadorHiloPrincipal.post {
            try {
                reconocedorDeVoz?.stopListening()
                _estaEscuchando.value = false
                alCambiarEstadoEscuchaCallback?.invoke(false)
                Log.d(ETIQUETA_LOG, "Escucha detenida por el usuario.")
            } catch (excepcion: Exception) {
                Log.e(ETIQUETA_LOG, "Error al detener la escucha", excepcion)
            }
        }
    }

    /**
     * Cancela la sesión actual de reconocimiento descartando el audio pendiente.
     */
    fun cancelarEscucha() {
        manejadorHiloPrincipal.post {
            try {
                reconocedorDeVoz?.cancel()
                _estaEscuchando.value = false
                _nivelDeVoz.value = 0f
                alCambiarEstadoEscuchaCallback?.invoke(false)
                Log.d(ETIQUETA_LOG, "Escucha cancelada.")
            } catch (excepcion: Exception) {
                Log.e(ETIQUETA_LOG, "Error al cancelar la escucha", excepcion)
            }
        }
    }

    /**
     * Libera los recursos del reconocedor de voz.
     */
    fun liberarRecursos() {
        manejadorHiloPrincipal.post {
            try {
                reconocedorDeVoz?.destroy()
                reconocedorDeVoz = null
                _estaEscuchando.value = false
                alRecibirTextoFinalCallback = null
                alRecibirTextoParcialCallback = null
                alOcurrirErrorCallback = null
                alCambiarEstadoEscuchaCallback = null
                Log.d(ETIQUETA_LOG, "Recursos del transcriptor de voz liberados exitosamente.")
            } catch (excepcion: Exception) {
                Log.e(ETIQUETA_LOG, "Error al liberar recursos del reconocedor", excepcion)
            }
        }
    }

    /**
     * Construye el oyente (RecognitionListener) que intercepta todos los eventos
     * del ciclo de vida del reconocimiento de voz nativo.
     */
    private fun crearOyenteDeReconocimiento(): RecognitionListener {
        return object : RecognitionListener {

            // Se ejecuta cuando el reconocedor está listo para que el usuario empiece a hablar
            override fun onReadyForSpeech(params: Bundle?) {
                _estaEscuchando.value = true
                _mensajeDeError.value = null
                Log.d(ETIQUETA_LOG, "Listo para recibir voz del usuario.")
            }

            // Se ejecuta cuando el usuario comienza a emitir sonido
            override fun onBeginningOfSpeech() {
                _estaEscuchando.value = true
                Log.d(ETIQUETA_LOG, "Comienzo de detección de voz.")
            }

            // Se ejecuta constantemente mientras cambia el nivel de volumen capturado
            override fun onRmsChanged(rmsdB: Float) {
                _nivelDeVoz.value = rmsdB
            }

            // Se ejecuta si se reciben paquetes de buffer de sonido
            override fun onBufferReceived(buffer: ByteArray?) {
                // No se requiere procesamiento especial de buffer crudo
            }

            // Se ejecuta cuando el usuario hace una pausa o deja de hablar
            override fun onEndOfSpeech() {
                _estaEscuchando.value = false
                _nivelDeVoz.value = 0f
                alCambiarEstadoEscuchaCallback?.invoke(false)
                Log.d(ETIQUETA_LOG, "Fin de detección de voz (procesando resultados)...")
            }

            // Se ejecuta cuando ocurre cualquier error durante el reconocimiento
            override fun onError(codigoDeError: Int) {
                _estaEscuchando.value = false
                _nivelDeVoz.value = 0f
                alCambiarEstadoEscuchaCallback?.invoke(false)

                val textoExplicativoError = obtenerMensajeDeError(codigoDeError)
                _mensajeDeError.value = textoExplicativoError
                Log.w(ETIQUETA_LOG, "Error en reconocimiento [$codigoDeError]: $textoExplicativoError")
                alOcurrirErrorCallback?.invoke(textoExplicativoError)
            }

            // Se ejecuta cuando se completa el reconocimiento con los resultados finales
            override fun onResults(resultados: Bundle?) {
                _estaEscuchando.value = false
                _nivelDeVoz.value = 0f
                alCambiarEstadoEscuchaCallback?.invoke(false)

                val listaTextosPosibles = resultados?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!listaTextosPosibles.isNullOrEmpty()) {
                    // Tomamos la primera coincidencia (la de mayor confianza)
                    val textoFinal = listaTextosPosibles[0]
                    _textoTranscrito.value = textoFinal
                    _textoParcial.value = ""
                    Log.d(ETIQUETA_LOG, "Texto final transcrito: $textoFinal")
                    alRecibirTextoFinalCallback?.invoke(textoFinal)
                }
            }

            // Se ejecuta en tiempo real entregando fragmentos de texto mientras el usuario habla
            override fun onPartialResults(resultadosParciales: Bundle?) {
                val listaParcial = resultadosParciales?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!listaParcial.isNullOrEmpty()) {
                    val textoTemporal = listaParcial[0]
                    _textoParcial.value = textoTemporal
                    Log.d(ETIQUETA_LOG, "Texto parcial: $textoTemporal")
                    alRecibirTextoParcialCallback?.invoke(textoTemporal)
                }
            }

            // Se ejecuta cuando el servicio envía eventos personalizados del proveedor
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Eventos extendidos del sistema
            }
        }
    }

    /**
     * Traduce los códigos numéricos de error de SpeechRecognizer a explicaciones
     * claras y amigables en español.
     */
    fun obtenerMensajeDeError(codigoError: Int): String {
        return when (codigoError) {
            SpeechRecognizer.ERROR_AUDIO -> "Error al capturar el audio del micrófono."
            SpeechRecognizer.ERROR_CLIENT -> "Error del cliente de la aplicación."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permiso de micrófono no otorgado."
            SpeechRecognizer.ERROR_NETWORK -> "Error de conexión de red."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera de conexión agotado."
            SpeechRecognizer.ERROR_NO_MATCH -> "No se reconoció ninguna palabra. Intenta hablar más cerca."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El reconocedor de voz está ocupado en este momento."
            SpeechRecognizer.ERROR_SERVER -> "Error en el servidor de reconocimiento de voz."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó ninguna voz durante el tiempo de espera."
            else -> "Ocurrió un error inesperado al procesar la voz (Código: $codigoError)."
        }
    }
}
