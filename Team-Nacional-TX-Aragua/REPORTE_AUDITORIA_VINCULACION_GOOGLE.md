# 🛡️ INFORME DE AUDITORÍA SENIOR DE ARQUITECTURA MÓVIL
## Plataforma: Android (Kotlin / Jetpack Compose) & Firebase Firestore
**Proyecto:** Team Nacional TX Aragua  
**Fecha:** Septiembre 2026  
**Área de Auditoría:** Sistema de Vinculación Google Identity, Persistencia de Sesión, Manejo Multicuenta y Anti-Trampas en Gamificación  
**Estado de la Evaluación:** Puntos Críticos Identificados

---

## 1. FLUJO DE VINCULACIÓN ACTUAL

### 1.1 Punto de Invocación y Rastreo de Código
* **Interfaz de Usuario (UI):**  
  En el Carnet TX (`ProfileAndAdminScreen.kt`, líneas 86-98), se declara el lanzador de Google mediante `AutenticacionGoogle.recordarLauncherGoogle`.
  El usuario acciona el proceso mediante el botón *"Vincular con Google"* (líneas 513-540), el cual ejecuta `googleHelper.abrirSelector()`.
* **Procesamiento Criptográfico y Auth:**  
  En `AUTENTICACION_GOOGLE.kt` (líneas 108-228), se recibe el Intent, se extrae el `idToken` y se envía a Firebase mediante `FirebaseAuth.getInstance().signInWithCredential(...)`.  
  Devuelve el objeto `DatosUsuarioGoogle(uid, nombre, correo, fotoUrl, esNuevoRegistro)`.
* **Despacho a MainActivity:**  
  `MainActivity.kt` (líneas 1047-1055) recibe el callback, persiste temporalmente `PreferenciasApp.carnetGooglePhotoUrl = photo` y llama a `viewModel.vincularGoogle(uid, email, photo)`.

### 1.2 Datos Guardados Localmente y en Firestore
* **Persistencia Local (Room & SharedPreferences):**
  * `MemberProfile` en SQLite (Room): actualiza las columnas `firebaseUid = uid`, `email = email` y `profilePhotoUri = photo`.
  * `SharedPreferences`: guarda `PREF_LOGGED_IN_MEMBER_ID`, `PREF_LOGGED_IN_EMAIL` y `PREF_LOGGED_IN_FIREBASE_UID`.
* **Persistencia en la Nube (Firestore):**
  * Se escribe en el documento `usuarios/{uid}` dentro de `PerfilNube.kt` (líneas 88-136). La clave primaria del documento es el UID alfanumérico generado por Firebase Auth. Se serializan 45 propiedades (nombre, cédula, grupo sanguíneo, kilómetros totales, odómetro, contactos de emergencia, puntos de mérito, etc.).
  * También se respalda en la colección general `members/{member.id}`.

### 1.3 Validación Previa de Asignación (1-a-1)
* **Diagnóstico:** **INEXISTENTE en la arquitectura base.**
* **Falla:** `vincularGoogle` no ejecutaba ninguna consulta previa a Firestore (`whereEqualTo("email", ...)` o `whereEqualTo("firebaseUid", ...)`). Tampoco existía una colección autoritativa de asignación. Si el correo ya pertenecía a otro piloto, la app procedía con el enlace de forma ciega.

---

## 2. PERSISTENCIA Y MANEJO MULTICUENTA

### 2.1 Conmutación de Perfiles en el Mismo Dispositivo
* Cuando un usuario cambia de código de invitación (ej. `PILOTO1` a `DESARROLLO1`), la función `validateInvitationCode` busca en Room si existe el número de miembro asociado (`TX-PIL-001` vs `TX-DEV-001`).
* Si existe, actualiza el puntero reactivo `_currentMemberId.value`.
* **Vulnerabilidad de Concurrencia:** La tabla `members` de Room mantiene los perfiles de todos los pilotos que hayan ingresado en ese celular. Si la sesión anterior no cerró sus listeners en tiempo real (`PerfilNube.detenerSincronizacionDePerfil()`), los WebSockets de Firestore siguen sincronizando datos del UID anterior hacia el perfil local activo.

### 2.2 Residuos en SharedPreferences y Caché de Imágenes
* **Datos Residuales Identificados al Cerrar Sesión:**
  * `PreferenciasApp.carnetGooglePhotoUrl`: Si no se limpia, el nuevo piloto que entre en ese teléfono hereda visualmente la foto de Google del piloto previo.
  * `radar_prefs` (`radar_avatar`, `radar_alias`, `radar_piloto_foto`): Permanecían en el almacenamiento privado y transmitían la foto del usuario viejo en el radar en vivo.
  * Caché de memoria de Coil (`ImageLoader`): Mantenía en memoria RAM los mapas de bits de los carnets anteriores.
* **Seguridad de Tokens:** Si no se ejecuta un `signOut()` explícito en `FirebaseAuth`, las solicitudes a Firebase Storage continúan firmadas con el UID del usuario previo.

---

## 3. IDENTIFICACIÓN DEL DISPOSITIVO Y SESIÓN ÚNICA

