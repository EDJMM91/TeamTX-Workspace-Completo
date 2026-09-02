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
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
        MemberRole.PRESIDENTE, MemberRole.VICEPRESIDENTE, MemberRole.DIRECTIVA -> DashboardFondoConfig.ColorDoradoOro
        MemberRole.CAPITAN_RUTA, MemberRole.DISCIPLINARIO -> DashboardFondoConfig.ColorRojoCarrera
        MemberRole.MECANICO_OFICIAL, MemberRole.SECRETARIO, MemberRole.SEGURIDAD_VIAL -> Color(0xFF1E88E5)
        else -> DashboardFondoConfig.ColorRojoCarrera
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenCarnet),
        shape = RoundedCornerShape(16.dp),
        color = DashboardFondoConfig.ColorTarjetaClara,
        border = androidx.compose.foundation.BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
