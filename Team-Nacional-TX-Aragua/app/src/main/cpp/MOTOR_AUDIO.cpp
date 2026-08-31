#include <jni.h>
#include <cmath>
#include <vector>
#include <algorithm>

// ═══════════════════════════════════════════════════════════════════════════
// MOTOR DE AUDIO NATIVO C++ - REPRODUCTOR TX PRO (TEAM NACIONAL TX ARAGUA)
// Procesamiento digital de señales (DSP) en tiempo real para música en moto.
// ═══════════════════════════════════════════════════════════════════════════

// Estructura para filtros biquad de ecualización IIR
struct FiltroBiquad {
    float b0 = 1.0f, b1 = 0.0f, b2 = 0.0f;
    float a1 = 0.0f, a2 = 0.0f;
    float x1 = 0.0f, x2 = 0.0f;
    float y1 = 0.0f, y2 = 0.0f;

    void configurarPeaking(float frecHz, float gananciaDb, float q, float sampleRate) {
        float a = std::pow(10.0f, gananciaDb / 40.0f);
        float w0 = 2.0f * M_PI * (frecHz / sampleRate);
        float alpha = std::sin(w0) / (2.0f * q);
        float cosW0 = std::cos(w0);

        float a0 = 1.0f + alpha / a;
        b0 = (1.0f + alpha * a) / a0;
        b1 = (-2.0f * cosW0) / a0;
        b2 = (1.0f - alpha * a) / a0;
        a1 = (-2.0f * cosW0) / a0;
        a2 = (1.0f - alpha / a) / a0;
    }

    void configurarLowShelf(float frecHz, float gananciaDb, float sampleRate) {
        float a = std::pow(10.0f, gananciaDb / 40.0f);
        float w0 = 2.0f * M_PI * (frecHz / sampleRate);
        float alpha = std::sin(w0) / 2.0f * std::sqrt((a + 1.0f / a) * (1.0f / 0.707f - 1.0f) + 2.0f);
        float cosW0 = std::cos(w0);

        float a0 = (a + 1.0f) + (a - 1.0f) * cosW0 + 2.0f * std::sqrt(a) * alpha;
        b0 = (a * ((a + 1.0f) - (a - 1.0f) * cosW0 + 2.0f * std::sqrt(a) * alpha)) / a0;
        b1 = (2.0f * a * ((a - 1.0f) - (a + 1.0f) * cosW0)) / a0;
        b2 = (a * ((a + 1.0f) - (a - 1.0f) * cosW0 - 2.0f * std::sqrt(a) * alpha)) / a0;
        a1 = (-2.0f * ((a - 1.0f) + (a + 1.0f) * cosW0)) / a0;
        a2 = ((a + 1.0f) + (a - 1.0f) * cosW0 - 2.0f * std::sqrt(a) * alpha) / a0;
    }

    inline float procesar(float entrada) {
        float salida = b0 * entrada + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
        x2 = x1;
        x1 = entrada;
        y2 = y1;
        y1 = salida;
        return salida;
    }

    void reset() {
        x1 = x2 = y1 = y2 = 0.0f;
    }
};

// Instancias globales para canales Estéreo (Left y Right)
static FiltroBiquad gFiltroBassL;
static FiltroBiquad gFiltroBassR;
static FiltroBiquad gBandasL[5];
static FiltroBiquad gBandasR[5];

// Parámetros de retardo para espacialidad estéreo (Surround)
static const int MAX_DELAY_SAMPLES = 4096;
static float gBufferRetardoL[MAX_DELAY_SAMPLES] = {0};
static float gBufferRetardoR[MAX_DELAY_SAMPLES] = {0};
static int gIndiceRetardo = 0;

// Función limitadora suave (Soft-Clipper tipo Tanh) para Ultra Volumen
inline float softClip(float muestra) {
    if (muestra > 3.0f) return 1.0f;
    if (muestra < -3.0f) return -1.0f;
    return std::tanh(muestra);
}

