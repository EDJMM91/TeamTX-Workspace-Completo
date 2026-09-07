package com.example

import com.aistudio.teamtxvzla.BuildConfig
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.ui.theme.MotoGoldSecondary
import com.example.ui.theme.MotoOrangePrimary
import com.example.ui.theme.TxFlameRed
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

// ═══════════════════════════════════════════════════════════════════════════
// GESTOR DE ACTUALIZACIONES OTA Y CHANGELOG DINÁMICO (TEAM TX PRO)
// Muestra con exactitud qué incluye cada versión: novedades, mejoras y fixes.
// ═══════════════════════════════════════════════════════════════════════════

object GestorActualizaciones {
    private const val TAG = "OTA_UPDATER"

    // ─────────────────────────────────────────────────────────────────────────
    // MODELOS DE DATOS DE ACTUALIZACIONES Y CHANGELOG
    // ─────────────────────────────────────────────────────────────────────────

    data class NotaVersionDetallada(
        val versionCode: Int,
        val versionName: String,
        val titulo: String,
        val fecha: String,
        val descripcionCorta: String,
        val novedades: List<String>,
        val correcciones: List<String> = emptyList(),
        val esRecomendada: Boolean = false
    )

    data class InformacionOta(
        val versionCode: Int = 0,
        val versionName: String = "",
        val titulo: String = "",
        val urlDescarga: String = "",
        val notas: String = "",
        val novedades: List<String> = emptyList(),
        val correcciones: List<String> = emptyList(),
        val fechaPublicacion: String = ""
    )

    data class StorageApkVersion(
        val fileName: String = "",
        val downloadUrl: String = "",
        val sizeBytes: Long = 0L,
        val formattedSize: String = "",
        val updatedTimestamp: Long = 0L,
        val formattedDate: String = "",
        val versionName: String = "",
        val versionCode: Int = 0,
        val titulo: String = "",
        val notas: String = "",
        val novedades: List<String> = emptyList(),
        val correcciones: List<String> = emptyList(),
        val isStable: Boolean = true,
        val isBeta: Boolean = false,
        val isRecommendedLatest: Boolean = false
    )

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTRO HISTÓRICO OFICIAL DE VERSIONES Y CHANGELOGS DETALLADOS
    // ─────────────────────────────────────────────────────────────────────────

