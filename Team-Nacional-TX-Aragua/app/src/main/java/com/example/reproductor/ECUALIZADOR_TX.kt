package com.example.reproductor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MotoGoldSecondary
import com.example.ui.theme.MotoOrangePrimary
import com.example.ui.theme.TxFlameRed

// ═══════════════════════════════════════════════════════════════════════════
// ECUALIZADOR REAL Y EFECTOS DSP - REPRODUCTOR TX PRO (TEAM NACIONAL TX ARAGUA)
// Tema Claro Biker, 5 Sliders Verticales Reales, Ultra Volumen y Super Bass.
// ═══════════════════════════════════════════════════════════════════════════

object PRESETS_AUDIO {
    val LISTA_PRESETS = listOf(
        PresetEcualizador(
            nombre = "Rock Motero",
            gananciasDb = floatArrayOf(4.5f, 2.0f, -1.0f, 3.0f, 5.0f),
            superBass = 0.55f,
            ultraVolumen = 1.35f,
            espacialidad = 0.35f
        ),
        PresetEcualizador(
            nombre = "Carretera / Viento",
            gananciasDb = floatArrayOf(6.0f, 3.5f, 1.0f, 4.0f, 6.5f),
            superBass = 0.75f,
            ultraVolumen = 1.60f,
            espacialidad = 0.40f
        ),
        PresetEcualizador(
            nombre = "Bass Boost TX",
            gananciasDb = floatArrayOf(7.5f, 5.0f, 0.0f, 1.5f, 2.0f),
            superBass = 0.90f,
            ultraVolumen = 1.25f,
            espacialidad = 0.20f
        ),
        PresetEcualizador(
            nombre = "Electrónica / Club",
            gananciasDb = floatArrayOf(5.0f, 3.0f, -2.0f, 2.5f, 5.5f),
            superBass = 0.60f,
            ultraVolumen = 1.30f,
            espacialidad = 0.50f
        ),
        PresetEcualizador(
            nombre = "Vocal / Acústico",
            gananciasDb = floatArrayOf(-1.0f, 2.0f, 4.5f, 3.0f, 1.0f),
            superBass = 0.15f,
            ultraVolumen = 1.10f,
            espacialidad = 0.30f
        ),
        PresetEcualizador(
            nombre = "Plano / Flat",
            gananciasDb = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
            superBass = 0.0f,
            ultraVolumen = 1.0f,
            espacialidad = 0.0f
        )
    )
}

