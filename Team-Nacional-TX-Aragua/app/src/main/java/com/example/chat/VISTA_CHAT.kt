package com.example.chat

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.decode.GifDecoder
import android.os.Build
import androidx.compose.ui.layout.ContentScale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch


/**
 * ARCHIVO: VISTA_CHAT.kt
 * 
 * Interfaz de usuario (UI) en Jetpack Compose para el chat comunitario.
 * Se encarga de mostrar la lista de mensajes en tiempo real, barra rápida de emojis,
 * panel de stickers locales y el campo de entrada de texto para enviar mensajes a Firebase.
 */
@Composable
fun VistaChat(
    nombreUsuario: String = "Piloto TX",
    modifier: Modifier = Modifier,
    contextoAndroid: android.content.Context = androidx.compose.ui.platform.LocalContext.current
) {
    val corrutinaScope = rememberCoroutineScope()
    // Escucha en tiempo real de los mensajes desde Firebase
    val mensajes by NubeMensajes.escucharMensajes().collectAsState(initial = emptyList())
    var textoEscrito by remember { mutableStateOf("") }
    val estadoLista = rememberLazyListState()

    // Opciones de borrado automático (Mensajes Temporales)
    var mostrarMenuExpiracion by remember { mutableStateOf(false) }
    var duracionExpiracionMs by remember { mutableStateOf<Long?>(null) } // null = Off

    // Configuración de Coil para soportar GIFs y WebP Animados
    val imageLoader = remember {
        ImageLoader.Builder(contextoAndroid)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    // Estado del panel de stickers
    var mostrarPanelStickers by remember { mutableStateOf(false) }
    val stickersGuardados by GestorStickers.stickersGuardados.collectAsState(initial = emptyList())

    // Cargar stickers al iniciar
    androidx.compose.runtime.LaunchedEffect(Unit) {
        GestorStickers.cargarStickersGuardados(contextoAndroid)
    }

    // Desplazamiento automático al último mensaje recibido
    androidx.compose.runtime.LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) {
            estadoLista.animateScrollToItem(mensajes.size - 1)
        }
    }

    // Enviar sticker seleccionado
    fun enviarSticker(archivoSticker: java.io.File) {
        corrutinaScope.launch {
            val uriArchivo = GestorStickers.obtenerUriArchivo(contextoAndroid, archivoSticker)
            val urlDescarga = NubeArchivos.subirArchivo(
                uriArchivo = uriArchivo,
                tipoArchivo = NubeArchivos.TipoArchivo.STICKER,
                nombrePersonalizado = archivoSticker.name
            )
            urlDescarga?.let { url ->
                NubeMensajes.enviarMensajeMultimedia(
                    tipo = TipoMensaje.STICKER,
                    urlMultimedia = url,
                    nombreArchivo = archivoSticker.name,
                    emisor = nombreUsuario,
                    apodoEmisor = nombreUsuario,
                    expiresAt = duracionExpiracionMs?.let { System.currentTimeMillis() + it }
                )
            }
        }
        mostrarPanelStickers = false
    }

    // 📍 Gestor de Ubicación GPS
    val gestorUbicacion = remember { GestorUbicacion(contextoAndroid) }
    var estaCapturandoUbicacion by remember { mutableStateOf(false) }
    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            corrutinaScope.launch {
                estaCapturandoUbicacion = true
                val coords = gestorUbicacion.capturarCoordenadaActual()
                estaCapturandoUbicacion = false
                if (coords != null) {
                    NubeMensajes.enviarMensajeMultimedia(
                        tipo = TipoMensaje.UBICACION,
                        urlMultimedia = "geo:$coords",
                        nombreArchivo = "ubicacion.geo",
                        texto = coords,
                        emisor = nombreUsuario,
                        apodoEmisor = nombreUsuario,
                        expiresAt = duracionExpiracionMs?.let { System.currentTimeMillis() + it }
                    )
                    android.widget.Toast.makeText(contextoAndroid, "📍 Ubicación compartida", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(contextoAndroid, "No se pudo obtener la posición GPS", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            android.widget.Toast.makeText(contextoAndroid, "Permiso de ubicación denegado", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun compartirUbicacion() {
        val fine = androidx.core.content.ContextCompat.checkSelfPermission(
            contextoAndroid,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
            contextoAndroid,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (fine || coarse) {
            corrutinaScope.launch {
                estaCapturandoUbicacion = true
                val coords = gestorUbicacion.capturarCoordenadaActual()
                estaCapturandoUbicacion = false
                if (coords != null) {
                    NubeMensajes.enviarMensajeMultimedia(
                        tipo = TipoMensaje.UBICACION,
                        urlMultimedia = "geo:$coords",
                        nombreArchivo = "ubicacion.geo",
                        texto = coords,
                        emisor = nombreUsuario,
                        apodoEmisor = nombreUsuario,
                        expiresAt = duracionExpiracionMs?.let { System.currentTimeMillis() + it }
                    )
                    android.widget.Toast.makeText(contextoAndroid, "📍 Ubicación compartida", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(contextoAndroid, "No se pudo obtener la posición GPS", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0E121A))
    ) {
        // Encabezado del Chat
        Surface(
            color = Color(0xFF161C26),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = "Chat",
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sala de Chat Comunitaria",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Conectado como: $nombreUsuario",
                        fontSize = 11.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }

                // Menú de Mensajes Temporales
                Box {
                    IconButton(onClick = { mostrarMenuExpiracion = true }) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Borrado Automático",
                            tint = if (duracionExpiracionMs != null) Color(0xFF00E676) else Color(0xFFB0BEC5),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = mostrarMenuExpiracion,
                        onDismissRequest = { mostrarMenuExpiracion = false },
                        modifier = Modifier.background(Color(0xFF1B2230))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Desactivado", color = Color.White) },
                            onClick = { 
                                duracionExpiracionMs = null
                                mostrarMenuExpiracion = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("24 horas", color = Color.White) },
                            onClick = { 
                                duracionExpiracionMs = 24L * 60 * 60 * 1000
                                mostrarMenuExpiracion = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("7 días", color = Color.White) },
                            onClick = { 
                                duracionExpiracionMs = 7L * 24 * 60 * 60 * 1000
                                mostrarMenuExpiracion = false 
                            }
                        )
                    }
                }
            }
        }

        // Lista de Mensajes
        LazyColumn(
            state = estadoLista,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (mensajes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no hay mensajes. ¡Escribe el primero!",
                            color = Color(0xFF78909C),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(mensajes, key = { it.id.ifEmpty { "${it.hora}_${it.texto.hashCode()}" } }) { msg ->
                    val esMio = msg.emisor == nombreUsuario
                    TarjetaMensaje(
                        mensaje = msg, 
                        esMio = esMio,
                        imageLoader = imageLoader
                    )
                }
            }
        }

        // Panel de Stickers (se muestra arriba de la barra de emojis/entrada)
        if (mostrarPanelStickers) {
            PanelStickers(
                stickers = stickersGuardados,
                imageLoader = imageLoader,
                onStickerClick = { archivo -> enviarSticker(archivo) },
                onCerrar = { mostrarPanelStickers = false },
                onImportar = { GestorStickers.abrirSelectorStickers(contextoAndroid as android.app.Activity) }
            )
        }

        // Barra Rápida de Emojis Básicos
        Surface(
            color = Color(0xFF121722),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Fila de botones: Emojis + Stickers + Importar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón toggle Stickers
                    IconButton(
                        onClick = { mostrarPanelStickers = !mostrarPanelStickers },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (mostrarPanelStickers) Color(0xFFFF3B30) else Color(0xFF1B2232),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("boton_stickers")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEmotions,
                            contentDescription = "Stickers",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Botón Importar Sticker
                    IconButton(
                        onClick = { GestorStickers.abrirSelectorStickers(contextoAndroid as android.app.Activity) },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFF1B2232),
                            contentColor = Color(0xFF00B0FF)
                        ),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("boton_importar_sticker")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Importar sticker",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 📍 Botón Compartir Ubicación
                    IconButton(
                        onClick = { compartirUbicacion() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFF1B2232),
                            contentColor = Color(0xFFEF5350)
                        ),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("boton_compartir_ubicacion")
                    ) {
                        if (estaCapturandoUbicacion) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFFEF5350))
                        } else {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Compartir Ubicación",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Emojis básicos
                    val listaEmojis = listOf("🏍️", "🏁", "🔧", "⛽", "⚠️", "👍", "🔥", "💨", "✌️", "🤝", "🛑", "👏")
                    LazyRow(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listaEmojis) { emoji ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1B2232),
                                border = BorderStroke(0.5.dp, Color(0xFF2C384E)),
                                modifier = Modifier.clickable {
                                    textoEscrito += emoji
                                }
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Barra Inferior de Entrada de Texto
        Surface(
            color = Color(0xFF161C26),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = textoEscrito,
                    onValueChange = { textoEscrito = it },
                    placeholder = { Text("Escribe un mensaje...", fontSize = 13.sp, color = Color(0xFF78909C)) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_mensaje_chat"),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF3B30),
                        unfocusedBorderColor = Color(0xFF2C384E),
                        focusedContainerColor = Color(0xFF0E121A),
                        unfocusedContainerColor = Color(0xFF0E121A)
                    ),
                    maxLines = 3
                )

                IconButton(
                    onClick = {
                        if (textoEscrito.isNotBlank()) {
                            NubeMensajes.enviarMensaje(
                                texto = textoEscrito,
                                emisor = nombreUsuario,
                                apodoEmisor = nombreUsuario,
                                expiresAt = duracionExpiracionMs?.let { System.currentTimeMillis() + it }
                            )
                            textoEscrito = ""
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (textoEscrito.isNotBlank()) Color(0xFFFF3B30) else Color(0xFF2C384E),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("boton_enviar_chat")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar mensaje",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Panel de stickers locales con grid y botón importar.
 */
@Composable
fun PanelStickers(
    stickers: List<java.io.File>,
    imageLoader: coil.ImageLoader,
    onStickerClick: (java.io.File) -> Unit,
    onCerrar: () -> Unit,
    onImportar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF121722),
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Encabezado del panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mis Stickers (${stickers.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Botón Importar
                    TextButton(
                        onClick = onImportar,
                        modifier = Modifier.testTag("btn_importar_sticker_panel")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF00B0FF))
                            Text("Importar", color = Color(0xFF00B0FF), fontSize = 12.sp)
                        }
                    }
                    // Botón Cerrar
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar stickers", tint = Color(0xFFB0BEC5), modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Grid de stickers
            if (stickers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EmojiEmotions, contentDescription = null, tint = Color(0xFF78909C), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No hay stickers guardados", color = Color(0xFF78909C), fontSize = 12.sp)
                        Text("Toca 'Importar' para añadir stickers .webp", color = Color(0xFF78909C).copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 8.dp)
                ) {
                    items(stickers) { archivo ->
                        StickerThumbnail(
                            archivo = archivo,
                            imageLoader = imageLoader,
                            onClick = { onStickerClick(archivo) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Miniatura de sticker en el grid.
 */
@Composable
fun StickerThumbnail(
    archivo: java.io.File,
    imageLoader: coil.ImageLoader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriArchivo = androidx.core.content.FileProvider.getUriForFile(
        androidx.compose.ui.platform.LocalContext.current,
        "${androidx.compose.ui.platform.LocalContext.current.packageName}.fileprovider",
        archivo
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1B2232),
        border = BorderStroke(1.dp, Color(0xFF2C384E)),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(uriArchivo)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = "Sticker ${archivo.name}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }
    }
}

/**
 * Componente que dibuja la burbuja de cada mensaje.
 * Soporta: texto, imagen, audio, sticker (sin burbuja para stickers).
 */
@Composable
fun TarjetaMensaje(
    mensaje: Mensaje,
    esMio: Boolean,
    imageLoader: coil.ImageLoader,
    modifier: Modifier = Modifier
) {
    val formatoHora = remember(mensaje.hora) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(mensaje.hora))
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!esMio) {
            if (mensaje.senderPhotoUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(mensaje.senderPhotoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto de perfil de ${mensaje.emisor}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C384E)),
                    contentAlignment = Alignment.Center
                ) {
                    val iniciales = mensaje.emisor.take(2).uppercase()
                    Text(
                        text = if (iniciales.isBlank()) "TX" else iniciales,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9500)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (esMio) Alignment.End else Alignment.Start
        ) {
            if (!esMio) {
                Text(
                    text = mensaje.emisor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9500)
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Renderizado según tipo de mensaje
            when (mensaje.tipo) {
                TipoMensaje.STICKER -> {
                    // Sticker SIN burbuja, sin fondo - solo la imagen WebP
                    mensaje.urlMultimedia.let { url ->
                        if (url.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(url)
                                    .build(),
                                imageLoader = imageLoader,
                                contentDescription = "Sticker de ${mensaje.emisor}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }
                    }
                }
                TipoMensaje.IMAGEN -> {
                    // Imagen con burbuja normal
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (esMio) Color(0xFFFF3B30) else Color(0xFF1B2230),
                        border = BorderStroke(0.5.dp, Color(0xFF2C384E).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            AsyncImage(
                                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(mensaje.urlMultimedia)
                                    .build(),
                                contentDescription = "Imagen de ${mensaje.emisor}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.5f)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            if (mensaje.texto.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = mensaje.texto, color = Color.White, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = formatoHora, fontSize = 9.sp, color = if (esMio) Color.White.copy(alpha = 0.8f) else Color(0xFF90A4AE), modifier = Modifier.align(Alignment.End))
                        }
                    }
                }
                TipoMensaje.UBICACION -> {
                    TarjetaUbicacion(
                        coordenadasString = mensaje.texto.removePrefix("📍 ").trim(),
                        esRemitentePropio = esMio,
                        nombrePiloto = mensaje.apodoEmisor.ifBlank { mensaje.emisor }
                    )
                }
                else -> {
                    // Texto, audio y otros con burbuja normal
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = if (esMio) 12.dp else 2.dp,
                            topEnd = if (esMio) 2.dp else 12.dp,
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp
                        ),
                        color = if (esMio) Color(0xFFFF3B30) else Color(0xFF1B2230),
                        border = BorderStroke(0.5.dp, Color(0xFF2C384E).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            if (mensaje.tipo == TipoMensaje.AUDIO) {
                                var isPlaying by remember { mutableStateOf(false) }
                                var progress by remember { mutableFloatStateOf(0f) }
                                var transcripcion by remember { mutableStateOf<String?>(null) }
                                var cargandoTranscripcion by remember { mutableStateOf(false) }

                                Column(modifier = Modifier.widthIn(min = 180.dp, max = 220.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        IconButton(
                                            onClick = { isPlaying = !isPlaying },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                                tint = Color.White,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Text(
                                            text = mensaje.nombreArchivo.ifBlank { "Nota de voz" }, 
                                            color = Color.White, 
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        // Botón Transcripción
                                        IconButton(
                                            onClick = { 
                                                cargandoTranscripcion = true
                                                // Simular extracción
                                                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                                    kotlinx.coroutines.delay(2000)
                                                    transcripcion = "Audio convertido a texto (Simulación)."
                                                    cargandoTranscripcion = false
                                                }
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            if (cargandoTranscripcion) {
                                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.ClosedCaption,
                                                    contentDescription = "Transcribir audio",
                                                    tint = Color.White,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Progress bar de audio en la parte inferior
                                    Slider(
                                        value = progress,
                                        onValueChange = { progress = it },
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(20.dp)
                                            .padding(horizontal = 4.dp)
                                    )

                                    transcripcion?.let { txt ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "“$txt”",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 12.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            modifier = Modifier
                                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                .padding(6.dp)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = mensaje.texto,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatoHora,
                                fontSize = 9.sp,
                                color = if (esMio) Color.White.copy(alpha = 0.8f) else Color(0xFF90A4AE),
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }
    }
}