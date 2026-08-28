package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MotoOrangePrimary

@Composable
fun NormativaNacionalScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Estructura y Organización",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            SectionCard(
                title = "Directiva del Team",
                icon = Icons.Default.AccountTree
            ) {
                RuleItem("Presidente")
                RuleItem("Vicepresidente")
                RuleItem("Secretario (a)")
                RuleItem("Tesorero (a)")
                RuleItem("Vocero (a)")
            }
        }

        item {
            SectionCard(
                title = "Funciones de la Directiva",
                icon = Icons.Default.Assignment
            ) {
                RuleItem("PRESIDENTE: Hacer cumplir las normativas estipuladas.", isBold = true)
                RuleItem("VICEPRESIDENTE: Apoyo en rodadas, actividades y representación en ausencia del presidente.", isBold = true)
                RuleItem("SECRETARIO(A): Organizar y plasmar informaciones pautadas en cada reunión, actividad y evidencias para redes sociales.", isBold = true)
                RuleItem("TESORERO(A): Administrar ingresos y egresos del Team Nacional TX 200 Venezuela (Capítulo que no cobre membresía no amerita tesorero).", isBold = true)
                RuleItem("VOCERO(A): Es quien se encarga de hacer saber las incomodidades, preguntas o sugerencias de los integrantes y también quien le da fuerza a la información que da la directiva.", isBold = true)
            }
        }

        item {
            SectionCard(
                title = "Condiciones para el Ingreso (Chat Filtro)",
                icon = Icons.Default.PersonAdd
            ) {
                Text("El líder debe tener en cuenta para el ingreso de un nuevo integrante:", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                RuleItem("Observación")
                RuleItem("Comportamiento")
                RuleItem("Conocimientos de señalizaciones")
                RuleItem("Manejo")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tiempo de evaluación: Dos reuniones y dos rodadas mínimas antes de pasar al Chat Oficial.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MotoOrangePrimary)
            }
        }

        item {
            SectionCard(
                title = "Comportamiento de un Buen Líder",
                icon = Icons.Default.Star
            ) {
                RuleItem("1. Empatía con todos los integrantes.")
                RuleItem("2. Ser prudente a la hora de dirigirse a los integrantes.")
                RuleItem("3. Respetar a los integrantes del team.")
                RuleItem("4. Tomar en cuenta las opiniones de los integrantes para beneficio del team.")
                RuleItem("5. Oración antes de cada rodada.")
                RuleItem("6. Incentivar y motivar de la mejor manera dando el ejemplo.")
                RuleItem("7. Realizar llamado de atención de buena manera al momento de una falta en caravanas, rutas y actividades.")
                RuleItem("8. Analizarse constantemente, verificar su manera de actuar, decisiones y actitudes para bienestar del team.")
                RuleItem("9. Tomar en cuenta el apoyo de la directiva para solucionar problemas.")
            }
        }

        item {
            SectionCard(
                title = "Reglas para Dirigir una Rodada",
                icon = Icons.Default.TwoWheeler
            ) {
                RuleItem("1. El Líder debe colocar un Capitán de Ruta y recordar señalizaciones, distanciamientos y velocidad antes de iniciar.")
                RuleItem("2. Antes de cada rodada, informar a sus integrantes las paradas y destinos correspondientes.")
                RuleItem("3. El Capitán de Ruta debe tener conocimiento del destino, ubicación de cada participante y establece la velocidad crucero.")
                RuleItem("4. El Capitán de Cola debe ser quien tenga la moto más rápida. A la hora de un accidente avisar al Capitán de Ruta para detener el pelotón. El Capitán de Cola se dirige al accidentado quedando el resto del team con el Capitán de Ruta.")
                Text("Nota: El líder es quien elige el capitán de ruta y el capitán de cola.", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            SectionCard(
                title = "Normativa (Dirigidas al Líder)",
                icon = Icons.Default.Gavel
            ) {
                RuleItem("01. Período de cargo del Presidente y Vicepresidente es de 1 AÑO, con derecho a reelección.")
                RuleItem("02. Presidente y Vicepresidente deben estar en todos los chats.")
                RuleItem("03. Los líderes pueden notificar su entrega de cargo y pasar a otro integrante que se comprometa (1 año).")
                RuleItem("04. Realizar calendario en cada capítulo de cumpleañeros y rutas.")
                RuleItem("05. Cada capítulo debe mostrar evidencia de que está desarrollando vida biker en su estado.")
                RuleItem("06. Realizar labores sociales y pasar evidencias.")
                RuleItem("07. El líder debe presentar obligatoriamente su team ante cada capítulo visitante y al Presidente Fundador.")
                RuleItem("08. Recibimiento de los capítulos visitantes (Hospedaje, refrigerio, entre otros).")
                RuleItem("09. Por cada integrante que sea expulsado se pasará un comunicado.")
            }
        }

        item {
            SectionCard(
                title = "Normativa (Dirigidas a Líder e Integrantes)",
                icon = Icons.Default.Groups
            ) {
                RuleItem("01. Ser titular de Moto TX.")
                RuleItem("02. Usar el jersey en reuniones, actividades, rodadas, labores sociales o cuando el presidente lo pida.")
                RuleItem("03. El diseño de la jersey no puede ser modificada.")
                RuleItem("04. El piloto no debe usar la jersey en rodadas o caravanas si su acompañante es un infante.")
                RuleItem("05. Documentación de la moto al día y ser mayor de edad. La directiva debe tener acceso a copias de documentación.")
                RuleItem("06. Uso del casco obligatorio (NO AL SANDOVAL).")
                RuleItem("07. Límite de dos personas por moto obligatorio.")
                RuleItem("08. Respetar señales de tránsito.")
                RuleItem("09. Jueves Moteros.")
                RuleItem("10. Cualquier integrante que realice mal negocio no podrá vincular al líder o team en dicha situación.")
                RuleItem("11. Se podrá realizar revocatorio antes del periodo estipulado si el líder pierde dominio y la mayoría está en desacuerdo.")
                RuleItem("12. El fundador (Richard Herrera) no tiene relevo o destitución de su cargo en el Team.")
                RuleItem("13. Participar al líder asistencia o presencia de invitados a cualquier actividad o rodada.")
                RuleItem("14. Cada capítulo puede celebrar su aniversario y debe asistir a los dos encuentros nacionales (13 de Febrero y Septiembre).")
                RuleItem("15. Ningún presidente de otra agrupación puede ser parte de la directiva del team.")
                RuleItem("16. Mensualmente se debe realizar charla y discusión socializada reforzando conocimientos.")
            }
        }

        item {
            SectionCard(
                title = "Misión y Visión",
                icon = Icons.Default.Visibility
            ) {
                Text("Misión:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Fomentar de manera nacional la cultura Biker respetando los principios fundamentales (honor, respeto y lealtad), crear hermandad entre pilotos TX a nivel nacional para eventos sociales, culturales, recreativos y beneficios para la población.", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Visión:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Ser una organización biker respetuosos de las leyes de tránsito, reconocida a nivel nacional e internacional, fomentando la culturalización en la conducción, logrando disminución de accidentes y uso adecuado de vehículos de dos ruedas.", fontSize = 14.sp)
            }
        }

        item {
            SectionCard(
                title = "Código Biker",
                icon = Icons.Default.Shield
            ) {
                Text(
                    text = "HONOR, LEALTAD, RESPETO Y HUMILDAD",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MotoOrangePrimary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun RuleItem(text: String, isBold: Boolean = false) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MotoOrangePrimary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 14.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
    }
}
