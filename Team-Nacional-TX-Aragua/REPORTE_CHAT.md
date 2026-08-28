# REPORTE TECNICO: MODULO DE CHAT - Team Nacional TX Aragua

**Fecha:** 26 de Agosto, 2026  
**Estado:** En desarrollo (PERMISSION_DENIED pendiente de resolver)

---

## 1. ARQUITECTURA GENERAL

El chat funciona bajo un modelo **offline-first con sincronización dual** (Room local + Firestore remoto):

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  ClubChatScreen  │────▶│  TeamTxViewModel  │────▶│ TeamTxRepository│
│   (Compose UI)   │     │   (StateFlow)     │     │   (Coordinator)  │
└─────────────────┘     └──────────────────┘     └────────┬────────┘
                                                          │
                                           ┌──────────────┴──────────────┐
                                           │                             │
                                   ┌───────▼───────┐          ┌─────────▼─────────┐
                                   │  ChatDao      │          │ FirebaseChatSync  │
                                   │  (Room/SQLite)│          │ (Firestore remote) │
                                   └───────┬───────┘          └─────────┬─────────┘
                                           │                            │
                                   ┌───────▼───────┐          ┌─────────▼─────────┐
                                   │ AppDatabase   │          │  Firestore        │
                                   │ v15           │          │  chat_channels/   │
                                   └───────────────┘          └───────────────────┘
```

---

## 2. CANALES DE CHAT

Definidos en `ClubChatScreen.kt:36-47` como enum `ChatChannel`:

| Canal ID          | Nombre              | Descripción                                    | Acceso         |
|-------------------|---------------------|------------------------------------------------|----------------|
| `GENERAL`         | #General TX         | Conversación abierta, hermandad y fotos        | Todos          |
| `RODADAS`         | #Rutas & Rodadas    | Coordinación de salidas, ritmo y puntos        | Todos          |
| `MECANICA_AUXILIO`| #Auxilio Mecánico   | Diagnósticos, repuestos y ayuda técnica        | Todos          |
| `DIRECTIVA`       | #Directiva & Staff  | Consejo de directiva y administración central  | Solo Directiva |

**Nota:** Los canales mostrados se filtran según `showOnlyDirectiva`. Si es `true`, solo se muestra el canal DIRECTIVA.

---

## 3. MODELO DE DATOS

### ChatMessage (Room Entity: `club_chat_messages`)
Ubicación: `Models.kt:659-685`

| Campo                | Tipo          | Descripción                                    |
|----------------------|---------------|------------------------------------------------|
| `id`                 | `Long`        | ID primario, generado con `System.currentTimeMillis()` |
| `channelId`          | `String`      | Canal al que pertenece (GENERAL, RODADAS, etc.) |
| `senderMemberId`     | `Long`        | ID del miembro en `member_profiles`            |
| `senderName`         | `String`      | Nombre completo del remitente                  |
| `senderNickname`     | `String`      | Alias del remitente                            |
| `senderMemberNumber` | `String`      | Número de miembro (ej: TX-000)                 |
| `senderRole`         | `MemberRole`  | Rol del remitente (MIEMBRO_ACTIVO, PRESIDENTE, etc.) |
| `senderCustomRoleTitle` | `String?`  | Título custom del rol (de `role_configs`)      |
| `senderInitials`     | `String`      | Iniciales del avatar                           |
| `senderPhotoUrl`     | `String?`     | URL de foto de perfil (Google Storage o manual) |
| `messageText`        | `String`      | Contenido del mensaje                          |
| `isRadioCallout`     | `Boolean`     | Si es llamada de radio (diseño especial)        |
| `timestamp`          | `Long`        | Timestamp de creación en milisegundos          |
| `stickerFileName`    | `String?`     | Nombre del archivo del sticker                 |
| `stickerFilePath`    | `String?`     | URL remota del sticker (Firebase Storage)      |
| `messageType`        | `MessageType` | TEXT, STICKER, IMAGE, AUDIO, LOCATION          |
| `readBy`             | `List<Long>`  | Lista de IDs de miembros que leyeron el mensaje |

### MessageType (Enum)
```kotlin
enum class MessageType {
    TEXT, STICKER, IMAGE, AUDIO, LOCATION
}
```

---

## 4. FLUJO DE ENVÍO DE MENSAJES

### 4.1 Envío de texto
```
Usuario escribe → Botón Enviar → sendChatMessage()
  → ViewModel crea ChatMessage con ID = System.currentTimeMillis()
  → repository.insertChatMessage(message)
    → FirebaseChatSync.sendMessage(message)
      → 1. INSERT en Room (ChatDao.insertMessage)
      → 2. Envío a Firestore via pendingWritesChannel
        → docRef.set(message, SetOptions.merge())
        → Ruta: chat_channels/{channelId}/messages/{id}
```

**Código clave:** `TeamTxViewModel.kt:818-840`

### 4.2 Envío de sticker
```
Usuario selecciona sticker → sendStickerMessage()
  → Upload a Firebase Storage (chat_stickers/)
  → Obtener downloadUrl
  → Crear ChatMessage con messageType=STICKER
  → repository.insertChatMessage(message)
  → Mismo flujo que texto
