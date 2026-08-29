package com.example

import com.aistudio.teamtxvzla.BuildConfig
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.ui.theme.MotoOrangePrimary
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object GestorActualizaciones {
    private const val TAG = "OTA_UPDATER"

    data class InformacionOta(
        val versionCode: Int = 0,
        val urlDescarga: String = "",
        val notas: String = ""
    )

    /**
     * Consulta Firestore en la colección 'configuracion' / documento 'OTA'
     * con fallback automático a GitHub CDN (ota_info.json)
     */
    suspend fun verificarActualizacion(): InformacionOta? = withContext(Dispatchers.IO) {
        // 1. Intentar por Firestore (con timeout de 4 segundos)
        try {
            kotlinx.coroutines.withTimeoutOrNull(4000L) {
                // Garantizar sesión Firebase Auth previa para evitar PERMISSION_DENIED por reglas
                try {
                    if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
                        com.aistudio.teamtxvzla.nube.AutenticacionNube.inicializar()
                    }
                } catch (_: Exception) {}

                val db = FirebaseFirestore.getInstance()
                var doc = db.collection("configuracion").document("OTA").get().await()
                if (!doc.exists()) {
                    // Intento alternativo con mayúscula por si acaso
                    doc = db.collection("Configuracion").document("OTA").get().await()
                }

                if (doc.exists()) {
                    val code = when (val rawCode = doc.get("versionCode")) {
                        is Number -> rawCode.toInt()
                        is String -> rawCode.trim().toIntOrNull() ?: 0
                        else -> 0
                    }
                    val url = (doc.getString("urlDescarga") ?: "").trim()
                    val notas = doc.getString("notas") ?: ""

                    if (code > 0 && url.isNotBlank()) {
                        Log.d(TAG, "✅ OTA obtenido desde Firestore -> code: $code, url: $url")
                        return@withTimeoutOrNull InformacionOta(
                            versionCode = code,
                            urlDescarga = url,
                            notas = notas
                        )
                    }
                }
                null
            }?.let { return@withContext it }
        } catch (e: Exception) {
            Log.w(TAG, "Advertencia consultando Firestore OTA: ${e.message}. Probando fallback GitHub...")
        }

        // 2. Fallback de Alta Disponibilidad: GitHub CDN directo (ota_info.json)
        try {
            val urlsFallback = listOf(
                "https://raw.githubusercontent.com/EDJMM91/TeamTX-Workspace-Completo/main/apk/ota_info.json",
                "https://github.com/EDJMM91/TeamTX-Workspace-Completo/raw/main/apk/ota_info.json"
            )

            for (urlStr in urlsFallback) {
                try {
                    val url = URL(urlStr)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        instanceFollowRedirects = true
                    }
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
                        conn.disconnect()

                        val json = org.json.JSONObject(jsonString)
                        val code = json.optInt("versionCode", 0)
                        val urlDescarga = json.optString("urlDescarga", "").trim()
                        val notas = json.optString("notas", "")

                        if (code > 0 && urlDescarga.isNotBlank()) {
                            Log.d(TAG, "✅ OTA obtenido desde GitHub CDN -> code: $code, url: $urlDescarga")
                            return@withContext InformacionOta(
                                versionCode = code,
                                urlDescarga = urlDescarga,
                                notas = notas
                            )
                        }
                    }
                    conn.disconnect()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en fallback GitHub OTA: ${e.message}")
        }

        Log.w(TAG, "No se pudo obtener información de actualización de ninguna fuente")
        return@withContext null
    }

    /**
     * Descarga directa vía HTTP con seguimiento de redirecciones y reporte de progreso en tiempo real
     */
    suspend fun descargarApkDirecto(
        context: Context,
        urlOriginal: String,
        onProgreso: (progreso: Float, leidosMb: String, totalMb: String) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            var urlString = urlOriginal.trim()
            if (urlString.isBlank()) {
                Log.e(TAG, "URL de descarga vacía")
                return@withContext null
            }

            Log.d(TAG, "Iniciando descarga directa desde: $urlString")

            // Preparar destino
            val carpetaDestino = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            if (!carpetaDestino.exists()) {
                carpetaDestino.mkdirs()
            }
            val archivoApk = File(carpetaDestino, "TeamTX_Update.apk")
            if (archivoApk.exists()) {
                archivoApk.delete()
            }

            // Manejo de Redirecciones HTTP (GitHub raw 302 -> objects.githubusercontent.com)
            var conexion: HttpURLConnection? = null
            var redirecciones = 0
            var conectada = false

            while (!conectada && redirecciones < 6) {
                val url = URL(urlString)
                conexion = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 20000
                    readTimeout = 45000
                    setRequestProperty("User-Agent", "TeamTX-Updater/1.0 (Android)")
                    setRequestProperty("Accept", "*/*")
                }

                val codigoRespuesta = conexion.responseCode
                if (codigoRespuesta in listOf(
                        HttpURLConnection.HTTP_MOVED_PERM,
                        HttpURLConnection.HTTP_MOVED_TEMP,
                        HttpURLConnection.HTTP_SEE_OTHER,
                        307,
                        308
                    )
                ) {
                    val nuevaUrl = conexion.getHeaderField("Location")
                    conexion.disconnect()
                    if (nuevaUrl != null) {
                        urlString = nuevaUrl
                        redirecciones++
                        Log.d(TAG, "Siguiendo redirección #$redirecciones -> $urlString")
                    } else {
                        break
                    }
                } else if (codigoRespuesta == HttpURLConnection.HTTP_OK) {
                    conectada = true
                } else {
                    Log.e(TAG, "Código HTTP no satisfactorio: $codigoRespuesta")
                    conexion.disconnect()
                    return@withContext null
                }
            }

            if (!conectada || conexion == null) {
                Log.e(TAG, "No se pudo establecer conexión válida para descarga")
                return@withContext null
            }

            val longitudTotal = conexion.contentLengthLong
            val totalMbStr = if (longitudTotal > 0) String.format("%.1f MB", longitudTotal / (1024f * 1024f)) else "Desconocido"

            val entrada = conexion.inputStream
            val salida = FileOutputStream(archivoApk)
            val buffer = ByteArray(8 * 1024)
            var bytesLeidos: Long = 0
            var bytesActuales: Int

            while (entrada.read(buffer).also { bytesActuales = it } != -1) {
                salida.write(buffer, 0, bytesActuales)
                bytesLeidos += bytesActuales
                if (longitudTotal > 0) {
                    val progreso = (bytesLeidos.toFloat() / longitudTotal.toFloat()).coerceIn(0f, 1f)
                    val leidosMbStr = String.format("%.1f MB", bytesLeidos / (1024f * 1024f))
                    withContext(Dispatchers.Main) {
                        onProgreso(progreso, leidosMbStr, totalMbStr)
                    }
                }
            }

            salida.flush()
            salida.close()
            entrada.close()
            conexion.disconnect()

            if (archivoApk.exists() && archivoApk.length() > 100 * 1024) {
                Log.d(TAG, "Descarga completada exitosamente: ${archivoApk.absolutePath} (${archivoApk.length()} bytes)")
                withContext(Dispatchers.Main) {
                    onProgreso(1f, totalMbStr, totalMbStr)
                }
                archivoApk
            } else {
                Log.e(TAG, "El archivo descargado es demasiado pequeño o no existe (${archivoApk.length()} bytes)")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la descarga del APK: ${e.message}", e)
            null
        }
    }

    /**
     * Lanza el instalador oficial de Android usando FileProvider y validación de permisos
     */
    fun instalarApk(context: Context, archivoApk: File) {
        try {
            if (!archivoApk.exists()) {
                Toast.makeText(context, "El archivo de actualización no se encontró.", Toast.LENGTH_SHORT).show()
                return
            }

            // Validar permiso de instalar aplicaciones desconocidas en Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(
                        context,
                        "Por favor autoriza la instalación de actualizaciones para Team TX",
                        Toast.LENGTH_LONG
                    ).show()
                    val intentPermiso = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intentPermiso)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                archivoApk
            )

            val intentInstalar = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intentInstalar)
            Log.d(TAG, "Intent de instalación lanzado con URI: $apkUri")
        } catch (e: Exception) {
            Log.e(TAG, "Error lanzando el instalador: ${e.message}", e)
            Toast.makeText(context, "Error al iniciar el instalador: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Alternativa: Descarga directa abriendo el navegador predeterminado
     */
    fun abrirDescargaEnNavegador(context: Context, url: String) {
        try {
            val urlLimpia = url.trim()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlLimpia)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error abriendo navegador: ${e.message}")
            Toast.makeText(context, "No se pudo abrir el enlace en el navegador.", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Diálogo interactivo con barra de progreso en vivo para la descarga e instalación OTA
 */
@Composable
fun DialogoProgresoDescargaOta(
    infoOta: GestorActualizaciones.InformacionOta,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var progreso by remember { mutableFloatStateOf(0f) }
    var textoLeidos by remember { mutableStateOf("Iniciando...") }
    var textoTotal by remember { mutableStateOf("") }
    var estadoDescarga by remember { mutableStateOf("Descargando actualización...") }
    var huboError by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val archivo = GestorActualizaciones.descargarApkDirecto(
            context = context,
            urlOriginal = infoOta.urlDescarga,
            onProgreso = { p, leidos, total ->
                progreso = p
                textoLeidos = leidos
                textoTotal = total
                estadoDescarga = "Descargando: ${(p * 100).toInt()}%"
            }
        )

        if (archivo != null) {
            estadoDescarga = "¡Descarga completada! Abriendo instalador..."
            kotlinx.coroutines.delay(500)
            GestorActualizaciones.instalarApk(context, archivo)
            onDismiss()
        } else {
            huboError = true
            mensajeError = "No se pudo descargar el archivo automáticamente. Verifica tu conexión o descarga desde el navegador."
        }
    }

    Dialog(onDismissRequest = { if (huboError) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E2433),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        if (huboError) Icons.Default.ErrorOutline else Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = if (huboError) Color(0xFFEF4444) else MotoOrangePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = if (huboError) "Error de Descarga" else "Actualización Team TX",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                if (!huboError) {
                    Text(
                        text = estadoDescarga,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    LinearProgressIndicator(
                        progress = { progreso },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MotoOrangePrimary,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "$textoLeidos / $textoTotal", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "${(progreso * 100).toInt()}%", fontSize = 11.sp, color = MotoOrangePrimary, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = mensajeError,
                        fontSize = 13.sp,
                        color = Color(0xFFEF4444)
                    )

                    Button(
                        onClick = {
                            GestorActualizaciones.abrirDescargaEnNavegador(context, infoOta.urlDescarga)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Descargar en Navegador")
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Cerrar", color = Color.Gray)
                    }
                }
            }
        }
    }
}

/**
 * Verificador automático en el inicio de la app
 */
@Composable
fun VerificadorOta() {
    val context = LocalContext.current
    var infoOta by remember { mutableStateOf<GestorActualizaciones.InformacionOta?>(null) }
    var mostrarDialogoConfirmacion by remember { mutableStateOf(false) }
    var mostrarDialogoDescarga by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val ota = GestorActualizaciones.verificarActualizacion()
        if (ota != null) {
            val currentVersionCode = BuildConfig.VERSION_CODE
            Log.d("OTA", "versionCode nube=${ota.versionCode} vs local=$currentVersionCode")
            if (ota.versionCode > currentVersionCode) {
                infoOta = ota
                mostrarDialogoConfirmacion = true
            }
        }
    }

    if (mostrarDialogoConfirmacion && infoOta != null) {
        AlertDialog(
            onDismissRequest = { /* No cerrar para no ignorar */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Update, contentDescription = null, tint = MotoOrangePrimary)
                    Text(
                        text = "¡Nueva Versión Disponible!",
                        fontWeight = FontWeight.Bold,
                        color = MotoOrangePrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Se ha publicado una nueva actualización importante para la aplicación.")
                    if (infoOta!!.notas.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Novedades:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = infoOta!!.notas,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoConfirmacion = false
                        mostrarDialogoDescarga = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MotoOrangePrimary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Actualizar Ahora")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { mostrarDialogoConfirmacion = false }) {
                    Text("Más tarde")
                }
            }
        )
    }

    if (mostrarDialogoDescarga && infoOta != null) {
        DialogoProgresoDescargaOta(
            infoOta = infoOta!!,
            onDismiss = { mostrarDialogoDescarga = false }
        )
    }
}
