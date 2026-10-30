package com.rohit.spendsense

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohit.spendsense.ui.theme.BrandBlue
import com.rohit.spendsense.ui.theme.BrandCyan
import com.rohit.spendsense.ui.theme.BrandPurple
import java.util.Locale

@Composable
fun HeroCard(summary: DashboardSummary) {
    val usage = summary.budgetUsage
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(BrandPurple, BrandBlue)))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Your Expenses",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = "This Month",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        Text(
            text = formatRupees(summary.totalExpenses),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        LinearProgressIndicator(
            progress = { (usage / 100).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.3f)
        )
        Text(
            text = "Budget usage: ${String.format(Locale.US, "%.0f", usage)}%",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Budget: ${formatRupees(summary.monthlyBudget)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Text(
                text = "Remaining: ${formatRupees(summary.remainingBudget)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Composable
fun WeeklyBarChart(bars: List<WeekBar>, modifier: Modifier = Modifier) {
    val maxAmount = bars.maxOfOrNull { it.amount } ?: 0.0
    val barBrush = Brush.verticalGradient(listOf(BrandCyan, BrandPurple))
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val slot = size.width / bars.size
            val barWidth = slot * 0.5f
            bars.forEachIndexed { index, bar ->
                val fraction = if (maxAmount > 0) (bar.amount / maxAmount).toFloat() else 0f
                val barHeight = maxOf(fraction * size.height, size.height * 0.05f)
                drawRoundRect(
                    brush = if (bar.amount > 0) barBrush else SolidColor(emptyColor),
                    topLeft = Offset(
                        index * slot + (slot - barWidth) / 2f,
                        size.height - barHeight
                    ),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            bars.forEach { bar ->
                Text(
                    text = bar.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (maxAmount == 0.0) {
            Text(
                text = "No spending in the last 7 days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SmallStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    InfoCard(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}