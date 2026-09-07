package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.MotoOrangePrimary

/**
 * Diálogo / Pantalla Solemne de Bienvenida y Compromiso de Honor Biker.
 * Se presenta una sola vez a cada piloto tras autenticar su código o registrarse.
 */
@Composable
fun DialogoCompromisoHonorBiker(
    nombrePiloto: String = "Hermano Motero",
    onAceptarCompromiso: () -> Unit
) {
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = { /* No cancelable tocando afuera para exigir aceptación */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.5.dp, Color(0xFFFED7AA)),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Cabecera fija
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF7ED))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(
                                id = com.aistudio.teamtxvzla.R.drawable.logoteam
                            ),
                            contentDescription = "Escudo Team TX",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "PACTO DE HONOR Y BIENVENIDA",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A),
                        letterSpacing = 0.8.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Team Nacional TX • Capítulo Aragua",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MotoOrangePrimary
                    )

                    HorizontalDivider(
                        color = Color(0xFFF1F5F9),
                        thickness = 1.dp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                // Contenido desplazable con los 4 pilares
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Saludo personalizado
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.TwoWheeler,
                                contentDescription = null,
                                tint = MotoOrangePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "¡Saludos, $nombrePiloto! Bienvenido a la hermandad más disciplinada del asfalto.",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    // Pilar 1: Los Colores del Team TX
                    ItemPilarCompromiso(
                        icono = Icons.Default.Security,
                        titulo = "1. RESPETO A LOS COLORES DEL TEAM",
                        descripcion = "Esta aplicación no es solo una herramienta, es una extensión digital de nuestros colores y nuestro parche. Al portarla en tu teléfono, representas la reputación de toda la familia motera nacional. Úsala con dignidad y honor."
                    )

                    // Pilar 2: Valores Morales y Humildad
                    ItemPilarCompromiso(
                        icono = Icons.Default.Handshake,
                        titulo = "2. HONOR, LEALTAD Y 100% HUMILDAD",
                        descripcion = "Nuestra ley de rodada: respeto absoluto hacia tus hermanos y hacia la directiva. Cero prepotencia y 100% humildad. En la ruta nos cuidamos mutuamente; en nuestro convoy ningún piloto se queda atrás jamás."
                    )

                    // Pilar 3: Responsabilidad y Seguridad Vial
                    ItemPilarCompromiso(
                        icono = Icons.Default.HealthAndSafety,
                        titulo = "3. SEGURIDAD Y RESPONSABILIDAD AL RODAR",
                        descripcion = "El buen uso de esta app es bajo tu exclusiva responsabilidad como piloto consciente. Rueda siempre con tu casco reglamentario y protecciones. Por tu seguridad y la de tu copiloto, NUNCA manipules la pantalla mientras tu TX esté en movimiento."
                    )

                    // Pilar 4: Aviso de Consumo de Batería
                    ItemPilarCompromiso(
                        icono = Icons.Default.BatteryChargingFull,
                        titulo = "4. CONSUMO DE BATERÍA Y MÓDULOS TÁCTICOS",
                        descripcion = "Módulos de alta tecnología como el Radar GPS en vivo, el Intercomunicador Mesh de Casco (Wi-Fi/Bluetooth) y el Reproductor de Audio en segundo plano mantienen antenas activas y procesamiento constante. Para rodadas largas, te recomendamos instalar un soporte con cargador USB en tu moto."
                    )
                }

                // Botón de Aceptación Fijo
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Button(
                        onClick = onAceptarCompromiso,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MotoOrangePrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ACEPTO EL COMPROMISO CON HONOR",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemPilarCompromiso(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    descripcion: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = MotoOrangePrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = MotoOrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.5.sp,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = descripcion,
                    fontSize = 11.5.sp,
                    color = Color(0xFF475569),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
