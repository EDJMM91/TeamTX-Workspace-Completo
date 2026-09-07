package com.example.reproductor

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.MotoGoldSecondary
import com.example.ui.theme.MotoOrangePrimary
import com.example.ui.theme.TxFlameRed

// ═══════════════════════════════════════════════════════════════════════════
// PANEL DE DESCARGAS YT - REPRODUCTOR TX PRO (TEMA CLARO MODERNO)
// Interfaz de búsqueda y descarga de audio con alta fidelidad y descargas directas.
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun PANEL_DESCARGAS_YT(
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current

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
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // ═══════════════════════════════════════════════════════════════════
        // BARRA DE BÚSQUEDA MODERNA (TEMA CLARO - TEXTO OSCURO NÍTIDO)
        // ═══════════════════════════════════════════════════════════════════
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = textoBusqueda,
                    onValueChange = { textoBusqueda = it },
                    placeholder = {
                        Text(
                            "Buscar canción, artista o video...",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MotoOrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (textoBusqueda.isNotBlank()) {
                            IconButton(onClick = { textoBusqueda = "" }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = MotoOrangePrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        if (textoBusqueda.isNotBlank()) {
                            GESTOR_DESCARGAS_YT.iniciarBusqueda(textoBusqueda)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                    enabled = textoBusqueda.isNotBlank() && estadoPanel !is EstadoPanelYT.Buscando,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    if (estadoPanel is EstadoPanelYT.Buscando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Buscar",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    "DESCARGAS EN CURSO",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = MotoOrangePrimary
                )
                Surface(
                    color = MotoOrangePrimary.copy(alpha = 0.15f),
                    shape = CircleShape
                ) {
                    Text(
                        "${descargasEnCurso.size}",
                        color = MotoOrangePrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            descargasEnCurso.forEach { descarga ->
                CardDescargaEnCurso(descarga = descarga)
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // ═══════════════════════════════════════════════════════════════════
        // CONTENIDO PRINCIPAL SEGÚN ESTADO
        // ═══════════════════════════════════════════════════════════════════
        when (val estado = estadoPanel) {
            is EstadoPanelYT.Inactivo -> {
                EstadoVacio(
                    icono = "🎧",
                    titulo = "Descarga tu Música Favorita",
                    subtitulo = "Busca cualquier canción, artista o álbum para descargar directamente en MP3 320kbps a tu biblioteca motera.",
                    onSugerenciaSeleccionada = { sugerencia ->
                        textoBusqueda = sugerencia
                        GESTOR_DESCARGAS_YT.iniciarBusqueda(sugerencia)
                    }
                )
            }

            is EstadoPanelYT.Buscando -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MotoOrangePrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(46.dp)
                        )
                        Text(
                            "Buscando '${estado.termino}' en catálogo global...",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            is EstadoPanelYT.ListaResultados -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "RESULTADOS ENCONTRADOS (${estado.resultados.size})",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = Color(0xFF0F172A)
                        )
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "320kbps & HD",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    titulo = "No se pudieron obtener resultados",
                    subtitulo = estado.mensaje,
                    conBotonReintentar = true,
                    onReintentar = {
                        if (textoBusqueda.isNotBlank()) {
                            GESTOR_DESCARGAS_YT.iniciarBusqueda(textoBusqueda)
                        }
                    },
                    onSugerenciaSeleccionada = { sugerencia ->
                        textoBusqueda = sugerencia
                        GESTOR_DESCARGAS_YT.iniciarBusqueda(sugerencia)
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTE: ITEM DE RESULTADO DE BÚSQUEDA (TEMA CLARO MODERNO)
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
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Thumbnail de la canción / video
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1F5F9))
                ) {
                    AsyncImage(
                        model = resultado.urlThumbnail,
                        contentDescription = resultado.titulo,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Información de la pista
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resultado.titulo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = resultado.autor,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = resultado.duracionFormateada,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                        Surface(
                            color = if (resultado.urlDescargaDirecta != null) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, if (resultado.urlDescargaDirecta != null) MotoGoldSecondary.copy(alpha = 0.5f) else Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = if (resultado.urlDescargaDirecta != null) "⚡ 320kbps" else "🎬 YouTube",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (resultado.urlDescargaDirecta != null) Color(0xFFB45309) else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Botones de acción / estado de descarga
                if (descarga != null && descarga.estado == TipoEstadoDescarga.COMPLETADO) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { onReproducirDescarga?.invoke(resultado) },
                            colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Oír", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Descargado",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (descarga != null && descarga.estado == TipoEstadoDescarga.ERROR) {
                    IconButton(onClick = onDescargar, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reintentar",
                            tint = TxFlameRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else if (estaDescargando) {
                    Surface(
                        shape = CircleShape,
                        color = MotoOrangePrimary.copy(alpha = 0.1f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { descarga.progreso.coerceIn(0f, 1f) },
                                modifier = Modifier.size(22.dp),
                                color = MotoOrangePrimary,
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onDescargar,
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bajar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Barra de progreso si está descargando
            if (descarga != null && estaDescargando) {
                Spacer(modifier = Modifier.height(8.dp))

                val textoEstado = when (descarga.estado) {
                    TipoEstadoDescarga.EN_COLA -> "En cola de espera..."
                    TipoEstadoDescarga.DESCARGANDO -> "Descargando audio... ${descarga.porcentaje}%"
                    TipoEstadoDescarga.CONVIRTIENDO -> "Procesando audio a MP3..."
                    TipoEstadoDescarga.GUARDANDO -> "Guardando en almacenamiento..."
                    else -> ""
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        textoEstado,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MotoOrangePrimary
                    )
                    Text(
                        "${descarga.porcentaje}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MotoOrangePrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { descarga.progreso.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = when (descarga.estado) {
                        TipoEstadoDescarga.CONVIRTIENDO -> MotoGoldSecondary
                        TipoEstadoDescarga.GUARDANDO -> Color(0xFF10B981)
                        else -> MotoOrangePrimary
                    },
                    trackColor = Color(0xFFF1F5F9)
                )
            }

            // Mostrar mensaje de error si falló
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
// COMPONENTE: TARJETA DE DESCARGA EN CURSO (TEMA CLARO)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CardDescargaEnCurso(descarga: EstadoDescarga) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.35f)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    progress = { descarga.progreso.coerceIn(0f, 1f) },
                    modifier = Modifier.size(24.dp),
                    color = when (descarga.estado) {
                        TipoEstadoDescarga.CONVIRTIENDO -> MotoGoldSecondary
                        TipoEstadoDescarga.GUARDANDO -> Color(0xFF10B981)
                        else -> MotoOrangePrimary
                    },
                    strokeWidth = 2.5.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        descarga.titulo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val textoEstado = when (descarga.estado) {
                        TipoEstadoDescarga.EN_COLA -> "En cola..."
                        TipoEstadoDescarga.DESCARGANDO -> "Descargando pista..."
                        TipoEstadoDescarga.CONVIRTIENDO -> "Convirtiendo a MP3 320kbps..."
                        TipoEstadoDescarga.GUARDANDO -> "Guardando en almacenamiento..."
                        else -> ""
                    }
                    Text(textoEstado, fontSize = 10.sp, color = Color(0xFF64748B))
                }

                Surface(
                    color = MotoOrangePrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "${descarga.porcentaje}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MotoOrangePrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { descarga.progreso.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MotoOrangePrimary,
                trackColor = Color(0xFFF1F5F9)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTE: ESTADO VACÍO CON CHIPS SUGERIDOS (TEMA CLARO)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EstadoVacio(
    icono: String,
    titulo: String,
    subtitulo: String,
    conBotonReintentar: Boolean = false,
    onReintentar: (() -> Unit)? = null,
    onSugerenciaSeleccionada: ((String) -> Unit)? = null
) {
    val sugerencias = remember {
        listOf(
            "Rock Clásico",
            "AC/DC",
            "Metallica",
            "Caramelos de Cianuro",
            "Rutas Moteras",
            "Rock en Español",
            "Heavy Metal",
            "Guns N' Roses"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(icono, fontSize = 44.sp)
            Text(
                titulo,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Text(
                subtitulo,
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (onSugerenciaSeleccionada != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "🔥 SUGERENCIAS RÁPIDAS MOTERAS:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = MotoOrangePrimary
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(sugerencias) { sug ->
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            shadowElevation = 1.dp,
                            modifier = Modifier.clickable { onSugerenciaSeleccionada(sug) }
                        ) {
                            Text(
                                text = sug,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            if (conBotonReintentar && onReintentar != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onReintentar,
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reintentar búsqueda")
                }
            }
        }
    }
}
