# ═══════════════════════════════════════════════════════════════════════════
# CONFIGURACIÓN CMAKE - REPRODUCTOR TX PRO (TEAM NACIONAL TX ARAGUA)
# Compilación del motor nativo C++ de procesamiento digital de audio.
# ═══════════════════════════════════════════════════════════════════════════

cmake_minimum_required(VERSION 3.22.1)

project("motor_audio_tx")

# Compilar librería compartida con optimizaciones O3 para procesamiento DSP
add_library(
        motor_audio_tx
        SHARED
        MOTOR_AUDIO.cpp
)

find_library(
        log-lib
        log
)

find_library(
        android-lib
        android
)

target_link_libraries(
        motor_audio_tx
        ${log-lib}
        ${android-lib}
)
