# Plan de Implementación: Tema Claro en Carnet TX (Pasaporte) y Feed, y Selector de Cuenta Google

## Descripción del Problema
1. **Selector de Cuentas Google**: Al vincular o cambiar de cuenta Google, la app inicia sesión automáticamente con la cuenta cacheada sin mostrar el selector. Se requiere forzar el cierre de sesión local de Google (`signOut` + `revokeAccess`) antes de lanzar el selector para que el usuario pueda elegir explícitamente qué cuenta usar.
2. **Tema Claro en Carnet TX (`PassportScreen.kt`) y Feed (`FeedScreen.kt`)**: Actualmente usan colores oscuros fijos. Deben adaptarse al tema claro del Dashboard (`DashboardFondoConfig`) cuando esté seleccionado, utilizando los mismos colores de fondo, tarjetas y tipografía sin alterar la estabilidad actual.

## Propuesta de Cambios (Enfoque Quirúrgico y No-Regresión)

### 1. Módulo Nube / Google Auth
#### [MODIFY] [AUTENTICACION_GOOGLE.kt](file:///D:/MAPA/Team-Nacional-TX-Aragua/app/src/main/java/com/aistudio/teamtxvzla/nube/AUTENTICACION_GOOGLE.kt)
- En `GoogleLoginHelper.abrirSelector()`, ejecutar `cliente.signOut().addOnCompleteListener { cliente.revokeAccess().addOnCompleteListener { launcher.launch(cliente.signInIntent) } }` para forzar la aparición del selector de cuentas de Google.

### 2. Módulo Carnet TX / Pasaporte
#### [MODIFY] [PassportScreen.kt](file:///D:/MAPA/Team-Nacional-TX-Aragua/app/src/main/java/com/example/ui/screens/PassportScreen.kt)
- Integrar `DashboardFondoConfig` para detectar el tema activo.
- Adaptar colores de fondo (`DashboardFondoConfig.ColorFondoClaro` / oscuro) y tarjetas (`DashboardFondoConfig.ColorTarjetaClara` / `TxCarbonDark`) manteniendo la estética dorada/bronce de pasaporte.

### 3. Módulo Muro / Feed
#### [MODIFY] [FeedScreen.kt](file:///D:/MAPA/Team-Nacional-TX-Aragua/app/src/main/java/com/example/ui/screens/FeedScreen.kt)
- Integrar soporte opcional para tema claro usando `DashboardFondoConfig` en su fondo y tarjetas de publicaciones, manteniendo intacta la lógica de comentarios y reacciones.

## Plan de Verificación
- Compilación exitosa (`:app:assembleDebug`).
- Verificación visual del tema claro en Pasaporte y Feed.
- Prueba del flujo de vinculación Google para confirmar que abre el selector de cuentas.
