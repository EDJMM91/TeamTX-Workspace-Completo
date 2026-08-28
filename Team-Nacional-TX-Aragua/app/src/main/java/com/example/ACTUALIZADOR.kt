package com.example

import com.aistudio.teamtxvzla.BuildConfig
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MotoOrangePrimary
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.File

object GestorActualizaciones {
    private var downloadId: Long = -1L
    private var onCompleteReceiver: BroadcastReceiver? = null

    data class InformacionOta(
        val versionCode: Int = 0,
        val urlDescarga: String = "",
        val notas: String = ""
    )

    suspend fun verificarActualizacion(): InformacionOta? {
        return try {
            val db = FirebaseFirestore.getInstance()
            // Ruta EXACTA en Firestore: coleccion 'configuracion' (minusculas), documento 'OTA' (mayusculas)
            val documento = db.collection("configuracion").document("OTA").get().await()
            if (documento.exists()) {
                // Lectura explicita: versionCode SIEMPRE numerico para comparar contra BuildConfig.VERSION_CODE
                InformacionOta(
                    versionCode = documento.getLong("versionCode")?.toInt() ?: 0,
                    urlDescarga = documento.getString("urlDescarga") ?: "",
                    notas = documento.getString("notas") ?: ""
                )
            } else {
                Log.w("OTA", "El documento configuracion/OTA no existe o esta vacio en Firestore")
                null
            }
        } catch (e: Exception) {
            Log.e("OTA", "Error verificando OTA en Firestore (configuracion/OTA)", e)
            null
        }
    }

    fun descargarEInstalarApk(context: Context, url: String) {
        val uri = Uri.parse(url)
        val request = DownloadManager.Request(uri).apply {
            setTitle("Actualización Team TX")
            setDescription("Descargando la nueva versión de la aplicación...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            
            // Guardar en la carpeta de descargas pública para que el instalador tenga acceso
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "TeamTX_Update.apk"
            )
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
        Toast.makeText(context, "Descargando actualización...", Toast.LENGTH_SHORT).show()

        // Registrar el BroadcastReceiver para saber cuando termine la descarga
        if (onCompleteReceiver == null) {
            onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        iniciarInstalador(context, downloadManager, downloadId)
                        // Desregistrar para no dejar memoria colgada
                        context.unregisterReceiver(this)
                        onCompleteReceiver = null
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        }
    }

    private fun iniciarInstalador(context: Context, downloadManager: DownloadManager, downloadId: Long) {
        try {
            val apkUri = downloadManager.getUriForDownloadedFile(downloadId)
            if (apkUri != null) {
                val intentInstalar = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intentInstalar)
            } else {
                Toast.makeText(context, "Error al ubicar el archivo descargado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("OTA", "Error al iniciar la instalación: ${e.message}")
            Toast.makeText(context, "No se pudo iniciar el instalador automáticamente.", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun VerificadorOta() {
    val context = LocalContext.current
    var infoOta by remember { mutableStateOf<GestorActualizaciones.InformacionOta?>(null) }
    var mostrarDialogo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val ota = GestorActualizaciones.verificarActualizacion()
        if (ota != null) {
            val currentVersionCode = BuildConfig.VERSION_CODE
            Log.d("OTA", "versionCode nube=${ota.versionCode} vs local=$currentVersionCode")
            if (ota.versionCode > currentVersionCode) {
                infoOta = ota
                mostrarDialogo = true
            }
        }
    }

    if (mostrarDialogo && infoOta != null) {
        AlertDialog(
            onDismissRequest = { /* Para hacerlo forzoso, no cerramos si hacen clic fuera */ },
            title = {
                Text(
                    text = "¡Nueva Versión Disponible!",
                    fontWeight = FontWeight.Bold,
                    color = MotoOrangePrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Se ha encontrado una actualización importante para la aplicación.")
                    if (infoOta!!.notas.isNotBlank()) {
                        Text(
                            text = "Novedades: ${infoOta!!.notas}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Al presionar descargar, se bajará el archivo y se te pedirá permiso para instalarlo.",
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogo = false
                        GestorActualizaciones.descargarEInstalarApk(context, infoOta!!.urlDescarga)
                    }
                ) {
                    Text("Descargar e Instalar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { mostrarDialogo = false }) {
                    Text("Más tarde")
                }
            }
        )
    }
}
