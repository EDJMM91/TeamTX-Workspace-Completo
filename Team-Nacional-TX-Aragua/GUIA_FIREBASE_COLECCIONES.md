# 🔥 GUÍA FIREBASE — COLECCIONES Y CONFIGURACIÓN (Team Nacional TX Aragua)

> **Fuente de verdad:** escaneo completo del código (`data/remote/*`, `chat/*`, `nube/*`, `ACTUALIZADOR.kt`, `TeamTxViewModel.kt`).
> **Regla clave:** Firestore crea colecciones **automáticamente** al primer documento escrito por la app. Solo se crean a mano las marcadas como **MANUAL**.
> ⚠️ Respetar EXACTAMENTE nombres (mayúsculas/minúsculas) y tipos. Un tipo mal puesto rompe la lectura `toObject()`.

---

## ✅ Configuración previa (YA HECHA)

| Ítem | Estado |
|---|---|
| Reglas Firestore: `allow read, write: if request.auth != null` | ✅ Pegadas |
| Proveedores Auth: Email, Google, **Anónimo** | ✅ Habilitados |
| SHA-1 `my-upload-key.jks` = `10:84:47:F9:E6:D1:DD:F2:04:05:E1:9F:3C:5A:E5:02:13:CB:05:50` | ✅ Registrado en JSON/consola |
| `google-services.json` (paquete `com.aistudio.teamtxvzla.rkqp`) | ✅ Correcto |

---

## 🗺️ MAPA MAESTRO DE COLECCIONES (17 rutas)

| # | Ruta Firestore | Módulo que la usa | Creación |
|---|---|---|---|
| 1 | `feed` | **Muro** (avisos) | 🤖 AUTO al iniciar sesión (aviso de bienvenida) |
| 2 | `notice_comments` | **Muro** (comentarios) | 🤖 AUTO al primer comentario |
| 3 | `chat_channels/{CANAL}/messages` | **Chat** | 🟡 MANUAL los 4 canales padre (mensajes = AUTO) |
| 4 | `members` | **Miembros / Carnet / Login** | 🤖 AUTO al registrarse/sincronizar perfil |
| 5 | `usuarios/{uid}` | Respaldo de perfil por UID (PerfilNube) | 🤖 AUTO |
| 6 | `rides` | **Rodadas** | 🤖 AUTO al crear la 1ra rodada |
| 7 | `ride_registrations` | Inscripciones a rodadas | 🤖 AUTO |
| 8 | `finances` | **Tesorería** | 🤖 AUTO al 1ra transacción |
| 9 | `inventory` | **Inventario** | 🤖 AUTO |
| 10 | `equipment_loans` | Préstamos de equipo | 🤖 AUTO |
| 11 | `emergencies` | **SOS Vial** | 🤖 AUTO al 1ra alerta |
| 12 | `invitations` | Códigos de invitación (24h) | 🤖 AUTO (Directiva genera códigos) |
| 13 | `access_requests` | Solicitudes de ingreso | 🤖 AUTO (formulario del portón) |
| 14 | `role_configs` | Títulos/permisos personalizados de roles | 🤖 AUTO |
| 15 | `app_settings/global` | Switch global del Chat | 🔴 MANUAL (recomendado) |
| 16 | `configuracion/OTA` | **Actualizar App** (OTA) | 🔴 MANUAL (obligatoria para OTA) |
| 17 | `mensajes_chat` | Chat legacy (`chat/NUBE_MENSAJES.kt`) | 🤖 AUTO si ese módulo se usa |

---

## 🔴 MANUALES — paso a paso

### 1) `chat_channels` — Canales del Chat

Iniciar colección → ID: `chat_channels`. Primer documento:

- **ID de documento:** `GENERAL`
- Campo obligatorio mínimo: `nombre` (string) = `GENERAL`

Luego botón "Agregar documento" para los otros 3:

| ID de documento | campo nombre (string) |
|---|---|
| `RODADAS` | RODADAS |
| `MECANICA_AUXILIO` | MECANICA_AUXILIO |
| `DIRECTIVA` | DIRECTIVA |

⚠️ IDs exactos en MAYÚSCULAS y sin acentos — el código busca por ellos.

Subcolección `messages`: NO crear a mano; nace al enviar el primer mensaje.

