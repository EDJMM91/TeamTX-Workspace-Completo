package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.VenezuelanFlagRibbon
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * VISTA_LOGIN — Pantalla de login (Solo Código).
 * Tema Claro de Alto Contraste sincronizado con el Dashboard.
 * Flujo: código de invitación / maestro → acceso directo al sistema.
 * Soporta verificación y recuperación con correo sincronizado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaLogin(
    onRequestCodeLogin: suspend (code: String) -> Pair<Boolean, String>,
    onSubmitAccessRequest: (name: String, phone: String, dni: String, brand: String, model: String, color: String, plate: String, chapter: String, reason: String, birthDate: String, role: String) -> Boolean,
    onSolicitarCodigoPorCorreo: suspend (email: String) -> Pair<Boolean, String> = { Pair(false, "") },
    attemptsLeft: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var inviteCode by remember { mutableStateOf("") }
    var showCode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var showIconMenuDialog by remember { mutableStateOf(false) }
    var showRequestCodeDialog by remember { mutableStateOf(false) }
    var showEmailRecoveryDialog by remember { mutableStateOf(false) }

    fun ejecutarLogin() {
        val codigoLimpio = inviteCode.trim()
        if (codigoLimpio.isBlank()) {
            errorMessage = "Debes ingresar un código de invitación o credencial válida."
            return
        }
        isLoading = true
        errorMessage = null
        focusManager.clearFocus()
        coroutineScope.launch {
            val (exito, mensaje) = onRequestCodeLogin(codigoLimpio)
            isLoading = false
            if (!exito) {
                errorMessage = mensaje
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC) // Tema claro consistente con el Dashboard
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VenezuelanFlagRibbon()

            Spacer(modifier = Modifier.height(24.dp))

            // --- LOGO INTERACTIVO ---
            Box(
                modifier = Modifier
                    .size(95.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { showIconMenuDialog = true }
                    .padding(6.dp)
                    .testTag("app_logo_interactive_button"),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(
                        id = com.aistudio.teamtxvzla.R.drawable.logoteam
                    ),
                    contentDescription = "Team TX Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chip interactivo "Toca el logo para abrir el menú de acceso"
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFF7ED),
                border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                modifier = Modifier.clickable { showIconMenuDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MotoOrangePrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Toca el logo para abrir el menú de acceso",
                        color = Color(0xFFC2410C),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "TEAM NACIONAL TX ARAGUA",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                letterSpacing = 1.sp
            )
            Text(
                text = "Hermandad • Rutas • Control de Acceso",
                style = MaterialTheme.typography.bodySmall,
                color = MotoOrangePrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(22.dp))

            // --- TARJETA CENTRAL: INGRESO CON CÓDIGO (TEMA CLARO) ---
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Encabezado
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MotoOrangePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = MotoOrangePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Ingreso Protegido",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Ingresa tu código único de invitación o credencial.",
                                color = Color(0xFF64748B),
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    // Campo de código con texto oscuro y visible de alto contraste
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = {
                            inviteCode = it
                            errorMessage = null
                        },
                        label = { Text("Código de Acceso") },
                        placeholder = { Text("Ej: TX-0000") },
                        textStyle = TextStyle(
                            color = Color(0xFF0F172A), // Letras oscuras y legibles garantizadas
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = if (showCode) 1.5.sp else 0.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color.White,
                            cursorColor = MotoOrangePrimary,
                            focusedBorderColor = MotoOrangePrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = MotoOrangePrimary,
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedPlaceholderColor = Color(0xFF94A3B8),
                            unfocusedPlaceholderColor = Color(0xFF94A3B8),
                            focusedLeadingIconColor = MotoOrangePrimary,
                            unfocusedLeadingIconColor = Color(0xFF64748B)
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MotoOrangePrimary
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (inviteCode.isNotEmpty()) {
                                    IconButton(onClick = {
                                        inviteCode = ""
                                        errorMessage = null
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Limpiar campo",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { showCode = !showCode }) {
                                    Icon(
                                        imageVector = if (showCode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (showCode) "Ocultar código" else "Mostrar código",
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            }
                        },
                        visualTransformation = if (showCode) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (inviteCode.isNotBlank() && !isLoading) {
                                    ejecutarLogin()
                                }
                            }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_login_code")
                    )

                    // Error message con banner claro
                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = errorMessage ?: "",
                                    color = Color(0xFFB91C1C),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Botón: ACCEDER (color institucional llamativo)
                    Button(
                        onClick = { ejecutarLogin() },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MotoOrangePrimary,
                            disabledContainerColor = MotoOrangePrimary.copy(alpha = 0.5f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_login_code")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ACCEDER A LA APLICACIÓN", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- TARJETA DE RECUPERACIÓN CON CORREO SINCRONIZADO ---
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clickable { showEmailRecoveryDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MotoOrangePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AlternateEmail,
                                    contentDescription = null,
                                    tint = MotoOrangePrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "¿Ya tienes cuenta vinculada?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Ingresa con tu correo sincronizado",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- TARJETA INFERIOR: SOLICITUD DE INGRESO ---
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SOLICITUD DE INGRESO",
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (attemptsLeft > 0) Color(0xFFFFFBEB) else Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, if (attemptsLeft > 0) Color(0xFFFCD34D) else Color(0xFFFCA5A5))
                        ) {
                            Text(
                                text = "Intentos: $attemptsLeft / 3",
                                color = if (attemptsLeft > 0) Color(0xFFB45309) else Color(0xFFB91C1C),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "¿No posees código de acceso? Solicita un código válido por 24h directamente a la directiva para que evalúe tu perfil de piloto.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            if (attemptsLeft <= 0) {
                                errorMessage = "Has agotado los 3 intentos permitidos para solicitar código."
                            } else {
                                showRequestCodeDialog = true
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFF8FAFC),
                            contentColor = Color(0xFF0F172A)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SOLICITAR CÓDIGO (3 INTENTOS)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // ==========================================
    // DIALOG: RECUPERAR ACCESO CON CORREO SINCRONIZADO
    // ==========================================
    if (showEmailRecoveryDialog) {
        DialogoRecuperarPorCorreo(
            onDismiss = { showEmailRecoveryDialog = false },
            onSolicitarCodigoPorCorreo = onSolicitarCodigoPorCorreo
        )
    }

    // ==========================================
    // DIALOG: MENÚ DESPLEGABLE AL TOCAR EL LOGO
    // ==========================================
    if (showIconMenuDialog) {
        Dialog(onDismissRequest = { showIconMenuDialog = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(MotoOrangePrimary, Color(0xFFEA580C)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    Text(
                        text = "MENÚ PRINCIPAL NACIONAL TX ARAGUA",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    ListItem(
                        headlineContent = { Text("Ingresar al Grupo de la Hermandad", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.5.sp) },
                        supportingContent = { Text("Únete a los canales oficiales en WhatsApp y Telegram", fontSize = 11.sp, color = Color(0xFF64748B)) },
                        leadingContent = { Icon(Icons.Default.GroupAdd, contentDescription = null, tint = WhatsAppGreen) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                            showIconMenuDialog = false
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://chat.whatsapp.com/teamtxvenezuela"))
                            context.startActivity(intent)
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Recuperar con Correo Sincronizado", fontWeight = FontWeight.Bold, color = MotoOrangePrimary, fontSize = 13.5.sp) },
                        supportingContent = { Text("Si ya eres miembro, verifica tu correo", fontSize = 11.sp, color = Color(0xFF64748B)) },
                        leadingContent = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = MotoOrangePrimary) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                            showIconMenuDialog = false
                            showEmailRecoveryDialog = true
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Solicitar Código de Entrada", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.5.sp) },
                        supportingContent = { Text("Formulario de solicitud (3 intentos / 24h)", fontSize = 11.sp, color = Color(0xFF64748B)) },
                        leadingContent = { Icon(Icons.Default.LockReset, contentDescription = null, tint = Color(0xFF475569)) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                            showIconMenuDialog = false
                            showRequestCodeDialog = true
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Normas & Estatutos del Club", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.5.sp) },
                        supportingContent = { Text("Reglamento de rodadas, honores y convivencia", fontSize = 11.sp, color = Color(0xFF64748B)) },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color(0xFF475569)) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                            showIconMenuDialog = false
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { showIconMenuDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar Menú", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG: SOLICITAR CÓDIGO DE ENTRADA
    // ==========================================
    if (showRequestCodeDialog) {
        DialogoSolicitudCodigo(
            onDismiss = { showRequestCodeDialog = false },
            onSubmitAccessRequest = onSubmitAccessRequest
        )
    }
}

// ==========================================
// DIALOG: RECUPERACIÓN CON CORREO
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoRecuperarPorCorreo(
    onDismiss: () -> Unit,
    onSolicitarCodigoPorCorreo: suspend (email: String) -> Pair<Boolean, String>
) {
    val coroutineScope = rememberCoroutineScope()
    var emailInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var resultadoMensaje by remember { mutableStateOf<String?>(null) }
    var esExitoso by remember { mutableStateOf(false) }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedContainerColor = Color(0xFFF8FAFC),
        unfocusedContainerColor = Color.White,
        cursorColor = MotoOrangePrimary,
        focusedBorderColor = MotoOrangePrimary,
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedLabelColor = MotoOrangePrimary,
        unfocusedLabelColor = Color(0xFF64748B)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MotoOrangePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text(
                            text = "RECUPERAR ACCESO",
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            fontSize = 15.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF64748B))
                    }
                }

                Text(
                    text = "Ingresa el correo electrónico con el que te registraste en Team TX. Verificaremos tu cuenta en el servidor de Firebase y solicitaremos tu nuevo código a la directiva.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = {
                        emailInput = it
                        resultadoMensaje = null
                    },
                    label = { Text("Correo Electrónico Sincronizado") },
                    placeholder = { Text("ejemplo@gmail.com") },
                    colors = tfColors,
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = MotoOrangePrimary)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = resultadoMensaje != null) {
                    Surface(
                        color = if (esExitoso) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, if (esExitoso) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (esExitoso) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (esExitoso) Color(0xFF16A34A) else Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = resultadoMensaje ?: "",
                                color = if (esExitoso) Color(0xFF15803D) else Color(0xFFB91C1C),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (emailInput.isBlank()) {
                            resultadoMensaje = "Ingresa tu correo electrónico."
                            esExitoso = false
                            return@Button
                        }
                        isLoading = true
                        resultadoMensaje = null
                        coroutineScope.launch {
                            val (ok, msg) = onSolicitarCodigoPorCorreo(emailInput)
                            isLoading = false
                            esExitoso = ok
                            resultadoMensaje = msg
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MotoOrangePrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VERIFICAR Y SOLICITAR CÓDIGO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (esExitoso) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("IR A INGRESAR CÓDIGO", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// NOTIFICACIÓN: PERFIL INCOMPLETO
// ==========================================

/**
 * Muestra un toast indicando que el perfil del Carnet TX está incompleto.
 * Se llama después del login exitoso cuando el perfil no tiene todos los campos requeridos.
 */
fun notificarPerfilIncompleto(context: Context) {
    Toast.makeText(
        context,
        "Completa los datos de tu carnet en la sección de Perfil",
        Toast.LENGTH_LONG
    ).show()
}

// ==========================================
// DIALOGO INTERNO: SOLICITAR CÓDIGO (TEMA CLARO)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoSolicitudCodigo(
    onDismiss: () -> Unit,
    onSubmitAccessRequest: (name: String, phone: String, dni: String, brand: String, model: String, color: String, plate: String, chapter: String, reason: String, birthDate: String, role: String) -> Boolean
) {
    var reqFullName by remember { mutableStateOf("") }
    var reqPhone by remember { mutableStateOf("") }
    var reqDni by remember { mutableStateOf("") }
    var reqModel by remember { mutableStateOf("TX 200 SM") }
    var reqPlate by remember { mutableStateOf("") }
    var reqRole by remember { mutableStateOf("Piloto") }
    var reqBirthDate by remember { mutableStateOf("") }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedContainerColor = Color(0xFFF8FAFC),
        unfocusedContainerColor = Color.White,
        cursorColor = MotoOrangePrimary,
        focusedBorderColor = MotoOrangePrimary,
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedLabelColor = MotoOrangePrimary,
        unfocusedLabelColor = Color(0xFF64748B)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SOLICITUD DE CÓDIGO (24H)",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF64748B))
                    }
                }

                Text(
                    text = "Completa tus datos de piloto y de tu moto. Esta solicitud llegará directamente a la directiva para evaluar tu código temporal.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                OutlinedTextField(
                    value = reqFullName,
                    onValueChange = { reqFullName = it },
                    label = { Text("Nombre y Apellido") },
                    colors = tfColors,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reqBirthDate,
                    onValueChange = { reqBirthDate = it },
                    label = { Text("Fecha de Nacimiento") },
                    placeholder = { Text("DD/MM/AAAA") },
                    colors = tfColors,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Rol Solicitado",
                    color = Color(0xFF0F172A),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp, start = 2.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { reqRole = "Piloto" }
                    ) {
                        RadioButton(
                            selected = reqRole == "Piloto",
                            onClick = { reqRole = "Piloto" },
                            colors = RadioButtonDefaults.colors(selectedColor = MotoOrangePrimary)
                        )
                        Text("Piloto", color = Color(0xFF0F172A), fontSize = 14.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { reqRole = "Copiloto" }
                    ) {
                        RadioButton(
                            selected = reqRole == "Copiloto",
                            onClick = { reqRole = "Copiloto" },
                            colors = RadioButtonDefaults.colors(selectedColor = MotoOrangePrimary)
                        )
                        Text("Copiloto", color = Color(0xFF0F172A), fontSize = 14.sp)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reqPhone,
                        onValueChange = { reqPhone = it },
                        label = { Text("Teléfono") },
                        colors = tfColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reqDni,
                        onValueChange = { reqDni = it },
                        label = { Text("Cédula / DNI") },
                        colors = tfColors,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reqModel,
                        onValueChange = { reqModel = it },
                        label = { Text("Modelo Moto") },
                        colors = tfColors,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reqPlate,
                        onValueChange = { reqPlate = it },
                        label = { Text("Placa") },
                        colors = tfColors,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        if (reqFullName.isBlank() || reqPhone.isBlank()) return@Button
                        onSubmitAccessRequest(
                            reqFullName, reqPhone, reqDni, "Keeway", reqModel,
                            "Negro / Naranja", reqPlate, "Venezuela", "", reqBirthDate, reqRole
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MotoOrangePrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ENVIAR SOLICITUD", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
