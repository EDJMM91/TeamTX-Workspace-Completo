# Fases Restantes - Módulo Reproductor TX Pro

> Documento de seguimiento para continuar el trabajo después de compilar la FASE 1.
> Basado en `MODULOS_GESTOR.md` y plan aprobado.

---

## ✅ FASE 1 - Estabilidad Core (EN PROGRESO - Compilando)

**Objetivo**: Reproducción estable al pasar canción (siguiente/anterior) sin crashes, bloqueos ni silencio.

### Cambios aplicados en `GESTOR_AUDIO_TX.kt`:
- [x] `playbackMutex = Mutex()` para serializar transiciones
- [x] `liberarMediaPlayer()` ahora `suspend` + `Thread.sleep(50)` → liberación síncrona/bloqueante
- [x] `awaitLiberarMediaPlayer()` wrapper para callbacks no-suspend
- [x] `reproducirCancionInternal()` bajo `playbackMutex.withLock { }` en `Dispatchers.IO`
- [x] `siguienteCancion()` / `anteriorCancion()` reescritas bajo mutex (eliminado `isTransitioning` + `delay(400)`)
- [x] `detener()` bajo mutex
- [x] `MIN_DURACION_VALIDA_MS = 500L` (era 2000ms)
- [x] `MAX_AUTO_SKIPS = 3` (era 5)
- [x] `setOnSeekCompleteListener` agregado
- [x] `tiempoInicioReproduccionMs = 0L` reseteado ANTES de async en `reproducirCancion()`
- [x] Callbacks (`onPrepared`, `onCompletion`, `onError`) usan `scopeCoroutine.launch(Dispatchers.Main)` para updates UI
- [x] Imports: `kotlinx.coroutines.sync.Mutex`, `kotlinx.coroutines.sync.withLock`

### Verificación FASE 1:
- [ ] Compila sin errores (`./gradlew :app:compileDebugKotlin`)
- [ ] Test manual: 20 cambios rápidos siguiente/anterior → 0 crashes, 0 silencios >1s
- [ ] Logcat `TEAM_TX_REPRODUCTOR` muestra flujo limpio sin "BLOQUEADO por errorEnCurso"

---

## 🟠 FASE 2 - Service Foreground + MediaSession (PENDIENTE)

**Objetivo**: Reproducción en background real, controles lockscreen, Android Auto, Wear OS.

### Archivos nuevos:
| Archivo | Descripción |
|---------|-------------|
| `ReproductorForegroundService.kt` | `Service` + `MediaSessionCompat.Callback` (`onPlay`, `onPause`, `onSkipToNext`, `onSkipToPrevious`, `onSeekTo`) |
| `MediaSessionHelper.kt` | Helpers: `createMediaSession()`, `updateMetadata()`, `updatePlaybackState()`, `setMediaButtonReceiver()` |

### Modificaciones:
| Archivo | Cambios |
|---------|---------|
| `GESTOR_AUDIO_TX.kt` | Delegar playback (`mediaPlayer`, `audioFocus`, `estado`, `posicionActualMs`) al Service vía `MediaControllerCompat` |
| `MainActivity.kt` | `startForegroundService(Intent(this, ReproductorForegroundService::class.java))` en `onCreate()` |
| `AndroidManifest.xml` | **⚠️ REQUIERE TU AUTORIZACIÓN** `<service android:name=".reproductor.ReproductorForegroundService" android:foregroundServiceType="mediaPlayback" android:exported="false">` |

### Detalles técnicos:
```kotlin
// ReproductorForegroundService.kt estructura
class ReproductorForegroundService : Service() {
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaPlayer: MediaPlayer
    
    override fun onCreate() {
        mediaSession = MediaSessionCompat(this, "TX_PRO_SESSION").apply {
            setCallback(MediaSessionCallback())
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS 
                     or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setMediaButtonReceiver(null)
        }
        setMediaSession(mediaSession.sessionToken)
    }
    
    inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() { /* delegar a GESTOR_AUDIO_TX */ }
        override fun onPause() { /* delegar a GESTOR_AUDIO_TX */ }
        override fun onSkipToNext() { /* delegar a GESTOR_AUDIO_TX */ }
        override fun onSkipToPrevious() { /* delegar a GESTOR_AUDIO_TX */ }
        override fun onSeekTo(pos: Long) { /* delegar a GESTOR_AUDIO_TX */ }
    }
    
    fun updateNotification(cancion: CancionMotera, estado: EstadoReproductor) {
        val notification = NotificationCompat.Builder(this, CANAL_ID)
            .setStyle(NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2))
            .setSmallIcon(R.drawable.logoteam)
            // ... metadata, actions
        startForeground(NOTIFICACION_ID, notification.build())
    }
}
```

