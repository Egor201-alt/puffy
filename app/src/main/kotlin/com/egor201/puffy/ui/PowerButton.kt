package com.egor201.puffy.ui

// Power button — glow ring, shield/power icon, pulse while connected
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.egor201.puffy.ui.theme.CardBorder
import com.egor201.puffy.ui.theme.Cyan
import com.egor201.puffy.ui.theme.CyanSoft
import com.egor201.puffy.ui.theme.powerRingGradient

@Composable
fun PowerButton(isConnected: Boolean, isConnecting: Boolean, onClick: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnecting) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(if (isConnected || isConnecting) pulse else 1f)
                .clip(CircleShape)
                .background(powerRingGradient(isConnected || isConnecting))
        )

        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .border(2.dp, if (isConnected) Cyan else CardBorder, CircleShape)
                .background(Color.Black.copy(alpha = 0.25f))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.Security else Icons.Default.PowerSettingsNew,
                contentDescription = "Toggle connection",
                tint = if (isConnected) CyanSoft else CardBorder,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}
