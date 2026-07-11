package com.saju.analysis

import com.saju.domain.core.Element
import com.saju.domain.core.Gender
import com.saju.domain.core.PillarPosition.*
import com.saju.domain.core.SipSeong
import com.saju.domain.core.SixtyGapja
import com.saju.engine.BirthInput
import com.saju.engine.SajuCalculator
import com.saju.engine.SajuResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeunCalculatorTest {

    private val seunCalc = SeunCalculator()

    private fun wonguk(): SajuResult = SajuCalculator().calculate(
        BirthInput(2024, 6, 15, 12, 0, gender = Gender.MALE)
    )

    // ── 세운 간지 ───────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0}년 세운 = {1}")
    @CsvSource(
        "1984, 甲子",
        "2000, 庚辰",
        "2024, 甲辰",
        "2025, 乙巳",
        "2026, 丙午",
        "1900, 庚子",
        "2100, 庚申",
    )
    fun `연도별 세운 간지`(year: Int, expected: String) {
        assertEquals(expected, seunCalc.ganJiOf(year).hanja)
    }

    @Test
    fun `범위 밖 연도는 예외`() {
        assertThrows<IllegalArgumentException> { seunCalc.ganJiOf(1899) }
        assertThrows<IllegalArgumentException> { seunCalc.ganJiOf(2101) }
    }

    // ── 십성 분석 ───────────────────────────────────────────────────────

    @Test
    fun `2025 세운 乙巳 - 庚 일간 기준 십성`() {
        val result = seunCalc.analyze(wonguk(), 2025)

        // 乙: 金克木 다른 음양 → 정재 / 巳 본기 丙: 火克金 다른 음양 → 편관
        assertEquals(SipSeong.JEONGJAE, result.ganSipSeong)
        assertEquals(SipSeong.PYEONGWAN, result.jiSipSeong)
    }

    // ── 원국과의 관계 ───────────────────────────────────────────────────

    @Test
    fun `2025 세운 乙巳와 원국 - 乙庚합금 2건만`() {
        val result = seunCalc.analyze(wonguk(), 2025)

        val ganHap = result.relationsWithWonguk.filter { it.type == RelationType.GAN_HAP }
        assertEquals(
            setOf(setOf(MONTH), setOf(DAY)),
            ganHap.map { it.positions }.toSet(),
        )
        assertTrue(ganHap.all { it.resultElement == Element.METAL })

        // 巳는 원국 辰午戌午와 아무 지지 관계 없음
        assertEquals(2, result.relationsWithWonguk.size, "예상 밖 관계: ${result.relationsWithWonguk}")
    }

    @Test
    fun `2030 세운 庚戌과 원국 - 辰戌충과 午戌 반합`() {
        val result = seunCalc.analyze(wonguk(), 2030)
        assertEquals("庚戌", result.ganJi.hanja)

        val chung = result.relationsWithWonguk.filter { it.type == RelationType.YUK_CHUNG }
        assertEquals(listOf(setOf(YEAR)), chung.map { it.positions }, "戌운-辰연지 충")

        val banhap = result.relationsWithWonguk.filter { it.type == RelationType.BAN_HAP }
        assertEquals(
            setOf(setOf(MONTH), setOf(HOUR)),
            banhap.map { it.positions }.toSet(),
            "戌운-午월지·午시지 반합",
        )
    }

    // ── 대운과의 관계 ───────────────────────────────────────────────────

    @Test
    fun `세운-대운 관계 - 乙巳 세운과 辛未 대운은 乙辛충`() {
        val daeun = SixtyGapja.fromHanja("辛未")
        val result = seunCalc.analyze(wonguk(), 2025, currentDaeun = daeun)

        assertEquals(
            listOf(RelationType.GAN_CHUNG),
            result.relationsWithDaeun.map { it.type },
        )
    }

    @Test
    fun `세운-대운 관계 - 甲辰 세운과 戊辰 대운은 辰辰 자형`() {
        val daeun = SixtyGapja.fromHanja("戊辰")
        val result = seunCalc.analyze(wonguk(), 2024, currentDaeun = daeun)

        assertEquals(
            listOf(RelationType.HYEONG),
            result.relationsWithDaeun.map { it.type },
        )
    }

    @Test
    fun `대운 미지정 시 대운 관계는 빈 목록`() {
        val result = seunCalc.analyze(wonguk(), 2025)
        assertTrue(result.relationsWithDaeun.isEmpty())
    }

    // ── 구조 불변식 ─────────────────────────────────────────────────────

    @Test
    fun `세운은 60년 주기로 반복`() {
        (1900..2040).forEach { year ->
            assertEquals(
                seunCalc.ganJiOf(year), seunCalc.ganJiOf(year + 60),
                "${year}년과 ${year + 60}년 세운 불일치",
            )
        }
    }
}
