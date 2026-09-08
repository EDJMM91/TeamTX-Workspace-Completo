package com.example.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MemberProfile
import com.example.data.model.MemberRole

/**
 * Cabecera principal del Dashboard con logotipo oficial y acceso al Carnet TX.
 */
@Composable
fun DashboardHeader(
    currentMember: MemberProfile?,
    onOpenCarnet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resLogo = remember {
        val id = context.resources.getIdentifier("logoteam", "drawable", context.packageName)
        if (id != 0) id else android.R.drawable.ic_menu_compass
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = resLogo),
                contentDescription = "Logotipo Oficial Team TX",
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, DashboardFondoConfig.ColorRojoCarrera, CircleShape)
            )
            Column {
                Text(
                    text = "Team Nacional TX VZLA",
                    color = DashboardFondoConfig.ColorTextoPrimario,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Capítulo Aragua",
                    color = DashboardFondoConfig.ColorRojoCarrera,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Botón de Perfil / Carnet TX
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(DashboardFondoConfig.ColorTarjetaClara)
                .border(1.dp, DashboardFondoConfig.ColorBordeClaro, CircleShape)
                .clickable(onClick = onOpenCarnet),
            contentAlignment = Alignment.Center
        ) {
            if (!currentMember?.profilePhotoUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentMember?.profilePhotoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Carnet TX",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = "Carnet TX",
                    tint = DashboardFondoConfig.ColorRojoCarrera,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Indicador de solvencia / estado activo
            val colorEstado = if (currentMember?.solvencyStatus == true) Color(0xFF00E676) else Color(0xFFFF9100)
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(colorEstado)
                    .border(1.5.dp, Color.White, CircleShape)
            )
        }
    }
}

/**
 * Tarjeta de estado y perfil del piloto autenticado en el Dashboard.
 */
@Composable
fun PilotStatusCard(
    currentMember: MemberProfile?,
    onOpenCarnet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roleColor = when (currentMember?.role) {
        MemberRole.DESARROLLADOR -> Color(0xFF00E5FF)
        MemberRole.PRESIDENTE, MemberRole.VICEPRESIDENTE, MemberRole.DIRECTIVA -> DashboardFondoConfig.ColorDoradoOro
        MemberRole.CAPITAN_RUTA, MemberRole.DISCIPLINARIO -> DashboardFondoConfig.ColorRojoCarrera
        MemberRole.MECANICO_OFICIAL, MemberRole.SECRETARIO, MemberRole.SEGURIDAD_VIAL -> Color(0xFF1E88E5)
        else -> DashboardFondoConfig.ColorRojoCarrera
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(14.dp))
            .clickable(onClick = onOpenCarnet),
        shape = RoundedCornerShape(14.dp),
        color = DashboardFondoConfig.ColorTarjetaClara,
        border = androidx.compose.foundation.BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PILOTO AUTENTICADO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardFondoConfig.ColorTextoSecundario,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = currentMember?.fullName?.ifBlank { "Piloto Team TX" } ?: "Piloto Oficial",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DashboardFondoConfig.ColorTextoPrimario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!currentMember?.nickname.isNullOrBlank()) {
                        Text(
                            text = "\"${currentMember?.nickname}\"",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DashboardFondoConfig.ColorRojoCarrera,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = roleColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, roleColor.copy(alpha = 0.5f)),
                    modifier = Modifier.widthIn(max = 145.dp)
                ) {
                    Text(
                        text = currentMember?.role?.displayName ?: "Aspirante",
                        color = roleColor,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DashboardFondoConfig.ColorBordeClaro.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = "Moto",
                        tint = DashboardFondoConfig.ColorTextoSecundario,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${currentMember?.bikeBrand ?: "Keeway"} ${currentMember?.bikeModel ?: "TX 200"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = DashboardFondoConfig.ColorTextoPrimario
                    )
                    if (!currentMember?.bikePlate.isNullOrBlank()) {
                        Text(
                            text = "• ${currentMember?.bikePlate}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DashboardFondoConfig.ColorTextoSecundario
                        )
                    }
                }

                Text(
                    text = "N° Socio: ${currentMember?.memberNumber?.ifBlank { "-" } ?: "-"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardFondoConfig.ColorRojoCarrera
                )
            }
        }
    }
}