### Verificación FASE 2:
- [ ] Cerrar app (swipe recent) → música sigue sonando
- [ ] Lockscreen muestra controles + carátula + título/artista
- [ ] Auriculares BT: play/pausa/siguiente/anterior funcionan
- [ ] Android Auto detecta app de música

---

## 🟡 FASE 3 - Room Database Persistencia (PENDIENTE)

**Objetivo**: Datos sobreviven a reinicios, base para sync futura con Firebase.

### Entidades nuevas (`ModelosReproductorRoom.kt` o en `Models.kt`):
```kotlin
@Entity(tableName = "canciones_locales")
data class CancionLocalEntity(
    @PrimaryKey val id: Long,
    val titulo: String,
    val artista: String,
    val album: String,
    val duracionMs: Long,
    val rutaArchivo: String,
    val uriStr: String,
    val portadaUriStr: String?,
    val fechaAgregada: Long,
    val esFavorita: Boolean,
    val tamanoBytes: Long,
    val carpetaContenedora: String,
    val metadatosPersonalizadosJson: String?
)

@Entity(tableName = "listas_reproduccion")
data class ListaReproduccionEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val descripcion: String,
    val fechaCreacion: Long,
    val icono: String,
    val cancionIdsJson: String  // JSONArray<Long>
)

@Entity(tableName = "estado_reproductor")
data class EstadoReproductorEntity(
    @PrimaryKey val clave: String = "global",
    val ultimaCancionId: Long?,
    val colaIdsJson: String,
    val indiceColaActual: Int,
    val modoBucle: Int,
    val modoAleatorio: Boolean,
    val posicionMs: Long,
    val timestamp: Long
)
```

### DAOs nuevos:
| DAO | Métodos clave |
|-----|---------------|
| `CancionLocalDao` | `upsertAll()`, `getAll()`, `findById()`, `updateFavorita()`, `updateMetadatos()`, `deleteAll()` |
| `ListaReproduccionDao` | `upsert()`, `getAll()`, `findById()`, `delete()`, `addCancionIds()`, `removeCancionId()` |
| `EstadoReproductorDao` | `upsert()`, `get()` (suspend Flow) |

### Migración `AppDatabase.kt` **⚠️ REQUIERE TU AUTORIZACIÓN**:
```kotlin
// version = 31
private val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `canciones_locales` (
                `id` INTEGER PRIMARY KEY NOT NULL,
                `titulo` TEXT NOT NULL,
                `artista` TEXT NOT NULL,
                `album` TEXT NOT NULL,
                `duracionMs` INTEGER NOT NULL,
                `rutaArchivo` TEXT NOT NULL,
                `uriStr` TEXT NOT NULL,
                `portadaUriStr` TEXT,
                `fechaAgregada` INTEGER NOT NULL,
                `esFavorita` INTEGER NOT NULL DEFAULT 0,
                `tamanoBytes` INTEGER NOT NULL,
                `carpetaContenedora` TEXT NOT NULL,
                `metadatosPersonalizadosJson` TEXT
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `listas_reproduccion` (
                `id` TEXT PRIMARY KEY NOT NULL,
                `nombre` TEXT NOT NULL,
                `descripcion` TEXT NOT NULL,
                `fechaCreacion` INTEGER NOT NULL,
                `icono` TEXT NOT NULL,
                `cancionIdsJson` TEXT NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `estado_reproductor` (
                `clave` TEXT PRIMARY KEY NOT NULL DEFAULT 'global',
                `ultimaCancionId` INTEGER,
                `colaIdsJson` TEXT NOT NULL,
                `indiceColaActual` INTEGER NOT NULL,
                `modoBucle` INTEGER NOT NULL,
                `modoAleatorio` INTEGER NOT NULL DEFAULT 0,
                `posicionMs` INTEGER NOT NULL,
                `timestamp` INTEGER NOT NULL
            )
        """)
    }
}
```

### Integración en `GESTOR_AUDIO_TX.kt`:
- `escanearMusicaLocal()` → `cancionLocalDao.upsertAll()` + emitir Flow
- `crearLista()`/`agregarCancionesALista()` → `listaReproduccionDao`
- `alternarFavorita()` → `cancionLocalDao.updateFavorita()`
- `guardarUltimaCancion()`/`restaurarUltimaCancionSiExiste()` → `estadoReproductorDao`
- **Eliminar** todos los `SharedPreferences` de favoritas, listas, cola, última canción

### Verificación FASE 3:
- [ ] Kill app → abrir → cola, posición, favoritas, listas intactas
- [ ] Re-escanear → no duplica, actualiza metadatos
- [ ] Reinicio dispositivo → estado restaurado

---

## 🟢 FASE 4 - UI Tema Claro + Ecualizador Vertical (PENDIENTE)

**Regla `MODULOS_GESTOR.md`**: *"tema claro usado en el dashboard, evitar siempre usar tarjetas o fondos colores oscuros y letras blancas en fondos claros"*

