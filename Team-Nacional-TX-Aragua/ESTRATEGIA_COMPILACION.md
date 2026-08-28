# ESTRATEGIA DE COMPILACIÓN — Guía técnica para agentes

> **Audiencia:** agentes de IA (Antigravity, opencode, Claude Code, etc.) y desarrolladores.
> **Última verificación:** 26/08/2026 — `BUILD SUCCESSFUL`, APK instalado y validado con rollback AGP 8.7.3.
> **Regla de oro:** este documento describe un sistema ESTABLE. No "mejores" nada; replica exactamente lo que está aquí.

---

## 1. Punto de entrada (CRÍTICO)

| Ítem | Valor |
|---|---|
| Carpeta a abrir / workdir | `D:\MAPA\Team-Nacional-TX-Aragua` |
| **NUNCA** abrir como proyecto | `D:\MAPA` (su configuración Gradle fue retirada a cuarentena) |
| Comando de compilación | `.\gradlew.bat :app:assembleDebug` |
| APK de salida | `app\build\outputs\apk\debug\app-debug.apk` (~292 MB, firma debug = `my-upload-key.jks`) |
| Paquete | `com.aistudio.teamtxvzla.rkqp` |

---

## 2. Arquitectura del build (multi-módulo con motor externo)

```
Team-Nacional-TX-Aragua\            ← RAÍZ Gradle (settings.gradle.kts propio)
├── settings.gradle.kts             ← incluye :app + 4 módulos del motor vía projectDir
├── build.gradle.kts                ← plugins raíz (versiones fijadas AQUÍ una sola vez)
├── gradle.properties               ← banderas obligatorias del motor
├── versions.gradle                 ← versiones osmand_* consumidas por el motor
├── gradle/libs.versions.toml       ← catálogo FUSIONADO (app + aliases del motor)
└── app\                            ← aplicación Compose (módulo :app)

D:\MAPA\android\                    ← MOTOR OsMAND (DEPENDENCIA VIVA — no es basura)
├── OsmAnd\         → módulo :OsmAnd        (app Android del mapa)
├── OsmAnd-api\     → módulo :OsmAnd-api    (AIDL API)
├── OsmAnd-java\    → módulo :OsmAnd-java   (núcleo Java)
└── OsmAnd-shared\  → módulo :OsmAnd-shared (KMP)

D:\MAPA\resources\                  ← ASSETS DEL MAPA (DEPENDENCIA VIVA)
                                    ← fonts/, rendering_styles/, voice/, poi/, routing/
                                    ← leídos por tasks collect* del motor vía ../../resources
```

**PROHIBIDO mover, renombrar o borrar** `D:\MAPA\android` ni `D:\MAPA\resources`.
El proyecto NO es autocontenido: vive en simbiosis con esas dos carpetas hermanas.

---

## 3. Invariantes del sistema de compilación (no romper)

### 3.0 Versiones Obligatorias (ESTRICTO - NO MODIFICAR)
- **Android Gradle Plugin (AGP):** `8.7.3`
- **Kotlin:** `2.0.21`
- **Compose Compiler:** Integrado en Kotlin 2.0+
- **JDK:** `17`
- **Regla:** Queda estrictamente prohibido subir de estas versiones para evitar incompatibilidades con el motor OsmAnd y la compresión de recursos SVG.

---

## 8. Depuración de Imágenes (Muro)
Si las imágenes de los avisos no cargan, usa este filtro en Logcat:
`TEAM_TX_IMAGES`

Este log reportará la URL exacta y el error del servidor (Firebase).

### 3.1 Plugins — patrón "versión solo en el raíz"
- El Kotlin Gradle Plugin se fija **una vez** en `build.gradle.kts` raíz con
  `alias(libs.plugins.kotlin.android) apply false`.
- Los subproyectos lo aplican **sin versión**: `id("org.jetbrains.kotlin.android")`.
- Motivo: si un módulo pide versión y el KGP ya está en classpath por otra vía (compose/KSP),
  Gradle falla con *"already on the classpath with an unknown version"*.
- `kotlin-multiplatform` e `ivy-publish` están en el TOML **sin versión** (resueltos del classpath;
  ivy-publish es plugin core de Gradle). `de.undercouch.download` SÍ lleva versión `4.1.1` en el raíz.

### 3.2 Banderas obligatorias (`gradle.properties`)
```properties
android.builtInKotlin=false        # el motor aplica kotlin-android clásico; con builtin activo chocan extensiones
android.newDsl=false
android.enableJetifier=true
android.nonTransitiveRClass=false  # el motor usa R transitivo de sus librerías (TextFieldBoxes, etc.)
```

### 3.3 JVM target global (raíz `build.gradle.kts`)
```kotlin
allprojects {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
}
```
Sin esto, `:OsmAnd` compila Kotlin a 21 mientras su Java va a 17 → fallo de consistencia.

