Actúa como un Ingeniero Senior en Android (Kotlin) especializado en arquitectura modular estricta. Necesito implementar un sistema OTA (Over-The-Air) de actualizaciones y notificaciones para mi aplicación. El código debe compilar y funcionar en la vida real.

REGLAS ESTRICTAS DE DESARROLLO (NO OMITIR NINGUNA):

Arquitectura Modular: Prohibido el código monolítico. Cada funcionalidad nueva debe vivir en su propio archivo .kt independiente (clase u objeto). El MainActivity solo debe contener instancias o llamadas a estos módulos.

Nombres en Español Básico: Queda prohibido el inglés técnico. Los archivos y módulos deben usar nombres cortos y claros en español (ej. ACTUALIZADOR.kt, NOTIFICACIONES.kt, VISTA_INFO.kt).

Código Completo e Incremental: Prohibido enviar fragmentos de código, "código por partes" o usar comentarios como // Aquí va tu código. Siempre entrega la versión total y funcional de cada archivo afectado.

Respeto de Funciones Previas: El nuevo código debe integrarse sin romper ninguna función existente.

TAREAS A EJECUTAR Y ENTREGAR:

TAREA 1: Preparación del Entorno en GitHub (Instrucciones Claras)
Explícame paso a paso cómo crear y gestionar las versiones usando el apartado oficial de "Releases" (Lanzamientos) de GitHub en mi repositorio Team-Nacional-TX-Aragua. Explícame cómo subir el APK allí para que el módulo de la app pueda leerlo de forma pública y visible.

TAREA 2: Interfaz de Usuario (VISTA_INFO)
Genera el código para añadir un botón de "Info" al final del menú inferior (Bottom Navigation). Al presionar este botón, debe abrir un panel limpio que muestre la versión actual de la app y un listado o botón para buscar versiones anteriores y nuevas.

TAREA 3: Módulo Gestor de Actualizaciones (ACTUALIZADOR.kt)
Crea el archivo independiente ACTUALIZADOR.kt. Este módulo debe:

Conectarse a la API pública de mi repositorio (Releases).

Leer el historial de versiones subidas.

Comparar la versión instalada en el teléfono con la última de GitHub.

Permitir seleccionar una versión del historial, descargar el APK y lanzar el instalador (Intent de instalación de paquetes de Android).

TAREA 4: Módulo de Notificaciones (NOTIFICACIONES.kt)
Crea el archivo independiente NOTIFICACIONES.kt. Este módulo debe:

Configurar los canales de notificación nativos de Android.

Implementar una tarea en segundo plano (usando WorkManager de Android) que consulte silenciosamente el ACTUALIZADOR.kt una vez al día.

Si hay una versión nueva en GitHub, debe lanzar una notificación visual en el teléfono del usuario invitándolo a actualizar.

ENTREGABLES ESPERADOS:

Explicación de GitHub Releases.

Código completo de la actualización de la UI (navegación).

Código completo de ACTUALIZADOR.kt (con comentarios de uso).

Código completo de NOTIFICACIONES.kt (con comentarios de uso).

Las dependencias necesarias para el archivo build.gradle.kts (como permisos de internet, instalación de paquetes y WorkManager).