```

**Código clave:** `TeamTxViewModel.kt:842-878`

### 4.3 Recepción de mensajes (escucha en tiempo real)
```
FirebaseChatSync.setupChannelListener()
  → Escucha Firestore: chat_channels/{canal}/messages (ordenados por timestamp, últimos 200)
  → SnapshotListener recibe documentos
  → Parsea a ChatMessage via toObject()
  → Guarda en Room via chatDao().upsertMessages()
  → Room notifica a Flow<List<ChatMessage>>
  → ViewModel actualiza StateFlow
  → Compose recomienda la UI
```

**Código clave:** `FirebaseChatSync.kt:72-112`

---

## 5. SINCRONIZACIÓN FIREBASE

### 5.1 Rutas en Firestore
```
chat_channels/
  ├── GENERAL/
  │   └── messages/
  │       ├── 1787785688999  (doc ID = ChatMessage.id)
  │       ├── 1787782931285
  │       └── ...
  ├── RODADAS/
  │   └── messages/
  ├── MECANICA_AUXILIO/
  │   └── messages/
  └── DIRECTIVA/
      └── messages/
```

### 5.2 Reglas de Firestore (requiere configuración)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### 5.3 Configuración de Chat (app_settings)
```
app_settings/global/
  └── chatEnabled: Boolean  (true/false, controlado por Directiva)
```

Escuchado en `TeamTxViewModel.kt:55-59` para habilitar/deshabilitar el chat globalmente.

### 5.4 Stickers en Firebase Storage
```
chat_stickers/
  └── {UUID}_{filename}  (archivos subidos por usuarios)
```

---

## 6. AUTENTICACIÓN Y SEGURIDAD

### 6.1 Flujo de Auth
Ubicación: `AUTENTICACION_NUBE.kt`

1. **Login con Google:** `iniciarSesionConGoogle(tokenId)` → `signInWithCredential(GoogleAuthProvider)`
2. **Sesión anónima (respaldo):** `esperarSesionActiva()` → `signInAnonymously().await()`
3. **Auth State Listener:** Re-adjunta listeners de Firestore cuando la sesión cambia

### 6.2 Protección de listeners
- `FirebaseChatSync` espera a que `esperarSesionActiva()` termine antes de adjuntar listeners
- `AuthStateListener` detecta cambios de sesión y re-adjunta todos los listeners de canales
- `isInitialized` flag evita doble inicialización

### 6.3 Control de acceso por canal
- Canal `DIRECTIVA` tiene `directivaOnly = true`
- La UI filtra canales según `showOnlyDirectiva` (true cuando el usuario tiene rol de Directiva)

---

## 7. UI - CLUB CHAT SCREEN

### 7.1 Estructura de la pantalla
```
┌──────────────────────────────────────────────┐
│  Header: Chips de canal + Botón ⋮ settings   │
├──────────────────────────────────────────────┤
│  Barra de descripción del canal activo       │
├──────────────────────────────────────────────┤
│                                              │
│  Lista de mensajes (LazyColumn)              │
│  ┌─────────────────────────────────────────┐ │
│  │ Avatar + Nombre + Rol + Badge          │ │
│  │ ┌─────────────────────────────────┐    │ │
│  │ │ Burbuja del mensaje             │    │ │
│  │ │ (color según autor/propio)      │    │ │
│  │ └─────────────────────────────────┘    │ │
│  │ Hora + Checks de lectura (✓/✓✓)        │ │
│  └─────────────────────────────────────────┘ │
│                                              │
├──────────────────────────────────────────────┤
│  Input bar: 📎 Menu + Campo + 😊 Stickers    │
│             + 🔴 Send/Mic                   │
├──────────────────────────────────────────────┤
│  Sticker box (expandible)                    │
└──────────────────────────────────────────────┘
```

### 7.2 Burbujas de mensaje (`ChatMessageBubble`)
- **Mensajes propios:** Alineados a la derecha, color `TxFlameRed` (rojo/naranja)
- **Mensajes de otros:** Alineados a la izquierda, color `#2A2F3E` (gris oscuro)
- **Mensajes de radio:** Borde dorado, fondo especial `#241C10`
- **Avatares:** Circulares con borde del color del rol
- **Read receipts:**
  - ✓ (1 check) = Enviado (blanco semi-transparente)
  - ✓✓ (2 checks gris) = Entregado
  - ✓✓ (2 checks azul `#4FC3F7`) = Visto

### 7.3 Long-press context menu
Opciones: Copiar, Reenviar, Eliminar (solo propios), Info

### 7.4 Configuración del chat (menú ⋮)
- Notificaciones ON/OFF
- Sonido de mensajes ON/OFF
- Info del canal
- Diálogo de configuración: Sonido, Vibración, Notificaciones, Mostrar avatares, Limpiar chat

---

## 8. PREFERENCIAS DEL CHAT

Archivo: `PreferenciasChat.kt` (SharedPreferences)

