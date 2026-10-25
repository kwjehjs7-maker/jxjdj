package com.so2matchtracker.domain

import com.so2matchtracker.data.MatchEntity

data class CategoryFrequency(
    val category: ProblemCategory,
    val matches: Int,
    val sharePercent: Double,
)

object MatchStats {
    fun totalKills(matches: List<MatchEntity>): Int = matches.sumOf { it.kills }

    fun totalDeaths(matches: List<MatchEntity>): Int = matches.sumOf { it.deaths }

    fun overallKd(matches: List<MatchEntity>): Double {
        val kills = totalKills(matches)
        val deaths = totalDeaths(matches)
        return when {
            deaths > 0 -> kills.toDouble() / deaths
            kills > 0 -> Double.POSITIVE_INFINITY
            else -> 0.0
        }
    }

    fun matchKd(match: MatchEntity): Double = when {
        match.deaths > 0 -> match.kills.toDouble() / match.deaths
        match.kills > 0 -> Double.POSITIVE_INFINITY
        else -> 0.0
    }

    fun winRate(matches: List<MatchEntity>): Double {
        if (matches.isEmpty()) return 0.0
        return matches.count { it.result == MatchResult.WIN.name } * 100.0 / matches.size
    }

    fun totalMmr(matches: List<MatchEntity>): Long? {
        val recorded = matches.filter { it.mmrChange != null }
        return if (recorded.isEmpty()) null else recorded.sumOf { it.mmrChange!!.toLong() }
    }

    fun categoryFrequencies(matches: List<MatchEntity>): List<CategoryFrequency> {
        if (matches.isEmpty()) return emptyList()
        return ProblemCategory.entries
            .map { category ->
                val count = matches.count { it.problemCategory == category.name }
                CategoryFrequency(category, count, count * 100.0 / matches.size)
            }
            .sortedByDescending { it.matches }
    }

    fun rollingWinRate(chronological: List<MatchEntity>, window: Int = 5): List<Double> {
        if (window <= 0) return emptyList()
        return chronological.indices.map { index ->
            val start = (index - window + 1).coerceAtLeast(0)
            winRate(chronological.subList(start, index + 1))
        }
    }

    fun formatKd(kd: Double): String = when {
        kd.isInfinite() -> "∞"
        else -> String.format(java.util.Locale.forLanguageTag("ru-RU"), "%.2f", kd)
    }
}
