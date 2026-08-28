📊 Análisis Comparativo
Aspecto	Team-Nacional-TX-Aragua	android (OsmAnd)
Build System	KTS + Version Catalogs (moderno)	Groovy DSL (legacy)
AGP	9.1.1	8.7.3
Kotlin	2.2.10	2.1.20
Compose BOM	2024.09.00	manual
Package/NS	com.example / com.aistudio.teamtxvzla.rkqp	net.osmand.plus
Min/Target SDK	24/35	24/35
Nativos	No	Sí (JNI + libc++ + OsmAndCore)
Flavors	1	5 (androidFull, gplayFree, nightlyFree, gplayFull, huawei)
Módulos	app + mapa (condicional)	OsmAnd + OsmAnd-java + OsmAnd-api + OsmAnd-shared
🎯 Mejor Estrategia: TX como App Principal + OsmAnd como Librería
Por qué:
1. TX ya usa toolchain moderno (KTS, version catalogs, AGP 9.1)
2. OsmAnd ya expone sus módulos core como librerías (OsmAnd-java, OsmAnd-api, OsmAnd-shared)
3. TX es la app "en desarrollo" con el botón "Mapa TX" en la barra inferior
4. Evitas reescribir el complejo sistema de flavors/NDK de OsmAnd
📋 Plan de Implementación
Fase 1: Preparación del Entorno Unificado
- Crear settings.gradle.kts unificado en D:\MAPA que incluya ambos proyectos
- Migrar versions.gradle de OsmAnd → libs.versions.toml centralizado
- Unificar AGP a 9.1.1 y Kotlin a 2.2.10 en todo el proyecto
- Convertir módulos OsmAnd a KTS (OsmAnd-java, OsmAnd-api, OsmAnd-shared)
Fase 2: OsmAnd como Módulos de Librería
- Cambiar OsmAnd de com.android.application → com.android.library
- Quitar flavors complejos, mantener solo legacy + fat (arm64-v8a + armeabi-v7a)
- Exponer MapActivity / MapFragment como API pública
- Generar AARs de OsmAnd-java, OsmAnd-api, OsmAnd-shared
Fase 3: Integración en TX (app principal)
- En app/build.gradle.kts: agregar implementation(project(":OsmAnd-java")), implementation(project(":OsmAnd-api")), implementation(project(":OsmAnd-shared"))
- Copiar libOsmAndCoreWithJNI.so a app/src/main/jniLibs/ (armeabi-v7a, arm64-v8a)
- Crear TxMapFragment/Activity wrapper que lance el mapa de OsmAnd
Fase 4: Navegación TX ↔ Mapa
- En MainAppScreen() (TX): botón "Mapa TX" → navega a OsmAndMapScreen
- OsmAndMapScreen: usa AndroidViewBinding o ComposeView para hostear MapFragment de OsmAnd
- Botón "Volver" en toolbar del mapa → popBackStack() a pantalla principal TX
Fase 5: Limpieza y Build
- Resolver conflictos de dependencias (AppCompat, Material3, Compose, Room, etc.)
- Configurar proguard-rules.pro unificado
- Build único: ./gradlew :app:assembleDebug
- Verificar: un solo APK con ambas funcionalidades
⚠️ Puntos Críticos a Resolver
1. NDK/JNI: Los .so de OsmAndCore deben empaquetarse en el APK final de TX
2. AppCompat vs Material3: OsmAnd usa androidx.appcompat + material; TX usa Material3 → migrar OsmAnd a Material3 o usar MaterialTheme wrapper
3. Namespace collision: net.osmand.plus vs com.example → mantener separados, usar manifestPlaceholders si hace falta
4. Recursos OsmAnd (assets, fonts, rendering styles): copiar a app/src/main/assets o mantener en módulo library
¿Quieres que inicie la Fase 1 creando la estructura unificada en D:\MAPA?
▣  Build · Nemotron 3 Ultra Free · 2m 42s
otra cosa si o si el com.example / com.aistudio.teamtxvzla.rkqp debe ser el definitivo ya que con ese esta configurado el firebase haz la unificacion con la mejor estrategia , resultado final debe ser un solo apk compilado al abrir la app se abre todo lo de D:\MAPA\Team-Nacional-TX-Aragua en la barra inferior en los botones de navegacion y funciones de la app debe estar un boton de mapa tx al tocarlo abre nuestro motor . osmand nativo de la app 
+ Thought: 4.3s
Entendido. El package final com.aistudio.teamtxvzla.rkqp es innegociable (Firebase). Voy a implementar la unificación completa ahora.
# Todos
[•] Crear settings.gradle.kts unificado en D:\MAPA con todos los módulos
[ ] Migrar versions.gradle de OsmAnd a libs.versions.toml centralizado
[ ] Convertir módulos OsmAnd (java, api, shared) a KTS
[ ] Cambiar OsmAnd app a library module, exponer MapFragment
[ ] Integrar dependencias OsmAnd en app/build.gradle.kts de TX
[ ] Copiar libOsmAndCoreWithJNI.so a app/src/main/jniLibs/
[ ] Crear TxMapScreen wrapper para hostear MapFragment de OsmAnd
[ ] Agregar botón 'Mapa TX' en navegación inferior de MainAppScreen
[ ] Implementar navegación TX ↔ Mapa con botón volver
[ ] Resolver conflictos de dependencias (AppCompat, Material3, Compose, Room)
[ ] Build final: ./gradlew :app:assembleDebug → 1 APK