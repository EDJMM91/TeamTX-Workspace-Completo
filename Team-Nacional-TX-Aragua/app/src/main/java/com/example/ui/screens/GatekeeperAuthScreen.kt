package com.example.ui.screens

import com.aistudio.teamtxvzla.R
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.aistudio.teamtxvzla.nube.AutenticacionNube

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatekeeperAuthScreen(
    onRequestPhoneLogin: (String) -> Boolean,
    onRequestCodeLogin: suspend (code: String, name: String, phone: String, bikeModel: String, bikePlate: String, birthDate: String, role: String) -> Pair<Boolean, String>,
    onSubmitAccessRequest: (name: String, phone: String, dni: String, brand: String, model: String, color: String, plate: String, chapter: String, reason: String, birthDate: String, role: String) -> Boolean,
    onGoogleSignInSuccess: suspend (uid: String, email: String, photoUrl: String?) -> Pair<Boolean, String>,
    attemptsLeft: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Telefono, 1 = Codigo Invitacion
    var phoneNumber by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var applicantName by remember { mutableStateOf("") }
    var applicantBikeModel by remember { mutableStateOf("TX 200 SM") }
    var applicantBikePlate by remember { mutableStateOf("") }
    var applicantBirthDate by remember { mutableStateOf("") }
    var applicantRole by remember { mutableStateOf("Piloto") }
    var isCodePasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Dialog States
    var showIconMenuDialog by remember { mutableStateOf(false) }
    var showRequestCodeDialog by remember { mutableStateOf(false) }
    var showMasterCodeDialog by remember { mutableStateOf(false) }
    var showStatutesDialog by remember { mutableStateOf(false) }
    var showGooglePhoneLinkDialog by remember { mutableStateOf(false) }
    var isGoogleAuthLoading by remember { mutableStateOf(false) }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("554561749183-q637440n43im5mjfg5pp9lsb4uu0a441.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isGoogleAuthLoading = false
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            val fallbackEmail = account?.email ?: ""
            val googlePhotoUrl = account?.photoUrl?.toString()
            
            if (idToken != null) {
                // LLAMADA AL NUEVO MÓDULO DE AUTENTICACIÓN
                AutenticacionNube.iniciarSesionConGoogle(idToken) { usuarioFirebase ->
                    if (usuarioFirebase != null) {
                        coroutineScope.launch {
                            val uid = usuarioFirebase.uid
                            val email = usuarioFirebase.email ?: fallbackEmail
                            val photo = usuarioFirebase.photoUrl?.toString() ?: googlePhotoUrl
                            
                            val response = onGoogleSignInSuccess(uid, email, photo)
                            if (response.first) {
                                Toast.makeText(context, response.second, Toast.LENGTH_LONG).show()
                            } else {
                                showGooglePhoneLinkDialog = true
                            }
                        }
                    } else {
                        Toast.makeText(context, "Fallo al vincular con la nube de Firebase. Revisa tu conexión.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(context, "No se pudo obtener el token de Google", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            if (e.statusCode != 12501 && e.statusCode != 12502) { // 12501 = Cancelled by user
                Log.e("GoogleSignIn", "ApiException code: ${e.statusCode}", e)
                Toast.makeText(context, "Error al acceder con Google (${e.statusCode}): ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

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

            // --- APP ICON (INTERACTIVE ON TAP) ---
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clickable {
                        showIconMenuDialog = true
                    }
                    .testTag("app_logo_interactive_button"),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.logoteam),
                    contentDescription = "Team TX Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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

            // --- TABS: TELEFONO VS CODIGO ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF141923),
                contentColor = MotoOrangePrimary,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, TxSteelSilver.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        errorMessage = null
                    },
                    text = {
                        Text(
                            text = "MIEMBRO REGISTRADO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    },
                    icon = { Icon(Icons.Default.PhoneIphone, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        errorMessage = null
                    },
                    text = {
                        Text(
                            text = "CÓDIGO DE ACCESO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    },
                    icon = { Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Feedback Messages
            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    color = StatusError.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, StatusError),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusError)
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFFF8A80),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            AnimatedVisibility(visible = successMessage != null) {
                Surface(
                    color = StatusSuccess.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, StatusSuccess),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                        Text(
                            text = successMessage ?: "",
                            color = Color(0xFF69F0AE),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- TAB CONTENT ---
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
                    if (selectedTab == 0) {
                        // --- INGRESO CON TELEFONO ---
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = StatusSuccess)
                            Text(
                                text = "Ingreso Rápido de Piloto",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "Si ya eres un integrante activo de Team Nacional TX Aragua, ingresa tu número telefónico registrado para acceder de forma directa y sin restricciones.",
                            color = TxSteelSilver,
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Número de Teléfono") },
                            placeholder = { Text("Ej: +58 412 123 4567 ó 0414...") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MotoOrangePrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_login_phone")
                        )

                        Button(
                            onClick = {
                                if (phoneNumber.isBlank()) {
                                    errorMessage = "Por favor ingresa tu número de teléfono registrado."
                                    return@Button
                                }
                                isLoading = true
                                val success = onRequestPhoneLogin(phoneNumber)
                                isLoading = false
                                if (!success) {
                                    errorMessage = "El número telefónico '$phoneNumber' no se encuentra registrado en el sistema del Club. Usa un código de invitación o solicita tu acceso a la Directiva."
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_login_phone_submit")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.Login, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("INGRESAR CON MI TELÉFONO", fontWeight = FontWeight.Black)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Google Sign-In Button
                        Button(
                            onClick = { 
                                if (!isGoogleAuthLoading) {
                                    isGoogleAuthLoading = true
                                    googleSignInClient.signOut().addOnCompleteListener {
                                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                    }
                                }
                            },
                            enabled = !isGoogleAuthLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                disabledContainerColor = Color(0xFFE0E0E0)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (isGoogleAuthLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = TxFlameRed,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Accediendo con Google...", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                            } else {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = TxFlameRed, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("ACCEDER CON GOOGLE", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                            }
                        }
                    } else {
                        // --- INGRESO CON CODIGO DE INVITACION O MAESTRO ---
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = TxGoldBrass)
                            Text(
                                text = "Código de Invitación / Maestro",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "Nadie puede ingresar sin código. Introduce el código proporcionado por el Líder (válido por 24h) o tu Código Maestro de Directiva.",
                            color = TxSteelSilver,
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = inviteCode,
                            onValueChange = { inviteCode = it },
                            label = { Text("Código de Invitación o Maestro") },
                            placeholder = { Text("Introduce tu código") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TxGoldBrass) },
                            trailingIcon = {
                                IconButton(onClick = { isCodePasswordVisible = !isCodePasswordVisible }) {
                                    Icon(
                                        imageVector = if (isCodePasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = TxSteelSilver
                                    )
                                }
                            },
                            visualTransformation = if (isCodePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_login_code")
                        )

                        // Quick access buttons for master codes
                        if (inviteCode.isBlank()) {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Códigos Maestros de Acceso Directo:", fontSize = 11.sp, color = TxGoldBrass, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            inviteCode = "19554402sb"
                                            applicantName = "Eduardo Androide"
                                            phoneNumber = "04243769999"
                                            applicantBirthDate = "01/01/1990"
                                            applicantBikeModel = "TX 200 DEV"
                                            applicantBikePlate = "DEV01"
                                            applicantRole = "Piloto"
                                        },
                                        border = BorderStroke(1.dp, StatusSuccess),
                                        colors = ButtonDefaults.buttonColors(contentColor = StatusSuccess),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("DEV", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            inviteCode = "tx19554402"
                                            applicantName = "Presidente"
                                            applicantBirthDate = "01/01/1980"
                                            applicantBikeModel = "TX 200 SM"
                                            applicantBikePlate = "DIR01"
                                            applicantRole = "Piloto"
                                        },
                                        border = BorderStroke(1.dp, MotoOrangePrimary),
                                        colors = ButtonDefaults.buttonColors(contentColor = MotoOrangePrimary),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("M1", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            inviteCode = "tx19554402sb"
                                            applicantName = "Vicepresidente"
                                            applicantBirthDate = "01/01/1985"
                                            applicantBikeModel = "TX 200 EN"
                                            applicantBikePlate = "DIR02"
                                            applicantRole = "Piloto"
                                        },
                                        border = BorderStroke(1.dp, MotoOrangePrimary),
                                        colors = ButtonDefaults.buttonColors(contentColor = MotoOrangePrimary),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("M2", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // If user is inputting a regular invite code, allow entering their name
                        OutlinedTextField(
                            value = applicantName,
                            onValueChange = { applicantName = it },
                            label = { Text("Nombre Completo (Para nuevos integrantes)") },
                            placeholder = { Text("Ej: Alexander Rondón") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = applicantBirthDate,
                            onValueChange = { applicantBirthDate = it },
                            label = { Text("Fecha de Nacimiento") },
                            placeholder = { Text("DD/MM/AAAA") },
                            leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Rol en el Club", color = TxSteelSilver, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { applicantRole = "Piloto" }) {
                                RadioButton(selected = applicantRole == "Piloto", onClick = { applicantRole = "Piloto" })
                                Text("Piloto", color = Color.White, fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { applicantRole = "Copiloto" }) {
                                RadioButton(selected = applicantRole == "Copiloto", onClick = { applicantRole = "Copiloto" })
                                Text("Copiloto", color = Color.White, fontSize = 14.sp)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = applicantBikeModel,
                                onValueChange = { applicantBikeModel = it },
                                label = { Text("Modelo Moto") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = applicantBikePlate,
                                onValueChange = { applicantBikePlate = it },
                                label = { Text("Placa") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Surface(
                            color = Color(0xFF1B2230),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "Aviso: Estos códigos son personales e intransferibles. Deben guardarse y manejarse con total responsabilidad.",
                                    color = TxSteelSilver,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (inviteCode.isBlank()) {
                                    errorMessage = "Por favor ingresa el código de invitación o código maestro."
                                    return@Button
                                }
                                isLoading = true
                                coroutineScope.launch {
                                    val (success, msg) = onRequestCodeLogin(
                                        inviteCode,
                                        applicantName,
                                        phoneNumber,
                                        applicantBikeModel,
                                        applicantBikePlate,
                                        applicantBirthDate,
                                        applicantRole
                                    )
                                    isLoading = false
                                    if (success) {
                                        successMessage = msg
                                        errorMessage = null
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = TxFlameRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_login_code_submit")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.Key, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("VALIDAR Y ACCEDER A LA APP", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- SOLICITAR CODIGO DE ENTRADA (3 INTENTOS, 24H) ---
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
                                errorMessage = "Has agotado los 3 intentos permitidos para solicitar código. Contacta directamente a la Directiva."
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_request_access_code")
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
    // DIALOG: MENU DESPLEGABLE AL TOCAR EL LOGO
    // ==========================================
    if (showIconMenuDialog) {
        Dialog(onDismissRequest = { showIconMenuDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141923),
                border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(TxFlameRed, TxGoldBrass))),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
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

                    // Option 1: Ingresar al Grupo (WhatsApp / Telegram)
                    ListItem(
                        headlineContent = { Text("Ingresar al Grupo de la Hermandad", fontWeight = FontWeight.Bold, color = Color.White) },
                        supportingContent = { Text("Únete a los canales oficiales en WhatsApp y Telegram", fontSize = 11.sp, color = TxSteelSilver) },
                        leadingContent = { Icon(Icons.Default.GroupAdd, contentDescription = null, tint = WhatsAppGreen) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showIconMenuDialog = false
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://chat.whatsapp.com/teamtxvenezuela"))
                                context.startActivity(intent)
                            }
                    )

                    // Option 2: Solicitar Código de Entrada
                    ListItem(
                        headlineContent = { Text("Solicitar Código de Entrada", fontWeight = FontWeight.Bold, color = TxGoldLight) },
                        supportingContent = { Text("Formulario de solicitud (3 intentos / 24h)", fontSize = 11.sp, color = TxSteelSilver) },
                        leadingContent = { Icon(Icons.Default.LockReset, contentDescription = null, tint = TxGoldBrass) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showIconMenuDialog = false
                                showRequestCodeDialog = true
                            }
                    )

                    // Option 3: Código Maestro Directiva
                    ListItem(
                        headlineContent = { Text("Acceso Directiva con Código Maestro", fontWeight = FontWeight.Bold, color = MotoOrangePrimary) },
                        supportingContent = { Text("Exclusivo para el Líder Nacional y Directivos", fontSize = 11.sp, color = TxSteelSilver) },
                        leadingContent = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MotoOrangePrimary) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showIconMenuDialog = false
                                showMasterCodeDialog = true
                            }
                    )

                    // Option 4: Estatutos y Normas
                    ListItem(
                        headlineContent = { Text("Normas & Estatutos del Club", fontWeight = FontWeight.Bold, color = Color.White) },
                        supportingContent = { Text("Reglamento de rodadas, honores y convivencia", fontSize = 11.sp, color = TxSteelSilver) },
                        leadingContent = { Icon(Icons.Default.MenuBook, contentDescription = null, tint = TxChromeSilver) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showIconMenuDialog = false
                                showStatutesDialog = true
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
    // DIALOG: SOLICITAR CODIGO DE ENTRADA (3 INTENTOS / 24H)
    // ==========================================
    if (showRequestCodeDialog) {
        var reqFullName by remember { mutableStateOf("") }
        var reqPhone by remember { mutableStateOf("") }
        var reqDni by remember { mutableStateOf("") }
        var reqBrand by remember { mutableStateOf("Keeway") }
        var reqModel by remember { mutableStateOf("TX 200 SM") }
        var reqColor by remember { mutableStateOf("Negro / Naranja") }
        var reqPlate by remember { mutableStateOf("") }
        var reqChapter by remember { mutableStateOf("Caracas / Miranda") }
        var reqReason by remember { mutableStateOf("") }
        var reqBirthDate by remember { mutableStateOf("") }
        var reqRole by remember { mutableStateOf("Piloto") }

        Dialog(onDismissRequest = { showRequestCodeDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141923),
                border = BorderStroke(1.5.dp, TxGoldBrass),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
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
                        IconButton(onClick = { showRequestCodeDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TxSteelSilver)
                        }
                    }

                    Text(
                        text = "Completa tus datos de piloto y de tu moto. Esta solicitud llegará directamente al Líder para la generación de tu código de 24 horas.",
                        fontSize = 11.sp,
                        color = TxSteelSilver
                    )

                    Surface(
                        color = Color(0xFF2A1010),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "¡ATENCIÓN! Estos códigos NO son transferibles. Deben guardarse de manera personal y responsable.",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

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

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = reqColor,
                            onValueChange = { reqColor = it },
                            label = { Text("Color") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = reqChapter,
                            onValueChange = { reqChapter = it },
                            label = { Text("Capítulo / Estado") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = reqReason,
                        onValueChange = { reqReason = it },
                        label = { Text("Motivo / ¿Por qué deseas unirte?") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            if (reqFullName.isBlank() || reqPhone.isBlank() || reqPlate.isBlank()) {
                                Toast.makeText(context, "Por favor completa los campos principales (Nombre, Teléfono, Placa)", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val ok = onSubmitAccessRequest(
                                reqFullName,
                                reqPhone,
                                reqDni,
                                reqBrand,
                                reqModel,
                                reqColor,
                                reqPlate,
                                reqChapter,
                                reqReason,
                                reqBirthDate,
                                reqRole
                            )
                            showRequestCodeDialog = false
                            if (ok) {
                                successMessage = "¡Solicitud de acceso enviada al Líder! Recibirás tu código de 24h para ingresar."
                            } else {
                                errorMessage = "No tienes más intentos disponibles."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_submit_access_request_form")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ENVIAR SOLICITUD AL LÍDER", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG: ACCESO RAPIDO CODIGO MAESTRO DIRECTIVA
    // ==========================================
    if (showMasterCodeDialog) {
        var masterInput by remember { mutableStateOf("") }
        var masterError by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showMasterCodeDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141923),
                border = BorderStroke(1.5.dp, MotoOrangePrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(36.dp))
                    Text(
                        text = "Acceso Directiva / Código Maestro",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Ingresa uno de los códigos maestros permanentes del Líder.",
                        fontSize = 11.sp,
                        color = TxSteelSilver,
                        textAlign = TextAlign.Center
                    )

                    var masterPasswordVisible by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = masterInput,
                        onValueChange = {
                            masterInput = it
                            masterError = null
                        },
                        label = { Text("Código Maestro de Líder") },
                        placeholder = { Text("Ingresa el código maestro") },
                        visualTransformation = if (masterPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (masterPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { masterPasswordVisible = !masterPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = if (masterPasswordVisible) "Ocultar" else "Mostrar")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (masterError != null) {
                        Text(masterError ?: "", color = StatusError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val (ok, msg) = onRequestCodeLogin(masterInput, "", "", "", "", "", "Piloto")
                                if (ok) {
                                    showMasterCodeDialog = false
                                } else {
                                    masterError = "Código Maestro inválido."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Acceder como Directiva / Líder", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG: ESTATUTOS Y NORMAS DEL CLUB
    // ==========================================
    if (showStatutesDialog) {
        Dialog(onDismissRequest = { showStatutesDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141923),
                border = BorderStroke(1.dp, TxSteelSilver),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "📜 ESTATUTOS & CÓDIGO DE HONOR NACIONAL TX ARAGUA",
                        fontWeight = FontWeight.Black,
                        color = TxGoldLight,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "1. Disciplina en Ruta: Prohibido adelantar al Capitán Puntero o quedarse rezagado del Barredor.\n\n" +
                                "2. Equipamiento Obligatorio: Casco integral o modular certificado, guantes, chaqueta y calzado cerrado.\n\n" +
                                "3. Mantenimiento Preventivo: Cadena lubricada, frenos y niveles de aceite al 100% antes de cada rodada.\n\n" +
                                "4. Hermandad y Asistencia: Ningún hermano con fallas mecánicas o emergencia médica se deja solo en carretera.\n\n" +
                                "5. Códigos de Acceso: El acceso a la app y rangos de Directiva son administrados por el Presidente y Líder Nacional.",
                        fontSize = 12.sp,
                        color = Color.White,
                        lineHeight = 18.sp
                    )
                    Button(
                        onClick = { showStatutesDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263045)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Entendido", color = Color.White)
                    }
                }
            }
        }
    }
    
    // ==========================================
    // DIALOG: VINCULAR GOOGLE CON TELEFONO
    // ==========================================
    if (showGooglePhoneLinkDialog) {
        var linkPhone by remember { mutableStateOf("") }
        var linkError by remember { mutableStateOf<String?>(null) }
        
        Dialog(onDismissRequest = { showGooglePhoneLinkDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF141923),
                border = BorderStroke(1.5.dp, Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Text(
                        text = "Vincular Cuenta de Google",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Por seguridad y sincronización en la nube, ingresa tu número telefónico registrado en el Club para vincular tu cuenta de Google.",
                        fontSize = 12.sp,
                        color = TxSteelSilver,
                        textAlign = TextAlign.Center
                    )
                    
                    OutlinedTextField(
                        value = linkPhone,
                        onValueChange = { 
                            linkPhone = it
                            linkError = null
                        },
                        label = { Text("Teléfono Registrado") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (linkError != null) {
                        Text(linkError ?: "", color = StatusError, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            if (linkPhone.isBlank()) {
                                linkError = "Ingresa un número telefónico."
                                return@Button
                            }
                            coroutineScope.launch {
                                // MOCK: Llama al login telefónico estándar como fallback si no hay Firebase configurado.
                                // En una app real, aquí se llama a teamTxViewModel.linkPhoneToGoogleAccount(...)
                                val ok = onRequestPhoneLogin(linkPhone)
                                if (ok) {
                                    showGooglePhoneLinkDialog = false
                                } else {
                                    linkError = "Número no encontrado. Registrate primero o solicita acceso."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Vincular y Acceder", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
