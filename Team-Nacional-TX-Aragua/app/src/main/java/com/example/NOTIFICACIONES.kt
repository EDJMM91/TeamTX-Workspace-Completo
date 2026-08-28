package com.example

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.messaging.FirebaseMessaging

object GestorNotificaciones {
    fun suscribirATopico(topico: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topico)
            .addOnCompleteListener { tarea ->
                var msj = "Suscrito al tópico: $topico"
                if (!tarea.isSuccessful) {
                    msj = "Falló suscripción al tópico: $topico"
                }
                Log.d("FCM_TOPICS", msj)
            }
    }

    fun desuscribirDeTopico(topico: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topico)
            .addOnCompleteListener { tarea ->
                var msj = "Desuscrito del tópico: $topico"
                if (!tarea.isSuccessful) {
                    msj = "Falló desuscripción del tópico: $topico"
                }
                Log.d("FCM_TOPICS", msj)
            }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SolicitadorNotificaciones() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val estadoPermiso = rememberPermissionState(
            permission = Manifest.permission.POST_NOTIFICATIONS
        )

        LaunchedEffect(Unit) {
            if (!estadoPermiso.status.isGranted) {
                estadoPermiso.launchPermissionRequest()
            } else {
                // Si ya están concedidos, suscribirse a tópicos globales por defecto
                GestorNotificaciones.suscribirATopico("global")
                GestorNotificaciones.suscribirATopico("alertas_sos")
            }
        }
        
        // También suscribirse si el permiso se concede tras la solicitud
        LaunchedEffect(estadoPermiso.status.isGranted) {
            if (estadoPermiso.status.isGranted) {
                GestorNotificaciones.suscribirATopico("global")
                GestorNotificaciones.suscribirATopico("alertas_sos")
            }
        }
    } else {
        // En Android 12 y anteriores no se pide permiso en runtime para notificaciones,
        // simplemente nos suscribimos.
        LaunchedEffect(Unit) {
            GestorNotificaciones.suscribirATopico("global")
            GestorNotificaciones.suscribirATopico("alertas_sos")
        }
    }
}