### Paleta (basada en `DashboardScreen.kt`):
| Elemento | Color Claro |
|----------|-------------|
| Scaffold background | `Color(0xFFF5F5F5)` / `MaterialTheme.colorScheme.surface` |
| TopBar / AppBar | `Color.White` + `shadowElevation = 1.dp` |
| Tabs container | `Color.White`, indicator `MotoOrangePrimary` |
| Cards / Filas | `Color.White`, divider `Color(0xFFE0E0E0)` |
| Mini-player | `Color.White`, border `Color(0xFFE0E0E0)` |
| Dialogs (Reproductor completo, Ecualizador, Ajustes) | `Color.White`, textos `Color.Black` / `onSurface` |
| Nube flotante | `Color.White` 95% opacidad, borde `MotoOrangePrimary` si playing |

### Archivos a modificar:
| Archivo | Cambios clave |
|---------|---------------|
| `REPRODUCTOR_PRINCIPAL.kt` | Scaffold bg, TopBar, Tabs, Filas, Mini-player, Dialogs → tema claro |
| `ECUALIZADOR_TX.kt` | **Sliders verticales** (ver abajo), fondo blanco |
| `NUBE_AUDIO.kt` | Fondo blanco 95%, bordes grises/naranja |
| `AJUSTES_REPRODUCTOR.kt` | Cards blancas, bordes grises, switches con `MaterialTheme.colorScheme` |
| `PANEL_DESCARGAS_YT.kt` | Tema claro consistente |

### Ecualizador - Controles Verticales Clásicos:
```kotlin
// En PanelEcualizadorTX.kt - reemplazar Row horizontal por Column vertical
Column(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    // Ultra Volumen - Slider vertical
    SliderVertical(
        value = localUltraVol,
        onValueChange = { ... },
        valueRange = 1.0f..3.0f,
        modifier = Modifier.height(140.dp).width(48.dp)
    )
    // Super Bass - Slider vertical
    // Espacialidad - Slider vertical
    // 5 Bandas - Row de 5 sliders verticales (ecualizador gráfico real)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz").forEach { banda ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${gananciaDb}dB", style = labelSmall)
                SliderVertical(value = ..., modifier = Modifier.height(140.dp))
                Text(banda, style = labelSmall)
            }
        }
    }
}

// Helper SliderVertical (rotado -90°)
@Composable
fun SliderVertical(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier
            .graphicsLayer { rotationZ = -90f }
            .width(140.dp)
            .height(48.dp)
    )
}
```

### Verificación FASE 4:
- [ ] Capturas lado a lado: Dashboard vs Reproductor → misma paleta
- [ ] Modo claro/oscuro sistema → respeta (`MaterialTheme.colorScheme`)
- [ ] Ecualizador: 5 sliders verticales funcionales, preset "Personalizado" guarda estado

---

## 🔵 FASE 5 - Widget Android Mejorado (PENDIENTE)

### Layout XML nuevo: `res/layout/widget_reproductor_tx.xml`
```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@android:color/white"
    android:padding="12dp">

    <!-- Carátula + Info -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingBottom="8dp">
        
        <ImageView
            android:id="@+id/widget_caratula"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:scaleType="centerCrop"
            android:background="@color/gray_200" />
        
        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="12dp"
            android:orientation="vertical">
            <TextView
                android:id="@+id/widget_titulo"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textColor="@android:color/black"
                android:textSize="14sp"
                android:maxLines="1"
                android:ellipsize="marquee"
                android:singleLine="true" />
            <TextView
                android:id="@+id/widget_artista"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textColor="@color/gray_600"
                android:textSize="12sp"
                android:maxLines="1"
                android:ellipsize="end" />
        </LinearLayout>
    </LinearLayout>

    <!-- Controles -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center"
        android:layout_marginTop="4dp">
        
            <Button android:id="@+id/widget_prev" ... />
            <Button android:id="@+id/widget_play_pause" ... />
            <Button android:id="@+id/widget_next" ... />
    </LinearLayout>

    <!-- Progreso -->
    <ProgressBar
        android:id="@+id/widget_progreso"
        style="@android:style/Widget.ProgressBar.Horizontal"
        android:layout_width="match_parent"
        android:layout_height="4dp"
        android:layout_marginTop="8dp"
        android:progressTint="@color/moto_orange_primary"
        android:progressBackgroundTint="@color/gray_300" />
</LinearLayout>
```

### `WIDGET_REPRODUCTOR_TX.kt`:
- Usar `RemoteViews` con layout personalizado
- Actualizar vía `MediaSession` metadata (`MediaControllerCompat` callback)
- Carátula: `setImageViewBitmap()` desde `MediaMetadataCompat`
- Progreso: `setProgress()` desde `PlaybackStateCompat`

