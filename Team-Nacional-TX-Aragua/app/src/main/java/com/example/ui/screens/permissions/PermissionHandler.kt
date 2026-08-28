package com.example.ui.screens.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    onAllPermissionsGranted: () -> Unit,
    scope: CoroutineScope
) {
    val permissions = mutableListOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CALL_PHONE,
        android.Manifest.permission.CAMERA
    )
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
        permissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)
        permissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    
    val multiplePermissionsState = rememberMultiplePermissionsState(permissions)
    var showRationale by remember { mutableStateOf(false) }

    // Solicitar permisos automáticamente al entrar
    LaunchedEffect(Unit) {
        if (!multiplePermissionsState.allPermissionsGranted) {
            multiplePermissionsState.launchMultiplePermissionRequest()
        }
    }
    
    LaunchedEffect(multiplePermissionsState.allPermissionsGranted) {
        if (multiplePermissionsState.allPermissionsGranted) {
            onAllPermissionsGranted()
        }
    }
    
    if (!multiplePermissionsState.allPermissionsGranted) {
        val shouldShowRationaleForAny = multiplePermissionsState.shouldShowRationale
        
        if (shouldShowRationaleForAny && !showRationale) {
            showRationale = true
        }
        
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { 
                onAllPermissionsGranted()
            },
            title = { Text("Permisos de Navegación y Mapa TX") },
            text = { 
                Text("Para que el Mapa Integrado y las funciones del Team funcionen al 100%, se requieren los siguientes permisos:\n\n" +
                     "📍 Ubicación Precisa (GPS): Navegación guiada, búsqueda de direcciones y SOS.\n" +
                     "🎙️ Micrófono / Audio: Búsqueda por voz y notas en chat.\n" +
                     "🔔 Notificaciones: Guía de navegación en segundo plano y alertas SOS.\n" +
                     "📞 Teléfono: Llamadas rápidas de auxilio y directiva.\n" +
                     "📷 Cámara: Fotos de perfil y reportes viales.")
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        multiplePermissionsState.launchMultiplePermissionRequest()
                    }
                ) { Text("Conceder Permisos") }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        onAllPermissionsGranted()
                    }
                ) { Text("Continuar") }
            }
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionHandler(
    onPermissionGranted: () -> Unit,
    scope: CoroutineScope
) {
    val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    
    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            onPermissionGranted()
        }
    }
    
    if (!locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.shouldShowRationale) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { locationPermissionState.launchPermissionRequest() },
                title = { Text("Permiso de Ubicación") },
                text = { Text("Se necesita acceso a la ubicación precisa para:\n• Sistema SOS con GPS\n• Seguimiento de rodadas\n• Punto de encuentro") },
                confirmButton = {
                    TextButton(onClick = { locationPermissionState.launchPermissionRequest() }) { Text("Permitir") }
                },
                dismissButton = { TextButton(onClick = { locationPermissionState.launchPermissionRequest() }) { Text("Cancelar") } }
            )
        } else {
            LaunchedEffect(Unit) {
                locationPermissionState.launchPermissionRequest()
            }
        }
    }
}