### 3.4 Repositorios (`settings.gradle.kts`)
google() · mavenCentral() · `builder.osmand.net/maven2` (Maven) ·
**Ivy `OsmAndBinariesIvy`** con patrón `ivy/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]`
(necesario para `net.osmand:antpluginlib`) · `jitpack.io` (dependencias `com.github.*`).

### 3.5 Catálogo `libs.versions.toml`
Contiene DOS secciones conviviendo: las de la app y un bloque marcado
`# --- ... del motor OsmAnd ... ---`. Los aliases del motor usan nombres EXACTOS que los scripts
del motor esperan (ej.: `play-location`, NO `play-services-location`; `androidx-test-junit-ext`,
NO `androidx-test-junit`). Si agregas aliases nuevos, no renombres los existentes de ningún bando.

### 3.6 Blindaje AGENTS.md (`app/AGENTS.md`)
Vigente: prohibido alterar scripts de compilación, dependencias NDK o la estructura de integración
del mapa sin autorización explícita del usuario.

---

## 4. Flujo de trabajo recomendado para agentes

### Paso 1 — Compilar
```powershell
.\gradlew.bat :app:assembleDebug 2>&1 | Select-Object -Last 25
```
- Build frío completo: ~4–10 min. Incremental: 8 s – 2 min.
- Timeout sugerido: 1800000 ms (30 min).
- Éxito = línea `BUILD SUCCESSFUL` + `EXIT=0`.

### Paso 2 — Instalar (ADB inalámbrico, dispositivos conocidos)
```powershell
$apk = "D:\MAPA\Team-Nacional-TX-Aragua\app\build\outputs\apk\debug\app-debug.apk"
adb -s "adb-A9FRUT4315006621-5J9p3G._adb-tls-connect._tcp" install -r $apk   # ALI_NX3
adb -s "adb-RFGL52W368D-0jvEBz._adb-tls-connect._tcp"     install -r $apk   # SM_A165M
```
Los seriales TLS cambian si se reconectan → listar primero con `adb devices -l`.

### Paso 3 — Lanzar y verificar
```powershell
$pkg = "com.aistudio.teamtxvzla.rkqp"
adb -s <SERIAL> logcat -c
adb -s <SERIAL> shell "monkey -p $pkg -c android.intent.category.LAUNCHER 1"
Start-Sleep -Seconds 14
(adb -s <SERIAL> shell pidof $pkg)          # debe devolver PID (proceso vivo)
adb -s <SERIAL> logcat -d | Select-String "FATAL EXCEPTION|Process $pkg.*has died"
```
⚠️ En PowerShell `$pid` es variable reservada de solo lectura — usar otro nombre (`$procId`).

---

## 5. Errores conocidos → solución rápida

| Síntoma | Solución |
|---|---|
| `Cannot add extension 'kotlin'` al configurar `:OsmAnd` | Falta `android.builtInKotlin=false` |
| `Plugin 'de.undercouch.download' not found` | Debe estar con versión en raíz `build.gradle.kts` (NO en settings) |
| `No such property 'kotlin'/'location' for extension 'libs'` | Alias faltante en `libs.versions.toml` (sección motor) |
| `already on the classpath with an unknown version` | Quitar versión de la petición en submódulo; fijar versión solo en raíz |
| `Could not find com.github.HITGIF:*` | Falta repo JitPack en settings |
| `Could not find net.osmand:antpluginlib` | Falta repo Ivy con patternLayout en settings |
| `Inconsistent JVM Target (17 vs 21)` | Bloque `allprojects { KotlinCompile → JVM_17 }` en raíz |
| `symbol R.id.text_field_boxes_* not found` | `android.nonTransitiveRClass=false` |
| `Unresolved reference 'net.osmand...'` | Alguien desconectó el motor — revisar sección 2/3 y git diff |
| Sobrecarga `.padding(horizontal=, top=)` | Usar forma start/end/top (Compose no mezcla horizontal+top) |

---

## 6. Historial relevante

- **23/08/2026** — Última sesión exitosa de Antigravity (brain `73e21d65…`, task-4053,
  `BUILD SUCCESSFUL in 58s`) y commit `eb3bff29`. Después, ediciones sin commitear dejaron el árbol roto.
- **24/08/2026** — Restauración completa del cableado del motor, reparación de código, build verde,
  despliegue en 2 dispositivos, limpieza de `D:\MAPA` (~2,9 GB a `D:\MAPA_LIMPIEZA_20260824\`,
  pendiente de commit git y borrado definitivo).
- Detalle completo de la sesión: `REPORTE_SESION_20260824.md` (misma carpeta).

## 7. Checklist antes de declarar éxito

- [ ] `BUILD SUCCESSFUL` con `EXIT=0` en `:app:assembleDebug`
- [ ] APK existe y con timestamp reciente en `app\build\outputs\apk\debug\`
- [ ] Instalación `Success` en ambos dispositivos
- [ ] Proceso vivo tras lanzamiento (pidof devuelve PID)
- [ ] `logcat` sin `FATAL EXCEPTION` ni `has died`
- [ ] `git status`: solo cambios intencionales; motor (`../android`) intacto
