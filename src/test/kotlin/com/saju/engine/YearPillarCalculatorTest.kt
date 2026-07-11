package com.saju.engine

import com.saju.domain.core.Jeolgi
import com.saju.domain.core.JiJi
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YearPillarCalculatorTest {

    private val jeolgiCalc = JeolgiCalculator()
    private val calc = YearPillarCalculator(jeolgiCalc)

    // ── 연 중반 날짜 (입춘 경계에서 먼 날짜) ────────────────────────────

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(
        "1984-06-15T12:00, 甲子",
        "1995-06-15T12:00, 乙亥",
        "2000-06-15T12:00, 庚辰",
        "2024-06-15T12:00, 甲辰",
        "2025-06-15T12:00, 乙巳",
        "1900-06-15T12:00, 庚子",
        "2100-06-15T12:00, 庚申",
    )
    fun `연 중반 날짜의 연주 검증`(dateTimeStr: String, expectedHanja: String) {
        val result = calc.calculate(LocalDateTime.parse(dateTimeStr))
        assertEquals(expectedHanja, result.ganJi.hanja)
    }

    // ── 입춘 이전 = 전년도 연주 ─────────────────────────────────────────

    @Test
    fun `1월 출생은 전년도 연주`() {
        val result = calc.calculate(LocalDateTime.of(2024, 1, 15, 12, 0))
        assertEquals("癸卯", result.ganJi.hanja)
        assertEquals(2023, result.sajuYear)
    }

    @Test
    fun `입춘 전날 2월 초 출생은 전년도 연주`() {
        val result = calc.calculate(LocalDateTime.of(2024, 2, 3, 12, 0))
        assertEquals(2023, result.sajuYear)
    }

    @Test
    fun `12월 31일은 당해 연주`() {
        val result = calc.calculate(LocalDateTime.of(2024, 12, 31, 23, 59))
        assertEquals("甲辰", result.ganJi.hanja)
        assertEquals(2024, result.sajuYear)
    }

    // ── 입춘 절입 시각 경계 (초 단위) ───────────────────────────────────

    @Test
    fun `입춘 절입 정각은 새해로 판정`() {
        val ipchun = jeolgiCalc.getMoment(2024, Jeolgi.IPCHUN)
        val result = calc.calculate(ipchun)
        assertEquals(2024, result.sajuYear)
        assertEquals("甲辰", result.ganJi.hanja)
    }

    @Test
    fun `입춘 1초 전은 전년도로 판정`() {
        val ipchun = jeolgiCalc.getMoment(2024, Jeolgi.IPCHUN)
        val result = calc.calculate(ipchun.minusSeconds(1))
        assertEquals(2023, result.sajuYear)
        assertEquals("癸卯", result.ganJi.hanja)
    }

    @Test
    fun `여러 해에 걸쳐 입춘 경계 일관성 검증`() {
        listOf(1950, 1988, 2000, 2033, 2077).forEach { year ->
            val ipchun = jeolgiCalc.getMoment(year, Jeolgi.IPCHUN)
            assertEquals(year - 1, calc.calculate(ipchun.minusSeconds(1)).sajuYear, "${year}년 입춘 직전")
            assertEquals(year, calc.calculate(ipchun).sajuYear, "${year}년 입춘 정각")
        }
    }

    // ── 월 경계 판정과의 정합성 ─────────────────────────────────────────

    @Test
    fun `입춘 직후는 연주 전환과 인월 시작이 동시에 일어남`() {
        val resolver = MonthBoundaryResolver(jeolgiCalc)
        val ipchun = jeolgiCalc.getMoment(2024, Jeolgi.IPCHUN)

        val monthAfter = resolver.resolve(ipchun.plusMinutes(1))
        val yearAfter = calc.calculate(ipchun.plusMinutes(1))

        assertEquals(JiJi.IN, monthAfter.monthBranch, "입춘 직후는 인월")
        assertEquals(2024, yearAfter.sajuYear, "입춘 직후는 새해")

        val monthBefore = resolver.resolve(ipchun.minusMinutes(1))
        val yearBefore = calc.calculate(ipchun.minusMinutes(1))

        assertEquals(JiJi.CHUK, monthBefore.monthBranch, "입춘 직전은 축월")
        assertEquals(2023, yearBefore.sajuYear, "입춘 직전은 전년")
    }

    @Test
    fun `연간의 음양은 60갑자 규칙과 일치 - 짝수 인덱스는 양간`() {
        (2000..2010).forEach { year ->
            val result = calc.calculate(LocalDateTime.of(year, 6, 15, 12, 0))
            assertEquals(result.ganJi.gan.yinYang, result.ganJi.ji.yinYang, "${year}년 연주 음양 불일치")
        }
    }
}
