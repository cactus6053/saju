package com.saju.engine

import com.saju.domain.core.Jeolgi
import com.saju.domain.core.SixtyGapja
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime
import kotlin.test.assertEquals

class MonthPillarCalculatorTest {

    private val jeolgiCalc = JeolgiCalculator()
    private val calc = MonthPillarCalculator(
        MonthBoundaryResolver(jeolgiCalc),
        YearPillarCalculator(jeolgiCalc),
    )

    // ── 2024년(甲辰年) 12개월 전체 검증 ─────────────────────────────────

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(
        "2024-02-20T12:00, 丙寅",
        "2024-03-20T12:00, 丁卯",
        "2024-04-20T12:00, 戊辰",
        "2024-05-20T12:00, 己巳",
        "2024-06-15T12:00, 庚午",
        "2024-07-20T12:00, 辛未",
        "2024-08-20T12:00, 壬申",
        "2024-09-20T12:00, 癸酉",
        "2024-10-20T12:00, 甲戌",
        "2024-11-20T12:00, 乙亥",
        "2024-12-20T12:00, 丙子",
        "2025-01-20T12:00, 丁丑",  // 양력 2025년이지만 사주 연도는 2024(甲辰)
    )
    fun `갑진년 12개월 월주 검증`(dateTimeStr: String, expectedHanja: String) {
        val result = calc.calculate(LocalDateTime.parse(dateTimeStr))
        assertEquals(expectedHanja, result.ganJi.hanja)
    }

    // ── 오호둔년법 조견표 전체 검증 ─────────────────────────────────────

    @ParameterizedTest(name = "{0}년(연간 {1}) 인월 = {2}")
    @CsvSource(
        "1984, 甲, 丙寅",
        "1989, 己, 丙寅",
        "1985, 乙, 戊寅",
        "1990, 庚, 戊寅",
        "1986, 丙, 庚寅",
        "1991, 辛, 庚寅",
        "1987, 丁, 壬寅",
        "1992, 壬, 壬寅",
        "1988, 戊, 甲寅",
        "1993, 癸, 甲寅",
    )
    fun `오호둔년법 - 연간별 인월 월간 검증`(year: Int, yearGan: String, expectedHanja: String) {
        // 3월 1일은 항상 인월 (경칩은 3월 5~6일)
        val result = calc.calculate(LocalDateTime.of(year, 3, 1, 12, 0))
        assertEquals(expectedHanja, result.ganJi.hanja, "${year}년 인월")
    }

    // ── 연 경계: 1월은 전년도 연간 기준 ─────────────────────────────────

    @Test
    fun `1월 축월은 전년도 연간 기준 월간`() {
        // 2024-01-15: 사주 연도 2023(癸卯) → 계년 甲寅 시작 → 丑월 = 乙丑
        val result = calc.calculate(LocalDateTime.of(2024, 1, 15, 12, 0))
        assertEquals("乙丑", result.ganJi.hanja)
    }

    @Test
    fun `입춘 경계에서 월주가 축월에서 인월로 전환`() {
        val ipchun = jeolgiCalc.getMoment(2024, Jeolgi.IPCHUN)

        val before = calc.calculate(ipchun.minusSeconds(1))
        assertEquals("乙丑", before.ganJi.hanja, "입춘 직전: 계묘년 축월")

        val after = calc.calculate(ipchun)
        assertEquals("丙寅", after.ganJi.hanja, "입춘 정각: 갑진년 인월")
    }

    // ── 구조 불변식 ─────────────────────────────────────────────────────

    @Test
    fun `연속된 월의 월간은 60갑자 순서로 1씩 증가`() {
        val jeolMoments = Jeolgi.JEOL_LIST.map { jeolgiCalc.getMoment(2024, it) }.sorted()
        val pillars = jeolMoments.map { calc.calculate(it.plusHours(1)).ganJi }

        pillars.zipWithNext().forEach { (a, b) ->
            val expected = SixtyGapja.at(SixtyGapja.indexOf(a) + 1)
            assertEquals(expected, b, "${a.hanja} 다음 월주가 ${b.hanja}")
        }
    }

    @Test
    fun `5년 주기 - 같은 월지의 월간은 연간 5주기로 반복`() {
        // 오호둔년법 특성: 甲년과 己년의 월간 배열이 동일
        val gap = calc.calculate(LocalDateTime.of(1984, 6, 15, 12, 0))  // 甲子년
        val gi = calc.calculate(LocalDateTime.of(1989, 6, 15, 12, 0))   // 己巳년
        assertEquals(gap.ganJi, gi.ganJi)
    }

    @Test
    fun `월주와 사주월 정보의 월지 일치`() {
        val result = calc.calculate(LocalDateTime.of(2024, 6, 15, 12, 0))
        assertEquals(result.sajuMonth.monthBranch, result.ganJi.ji)
    }
}
