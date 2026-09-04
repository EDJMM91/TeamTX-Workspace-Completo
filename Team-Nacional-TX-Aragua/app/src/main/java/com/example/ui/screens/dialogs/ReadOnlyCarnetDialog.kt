package com.example.ui.screens.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.dashboard.DashboardFondoConfig
import com.example.data.model.MemberProfile
import com.example.ui.components.DigitalCredentialCard
import com.example.ui.theme.*

/**
 * Diálogo de Carnet TX y Ficha de Piloto en Modo Solo Lectura.
 * Permite a cualquier miembro consultar la información oficial de otro piloto
 * sin alterar la sesión del usuario ni suplantar su identidad.
 */
@Composable
fun ReadOnlyCarnetDialog(
    member: MemberProfile,
    currentLoggedInMemberId: Long = 0L,
    onDismiss: () -> Unit,
    onRateMember: ((MemberProfile) -> Unit)? = null
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DashboardFondoConfig.ColorTarjetaClara,
            border = BorderStroke(1.5.dp, DashboardFondoConfig.ColorBordeClaro),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header: Título + Badge de solo lectura + Cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = DashboardFondoConfig.ColorDoradoOro,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Carnet TX Oficial",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = DashboardFondoConfig.ColorTextoPrimario
                            )
                            Text(
                                text = "🔒 Modo Solo Lectura • Identidad Verificada",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusSuccess
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = DashboardFondoConfig.ColorTextoSecundario
                        )
                    }
                }

                HorizontalDivider(
                    color = DashboardFondoConfig.ColorBordeClaro,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // Contenido Scrolleable: Carnet TX + Detalles del Piloto
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Carnet Digital TX
                    item {
                        DigitalCredentialCard(
                            member = member,
                            currentLoggedInMemberId = currentLoggedInMemberId,
                            onRateMember = { onRateMember?.invoke(it) },
                            isLightTheme = true
                        )
                    }

                    // Foto de la Moto si existe
                    if (!member.bikePhotoUri.isNullOrBlank()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                            ) {
                                AsyncImage(
                                    model = member.bikePhotoUri,
                                    contentDescription = "Moto de ${member.fullName}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Ficha Técnica de la Moto
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorFondoClaro),
                            border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "DATOS DE LA MOTO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DashboardFondoConfig.ColorRojoCarrera
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Modelo:", fontSize = 12.sp, color = DashboardFondoConfig.ColorTextoSecundario)
                                    Text("${member.bikeBrand} ${member.bikeModel}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DashboardFondoConfig.ColorTextoPrimario)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Placa Oficial:", fontSize = 12.sp, color = DashboardFondoConfig.ColorTextoSecundario)
                                    Text(member.bikePlate.ifBlank { "Sin placa registrada" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DashboardFondoConfig.ColorTextoPrimario)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Color / Cilindrada:", fontSize = 12.sp, color = DashboardFondoConfig.ColorTextoSecundario)
                                    Text("${member.bikeColor} • ${member.bikeDisplacementCc}cc", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DashboardFondoConfig.ColorTextoPrimario)
                                }
                            }
                        }
                    }

                    // Asistencia SOS y Contacto
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DashboardFondoConfig.ColorFondoClaro),
                            border = BorderStroke(1.dp, DashboardFondoConfig.ColorBordeClaro),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "CONTACTO DE ASISTENCIA Y RUTA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DashboardFondoConfig.ColorDoradoOro
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Teléfono Piloto:", fontSize = 11.sp, color = DashboardFondoConfig.ColorTextoSecundario)
                                        Text(member.phone.ifBlank { "No disponible" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DashboardFondoConfig.ColorTextoPrimario)
                                    }
                                    if (member.phone.isNotBlank()) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${member.phone}"))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "No se pudo abrir el marcador", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Default.Phone, contentDescription = "Llamar", tint = StatusSuccess)
                                            }
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        val cleanPhone = member.phone.replace(Regex("[^0-9]"), "")
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanPhone"))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                                            }
                                        }
                                    }
                                }

                                if (member.emergencyContactName.isNotBlank() || member.emergencyContactPhone.isNotBlank()) {
                                    HorizontalDivider(color = DashboardFondoConfig.ColorBordeClaro)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Contacto SOS:", fontSize = 11.sp, color = DashboardFondoConfig.ColorTextoSecundario)
                                            Text("${member.emergencyContactName} (${member.emergencyContactRelation})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DashboardFondoConfig.ColorTextoPrimario)
                                            Text(member.emergencyContactPhone, fontSize = 11.sp, color = DashboardFondoConfig.ColorTextoSecundario)
                                        }
                                        if (member.emergencyContactPhone.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${member.emergencyContactPhone}"))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "No se pudo abrir el marcador", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Default.Call, contentDescription = "Llamar SOS", tint = StatusError)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botón Inferior: Cerrar Ficha
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DashboardFondoConfig.ColorRojoCarrera),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Cerrar Ficha de Piloto", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
