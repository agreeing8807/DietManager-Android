package com.example.diet.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
 * 紧凑型月历：每天两个圆环。
 * - 上环 = 当日计入水分总量 / 每日饮水目标
 * - 下环 = 当日饮食+饮品热量 / 每日热量目标（此处并排显示）
 * - 填充部分达到的比例，剩余部分为灰色底环；超出目标按 100% 显示
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
    cellHeight: Dp = 46.dp,
    ringDiameter: Dp = 13.dp,
    ringStroke: Dp = 2.2.dp
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

        // 日期网格
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).height(cellHeight).padding(1.dp)) {
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
                                ringDiameter = ringDiameter,
                                ringStroke = ringStroke,
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
            LegendItem(WaterRingColor, "饮水")
            LegendItem(CalorieRingColor, "热量")
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
    ringDiameter: Dp,
    ringStroke: Dp,
    onClick: () -> Unit
) {
    val container = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val border = if (isToday) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
    } else null
    val dayColor = when {
        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        enabled = !isFuture,
        shape = RoundedCornerShape(10.dp),
        color = container,
        border = border,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            Modifier.fillMaxSize().padding(vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                day.toString(),
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = dayColor
            )
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                MiniRing(waterProgress, WaterRingColor, ringDiameter, ringStroke, dim = isFuture)
                MiniRing(calorieProgress, CalorieRingColor, ringDiameter, ringStroke, dim = isFuture)
            }
        }
    }
}

/** 极简进度环：底环灰色，填充部分按进度从 12 点方向顺时针 */
@Composable
private fun MiniRing(
    progress: Float,
    color: Color,
    diameter: Dp,
    stroke: Dp,
    dim: Boolean = false
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.size(diameter)) {
        val strokePx = stroke.toPx()
        val d = this.size.minDimension - strokePx
        val topLeft = Offset((this.size.width - d) / 2f, (this.size.height - d) / 2f)
        val arcSize = Size(d, d)
        drawArc(
            color = if (dim) trackColor.copy(alpha = 0.5f) else trackColor,
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
