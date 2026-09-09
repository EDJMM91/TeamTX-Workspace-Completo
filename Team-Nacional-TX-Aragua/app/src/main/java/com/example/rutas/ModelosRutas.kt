package com.example.rutas

/**
 * Modelos de datos para el Módulo de Rutas y el Seguro de Vida de Batería de Team Nacional TX Aragua.
 * Todos los modelos están documentados en español y estructurados para ser reutilizables y serializables.
 */

/**
 * Representa un punto de localización GPS grabado dentro de un trayecto o ruta motera.
 *
 * @property latitud Coordenada de latitud en grados decimales.
 * @property longitud Coordenada de longitud en grados decimales.
 * @property altitudMts Altitud sobre el nivel del mar en metros.
 * @property velocidadKmh Velocidad instantánea en kilómetros por hora.
 * @property timestamp Marca de tiempo en milisegundos cuando se capturó la posición.
 */
data class PuntoRutaGPS(
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val altitudMts: Double = 0.0,
    val velocidadKmh: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Estados operativos por los que puede pasar una grabación de ruta en vivo.
 */
enum class EstadoRutaEnum {
    /** La grabación está detenida o no se ha iniciado. */
    INACTIVA,

    /** Se están grabando y guardando puntos GPS activamente. */
    GRABANDO,

    /** La grabación está en pausa temporal por decisión del piloto. */
    PAUSADA,

    /** La ruta fue empaquetada y respaldada en la nube por batería baja (<=5%). */
    RESPALDADA_BATERIA,

    /** La ruta fue completada y guardada con éxito por el piloto. */
    FINALIZADA
}

/**
 * Representa una fotografía capturada durante la ruta para su inyección visual en el reproductor 2D.
 *
 * @property idFoto Identificador único de la foto.
 * @property uriFoto Ruta local o URI de la imagen en el dispositivo.
 * @property latitud Coordenada de latitud donde fue tomada.
 * @property longitud Coordenada de longitud donde fue tomada.
 * @property timestamp Momento exacto en milisegundos en que se tomó la foto.
 * @property descripcion Nota u observación opcional del piloto.
 */
data class FotoRuta(
    val idFoto: String = "",
    val uriFoto: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val descripcion: String = ""
)

/**
 * Estados del motor cinemático del reproductor 2D tipo Relive.
 */
enum class EstadoReproductorRuta {
    DETENIDO,
    REPRODUCIENDO,
    PAUSADO_FOTO,
    PAUSADO_MANUAL,
    FINALIZADO
}

/**
 * Telemetría en tiempo real emitida por el reproductor durante la animación.
 *
 * @property indicePuntoActual Índice del punto actual en la matriz de recorrido.
 * @property totalPuntos Cantidad total de puntos en la ruta.
 * @property latitudActual Latitud actual de la cámara/marcador.
 * @property longitudActual Longitud actual de la cámara/marcador.
 * @property rumboAzimuth Grados de orientación (0 a 360°) de la moto sobre el mapa.
 * @property velocidadSimuladaKmh Velocidad en km/h calculada en este segmento.
 * @property distanciaRecorridaKm Kilómetros simulados recorridos hasta el momento.
 * @property porcentajeProgreso Progreso de 0.0f a 1.0f.
 * @property fotoVisible Foto actual mostrada en overlay flotante (si aplica).
 */
data class TelemetriaReproduccion(
    val indicePuntoActual: Int = 0,
    val totalPuntos: Int = 0,
    val latitudActual: Double = 0.0,
    val longitudActual: Double = 0.0,
    val rumboAzimuth: Float = 0f,
    val velocidadSimuladaKmh: Float = 0f,
    val distanciaRecorridaKm: Double = 0.0,
    val porcentajeProgreso: Float = 0f,
    val fotoVisible: FotoRuta? = null
)

/**
 * Estructura completa que empaqueta y resume una ruta realizada por un piloto del club.
 *
 * @property idRuta Identificador único de la ruta (UUID o Timestamp).
 * @property tituloRuta Nombre o título asignado a la ruta/rodada.
 * @property idPiloto ID o UID del piloto que realizó el trayecto.
 * @property nombrePiloto Nombre o apodo del piloto registrado.
 * @property fechaInicioMs Timestamp en milisegundos del inicio de la ruta.
 * @property fechaFinMs Timestamp en milisegundos de finalización o último respaldo.
 * @property puntos Lista de puntos GPS recolectados durante la ruta.
 * @property fotos Lista de fotos tomadas durante la ruta para inyección en el reproductor.
 * @property distanciaTotalKm Kilómetros totales acumulados en el recorrido.
 * @property velocidadPromedioKmh Velocidad media registrada durante el recorrido.
 * @property velocidadMaximaKmh Velocidad máxima alcanzada en el recorrido.
 * @property estadoRuta Estado actual de la ruta (INACTIVA, GRABANDO, PAUSADA, RESPALDADA_BATERIA, FINALIZADA).
 * @property nivelBateriaRespaldo Porcentaje de batería del dispositivo en el momento del último respaldo.
 */
data class ResumenRutaTX(
    val idRuta: String = "",
    val tituloRuta: String = "Ruta TX",
    val idPiloto: String = "",
    val nombrePiloto: String = "Piloto TX",
    val fechaInicioMs: Long = System.currentTimeMillis(),
    val fechaFinMs: Long = System.currentTimeMillis(),
    val puntos: List<PuntoRutaGPS> = emptyList(),
    val fotos: List<FotoRuta> = emptyList(),
    val distanciaTotalKm: Double = 0.0,
    val velocidadPromedioKmh: Float = 0f,
    val velocidadMaximaKmh: Float = 0f,
    val estadoRuta: EstadoRutaEnum = EstadoRutaEnum.INACTIVA,
    val nivelBateriaRespaldo: Int = 100
)

/**
 * Paquete de respaldo automático enviado a la nube cuando el seguro de vida detecta batería <= 5%.
 *
 * @property idRespaldo Identificador único del evento de emergencia por batería.
 * @property idRuta Identificador de la ruta asociada.
 * @property porcentajeBateria Porcentaje de carga reportado por el sistema Android (ej: 4%).
 * @property timestamp Momento exacto en que se ejecutó el respaldo de emergencia.
 * @property resumenRuta Objeto con todos los puntos y datos acumulados de la ruta.
 * @property modeloDispositivo Modelo del teléfono que emitió el respaldo (ej: KEEWAY TX / Samsung S21).
 */
data class RespaldoBateriaRuta(
    val idRespaldo: String = "",
    val idRuta: String = "",
    val porcentajeBateria: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val resumenRuta: ResumenRutaTX = ResumenRutaTX(),
    val modeloDispositivo: String = android.os.Build.MODEL ?: "Dispositivo Android"
)
