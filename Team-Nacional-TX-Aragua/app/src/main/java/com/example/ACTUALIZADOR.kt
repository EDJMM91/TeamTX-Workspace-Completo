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
            versionCode = 45,
            versionName = "1.8.3-beta",
            titulo = "Tarjetas de Pilotos Minimizadas a Fila Única y Desplegables",
            fecha = "11 sep 2026",
            descripcionCorta = "Rediseño compacto de las tarjetas de miembros en el directorio de pilotos, iniciando en una sola línea minimizada con avatar, ficha, apodo y rol, expandiéndose suavemente al tocar.",
            novedades = listOf(
                "🪪 Tarjetas Minimizadas de Pilotos: Fila compacta de una sola línea por piloto mostrando foto circular con LED online, número de ficha (TX-001), apodo y badge de rango.",
                "📐 Expansión Suave Interactiva: Al tocar la tarjeta o pestaña de expansión, se despliega la ficha técnica completa con datos de la moto, tipo de sangre, solvencia y botones tácticos de WhatsApp, Llamada y Chat Privado.",
                "📊 Optimización de Espacio en Pantalla: Permite visualizar múltiples pilotos de un vistazo rápido sin consumo excesivo de espacio vertical."
            ),
            correcciones = listOf(
                "Corregido: Consumo excesivo de espacio en la lista del directorio de miembros/pilotos."
            ),
            esRecomendada = true
        ),
        NotaVersionDetallada(
            versionCode = 44,
            versionName = "1.8.2-beta",
            titulo = "Unificación del Formulario DialogoCrearPuntoTX en Directorio Global y Mapeo Doble",
            fecha = "11 sep 2026",
            descripcionCorta = "Invocación unificada del formulario DialogoCrearPuntoTX tanto desde el Mapa TX como desde la pantalla del Directorio Global, con enrutamiento automático hacia comercios o sitios de interés.",
            novedades = listOf(
                "➕ Formulario Unificado DialogoCrearPuntoTX: Presionar 'Registrar Punto / Comercio' en el Directorio Global abre exactamente el mismo formulario del mapa con cámara/galería, íconos tácticos, Cashea y categorías de ruta.",
                "🗂️ Enrutamiento Inteligente Doble: Clasificación automática a 'workshops_directory' o 'sitios_interes' en Firestore y Room DB según la categoría seleccionada.",
                "🌐 Coexistencia Total de Bases de Datos: Mantenimiento 100% libre de errores de los datos comerciales históricos y los nuevos destinos turísticos."
            ),
            correcciones = listOf(
                "Corregido: Unificación de formularios entre el Mapa TX y el módulo Directorio Global."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 43,
            versionName = "1.8.1-beta",
            titulo = "Directorio Global TX Unificado con Icono Explícito de Mapa y Puntos Turísticos",
            fecha = "10 sep 2026",
            descripcionCorta = "Unificación oficial del módulo bajo el nombre 'Directorio Global TX', icono alusivo a mapa/exploración (Icons.Default.Explore) en el Dashboard y registro de puntos unificado desde la app o mapa.",
            novedades = listOf(
                "🌐 Directorio Global TX Unificado: Módulo oficial renombrado a 'Directorio Global TX' en la barra superior, Bento Grid y catálogo general del Dashboard.",
                "🧭 Icono Alusivo a Mapa y Directorio: Iconografía oficial de exploración/mapa (`Icons.Default.Explore`) en todas las tarjetas del Dashboard.",
                "➕ Registro Unificado de Puntos y Comercios: Formulario 'Registrar Punto de Interés / Comercio' unificado para la base de datos de la plataforma."
            ),
            correcciones = listOf(
                "Corregido: Eliminación de botones duplicados en la barra superior del módulo.",
                "Corregido: Nomenclatura del módulo en la navegación principal."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 42,
            versionName = "1.8.0-beta",
            titulo = "Capa de Mapa Táctico Sitios de Interés TX, Captura de Fotos, Navegación GPS por Voz y Moderación de Denuncias",
            fecha = "10 sep 2026",
            descripcionCorta = "Renderizado completo en tiempo real de sitios turísticos y paradores en el Mapa TX, captura de fotos por cámara/galería, inyección automática de rutas guiadas por voz en OsmAnd, acciones de WhatsApp/Llamar y moderación de denuncias en Directiva.",
            novedades = listOf(
                "🏕️ Capa de Mapa Sitios de Interés TX: Visualización de marcadores e iconos personalizados para miradores, playas, cascadas y paradores biker en el mapa con botón toggle.",
                "📸 Captura de Foto con Cámara y Galería: Selector en el formulario para tomar fotos con la cámara o elegir de la galería con resguardo atómico en nube.",
                "🚀 Navegación Nativa por Voz: Tocar 'Navegar con GPS' en cualquier ficha in-map inyecta el destino e inicia la ruta por voz automáticamente en OsmAnd.",
                "💬 Acciones Tácticas Directas: Integración de botones directos de WhatsApp y Llamadas telefónicas en todas las fichas del mapa.",
                "🚩 Moderación de Denuncias para Directiva: Tarjeta 10 en el Hub de Directiva para auditar reportes, editar o eliminar sitios en tiempo real."
            ),
            correcciones = listOf(
                "Corregido: Falta de renderizado directo de puntos de interés turístico en el mapa.",
                "Corregido: Apertura fluida de fichas in-map al tocar marcadores turísticos."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 41,
            versionName = "1.7.2-beta",
            titulo = "Módulo Unificado Directorio & Puntos TX, Tarjetas Colapsables y Botones sin Recortes",
            fecha = "10 sep 2026",
            descripcionCorta = "Módulo unificado Directorio Comercial & Puntos TX con tarjetas colapsables por defecto para optimizar espacio, botones de acción sin recorte de texto y garantía de preservación total de la base de datos.",
            novedades = listOf(
                "🏬 Módulo Unificado 'Directorio Comercial & Puntos TX': Unificación en el Dashboard para explorar comercios, talleres, repuestos, miradores y destinos turísticos.",
                "📐 Tarjetas Colapsables Minimizadas: Tarjetas compactas por defecto mostrando únicamente nombre, tipo, ubicación y calificación ⭐ con pestaña de expansión.",
                "📐 Ajuste de Botones sin Recortes: Botones de WhatsApp, Llamar, Mapa TX y G-Maps reajustados para garantizar que el texto encaje 100% visible.",
                "🚀 Navegación Nativa OsmAnd: Tocar 'Mapa TX' inyecta el destino e inicia la navegación guiada por voz automáticamente."
            ),
            correcciones = listOf(
                "Corregido: Recorte de texto en botones de WhatsApp y Mapa TX en pantallas angostas.",
                "Corregido: Consumo excesivo de espacio vertical en el catálogo de comercios."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 40,
            versionName = "1.7.1-beta",
            titulo = "Etiqueta Verde de Categoría de Usuario, Eliminación de Scroll de Accesos y Pestaña Sidebar a la Izquierda",
            fecha = "10 sep 2026",
            descripcionCorta = "Eliminación de la palabra AUTENTICADO en la tarjeta superior reemplazada por PILOTO, COPILOTO o INVITADO en texto verde brillante, depuración del scroll horizontal de accesos rápidos y alineación de la pestaña Sidebar a la izquierda.",
            novedades = listOf(
                "🟢 Etiqueta Verde de Usuario: En la tarjeta superior se muestra dinámicamente PILOTO, COPILOTO o INVITADO en verde brillante (Color 0xFF00E676), eliminando la palabra AUTENTICADO.",
                "🧹 Eliminación de Scroll de Accesos: Depuración completa de la fila superior de scroll horizontal de accesos rápidos en la sección Mi Panel.",
                "⬅️ Pestaña Sidebar a la Izquierda: Alineación de la pestaña minimalista desplegable de Sidebar al extremo izquierdo para dar espacio a futuros controles tácticos."
            ),
            correcciones = listOf(
                "Corregido: Redundancia de scroll de accesos rápidos en la vista principal.",
                "Corregido: Alineación central de la pestaña de Sidebar."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 39,
            versionName = "1.7.0-beta",
            titulo = "Rediseño Nativo de Filas del Dashboard, Unificación de Perfil y Pestaña Táctica de Sidebar",
            fecha = "10 sep 2026",
            descripcionCorta = "Rediseño limpio de filas del catálogo sin recuadros pesados de fondo, unificación del avatar de perfil en Piloto Autenticado, botón de Ajustes ⚙️ arriba a la derecha y pestaña táctica minimalista de Sidebar.",
            novedades = listOf(
                "🎨 Rediseño Nativo de Filas: Filas limpias y elegantes en 'Todos los Módulos' sin marcos ni cajas pesadas de fondo, logrando un aspecto nativo y fluido.",
                "🪪 Unificación de Foto de Perfil: Avatar circular integrado en la tarjeta de Piloto Autenticado, permitiendo abrir el Carnet TX directamente.",
                "⚙️ Botón de Ajustes en Cabecera Superior: Reemplazo del botón duplicado por el icono de Engranaje de Ajustes y Configuración arriba a la derecha.",
                "📑 Pestaña Táctica de Sidebar: La barra de Sidebar se pliega en una pestaña/píldora minimalista sin texto de bajo perfil.",
                "🧹 Cero Módulos Repetidos: Depuración de accesos duplicados de Carnet TX y Ajustes en el catálogo inferior."
            ),
            correcciones = listOf(
                "Corregido: Aspecto de plantilla genérica con recuadros/marcos pesados detrás de cada módulo.",
                "Corregido: Redundancia de fotos de perfil y accesos duplicados en el Dashboard."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 38,
            versionName = "1.6.2-beta",
            titulo = "Navegación GPS Guiada Nativa por Voz, Selector de Iconos Sincronizado a Nube y Financiación Cashea",
            fecha = "10 sep 2026",
            descripcionCorta = "Inyección directa de rutas guiadas por voz en el motor OsmAnd al tocar 'Navegar GPS' en cualquier sitio, selector desplegable de iconos tácticos sincronizado con la nube y dibujado automático de la insignia Cashea con línea flotante en el mapa.",
            novedades = listOf(
                "🚀 Navegación Guiada Nativa por Voz: Tocar 'Navegar GPS' en cualquier tarjeta o disco informativo del mapa inyecta el destino en OsmAnd, dibuja la línea oficial e inicia la navegación por voz paso a paso.",
                "🎨 Selector de Iconos Tácticos Sincronizado: Desplegable en el formulario para seleccionar el icono del mapa (`logoteam`, `ic_menu_compass`, `ic_action_flag`, `ic_action_gas_station`, `ic_action_repair`, `ic_action_food`, `ic_action_hotel`, `ic_action_water`, `ic_action_mountain`), sincronizado con la nube para toda la flota.",
                "💳 Insignia Cashea Flotante en Mapa: Al marcar financiación Cashea en un comercio, el mapa dibuja automáticamente el logotipo `cashea.png` unida mediante una línea al disco del negocio.",
                "💬 Acciones Tácticas Directas: Botones de WhatsApp (`wa.me`) y Llamada Telefónica (`tel:`) integrados en las fichas del mapa y listas del módulo."
            ),
            correcciones = listOf(
                "Corregido: Falta de inicio automático de navegación guiada al seleccionar destino en el mapa.",
                "Corregido: Sincronización del icono táctico personalizado entre dispositivos."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 37,
            versionName = "1.6.1-beta",
            titulo = "Integración Directa de 'Agregar Punto TX' en Menú Contextual OsmAnd y Moderación en Directiva",
            fecha = "10 sep 2026",
            descripcionCorta = "Ubicación de '📍 Agregar Punto TX' con icono logoteam.png directamente dentro del menú principal contextual de long-press de OsmAnd (al lado de Agregar, Marcador, Compartir) y tarjeta 10 de Moderación en el Hub de Directiva.",
            novedades = listOf(
                "📍 Integración Directa en Menú OsmAnd: La opción '📍 Agregar Punto TX' con el logotipo oficial logoteam.png aparece justo al dejar presionado el mapa junto a 'Agregar', 'Marcador' y 'Compartir'.",
                "💻 Tarjeta 10 en Hub de Directiva: Tarjeta interactiva '10. Moderación de Puntos & Denuncias TX' visible en el panel principal de Directiva para el Desarrollador Máster.",
                "📡 Sincronización Inmediata Broadcast: Captura inmediata de coordenadas (lat, lon) enviadas por broadcast interno desde MapActivity hacia el ViewModel de la app."
            ),
            correcciones = listOf(
                "Corregido: Ubicación de la opción 'Agregar Punto TX' en el menú contextual de OsmAnd.",
                "Corregido: Visibilidad de la tarjeta de moderación en el Hub de Directiva."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 36,
            versionName = "1.6.0-beta",
            titulo = "Nuevo Módulo 'Sitios de Interés TX', Menú Contextual OsmAnd (logoteam.png), Denuncias, Likes/Dislikes y Moderación Desarrollador",
            fecha = "10 sep 2026",
            descripcionCorta = "Nuevo módulo Sitios de Interés TX para la base de datos de destinos turísticos y paradores biker, opción 'Agregar Punto TX' en el menú contextual del mapa con logoteam.png, doble guardado automático en Directorio Comercial o Sitios de Interés, sistema de Likes/Dislikes, denuncias de pilotos y panel de moderación para Desarrollador.",
            novedades = listOf(
                "📍 Menú Contextual OsmAnd con logoteam.png: Al dejar presionado cualquier punto en el Mapa TX, se integra la opción 'Agregar Punto TX' identificada con el logotipo oficial logoteam.png sin alterar las funciones nativas de OsmAnd.",
                "🏕️ Nuevo Módulo 'Sitios de Interés TX': Pantalla oficial para explorar miradores, playas, montañas, paradores biker y campings con navegación GPS paso a paso.",
                "📝 Categorización e Integración Doble: Los puntos comerciales (talleres, repuestos, posadas, restaurantes) se guardan automáticamente en el Directorio Comercial & Servicios, mientras los puntos turísticos se guardan en la colección sitios_interes.",
                "👍/👎 Likes, Dislikes y Denuncias Tácticas: Sistema de reputación y denuncias enviadas en tiempo real al canal de la Directiva.",
                "💻 Moderación de Desarrollador Máster: Apartado exclusivo en el panel de Gobernanza para auditar denuncias, editar información o eliminar sitios definitivamente."
            ),
            correcciones = listOf(
                "Corregido: Falta de integración de puntos de interés turístico en el mapa con persistencia local y nube."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 35,
            versionName = "1.5.4-beta",
            titulo = "Algoritmo Orbital Táctico (Discos sin Solapamiento) y Lista por Jerarquía de Roles In-Map",
            fecha = "10 sep 2026",
            descripcionCorta = "Implementación del algoritmo de dispersión orbital para evitar el solapamiento de avatares en el mismo punto GPS y despliegue de lista táctica de pilotos ordenada por rango institucional.",
            novedades = listOf(
                "🌀 Algoritmo Orbital Spiderifier: Dispersión circular automática de avatares cuando 2 o más pilotos están en un rango de 30m, conectados por líneas tácticas al centro GPS.",
                "👥 Lista Táctica Ordenada por Jerarquía: Al tocar un grupo de pilotos en el mapa, se despliega una lista ordenada por rango institucional (Desarrollador -> Presidente -> Directiva -> Capitán -> Disciplinario -> Miembro).",
                "📍 Toques Orbitales Directos: Detección precisa de toques en cada disco orbital individual para inspección fluida sin salir del mapa."
            ),
            correcciones = listOf(
                "Corregido: Solapamiento visual de avatares cuando dos pilotos están en la misma ubicación GPS.",
                "Corregido: Detección de toques en grupos de pilotos cercanos."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 34,
            versionName = "1.5.3-beta",
            titulo = "Fijación Inamovible de Estado Aprobado e Inspección In-Map Táctico sin Salir del Mapa",
            fecha = "10 sep 2026",
            descripcionCorta = "Fijación permanente de 'usuarioEstado = ACTIVO' para cuentas aprobadas eliminando el bucle de 'cuenta en evaluación' o 'cuenta verificada', y despliegue del Carnet TX in-map manteniendo el mapa 100% activo.",
            novedades = listOf(
                "🚀 Fijación de Estado Aprobado: Cuentas verificadas y aprobadas ingresan directamente a MainAppScreen sin parpadeos ni caídas a 'Cuenta en evaluación'.",
                "🗺️ Inspección In-Map Táctica: Tocar el marcador o disco de un piloto despliega su Carnet TX flotante directamente sobre el mapa sin cerrar MapActivity ni reiniciar la app.",
                "🔒 Cero Pantallas Redundantes: Las pantallas de evaluación y verificación inicial se muestran una sola vez en la vida de la cuenta al ser aprobada."
            ),
            correcciones = listOf(
                "Corregido: Cierre de la actividad del mapa al tocar el botón de inspección de un piloto.",
                "Corregido: Reaparición temporal de la pantalla de 'Cuenta en evaluación' al reanudar la app desde segundo plano o mapas."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 33,
            versionName = "1.5.2-beta",
            titulo = "Rol Desarrollador Inmutable, Eliminación Definitiva, Sincronización de Radar y Fix Mapa",
            fecha = "10 sep 2026",
            descripcionCorta = "Asignación inmutable del cargo Desarrollador Máster, designación exclusiva del cargo de Presidente por el Desarrollador, opción 'Eliminar Definitivo' en Directiva, enrutamiento directo de cuentas aprobadas, sincronización bidireccional de 'Mostrarme en el Mapa' y prevención de cierres en el Mapa TX.",
            novedades = listOf(
                "💻 Rol Inmutable Desarrollador Máster: Fijación del rol `DESARROLLADOR` para eduardo.androide.em@gmail.com con jerarquía suprema y asignación exclusiva del cargo de Presidente.",
                "🗑️ Opción 'Eliminar Definitivo': Purga atómica del usuario de todas las colecciones de Firestore (`usuarios`, `users`, `members`, `vinculos_google`) y Room local.",
                "🚀 Enrutamiento Directo sin Bucles: Las cuentas aprobadas ingresan directo a `MainAppScreen` sin caer en la pantalla de 'Cuenta en evaluación'.",
                "🛰️ Radar GPS Continuo y Sincronizado: Sincronización bidireccional del interruptor de ubicación entre Ajustes y Mapa TX, con umbral de tolerancia ampliado a 12h.",
                "📍 Estabilidad en Mapa TX: Prevención de cierres del mapa al tocar marcadores de pilotos y soporte de avatares Base64 en el radar."
            ),
            correcciones = listOf(
                "Corregido: Cierre/crash del mapa al tocar la ficha de un piloto en el Radar GPS.",
                "Corregido: Sobreescritura del rol del Desarrollador a Presidente durante la vinculación de Google OAuth."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 32,
            versionName = "1.5.1-beta",
            titulo = "Sincronización Unificada de Foto de Perfil en Dashboard y Chat Táctico",
            fecha = "10 sep 2026",
            descripcionCorta = "Unificación de la foto de perfil en el avatar del Chat Táctico y en el botón superior derecho del Carnet TX en el Dashboard, garantizando el renderizado idéntico de la foto del piloto.",
            novedades = listOf(
                "💬 Foto de Perfil en Chat Táctico: El círculo avatar del piloto que escribe en el Chat Táctico muestra de forma idéntica la foto de perfil configurada en el Carnet TX.",
                "📊 Botón Carnet TX en Dashboard: El círculo superior derecho del Dashboard integra la misma foto del perfil con fallbacks de respaldo para coincidir 1:1 con el Carnet TX.",
                "🔒 Asignación Atómica de Mensaje: Asignación automática de foto de perfil con contingencia local al enviar mensajes en cualquier canal del chat."
            ),
            correcciones = listOf(
                "Corregido: Parpadeo o discrepancia entre la foto de perfil del Chat y la foto del botón superior del Dashboard."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 31,
            versionName = "1.5.0-beta",
            titulo = "Decodificación Nativa de Data URIs Base64 en Coil para Carga Instantánea de Imágenes",
            fecha = "10 sep 2026",
            descripcionCorta = "Creación de `obtenerModelParaCoil` en `SanitizadorImagenUrl` para decodificar cadenas Data URIs Base64 directamente en `ByteArray`, permitiendo que Coil renderice instantáneamente flyers, fotos de perfil, carnet y calendario.",
            novedades = listOf(
                "🖼️ Decodificación Nativa en Coil (`obtenerModelParaCoil`): Conversión atómica de Data URIs Base64 a `ByteArray` para que Coil renderice las imágenes inmediatamente sin excepciones de esquema HTTP no soportado.",
                "📸 Carga Instantánea en Todos los Módulos: Muro de Avisos, Carnet TX, Calendario Motero, Mercado y Radar GPS renderizan instantáneamente las fotos creadas.",
                "🔒 Sincronización Ininterrumpida: Rendimiento offline y en vivo sin depender de servidores de almacenamiento ni facturación de GCP."
            ),
            correcciones = listOf(
                "Corregido: Error 'Error al cargar la imagen' en flyers de avisos y fotos de carnet que contenían formato Data URI Base64.",
                "Corregido: Fallo de renderizado en Coil al recibir esquemas de URL `data:image/jpeg;base64`."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 30,
            versionName = "1.4.9-beta",
            titulo = "Restablecimiento Definitivo de Subida de Fotos en Carnet TX y Perfiles",
            fecha = "10 sep 2026",
            descripcionCorta = "Corrección en los lanzadores de cámara y galería de Carnet TX y Ficha de Piloto para conectar con la subida de contingencia Base64/Firestore, eliminando por completo los errores de subida de fotos.",
            novedades = listOf(
                "📇 Subida de Fotos en Carnet TX e Identificación: Conexión directa de los selectores de cámara y galería con el motor de contingencia multimedia NubeMultimedia.",
                "🖼️ Sincronización Inmediata en Nube: Las fotos de perfil y de la moto se almacenan atómicamente en Firestore y Room local sin fallar ante incidencias de facturación de Storage.",
                "🔒 Persistencia Garantizada de Fichas Técnicas: Protección permanente de la foto del carnet, fotos de la moto y documentos."
            ),
            correcciones = listOf(
                "Corregido: Mensaje 'Error al subir la foto de perfil' en la ficha de edición de Carnet TX al seleccionar imagen de cámara o galería.",
                "Corregido: Error de subida en fotos de la moto en el Carnet TX."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 29,
            versionName = "1.4.8-beta",
            titulo = "Restablecimiento Ultra-Táctico de Carga y Sincronización de Imágenes en Nube",
            fecha = "10 sep 2026",
            descripcionCorta = "Resolución del error de almacenamiento de Firebase Storage mediante mecanismo de contingencia automática con Data URI Base64, corrección de duplicidad de carpetas y lectura directa de cámaras/galerías.",
            novedades = listOf(
                "🖼️ Mecanismo de Contingencia Data URI Base64: Si Firebase Storage se encuentra no disponible o bloqueado por facturación GCP, la app convierte de forma transparente las fotos a Data URI Base64 comprimidas (~50-80 KB) para guardarlas e intercambiarlas en tiempo real por Firestore y Room.",
                "📷 Lectura Unificada de Cámara y Galería: Soporte total para fotos tomadas con la cámara (FileProvider/ContentResolver) sin excepciones de relectura o permisos.",
                "🔒 Sostenibilidad de Datos de Carnet TX: Eliminada la rutina destructiva que limpiaba la foto de perfil y fotos de la moto al validar sesiones de Google.",
                "📂 Corregidas Rutas de Almacenamiento: Eliminada la anidación duplicada de carpetas (`avisos/perfiles/`) que generaba errores de permisos 403 en Firebase Storage."
            ),
            correcciones = listOf(
                "Corregido: Error al subir fotos en Carnet TX, Muro de Avisos, Calendario Motero y Módulos del Panel.",
                "Corregido: Fallo en la carga de imágenes de cámara por restricciones de permisos de lectura temporal."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 28,
            versionName = "1.4.7-beta",
            titulo = "Restablecimiento de Carga de Cámara, Almacenamiento y Protección de Fotos de Perfil",
            fecha = "10 sep 2026",
            descripcionCorta = "Corrección en el procesamiento de fotos tomadas con la cámara, protección de fotos locales al validar sesiones Google y solución de rutas de carpetas en Firebase Storage.",
            novedades = listOf(
                "📷 Soporte Completo de Cámara: Lectura directa de streams de memoria para fotos de cámara, galería y FileProvider sin errores de permisos.",
                "🔒 Protección Permanente de Perfil: Eliminación de la limpieza reactiva de fotos de perfil al restaurar sesión.",
                "📂 Corregida Estructura de Carpetas Firebase Storage: Eliminación de anidamientos duplicados (`avisos/perfiles/`) que violaban las reglas de seguridad 403 de Firebase Storage.",
                "🌐 Verificación Flexible de Conectividad: Eliminada la restricción rígida `NET_CAPABILITY_VALIDATED` que bloqueaba subidas en conexiones móviles y VPNs."
            ),
            correcciones = listOf(
                "Corregido: Error al tomar fotos con la cámara para el Carnet TX o publicaciones del Feed.",
                "Corregido: Desaparición de la foto de perfil y fotos de la moto al reiniciar la aplicación por validación de sesión de Google."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 27,
            versionName = "1.4.6-beta",
            titulo = "Restablecimiento Total de Carga y Sincronización de Imágenes en Tiempo Real",
            fecha = "09 sep 2026",
            descripcionCorta = "Creación del sanitizador universal de URLs para Coil, enriquecimiento de esquemas de imagen en Firestore (feed, chat, calendario, carnet, mapa) y normalización de descargas con soporte para gs://, rutas relativas y tokens de Firebase Storage.",
            novedades = listOf(
                "🖼️ Sanitizador Universal de Imágenes (`SanitizadorImagenUrl`): Normalización automática de esquemas `gs://`, rutas relativas (`avisos/`, `perfiles/`, `imagenes_chat/`) y URLs de Firebase Storage con `alt=media` para carga instantánea en Coil.",
                "📡 Enriquecimiento en Vivo de Firestore (`BaseFirestoreSync`): Mapeo inteligente con fallbacks de nombres de campo (camelCase / snake_case) para no perder ninguna foto de aviso, flyer o avatar en la sincronización en tiempo real.",
                "📸 Carga Multimodular Garantizada: Solución aplicada en Muro/Feed, Chat Táctico, Calendario, Carnet TX, Mercado Biker, Directorio de Miembros y Radar GPS.",
                "🔒 Garantía de Autenticación en Almacenamiento: Sesión activa de Firebase Auth verificada antes de subir o descargar archivos multimedia."
            ),
            correcciones = listOf(
                "Corregido: Error de carga de imágenes en avisos, chat, carnet y calendario por URLs desformateadas o sin token de acceso de Firebase Storage.",
                "Corregido: Pérdida de foto de perfil por disparidades entre los campos `fotoPerfilUri` y `profilePhotoUri` al deserializar de Firestore."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 26,
            versionName = "1.4.5-beta",
            titulo = "Desarrollador Máster Inmutable, Eliminación Definitiva de Usuarios & Killswitch Modular",
            fecha = "08 sep 2026",
            descripcionCorta = "Rol inmutable Desarrollador Máster para la cuenta del creador, jerarquía de designación de Presidente, purga definitiva de usuarios eliminados por la directiva, Killswitch modular por piloto y nuevo rol Director de Redes TX.",
            novedades = listOf(
                "💻 Rol Desarrollador Máster: Rol inmutable para eduardo.androide.em@gmail.com con jerarquía suprema y asignación exclusiva del cargo de Presidente.",
                "🗑️ Eliminación Definitiva de Usuarios: Purga completa e instantánea en Firestore (usuarios, users, members, vinculos_google) y Room local al eliminar un usuario desde la Directiva.",
                "🎛️ Killswitch Modular por Piloto: La Directiva puede restringir el acceso a módulos específicos (Player TX, Velocímetro, Mercado, Chat, Radar, Mesh TX) a pilotos individuales.",
                "📢 Nuevo Cargo 'Director de Redes TX & Avisos': Creación del rol oficial para la gestión de redes comunitarias y comunicados en el Muro.",
                "👥 Conteo Real de Pilotos: Filtrado estricto del contador oficial de miembros activos reales registrados en la plataforma."
            ),
            correcciones = listOf(
                "Corregido: El desarrollador aparecía como 'Presidente' en lugar de su cargo oficial 'Desarrollador Máster'.",
                "Corregido: Usuarios eliminados podían reingresar sin pasar por el proceso inicial de solicitud."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 25,
            versionName = "1.4.4-beta",
            titulo = "Desbloqueo Inmediato de Cuentas Aprobadas por Directiva & Enlace de Creación Google",
            fecha = "08 sep 2026",
            descripcionCorta = "Sincronización instantánea de estado 'ACTIVO' en restauración de sesión para cuentas aprobadas, actualización completa de colecciones Firestore (usuarios, users, members, vinculos_google y Room) y panel de creación de cuenta Google si no posee correo.",
            novedades = listOf(
                "✅ Desbloqueo Inmediato de Cuentas Aprobadas: Consulta dinámica de usuarioEstado en Firestore al restaurar sesión para ingresar directo a la app sin quedarse en Sala de Espera.",
                "🛡️ Aprobación Integral por Directiva: Al aprobar en Solicitudes de Ingreso, se actualizan de forma atómica 'usuarios', 'users', 'members', 'vinculos_google' y la base de datos local Room.",
                "🌐 Panel de Creación de Cuenta Google: Tarjeta intuitiva en la pantalla de inicio de sesión que redirige a la creación de cuenta Google y retorno a la app.",
                "👥 Sincronización Total con Directorio de Miembros: Los usuarios aprobados aparecen de forma instantánea en el Módulo de Miembros y Ranking."
            ),
            correcciones = listOf(
                "Corregido: Cuentas aprobadas por la directiva quedaban atrapadas en la pantalla de 'Cuenta en evaluación' por estado 'PENDIENTE' en caché.",
                "Corregido: Falta de actualización de la colección 'members' al aprobar solicitudes de ingreso."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 24,
            versionName = "1.4.3-beta",
            titulo = "Sincronización Atómica de Vinculación 1-a-1 y Estabilidad de Sesión Creador",
            fecha = "08 sep 2026",
            descripcionCorta = "Sincronización atómica de Vinculación Google en vinculos_google, inmunidad de sesión para cuenta creador/desarrollador, estandarización de Device ID en SEGURIDAD_CUENTAS y depuración de cierres de sesión por deserialización.",
            novedades = listOf(
                "🔑 Vinculación 1-a-1 Atómica: Registro automático e instantáneo en la colección 'vinculos_google' durante el inicio de sesión con Google.",
                "👑 Inmunidad Creador / Super Admin: Excepción permanente para eduardo.androide.em@gmail.com en el listener de perfil para evitar cierres de sesión por cambio de dispositivo o estado.",
                "📱 Estandarización de Device ID: Unificación de la generación de UUID único a través de SEGURIDAD_CUENTAS.obtenerIdDispositivo().",
                "🛡️ Deserialización Segura de Perfil: Manejo seguro en 'PerfilNube.escucharPerfil()' para prevenir fallos al mapear documentos de Firestore.",
                "🔒 Cierre de Sesión Limpio: Purga local física completa preservando siempre el documento original del piloto en la nube."
            ),
            correcciones = listOf(
                "Corregido: Error 'Acceso Revocado' y cierre automático tras ingresar con la cuenta de desarrollador.",
                "Corregido: Desajuste de IDs entre GestorSesion y SEGURIDAD_CUENTAS que provocaba falso positivo de sesión en otro celular.",
                "Corregido: Incoherencia en la verificación de vínculos de correo en checkAndRestoreSession()."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 23,
            versionName = "1.4.2-beta",
            titulo = "Unificación de Autenticación, Cuarentena por Estados y Panel de Gobernanza",
            fecha = "08 sep 2026",
            descripcionCorta = "Corregido el bucle de inicio de sesión con Google. Flujo de estados en Firestore (NUEVO_REGISTRO -> PENDIENTE -> ACTIVO), Sala de Espera con desbloqueo en tiempo real, Panel de Gobernanza reactivo de Directiva y Bypass de Creador.",
            novedades = listOf(
                "🔑 Selector de Cuentas Forzado en Google: Sign-Out previo explícito para garantizar la ventana emergente de selección de cuenta.",
                "📋 Enrutamiento por Estados de Firestore: Validación en vivo de UID (NUEVO_REGISTRO -> PENDIENTE -> ACTIVO) con formulario de registro automático.",
                "⏳ Sala de Espera Reactiva: Escucha en vivo de aprobación por Firestore para desbloquear automáticamente el sistema.",
                "👑 Bypass Nivel Dios Creador: Reconocimiento inmediato de eduardo.androide.em@gmail.com con perfil activo de SUPER_ADMIN.",
                "🛡️ Panel de Gobernanza Reactivo: Lista en vivo de solicitudes pendientes en Firestore con asignación de roles y activación instantánea.",
                "📱 Purga Local Segura: Limpieza física de Room, SharedPreferences y caché de imágenes en Cierre de Sesión sin borrar el documento en Firestore."
            ),
            correcciones = listOf(
                "Corregido: Bucle de cierre de sesión al acceder con Google por falta de documento inicial en Firestore.",
                "Corregido: Eliminados bloques y botones obsoletos de códigos de acceso manuales en el panel de directiva.",
                "Corregido: Control estricto de dispositivo activo por id_dispositivo_activo."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 22,
            versionName = "1.4.1-beta",
            titulo = "Botón SOS Flotante Nativo en el Mapa (OsmAnd HUD)",
            fecha = "07 sep 2026",
            descripcionCorta = "Integración del botón SOS flotante directamente sobre la interfaz nativa del mapa OsmAnd. Toca el botón rojo SOS en el mapa para desplegar el selector de auxilios con emisión en vivo.",
            novedades = listOf(
                "🚨 Botón SOS Flotante Nativo en Mapa OsmAnd: Integración directa en la barra derecha de controles sobre el mapa nativo.",
                "📱 Diálogo Táctico SOS en el Mapa: Selector interactivo con los 7 tipos de auxilio vial (Gasolina, Mecánico, Caída, Choque, Médico, Peligro, Alcabala).",
                "📡 Transmisión Satelital en Vivo: Emisión instantánea a Firestore, Radar GPS, Chat Auxilio y Radio Malla."
            ),
            correcciones = listOf(
                "Corregido: El botón SOS anterior quedaba oculto detrás de la actividad nativa de OsmAnd.",
                "Alineación perfecta con controles de zoom, radar y radio intercomunicador."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 21,
            versionName = "1.4.0-beta",
            titulo = "Botón SOS Rápido en Mapa TX + Contactos Reales de Especialistas",
            fecha = "07 sep 2026",
            descripcionCorta = "Nuevo botón SOS rápido en la pantalla del Mapa TX. Toca para emitir, mantén presionado para cambiar el tipo. Limpieza total de datos simulados en el módulo SOS: Médico, Mecánico y Seguridad Vial ahora muestran miembros reales registrados en la app.",
            novedades = listOf(
                "🚨 Botón SOS Rápido en Mapa TX: Toca para emitir alerta directamente desde la pantalla del mapa. Mantén presionado para elegir el tipo de emergencia (Gasolina, Mecánico, Caída, Choque, Médico, Seguridad, Alcabala).",
                "💾 Tipo de Alerta Persistente: El tipo de alerta elegido en el mapa se recuerda entre sesiones para emisión más rápida.",
                "👨‍⚕️ Contactos Especialistas Reales: El Módulo SOS muestra el nombre y teléfono real del Mecánico Oficial, Médico del Club y Oficial de Seguridad registrados en la app.",
                "📵 Botones Deshabilitados si No hay Especialista: Si no hay miembro con ese rol, el botón aparece en gris indicando 'Sin asignar' en lugar de marcar un número falso.",
                "📞 Contacto Familiar Real: El botón de contacto familiar muestra el nombre real del contacto de emergencia registrado en el perfil del piloto.",
                "📲 Compartir APK por WhatsApp: Desde Vista Info puedes enviar el enlace de descarga a cualquier contacto por WhatsApp."
            ),
            correcciones = listOf(
                "Eliminados números de teléfono falsos: +584129904433 (P. Torrealba), +584245551290 (Dra. Gómez), +584128887766 (Seguridad), +584141234567 (Familiar fijo).",
                "Corregido fallback de contacto familiar: ya no usa número hardcoded sino emergencyContactPhone real del perfil.",
                "Corregido bug de navegación inferior: los botones de módulos reconocen inmediatamente el cambio de pantalla."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 20,
            versionName = "1.3.9-beta",
            titulo = "Geolocalización Satelital Precisa SOS, Captura Multi-Proveedor y Centrado en Mapa TX",
            fecha = "07 sep 2026",
            descripcionCorta = "Eliminación total de coordenadas fijas de Caracas en el sistema SOS. Detección en vivo multi-proveedor (FusedLocation, LocationManager, OsmAnd y Radar TX) con geocodificación inversa real de Venezuela y centrado automático en el mapa sobre la posición exacta del piloto.",
            novedades = listOf(
                "📍 Coordenadas Exactas del Piloto: El sistema SOS captura satelitalmente en tiempo real la ubicación exacta del piloto donde ocurrió la emergencia.",
                "🚫 Eliminación de Coordenadas Fijas de Caracas: Cero ubicaciones falsas en Tazón o Caracas; si el dispositivo no tiene GPS al instante, consulta múltiples proveedores satelitales y de red.",
                "🗺️ Centrado Inmediato en Mapa TX: Al emitir o tocar la alerta SOS, el mapa se enfoca y centra de forma automática en el punto satelital del piloto.",
                "🔄 Botón de Re-captura GPS en Diálogo: Indicador visual en vivo con spinner de detección y botón para refrescar satélites al instante.",
                "📋 Copia Satelital al Portapapeles: Copia automática de latitud y longitud exactas para compartir a grupos de rescate y convoyes.",
                "🇻🇪 Geocodificación Inversa Venezolana: Identificación automática de tramos viales, municipios y referencias viales locales basada en las coordenadas del dispositivo."
            ),
            correcciones = listOf(
                "Corregido: Alertas SOS reportaban erróneamente Caracas/Tazón al emitirse en otras regiones como Aragua.",
                "Corregido: Fallback de EmergencyAlert y TeamTxViewModel configurados a resolución satelital dinámica en lugar de valores fijos."
            ),
            esRecomendada = false
        ),
        NotaVersionDetallada(
            versionCode = 19,
            versionName = "1.3.8-beta",
            titulo = "Sincronización Total SOS Vial, Iconos Alusivos en Mapa TX y Resolución Táctica",
            fecha = "07 sep 2026",
            descripcionCorta = "Sincronización integral del módulo SOS Vial con Muro/Feed, Chat General, Auxilio, Directiva y Mapa TX. Iconos de emergencia alusivos tocando el disco de avatar en el mapa con nombres personalizables en español, copia rápida de coordenadas GPS y resolución compartida por piloto y directiva.",
            novedades = listOf(
                "🚨 Sincronización Total SOS Vial: Conexión en tiempo real entre SOS, Feed de Noticias, Chat General, Auxilio y Canal Directiva.",
                "🗺️ Disco de Usuario con Icono Tocándolo en Mapa TX: Al haber emergencia activa, el avatar muestra un badge táctico tangencial con halo pulsante animado.",
                "🎨 8 Iconos Alusivos Personalizables: Integración con nombres en español (sos_gasolina, sos_mecanico, sos_caida, sos_choque, sos_medico, sos_seguridad, sos_alcabala, sos_alerta).",
                "📋 Copia Rápida de GPS: Botón interactivo en diálogo de emisión y detalle para copiar coordenadas satelitales al portapapeles con confirmación Toast.",
                "✅ Resolución Compartida de Emergencias: El piloto afectado y la directiva pueden marcar la alerta como RESUELTA, retirando el aviso del mapa y notificando a chats y feed.",
                "🛒 Carrito de Compras en Mercado Biker: Icono renovado a carrito de compras con el estilo visual unificado del club.",
                "🔢 Teclado Numérico en Carnet TX: Activación forzada de teclado numérico en campos de teléfonos.",
                "🛡️ Gobernanza de Directiva: Restricción para que solo el Presidente y Desarrollador puedan gestionar miembros de directiva con opción de transferir o anular cargo."
            ),
            correcciones = listOf(
                "Corregido: Visibilidad de textos y padding en tarjetas de alerta rápida SOS por nivel.",
                "Corregido: Filtro de Muro para publicar avisos únicamente en incidentes críticos (choque, caída, accidentes viales).",
                "Corregido: Desvinculación automática inmediata al reclamar sesión en un nuevo dispositivo."
            ),
            esRecomendada = false
        ),
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
            esRecomendada = false
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
        val fallbackUrl = "https://firebasestorage.googleapis.com/v0/b/teamnacionaltx.firebasestorage.app/o/updates%2FTeamTX-latest.apk?alt=media&token=422ded53-82b3-48a5-945a-87e2c30745bb"
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
                            item.name.equals("TeamTX-latest.apk", ignoreCase = true) -> "v1.3.8-beta (Última)"
                            else -> item.name.removeSuffix(".apk")
                        }

                        val estimatedCode = when {
                            item.name.contains("1.3.8") -> 19
                            isLatest -> 19
                            item.name.contains("1.3.7") -> 18
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
            val detalleTop = obtenerDetalleVersion(19, "1.3.8-beta")
            listOf(
                StorageApkVersion(
                    fileName = "TeamTX-latest.apk",
                    downloadUrl = fallbackUrl,
                    sizeBytes = 395443512L,
                    formattedSize = "377.1 MB",
                    updatedTimestamp = System.currentTimeMillis(),
                    formattedDate = "07 sep 2026, 01:40 PM",
                    versionName = "v1.3.8-beta (Recomendada)",
                    versionCode = 19,
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
     * Consulta y devuelve la lista completa de versiones/rollbacks desde la colección 'updates' en Firestore.
     */
    suspend fun obtenerListaVersionesFirestore(): List<InformacionOta> = withContext(Dispatchers.IO) {
        val listaResult = mutableListOf<InformacionOta>()
        try {
            val db = FirebaseFirestore.getInstance()
            val querySnapshot = db.collection("updates").get().await()

            for (doc in querySnapshot.documents) {
                val code = when (val rawCode = doc.get("versionCode")) {
                    is Number -> rawCode.toInt()
                    is String -> rawCode.trim().toIntOrNull() ?: 0
                    else -> 0
                }
                val name = doc.getString("versionName") ?: "v$code"
                val url = (doc.getString("urlDescarga") ?: doc.getString("downloadUrl") ?: "").trim()
                val customNotas = doc.getString("notas") ?: ""
                val customTitulo = doc.getString("titulo") ?: ""
                val fecha = doc.getString("fechaPublicacion") ?: ""

                val novedadesList = mutableListOf<String>()
                val rawNov = doc.get("novedades")
                if (rawNov is List<*>) rawNov.filterIsInstance<String>().forEach { novedadesList.add(it) }

                val correccionesList = mutableListOf<String>()
                val rawCorr = doc.get("correcciones")
                if (rawCorr is List<*>) rawCorr.filterIsInstance<String>().forEach { correccionesList.add(it) }

                if (code > 0 && url.isNotBlank()) {
                    val detalle = obtenerDetalleVersion(code, name)
                    listaResult.add(
                        InformacionOta(
                            versionCode = code,
                            versionName = name,
                            titulo = if (customTitulo.isNotBlank()) customTitulo else detalle.titulo,
                            urlDescarga = url,
                            notas = if (customNotas.isNotBlank()) customNotas else detalle.descripcionCorta,
                            novedades = if (novedadesList.isNotEmpty()) novedadesList else detalle.novedades,
                            correcciones = if (correccionesList.isNotEmpty()) correccionesList else detalle.correcciones,
                            fechaPublicacion = if (fecha.isNotBlank()) fecha else detalle.fecha
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando colección 'updates' en Firestore: ${e.message}")
        }

        if (listaResult.isEmpty()) {
            HISTORIAL_VERSIONES_OFICIALES.forEach { ver ->
                val fallbackUrl = "https://github.com/EDJMM91/TeamTX-Workspace-Completo/raw/main/Team-Nacional-TX-Aragua/apk/TeamTX-v${ver.versionName}.apk"
                listaResult.add(
                    InformacionOta(
                        versionCode = ver.versionCode,
                        versionName = ver.versionName,
                        titulo = ver.titulo,
                        urlDescarga = if (ver.versionCode == 27) "https://github.com/EDJMM91/TeamTX-Workspace-Completo/raw/main/Team-Nacional-TX-Aragua/apk/TeamTX-latest.apk" else fallbackUrl,
                        notas = ver.descripcionCorta,
                        novedades = ver.novedades,
                        correcciones = ver.correcciones,
                        fechaPublicacion = ver.fecha
                    )
                )
            }
        }

        listaResult.sortedByDescending { it.versionCode }
    }

    /**
     * Consulta las actualizaciones OTA sincronizando en tiempo real con Firebase Storage
     * y la base de datos Firestore.
     */
    suspend fun verificarActualizacion(): InformacionOta = withContext(Dispatchers.IO) {
        val urlOficialFirebaseStorage = "https://github.com/EDJMM91/TeamTX-Workspace-Completo/raw/main/Team-Nacional-TX-Aragua/apk/TeamTX-latest.apk"
        val urlInfoOtaJson = "https://raw.githubusercontent.com/EDJMM91/TeamTX-Workspace-Completo/main/Team-Nacional-TX-Aragua/apk/ota_info.json"

        // 1. Consultar PRIMERO Firebase Storage directamente (la fuente real de los APKs)
        var versionStorage: StorageApkVersion? = null
        try {
            val lista = obtenerListaVersionesStorage()
            versionStorage = lista.firstOrNull { it.isRecommendedLatest }
                ?: lista.maxByOrNull { it.versionCode }
        } catch (e: Exception) {
            Log.e(TAG, "Error consultando versiones en Storage: ${e.message}")
        }

        // 2. Consultar ota_info.json si está disponible
        if (versionStorage == null) {
            try {
                val connection = URL(urlInfoOtaJson).openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
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
                        downloadUrl = if (urlDesc.isNotBlank()) urlDesc else urlOficialFirebaseStorage,
                        versionCode = code,
                        versionName = name,
                        titulo = "Actualización Oficial (GitHub/Storage)",
                        notas = notasJson,
                        novedades = listaNovedades,
                        correcciones = listaCorrecciones,
                        isRecommendedLatest = true
                    )
                    Log.d(TAG, "Versión detectada en ota_info.json: $name (Build $code)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error consultando ota_info.json: ${e.message}")
            }
        }

        // 3. Consultar Firestore en colección 'configuracion' / 'OTA'
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
                var url = (doc.getString("urlDescarga") ?: doc.getString("downloadUrl") ?: "").trim()
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

        // 4. Determinar la versión ganadora (priorizando la real más reciente de Storage)
        val codeStorage = versionStorage?.versionCode ?: 0
        val codeFirestore = infoFirestore?.versionCode ?: 0

        val resultadoFinal: InformacionOta = if (versionStorage != null && codeStorage >= codeFirestore) {
            val detalle = obtenerDetalleVersion(codeStorage, versionStorage.versionName)
            InformacionOta(
                versionCode = codeStorage,
                versionName = versionStorage.versionName.ifBlank { detalle.versionName },
                titulo = versionStorage.titulo.ifBlank { detalle.titulo },
                urlDescarga = if (versionStorage.downloadUrl.isNotBlank() && !versionStorage.downloadUrl.contains("github.com")) versionStorage.downloadUrl else urlOficialFirebaseStorage,
                notas = versionStorage.notas.ifBlank { detalle.descripcionCorta },
                novedades = if (versionStorage.novedades.isNotEmpty()) versionStorage.novedades else detalle.novedades,
                correcciones = if (versionStorage.correcciones.isNotEmpty()) versionStorage.correcciones else detalle.correcciones,
                fechaPublicacion = versionStorage.formattedDate.ifBlank { detalle.fecha }
            )
        } else if (infoFirestore != null) {
            infoFirestore
        } else {
            val detalleV19 = obtenerDetalleVersion(19, "1.3.8-beta")
            InformacionOta(
                versionCode = 19,
                versionName = "1.3.8-beta",
                titulo = detalleV19.titulo,
                urlDescarga = urlOficialFirebaseStorage,
                notas = detalleV19.descripcionCorta,
                novedades = detalleV19.novedades,
                correcciones = detalleV19.correcciones,
                fechaPublicacion = detalleV19.fecha
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
        // Preparar destino
        val carpetaDestino = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        if (!carpetaDestino.exists()) {
            carpetaDestino.mkdirs()
        }
        val archivoApk = File(carpetaDestino, "TeamTX_Update.apk")
        if (archivoApk.exists()) {
            archivoApk.delete()
        }

        try {
            var urlString = urlOriginal.trim()
            if (urlString.isBlank()) {
                Log.e(TAG, "URL de descarga vacía")
                return@withContext null
            }

            Log.d(TAG, "Iniciando descarga directa desde: $urlString")

            // 1. Si la URL es de Firebase Storage o ruta interna, descargar por Firebase Storage SDK
            if (urlString.startsWith("gs://") || urlString.contains("firebasestorage.googleapis.com") || urlString.startsWith("updates/")) {
                try {
                    val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                    val storageRef = when {
                        urlString.startsWith("gs://") -> try { storage.getReferenceFromUrl(urlString) } catch (_: Exception) { storage.reference.child("updates/TeamTX-latest.apk") }
                        urlString.contains("/o/") -> {
                            try {
                                val encodedPath = urlString.substringAfter("/o/").substringBefore("?")
                                val decodedPath = java.net.URLDecoder.decode(encodedPath, "UTF-8")
                                storage.reference.child(decodedPath)
                            } catch (_: Exception) {
                                storage.reference.child("updates/TeamTX-latest.apk")
                            }
                        }
                        urlString.startsWith("updates/") -> storage.reference.child(urlString)
                        else -> storage.reference.child("updates/TeamTX-latest.apk")
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
            Log.e(TAG, "Error durante la descarga HTTP del APK: ${e.message}", e)
            // Rescate definitivo: Descargar directamente de Firebase Storage bucket 'updates/TeamTX-latest.apk'
            try {
                Log.w(TAG, "Iniciando descarga de rescate directa desde Firebase Storage updates/TeamTX-latest.apk...")
                val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                val rescueRef = storage.reference.child("updates/TeamTX-latest.apk")
                var completado = false
                var errDescarga: Exception? = null
                val rescueTask = rescueRef.getFile(archivoApk)
                rescueTask.addOnProgressListener { snapshot ->
                    val transferred = snapshot.bytesTransferred
                    val total = snapshot.totalByteCount
                    if (total > 0) {
                        val prog = transferred.toFloat() / total.toFloat()
                        val mbLeidos = String.format(java.util.Locale.US, "%.1f", transferred / (1024f * 1024f))
                        val mbTotal = String.format(java.util.Locale.US, "%.1f", total / (1024f * 1024f))
                        onProgreso(prog, mbLeidos, mbTotal)
                    }
                }.addOnSuccessListener {
                    completado = true
                }.addOnFailureListener { e ->
                    errDescarga = e
                    completado = true
                }
                while (!completado) {
                    kotlinx.coroutines.delay(100)
                }
                if (errDescarga == null && archivoApk.exists() && archivoApk.length() > 0) {
                    Log.i(TAG, "Descarga de rescate exitosa: ${archivoApk.length()} bytes")
                    return@withContext archivoApk
                }
            } catch (exRescue: Exception) {
                Log.e(TAG, "Descarga de rescate falló: ${exRescue.message}")
            }
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
            if (urlFinal.isBlank() || urlFinal.contains("firebasestorage.googleapis.com")) {
                urlFinal = "https://github.com/EDJMM91/TeamTX-Workspace-Completo/raw/main/Team-Nacional-TX-Aragua/apk/TeamTX-latest.apk"
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
