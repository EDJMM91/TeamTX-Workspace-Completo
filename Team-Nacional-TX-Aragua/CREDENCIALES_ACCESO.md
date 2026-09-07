# 🔑 Credenciales y Códigos de Acceso — Team Nacional TX Aragua

Este documento contiene la lista completa y actualizada de todos los **códigos maestros y credenciales válidas** para ingresar directamente a la aplicación a través de la pantalla principal de acceso (**[VISTA_LOGIN.kt](file:///d:/MAPA/Team-Nacional-TX-Aragua/app/src/main/java/com/example/ui/screens/VISTA_LOGIN.kt)** / **[TeamTxViewModel.kt](file:///d:/MAPA/Team-Nacional-TX-Aragua/app/src/main/java/com/example/ui/viewmodel/TeamTxViewModel.kt)**).

> **Nota de uso:** Los códigos **no distinguen entre mayúsculas y minúsculas**. Puedes escribir tanto `desarrollo1` como `DESARROLLO1`. No se requiere contraseña.

---

## 1. 👑 Desarrolladores / Super Admin (Control Total Supremo)
*Otorga privilegios supremos en el sistema, acceso al panel de Directiva, gestión de base de datos, depuración, carnet digital y herramientas de administración sin restricciones.*

| Código | Rol Asignado | Ficha TX | Nombre en Perfil | Alias | Permisos Clave |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`DESARROLLO1`** | `DESARROLLADOR` | `TX-DEV-001` | Desarrollador TX 1 | Dev Master 1 | Control total, panel directiva, radio, radar y bypass |
| **`DESARROLLO2`** | `DESARROLLADOR` | `TX-DEV-002` | Desarrollador TX 2 | Dev Master 2 | Control total, panel directiva, radio, radar y bypass |
| **`DESARROLLO3`** | `DESARROLLADOR` | `TX-DEV-003` | Desarrollador TX 3 | Dev Master 3 | Control total, panel directiva, radio, radar y bypass |

---

## 2. 🏛️ Presidencia / Líder Fundador
*Otorga el rol de Presidente del Club. Acceso como Líder Super Admin, aprobación de miembros, generación de códigos, sanciones y carnet presidencial.*

| Código | Rol Asignado | Ficha TX | Nombre en Perfil | Alias | Observaciones |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`TX19554402`** | `PRESIDENTE` | `TX-001` | Presidente TX | Presidente | Código maestro principal de entrega de la app |
| **`TX19554402SB`** | `PRESIDENTE` | `TX-001` | Eduardo Androide | Dev TX | Cuenta de líder / fundador con bypass |
| **`19554402SB`** | `PRESIDENTE` | `TX-001` | Eduardo Androide | Dev TX | Requiere teléfono `04243769999` si se usa formulario extendido |
| **`TX-TEST-ADMIN`** | `PRESIDENTE` | `TX-TEST-001` | Admin de Prueba | Admin Test | Código rápido de test para pruebas de Google / QA |

---

## 3. 🛡️ Directiva (Gobernanza, Rutas y Moderación)
*Habilita el Módulo de Directiva, canales de radio exclusivos de directiva (Canal 4), gestión de rodadas, inventario y asistencia del convoy.*

| Código | Rol Asignado | Ficha TX | Nombre en Perfil | Alias | Permisos Clave |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`DIRECTIVO1`** | `DIRECTIVA` | `TX-DIR-001` | Directivo TX 1 | Directivo 1 | Panel de directiva, canal 4 privado, control caravana |
| **`DIRECTIVO2`** | `DIRECTIVA` | `TX-DIR-002` | Directivo TX 2 | Directivo 2 | Panel de directiva, canal 4 privado, control caravana |
| **`TX-TEST-DIRECTIVA`** | `DIRECTIVA` | `TX-TEST-002` | Directivo de Prueba | Directiva Test | Código de testing para validar vista de directiva |

---

## 4. 🏍️ Piloto Común (Miembro Activo Estándar)
*Acceso como miembro oficial del club sin privilegios administrativos: Rutas, Intercomunicador Mesh TX, Radio, Velocímetro GPS, Talleres, Ranking y Directorio.*

| Código | Rol Asignado | Ficha TX | Nombre en Perfil | Alias | Permisos Clave |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`PILOTO1`** | `MIEMBRO_ACTIVO` | `TX-PIL-001` | Piloto TX 1 | Piloto 1 | Acceso general de piloto activo |
| **`PILOTO2`** | `MIEMBRO_ACTIVO` | `TX-PIL-002` | Piloto TX 2 | Piloto 2 | Acceso general de piloto activo |
| **`PILOTO3`** | `MIEMBRO_ACTIVO` | `TX-PIL-003` | Piloto TX 3 | Piloto 3 | Acceso general de piloto activo |
| **`PILOTO19`** (o `PILOT019`) | `MIEMBRO_ACTIVO` | `TX-999` | Piloto de Pruebas | Tester | Perfil especial preconfigurado para pruebas de audio y campo |
| **`TX-TEST-PILOTO`** | `MIEMBRO_ACTIVO` | `TX-TEST-003` | Piloto de Prueba Google | Piloto Test | Código de testing para simular compañero en ruta |

---

## 5. 🌐 Acceso por Google Sign-In
Si se utiliza el botón de autenticación con cuenta de Google:
- **`eduardo.androide.em@gmail.com`**: Se enlaza automáticamente al perfil de **Presidente / Dev TX** (`TX-001`) con rol de Super Administrador.
- **Cualquier otra cuenta de Google**: El sistema crea automáticamente un perfil de **Miembro Activo** con ficha aleatoria y permite la entrada directa al Dashboard.

---

## 6. 🎟️ Códigos Temporales de Invitación (Base de Datos)
* Generados por la Directiva desde el panel de control de la app.
* Tienen una vigencia predeterminada de **24 horas**.
* Se pueden emitir para roles de **Piloto**, **Copiloto** o **Invitado Especial**.
* Una vez utilizados o vencidos, se desactivan automáticamente.
