# 📋 REPORTE DE SESIÓN — 26/08/2026
## Reconexión Firebase (Auth + Firestore + Storage) y Blindaje Tiempo Real

> **Alcance:** Módulos Muro, Chat, Autenticación y sincronización en la nube.
> **Reglas respetadas:** `MODULOS_GESTOR.md`, `app/AGENTS.md` (blindaje motor OsmAnd), `ESTRATEGIA_COMPILACION.md`.
> **Resultado final:** ✅ `BUILD SUCCESSFUL in 1m 37s` — sin regresiones. No se tocó ninguna interfaz visual, ningún gradle ni manifest.

---

## 1) DIAGNÓSTICO ENCONTRADO

| Problema | Causa raíz |
|---|---|
| Solo autenticaba la cuenta dueña | APK antiguo firmado con `debug.keystore` (SHA-1 `ED:AB:5F:1B…`) NO registrado → error 10 DEVELOPER_ERROR en otros dispositivos. El SHA correcto de `my-upload-key.jks` (`10:84:47:F9:E6:D1:DD:F2:04:05:E1:9F:3C:5A:E5:02:13:CB:05:50`) SÍ coincide con `google-services.json` |
| Reglas Firestore en modo prueba | Expiraban el **19/09/2026** (todo dejaría de funcionar) y no exigían autenticación |
| Muro/Chat congelados | Los listeners se adjuntaban ANTES de tener sesión activa → `PERMISSION_DENIED` permanente con las reglas nuevas |
| Cualquier cuenta Google entraba | `loginWithGoogle` creaba perfil ASPIRANTE automático sin validar membresía ni invitación |
| Proveedor Anónimo deshabilitado | La app lo usa internamente (`TeamTxApplication.kt:55`, `TeamTxViewModel.kt`, `NUBE_MULTIMEDIA.kt:131`) |

## 2) ACCIONES EN CONSOLA FIREBASE (hechas por el usuario, guiadas por el agente)

1. **Reglas Firestore nuevas pegadas:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```
2. **Proveedor Anónimo habilitado** en Authentication → Sign-in method (ya estaban Email y Google).
3. Verificación de Storage: reglas existentes en `FIREBASE_STORAGE_RULES.txt` son correctas (no se cambiaron).

## 3) CAMBIOS EXACTOS EN EL CÓDIGO (4 archivos)

### A) `app/src/main/java/com/aistudio/teamtxvzla/nube/AUTENTICACION_NUBE.kt`
- ➕ `esperarSesionActiva()`: garantiza sesión antes de operar contra la nube (respaldo anónimo + espera máx. 15 s a sesión Google en progreso).
- ➕ `diagnosticarError(error)`: traduce códigos técnicos (`ERROR_INVALID_CREDENTIAL`, `ERROR_OPERATION_NOT_ALLOWED`, etc.) a causas accionables en Logcat tag `AUTENTICACION_NUBE`.
- ✔️ Se mantuvo intacto `iniciarSesionConGoogle()` y su firma.

### B) `app/src/main/java/com/example/ui/viewmodel/TeamTxViewModel.kt`
- 🔁 En `loginWithGoogle`: se eliminó la creación automática de perfil ASPIRANTE para cuentas desconocidas.
- ➕ Ahora si el email/UID no está en Room local, ni en Firestore `members`, ni en `usuarios/{uid}`, ni es el caso dev especial → **`signOut()` + rechazo** (`Pair(false, mensaje)`).
- ✔️ Esto activa el diálogo YA EXISTENTE `showGooglePhoneLinkDialog` de `GatekeeperAuthScreen.kt:122` — cero cambios visuales.
- ℹ️ Los miembros nuevos ingresan por la pestaña "Código de Acceso" (que sí valida invitación), igual que pide la política del club.

### C) `app/src/main/java/com/example/data/remote/BaseFirestoreSync.kt`
- ➕ Registro de `canalesActivos` por cada listener adjuntado.
- ➕ `authStateListener`: al aparecer/cambiar sesión → re-adjunta automáticamente todos los listeners (recuperación de PERMISSION_DENIED tras logout/cambio de usuario). Guardado anti-duplicados (`listeners.isEmpty()` / cambio real de UID).
- 🔁 `setupChannelListener()`: ahora ejecuta `AutenticacionNube.esperarSesionActiva()` DENTRO de la corutina ANTES de adjuntar el snapshot listener.
- ➕ Log de errores ahora incluye `code=` del error de Firestore.
- ➕ `shutdown()` libera también el `authStateListener`.

### D) `app/src/main/java/com/example/data/remote/FirebaseChatSync.kt`
- Mismo blindaje que C): espera de sesión activa por canal antes de adjuntar, `authStateListener` con guardas anti-duplicado (`isInitialized` + UID distinto), re-conexión automática de los 4 canales (`GENERAL`, `RODADAS`, `MECANICA_AUXILIO`, `DIRECTIVA`), liberación en `shutdown()`.
- ➕ Logs con `code=` del error.

## 4) ARCHIVOS NUEVOS EN LA RAÍZ

| Archivo | Contenido |
|---|---|
| `GUIA_FIREBASE_COLECCIONES.md` | Mapa completo de las 17 rutas Firestore usadas por los módulos (feed, notice_comments, chat_channels/{4}/messages, members, usuarios/{uid}, rides, ride_registrations, finances, inventory, equipment_loans, emergencies, invitations, access_requests, role_configs, app_settings/global, configuracion/OTA, mensajes_chat legacy), campos/tipos/enums válidos, paso a paso de consola y filtros Logcat por módulo |
| `REPORTE_SESION_20260826.md` | Este reporte |

**Colecciones creadas manualmente en consola (guiado):**
- `chat_channels` → docs: GENERAL, RODADAS, MECANICA_AUXILIO, DIRECTIVA (campo `nombre`)
- Opcionales documentadas: `app_settings/global {chatEnabled:true}` y `configuracion/OTA {versionCode,urlDescarga,notas}`
- El resto se auto-crean al primer uso desde la app.

## 5) VERIFICACIÓN

```
.\gradlew.bat :app:assembleDebug
BUILD SUCCESSFUL in 1m 37s
128 actionable tasks: 8 executed, 1 from cache, 119 up-to-date
```
- APK generado: `app\build\outputs\apk\debug\app-debug.apk`
- Sin errores de compilación; sin cambios en gradles/manifiestos/UI/motor OsmAnd.

## 6) PENDIENTE / SIGUIENTE PASO

- [ ] Instalar APK nuevo en ambos dispositivos de confianza (RFGL52W368D, A9FRUT4315006621) — los que tengan APK viejo firmado con debug.keystore seguirán fallando Google hasta reinstalar.
- [ ] Iniciar sesión → verificar en consola la aparición de `feed`, `members`, `usuarios`.
- [ ] Probar login Google con cuenta NO registrada → debe rechazar mostrando el diálogo.
- [ ] Probar Chat en tiempo real entre ambos dispositivos.
- [ ] Si falla algo: Logcat con filtros `AUTENTICACION_NUBE`, `FIREBASE_SYNC`, `TEAM_TX_CHAT`.
