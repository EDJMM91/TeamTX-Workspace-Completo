package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.io.File

/**
 * Gestor modular para captura de fotos con la cámara y selección de imágenes desde la galería.
 * Compatible tanto con MainActivity (Compose) como con MapActivity (OsmAnd).
 * No requiere registrar nuevas actividades en el AndroidManifest.
 */
object GestorSelectorFoto {

    private const val TAG = "TAG_COMERCIOS_TX"
    private const val FRAGMENT_TAG = "HeadlessImagePickerFragment"

    class ImagePickerFragment : Fragment() {
        private var onImagePickedCallback: ((Uri?) -> Unit)? = null
        private var tempCameraUri: Uri? = null

        private val galleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            val callback = onImagePickedCallback
            onImagePickedCallback = null
            detachSelf()
            callback?.invoke(uri)
        }

        private val cameraLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success: Boolean ->
            val callback = onImagePickedCallback
            val uri = if (success) tempCameraUri else null
            onImagePickedCallback = null
            tempCameraUri = null
            detachSelf()
            callback?.invoke(uri)
        }

        fun seleccionarGaleria(callback: (Uri?) -> Unit) {
            this.onImagePickedCallback = callback
            try {
                galleryLauncher.launch("image/*")
            } catch (e: Exception) {
                Log.e(TAG, "Error lanzando galería: ${e.message}")
                callback(null)
                detachSelf()
            }
        }

        fun tomarFoto(callback: (Uri?) -> Unit) {
            this.onImagePickedCallback = callback
            try {
                val ctx = context ?: activity
                if (ctx == null) {
                    callback(null)
                    detachSelf()
                    return
                }
                val tempFile = File(ctx.cacheDir, "foto_tx_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(
                    ctx,
                    "${ctx.packageName}.fileprovider",
                    tempFile
                )
                this.tempCameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Log.e(TAG, "Error lanzando cámara: ${e.message}")
                callback(null)
                detachSelf()
            }
        }

        private fun detachSelf() {
            try {
                parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
            } catch (_: Exception) {}
        }
    }

    /**
     * Lanza la galería para seleccionar una imagen.
     */
    fun seleccionarDeGaleria(activity: Activity, onFotoSeleccionada: (Uri?) -> Unit) {
        val fragActivity = activity as? FragmentActivity
        if (fragActivity != null && !fragActivity.isFinishing && !fragActivity.isDestroyed) {
            try {
                val fm = fragActivity.supportFragmentManager
                var fragment = fm.findFragmentByTag(FRAGMENT_TAG) as? ImagePickerFragment
                if (fragment == null) {
                    fragment = ImagePickerFragment()
                    fm.beginTransaction().add(fragment, FRAGMENT_TAG).commitNowAllowingStateLoss()
                }
                fragment.seleccionarGaleria(onFotoSeleccionada)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Fallo al usar Fragment headless para galería: ${e.message}")
            }
        }

        // Fallback genérico mediante Intent si no es FragmentActivity
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            activity.startActivity(Intent.createChooser(intent, "Selecciona una imagen"))
        } catch (e: Exception) {
            Log.e(TAG, "Error en selector de galería: ${e.message}")
            onFotoSeleccionada(null)
        }
    }

    /**
     * Lanza la cámara nativa para tomar una foto.
     */
    fun tomarFotoCamara(activity: Activity, onFotoCapturada: (Uri?) -> Unit) {
        val fragActivity = activity as? FragmentActivity
        if (fragActivity != null && !fragActivity.isFinishing && !fragActivity.isDestroyed) {
            try {
                val fm = fragActivity.supportFragmentManager
                var fragment = fm.findFragmentByTag(FRAGMENT_TAG) as? ImagePickerFragment
                if (fragment == null) {
                    fragment = ImagePickerFragment()
                    fm.beginTransaction().add(fragment, FRAGMENT_TAG).commitNowAllowingStateLoss()
                }
                fragment.tomarFoto(onFotoCapturada)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Fallo al usar Fragment headless para cámara: ${e.message}")
            }
        }

        try {
            val tempFile = File(activity.cacheDir, "foto_tx_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                tempFile
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error en captura con cámara: ${e.message}")
            onFotoCapturada(null)
        }
    }

    /**
     * Decodifica de forma segura un Uri local a un Bitmap optimizado para vista previa sin sobrecargar memoria.
     */
    fun cargarBitmapOptimizado(context: Context, uri: Uri, maxDim: Int = 512): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            var scale = 1
            while (options.outWidth / scale / 2 >= maxDim && options.outHeight / scale / 2 >= maxDim) {
                scale *= 2
            }

            val readStream = context.contentResolver.openInputStream(uri) ?: return null
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = scale }
            val bitmap = BitmapFactory.decodeStream(readStream, null, decodeOptions)
            readStream.close()
            bitmap
        } catch (e: Exception) {
            Log.w(TAG, "Error optimizando bitmap: ${e.message}")
            null
        }
    }
}
