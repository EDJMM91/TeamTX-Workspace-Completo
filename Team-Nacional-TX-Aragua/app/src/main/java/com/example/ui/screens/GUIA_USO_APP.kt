package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.MotoOrangePrimary

/**
 * Modelo de datos para la Guía de Usuario orientada al público general (no desarrolladores)
 */
data class ModuloGuiaUsuario(
    val id: String,
    val titulo: String,
    val subtitulo: String,
    val icono: ImageVector,
    val colorIcono: Color,
    val categoria: String,
    val resumen: String,
    val pasosUso: List<String>,
    val funcionesClave: List<Pair<String, String>>,
    val consejos: List<String>
)

object BaseDatosGuiaUsuario {
    val modulos = listOf(
        ModuloGuiaUsuario(
            id = "muro",
            titulo = "Muro y Noticias (Feed)",
            subtitulo = "Comunicados oficiales, retos y avisos",
            icono = Icons.Default.Campaign,
            colorIcono = Color(0xFF38BDF8),
            categoria = "Comunidad",
            resumen = "Es el periódico y cartelera digital de Team TX. Aquí encuentras las convocatorias oficiales, los retos de kilometraje y las noticias importantes de la directiva.",
            pasosUso = listOf(
                "Navega deslizando hacia arriba o abajo para leer las publicaciones recientes.",
                "Usa los botones superiores (Todos, Avisos, Retos, Comunicados) para filtrar lo que deseas ver.",
                "Toca el corazón ❤️ para dar like y apoyar las publicaciones de tus compañeros.",
                "Toca en 'Comentarios' para dejar un mensaje u opinar en el tablón.",
                "Si eres Directiva, usa el botón '+' flotante para redactar y fijar anuncios urgentes."
            ),
            funcionesClave = listOf(
                "Retos Moteros" to "Retos con meta de kilometraje y medalla especial para los participantes.",
                "Alertas Urgentes" to "Comunicados destacados en color rojo que requieren atención inmediata.",
                "Cumpleañeros" to "Banner que avisa quiénes están de cumpleaños en el mes."
            ),
            consejos = listOf(
                "Las publicaciones fijadas (con pin 📌) siempre aparecen arriba.",
                "Revisa el muro antes de salir a rodar para verificar cambios de ruta de última hora."
            )
        ),
        ModuloGuiaUsuario(
            id = "chat",
            titulo = "Chat General y Directiva",
            subtitulo = "Mensajería en vivo, audios y stickers",
            icono = Icons.Default.Chat,
            colorIcono = Color(0xFF4ADE80),
            categoria = "Comunicación",
            resumen = "Comunícate en tiempo real con todos los miembros del club. Diseñado para funcionar de manera fluida y similar a WhatsApp.",
            pasosUso = listOf(
                "Escribe tu mensaje en la barra inferior y pulsa la flecha naranja para enviar.",
                "Mantén presionado el icono del micrófono 🎙️ para grabar un audio y suéltalo para enviar.",
                "Si activas 'Transcripción', los audios se convertirán a texto automáticamente en pantalla.",
                "Toca el icono de stickers para enviar stickers transparentes o emojis en tus mensajes.",
                "Mantén presionado cualquier mensaje para ver el menú: Responder, Reaccionar con emojis o ver 'Info del Mensaje' (quiénes lo leyeron)."
            ),
            funcionesClave = listOf(
                "Info del Mensaje" to "Descubre exactamente qué miembros ya leyeron tu mensaje y a qué hora.",
                "Voz a Texto" to "Transcribe los audios a texto automáticamente sin necesidad de escucharlos.",
                "Stickers Flotantes" to "Stickers limpios tipo PNG sin marcos molestos."
            ),
            consejos = listOf(
                "En el chat de Directiva solo pueden entrar los miembros autorizados por la directiva.",
                "Puedes deslizar un mensaje hacia la derecha para responderlo directamente."
            )
        ),
        ModuloGuiaUsuario(
            id = "rodadas",
            titulo = "Rodadas y Convoy",
            subtitulo = "Salidas grupales y rutas oficiales",
            icono = Icons.Default.TwoWheeler,
            colorIcono = MotoOrangePrimary,
            categoria = "Rutas",
            resumen = "Organización de todas las salidas en moto del club. Conoce los puntos de encuentro, la hora de partida, la ruta y quiénes conforman el convoy.",
            pasosUso = listOf(
                "Entra al módulo Rodadas para ver las salidas programadas.",
                "Toca en una rodada para ver detalles: hora, lugar de concentración, destino y nivel de dificultad.",
                "Presiona 'Anotarme' para confirmar tu participación y que el capitán de ruta te tome en cuenta.",
                "Durante la rodada, mantén tu posición asignada en el convoy (Puntero, Bloqueadores, Escoba)."
            ),
            funcionesClave = listOf(
                "Roles de Convoy" to "Identifica quién es el Capitán de Ruta, el Puntero guía y la Moto Escoba de cierre.",
                "Punto de Salida GPS" to "Toca la ubicación para abrir la ruta directa en el mapa."
            ),
            consejos = listOf(
                "Llega siempre con tanque lleno y 15 minutos antes de la hora de briefing.",
                "Revisa tu presión de cauchos y frenos antes de salir."
            )
        ),
        ModuloGuiaUsuario(
            id = "miembros",
            titulo = "Directorio de Miembros",
            subtitulo = "Pilotos, placas TX y rangos",
            icono = Icons.Default.Groups,
            colorIcono = Color(0xFFA78BFA),
            categoria = "Comunidad",
            resumen = "Directorio oficial con las fichas de todos los pilotos activos, aspirantes y directiva del club.",
            pasosUso = listOf(
                "Usa el buscador por nombre, apodo o número de placa (TX-XXX).",
                "Toca sobre la tarjeta de un miembro para ver su ficha completa, modelo de moto y rango.",
                "Puedes ver el estado de asistencia a eventos y el progreso de los aspirantes."
            ),
            funcionesClave = listOf(
                "Filtro por Rol" to "Visualiza Directiva, Pilotos Oficiales, Aspirantes y Aliados.",
                "Placas TX" to "Identificación única de cada moto dentro de la hermandad."
            ),
            consejos = listOf(
                "Mantén tu número de teléfono y foto de perfil actualizados en tu ficha."
            )
        ),
        ModuloGuiaUsuario(
            id = "sos",
            titulo = "SOS Vial (Emergencias)",
            subtitulo = "Auxilio mecánico y médico en ruta",
            icono = Icons.Default.Emergency,
            colorIcono = Color(0xFFEF4444),
            categoria = "Seguridad",
            resumen = "Sistema de auxilio rápido en carretera. Si tienes un percance, caída o falla mecánica, emite una señal que alerta a los miembros cercanos.",
            pasosUso = listOf(
                "En caso de emergencia, abre el módulo SOS Vial.",
                "Selecciona el nivel de gravedad: Falla Mecánica leve, Falta de Gasolina, Auxilio Médico o Accidente.",
                "Confirma la emisión: el sistema enviará tu ubicación GPS exacta a la directiva y compañeros cercanos.",
                "Mantén la calma y permanece en un lugar seguro junto a tu moto mientras llega la asistencia."
            ),
            funcionesClave = listOf(
                "Triage de 4 Niveles" to "Clasifica la urgencia para enviar la ayuda adecuada.",
                "Ubicación GPS en Vivo" to "Coordenadas precisas para que la escoba o taller llegue a tu rescate."
            ),
            consejos = listOf(
                "No uses el botón SOS para bromas ni pruebas: moviliza a la directiva y equipo de rescate."
            )
        ),
        ModuloGuiaUsuario(
            id = "mapa",
            titulo = "Mapa TX y Talleres",
            subtitulo = "Navegación GPS y puntos de interés",
            icono = Icons.Default.Map,
            colorIcono = Color(0xFF2DD4BF),
            categoria = "Rutas",
            resumen = "Mapa interactivo diseñado para motociclistas con navegación sin necesidad de internet constante y ubicación de talleres aliados.",
            pasosUso = listOf(
                "Abre el mapa para visualizar tu posición y puntos de interés motero.",
                "Explora la capa de talleres mecánicos, caucheras y ventas de repuestos verificadas.",
                "Guarda tus rutas favoritas para rodar en grupo."
            ),
            funcionesClave = listOf(
                "Talleres Aliados" to "Directorio con teléfonos y especialidad de talleres de confianza.",
                "Puntos de Reagrupación" to "Lugares estratégicos marcados para paradas del convoy."
            ),
            consejos = listOf(
                "Descarga los mapas previamente con WiFi para usarlos en carretera sin gastar datos."
            )
        ),
        ModuloGuiaUsuario(
            id = "carnet",
            titulo = "Carnet Digital TX",
            subtitulo = "Pasaporte y credencial oficial",
            icono = Icons.Default.Badge,
            colorIcono = Color(0xFFFBBF24),
            categoria = "Identidad",
            resumen = "Tu credencial digital que te acredita como miembro oficial de Team Nacional TX Venezuela.",
            pasosUso = listOf(
                "Muestra tu carnet en eventos, alcabalas o talleres aliados para identificarte.",
                "Contiene tu código QR único, número de placa TX, tipo de sangre y contacto de emergencia.",
                "Se actualiza automáticamente con tus ascensos de rango y estatus."
            ),
            funcionesClave = listOf(
                "QR Verificable" to "Permite a la directiva escanear y validar que eres miembro activo.",
                "Datos Médicos" to "Muestra tu grupo sanguíneo y alergias en caso de emergencia médica."
            ),
            consejos = listOf(
                "Ten siempre tu carnet a mano en el teléfono antes de salir a rodar."
            )
        ),
        ModuloGuiaUsuario(
            id = "tesoreria",
            titulo = "Tesorería y Finanzas",
            subtitulo = "Cuentas claras y aportes del club",
            icono = Icons.Default.AccountBalanceWallet,
            colorIcono = Color(0xFF34D399),
            categoria = "Gestión",
            resumen = "Transparencia absoluta en los fondos del club. Registro de ingresos, gastos de eventos, fondos de emergencia y donaciones.",
            pasosUso = listOf(
                "Consulta el balance actual y el historial de movimientos aprobados.",
                "Verifica tus aportes mensuales y cuotas de eventos.",
                "Revisa las compras de insumos para rodadas y botiquines."
            ),
            funcionesClave = listOf(
                "Libro de Cuentas" to "Cada bolívar o dólar está justificado con recibos y notas de compra.",
                "Fondo de Ayuda" to "Apartado exclusivo para apoyar a hermanos en emergencias médicas."
            ),
            consejos = listOf(
                "Paga tus aportes a tiempo para mantener los fondos de auxilio vial al 100%."
            )
        ),
        ModuloGuiaUsuario(
            id = "normativas",
            titulo = "Normativas y Estatutos",
            subtitulo = "Honor, Lealtad, Respeto y Humildad",
            icono = Icons.Default.MenuBook,
            colorIcono = Color(0xFFE879F9),
            categoria = "Reglamento",
            resumen = "Los principios y reglas que rigen nuestra hermandad motera a nivel regional y nacional.",
            pasosUso = listOf(
                "Lee las normas de convivencia en carretera, eventos y grupos de WhatsApp.",
                "Conoce el reglamento de vestimenta, uso de chaleco y porte del parche TX.",
                "Consulta los deberes y derechos según tu rango."
            ),
            funcionesClave = listOf(
                "Código de Honor" to "Reglas básicas de compañerismo: nunca dejar a un hermano botado en ruta.",
                "Tribunal Biker" to "Procedimientos justos para resolver diferencias dentro del club."
            ),
            consejos = listOf(
                "El respeto y la prudencia en dos ruedas son la mejor carta de presentación de Team TX."
            )
        ),
        ModuloGuiaUsuario(
            id = "configuracion",
            titulo = "Configuraciones y Perfil",
            subtitulo = "Personaliza tu experiencia",
            icono = Icons.Default.Settings,
            colorIcono = Color(0xFF94A3B8),
            categoria = "Ajustes",
            resumen = "Ajusta la aplicación a tu gusto: cambia tu foto, datos de la moto, preferencias de sonido y privacidad.",
            pasosUso = listOf(
                "Entra a Configuraciones tocando el engranaje.",
                "Edita tu foto de perfil, apodo, teléfono y número de placa.",
                "Activa o desactiva la 'Transcripción Automática de Voz' para el chat.",
                "Configura las notificaciones de avisos urgentes."
            ),
            funcionesClave = listOf(
                "Modo Noche / Estilo TX" to "Diseño oscuro de alto contraste para uso diurno y nocturno.",
                "Datos de Moto" to "Registra el color, año y modificaciones de tu TX."
            ),
            consejos = listOf(
                "Si cambias de número telefónico, actualízalo de inmediato en tu perfil."
            )
        ),
        ModuloGuiaUsuario(
            id = "actualizaciones",
            titulo = "Actualizaciones de la App (OTA)",
            subtitulo = "Mantén tu app siempre al día",
            icono = Icons.Default.SystemUpdate,
            colorIcono = Color(0xFFF97316),
            categoria = "Sistema",
            resumen = "Cómo descargar e instalar las nuevas versiones de la aplicación sin complicaciones.",
            pasosUso = listOf(
                "Ve al módulo Info (ℹ️) en la barra inferior.",
                "Toca el botón naranja 'Buscar Actualizaciones'.",
                "Si hay una versión nueva, verás la lista de mejoras.",
                "Pulsa 'Descargar e Instalar' y observa la barra de progreso.",
                "Cuando termine, el instalador se abrirá automáticamente para actualizar tu app."
            ),
            funcionesClave = listOf(
                "Descarga Directa Rápida" to "Descarga el archivo APK con barra de porcentaje en tiempo real.",
                "Opción Navegador" to "Si prefieres, puedes descargarla directamente desde tu navegador Chrome."
            ),
            consejos = listOf(
                "No desinstales la app antes de actualizar; así mantendrás tus chats y preferencias intactos."
            )
        )
    )
}

