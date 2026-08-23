# 🏍️ Team Nacional TX Aragua - App Oficial & Motor de Mapas Offline (Full Estable)

[![Android](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024--35)-orange?logo=android)](https://android.com)
[![Gradle](https://img.shields.io/badge/Build-Gradle%209.3.1%20%7C%20AGP%208.9.0-green?logo=gradle)](https://gradle.org)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.2.10%20%2B%20Java%2017-purple?logo=kotlin)](https://kotlinlang.org)
[![Firebase](https://img.shields.io/badge/Backend-Firebase%20Firestore%20%2B%20Auth%20%2B%20Storage-yellow?logo=firebase)](https://firebase.google.com)
[![Status](https://img.shields.io/badge/Release-v1.2.7%20Full%20Estable-brightgreen)](#)

---

## 📌 Visión General
**Team Nacional TX Aragua** es la aplicación móvil integral y oficial para la comunidad motera del club Team TX en Venezuela. Unifica en un **ÚNICO APK COMPACTO Y DE ALTO RENDIMIENTO** la gestión social del club, sincronización en la nube con Firebase Firestore, sistema de emergencias SOS, finanzas, inventario, y un potente **motor de mapas vectoriales sin conexión (Offline)** basado en OsmAnd Core.

---

## 🚀 Características Principales

### 1. 🛡️ Portal de Acceso y Onboarding
* **Gatekeeper Seguro:** Autenticación por número de teléfono, Google Sign-In o Código de Invitación con firma criptográfica.
* **Ficha del Piloto:** Validación de datos personales, Cédula/DNI, número de WhatsApp, modelo de moto (TX 200), placa y grupo sanguíneo.
* **Teclados Optimizados:** Teclado numérico nativo para Cédula/DNI y telefónico para números de contacto y emergencia.

### 2. 📢 Muro de Avisos y Retos TX en Tiempo Real
* **Sincronización Bidireccional Firestore + Room:** Cualquier publicación creada por la directiva se propaga en milisegundos a todos los miembros.
* **Borrado Seguro y Remoto:** Si la directiva elimina un aviso, se elimina automáticamente de Firestore y se purga en tiempo real de los dispositivos de todos los usuarios.
* **Botón de Actualización Rápida:** Botón de refresco manual en la cabecera del muro con retroalimentación visual animada y sincronización automática silenciosa al iniciar la aplicación.
* **Categorías:** Avisos Oficiales, Retos Moteros, Fichas de Mecánica y Capacitación.

### 3. 🗺️ Motor de Mapas TX Offline Integrado (OsmAnd Embedded)
* **Acceso Directo:** Lanzamiento del mapa nativo exclusivamente desde el botón *"Mapa TX"* del Dashboard.
* **Retorno al Dashboard:**
  * **Asistente Inicial de Mapas:** Botón visible *"← VOLVER AL DASHBOARD"* para regresar a la aplicación sin necesidad de descargar mapas de inmediato.
  * **Pantalla de Navegación del Mapa:** Botón circular flotante y simétrico en la esquina superior derecha con el **logo oficial del Team TX** que finaliza la actividad del mapa y regresa limpiamente al Dashboard.
* **Cero Colisiones:** Namespace interno de mapas re-empaquetado a `net.osmand.protobuf.*` garantizando 100% de compatibilidad con las librerías modernas de Google Play Services y Firebase.

### 4. 🚨 Módulo SOS y Asistencia en Ruta
* Alertas de auxilio geolocalizadas con cálculo de ruta y llamada directa de emergencia al contacto asignado.

### 5. 👥 Gestión Directiva y Roles
* Modo Directiva con conmutación protegida para Directores de Ruta, Oficiales de Seguridad, Paramédicos de Ruta y Presidente.
* Control global del chat y auditoría de eventos.

---

## 🏗️ Arquitectura del Mono-Repositorio

```
D:\MAPA (Root)
│
├── Team-Nacional-TX-Aragua/
│   └── app/                          # Módulo principal de la App TX (Jetpack Compose + ViewModel + Room + Firebase)
│       ├── src/main/java/com/example/
│       │   ├── data/                 # Modelos, Room DAOs, Repositorios y Sincronizadores Firestore
│       │   ├── ui/                   # Pantallas Compose (Feed, Chat, Rides, Onboarding, Gatekeeper)
│       │   └── MainActivity.kt       # Entrada principal y control de navegación
│       └── build.gradle.kts
│
├── android/                          # Módulos del Motor de Mapas OsmAnd integrado
│   ├── OsmAnd/                       # Interfaz nativa de mapas (MapActivity, FirstUsageWizard, HUD)
│   ├── OsmAnd-java/                  # Lógica del motor, ruteo OBF y Protobuf aislado (net.osmand.protobuf)
│   ├── OsmAnd-api/                   # API de comunicación y capas
│   └── OsmAnd-shared/                # Modelos y utilidades compartidas
│
├── build.gradle.kts                  # Configuración raíz de Gradle y plugins
├── settings.gradle.kts               # Inclusión de submódulos (:app, :OsmAnd, :OsmAnd-java, etc.)
└── my-upload-key.jks                 # Keystore de firma de producción
```

---

## 🔑 Huellas Digitales SHA (Firebase Configuration)

Para el funcionamiento de Google Sign-In y Firebase App Check, las siguientes huellas se encuentran registradas:

| Entorno | Alias | SHA-1 | SHA-256 |
| :--- | :--- | :--- | :--- |
| **Debug** | `androiddebugkey` | `72:66:C6:E5:1C:18:72:02:CF:88:8F:00:17:D1:0D:A7:A4:99:BA:7D` | `E3:0B:40:64:95:8C:2A:B8:23:47:AA:0F:27:FA:56:49:C9:F1:48:03:18:A6:42:1F:3B:0C:0B:4B:F7:76:56:E8` |
| **Release** | `upload` | `10:84:47:F9:E6:D1:DD:F2:04:05:E1:9F:3C:5A:E5:02:13:CB:05:50` | `8A:3F:AC:63:F7:23:62:DF:02:16:7C:51:87:86:6C:A3:67:2D:49:21:B4:DD:ED:51:E2:C7:E8:0E:EC:13:7D:FB` |

---

## 🛠️ Compilación e Instalación

### Compilar APK Debug
```bash
./gradlew :app:assembleDebug
```
El APK se genera en: `Team-Nacional-TX-Aragua/app/build/outputs/apk/debug/app-debug.apk`

### Compilar APK Release Firmado
```bash
./gradlew :app:assembleRelease
```

### Instalar en Dispositivo Conectado por ADB
```bash
adb install -r Team-Nacional-TX-Aragua/app/build/outputs/apk/debug/app-debug.apk
```

---

## 🏍️ Lema Oficial
> *"HONOR, LEALTAD Y RESPETO"*  
> **Team Nacional TX Aragua - Venezuela**