#### Campos de cada mensaje (referencia — modelo `ChatMessage`)
| Campo | Tipo | Ejemplo/Default |
|---|---|---|
| id | number | timestamp |
| channelId | string | GENERAL / RODADAS / MECANICA_AUXILIO / DIRECTIVA |
| senderMemberId | number | 0 |
| senderName / senderNickname / senderMemberNumber | string | "" |
| senderRole | string | ver MemberRole abajo |
| senderCustomRoleTitle | null/string | null |
| senderInitials | string | "TX" |
| senderPhotoUrl | null/string | null |
| messageText | string | "" |
| isRadioCallout | boolean | false |
| timestamp | number | — |
| stickerFileName / stickerFilePath | null/string | null |
| messageType | string | TEXT / STICKER / IMAGE / AUDIO / LOCATION |
| readBy | array (numbers) | [] |

### 2) `app_settings` — Ajustes globales

- Iniciar colección → ID: `app_settings`
- **ID de documento:** `global`
- Campos:

| Campo | Tipo | Valor |
|---|---|---|
| chatEnabled | boolean | true |

### 3) `configuracion` — OTA (Actualizar App)

⚠️ Ruta EXACTA: colección `configuracion` (minúsculas), documento `OTA` (MAYÚSCULAS). La lee `ACTUALIZADOR.kt`.

| Campo | Tipo | Valor ejemplo |
|---|---|---|
| versionCode | number | debe ser MAYOR al actual instalado (actual: 8) |
| urlDescarga | string | https://…/TeamTX-v1.2.8.apk |
| notas | string | "Mejoras del módulo X" |

Si el documento no existe o versionCode ≤ instalado → no hay actualización (comportamiento normal).

---

## 📚 REFERENCIA DE CAMPOS — Colecciones automáticas
*(La app las llena sola; sirve para verificar tipos en consola)*

### `feed` (Publicación del Muro)
id:number · title:string · content:string · category:string(`AVISO_OFICIAL`/`RETO_MOTERO`/`COMUNICADO`/`NOTICIA_RUTA`/`CAPACITACION`) · priority:string(`NORMAL`/`IMPORTANTE`/`URGENTE`) · authorName:string · authorRole:string · timestamp:number · likesCount:number · isPinned:boolean · targetChallengeDistanceKm:number · challengeBadgeText:null · telegramPostUrl:null · imageUrl:null(string URL Firebase Storage) · allowComments:boolean

### `notice_comments`
id:number · publicationId:number · memberId:number · authorName/authorNickname/authorMemberNumber:string · authorRole:string(MemberRole) · authorInitials:string · content:string · timestamp:number

### `members` (Perfil del miembro)
id:number · fullName/nickname/memberNumber:string · cedulaDni/phone:string · role:string(**MemberRole**) · chapterState/birthDate:string · bikeBrand/bikeModel/bikeColor/bikeDisplacementCc/bikeTankCapacityLiters/bikeYear/bikePlate:string · bloodType/medicalNotes:string · emergencyContactName/Phone/Relation:string · isDirectiva:boolean · solvencyStatus:boolean · joinYear/avatarInitials:string · isSuspended:boolean · suspensionReason:string · suspensionDurationDays:number · suspendedBy:string · suspensionStartTimestamp/suspensionEndTimestamp:number · isOnline:boolean · lastActiveTimestamp:number · licenseImageUri/medicalCertImageUri/bikeRegImageUri/insuranceImageUri:null · copilotName/copilotRelation:null · attendanceCount/longRidesCount/bigEventsCount:number · prospectStartDate:null · profilePhotoUri/bikePhotoUri:null(URL) · firebaseUid/email:null(string)

**MemberRole válido:** PRESIDENTE, VICEPRESIDENTE, DISCIPLINARIO, SECRETARIO, TESORERO, DIRECTIVA, CAPITAN_RUTA, SEGURIDAD_VIAL, MECANICO_OFICIAL, MEDICO_CLUB, PARAMEDICO_MOTERO, MIEMBRO_ACTIVO, ASPIRANTE, COPILOTO, INVITADO

### `usuarios/{uid}` (mismo contenido que members, anclado al UID de Firebase)

### `rides` (Rodada)
id:number · title/description/originCity/destinationCity/departureDate/meetingTime:string · departureTimestamp:number · distanceKm:number · terrainType:string · convoyLeader/secondLeader/roadSafetyOfficer/tailRider/medicOfficer/mechanicOfficer:string · gasStops/requiredGear:string · status:string(`PROGRAMADA`/`EN_CURSO`/`FINALIZADA`/`CANCELADA`) · costUsdCents/costVesCents:number · registeredCount/maxParticipants:number · whatsappGroupUrl:null

### `ride_registrations`
id:number · rideId/memberId:number · memberName/memberAlias/bikePlate:string · hasPillion:boolean · pillionName:string · convoyRole:string(PILOTO_CENTRAL, PUNTERO, SEGUNDO_GUIA, OFICIAL_SEGURIDAD, MEDICO, MECANICO, BARREDOR_COLA, COPILOTO…) · paymentConfirmed/checkedIn:boolean · registeredAt:number

