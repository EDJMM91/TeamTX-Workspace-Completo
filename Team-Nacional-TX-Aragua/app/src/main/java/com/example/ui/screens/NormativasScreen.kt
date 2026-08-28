package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MotoOrangePrimary

enum class NormativaView { MAIN, NACIONAL, ARAGUA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormativasScreen() {
    var currentView by remember { mutableStateOf(NormativaView.MAIN) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (currentView != NormativaView.MAIN) {
            TopAppBar(
                title = { Text(if (currentView == NormativaView.NACIONAL) "Normativa Nacional" else "Normativa Aragua", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { currentView = NormativaView.MAIN }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        } else {
            TopAppBar(
                title = { Text("Normativas y Reglamentos", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        Crossfade(targetState = currentView, label = "normativas_view") { view ->
            when (view) {
                NormativaView.MAIN -> NormativasMainView(
                    onNacionalClick = { currentView = NormativaView.NACIONAL },
                    onAraguaClick = { currentView = NormativaView.ARAGUA }
                )
                NormativaView.NACIONAL -> NormativaNacionalScreen()
                NormativaView.ARAGUA -> NormativaAraguaView()
            }
        }
    }
}

@Composable
fun NormativasMainView(onNacionalClick: () -> Unit, onAraguaClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Seleccione el documento que desea consultar:",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp
        )

        NormativaCard(
            title = "Normativa Nacional TX 200",
            subtitle = "Reglas, estructura y lineamientos generales a nivel nacional.",
            icon = Icons.Default.Public,
            onClick = onNacionalClick
        )

        NormativaCard(
            title = "Normativa Capítulo Aragua",
            subtitle = "Reglamento interno específico del estado Aragua. (Pendiente de cargar)",
            icon = Icons.Default.Map,
            onClick = onAraguaClick
        )
    }
}

@Composable
fun NormativaCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MotoOrangePrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = MotoOrangePrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

