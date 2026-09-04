package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MemberProfile
import com.example.ui.screens.calculateMemberMeritPoints
import com.example.ui.screens.getMemberHonorRank
import com.example.ui.theme.*

data class RatingOptionItem(
    val id: String,
    val title: String,
    val description: String,
    val pointsDelta: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isPositive: Boolean
)

val POSITIVE_RATING_OPTIONS = listOf(
    RatingOptionItem(
        id = "PUNTUALIDAD",
        title = "Puntualidad en Rodadas",
        description = "Llega a tiempo y respeta los horarios oficiales de salida.",
        pointsDelta = 15,
        icon = Icons.Default.Schedule,
        isPositive = true
    ),
    RatingOptionItem(
        id = "MANEJO_SEGURO",
        title = "Manejo Seguro y Responsable",
        description = "Mantiene distancias de seguridad y respeta la formación de caravana.",
        pointsDelta = 20,
        icon = Icons.Default.Shield,
        isPositive = true
    ),
    RatingOptionItem(
        id = "AUXILIO_VIAL",
        title = "Compañerismo y Auxilio en Ruta",
        description = "Apoyo solidario y asistencia mecánica o vial a otros hermanos.",
        pointsDelta = 25,
        icon = Icons.Default.Handshake,
        isPositive = true
    ),
    RatingOptionItem(
        id = "HERMANDAD_TX",
        title = "Espíritu de Hermandad TX",
        description = "Fomenta la unión, el respeto mutuo y los valores del club.",
        pointsDelta = 15,
        icon = Icons.Default.Favorite,
        isPositive = true
    ),
    RatingOptionItem(
        id = "CONDUCCION_GRUPO",
        title = "Conducción Ejemplar en Grupo",
        description = "Excelente técnica de manejo, señalización de baches y liderazgo en ruta.",
        pointsDelta = 20,
        icon = Icons.Default.TwoWheeler,
        isPositive = true
    ),
    RatingOptionItem(
        id = "LIKE_GENERAL",
        title = "👍 Buen Piloto (Voto General)",
        description = "Reconocimiento general de aprecio y buen desempeño como motero.",
        pointsDelta = 10,
        icon = Icons.Default.ThumbUp,
        isPositive = true
    )
)

val NEGATIVE_RATING_OPTIONS = listOf(
    RatingOptionItem(
        id = "MANEJO_TEMERARIO",
        title = "Manejo Temerario / Imprudente",
        description = "Adelantamientos indebidos en curvas ciegas o romper formación sin aviso.",
        pointsDelta = -20,
        icon = Icons.Default.Warning,
        isPositive = false
    ),
    RatingOptionItem(
        id = "IMPUNTUALIDAD",
        title = "Impuntualidad Reiterada",
        description = "Retrasos constantes e injustificados que afectan la salida de la caravana.",
        pointsDelta = -10,
        icon = Icons.Default.HourglassBottom,
        isPositive = false
    ),
    RatingOptionItem(
        id = "FALTA_RESPETO",
        title = "Falta de Respeto o Convivencia",
        description = "Conducta inadecuada o contraria a las normas de hermandad del club.",
        pointsDelta = -25,
        icon = Icons.Default.ThumbDown,
        isPositive = false
    ),
    RatingOptionItem(
        id = "DISLIKE_GENERAL",
        title = "👎 Manito Abajo (Desempeño Deficiente)",
        description = "Falta de compromiso o llamado de atención por manejo en carretera.",
        pointsDelta = -10,
        icon = Icons.Default.SentimentDissatisfied,
        isPositive = false
    )
)

/**
 * Diálogo interactivo para calificar el desempeño de un piloto.
 * Permite otorgar likes positivos o manitos abajo que suman o restan puntos de mérito en el Ranking.
 */
