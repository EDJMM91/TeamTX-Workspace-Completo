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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MotoGoldSecondary
import com.example.ui.theme.MotoOrangePrimary
import com.example.ui.theme.TxFlameRed

// ═══════════════════════════════════════════════════════════════════════════
// ECUALIZADOR REAL Y EFECTOS DSP - REPRODUCTOR TX PRO (TEAM NACIONAL TX ARAGUA)
// Controles de Super Bass, Ultra Volumen (+300%), Espacialidad 3D y 5 Bandas.
// Sin ruido acústico ni micro-congelamientos gracias a aislamiento de estado.
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

    // Estados locales para evitar recalcular y saturar audio al arrastrar perillas
    var localUltraVol by remember(config.ultraVolumenNivel) { mutableStateOf(config.ultraVolumenNivel) }
    var localSuperBass by remember(config.superBassNivel) { mutableStateOf(config.superBassNivel) }
    var localEspacialidad by remember(config.espacialidadNivel) { mutableStateOf(config.espacialidadNivel) }
    var localBandas by remember(config.bandasEcualizador) { mutableStateOf(config.bandasEcualizador.clone()) }

    Surface(
        color = Color(0xFF0F131C),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Cabecera del Ecualizador
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = MotoOrangePrimary.copy(alpha = 0.2f)) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.padding(6.dp))
                    }
                    Column {
                        Text("ECUALIZADOR PRO TX", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                        Text("Motor DSP Nativo sin distorsión ni zumbidos", fontSize = 11.sp, color = Color(0xFF90A4AE))
                    }
                }
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF90A4AE))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selector de Presets
            Text("PRESETS MOTEROS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PRESETS_AUDIO.LISTA_PRESETS.take(3).forEach { preset ->
                    val esSeleccionado = config.presetActual == preset.nombre
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (esSeleccionado) MotoOrangePrimary else Color(0xFF1E2433),
                        border = BorderStroke(1.dp, if (esSeleccionado) MotoGoldSecondary else Color(0xFF374151)),
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
                            color = if (esSeleccionado) Color.White else Color(0xFFCFD8DC),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PRESETS_AUDIO.LISTA_PRESETS.drop(3).forEach { preset ->
                    val esSeleccionado = config.presetActual == preset.nombre
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (esSeleccionado) MotoOrangePrimary else Color(0xFF1E2433),
                        border = BorderStroke(1.dp, if (esSeleccionado) MotoGoldSecondary else Color(0xFF374151)),
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
                            color = if (esSeleccionado) Color.White else Color(0xFFCFD8DC),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════════════════════════════════
            // CONTROLES POTENCIADORES: ULTRA VOLUMEN, SUPER BASS, ESPACIALIDAD 3D
            // ═══════════════════════════════════════════════════════════════════════
            Surface(
                color = Color(0xFF161B26),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF263238)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 1. ULTRA VOLUMEN (+100% a +300%)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🔊", fontSize = 14.sp)
                                Text("ULTRA VOLUMEN PRO", fontSize = 12.sp, fontWeight = FontWeight.Black, color = TxFlameRed)
                            }
                            Text(
                                text = "${(localUltraVol * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (localUltraVol > 1.5f) TxFlameRed else MotoGoldSecondary
                            )
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
                                inactiveTrackColor = Color(0xFF374151)
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🚀", fontSize = 14.sp)
                                Text("SUPER BASS (Sub-Graves)", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MotoOrangePrimary)
                            }
                            Text(
                                text = "${(localSuperBass * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = MotoOrangePrimary
                            )
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
                                inactiveTrackColor = Color(0xFF374151)
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🌌", fontSize = 14.sp)
                                Text("ESPACIALIDAD 3D (Surround)", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF38BDF8))
                            }
                            Text(
                                text = "${(localEspacialidad * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF38BDF8)
                            )
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
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = Color(0xFF38BDF8),
                                inactiveTrackColor = Color(0xFF374151)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════════════════════════════════
            // SLIDERS DE LAS 5 BANDAS DEL ECUALIZADOR (CON FILTRO SIN CHISPORROTEO)
            // ═══════════════════════════════════════════════════════════════════════
            Text("BANDAS DE FRECUENCIA (-15dB a +15dB)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary)
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = Color(0xFF161B26),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF263238)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    localBandas.forEachIndexed { indice, gananciaDb ->
                        val nombreBanda = nombresBandas.getOrElse(indice) { "${indice + 1}" }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(55.dp)
                        ) {
                            Text(
                                text = "${if (gananciaDb > 0) "+" else ""}${gananciaDb.toInt()} dB",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (gananciaDb != 0f) MotoOrangePrimary else Color(0xFF90A4AE)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
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
                                colors = SliderDefaults.colors(
                                    thumbColor = MotoGoldSecondary,
                                    activeTrackColor = MotoGoldSecondary,
                                    inactiveTrackColor = Color(0xFF374151)
                                ),
                                modifier = Modifier.height(140.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = nombreBanda,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                border = BorderStroke(1.dp, Color(0xFF475569)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFCFD8DC), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restablecer a Valores Predeterminados", color = Color(0xFFCFD8DC), fontSize = 12.sp)
            }
        }
    }
}
