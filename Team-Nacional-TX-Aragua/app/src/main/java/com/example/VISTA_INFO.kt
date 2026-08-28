package com.example

import com.aistudio.teamtxvzla.BuildConfig
import com.aistudio.teamtxvzla.R
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun VistaInfoScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var buscando by remember { mutableStateOf(false) }
    
    // Estado para el cuadro de diálogo de actualización manual
    var infoOta by remember { mutableStateOf<GestorActualizaciones.InformacionOta?>(null) }
    var mostrarDialogo by remember { mutableStateOf(false) }
    var showDonationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Logo de la aplicación
        Image(
            painter = painterResource(id = R.drawable.logoteam),
            contentDescription = "Logo Team TX",
            modifier = Modifier.size(110.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Team Nacional TX Aragua",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = MotoOrangePrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = MotoOrangePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Información de Versión", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Versión Instalada: ${BuildConfig.VERSION_NAME}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Código de Compilación: ${BuildConfig.VERSION_CODE}", fontSize = 13.sp, color = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                buscando = true
                coroutineScope.launch {
                    val ota = GestorActualizaciones.verificarActualizacion()
                    buscando = false
                    if (ota != null) {
                        if (ota.versionCode > BuildConfig.VERSION_CODE) {
                            infoOta = ota
                            mostrarDialogo = true
                        } else {
                            Toast.makeText(context, "¡Ya tienes la última versión instalada!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Error al buscar actualizaciones o no hay conexión.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = !buscando,
            colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(0.9f).height(48.dp)
        ) {
            if (buscando) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Buscando...", fontSize = 14.sp)
            } else {
                Icon(Icons.Default.Update, contentDescription = "Update")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buscar Actualizaciones", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sección de Donar al Desarrollador
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2230)),
            border = BorderStroke(1.dp, TxGoldBrass.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = TxFlameRed, modifier = Modifier.size(20.dp))
                    Text(
                        text = "APOYO AL DESARROLLO",
                        fontWeight = FontWeight.Black,
                        color = TxGoldLight,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "La infraestructura y servidores en la nube requieren mantenimiento continuo. Puedes contribuir voluntariamente para mantener el sistema 100% activo.",
                    color = TxSteelSilver,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Button(
                    onClick = { showDonationDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                    border = BorderStroke(1.dp, TxGoldBrass),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = TxFlameRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Donar al Desarrollador", fontWeight = FontWeight.Bold, color = TxGoldLight, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }

    // Diálogo de Donación al Desarrollador
    if (showDonationDialog) {
        InfoDeveloperDonationDialog(onDismiss = { showDonationDialog = false })
    }

    // Diálogo si se encuentra una actualización manualmente
    if (mostrarDialogo && infoOta != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = {
                Text("¡Nueva Versión Encontrada!", fontWeight = FontWeight.Bold, color = MotoOrangePrimary)
            },
            text = {
                Column {
                    Text("Hay una nueva versión disponible para descargar.")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (infoOta!!.notas.isNotBlank()) {
                        Text("Novedades:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(infoOta!!.notas, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogo = false
                        GestorActualizaciones.descargarEInstalarApk(context, infoOta!!.urlDescarga)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
                ) {
                    Text("Descargar ahora")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { mostrarDialogo = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun InfoDeveloperDonationDialog(
    onDismiss: () -> Unit
) {
    var userMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B2230),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Code, contentDescription = null, tint = TxGoldBrass)
                Text("Desarrollo de la App", fontWeight = FontWeight.Black, color = Color.White)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Este proyecto fue realizado por Eduardo Márquez, integrante del Team Nacional TX Aragua. Agradezco el buen uso de la app con los amores bikers.",
                    fontSize = 13.sp,
                    color = Color.White
                )
                Text(
                    text = "Es un trabajo de meses de programación y cuenta con un servidor en la nube de Google que necesita mantenimiento.",
                    fontSize = 13.sp,
                    color = Color.White
                )
                Text(
                    text = "Se puede gestionar cualquier donación voluntaria para mantenerlo vivo y poder seguir contribuyendo al team y al desarrollo de la aplicación.",
                    fontSize = 12.sp,
                    color = Color(0xFFB0BEC5)
                )

                HorizontalDivider(color = TxGoldSecondary.copy(alpha = 0.3f))

                Text("DATOS DE PAGO MÓVIL", fontWeight = FontWeight.Bold, color = TxGoldBrass, fontSize = 12.sp)
                
                Surface(
                    color = Color(0xFF141822),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.DarkGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Eduardo Márquez", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Banco Bancamiga", color = Color.White, fontSize = 13.sp)
                        Text("Cédula: 19554402", color = Color.White, fontSize = 13.sp)
                        Text("Teléfono: 04243769999", color = Color.White, fontSize = 13.sp)
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        OutlinedButton(
                            onClick = {
                                val clip = ClipData.newPlainText(
                                    "Pago Movil",
                                    "Eduardo Márquez\nBanco Bancamiga\nCédula: 19554402\nTeléfono: 04243769999"
                                )
                                clipboardManager?.setPrimaryClip(clip)
                                Toast.makeText(context, "Datos de Pago Móvil copiados al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TxGoldBrass),
                            border = BorderStroke(1.dp, TxGoldBrass)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copiar Datos", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("ENVIAR REFERENCIA Y MENSAJE", fontWeight = FontWeight.Bold, color = TxFlameRed, fontSize = 12.sp)

                OutlinedTextField(
                    value = userMessage,
                    onValueChange = { userMessage = it },
                    placeholder = { Text("Escribe tu mensaje o referencia...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TxGoldBrass,
                        focusedLabelColor = TxGoldBrass
                    )
                )
                
                Text(
                    text = "Honor, lealtad y respeto.",
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = TxGoldSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val encodedMsg = Uri.encode(userMessage.ifBlank { "Hola Eduardo, te escribo desde la app Team TX" })
                    val uri = Uri.parse("https://wa.me/584243769999?text=$encodedMsg")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TxFlameRed)
            ) {
                Text("Enviar WhatsApp", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color.White)
            }
        }
    )
}
