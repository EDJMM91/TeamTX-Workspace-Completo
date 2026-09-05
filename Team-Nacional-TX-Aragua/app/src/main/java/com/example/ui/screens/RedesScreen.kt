package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dashboard.DashboardFondoConfig
import com.example.ui.components.openUrl
import com.example.ui.theme.TxFlameRed

data class RedSocialItem(
    val id: String,
    val nombre: String,
    val handle: String,
    val categoria: String,
    val descripcion: String,
    val url: String,
    val colorMarca: Color,
    val icono: ImageVector,
    val esOficialVerificada: Boolean = true,
    val esDestacada: Boolean = false,
    val consejosUso: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var categoriaSeleccionada by remember { mutableStateOf("TODAS") }
    var textoBusqueda by remember { mutableStateOf("") }

    val redesOficiales = remember {
        listOf(
            RedSocialItem(
                id = "linktree",
                nombre = "Linktree Oficial TX",
                handle = "@Teamnacionaltx200aragua",
                categoria = "HUB CENTRAL",
                descripcion = "Portal central con todos los accesos rápidos, reglamentos, rifas activas, enlaces de compra y convocatorias del Team TX.",
                url = "https://linktr.ee/Teamnacionaltx200aragua",
                colorMarca = Color(0xFF43E660),
                icono = Icons.Default.Link,
                esDestacada = true,
                consejosUso = "Accede a todos los enlaces del club en un solo toque."
            ),
            RedSocialItem(
                id = "whatsapp",
                nombre = "Comunidad WhatsApp Oficial",
                handle = "Team TX Aragua & Nacional",
                categoria = "MENSAJERÍA",
                descripcion = "Grupos de rodadas, canal de alertas viales, avisos de punto de encuentro y hermandad en vivo.",
                url = "https://chat.whatsapp.com/teamtxvenezuela",
                colorMarca = Color(0xFF25D366),
                icono = Icons.Default.Chat,
                esDestacada = true,
                consejosUso = "Usa el canal para avisos de salida y emergencias en ruta."
            ),
            RedSocialItem(
                id = "telegram",
                nombre = "Telegram Oficial Team TX",
                handle = "t.me/+R_oloXwkGqhkNzJh",
                categoria = "MENSAJERÍA",
                descripcion = "Canal oficial con fotos y videos de rodadas en resolución completa sin compresión, avisos de ruta y archivos de comunidad.",
                url = "https://t.me/+R_oloXwkGqhkNzJh",
                colorMarca = Color(0xFF229ED9),
                icono = Icons.Default.Send,
                consejosUso = "Ideal para descargar álbumes de fotos de cada rodada."
            ),
            RedSocialItem(
                id = "tiktok",
                nombre = "TikTok Oficial Team TX",
                handle = "@teamnacionaltx.aragua",
                categoria = "MULTIMEDIA",
                descripcion = "Videos de arrancadas, exhibiciones de motos TX 200, resúmenes de caravana y los mejores momentos en carretera.",
                url = "https://www.tiktok.com/@teamnacionaltx.aragua",
                colorMarca = Color(0xFF111827),
                icono = Icons.Default.MusicNote,
                esDestacada = true,
                consejosUso = "Etiquétanos en tus videos de ruta con el hashtag #TeamTX."
            ),
            RedSocialItem(
                id = "instagram",
                nombre = "Instagram Oficial",
                handle = "@teamnacionaltx.aragua",
                categoria = "MULTIMEDIA",
                descripcion = "Fotografía oficial de miembros, historias en vivo de rodadas nacionales, parches conmemorativos y eventos especiales.",
                url = "https://instagram.com/teamnacionaltx.aragua",
                colorMarca = Color(0xFFE1306C),
                icono = Icons.Default.CameraAlt,
                consejosUso = "Menciona la cuenta en tus historias de ruta para ser reposteado."
            ),
            RedSocialItem(
                id = "youtube",
                nombre = "YouTube Oficial Team TX",
                handle = "Team Nacional TX Venezuela",
                categoria = "MULTIMEDIA",
                descripcion = "Documentales de viajes largos a través de Venezuela, guías técnicas de la TX 200 y tomas aéreas de caravanas.",
                url = "https://www.youtube.com/results?search_query=Team+Nacional+TX+Aragua",
                colorMarca = Color(0xFFFF0000),
                icono = Icons.Default.PlayCircle,
                consejosUso = "Suscríbete para ver las travesías nacionales en alta definición."
            ),
            RedSocialItem(
                id = "soporte_directiva",
                nombre = "Atención & Soporte Directiva",
                handle = "+58 424-3769999",
                categoria = "DIRECTIVA",
                descripcion = "Línea directa para verificación de estatus, vinculación de cuenta, dudas de directiva o reporte de grupos zonales.",
                url = "https://wa.me/584243769999?text=Hola%20Directiva%20Team%20TX,%20escribo%20desde%20la%20App%20Team%20TX",
                colorMarca = Color(0xFFB45309),
                icono = Icons.Default.Shield,
                consejosUso = "Disponible para pilotos activos que requieran asistencia de carnet o membresía."
            )
        )
    }

    val categorias = listOf("TODAS", "HUB CENTRAL", "MENSAJERÍA", "MULTIMEDIA", "DIRECTIVA")

    val redesFiltradas = remember(categoriaSeleccionada, textoBusqueda) {
        redesOficiales.filter { red ->
            val coincideCategoria = (categoriaSeleccionada == "TODAS" || red.categoria.equals(categoriaSeleccionada, ignoreCase = true))
            val coincideTexto = textoBusqueda.isBlank() ||
                red.nombre.contains(textoBusqueda, ignoreCase = true) ||
                red.handle.contains(textoBusqueda, ignoreCase = true) ||
                red.descripcion.contains(textoBusqueda, ignoreCase = true)
            coincideCategoria && coincideTexto
        }
    }

    Scaffold(
        containerColor = DashboardFondoConfig.ColorFondoClaro,
        topBar = {
            Surface(
                color = DashboardFondoConfig.ColorTarjetaClara,
                border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Volver",
                                    tint = DashboardFondoConfig.ColorTextoPrimario
                                )
                            }
                            Column {
                                Text(
                                    text = "REDES SOCIALES & CANALES",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = DashboardFondoConfig.ColorTextoPrimario,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Team Nacional TX Aragua • Venezuela",
                                    fontSize = 11.sp,
                                    color = DashboardFondoConfig.ColorTextoSecundario
                                )
                            }
                        }

                        // Botón de acceso rápido al Linktree
                        IconButton(
                            onClick = {
                                openUrl(context, "https://linktr.ee/Teamnacionaltx200aragua")
                            }
                        ) {
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = "Linktree",
                                tint = TxFlameRed
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 14.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(DashboardFondoConfig.ColorFondoClaro)
        ) {
            // ═══════════════════════════════════════════════════════════════
            // BANNER HERO TÁCTICO DE BIENVENIDA
            // ═══════════════════════════════════════════════════════════════
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
                    border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        DashboardFondoConfig.ColorContenedorAzul,
                                        DashboardFondoConfig.ColorTarjetaClara
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(TxFlameRed.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Public,
                                        contentDescription = null,
                                        tint = TxFlameRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "CANALES OFICIALES DE LA HERMANDAD",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = DashboardFondoConfig.ColorTextoPrimario,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "Conéctate, comparte rutas y entérate de cada rodada nacional",
                                        fontSize = 11.sp,
                                        color = DashboardFondoConfig.ColorTextoSecundario
                                    )
                                }
                            }

                            Text(
                                text = "En este módulo encuentras todos los enlaces oficiales verificados del Team TX. Puedes ingresar directamente, copiar los links al portapapeles o compartirlos con tus compañeros de ruta.",
                                fontSize = 12.sp,
                                color = DashboardFondoConfig.ColorTextoSecundario,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // BUSCADOR Y FILTROS POR CATEGORÍA
            // ═══════════════════════════════════════════════════════════════
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Campo de búsqueda
                    OutlinedTextField(
                        value = textoBusqueda,
                        onValueChange = { textoBusqueda = it },
                        placeholder = {
                            Text("Buscar red, canal o cuenta...", fontSize = 13.sp, color = DashboardFondoConfig.ColorTextoSecundario)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TxFlameRed)
                        },
                        trailingIcon = {
                            if (textoBusqueda.isNotBlank()) {
                                IconButton(onClick = { textoBusqueda = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TxFlameRed,
                            unfocusedBorderColor = DashboardFondoConfig.ColorBordeClaro,
                            focusedContainerColor = DashboardFondoConfig.ColorTarjetaClara,
                            unfocusedContainerColor = DashboardFondoConfig.ColorTarjetaClara
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Carrusel de categorías
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categorias) { cat ->
                            val estaSeleccionada = categoriaSeleccionada == cat
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (estaSeleccionada) TxFlameRed else DashboardFondoConfig.ColorContenedorAzul,
                                border = BorderStroke(
                                    1.dp,
                                    if (estaSeleccionada) TxFlameRed else DashboardFondoConfig.ColorBordeClaro
                                ),
                                modifier = Modifier.clickable { categoriaSeleccionada = cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (estaSeleccionada) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (estaSeleccionada) Color.White else DashboardFondoConfig.ColorTextoPrimario,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // LISTA DE TARJETAS INTERACTIVAS
            // ═══════════════════════════════════════════════════════════════
            if (redesFiltradas.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
                        border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                "No se encontraron redes",
                                fontWeight = FontWeight.Bold,
                                color = DashboardFondoConfig.ColorTextoPrimario,
                                fontSize = 14.sp
                            )
                            Text(
                                "Prueba con otra palabra clave o selecciona la categoría 'TODAS'.",
                                fontSize = 12.sp,
                                color = DashboardFondoConfig.ColorTextoSecundario,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(redesFiltradas, key = { it.id }) { red ->
                    TarjetaRedSocialInteractiva(red = red)
                }
            }
        }
    }
}

@Composable
fun TarjetaRedSocialInteractiva(
    red: RedSocialItem
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorTarjetaClara),
        border = BorderStroke(
            if (red.esDestacada) 1.5.dp else 1.dp,
            if (red.esDestacada) red.colorMarca.copy(alpha = 0.6f) else DashboardFondoConfig.ColorBordeClaro
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Fila superior: Icono de red + Nombre / Handle + Badge oficial
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(red.colorMarca.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = red.icono,
                            contentDescription = red.nombre,
                            tint = red.colorMarca,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = red.nombre,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DashboardFondoConfig.ColorTextoPrimario,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (red.esOficialVerificada) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verificado",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Text(
                            text = red.handle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = red.colorMarca,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Tag de categoría
                Surface(
                    color = DashboardFondoConfig.ColorContenedorAzul,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, DashboardFondoConfig.ColorBordeClaro)
                ) {
                    Text(
                        text = red.categoria,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardFondoConfig.ColorTextoSecundario,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Descripción
            Text(
                text = red.descripcion,
                fontSize = 12.sp,
                color = DashboardFondoConfig.ColorTextoSecundario,
                lineHeight = 16.sp
            )

            // Consejo de uso si existe
            if (red.consejosUso.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DashboardFondoConfig.ColorContenedorAzul)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = red.consejosUso,
                        fontSize = 10.sp,
                        color = DashboardFondoConfig.ColorTextoSecundario,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Fila de botones de acción interactiva
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón principal: Abrir
                Button(
                    onClick = {
                        openUrl(context, red.url)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = red.colorMarca
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Abrir Canal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                // Botón secundario: Copiar Enlace
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText(red.nombre, red.url)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(context, "¡Enlace de ${red.nombre} copiado!", Toast.LENGTH_SHORT).show()
                    },
                    border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DashboardFondoConfig.ColorTarjetaClara
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copiar",
                        tint = DashboardFondoConfig.ColorTextoPrimario,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Copiar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DashboardFondoConfig.ColorTextoPrimario
                    )
                }

                // Botón secundario: Compartir
                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "🏍️ *${red.nombre} - Team Nacional TX*\nSigue nuestra comunidad oficial en:\n${red.url}"
                            )
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Compartir ${red.nombre}")
                        context.startActivity(shareIntent)
                    },
                    border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DashboardFondoConfig.ColorTarjetaClara
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Compartir",
                        tint = DashboardFondoConfig.ColorTextoPrimario,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Enviar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DashboardFondoConfig.ColorTextoPrimario
                    )
                }
            }
        }
    }
}
