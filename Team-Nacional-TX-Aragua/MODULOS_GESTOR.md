# Gestor de Módulos - Team Nacional TX Aragua

> ## 🔒 REPOSITORIO OFICIAL DEL PROYECTO (REGLA INMUTABLE PARA TODOS LOS AGENTES)
> **A partir de ahora TODO se sube a:**  
> ### 👉 `https://github.com/EDJMM91/TeamTX-Workspace-Completo`
>
> - Los commits siempre deben hacerse desde la raíz `D:\MAPA` (nunca desde `D:\MAPA\Team-Nacional-TX-Aragua`).
> - El repositorio `Team-Nacional-TX-Aragua` en GitHub es solo referencia histórica, ya no es el primario.
> - APK oficial OTA: `https://github.com/EDJMM91/Team-Nacional-TX-Aragua/raw/main/apk/TeamTX-v1.2.9-beta.apk`

Respetando @AGENTS.md. y @ESTRATEGIA_COMPILACION.md(confirmame que los leiste) Este plan es para buscar errores en módulos y cosas de la app, actualizar funciones , mejorar cosas pero se trabajará uno a uno. Yo llamo "módulos" a cada interfaz que muestra cada botón inferior. Vamos a gestionar uno por uno, al terminar y dejar estable desde el módulo uno se procede a mejorar el siguiente.
inicia en cmd graphify sus comandos para estudiar toda la rama y conexiones de todo este proyecto y la salida la pegas en su carpeta que ya esta creada en la raiz del proyecto. una vez hecho estudia la salida para que tebngas contexto y sepas donde esta todo 
((((()))))
 este mensaje  es para cualquier agente que  se tope con este archivo o desee trabajar en algun modulo   debe leerlo y entenderlo. si no esta deacuerdo con algo , por favor consultame antes de realizar cambios 
opencode antigravity usa el nombre de los modulos en español

Cada módulo se dejará **"estable"** sin errores ni bugs. Una vez terminado con uno, viene el siguiente.

> **Nota**: Estas instrucciones pueden variar de repente. Puedo dejar de seguir el orden, dejando memoria de los módulos faltantes o a mitad de desarrollo, por si decido ajustar algo de otro módulo.

---

## Regla Global: Persistencia

Una cosa segura para **todos los módulos y funciones**: cada cosa debe ser **persistente**. Si me salgo o cierro la app, al volver debe estar todo como lo dejé, a menos que:
- **Cierre sesión** *(función faltante en el módulo Perfil/Carnet)*
- **Me dé de baja del team** *(con opción a poner un porqué de la baja)*

---
cada modulo esta visualmente casi completo 
pero su logica de trabajo llevara una reestructuracion en base de datos y 
sincronizacon con la nube 
depende del caso llevara una restructuracion (tomando en cuenta que el acceso a la cuenta de google 
me deja iniciar conuna cuenta de google me da el acceso ) pero lo demas fuera de eso no ha funcionado .
en eso nos encargaremos de hacer un sistema de base de datos nuevo limpio funcioinal donde cada archivo sera 
actualizado y visto por firebase y los usuarios en tiempo real , firebase no esta autenticando solo kme autentico jmi cuenta dueña
del proyecto de alli no me salen mas usuarios autenticados a pesar de que inicio con cuentas dierentes .
## Lista de Módulos

### Módulos Activos (en la barra inferior)
POR CADA MODULO INTEGRALE UNA DIRECCION DE LOGCAST PARA PONERLA EN 
EL LOGCAST DE ANDROID STUDIO Y VER CADA ERROR ALLI 
| # | Módulo | Estado |
|---|--------|--------|
| 1 | **Muro** | 🟢 Estable (Visor Flyer Zoom, Descarga en Galería, Compartir WhatsApp/Estados, Limpieza Local Persistente, Sync Global y Auto-avisos Intermodulares) |
| 2 | **Chat** | 🟢 Estable (Multicanal en vivo + Directiva + Killswitch) |
| 3 | **Rodadas** | ⚪ Pendiente |
| 4 | **Miembros** | 🟢 Estable (Directorio real, logo oficial y sync en vivo) |
| 5 | **Tesorería** | ⚪ Pendiente |
| 6 | **Inventario** | ⚪ Pendiente |
| 7 | **SOS Vial** | ⚪ Pendiente |
| 8 | **Normativas** | ⚪ Pendiente |
| 9 | **Directiva** | 🟢 Estable (Gobernanza, Códigos, Limpieza y Bloqueo) |
| 10 | **Carnet TX** | 🟢 Estable (Vinculación Google en vivo sin App Check) |
| 11 | **Mapa TX Info** | 🟢 Estable (Navegación y búsqueda con assets noCompress) |

