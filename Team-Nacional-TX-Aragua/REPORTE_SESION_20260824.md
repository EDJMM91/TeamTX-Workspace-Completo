# REPORTE DE SESIÓN — Restauración del build, compilación, despliegue y limpieza

**Fecha:** 24 de agosto de 2026
**Proyecto:** `D:\MAPA\Team-Nacional-TX-Aragua` (Team TX Venezuela)
**Resultado final:** ✅ APK generado e instalado en 2 dispositivos · Workspace limpio · Build verificado post-limpieza

---

## 1. Objetivo inicial

Identificar la última compilación exitosa realizada por Antigravity sobre este proyecto,
reproducir su estrategia de compilación, reparar lo roto, desplegar en dispositivos y limpiar
el workspace dejando únicamente lo necesario.

---

## 2. Diagnóstico forense (Antigravity)

### Última compilación exitosa encontrada
- **Sesión del agente:** `C:\Users\PC\.gemini\antigravity-ide\brain\73e21d65-9bbc-4d72-9120-33961d073538`
- **Tarea:** `task-4053.log` → `BUILD SUCCESSFUL in 58s` (23/08/2026 ~22:16 local)
- **Commit asociado:** `eb3bff29` — *feat(core): version full estable - resolucion protobuf osmand, single launcher, sync avisos en tiempo real y retorno al dashboard*

### Estrategia que usaba Antigravity
1. Comando: `./gradlew :app:assembleDebug 2>&1` desde `Team-Nacional-TX-Aragua\`
2. Compilación asíncrona + polling con temporizadores cada ~45–60 s hasta ver `BUILD SUCCESSFUL`
3. Instalación vía `adb install` y monitoreo de `logcat` buscando crashes
4. Ciclo corrección → recompilación ante fallos

### Hallazgo crítico
Los logs de tareas (`task-4019.log`) probaron que la compilación exitosa **sí incluía el motor
OsmAnd como módulos** (`> Task :OsmAnd:collectFonts`, `:OsmAnd-api:preDebugBuild`, etc.), pero el
árbol de trabajo tenía cambios sin commitear que lo habían **desconectado**:
- `implementation(project(":OsmAnd"))` comentado en `app/build.gradle.kts`
- AARs nativos (`net.osmand:OsmAndCore_android`) comentados
- `packaging.jniLibs` (pickFirsts), `ndkVersion`, `multiDexEnabled` eliminados
- Plugin `kotlin.android` removido de `:app`
- Banderas borradas de `gradle.properties` (`builtInKotlin`, `newDsl`, `enableJetifier`)
- Errores de sintaxis nuevos introducidos post-compilación-exitosa

Conclusión: ni el HEAD ni el working tree actual eran construibles; el estado bueno solo existía
en la historia efímera de la sesión del agente.

---

## 3. Reparaciones de código Kotlin

| Archivo | Problema | Corrección |
|---|---|---|
| `ui/screens/FeedScreen.kt:132` | `uploadError!` — sintaxis inválida | `if (uploadError != null) uploadError else "..."` |
| `ui/screens/FeedScreen.kt:98` | `.padding(horizontal=…, top=…)` sobrecarga inexistente | `.padding(start=16.dp, end=16.dp, top=innerPadding.calculateTopPadding()+8.dp)` |
| `nube/NUBE_MULTIMEDIA.kt` | Plugin `Retry` / `io.ktor.client.plugins.retry.*` no existe en Ktor 3.x; `delay = {...}` chocaba con `kotlinx.coroutines.delay` | Plugin estándar `HttpRequestRetry`: `maxRetries=2`, `retryOnExceptionIf { UnknownHostException \| TimeoutException \| IOException }`, `constantDelay(1000)` |

---

## 4. Restauración del cableado del motor OsmAnd

### 4.1 `settings.gradle.kts`
- Incluidos los 4 módulos del motor desde fuentes locales:
  `../android/OsmAnd`, `../android/OsmAnd-api`, `../android/OsmAnd-java`, `../android/OsmAnd-shared`
- `gradle.extra["java_shared_conf"] = ""` (equivalente de la variable que definía el settings del motor)
- Repositorios agregados a `dependencyResolutionManagement`:
  - `https://jitpack.io` (dependencias `com.github.*`, p.ej. TextFieldBoxes)
  - Repo **Ivy** `OsmAndBinariesIvy` con patrón `ivy/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]`
    (necesario para `net.osmand:antpluginlib:3.8.0@aar`)

### 4.2 `app/build.gradle.kts`
- Re-agregado plugin `org.jetbrains.kotlin.android` (sin versión, se resuelve del classpath fijado en raíz)
- Restaurado bloque `packaging { jniLibs { useLegacyPackaging, pickFirsts (libc++_shared.so x4) }, resources excludes }`
- Restaurado `ndkVersion = "27.0.12077973"` y `multiDexEnabled = true`
- Descomentadas dependencias: `project(":OsmAnd")`, `OsmAndCore_android@aar`, `OsmAndCore_androidNativeRelease@aar`

