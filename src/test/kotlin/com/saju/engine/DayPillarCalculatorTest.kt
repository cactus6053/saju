package com.saju.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

class DayPillarCalculatorTest {

    private val calc = DayPillarCalculator()

    // ── 기본 계산 (자시 아닌 시각) ──────────────────────────────────────

    @Test
    fun `낮 시간 출생은 벽시계 날짜의 일진`() {
        val result = calc.calculate(LocalDateTime.of(2024, 1, 1, 12, 0))
        assertEquals("甲子", result.ganJi.hanja)
        assertEquals(LocalDate.of(2024, 1, 1), result.effectiveDate)
    }

    @Test
    fun `밀레니엄 기준일 검증`() {
        val result = calc.calculate(LocalDateTime.of(2000, 1, 1, 12, 0))
        assertEquals("戊午", result.ganJi.hanja)
    }

    // ── 야자시/정자시 모드 (기본값) ─────────────────────────────────────

    @Test
    fun `야자시 모드 - 23시대는 당일 일주 유지`() {
        val result = calc.calculate(
            LocalDateTime.of(2024, 1, 1, 23, 30),
            ZasiMode.YAJASI_JEONGJASI,
        )
        assertEquals("甲子", result.ganJi.hanja, "야자시: 당일 유지")
        assertEquals(LocalDate.of(2024, 1, 1), result.effectiveDate)
    }

    @Test
    fun `야자시 모드 - 00시대는 당일 일주 (정자시)`() {
        val result = calc.calculate(
            LocalDateTime.of(2024, 1, 2, 0, 30),
            ZasiMode.YAJASI_JEONGJASI,
        )
        assertEquals("乙丑", result.ganJi.hanja, "정자시: 벽시계 날짜(1/2) 일주")
        assertEquals(LocalDate.of(2024, 1, 2), result.effectiveDate)
    }

    // ── 단순 자시 모드 ──────────────────────────────────────────────────

    @Test
    fun `단순 자시 - 23시부터 다음날 일주`() {
        val result = calc.calculate(
            LocalDateTime.of(2024, 1, 1, 23, 30),
            ZasiMode.SIMPLE,
        )
        assertEquals("乙丑", result.ganJi.hanja, "단순 자시: 다음날(1/2) 일주")
        assertEquals(LocalDate.of(2024, 1, 2), result.effectiveDate)
    }

    @Test
    fun `단순 자시 - 23시 정각도 다음날`() {
        val result = calc.calculate(LocalDateTime.of(2024, 1, 1, 23, 0), ZasiMode.SIMPLE)
        assertEquals(LocalDate.of(2024, 1, 2), result.effectiveDate)
    }

    @Test
    fun `단순 자시 - 22시 59분은 당일`() {
        val result = calc.calculate(LocalDateTime.of(2024, 1, 1, 22, 59), ZasiMode.SIMPLE)
        assertEquals(LocalDate.of(2024, 1, 1), result.effectiveDate)
        assertEquals("甲子", result.ganJi.hanja)
    }

    @Test
    fun `단순 자시 - 00시대는 당일 그대로`() {
        val result = calc.calculate(LocalDateTime.of(2024, 1, 2, 0, 30), ZasiMode.SIMPLE)
        assertEquals(LocalDate.of(2024, 1, 2), result.effectiveDate)
    }

    @Test
    fun `두 모드 비교 - 23시대만 다르고 나머지는 동일`() {
        (0..22).forEach { hour ->
            val kst = LocalDateTime.of(2024, 6, 15, hour, 30)
            assertEquals(
                calc.calculate(kst, ZasiMode.YAJASI_JEONGJASI).ganJi,
                calc.calculate(kst, ZasiMode.SIMPLE).ganJi,
                "${hour}시: 두 모드 결과가 같아야 함",
            )
        }

        val night = LocalDateTime.of(2024, 6, 15, 23, 30)
        val yajasi = calc.calculate(night, ZasiMode.YAJASI_JEONGJASI)
        val simple = calc.calculate(night, ZasiMode.SIMPLE)
        assertEquals(yajasi.effectiveDate.plusDays(1), simple.effectiveDate, "23시대: 하루 차이")
    }

    // ── 날짜 경계 넘김 ──────────────────────────────────────────────────

    @Test
    fun `단순 자시 - 월말 23시는 다음달 1일 일주`() {
        val result = calc.calculate(LocalDateTime.of(2024, 1, 31, 23, 30), ZasiMode.SIMPLE)
        assertEquals(LocalDate.of(2024, 2, 1), result.effectiveDate)
    }

    @Test
    fun `단순 자시 - 연말 23시는 다음해 1월 1일 일주`() {
        val result = calc.calculate(LocalDateTime.of(2024, 12, 31, 23, 30), ZasiMode.SIMPLE)
        assertEquals(LocalDate.of(2025, 1, 1), result.effectiveDate)
        // 2024-01-01(甲子, index 0) + 366일(윤년) = index 6 = 庚午
        assertEquals("庚午", result.ganJi.hanja)
    }

    @Test
    fun `단순 자시 - 윤일 경계`() {
        val result = calc.calculate(LocalDateTime.of(2024, 2, 28, 23, 30), ZasiMode.SIMPLE)
        assertEquals(LocalDate.of(2024, 2, 29), result.effectiveDate)
    }

    @Test
    fun `단순 자시 - 지원 범위 마지막 날 23시는 범위 초과 예외`() {
        assertThrows<IllegalArgumentException> {
            calc.calculate(LocalDateTime.of(2100, 12, 31, 23, 30), ZasiMode.SIMPLE)
        }
    }
}