### Verificación FASE 5:
- [ ] Añadir widget a home → muestra canción actual, carátula, controles funcionan
- [ ] Progreso se actualiza en tiempo real
- [ ] Click en widget abre app en pestaña Reproductor

---

## 🟣 FASE 6 - DSP Nativo Hardening (PENDIENTE)

### Acciones:
- [ ] Verificar carga `libmotor_audio_tx.so` en `MOTOR_AUDIO_NATIVO.kt:init` (ya OK según tu feedback)
- [ ] Fallback AudioFX robusto si nativo falla (ya existe en `liberarEfectos()`)
- [ ] Exponer preset "Personalizado" que guarda bandas + DSP en config
- [ ] Unificar pipeline: `aplicarUltraVolumen/SuperBass/Espacialidad/Bandas` → solo si `libreriaNativaCargada` true, sino AudioFX
- [ ] Log de latencia DSP: medir ms por buffer en `procesarEcualizador` CPP

### Verificación FASE 6:
- [ ] DSP nativo activo → logs muestran "Pipeline de Efectos Biker Pro vinculado"
- [ ] Fallback AudioFX funciona si `.so` no carga
- [ ] Latencia < 20ms por buffer (4096 samples @ 48kHz ≈ 85ms teórico, target < 20ms procesamiento)

---

## 🟤 FASE 7 - Descargas YT Robustez (PENDIENTE - Baja prioridad)

### Mejoras puntuales en `GESTOR_DESCARGAS_YT.kt` / `BUSCADOR_YT.kt`:
- [ ] Cola de descargas persistente (Room) → sobrevive a reinicio
- [ ] Reintentos exponenciales en `resolverUrlDirecta()` / `descargarStream()`
- [ ] Verificación post-descarga: `MediaMetadataRetriever` valida `duration > 0` antes de `MediaScanner`
- [ ] Integración futura con módulo Avisos: `AvisosRepository.insert(Aviso(...))` cuando exista

### Verificación FASE 7:
- [ ] Reiniciar app durante descarga → continúa al abrir
- [ ] Archivo corrupto/0 bytes → no aparece en biblioteca, log claro
- [ ] Búsqueda falla → retry automático con backoff

---

## 📋 Checklist Maestro

| Fase | Estado | Bloqueante | Archivos clave |
|------|--------|------------|----------------|
| FASE 1 | 🔄 Compilando | - | `GESTOR_AUDIO_TX.kt` |
| FASE 2 | ⏸️ | `AndroidManifest.xml` (permiso) | `ReproductorForegroundService.kt`, `MediaSessionHelper.kt`, `MainActivity.kt` |
| FASE 3 | ⏸️ | `AppDatabase.kt` version 31 + migración | `ModelosReproductorRoom.kt`, `*Dao.kt`, `AppDatabase.kt` |
| FASE 4 | ⏸️ | - | `REPRODUCTOR_PRINCIPAL.kt`, `ECUALIZADOR_TX.kt`, `NUBE_AUDIO.kt`, `AJUSTES_REPRODUCTOR.kt`, `PANEL_DESCARGAS_YT.kt` |
| FASE 5 | ⏸️ | - | `widget_reproductor_tx.xml`, `WIDGET_REPRODUCTOR_TX.kt` |
| FASE 6 | ⏸️ | - | `MOTOR_AUDIO_NATIVO.kt`, `MOTOR_AUDIO.cpp` |
| FASE 7 | ⏸️ | Módulo Avisos | `GESTOR_DESCARGAS_YT.kt`, `BUSCADOR_YT.kt` |

---

## 🔒 Recordatorios de Reglas (`MODULOS_GESTOR.md`)

1. **NO tocar** `build.gradle.kts`, `settings.gradle.kts`, `AndroidManifest.xml` sin autorización explícita
2. **No regresiones**: cada fase compila y pasa test manual antes de siguiente
3. **Logcat obligatorio**: cada fix incluye `Log.d("TEAM_TX_REPRODUCTOR", ...)`
4. **Tema claro obligatorio** (FASE 4): usar `MaterialTheme.colorScheme.surface/onSurface`
5. **Room**: siempre incrementar versión + migración SQL + registrar en `.addMigrations()`

---

## 📝 Notas para Continuación

- **Estado actual**: FASE 1 en compilación (tú la estás resolviendo desde otro lado)
- **Próximo handoff**: Cuando FASE 1 compile → crear `ReproductorForegroundService.kt` + `MediaSessionHelper.kt` + solicitar permiso `AndroidManifest.xml`
- **Dependencias**: FASE 2 desbloquea background playback real; FASE 3 desbloquea persistencia real; FASE 4 es puramente visual; FASE 5/6/7 son mejoras incrementales

---

*Generado automáticamente - Actualizar al completar cada fase*