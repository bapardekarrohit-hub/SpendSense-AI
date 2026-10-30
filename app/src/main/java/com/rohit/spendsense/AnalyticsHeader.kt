package com.rohit.spendsense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohit.spendsense.ui.theme.BrandBlue
import com.rohit.spendsense.ui.theme.BrandPurple
import java.util.Locale

@Composable
fun AnalyticsHero(stats: AnalyticsStats, period: Period) {
    val change = stats.changePercent
    val changeText = if (change == null) {
        "No spending in the previous ${stats.daysInRange} days to compare with."
    } else {
        val arrow = if (change >= 0) "↑" else "↓"
        "$arrow ${String.format(Locale.US, "%.0f", Math.abs(change))}% spending vs previous ${stats.daysInRange} days"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(BrandPurple, BrandBlue)))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Net balance · ${period.label}",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.85f)
        )
        Text(
            text = formatRupees(stats.net),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroChip("Income", formatRupees(stats.income), Modifier.weight(1f))
            HeroChip("Expenses", formatRupees(stats.expenses), Modifier.weight(1f))
        }
        Text(
            text = changeText,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun HeroChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
fun KpiRow(stats: AnalyticsStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        KpiCard("Avg / day", formatRupees(stats.avgPerDay), Modifier.weight(1f))
        KpiCard("Expenses", stats.expenseCount.toString(), Modifier.weight(1f))
        KpiCard(
            "Saved",
            String.format(Locale.US, "%.0f%%", stats.savingsRate),
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun KpiCard(title: String, value: String, modifier: Modifier = Modifier) {
    InfoCard(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun InsightsCard(insights: List<String>) {
    InfoCard {
        Text(
            text = "Smart insights",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        insights.forEach { line ->
            Text(
                text = "•  $line",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "Calculated from your transactions in this period.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BiggestExpensesCard(items: List<Transaction>) {
    InfoCard {
        Text(
            text = "Biggest expenses",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(BrandPurple, BrandBlue))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (index + 1).toString(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.category.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatRupees(item.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFDC2626)
                )
            }
        }
    }
}