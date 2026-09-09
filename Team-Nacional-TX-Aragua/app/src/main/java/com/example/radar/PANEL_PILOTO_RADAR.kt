package com.example.radar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MemberProfile
import java.util.concurrent.TimeUnit

@Composable
fun PanelPilotoRadar(
    pilotos: List<PilotoRadar>,
    perfilSeleccionado: MemberProfile? = null,
    onCerrar: () -> Unit,
    onCerrarPerfil: () -> Unit = {},
    onPilotoSeleccionado: (PilotoRadar) -> Unit,
    onAccion: (String, MemberProfile) -> Unit = { _, _ -> }
) {
    val ahora = System.currentTimeMillis()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(width = 40.dp, height = 4.dp),
                color = Color.Gray,
                shape = RoundedCornerShape(2.dp)
            ) {}
        }

        if (perfilSeleccionado != null) {
            CarnetTxDetallado(
                perfil = perfilSeleccionado,
                onCerrar = onCerrarPerfil,
                onAccion = onAccion
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Radar Táctico",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${pilotos.size} piloto(s) en línea",
                        color = Color(0xFF808080),
                        fontSize = 13.sp
                    )
                }
                TextButton(onClick = onCerrar) {
                    Text("Cerrar", color = Color(0xFFE53935))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (pilotos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay pilotos en línea",
                        color = Color(0xFF606060),
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pilotos) { piloto ->
                        PilotoRadarItem(
                            piloto = piloto,
                            ahora = ahora,
                            onClick = { onPilotoSeleccionado(piloto) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CarnetTxDetallado(
    perfil: MemberProfile,
    onCerrar: () -> Unit,
    onAccion: (String, MemberProfile) -> Unit
) {
    val colorRango = when {
        perfil.role.displayName.contains("Capitán", true) -> Color(0xFFE53935)
        perfil.role.displayName.contains("Mecánico", true) -> Color(0xFF1E88E5)
        perfil.role.displayName.contains("Directiva", true) -> Color(0xFFFDD835)
        perfil.role.displayName.contains("Vocal", true) -> Color(0xFF43A047)
        else -> Color(0xFFFF9800)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Carnet TX",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onCerrar) {
                Text("Volver", color = Color(0xFFE53935))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!perfil.profilePhotoUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(com.example.util.SanitizadorImagenUrl.obtenerUrlEfectiva(perfil.profilePhotoUri) ?: "")
                    .crossfade(true)
                    .build(),
                contentDescription = perfil.fullName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(colorRango),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = perfil.fullName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = perfil.fullName,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        if (perfil.nickname.isNotBlank()) {
            Text(
                text = perfil.nickname,
                color = Color(0xFF808080),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            color = colorRango.copy(alpha = 0.2f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = perfil.role.displayName,
                color = colorRango,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        if (perfil.memberNumber.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "N° ${perfil.memberNumber}",
                color = Color(0xFF606060),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF252540), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            if (perfil.bikeBrand.isNotBlank() || perfil.bikeModel.isNotBlank()) {
                Text(text = "Moto", color = Color(0xFF808080), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "${perfil.bikeBrand} ${perfil.bikeModel}", color = Color.White, fontSize = 14.sp)
                if (perfil.bikeColor.isNotBlank()) {
                    Text(text = perfil.bikeColor, color = Color(0xFF606060), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (perfil.bikePlate.isNotBlank()) {
                Text(text = "Placa", color = Color(0xFF808080), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(text = perfil.bikePlate, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccionButton(
                modifier = Modifier.weight(1f),
                icono = Icons.Default.Person,
                label = "Info",
                color = Color(0xFF1E88E5),
                onClick = { onAccion("info", perfil) }
            )
            AccionButton(
                modifier = Modifier.weight(1f),
                icono = Icons.Default.Chat,
                label = "Saludo",
                color = Color(0xFF43A047),
                onClick = { onAccion("saludo", perfil) }
            )
            AccionButton(
                modifier = Modifier.weight(1f),
                icono = Icons.Default.Warning,
                label = "Auxilio",
                color = Color(0xFFE53935),
                onClick = { onAccion("auxilio", perfil) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccionButton(
                modifier = Modifier.weight(1f),
                icono = Icons.Default.Badge,
                label = "Carnet TX",
                color = Color(0xFFFF9800),
                onClick = { onAccion("carnet", perfil) }
            )
            AccionButton(
                modifier = Modifier.weight(1f),
                icono = Icons.Default.Navigation,
                label = "Navegar",
                color = Color(0xFF7C4DFF),
                onClick = { onAccion("navegar", perfil) }
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AccionButton(
    modifier: Modifier = Modifier,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icono,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PilotoRadarItem(
    piloto: PilotoRadar,
    ahora: Long,
    onClick: () -> Unit
) {
    val minutos = TimeUnit.MILLISECONDS.toMinutes(ahora - piloto.timestamp)
    val tiempoAgo = when {
        minutos < 1 -> "ahora"
        minutos < 60 -> "hace ${minutos}m"
        minutos < 1440 -> "hace ${minutos / 60}h"
        else -> "hace ${minutos / 1440}d"
    }

    val colorRango = when {
        piloto.rango.contains("Capitán", true) -> Color(0xFFE53935)
        piloto.rango.contains("Mecánico", true) -> Color(0xFF1E88E5)
        piloto.rango.contains("Directiva", true) -> Color(0xFFFDD835)
        piloto.rango.contains("Vocal", true) -> Color(0xFF43A047)
        else -> Color(0xFFFF9800)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF252540))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (piloto.avatarUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(piloto.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = piloto.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colorRango),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = piloto.nombre.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = piloto.nombre,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = piloto.rango, color = colorRango, fontSize = 12.sp)
                Text(text = " · ", color = Color(0xFF606060), fontSize = 12.sp)
                Text(text = tiempoAgo, color = Color(0xFF808080), fontSize = 12.sp)
            }
        }

        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (piloto.activo) Color(0xFF4CAF50) else Color(0xFF808080))
        )
    }
}
