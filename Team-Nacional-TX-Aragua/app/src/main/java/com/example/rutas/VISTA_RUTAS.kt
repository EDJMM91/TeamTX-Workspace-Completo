package com.example.rutas

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MemberProfile
import com.example.dashboard.DashboardFondoConfig
import java.util.Locale

import coil.compose.AsyncImage
import net.osmand.plus.OsmandApplication

/**
 * Pantalla principal e Interfaz de Usuario Compose del Módulo de Rutas Tácticas de Team Nacional TX Aragua.
 * Implementa las 3 Opciones principales del botón central inferior:
 * - Opción A: Iniciar/Pausar/Detener Tracking Táctico en segundo plano.
 * - Opción B: Crear Ruta Guiada.
 * - Opción C: Generar Reporte y Video MP4 en Galería (RUTASVIDEO.kt) con animación 2D en REPRODUCTOR.kt.
 * - Seguro de Batería: Monitoreo en vivo de carga con respaldo automático en NUBE.kt si baja del 5%.
 *
 * Totalmente en Tema Claro del Dashboard y documentado en español.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaRutasScreen(
    currentMember: MemberProfile?,
    onVolver: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val resumenRuta by RUTA.resumenRutaState.collectAsStateWithLifecycle()
    val nivelBateria by RUTA.bateriaNivelState.collectAsStateWithLifecycle()
    val respaldoEjecutado by RUTA.respaldoCriticoEjecutadoState.collectAsStateWithLifecycle()
    val servicioActivo by SERVICIO.servicioActivoState.collectAsStateWithLifecycle()
    val grabandoVideo by RUTASVIDEO.grabandoVideoState.collectAsStateWithLifecycle()

    // Estados del Reproductor 2D Relive
    val estadoReproductor by REPRODUCTOR.estadoReproductor.collectAsStateWithLifecycle()
    val telemetriaReproduccion by REPRODUCTOR.telemetria.collectAsStateWithLifecycle()
    val fotoEnPantalla by REPRODUCTOR.fotoEnPantalla.collectAsStateWithLifecycle()

    var mostrarModalOpcionesCentrales by remember { mutableStateOf(false) }
    var mostrarModalCrearRutaGuiada by remember { mutableStateOf(false) }
    var mostrarModalReporteTexto by remember { mutableStateOf<String?>(null) }

    var tituloNuevaRuta by remember { mutableStateOf("") }
    var puntoOrigenGuiado by remember { mutableStateOf("") }
    var puntoDestinoGuiado by remember { mutableStateOf("") }

    // Launcher para la API nativa de Android MediaProjection (Grabación de Pantalla en Video MP4)
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            Toast.makeText(contexto, "📹 Generando animación 2D y video MP4...", Toast.LENGTH_SHORT).show()
            RUTASVIDEO.crearVideo(
                contexto = contexto,
                mediaProjectionIntent = result.data!!,
                resultCode = result.resultCode,
                ruta = resumenRuta
            ) { rutaArchivoMp4 ->
                if (rutaArchivoMp4.isNotBlank()) {
                    Toast.makeText(contexto, "🎬 Grabando recorrido. Toca 'Finalizar Video' para guardar en Galería.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(contexto, "🔴 Error al iniciar la grabación del video.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(contexto, "Captura de pantalla cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    // Asegurar registro del receptor de batería
    DisposableEffect(Unit) {
        RUTA.registrarReceptorBateria(contexto)
        onDispose {
            // El receptor sigue vivo en el SERVICIO si la ruta está activa
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Módulo de Rutas TX",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Tracking en vivo, Seguro de Batería y Video 2D",
                            fontSize = 11.sp,
                            color = Color(0xFFFF6B00),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color(0xFF0F172A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = DashboardFondoConfig.ColorFondoClaro
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. Tarjeta del Seguro de Batería (Seguro de Vida)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                TarjetaSeguroBateria(
                    nivelBateria = nivelBateria,
                    respaldoEjecutado = respaldoEjecutado,
                    estadoRuta = resumenRuta.estadoRuta
                )
            }

            // 2. Tarjeta del Estado Actual de la Ruta (Telemetría en tiempo real)
            item {
                TarjetaTelemetriaRuta(
                    resumenRuta = resumenRuta,
                    servicioActivo = servicioActivo
                )
            }

            // 3. Botón Principal Flotante/Central de Selección de Opciones
            item {
                Button(
                    onClick = { mostrarModalOpcionesCentrales = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🎛️ Menú de Opciones de Rutas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }

            // 4. Panel de Acciones Rápidas (Opción A, B, C directas)
            item {
                Text(
                    text = "Acciones Directas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Opción A: Iniciar / Detener Grabación de Ruta
                    CardOpcionRuta(
                        titulo = "Opción A: Tracking Táctico en Segundo Plano",
                        subtitulo = if (resumenRuta.estadoRuta == EstadoRutaEnum.GRABANDO) "Grabando en vivo (Pausar/Detener)" else "Iniciar grabación de nueva ruta",
                        icono = if (resumenRuta.estadoRuta == EstadoRutaEnum.GRABANDO) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        colorIcono = Color(0xFF2E7D32),
                        onClick = {
                            if (resumenRuta.estadoRuta == EstadoRutaEnum.GRABANDO) {
                                RUTA.pausarRuta()
                                Toast.makeText(contexto, "Ruta pausada", Toast.LENGTH_SHORT).show()
                            } else if (resumenRuta.estadoRuta == EstadoRutaEnum.PAUSADA) {
                                RUTA.reanudarRuta()
                                Toast.makeText(contexto, "Ruta reanudada", Toast.LENGTH_SHORT).show()
                            } else {
                                val idPiloto = currentMember?.firebaseUid ?: currentMember?.id?.toString() ?: "DEV"
                                val nombre = currentMember?.nickname?.ifBlank { currentMember?.fullName } ?: "Piloto TX"
                                SERVICIO.iniciarServicioRuta(contexto, "Ruta Libre TX", idPiloto, nombre)
                                Toast.makeText(contexto, "🟢 Grabación iniciada en segundo plano", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // Opción B: Crear Ruta Guiada
                    CardOpcionRuta(
                        titulo = "Opción B: Crear Ruta Guiada",
                        subtitulo = "Planificar origen, destino y waypoints con el mapa",
                        icono = Icons.Default.AltRoute,
                        colorIcono = Color(0xFF0288D1),
                        onClick = { mostrarModalCrearRutaGuiada = true }
                    )

                    // Opción C: Generar Reporte y Video 2D (MediaProjection)
                    CardOpcionRuta(
                        titulo = "Opción C: Generar Reporte y Video 2D",
                        subtitulo = "Compilar reporte técnico y exportar animación MP4 a la Galería",
                        icono = Icons.Default.VideoCameraBack,
                        colorIcono = Color(0xFF8E24AA),
                        onClick = {
                            if (grabandoVideo) {
                                RUTASVIDEO.detenerYGuardarVideoGaleria(contexto) { uriVideo ->
                                    if (uriVideo != null) {
                                        Toast.makeText(contexto, "🎬 Video guardado en Galería", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(contexto, "Error guardando video", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                val reporte = RUTASVIDEO.generarReporteTexto(resumenRuta)
                                mostrarModalReporteTexto = reporte
                            }
                        }
                    )
                }
            }

            // 5. Estudio de Reproducción Cinemática 2D (Relive con OsmAnd y Fotos)
            item {
                TarjetaEstudioReproduccion2D(
                    resumenRuta = resumenRuta,
                    estadoReproductor = estadoReproductor,
                    telemetria = telemetriaReproduccion,
                    grabandoVideo = grabandoVideo,
                    onIniciarReproduccion = {
                        val app = contexto.applicationContext as? OsmandApplication
                        val mapView = app?.osmandMap?.mapView
                        REPRODUCTOR.iniciarReproduccion2D(
                            ruta = resumenRuta,
                            mapView = mapView,
                            multiplicadorVelocidad = 2.0f
                        ) {
                            Toast.makeText(contexto, "🏁 Recorrido 2D completado", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onPausarOReanudar = { REPRODUCTOR.alternarPausa() },
                    onDetener = { REPRODUCTOR.detenerReproduccion() },
                    onGrabarVideo = {
                        val projManager = contexto.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        mediaProjectionLauncher.launch(projManager.createScreenCaptureIntent())
                    },
                    onFinalizarVideo = {
                        RUTASVIDEO.detenerYGuardarVideoGaleria(contexto) { uri ->
                            if (uri != null) {
                                Toast.makeText(contexto, "🎬 Video guardado en Galería", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }

    // Modal de Opciones del Botón Central Inferior
    if (mostrarModalOpcionesCentrales) {
        AlertDialog(
            onDismissRequest = { mostrarModalOpcionesCentrales = false },
            title = {
                Text(
                    text = "🗺️ Opciones del Módulo de Rutas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Selecciona la acción requerida para la ruta:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    // A: Tracking
                    Button(
                        onClick = {
                            mostrarModalOpcionesCentrales = false
                            val idPiloto = currentMember?.firebaseUid ?: currentMember?.id?.toString() ?: "DEV"
                            val nombre = currentMember?.nickname?.ifBlank { currentMember?.fullName } ?: "Piloto TX"
                            if (resumenRuta.estadoRuta == EstadoRutaEnum.GRABANDO) {
                                SERVICIO.detenerServicioRuta(contexto)
                                RUTA.detenerRuta(contexto)
                                Toast.makeText(contexto, "🏁 Ruta finalizada y guardada en NUBE.kt", Toast.LENGTH_LONG).show()
                            } else {
                                SERVICIO.iniciarServicioRuta(contexto, "Ruta Táctica", idPiloto, nombre)
                                Toast.makeText(contexto, "🟢 Tracking de ruta iniciado", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (resumenRuta.estadoRuta == EstadoRutaEnum.GRABANDO) "Detener y Guardar Ruta" else "Opción A: Iniciar Tracking Táctico")
                    }

                    // B: Ruta Guiada
                    Button(
                        onClick = {
                            mostrarModalOpcionesCentrales = false
                            mostrarModalCrearRutaGuiada = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AltRoute, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Opción B: Crear Ruta Guiada")
                    }

                    // C: Reporte y Video
                    Button(
                        onClick = {
                            mostrarModalOpcionesCentrales = false
                            val reporte = RUTASVIDEO.generarReporteTexto(resumenRuta)
                            mostrarModalReporteTexto = reporte
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E24AA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VideoCameraBack, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Opción C: Generar Reporte y Video MP4")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarModalOpcionesCentrales = false }) {
                    Text("Cerrar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

    // Modal para Crear Ruta Guiada (Opción B)
    if (mostrarModalCrearRutaGuiada) {
        AlertDialog(
            onDismissRequest = { mostrarModalCrearRutaGuiada = false },
            title = {
                Text("📍 Opción B: Crear Ruta Guiada", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tituloNuevaRuta,
                        onValueChange = { tituloNuevaRuta = it },
                        label = { Text("Título de la Ruta") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = puntoOrigenGuiado,
                        onValueChange = { puntoOrigenGuiado = it },
                        label = { Text("Punto de Origen (Ej: Maracay)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = puntoDestinoGuiado,
                        onValueChange = { puntoDestinoGuiado = it },
                        label = { Text("Punto de Destino (Ej: Colonia Tovar)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarModalCrearRutaGuiada = false
                        val idPiloto = currentMember?.firebaseUid ?: currentMember?.id?.toString() ?: "DEV"
                        val nombre = currentMember?.nickname?.ifBlank { currentMember?.fullName } ?: "Piloto TX"
                        val titulo = tituloNuevaRuta.ifBlank { "Ruta Guiada: $puntoOrigenGuiado -> $puntoDestinoGuiado" }
                        SERVICIO.iniciarServicioRuta(contexto, titulo, idPiloto, nombre)
                        Toast.makeText(contexto, "🟢 Ruta Guiada activada: $titulo", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
                ) {
                    Text("Activar Ruta Guiada", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalCrearRutaGuiada = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = Color.White
        )
    }

    // Modal de Reporte y Video MP4 (Opción C)
    if (mostrarModalReporteTexto != null) {
        AlertDialog(
            onDismissRequest = { mostrarModalReporteTexto = null },
            title = {
                Text("🎬 Opción C: Reporte y Animación de Ruta", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = mostrarModalReporteTexto!!,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Puedes compilar la animación 2D en formato video MP4 y guardarla directamente en tu Galería.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarModalReporteTexto = null
                        val projManager = contexto.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        mediaProjectionLauncher.launch(projManager.createScreenCaptureIntent())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E24AA))
                ) {
                    Icon(Icons.Default.VideoCameraBack, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Grabar Video MP4", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalReporteTexto = null }) {
                    Text("Cerrar")
                }
            },
            containerColor = Color.White
        )
    }

    // Overlay visual para foto inyectada en vivo durante la animación 2D (Relive)
    if (fotoEnPantalla != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 14.dp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFFFF6B00), modifier = Modifier.size(18.dp))
                            Text("Foto en Ruta", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                        }
                        Text("Pausa 2.5s", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                    }

                    AsyncImage(
                        model = fotoEnPantalla!!.uriFoto,
                        contentDescription = "Foto en Ruta",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )

                    if (fotoEnPantalla!!.descripcion.isNotBlank()) {
                        Text(
                            text = fotoEnPantalla!!.descripcion,
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaEstudioReproduccion2D(
    resumenRuta: ResumenRutaTX,
    estadoReproductor: EstadoReproductorRuta,
    telemetria: TelemetriaReproduccion,
    grabandoVideo: Boolean,
    onIniciarReproduccion: () -> Unit,
    onPausarOReanudar: () -> Unit,
    onDetener: () -> Unit,
    onGrabarVideo: () -> Unit,
    onFinalizarVideo: () -> Unit
) {
    val estaReproduciendo = estadoReproductor == EstadoReproductorRuta.REPRODUCIENDO || estadoReproductor == EstadoReproductorRuta.PAUSADO_FOTO
    val estaEnPausa = estadoReproductor == EstadoReproductorRuta.PAUSADO_MANUAL

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieCreation,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Estudio 2D Relive & Video",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = when (estadoReproductor) {
                                EstadoReproductorRuta.REPRODUCIENDO -> "Reproduciendo recorrido con OsmAnd"
                                EstadoReproductorRuta.PAUSADO_FOTO -> "Pausa fotográfica en ruta"
                                EstadoReproductorRuta.PAUSADO_MANUAL -> "Animación pausada"
                                EstadoReproductorRuta.FINALIZADO -> "Recorrido completado"
                                else -> "Listo para simular la ruta"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                if (grabandoVideo) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFE53935), CircleShape))
                            Text("GRABANDO MP4", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                        }
                    }
                }
            }

            // Barra de progreso interactiva
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { telemetria.porcentajeProgreso },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF8B5CF6),
                    trackColor = Color(0xFFEDE9FE)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Punto ${telemetria.indicePuntoActual + 1} de ${telemetria.totalPuntos.coerceAtLeast(resumenRuta.puntos.size)}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "%.0f%%".format(telemetria.porcentajeProgreso * 100),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5CF6)
                    )
                }
            }

            // Métricas instantáneas de la animación
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rumbo (Azimuth)", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text("%.0f°".format(telemetria.rumboAzimuth), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Velocidad Sim.", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text("%.1f Km/h".format(telemetria.velocidadSimuladaKmh), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFFF6B00))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Distancia Sim.", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text("%.2f Km".format(telemetria.distanciaRecorridaKm), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0288D1))
                }
            }

            // Botones de acción del reproductor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!estaReproduciendo && !estaEnPausa) {
                    Button(
                        onClick = onIniciarReproduccion,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reproducir 2D", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onPausarOReanudar,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (estaEnPausa) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (estaEnPausa) "Reanudar" else "Pausar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDetener,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE53935)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Detener", color = Color(0xFFE53935), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Botón Grabar / Finalizar Video MP4
                if (!grabandoVideo) {
                    Button(
                        onClick = onGrabarVideo,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FiberManualRecord, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Grabar MP4", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onFinalizarVideo,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Guardar MP4", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaSeguroBateria(
    nivelBateria: Int,
    respaldoEjecutado: Boolean,
    estadoRuta: EstadoRutaEnum
) {
    val esBateriaBaja = nivelBateria <= 15
    val colorBateria = when {
        nivelBateria <= 5 -> Color(0xFFE53935)
        nivelBateria <= 15 -> Color(0xFFFF9800)
        else -> Color(0xFF43A047)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (esBateriaBaja) Color(0xFFFFEBEE) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, colorBateria.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colorBateria.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = if (nivelBateria <= 15) Icons.Default.BatteryAlert else Icons.Default.BatteryFull,
                    contentDescription = null,
                    tint = colorBateria,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Seguro de Vida (Batería): $nivelBateria%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color(0xFF0F172A)
                    )
                }
                Text(
                    text = if (respaldoEjecutado) "⚡ Respaldo automático ejecutado en Firebase (NUBE.kt)" else "Monitoreo activo. Si baja de 5%, la ruta se guardará en la nube automáticamente.",
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}

@Composable
private fun TarjetaTelemetriaRuta(
    resumenRuta: ResumenRutaTX,
    servicioActivo: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = resumenRuta.tituloRuta,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Estado: ${resumenRuta.estadoRuta.name}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (resumenRuta.estadoRuta) {
                            EstadoRutaEnum.GRABANDO -> Color(0xFF2E7D32)
                            EstadoRutaEnum.PAUSADA -> Color(0xFFFF9800)
                            EstadoRutaEnum.RESPALDADA_BATERIA -> Color(0xFFE53935)
                            else -> Color(0xFF64748B)
                        }
                    )
                }

                if (servicioActivo) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "🟢 EN SEGUNDO PLANO",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(14.dp))

            // Fila de Estadísticas en tiempo real
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ItemMetricaTelemetria(
                    titulo = "Distancia",
                    valor = String.format(Locale.getDefault(), "%.2f", resumenRuta.distanciaTotalKm),
                    unidad = "Km",
                    color = Color(0xFFFF6B00)
                )

                ItemMetricaTelemetria(
                    titulo = "Vel. Máxima",
                    valor = String.format(Locale.getDefault(), "%.1f", resumenRuta.velocidadMaximaKmh),
                    unidad = "Km/h",
                    color = Color(0xFFD81B60)
                )

                ItemMetricaTelemetria(
                    titulo = "Vel. Promedio",
                    valor = String.format(Locale.getDefault(), "%.1f", resumenRuta.velocidadPromedioKmh),
                    unidad = "Km/h",
                    color = Color(0xFF0288D1)
                )

                ItemMetricaTelemetria(
                    titulo = "Puntos GPS",
                    valor = "${resumenRuta.puntos.size}",
                    unidad = "pts",
                    color = Color(0xFF8E24AA)
                )
            }
        }
    }
}

@Composable
private fun ItemMetricaTelemetria(
    titulo: String,
    valor: String,
    unidad: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = titulo, fontSize = 10.5.sp, color = Color(0xFF64748B))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = valor,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                color = color
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(text = unidad, fontSize = 10.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable
private fun CardOpcionRuta(
    titulo: String,
    subtitulo: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    colorIcono: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colorIcono.copy(alpha = 0.12f))
            ) {
                Icon(imageVector = icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(22.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = titulo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                Text(text = subtitulo, fontSize = 11.sp, color = Color(0xFF64748B))
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
        }
    }
}