### 3.1 Identificador Persistente (DeviceId)
* **Diagnóstico:** El proyecto **NO contaba** con ningún generador o registro de identificador de dispositivo (`DeviceId`, `UUID` o `ANDROID_ID`).
* La sesión era tratada como "ubicua", asumiendo erróneamente que un UID solo existiría en un teléfono a la vez.

### 3.2 Comportamiento Multi-Dispositivo del Listener de Firestore
* `PerfilNube.escucharPerfil(uid)` registra un `addSnapshotListener` directo sobre `usuarios/{uid}`.
* Si el mismo usuario abre la app en el Teléfono A y en el Teléfono B:
  1. No existe bloqueo ni desconexión en ninguno de los dos equipos.
  2. Cada modificación hecha en el Teléfono A viaja a Firestore y se refleja de inmediato en el Teléfono B.
  3. **Efecto de Bucle Concurrente:** Ambos teléfonos ejecutan corrutinas de actualización en Room. Si ambos tienen activado el colector de cambios locales (`perfilSyncJob`), se produce un ping-pong de sincronizaciones que satura las cuotas de lectura/escritura de Firestore.

---

## 4. PROCESO DE DESVINCULACIÓN

### 4.1 Estado Original de la Desvinculación
* **Diagnóstico:** **NO existía ningún botón ni método de desvinculación.**
* En `ProfileAndAdminScreen.kt` (línea 574), el único botón disponible era *"Cambiar"*.
* Al presionar *"Cambiar"*, se ejecutaba un nuevo inicio de sesión sobreescribiendo los valores locales, sin liberar las credenciales en Google ni revocar el acceso (`revokeAccess()`).

### 4.2 Estado de los Registros en la Nube
* Al no haber desvinculación, el documento `usuarios/{uid}` quedaba permanentemente ocupado.
* Si un piloto intentaba usar su correo personal para registrarse en otro perfil (por ejemplo, al ser ascendido a Directiva con un código `DIRECTIVO1`), era imposible liberar el correo sin manipular directamente la consola web de Firebase.

---

## 5. VULNERABILIDADES CRÍTICAS DETECTADAS

```
+----------------------------------------------------------------------------------------------------+
| 🚨 VULNERABILIDAD #1: COLISIÓN DE UID Y MUTACIÓN DE IDENTIDAD                                      |
| Severidad: CRÍTICA (CVSS 8.9)                                                                      |
| Descripción: Al vincular la misma cuenta Google en dos perfiles locales diferentes (ej. piloto1 y  |
| desarrollo1), Firebase asigna el mismo UID. El segundo perfil sobreescribe el documento en         |
| Firestore usuarios/{uid}. El primer teléfono, al estar escuchando ese documento, recibe el cambio |
| en vivo y muta silenciosamente a la identidad del segundo perfil.                                 |
+----------------------------------------------------------------------------------------------------+
| 🚨 VULNERABILIDAD #2: FRAUDE EN GAMIFICACIÓN POR SESIÓN MULTI-DISPOSITIVO                          |
| Severidad: CRÍTICA (CVSS 8.2)                                                                      |
| Descripción: La ausencia de control de sesión única (activeDeviceId) permite que una misma        |
| cuenta acumule kilometraje y complete retos simultáneamente desde dos o más celulares en ruta,    |
| alterando el odómetro, las insignias de mérito y el Ranking Nacional sin ser detectado.           |
+----------------------------------------------------------------------------------------------------+
| ⚠️ VULNERABILIDAD #3: RESIDUALIDAD DE SESIÓN Y FUGA DE FOTOS EN DISPOSITIVOS COMPARTIDOS           |
| Severidad: ALTA (CVSS 7.1)                                                                         |
| Descripción: El cierre de sesión anterior no purgaba la caché de imágenes de Coil ni las           |
| preferencias del radar, permitiendo que un nuevo usuario que ingrese en el mismo equipo herede la  |
| foto de perfil, el alias y los permisos en memoria del piloto saliente.                           |
+----------------------------------------------------------------------------------------------------+
```

---

## 6. RECOMENDACIONES DE ARQUITECTURA (PLAN DE ACCIÓN)

1. **Tabla de Autoridad 1-a-1 en Firestore (`google_bindings`):**
   Crear la colección `google_bindings` con ID = `email_sanitizado`. Al intentar vincular, verificar si el correo ya está ocupado por otro miembro (`memberNumber`). Si está ocupado por otro, rechazar con mensaje de error bloqueante.
2. **Dispositivo Activo Obligatorio (`activeDeviceId`):**
   Generar un UUID persistente por instalación (`PreferenciasApp.obtenerDeviceId()`). Guardar este identificador en `google_bindings` y `usuarios/{uid}`. En el listener en vivo, si `activeDeviceId != localDeviceId`, pausar la sesión de inmediato alertando al piloto.
3. **Mecanismo Formal de Desvinculación:**
   Incorporar botón `[Desvincular]` en Carnet TX con confirmación. Al pulsar, borrar el documento de `google_bindings`, desasociar `firebaseUid` localmente y ejecutar `signOut()` + `revokeAccess()` en Google SignIn.
4. **Respaldo Atómico previo a Logout:**
   Antes de limpiar la memoria local en `logout()`, asegurar que el perfil completo se suba a Firestore para que el usuario no pierda ningún dato al cambiar de teléfono.
