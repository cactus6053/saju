package com.saju.engine

import com.saju.domain.core.LunarDate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.time.MonthDay
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LunarConverterTest {

    private val conv = LunarConverter()

    // ── 설날(음력 1월 1일) 앵커 검증 ─────────────────────────────────────

    @ParameterizedTest(name = "설날 {1}년 = 양력 {0}")
    @CsvSource(
        "1900-01-31, 1900",
        "2000-02-05, 2000",
        "2008-02-07, 2008",
        "2012-01-23, 2012",
        "2020-01-25, 2020",
        "2021-02-12, 2021",
        "2022-02-01, 2022",
        "2023-01-22, 2023",
        "2024-02-10, 2024",
        "2025-01-29, 2025",
    )
    fun `설날 양력 날짜 검증`(solarStr: String, lunarYear: Int) {
        val solar = LocalDate.parse(solarStr)
        assertEquals(LunarDate(lunarYear, 1, 1), conv.solarToLunar(solar))
        assertEquals(solar, conv.lunarToSolar(LunarDate(lunarYear, 1, 1)))
    }

    // ── 추석 등 중요 날짜 ────────────────────────────────────────────────

    @Test
    fun `추석 2024 - 음력 8월 15일 = 양력 9월 17일`() {
        assertEquals(LunarDate(2024, 8, 15), conv.solarToLunar(LocalDate.of(2024, 9, 17)))
        assertEquals(LocalDate.of(2024, 9, 17), conv.lunarToSolar(LunarDate(2024, 8, 15)))
    }

    // ── 윤달 검증 ───────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0}년 윤달 = {1}월")
    @CsvSource(
        "1900, 8",
        "1984, 10",
        "2017, 6",
        "2020, 4",
        "2023, 2",
        "2024, 0",
        "2025, 6",
        "2033, 11",
    )
    fun `연도별 윤달 위치 검증`(year: Int, expectedLeapMonth: Int) {
        assertEquals(expectedLeapMonth, conv.leapMonthOf(year))
    }

    @Test
    fun `윤달 날짜 왕복 변환 - 2020년 윤4월`() {
        val leapDate = LunarDate(2020, 4, 15, isLeapMonth = true)
        val solar = conv.lunarToSolar(leapDate)
        assertEquals(leapDate, conv.solarToLunar(solar))

        // 평4월 15일과 윤4월 15일은 다른 양력 날짜
        val normalDate = LunarDate(2020, 4, 15, isLeapMonth = false)
        val normalSolar = conv.lunarToSolar(normalDate)
        assertTrue(solar.isAfter(normalSolar), "윤달은 평달보다 뒤에 와야 함")
    }

    @Test
    fun `윤달이 없는 해에 윤달 입력 시 예외`() {
        assertThrows<IllegalArgumentException> {
            conv.lunarToSolar(LunarDate(2024, 4, 1, isLeapMonth = true))
        }
    }

    @Test
    fun `윤달 월 번호가 틀리면 예외`() {
        // 2020년 윤달은 4월 — 윤5월은 존재하지 않음
        assertThrows<IllegalArgumentException> {
            conv.lunarToSolar(LunarDate(2020, 5, 1, isLeapMonth = true))
        }
    }

    // ── 왕복 변환 (전 범위) ─────────────────────────────────────────────

    @Test
    fun `전 범위 왕복 변환 - 37일 간격 샘플링`() {
        var date = LunarConverter.MIN_DATE
        while (!date.isAfter(LunarConverter.MAX_DATE)) {
            val lunar = conv.solarToLunar(date)
            val back = conv.lunarToSolar(lunar)
            assertEquals(date, back, "왕복 변환 불일치: $date → $lunar → $back")
            date = date.plusDays(37)
        }
    }

    @Test
    fun `연속된 양력 날짜의 음력은 하루씩 증가하거나 월이 바뀜`() {
        var date = LocalDate.of(2023, 1, 1)
        var prev = conv.solarToLunar(date)
        repeat(400) {
            date = date.plusDays(1)
            val cur = conv.solarToLunar(date)
            if (cur.day != 1) {
                assertEquals(prev.day + 1, cur.day, "$date: 일이 연속되지 않음")
            }
            prev = cur
        }
    }

    // ── 테이블 구조 불변식 (1900~2100 전체) ─────────────────────────────

    @Test
    fun `모든 연도의 총 일수는 평년 353~355 또는 윤년 383~385`() {
        (1900..2100).forEach { year ->
            val days = LunarTable.yearDays(year)
            val hasLeap = LunarTable.leapMonth(year) != 0
            if (hasLeap) {
                assertTrue(days in 383..385, "${year}년(윤년) 총 일수 이상: $days")
            } else {
                assertTrue(days in 353..355, "${year}년(평년) 총 일수 이상: $days")
            }
        }
    }

    @Test
    fun `모든 연도의 설날은 양력 1월 21일 ~ 2월 21일 사이`() {
        val min = MonthDay.of(1, 21)
        val max = MonthDay.of(2, 21)
        (1900..2100).forEach { year ->
            val newYear = conv.lunarToSolar(LunarDate(year, 1, 1))
            val md = MonthDay.from(newYear)
            assertTrue(
                md in min..max,
                "${year}년 설날이 정상 범위를 벗어남: $newYear"
            )
        }
    }

    // ── 범위 경계 ───────────────────────────────────────────────────────

    @Test
    fun `지원 범위 이전 날짜는 예외`() {
        assertThrows<IllegalArgumentException> {
            conv.solarToLunar(LocalDate.of(1900, 1, 30))
        }
    }

    @Test
    fun `존재하지 않는 음력 일자는 예외 - 작은달 30일`() {
        // 2024년 음력 1월은 29일까지 (작은달)
        val monthLen = LunarTable.monthDays(2024, 1)
        if (monthLen == 29) {
            assertThrows<IllegalArgumentException> {
                conv.lunarToSolar(LunarDate(2024, 1, 30))
            }
        }
    }
}
