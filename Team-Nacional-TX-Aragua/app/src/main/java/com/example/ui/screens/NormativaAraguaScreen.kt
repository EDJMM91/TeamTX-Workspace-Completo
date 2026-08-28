package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MotoOrangePrimary

@Composable
fun NormativaAraguaView() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Estructura y Normas",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Capítulo Aragua (Periodo 2025-2026)",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            SectionCard(
                title = "Estructura del Capítulo",
                icon = Icons.Default.AccountTree
            ) {
                RuleItem("La lista de integrantes de la Directiva se gestiona de forma dinámica.")
                RuleItem("Los roles (Presidente, Vicepresidente, etc.) son asignados y editables exclusivamente por el Presidente Nacional desde el módulo de Gobernanza (Carnet TX).")
            }
        }

        item {
            SectionCard(
                title = "Funciones Adicionales",
                icon = Icons.Default.Assignment
            ) {
                RuleItem("Disciplinarios: Establecen las normas de comportamiento y las consecuencias por su incumplimiento para mantener armonía, seguridad y respeto.", isBold = true)
                RuleItem("Enlaces: Mantienen vínculos con entidades públicas, privadas y comunitarias para facilitar acceso a espacios y coordinar permisos.", isBold = true)
            }
        }

        item {
            SectionCard(
                title = "Conceptos Claves",
                icon = Icons.Default.MenuBook
            ) {
                RuleItem("Copiloto oficial: Quien cumplido los requisitos exigidos a su piloto acompañante y por decisión del mismo se le es entregado el Jersey (esposo/a, novio/a, hermano/a, hijo/a).")
                RuleItem("Prospecto: Aspirante a piloto oficial.")
                RuleItem("Barredores: Pilotos encargados de mantener el orden en una rodada, y los únicos con el poder de salir de la fila.")
                RuleItem("Auditoria disciplinaria: Proceso en el que un piloto o copiloto es llamado a reunión para debatir su situación.")
                RuleItem("Membresía: Aporte económico semanal exigido a los pilotos oficiales.")
            }
        }

        item {
            SectionCard(
                title = "Normativa Interna",
                icon = Icons.Default.Gavel
            ) {
                RuleItem("1. Prohibido el juego de manos entre integrantes (lepes, golpes jugando, empujones).")
                RuleItem("2. Prohibido dirigirse a otros con groserías, tono alterado o actitud agresiva.")
                RuleItem("3. Obligatorio en jueves moteros y rodadas oficiales el uso del casco, pantalón largo o mono, y zapatos cerrados.")
                RuleItem("4. Las rodadas fuera de la ciudad tendrán directrices específicas que deben ser cumplidas a cabalidad.")
                RuleItem("5. Al estacionar dos o más motos, deben quedar alineadas en posición de salida con el mismo frente.")
                RuleItem("6. Mientras se use el jersey oficial, está terminantemente prohibido hacer piruetas o infringir las leyes de tránsito.")
                RuleItem("7. Prohibido conducir bajo los efectos del alcohol o sustancias psicotrópicas.")
                RuleItem("8. Prohibido adelantar dentro del grupo sin causa justificada. Las señales del capitán de ruta y barredores deben acatarse.")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Nota: El incumplimiento conlleva suspensión de 4 semanas sin participación y expulsión temporal de los grupos.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            SectionCard(
                title = "Auditoría Disciplinaria Inmediata",
                icon = Icons.Default.Warning
            ) {
                RuleItem("1. Cometer agresiones físicas entre integrantes.")
                RuleItem("2. Alterar componentes mecánicos o eléctricos de la moto de algún integrante.")
                RuleItem("3. Faltar el respeto al copiloto o acompañante de otro miembro.")
                RuleItem("4. Involucrarse sentimental/sexualmente con el/la copiloto de otro integrante.")
                RuleItem("5. Hablar mal del Team nacional (cualquier capítulo). Las quejas se canalizan con el vocero.")
                RuleItem("6. Manipular o interferir con las finanzas del team.")
            }
        }

        item {
            SectionCard(
                title = "Requisitos para Prospecto (Ascenso a General)",
                icon = Icons.Default.TrendingUp
            ) {
                RuleItem("1. Ser propietario de una Moto Tx Marca EK (TX150, TX200, TX250).")
                RuleItem("2. Tener toda la documentación vigente.")
                RuleItem("3. Participar en jueves moteros durante 4 meses.")
                RuleItem("4. Participar en una rodada larga oficial, de al menos 2 estados de distancia.")
                RuleItem("5. Participar en dos actividades de envergadura (Evento Motero y Actividad Benéfica).")
                RuleItem("6. Mostrar actitud positiva hacia el Team.")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Logrados los requisitos: Formalizar pago de membresía, adquirir jersey oficial, proporcionar datos y disfrutar beneficios.", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            SectionCard(
                title = "Aporte Voluntario (Anteriormente Membresía)",
                icon = Icons.Default.AccountBalanceWallet
            ) {
                RuleItem("Costo sugerido: \$1 Semanal o su equivalente al BCV, 100% Voluntario.", isBold = true)
                RuleItem("Los fondos recaudados se destinan a proyectos, eventualidades y objetivos comunes, bajo administración del tesorero.")
                RuleItem("No tendrá como finalidad cubrir gastos personales de pilotos ni realizar préstamos o financiamientos.")
                RuleItem("Al ser voluntario, no genera deuda acumulable ni suspensión por falta de pago.")
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
