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
 * VISTA_LOGIN — Pantalla de login simplificada (Solo Código).
 * Flujo: código de invitación → acceso directo al dashboard.
 * Google se vincula después desde el Carnet TX.
 * Si el perfil está incompleto → notificación (no bloquea).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaLogin(
    onRequestCodeLogin: suspend (code: String) -> Pair<Boolean, String>,
    onSubmitAccessRequest: (name: String, phone: String, dni: String, brand: String, model: String, color: String, plate: String, chapter: String, reason: String, birthDate: String, role: String) -> Boolean,
    attemptsLeft: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var inviteCode by remember { mutableStateOf("") }
    var showCode by remember { mutableStateOf(false) }
    var codeStatus by remember { mutableStateOf(CodeStatus.PENDING) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var showIconMenuDialog by remember { mutableStateOf(false) }
    var showRequestCodeDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = AsphaltDarkBackground
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
                    .size(90.dp)
                    .clickable { showIconMenuDialog = true }
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

            Spacer(modifier = Modifier.height(8.dp))

            // Chip "Toca el logo para abrir el menú de acceso"
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TxFlameRed.copy(alpha = 0.15f),
                border = BorderStroke(0.8.dp, TxFlameRed.copy(alpha = 0.5f)),
                modifier = Modifier.clickable { showIconMenuDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MotoOrangePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Toca el logo para abrir el menú de acceso",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "TEAM NACIONAL TX ARAGUA",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = "Hermandad • Rutas • Control de Acceso",
                style = MaterialTheme.typography.bodySmall,
                color = TxGoldBrass,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- TARJETA CENTRAL: INGRESO PROTEGIDO ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                border = BorderStroke(1.dp, TxSteelSilver.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Encabezado: Ingreso Protegido
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusSuccess
                        )
                        Text(
                            text = "Ingreso Protegido",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }

                    Text(
                        text = "Ingresa tu código único de invitación para acceder a la app.",
                        color = TxSteelSilver,
                        fontSize = 12.sp
                    )

                    // Campo de código (asteriscos + ojito + botón OK)
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { 
                            inviteCode = it
                            codeStatus = CodeStatus.PENDING
                            errorMessage = null
                        },
                        label = { Text("Código Único de Invitación") },
                        placeholder = { Text("Ej: TX-TEST-ADMIN") },
                        leadingIcon = {
                            Icon(
                                imageVector = when (codeStatus) {
                                    CodeStatus.VALID -> Icons.Default.CheckCircle
                                    CodeStatus.INVALID -> Icons.Default.Error
                                    else -> Icons.Default.VpnKey
                                },
                                contentDescription = null,
                                tint = when (codeStatus) {
                                    CodeStatus.VALID -> StatusSuccess
                                    CodeStatus.INVALID -> StatusError
                                    else -> TxGoldBrass
                                }
                            )
                        },
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { showCode = !showCode }) {
                                    Icon(
                                        imageVector = if (showCode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (showCode) "Ocultar" else "Mostrar",
                                        tint = TxSteelSilver
                                    )
                                }
                                IconButton(onClick = {
                                    val code = inviteCode.trim().uppercase()
                                    codeStatus = if (code.isNotEmpty() && (CODIGOS_MAESTROS.containsKey(code) || 
                                        code == "PILOTO19" || code == "PILOT019")) {
                                        CodeStatus.VALID
                                    } else if (code.isNotEmpty()) {
                                        CodeStatus.INVALID
                                    } else {
                                        CodeStatus.PENDING
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Validar código",
                                        tint = when (codeStatus) {
                                            CodeStatus.VALID -> StatusSuccess
                                            CodeStatus.INVALID -> StatusError
                                            else -> TxSteelSilver
                                        }
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
                                val code = inviteCode.trim().uppercase()
                                codeStatus = if (code.isNotEmpty() && (CODIGOS_MAESTROS.containsKey(code) || 
                                    code == "PILOTO19" || code == "PILOT019")) {
                                    CodeStatus.VALID
                                } else if (code.isNotEmpty()) {
                                    CodeStatus.INVALID
                                } else {
                                    CodeStatus.PENDING
                                }
                                focusManager.clearFocus()
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_login_code")
                    )

                    // Feedback visual del código
                    AnimatedVisibility(visible = codeStatus == CodeStatus.VALID) {
                        Surface(
                            color = StatusSuccess.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, StatusSuccess),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                                Text("Código válido — presiona ACCEDER para continuar", color = StatusSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    AnimatedVisibility(visible = codeStatus == CodeStatus.INVALID) {
                        Surface(
                            color = StatusError.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, StatusError),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                                Text("Código no reconocido — verifica o solicita uno nuevo", color = StatusError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Botón: ACCEDER (solo código)
                    Button(
                        onClick = {
                            if (inviteCode.isBlank()) {
                                errorMessage = "Debes ingresar un código de invitación válido."
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val (exito, mensaje) = onRequestCodeLogin(inviteCode)
                                isLoading = false
                                if (!exito) {
                                    errorMessage = mensaje
                                    codeStatus = CodeStatus.INVALID
                                }
                                // Si es exitoso, la navegación se encarga
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TxFlameRed,
                            disabledContainerColor = TxFlameRed.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_login_code")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ACCEDER", fontWeight = FontWeight.Black)
                        }
                    }

                    // Error message
                    AnimatedVisibility(visible = errorMessage != null) {
                        Surface(
                            color = StatusError.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, StatusError),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                                Text(errorMessage ?: "", color = StatusError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- TARJETA INFERIOR: SOLICITUD DE INGRESO ---
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202D)),
                border = BorderStroke(1.2.dp, Brush.horizontalGradient(listOf(MotoOrangePrimary, TxGoldBrass))),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SOLICITUD DE INGRESO",
                            fontWeight = FontWeight.Black,
                            color = TxGoldLight,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (attemptsLeft > 0) StatusWarning.copy(alpha = 0.2f) else StatusError.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, if (attemptsLeft > 0) StatusWarning else StatusError)
                        ) {
                            Text(
                                text = "Intentos: $attemptsLeft / 3",
                                color = if (attemptsLeft > 0) StatusWarning else StatusError,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "¿No posees código de acceso? Solicita un código válido por 24h directamente al Líder Nacional para que evalúe tu perfil de piloto.",
                        color = TxSteelSilver,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (attemptsLeft <= 0) {
                                errorMessage = "Has agotado los 3 intentos permitidos para solicitar código."
                            } else {
                                showRequestCodeDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF263045),
                            contentColor = TxGoldLight
                        ),
                        border = BorderStroke(1.dp, TxGoldBrass.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = TxGoldBrass)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SOLICITAR CÓDIGO DE ENTRADA (3 INTENTOS)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ==========================================
    // DIALOG: MENÚ DESPLEGABLE AL TOCAR EL LOGO
    // ==========================================
    if (showIconMenuDialog) {
        Dialog(onDismissRequest = { showIconMenuDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141923),
                border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(TxFlameRed, TxGoldBrass))),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(50.dp).clip(CircleShape)
                            .background(Brush.radialGradient(listOf(MotoOrangePrimary, Color(0xFF8B1E0F)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    Text(
                        text = "MENÚ PRINCIPAL NACIONAL TX ARAGUA",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )

                    Divider(color = TxSteelSilver.copy(alpha = 0.2f))

                    ListItem(
                        headlineContent = { Text("Ingresar al Grupo de la Hermandad", fontWeight = FontWeight.Bold, color = Color.White) },
                        supportingContent = { Text("Únete a los canales oficiales en WhatsApp y Telegram", fontSize = 11.sp, color = TxSteelSilver) },
                        leadingContent = { Icon(Icons.Default.GroupAdd, contentDescription = null, tint = WhatsAppGreen) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                            showIconMenuDialog = false
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://chat.whatsapp.com/teamtxvenezuela"))
                            context.startActivity(intent)
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Solicitar Código de Entrada", fontWeight = FontWeight.Bold, color = TxGoldLight) },
                        supportingContent = { Text("Formulario de solicitud (3 intentos / 24h)", fontSize = 11.sp, color = TxSteelSilver) },
                        leadingContent = { Icon(Icons.Default.LockReset, contentDescription = null, tint = TxGoldBrass) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                            showIconMenuDialog = false
                            showRequestCodeDialog = true
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Normas & Estatutos del Club", fontWeight = FontWeight.Bold, color = Color.White) },
                        supportingContent = { Text("Reglamento de rodadas, honores y convivencia", fontSize = 11.sp, color = TxSteelSilver) },
                        leadingContent = { Icon(Icons.Default.MenuBook, contentDescription = null, tint = TxChromeSilver) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                            showIconMenuDialog = false
                        }
                    )

                    Button(
                        onClick = { showIconMenuDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263045)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar Menú", color = Color.White)
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
// ESTADO DEL CÓDIGO
// ==========================================

private enum class CodeStatus { PENDING, VALID, INVALID }

// ==========================================
// CÓDIGOS MAESTROS (hardcoded, sin Firestore)
// ==========================================

private val CODIGOS_MAESTROS = mapOf(
    // Maestros originales (Líder / Presidente)
    "TX19554402" to "PRESIDENTE",
    "TX19554402SB" to "PRESIDENTE",
    "19554402SB" to "PRESIDENTE",
    // Códigos de prueba Google (asignan rol específico)
    "TX-TEST-ADMIN" to "PRESIDENTE",
    "TX-TEST-DIRECTIVA" to "DIRECTIVA",
    "TX-TEST-PILOTO" to "MIEMBRO_ACTIVO"
)

// ==========================================
// DIALOGO INTERNO: SOLICITAR CÓDIGO
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF141923),
            border = BorderStroke(1.5.dp, TxGoldBrass),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SOLICITUD DE CÓDIGO (24H)",
                        fontWeight = FontWeight.Black,
                        color = TxGoldLight,
                        fontSize = 15.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TxSteelSilver)
                    }
                }

                Text(
                    text = "Completa tus datos de piloto y de tu moto. Esta solicitud llegará directamente al Líder para la generación de tu código de 24 horas.",
                    fontSize = 11.sp,
                    color = TxSteelSilver
                )

                OutlinedTextField(
                    value = reqFullName,
                    onValueChange = { reqFullName = it },
                    label = { Text("Nombre y Apellido") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reqBirthDate,
                    onValueChange = { reqBirthDate = it },
                    label = { Text("Fecha de Nacimiento") },
                    placeholder = { Text("DD/MM/AAAA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Rol Solicitado", color = TxSteelSilver, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { reqRole = "Piloto" }) {
                        RadioButton(selected = reqRole == "Piloto", onClick = { reqRole = "Piloto" })
                        Text("Piloto", color = Color.White, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { reqRole = "Copiloto" }) {
                        RadioButton(selected = reqRole == "Copiloto", onClick = { reqRole = "Copiloto" })
                        Text("Copiloto", color = Color.White, fontSize = 14.sp)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reqPhone,
                        onValueChange = { reqPhone = it },
                        label = { Text("Teléfono WhatsApp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reqDni,
                        onValueChange = { reqDni = it },
                        label = { Text("Cédula / DNI") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reqModel,
                        onValueChange = { reqModel = it },
                        label = { Text("Modelo Moto") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reqPlate,
                        onValueChange = { reqPlate = it },
                        label = { Text("Placa") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        if (reqFullName.isBlank() || reqPhone.isBlank()) return@Button
                        onSubmitAccessRequest(
                            reqFullName, reqPhone, reqDni, "Keeway", reqModel,
                            "Negro / Naranja", reqPlate, "Venezuela", "", reqBirthDate, reqRole
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TxFlameRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ENVIAR SOLICITUD", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
