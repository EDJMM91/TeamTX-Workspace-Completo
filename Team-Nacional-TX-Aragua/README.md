# 🏍️ Team Nacional TX Venezuela - Aplicación Móvil Oficial

> **"Hermandad, Pasión y Asfalto"**  
> Plataforma móvil integral desarrollada en **Kotlin** y **Jetpack Compose (Material 3)** para la gestión y comunidad del **Team Nacional TX Venezuela**.

---

## 📱 Módulos y Funcionalidades del Sistema

### 1. 📰 Muro de Noticias & Avisos Comunitarios (`FeedScreen`)
- Publicación de comunicados, rodadas y eventos con soporte multimedia.
- Sistema de categorías, prioridades y publicaciones fijadas.
- Comentarios en tiempo real y reacciones de la hermandad.
- Sincronización bidireccional con Room Database y Firebase Firestore.

### 2. 💬 Chat General & Grupos Privados (`ClubChatScreen`)
- **Mensajería en Tiempo Real**: Canales públicos (General, Directiva, SOS) y Grupos Privados protegidos.
- **Reacciones con Emojis al Mensaje**: Reacción instantánea (`👍`, `❤️`, `😂`, `🏍️`, `🔥`, `🙏`, `😮`, `😢`) dejando presionado el mensaje, con contador dinámico en la burbuja.
- **Notas de Voz de Alta Duración (1 a 5 min)**: Grabación en formato ultra ligero **AAC/MPEG-4 a 32 kbps mono (~240 KB por minuto)** con reproductor integrado en burbuja.
- **🛡️ Ciclo de Vida y Protección del Plan Gratuito de Firebase Storage (5 GB)**: Rutina preventiva que purga automáticamente notas de voz de más de 30 días tanto en la nube como en local.
- **Auto-ocultado Inteligente del Teclado**: Se esconde automáticamente tras 15 segundos sin escribir o al hacer scroll en la lista.
- **Escáner Multiruta de Stickers de WhatsApp**:
  - Escaneo automático en `Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Stickers` (Android 11+).
  - Escaneo en `WhatsApp/Media/WhatsApp Stickers` (Android 10 y anteriores).
  - Selector de carpeta mediante SAF (`ACTION_OPEN_DOCUMENT_TREE`) y guardado de stickers en Favoritos con un toque.

### 3. 🏁 Velocímetro TX & Telemetría Racing (`VelocimetroScreen`)
- **Odómetro Total (ODO)**: Kilometraje acumulado histórico de la moto persistido en almacenamiento local.
- **Odómetro Parcial (TRIP)**: Distancia del viaje actual con botón interactivo de **`[ RESET ]`** a 0.00 en tiempo real.
- **Modo Pantalla Completa Gigante (`Fullscreen`)**: Botón **`[ ⛶ ]`** que expande los números digitales y la aguja al 100% de la pantalla para máxima visibilidad en el manubrio de la moto.
- **Auto-escondido de Navegación**: Oculta automáticamente la barra inferior de navegación de la app al entrar para maximizar el espacio de conducción.
- **4 Modos de Tacómetro Calibrados**:
  - `Digital Sport`: Números grandes con tacómetro LED progresivo.
  - `Analógico TX`: Esfera deportiva de 0 a 200 KM/H (y 0 a 120 MPH) con aguja de fuego animada y números calibrados.
  - `Mixto Rally`: Brújula cardinal central, altímetro y medidor G-Force.
  - `Proyector HUD`: Visión nocturna de alto contraste con opción de **Modo Espejo** para reflejar en el parabrisas.
- **Fusión Sensorial**: Sensor GPS + Acelerómetro lineal (`TYPE_LINEAR_ACCELERATION`) para arrancadas y medidor de Fuerza G.
- **Récord Top Speed**: Guarda la velocidad máxima histórica en el Carnet Digital del Piloto con opción de reinicio.
- **Selector KM/H <-> MPH**: Cambio de unidades con persistencia.

### 4. 🗺️ Mapa TX & Navegación Offline (`OsmAnd Integration`)
- Motor cartográfico integrado basado en mapas vectoriales offline.
- Puntos de interés motero: talleres, estaciones de servicio, paradas seguras y puntos de encuentro de rodadas.
- Soporte para importación y renderizado de trazados GPX y waypoints.

### 5. 🛒 Mercado Motero (`MarketplaceScreen`)
- Compra, venta e intercambio de repuestos, motos, accesorios e indumentaria motera.
- Conversión de precios con tasa oficial del Banco Central de Venezuela (BCV) en vivo.
- Filtros por categoría, condición (nuevo/usado), precio y ubicación.

### 6. 🔧 Bitácora Mecánica & Mantenimiento (`MaintenanceScreen`)
- Registro de servicios mecánicos, cambio de aceite, pastillas de freno, bujías y cauchos.
- Alertas predictivas por kilometraje para los próximos mantenimientos preventivos.

### 7. 🏪 Directorio de Talleres y Repuestos (`DirectorioScreen`)
- Directorio clasificado de talleres mecánicos, venta de repuestos y servicios de auxilio recomendados por la hermandad.

### 8. 🛂 Pasaporte Motero & Retos (`PassportScreen`)
- Registro de destinos visitados, sellos digitales de rodadas y desafíos de carretera completados con insignias especiales.

### 9. 📅 Calendario de Rodadas y Eventos (`CalendarScreen`)
- Planificación de rutas, eventos sociales, horas de concentración y salida ("ruedas en el asfalto").

### 10. 🚨 SOS Vial & Auxilio de Emergencias (`SosScreen`)
- Botón de pánico vial con geolocalización precisa instantánea.
- Notificaciones de alerta a miembros cercanos y a la junta directiva.

