package com.example.reproductor

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
// PANEL DE AJUSTES - REPRODUCTOR TX PRO (TEAM NACIONAL TX ARAGUA)
// Configuración de escaneo, carátulas, burbuja flotante in-app y DSP.
// Tema Claro Biker de Alto Contraste.
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun PanelAjustesReproductor(
    config: ConfiguracionReproductor,
    onActualizarConfig: (ConfiguracionReproductor) -> Unit,
    onReescanearMusica: () -> Unit,
    totalCanciones: Int,
    totalCarpetas: Int,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = CircleShape, color = MotoOrangePrimary.copy(alpha = 0.12f)) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.padding(8.dp))
            }
            Column {
                Text("AJUSTES DE MÚSICA TX", fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color(0xFF0F172A))
                Text("Personalización del reproductor, carátulas y burbuja flotante", fontSize = 11.sp, color = Color(0xFF64748B))
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // SECCIÓN 1: BIBLIOTECA Y ESCÁNER LOCAL
        // ═══════════════════════════════════════════════════════════════════════
        Text("BIBLIOTECA Y ALMACENAMIENTO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Canciones Detectadas", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text("$totalCanciones pistas en $totalCarpetas carpetas locales", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Button(
                        onClick = onReescanearMusica,
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Escanear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Filtro de duración mínima (excluir notas de voz / ringtones)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (config.duracionMinimaSegundos == 0) "Excluir audios cortos (Desactivado)" else "Excluir audios cortos (< ${config.duracionMinimaSegundos}s)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            if (config.duracionMinimaSegundos == 0) "Todos" else "${config.duracionMinimaSegundos} seg",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MotoOrangePrimary
                        )
                    }
                    Slider(
                        value = config.duracionMinimaSegundos.toFloat(),
                        onValueChange = { nuevoSeg ->
                            val seg = nuevoSeg.toInt()
                            onActualizarConfig(config.copy(duracionMinimaSegundos = seg, excluirAudiosCortos = seg > 0))
                        },
                        valueRange = 0f..120f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = MotoOrangePrimary,
                            activeTrackColor = MotoOrangePrimary,
                            inactiveTrackColor = Color(0xFFF1F5F9)
                        )
                    )
                    Text("Ignora notas de voz cortas de WhatsApp, timbres y efectos de sonido.", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // SECCIÓN 2: DESCARGA DE CARÁTULAS ONLINE (ÁLBUM / ARTISTA)
        // ═══════════════════════════════════════════════════════════════════════
        Text("CARÁTULAS Y PORTADAS (HD)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Descarga Automática de Portadas Online",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    "Si la pista local no tiene carátula, se buscará la imagen oficial en alta resolución en segundo plano.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                // Opciones de red para carátulas
                ModoDescargaCaratulas.values().forEach { modo ->
                    val esSeleccionado = config.descargaCaratulasModo == modo
                    Surface(
                        color = if (esSeleccionado) MotoOrangePrimary.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (esSeleccionado) MotoOrangePrimary else Color(0xFFE2E8F0)),
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
                                Text(modo.titulo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                                Text(modo.descripcion, fontSize = 11.sp, color = Color(0xFF64748B))
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
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Limpiar Caché de Carátulas", fontSize = 12.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // SECCIÓN 3: LA NUBE (BURBUJA FLOTANTE IN-APP)
        // ═══════════════════════════════════════════════════════════════════════
        Text("LA NUBE (WIDGET FLOTANTE EN APP)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Activar Nube Flotante In-App", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text("Muestra un mini widget flotante para controlar la música mientras navegas en el Muro, Chat o Mapa.", fontSize = 11.sp, color = Color(0xFF64748B))
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

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-ocultar al Borde Izquierdo", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text("Tras 4 segundos de inactividad, se contrae a una pequeña lengüeta para no tapar contenido.", fontSize = 11.sp, color = Color(0xFF64748B))
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

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bloquear Posición de la Nube", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text("Evita arrastrar la burbuja por error mientras manejas la moto o usas la app.", fontSize = 11.sp, color = Color(0xFF64748B))
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
        Text("MOTOR DSP NATIVO C++", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚡", fontSize = 16.sp)
                    Text("Procesamiento Digital de Audio para Motos", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                }
                Text(
                    "El motor C++ (MOTOR_AUDIO.cpp) optimiza el ancho de banda y la respuesta dinámica en altavoces de motocicleta e intercomunicadores de casco Bluetooth, compensando ruidos aerodinámicos.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Librería Nativa:", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text("libmotor_audio_tx.so", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MotoOrangePrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