@Composable
fun PanelEcualizadorTX(
    config: ConfiguracionReproductor,
    onActualizarConfig: (ConfiguracionReproductor) -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nombresBandas = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    val scrollState = rememberScrollState()

    var localUltraVol by remember(config.ultraVolumenNivel) { mutableStateOf(config.ultraVolumenNivel) }
    var localSuperBass by remember(config.superBassNivel) { mutableStateOf(config.superBassNivel) }
    var localEspacialidad by remember(config.espacialidadNivel) { mutableStateOf(config.espacialidadNivel) }
    var localBandas by remember(config.bandasEcualizador) { mutableStateOf(config.bandasEcualizador.clone()) }

    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // Tirador superior modal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFCBD5E1))
                )
            }

            // Cabecera del Ecualizador
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = CircleShape, color = MotoOrangePrimary.copy(alpha = 0.12f)) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MotoOrangePrimary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Column {
                        Text(
                            "ECUALIZADOR PRO TX",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "Motor DSP Nativo sin distorsión • Modo Claro",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de Presets
            Text(
                "PRESETS MOTEROS RECOMENDADOS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PRESETS_AUDIO.LISTA_PRESETS.take(3).forEach { preset ->
                    val esSeleccionado = config.presetActual == preset.nombre
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (esSeleccionado) MotoOrangePrimary else Color.White,
                        border = BorderStroke(1.dp, if (esSeleccionado) MotoOrangePrimary else Color(0xFFE2E8F0)),
                        shadowElevation = if (esSeleccionado) 2.dp else 0.5.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                localUltraVol = preset.ultraVolumen
                                localSuperBass = preset.superBass
                                localEspacialidad = preset.espacialidad
                                localBandas = preset.gananciasDb.clone()
                                onActualizarConfig(
                                    config.copy(
                                        presetActual = preset.nombre,
                                        bandasEcualizador = preset.gananciasDb.clone(),
                                        superBassNivel = preset.superBass,
                                        ultraVolumenNivel = preset.ultraVolumen,
                                        espacialidadNivel = preset.espacialidad
                                    )
                                )
                            }
                    ) {
                        Text(
                            text = preset.nombre,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (esSeleccionado) Color.White else Color(0xFF334155),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PRESETS_AUDIO.LISTA_PRESETS.drop(3).forEach { preset ->
                    val esSeleccionado = config.presetActual == preset.nombre
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (esSeleccionado) MotoOrangePrimary else Color.White,
                        border = BorderStroke(1.dp, if (esSeleccionado) MotoOrangePrimary else Color(0xFFE2E8F0)),
                        shadowElevation = if (esSeleccionado) 2.dp else 0.5.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                localUltraVol = preset.ultraVolumen
                                localSuperBass = preset.superBass
                                localEspacialidad = preset.espacialidad
                                localBandas = preset.gananciasDb.clone()
                                onActualizarConfig(
                                    config.copy(
                                        presetActual = preset.nombre,
                                        bandasEcualizador = preset.gananciasDb.clone(),
                                        superBassNivel = preset.superBass,
                                        ultraVolumenNivel = preset.ultraVolumen,
                                        espacialidadNivel = preset.espacialidad
                                    )
                                )
                            }
                    ) {
                        Text(
                            text = preset.nombre,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (esSeleccionado) Color.White else Color(0xFF334155),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ═══════════════════════════════════════════════════════════════════════
            // CONTROLES POTENCIADORES: ULTRA VOLUMEN, SUPER BASS, ESPACIALIDAD 3D
            // ═══════════════════════════════════════════════════════════════════════
            Text(
                "POTENCIADORES DE CASCO Y CARRETERA",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // 1. ULTRA VOLUMEN (+100% a +300%)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🔊", fontSize = 14.sp)
                                Text("ULTRA VOLUMEN PRO", fontSize = 13.sp, fontWeight = FontWeight.Black, color = TxFlameRed)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = TxFlameRed.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "${(localUltraVol * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TxFlameRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Slider(
                            value = localUltraVol,
                            onValueChange = { nuevoValor ->
                                localUltraVol = nuevoValor
                                MOTOR_AUDIO_NATIVO.aplicarUltraVolumen(nuevoValor)
                            },
                            onValueChangeFinished = {
                                onActualizarConfig(config.copy(ultraVolumenNivel = localUltraVol, presetActual = "Personalizado"))
                            },
                            valueRange = 1.0f..3.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = TxFlameRed,
                                activeTrackColor = TxFlameRed,
                                inactiveTrackColor = Color(0xFFF1F5F9)
                            )
                        )
                    }

                    // 2. SUPER BASS (Graves para carretera)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🚀", fontSize = 14.sp)
                                Text("SUPER BASS (Sub-Graves)", fontSize = 13.sp, fontWeight = FontWeight.Black, color = MotoOrangePrimary)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MotoOrangePrimary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "${(localSuperBass * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MotoOrangePrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Slider(
                            value = localSuperBass,
                            onValueChange = { nuevoValor ->
                                localSuperBass = nuevoValor
                                MOTOR_AUDIO_NATIVO.aplicarSuperBass(nuevoValor)
                            },
                            onValueChangeFinished = {
                                onActualizarConfig(config.copy(superBassNivel = localSuperBass, presetActual = "Personalizado"))
                            },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MotoOrangePrimary,
                                activeTrackColor = MotoOrangePrimary,
                                inactiveTrackColor = Color(0xFFF1F5F9)
                            )
                        )
                    }

                    // 3. ESPACIALIDAD 3D (Surround estéreo)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🌌", fontSize = 14.sp)
                                Text("ESPACIALIDAD 3D (Surround)", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0284C7))
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "${(localEspacialidad * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0284C7),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Slider(
                            value = localEspacialidad,
                            onValueChange = { nuevoValor ->
                                localEspacialidad = nuevoValor
                                MOTOR_AUDIO_NATIVO.aplicarEspacialidad(nuevoValor)
                            },
                            onValueChangeFinished = {
                                onActualizarConfig(config.copy(espacialidadNivel = localEspacialidad, presetActual = "Personalizado"))
                            },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF0284C7),
                                activeTrackColor = Color(0xFF0284C7),
                                inactiveTrackColor = Color(0xFFF1F5F9)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ═══════════════════════════════════════════════════════════════════════
            // 5 SLIDERS VERTICALES ERGONÓMICOS (-15dB a +15dB)
            // ═══════════════════════════════════════════════════════════════════════
            Text(
                "ECUALIZADOR GRÁFICO (5 BANDAS VERTICALES)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    localBandas.forEachIndexed { indice, gananciaDb ->
                        val nombreBanda = nombresBandas.getOrElse(indice) { "${indice + 1}" }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(58.dp)
                        ) {
                            Text(
                                text = "${if (gananciaDb > 0) "+" else ""}${gananciaDb.toInt()}dB",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (gananciaDb > 0f) MotoOrangePrimary else if (gananciaDb < 0f) Color(0xFF64748B) else Color(0xFF0F172A)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            SliderVertical(
                                value = gananciaDb,
                                onValueChange = { nuevoDb ->
                                    val nuevasBandas = localBandas.clone()
                                    nuevasBandas[indice] = nuevoDb
                                    localBandas = nuevasBandas
                                    MOTOR_AUDIO_NATIVO.aplicarBandasEcualizador(nuevasBandas)
                                },
                                onValueChangeFinished = {
                                    onActualizarConfig(config.copy(bandasEcualizador = localBandas.clone(), presetActual = "Personalizado"))
                                },
                                valueRange = -15.0f..15.0f,
                                activeColor = MotoOrangePrimary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = nombreBanda,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Botón de restablecer ecualizador a plano
            OutlinedButton(
                onClick = {
                    val bandasPlanas = floatArrayOf(0f, 0f, 0f, 0f, 0f)
                    localUltraVol = 1.0f
                    localSuperBass = 0f
                    localEspacialidad = 0f
                    localBandas = bandasPlanas
                    onActualizarConfig(
                        config.copy(
                            presetActual = "Plano / Flat",
                            bandasEcualizador = bandasPlanas,
                            superBassNivel = 0f,
                            ultraVolumenNivel = 1.0f,
                            espacialidadNivel = 0f
                        )
                    )
                },
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restablecer a Valores Predeterminados", color = Color(0xFF334155), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SLIDER VERTICAL COMPOSABLE ERGONÓMICO
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SliderVertical(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(140.dp)
            .width(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor,
                inactiveTrackColor = Color(0xFFE2E8F0)
            ),
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = 270f
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                .width(130.dp)
                .height(44.dp)
        )
    }
}
