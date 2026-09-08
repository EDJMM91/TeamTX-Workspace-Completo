package com.example.rutas

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Gestor de sincronización en la nube para el Módulo de Rutas de Team Nacional TX Aragua.
 * Conecta directamente con Firebase Firestore para almacenar rutas, copias de seguridad de batería
 * y trazados GPS en tiempo real.
 *
 * Todos los métodos y logs utilizan nomenclatura básica en español.
 */
object NUBE {

    private const val TAG = "TEAM_TX_RUTAS_NUBE"
    private const val COLECCION_RUTAS = "rutas_moteras_tx"
    private const val COLECCION_RESPALDOS_BATERIA = "respaldos_bateria_tx"

    /**
     * Guarda o actualiza una ruta completa en la nube Firebase Firestore.
     *
     * @param resumenRuta Objeto con todos los datos y puntos acumulados de la ruta.
     * @param alTerminar Callback que retorna `true` si la operación en la nube fue exitosa.
     */
    fun guardarRutaNube(resumenRuta: ResumenRutaTX, alTerminar: (Boolean) -> Unit = {}) {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection(COLECCION_RUTAS).document(resumenRuta.idRuta)

        val mapaPuntos = resumenRuta.puntos.map { punto ->
            mapOf(
                "latitud" to punto.latitud,
                "longitud" to punto.longitud,
                "altitudMts" to punto.altitudMts,
                "velocidadKmh" to punto.velocidadKmh,
                "timestamp" to punto.timestamp
            )
        }

        val datosRuta = mapOf(
            "idRuta" to resumenRuta.idRuta,
            "tituloRuta" to resumenRuta.tituloRuta,
            "idPiloto" to resumenRuta.idPiloto,
            "nombrePiloto" to resumenRuta.nombrePiloto,
            "fechaInicioMs" to resumenRuta.fechaInicioMs,
            "fechaFinMs" to resumenRuta.fechaFinMs,
            "distanciaTotalKm" to resumenRuta.distanciaTotalKm,
            "velocidadPromedioKmh" to resumenRuta.velocidadPromedioKmh,
            "velocidadMaximaKmh" to resumenRuta.velocidadMaximaKmh,
            "estadoRuta" to resumenRuta.estadoRuta.name,
            "nivelBateriaRespaldo" to resumenRuta.nivelBateriaRespaldo,
            "cantidadPuntos" to resumenRuta.puntos.size,
            "puntos" to mapaPuntos,
            "ultimaActualizacion" to System.currentTimeMillis()
        )

        docRef.set(datosRuta, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "🟢 Ruta guardada exitosamente en Firebase: ${resumenRuta.idRuta}")
                alTerminar(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "🔴 Error guardando ruta en Firebase: ${e.localizedMessage}", e)
                alTerminar(false)
            }
    }

    /**
     * Respalda inmediatamente una ruta cuando el seguro de vida detecta batería en nivel crítico (<= 5%).
     *
     * @param respaldo Objeto con los datos de empaquetado de la batería y la ruta.
     * @param alTerminar Callback que retorna `true` si el respaldo crítico fue guardado en Firebase.
     */
    fun respaldarBateriaCritica(respaldo: RespaldoBateriaRuta, alTerminar: (Boolean) -> Unit = {}) {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection(COLECCION_RESPALDOS_BATERIA).document(respaldo.idRespaldo)

        val mapaPuntos = respaldo.resumenRuta.puntos.map { punto ->
            mapOf(
                "latitud" to punto.latitud,
                "longitud" to punto.longitud,
                "altitudMts" to punto.altitudMts,
                "velocidadKmh" to punto.velocidadKmh,
                "timestamp" to punto.timestamp
            )
        }

        val datosRespaldo = mapOf(
            "idRespaldo" to respaldo.idRespaldo,
            "idRuta" to respaldo.idRuta,
            "porcentajeBateria" to respaldo.porcentajeBateria,
            "timestamp" to respaldo.timestamp,
            "modeloDispositivo" to respaldo.modeloDispositivo,
            "tituloRuta" to respaldo.resumenRuta.tituloRuta,
            "idPiloto" to respaldo.resumenRuta.idPiloto,
            "nombrePiloto" to respaldo.resumenRuta.nombrePiloto,
            "distanciaTotalKm" to respaldo.resumenRuta.distanciaTotalKm,
            "puntos" to mapaPuntos
        )

        // 1. Guardar en la colección de respaldos de emergencia por batería
        docRef.set(datosRespaldo)
            .addOnSuccessListener {
                Log.d(TAG, "⚡ Respaldo por Batería Crítica (${respaldo.porcentajeBateria}%) subido a Firebase Firestore")
                // 2. Actualizar también la ruta principal
                guardarRutaNube(respaldo.resumenRuta, alTerminar)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "🔴 Error en respaldo por batería en Firebase: ${e.localizedMessage}", e)
                alTerminar(false)
            }
    }

    /**
     * Obtiene la lista de rutas registradas para un piloto en particular desde Firebase.
     *
     * @param idPiloto Identificador del piloto.
     * @param alRecibir Callback con la lista de rutas obtenidas.
     */
    fun obtenerRutasNube(idPiloto: String, alRecibir: (List<ResumenRutaTX>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection(COLECCION_RUTAS)
            .whereEqualTo("idPiloto", idPiloto)
            .get()
            .addOnSuccessListener { snapshot ->
                val lista = snapshot.documents.mapNotNull { doc ->
                    try {
                        val idRuta = doc.getString("idRuta") ?: doc.id
                        val titulo = doc.getString("tituloRuta") ?: "Ruta TX"
                        val nombrePiloto = doc.getString("nombrePiloto") ?: ""
                        val distancia = doc.getDouble("distanciaTotalKm") ?: 0.0
                        val velProm = (doc.getDouble("velocidadPromedioKmh") ?: 0.0).toFloat()
                        val velMax = (doc.getDouble("velocidadMaximaKmh") ?: 0.0).toFloat()
                        val estadoStr = doc.getString("estadoRuta") ?: EstadoRutaEnum.FINALIZADA.name
                        val estado = try { EstadoRutaEnum.valueOf(estadoStr) } catch (_: Exception) { EstadoRutaEnum.FINALIZADA }

                        ResumenRutaTX(
                            idRuta = idRuta,
                            tituloRuta = titulo,
                            idPiloto = idPiloto,
                            nombrePiloto = nombrePiloto,
                            distanciaTotalKm = distancia,
                            velocidadPromedioKmh = velProm,
                            velocidadMaximaKmh = velMax,
                            estadoRuta = estado
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                alRecibir(lista)
            }
            .addOnFailureListener {
                alRecibir(emptyList())
            }
    }
}
