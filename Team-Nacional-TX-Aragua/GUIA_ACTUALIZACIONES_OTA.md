# Guía de Actualizaciones OTA (Over-The-Air) usando Google Drive

Este documento explica paso a paso cómo lanzar una nueva versión de la aplicación **Team Nacional TX Aragua** para que los usuarios reciban la actualización automáticamente al abrir la app o de forma manual mediante la pestaña de "Info".

---

## 1. Preparar la nueva versión en Android Studio

Cada vez que vayas a lanzar una actualización con nuevos cambios, debes incrementar obligatoriamente el número de versión interno de la aplicación.

1. Abre el archivo `app/build.gradle.kts`.
2. Busca la sección `defaultConfig`:
   ```kotlin
   defaultConfig {
       applicationId = "com.aistudio.teamtxvzla.rkqp"
       minSdk = 26
       targetSdk = 35
       versionCode = 9 // ¡AUMENTA ESTE NÚMERO! (ej. pasarlo a 10)
       versionName = "1.0.1" // Cambia la versión visual si quieres (ej. "1.1.0")
   }
   ```
3. Aumenta `versionCode` sumándole 1. Este es el número clave que el sistema lee para saber si hay una actualización.
4. Genera el APK compilando el proyecto.
5. El APK compilado quedará guardado localmente en tu computadora en la ruta:
   `D:\Team-Nacional-TX-Aragua\app\build\outputs\apk\debug\app-debug.apk`

---

## 2. Subir el APK a Google Drive y obtener el enlace directo

Como no podemos usar GitHub por ser privado ni Firebase Storage por requerir tarjeta, usaremos Google Drive mediante un truco para descargas directas.

1. Entra a tu **Google Drive**.
2. Sube el archivo `app-debug.apk` que generaste en el paso anterior.
3. Una vez subido, haz clic derecho sobre el archivo, selecciona **"Compartir"** y luego de nuevo **"Compartir"**.
4. En "Acceso general", cambia "Restringido" a **"Cualquier usuario que tenga el vínculo"**.
5. Haz clic en **Copiar vínculo** y pégalo en un bloc de notas. Se verá algo así:
   `https://drive.google.com/file/d/1A2b3C4d5E6f7G8h9I0jK/view?usp=sharing`
6. **¡TRUCO CLAVE!** Ese enlace no sirve para el teléfono porque abre una página web. Tienes que extraer el **ID** del archivo (lo que está entre `/d/` y `/view`), que en el ejemplo de arriba es `1A2b3C4d5E6f7G8h9I0jK`.
7. Ahora, crea el enlace directo de descarga usando esta plantilla y pegando tu ID al final:
   `https://drive.google.com/uc?export=download&id=PON_TU_ID_AQUI`
   
   Ejemplo final: 
   `https://drive.google.com/uc?export=download&id=1A2b3C4d5E6f7G8h9I0jK`

*(OJO: Google Drive solo permite este truco para archivos de menos de 100 MB. Si la app pesa más, Drive forzará una pantalla de advertencia de virus y la descarga automática fallará).*

---

## 3. Informar a la App mediante Firebase Firestore

Finalmente, debes avisarle a la app que hay una versión nueva lista para descargar pasando el enlace que creamos.

1. Entra a la consola de **Firebase**.
2. Ve a **Firestore Database**.
3. Navega hasta la colección principal **`Configuracion`**.
4. Haz clic en el documento **`OTA`**.
5. Actualiza los siguientes 3 campos EXACTAMENTE con estos nombres:
   
   - **`versionCode`** (Tipo: `number` o `int64`): 
     Cambia el valor al mismo número nuevo que pusiste en Android Studio.
   
   - **`urlDescarga`** (Tipo: `string`): 
     Pega el **enlace directo de Google Drive** que armaste en el paso 2 (el que empieza por `https://drive.google.com/uc?export=download...`).
   
   - **`notas`** (Tipo: `string`): 
     Escribe las novedades de esta versión.

**¡Y listo!** La próxima vez que cualquier usuario abra la aplicación, o busque actualizaciones manualmente, bajará el archivo silenciosamente desde tu Google Drive.
