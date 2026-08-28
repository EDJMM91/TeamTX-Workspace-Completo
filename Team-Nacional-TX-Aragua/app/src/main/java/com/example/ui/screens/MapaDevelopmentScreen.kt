package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MapaDevelopmentScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Construction,
            contentDescription = "Módulo en desarrollo",
            modifier = Modifier.size(100.dp),
            tint = Color(0xFFFF9800) // Orange
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Módulo en Desarrollo",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "El motor del mapa OsmAnd se integrará próximamente en este módulo. Actualmente, el repositorio se descargará y funcionará como una librería de Android nativa.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = Color.LightGray
        )
    }
}