### Módulos en Camino (por integrar)

| # | Módulo | Estado |
|---|--------|--------|
| 12 | **Actualizar App** | 🔜 En camino |
| 13 | **OTA** | 🔜 En camino |
| 14 | **Notificaciones** | 🔜 En camino |
| 15 | **Estadísticas del Grupo** | 🔜 En camino |

### Módulo Nuevo a Integrar — Paso 1

**Configuraciones (⚙️ Engranaje)**: Se encarga de contener configuraciones previas de toda la app, cosas a ajustar, temas, etc., y configuraciones específicas de cada módulo.

---

## Instrucciones de Trabajo

- Esperaré órdenes de mejoras, a no ser de que me digas que trabajes en uno específico o que te faltan módulos por mostrar.
- Hay cosas que mejorar: vistas, etc.

por cada mejora o peticion antes dee ,mofificar algo genera un plan de implementacion y me lo muestras para aprobarlo o que me digas que lo modifique a tu gusto.

si te topas con algo que necesites cambiar en un gradle o manifest por favor notificame y espera mi permiso evita siempre cosas que hagan que la app se cierre 

este archivo puede ser editable menos sus directrices osea el mestado de cada modulo , pero las reglas no .

si intentas compilar y marca error no intentes arreglarlo muestrame el error y yo te digo si lo arreglo o no 

prohibido tocar gradles o manifest leerlos si pero editarlos no sin mi autorizacion 

NO PUEDE HABER REGRESION EL CODIGO COMPILA SOLO MEJORAR O OPTIMIZAR AGREGAR COSAS TIPO ACTUALIZACION O PARCHE 

VAS A TRABAJAR BAJO LOS MANIFEST ACTUALES NADA DE MODIFICARLOS NI TOCARLOS ESTAN BLOQUEADOS PARA TI 

antes de proceder a editar codigo crea un plan de implementacion detallado de que es lo que vas a hacer paso a paso .

al compilar ✅ App lanzada en ambos dispositivos PERO DESPUES DE MI ORDEN PARA INSTALAR A MENOS QUE ESTES HACIENDO PRUEBAS DE LOGCAST  (com.aistudio.teamtxvzla.rkqp):
- RFGL52W368D → iniciada
- A9FRUT4315006621
-  estos son los de confianza que se usan para probar la app y ver que todo este bien antes de subir a producción

---

## 🛡️ Regla Crítica: Base de Datos Room y Migraciones SQL (`AppDatabase.kt`)

> **REGLA OBLIGATORIA PARA TODOS LOS AGENTES:**
> Cada vez que se agregue, modifique o elimine cualquier campo en las entidades de base de datos (`Models.kt` / `@Entity`):
> 1. **SIEMPRE verificar y actualizar `AppDatabase.kt`**:
>    - Incrementar la versión de la base de datos Room (`version = N + 1`).
>    - Crear el objeto de migración correspondiente (ejemplo: `MIGRATION_25_26 = object : Migration(25, 26)` con los `ALTER TABLE ... ADD COLUMN ...`).
>    - Registrar obligatoriamente la nueva migración en `.addMigrations(...)` dentro de `getDatabase()`.
> 2. **Evitar el error de verificación de integridad (`Room cannot verify the data integrity`)**:
>    - Si se modifica alguna entidad sin registrar la migración SQL e incrementar la versión en `AppDatabase.kt`, la aplicación crasheará en segundo plano y se cerrará sin llegar al panel principal (Feed / Muro).
>    - **Regla preventiva:** Siempre auditar y revisar si se hizo un cambio en SQL, DAOs o Base de Datos antes de compilar y desplegar.
>
> ### 📜 Registro Histórico de Migraciones Room:
> - **Migración SQL 25 -> 26 Registrada:** Gamificación y reputación de pilotos (`positiveRatingsCount`, `negativeRatingsCount`, `reputationPoints`, `ratedByMemberIdsJson`).
> - **Migración SQL 26 -> 27 Registrada:** Ubicación de eventos en publicaciones (`locationCoordinates`, `locationName` en tabla `publications`).