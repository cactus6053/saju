package com.saju.analysis

import com.saju.domain.core.Gender
import com.saju.engine.BirthInput
import com.saju.engine.SajuCalculator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YearlySummaryCalculatorTest {

    private val calculator = YearlySummaryCalculator()
    private val fortuneService = FortuneService()
    private val saju = SajuCalculator().calculate(
        BirthInput(1994, 10, 24, 12, 14, gender = Gender.FEMALE)
    )
    private val gyeokGuk = GyeokGukAnalyzer().analyze(saju)

    @Test
    fun `카테고리 점수 - 5종 전부, 1~5 범위, 결정적`() {
        val fortune = fortuneService.fortuneOfYear(saju, 2026)

        val first = calculator.categoryScores(saju, gyeokGuk, fortune.seun)
        val second = calculator.categoryScores(saju, gyeokGuk, fortune.seun)

        assertEquals(FortuneCategory.entries.toList(), first.map { it.category })
        first.forEach { assertTrue(it.score in 1..5, "${it.category} 점수 ${it.score}가 범위 밖") }
        assertEquals(first, second)
    }

    @Test
    fun `월별 점수 - 12개 전부, 1~5 범위`() {
        val fortune = fortuneService.fortuneOfYear(saju, 2026)

        val scores = calculator.monthScores(gyeokGuk, fortune.wolunList)

        assertEquals((1..12).toList(), scores.map { it.month })
        scores.forEach { assertTrue(it.score in 1..5, "${it.month}월 점수 ${it.score}가 범위 밖") }
    }

    @Test
    fun `월별 점수 분포가 한 값에 쏠리지 않음 - 3년 스캔`() {
        val scores = (2026..2028).flatMap { year ->
            calculator.monthScores(gyeokGuk, fortuneService.fortuneOfYear(saju, year).wolunList)
        }.map { it.score }

        val distribution = scores.groupingBy { it }.eachCount()
        assertTrue(distribution.size >= 2, "점수 종류가 2개 미만: $distribution")
        assertTrue(distribution.values.max() < scores.size * 9 / 10, "한 점수가 90% 이상: $distribution")
    }

    @Test
    fun `애정 점수는 성별 기준이 다르다 - 남명 재성, 여명 관성`() {
        // 같은 사주라도 성별에 따라 이성성 그룹이 달라 다른 연도들에서 점수 차이가 난다
        val male = SajuCalculator().calculate(BirthInput(1994, 10, 24, 12, 14, gender = Gender.MALE))
        val maleGyeokGuk = GyeokGukAnalyzer().analyze(male)

        val differs = (2026..2035).any { year ->
            val femaleScore = calculator.categoryScores(saju, gyeokGuk, fortuneService.fortuneOfYear(saju, year).seun)
                .first { it.category == FortuneCategory.LOVE }.score
            val maleScore = calculator.categoryScores(male, maleGyeokGuk, fortuneService.fortuneOfYear(male, year).seun)
                .first { it.category == FortuneCategory.LOVE }.score
            femaleScore != maleScore
        }
        assertTrue(differs, "10년간 남녀 애정 점수가 전부 동일 — 이성성 분기가 동작하지 않음")
    }
}
