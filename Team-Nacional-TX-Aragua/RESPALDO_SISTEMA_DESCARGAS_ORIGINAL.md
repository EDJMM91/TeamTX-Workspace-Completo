# Registro y Respaldo del Sistema Original de Descargas de Música TX Pro

> **Fecha de Respaldo:** 06 de Septiembre de 2026  
> **Ubicación de respaldos de código fuente:**  
> - `app/src/main/java/com/example/reproductor/GESTOR_DESCARGAS_YT.kt.original_backup`  
> - `app/src/main/java/com/example/reproductor/BUSCADOR_YT.kt.original_backup`

---

## 1. Resumen de la Arquitectura Actual de Descargas

El sistema actual de descargas en el Reproductor TX Pro fue diseñado con un motor híbrido inteligente que garantiza descargas directas a **320 kbps** sin depender de servidores intermediarios caídos ni sufrir los bloqueos de bots comunes de YouTube:

### A. Búsqueda y Resolución Híbrida (`BUSCADOR_YT.kt`)
1. **Fuente Primaria (JioSaavn Global Music API):**
   - Endpoint: `https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=20&p=1&q={termino}`
   - Clave de descifrado DES: `"38346591"` en modo ECB / PKCS5Padding.
   - Ventaja: Devuelve enlaces directos MP4/AAC/MP3 a 320 kbps (`_320.mp4` / `_320.mp3`), permitiendo descargas ultra-rápidas y con calidad de estudio.
2. **Fuente Secundaria (Invidious API Rotativa):**
   - Instancias:
     - `https://invidious.privacydev.net`
     - `https://invidious.fdn.fr`
     - `https://inv.zzls.xyz`
     - `https://invidious.perennialte.ch`
     - `https://inv.tux.pizza`
     - `https://yewtu.be`
   - Endpoint: `/api/v1/search?q={termino}&type=video`
3. **Fuente Terciaria (Piped API Rotativa):**
   - Instancias:
     - `https://pipedapi.kavin.rocks`
     - `https://piped-api.lunar.icu`
     - `https://api.piped.projectsegfau.lt`
     - `https://piped.video/api`
   - Endpoint: `/search?q={termino}&filter=music_songs`
4. **Fuente Cuaternaria (Web Scraper YouTube Fallback):**
   - Scraping directo de `https://www.youtube.com/results?search_query={termino}` extrayendo `ytInitialData`.

---

## 2. Flujo de Descarga y Almacenamiento (`GESTOR_DESCARGAS_YT.kt`)

1. **Destino de Archivos:**
   - Carpeta: `Environment.DIRECTORY_MUSIC/TX_PRO_Descargas`
   - Sanitización de nombre: `[artista] - [titulo].mp3` (reemplazando caracteres ilegales con `_`).
2. **Descarga en Stream Directo:**
   - Búfer de red de 16 KB (`ByteArray(16384)`).
   - Reporte continuo de progreso hacia `_descargasActivas`.
   - Timeout de conexión: 12.000 ms; Timeout de lectura: 15.000 ms.
3. **Integración con Biblioteca Local y MediaStore:**
   - Notificación al sistema operativo vía `MediaScannerConnection.scanFile()` con MIME `audio/mpeg`.
   - Re-escaneo automático de la biblioteca de la app (`GESTOR_AUDIO_TX.escanearMusicaLocal()`).
   - Asociación automática a la playlist local **"Descargas TX"**.
   - Registro en el archivo de log motero: `Environment.DIRECTORY_DOCUMENTS/tx_reproductor_log.txt`.

---

## 3. Procedimiento de Restauración Inmediata

Si en algún momento se desea restaurar el sistema a su estado exacto original:
```powershell
Copy-Item 'app\src\main\java\com\example\reproductor\GESTOR_DESCARGAS_YT.kt.original_backup' 'app\src\main\java\com\example\reproductor\GESTOR_DESCARGAS_YT.kt' -Force
Copy-Item 'app\src\main\java\com\example\reproductor\BUSCADOR_YT.kt.original_backup' 'app\src\main\java\com\example\reproductor\BUSCADOR_YT.kt' -Force
```
Y compilar el proyecto con `./gradlew :app:compileDebugKotlin`.
