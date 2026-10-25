package com.so2matchtracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.so2matchtracker.TrackerColors
import com.so2matchtracker.data.MatchEntity
import com.so2matchtracker.domain.AnalysisPeriod
import com.so2matchtracker.domain.MatchStats
import com.so2matchtracker.domain.ProblemCategory
import java.util.Locale

@Composable
fun AnalyticsScreen(matches: List<MatchEntity>) {
    var period by remember { mutableStateOf(AnalysisPeriod.MONTH) }
    val now = System.currentTimeMillis()
    val selectedMatches = matches
        .filter { period.days == null || it.playedAt >= now - period.days * 86_400_000L }
        .sortedBy { it.playedAt }
    val kdValues = selectedMatches.map { match ->
        MatchStats.matchKd(match).let { if (it.isInfinite()) 5.0 else it.coerceAtMost(5.0) }
    }
    val winValues = MatchStats.rollingWinRate(selectedMatches, window = 5)
    val categoryStats = MatchStats.categoryFrequencies(selectedMatches)
    val mmrMatches = selectedMatches.filter { it.mmrChange != null }
    var cumulativeMmr = 0L
    val mmrValues = mmrMatches.map { match ->
        cumulativeMmr += (match.mmrChange ?: 0).toLong()
        cumulativeMmr.toDouble()
    }

    LazyAnalyticsColumn(
        period = period,
        onPeriodChange = { period = it },
        matchCount = selectedMatches.size,
        kdValues = kdValues,
        winValues = winValues,
        categoryStats = categoryStats,
        mmrValues = mmrValues,
        mmrMatchCount = mmrMatches.size,
    )
}

@Composable
private fun LazyAnalyticsColumn(
    period: AnalysisPeriod,
    onPeriodChange: (AnalysisPeriod) -> Unit,
    matchCount: Int,
    kdValues: List<Double>,
    winValues: List<Double>,
    categoryStats: List<com.so2matchtracker.domain.CategoryFrequency>,
    mmrValues: List<Double>,
    mmrMatchCount: Int,
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionTitle("Аналитика", "Смотри на тенденции, а не на один отдельный матч")
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AnalysisPeriod.entries.forEach { option ->
                    ChoicePill(
                        label = option.label,
                        selected = period == option,
                        onClick = { onPeriodChange(option) },
                    )
                }
            }
        }
        item {
            Text(
                text = "$matchCount матч${pluralMatches(matchCount)} за период",
                style = MaterialTheme.typography.bodySmall,
                color = TrackerColors.muted,
            )
        }
        item {
            if (matchCount < 2) {
                ChartNotReady("K/D по матчам", "Для графика нужно записать хотя бы два матча.")
            } else {
                TrackerCard {
                    SectionTitle("K/D по матчам", "Значения без смертей показаны на верхней отметке 5,00")
                    Spacer(Modifier.height(10.dp))
                    LineChart(values = kdValues, color = TrackerColors.lime, fixedMaximum = 5.0)
                    ChartAxisLabels()
                }
            }
        }
        item {
            if (matchCount < 2) {
                ChartNotReady("Доля побед", "Добавь ещё матчи, чтобы сравнивать динамику.")
            } else {
                TrackerCard {
                    SectionTitle("Динамика побед", "Скользящая доля побед на отрезке до 5 матчей")
                    Spacer(Modifier.height(10.dp))
                    LineChart(values = winValues, color = TrackerColors.blue, fixedMaximum = 100.0)
                    ChartAxisLabels()
                }
            }
        }
        item {
            if (matchCount < 2) {
                ChartNotReady("Отмеченные проблемы", "Частоты появятся после нескольких записей.")
            } else {
                TrackerCard {
                    SectionTitle("Отмеченные проблемы", "Доля от всех матчей за выбранный период")
                    Spacer(Modifier.height(4.dp))
                    if (categoryStats.isEmpty()) {
                        Text(
                            "За этот период проблемы не отмечались.",
                            color = TrackerColors.muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        categoryStats.forEach { stat ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        stat.category.label,
                                        color = TrackerColors.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "${stat.matches} · ${String.format(Locale.forLanguageTag("ru-RU"), "%.0f", stat.sharePercent)}%",
                                        color = TrackerColors.lime,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (stat.sharePercent / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = TrackerColors.lime,
                                    trackColor = TrackerColors.surfaceRaised,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Это частота твоих отметок, а не доказательство причины поражений. Матчи могут проходить в разных условиях.",
                        color = TrackerColors.muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        item {
            when {
                matchCount < 2 -> ChartNotReady("Изменение MMR", "Для линии нужно не менее двух записей с MMR.")
                mmrMatchCount < 2 -> ChartNotReady("Изменение MMR", "Запиши изменение MMR хотя бы в двух матчах.")
                else -> TrackerCard {
                    SectionTitle("Изменение MMR", "Накопленная сумма за выбранный период")
                    Spacer(Modifier.height(10.dp))
                    LineChart(values = mmrValues, color = TrackerColors.lime, centered = true)
                    ChartAxisLabels()
                }
            }
        }
    }
}

@Composable
private fun ChartNotReady(title: String, message: String) {
    TrackerCard {
        Text(title, color = TrackerColors.text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(message, color = TrackerColors.muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LineChart(
    values: List<Double>,
    color: Color,
    fixedMaximum: Double? = null,
    centered: Boolean = false,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .background(TrackerColors.surfaceRaised, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        if (values.isEmpty()) return@Canvas
        val left = 2.dp.toPx()
        val right = size.width - 2.dp.toPx()
        val top = 4.dp.toPx()
        val bottom = size.height - 4.dp.toPx()
        val chartHeight = bottom - top
        val low = if (centered && values.minOrNull()!! < 0) values.minOrNull()!! else 0.0
        val high = fixedMaximum ?: values.maxOrNull() ?: 1.0
        val safeHigh = if (high <= low) low + 1.0 else high
        val span = safeHigh - low

        for (grid in 0..3) {
            val y = top + chartHeight * grid / 3f
            drawLine(
                color = TrackerColors.border,
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.dp.toPx(),
            )
        }
        if (values.size == 1) {
            val y = bottom - (((values.first() - low) / span).toFloat().coerceIn(0f, 1f) * chartHeight)
            drawCircle(color = color, radius = 5.dp.toPx(), center = Offset((left + right) / 2, y))
            return@Canvas
        }
        val points = values.mapIndexed { index, value ->
            val x = left + (right - left) * index / (values.size - 1).toFloat()
            val y = bottom - (((value - low) / span).toFloat().coerceIn(0f, 1f) * chartHeight)
            Offset(x, y)
        }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, color = color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        points.forEach { point ->
            drawCircle(color = color, radius = 3.5.dp.toPx(), center = point)
        }
    }
}

@Composable
private fun ChartAxisLabels() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Старые", color = TrackerColors.muted, style = MaterialTheme.typography.labelSmall)
        Text("Новые", color = TrackerColors.muted, style = MaterialTheme.typography.labelSmall)
    }
}

private fun pluralMatches(count: Int): String {
    val lastTwo = count % 100
    val last = count % 10
    return when {
        lastTwo in 11..14 -> "ей"
        last == 1 -> ""
        last in 2..4 -> "а"
        else -> "ей"
    }
}
