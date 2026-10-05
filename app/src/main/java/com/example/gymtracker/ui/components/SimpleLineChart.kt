package com.example.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SimpleLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (values.size < 2) return

    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val maxVal = values.maxOrNull() ?: 1f
        val minVal = values.minOrNull() ?: 0f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val spacing = size.width / (values.size - 1)
        val path = Path()

        values.forEachIndexed { index, value ->
            val x = index * spacing
            val normalizedY = (value - minVal) / range
            val y = size.height - (normalizedY * (size.height - 40f)) - 20f

            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color = lineColor, radius = 6f, center = Offset(x, y))
        }

        drawPath(path = path, color = lineColor, style = Stroke(width = 4f))
    }
}
