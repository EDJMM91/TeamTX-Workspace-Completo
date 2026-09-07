package com.example.reproductor

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import com.example.ui.theme.MotoGoldSecondary
import com.example.ui.theme.MotoOrangePrimary
import com.example.ui.theme.TxFlameRed

// ═══════════════════════════════════════════════════════════════════════════
// INTERFAZ PRINCIPAL - REPRODUCTOR TX PRO (TEAM NACIONAL TX ARAGUA)
// Pantalla completa, auto-ocultación de barras, gestión de listas y DSP.
// ═══════════════════════════════════════════════════════════════════════════

private fun iconoVectorialPestana(pestana: PestanaReproductor): androidx.compose.ui.graphics.vector.ImageVector = when (pestana) {
    PestanaReproductor.CANCIONES -> Icons.Default.MusicNote
    PestanaReproductor.LISTAS -> Icons.Default.QueueMusic
    PestanaReproductor.FAVORITAS -> Icons.Default.Favorite
    PestanaReproductor.ALBUMES -> Icons.Default.Album
    PestanaReproductor.ARTISTAS -> Icons.Default.Person
    PestanaReproductor.CARPETAS -> Icons.Default.Folder
    PestanaReproductor.DESCARGAS_YT -> Icons.Default.CloudDownload
    PestanaReproductor.AJUSTES -> Icons.Default.Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun REPRODUCTOR_PRINCIPAL(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current

    // Inicializar singleton de audio
    LaunchedEffect(Unit) {
        GESTOR_AUDIO_TX.inicializar(contexto)
    }

    // Estados reactivos recolectados
    val cancionActual by GESTOR_AUDIO_TX.cancionActual.collectAsState()
    val estado by GESTOR_AUDIO_TX.estado.collectAsState()
    val posicionActualMs by GESTOR_AUDIO_TX.posicionActualMs.collectAsState()
    val duracionTotalMs by GESTOR_AUDIO_TX.duracionTotalMs.collectAsState()
    val modoBucle by GESTOR_AUDIO_TX.modoBucle.collectAsState()
    val modoAleatorio by GESTOR_AUDIO_TX.modoAleatorio.collectAsState()
    val todasLasCanciones by GESTOR_AUDIO_TX.todasLasCanciones.collectAsState()
    val listasPersonalizadas by GESTOR_AUDIO_TX.listasPersonalizadas.collectAsState()
    val favoritasIds by GESTOR_AUDIO_TX.favoritasIds.collectAsState()
    val config by GESTOR_AUDIO_TX.configuracion.collectAsState()
    val rutaCaratula by GESTOR_AUDIO_TX.caratulaActualRuta.collectAsState()

    var pestanaActual by remember { mutableStateOf(PestanaReproductor.CANCIONES) }
    var textoBusqueda by remember { mutableStateOf("") }
    var modoBusquedaActivo by remember { mutableStateOf(false) }
    var mostrarEcualizadorModal by remember { mutableStateOf(false) }
    var mostrarReproductorExpandido by remember { mutableStateOf(false) }
    var mostrarDialogoNuevaLista by remember { mutableStateOf(false) }

    // Modo de selección múltiple por lote
    var cancionesSeleccionadas by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val enModoSeleccion = cancionesSeleccionadas.isNotEmpty()

    // Fase 2: Gestión de listas - Vista detalle y selector
    var listaDetalleActual by remember { mutableStateOf<ListaReproduccionMotera?>(null) }
    var mostrarSelectorListaParaAgregar by remember { mutableStateOf(false) }
    var mostrarSelectorCancionesParaLista by remember { mutableStateOf(false) }
    var cancionParaPropiedades by remember { mutableStateOf<CancionMotera?>(null) }

    // Filtrado reactivo de canciones
    val cancionesFiltradas = remember(todasLasCanciones, textoBusqueda, pestanaActual, favoritasIds) {
        val base = when (pestanaActual) {
            PestanaReproductor.FAVORITAS -> todasLasCanciones.filter { favoritasIds.contains(it.id) }
            else -> todasLasCanciones
        }
        if (textoBusqueda.isBlank()) base
        else base.filter {
            it.titulo.contains(textoBusqueda, ignoreCase = true) ||
            it.artista.contains(textoBusqueda, ignoreCase = true) ||
            it.album.contains(textoBusqueda, ignoreCase = true)
        }
    }

    // Agrupaciones para pestañas de Álbumes, Artistas y Carpetas
    val albumesAgrupados = remember(todasLasCanciones) {
        todasLasCanciones.groupBy { it.album }
    }
    val artistasAgrupados = remember(todasLasCanciones) {
        todasLasCanciones.groupBy { it.artista }
    }
    val carpetasAgrupadas = remember(todasLasCanciones) {
        todasLasCanciones.groupBy { it.carpetaContenedora }
    }

    val esPlaying = estado == EstadoReproductor.REPRODUCIENDO

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                // Barra superior con botón de salida / minimizar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Minimizar a la App", tint = MotoOrangePrimary)
                    }

                    if (modoBusquedaActivo) {
                        TextField(
                            value = textoBusqueda,
                            onValueChange = { textoBusqueda = it },
                            placeholder = { Text("Buscar canción, artista...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedIndicatorColor = MotoOrangePrimary
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            textoBusqueda = ""
                            modoBusquedaActivo = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar búsqueda", tint = Color(0xFF475569))
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🎧", fontSize = 16.sp)
                            Text("REPRODUCTOR TX PRO", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF0F172A))
                        }

                        Row {
                            IconButton(onClick = { modoBusquedaActivo = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color(0xFF475569))
                            }
                            IconButton(onClick = { mostrarEcualizadorModal = true }) {
                                Icon(Icons.Default.GraphicEq, contentDescription = "Ecualizador", tint = MotoOrangePrimary)
                            }
                        }
                    }
                }

                // Pestañas horizontales de navegación modernas con iconos vectoriales
                ScrollableTabRow(
                    selectedTabIndex = pestanaActual.ordinal,
                    containerColor = Color.White,
                    contentColor = MotoOrangePrimary,
                    edgePadding = 10.dp,
                    indicator = {},
                    divider = {}
                ) {
                    PestanaReproductor.values().forEach { pestana ->
                        val esSeleccionada = pestanaActual == pestana
                        val colorFondo by animateColorAsState(
                            targetValue = if (esSeleccionada) MotoOrangePrimary.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                            label = "tabBg_${pestana.name}"
                        )
                        val colorBorde by animateColorAsState(
                            targetValue = if (esSeleccionada) MotoOrangePrimary else Color(0xFFE2E8F0),
                            label = "tabBorder_${pestana.name}"
                        )
                        val colorContenido = if (esSeleccionada) MotoOrangePrimary else Color(0xFF475569)

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colorFondo,
                            border = BorderStroke(1.dp, colorBorde),
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .clickable {
                                    pestanaActual = pestana
                                    cancionesSeleccionadas = emptySet()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    imageVector = iconoVectorialPestana(pestana),
                                    contentDescription = pestana.titulo,
                                    tint = colorContenido,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = pestana.titulo,
                                    fontSize = 11.sp,
                                    fontWeight = if (esSeleccionada) FontWeight.Black else FontWeight.SemiBold,
                                    color = colorContenido
                                )

                                val contador = when (pestana) {
                                    PestanaReproductor.CANCIONES -> todasLasCanciones.size
                                    PestanaReproductor.FAVORITAS -> favoritasIds.size
                                    PestanaReproductor.LISTAS -> listasPersonalizadas.size
                                    else -> null
                                }
                                if (contador != null && contador > 0) {
                                    Surface(
                                        color = if (esSeleccionada) MotoOrangePrimary else Color(0xFFE2E8F0),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "$contador",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (esSeleccionada) Color.White else Color(0xFF475569),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
            }
        },
        bottomBar = {
            // Mini reproductor inferior persistente cuando se navega en listas
            if (cancionActual != null) {
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mostrarReproductorExpandido = true }
                ) {
                    Column {
                        // Barra de progreso delgada interactiva
                        val progreso = if (duracionTotalMs > 0) posicionActualMs.toFloat() / duracionTotalMs.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { progreso.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = MotoOrangePrimary,
                            trackColor = Color(0xFFE2E8F0)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Icono de disco
                            Surface(
                                shape = CircleShape,
                                color = if (esPlaying) TxFlameRed.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(if (esPlaying) "🔥" else "🎵", fontSize = 16.sp)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cancionActual!!.titulo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${cancionActual!!.artista} • ${cancionActual!!.duracionFormateada}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Controles rápidos
                            IconButton(onClick = { GESTOR_AUDIO_TX.anteriorCancion() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = Color(0xFF334155), modifier = Modifier.size(20.dp))
                            }

                            IconButton(
                                onClick = { GESTOR_AUDIO_TX.alternarPlayPausa() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Surface(shape = CircleShape, color = MotoOrangePrimary, modifier = Modifier.fillMaxSize()) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (esPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pausa",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = { GESTOR_AUDIO_TX.siguienteCancion() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = Color(0xFF334155), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValores ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValores)
        ) {
            // Contenido según la pestaña seleccionada
            when (pestanaActual) {
                PestanaReproductor.CANCIONES, PestanaReproductor.FAVORITAS -> {
                    if (cancionesFiltradas.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📁", fontSize = 48.sp)
                                Text(
                                    if (pestanaActual == PestanaReproductor.FAVORITAS) "No tienes canciones favoritas aún" else "No se encontraron canciones",
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text("Presiona 'Escanear' en Ajustes para detectar música local.", color = Color(0xFF64748B), fontSize = 11.sp, textAlign = TextAlign.Center)
                                Button(
                                    onClick = { GESTOR_AUDIO_TX.escanearMusicaLocal() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Escanear Almacenamiento")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            // Barra de acciones por lote si hay selección
                            if (enModoSeleccion) {
                                item {
                                    Surface(
                                        color = Color.White,
                                        border = BorderStroke(1.dp, MotoOrangePrimary),
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${cancionesSeleccionadas.size} seleccionadas",
                                                color = Color(0xFF0F172A),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = {
                                                        GESTOR_AUDIO_TX.marcarFavoritasPorLote(cancionesSeleccionadas.toList(), true)
                                                        cancionesSeleccionadas = emptySet()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = TxFlameRed),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(40.dp)
                                                ) {
                                                    Text("Favoritas", fontSize = 12.sp)
                                                }
                                                Button(
                                                    onClick = { mostrarSelectorListaParaAgregar = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(40.dp)
                                                ) {
                                                    Text("Agregar a Lista", fontSize = 12.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = { cancionesSeleccionadas = emptySet() },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(40.dp)
                                                ) {
                                                    Text("Cancelar", fontSize = 12.sp, color = Color(0xFF475569))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            items(cancionesFiltradas) { cancion ->
                                val esLaActual = cancionActual?.id == cancion.id
                                val estaSeleccionada = cancionesSeleccionadas.contains(cancion.id)

                                ElementoFilaCancion(
                                    cancion = cancion,
                                    esActual = esLaActual,
                                    estaSeleccionada = estaSeleccionada,
                                    enModoSeleccion = enModoSeleccion,
                                    onReproducir = {
                                        if (enModoSeleccion) {
                                            cancionesSeleccionadas = if (estaSeleccionada) cancionesSeleccionadas - cancion.id else cancionesSeleccionadas + cancion.id
                                        } else {
                                            GESTOR_AUDIO_TX.reproducirCancion(cancion, cancionesFiltradas)
                                        }
                                    },
                                    onSeleccionLarga = {
                                        cancionesSeleccionadas = if (estaSeleccionada) cancionesSeleccionadas - cancion.id else cancionesSeleccionadas + cancion.id
                                    },
                                    onAlternarFavorita = { GESTOR_AUDIO_TX.alternarFavorita(cancion.id) },
                                    onVerPropiedades = { cancionParaPropiedades = cancion }
                                )
                            }
                        }
                    }
                }

                PestanaReproductor.LISTAS -> {
                    if (listaDetalleActual != null) {
                        // VISTA DETALLE DE LISTA
                        VistaDetalleLista(
                            lista = listaDetalleActual!!,
                            todasLasCanciones = todasLasCanciones,
                            cancionActual = cancionActual,
                            onVolver = { listaDetalleActual = null },
                            onReproducirLista = { lista ->
                                val canciones = todasLasCanciones.filter { lista.cancionIds.contains(it.id) }
                                if (canciones.isNotEmpty()) GESTOR_AUDIO_TX.reproducirCancion(canciones.first(), canciones)
                            },
                            onReproducirCancion = { cancion ->
                                val canciones = todasLasCanciones.filter { listaDetalleActual!!.cancionIds.contains(it.id) }
                                GESTOR_AUDIO_TX.reproducirCancion(cancion, canciones)
                            },
                            onEliminarCancion = { cancionId ->
                                GESTOR_AUDIO_TX.eliminarCancionDeLista(listaDetalleActual!!.id, cancionId)
                                listaDetalleActual = listaDetalleActual?.copy(
                                    cancionIds = listaDetalleActual!!.cancionIds.filter { it != cancionId }
                                )
                            },
                            onAgregarCanciones = { mostrarSelectorCancionesParaLista = true },
                            onEliminarLista = {
                                GESTOR_AUDIO_TX.eliminarLista(listaDetalleActual!!.id)
                                listaDetalleActual = null
                            },
                            onVerPropiedades = { cancionParaPropiedades = it }
                        )
                    } else {
                        // LISTA DE LISTAS
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("LISTAS DE REPRODUCCION TX", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MotoOrangePrimary)
                                Button(
                                    onClick = { mostrarDialogoNuevaLista = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Nueva Lista", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (listasPersonalizadas.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No hay listas creadas. Crea una para tus rodadas!", color = Color(0xFF64748B), fontSize = 13.sp)
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 120.dp)
                                ) {
                                    items(listasPersonalizadas) { lista ->
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            shadowElevation = 1.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { listaDetalleActual = lista }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(lista.icono, fontSize = 28.sp)
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(lista.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                                    Text(
                                                        "${lista.cancionIds.size} canciones${if (lista.descripcion.isNotBlank()) " • ${lista.descripcion}" else ""}",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                                IconButton(onClick = {
                                                    val cancionesDeLista = todasLasCanciones.filter { lista.cancionIds.contains(it.id) }
                                                    if (cancionesDeLista.isNotEmpty()) {
                                                        GESTOR_AUDIO_TX.reproducirCancion(cancionesDeLista.first(), cancionesDeLista)
                                                    }
                                                }) {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir Lista", tint = MotoOrangePrimary, modifier = Modifier.size(28.dp))
                                                }
                                                IconButton(onClick = { GESTOR_AUDIO_TX.eliminarLista(lista.id) }) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                PestanaReproductor.ALBUMES -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(albumesAgrupados.entries.toList()) { entry ->
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (entry.value.isNotEmpty()) GESTOR_AUDIO_TX.reproducirCancion(entry.value.first(), entry.value)
                                    }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("💿", fontSize = 24.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.key, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        Text("${entry.value.size} pistas • ${entry.value.firstOrNull()?.artista ?: ""}", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MotoOrangePrimary)
                                }
                            }
                        }
                    }
                }

                PestanaReproductor.ARTISTAS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(artistasAgrupados.entries.toList()) { entry ->
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (entry.value.isNotEmpty()) GESTOR_AUDIO_TX.reproducirCancion(entry.value.first(), entry.value)
                                    }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("🎙️", fontSize = 24.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.key, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        Text("${entry.value.size} pistas de este artista", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MotoOrangePrimary)
                                }
                            }
                        }
                    }
                }

                PestanaReproductor.CARPETAS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(carpetasAgrupadas.entries.toList()) { entry ->
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (entry.value.isNotEmpty()) GESTOR_AUDIO_TX.reproducirCancion(entry.value.first(), entry.value)
                                    }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("📁", fontSize = 24.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.key, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        Text("${entry.value.size} archivos de audio", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MotoOrangePrimary)
                                }
                            }
                        }
                    }
                }

                PestanaReproductor.AJUSTES -> {
                    PanelAjustesReproductor(
                        config = config,
                        totalCanciones = todasLasCanciones.size,
                        totalCarpetas = carpetasAgrupadas.size,
                        onActualizarConfig = { GESTOR_AUDIO_TX.actualizarConfiguracion(it) },
                        onReescanearMusica = { GESTOR_AUDIO_TX.escanearMusicaLocal() }
                    )
                }

                PestanaReproductor.DESCARGAS_YT -> {
                    PANEL_DESCARGAS_YT()
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MODAL: REPRODUCTOR EXPANDIDO A PANTALLA COMPLETA
    // ═══════════════════════════════════════════════════════════════════════
    if (mostrarReproductorExpandido && cancionActual != null) {
        Dialog(
            onDismissRequest = { mostrarReproductorExpandido = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            VistaReproductorCompleto(
                cancion = cancionActual!!,
                estado = estado,
                posicionMs = posicionActualMs,
                duracionMs = duracionTotalMs,
                modoBucle = modoBucle,
                modoAleatorio = modoAleatorio,
                listas = listasPersonalizadas,
                rutaCaratula = rutaCaratula,
                onMinimizar = { mostrarReproductorExpandido = false },
                onPlayPausa = { GESTOR_AUDIO_TX.alternarPlayPausa() },
                onSiguiente = { GESTOR_AUDIO_TX.siguienteCancion() },
                onAnterior = { GESTOR_AUDIO_TX.anteriorCancion() },
                onBuscarPosicion = { GESTOR_AUDIO_TX.buscarPosicion(it) },
                onAlternarBucle = { GESTOR_AUDIO_TX.alternarModoBucle() },
                onAlternarAleatorio = { GESTOR_AUDIO_TX.alternarModoAleatorio() },
                onAlternarFavorita = { GESTOR_AUDIO_TX.alternarFavorita(cancionActual!!.id) },
                onAbrirEcualizador = { mostrarEcualizadorModal = true },
                onAlternarLista = { listaId -> GESTOR_AUDIO_TX.alternarCancionEnLista(listaId, cancionActual!!.id) }
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MODAL: ECUALIZADOR REAL PRO
    // ═══════════════════════════════════════════════════════════════════════
    if (mostrarEcualizadorModal) {
        Dialog(
            onDismissRequest = { mostrarEcualizadorModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            PanelEcualizadorTX(
                config = config,
                onActualizarConfig = { GESTOR_AUDIO_TX.actualizarConfiguracion(it) },
                onCerrar = { mostrarEcualizadorModal = false }
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DIÁLOGO CREAR NUEVA LISTA DE REPRODUCCIÓN
    // ═══════════════════════════════════════════════════════════════════════
    if (mostrarDialogoNuevaLista) {
        var nombreLista by remember { mutableStateOf("") }
        var descLista by remember { mutableStateOf("") }
        var iconoSeleccionado by remember { mutableStateOf("🎵") }
        val iconosDisponibles = listOf("🎵", "🏍️", "⚡", "🔥", "🏖️", "🏔️", "🍺", "🎧")

        AlertDialog(
            onDismissRequest = { mostrarDialogoNuevaLista = false },
            title = { Text("Nueva Lista de Reproduccion", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nombreLista,
                        onValueChange = { nombreLista = it },
                        label = { Text("Nombre de la Lista") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoOrangePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = MotoOrangePrimary,
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descLista,
                        onValueChange = { descLista = it },
                        label = { Text("Descripcion (opcional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotoOrangePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = MotoOrangePrimary,
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Selecciona un icono:", fontSize = 12.sp, color = Color(0xFF64748B))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        iconosDisponibles.forEach { ic ->
                            Surface(
                                shape = CircleShape,
                                color = if (iconoSeleccionado == ic) MotoOrangePrimary else Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { iconoSeleccionado = ic }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(ic, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nombreLista.isNotBlank()) {
                            GESTOR_AUDIO_TX.crearLista(nombreLista, descLista, iconoSeleccionado)
                            mostrarDialogoNuevaLista = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
                ) {
                    Text("Crear Lista")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoNuevaLista = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DIÁLOGO: SELECCIONAR LISTA PARA AGREGAR CANCIONES SELECCIONADAS
    // ═══════════════════════════════════════════════════════════════════════
    if (mostrarSelectorListaParaAgregar) {
        SelectorListaParaAgregar(
            listas = listasPersonalizadas,
            cancionesIds = cancionesSeleccionadas.toList(),
            onSeleccionar = { lista ->
                GESTOR_AUDIO_TX.agregarCancionesALista(lista.id, cancionesSeleccionadas.toList())
                cancionesSeleccionadas = emptySet()
                mostrarSelectorListaParaAgregar = false
            },
            onCerrar = { mostrarSelectorListaParaAgregar = false }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DIÁLOGO: SELECCIONAR CANCIONES PARA AGREGAR A UNA LISTA
    // ═══════════════════════════════════════════════════════════════════════
    if (mostrarSelectorCancionesParaLista && listaDetalleActual != null) {
        SelectorCancionesParaLista(
            lista = listaDetalleActual!!,
            todasLasCanciones = todasLasCanciones,
            onAgregar = { ids ->
                GESTOR_AUDIO_TX.agregarCancionesALista(listaDetalleActual!!.id, ids)
                listaDetalleActual = listaDetalleActual?.copy(
                    cancionIds = (listaDetalleActual!!.cancionIds + ids).distinct()
                )
                mostrarSelectorCancionesParaLista = false
            },
            onCerrar = { mostrarSelectorCancionesParaLista = false }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DIÁLOGO: PROPIEDADES E INFORMACIÓN DE LA CANCIÓN / EDICIÓN DE METADATOS
    // ═══════════════════════════════════════════════════════════════════════
    if (cancionParaPropiedades != null) {
        DialogoPropiedadesCancion(
            cancion = cancionParaPropiedades!!,
            listas = listasPersonalizadas,
            onCerrar = { cancionParaPropiedades = null },
            onGuardar = { cancionId, nuevoTitulo, nuevoArtista, nuevoAlbum ->
                GESTOR_AUDIO_TX.actualizarMetadatosCancion(cancionId, nuevoTitulo, nuevoArtista, nuevoAlbum)
                cancionParaPropiedades = null
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTE: ELEMENTO DE FILA DE CANCIÓN
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ElementoFilaCancion(
    cancion: CancionMotera,
    esActual: Boolean,
    estaSeleccionada: Boolean,
    enModoSeleccion: Boolean,
    onReproducir: () -> Unit,
    onSeleccionLarga: () -> Unit,
    onAlternarFavorita: () -> Unit,
    onVerPropiedades: () -> Unit = {}
) {
    Surface(
        color = when {
            estaSeleccionada -> MotoOrangePrimary.copy(alpha = 0.15f)
            esActual -> Color(0xFFFFF7ED)
            else -> Color.White
        },
        border = BorderStroke(1.dp, if (esActual) MotoOrangePrimary.copy(alpha = 0.4f) else Color(0xFFF1F5F9)),
        shadowElevation = if (esActual) 1.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onReproducir,
                onLongClick = onSeleccionLarga
            )
            .padding(horizontal = 12.dp, vertical = 3.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (enModoSeleccion) {
                Checkbox(
                    checked = estaSeleccionada,
                    onCheckedChange = { onReproducir() },
                    colors = CheckboxDefaults.colors(checkedColor = MotoOrangePrimary)
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (esActual) TxFlameRed.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (esActual) "▶" else "🎵", fontSize = 15.sp, color = if (esActual) TxFlameRed else Color(0xFF475569))
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cancion.titulo,
                    fontWeight = if (esActual) FontWeight.Black else FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (esActual) MotoOrangePrimary else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${cancion.artista} • ${cancion.album}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = cancion.duracionFormateada,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )

            IconButton(onClick = onAlternarFavorita, modifier = Modifier.size(26.dp)) {
                Icon(
                    imageVector = if (cancion.esFavorita) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorita",
                    tint = if (cancion.esFavorita) TxFlameRed else Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(onClick = onVerPropiedades, modifier = Modifier.size(26.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Propiedades e Info",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTE: VISTA DEL REPRODUCTOR EXPANDIDO A PANTALLA COMPLETA
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VistaReproductorCompleto(
    cancion: CancionMotera,
    estado: EstadoReproductor,
    posicionMs: Long,
    duracionMs: Long,
    modoBucle: ModoBucle,
    modoAleatorio: Boolean,
    listas: List<ListaReproduccionMotera>,
    rutaCaratula: String?,
    onMinimizar: () -> Unit,
    onPlayPausa: () -> Unit,
    onSiguiente: () -> Unit,
    onAnterior: () -> Unit,
    onBuscarPosicion: (Long) -> Unit,
    onAlternarBucle: () -> Unit,
    onAlternarAleatorio: () -> Unit,
    onAlternarFavorita: () -> Unit,
    onAbrirEcualizador: () -> Unit,
    onAlternarLista: (String) -> Unit
) {
    val esPlaying = estado == EstadoReproductor.REPRODUCIENDO
    val scrollState = rememberScrollState()

    var progresoArrastrando by remember { mutableStateOf<Float?>(null) }
    var mostrarModalGestionListas by remember { mutableStateOf(false) }

    // Listas a las que pertenece la canción actual
    val listasDeEstaCancion = remember(listas, cancion.id) {
        listas.filter { it.cancionIds.contains(cancion.id) }
    }

    // Animación de rotación del vinilo
    val rotacion = remember { Animatable(0f) }
    LaunchedEffect(esPlaying) {
        if (esPlaying) {
            rotacion.animateTo(
                targetValue = rotacion.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    Surface(
        color = Color(0xFFF8FAFC),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF8FAFC))))
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cabecera del reproductor completo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMinimizar) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimizar", tint = Color(0xFF0F172A), modifier = Modifier.size(32.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("REPRODUCIENDO EN VIVO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MotoOrangePrimary)
                    Text(cancion.album, fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 1)
                }
                IconButton(onClick = onAbrirEcualizador) {
                    Icon(Icons.Default.GraphicEq, contentDescription = "Ecualizador", tint = MotoOrangePrimary, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Disco de Vinilo / Carátula de Álbum
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .shadow(16.dp, CircleShape, spotColor = MotoOrangePrimary)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .border(3.dp, Brush.radialGradient(listOf(MotoOrangePrimary, Color(0xFF475569))), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Surcos del vinilo
                Canvas(modifier = Modifier.fillMaxSize().rotate(rotacion.value)) {
                    drawCircle(color = Color(0xFF1E293B), radius = size.minDimension / 2.3f)
                    drawCircle(color = Color(0xFF334155), radius = size.minDimension / 2.8f)
                    drawCircle(color = Color(0xFF475569), radius = size.minDimension / 3.6f)
                }

                // Carátula real de álbum o centro del vinilo
                val bitmapCaratula = remember(rutaCaratula) {
                    if (!rutaCaratula.isNullOrBlank()) {
                        try { BitmapFactory.decodeFile(rutaCaratula)?.asImageBitmap() } catch (_: Exception) { null }
                    } else null
                }

                if (bitmapCaratula != null) {
                    Image(
                        bitmap = bitmapCaratula,
                        contentDescription = "Carátula",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(2.dp, MotoGoldSecondary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = TxFlameRed,
                        modifier = Modifier.size(70.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("TX", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                        }
                    }
                }
            }

            // Información de la Pista (Título y Artista)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = cancion.titulo,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = cancion.artista,
                    fontSize = 13.sp,
                    color = MotoOrangePrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Botón/Badge de Listas de Reproducción
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (listasDeEstaCancion.isNotEmpty()) MotoOrangePrimary else Color(0xFFCBD5E1)),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { mostrarModalGestionListas = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📂", fontSize = 14.sp)
                        Text(
                            text = if (listasDeEstaCancion.isEmpty()) "Agregar a Lista de Reproducción..."
                            else "En: ${listasDeEstaCancion.joinToString { it.nombre }}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (listasDeEstaCancion.isNotEmpty()) MotoOrangePrimary else Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = "Administrar Listas",
                        tint = MotoOrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Barra de Progreso Seekbar (Con protección antibloqueo al arrastrar)
            Column(modifier = Modifier.fillMaxWidth()) {
                val progresoCalculado = if (duracionMs > 0) (posicionMs.toFloat() / duracionMs.toFloat()).coerceIn(0f, 1f) else 0f
                val progresoMostrado = progresoArrastrando ?: progresoCalculado

                Slider(
                    value = progresoMostrado,
                    onValueChange = { nuevoProgreso ->
                        progresoArrastrando = nuevoProgreso
                    },
                    onValueChangeFinished = {
                        progresoArrastrando?.let { p ->
                            val nuevaPosMs = (p * duracionMs).toLong()
                            onBuscarPosicion(nuevaPosMs)
                            progresoArrastrando = null
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MotoOrangePrimary,
                        activeTrackColor = MotoOrangePrimary,
                        inactiveTrackColor = Color(0xFFE2E8F0)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val posParaTexto = if (progresoArrastrando != null) (progresoArrastrando!! * duracionMs).toLong() else posicionMs
                    val posSec = posParaTexto / 1000
                    val durSec = duracionMs / 1000
                    Text(
                        String.format("%02d:%02d", posSec / 60, posSec % 60),
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        String.format("%02d:%02d", durSec / 60, durSec % 60),
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Controles Principales (Shuffle, Prev, Play/Pause, Next, Loop)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Aleatorio
                IconButton(onClick = onAlternarAleatorio) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Aleatorio",
                        tint = if (modoAleatorio) MotoOrangePrimary else Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Anterior
                IconButton(onClick = onAnterior) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = Color(0xFF0F172A), modifier = Modifier.size(36.dp))
                }

                // Play / Pausa (Botón Central Gigante)
                IconButton(
                    onClick = onPlayPausa,
                    modifier = Modifier.size(64.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MotoOrangePrimary,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (esPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pausa",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                // Siguiente
                IconButton(onClick = onSiguiente) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = Color(0xFF0F172A), modifier = Modifier.size(36.dp))
                }

                // Modo Bucle
                IconButton(onClick = onAlternarBucle) {
                    val iconoBucle = when (modoBucle) {
                        ModoBucle.BUCLE_UNA -> Icons.Default.RepeatOne
                        ModoBucle.BUCLE_TODAS -> Icons.Default.Repeat
                        ModoBucle.SIN_BUCLE -> Icons.Default.Repeat
                    }
                    Icon(
                        imageVector = iconoBucle,
                        contentDescription = "Bucle",
                        tint = if (modoBucle != ModoBucle.SIN_BUCLE) MotoOrangePrimary else Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Fila Inferior: Botón de Favorita y Volver a la App
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (cancion.esFavorita) TxFlameRed.copy(alpha = 0.12f) else Color.White,
                    border = BorderStroke(1.dp, if (cancion.esFavorita) TxFlameRed else Color(0xFFCBD5E1)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.clickable(onClick = onAlternarFavorita)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (cancion.esFavorita) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorita",
                            tint = if (cancion.esFavorita) TxFlameRed else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (cancion.esFavorita) "Favorita" else "Marcar Favorita",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cancion.esFavorita) TxFlameRed else Color(0xFF334155)
                        )
                    }
                }

                Button(
                    onClick = onMinimizar,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Volver a la App", fontSize = 12.sp, color = Color(0xFF0F172A))
                }
            }
        }
    }

    // Modal de asignación rápida a listas de reproducción
    if (mostrarModalGestionListas) {
        var modoCrearNuevaListaRapida by remember { mutableStateOf(false) }
        var nombreNuevaListaRapida by remember { mutableStateOf("") }
        var iconoNuevaListaRapida by remember { mutableStateOf("🎵") }

        AlertDialog(
            onDismissRequest = { mostrarModalGestionListas = false },
            title = { Text("Listas de Reproducción", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selecciona las listas donde deseas incluir esta canción:", fontSize = 12.sp, color = Color(0xFF64748B))

                    if (!modoCrearNuevaListaRapida) {
                        OutlinedButton(
                            onClick = { modoCrearNuevaListaRapida = true },
                            border = BorderStroke(1.dp, MotoOrangePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Crear y Asignar a Nueva Lista", fontSize = 12.sp, color = MotoOrangePrimary, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Nombre de la nueva lista:", fontSize = 11.sp, color = MotoOrangePrimary, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = nombreNuevaListaRapida,
                                onValueChange = { nombreNuevaListaRapida = it },
                                placeholder = { Text("Ej: Rodada Aragua", fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MotoOrangePrimary,
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("🎵", "🏍️", "⚡", "🔥", "🏖️", "🏔️", "🎧").forEach { ic ->
                                    Surface(
                                        shape = CircleShape,
                                        color = if (iconoNuevaListaRapida == ic) MotoOrangePrimary else Color(0xFFF1F5F9),
                                        modifier = Modifier.size(28.dp).clickable { iconoNuevaListaRapida = ic }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) { Text(ic, fontSize = 12.sp) }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        if (nombreNuevaListaRapida.isNotBlank()) {
                                            GESTOR_AUDIO_TX.crearLista(nombreNuevaListaRapida, "", iconoNuevaListaRapida, listOf(cancion.id))
                                            nombreNuevaListaRapida = ""
                                            modoCrearNuevaListaRapida = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Crear", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { modoCrearNuevaListaRapida = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancelar", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (listas.isEmpty()) {
                        Text("No has creado listas aún.", color = Color(0xFF64748B), fontSize = 13.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.heightIn(max = 240.dp)) {
                            items(listas) { lista ->
                                val estaEnLista = lista.cancionIds.contains(cancion.id)
                                Surface(
                                    color = if (estaEnLista) MotoOrangePrimary.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (estaEnLista) MotoOrangePrimary else Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAlternarLista(lista.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(lista.icono, fontSize = 18.sp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(lista.nombre, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                            Text("${lista.cancionIds.size} canciones", fontSize = 11.sp, color = Color(0xFF64748B))
                                        }
                                        Checkbox(
                                            checked = estaEnLista,
                                            onCheckedChange = { onAlternarLista(lista.id) },
                                            colors = CheckboxDefaults.colors(checkedColor = MotoOrangePrimary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { mostrarModalGestionListas = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
                ) {
                    Text("Listo")
                }
            },
            containerColor = Color.White
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTE: VISTA DETALLE DE LISTA DE REPRODUCCION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VistaDetalleLista(
    lista: ListaReproduccionMotera,
    todasLasCanciones: List<CancionMotera>,
    cancionActual: CancionMotera?,
    onVolver: () -> Unit,
    onReproducirLista: (ListaReproduccionMotera) -> Unit,
    onReproducirCancion: (CancionMotera) -> Unit,
    onEliminarCancion: (Long) -> Unit,
    onAgregarCanciones: () -> Unit,
    onEliminarLista: () -> Unit,
    onVerPropiedades: (CancionMotera) -> Unit = {}
) {
    val cancionesDeLista = remember(lista.cancionIds, todasLasCanciones) {
        lista.cancionIds.mapNotNull { id -> todasLasCanciones.find { it.id == id } }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Cabecera con botón volver
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onVolver) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MotoOrangePrimary)
            }
            Text(lista.icono, fontSize = 24.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(lista.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                Text("${cancionesDeLista.size} canciones", fontSize = 12.sp, color = Color(0xFF64748B))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botones de acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onReproducirLista(lista) },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                modifier = Modifier.weight(1f),
                enabled = cancionesDeLista.isNotEmpty()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reproducir Todo")
            }
            Button(
                onClick = onAgregarCanciones,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Agregar Canciones", color = Color(0xFF0F172A))
            }
            OutlinedButton(onClick = onEliminarLista) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar Lista", tint = TxFlameRed, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Lista de canciones
        if (cancionesDeLista.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📂", fontSize = 48.sp)
                    Text("Lista vacia", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Agrega canciones desde tu biblioteca", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(cancionesDeLista) { cancion ->
                    val esActual = cancionActual?.id == cancion.id
                    Surface(
                        color = if (esActual) Color(0xFFFFF7ED) else Color.White,
                        border = BorderStroke(1.dp, if (esActual) MotoOrangePrimary.copy(alpha = 0.4f) else Color(0xFFF1F5F9)),
                        shadowElevation = if (esActual) 1.dp else 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onReproducirCancion(cancion) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (esActual) TxFlameRed.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(if (esActual) "▶" else "🎵", fontSize = 14.sp, color = if (esActual) TxFlameRed else Color(0xFF475569))
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    cancion.titulo,
                                    fontWeight = if (esActual) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (esActual) MotoOrangePrimary else Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${cancion.artista} • ${cancion.duracionFormateada}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = { onVerPropiedades(cancion) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Propiedades", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onEliminarCancion(cancion.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Quitar de lista", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTE: SELECTOR DE LISTA PARA AGREGAR CANCIONES SELECCIONADAS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SelectorListaParaAgregar(
    listas: List<ListaReproduccionMotera>,
    cancionesIds: List<Long>,
    onSeleccionar: (ListaReproduccionMotera) -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Agregar a Lista", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
        text = {
            Column {
                Text("${cancionesIds.size} canciones seleccionadas", fontSize = 12.sp, color = MotoOrangePrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                if (listas.isEmpty()) {
                    Text("No tienes listas creadas", color = Color(0xFF64748B), fontSize = 13.sp)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(listas) { lista ->
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSeleccionar(lista) }
                                    .padding(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(lista.icono, fontSize = 20.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(lista.nombre, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        Text("${lista.cancionIds.size} canciones", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Icon(Icons.Default.Add, contentDescription = null, tint = MotoOrangePrimary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar", color = Color(0xFF64748B)) }
        },
        containerColor = Color.White
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTE: SELECTOR DE CANCIONES PARA AGREGAR A UNA LISTA
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SelectorCancionesParaLista(
    lista: ListaReproduccionMotera,
    todasLasCanciones: List<CancionMotera>,
    onAgregar: (List<Long>) -> Unit,
    onCerrar: () -> Unit
) {
    var seleccionadas by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var textoBusquedaLocal by remember { mutableStateOf("") }

    val cancionesDisponibles = remember(todasLasCanciones, lista.cancionIds, textoBusquedaLocal) {
        val disponibles = todasLasCanciones.filter { !lista.cancionIds.contains(it.id) }
        if (textoBusquedaLocal.isBlank()) disponibles
        else disponibles.filter {
            it.titulo.contains(textoBusquedaLocal, ignoreCase = true) ||
            it.artista.contains(textoBusquedaLocal, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Column {
                Text("Agregar a '${lista.nombre}'", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("${seleccionadas.size} seleccionadas de ${cancionesDisponibles.size} disponibles", fontSize = 11.sp, color = MotoOrangePrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxHeight(0.7f)) {
                OutlinedTextField(
                    value = textoBusquedaLocal,
                    onValueChange = { textoBusquedaLocal = it },
                    placeholder = { Text("Buscar canción...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MotoOrangePrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (cancionesDisponibles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay canciones disponibles", color = Color(0xFF64748B))
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(cancionesDisponibles) { cancion ->
                            val estaSeleccionada = seleccionadas.contains(cancion.id)
                            Surface(
                                color = if (estaSeleccionada) MotoOrangePrimary.copy(alpha = 0.15f) else Color.White,
                                border = BorderStroke(1.dp, if (estaSeleccionada) MotoOrangePrimary else Color(0xFFF1F5F9)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        seleccionadas = if (estaSeleccionada) seleccionadas - cancion.id
                                        else seleccionadas + cancion.id
                                    }
                                    .padding(vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = estaSeleccionada,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = MotoOrangePrimary)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cancion.titulo, fontSize = 13.sp, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${cancion.artista} • ${cancion.duracionFormateada}", fontSize = 10.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (seleccionadas.isNotEmpty()) onAgregar(seleccionadas.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                enabled = seleccionadas.isNotEmpty()
            ) {
                Text("Agregar (${seleccionadas.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar", color = Color(0xFF64748B)) }
        },
        containerColor = Color.White
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPONENTE: DIÁLOGO DE PROPIEDADES, FORMATO Y EDICIÓN DE METADATOS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DialogoPropiedadesCancion(
    cancion: CancionMotera,
    listas: List<ListaReproduccionMotera>,
    onCerrar: () -> Unit,
    onGuardar: (cancionId: Long, nuevoTitulo: String, nuevoArtista: String, nuevoAlbum: String) -> Unit
) {
    val contexto = LocalContext.current
    var tituloEditable by remember { mutableStateOf(cancion.titulo) }
    var artistaEditable by remember { mutableStateOf(cancion.artista) }
    var albumEditable by remember { mutableStateOf(cancion.album) }

    var rutaCaratulaLocal by remember { mutableStateOf(cancion.portadaUriStr) }
    var descargandoCaratula by remember { mutableStateOf(false) }
    var mensajeEstadoCaratula by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Intentar resolver la carátula inicial desde caché
    LaunchedEffect(cancion) {
        val ruta = GESTOR_CARATULAS_TX.obtenerCaratula(contexto, cancion, ModoDescargaCaratulas.WIFI_Y_DATOS)
        if (ruta != null) {
            rutaCaratulaLocal = ruta
        }
    }

    val listasQueLaContienen = remember(listas, cancion.id) {
        listas.filter { it.cancionIds.contains(cancion.id) }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ℹ️", fontSize = 20.sp)
                    Text("INFORMACIÓN Y EDICIÓN", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MotoOrangePrimary)
                }
                IconButton(onClick = onCerrar, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. SECCIÓN DE CARÁTULA Y ACCIÓN DE DESCARGA ONLINE
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Recuadro de imagen
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.size(80.dp)
                        ) {
                            val rutaImg = rutaCaratulaLocal
                            val bitmap = remember(rutaImg) {
                                if (!rutaImg.isNullOrBlank() && !rutaImg.startsWith("content://")) {
                                    try { BitmapFactory.decodeFile(rutaImg) } catch (_: Exception) { null }
                                } else null
                            }

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Carátula",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text("🎵", fontSize = 32.sp)
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = MotoOrangePrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = cancion.formato,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MotoOrangePrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = cancion.tamanoLegible,
                                        fontSize = 11.sp,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    descargandoCaratula = true
                                    mensajeEstadoCaratula = "Buscando carátula HD..."
                                    scope.launch {
                                        val ruta = GESTOR_CARATULAS_TX.forzarDescargaCaratula(contexto, cancion)
                                        descargandoCaratula = false
                                        if (ruta != null) {
                                            rutaCaratulaLocal = ruta
                                            mensajeEstadoCaratula = "✅ Portada descargada"
                                        } else {
                                            mensajeEstadoCaratula = "❌ No se encontró portada online"
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                enabled = !descargandoCaratula,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                if (descargandoCaratula) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = MotoOrangePrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Buscando...", fontSize = 10.sp, color = Color(0xFF0F172A))
                                } else {
                                    Icon(Icons.Default.ImageSearch, contentDescription = null, modifier = Modifier.size(14.dp), tint = MotoOrangePrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Buscar Carátula HD", fontSize = 10.sp, color = Color(0xFF0F172A))
                                }
                            }

                            if (mensajeEstadoCaratula != null) {
                                Text(mensajeEstadoCaratula!!, fontSize = 10.sp, color = MotoOrangePrimary)
                            }
                        }
                    }
                }

                // 2. DETALLES TÉCNICOS
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⏱️ Duración: ", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(cancion.duracionFormateada, fontSize = 11.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📁 Carpeta: ", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(cancion.carpetaContenedora, fontSize = 11.sp, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (cancion.rutaArchivo.isNotBlank()) {
                            Text("📂 Ruta: ${cancion.rutaArchivo}", fontSize = 10.sp, color = Color(0xFF64748B), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        if (listasQueLaContienen.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("📂 En Listas: ", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(listasQueLaContienen.joinToString { it.nombre }, fontSize = 11.sp, color = MotoOrangePrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                Text("EDITAR METADATOS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MotoOrangePrimary)

                // 3. CAMPOS EDITABLES
                OutlinedTextField(
                    value = tituloEditable,
                    onValueChange = { tituloEditable = it },
                    label = { Text("Nombre / Título de la pista", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MotoOrangePrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedLabelColor = MotoOrangePrimary,
                        unfocusedLabelColor = Color(0xFF64748B),
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = artistaEditable,
                    onValueChange = { artistaEditable = it },
                    label = { Text("Artista / Intérprete", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MotoOrangePrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedLabelColor = MotoOrangePrimary,
                        unfocusedLabelColor = Color(0xFF64748B),
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = albumEditable,
                    onValueChange = { albumEditable = it },
                    label = { Text("Álbum / Colección", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MotoOrangePrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedLabelColor = MotoOrangePrimary,
                        unfocusedLabelColor = Color(0xFF64748B),
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onGuardar(cancion.id, tituloEditable, artistaEditable, albumEditable)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Guardar Cambios", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCerrar) {
                Text("Cancelar", color = Color(0xFF64748B))
            }
        },
        containerColor = Color.White
    )
}