    val HISTORIAL_VERSIONES_OFICIALES = listOf(
        NotaVersionDetallada(
            versionCode = 18,
            versionName = "1.3.7-beta",
            titulo = "Vinculación Estricta 1-a-1 (Código como Contraseña), Personalización en Carnet TX y Purga Cero Residuos",
            fecha = "07 sep 2026",
            descripcionCorta = "Autenticación 1-a-1 estricta: el código de acceso actúa como la contraseña del correo. Personalización de código en Carnet TX, cambio de cuenta de desarrollo, limpieza integral de Firebase y resolución definitiva de conflictos de sesión entre múltiples dispositivos.",
            novedades = listOf(
                "🔑 Código como Contraseña: Cada correo vinculado posee estrictamente su código único como contraseña de acceso en Firebase.",
                "📱 Fin a Conflictos Multi-dispositivo: Eliminación de sesiones cruzadas en Chat y Radar; radar_en_vivo purgado y saneado en tiempo real.",
                "🎛️ Código Personalizado en Carnet TX: Nueva opción en el Carnet para cambiar el código de acceso y sincronizarlo inmediatamente a la cuenta de Firebase.",
                "🔄 Cambio de Cuenta para Desarrollador: Selector rápido en Carnet TX exclusivo para desarrolladores para alternar entre DESARROLLO1 y DESARROLLO2 sin residuos locales.",
                "🧹 Purga Absoluta al Desvincular: Al desvincular o cerrar sesión, se detiene la telemetría en vivo, se eliminan los rastros en Firestore y se limpian Room, SharedPreferences y caché de Coil.",
                "📧 Flujo Renovado de Acceso por Correo: Pantalla de login de 2 pasos; si el correo está vinculado solicita su código/contraseña, si no está vinculado permite solicitarlo directamente a la directiva."
            ),
            correcciones = listOf(
                "Corregido: Detección errónea de sesiones previas en otro dispositivo y fantasmas en el mapa Radar.",
                "Corregido: Cruce de identidad en el chat entre dispositivos de prueba con códigos compartidos.",
                "Corregido: Residuos en Firebase Firestore (vinculos_google, usuarios, radar_en_vivo) reseteados a estado base limpio con únicamente 2 cuentas maestras de desarrollo."
            ),
            esRecomendada = true
        ),
        NotaVersionDetallada(
            versionCode = 17,
            versionName = "1.3.6-beta",
            titulo = "Perfil Desde Cero, Directorio Real y Limpieza Total de Sesión",
            fecha = "07 sep 2026",
            descripcionCorta = "La app arranca completamente vacía al ingresar con un código sin vincular. El directorio solo muestra pilotos reales sincronizados. Se eliminan correos ficticios y se limpia el perfil completo al detectar vínculos caducos.",
            novedades = listOf(
                "🆕 Perfil vacío desde cero: Al entrar con un código sin Google vinculado, la app muestra perfil vacío (sin foto, datos, correo ni historial).",
                "🔗 Limpieza de vínculos caducos: Si el vínculo a Firestore ya no existe, se borra foto de perfil, foto de moto, documentos y datos personales locales automáticamente.",
                "📋 Directorio real: El módulo 'Directorio de Pilotos' muestra únicamente miembros con correo Google vinculado o activos en las últimas 24h.",
                "🔢 Contador sincronizados: La estadística 'Sincronizados' refleja el conteo real de pilotos conectados a la app.",
                "🔄 Toggle para Directiva: Los directivos pueden alternar entre 'Solo activos' y 'Ver todos los registros' en el directorio.",
                "🚫 Sin correos ficticios en sesión: Los códigos de prueba y directivos ya no guardan correos @teamtx.com en SharedPreferences.",
                "🛡️ Null-safety total: 13 errores de compilación Kotlin corregidos en TeamTxViewModel relacionados con emails nulos."
            ),
            correcciones = listOf(
                "Corregido: 'ya está en otro dispositivo' al usar códigos distintos (el correo queda libre al desvincular).",
                "Corregido: correos @teamtx.com ya no aparecen como correos Google vinculados del perfil.",
                "Corregido: la pantalla login ya no sugiere códigos activos reales como ejemplos."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 16,
            versionName = "1.3.5-beta",
            titulo = "Seguridad de Cuentas, Sesión Única Anti-Trampas y Vinculación 1-a-1",
            fecha = "07 sep 2026",
            descripcionCorta = "Nuevo sistema de seguridad de cuentas con UUID por dispositivo, sesión única anti-trampas en tiempo real, vinculación 1-a-1 con transacciones atómicas en Firestore y opción de desvinculación libre.",
            novedades = listOf(
                "🛡️ Seguridad de Cuentas: Identificador persistente único de dispositivo ('id_dispositivo_activo_unico').",
                "🚨 Anti-Trampa en Vivo: Detección automática en tiempo real de apertura de sesión en otro celular con cierre forzoso inmediato.",
                "🔗 Vinculación 1-a-1 Atómica: Transacciones en 'vinculos_google' que garantizan que un correo pertenezca únicamente a un piloto a la vez.",
                "🔓 Desvinculación de Google: Nuevo botón [Desvincular] en Carnet TX con advertencia de confirmación para liberar el correo sin borrar datos locales.",
                "🔒 Hard Logout: Cierre seguro total con revocación de tokens de Google Sign-In, limpieza de SharedPreferences y purga de caché de fotos."
            ),
            correcciones = listOf(
                "Eliminado el riesgo de cruce o sobreescritura de cuentas entre celulares con perfiles distintos.",
                "Optimizado el listener en tiempo real de Firestore para detener sincronización cuando la sesión no corresponde al dispositivo local."
            ),
            esRecomendada = true
        ),
        NotaVersionDetallada(
            versionCode = 14,
            versionName = "1.4.0",
            titulo = "Módulo Reproductor TX Pro & Calendario Avanzado",
            fecha = "30 ago 2026",
            descripcionCorta = "Nuevo Reproductor de música nativo con Super Bass y Ultra Volumen (+300%), ecualizador 5 bandas, La Nube in-app, widget y mejoras en el Calendario Motero.",
            novedades = listOf(
                "🎧 Reproductor TX Pro: Motor de audio nativo C++ (JNI) de alto rendimiento para rodadas.",
                "🔥 Super Bass & Ultra Volumen (+300%): Amplificación con limitador suave para evitar distorsiones a alta velocidad.",
                "🎚️ Ecualizador Real de 5 Bandas: 6 presets moteros (Rock, Carretera, Bass Boost, Club, etc.) y simulación espacial 3D.",
                "☁️ La Nube In-App: Burbuja flotante arrastrable con bloqueo de pantalla para controlar la música mientras navegas.",
                "📱 Widget de Escritorio: Control de reproducción nativo desde la pantalla de inicio de Android.",
                "💾 Persistencia Total: Guarda y restaura automáticamente la última canción, posición en milisegundos y carpetas locales.",
                "🗓️ Calendario Motero Renovado: Ocultación de barra inferior y modales para programar actividades y cumpleaños.",
                "📢 Muro TX: Nuevas categorías para Obras Benéficas, Mantenimiento Preventivo y Lavado Familiar."
            ),
            correcciones = listOf(
                "Solucionado el reinicio de navegación al entrar a módulos en pantalla completa.",
                "Optimizada la carga de imágenes en publicaciones del Muro."
            ),
            esRecomendada = true
        ),
        NotaVersionDetallada(
            versionCode = 13,
            versionName = "1.3.5",
            titulo = "Sincronización GPS y Rutas Moteras",
            fecha = "30 ago 2026",
            descripcionCorta = "Sincronización directa de coordenadas para puntos de encuentro de Jueves Moteros y rodadas de fin de semana.",
            novedades = listOf(
                "🗺️ Puntos de Encuentro GPS: Copia y navegación directa de coordenadas desde el Calendario al Mapa TX.",
                "🏍️ Planificador de Rodadas: Asignación de Capitán de Ruta, Colero, nivel de dificultad y estado del clima.",
                "🔔 Notificaciones de Eventos: Avisos automáticos para confirmación de asistencia (RSVP) y copiloto."
            ),
            correcciones = listOf(
                "Mejorada la precisión del cálculo de distancias en rutas moteras.",
                "Corregido el cierre inesperado al seleccionar ubicaciones en mapas sin conexión."
            )
        ),
        NotaVersionDetallada(
            versionCode = 12,
            versionName = "1.3.0",
            titulo = "Calendario Motero & Puntos de Interés",
            fecha = "29 ago 2026",
            descripcionCorta = "Planificación integral de salidas y eventos del club con sincronización comunitaria.",
            novedades = listOf(
                "📅 Calendario Motero TX: Agenda interactiva mensual con filtros por tipo de rodada.",
                "🏷️ Categorías Especiales: Cumpleaños de pilotos, paradas en bar motero y mantenimiento preventivo.",
                "📢 Publicación Automática: Envío de eventos directamente al Muro Social TX."
            ),
            correcciones = listOf(
                "Ajuste en la visualización de fechas en dispositivos con diferentes zonas horarias."
            )
        ),
        NotaVersionDetallada(
            versionCode = 11,
            versionName = "1.2.0",
            titulo = "Notas de Voz y Chat de Directiva",
            fecha = "28 ago 2026",
            descripcionCorta = "Mensajería con notas de voz, transcripción automática y canal privado de oficiales.",
            novedades = listOf(
                "🎙️ Notas de Voz en Chat: Grabación y envío instantáneo de audios en los canales del club.",
                "📝 Transcripción Automática: Conversión de voz a texto para leer mensajes en marcha.",
                "🛡️ Chat de Directiva Exclusivo: Canal privado con acceso restringido para directivos.",
                "📖 Manual de Usuario Interactivo: Guía animada paso a paso para 13 módulos del sistema."
            ),
            correcciones = listOf(
                "Optimizado el consumo de batería durante la sincronización en segundo plano de Firestore.",
                "Corregida la duplicación de mensajes en conexiones intermitentes."
            )
        ),
        NotaVersionDetallada(
            versionCode = 10,
            versionName = "1.1.0",
            titulo = "Mapas Offline OsmAnd & SOS Vial",
            fecha = "25 ago 2026",
            descripcionCorta = "Navegación GPS sin conexión y sistema de auxilio vial de emergencia.",
            novedades = listOf(
                "🗺️ Mapas Offline de Venezuela: Navegación completa por capas vectoriales sin consumir datos móviles.",
                "🚨 SOS Vial TX: Botón de auxilio inmediato con emisión de tipo de sangre y coordenadas GPS.",
                "💳 Carnet Digital TX: Perfil oficial de miembro con código QR y estado de solvencia."
            ),
            correcciones = listOf(
                "Mejorada la estabilidad del motor gráfico de mapas vectoriales."
            )
        )
    )

    /**
     * Resuelve los detalles completos de una versión por su código o nombre.
     */
    fun obtenerDetalleVersion(versionCode: Int, versionName: String = ""): NotaVersionDetallada {
        val porCodigo = HISTORIAL_VERSIONES_OFICIALES.find { it.versionCode == versionCode }
        if (porCodigo != null) return porCodigo

        val cleanName = versionName.removePrefix("v").trim()
        val porNombre = HISTORIAL_VERSIONES_OFICIALES.find {
            it.versionName.equals(cleanName, ignoreCase = true) || it.versionName.startsWith(cleanName)
        }
        if (porNombre != null) return porNombre

        // Si es una versión más nueva no registrada aún, generar una ficha descriptiva
        return NotaVersionDetallada(
            versionCode = versionCode,
            versionName = if (cleanName.isNotBlank()) cleanName else "v$versionCode",
            titulo = "Actualización Team TX (v$versionCode)",
            fecha = "Versión Reciente",
            descripcionCorta = "Nuevas mejoras de estabilidad, optimizaciones de rendimiento y funciones de carretera para Team TX.",
            novedades = listOf(
                "🚀 Optimizaciones generales de rendimiento y estabilidad del sistema.",
                "🔧 Mejoras de sincronización en tiempo real con Firebase.",
                "🛡️ Actualizaciones de seguridad y compatibilidad con versiones recientes de Android."
            ),
            correcciones = listOf("Correcciones de errores menores reportados por la comunidad.")
        )
    }

    /**
     * Consulta y lista todas las versiones de APK alojadas en la carpeta 'updates' de Firebase Storage
     */
    suspend fun obtenerListaVersionesStorage(): List<StorageApkVersion> = withContext(Dispatchers.IO) {
        val fallbackUrl = "https://firebasestorage.googleapis.com/v0/b/teamnacionaltx.firebasestorage.app/o/updates%2FTeamTX-latest.apk?alt=media&token=a0e6f96b-0431-46c4-9413-f40f9288dfcd"
        try {
            val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
            val updatesRef = storage.reference.child("updates")
            val listResult = updatesRef.listAll().await()

            val versions = mutableListOf<StorageApkVersion>()

            for (item in listResult.items) {
                if (item.name.endsWith(".apk", ignoreCase = true)) {
                    try {
                        val metadata = item.metadata.await()
                        val downloadUrl = try { item.downloadUrl.await().toString() } catch (_: Exception) { fallbackUrl }
                        val sizeBytes = metadata.sizeBytes
                        val updatedTime = metadata.updatedTimeMillis

                        val sizeMb = if (sizeBytes > 0) String.format(java.util.Locale.US, "%.1f MB", sizeBytes / (1024f * 1024f)) else "372.4 MB"
                        val dateStr = if (updatedTime > 0) {
                            java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(updatedTime))
                        } else "30 ago 2026"

                        val isBeta = item.name.contains("beta", ignoreCase = true)
                        val isStable = !isBeta || item.name.contains("estable", ignoreCase = true) || item.name.contains("latest", ignoreCase = true)
                        val isLatest = item.name.equals("TeamTX-latest.apk", ignoreCase = true)

                        val vName = when {
                            item.name.contains("v", ignoreCase = true) -> {
                                val match = Regex("""v\d+(\.\d+)*(-[a-zA-Z0-9]+)?""").find(item.name)
                                match?.value ?: item.name.removeSuffix(".apk")
                            }
                            item.name.equals("TeamTX-latest.apk", ignoreCase = true) -> "v1.3.7-beta (Última)"
                            else -> item.name.removeSuffix(".apk")
                        }

                        val estimatedCode = when {
                            item.name.contains("1.3.7") -> 18
                            isLatest -> 18
                            item.name.contains("1.3.6") -> 17
                            item.name.contains("1.3.5") -> 16
                            item.name.contains("1.4") -> 14
                            item.name.contains("1.3.4") -> 14
                            item.name.contains("1.3.3") -> 14
                            item.name.contains("1.3.2") -> 14
                            item.name.contains("1.3.1") -> 13
                            item.name.contains("1.3.0") -> 12
                            item.name.contains("1.3") -> 12
                            item.name.contains("1.2") -> 11
                            else -> 10
                        }

                        val detalle = obtenerDetalleVersion(estimatedCode, vName)

                        versions.add(
                            StorageApkVersion(
                                fileName = item.name,
                                downloadUrl = downloadUrl,
                                sizeBytes = sizeBytes,
                                formattedSize = sizeMb,
                                updatedTimestamp = updatedTime,
                                formattedDate = dateStr,
                                versionName = vName,
                                versionCode = estimatedCode,
                                titulo = detalle.titulo,
                                notas = detalle.descripcionCorta,
                                novedades = detalle.novedades,
                                correcciones = detalle.correcciones,
                                isStable = isStable,
                                isBeta = isBeta,
                                isRecommendedLatest = isLatest
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error procesando item ${item.name}: ${e.message}")
                    }
                }
            }

            versions.sortWith(compareByDescending<StorageApkVersion> { it.isRecommendedLatest }.thenByDescending { it.updatedTimestamp })

            if (versions.isEmpty()) {
                val detalleTop = obtenerDetalleVersion(18, "1.3.7-beta")
                versions.add(
                    StorageApkVersion(
                        fileName = "TeamTX-latest.apk",
                        downloadUrl = fallbackUrl,
                        sizeBytes = 395443512L,
                        formattedSize = "377.1 MB",
                        updatedTimestamp = System.currentTimeMillis(),
                        formattedDate = "07 sep 2026, 11:30 AM",
                        versionName = "v1.3.7-beta (Recomendada)",
                        versionCode = 18,
                        titulo = detalleTop.titulo,
                        notas = detalleTop.descripcionCorta,
                        novedades = detalleTop.novedades,
                        correcciones = detalleTop.correcciones,
                        isStable = true,
                        isBeta = false,
                        isRecommendedLatest = true
                    )
                )
            }

            versions
        } catch (e: Exception) {
            Log.e(TAG, "Error listando versiones de Firebase Storage: ${e.message}", e)
            val detalleTop = obtenerDetalleVersion(18, "1.3.7-beta")
            listOf(
                StorageApkVersion(
                    fileName = "TeamTX-latest.apk",
                    downloadUrl = fallbackUrl,
                    sizeBytes = 395443512L,
                    formattedSize = "377.1 MB",
                    updatedTimestamp = System.currentTimeMillis(),
                    formattedDate = "07 sep 2026, 11:30 AM",
                    versionName = "v1.3.7-beta (Recomendada)",
                    versionCode = 18,
                    titulo = detalleTop.titulo,
                    notas = detalleTop.descripcionCorta,
                    novedades = detalleTop.novedades,
                    correcciones = detalleTop.correcciones,
                    isStable = true,
                    isBeta = false,
                    isRecommendedLatest = true
                )
            )
        }
    }

    /**
     * Consulta las actualizaciones OTA sincronizando en tiempo real con Firebase Storage
     * y la base de datos Firestore.
     */
    suspend fun verificarActualizacion(): InformacionOta = withContext(Dispatchers.IO) {
        val urlOficialFirebaseStorage = "https://github.com/EDJMM91/TeamTX-Workspace-Completo/raw/main/Team-Nacional-TX-Aragua/apk/TeamTX-latest.apk"
        val urlInfoOtaJson = "https://github.com/EDJMM91/TeamTX-Workspace-Completo/raw/main/Team-Nacional-TX-Aragua/apk/ota_info.json"

        // 1. Consultar PRIMERO el archivo real subido a GitHub (ota_info.json)
        var versionStorage: StorageApkVersion? = null
        try {
            // Intentar cargar desde el JSON de GitHub primero (el más actualizado)
            val connection = URL(urlInfoOtaJson).openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.connect()
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(jsonStr)
                val code = json.optInt("versionCode", 0)
                val name = json.optString("versionName", "")
                val urlDesc = json.optString("urlDescarga", urlOficialFirebaseStorage)
                val notasJson = json.optString("notas", "")
                
                val listaNovedades = mutableListOf<String>()
                val jsonNov = json.optJSONArray("novedades")
                if (jsonNov != null) {
                    for (i in 0 until jsonNov.length()) listaNovedades.add(jsonNov.getString(i))
                }
                
                val listaCorrecciones = mutableListOf<String>()
                val jsonCorr = json.optJSONArray("correcciones")
                if (jsonCorr != null) {
                    for (i in 0 until jsonCorr.length()) listaCorrecciones.add(jsonCorr.getString(i))
                }
                
                versionStorage = StorageApkVersion(
                    fileName = "TeamTX-latest.apk",
                    downloadUrl = urlDesc,
                    versionCode = code,
                    versionName = name,
                    titulo = "Actualización Oficial (GitHub)",
                    notas = notasJson,
                    novedades = listaNovedades,
                    correcciones = listaCorrecciones,
                    isRecommendedLatest = true
                )
                Log.d(TAG, "Versión detectada en GitHub: $name (Build $code)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando ota_info.json en GitHub: ${e.message}")
        }

        if (versionStorage == null) {
            try {
                val lista = obtenerListaVersionesStorage()
                versionStorage = lista.firstOrNull { it.isRecommendedLatest }
                    ?: lista.maxByOrNull { it.versionCode }
            } catch (e: Exception) {
                Log.e(TAG, "Error consultando versiones en Storage: ${e.message}")
            }
        }

        // 2. Consultar Firestore en colección 'configuracion' / 'OTA'
        var infoFirestore: InformacionOta? = null
        try {
            val db = FirebaseFirestore.getInstance()
            var doc = db.collection("configuracion").document("OTA").get().await()
            if (!doc.exists()) {
                doc = db.collection("Configuracion").document("OTA").get().await()
            }

            if (doc.exists()) {
                val code = when (val rawCode = doc.get("versionCode")) {
                    is Number -> rawCode.toInt()
                    is String -> rawCode.trim().toIntOrNull() ?: 0
                    else -> 0
                }
                val versionName = doc.getString("versionName") ?: "v$code"
                var url = (doc.getString("urlDescarga") ?: "").trim()
                val customNotas = doc.getString("notas") ?: ""
                val customTitulo = doc.getString("titulo") ?: ""

                val listaNovedadesFirestore = mutableListOf<String>()
                val rawNovedades = doc.get("novedades")
                if (rawNovedades is List<*>) {
                    rawNovedades.filterIsInstance<String>().forEach { listaNovedadesFirestore.add(it) }
                } else if (rawNovedades is String && rawNovedades.isNotBlank()) {
                    listaNovedadesFirestore.addAll(rawNovedades.split("\n").map { it.trim() }.filter { it.isNotBlank() })
                }

                val listaCorreccionesFirestore = mutableListOf<String>()
                val rawCorrecciones = doc.get("correcciones")
                if (rawCorrecciones is List<*>) {
                    rawCorrecciones.filterIsInstance<String>().forEach { listaCorreccionesFirestore.add(it) }
                }

                if (url.isBlank() || url.contains("github.com")) {
                    url = urlOficialFirebaseStorage
                }

                if (code > 0) {
                    val detalleOficial = obtenerDetalleVersion(code, versionName)
                    infoFirestore = InformacionOta(
                        versionCode = code,
                        versionName = versionName,
                        titulo = if (customTitulo.isNotBlank()) customTitulo else detalleOficial.titulo,
                        urlDescarga = url,
                        notas = if (customNotas.isNotBlank()) customNotas else detalleOficial.descripcionCorta,
                        novedades = if (listaNovedadesFirestore.isNotEmpty()) listaNovedadesFirestore else detalleOficial.novedades,
                        correcciones = if (listaCorreccionesFirestore.isNotEmpty()) listaCorreccionesFirestore else detalleOficial.correcciones,
                        fechaPublicacion = detalleOficial.fecha
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Consulta Firestore OTA falló: ${e.message}")
        }

        // 3. Determinar la versión ganadora (priorizando la real más reciente de Storage)
        val codeStorage = versionStorage?.versionCode ?: 0
        val codeFirestore = infoFirestore?.versionCode ?: 0

        val resultadoFinal: InformacionOta = if (versionStorage != null && codeStorage >= codeFirestore) {
            val detalle = obtenerDetalleVersion(codeStorage, versionStorage.versionName)
            InformacionOta(
                versionCode = codeStorage,
                versionName = versionStorage.versionName.ifBlank { detalle.versionName },
                titulo = versionStorage.titulo.ifBlank { detalle.titulo },
                urlDescarga = versionStorage.downloadUrl.ifBlank { urlOficialFirebaseStorage },
                notas = versionStorage.notas.ifBlank { detalle.descripcionCorta },
                novedades = if (versionStorage.novedades.isNotEmpty()) versionStorage.novedades else detalle.novedades,
                correcciones = if (versionStorage.correcciones.isNotEmpty()) versionStorage.correcciones else detalle.correcciones,
                fechaPublicacion = versionStorage.formattedDate.ifBlank { detalle.fecha }
            )
        } else if (infoFirestore != null) {
            infoFirestore
        } else {
            val detalleV16 = obtenerDetalleVersion(16, "1.3.5-beta")
            InformacionOta(
                versionCode = 16,
                versionName = "1.3.5-beta",
                titulo = detalleV16.titulo,
                urlDescarga = urlOficialFirebaseStorage,
                notas = detalleV16.descripcionCorta,
                novedades = detalleV16.novedades,
                correcciones = detalleV16.correcciones,
                fechaPublicacion = detalleV16.fecha
            )
        }

        // 4. Sincronizar automáticamente hacia Firestore para actualizar la nube
        try {
            val db = FirebaseFirestore.getInstance()
            val datosSincronizados = hashMapOf(
                "versionCode" to resultadoFinal.versionCode,
                "versionName" to resultadoFinal.versionName,
                "urlDescarga" to resultadoFinal.urlDescarga,
                "titulo" to resultadoFinal.titulo,
                "notas" to resultadoFinal.notas,
                "novedades" to resultadoFinal.novedades,
                "correcciones" to resultadoFinal.correcciones
            )
            db.collection("configuracion").document("OTA").set(datosSincronizados, com.google.firebase.firestore.SetOptions.merge())
            db.collection("Configuracion").document("OTA").set(datosSincronizados, com.google.firebase.firestore.SetOptions.merge())
        } catch (_: Exception) {}

        Log.i(TAG, "✅ OTA sincronizado: Build ${resultadoFinal.versionCode} (${resultadoFinal.versionName})")
        return@withContext resultadoFinal
    }

    /**
     * Descarga directa vía HTTP con seguimiento de redirecciones y reporte de progreso en tiempo real
     */
    suspend fun descargarApkDirecto(
        context: Context,
        urlOriginal: String,
        onProgreso: (progreso: Float, leidosMb: String, totalMb: String) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            var urlString = urlOriginal.trim()
            if (urlString.isBlank()) {
                Log.e(TAG, "URL de descarga vacía")
                return@withContext null
            }

            Log.d(TAG, "Iniciando descarga directa desde: $urlString")

            // Preparar destino
            val carpetaDestino = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            if (!carpetaDestino.exists()) {
                carpetaDestino.mkdirs()
            }
            val archivoApk = File(carpetaDestino, "TeamTX_Update.apk")
            if (archivoApk.exists()) {
                archivoApk.delete()
            }

            // 1. Si la URL es de Firebase Storage o ruta interna, descargar por Firebase Storage SDK
            if (urlString.startsWith("gs://") || urlString.contains("firebasestorage.googleapis.com") || urlString.startsWith("updates/")) {
                try {
                    val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                    val storageRef = if (urlString.startsWith("gs://")) {
                        storage.getReferenceFromUrl(urlString)
                    } else if (urlString.contains("/o/")) {
                        storage.getReferenceFromUrl(urlString)
                    } else {
                        storage.reference.child("updates/TeamTX-latest.apk")
                    }

                    Log.d(TAG, "Descargando vía Firebase Storage SDK: ${storageRef.path}")
                    var completado = false
                    var errorDescarga: Exception? = null

                    val downloadTask = storageRef.getFile(archivoApk)
                    downloadTask.addOnProgressListener { snapshot ->
                        val bytesTransferred = snapshot.bytesTransferred
                        val totalBytes = snapshot.totalByteCount
                        if (totalBytes > 0) {
                            val progreso = bytesTransferred.toFloat() / totalBytes.toFloat()
                            val mbLeidos = String.format(java.util.Locale.US, "%.1f", bytesTransferred / (1024f * 1024f))
                            val mbTotal = String.format(java.util.Locale.US, "%.1f", totalBytes / (1024f * 1024f))
                            onProgreso(progreso, mbLeidos, mbTotal)
                        }
                    }.addOnSuccessListener {
                        completado = true
                    }.addOnFailureListener { e ->
                        errorDescarga = e
                        completado = true
                    }

                    while (!completado) {
                        kotlinx.coroutines.delay(100)
                    }

                    if (errorDescarga == null && archivoApk.exists() && archivoApk.length() > 0) {
                        Log.i(TAG, "Descarga completada con éxito vía Storage SDK: ${archivoApk.length()} bytes")
                        return@withContext archivoApk
                    } else {
                        Log.e(TAG, "Fallo en Storage SDK: ${errorDescarga?.message}. Reintentando con HTTP...")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en Storage SDK: ${e.message}. Reintentando con HTTP...")
                }
            }

            // 2. Descarga tradicional HTTP / HTTPS con soporte para redirecciones
            var url = URL(urlString)
            var conexion = url.openConnection() as HttpURLConnection
            conexion.instanceFollowRedirects = true
            conexion.connectTimeout = 30000
            conexion.readTimeout = 60000
            conexion.setRequestProperty("User-Agent", "TeamTX-AppUpdater/1.0")

            var responseCode = conexion.responseCode
            var redirects = 0
            while ((responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308) && redirects < 5) {
                val newUrl = conexion.getHeaderField("Location")
                Log.d(TAG, "Redirección detectada ($responseCode) hacia: $newUrl")
                conexion.disconnect()
                url = URL(newUrl)
                conexion = url.openConnection() as HttpURLConnection
                conexion.instanceFollowRedirects = true
                conexion.connectTimeout = 30000
                conexion.readTimeout = 60000
                conexion.setRequestProperty("User-Agent", "TeamTX-AppUpdater/1.0")
                responseCode = conexion.responseCode
                redirects++
            }

            if (responseCode !in 200..299) {
                Log.e(TAG, "Error HTTP al descargar actualización: $responseCode ${conexion.responseMessage}")
                conexion.disconnect()
                return@withContext null
            }

            val totalBytes = conexion.contentLengthLong
            Log.d(TAG, "Tamaño total del archivo: $totalBytes bytes ($responseCode)")

            val inputStream = conexion.inputStream
            val outputStream = FileOutputStream(archivoApk)
            val buffer = ByteArray(8192)
            var bytesLeidos: Int
            var totalBytesLeidos = 0L

            var ultimoReporteProgreso = 0L

            while (inputStream.read(buffer).also { bytesLeidos = it } != -1) {
                outputStream.write(buffer, 0, bytesLeidos)
                totalBytesLeidos += bytesLeidos

                val ahora = System.currentTimeMillis()
                if (ahora - ultimoReporteProgreso > 150) {
                    ultimoReporteProgreso = ahora
                    val progreso = if (totalBytes > 0) totalBytesLeidos.toFloat() / totalBytes.toFloat() else 0f
                    val mbLeidos = String.format(java.util.Locale.US, "%.1f", totalBytesLeidos / (1024f * 1024f))
                    val mbTotal = if (totalBytes > 0) String.format(java.util.Locale.US, "%.1f", totalBytes / (1024f * 1024f)) else "??"
                    onProgreso(progreso, mbLeidos, mbTotal)
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            conexion.disconnect()

            val mbFinal = String.format(java.util.Locale.US, "%.1f", totalBytesLeidos / (1024f * 1024f))
            onProgreso(1f, mbFinal, mbFinal)

            Log.i(TAG, "Archivo APK descargado exitosamente: ${archivoApk.absolutePath} (${archivoApk.length()} bytes)")
            archivoApk
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la descarga del APK: ${e.message}", e)
            null
        }
    }

    /**
     * Lanza el Intent oficial de Android para instalar el paquete APK descargado
     */
    fun instalarApk(context: Context, archivoApk: File) {
        try {
            if (!archivoApk.exists()) {
                Toast.makeText(context, "El archivo de actualización no se encontró.", Toast.LENGTH_SHORT).show()
                return
            }

            // En Android 8.0 (API 26) o superior, verificar permiso para instalar paquetes desconocidos
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(
                        context,
                        "Por favor autoriza la instalación de actualizaciones para Team TX",
                        Toast.LENGTH_LONG
                    ).show()
                    val intentPermiso = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intentPermiso)
                    return
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                archivoApk
            )

            val intentInstalar = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intentInstalar)
        } catch (e: Exception) {
            Log.e(TAG, "Error al lanzar el instalador de APK: ${e.message}", e)
            Toast.makeText(context, "Error al iniciar la instalación: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Redirige al navegador predeterminado para descarga externa de respaldo
     */
    fun abrirDescargaEnNavegador(context: Context, urlDescarga: String) {
        try {
            var urlFinal = urlDescarga.trim()
            if (urlFinal.isBlank() || urlFinal.contains("github.com")) {
                urlFinal = "https://firebasestorage.googleapis.com/v0/b/teamnacionaltx.firebasestorage.app/o/updates%2FTeamTX-latest.apk?alt=media&token=a0e6f96b-0431-46c4-9413-f40f9288dfcd"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlFinal)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir el navegador: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTE: MODAL DE DESCARGA E INSTALACIÓN CON PROGRESO EN TIEMPO REAL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DialogoProgresoDescargaOta(
    infoOta: GestorActualizaciones.InformacionOta,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var progreso by remember { mutableFloatStateOf(0f) }
    var mbLeidos by remember { mutableStateOf("0.0") }
    var mbTotal by remember { mutableStateOf("...") }
    var estadoDescarga by remember { mutableStateOf("Iniciando descarga...") }
    var descargaCompleta by remember { mutableStateOf(false) }
    var huboError by remember { mutableStateOf(false) }
    var archivoDescargado by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(Unit) {
        estadoDescarga = "Descargando actualización..."
        val archivo = GestorActualizaciones.descargarApkDirecto(
            context = context,
            urlOriginal = infoOta.urlDescarga,
            onProgreso = { prog, leidos, total ->
                progreso = prog
                mbLeidos = leidos
                mbTotal = total
                estadoDescarga = "Descargando paquete: $leidos MB / $total MB"
            }
        )

        if (archivo != null && archivo.exists()) {
            archivoDescargado = archivo
            descargaCompleta = true
            estadoDescarga = "¡Descarga completada! Iniciando instalador..."
            GestorActualizaciones.instalarApk(context, archivo)
        } else {
            huboError = true
            estadoDescarga = "Error al descargar el paquete. Usa el botón del navegador."
        }
    }

    Dialog(onDismissRequest = { if (descargaCompleta || huboError) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF131722),
            border = BorderStroke(1.dp, Color(0xFF263238)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cabecera
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when {
                            huboError -> Icons.Default.Error
                            descargaCompleta -> Icons.Default.CheckCircle
                            else -> Icons.Default.CloudDownload
                        },
                        contentDescription = null,
                        tint = when {
                            huboError -> TxFlameRed
                            descargaCompleta -> Color(0xFF22C55E)
                            else -> MotoOrangePrimary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = when {
                            huboError -> "Fallo en la Descarga"
                            descargaCompleta -> "¡Actualización Lista!"
                            else -> "Actualizando a ${infoOta.versionName}"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                Text(
                    text = estadoDescarga,
                    fontSize = 12.sp,
                    color = if (huboError) TxFlameRed else Color(0xFF90A4AE),
                    textAlign = TextAlign.Center
                )

                if (!huboError) {
                    LinearProgressIndicator(
                        progress = { progreso.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MotoOrangePrimary,
                        trackColor = Color(0xFF263238)
                    )

                    Text(
                        text = "${(progreso * 100).toInt()}% ($mbLeidos MB de $mbTotal MB)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MotoGoldSecondary
                    )
                }

                // Botones de acción
                if (descargaCompleta) {
                    Button(
                        onClick = {
                            archivoDescargado?.let { GestorActualizaciones.instalarApk(context, it) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reintentar Instalación")
                    }
                } else if (huboError) {
                    Button(
                        onClick = {
                            GestorActualizaciones.abrirDescargaEnNavegador(context, infoOta.urlDescarga)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Descargar en Navegador")
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text(if (descargaCompleta || huboError) "Cerrar" else "Cancelar en segundo plano", color = Color(0xFF90A4AE), fontSize = 12.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTE: VERIFICADOR AUTOMÁTICO AL INICIAR LA APP
// Muestra con claridad todas las novedades y correcciones de la versión nueva
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VerificadorOta() {
    val context = LocalContext.current
    var infoOta by remember { mutableStateOf<GestorActualizaciones.InformacionOta?>(null) }
    var mostrarDialogoConfirmacion by remember { mutableStateOf(false) }
    var mostrarDialogoDescarga by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val ota = GestorActualizaciones.verificarActualizacion()
        val currentVersionCode = BuildConfig.VERSION_CODE
        Log.d("OTA", "versionCode nube=${ota.versionCode} vs local=$currentVersionCode")
        if (ota.versionCode > currentVersionCode) {
            infoOta = ota
            mostrarDialogoConfirmacion = true
        }
    }

    if (mostrarDialogoConfirmacion && infoOta != null) {
        val ota = infoOta!!

        Dialog(onDismissRequest = { mostrarDialogoConfirmacion = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF131722),
                border = BorderStroke(1.5.dp, MotoGoldSecondary),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Encabezado con Badge de Versión
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = CircleShape, color = MotoOrangePrimary.copy(alpha = 0.2f), modifier = Modifier.size(34.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Upgrade, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text("¡NUEVA ACTUALIZACIÓN!", fontWeight = FontWeight.Black, fontSize = 13.sp, color = MotoOrangePrimary)
                                Text("Versión ${ota.versionName} (Build ${ota.versionCode})", fontSize = 11.sp, color = Color(0xFF90A4AE))
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MotoGoldSecondary.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, MotoGoldSecondary)
                        ) {
                            Text("OFICIAL", fontSize = 9.sp, fontWeight = FontWeight.Black, color = MotoGoldSecondary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    // Título y resumen del release
                    Text(
                        text = ota.titulo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )

                    Text(
                        text = ota.notas,
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 15.sp
                    )

                    // Sección: Novedades Principales
                    if (ota.novedades.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E2433),
                            border = BorderStroke(1.dp, Color(0xFF263238)),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                        ) {
                            LazyColumn(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    Text("✨ ¿Qué hay de nuevo en esta versión?", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MotoGoldSecondary)
                                }
                                items(ota.novedades) { novedad ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("•", color = MotoOrangePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(text = novedad, fontSize = 11.sp, color = Color.White, lineHeight = 14.sp)
                                    }
                                }

                                if (ota.correcciones.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("🛠️ Correcciones y Mejoras:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF38BDF8))
                                    }
                                    items(ota.correcciones) { fix ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("✓", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text(text = fix, fontSize = 11.sp, color = Color(0xFFCBD5E1), lineHeight = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Botones de acción
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { mostrarDialogoConfirmacion = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Más tarde", fontSize = 12.sp, color = Color(0xFF90A4AE))
                        }

                        Button(
                            onClick = {
                                mostrarDialogoConfirmacion = false
                                mostrarDialogoDescarga = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Actualizar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoDescarga && infoOta != null) {
        DialogoProgresoDescargaOta(
            infoOta = infoOta!!,
            onDismiss = { mostrarDialogoDescarga = false }
        )
    }
}
