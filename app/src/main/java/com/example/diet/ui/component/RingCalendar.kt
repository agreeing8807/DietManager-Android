package com.example.diet.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diet.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

/** 水分圆环颜色（与首页进度条一致） */
private val WaterRingColor = Color(0xFF0288D1)

/** 热量圆环颜色（与首页进度条一致） */
private val CalorieRingColor = Color(0xFFFF7043)

private val WeekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 紧凑型月历：每一天是两个以日期为圆心的同心圆环。
 * - 内环 = 当日计入水分总量 / 每日饮水目标
 * - 外环 = 当日饮食+饮品热量 / 每日热量目标
 * - 已达到的部分按进度着色，未达到的部分为灰色底环；超出目标按 100% 显示
 * - 点击某天回调 onSelectDate，未来日期不可点击
 */
@Composable
fun RingCalendar(
    month: YearMonth,
    waterByDate: Map<String, Int>,
    caloriesByDate: Map<String, Int>,
    waterGoal: Int,
    calorieGoal: Int,
    selectedDate: String,
    today: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    canGoNext: Boolean,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier,
    cellHeight: Dp = 44.dp,
    ringStroke: Dp = 2.dp,
    ringGap: Dp = 4.dp
) {
    val cells = buildList<LocalDate?> {
        val leading = month.atDay(1).dayOfWeek.value - 1   // 周一为第一列
        repeat(leading) { add(null) }
        for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
        while (size % 7 != 0) add(null)
    }

    Column(modifier.fillMaxWidth()) {
        // 月份切换
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevMonth, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "上个月", Modifier.size(18.dp))
            }
            Text(
                "${month.year}年${month.monthValue}月",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onNextMonth,
                enabled = canGoNext,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "下个月",
                    Modifier.size(18.dp),
                    tint = if (canGoNext) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }
        }

        // 星期表头
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            WeekdayLabels.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        // 日期网格：每个格内是「日期 + 同心双环」
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        Modifier.weight(1f).height(cellHeight).padding(1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            val key = DateUtils.dateOf(date)
                            val isFuture = today.isNotBlank() && key > today
                            val waterProgress = if (waterGoal > 0) {
                                (waterByDate[key] ?: 0).toFloat() / waterGoal
                            } else 0f
                            val calorieProgress = if (calorieGoal > 0) {
                                (caloriesByDate[key] ?: 0).toFloat() / calorieGoal
                            } else 0f
                            DayCell(
                                day = date.dayOfMonth,
                                waterProgress = if (isFuture) 0f else waterProgress,
                                calorieProgress = if (isFuture) 0f else calorieProgress,
                                isSelected = key == selectedDate,
                                isToday = key == today,
                                isFuture = isFuture,
                                ringStroke = ringStroke,
                                ringGap = ringGap,
                                onClick = { onSelectDate(key) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // 图例
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendItem(CalorieRingColor, "热量（外环）")
            LegendItem(WaterRingColor, "饮水（内环）")
            Spacer(Modifier.weight(1f))
            Text(
                "点击日期查看当日记录",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    waterProgress: Float,
    calorieProgress: Float,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    ringStroke: Dp,
    ringGap: Dp,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val dayColor = when {
        isFuture -> scheme.onSurfaceVariant.copy(alpha = 0.35f)
        isSelected -> scheme.onPrimaryContainer
        else -> scheme.onSurface
    }
    val trackColor = if (isFuture) scheme.surfaceVariant.copy(alpha = 0.5f) else scheme.surfaceVariant

    Box(
        Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(if (isSelected) scheme.primaryContainer else Color.Transparent)
            .clickable(enabled = !isFuture, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = ringStroke.toPx()
            // 环半径随格子自适应，小屏也不会溢出
            val outerRadius = size.minDimension / 2f - strokePx / 2f - 2.5.dp.toPx()
            val innerRadius = outerRadius - ringGap.toPx() - strokePx

            // 内环：饮水
            drawProgressRing(
                radius = innerRadius,
                progress = waterProgress,
                color = WaterRingColor,
                trackColor = trackColor,
                strokePx = strokePx,
                dim = isFuture
            )
            // 外环：热量
            drawProgressRing(
                radius = outerRadius,
                progress = calorieProgress,
                color = CalorieRingColor,
                trackColor = trackColor,
                strokePx = strokePx,
                dim = isFuture
            )
            // 今天：在最外侧加一圈细描边
            if (isToday) {
                drawCircle(
                    color = scheme.primary.copy(alpha = 0.55f),
                    radius = outerRadius + strokePx / 2f + 1.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
        Text(
            day.toString(),
            fontSize = 11.sp,
            lineHeight = 12.sp,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = dayColor
        )
    }
}

/** 从 12 点方向顺时针绘制进度环，底环始终为灰色 */
private fun DrawScope.drawProgressRing(
    radius: Float,
    progress: Float,
    color: Color,
    trackColor: Color,
    strokePx: Float,
    dim: Boolean
) {
    val topLeft = center - Offset(radius, radius)
    val arcSize = Size(radius * 2f, radius * 2f)
    drawArc(
        color = trackColor,
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokePx, cap = StrokeCap.Round)
    )
    val p = progress.coerceIn(0f, 1f)
    if (p > 0f) {
        drawArc(
            color = if (dim) color.copy(alpha = 0.35f) else color,
            startAngle = -90f,
            sweepAngle = 360f * p,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(8.dp)) {
            drawCircle(color = color, radius = this.size.minDimension / 2f)
        }
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
