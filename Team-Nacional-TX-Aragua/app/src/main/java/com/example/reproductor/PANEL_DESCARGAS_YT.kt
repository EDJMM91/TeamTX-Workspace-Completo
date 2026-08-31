package com.example.reproductor

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.MotoGoldSecondary
import com.example.ui.theme.MotoOrangePrimary
import com.example.ui.theme.TxFlameRed

// ═══════════════════════════════════════════════════════════════════════════
// PANEL DE DESCARGAS YT - REPRODUCTOR TX PRO
// Interfaz de búsqueda y descarga de audio desde YouTube.
// Estilo visual inspirado en Spotify y apps de música modernas.
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun PANEL_DESCARGAS_YT(
    modifier: Modifier = Modifier
) {
    val contexto = androidx.compose.ui.platform.LocalContext.current

    // Inicializar gestor
    LaunchedEffect(Unit) {
        GESTOR_DESCARGAS_YT.inicializar(contexto)
    }

    val estadoPanel by GESTOR_DESCARGAS_YT.estadoPanel.collectAsState()
    val descargasActivas by GESTOR_DESCARGAS_YT.descargasActivas.collectAsState()

    var textoBusqueda by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // ═══════════════════════════════════════════════════════════════════
        // BARRA DE BÚSQUEDA
        // ═══════════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = { textoBusqueda = it },
                placeholder = {
                    Text(
                        "Buscar en YouTube...",
                        fontSize = 14.sp,
                        color = Color(0xFFB0BEC5)
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFB0BEC5))
                },
                trailingIcon = {
                    if (textoBusqueda.isNotBlank()) {
                        IconButton(onClick = { textoBusqueda = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color(0xFFB0BEC5))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MotoOrangePrimary,
                    unfocusedBorderColor = Color(0xFF374151),
                    cursorColor = MotoOrangePrimary
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = { GESTOR_DESCARGAS_YT.iniciarBusqueda(textoBusqueda) },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                enabled = textoBusqueda.isNotBlank() && estadoPanel !is EstadoPanelYT.Buscando,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(52.dp)
            ) {
                if (estadoPanel is EstadoPanelYT.Buscando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ═══════════════════════════════════════════════════════════════════
        // DESCARGAS EN CURSO
        // ═══════════════════════════════════════════════════════════════════
        val descargasEnCurso = descargasActivas.filter {
            it.estado == TipoEstadoDescarga.DESCARGANDO ||
            it.estado == TipoEstadoDescarga.CONVIRTIENDO ||
            it.estado == TipoEstadoDescarga.GUARDANDO ||
            it.estado == TipoEstadoDescarga.EN_COLA
        }

        if (descargasEnCurso.isNotEmpty()) {
            Text(
                "DESCARGAS EN CURSO",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MotoGoldSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            descargasEnCurso.forEach { descarga ->
                CardDescargaEnCurso(descarga = descarga)
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // ═══════════════════════════════════════════════════════════════════
        // CONTENIDO PRINCIPAL SEGÚN ESTADO
        // ═══════════════════════════════════════════════════════════════════
        when (val estado = estadoPanel) {
            is EstadoPanelYT.Inactivo -> {
                EstadoVacio(
                    icono = "⬇️",
                    titulo = "Descarga música de YouTube",
                    subtitulo = "Busca cualquier canción, artista o álbum y descárgalo en MP3 320kbps directo a tu biblioteca."
                )
            }

            is EstadoPanelYT.Buscando -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MotoOrangePrimary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Buscando '${estado.termino}'...", color = Color(0xFFB0BEC5), fontSize = 13.sp)
                    }
                }
            }

            is EstadoPanelYT.ListaResultados -> {
                Column {
                    Text(
                        "RESULTADOS (${estado.resultados.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MotoGoldSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(estado.resultados) { resultado ->
                            val descargaActual = descargasActivas.find { it.idVideo == resultado.idVideo }
                            ItemResultadoYT(
                                resultado = resultado,
                                descarga = descargaActual,
                                onDescargar = { GESTOR_DESCARGAS_YT.iniciarDescarga(resultado) },
                                onReproducirDescarga = { res ->
                                    val cancionDescargada = GESTOR_AUDIO_TX.todasLasCanciones.value.find {
                                        it.rutaArchivo.contains("TX_PRO_Descargas", ignoreCase = true) &&
                                        (it.titulo.contains(res.titulo.substringAfter(" - ").ifEmpty { res.titulo }, ignoreCase = true) ||
                                         it.titulo.contains(res.titulo, ignoreCase = true))
                                    }
                                    if (cancionDescargada != null) {
                                        val descargas = GESTOR_AUDIO_TX.listasPersonalizadas.value
                                            .find { it.nombre.equals("Descargas TX", ignoreCase = true) }
                                            ?.let { lista ->
                                                lista.cancionIds.mapNotNull { id ->
                                                    GESTOR_AUDIO_TX.todasLasCanciones.value.find { it.id == id }
                                                }
                                            }
                                            ?: GESTOR_AUDIO_TX.todasLasCanciones.value.filter {
                                                it.rutaArchivo.contains("TX_PRO_Descargas", ignoreCase = true)
                                            }
                                        GESTOR_AUDIO_TX.reproducirCancion(cancionDescargada, descargas.ifEmpty { listOf(cancionDescargada) })
                                    }
                                }
                            )
                        }
                    }
                }
            }

            is EstadoPanelYT.ErrorBusqueda -> {
                EstadoVacio(
                    icono = "⚠️",
                    titulo = "Error en la búsqueda",
                    subtitulo = estado.mensaje,
                    conBotonReintentar = true,
                    onReintentar = {
                        if (textoBusqueda.isNotBlank()) {
                            GESTOR_DESCARGAS_YT.iniciarBusqueda(textoBusqueda)
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTE: ITEM DE RESULTADO DE BÚSQUEDA YT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ItemResultadoYT(
    resultado: ResultadoBusquedaYT,
    descarga: EstadoDescarga?,
    onDescargar: () -> Unit,
    onReproducirDescarga: ((ResultadoBusquedaYT) -> Unit)? = null
) {
    val estaDescargando = descarga != null && descarga.estado != TipoEstadoDescarga.COMPLETADO && descarga.estado != TipoEstadoDescarga.ERROR

    Surface(
        color = Color(0xFF161B26),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF263238)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Thumbnail del video
                AsyncImage(
                    model = resultado.urlThumbnail,
                    contentDescription = resultado.titulo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2433))
                )

                // Información del video
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resultado.titulo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = resultado.autor,
                        fontSize = 11.sp,
                        color = Color(0xFFB0BEC5),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = resultado.duracionFormateada,
                        fontSize = 10.sp,
                        color = Color(0xFF78909C)
                    )
                }

                // Botón de descarga o progreso
                if (descarga != null && descarga.estado == TipoEstadoDescarga.COMPLETADO) {
                    // Descarga completada: mostrar botón de reproducir
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { onReproducirDescarga?.invoke(resultado) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MotoOrangePrimary,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Descargado",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (descarga != null && descarga.estado == TipoEstadoDescarga.ERROR) {
                    IconButton(onClick = onDescargar) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reintentar",
                            tint = TxFlameRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else if (estaDescargando) {
                    // Ya está en proceso
                } else {
                    Button(
                        onClick = onDescargar,
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Descargar", fontSize = 11.sp)
                    }
                }
            }

            // Barra de progreso si está descargando
            if (descarga != null && estaDescargando) {
                Spacer(modifier = Modifier.height(8.dp))

                val textoEstado = when (descarga.estado) {
                    TipoEstadoDescarga.EN_COLA -> "En cola..."
                    TipoEstadoDescarga.DESCARGANDO -> "Descargando... ${descarga.porcentaje}%"
                    TipoEstadoDescarga.CONVIRTIENDO -> "Convirtiendo a MP3..."
                    TipoEstadoDescarga.GUARDANDO -> "Guardando en galería..."
                    else -> ""
                }

                Text(
                    textoEstado,
                    fontSize = 10.sp,
                    color = MotoGoldSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { descarga.progreso.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = when (descarga.estado) {
                        TipoEstadoDescarga.CONVIRTIENDO -> MotoGoldSecondary
                        TipoEstadoDescarga.GUARDANDO -> Color(0xFF4CAF50)
                        else -> MotoOrangePrimary
                    },
                    trackColor = Color(0xFF1E2433)
                )
            }

            // Mostrar error si falló
            if (descarga != null && descarga.estado == TipoEstadoDescarga.ERROR && descarga.mensajeError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Error: ${descarga.mensajeError}",
                    fontSize = 10.sp,
                    color = TxFlameRed
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTE: TARJETA DE DESCARGA EN CURSO
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CardDescargaEnCurso(descarga: EstadoDescarga) {
    Surface(
        color = Color(0xFF1E2433),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    progress = { descarga.progreso.coerceIn(0f, 1f) },
                    modifier = Modifier.size(24.dp),
                    color = when (descarga.estado) {
                        TipoEstadoDescarga.CONVIRTIENDO -> MotoGoldSecondary
                        TipoEstadoDescarga.GUARDANDO -> Color(0xFF4CAF50)
                        else -> MotoOrangePrimary
                    },
                    strokeWidth = 2.5.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        descarga.titulo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val textoEstado = when (descarga.estado) {
                        TipoEstadoDescarga.EN_COLA -> "En cola..."
                        TipoEstadoDescarga.DESCARGANDO -> "Descargando audio..."
                        TipoEstadoDescarga.CONVIRTIENDO -> "Convirtiendo a MP3 320kbps..."
                        TipoEstadoDescarga.GUARDANDO -> "Guardando en galería..."
                        else -> ""
                    }
                    Text(textoEstado, fontSize = 10.sp, color = MotoGoldSecondary)
                }

                Text(
                    "${descarga.porcentaje}%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MotoOrangePrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { descarga.progreso.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MotoOrangePrimary,
                trackColor = Color(0xFF0B0E14)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTE: ESTADO VACÍO
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EstadoVacio(
    icono: String,
    titulo: String,
    subtitulo: String,
    conBotonReintentar: Boolean = false,
    onReintentar: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(icono, fontSize = 48.sp)
            Text(
                titulo,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                subtitulo,
                color = Color(0xFFB0BEC5),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            if (conBotonReintentar && onReintentar != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onReintentar,
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reintentar")
                }
            }
        }
    }
}