| Preferencia          | Key                        | Default | Tipo      |
|----------------------|----------------------------|---------|-----------|
| Sonido de mensajes   | `sonido_mensajes`          | `true`  | Boolean   |
| Vibración            | `vibracion`                | `true`  | Boolean   |
| Notificaciones canal | `notificaciones_canal`     | `true`  | Boolean   |
| Tema claro           | `tema_claro`               | `false` | Boolean   |
| Mostrar avatares     | `mostrar_avatares`         | `true`  | Boolean   |
| Enviar con Enter     | `enviar_con_enter`         | `false` | Boolean   |
| Tamaño de fuente     | `tamano_fuente`            | `14f`   | Float     |

### Sonidos disponibles
- `sonidoMensajeEnviado()` → Tono ACK corto (60ms)
- `sonidoMensajeRecibido()` → Tono BEEP suave (80ms)
- `sonidoRadio()` → Tono CDMA alert (150ms)
- `sonidoSticker()` → Tono PROMPT (50ms)

---

## 9. NOTIFICACIONES INTERNAS

Archivo: `NOTIFICACIONES_APP.kt` + `NotificacionApp.kt` (Room Entity)

El módulo de notificaciones internas de la app (no Firebase Cloud Messaging) se integra con el chat:

| Tipo          | Descripción                    | Icono          | Color          |
|---------------|--------------------------------|----------------|----------------|
| `CHAT`        | Mensaje nuevo en canal         | `chat_bubble`  | `#FF6B35`      |
| `MURO`        | Publicación en el muro         | `article`      | `#4CAF50`      |
| `SOS`         | Alerta de emergencia           | `emergency`    | `#FF1744`      |
| `PERFIL`      | Actualización de perfil        | `person`       | `#2196F3`      |
| `VINCULAR_GOOGLE` | Vinculación con Google    | `link`         | `#7C4DFF`      |
| `GENERAL`     | Notificación genérica          | `notifications`| `#9E9E9E`      |

Tabla Room: `notificaciones_app` (v15 de AppDatabase)

---

## 10. PROBLEMA ACTUAL: PERMISSION_DENIED

### Síntoma
Todos los errores en Logcat muestran:
```
PERMISSION_DENIED: Missing or insufficient permissions.
at chat_channels/GENERAL/messages/{id}
```

### Causa raíz
1. Las reglas de Firestore requieren `request.auth != null`
2. La sesión anónima (`signInAnonymously()`) no se está estableciendo correctamente
3. El log confirma: `"❌ Tras 15 s no hay sesión activa: Firestore/Storage rechazarán operaciones."`
4. Sin sesión activa, `request.auth` es `null` → Firestore rechaza la escritura

### Solución requerida (Firebase Console)
1. **Authentication → Sign-in method** → Habilitar "Anonymous"
2. **Firestore Database → Reglas** → Publicar:
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```
3. Cerrar y reabrir la app después de publicar reglas

---

## 11. ARCHIVOS RELACIONADOS

| Archivo | Función |
|---------|---------|
| `ClubChatScreen.kt` | UI completa del chat (Compose) |
| `FirebaseChatSync.kt` | Sincronización con Firestore |
| `PreferenciasChat.kt` | Configuración del chat (SharedPreferences + sonidos) |
| `NOTIFICACIONES_APP.kt` | Módulo de notificaciones internas |
| `NotificacionApp.kt` | Entity Room para notificaciones |
| `NotificacionDao.kt` | DAO para notificaciones |
| `Models.kt` | ChatMessage, MessageType, ChatChannel |
| `TeamTxDao.kt` | ChatDao (líneas 190-206) |
| `TeamTxRepository.kt` | Capa de acceso a datos (líneas 107-111) |
| `TeamTxViewModel.kt` | Lógica de chat (líneas 804-878) |
| `AUTENTICACION_NUBE.kt` | Autenticación Firebase |
| `BaseFirestoreSync.kt` | Base de sincronización con auth guard |
| `AppDatabase.kt` | Room DB v15, incluye ChatDao |

---

## 12. TABLA Room `club_chat_messages`

```sql
CREATE TABLE club_chat_messages (
    id INTEGER PRIMARY KEY NOT NULL,
    channelId TEXT NOT NULL DEFAULT 'GENERAL',
    senderMemberId INTEGER NOT NULL DEFAULT 0,
    senderName TEXT NOT NULL DEFAULT '',
    senderNickname TEXT NOT NULL DEFAULT '',
    senderMemberNumber TEXT NOT NULL DEFAULT '',
    senderRole TEXT NOT NULL DEFAULT 'ASPIRANTE',
    senderCustomRoleTitle TEXT,
    senderInitials TEXT NOT NULL DEFAULT '',
    senderPhotoUrl TEXT,
    messageText TEXT NOT NULL DEFAULT '',
    isRadioCallout INTEGER NOT NULL DEFAULT 0,
    timestamp INTEGER NOT NULL DEFAULT 0,
    stickerFileName TEXT,
    stickerFilePath TEXT,
    messageType TEXT NOT NULL DEFAULT 'TEXT',
    readBy TEXT NOT NULL DEFAULT '[]'  -- JSON serialization de List<Long>
);
```

**Nota sobre `readBy`:** Se almacena como JSON serializado en Room. La serialización/deserialización se maneja en el código con TypeConverter.
