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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ui.screens.GuiaUsoAppDialog
import com.example.data.model.MemberProfile
import android.util.Log
import kotlinx.coroutines.launch

private const val TAG_LOGCAT = "TEAM_TX_INFO"

@Composable
fun VistaInfoScreen(
    currentMember: MemberProfile? = null,
    allMembers: List<MemberProfile> = emptyList(),
    viewModel: com.example.ui.viewmodel.TeamTxViewModel? = null,
    onNavigateToAvisos: () -> Unit = {},
    onOpenPrivateChatWithDeveloper: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var buscando by remember { mutableStateOf(false) }
    
    // Estado para el cuadro de diálogo de actualización manual
    var infoOta by remember { mutableStateOf<GestorActualizaciones.InformacionOta?>(null) }
    var mostrarDialogo by remember { mutableStateOf(false) }
    var mostrarDialogoDescarga by remember { mutableStateOf(false) }
    var showDonationDialog by remember { mutableStateOf(false) }
    var showUserGuideDialog by remember { mutableStateOf(false) }
    var showHelpImproveDialog by remember { mutableStateOf(false) }
    var showStorageVersionsDialog by remember { mutableStateOf(false) }

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
        
        Spacer(modifier = Modifier.height(16.dp))

        // 📢 Tarjeta de Sincronización con Muro de Avisos
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Campaign, contentDescription = "Avisos", tint = MotoOrangePrimary, modifier = Modifier.size(18.dp))
                        Text("Sincronización con Avisos", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                    }
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("CONECTADO", color = Color(0xFF15803D), fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Text(
                    text = "Las notas de cada actualización oficial se publican en el Muro de Avisos con su lista de novedades y mejoras para toda la hermandad.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 15.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToAvisos,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF475569))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver Muro", fontSize = 11.sp, color = Color(0xFF334155), fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val ota = GestorActualizaciones.verificarActualizacion()
                                if (ota != null) {
                                    viewModel?.sincronizarAvisoActualizacionOta(ota, forzar = true)
                                    Toast.makeText(context, "📢 Aviso de versión ${ota.versionName} publicado en el Muro de Avisos", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No se pudo obtener información de la versión", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Publicar Aviso", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                            // Sincronización automática con Avisos
                            viewModel?.sincronizarAvisoActualizacionOta(ota)
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

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = { showStorageVersionsDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.7f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(0.9f).height(44.dp)
        ) {
            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF38BDF8))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Historial de Versiones y Rollback", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
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

        Spacer(modifier = Modifier.height(16.dp))

        // Sección: Ayúdanos a Mejorar (Mensaje al Desarrollador Eduardo Márquez)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
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
                    Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    Text(
                        text = "AYÚDANOS A MEJORAR LA APP",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "¿Tienes ideas, sugerencias o propuestas de mejora para la app? Envía tus observaciones directamente al desarrollador Eduardo Márquez vía WhatsApp o por Chat Privado.",
                    color = TxSteelSilver,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Button(
                    onClick = { showHelpImproveDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Feedback, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar Sugerencia / Mejora", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sección de Guía de Uso de la App (Para todo público)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF162032)),
            border = BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.5f)),
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
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(20.dp))
                    Text(
                        text = "MANUAL Y GUÍA DE USO",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "Aprende a usar paso a paso todas las funciones de la app: Muro, Chat, Rodadas, SOS Vial, Mapa y más sin términos complicados.",
                    color = TxSteelSilver,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Button(
                    onClick = { showUserGuideDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.AutoStories, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir Guía Interactiva", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }

    // Diálogo de Guía de Uso de la App
    if (showUserGuideDialog) {
        GuiaUsoAppDialog(onDismiss = { showUserGuideDialog = false })
    }

    // Diálogo de Donación al Desarrollador
    if (showDonationDialog) {
        InfoDeveloperDonationDialog(onDismiss = { showDonationDialog = false })
    }

    // Diálogo de Ayúdanos a Mejorar
    if (showHelpImproveDialog) {
        HelpImproveAppDialog(
            onDismiss = { showHelpImproveDialog = false },
            onOpenPrivateChat = onOpenPrivateChatWithDeveloper
        )
    }

    // Diálogo de Historial de Versiones en Storage y Rollback
    if (showStorageVersionsDialog) {
        StorageVersionsHistoryDialog(
            onDismiss = { showStorageVersionsDialog = false },
            onDownloadOta = { otaInfo ->
                infoOta = otaInfo
                mostrarDialogoDescarga = true
            }
        )
    }

    // Diálogo si se encuentra una actualización manualmente
    if (mostrarDialogo && infoOta != null) {
        val ota = infoOta!!
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Update, contentDescription = null, tint = MotoOrangePrimary)
                    Text("¡Versión ${ota.versionName} Disponible!", fontWeight = FontWeight.Bold, color = MotoOrangePrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (ota.titulo.isNotBlank()) {
                        Text(ota.titulo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                    Text(ota.notas, fontSize = 11.sp, color = Color(0xFFCBD5E1))

                    if (ota.novedades.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF131722),
                            border = BorderStroke(1.dp, Color(0xFF263238)),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)
                        ) {
                            LazyColumn(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                item {
                                    Text("✨ ¿Qué incluye esta versión?", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = MotoGoldSecondary)
                                }
                                items(ota.novedades) { nov ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Top) {
                                        Text("•", fontSize = 11.sp, color = MotoOrangePrimary, fontWeight = FontWeight.Bold)
                                        Text(nov, fontSize = 10.sp, color = Color.White, lineHeight = 13.sp)
                                    }
                                }
                                if (ota.correcciones.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("🛠️ Correcciones:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF38BDF8))
                                    }
                                    items(ota.correcciones) { corr ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Top) {
                                            Text("✓", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                            Text(corr, fontSize = 10.sp, color = Color(0xFFCBD5E1), lineHeight = 13.sp)
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
                    onClick = {
                        mostrarDialogo = false
                        mostrarDialogoDescarga = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Descargar e Instalar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        mostrarDialogo = false
                        GestorActualizaciones.abrirDescargaEnNavegador(context, infoOta!!.urlDescarga)
                    }
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Navegador")
                }
            }
        )
    }

    if (mostrarDialogoDescarga && infoOta != null) {
        DialogoProgresoDescargaOta(
            infoOta = infoOta!!,
            onDismiss = { mostrarDialogoDescarga = false }
        )
    }
}

/**
 * Diálogo para enviar sugerencias y mejoras directamente al Desarrollador
 */
@Composable
fun HelpImproveAppDialog(
    onDismiss: () -> Unit,
    onOpenPrivateChat: () -> Unit
) {
    var suggestionText by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B2230),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFF38BDF8))
                Text("Ayúdanos a Mejorar", fontWeight = FontWeight.Black, color = Color.White)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tu opinión como miembro de la hermandad biker es vital para evolucionar la app. Comparte tus sugerencias, funciones deseadas o propuestas de optimización:",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = suggestionText,
                    onValueChange = { suggestionText = it },
                    placeholder = { Text("Escribe tu propuesta o sugerencia para Eduardo Márquez...", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp),
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        focusedLabelColor = Color(0xFF38BDF8)
                    )
                )

                Text(
                    text = "Enviar propuesta al desarrollador Eduardo (+584243769999):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TxGoldSecondary
                )

                // Botón 1: WhatsApp
                Button(
                    onClick = {
                        val fullMessage = if (suggestionText.isBlank()) {
                            "Hola Eduardo Márquez, te escribo desde la app Team TX para compartirte una sugerencia de mejora."
                        } else {
                            "Hola Eduardo Márquez, te escribo desde la app Team TX con la siguiente sugerencia de mejora:\n\n$suggestionText"
                        }
                        val encodedMsg = Uri.encode(fullMessage)
                        val uri = Uri.parse("https://wa.me/584243769999?text=$encodedMsg")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            context.startActivity(intent)
                            Log.d(TAG_LOGCAT, "📤 Sugerencia enviada vía WhatsApp a Eduardo Márquez")
                            onDismiss()
                        } catch (e: Exception) {
                            Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar por WhatsApp", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                }

                // Botón 2: Chat Privado en la App
                OutlinedButton(
                    onClick = {
                        Log.d(TAG_LOGCAT, "💬 Abriendo chat privado con desarrollador Eduardo Márquez")
                        onDismiss()
                        onOpenPrivateChat()
                    },
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Forum, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chat Privado en la App", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color.White)
            }
        }
    )
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
                    text = "Este proyecto fue realizado por Eduardo Márquez, integrante del Team Nacional TX Aragua. Cuidamos nuestros colores bikers, esta app es parte de ellos. La finalidad es fomentar la hermandad y unión de esta gran familia biker.",
                    fontSize = 13.sp,
                    color = Color.White,
                    lineHeight = 18.sp
                )
                Text(
                    text = "Es un trabajo de meses de programación y cuenta con un servidor en la nube de Google que necesita mantenimiento continuo.",
                    fontSize = 13.sp,
                    color = Color.White,
                    lineHeight = 18.sp
                )
                Text(
                    text = "Se puede gestionar cualquier donación voluntaria para mantenerlo activo y poder seguir contribuyendo al team y al desarrollo de la aplicación.",
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
                                    "0172    04243769999  19554402"
                                )
                                clipboardManager?.setPrimaryClip(clip)
                                Toast.makeText(context, "Datos de Pago Móvil copiados (0172    04243769999  19554402)", Toast.LENGTH_SHORT).show()
                                Log.d(TAG_LOGCAT, "📋 Datos exactos de Pago Móvil copiados: 0172    04243769999  19554402")
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

/**
 * Diálogo interactivo para ver todas las versiones subidas en Firebase Storage (updates/)
 * Compara la versión instalada contra cada APK disponible y determina si hace falta actualizar o permite rollback.
 */
@Composable
fun StorageVersionsHistoryDialog(
    onDismiss: () -> Unit,
    onDownloadOta: (GestorActualizaciones.InformacionOta) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var cargando by remember { mutableStateOf(true) }
    var versionsList by remember { mutableStateOf<List<GestorActualizaciones.StorageApkVersion>>(emptyList()) }

    fun compararVersiones(versionString: String, versionActual: String): Int {
        val nums1 = Regex("""\d+(\.\d+)*""").find(versionString)?.value?.split(".")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        val nums2 = Regex("""\d+(\.\d+)*""").find(versionActual)?.value?.split(".")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        val maxLen = maxOf(nums1.size, nums2.size)
        for (i in 0 until maxLen) {
            val p1 = nums1.getOrElse(i) { 0 }
            val p2 = nums2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    fun recargar() {
        cargando = true
        coroutineScope.launch {
            versionsList = GestorActualizaciones.obtenerListaVersionesStorage()
            cargando = false
        }
    }

    LaunchedEffect(Unit) {
        recargar()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161B26),
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MotoOrangePrimary)
                    Text("CONTROL DE VERSIONES", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                }
                IconButton(onClick = { recargar() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Recargar", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                val installedVersion = BuildConfig.VERSION_NAME
                val latestApk = versionsList.firstOrNull { it.isRecommendedLatest } ?: versionsList.firstOrNull()
                val hayNuevaActualizacion = latestApk != null && (
                    compararVersiones(latestApk.versionName, installedVersion) > 0 ||
                    (latestApk.isRecommendedLatest && !latestApk.fileName.contains("v$installedVersion"))
                )

                // ══════════════════════════════════════════════════════════
                // TARJETA DE AUDITORÍA Y COMPARACIÓN DE VERSIÓN
                // ══════════════════════════════════════════════════════════
                Surface(
                    color = if (hayNuevaActualizacion) Color(0xFF2E2210) else Color(0xFF0F291E),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (hayNuevaActualizacion) MotoGoldSecondary else Color(0xFF22C55E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                if (hayNuevaActualizacion) Icons.Default.NewReleases else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (hayNuevaActualizacion) MotoGoldSecondary else Color(0xFF22C55E),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (hayNuevaActualizacion) "¡ACTUALIZACIÓN DISPONIBLE!" else "APLICACIÓN AL DÍA",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = if (hayNuevaActualizacion) MotoGoldSecondary else Color(0xFF22C55E)
                            )
                        }

                        Text(
                            text = "Versión instalada: v$installedVersion (Build ${BuildConfig.VERSION_CODE})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = if (hayNuevaActualizacion) {
                                "Hay una versión más reciente disponible en Storage. Se recomienda actualizar para disfrutar de las últimas mejoras y correcciones."
                            } else {
                                "Tienes la última versión oficial instalada. Puedes utilizar este panel para hacer rollback a versiones anteriores en caso de alguna contingencia."
                            },
                            fontSize = 10.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (cargando) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MotoOrangePrimary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Consultando versiones en Firebase Storage...", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else if (versionsList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No se encontraron archivos APK en Storage", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(versionsList) { apk ->
                            val comp = compararVersiones(apk.versionName, installedVersion)
                            val isCurrent = apk.fileName.contains("v$installedVersion") || (apk.isRecommendedLatest && !hayNuevaActualizacion)
                            val isNewer = !isCurrent && (comp > 0 || (apk.isRecommendedLatest && hayNuevaActualizacion))
                            val isOlder = !isCurrent && !isNewer

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isCurrent -> Color(0xFF132838)
                                    isNewer -> Color(0xFF1C2C20)
                                    else -> Color(0xFF1E2330)
                                },
                                border = BorderStroke(
                                    if (isCurrent || isNewer) 1.5.dp else 1.dp,
                                    when {
                                        isCurrent -> Color(0xFF38BDF8)
                                        isNewer -> Color(0xFF22C55E)
                                        else -> Color(0xFF2E384D)
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = apk.fileName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )

                                        // Badges de estado de versión
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (isCurrent) {
                                                Surface(
                                                    color = Color(0xFF38BDF8).copy(alpha = 0.25f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                                                ) {
                                                    Text(
                                                        text = "INSTALADA",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF38BDF8),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else if (isNewer) {
                                                Surface(
                                                    color = Color(0xFF22C55E).copy(alpha = 0.25f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.dp, Color(0xFF22C55E))
                                                ) {
                                                    Text(
                                                        text = "NUEVA",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF22C55E),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else if (isOlder) {
                                                Surface(
                                                    color = MotoGoldSecondary.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.dp, MotoGoldSecondary)
                                                ) {
                                                    Text(
                                                        text = "ROLLBACK",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = MotoGoldSecondary,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            // Badge Beta / Estable
                                            if (apk.isBeta) {
                                                Surface(
                                                    color = Color(0xFFF97316).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.dp, Color(0xFFF97316))
                                                ) {
                                                    Text(
                                                        text = "BETA",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFFF97316),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else {
                                                Surface(
                                                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.dp, Color(0xFF10B981))
                                                ) {
                                                    Text(
                                                        text = "ESTABLE",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF10B981),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Título de la versión y notas específicas
                                    if (apk.titulo.isNotBlank()) {
                                        Text(
                                            text = apk.titulo,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MotoGoldSecondary
                                        )
                                    }

                                    if (apk.notas.isNotBlank()) {
                                        Text(
                                            text = apk.notas,
                                            fontSize = 10.sp,
                                            color = Color(0xFFCBD5E1),
                                            lineHeight = 13.sp
                                        )
                                    }

                                    // Sección expandible de novedades detalladas
                                    if (apk.novedades.isNotEmpty()) {
                                        var mostrarDetalles by remember { mutableStateOf(false) }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { mostrarDetalles = !mostrarDetalles }
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (mostrarDetalles) "Ocultar cambios ▲" else "Ver cambios y novedades (${apk.novedades.size}) ▼",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MotoOrangePrimary
                                            )
                                        }

                                        if (mostrarDetalles) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF131722),
                                                border = BorderStroke(1.dp, Color(0xFF263238)),
                                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    apk.novedades.forEach { nov ->
                                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Top) {
                                                            Text("•", fontSize = 10.sp, color = MotoOrangePrimary, fontWeight = FontWeight.Bold)
                                                            Text(nov, fontSize = 9.5.sp, color = Color.White, lineHeight = 13.sp)
                                                        }
                                                    }
                                                    if (apk.correcciones.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text("Correcciones:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                                        apk.correcciones.forEach { corr ->
                                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Top) {
                                                                Text("✓", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                                                Text(corr, fontSize = 9.5.sp, color = Color(0xFFCBD5E1), lineHeight = 13.sp)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "📅 ${apk.formattedDate}", fontSize = 10.sp, color = TxSteelSilver)
                                        Text(text = "•", fontSize = 10.sp, color = Color.Gray)
                                        Text(text = "💾 ${apk.formattedSize}", fontSize = 10.sp, color = TxSteelSilver)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val btnText = when {
                                            isCurrent -> "Reinstalar Actual"
                                            isNewer -> "Actualizar Ahora"
                                            else -> "Instalar Rollback"
                                        }
                                        val btnColor = when {
                                            isCurrent -> Color(0xFF0284C7)
                                            isNewer -> Color(0xFF16A34A)
                                            else -> MotoOrangePrimary
                                        }

                                        Button(
                                            onClick = {
                                                onDismiss()
                                                onDownloadOta(
                                                    GestorActualizaciones.InformacionOta(
                                                        versionCode = apk.versionCode,
                                                        versionName = apk.versionName,
                                                        titulo = apk.titulo,
                                                        urlDescarga = apk.downloadUrl,
                                                        notas = apk.notas,
                                                        novedades = apk.novedades,
                                                        correcciones = apk.correcciones,
                                                        fechaPublicacion = apk.formattedDate
                                                    )
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.weight(1f).height(32.dp)
                                        ) {
                                            Icon(
                                                if (isNewer) Icons.Default.Upgrade else Icons.Default.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(btnText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                GestorActualizaciones.abrirDescargaEnNavegador(context, apk.downloadUrl)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Navegador", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color.White)
            }
        }
    )
}