### 11. 🪪 Carnet Digital del Piloto TX (`ProfileScreen`)
- Carnet de identificación digital con fotografía, rango dentro del club, tipo de sangre, contacto de emergencia, código QR y medalla de récord de velocidad máxima.

### 12. 💰 Tesorería & Finanzas (`FinancesScreen`)
- Registro y balance de ingresos, egresos, cuotas de miembros y reportes de tesorería del club.

### 13. 📦 Inventario del Club (`InventoryScreen`)
- Control de activos, herramientas comunitarias y equipamiento en préstamo.

### 14. 📜 Normativas y Leyes (`NormativasScreen`)
- Código de convivencia del club, normativas viales y reglamentos oficiales.

### 15. 🛡️ Panel de Directiva (`DirectivaExclusiveScreen`)
- Gestión de solicitudes de ingreso, asignación de rangos, moderación de canales y anuncios oficiales.

### 16. 📁 Sistema Organizado de Carpetas en Español (`GestorCarpetasApp`)
Jerarquía de carpetas creada automáticamente en la memoria del teléfono para un fácil acceso:
```
📁 Team Nacional TX/
   ├── 📁 Audios de Voz/          (Notas de voz y mensajes grabados)
   ├── 📁 Stickers y Memes/       (Colección WebP y WhatsApp)
   ├── 📁 Fotos del Club/         (Fotos de eventos, perfil, carnet y mercado)
   ├── 📁 Documentos y Reportes/  (PDFs de normativas, carnets y tesorería)
   ├── 📁 Rutas y Mapas/          (Trazados GPX y waypoints)
   └── 📁 Copias de Seguridad/    (Respaldos locales)
```

---

## 🛠 Stack Tecnológico

- **Lenguaje**: Kotlin 2.1+
- **UI Toolkit**: Jetpack Compose con Material 3 y animaciones avanzadas
- **Arquitectura**: MVVM (Model-View-ViewModel) + Repository Pattern + Clean Architecture
- **Persistencia Local**: Room Database v25 (SQLite) + SharedPreferences
- **Backend & Sincronización**:
  - Firebase Authentication (Control de acceso Gatekeeper)
  - Firebase Firestore (Sincronización en tiempo real)
  - Firebase Cloud Messaging (Notificaciones push)
  - Firebase Storage (Archivos multimedia, notas de voz y distribución OTA en `updates/`)
  - Supabase Storage (Almacenamiento CDN secundario)
- **Mapas y GIS**: OsmAnd Core Engine + Vector Maps
- **Procesamiento Asíncrono**: Kotlin Coroutines + StateFlow / SharedFlow
- **Permisos y Almacenamiento**: Scoped Storage + SAF DocumentTree + Android 14/15 Media Permissions

---

## 🚀 Instalación y Compilación

### Requisitos
- Android Studio Ladybug (2024.2.1) o superior
- JDK 17
- Android SDK 24+ (minSdk 24, targetSdk 36)

### Compilar APK Debug
```bash
./gradlew assembleDebug
```

### Instalar en Dispositivo mediante ADB
```bash
adb install -r apk/TeamTX-latest.apk
```

---

## 📡 Sistema Oficial de Distribución OTA, Firebase Storage & Nomenclatura de APKs

> ⚠️ **ESTÁNDAR OBLIGATORIO DE NOMENCLATURA Y DISTRIBUCIÓN DE VERSIONES**:  
> A partir de ahora, cada archivo APK generado y subido a Firebase Storage (`gs://teamnacionaltx.firebasestorage.app/updates/`) debe seguir la siguiente estructura estandarizada:

### 1. Nombres Oficiales de Archivos APK:
- **Versión Estable**: `TeamTX-v<VersionName>-estable.apk` (Ejemplo: `TeamTX-v2.5.0-estable.apk`).
- **Versión Beta / Pruebas**: `TeamTX-v<VersionName>-beta.apk` (Ejemplo: `TeamTX-v2.5.1-beta.apk`).
- **Alias Última Versión Oficial**: `TeamTX-latest.apk` (siempre debe actualizarse y mantenerse en `apk/TeamTX-latest.apk` y en Storage `updates/TeamTX-latest.apk`).

### 2. Repositorio de Versiones en Firebase Storage (`updates/`):
- **Bucket**: `gs://teamnacionaltx.firebasestorage.app/updates/`
- **Capacidad de Rollback**: La app consulta dinámicamente este directorio en tiempo real dentro del **Módulo Info** (`Historial de Versiones y Rollback`), destacando la **Última Versión Estable [RECOMENDADA]** y permitiendo a cualquier usuario instalar versiones anteriores en caso de requerir un rollback por fallos de compatibilidad o bugs.

### 3. Configuración en Firebase Firestore:
- **Colección**: `configuracion` *(en minúsculas)*
- **Documento**: `OTA` *(en mayúsculas)*
- **Campos Requeridos**:
  - `versionCode` (`number` / `int64`): Código numérico incremental de versión (ej. `25`, `26`...).
  - `urlDescarga` (`string`): URL de descarga directa en Firebase Storage (`updates/TeamTX-latest.apk`).
  - `notas` (`string`): Novedades y mejoras incluidas en la versión.

---

## 📄 Licencia y Créditos
Desarrollado con pasión para el **Team Nacional TX Venezuela**.  
Desarrollador Principal: **Eduardo Márquez** (`+584243769999` / `eduardo.androide.em@gmail.com`).  
*Cuidamos nuestros colores bikers, esta app es parte de ellos. Fomentando la hermandad y unión.*


