package com.rohit.spendsense

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun TrendLineChart(points: List<TrendPoint>, modifier: Modifier = Modifier) {
    val maxAmount = points.maxOfOrNull { it.amount } ?: 0.0
    val average = if (points.isNotEmpty()) points.sumOf { it.amount } / points.size else 0.0
    val lineColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    val step = maxOf(1, Math.ceil(points.size / 6.0).toInt())

    val reveal = remember { Animatable(0f) }
    LaunchedEffect(points) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, animationSpec = tween(durationMillis = 900))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val count = points.size
            if (count == 0) return@Canvas
            val slot = size.width / count
            val bottom = size.height - 4f

            fun xAt(i: Int): Float = slot * i + slot / 2f
            fun yAt(value: Double): Float {
                val fraction = if (maxAmount > 0) (value / maxAmount).toFloat() else 0f
                return bottom - fraction * reveal.value * (size.height - 12f)
            }

            drawLine(
                color = gridColor,
                start = Offset(0f, bottom),
                end = Offset(size.width, bottom),
                strokeWidth = 2f
            )

            if (maxAmount > 0) {
                drawLine(
                    color = lineColor.copy(alpha = 0.5f),
                    start = Offset(0f, yAt(average)),
                    end = Offset(size.width, yAt(average)),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 14f))
                )
            }

            val fill = Path()
            fill.moveTo(xAt(0), bottom)
            points.forEachIndexed { i, p -> fill.lineTo(xAt(i), yAt(p.amount)) }
            fill.lineTo(xAt(count - 1), bottom)
            fill.close()
            drawPath(
                path = fill,
                brush = Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
                )
            )

            val line = Path()
            points.forEachIndexed { i, p ->
                if (i == 0) line.moveTo(xAt(i), yAt(p.amount))
                else line.lineTo(xAt(i), yAt(p.amount))
            }
            drawPath(
                path = line,
                color = lineColor,
                style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            if (count <= 14) {
                points.forEachIndexed { i, p ->
                    drawCircle(
                        color = lineColor,
                        radius = 7f,
                        center = Offset(xAt(i), yAt(p.amount))
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEachIndexed { i, p ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (i % step == 0) p.label else "",
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(
            text = "Average ${formatRupees(average)} · Peak ${formatRupees(maxAmount)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}