### 4.3 `build.gradle.kts` (raíz)
- `alias(libs.plugins.kotlin.android) apply false` — fija KGP 2.2.10 una sola vez
- `id("org.jetbrains.kotlin.plugin.parcelize") version "2.2.10" apply false`
- `alias(libs.plugins.android.library) apply false` (AGP library para módulos del motor)
- `id("de.undercouch.download") version "4.1.1" apply false` (requerido por `android/OsmAnd/build.gradle`)
- Bloque global alineando JVM target:
  ```kotlin
  allprojects {
      tasks.withType<KotlinCompile>().configureEach {
          compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
      }
  }
  ```

### 4.4 `gradle.properties`
Banderas restauradas (existentes durante la compilación exitosa, borradas después):
```properties
android.builtInKotlin=false
android.newDsl=false
android.enableJetifier=true
android.nonTransitiveRClass=false   # el motor referencia recursos de libs vía R transitivo
```

### 4.5 `versions.gradle` (raíz del proyecto)
Copiado desde `../android/versions.gradle` (los scripts del motor hacen
`apply from: rootProject.file("versions.gradle")`).

### 4.6 `gradle/libs.versions.toml` — fusión de catálogo (+~40 entradas)
- **Versions:** bloque "motor OsmAnd" (osmand-*Sdk/buildTools, appcompat, material, gridlayout,
  cardview, browser, preference, lifecycle, activity, car-app, sqlite, espresso, test-rules,
  junit-ext, datetime, gson, json, commons-*, junidecode, jts, openlocationcode, billing,
  picasso, rhino, taptargetview, textfieldboxes, scribejava, colorpicker, bouncycastle,
  play-review, shimmer, stately, coil, okio, kxml2, sqlite-jdbc)
- **Libraries:** kotlin-stdlib/reflect, androidx-appcompat/material/gridlayout/cardview/browser/
  preference/lifecycle-process/activity/sqlite(-framework)/car-app(-projected), gson, immutables-gson,
  json, commons-compress/logging/codec, junidecode, jts-core, openlocationcode, billing, picasso,
  rhino, taptargetview, textfieldboxes, scribejava, colorpicker, bouncycastle, play-review,
  **play-location**, shimmer, okio, kxml2, sqlite-jdbc, ktor-network/client-mock, kotlinx-datetime,
  hamcrest-core, test-rules/espresso-contrib/junit-ext, stately-concurrent-collections, coil-core/network-okhttp
- **Plugins:** `android-library` (ref agp), `kotlin-multiplatform` y `ivy-publish` **sin versión**
  (resueltos del classpath; ivy-publish es plugin core de Gradle)

### Nota sobre resolución de plugins (patrón aplicado)
La versión se declara **una sola vez en el raíz** (`apply false`); los submódulos aplican **sin versión**
para evitar el error *"already on the classpath with an unknown version"*.

---

## 5. Cadena de errores resueltos

| # | Error | Causa | Solución |
|---|---|---|---|
| 1 | `compileDebugKotlin FAILED` (FeedScreen sintaxis, `net.osmand` unresolved) | Motor desconectado + ediciones rotas post-success | Secciones 3 y 4 |
| 2 | `Cannot add extension 'kotlin'` en `:OsmAnd` | AGP 9 con Kotlin integrado vs `apply plugin: 'kotlin-android'` | `android.builtInKotlin=false` |
| 3 | `Plugin 'de.undercouch.download' not found` | El settings del motor lo declaraba; el nuestro no | Alias con versión en raíz `build.gradle.kts` |
| 4 | `Unexpected plugin type` en settings | download-task es plugin de Project, no de Settings | Movido al raíz `build.gradle.kts` |
| 5 | `No such property 'kotlin' for extension 'libs'` | Catálogo del proyecto sin aliases del motor | Fusión TOML (4.6) |
| 6 | `No such property 'location' for PlayLibraryAccessors` | El motor usa alias `play-location`, nosotros `play-services-location` | Alias adicional `play-location` |
| 7 | `Error resolving plugin kotlin-multiplatform` | KGP ya en classpath con versión desconocida | Declarar KGP versionado solo en raíz; resto sin versión |
| 8 | `Plugin [ivy-publish, apply:false] no-op` | Es plugin core de Gradle | Sin declaración en raíz |
| 9 | `Could not find com.github.HITGIF:TextFieldBoxes` | Falta JitPack | Repo jitpack en settings |
| 10 | `Could not find net.osmand:antpluginlib` | Artefacto publicado en repo **Ivy**, no Maven | Bloque `ivy { patternLayout }` en settings |
| 11 | `Inconsistent JVM Target (Java 17 vs Kotlin 21)` en `:OsmAnd` | KotlinCompile sin jvmTarget explícito | Bloque `allprojects` con JVM_17 |
| 12 | `symbol text_field_boxes_editTextLayout not found` | `nonTransitiveRClass=true` recortaba R de librerías | `android.nonTransitiveRClass=false` |
| 13 | Sobrecarga `padding(horizontal, top)` inexistente | API Compose | Forma start/end/top |

