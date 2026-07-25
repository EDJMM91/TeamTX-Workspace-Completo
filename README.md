1. Error: Cannot resolve method 'getMyApplication' in 'MapActivity'
•
Qué hice: Cambié la llamada mapActivity.getMyApplication().getSettings() por simplemente settings.
•
Por qué estaba: El código intentaba usar un método llamado getMyApplication() sobre el objeto mapActivity. Sin embargo, la clase MapActivity no tiene ese método.
•
La solución técnica: Como ProfileAppearanceButton hereda de MapButton, ya tiene acceso a campos protegidos como settings y app. No era necesario pedirle a la actividad que nos diera la aplicación para llegar a los ajustes; ya los teníamos disponibles directamente en la clase base.
2. Advertencia: Unused import statement (Import de MapActivity no usado)
•
Qué hice: Eliminé la línea import net.osmand.plus.activities.MapActivity;.
•
Por qué estaba: Al simplificar el código anterior, la palabra "MapActivity" dejó de usarse explícitamente en el archivo. Aunque usamos la variable mapActivity (que heredamos), no estamos declarando ninguna variable nueva ni llamando a métodos estáticos de esa clase, por lo que el compilador de Java considera que el "import" es innecesario y ensucia el código.
En resumen: El error principal era una redundancia incorrecta (intentar buscar la configuración a través de un método inexistente en la actividad en lugar de usar la que ya tiene el botón por herencia). Al corregirlo, el código quedó más limpio y el import de la actividad dejó de ser necesario.
