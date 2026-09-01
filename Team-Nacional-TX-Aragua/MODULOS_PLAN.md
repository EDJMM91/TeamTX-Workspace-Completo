# Plan de Implementación de Modulos - Team Nacional TX Venezuela

## Estado Actual: En desarrollo iterativo modulo por modulo

---
ESTA ES UNA GUIA PASO A PASO DE 
## Módulo 1: MURAL / FEED (Wall)

### Estado: **PLANIFICACIÓN INICIAL**

### Funciones Actuales (FeedScreen.kt):
- [x] Lista de publicaciones con filtro por categoría
- [x] Filtrar por: Todos, Avisos, Retos TX, Mecánica
- [x] Tarjetas de publicación con categoría y prioridad
- [x] Botones de like y comentarios
- [x] Sección de cumpleaños del mes
- [x] Banner de canal de Telegram
- [x] Modo directiva: botón de eliminar publicaciones
- [x] Publicaciones ancladas (pinned)
- [x] Publicaciones URGENTE con alerta visual
- [x] Comentarios expandibles con respuesta de miembros
- [x] Input de comentario con envío
- [x] Diálogo de creación de publicación con categorías, reto motero (km, badge), enlace Telegram, fijar

### Mejoras Identificadas para Muro:
1. **Paginación infinita** - Cargar más publicaciones al hacer scroll (actualmente trae todas)
2. **Ordenamiento por fecha** - Las publicaciones deberían ordenarse por fecha de creación (más recientes primero)
3. **Vista previa de imágenes** - Si la publicación tiene imagen/adjunto, mostrar miniatura
4. **Búsqueda de publicaciones** - Buscar por título o contenido
5. **Compartir publicación** - Compartir a Telegram u otras apps
6. **Estadísticas de engagement** - Mostrar vistas/alcance de cada publicación
7. **Modo oscuro compatible** - Verificar que todos los elementos se vean bien en modo oscuro
8. **Accesibilidad** - Mejorar contrastes y labels para screen readers
9. **Offline support** - Cachear publicaciones para ver sin internet
10. **Pull-to-refresh** - Actualizar lista manualmente

### Componentes Relacionados:
- `FeedScreen.kt` - Pantalla principal del muro
- `NoticeCard.kt` - Tarjeta individual de publicación
- `CreateNoticeDialog.kt` - Diálogo para crear nueva publicación
- `TeamTxViewModel.kt` - Lógica: `createPublication`, `likePublication`, `deletePublication`, `publications`

### Archivos para Revisar/Modificar:
- `app/src/main/java/com/example/ui/screens/FeedScreen.kt`
- `app/src/main/java/com/example/ui/screens/dialogs/CreateNoticeDialog.kt`
- `app/src/main/java/com/example/ui/viewmodel/TeamTxViewModel.kt`

---

## Módulo 2: CHAT

### Estado: **POR INICIAR**

### Funciones Planificadas:
- [ ] Lista de canales de chat
- [ ] Mensajería en tiempo real
- [ ] Enviar stickers/archivos
- [ ] Búsqueda de mensajes
- [ ] Notificaciones de mensajes nuevos
- [ ] Marcar mensajes como leídos
- [ ] Chat directiva privado
- [ ] Historial de chat persistente

### Por Definir:
- Estructura de archivos
- Mejoras específicas

---

## Módulo 3: RODADAS

### Estado: **POR INICIAR**

### Funciones Planificadas:
- [ ] Calendario de salidas grupales
- [ ] Registro de participantes
- [ ] Roles de convoy (líder, segundo, seguridad, etc.)
- [ ] Seguimiento de ruta GPS
- [ ] Estados de ride (programado, en curso, completado, cancelado)

---

## Módulo 4: MIEMBROS

### Estado: **POR INICIAR**

### Funciones Planificadas:
- [ ] Directorio de miembros
- [ ] Perfiles completos con datos biográficos
- [ ] Búsqueda y filtrado de miembros
- [ ] Gestión de roles y cargos
- [ ] Estado de solvencia
- [ ] Documentos personales (licencia, certificado médico, etc.)
- [ ] Historial de asistencia a eventos

---

## Módulo 5: TESORERÍA / FINANZAS

### Estado: **POR INICIAR**

### Funciones Planificadas:
- [ ] Registro de ingresos y egresos
- [ ] Cuotas mensuales
- [ ] Fondos de emergencia
- [ ] Potes de eventos
- [ ] Reportes financieros
- [ ] Historial de pagos
- [ ] Métodos de pago

