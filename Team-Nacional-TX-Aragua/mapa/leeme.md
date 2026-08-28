# Módulo de Mapa (OsmAnd)

Este directorio está preparado para contener el repositorio del motor de mapas basado en OsmAnd:
`https://github.com/EDJMM91/WR-MapaBase`

## Instrucciones para el Desarrollador

Cuando estés listo para integrar el motor del mapa:

1. **Clonar el Repositorio:**
   Abre una terminal en este mismo directorio (`mapa/`) y ejecuta:
   ```bash
   git clone https://github.com/EDJMM91/WR-MapaBase.git .
   ```
   *(Nota el punto al final para clonar directamente en esta carpeta)*

2. **Habilitar el Módulo:**
   Abre el archivo `settings.gradle.kts` (en la raíz del proyecto) y añade al final:
   ```kotlin
   include(":mapa")
   ```
   *(Nota: La aplicación ya está configurada en `app/build.gradle.kts` para enlazar este módulo automáticamente si detecta los archivos de compilación, pero revisa si hay conflictos).*

3. **Modificar la UI:**
   Abre `MainActivity.kt` (o el lugar donde dejaste el botón de Mapa), busca `MapaDevelopmentScreen()` y reemplázalo por el Activity o Fragment que el motor OsmAnd expone para su uso.
