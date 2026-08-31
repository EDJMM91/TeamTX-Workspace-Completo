package com.example.reproductor

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MotoGoldSecondary
import com.example.ui.theme.MotoOrangePrimary
import com.example.ui.theme.TxFlameRed
import java.io.File

// ═══════════════════════════════════════════════════════════════════════════
// PANTALLA DE AJUSTES - REPRODUCTOR TX PRO (TEAM NACIONAL TX ARAGUA)
// Configuración dedicada: Escaneo de carpetas, Nube flotante, Carátulas y DSP.
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun VistaAjustesReproductor(
    config: ConfiguracionReproductor,
    totalCanciones: Int,
    totalCarpetas: Int,
    onActualizarConfig: (ConfiguracionReproductor) -> Unit,
    onReescanearMusica: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
            .padding(14.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cabecera
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = CircleShape, color = MotoOrangePrimary.copy(alpha = 0.2f)) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.padding(8.dp))
            }
            Column {
                Text("AJUSTES DE MÚSICA TX", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                Text("Personalización del reproductor, carátulas y burbuja flotante", fontSize = 11.sp, color = Color(0xFF90A4AE))
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // SECCIÓN 1: BIBLIOTECA Y ESCÁNER LOCAL
        // ═══════════════════════════════════════════════════════════════════════
        Text("BIBLIOTECA Y ALMACENAMIENTO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary)

        Surface(
            color = Color(0xFF161B26),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF263238)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Canciones Detectadas", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        Text("$totalCanciones pistas en $totalCarpetas carpetas locales", fontSize = 11.sp, color = Color(0xFF90A4AE))
                    }
                    Button(
                        onClick = onReescanearMusica,
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Escanear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color(0xFF263238))

                // Filtro de duración mínima (excluir notas de voz / ringtones)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Excluir audios cortos (< ${config.duracionMinimaSegundos}s)", fontSize = 12.sp, color = Color.White)
                        Text("${config.duracionMinimaSegundos} seg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary)
                    }
                    Slider(
                        value = config.duracionMinimaSegundos.toFloat(),
                        onValueChange = { nuevoSeg ->
                            onActualizarConfig(config.copy(duracionMinimaSegundos = nuevoSeg.toInt()))
                        },
                        valueRange = 0f..120f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = MotoGoldSecondary,
                            activeTrackColor = MotoGoldSecondary,
                            inactiveTrackColor = Color(0xFF374151)
                        )
                    )
                    Text("Ignora notas de voz cortas de WhatsApp, timbres y efectos de sonido.", fontSize = 10.sp, color = Color(0xFF78909C))
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // SECCIÓN 2: DESCARGA DE CARÁTULAS ONLINE (ÁLBUM / ARTISTA)
        // ═══════════════════════════════════════════════════════════════════════
        Text("CARÁTULAS Y PORTADAS (HD)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary)

        Surface(
            color = Color(0xFF161B26),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF263238)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Descarga Automática de Portadas Online",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
                Text(
                    "Si la pista local no tiene carátula, se buscará la imagen oficial en alta resolución en segundo plano.",
                    fontSize = 11.sp,
                    color = Color(0xFF90A4AE)
                )

                // Opciones de red para carátulas
                ModoDescargaCaratulas.values().forEach { modo ->
                    val esSeleccionado = config.descargaCaratulasModo == modo
                    Surface(
                        color = if (esSeleccionado) MotoOrangePrimary.copy(alpha = 0.15f) else Color(0xFF0F131C),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (esSeleccionado) MotoOrangePrimary else Color(0xFF374151)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onActualizarConfig(config.copy(descargaCaratulasModo = modo)) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = esSeleccionado,
                                onClick = { onActualizarConfig(config.copy(descargaCaratulasModo = modo)) },
                                colors = RadioButtonDefaults.colors(selectedColor = MotoOrangePrimary)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(modo.titulo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text(modo.descripcion, fontSize = 10.sp, color = Color(0xFF90A4AE))
                            }
                        }
                    }
                }

                // Botón para limpiar caché de carátulas
                OutlinedButton(
                    onClick = {
                        try {
                            val carpeta = File(contexto.cacheDir, "caratulas_tx")
                            if (carpeta.exists()) {
                                carpeta.deleteRecursively()
                                carpeta.mkdirs()
                            }
                            Toast.makeText(contexto, "Caché de carátulas liberada exitosamente.", Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {}
                    },
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFFCFD8DC), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Limpiar Caché de Carátulas", fontSize = 11.sp, color = Color(0xFFCFD8DC))
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // SECCIÓN 3: LA NUBE (BURBUJA FLOTANTE IN-APP)
        // ═══════════════════════════════════════════════════════════════════════
        Text("LA NUBE (WIDGET FLOTANTE EN APP)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary)

        Surface(
            color = Color(0xFF161B26),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF263238)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Activar Nube Flotante In-App", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        Text("Muestra un mini widget flotante para controlar la música mientras navegas en el Muro, Chat o Mapa.", fontSize = 11.sp, color = Color(0xFF90A4AE))
                    }
                    Switch(
                        checked = config.nubeFlotanteActiva,
                        onCheckedChange = { activada ->
                            onActualizarConfig(config.copy(nubeFlotanteActiva = activada))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MotoOrangePrimary
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFF263238))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-ocultar al Borde Izquierdo", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        Text("Tras 4 segundos de inactividad, se contrae a una pequeña lengüeta para no tapar contenido.", fontSize = 11.sp, color = Color(0xFF90A4AE))
                    }
                    Switch(
                        checked = config.autoOcultarNube,
                        onCheckedChange = { autoOcultar ->
                            onActualizarConfig(config.copy(autoOcultarNube = autoOcultar))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MotoOrangePrimary
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFF263238))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bloquear Posición de la Nube", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        Text("Evita arrastrar la burbuja por error mientras manejas la moto o usas la app.", fontSize = 11.sp, color = Color(0xFF90A4AE))
                    }
                    Switch(
                        checked = config.nubeBloqueada,
                        onCheckedChange = { bloqueada ->
                            onActualizarConfig(config.copy(nubeBloqueada = bloqueada))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TxFlameRed
                        )
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // SECCIÓN 4: MOTOR NATIVO C++ Y PROCESAMIENTO DSP
        // ═══════════════════════════════════════════════════════════════════════
        Text("MOTOR DSP NATIVO C++", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary)

        Surface(
            color = Color(0xFF161B26),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF263238)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚡", fontSize = 16.sp)
                    Text("Procesamiento Digital de Audio para Motos", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
                Text(
                    "El motor C++ (MOTOR_AUDIO.cpp) optimiza el ancho de banda y la respuesta dinámica en altavoces de motocicleta e intercomunicadores de casco Bluetooth, compensando ruidos aerodinámicos.",
                    fontSize = 11.sp,
                    color = Color(0xFF90A4AE)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xFF0F131C),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, Color(0xFF374151)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Librería Nativa:", fontSize = 11.sp, color = Color(0xFF90A4AE))
                        Text("libmotor_audio_tx.so", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MotoGoldSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
