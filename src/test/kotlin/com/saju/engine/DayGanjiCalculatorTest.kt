package com.saju.engine

import com.saju.domain.core.SixtyGapja
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import kotlin.test.assertEquals

class DayGanjiCalculatorTest {

    private val calc = DayGanjiCalculator()

    // ── 독립 앵커 검증 ───────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(
        "2000-01-01, 戊午",  // 밀레니엄 기준일
        "2024-01-01, 甲子",  // 60갑자 첫 간지로 시작한 해
        "1949-10-01, 甲子",  // 역사적 기준일
        "1970-01-01, 辛巳",  // epoch day 0
    )
    fun `알려진 날짜의 일진 검증`(dateStr: String, expectedHanja: String) {
        val date = LocalDate.parse(dateStr)
        assertEquals(expectedHanja, calc.calculate(date).hanja)
    }

    // ── 순환 규칙 검증 ───────────────────────────────────────────────────

    @Test
    fun `연속된 날짜는 60갑자 순서대로 진행`() {
        val start = LocalDate.of(2024, 1, 1)
        val startIndex = SixtyGapja.indexOf(calc.calculate(start))

        (1..120).forEach { offset ->
            val expected = SixtyGapja.at(startIndex + offset)
            val actual = calc.calculate(start.plusDays(offset.toLong()))
            assertEquals(expected, actual, "${start.plusDays(offset.toLong())} 일진 불일치")
        }
    }

    @Test
    fun `60일 간격의 두 날짜는 같은 일진`() {
        val base = LocalDate.of(1950, 6, 15)
        assertEquals(calc.calculate(base), calc.calculate(base.plusDays(60)))
        assertEquals(calc.calculate(base), calc.calculate(base.plusDays(6000)))
    }

    @Test
    fun `윤년 경계 - 2000년 2월 28일부터 3월 1일까지 연속`() {
        val feb28 = calc.calculate(LocalDate.of(2000, 2, 28))
        val feb29 = calc.calculate(LocalDate.of(2000, 2, 29))
        val mar01 = calc.calculate(LocalDate.of(2000, 3, 1))

        val i28 = SixtyGapja.indexOf(feb28)
        assertEquals(SixtyGapja.at(i28 + 1), feb29, "윤일 불연속")
        assertEquals(SixtyGapja.at(i28 + 2), mar01, "윤일 다음날 불연속")
    }

    @Test
    fun `비윤년 경계 - 1900년은 윤년 아님 (2월 28일 다음날 3월 1일)`() {
        val feb28 = calc.calculate(LocalDate.of(1900, 2, 28))
        val mar01 = calc.calculate(LocalDate.of(1900, 3, 1))
        assertEquals(SixtyGapja.at(SixtyGapja.indexOf(feb28) + 1), mar01)
    }

    // ── 범위 경계 검증 ───────────────────────────────────────────────────

    @Test
    fun `최소 지원일 1900-01-01 계산 가능`() {
        assertEquals("甲戌", calc.calculate(LocalDate.of(1900, 1, 1)).hanja)
    }

    @Test
    fun `최대 지원일 2100-12-31 계산 가능`() {
        // 1900-01-01(甲戌=10)부터 73413일 경과: (10 + 73413) % 60 = 13 = 丁丑
        val result = calc.calculate(LocalDate.of(2100, 12, 31))
        val expectedIndex = ((10 + LocalDate.of(2100, 12, 31).toEpochDay() -
            LocalDate.of(1900, 1, 1).toEpochDay()) % 60).toInt()
        assertEquals(SixtyGapja.at(expectedIndex), result)
    }

    @Test
    fun `범위 이전 날짜는 예외`() {
        assertThrows<IllegalArgumentException> {
            calc.calculate(LocalDate.of(1899, 12, 31))
        }
    }

    @Test
    fun `범위 이후 날짜는 예외`() {
        assertThrows<IllegalArgumentException> {
            calc.calculate(LocalDate.of(2101, 1, 1))
        }
    }
}