/**
 * Barra / Sidebar desplegable interactiva entre el piloto autenticado y el panel.
 * Muestra los módulos más usados ordenados por frecuencia de uso, accesos de sistema (Info, Ajustes)
 * y el acceso exclusivo a Gobernanza Directiva solo si el usuario tiene rol directivo.
 */
@Composable
fun DashboardSidebarBar(
    currentMember: MemberProfile?,
    isDirectivaMode: Boolean = false,
    onNavigateToTab: (com.example.NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val esDirectivo = isDirectivaMode ||
            currentMember?.isDirectiva == true ||
            currentMember?.role?.canManageApp == true ||
            currentMember?.role == MemberRole.PRESIDENTE ||
            currentMember?.role == MemberRole.DESARROLLADOR

    val usoMap = remember(isExpanded) { com.example.ui.preferences.PreferenciasApp.obtenerUsoModulosMap() }

    val modulosCandidatos = remember {
        listOf(
            com.example.NavigationTab.FEED to "Muro de Noticias",
            com.example.NavigationTab.CHAT to "Chat Táctico",
            com.example.NavigationTab.NOTIFICACIONES to "Avisos y Notificaciones",
            com.example.NavigationTab.MAPA to "Mapa TX y Radar",
            com.example.NavigationTab.VELOCIMETRO to "Velocímetro",
            com.example.NavigationTab.PLAYER to "Player TX Pro",
            com.example.NavigationTab.RIDES to "Rodadas",
            com.example.NavigationTab.MERCADO to "Mercado Biker",
            com.example.NavigationTab.CALENDARIO to "Calendario",
            com.example.NavigationTab.RANKING to "Ranking",
            com.example.NavigationTab.MEMBERS to "Directorio Miembros",
            com.example.NavigationTab.DIRECTORIO to "Directorio Comercios",
            com.example.NavigationTab.RETOS to "Retos y Desafíos",
            com.example.NavigationTab.PASAPORTE to "Pasaporte Motero",
            com.example.NavigationTab.FINANCES to "Tesorería",
            com.example.NavigationTab.INVENTORY to "Inventario",
            com.example.NavigationTab.SOS to "SOS Vial",
            com.example.NavigationTab.NORMATIVAS to "Normativas",
            com.example.NavigationTab.REDES to "Redes TX"
        )
    }

    val modulosOrdenados = remember(usoMap) {
        modulosCandidatos.sortedByDescending { (tab, _) ->
            usoMap[tab.name] ?: usoMap[tab.tag] ?: 0
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .shadow(elevation = 1.5.dp, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = DashboardFondoConfig.ColorTarjetaClara,
        border = androidx.compose.foundation.BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(DashboardFondoConfig.ColorContenedorRojo),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menú Sidebar",
                            tint = DashboardFondoConfig.ColorRojoCarrera,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "Módulos & Accesos Rápidos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DashboardFondoConfig.ColorTextoPrimario
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DashboardFondoConfig.ColorRojoCarrera.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (isExpanded) "Ocultar" else "Ver Sidebar",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DashboardFondoConfig.ColorRojoCarrera,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = DashboardFondoConfig.ColorTextoSecundario,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = DashboardFondoConfig.ColorBordeClaro.copy(alpha = 0.5f))

                    Text(
                        text = "🔥 MÓDULOS MÁS USADOS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = DashboardFondoConfig.ColorRojoCarrera,
                        letterSpacing = 0.5.sp
                    )

                    val topModulos = modulosOrdenados.take(5)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        topModulos.forEach { (tab, label) ->
                            val conteo = usoMap[tab.name] ?: usoMap[tab.tag] ?: 0
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DashboardFondoConfig.ColorContenedorClaro,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo(tab.name)
                                        isExpanded = false
                                        onNavigateToTab(tab)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = tab.iconFilled,
                                            contentDescription = null,
                                            tint = DashboardFondoConfig.ColorRojoCarrera,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DashboardFondoConfig.ColorTextoPrimario
                                        )
                                    }
                                    if (conteo > 0) {
                                        Text(
                                            text = "$conteo accesos",
                                            fontSize = 9.5.sp,
                                            color = DashboardFondoConfig.ColorTextoSecundario
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DashboardFondoConfig.ColorBordeClaro.copy(alpha = 0.5f))

                    Text(
                        text = "⚙️ MÓDULOS DE SISTEMA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = DashboardFondoConfig.ColorTextoSecundario,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("INFO")
                                isExpanded = false
                                onNavigateToTab(com.example.NavigationTab.INFO)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = DashboardFondoConfig.ColorRojoCarrera, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Info App", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DashboardFondoConfig.ColorTextoPrimario)
                        }

                        OutlinedButton(
                            onClick = {
                                com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("CONFIGURACIONES")
                                isExpanded = false
                                onNavigateToTab(com.example.NavigationTab.CONFIGURACIONES)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(32.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF546E7A), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajustes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DashboardFondoConfig.ColorTextoPrimario)
                        }
                    }

                    if (esDirectivo) {
                        HorizontalDivider(color = DashboardFondoConfig.ColorBordeClaro.copy(alpha = 0.5f))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DashboardFondoConfig.ColorContenedorDorado,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DashboardFondoConfig.ColorDoradoOro.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("DIRECTIVA")
                                    isExpanded = false
                                    onNavigateToTab(com.example.NavigationTab.DIRECTIVA)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = DashboardFondoConfig.ColorDoradoOro, modifier = Modifier.size(16.dp))
                                    Column {
                                        Text("Gobernanza Directiva", fontWeight = FontWeight.Black, fontSize = 12.sp, color = DashboardFondoConfig.ColorTextoPrimario)
                                        Text("Panel exclusivo de decisiones y sanciones", fontSize = 9.5.sp, color = DashboardFondoConfig.ColorTextoSecundario)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = DashboardFondoConfig.ColorDoradoOro, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta Bento táctica para acceso directo a módulos destacados.
 */
@Composable
fun BentoGridCard(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    colorIcono: Color,
    colorFondoIcono: Color,
    badgeTexto: String? = null,
    badgeColor: Color = DashboardFondoConfig.ColorRojoCarrera,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    esDestacadoLargo: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, label = "bentoScale")

    Surface(
        modifier = modifier
            .scale(scale)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = DashboardFondoConfig.ColorTarjetaClara,
        border = androidx.compose.foundation.BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Marca de agua sutil de fondo para tarjeta destacada
            if (esDestacadoLargo) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = colorIcono.copy(alpha = 0.06f),
                    modifier = Modifier
                        .size(110.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 12.dp, y = 12.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colorFondoIcono),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icono,
                            contentDescription = titulo,
                            tint = colorIcono,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (!badgeTexto.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = badgeTexto,
                                color = badgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column {
                    Text(
                        text = titulo,
                        fontSize = if (esDestacadoLargo) 17.sp else 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardFondoConfig.ColorTextoPrimario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitulo,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = DashboardFondoConfig.ColorTextoSecundario,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Fila de módulo interactiva para el catálogo vertical "Todos los Módulos".
 */
@Composable
fun ModuleListRow(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    colorIcono: Color = DashboardFondoConfig.ColorRojoCarrera,
    badgeCount: Int = 0,
    badgeAlerta: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = DashboardFondoConfig.ColorTarjetaClara,
        border = androidx.compose.foundation.BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorIcono.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icono,
                        contentDescription = titulo,
                        tint = colorIcono,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titulo,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DashboardFondoConfig.ColorTextoPrimario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = descripcion,
                        fontSize = 12.sp,
                        color = DashboardFondoConfig.ColorTextoSecundario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (badgeCount > 0 || badgeAlerta) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DashboardFondoConfig.ColorRojoCarrera
                    ) {
                        Text(
                            text = if (badgeCount > 0) (if (badgeCount > 99) "99+" else "$badgeCount") else "!",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Abrir",
                    tint = DashboardFondoConfig.ColorTextoSecundario.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Título de sección limpio con tipografía táctica.
 */
@Composable
fun SectionHeader(
    titulo: String,
    subtitulo: String? = null,
    accionTexto: String? = null,
    onAccionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = titulo,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = DashboardFondoConfig.ColorTextoPrimario,
                letterSpacing = (-0.2).sp
            )
            if (!subtitulo.isNullOrBlank()) {
                Text(
                    text = subtitulo,
                    fontSize = 12.sp,
                    color = DashboardFondoConfig.ColorTextoSecundario
                )
            }
        }

        if (!accionTexto.isNullOrBlank() && onAccionClick != null) {
            TextButton(
                onClick = onAccionClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = accionTexto,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DashboardFondoConfig.ColorRojoCarrera
                )
            }
        }
    }
}
