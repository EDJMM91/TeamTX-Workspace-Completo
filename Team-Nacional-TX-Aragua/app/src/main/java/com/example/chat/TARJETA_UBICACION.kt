package com.example.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mapa.PuenteMapa

/**
 * ARCHIVO: TARJETA_UBICACION.kt
 * 
 * Módulo visual independiente para mostrar la burbuja de ubicación en el historial del chat.
 * Solo al tocarla se despierta el motor nativo del mapa; no procesa mapas pesados por sí misma.
 */
@Composable
fun TarjetaUbicacion(
    coordenadasString: String,
    esRemitentePropio: Boolean,
    nombrePiloto: String = "Piloto TX",
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val colorBurbuja = if (esRemitentePropio) Color(0xFF1E88E5) else Color(0xFF263238)
    val colorTexto = Color.White

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .fillMaxWidth(),
        contentAlignment = if (esRemitentePropio) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colorBurbuja,
            shadowElevation = 2.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                    PuenteMapa.mostrarUbicacionEnMapa(
                        contexto = contexto,
                        coordenadas = coordenadasString,
                        tituloEtiqueta = nombrePiloto
                    )
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEF5350).copy(alpha = 0.18f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Pin de Ubicación",
                            tint = Color(0xFFEF5350), // Marcador rojo vivo
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Ubicación en Tiempo Real",
                            color = colorTexto,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = "Piloto: $nombrePiloto",
                        color = colorTexto.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "📍 $coordenadasString • Toca para abrir Mapa TX",
                        color = colorTexto.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
