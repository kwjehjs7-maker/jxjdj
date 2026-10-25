package com.so2matchtracker.domain

import com.so2matchtracker.data.MatchEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchStatsTest {
    @Test
    fun overallKdUsesTotalsRatherThanAverageOfMatchRatios() {
        val matches = listOf(
            match(id = "a", kills = 9, deaths = 3),
            match(id = "b", kills = 1, deaths = 7),
        )

        assertEquals(1.0, MatchStats.overallKd(matches), 0.0001)
    }

    @Test
    fun zeroDeathsAreHandledWithoutDivisionByZero() {
        assertTrue(MatchStats.overallKd(listOf(match(kills = 12, deaths = 0))).isInfinite())
        assertEquals(0.0, MatchStats.overallKd(listOf(match(kills = 0, deaths = 0))), 0.0)
        assertEquals(0.0, MatchStats.overallKd(emptyList()), 0.0)
        assertTrue(MatchStats.matchKd(match(kills = 12, deaths = 0)).isInfinite())
    }

    @Test
    fun winRateCountsDrawsAndLossesInTheDenominator() {
        val matches = listOf(
            match(id = "win", result = MatchResult.WIN.name),
            match(id = "draw", result = MatchResult.DRAW.name),
            match(id = "loss", result = MatchResult.LOSS.name),
            match(id = "win-2", result = MatchResult.WIN.name),
        )

        assertEquals(50.0, MatchStats.winRate(matches), 0.0001)
    }

    @Test
    fun problemShareUsesAllMatchesAsTheDenominator() {
        val matches = listOf(
            match(id = "a", problem = ProblemCategory.AIM.name),
            match(id = "b", problem = null),
            match(id = "c", problem = ProblemCategory.AIM.name),
            match(id = "d", problem = ProblemCategory.TECHNICAL.name),
        )

        val aim = MatchStats.categoryFrequencies(matches).first { it.category == ProblemCategory.AIM }
        assertEquals(2, aim.matches)
        assertEquals(50.0, aim.sharePercent, 0.0001)
        assertEquals(ProblemCategory.entries.size, MatchStats.categoryFrequencies(matches).size)
    }

    @Test
    fun totalMmrIgnoresMissingValuesAndUsesLongSum() {
        val matches = listOf(
            match(id = "a").copy(mmrChange = 12),
            match(id = "b").copy(mmrChange = -5),
            match(id = "c").copy(mmrChange = null),
        )

        assertEquals(7L, MatchStats.totalMmr(matches))
        assertEquals(null, MatchStats.totalMmr(emptyList()))
    }

    @Test
    fun rollingWinRateUsesOnlyTheSelectedWindow() {
        val matches = listOf(
            match(id = "1", result = MatchResult.WIN.name),
            match(id = "2", result = MatchResult.LOSS.name),
            match(id = "3", result = MatchResult.WIN.name),
        )

        assertEquals(listOf(100.0, 50.0, 66.666667), MatchStats.rollingWinRate(matches, 5).map { round(it) })
    }

    private fun match(
        id: String = "match",
        result: String = MatchResult.WIN.name,
        kills: Int = 0,
        deaths: Int = 0,
        problem: String? = null,
    ) = MatchEntity(
        id = id,
        result = result,
        kills = kills,
        deaths = deaths,
        mapName = null,
        mmrChange = null,
        problemCategory = problem,
        note = null,
        playedAt = 1L,
    )

    private fun round(value: Double): Double = kotlin.math.round(value * 1_000_000.0) / 1_000_000.0
}
