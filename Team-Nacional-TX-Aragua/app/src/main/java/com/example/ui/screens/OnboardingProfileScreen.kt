package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import com.example.data.model.MemberProfile
import com.example.ui.theme.MotoOrangePrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TxFlameRed
import com.example.ui.theme.TxGoldBrass
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingProfileScreen(
    currentMember: MemberProfile,
    onSaveProfile: (MemberProfile) -> Unit,
    onGoogleSignInSuccess: suspend (uid: String, email: String, photoUrl: String?) -> Unit,
    onSkip: () -> Unit = {}
) {
    var fullName by remember { mutableStateOf(if (currentMember.fullName == "Piloto de Pruebas") "" else currentMember.fullName) }
    var phone by remember { mutableStateOf(currentMember.phone) }
    var cedulaDni by remember { mutableStateOf(currentMember.cedulaDni) }
    
    // Motorcycle specs
    var bikeBrand by remember { mutableStateOf(if (currentMember.bikeBrand == "Keeway") "" else currentMember.bikeBrand) }
    var bikeModel by remember { mutableStateOf(if (currentMember.bikeModel == "TX 200 SM") "" else currentMember.bikeModel) }
    var bikePlate by remember { mutableStateOf(currentMember.bikePlate) }

    // Medical & SOS
    var bloodType by remember { mutableStateOf(currentMember.bloodType) }
    var emergencyContactName by remember { mutableStateOf(currentMember.emergencyContactName) }
    var emergencyContactPhone by remember { mutableStateOf(currentMember.emergencyContactPhone) }

    val bloodOptions = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")
    var showBloodMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf("") }
    
    // Google Sync State
    val isGoogleSynced = currentMember.firebaseUid != null
    var isGoogleLoading by remember { mutableStateOf(false) }

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
        isGoogleLoading = false
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            val fallbackEmail = account?.email ?: ""
            if (idToken != null) {
                coroutineScope.launch {
                    var firebaseUid = account.id ?: ""
                    var firebaseEmail = fallbackEmail
                    var firebasePhotoUrl: String? = account.photoUrl?.toString()
                    try {
                        val firebaseAuth = FirebaseAuth.getInstance()
                        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                        val user = authResult.user
                        if (user != null) {
                            firebaseUid = user.uid
                            firebaseEmail = user.email ?: fallbackEmail
                            if (firebasePhotoUrl == null) firebasePhotoUrl = user.photoUrl?.toString()
                        }
                    } catch (authErr: Exception) {
                        android.util.Log.e("FirebaseAuth", "Firebase sign-in with credential notice: ${authErr.message}")
                    }

                    onGoogleSignInSuccess(firebaseUid, firebaseEmail, firebasePhotoUrl)
                    Toast.makeText(context, "Sincronizado y autenticado con Google exitosamente", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "No se pudo obtener el token de Google", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            if (e.statusCode != 12501 && e.statusCode != 12502) {
                android.util.Log.e("GoogleSignIn", "ApiException code: ${e.statusCode}", e)
                Toast.makeText(context, "Error al vincular con Google (${e.statusCode}): ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración Inicial Obligatoria", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TxFlameRed, titleContentColor = Color.White)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "¡Bienvenido al Team!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MotoOrangePrimary
                )
                Text(
                    text = "Para poder usar la aplicación y ver el contenido del Team Nacional TX Aragua, debes completar tu perfil y vincular tu cuenta con Google. Estos datos son obligatorios por seguridad.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Text(
                    text = "1. DATOS PERSONALES OBLIGATORIOS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = TxGoldBrass
                )
            }
            item {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nombre y Apellido") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cedulaDni,
                        onValueChange = { cedulaDni = it },
                        label = { Text("Cédula / DNI") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono WhatsApp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "2. DATOS DE LA MOTO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = TxGoldBrass
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bikeBrand,
                        onValueChange = { bikeBrand = it },
                        label = { Text("Marca (ej: Keeway)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bikeModel,
                        onValueChange = { bikeModel = it },
                        label = { Text("Modelo (ej: TX 200 SM)") },
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = bikePlate,
                    onValueChange = { bikePlate = it },
                    label = { Text("Placa") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "3. FICHA MÉDICA & EMERGENCIA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = StatusError
                )
            }
            item {
                Box {
                    OutlinedButton(
                        onClick = { showBloodMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grupo Sanguíneo: $bloodType", fontWeight = FontWeight.Bold, color = StatusError)
                    }
                    DropdownMenu(
                        expanded = showBloodMenu,
                        onDismissRequest = { showBloodMenu = false }
                    ) {
                        bloodOptions.forEach { b ->
                            DropdownMenuItem(
                                text = { Text("🩸 $b") },
                                onClick = {
                                    bloodType = b
                                    showBloodMenu = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emergencyContactName,
                        onValueChange = { emergencyContactName = it },
                        label = { Text("Nombre Contacto Emergencia") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = emergencyContactPhone,
                        onValueChange = { emergencyContactPhone = it },
                        label = { Text("Telf. de Emergencia") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "4. SINCRONIZACIÓN CON GOOGLE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = TxGoldBrass
                )
            }
            item {
                if (isGoogleSynced) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StatusSuccess.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusSuccess),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                            Text("Cuenta vinculada a Google correctamente.", color = StatusSuccess, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = { 
                            if (!isGoogleLoading) {
                                isGoogleLoading = true
                                googleSignInClient.signOut().addOnCompleteListener {
                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                }
                            }
                        },
                        enabled = !isGoogleLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            disabledContainerColor = Color(0xFFE0E0E0)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = TxFlameRed,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Conectando con Google...", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = TxFlameRed, modifier = Modifier.size(24.dp))
                                Text("Vincular Cuenta de Google (Obligatorio)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
                item {
                    Text(
                        text = errorMessage,
                        color = StatusError,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        if (fullName.isBlank() || phone.isBlank() || cedulaDni.isBlank() || bikePlate.isBlank()) {
                            errorMessage = "Por favor completa todos los campos obligatorios."
                            return@Button
                        }
                        
                        if (!isGoogleSynced) {
                            errorMessage = "Debes vincular tu cuenta de Google antes de continuar."
                            return@Button
                        }

                        errorMessage = ""
                        val updatedMember = currentMember.copy(
                            fullName = fullName,
                            phone = phone,
                            cedulaDni = cedulaDni,
                            bikeBrand = bikeBrand,
                            bikeModel = bikeModel,
                            bikePlate = bikePlate,
                            bloodType = bloodType,
                            emergencyContactName = emergencyContactName,
                            emergencyContactPhone = emergencyContactPhone
                        )
                        onSaveProfile(updatedMember)
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("GUARDAR Y ENTRAR AL TEAM", fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }

            item {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TxGoldBrass.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TxGoldBrass, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("INGRESAR Y COMPLETAR EN CARNET TX", color = TxGoldBrass, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
