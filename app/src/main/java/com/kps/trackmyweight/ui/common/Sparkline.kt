package com.kps.trackmyweight.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Sparkline (mini line chart) 100 % Compose Canvas, sans dépendance externe.
 * Trace la série brute (ligne fine estompée) + la série lissée (ligne accent).
 */
@Composable
fun Sparkline(
    raw: List<Float>,
    smoothed: List<Float> = emptyList(),
    targetLine: Float? = null,
    modifier: Modifier = Modifier,
) {
    val rawColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val smoothColor = MaterialTheme.colorScheme.primary
    val targetColor = MaterialTheme.colorScheme.tertiary
    val allValues = buildList {
        addAll(raw)
        addAll(smoothed)
        targetLine?.let { add(it) }
    }
    if (allValues.isEmpty()) return
    val minY = allValues.min()
    val maxY = allValues.max()
    val range = (maxY - minY).coerceAtLeast(0.5f)

    Canvas(modifier = modifier.height(160.dp)) {
        val w = size.width
        val h = size.height
        val vPad = 12f

        fun y(v: Float): Float = h - vPad - ((v - minY) / range) * (h - vPad * 2)
        fun x(i: Int, count: Int): Float =
            if (count <= 1) w / 2f else (i.toFloat() / (count - 1)) * w

        targetLine?.let { t ->
            drawLine(
                color = targetColor.copy(alpha = 0.6f),
                start = Offset(0f, y(t)),
                end = Offset(w, y(t)),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
            )
        }

        if (raw.size >= 2) {
            drawPolyline(raw, ::x, ::y, rawColor, strokeWidthPx = 2f)
        }
        if (smoothed.size >= 2) {
            drawPolyline(smoothed, ::x, ::y, smoothColor, strokeWidthPx = 4f)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPolyline(
    values: List<Float>,
    xFn: (Int, Int) -> Float,
    yFn: (Float) -> Float,
    color: Color,
    strokeWidthPx: Float,
) {
    val path = Path()
    values.forEachIndexed { i, v ->
        val px = xFn(i, values.size)
        val py = yFn(v)
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    drawPath(path, color = color, style = Stroke(width = strokeWidthPx))
}
