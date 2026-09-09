package com.example.rutas

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dashboard.DashboardFondoConfig
import com.example.data.model.MemberProfile

/**
 * Menú Táctico Desplegable (BottomSheet) para el Botón Central Inferior de Rutas TX.
 * Ofrece acceso instantáneo a las 3 opciones clave y al estado del Seguro de Batería.
 *
 * Opciones:
 * - Opción A: Iniciar / Pausar / Detener Grabación Táctica en Vivo.
 * - Opción B: Crear Ruta Guiada.
 * - Opción C: Generar Reporte y Video 2D Relive.
 * - Acceso directo al panel completo de rutas.
 *
 * Diseño 100% en Tema Claro acorde con DashboardFondoConfig y reglas de MODULOS_GESTOR.md.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuRutasBottomSheet(
    currentMember: MemberProfile?,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onNavegarAPanelCompleto: () -> Unit,
    onCrearRutaGuiada: () -> Unit,
    onGenerarVideoYReporte: () -> Unit
) {
    val contexto = LocalContext.current
    val resumenRuta by RUTA.resumenRutaState.collectAsStateWithLifecycle()
    val servicioActivo by SERVICIO.servicioActivoState.collectAsStateWithLifecycle()
    val nivelBateria by RUTA.bateriaNivelState.collectAsStateWithLifecycle()

    var mostrarDialogoIniciarRuta by remember { mutableStateOf(false) }
    var tituloRutaInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFFCBD5E1))
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cabecera del BottomSheet
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Rutas TX & Telemetría",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Menú táctico del botón central",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Indicador de batería en vivo (Seguro de Vida)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (nivelBateria <= 5) Color(0xFFFFEBEE) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (nivelBateria <= 5) Color(0xFFE53935) else Color(0xFFCBD5E1)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                nivelBateria <= 20 -> Icons.Default.BatteryAlert
                                else -> Icons.Default.BatteryFull
                            },
                            contentDescription = "Batería",
                            tint = if (nivelBateria <= 5) Color(0xFFE53935) else Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$nivelBateria%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (nivelBateria <= 5) Color(0xFFE53935) else Color(0xFF0F172A)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Estado de la grabación actual si está activa
            if (resumenRuta.estadoRuta == EstadoRutaEnum.GRABANDO || resumenRuta.estadoRuta == EstadoRutaEnum.PAUSADA) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF7ED),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEDD5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (resumenRuta.estadoRuta == EstadoRutaEnum.GRABANDO) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        CircleShape
                                    )
                            )
                            Column {
                                Text(
                                    text = resumenRuta.tituloRuta,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "%.2f Km recorridos • %.1f Km/h máx".format(
                                        resumenRuta.distanciaTotalKm,
                                        resumenRuta.velocidadMaximaKmh
                                    ),
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Text(
                            text = if (resumenRuta.estadoRuta == EstadoRutaEnum.GRABANDO) "GRABANDO" else "PAUSADA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (resumenRuta.estadoRuta == EstadoRutaEnum.GRABANDO) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                    }
                }
            }

            // ─── OPCIÓN A: Iniciar / Pausar / Detener Grabación ─────────────
            TarjetaOpcionRuta(
                titulo = when (resumenRuta.estadoRuta) {
                    EstadoRutaEnum.GRABANDO -> "Detener o Pausar Grabación"
                    EstadoRutaEnum.PAUSADA -> "Reanudar Grabación de Ruta"
                    else -> "Opción A: Iniciar Grabación Táctica"
                },
                subtitulo = when (resumenRuta.estadoRuta) {
                    EstadoRutaEnum.GRABANDO -> "Tracking continuo en segundo plano activo con GPS"
                    EstadoRutaEnum.PAUSADA -> "La ruta se encuentra en pausa temporal"
                    else -> "Comenzar a grabar puntos GPS, velocidad y kilometraje"
                },
                icono = when (resumenRuta.estadoRuta) {
                    EstadoRutaEnum.GRABANDO -> Icons.Default.Stop
                    EstadoRutaEnum.PAUSADA -> Icons.Default.PlayArrow
                    else -> Icons.Default.FiberManualRecord
                },
                colorIcono = when (resumenRuta.estadoRuta) {
                    EstadoRutaEnum.GRABANDO -> Color(0xFFE53935)
                    EstadoRutaEnum.PAUSADA -> Color(0xFFF59E0B)
                    else -> Color(0xFFFF6B00)
                },
                onClick = {
                    when (resumenRuta.estadoRuta) {
                        EstadoRutaEnum.GRABANDO -> {
                            INDICE_RUTAS.detenerTracking(contexto)
                            Toast.makeText(contexto, "🏁 Ruta detenida y guardada localmente", Toast.LENGTH_SHORT).show()
                        }
                        EstadoRutaEnum.PAUSADA -> {
                            RUTA.reanudarRuta()
                            Toast.makeText(contexto, "▶️ Grabación reanudada", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            mostrarDialogoIniciarRuta = true
                        }
                    }
                }
            )

            // ─── OPCIÓN B: Crear Ruta Guiada ────────────────────────────────
            TarjetaOpcionRuta(
                titulo = "Opción B: Crear Ruta Guiada",
                subtitulo = "Planificar origen, destino, puntos de encuentro y waypoints",
                icono = Icons.Default.AltRoute,
                colorIcono = Color(0xFF0284C7),
                onClick = {
                    onDismiss()
                    onCrearRutaGuiada()
                }
            )

            // ─── OPCIÓN C: Generar Reporte y Video 2D (Relive) ──────────────
            TarjetaOpcionRuta(
                titulo = "Opción C: Generar Reporte & Video 2D",
                subtitulo = "Estudio cinemático con OsmAnd, fotos y video MP4 en Galería",
                icono = Icons.Default.VideoCameraBack,
                colorIcono = Color(0xFF8B5CF6),
                onClick = {
                    onDismiss()
                    onGenerarVideoYReporte()
                }
            )

            // Botón de acceso al Panel Completo
            Button(
                onClick = {
                    onDismiss()
                    onNavegarAPanelCompleto()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.DashboardCustomize, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Abrir Panel Completo de Rutas", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    // Diálogo para nombrar la ruta al iniciar la Opción A
    if (mostrarDialogoIniciarRuta) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoIniciarRuta = false },
            title = {
                Text(
                    "Iniciar Grabación de Ruta",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Asigna un título a la ruta para identificarla en el historial y en los reportes del club:",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    OutlinedTextField(
                        value = tituloRutaInput,
                        onValueChange = { tituloRutaInput = it },
                        placeholder = { Text("Ej: Rodada Colonia Tovar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idPiloto = currentMember?.id?.toString() ?: "0"
                        val nombrePiloto = currentMember?.nickname?.ifBlank { currentMember.fullName } ?: "Piloto TX"
                        val titulo = tituloRutaInput.ifBlank { "Ruta TX ${System.currentTimeMillis()}" }

                        INDICE_RUTAS.iniciarTracking(contexto, titulo, idPiloto, nombrePiloto)
                        mostrarDialogoIniciarRuta = false
                        Toast.makeText(contexto, "🟢 Grabando ruta: $titulo", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Iniciar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoIniciarRuta = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
private fun TarjetaOpcionRuta(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    colorIcono: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorIcono.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = colorIcono,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = subtitulo,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 15.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
