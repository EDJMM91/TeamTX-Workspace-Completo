# MESH TX: INTERCOMUNICADOR TÁCTICO OFFLINE (MESH 3.0)
**Paquete Independiente:** `com.example.meshtx`  
**Destinado a:** Team Nacional TX Venezuela (Caravanas, Rutas y Auxilio Vial)

---

## 📡 1. Misión y Filosofía de Diseño
El módulo **Mesh TX** es una solución de intercomunicación por voz y datos tácticos de corto y medio alcance diseñada para operar con **CERO CONEXIÓN A INTERNET**.

En las carreteras venezolanas (curvas de montaña hacia Choroní, Cuyagua, Colonia Tovar o llanos abiertos), la señal celular suele ser nula o intermitente. Mesh TX crea un **enmallado dinámico de radiofrecuencia digital entre los propios dispositivos de los moteros**, permitiendo que cada teléfono funcione como un transmisor, receptor y repetidor (relay) para el resto del convoy.

---

## 🛠️ 2. Pila Tecnológica y Arquitectura de Transporte

### A. Descubrimiento y Enlace Local Híbrido (`BUSCADOR_MALLA.kt`)
1. **Bluetooth Low Energy (BLE - Beacons Pasivos Duales):**
   - **Formato Binario Compacto Little-Endian:** Emisión de tramas en Service Data (10 bytes: Node ID UInt32, Network/Channel ID UInt32, Saltos UInt8, Disponibilidad UInt8) y Manufacturer Data (Grupo ID UInt32 + Alias en UTF-8), reduciendo el overhead al mínimo absoluto.
   - **Compatibilidad Textual:** Soporte fallback de delimitación `id|alias` para interoperabilidad transparente.
   - **Control de Cadencia (ScanRateLimiter):** Ventanas de escaneo controlado (máximo 4 ráfagas por cada 30 segundos) para blindar el teléfono contra el throttling agresivo de ahorro de batería de Android.
2. **Wi-Fi Direct P2P Autónomo (Sin Host Central):**
   - Gestión mediante `WifiP2pManager` y `BroadcastReceiver` dedicado para descubrimiento rápido de pares moteros sin requerir un router o punto de acceso intermedio.
3. **Wi-Fi Aware / Neighbor Awareness Networking (NAN):**
   - Intercambio directo punto a punto a alta velocidad entre dispositivos Android compatibles (Android 8.0+).

### B. Enrutamiento Multisalto Mesh 3.0 (`ENRUTADOR_MALLA.kt`)
1. **Arquitectura Modelo Actor (Actor Model):**
   - Aislamiento de mutaciones de estado en un bucle secuencial alimentado por un `Channel` sin bloqueo (`Channel.UNLIMITED`). Elimina por completo carreras de hilos (race conditions) entre los hilos de hardware de audio y los sockets de red.
2. **Caché Temporal de Desalojo O(1) (`CacheExpiracionTemporal` / Time-Evicting Cache):**
   - Deduplicación instantánea de identificadores de paquetes con borrado perezoso (Lazy Deletion). Suprime las tormentas de difusión (Broadcast Storms) con consumo de memoria constante y cero pausas del recolector de basura (GC).
3. **Estrategia de Enrutamiento Diferenciado (TTL):**
   - **Voz en Tiempo Real:** Límite estricto de 3 saltos para garantizar latencia ultra-baja (<150ms).
   - **Alertas SOS y Telemetría:** Límite extendido de hasta 5 saltos con difusión garantizada (Flooding controlada) y aprendizaje de ruta inversa (Reverse Path Learning).
4. **Rastreador de Presencia y Salud de Nodos (Peer Liveness Tracker):**
   - Monitoreo continuo de timestamps de escucha para purgar nodos caídos y autorreparar la topología de la caravana.

### C. Motor de Audio para Casco Motero (`AUDIO_CASCO.kt`)
1. **Soporte de Intercomunicadores Bluetooth de Casco:**
   - Enrutamiento por Bluetooth SCO (`AudioManager.startBluetoothSco()`) y detección automática mediante `AudioDeviceCallback` para cascos con intercomunicador Sena, Cardo o Ejeas.
2. **Gestión de Foco de Audio Táctico (AudioFocus con Ducking):**
   - Atenuación automática de música o navegación GPS al presionar PTT o activarse VOX (`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`) y liberación inmediata al soltar.
3. **Búfer Anti-Jitter y Secuenciador de Voz:**
   - Amortiguación de micro-pausas y paquetes desordenados para una reproducción limpia y sin chasquidos en `AudioTrack`.
4. **Filtro Digital de Ruido de Viento:**
   - Filtro pasa-altos IIR a ~300 Hz que corta las turbulencias aerodinámicas del casco a velocidades de carretera.
5. **Detección VOX Dinámica & PTT:**
   - Selector configurable entre botón táctil/físico Push-to-Talk y activación por voz con calibración de umbral de energía RMS.
6. **Compresión Ligera DPCM 8-Bit:**
   - Compresión diferencial al vuelo de 16 kHz PCM a paquetes de bajo peso para maximizar el throughput de la malla.

### D. Canales Tácticos Virtuales
- **Canal 1 - General TX:** Difusión abierta a toda la rodada.
- **Canal 2 - Caravana & Convoy:** Canal operativo para Capitanes de Ruta, Punteros y Barredoras.
- **Canal 3 - Emergencia SOS:** Canal prioritario de interrupción para auxilio vial o mecánico.
- **Canal 4 - Directiva:** Enlace táctico reservado para líderes del club.
- **Canal 5 - Enlace Privado (1 a 1):** Comunicación directa entre dos pilotos específicos.

### E. Nube Pasiva Asíncrona (`NUBE_MALLA.kt`)
- La red de voz es **100% OFFLINE**.
- Si el teléfono detecta conexión a internet (Wi-Fi o datos celulares) en gasolineras o paradas, respalda de forma pasiva las estadísticas de uso y telemetría en Firebase sin interrumpir la operación local.

---

## 🔒 3. Políticas de Acceso y Visibilidad
- **Fase de Despliegue:** En el Dashboard principal de la aplicación, el botón del módulo se muestra visible para todos los miembros con el icono `Podcasts`.
- **Restricción Táctica:** Para usuarios con rol estándar, el acceso se encuentra temporalmente protegido con aviso modal informativo. Solo los Administradores y miembros de la Directiva pueden abrir la pantalla operativa y activar la transmisión de malla durante la fase inicial de pruebas en asfalto.
