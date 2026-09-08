package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemberProfile
import com.example.data.model.MemberRole
import com.google.firebase.auth.FirebaseAuth

/**
 * FORMULARIO_INGRESO.kt - Pantalla de registro inicial para nuevos usuarios.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioIngreso(
    onCompletarRegistro: (MemberProfile) -> Unit,
    onVolver: () -> Unit,
    existingMember: MemberProfile? = null,
    isEditMode: Boolean = false
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var moto by remember { mutableStateOf(existingMember?.bikeModel ?: "Keeway TX 200 SM") }
    var placa by remember { mutableStateOf(existingMember?.bikePlate ?: "") }
    var tipoSangre by remember { mutableStateOf(existingMember?.bloodType ?: "O+") }
    var contactoSOS by remember { mutableStateOf(existingMember?.emergencyContactPhone ?: "") }
    var nombreCompleto by remember { mutableStateOf(existingMember?.fullName ?: "") }
    var email by remember { mutableStateOf(existingMember?.email ?: "") }
    var selectedRole by remember { mutableStateOf(existingMember?.role ?: MemberRole.MIEMBRO_ACTIVO) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val rolesDisponibles = listOf(
        MemberRole.MIEMBRO_ACTIVO to "Miembro Activo / Piloto TX",
        MemberRole.ASPIRANTE to "Aspirante / En Prueba",
        MemberRole.CAPITAN_RUTA to "Capitán de Ruta",
        MemberRole.SEGURIDAD_VIAL to "Oficial de Seguridad Vial",
        MemberRole.MECANICO_OFICIAL to "Mecánico Oficial",
        MemberRole.PRESIDENTE to "Presidente",
        MemberRole.DESARROLLADOR to "Desarrollador / Control Supremo"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = if (isEditMode) "Editar Perfil del Piloto" else "Registro Inicial del Piloto",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A)
            )

            Text(
                text = if (isEditMode) "Actualiza tus datos del Carnet TX" else "Completa tus datos para solicitar activación de cuenta",
                fontSize = 14.sp,
                color = Color(0xFF64748B)
            )

            OutlinedTextField(
                value = nombreCompleto,
                onValueChange = { nombreCompleto = it },
                label = { Text("Nombre Completo*", fontWeight = FontWeight.Bold) },
                placeholder = { Text("Ej. Juan Pérez") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico*", fontWeight = FontWeight.Bold) },
                placeholder = { Text("ejemplo@gmail.com") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = moto,
                onValueChange = { moto = it },
                label = { Text("Modelo de Moto*", fontWeight = FontWeight.Bold) },
                placeholder = { Text("Ej. Keeway TX 200 SM") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = placa,
                onValueChange = { placa = it },
                label = { Text("Placa del Vehículo*", fontWeight = FontWeight.Bold) },
                placeholder = { Text("Ej. ABC-1234") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tipoSangre,
                onValueChange = { tipoSangre = it },
                label = { Text("Tipo de Sangre*", fontWeight = FontWeight.Bold) },
                placeholder = { Text("Ej. O+") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = contactoSOS,
                onValueChange = { contactoSOS = it },
                label = { Text("Contacto SOS / Familiar*", fontWeight = FontWeight.Bold) },
                placeholder = { Text("Ej. +58 412 1234567") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage!!,
                    fontSize = 12.sp,
                    color = Color(0xFFEF5350)
                )
            }

            Button(
                onClick = {
                    if (nombreCompleto.isBlank()) {
                        errorMessage = "El nombre completo es obligatorio."
                        return@Button
                    }
                    if (email.isBlank() || !email.contains("@")) {
                        errorMessage = "Ingresa un correo válido."
                        return@Button
                    }
                    if (moto.isBlank()) {
                        errorMessage = "Ingresa el modelo de moto."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val nuevoPerfil = MemberProfile(
                        id = existingMember?.id ?: System.currentTimeMillis(),
                        fullName = nombreCompleto.trim(),
                        nickname = nombreCompleto.trim().split(" ").firstOrNull() ?: "Piloto",
                        memberNumber = existingMember?.memberNumber ?: "TX-${(1000..9999).random()}",
                        role = selectedRole,
                        isDirectiva = selectedRole == MemberRole.PRESIDENTE || selectedRole == MemberRole.DESARROLLADOR,
                        solvencyStatus = true,
                        email = email.trim().lowercase(),
                        firebaseUid = firebaseUid,
                        avatarInitials = nombreCompleto.trim().take(2).uppercase(),
                        bikeModel = moto.trim(),
                        bikePlate = placa.trim(),
                        bloodType = tipoSangre.trim(),
                        emergencyContactPhone = contactoSOS.trim(),
                        isOnline = true,
                        lastActiveTimestamp = System.currentTimeMillis()
                    )

                    onCompletarRegistro(nuevoPerfil)
                    isLoading = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("GUARDAR Y ENVIAR SOLICITUD", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Button(
                onClick = onVolver,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFF0F172A))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Volver al Inicio", fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
            }
        }
    }
}
