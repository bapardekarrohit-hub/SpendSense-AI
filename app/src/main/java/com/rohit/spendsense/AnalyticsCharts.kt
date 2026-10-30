package com.rohit.spendsense

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.util.Locale

val chartColors = listOf(
    Color(0xFF8B5CF6), Color(0xFF38BDF8), Color(0xFF22C55E), Color(0xFFF59E0B),
    Color(0xFFEF4444), Color(0xFFEC4899), Color(0xFF14B8A6), Color(0xFF6366F1),
    Color(0xFFA3E635), Color(0xFF94A3B8)
)

@Composable
fun DonutChart(slices: List<Pair<Category, Double>>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.second }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 900))
    }

    Box(modifier = modifier.size(180.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.16f
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            var start = -90f
            slices.forEachIndexed { index, slice ->
                val full = if (total > 0) (slice.second / total * 360.0).toFloat() else 0f
                val sweep = full * progress.value
                drawArc(
                    color = chartColors[index % chartColors.size],
                    startAngle = start,
                    sweepAngle = maxOf(sweep - 1.5f, 0f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Total spent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatRupees(total),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CategoryLegend(slices: List<Pair<Category, Double>>) {
    val total = slices.sumOf { it.second }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slices.forEachIndexed { index, slice ->
            val share = if (total > 0) slice.second / total * 100 else 0.0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(chartColors[index % chartColors.size])
                    )
                    Text(
                        text = slice.first.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${formatRupees(slice.second)} · ${String.format(Locale.US, "%.0f", share)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun IncomeExpenseBars(income: Double, expenses: Double) {
    val maxValue = maxOf(income, expenses, 1.0)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProgressBarRow("Income", formatRupees(income), (income / maxValue).toFloat(), Color(0xFF16A34A))
        ProgressBarRow("Expenses", formatRupees(expenses), (expenses / maxValue).toFloat(), Color(0xFFDC2626))
    }
}

@Composable
fun PaymentMethodBars(items: List<Pair<String, Double>>) {
    val total = items.sumOf { it.second }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEachIndexed { index, item ->
            val share = if (total > 0) item.second / total else 0.0
            ProgressBarRow(
                label = item.first,
                valueText = "${formatRupees(item.second)} · ${String.format(Locale.US, "%.0f", share * 100)}%",
                fraction = share.toFloat(),
                color = chartColors[index % chartColors.size]
            )
        }
    }
}

@Composable
fun ProgressBarRow(label: String, valueText: String, fraction: Float, color: Color) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val animated by animateFloatAsState(
        targetValue = if (started) fraction.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "bar"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color)))
            )
        }
    }
}