### `finances` (Tesorería)
id:number · concept/description/referenceCode/memberName/memberNumber:string · category:string(`MEMBRESIA_MENSUAL`/`POTE_EVENTO`/`DONACION_BENEFICA`/`FONDO_EMERGENCIA`/`GASTO_LOGISTICA`/`COMPRA_REPUESTOS`) · type:string(`INGRESO`/`GASTO`) · amountUsdCents/amountVesCents:number · paymentMethod:string(`PAGO_MOVIL`/`BINANCE_PAY`/`ZELLE`/`EFECTIVO_DIVISAS`/`EFECTIVO_BS`/`TRANSFERENCIA_BANCARIA`) · status:string(`VERIFICADO`/`PENDIENTE`/`RECHAZADO`) · timestamp:number · approvedBy:string

### `inventory`
id:number · code/name/description/location:string · category:string(`HERRAMIENTAS_RUTA`/`EQUIPO_EVENTO`/`PRIMEROS_AUXILIOS`/`COMUNICACIONES`/`REPUESTOS_COMUNITARIOS`) · totalStock/availableStock:number · condition:string(`EXCELENTE`/`OPERATIVO`/`MANTENIMIENTO`/`DANADO`) · lastMaintenanceTimestamp:number · custodianName:string

### `equipment_loans`
id:number · itemId:number · itemName/itemCode:string · borrowerName/borrowerPhone/borrowerMemberNumber:string · loanTimestamp:number · expectedReturnTimestamp:number · actualReturnTimestamp:null · status:string(`ACTIVO`/`DEVUELTO`/`RETRASADO`/`DANADO`) · purpose/authorizedBy:string

### `emergencies` (SOS Vial)
id:number · reporterName/reporterPhone/memberNumber:string · emergencyType:string(`ACCIDENTADO_GASOLINA`/`ACCIDENTADO_MECANICO`/`CAIDA`/`CHOQUE`/`ACCIDENTE_VIAL`/`FALLA_MECANICA_GRAVE`/`EMERGENCIA_MEDICA`/`APOYO_SEGURIDAD`) · locationDescription:string · coordinateLat/coordinateLng:number · bikeDetails:string · bloodTypeNeeded:null · details:string · timestamp:number · status:string(`ACTIVA`/`EN_CAMINO`/`ATENDIDA`/`RESUELTA`) · respondersNotes:string

### `invitations`
id:number · code:string · isMaster:boolean · isSpecialGuest:boolean · durationHours:number · createdBy:string · createdAt/expiresAt:number · isUsed:boolean · usedByPhone/usedByName:null · targetRole:string(MemberRole) · note:string

### `access_requests`
id:number · fullName/phone/cedulaDni:string · bikeBrand/bikeModel/bikeColor/bikePlate:string · chapterState/birthDate:string · requestedRole:string · requestType:string(`ACCESO_APP`/`SOLICITUD_DIRECTIVA`) · reasonMessage:string · timestamp:number · status:string(`PENDIENTE`/`APROBADO`/`RECHAZADO`) · generatedCode/reviewedBy:null

### `role_configs`
ID documento = roleKey · customTitle/roleDuties:string · badgeIconName:string · canManageApp:boolean · lastUpdatedBy:string · lastUpdatedAt:number

---

## 🖼️ FOLDERS DE STORAGE (ya cubiertos por reglas actuales)

| Carpeta | Uso |
|---|---|
| `avisos/` | Imágenes del Muro |
| `stickers_chat/`, `imagenes_chat/`, `audios_chat/` | Chat |

---

## 🐛 FILTROS LOGCAT POR MÓDULO (Android Studio)

| Módulo | Filtro |
|---|---|
| Autenticación/Login | `AUTENTICACIO_NUBE` \| `GoogleSignIn` \| `TEAM_TX` |
| Muro (imágenes) | `TEAM_TX_IMAGES` |
| Sincronización general | `FIREBASE_SYNC` \| `BaseFirestoreSync` |
| Chat | `TEAM_TX_CHAT` |
| OTA | `OTA` |
| Perfil nube | `PERFIL_SYNC` |

## ✔️ CHECKLIST POST-CREACIÓN

- [ ] 4 documentos en `chat_channels` (GENERAL, RODADAS, MECANICA_AUXILIO, DIRECTIVA)
- [ ] `app_settings/global` con `chatEnabled=true`
- [ ] `configuracion/OTA` creada (aunque vacía de versión futura)
- [ ] Instalar APK nuevo e iniciar sesión → verificar que aparecen `feed`, `members`, `usuarios`
