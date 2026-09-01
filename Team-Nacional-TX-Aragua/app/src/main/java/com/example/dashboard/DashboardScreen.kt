package com.example.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.NavigationTab
import com.example.data.model.BikerCalendarEvent
import com.example.data.model.EmergencyAlert
import com.example.data.model.MemberProfile
import com.example.data.model.Publication

/**
 * Pantalla principal oficial (Dashboard) de la aplicación Team Nacional TX Aragua.
 * Gestiona y centraliza el acceso interactivo a todos los módulos del ecosistema.
 */
@Composable
fun DashboardScreen(
    currentMember: MemberProfile?,
    publications: List<Publication>,
    calendarEvents: List<BikerCalendarEvent>,
    emergencyAlerts: List<EmergencyAlert>,
    unreadChatCount: Int = 0,
    notificacionesNoLeidasCount: Int = 0,
    isDirectivaMode: Boolean = false,
    onNavigateToTab: (NavigationTab) -> Unit,
    onOpenSosModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fondoConfig = DashboardFondoConfig

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DashboardFondoConfig.ColorFondoClaro)
    ) {
        // Capa de fondo editable (Imagen personalizada / drawable con overlay translúcido)
        when (fondoConfig.tipoFondo) {
            TipoFondoDashboard.IMAGEN_PERSONALIZADA -> {
                if (!fondoConfig.imagenUri.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(fondoConfig.imagenUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Fondo Dashboard",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DashboardFondoConfig.ColorFondoClaro.copy(alpha = fondoConfig.opacidadSuperposicion))
                    )
                }
            }
            TipoFondoDashboard.IMAGEN_DRAWABLE -> {
                if (fondoConfig.drawableResId != null && fondoConfig.drawableResId != 0) {
                    Image(
                        painter = painterResource(id = fondoConfig.drawableResId!!),
                        contentDescription = "Fondo Dashboard",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DashboardFondoConfig.ColorFondoClaro.copy(alpha = fondoConfig.opacidadSuperposicion))
                    )
                }
            }
            else -> {
                // Color sólido claro estándar
            }
        }

        // Contenido scrolleable verticalmente
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // 1. Cabecera principal
            item {
                DashboardHeader(
                    currentMember = currentMember,
                    onOpenCarnet = { onNavigateToTab(NavigationTab.PROFILE) }
                )
            }

            // 2. Tarjeta de estado y perfil del piloto
            item {
                PilotStatusCard(
                    currentMember = currentMember,
                    onOpenCarnet = { onNavigateToTab(NavigationTab.PROFILE) }
                )
            }

            // 3. Sección "Mi Panel" (Bento Grid Táctico)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                SectionHeader(
                    titulo = "Mi Panel",
                    subtitulo = "Acceso rápido a tus herramientas activas"
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Bento 1: Tarjeta destacada grande: Muro
                    val ultimaPub = publications.firstOrNull()
                    val badgeMuro = if (publications.isNotEmpty()) "${publications.size} publicaciones" else null
                    BentoGridCard(
                        titulo = "Muro de Noticias",
                        subtitulo = ultimaPub?.title?.ifBlank { "Últimos comunicados y avisos" } ?: "Últimas publicaciones y novedades",
                        icono = Icons.Default.Campaign,
                        colorIcono = DashboardFondoConfig.ColorRojoCarrera,
                        colorFondoIcono = DashboardFondoConfig.ColorContenedorRojo,
                        badgeTexto = badgeMuro,
                        esDestacadoLargo = true,
                        onClick = { onNavigateToTab(NavigationTab.FEED) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(125.dp)
                    )

                    // Bento Fila 1: Chat y Avisos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoGridCard(
                            titulo = "Chat Táctico",
                            subtitulo = if (unreadChatCount > 0) "$unreadChatCount sin leer" else "Salas en vivo",
                            icono = Icons.Default.Forum,
                            colorIcono = Color(0xFF1E88E5),
                            colorFondoIcono = DashboardFondoConfig.ColorContenedorAzul,
                            badgeTexto = if (unreadChatCount > 0) "$unreadChatCount" else null,
                            badgeColor = DashboardFondoConfig.ColorRojoCarrera,
                            onClick = { onNavigateToTab(NavigationTab.CHAT) },
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                        )

                        BentoGridCard(
                            titulo = "Avisos",
                            subtitulo = if (notificacionesNoLeidasCount > 0) "$notificacionesNoLeidasCount nuevas" else "Alertas del club",
                            icono = Icons.Default.Notifications,
                            colorIcono = DashboardFondoConfig.ColorRojoCarrera,
                            colorFondoIcono = DashboardFondoConfig.ColorContenedorRojo,
                            badgeTexto = if (notificacionesNoLeidasCount > 0) "!" else null,
                            badgeColor = DashboardFondoConfig.ColorRojoCarrera,
                            onClick = { onNavigateToTab(NavigationTab.NOTIFICACIONES) },
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                        )
                    }

                    // Bento Fila 2: Ranking y Calendario
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoGridCard(
                            titulo = "Ranking",
                            subtitulo = "Podio y reputación",
                            icono = Icons.Default.MilitaryTech,
                            colorIcono = DashboardFondoConfig.ColorDoradoOro,
                            colorFondoIcono = DashboardFondoConfig.ColorContenedorDorado,
                            badgeTexto = "Top 10",
                            badgeColor = DashboardFondoConfig.ColorDoradoOro,
                            onClick = { onNavigateToTab(NavigationTab.RANKING) },
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                        )

                        val proxEvento = calendarEvents.firstOrNull()
                        BentoGridCard(
                            titulo = "Calendario",
                            subtitulo = proxEvento?.title?.take(16) ?: "Rodadas y eventos",
                            icono = Icons.Default.CalendarMonth,
                            colorIcono = Color(0xFF43A047),
                            colorFondoIcono = DashboardFondoConfig.ColorContenedorVerde,
                            badgeTexto = if (calendarEvents.isNotEmpty()) "${calendarEvents.size}" else null,
                            badgeColor = Color(0xFF43A047),
                            onClick = { onNavigateToTab(NavigationTab.CALENDARIO) },
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                        )
                    }

                    // Bento Fila 3: Mapa TX / Radar y Velocímetro
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoGridCard(
                            titulo = "Mapa TX & Radar",
                            subtitulo = "Navegación GPS en vivo",
                            icono = Icons.Default.Map,
                            colorIcono = DashboardFondoConfig.ColorRojoCarrera,
                            colorFondoIcono = DashboardFondoConfig.ColorContenedorRojo,
                            badgeTexto = "Radar",
                            badgeColor = DashboardFondoConfig.ColorRojoCarrera,
                            onClick = { onNavigateToTab(NavigationTab.MAPA) },
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                        )

                        BentoGridCard(
                            titulo = "Velocímetro",
                            subtitulo = "Odómetro y telemetría",
                            icono = Icons.Default.Speed,
                            colorIcono = Color(0xFF00ACC1),
                            colorFondoIcono = Color(0xFFE0F7FA),
                            badgeTexto = "Km",
                            badgeColor = Color(0xFF00ACC1),
                            onClick = { onNavigateToTab(NavigationTab.VELOCIMETRO) },
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                        )
                    }
                }
            }

            // 4. Sección "Todos los Módulos" (Scroll vertical)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    titulo = "Todos los Módulos",
                    subtitulo = "Ecosistema completo de herramientas del club"
                )
            }

            // Catálogo Completo de los 22 Módulos
            item {
                ModuleListRow(
                    titulo = "Muro de Publicaciones",
                    descripcion = "Noticias, avisos oficiales, comunicados y flyers",
                    icono = Icons.Default.Campaign,
                    colorIcono = DashboardFondoConfig.ColorRojoCarrera,
                    onClick = { onNavigateToTab(NavigationTab.FEED) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Chat Táctico y Canales",
                    descripcion = "Salas multicanal, directiva, notas de voz y stickers",
                    icono = Icons.Default.Forum,
                    colorIcono = Color(0xFF1E88E5),
                    badgeCount = unreadChatCount,
                    onClick = { onNavigateToTab(NavigationTab.CHAT) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Avisos y Notificaciones",
                    descripcion = "Centro de alertas importantes del club",
                    icono = Icons.Default.Notifications,
                    colorIcono = DashboardFondoConfig.ColorRojoCarrera,
                    badgeCount = notificacionesNoLeidasCount,
                    onClick = { onNavigateToTab(NavigationTab.NOTIFICACIONES) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Ranking de Pilotos",
                    descripcion = "Puntos de reputación, calificaciones y tabla de honor",
                    icono = Icons.Default.MilitaryTech,
                    colorIcono = DashboardFondoConfig.ColorDoradoOro,
                    onClick = { onNavigateToTab(NavigationTab.RANKING) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Calendario Motero",
                    descripcion = "Programación de rodadas, aniversarios y eventos",
                    icono = Icons.Default.CalendarMonth,
                    colorIcono = Color(0xFF43A047),
                    onClick = { onNavigateToTab(NavigationTab.CALENDARIO) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Mapa TX y Radar Offline",
                    descripcion = "Navegación vectorial, capas tácticas y ubicación en tiempo real",
                    icono = Icons.Default.Map,
                    colorIcono = DashboardFondoConfig.ColorRojoCarrera,
                    onClick = { onNavigateToTab(NavigationTab.MAPA) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Velocímetro y Telemetría",
                    descripcion = "Registro de velocidad máxima, odómetro continuo y viajes",
                    icono = Icons.Default.Speed,
                    colorIcono = Color(0xFF00ACC1),
                    onClick = { onNavigateToTab(NavigationTab.VELOCIMETRO) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Reproductor Musical TX Pro",
                    descripcion = "Audio motero, podcasts, transmisiones y nube flotante",
                    icono = Icons.Default.MusicNote,
                    colorIcono = Color(0xFF8E24AA),
                    onClick = { onNavigateToTab(NavigationTab.PLAYER) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Retos y Desafíos TX",
                    descripcion = "Metas de kilometraje, rutas de montaña y logros",
                    icono = Icons.Default.EmojiEvents,
                    colorIcono = DashboardFondoConfig.ColorDoradoOro,
                    onClick = { onNavigateToTab(NavigationTab.RETOS) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Rodadas y Caravanas",
                    descripcion = "Gestión de convoy, capitán de ruta, colas y bitácora",
                    icono = Icons.Default.TwoWheeler,
                    colorIcono = DashboardFondoConfig.ColorRojoCarrera,
                    onClick = { onNavigateToTab(NavigationTab.RIDES) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Mercado Biker",
                    descripcion = "Compra y venta de motos, repuestos y accesorios",
                    icono = Icons.Default.Storefront,
                    colorIcono = Color(0xFF3949AB),
                    onClick = { onNavigateToTab(NavigationTab.MERCADO) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Bitácora de Mantenimiento",
                    descripcion = "Control de cambios de aceite, repuestos y kilometraje",
                    icono = Icons.Default.Build,
                    colorIcono = Color(0xFF5D4037),
                    onClick = { onNavigateToTab(NavigationTab.BITACORA) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Directorio de Talleres",
                    descripcion = "Talleres mecánicos recomendados y especialistas TX",
                    icono = Icons.Default.Storefront,
                    colorIcono = Color(0xFF00897B),
                    onClick = { onNavigateToTab(NavigationTab.DIRECTORIO) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Pasaporte Motero",
                    descripcion = "Sellado de destinos turísticos y rutas nacionales",
                    icono = Icons.Default.Explore,
                    colorIcono = Color(0xFFD81B60),
                    onClick = { onNavigateToTab(NavigationTab.PASAPORTE) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Directorio de Miembros",
                    descripcion = "Padrón oficial del club, rangos y fichas técnicas",
                    icono = Icons.Default.Groups,
                    colorIcono = Color(0xFF0288D1),
                    onClick = { onNavigateToTab(NavigationTab.MEMBERS) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Tesorería y Finanzas",
                    descripcion = "Aportes mensuales, tasa oficial BCV y balance general",
                    icono = Icons.Default.AccountBalanceWallet,
                    colorIcono = Color(0xFF2E7D32),
                    onClick = { onNavigateToTab(NavigationTab.FINANCES) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Inventario de Equipos",
                    descripcion = "Herramientas del club, repuestos de emergencia y préstamos",
                    icono = Icons.Default.Handyman,
                    colorIcono = Color(0xFFEF6C00),
                    onClick = { onNavigateToTab(NavigationTab.INVENTORY) }
                )
            }

            item {
                val haySosActiva = emergencyAlerts.any { it.status == com.example.data.model.EmergencyStatus.ACTIVA }
                ModuleListRow(
                    titulo = "SOS Vial de Emergencia",
                    descripcion = "Alerta de pánico en ruta, auxilio y ficha médica",
                    icono = Icons.Default.Emergency,
                    colorIcono = DashboardFondoConfig.ColorRojoCarrera,
                    badgeAlerta = haySosActiva,
                    onClick = { onNavigateToTab(NavigationTab.SOS) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Normativas y Estatutos",
                    descripcion = "Reglamento interno Aragua y normativa nacional",
                    icono = Icons.Default.Gavel,
                    colorIcono = Color(0xFF455A64),
                    onClick = { onNavigateToTab(NavigationTab.NORMATIVAS) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Gobernanza Directiva",
                    descripcion = "Panel exclusivo para directivos, códigos y sanciones",
                    icono = Icons.Default.Shield,
                    colorIcono = DashboardFondoConfig.ColorDoradoOro,
                    onClick = { onNavigateToTab(NavigationTab.DIRECTIVA) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Carnet TX Digital",
                    descripcion = "Credencial oficial del piloto, solvencia y vinculación",
                    icono = Icons.Default.Badge,
                    colorIcono = DashboardFondoConfig.ColorRojoCarrera,
                    onClick = { onNavigateToTab(NavigationTab.PROFILE) }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Ajustes y Configuración",
                    descripcion = "Fondos del dashboard, sonidos, tema y gestión de cuenta",
                    icono = Icons.Default.Settings,
                    colorIcono = Color(0xFF546E7A),
                    onClick = { onNavigateToTab(NavigationTab.CONFIGURACIONES) }
                )
            }
        }
    }
}
