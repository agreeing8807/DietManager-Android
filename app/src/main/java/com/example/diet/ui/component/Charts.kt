package com.example.diet.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.math.min

/**
 * 体重折线图：传入按日期升序的 (yyyy-MM-dd, kg) 数据。
 * 目标体重以虚线展示，折线下方带渐变填充。
 */
@Composable
fun WeightLineChart(
    points: List<Pair<String, Float>>,
    targetKg: Float? = null,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
    lineColor: Color = Color(0xFF7E57C2)
) {
    if (points.isEmpty()) return

    val values = points.map { it.second }
    val rawMin = values.minOrNull() ?: 0f
    val rawMax = values.maxOrNull() ?: 0f
    val pad = ((rawMax - rawMin) * 0.25f).coerceAtLeast(0.5f)
    var lo = rawMin - pad
    var hi = rawMax + pad
    // 目标体重离数据太远时不参与缩放，避免把曲线压扁
    val showTarget = targetKg != null && targetKg >= lo - 3f && targetKg <= hi + 3f
    if (showTarget && targetKg != null) {
        lo = min(lo, targetKg - 0.2f)
        hi = maxOf(hi, targetKg + 0.2f)
    }
    val range = (hi - lo).coerceAtLeast(0.1f)

    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .padding(vertical = 8.dp)
        ) {
            val insetX = 6.dp.toPx()
            val usableW = size.width - insetX * 2
            val usableH = size.height

            fun xAt(index: Int): Float =
                if (points.size == 1) size.width / 2f
                else insetX + usableW * index / (points.size - 1).toFloat()

            fun yAt(value: Float): Float = usableH * (1f - (value - lo) / range)

            // 横向网格
            val gridColor = Color(0x1A000000)
            for (i in 0..3) {
                val y = usableH * i / 3f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 目标体重虚线
            if (showTarget && targetKg != null) {
                val y = yAt(targetKg)
                drawLine(
                    color = lineColor.copy(alpha = 0.45f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
            }

            if (points.size == 1) {
                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(size.width / 2f, yAt(values[0])))
            } else {
                val linePath = Path().apply {
                    moveTo(xAt(0), yAt(values[0]))
                    for (i in 1 until points.size) {
                        val prevX = xAt(i - 1)
                        val prevY = yAt(values[i - 1])
                        val currX = xAt(i)
                        val currY = yAt(values[i])
                        val midX = (prevX + currX) / 2f
                        cubicTo(midX, prevY, midX, currY, currX, currY)
                    }
                }

                // 渐变填充
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(xAt(points.size - 1), usableH)
                    lineTo(xAt(0), usableH)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f)),
                        startY = 0f,
                        endY = usableH
                    )
                )

                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                points.forEachIndexed { i, _ ->
                    val center = Offset(xAt(i), yAt(values[i]))
                    drawCircle(color = Color.White, radius = 4.dp.toPx(), center = center)
                    drawCircle(color = lineColor, radius = 2.6.dp.toPx(), center = center)
                }
            }
        }

        // 日期轴
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(shortDate(points.first().first), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (points.size > 2) {
                Text(shortDate(points[points.size / 2].first), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(shortDate(points.last().first), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (showTarget && targetKg != null) {
            Text(
                "虚线为目标体重 ${oneDecimal(targetKg)} kg",
                style = MaterialTheme.typography.labelSmall,
                color = lineColor,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * 每日饮水量柱状图：传入按日期升序的 (yyyy-MM-dd, ml) 数据，达标当天换色。
 */
@Composable
fun WaterBarChart(
    daily: List<Pair<String, Int>>,
    goalMl: Int,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    barColor: Color = Color(0xFF4FC3F7),
    reachedColor: Color = Color(0xFF0288D1)
) {
    if (daily.isEmpty()) return
    val maxValue = (daily.maxOf { it.second }).coerceAtLeast(goalMl).coerceAtLeast(1)
    val topValue = maxValue * 1.15f

    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val slot = size.width / daily.size
            val barWidth = slot * 0.48f
            val radius = min(barWidth / 2f, 8.dp.toPx())

            // 目标线
            val goalY = size.height * (1f - goalMl / topValue)
            drawLine(
                color = Color(0x660288D1),
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )

            daily.forEachIndexed { index, (_, volume) ->
                val left = slot * index + (slot - barWidth) / 2f
                val fullHeight = size.height * (volume / topValue)
                val barHeight = if (volume <= 0) 2.dp.toPx() else fullHeight.coerceAtLeast(4.dp.toPx())
                val top = size.height - barHeight
                val color = if (volume >= goalMl) reachedColor else barColor

                if (volume <= 0) {
                    drawRoundRect(
                        color = Color(0x22000000),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(radius, radius)
                    )
                } else {
                    val path = Path().apply {
                        moveTo(left, size.height)
                        lineTo(left, top + radius)
                        quadraticBezierTo(left, top, left + radius, top)
                        lineTo(left + barWidth - radius, top)
                        quadraticBezierTo(left + barWidth, top, left + barWidth, top + radius)
                        lineTo(left + barWidth, size.height)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            listOf(color, color.copy(alpha = 0.55f)),
                            startY = top,
                            endY = size.height
                        )
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            daily.forEach { (date, _) ->
                Text(
                    weekdayLabel(date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun shortDate(date: String): String {
    val parts = date.split("-")
    if (parts.size != 3) return date
    val month = parts[1].toIntOrNull() ?: return date
    val day = parts[2].toIntOrNull() ?: return date
    return "$month/$day"
}

private fun weekdayLabel(date: String): String = runCatching {
    when (LocalDate.parse(date).dayOfWeek.value) {
        1 -> "一"
        2 -> "二"
        3 -> "三"
        4 -> "四"
        5 -> "五"
        6 -> "六"
        else -> "日"
    }
}.getOrDefault("·")

internal fun oneDecimal(value: Float): String = String.format(java.util.Locale.CHINA, "%.1f", value)
