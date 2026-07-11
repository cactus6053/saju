package com.saju.engine

import com.saju.domain.core.Jeolgi
import com.saju.domain.core.JiJi
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JeolgiCalculatorTest {

    private val calc = JeolgiCalculator()

    // ── Jeolgi 열거형 구조 검증 ──────────────────────────────────────────

    @Test
    fun `절기는 24개`() = assertEquals(24, Jeolgi.entries.size)

    @Test
    fun `절은 12개, 중기는 12개`() {
        assertEquals(12, Jeolgi.JEOL_LIST.size)
        assertEquals(12, Jeolgi.JUNGGI_LIST.size)
    }

    @Test
    fun `모든 절은 monthBranch를 가짐`() {
        Jeolgi.JEOL_LIST.forEach { j ->
            assertNotNull(j.monthBranch, "${j.hangul}의 monthBranch가 null")
        }
    }

    @Test
    fun `중기는 monthBranch가 null`() {
        Jeolgi.JUNGGI_LIST.forEach { j ->
            assertEquals(null, j.monthBranch, "${j.hangul}의 monthBranch가 null이어야 함")
        }
    }

    @Test
    fun `입춘은 인월(寅月) 시작`() = assertEquals(JiJi.IN, Jeolgi.IPCHUN.monthBranch)

    @Test
    fun `대설은 자월(子月) 시작`() = assertEquals(JiJi.JA, Jeolgi.DAESEOL.monthBranch)

    @Test
    fun `태양 황경 값이 0~360 범위`() {
        Jeolgi.entries.forEach { j ->
            assertTrue(j.solarLongitude in 0.0..360.0, "${j.hangul} 황경 범위 초과")
        }
    }

    // ── 태양 황경 계산 정확도 ─────────────────────────────────────────────

    @Test
    fun `계산된 시각의 태양 황경이 목표값과 일치`() {
        val toleranceDeg = 0.001  // 수렴 정밀도

        Jeolgi.entries.forEach { jeolgi ->
            val kst = calc.getMoment(2024, jeolgi)
            val utc = kst.minusHours(9)
            val jde = SolarLongitude.toJde(utc.year, utc.monthValue, utc.dayOfMonth,
                utc.hour + utc.minute / 60.0 + utc.second / 3600.0)
            val computed = SolarLongitude.at(jde)

            var diff = jeolgi.solarLongitude - computed
            if (diff > 180) diff -= 360
            if (diff < -180) diff += 360

            assertTrue(
                abs(diff) < toleranceDeg,
                "${jeolgi.hangul}: 황경 diff=${diff}° (목표=${jeolgi.solarLongitude}°, 계산=${computed}°)"
            )
        }
    }

    // ── 2024년 KASI 역서 검증 (허용 오차 ±30분) ─────────────────────────

    // 4대 중기(춘분·하지·추분·동지)는 USNO 기준으로 정확히 검증 — 허용 오차 ±30분
    @ParameterizedTest(name = "2024년 {0} = {1}-{2}-{3} {4}:{5} (±30분)")
    @CsvSource(
        "춘분, 2024, 3, 20, 12,  6",   // USNO: 2024-03-20 03:06 UTC = 12:06 KST
        "하지, 2024, 6, 21,  5, 51",   // USNO: 2024-06-20 20:51 UTC = 05:51 KST
        "추분, 2024, 9, 22, 21, 44",   // USNO: 2024-09-22 12:44 UTC = 21:44 KST
        "동지, 2024, 12, 21, 18, 21",  // USNO: 2024-12-21 09:21 UTC = 18:21 KST
    )
    fun `2024년 4대 중기 USNO 기준 검증`(
        hangul: String,
        year: Int, month: Int, day: Int, hour: Int, minute: Int,
    ) {
        val jeolgi = Jeolgi.entries.first { it.hangul == hangul }
        val computed = calc.getMoment(year, jeolgi)
        val expected = LocalDateTime.of(year, month, day, hour, minute)

        val diffMinutes = abs(java.time.Duration.between(expected, computed).toMinutes())
        assertTrue(diffMinutes <= 30, "${hangul}: 계산=${computed}, 기대=${expected}, 차이=${diffMinutes}분")
    }

    // 그 외 절기는 Meeus 저정밀도 알고리즘 한계(±90분)로 검증
    @ParameterizedTest(name = "2024년 {0} = {1}-{2}-{3} 근방 (±90분)")
    @CsvSource(
        "입춘, 2024, 2,  4, 17, 27",   // KASI 역서: 2024-02-04 17:27 KST
        "경칩, 2024, 3,  5, 11, 23",   // KASI 역서: 2024-03-05 11:23 KST
        "망종, 2024, 6,  5, 13, 10",
        "입추, 2024, 8,  7,  9,  9",
        "한로, 2024, 10,  8,  4,  0",
        "소한, 2024, 1,  6,  6, 49",   // CNAO(자금산): 2024-01-06 05:49 CST = 06:49 KST
    )
    fun `2024년 절기 시각 90분 이내 검증`(
        hangul: String,
        year: Int, month: Int, day: Int, hour: Int, minute: Int,
    ) {
        val jeolgi = Jeolgi.entries.first { it.hangul == hangul }
        val computed = calc.getMoment(year, jeolgi)
        val expected = LocalDateTime.of(year, month, day, hour, minute)

        val diffMinutes = abs(java.time.Duration.between(expected, computed).toMinutes())
        assertTrue(diffMinutes <= 90, "${hangul}: 계산=${computed}, 기대=${expected}, 차이=${diffMinutes}분")
    }

    // ── 연도별 순서 검증 ──────────────────────────────────────────────────

    @Test
    fun `같은 연도의 절기들은 시간 순서가 단조 증가`() {
        val moments = Jeolgi.entries
            .map { calc.getMoment(2024, it) }
            .zipWithNext()

        moments.forEach { (a, b) ->
            assertTrue(a.isBefore(b), "절기 시간 순서 역전: $a >= $b")
        }
    }

    @Test
    fun `연도가 달라도 같은 절기는 약 365일 차이`() {
        val ipchun2024 = calc.getMoment(2024, Jeolgi.IPCHUN)
        val ipchun2025 = calc.getMoment(2025, Jeolgi.IPCHUN)

        val days = java.time.Duration.between(ipchun2024, ipchun2025).toDays()
        assertTrue(days in 364..366, "연간 입춘 차이: ${days}일")
    }

    // ── 1900·2100년 경계 검증 ────────────────────────────────────────────

    @Test
    fun `1900년 절기 계산 가능`() {
        val moment = calc.getMoment(1900, Jeolgi.IPCHUN)
        assertEquals(1900, moment.year)
        assertEquals(2, moment.monthValue)
    }

    @Test
    fun `2100년 절기 계산 가능`() {
        val moment = calc.getMoment(2100, Jeolgi.DONGJI)
        assertEquals(2100, moment.year)
        assertEquals(12, moment.monthValue)
    }

    // ── nextJeolAfter 검증 ────────────────────────────────────────────────

    @Test
    fun `nextJeolAfter - 입춘 직전 JDE는 입춘을 다음 절로 반환`() {
        val ipchun2024 = calc.getMoment(2024, Jeolgi.IPCHUN)
        val utc = ipchun2024.minusHours(9).minusMinutes(30)
        val jde = SolarLongitude.toJde(utc.year, utc.monthValue, utc.dayOfMonth,
            utc.hour + utc.minute / 60.0)

        val (jeolgi, _) = calc.nextJeolAfter(jde)
        assertEquals(Jeolgi.IPCHUN, jeolgi)
    }
}
