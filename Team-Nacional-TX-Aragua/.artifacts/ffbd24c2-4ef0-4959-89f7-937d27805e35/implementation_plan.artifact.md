# Plan de Implementación: Sistema de Encuestas Interactivas en el Chat

## Descripción del Problema / Requerimiento
El usuario solicitó integrar un sistema de creación y votación de **Encuestas Interactivas** en el chat del club (superando a WhatsApp en opciones), manteniendo el tema claro del Dashboard (`DashboardFondoConfig`) y cumpliendo estrictamente con la regla de contraste: **nunca texto blanco sobre fondo blanco** (`RECUERDA LETRAS BLANCAS NO DEBEN IR EN FONDO BLANCO`).

## Propuesta de Cambios (Sin Compilar aún, según instrucción)

### 1. Modelo de Datos
#### [MODIFY] [Models.kt](file:///D:/MAPA/Team-Nacional-TX-Aragua/app/src/main/java/com/example/data/model/Models.kt)
- Añadir `POLL` al enum `MessageType`.
- Añadir campos a `ChatMessage`: `isPoll`, `pollQuestion`, `pollOptionsJson`, `pollVotesJson`.

### 2. Interfaz de Chat e Interactividad
#### [MODIFY] [ClubChatScreen.kt](file:///D:/MAPA/Team-Nacional-TX-Aragua/app/src/main/java/com/example/ui/screens/ClubChatScreen.kt)
- Añadir botón de Encuesta (`Icons.Default.Poll`) en la barra de entrada del chat.
- Implementar `CreatePollDialog`: Diálogo táctico para configurar pregunta y opciones de encuesta.
- Implementar Tarjeta de Encuesta Interactiva en el feed de mensajes:
  - Muestra la pregunta y las opciones como barras de progreso interactivas con porcentaje y conteo de votos en tiempo real.
  - Al pulsar una opción, el piloto emite/cambia su voto instantáneamente.
  - Tema claro adaptado al Dashboard (`DashboardFondoConfig`), garantizando colores de texto oscuros sobre fondo claro y alto contraste.

## Plan de Verificación (Pendiente hasta orden de compilación)
- Compilación y pruebas posteriores con `:app:assembleDebug`.