extern "C" {

/**
 * 1. PROCESAMIENTO DE SUPER BASS (REALCE DE FRECUENCIAS BAJAS SIN DISTORSIÓN)
 */
JNIEXPORT void JNICALL
Java_com_example_reproductor_MOTOR_1AUDIO_1NATIVO_procesarSuperBass(
        JNIEnv *env,
        jclass clazz,
        jshortArray audioSamples,
        jint sampleCount,
        jfloat nivelGraves, // 0.0f a 1.0f
        jint sampleRate) {

    if (nivelGraves <= 0.001f || sampleCount <= 0) return;

    jshort *muestras = env->GetShortArrayElements(audioSamples, nullptr);
    if (!muestras) return;

    float gananciaDb = nivelGraves * 14.0f; // Hasta +14dB de graves profundos (60-100Hz)
    gFiltroBassL.configurarLowShelf(90.0f, gananciaDb, (float)sampleRate);
    gFiltroBassR.configurarLowShelf(90.0f, gananciaDb, (float)sampleRate);

    for (int i = 0; i < sampleCount; i += 2) {
        float l = (float)muestras[i] / 32768.0f;
        float r = (float)muestras[i + 1] / 32768.0f;

        float lFiltrado = gFiltroBassL.procesar(l);
        float rFiltrado = gFiltroBassR.procesar(r);

        // Mezcla con compresión suave para proteger altavoces de moto
        muestras[i] = (jshort)(softClip(lFiltrado) * 32767.0f);
        muestras[i + 1] = (jshort)(softClip(rFiltrado) * 32767.0f);
    }

    env->ReleaseShortArrayElements(audioSamples, muestras, 0);
}

/**
 * 2. PROCESAMIENTO DE ULTRA VOLUMEN (+100% A +300% CON LIMITADOR INTELIGENTE)
 */
JNIEXPORT void JNICALL
Java_com_example_reproductor_MOTOR_1AUDIO_1NATIVO_procesarUltraVolumen(
        JNIEnv *env,
        jclass clazz,
        jshortArray audioSamples,
        jint sampleCount,
        jfloat factorGanancia) { // 1.0f a 3.0f

    if (factorGanancia <= 1.01f || sampleCount <= 0) return;

    jshort *muestras = env->GetShortArrayElements(audioSamples, nullptr);
    if (!muestras) return;

    for (int i = 0; i < sampleCount; ++i) {
        float muestraNormalizada = ((float)muestras[i] / 32768.0f) * factorGanancia;
        // Aplicar limitador de clipping armónico
        float muestraLimitada = softClip(muestraNormalizada);
        muestras[i] = (jshort)(std::clamp(muestraLimitada * 32767.0f, -32768.0f, 32767.0f));
    }

    env->ReleaseShortArrayElements(audioSamples, muestras, 0);
}

/**
 * 3. PROCESAMIENTO DE ECUALIZADOR REAL (5 BANDAS: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
 */
JNIEXPORT void JNICALL
Java_com_example_reproductor_MOTOR_1AUDIO_1NATIVO_procesarEcualizador(
        JNIEnv *env,
        jclass clazz,
        jshortArray audioSamples,
        jint sampleCount,
        jfloatArray gananciasBandaDb, // 5 valores en dB (-15.0 a +15.0)
        jint sampleRate) {

    if (sampleCount <= 0) return;

    jfloat *ganancias = env->GetFloatArrayElements(gananciasBandaDb, nullptr);
    jshort *muestras = env->GetShortArrayElements(audioSamples, nullptr);

    if (!ganancias || !muestras) {
        if (ganancias) env->ReleaseFloatArrayElements(gananciasBandaDb, ganancias, JNI_ABORT);
        if (muestras) env->ReleaseShortArrayElements(audioSamples, muestras, JNI_ABORT);
        return;
    }

    float frecs[5] = {60.0f, 230.0f, 910.0f, 3600.0f, 14000.0f};
    for (int b = 0; b < 5; ++b) {
        gBandasL[b].configurarPeaking(frecs[b], ganancias[b], 1.2f, (float)sampleRate);
        gBandasR[b].configurarPeaking(frecs[b], ganancias[b], 1.2f, (float)sampleRate);
    }

    for (int i = 0; i < sampleCount; i += 2) {
        float l = (float)muestras[i] / 32768.0f;
        float r = (float)muestras[i + 1] / 32768.0f;

        for (int b = 0; b < 5; ++b) {
            l = gBandasL[b].procesar(l);
            r = gBandasR[b].procesar(r);
        }

        muestras[i] = (jshort)(softClip(l) * 32767.0f);
        muestras[i + 1] = (jshort)(softClip(r) * 32767.0f);
    }

    env->ReleaseFloatArrayElements(gananciasBandaDb, ganancias, 0);
    env->ReleaseShortArrayElements(audioSamples, muestras, 0);
}

/**
 * 4. PROCESAMIENTO DE ESPACIALIDAD 3D Y REVERBERACIÓN
 */
JNIEXPORT void JNICALL
Java_com_example_reproductor_MOTOR_1AUDIO_1NATIVO_procesarEspacialidad(
        JNIEnv *env,
        jclass clazz,
        jshortArray audioSamples,
        jint sampleCount,
        jfloat nivelEspacialidad) { // 0.0f a 1.0f

    if (nivelEspacialidad <= 0.01f || sampleCount <= 0) return;

    jshort *muestras = env->GetShortArrayElements(audioSamples, nullptr);
    if (!muestras) return;

    int retardoMuestras = (int)(nivelEspacialidad * 350.0f) + 40; // 40 a 390 muestras de retardo estéreo

    for (int i = 0; i < sampleCount; i += 2) {
        float l = (float)muestras[i] / 32768.0f;
        float r = (float)muestras[i + 1] / 32768.0f;

        int indiceLectura = (gIndiceRetardo - retardoMuestras + MAX_DELAY_SAMPLES) % MAX_DELAY_SAMPLES;
        float retardoL = gBufferRetardoL[indiceLectura];
        float retardoR = gBufferRetardoR[indiceLectura];

        gBufferRetardoL[gIndiceRetardo] = l;
        gBufferRetardoR[gIndiceRetardo] = r;
        gIndiceRetardo = (gIndiceRetardo + 1) % MAX_DELAY_SAMPLES;

        // Mezcla cruzada de fases para ampliación de escenario acústico
        float nuevoL = l + (l - retardoR) * (nivelEspacialidad * 0.45f);
        float nuevoR = r + (r - retardoL) * (nivelEspacialidad * 0.45f);

        muestras[i] = (jshort)(softClip(nuevoL) * 32767.0f);
        muestras[i + 1] = (jshort)(softClip(nuevoR) * 32767.0f);
    }

    env->ReleaseShortArrayElements(audioSamples, muestras, 0);
}

} // extern "C"
