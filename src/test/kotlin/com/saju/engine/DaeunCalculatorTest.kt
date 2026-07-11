package com.saju.engine

import com.saju.domain.core.Gender
import com.saju.domain.core.SixtyGapja
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DaeunCalculatorTest {

    private val calculator = SajuCalculator()
    private val daeunCalc = DaeunCalculator()

    private fun saju(year: Int, month: Int, day: Int, hour: Int, gender: Gender): SajuResult =
        calculator.calculate(BirthInput(year, month, day, hour, 0, gender = gender))

    // ── 순행 배열 ───────────────────────────────────────────────────────

    @Test
    fun `순행 - 월주 庚午 다음부터 순차 나열`() {
        // 2024-06-15 남자: 甲辰년(양간) → 순행, 월주 庚午, 대운수 7
        val result = daeunCalc.calculate(saju(2024, 6, 15, 12, Gender.MALE))

        assertEquals(DaeunStartCalculator.Direction.FORWARD, result.direction)
        assertEquals(7, result.daeunSu)
        assertEquals(
            listOf("辛未", "壬申", "癸酉", "甲戌", "乙亥", "丙子", "丁丑", "戊寅", "己卯", "庚辰"),
            result.daeunList.map { it.ganJi.hanja },
        )
    }

    @Test
    fun `순행 - 나이 구간은 대운수부터 10년 단위`() {
        val result = daeunCalc.calculate(saju(2024, 6, 15, 12, Gender.MALE))

        assertEquals(7, result.daeunList[0].startAge)
        assertEquals(16, result.daeunList[0].endAge)
        assertEquals(17, result.daeunList[1].startAge)
        assertEquals(97, result.daeunList[9].startAge)
        assertEquals(106, result.daeunList[9].endAge)
    }

    // ── 역행 배열 ───────────────────────────────────────────────────────

    @Test
    fun `역행 - 월주 庚午 이전부터 역순 나열`() {
        // 2024-06-15 여자: 양간 + 여자 → 역행
        val result = daeunCalc.calculate(saju(2024, 6, 15, 12, Gender.FEMALE))

        assertEquals(DaeunStartCalculator.Direction.BACKWARD, result.direction)
        assertEquals(
            listOf("己巳", "戊辰", "丁卯", "丙寅", "乙丑", "甲子", "癸亥", "壬戌", "辛酉", "庚申"),
            result.daeunList.map { it.ganJi.hanja },
        )
    }

    // ── 60갑자 순환 경계 ────────────────────────────────────────────────

    @Test
    fun `순행 - 60갑자 마지막 월주에서 처음으로 순환`() {
        // 癸亥(59) 월주 순행 → 甲子(0)부터
        // 1983-12-20: 癸亥년 → 甲子월? 확인: 癸년 甲寅 시작, 子월 offset 10 → 甲子월...
        // 대신 월주가 癸亥인 사주: 癸년 亥월 = 癸亥월 (offset 9 → 甲+9 = 癸)
        // 1983-11-15: 癸亥년 亥월 → 癸亥월
        val s = saju(1983, 11, 15, 12, Gender.MALE)
        assertEquals("癸亥", s.monthPillar.hanja, "전제: 월주가 癸亥")

        val result = daeunCalc.calculate(s)
        assertEquals(DaeunStartCalculator.Direction.BACKWARD, result.direction, "癸 음간 + 남자 = 역행")
        assertEquals("壬戌", result.daeunList[0].ganJi.hanja)

        // 여자는 순행 → 甲子부터
        val female = daeunCalc.calculate(saju(1983, 11, 15, 12, Gender.FEMALE))
        assertEquals("甲子", female.daeunList[0].ganJi.hanja, "癸亥 다음은 甲子 (순환)")
    }

    // ── at() 조회 ───────────────────────────────────────────────────────

    @Test
    fun `at - 나이로 현재 대운 조회`() {
        val result = daeunCalc.calculate(saju(2024, 6, 15, 12, Gender.MALE))

        assertNull(result.at(5), "대운수(7) 이전 나이는 null")
        assertEquals("辛未", result.at(7)?.ganJi?.hanja)
        assertEquals("辛未", result.at(16)?.ganJi?.hanja)
        assertEquals("壬申", result.at(17)?.ganJi?.hanja)
        assertEquals("庚辰", result.at(100)?.ganJi?.hanja)
        assertNull(result.at(107), "마지막 대운 이후는 null")
    }

    // ── 구조 불변식 ─────────────────────────────────────────────────────

    @Test
    fun `기본 10개 대운, 나이 구간 연속`() {
        (0 until 30).forEach { i ->
            val gender = if (i % 2 == 0) Gender.MALE else Gender.FEMALE
            val result = daeunCalc.calculate(saju(1960 + i * 2, 1 + (i % 12), 5 + (i % 23), i % 24, gender))

            assertEquals(10, result.daeunList.size)
            result.daeunList.zipWithNext().forEach { (a, b) ->
                assertEquals(a.endAge + 1, b.startAge, "나이 구간 불연속")
            }
        }
    }

    @Test
    fun `대운 간지는 60갑자 순서로 정확히 1씩 증감`() {
        val forward = daeunCalc.calculate(saju(2024, 6, 15, 12, Gender.MALE))
        forward.daeunList.zipWithNext().forEach { (a, b) ->
            assertEquals(
                SixtyGapja.at(SixtyGapja.indexOf(a.ganJi) + 1), b.ganJi,
                "순행 순서 오류: ${a.ganJi} → ${b.ganJi}",
            )
        }

        val backward = daeunCalc.calculate(saju(2024, 6, 15, 12, Gender.FEMALE))
        backward.daeunList.zipWithNext().forEach { (a, b) ->
            assertEquals(
                SixtyGapja.at(SixtyGapja.indexOf(a.ganJi) - 1), b.ganJi,
                "역행 순서 오류: ${a.ganJi} → ${b.ganJi}",
            )
        }
    }

    @Test
    fun `순행 첫 대운과 역행 첫 대운은 월주를 사이에 둠`() {
        val male = daeunCalc.calculate(saju(2024, 6, 15, 12, Gender.MALE))
        val female = daeunCalc.calculate(saju(2024, 6, 15, 12, Gender.FEMALE))
        val monthIndex = SixtyGapja.indexOf(saju(2024, 6, 15, 12, Gender.MALE).monthPillar)

        assertEquals(SixtyGapja.at(monthIndex + 1), male.daeunList[0].ganJi)
        assertEquals(SixtyGapja.at(monthIndex - 1), female.daeunList[0].ganJi)
    }

    @Test
    fun `개수 지정 - 12개 요청 시 12개 반환`() {
        val result = daeunCalc.calculate(saju(2024, 6, 15, 12, Gender.MALE), count = 12)
        assertEquals(12, result.daeunList.size)
    }

    @Test
    fun `개수 0 이하는 예외`() {
        assertThrows<IllegalArgumentException> {
            daeunCalc.calculate(saju(2024, 6, 15, 12, Gender.MALE), count = 0)
        }
    }
}
