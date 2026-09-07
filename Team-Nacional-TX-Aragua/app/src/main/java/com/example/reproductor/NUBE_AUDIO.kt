package com.example.reproductor

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MotoGoldSecondary
import com.example.ui.theme.MotoOrangePrimary
import com.example.ui.theme.TxFlameRed
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ═══════════════════════════════════════════════════════════════════════════
// LA NUBE (WIDGET FLOTANTE IN-APP) - REPRODUCTOR TX PRO (TEAM NACIONAL TX)
// Mini reproductor flotante con auto-ocultación lateral izquierda (lengüeta).
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun NubeAudioFlotante(
    cancionActual: CancionMotera?,
    estado: EstadoReproductor,
    config: ConfiguracionReproductor,
    onAnterior: () -> Unit = {},
    onAlternarPlayPausa: () -> Unit,
    onSiguiente: () -> Unit,
    onAbrirReproductor: () -> Unit,
    onActualizarPosicion: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!config.nubeFlotanteActiva || cancionActual == null) return

    val density = LocalDensity.current
    val configPantalla = LocalConfiguration.current
    val maxAnchoPx = with(density) { configPantalla.screenWidthDp.dp.toPx() }
    val maxAltoPx = with(density) { configPantalla.screenHeightDp.dp.toPx() }

    var offsetX by remember { mutableStateOf(config.posicionNubeX) }
    var offsetY by remember { mutableStateOf(config.posicionNubeY) }

    var estaColapsadaALaIzquierda by remember { mutableStateOf(false) }
    var ultimoToqueMs by remember { mutableStateOf(System.currentTimeMillis()) }

    val esPlaying = estado == EstadoReproductor.REPRODUCIENDO

    // Temporizador de auto-ocultación a la izquierda tras 4 segundos de inactividad
    LaunchedEffect(ultimoToqueMs, estaColapsadaALaIzquierda, config.autoOcultarNube) {
        if (config.autoOcultarNube && !estaColapsadaALaIzquierda) {
            delay(4000)
            estaColapsadaALaIzquierda = true
        }
    }

    val posXAnimada by animateFloatAsState(
        targetValue = if (estaColapsadaALaIzquierda) 0f else offsetX,
        animationSpec = tween(durationMillis = 350),
        label = "posXNube"
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (estaColapsadaALaIzquierda) {
            // ─────────────────────────────────────────────────────────────────
            // MODO DOCKED / LENGÜETA LATERAL IZQUIERDA MINIMALISTA
            // ─────────────────────────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .offset { IntOffset(0, offsetY.roundToInt()) }
                    .shadow(10.dp, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp), spotColor = Color(0x33000000))
                    .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                    .clickable {
                        estaColapsadaALaIzquierda = false
                        ultimoToqueMs = System.currentTimeMillis()
                    }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.98f),
                                Color(0xFFF8FAFC).copy(alpha = 0.98f)
                            )
                        )
                    ),
                color = Color.Transparent,
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                border = BorderStroke(1.dp, if (esPlaying) MotoOrangePrimary else Color(0xFFCBD5E1))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(if (esPlaying) "🔥" else "🎵", fontSize = 14.sp)
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Expandir Nube",
                        tint = MotoOrangePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            // ─────────────────────────────────────────────────────────────────
            // MODO COMPLETO FLOTANTE EXPANDIDO
            // ─────────────────────────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .offset { IntOffset(posXAnimada.roundToInt(), offsetY.roundToInt()) }
                    .pointerInput(config.nubeBloqueada) {
                        if (!config.nubeBloqueada) {
                            detectDragGestures(
                                onDragStart = {
                                    ultimoToqueMs = System.currentTimeMillis()
                                },
                                onDragEnd = {
                                    onActualizarPosicion(offsetX, offsetY)
                                    ultimoToqueMs = System.currentTimeMillis()
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                ultimoToqueMs = System.currentTimeMillis()
                                offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxAnchoPx - 260f)
                                offsetY = (offsetY + dragAmount.y).coerceIn(30f, maxAltoPx - 160f)
                                if (offsetX < 15f) {
                                    estaColapsadaALaIzquierda = true
                                }
                            }
                        }
                    }
                    .shadow(10.dp, RoundedCornerShape(26.dp), spotColor = Color(0x33000000))
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.98f),
                                Color(0xFFF8FAFC).copy(alpha = 0.98f)
                            )
                        )
                    ),
                color = Color.Transparent,
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.5.dp, if (esPlaying) MotoOrangePrimary else Color(0xFFCBD5E1))
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .widthIn(min = 230.dp, max = 285.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Botón para colapsar rápidamente a la izquierda
                    IconButton(
                        onClick = { estaColapsadaALaIzquierda = true },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Ocultar a la izquierda",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Icono animado de disco/fuego
                    Surface(
                        shape = CircleShape,
                        color = if (esPlaying) MotoOrangePrimary.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(if (esPlaying) "🔥" else "🎵", fontSize = 12.sp)
                        }
                    }

                    // Título y Artista
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                ultimoToqueMs = System.currentTimeMillis()
                                onAbrirReproductor()
                            }
                    ) {
                        Text(
                            text = cancionActual.titulo,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = cancionActual.artista,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MotoOrangePrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Controles Anterior, Play/Pause y Siguiente
                    IconButton(
                        onClick = {
                            ultimoToqueMs = System.currentTimeMillis()
                            onAnterior()
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Anterior",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            ultimoToqueMs = System.currentTimeMillis()
                            onAlternarPlayPausa()
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (esPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pausa",
                            tint = MotoOrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            ultimoToqueMs = System.currentTimeMillis()
                            onSiguiente()
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Botón Maximizar a Pantalla Completa
                    IconButton(
                        onClick = onAbrirReproductor,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Maximizar",
                            tint = MotoGoldSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