---

## 6. Compilación final

```
.\gradlew.bat :app:assembleDebug        (desde D:\MAPA\Team-Nacional-TX-Aragua)

BUILD SUCCESSFUL in 4m 10s
132 actionable tasks
APK: app\build\outputs\apk\debug\app-debug.apk — 292,5 MB (motor OsmAnd unificado)
Firma debug: my-upload-key.jks (debugConfig)
```

Verificación post-limpieza del workspace: `BUILD SUCCESSFUL in 8s` (incremental, todo UP-TO-DATE).

---

## 7. Despliegue en dispositivos (ADB inalámbrico)

| Dispositivo | Serial TLS | Instalación | Lanzamiento | PID |
|---|---|---|---|---|
| ALI_NX3 | `adb-A9FRUT4315006621-…._adb-tls-connect._tcp` | ✅ Success | ✅ Activo | 12265 |
| Samsung SM_A165M (A16) | `adb-RFGL52W368D-…._adb-tls-connect._tcp` | ✅ Success | ✅ Activo | 11384 |

Lanzamiento vía `monkey -p com.aistudio.teamtxvzla.rkqp -c android.intent.category.LAUNCHER 1`.
`logcat` sin `FATAL EXCEPTION` ni muertes de proceso en ambos equipos.
Confirmado por el usuario: la app abre correctamente.

---

## 8. Limpieza del workspace `D:\MAPA`

### Dependencias vivas identificadas (NO tocadas)
| Carpeta | Razón |
|---|---|
| `Team-Nacional-TX-Aragua/` | El proyecto |
| `android/` | Fuentes del motor (`settings.gradle.kts` → `../android/*`) |
| `resources/` | Estilos de renderizado, fuentes, voces y POI que las tasks `collect*` empaquetan leyendo `../../resources` |
| `.git/`, `.gitignore` | Versionamiento |

### Lo movido a cuarentena `D:\MAPA_LIMPIEZA_20260824\` (~2,9 GB)
- `java_pid2228.hprof` (734 MB heap dump), `startup_logcat.txt`, capturas y notas sueltas
- `BACKUP_OSMAND_MOTOR_20260822_193904/` (351 MB), `BACKUP_TXMAPS_STABLE*/` (581 MB)
- `txmaps/` (copia antigua del proyecto, 336 MB)
- `GRAPHIFY/`, `graphify-out/`, `.graphify/`, `.graphifyignore`, `build_graphify_output.py` (636 MB)
- Raíz Gradle muerta duplicada ("Team-TX-Aragua-Unified"): `settings.gradle.kts`, `build.gradle.kts`,
  `gradle.properties`, `versions.gradle`, `gradle/`, `gradlew*`, `local.properties`, keystores duplicados,
  `.gradle/`, `.kotlin/`, `.idea/`, `.vscode/`, `build/`
- `core/` (fuentes C++ del motor sin referencias desde los scripts actuales), `configuraciones/`,
  `README.md`, `leeme.md`, `.env.example`

### Estructura final de `D:\MAPA`
```
D:\MAPA
├── .git/
├── .gitignore
├── android/                    ← motor OsmAnd (dependencia viva)
├── resources/                  ← assets del mapa (dependencia viva)
└── Team-Nacional-TX-Aragua/    ← EL PROYECTO (abrir ESTA carpeta en Android Studio)
```

---

## 9. Pendientes / recomendaciones

1. **Git:** los archivos versionados movidos a cuarentena aparecen como *deleted* en
   `git status`. Cuando se confirme estabilidad, hacer commit; después se puede borrar
   `D:\MAPA_LIMPIEZA_20260824\` para liberar los ~2,9 GB definitivos. Mientras tanto, todo es reversible.
2. **Android Studio:** abrir siempre `D:\MAPA\Team-Nacional-TX-Aragua` (NO la raíz `D:\MAPA`,
   cuya configuración Gradle fue retirada). Hacer *Gradle Sync* tras abrir.
3. **Comando de compilación:** `.\gradlew.bat :app:assembleDebug`
4. **Blindaje (AGENTS.md en `app/`):** los scripts de integración del motor quedaron restaurados
   al estado de la última compilación exitosa; cualquier cambio futuro en ellos requiere autorización
   explícita según esa política.
