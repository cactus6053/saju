package com.saju.engine

import com.saju.domain.core.CheonGan
import com.saju.domain.core.Gender
import com.saju.domain.core.JiJi
import com.saju.domain.core.SixtyGapja
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SajuCalculatorTest {

    private val calc = SajuCalculator()

    private fun input(
        year: Int, month: Int, day: Int, hour: Int, minute: Int = 0,
        calendarType: CalendarType = CalendarType.SOLAR,
        zasiMode: ZasiMode = ZasiMode.YAJASI_JEONGJASI,
    ) = BirthInput(
        year, month, day, hour, minute,
        calendarType = calendarType, gender = Gender.MALE, zasiMode = zasiMode,
    )

    // ── 수기 검산 완료된 전체 사주 케이스 ───────────────────────────────

    @ParameterizedTest(name = "{0}-{1}-{2} {3}:00 → {4}")
    @CsvSource(
        // 연말연시: 입춘·소한 이전 → 전년 계묘년 자월
        "2024, 1,  1, 12, 癸卯 甲子 甲子 庚午",
        // 연 중반 표준 케이스
        "2024, 6, 15, 12, 甲辰 庚午 庚戌 壬午",
        // 세기말: 정자시 (00:30)
        "2000, 1,  1,  0, 己卯 丙子 戊午 壬子",
        // 90년대 오전 출생
        "1995, 6, 15, 10, 乙亥 壬午 丁丑 乙巳",
        // 입춘 당일 낮 (절입 전) → 전년도
        "2024, 2,  4, 12, 癸卯 乙丑 戊戌 戊午",
        // UTC+8:30 시대 (보정 +30분, 12:00 → 12:30 오시 유지)
        "1955, 1, 15, 12, 甲午 丁丑 丙子 甲午",
    )
    fun `수기 검산 사주 팔자 검증`(
        year: Int, month: Int, day: Int, hour: Int, expected: String,
    ) {
        val result = calc.calculate(input(year, month, day, hour, minute = if (hour == 0) 30 else 0))
        assertEquals(expected, result.paljaHanja)
    }

    @Test
    fun `입춘 당일 밤 - 절입 후 새해 인월, 야자시`() {
        // 2024-02-04 23:00: 입춘(17:27) 이후 → 甲辰년 丙寅월
        // 야자시: 일주는 당일(戊戌), 시간은 익일 己亥일 기준 甲子시
        val result = calc.calculate(input(2024, 2, 4, 23, 0))
        assertEquals("甲辰 丙寅 戊戌 甲子", result.paljaHanja)
    }

    @Test
    fun `음력 입력 - 2024년 설날 정오`() {
        // 음력 2024-01-01 = 양력 2024-02-10 (입춘 후) → 갑진년 병인월 갑진일 경오시
        val result = calc.calculate(
            input(2024, 1, 1, 12, calendarType = CalendarType.LUNAR)
        )
        assertEquals("甲辰 丙寅 甲辰 庚午", result.paljaHanja)
    }

    // ── 자시 모드 통합 ──────────────────────────────────────────────────

    @Test
    fun `자시 모드 - 일주만 다르고 시주는 동일`() {
        val yajasi = calc.calculate(input(2024, 1, 1, 23, 30))
        val simple = calc.calculate(input(2024, 1, 1, 23, 30, zasiMode = ZasiMode.SIMPLE))

        assertEquals("甲子", yajasi.dayPillar.hanja, "야자시: 당일 일주")
        assertEquals("乙丑", simple.dayPillar.hanja, "단순자시: 익일 일주")
        assertEquals(yajasi.hourPillar, simple.hourPillar, "시주는 두 모드 동일")
        assertEquals("丙子", yajasi.hourPillar.hanja)
    }

    // ── 1900~2100 스윕 검증 (51개 연도 × 모듈 교차 검증) ────────────────

    @ParameterizedTest(name = "{0}년 6월 15일 정오 파이프라인 정합성")
    @ValueSource(ints = [
        1900, 1904, 1908, 1912, 1916, 1920, 1924, 1928, 1932, 1936,
        1940, 1944, 1948, 1952, 1956, 1960, 1964, 1968, 1972, 1976,
        1980, 1984, 1988, 1992, 1996, 2000, 2004, 2008, 2012, 2016,
        2020, 2024, 2028, 2032, 2036, 2040, 2044, 2048, 2052, 2056,
        2060, 2064, 2068, 2072, 2076, 2080, 2084, 2088, 2092, 2096, 2100,
    ])
    fun `연도 스윕 - 4주 상호 정합성 검증`(year: Int) {
        val result = calc.calculate(input(year, 6, 15, 12, 0))

        // 연주: 6월은 항상 입춘 후 → 당해 연주
        assertEquals(SixtyGapja.fromYear(year), result.yearPillar, "${year}년 연주")
        assertEquals(year, result.sajuYear)

        // 월지: 6월 15일은 항상 오월 (망종 6/5~6 후, 소서 7/6~8 전)
        assertEquals(JiJi.O, result.monthPillar.ji, "${year}년 6월 월지")

        // 월간: 오호둔년법 공식과 일치
        val expectedMonthGan = CheonGan.fromIndex(
            (result.yearPillar.gan.index % 5) * 2 + 2 + (JiJi.O.index - JiJi.IN.index)
        )
        assertEquals(expectedMonthGan, result.monthPillar.gan, "${year}년 월간")

        // 일주: 독립 모듈과 일치
        assertEquals(
            DayGanjiCalculator().calculate(LocalDate.of(year, 6, 15)),
            result.dayPillar, "${year}년 일주"
        )

        // 시간: 오서둔일법 공식과 일치 (정오 = 午時)
        val expectedHourGan = CheonGan.fromIndex(
            (result.dayPillar.gan.index % 5) * 2 + JiJi.O.index
        )
        assertEquals(expectedHourGan, result.hourPillar.gan, "${year}년 시간")
        assertEquals(JiJi.O, result.hourPillar.ji, "${year}년 시지")
    }

    // ── 구조 검증 ───────────────────────────────────────────────────────

    @Test
    fun `dayMaster는 일간`() {
        val result = calc.calculate(input(2024, 6, 15, 12))
        assertEquals(result.dayPillar.gan, result.dayMaster)
        assertEquals(CheonGan.GYEONG, result.dayMaster)
    }

    @Test
    fun `pillars는 연월일시 순서`() {
        val result = calc.calculate(input(2024, 6, 15, 12))
        assertEquals(
            listOf(result.yearPillar, result.monthPillar, result.dayPillar, result.hourPillar),
            result.pillars,
        )
    }

    @Test
    fun `한글 팔자 출력`() {
        val result = calc.calculate(input(2024, 6, 15, 12))
        assertEquals("갑진 경오 경술 임오", result.paljaHangul)
    }

    // ── 예외 전파 ───────────────────────────────────────────────────────

    @Test
    fun `잘못된 입력은 명확한 예외로 전파`() {
        val ex = assertThrows<IllegalArgumentException> {
            calc.calculate(input(1899, 6, 15, 12))
        }
        assertTrue(ex.message!!.contains("1899"))
    }

    // ── 성능 ────────────────────────────────────────────────────────────

    @Test
    fun `단일 계산 50ms 이내`() {
        // 워밍업 (JIT)
        repeat(50) { calc.calculate(input(2024, 6, 15, 12)) }

        val samples = (0 until 20).map {
            val start = System.nanoTime()
            calc.calculate(input(1990 + it, 6, 15, 12))
            (System.nanoTime() - start) / 1_000_000.0
        }
        val median = samples.sorted()[samples.size / 2]
        assertTrue(median < 50.0, "단일 계산 중앙값 ${median}ms — 50ms 초과")
    }
}
