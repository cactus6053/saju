package com.saju.engine

import com.saju.domain.core.JiJi
import com.saju.domain.core.SixtyGapja
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime
import kotlin.test.assertEquals

class HourPillarCalculatorTest {

    private val calc = HourPillarCalculator()

    // ── 시지 매핑 ───────────────────────────────────────────────────────

    @Test
    fun `시지 - 24시간 전체 매핑 검증`() {
        val expected = mapOf(
            23 to JiJi.JA, 0 to JiJi.JA,
            1 to JiJi.CHUK, 2 to JiJi.CHUK,
            3 to JiJi.IN, 4 to JiJi.IN,
            5 to JiJi.MYO, 6 to JiJi.MYO,
            7 to JiJi.JIN, 8 to JiJi.JIN,
            9 to JiJi.SA, 10 to JiJi.SA,
            11 to JiJi.O, 12 to JiJi.O,
            13 to JiJi.MI, 14 to JiJi.MI,
            15 to JiJi.SHIN, 16 to JiJi.SHIN,
            17 to JiJi.YU, 18 to JiJi.YU,
            19 to JiJi.SUL, 20 to JiJi.SUL,
            21 to JiJi.HAE, 22 to JiJi.HAE,
        )
        expected.forEach { (hour, branch) ->
            assertEquals(branch, calc.branchOf(hour), "${hour}시 시지 불일치")
        }
    }

    @Test
    fun `시지 - 01시 경계에서 자시가 축시로 전환`() {
        assertEquals(JiJi.JA, calc.branchOf(0))
        assertEquals(JiJi.CHUK, calc.branchOf(1))
    }

    @Test
    fun `시지 - 범위 초과 예외`() {
        assertThrows<IllegalArgumentException> { calc.branchOf(24) }
        assertThrows<IllegalArgumentException> { calc.branchOf(-1) }
    }

    // ── 오서둔일법: 일간별 자시 시간 검증 ──────────────────────────────
    // 2024-01-01 = 甲子일부터 하루씩: 甲乙丙丁戊己庚辛壬癸 순서

    @ParameterizedTest(name = "{0}일(일간 {1}) 자시 = {2}")
    @CsvSource(
        "2024-01-01, 甲, 甲子",
        "2024-01-02, 乙, 丙子",
        "2024-01-03, 丙, 戊子",
        "2024-01-04, 丁, 庚子",
        "2024-01-05, 戊, 壬子",
        "2024-01-06, 己, 甲子",
        "2024-01-07, 庚, 丙子",
        "2024-01-08, 辛, 戊子",
        "2024-01-09, 壬, 庚子",
        "2024-01-10, 癸, 壬子",
    )
    fun `오서둔일법 - 일간별 자시 시간 검증`(dateStr: String, dayGan: String, expectedHanja: String) {
        // 00:30 = 정자시 (당일 일간 기준)
        val result = calc.calculate(LocalDateTime.parse("${dateStr}T00:30"))
        assertEquals(expectedHanja, result.hanja)
    }

    // ── 시간별 진행 검증 ────────────────────────────────────────────────

    @Test
    fun `甲일 낮 12시는 庚午시`() {
        // 甲일 자시 = 甲子, 午(offset 6) → 甲+6 = 庚午
        val result = calc.calculate(LocalDateTime.of(2024, 1, 1, 12, 0))
        assertEquals("庚午", result.hanja)
    }

    @Test
    fun `庚戌일 낮 12시는 壬午시`() {
        // 2024-06-15 = 庚戌일, 庚일 자시 = 丙子, 午 → 丙+6 = 壬午
        val result = calc.calculate(LocalDateTime.of(2024, 6, 15, 12, 0))
        assertEquals("壬午", result.hanja)
    }

    @Test
    fun `하루 12시진의 시간이 60갑자 순서로 연속`() {
        // 홀수 시(각 시진의 중심)로 12시진 순회: 01,03,...,23시
        val date = LocalDateTime.of(2024, 1, 1, 0, 0)
        val pillars = (0 until 12).map { i ->
            calc.calculate(date.plusHours(i * 2 + 1L))
        }
        pillars.zipWithNext().forEach { (a, b) ->
            assertEquals(SixtyGapja.at(SixtyGapja.indexOf(a) + 1), b, "${a.hanja} 다음 시주")
        }
    }

    // ── 자시 경계: 23시는 다음날 일간 기준 ──────────────────────────────

    @Test
    fun `23시 야자시와 다음날 00시 정자시는 같은 시주`() {
        // 둘 다 같은 자시에 속하므로 時干이 동일해야 함
        val yajasi = calc.calculate(LocalDateTime.of(2024, 1, 1, 23, 30))
        val jeongjasi = calc.calculate(LocalDateTime.of(2024, 1, 2, 0, 30))
        assertEquals(jeongjasi, yajasi, "야자시와 익일 정자시의 시주 일치")
    }

    @Test
    fun `23시 시주는 다음날 일간 기준 자시`() {
        // 2024-01-01 23:30 → 익일(乙丑일) 기준 자시 = 丙子
        val result = calc.calculate(LocalDateTime.of(2024, 1, 1, 23, 30))
        assertEquals("丙子", result.hanja)
    }

    @Test
    fun `22시 59분은 당일 일간 기준 해시`() {
        // 2024-01-01(甲子일) 22:59 → 亥時, 甲일 자시 甲子 + 11 = 乙亥
        val result = calc.calculate(LocalDateTime.of(2024, 1, 1, 22, 59))
        assertEquals("乙亥", result.hanja)
    }

    @Test
    fun `5일 주기 - 甲일과 己일의 시주 배열 동일`() {
        // 오서둔일법 특성
        val gapDay = calc.calculate(LocalDateTime.of(2024, 1, 1, 14, 0))  // 甲子일
        val giDay = calc.calculate(LocalDateTime.of(2024, 1, 6, 14, 0))   // 己巳일
        assertEquals(gapDay, giDay)
    }
}
