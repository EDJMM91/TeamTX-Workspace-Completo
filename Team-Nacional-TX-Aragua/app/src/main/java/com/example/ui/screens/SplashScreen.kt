package com.example.ui.screens

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.aistudio.teamtxvzla.R
import com.example.ui.theme.MotoOrangePrimary

@Composable
fun SplashScreen(
    onComplete: () -> Unit
) {
    val animationSpec: AnimationSpec<Float> = tween(durationMillis = 1500, delayMillis = 300)
    val fadeInSpec: AnimationSpec<Float> = tween(durationMillis = 1000, delayMillis = 1800)
    val slideUpSpec: AnimationSpec<Float> = tween(durationMillis = 800, delayMillis = 2200)
    
    val scale by animateFloatAsState(targetValue = 1f, animationSpec = animationSpec)
    val rotation by animateFloatAsState(targetValue = 0f, animationSpec = tween(durationMillis = 1200, delayMillis = 300))
    val logoAlpha by animateFloatAsState(targetValue = 1f, animationSpec = fadeInSpec)
    val textAlpha by animateFloatAsState(targetValue = 1f, animationSpec = fadeInSpec)
    val textSlide by animateFloatAsState(targetValue = 0f, animationSpec = slideUpSpec)
    val completeAlpha by animateFloatAsState(targetValue = 0f, animationSpec = tween(durationMillis = 500, delayMillis = 3200))

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3700)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.rotationZ = rotation * 15f
                        alpha = logoAlpha
                    }
            ) {
                androidx.compose.ui.res.painterResource(id = R.drawable.logoteam)
                    .let { painter ->
                        androidx.compose.foundation.Image(
                            painter = painter,
                            contentDescription = "Team Nacional TX Venezuela Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(androidx.compose.foundation.shape.CircleShape)
                        )
                    }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "by Eduardo Marquez",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MotoOrangePrimary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = textAlpha
                        translationY = textSlide * 30f
                    }
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.material3.CircularProgressIndicator(
                color = MotoOrangePrimary,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { alpha = textAlpha * 0.7f }
            )
        }
    }
}