/**
 * Pantalla / Diálogo Completo de la Guía de Usuario
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuiaUsoAppDialog(
    onDismiss: () -> Unit
) {
    var queryBusqueda by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Todas") }
    var moduloExpandidoId by remember { mutableStateOf<String?>(null) }

    val categorias = remember {
        listOf("Todas") + BaseDatosGuiaUsuario.modulos.map { it.categoria }.distinct()
    }

    val modulosFiltrados = remember(queryBusqueda, categoriaSeleccionada) {
        BaseDatosGuiaUsuario.modulos.filter { mod ->
            val coincideCategoria = (categoriaSeleccionada == "Todas" || mod.categoria == categoriaSeleccionada)
            val coincideTexto = queryBusqueda.isBlank() ||
                    mod.titulo.contains(queryBusqueda, ignoreCase = true) ||
                    mod.subtitulo.contains(queryBusqueda, ignoreCase = true) ||
                    mod.resumen.contains(queryBusqueda, ignoreCase = true) ||
                    mod.pasosUso.any { it.contains(queryBusqueda, ignoreCase = true) } ||
                    mod.funcionesClave.any { it.first.contains(queryBusqueda, ignoreCase = true) || it.second.contains(queryBusqueda, ignoreCase = true) }
            coincideCategoria && coincideTexto
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header Superior
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MotoOrangePrimary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MotoOrangePrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MotoOrangePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "GUÍA DE USO TEAM TX",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Manual interactivo para el piloto",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                // Barra de Búsqueda
                OutlinedTextField(
                    value = queryBusqueda,
                    onValueChange = { queryBusqueda = it },
                    placeholder = { Text("Buscar función, rodada, SOS, chat...", fontSize = 13.sp, color = Color.Gray) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (queryBusqueda.isNotEmpty()) {
                            IconButton(onClick = { queryBusqueda = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedBorderColor = MotoOrangePrimary,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )

                // Chips de Categorías
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    items(categorias) { cat ->
                        val isSelected = (cat == categoriaSeleccionada)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MotoOrangePrimary else Color(0xFF1E293B),
                            border = BorderStroke(1.dp, if (isSelected) MotoOrangePrimary else Color(0xFF334155)),
                            modifier = Modifier.clickable { categoriaSeleccionada = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Lista de Módulos
                if (modulosFiltrados.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No se encontraron funciones para '$queryBusqueda'", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(modulosFiltrados, key = { it.id }) { modulo ->
                            val isExpanded = (moduloExpandidoId == modulo.id)
                            TarjetaModuloGuia(
                                modulo = modulo,
                                isExpanded = isExpanded,
                                onToggle = {
                                    moduloExpandidoId = if (isExpanded) null else modulo.id
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            // Pie de página con lema
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "⚡ TEAM NACIONAL TX VENEZUELA ⚡",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = MotoOrangePrimary,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Honor • Lealtad • Respeto • 100% Humildad",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta individual colapsable de módulo con diseño biker premium
 */
