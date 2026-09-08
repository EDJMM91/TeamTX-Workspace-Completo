package com.example.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.data.model.MemberProfile
import com.example.data.model.MemberRole
import com.example.data.remote.FirestoreUtil
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * SOLICITUDES_INGRESO.kt - Panel de Gobernanza para Directiva y Super Admin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudesIngreso(
    userUid: String,
    onUsuarioAprobado: (MemberProfile) -> Unit,
    onVolver: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var listaPendientes by remember { mutableStateOf<List<MemberProfile>>(emptyList()) }
    var usuarioSeleccionado by remember { mutableStateOf<MemberProfile?>(null) }
    var nuevoRolSeleccionado by remember { mutableStateOf(MemberRole.MIEMBRO_ACTIVO) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val pendingUsersFlow = remember { FirestoreUtil.obtenerUsuariosConEstado("PENDIENTE") }
    val pendientesList by pendingUsersFlow.collectAsState(initial = emptyList())

    LaunchedEffect(pendientesList) {
        listaPendientes = pendientesList
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Panel de Gobernanza",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )

                Button(
                    onClick = onVolver,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Cerrar", fontWeight = FontWeight.Medium, color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }

            Text(
                text = "Solicitudes en Espera de Aprobación (${listaPendientes.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            if (listaPendientes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay solicitudes pendientes actualmente.",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(listaPendientes) { perfil ->
                        TarjetaUsuarioSolicitud(
                            perfil = perfil,
                            isSelected = usuarioSeleccionado?.id == perfil.id,
                            onSelect = { usuarioSeleccionado = perfil }
                        )
                    }
                }
            }

            usuarioSeleccionado?.let { perfil ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Aprobar Solicitud: ${perfil.fullName}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Text("Selecciona el Rol Asignado:", fontSize = 12.sp, color = Color(0xFF64748B))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = nuevoRolSeleccionado == MemberRole.MIEMBRO_ACTIVO,
                                onClick = { nuevoRolSeleccionado = MemberRole.MIEMBRO_ACTIVO },
                                label = { Text("Piloto TX") }
                            )
                            FilterChip(
                                selected = nuevoRolSeleccionado == MemberRole.ASPIRANTE,
                                onClick = { nuevoRolSeleccionado = MemberRole.ASPIRANTE },
                                label = { Text("Aspirante") }
                            )
                            FilterChip(
                                selected = nuevoRolSeleccionado == MemberRole.PRESIDENTE,
                                onClick = { nuevoRolSeleccionado = MemberRole.PRESIDENTE },
                                label = { Text("Directiva") }
                            )
                        }

                        if (!errorMessage.isNullOrBlank()) {
                            Text(errorMessage!!, color = Color(0xFFEF5350), fontSize = 12.sp)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    isLoading = true
                                    coroutineScope.launch {
                                        try {
                                            val uidDoc = (perfil.firebaseUid?.ifBlank { null } ?: perfil.email ?: "").trim()
                                            val idMember = perfil.id.takeIf { it > 0 } ?: System.currentTimeMillis()
                                            val aprobado = perfil.copy(
                                                id = idMember,
                                                role = nuevoRolSeleccionado,
                                                isDirectiva = nuevoRolSeleccionado == MemberRole.PRESIDENTE || nuevoRolSeleccionado == MemberRole.DESARROLLADOR,
                                                solvencyStatus = true,
                                                isSuspended = false
                                            )

                                            if (uidDoc.isNotBlank()) {
                                                val updateData = mapOf(
                                                    "id" to idMember,
                                                    "fullName" to aprobado.fullName,
                                                    "nickname" to aprobado.nickname,
                                                    "memberNumber" to aprobado.memberNumber,
                                                    "email" to aprobado.email,
                                                    "firebaseUid" to uidDoc,
                                                    "bikeModel" to aprobado.bikeModel,
                                                    "bikePlate" to aprobado.bikePlate,
                                                    "bloodType" to aprobado.bloodType,
                                                    "emergencyContactPhone" to aprobado.emergencyContactPhone,
                                                    "role" to nuevoRolSeleccionado.name,
                                                    "isDirectiva" to (nuevoRolSeleccionado == MemberRole.PRESIDENTE || nuevoRolSeleccionado == MemberRole.DESARROLLADOR),
                                                    "usuarioEstado" to "ACTIVO",
                                                    "estado" to "ACTIVO",
                                                    "solvencyStatus" to true,
                                                    "isSuspended" to false,
                                                    "lastActiveTimestamp" to System.currentTimeMillis()
                                                )
                                                try {
                                                    db.collection("usuarios").document(uidDoc).set(updateData, com.google.firebase.firestore.SetOptions.merge()).await()
                                                } catch (e: Exception) {
                                                    Log.w("SOLICITUDES", "Error escribiendo usuarios/$uidDoc: ${e.message}")
                                                }
                                                try {
                                                    db.collection("users").document(uidDoc).set(updateData, com.google.firebase.firestore.SetOptions.merge()).await()
                                                } catch (_: Exception) {}
                                                try {
                                                    db.collection("members").document(idMember.toString()).set(updateData, com.google.firebase.firestore.SetOptions.merge()).await()
                                                } catch (_: Exception) {}

                                                // Registrar/actualizar vinculación 1-a-1 en vinculos_google
                                                if (!aprobado.email.isNullOrBlank()) {
                                                    val vinc = com.example.data.remote.VinculacionGoogle(
                                                        email = aprobado.email!!.trim().lowercase(),
                                                        correo = aprobado.email!!.trim().lowercase(),
                                                        firebaseUid = uidDoc,
                                                        uid_firebase = uidDoc,
                                                        memberNumber = aprobado.memberNumber,
                                                        numero_miembro = aprobado.memberNumber,
                                                        memberName = aprobado.fullName,
                                                        nombre_piloto = aprobado.fullName,
                                                        memberId = idMember,
                                                        linkedAt = System.currentTimeMillis(),
                                                        lastActiveTimestamp = System.currentTimeMillis()
                                                    )
                                                    com.example.data.remote.PerfilNube.registrarVinculacion(vinc)
                                                }
                                            }

                                            onUsuarioAprobado(aprobado)
                                            usuarioSeleccionado = null
                                            isLoading = false
                                        } catch (e: Exception) {
                                            Log.e("SOLICITUDES", "Error aprobando: ${e.message}")
                                            errorMessage = "Error al aprobar: ${e.message}"
                                            isLoading = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                else Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("APROBAR")
                            }

                            Button(
                                onClick = { usuarioSeleccionado = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CANCELAR")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TarjetaUsuarioSolicitud(
    perfil: MemberProfile,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFFD97706) else Color(0xFFE2E8F0)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = perfil.fullName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Correo: ${perfil.email} | Moto: ${perfil.bikeModel}",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
            Text(
                text = "Placa: ${perfil.bikePlate} | Sangre: ${perfil.bloodType} | SOS: ${perfil.emergencyContactPhone}",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}