---

## Módulo 6: INVENTARIO

### Estado: **POR INICIAR**

### Funciones Planificadas:
- [ ] Catálogo de equipamiento
- [ ] Control de prestamos/loans
- [ ] Stock disponible
- [ ] Estado de conservación
- [ ] Asignación de custodio
- [ ] Devoluciones de equipo

---

## Módulo 7: SOS VIAL

### Estado: **POR INICIAR**

### Funciones Planificadas:
- [ ] Alertas de emergencia
- [ ] Ubicación GPS
- [ ] Tipo de emergencia
- [ ] Estado de respuesta (activa/respondida/cancelada)
- [ ] Historial de alertas
- [ ] Notificaciones a directiva

---

## Módulo 8: NORMATIVAS

### Estado: **POR INICIAR**

### Funciones Planificadas:
- [ ] Catálogo de normativas y reglamentos
- [ ] Búsqueda y filtrado
- [ ] Versiones antiguas
- [ ] Notificaciones de actualizaciones

---

## Módulo 9: DIRECTIVA

### Estado: **POR INICIAR**

### Funciones Planificadas:
- [ ] Panel exclusivo directiva
- [ ] Gestión de códigos de invitación
- [ ] Solicitudes de acceso
- [ ] Reportes y estadísticas
- [ ] Configuración de roles

---

## Módulo 10: CARNET TX (Profile)

### Estado: **POR INICIAR**

### Funciones Planificadas:
- [ ] Perfil de miembro
- [ ] Datos personales y de motocicleta
- [ ] Documentos adjuntos
- [ ] Configuración de notificaciones
- [ ] Historial de actividad

---

**Fecha de creación:** 2026-08-21
**Próximo módulo:** Módulo 2 - CHAT

ultima tarea al terminar lo de los modulos 
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

---

## Módulo 8: MAPA TX & RADAR TÁCTICO & BÚSQUEDA INTERACTIVA

### Estado: **COMPLETADO E INTEGRADO**

### Funcionalidades Implementadas y Verificadas:
- [x] **Radar Táctico en Tiempo Real**: Telemetría GPS con subida a Firestore (`TELEMETRIA_GPS.kt`) y renderizado táctico en OsmAnd (`RADAR_MAP_LAYER.kt`).
- [x] **Avatar Propio y Cache Persistente**:
  - Posición dinámica desplazada detrás de la flecha de navegación para evitar obstrucción visual.
  - Línea líder punteada del color del rango del piloto hacia las coordenadas exactas.
  - Cache persistente en disco (`avatar_local_permanente.jpg` y carpeta interna `radar_avatars`).
  - Actualización inmediata del cache al editar foto en Carnet TX (`ProfileAndAdminScreen.kt`).
- [x] **Selector de Iconos de Favoritos Personalizados**:
  - Añadidos iconos en `poi-icons-vector`: `mx_logoteamposicion`, `mx_logoteam`, `mx_iconomarcador`, `mx_iconoposicion`, `mx_bandera`, `mx_directiva`.
  - `mx_directiva` condicionado por rol (`es_directivo_o_admin` / rango directivo en `EditorIconController.java`).
- [x] **Unificación de Marcadores en el Mapa**:
  - Publicaciones del Muro y Eventos de Calendario renderizan uniformemente con el logotipo oficial `logoteam.png`.
- [x] **Buscador Interactivo de Sitios Team TX (`buscar.png`)**:
  - Botón posicionado en el HUD superior derecho (`map_hud_top.xml`), debajo del botón de retorno a la app.
  - Invocación decoupled vía reflexión (`MapActivity.java` -> `BuscadorSitiosTx.mostrar(this)`).
  - Menú modal inferior con diseño motero que consolida avisos del muro, eventos de calendario y puntos favoritos.
  - Filtro de búsqueda en tiempo real y navegación/centrado inmediato de cámara con zoom en el mapa.

### Componentes Relacionados:
- `BUSCADOR_SITIOS_TX.kt`, `EVENTOS_MAP_LAYER.kt`, `RADAR_MAP_LAYER.kt`, `RADAR_FIREBASE.kt`, `GESTOR_RADAR.kt`, `TELEMETRIA_GPS.kt`
- `map_hud_top.xml`, `MapActivity.java`, `EditorIconController.java`, `poi_categories.json`
