package com.saju.engine

import com.saju.domain.core.Gender
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DaeunStartCalculatorTest {

    private val calculator = SajuCalculator()
    private val daeunCalc = DaeunStartCalculator()

    private fun saju(year: Int, month: Int, day: Int, hour: Int, gender: Gender): SajuResult =
        calculator.calculate(BirthInput(year, month, day, hour, 0, gender = gender))

    // ── 순행/역행 판정 4조합 ────────────────────────────────────────────

    @Test
    fun `양간 연주 + 남자 = 순행`() {
        // 2024 = 甲辰 (甲 양간)
        val result = daeunCalc.calculate(saju(2024, 6, 15, 12, Gender.MALE))
        assertEquals(DaeunStartCalculator.Direction.FORWARD, result.direction)
    }

    @Test
    fun `양간 연주 + 여자 = 역행`() {
        val result = daeunCalc.calculate(saju(2024, 6, 15, 12, Gender.FEMALE))
        assertEquals(DaeunStartCalculator.Direction.BACKWARD, result.direction)
    }

    @Test
    fun `음간 연주 + 남자 = 역행`() {
        // 2023 = 癸卯 (癸 음간)
        val result = daeunCalc.calculate(saju(2023, 6, 15, 12, Gender.MALE))
        assertEquals(DaeunStartCalculator.Direction.BACKWARD, result.direction)
    }

    @Test
    fun `음간 연주 + 여자 = 순행`() {
        val result = daeunCalc.calculate(saju(2023, 6, 15, 12, Gender.FEMALE))
        assertEquals(DaeunStartCalculator.Direction.FORWARD, result.direction)
    }

    @Test
    fun `입춘 전 출생은 전년도 연간 기준 - 2024년 1월생 남자는 음간 역행`() {
        // 2024-01-15는 사주 연도 2023 癸卯 → 음간 + 남자 = 역행
        val result = daeunCalc.calculate(saju(2024, 1, 15, 12, Gender.MALE))
        assertEquals(DaeunStartCalculator.Direction.BACKWARD, result.direction)
    }

    // ── 대운수 계산 ─────────────────────────────────────────────────────

    @Test
    fun `절입 직후 출생 역행 - 대운수 1`() {
        // 2024-06-06 12:00: 망종(6/5 13시경) 하루 뒤 → 역행 시 약 0.95일 → 대운수 1
        val result = daeunCalc.calculate(saju(2024, 6, 6, 12, Gender.FEMALE))
        assertEquals(DaeunStartCalculator.Direction.BACKWARD, result.direction)
        assertTrue(result.daysToJeol < 1.5, "역행 일수: ${result.daysToJeol}")
        assertEquals(1, result.daeunSu)
    }

    @Test
    fun `절입 직후 출생 순행 - 대운수 10`() {
        // 2024-06-06 12:00 → 다음 절 소서(7/6)까지 약 30.4일 → 대운수 10
        val result = daeunCalc.calculate(saju(2024, 6, 6, 12, Gender.MALE))
        assertEquals(DaeunStartCalculator.Direction.FORWARD, result.direction)
        assertTrue(result.daysToJeol > 29.0, "순행 일수: ${result.daysToJeol}")
        assertEquals(10, result.daeunSu)
    }

    @Test
    fun `절입 직전 출생 순행 - 대운수 1`() {
        // 2024-07-06 12:00: 소서(당일 밤) 직전 → 순행 시 반나절 → 대운수 1
        val result = daeunCalc.calculate(saju(2024, 7, 6, 12, Gender.MALE))
        assertTrue(result.daysToJeol < 1.5, "순행 일수: ${result.daysToJeol}")
        assertEquals(1, result.daeunSu)
    }

    @Test
    fun `월 중간 출생 - 일수와 대운수 정합`() {
        val result = daeunCalc.calculate(saju(2024, 6, 15, 12, Gender.MALE))
        // 6/15 정오 → 소서(7/6 밤)까지 약 21.5일 → 대운수 7
        assertTrue(abs(result.daysToJeol - 21.5) < 0.5, "일수: ${result.daysToJeol}")
        assertEquals(7, result.daeunSu)
    }

    @Test
    fun `순행과 역행의 일수 합은 절기 간격(약 30일)`() {
        val forward = daeunCalc.calculate(saju(2024, 6, 15, 12, Gender.MALE))
        val backward = daeunCalc.calculate(saju(2024, 6, 15, 12, Gender.FEMALE))
        val sum = forward.daysToJeol + backward.daysToJeol
        assertTrue(sum in 29.0..32.0, "절기 간격: $sum")
    }

    // ── 구조 불변식 ─────────────────────────────────────────────────────

    @Test
    fun `대운수는 항상 1~10 - 100개 케이스 스윕`() {
        (0 until 100).forEach { i ->
            val gender = if (i % 2 == 0) Gender.MALE else Gender.FEMALE
            val result = daeunCalc.calculate(
                saju(1950 + (i % 70), 1 + (i % 12), 1 + (i * 7) % 28, (i * 5) % 24, gender)
            )
            assertTrue(result.daeunSu in 1..10, "대운수 범위 초과: $result")
            assertTrue(result.daysToJeol >= 0.0, "일수 음수: $result")
        }
    }

    @Test
    fun `같은 출생의 남녀는 항상 반대 방향`() {
        (0 until 20).forEach { i ->
            val male = daeunCalc.calculate(saju(1980 + i, 3 + (i % 9), 10, 12, Gender.MALE))
            val female = daeunCalc.calculate(saju(1980 + i, 3 + (i % 9), 10, 12, Gender.FEMALE))
            assertTrue(male.direction != female.direction, "${1980 + i}년: 방향이 같음")
        }
    }
}
