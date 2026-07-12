package com.saju.analysis

import com.saju.domain.core.Element
import com.saju.domain.core.Gender
import com.saju.domain.core.SixtyGapja
import com.saju.engine.BirthInput
import com.saju.engine.SajuCalculator
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IlunCalculatorTest {

    private val calculator = IlunCalculator()
    private val saju = SajuCalculator().calculate(
        BirthInput(1994, 10, 24, 12, 14, gender = Gender.FEMALE)
    )
    private val gyeokGuk = GyeokGukAnalyzer().analyze(saju)

    // ── 일진 계산 ───────────────────────────────────────────────────────

    @Test
    fun `일진 계산 - 2024-01-01은 갑자일`() {
        assertEquals(SixtyGapja.fromHanja("甲子"), calculator.ganJiOf(LocalDate.of(2024, 1, 1)))
    }

    @Test
    fun `일진은 60일 주기로 순환`() {
        val base = LocalDate.of(2026, 7, 13)
        assertEquals(calculator.ganJiOf(base), calculator.ganJiOf(base.plusDays(60)))
    }

    // ── 결정성 ─────────────────────────────────────────────────────────

    @Test
    fun `같은 입력이면 항상 같은 결과`() {
        val date = LocalDate.of(2026, 7, 13)
        val first = calculator.analyze(saju, gyeokGuk, date)
        val second = calculator.analyze(saju, gyeokGuk, date)
        assertEquals(first, second)
    }

    // ── 점수 규칙 ──────────────────────────────────────────────────────

    @Test
    fun `점수는 1~5 범위 - 1년 스캔`() {
        val start = LocalDate.of(2026, 1, 1)
        repeat(365) { offset ->
            val result = calculator.analyze(saju, gyeokGuk, start.plusDays(offset.toLong()))
            assertTrue(result.score in 1..5, "${result.date}의 점수 ${result.score}가 범위 밖")
        }
    }

    @Test
    fun `점수 분포가 한 값에 쏠리지 않음 - 1년 스캔`() {
        val start = LocalDate.of(2026, 1, 1)
        val scores = (0 until 365).map {
            calculator.analyze(saju, gyeokGuk, start.plusDays(it.toLong())).score
        }
        val distribution = scores.groupingBy { it }.eachCount()
        assertTrue(distribution.size >= 3, "점수 종류가 3개 미만: $distribution")
        assertTrue(
            distribution.values.max() < 365 * 9 / 10,
            "한 점수가 90% 이상 차지: $distribution",
        )
    }

    @Test
    fun `용신 오행 일진은 무관계 기준일보다 점수가 높다`() {
        val start = LocalDate.of(2026, 1, 1)
        val results = (0 until 365).map { calculator.analyze(saju, gyeokGuk, start.plusDays(it.toLong())) }

        // 합충이 전혀 없는 날 중: 용신 무관 날은 기준 3점, 천간·지지 모두 용신인 날은 5점
        val neutral = results.firstOrNull {
            it.relationsWithWonguk.isEmpty() &&
                it.ganJi.gan.element != gyeokGuk.yongsin && it.ganJi.ji.element != gyeokGuk.yongsin &&
                it.ganJi.gan.element != gyeokGuk.johuYongsin && it.ganJi.ji.element != gyeokGuk.johuYongsin
        }
        val boosted = results.firstOrNull {
            it.relationsWithWonguk.isEmpty() &&
                it.ganJi.gan.element == gyeokGuk.yongsin && it.ganJi.ji.element == gyeokGuk.yongsin
        }

        assertNotNull(neutral, "1년 내 무관계·용신무관 일진이 없음")
        assertEquals(3, neutral.score)
        if (boosted != null) {
            assertEquals(5, boosted.score)
        }
    }

    // ── 행운 요소 ──────────────────────────────────────────────────────

    @Test
    fun `행운의 색은 용신 오행의 오방색`() {
        val result = calculator.analyze(saju, gyeokGuk, LocalDate.of(2026, 7, 13))
        val expected = when (gyeokGuk.yongsin) {
            Element.WOOD -> LuckyColor.GREEN
            Element.FIRE -> LuckyColor.RED
            Element.EARTH -> LuckyColor.YELLOW
            Element.METAL -> LuckyColor.GOLD
            Element.WATER -> LuckyColor.BLUE
        }
        assertEquals(expected, result.luckyColor)
    }

    @Test
    fun `행운의 숫자는 용신 하도 수리 쌍에서 나오고 날마다 교대`() {
        val expectedPair = when (gyeokGuk.yongsin) {
            Element.WOOD -> setOf(3, 8)
            Element.FIRE -> setOf(2, 7)
            Element.EARTH -> setOf(5)
            Element.METAL -> setOf(4, 9)
            Element.WATER -> setOf(1, 6)
        }
        val day1 = calculator.analyze(saju, gyeokGuk, LocalDate.of(2026, 7, 13))
        val day2 = calculator.analyze(saju, gyeokGuk, LocalDate.of(2026, 7, 14))

        assertTrue(day1.luckyNumber in expectedPair)
        assertTrue(day2.luckyNumber in expectedPair)
        if (expectedPair.size == 2) {
            assertTrue(day1.luckyNumber != day2.luckyNumber, "60갑자 홀짝 교대로 연속 이틀은 달라야 함")
        }
    }

    @Test
    fun `행운 아이템은 날마다 순환`() {
        val day1 = calculator.analyze(saju, gyeokGuk, LocalDate.of(2026, 7, 13))
        val day2 = calculator.analyze(saju, gyeokGuk, LocalDate.of(2026, 7, 14))
        assertTrue(day1.luckyItem != day2.luckyItem)
    }

    // ── 십성·12운성 ────────────────────────────────────────────────────

    @Test
    fun `일진 십성과 12운성이 채워진다`() {
        val result = calculator.analyze(saju, gyeokGuk, LocalDate.of(2026, 7, 13))
        assertNotNull(result.ganSipSeong)
        assertNotNull(result.jiSipSeong)
        assertNotNull(result.unSeong)
    }
}
