package com.example.meshtx

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * BOTÓN PTT FLOTANTE MOVIPLE EN PANTALLA (OVERLAY TÁCTICO MESH TX)
 * ═══════════════════════════════════════════════════════════════════════════
 * Permite mantener la comunicación por voz (pulsar para hablar) en cualquier
 * pantalla de la app (Mapa, Rodadas, SOS, Chat, etc.) sin necesidad de estar
 * dentro del módulo de Mesh TX.
 *
 * Características:
 * - Arrastrable libremente por toda la pantalla del teléfono.
 * - Algoritmo antirrebote para digitalizadores táctiles.
 * - Indicador visual claro (Naranja en reposo, Rojo 'AL AIRE' al transmitir).
 * - Monitor especial cuando está en Protocolo Alcabala en vivo.
 */
@Composable
fun BotonPttFlotanteOverlay(
    modifier: Modifier = Modifier
) {
    val estaTransmitiendo by GestorMeshTx.estaTransmitiendoPtt.collectAsState()
    val modoAlcabala by GestorMeshTx.modoAlcabalaEnVivoActivo.collectAsState()
    val canalActual by GestorMeshTx.canalActual.collectAsState()
    val salaPrivada by GestorMeshTx.salaPrivadaActiva.collectAsState()

    var offsetX by remember { mutableFloatStateOf(40f) }
    var offsetY by remember { mutableFloatStateOf(350f) }

    val transicionPulsante = rememberInfiniteTransition(label = "pulse_ptt_overlay")
    val escalaAnim by transicionPulsante.animateFloat(
        initialValue = 1.0f,
        targetValue = if (estaTransmitiendo || modoAlcabala) 1.12f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escala_overlay"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(10f, 850f)
                        offsetY = (offsetY + dragAmount.y).coerceIn(60f, 1800f)
                    }
                }
        ) {
            // Badge superior con el Canal o Sala
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (modoAlcabala) Color(0xFFDC2626) else if (estaTransmitiendo) Color(0xFFDC2626) else Color(0xFF0F172A),
                shadowElevation = 3.dp,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when {
                            modoAlcabala -> "🔴 ALCABALA VIVO"
                            estaTransmitiendo -> "🔴 AL AIRE"
                            salaPrivada != null -> "🔒 $salaPrivada"
                            else -> "CH ${canalActual.idCanal}"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    // Botoncito de cerrar overlay
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar Botón Flotante",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    GestorMeshTx.alternarBotonFlotantePtt(false)
                                    if (modoAlcabala) {
                                        GestorMeshTx.desactivarModoAlcabalaSos()
                                    }
                                }
                            }
                    )
                }
            }

            // Botón Circular Principal PTT
            Surface(
                shape = CircleShape,
                color = when {
                    modoAlcabala -> Color(0xFF991B1B)
                    estaTransmitiendo -> Color(0xFFDC2626)
                    else -> Color(0xFFFF6B00)
                },
                border = BorderStroke(
                    2.dp,
                    if (estaTransmitiendo || modoAlcabala) Color.White else Color(0xFFFFD8A8)
                ),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .pointerInput(modoAlcabala) {
                        if (modoAlcabala) {
                            detectTapGestures(
                                onTap = { GestorMeshTx.desactivarModoAlcabalaSos() }
                            )
                        } else {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitFirstDown(requireUnconsumed = false)
                                    GestorMeshTx.setTransmitiendoPtt(true)
                                    var presionado = true
                                    while (presionado) {
                                        val evento = awaitPointerEvent()
                                        if (evento.changes.all { !it.pressed }) {
                                            presionado = false
                                        }
                                    }
                                    GestorMeshTx.setTransmitiendoPtt(false)
                                }
                            }
                        }
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (estaTransmitiendo || modoAlcabala) Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = "PTT Flotante",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (modoAlcabala) "EN VIVO" else if (estaTransmitiendo) "HABLANDO" else "PTT",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
