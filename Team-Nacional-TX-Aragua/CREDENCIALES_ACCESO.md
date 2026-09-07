# 🔑 Credenciales y Códigos de Acceso — Team Nacional TX Aragua

Este documento contiene la lista oficial y depurada de todos los **códigos maestros y credenciales de acceso directo** para la aplicación Team TX Venezuela.

> **🛡️ REGLA DE REINICIO ABSOLUTO:** Al ingresar un nuevo código o cambiar de cuenta, la aplicación ejecuta una **limpieza total automática**. Se borran datos de Room, SharedPreferences, fotos de perfil anteriores y sesiones residuales para asegurar que no haya mezcla de datos entre pilotos diferentes en el mismo dispositivo.

---

## 1. 👑 Desarrolladores (Control Total Supremo)
*Acceso exclusivo para el mantenimiento técnico del sistema. Solo estas dos cuentas tienen privilegios de código fuente y base de datos maestra.*

| Código | Correo Google Autorizado | Nombre en Perfil | Ficha TX | Alias |
| :--- | :--- | :--- | :--- | :--- |
| **`DESARROLLO1`** | `eduardo.androide.em@gmail.com` | Eduardo Marquez (EM) | `TX-DEV-001` | Dev EM |
| **`DESARROLLO2`** | `eduardo.jose.marquez.matos@gmail.com` | Eduardo Marquez (Matos) | `TX-DEV-002` | Dev Matos |

---

## 2. 🏛️ Presidencia Nacional
*Máxima autoridad del club. Acceso Líder Super Admin con capacidad de gestión multimodular y aprobación de solicitudes.*

| Código | Rol Asignado | Ficha TX | Nombre en Perfil | Alias |
| :--- | :--- | :--- | :--- | :--- |
| **`PRESIDENTE`** | `PRESIDENTE` | `TX-001` | Presidente Nacional TX | Presidente |

---

## 3. 🏍️ Pilotos de Prueba (Cuentas Fijas)
*Perfiles estándar pre-configurados para validación de funciones en convoy, radio y radar.*

| Código | Rol Asignado | Ficha TX | Nombre en Perfil | Alias |
| :--- | :--- | :--- | :--- | :--- |
| **`PILOTO1`** | `MIEMBRO_ACTIVO` | `TX-P01` | Piloto de Prueba 1 | Piloto 1 |
| **`PILOTO2`** | `MIEMBRO_ACTIVO` | `TX-P02` | Piloto de Prueba 2 | Piloto 2 |

---

## 🛡️ Gestión de Nuevos Miembros y Directiva
Ya no existen códigos fijos para Directivos o Miembros adicionales. Todo nuevo acceso se gestiona de la siguiente manera:
1. **Solicitud de Ingreso:** El nuevo usuario llena el formulario de solicitud en la pantalla de login.
2. **Aprobación:** La Directiva o el Presidente aprueban la solicitud desde el **Panel de Directiva**.
3. **Emisión de Código:** El sistema genera un código dinámico (ej. `582910`) único y temporal (24h) vinculado a los datos del solicitante.

---

## 🌐 Acceso por Google Sign-In (Vínculo 1-a-1)
Al usar una cuenta de Google:
- **Cuentas Desarrollador:** Se enlazan automáticamente a sus perfiles `TX-DEV` correspondientes.
- **Demás Pilotos:** Deben ingresar primero con un código válido (dinámico o fijo) y luego pulsar **"Vincular Google"** en su Carnet TX. Esto crea un vínculo permanente que impide que ese correo sea usado por otro piloto.

> **Nota:** Si un correo ya está en uso por otro dispositivo/ficha, el sistema bloqueará la vinculación hasta que se desvincule manualmente el perfil anterior.