@Composable
fun RatePilotDialog(
    targetMember: MemberProfile,
    currentMember: MemberProfile?,
    onDismiss: () -> Unit,
    onConfirmRating: (isPositive: Boolean, category: String, pointsDelta: Int, comment: String) -> Unit
) {
    var isPositiveTab by remember { mutableStateOf(true) }
    val currentOptions = if (isPositiveTab) POSITIVE_RATING_OPTIONS else NEGATIVE_RATING_OPTIONS
    var selectedOption by remember { mutableStateOf(currentOptions.first()) }
    var commentText by remember { mutableStateOf("") }

    // Actualizar opción seleccionada al cambiar de pestaña
    LaunchedEffect(isPositiveTab) {
        selectedOption = if (isPositiveTab) POSITIVE_RATING_OPTIONS.first() else NEGATIVE_RATING_OPTIONS.first()
    }

    val currentMerit = remember(targetMember) { calculateMemberMeritPoints(targetMember) }
    val projectedMerit = remember(currentMerit, selectedOption) {
        (currentMerit + selectedOption.pointsDelta).coerceAtLeast(0)
    }
    val currentRank = remember(currentMerit) { getMemberHonorRank(currentMerit) }
    val projectedRank = remember(projectedMerit) { getMemberHonorRank(projectedMerit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Stars,
                    contentDescription = null,
                    tint = if (isPositiveTab) Color(0xFF16A34A) else TxFlameRed
                )
                Text(
                    text = "CALIFICAR PILOTO TX",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Tarjeta de Piloto Seleccionado (Tema Claro) ───────────────
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Foto de perfil
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0))
                                .border(1.5.dp, currentRank.badgeColor, CircleShape)
                        ) {
                            if (!targetMember.profilePhotoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = targetMember.profilePhotoUri,
                                    contentDescription = targetMember.fullName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = targetMember.fullName.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = targetMember.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (targetMember.nickname.isNotBlank()) "\"${targetMember.nickname}\"" else targetMember.rankingTitle,
                                fontSize = 11.sp,
                                color = currentRank.badgeColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "👍 ${targetMember.positiveRatingsCount}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "👎 ${targetMember.negativeRatingsCount}",
                                    fontSize = 10.sp,
                                    color = TxFlameRed,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "•",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "$currentMerit PTS",
                                    fontSize = 10.sp,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // ── Selector de Pestañas: Positivo vs Negativo ─────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { isPositiveTab = true },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPositiveTab) Color(0xFFE8F5E9) else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            1.dp,
                            if (isPositiveTab) Color(0xFF22C55E) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Default.ThumbUp,
                                contentDescription = null,
                                tint = if (isPositiveTab) Color(0xFF1B5E20) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "👍 POSITIVO",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = if (isPositiveTab) Color(0xFF1B5E20) else Color(0xFF64748B)
                            )
                        }
                    }

                    Surface(
                        onClick = { isPositiveTab = false },
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isPositiveTab) Color(0xFFFEE2E2) else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            1.dp,
                            if (!isPositiveTab) TxFlameRed else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Default.ThumbDown,
                                contentDescription = null,
                                tint = if (!isPositiveTab) TxFlameRed else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "👎 NEGATIVO",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = if (!isPositiveTab) TxFlameRed else Color(0xFF64748B)
                            )
                        }
                    }
                }

                // ── Lista de Motivos / Categorías ───────────────────────────
                Text(
                    text = if (isPositiveTab) "SELECCIONA ASPECTO DESTACADO (+PUNTOS):" else "SELECCIONA MOTIVO O LLAMADO DE ATENCIÓN (-PUNTOS):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (isPositiveTab) Color(0xFF15803D) else TxFlameRed,
                    letterSpacing = 0.5.sp
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(currentOptions) { opt ->
                        val isSelected = opt.id == selectedOption.id
                        Surface(
                            onClick = { selectedOption = opt },
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isSelected && opt.isPositive -> Color(0xFFDCFCE7)
                                isSelected && !opt.isPositive -> Color(0xFFFEE2E2)
                                else -> Color(0xFFF8FAFC)
                            },
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                when {
                                    isSelected && opt.isPositive -> Color(0xFF22C55E)
                                    isSelected && !opt.isPositive -> TxFlameRed
                                    else -> Color(0xFFE2E8F0)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    opt.icon,
                                    contentDescription = null,
                                    tint = if (opt.isPositive) Color(0xFF16A34A) else TxFlameRed,
                                    modifier = Modifier.size(20.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = opt.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = opt.description,
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B),
                                        lineHeight = 13.sp
                                    )
                                }

                                Surface(
                                    color = if (opt.isPositive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (opt.isPositive) Color(0xFF22C55E) else TxFlameRed
                                    )
                                ) {
                                    Text(
                                        text = if (opt.pointsDelta > 0) "+${opt.pointsDelta} PTS" else "${opt.pointsDelta} PTS",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = if (opt.isPositive) Color(0xFF15803D) else TxFlameRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Campo de Comentario Opcional ─────────────────────────────
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Nota o comentario adicional (opcional)...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isPositiveTab) Color(0xFF16A34A) else TxFlameRed,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    )
                )

                // ── Impacto en el Ranking ────────────────────────────────────
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Impacto en Ranking:",
                            fontSize = 10.sp,
                            color = Color(0xFF475569)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$currentMerit PTS",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = if (selectedOption.pointsDelta >= 0) Color(0xFF16A34A) else TxFlameRed,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$projectedMerit PTS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selectedOption.pointsDelta >= 0) Color(0xFF15803D) else TxFlameRed
                            )
                            Text(
                                text = "(${projectedRank.title})",
                                fontSize = 9.sp,
                                color = projectedRank.badgeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmRating(
                        selectedOption.isPositive,
                        selectedOption.title,
                        selectedOption.pointsDelta,
                        commentText.trim()
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPositiveTab) Color(0xFF16A34A) else TxFlameRed
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    if (isPositiveTab) Icons.Default.ThumbUp else Icons.Default.ThumbDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPositiveTab) "Confirmar (+${selectedOption.pointsDelta} PTS)" else "Confirmar (${selectedOption.pointsDelta} PTS)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF64748B))
            }
        }
    )
}
