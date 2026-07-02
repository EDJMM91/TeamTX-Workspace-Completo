# Plantilla Base Cartográfica: Motor OsmAnd (Variante Libre)

Este repositorio contiene la configuración maestra y el entorno de compilación depurado para el motor cartográfico de OsmAnd. Está diseñado para servir como un lienzo virgen, funcional y de alta velocidad, ideal para construir aplicaciones de navegación modulares por encima sin arrastrar código privativo (Google/Huawei) ni telemetría innecesaria.

## 1. Arquitectura Multi-Módulo
El proyecto está segmentado estrictamente para mantener el núcleo aislado de las implementaciones:
* **`:OsmAnd`**: Módulo principal de la aplicación Android[cite: 3].
* **`:OsmAnd-java`**: Lógica central en Java puro (algoritmos matemáticos y ruteo independientes de Android)[cite: 3].
* **`:OsmAnd-shared`**: Módulo Kotlin Multiplatform (KMP) para compartir código entre plataformas[cite: 3].
* **`:OsmAnd-api`**: Interfaz de comunicación para aplicaciones externas[cite: 3].
* **`:plugins:*`**: Módulos aislados para funciones adicionales (Nautical, SRTM, Skimaps)[cite: 3].

## 2. Optimizaciones Críticas de Rendimiento (`gradle.properties`)
Para evitar cuellos de botella de memoria (Out of Memory) durante el ensamblaje nativo de C++ y acelerar las compilaciones futuras, se inyectaron los siguientes parámetros de sistema[cite: 2]:

* **Expansión de Memoria:** Asignación de 8GB de RAM dedicados al demonio de Gradle y 1GB para el Metaspace (`-Xmx8g`, `-XX:MaxMetaspaceSize=1g`)[cite: 2].
* **Paralelismo:** Activación de múltiples hilos de procesador para compilar módulos simultáneamente (`org.gradle.parallel=true`)[cite: 2].
* **Caché Agresivo:** Uso de `build cache` y `configuration cache` para omitir fases de indexación en ejecuciones repetidas[cite: 2].
* **VFS Watch:** Detección instantánea de cambios en archivos a nivel de sistema operativo (`org.gradle.vfs.watch=true`)[cite: 2].
* **Evasión de Tareas Zombi:** Se anularon globalmente los chequeos de Lint y las pruebas unitarias para reducir los tiempos de construcción en entornos de desarrollo (`skipTest=true`, `skipLint=true`)[cite: 2].

## 3. Matriz de Versiones y Dependencias
El entorno fue actualizado y estabilizado bajo los siguientes estándares modernos:
* **SDK:** Compile 35, Target 35, Min 24[cite: 4, 5].
* **Lenguajes:** Java 17 y Kotlin 2.1.20[cite: 1, 6].
* **Herramientas de Construcción:** Android Gradle Plugin (AGP) 8.7.3[cite: 1].
* **Interfaces Visuales:** Compose BOM 2026.06.00 y Material Design 3[cite: 5].
* **Librerías Core:** SQLite de AndroidX, Kotlin Coroutines y Ktor para redes[cite: 5].

## 4. Estructura de la Variante Objetivo: `androidFullLegacyFatDebug`
Esta es la variante obligatoria para mantener el motor limpio. Se compone de tres dimensiones de construcción cruzadas[cite: 5, 6]:

1. **Dimensión de Versión (`androidFull`):** Configurada bajo el namespace `net.osmand.plus`, compila exclusivamente los recursos libres (F-Droid), excluyendo binarios de compras e integraciones comerciales[cite: 5].
2. **Dimensión de Núcleo (`legacy`):** Desactiva el motor 3D OpenGL pesado, permitiendo una compilación nativa mucho más rápida apoyándose en binarios estáticos[cite: 5, 6].
3. **Dimensión de Arquitectura (`fat`):** Empaqueta las librerías C++ (`libc++_shared.so`) compiladas para todas las arquitecturas de CPU móviles simultáneamente (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`), garantizando que el APK corra en cualquier dispositivo físico o emulador[cite: 5].

## 5. Control de Tareas Condicionales
Para asegurar la limpieza del entorno, se aplicaron filtros condicionales en los scripts raíz:
* **Huawei Excluido:** El plugin de servicios móviles de Huawei (`agcp`) solo se inyecta en el classpath si la tarea solicitada por consola contiene explícitamente la palabra "huawei"[cite: 1].
* **Gestor de Recursos Inteligente:** En el archivo `build.gradle` de la app, tareas críticas de descarga como `downloadWorldMiniBasemap` y extracción de recursos vectoriales (`copyIcons`) operan bajo validaciones de caché para no repetir descargas en cada compilación[cite: 5].

## 🚀 Guía Rápida de Despliegue para Nuevos Proyectos
1. Clona este repositorio en un directorio raíz con ruta corta (ej. `D:\MapaBase`).
2. Abre Android Studio y selecciona **únicamente** la carpeta `android`.
3. En la esquina inferior izquierda, abre la pestaña **Build Variants**.
4. Cambia la variante del módulo `:app` a **`androidFullLegacyFatDebug`**.
5. Ejecuta el botón "Run". La primera vez demorará mientras descarga los assets y genera el binario en frío. Las siguientes iteraciones serán instantáneas gracias al Configuration Cache.
6. A partir de aquí, crea tus archivos `.kt` independientes (ej. `MAPA.kt`, `TELEMETRIA.kt`) para inyectar tus interfaces por encima del motor.