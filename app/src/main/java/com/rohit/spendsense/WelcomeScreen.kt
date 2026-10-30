package com.rohit.spendsense

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohit.spendsense.ui.theme.BrandBlue
import com.rohit.spendsense.ui.theme.BrandCyan
import com.rohit.spendsense.ui.theme.BrandPurple

@Composable
fun AppLogo(logoSize: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(logoSize)) {
        val s = this.size.minDimension
        drawRoundRect(
            brush = Brush.linearGradient(listOf(BrandPurple, BrandBlue)),
            cornerRadius = CornerRadius(s * 0.28f, s * 0.28f)
        )
        val barWidth = s * 0.13f
        val gap = s * 0.08f
        val baseY = s * 0.72f
        val startX = (s - (barWidth * 3 + gap * 2)) / 2f
        val heights = listOf(0.22f, 0.36f, 0.50f)
        heights.forEachIndexed { index, h ->
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(startX + index * (barWidth + gap), baseY - s * h),
                size = Size(barWidth, s * h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
        drawCircle(
            color = BrandCyan,
            radius = s * 0.07f,
            center = Offset(s * 0.74f, s * 0.24f)
        )
    }
}

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0F2C), Color(0xFF2A1B6B), Color(0xFF0E3A8A))
                )
            )
            .systemBarsPadding()
            .padding(horizontal = 28.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppLogo(logoSize = 128.dp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SpendSense AI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Track Your Spending.\nControl Your Money.",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Track expenses and understand your money.",
                fontSize = 14.sp,
                color = Color(0xFFB8C0E8),
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(BrandPurple, BrandBlue)))
                    .clickable { onGetStarted() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Skip", color = Color(0xFFB8C0E8))
            }
        }
    }
}