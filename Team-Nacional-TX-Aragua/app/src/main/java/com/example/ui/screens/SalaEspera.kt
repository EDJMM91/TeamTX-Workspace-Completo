package com.example.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * SALA_ESPERA.kt - Pantalla de cuarentena para usuarios PENDIENTE o RECHAZADO.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaEspera(
    onVerificarYRedirigir: () -> Unit,
    userUid: String,
    initialState: String = "PENDIENTE"
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    var estadoUsuario by remember(userUid) { mutableStateOf(initialState) }
    var tiempoEspera by remember { mutableStateOf(0) }

    DisposableEffect(userUid) {
        if (userUid.isBlank()) return@DisposableEffect onDispose {}
        val listenerUsuarios = db.collection("usuarios").document(userUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SALA_ESPERA", "Error escuchando 'usuarios': ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val nuevoEstado = snapshot.getString("usuarioEstado") ?: snapshot.getString("estado") ?: "PENDIENTE"
                    if (nuevoEstado != estadoUsuario) {
                        estadoUsuario = nuevoEstado
                        if (nuevoEstado == "ACTIVO") {
                            coroutineScope.launch {
                                delay(300)
                                onVerificarYRedirigir()
                            }
                        }
                    }
                }
            }

        val listenerUsers = db.collection("users").document(userUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val nuevoEstado = snapshot.getString("usuarioEstado") ?: snapshot.getString("estado") ?: "PENDIENTE"
                    if (nuevoEstado != estadoUsuario) {
                        estadoUsuario = nuevoEstado
                        if (nuevoEstado == "ACTIVO") {
                            coroutineScope.launch {
                                delay(300)
                                onVerificarYRedirigir()
                            }
                        }
                    }
                }
            }

        onDispose {
            listenerUsuarios.remove()
            listenerUsers.remove()
        }
    }

    LaunchedEffect(estadoUsuario) {
        while (estadoUsuario == "PENDIENTE" || estadoUsuario == "RECHAZADO") {
            delay(1000)
            tiempoEspera++
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (estadoUsuario == "ACTIVO") Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                contentDescription = null,
                modifier = Modifier.size(110.dp),
                tint = if (estadoUsuario == "ACTIVO") Color(0xFF4CAF50) else Color(0xFFFFC107)
            )

            Text(
                text = if (estadoUsuario == "ACTIVO") "Cuenta Aprobada" else "Cuenta en Evaluación",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )

            Text(
                text = if (estadoUsuario == "ACTIVO")
                    "¡Bienvenido al Team Nacional TX Aragua! Ya puedes ingresar al sistema."
                else
                    "Tu perfil está siendo evaluado por la Directiva. Te notificaremos al ser aprobado.",
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (estadoUsuario != "ACTIVO") {
                val minutos = tiempoEspera / 60
                val segundos = tiempoEspera % 60
                Text(
                    text = "Llevas ${minutos}m ${segundos}s en sala de espera.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                OutlinedButton(
                    onClick = {
                        Log.i("SALA_ESPERA", "Solicitando re-evaluación para $userUid")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFFD97706)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Solicitar Re-Evaluación", fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        onVerificarYRedirigir()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFF64748B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cerrar Sesión e Ingresar con otra cuenta", fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                    }
                }
            }
        }
    }
}
