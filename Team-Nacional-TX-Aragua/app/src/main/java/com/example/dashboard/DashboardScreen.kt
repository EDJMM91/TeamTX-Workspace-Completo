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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import com.example.NavigationTab
import com.example.ui.theme.TxFlameRed
import com.example.dashboard.DashboardFondoConfig
import com.example.data.model.BikerCalendarEvent
import com.example.data.model.EmergencyAlert
import com.example.data.model.MemberProfile
import com.example.data.model.MemberRole
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
    var mostrarDialogoMeshBloqueado by remember { mutableStateOf(false) }

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
                    onOpenCarnet = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("PROFILE")
                        onNavigateToTab(NavigationTab.PROFILE)
                    }
                )
            }

            // 2.5 Sidebar / Barra Desplegable Interactiva (Módulos más usados, Info, Ajustes, Directiva)
            item {
                DashboardSidebarBar(
                    currentMember = currentMember,
                    isDirectivaMode = isDirectivaMode,
                    onNavigateToTab = { tab ->
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo(tab.name)
                        onNavigateToTab(tab)
                    }
                )
            }

            // 3. Sección "Mi Panel" (Bento Grid Táctico)
            item {
                Spacer(modifier = Modifier.height(6.dp))
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Fila 1: Accesos rápidos (Scroll horizontal interactivo con iconos modernos)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Scroll Horizontal de Iconos Rápidos Estilo Moderno
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        ) {
                            val accesosRapidos = listOf(
                                Triple(NavigationTab.PLAYER, Color(0xFF8E24AA), Color(0xFFF3E5F5)),
                                Triple(NavigationTab.SOS, DashboardFondoConfig.ColorRojoCarrera, DashboardFondoConfig.ColorContenedorRojo),
                                Triple(NavigationTab.VELOCIMETRO, Color(0xFF00ACC1), Color(0xFFE0F7FA)),
                                Triple(NavigationTab.CALENDARIO, Color(0xFF43A047), Color(0xFFE8F5E9)),
                                Triple(NavigationTab.MAPA, DashboardFondoConfig.ColorRojoCarrera, DashboardFondoConfig.ColorContenedorRojo),
                                Triple(NavigationTab.DIRECTORIO, Color(0xFF00897B), Color(0xFFE0F2F1)),
                                Triple(NavigationTab.CONFIGURACIONES, Color(0xFF546E7A), Color(0xFFECEFF1))
                            )
                            items(accesosRapidos) { (tab, tint, bg) ->
                                Surface(
                                    onClick = {
                                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo(tab.name)
                                        onNavigateToTab(tab)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = bg,
                                    modifier = Modifier.width(80.dp).fillMaxHeight()
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = tab.iconFilled,
                                            contentDescription = tab.label,
                                            tint = tint,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = tab.label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = tint.copy(alpha = 0.9f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bento Fila 1: Directorio Comercios y Directorio de Miembros (con icono de contactos)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BentoGridCard(
                            titulo = "Comercios",
                            subtitulo = "Talleres y servicios",
                            icono = Icons.Default.Storefront,
                            colorIcono = Color(0xFF00897B),
                            colorFondoIcono = Color(0xFFE0F2F1),
                            onClick = {
                                com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("DIRECTORIO")
                                onNavigateToTab(NavigationTab.DIRECTORIO)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(102.dp)
                        )

                        BentoGridCard(
                            titulo = "Pilotos",
                            subtitulo = "Directorio de contactos",
                            icono = Icons.Default.Contacts, // 📇 Icono explícito de contactos
                            colorIcono = Color(0xFF0288D1),
                            colorFondoIcono = Color(0xFFE1F5FE),
                            onClick = {
                                com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("MEMBERS")
                                onNavigateToTab(NavigationTab.MEMBERS)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(102.dp)
                        )
                    }

                    // Bento Fila 2: Muro y Chat Táctico
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val ultimaPub = publications.firstOrNull()
                        val badgeMuro = if (publications.isNotEmpty()) "${publications.size}" else null
                        BentoGridCard(
                            titulo = "Muro Noticias",
                            subtitulo = ultimaPub?.title?.take(22) ?: "Avisos del club",
                            icono = Icons.Default.Campaign,
                            colorIcono = DashboardFondoConfig.ColorRojoCarrera,
                            colorFondoIcono = DashboardFondoConfig.ColorContenedorRojo,
                            badgeTexto = badgeMuro,
                            onClick = {
                                com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("FEED")
                                onNavigateToTab(NavigationTab.FEED)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(102.dp)
                        )

                        BentoGridCard(
                            titulo = "Chat Táctico",
                            subtitulo = if (unreadChatCount > 0) "$unreadChatCount sin leer" else "Salas en vivo",
                            icono = Icons.Default.Forum,
                            colorIcono = Color(0xFF1E88E5),
                            colorFondoIcono = DashboardFondoConfig.ColorContenedorAzul,
                            badgeTexto = if (unreadChatCount > 0) "$unreadChatCount" else null,
                            badgeColor = DashboardFondoConfig.ColorRojoCarrera,
                            onClick = {
                                com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("CHAT")
                                onNavigateToTab(NavigationTab.CHAT)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(102.dp)
                        )
                    }

                    // Bento Fila 3: Mapa TX / Radar y Velocímetro
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BentoGridCard(
                            titulo = "Mapa TX & Radar",
                            subtitulo = "Navegación GPS",
                            icono = Icons.Default.Map,
                            colorIcono = DashboardFondoConfig.ColorRojoCarrera,
                            colorFondoIcono = DashboardFondoConfig.ColorContenedorRojo,
                            badgeTexto = "Radar",
                            badgeColor = DashboardFondoConfig.ColorRojoCarrera,
                            onClick = {
                                com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("MAPA")
                                onNavigateToTab(NavigationTab.MAPA)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(102.dp)
                        )

                        BentoGridCard(
                            titulo = "Velocímetro",
                            subtitulo = "Telemetría y Km",
                            icono = Icons.Default.Speed,
                            colorIcono = Color(0xFF00ACC1),
                            colorFondoIcono = Color(0xFFE0F7FA),
                            badgeTexto = "Km",
                            badgeColor = Color(0xFF00ACC1),
                            onClick = {
                                com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("VELOCIMETRO")
                                onNavigateToTab(NavigationTab.VELOCIMETRO)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(102.dp)
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
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("FEED")
                        onNavigateToTab(NavigationTab.FEED)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Chat Táctico y Canales",
                    descripcion = "Salas multicanal, directiva, notas de voz y stickers",
                    icono = Icons.Default.Forum,
                    colorIcono = Color(0xFF1E88E5),
                    badgeCount = unreadChatCount,
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("CHAT")
                        onNavigateToTab(NavigationTab.CHAT)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Avisos y Notificaciones",
                    descripcion = "Centro de alertas importantes del club",
                    icono = Icons.Default.Notifications,
                    colorIcono = DashboardFondoConfig.ColorRojoCarrera,
                    badgeCount = notificacionesNoLeidasCount,
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("NOTIFICACIONES")
                        onNavigateToTab(NavigationTab.NOTIFICACIONES)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Ranking de Pilotos",
                    descripcion = "Puntos de reputación, calificaciones y tabla de honor",
                    icono = Icons.Default.MilitaryTech,
                    colorIcono = DashboardFondoConfig.ColorDoradoOro,
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("RANKING")
                        onNavigateToTab(NavigationTab.RANKING)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Calendario Motero",
                    descripcion = "Programación de rodadas, aniversarios y eventos",
                    icono = Icons.Default.CalendarMonth,
                    colorIcono = Color(0xFF43A047),
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("CALENDARIO")
                        onNavigateToTab(NavigationTab.CALENDARIO)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Mapa TX y Radar Offline",
                    descripcion = "Navegación vectorial, capas tácticas y ubicación en tiempo real",
                    icono = Icons.Default.Map,
                    colorIcono = DashboardFondoConfig.ColorRojoCarrera,
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("MAPA")
                        onNavigateToTab(NavigationTab.MAPA)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Rutas TX & Estudio 2D (Relive)",
                    descripcion = "Tracking GPS ininterrumpido, seguro de batería y video cinemático",
                    icono = Icons.Default.Navigation,
                    colorIcono = Color(0xFFFF6B00),
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("RUTAS")
                        onNavigateToTab(NavigationTab.RUTAS)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Velocímetro y Telemetría",
                    descripcion = "Registro de velocidad máxima, odómetro continuo y viajes",
                    icono = Icons.Default.Speed,
                    colorIcono = Color(0xFF00ACC1),
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("VELOCIMETRO")
                        onNavigateToTab(NavigationTab.VELOCIMETRO)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Reproductor Musical TX Pro",
                    descripcion = "Audio motero, podcasts, transmisiones y nube flotante",
                    icono = Icons.Default.MusicNote,
                    colorIcono = Color(0xFF8E24AA),
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("PLAYER")
                        onNavigateToTab(NavigationTab.PLAYER)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Retos y Desafíos TX",
                    descripcion = "Metas de kilometraje, rutas de montaña y logros",
                    icono = Icons.Default.EmojiEvents,
                    colorIcono = DashboardFondoConfig.ColorDoradoOro,
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("RETOS")
                        onNavigateToTab(NavigationTab.RETOS)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Rodadas y Caravanas",
                    descripcion = "Gestión de convoy, capitán de ruta, colas y bitácora",
                    icono = Icons.Default.TwoWheeler,
                    colorIcono = DashboardFondoConfig.ColorRojoCarrera,
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("RIDES")
                        onNavigateToTab(NavigationTab.RIDES)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Mercado Biker",
                    descripcion = "Compra y venta de motos, repuestos y accesorios",
                    icono = Icons.Default.ShoppingCart,
                    colorIcono = Color(0xFF3949AB),
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("MERCADO")
                        onNavigateToTab(NavigationTab.MERCADO)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Bitácora de Mantenimiento",
                    descripcion = "Control de cambios de aceite, repuestos y kilometraje",
                    icono = Icons.Default.Build,
                    colorIcono = Color(0xFF5D4037),
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("BITACORA")
                        onNavigateToTab(NavigationTab.BITACORA)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Directorio Comercial & Servicios",
                    descripcion = "Talleres, repuestos, autolavados y servicios para moteros",
                    icono = Icons.Default.Storefront,
                    colorIcono = Color(0xFF00897B),
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("DIRECTORIO")
                        onNavigateToTab(NavigationTab.DIRECTORIO)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Pasaporte Motero",
                    descripcion = "Sellado de destinos turísticos y rutas nacionales",
                    icono = Icons.Default.Explore,
                    colorIcono = Color(0xFFD81B60),
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("PASAPORTE")
                        onNavigateToTab(NavigationTab.PASAPORTE)
                    }
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
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("NORMATIVAS")
                        onNavigateToTab(NavigationTab.NORMATIVAS)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Gobernanza Directiva",
                    descripcion = "Panel exclusivo para directivos, códigos y sanciones",
                    icono = Icons.Default.Shield,
                    colorIcono = DashboardFondoConfig.ColorDoradoOro,
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("DIRECTIVA")
                        onNavigateToTab(NavigationTab.DIRECTIVA)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Carnet TX Digital",
                    descripcion = "Credencial oficial del piloto, solvencia y vinculación",
                    icono = Icons.Default.Badge,
                    colorIcono = DashboardFondoConfig.ColorRojoCarrera,
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("PROFILE")
                        onNavigateToTab(NavigationTab.PROFILE)
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Ajustes y Configuración",
                    descripcion = "Fondos del dashboard, sonidos, tema y gestión de cuenta",
                    icono = Icons.Default.Settings,
                    colorIcono = Color(0xFF546E7A),
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("CONFIGURACIONES")
                        onNavigateToTab(NavigationTab.CONFIGURACIONES)
                    }
                )
            }

            item {
                val esAdminODev = isDirectivaMode ||
                    currentMember?.role?.canManageApp == true ||
                    currentMember?.role == MemberRole.DESARROLLADOR ||
                    currentMember?.role == MemberRole.PRESIDENTE ||
                    currentMember?.role == MemberRole.VICEPRESIDENTE ||
                    currentMember?.role == MemberRole.DIRECTIVA ||
                    currentMember?.role == MemberRole.SECRETARIO ||
                    currentMember?.role == MemberRole.DISCIPLINARIO ||
                    currentMember?.role == MemberRole.TESORERO ||
                    currentMember?.isDirectiva == true

                ModuleListRow(
                    titulo = "Mesh TX (Intercom Táctico)",
                    descripcion = if (esAdminODev) "Intercomunicador offline 3.0 para caravana y convoy" else "🔒 Exclusivo Directiva y Desarrolladores (Fase Beta)",
                    icono = Icons.Default.Podcasts,
                    colorIcono = if (esAdminODev) Color(0xFFFF6B00) else Color(0xFF94A3B8),
                    badgeAlerta = !esAdminODev,
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("MESHTX")
                        if (esAdminODev) {
                            onNavigateToTab(NavigationTab.MESHTX)
                        } else {
                            mostrarDialogoMeshBloqueado = true
                        }
                    }
                )
            }

            item {
                ModuleListRow(
                    titulo = "Redes Sociales & Canales TX",
                    descripcion = "Canales oficiales WhatsApp, TikTok, Telegram, Instagram y Linktree",
                    icono = Icons.Default.Share,
                    colorIcono = Color(0xFF0284C7),
                    onClick = {
                        com.example.ui.preferences.PreferenciasApp.registrarUsoModulo("REDES")
                        onNavigateToTab(NavigationTab.REDES)
                    }
                )
            }
        }
    }

    // Diálogo informativo para usuarios sin rol de directiva/desarrollador
    if (mostrarDialogoMeshBloqueado) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoMeshBloqueado = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFFF6B00),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "🔒 Módulo Táctico Mesh TX",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "El Intercomunicador Offline Mesh 3.0 se encuentra en fase de pruebas activas de radiofrecuencia (Wi-Fi Aware y BLE) para caravanas. El acceso operativo está restringido temporalmente a miembros de la Directiva y Desarrolladores.",
                    fontSize = 12.sp,
                    color = Color(0xFF334155),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { mostrarDialogoMeshBloqueado = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))
                ) {
                    Text("Entendido", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White
        )
    }
}
