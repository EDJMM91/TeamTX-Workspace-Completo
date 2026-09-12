package com.example

import com.aistudio.teamtxvzla.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import net.osmand.plus.OsmandApplication

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TeamTxApplication : OsmandApplication() {
    companion object {
        var instance: TeamTxApplication? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // 🛡️ Registro preventivo de excepciones para diagnóstico de mapa y navegación
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("TEAM_TX_CRASH", "💥 EXCEPCIÓN NO CONTROLADA en [${thread.name}]: ${throwable.message}", throwable)
            try {
                FirebaseCrashlytics.getInstance().recordException(throwable)
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            
            // Enable Firestore offline persistence
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(104857600L) // 100 MB cache size
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
            
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            FirebaseCrashlytics.getInstance().setCustomKey("app_version", BuildConfig.VERSION_NAME)
            
            // 🔐 Inicializar módulo centralizado de autenticación
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                com.aistudio.teamtxvzla.nube.AutenticacionNube.inicializar()
            }
            
            Log.i("TEAM_TX", "✅ Firebase inicializado correctamente")
        } catch (e: Exception) {
            Log.e("TEAM_TX", "❌ Error crítico inicializando Firebase", e)
        }
    }
}