@Composable
fun TarjetaModuloGuia(
    modulo: ModuloGuiaUsuario,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(
            1.dp,
            if (isExpanded) modulo.colorIcono.copy(alpha = 0.6f) else Color(0xFF334155).copy(alpha = 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Encabezado de la Tarjeta
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = modulo.colorIcono.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, modulo.colorIcono.copy(alpha = 0.3f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            modulo.icono,
                            contentDescription = null,
                            tint = modulo.colorIcono,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = modulo.titulo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            Text(
                                text = modulo.categoria,
                                fontSize = 9.sp,
                                color = modulo.colorIcono,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = modulo.subtitulo,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isExpanded) modulo.colorIcono else Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Contenido Expandido
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Divider(color = Color(0xFF334155).copy(alpha = 0.6f), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Descripción / Resumen
                    Text(
                        text = modulo.resumen,
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Paso a Paso
                    Text(
                        text = "🚀 CÓMO USARLO PASO A PASO:",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = modulo.colorIcono,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    modulo.pasosUso.forEachIndexed { index, paso ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = modulo.colorIcono.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(top = 1.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = modulo.colorIcono
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = paso,
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Funciones Clave
                    if (modulo.funcionesClave.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "⭐ FUNCIONES DESTACADAS:",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = MotoOrangePrimary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        modulo.funcionesClave.forEach { (nombre, desc) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "• $nombre",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    // Consejos
                    if (modulo.consejos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        modulo.consejos.forEach { consejo ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(text = "💡", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = consejo,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFBBF24),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
