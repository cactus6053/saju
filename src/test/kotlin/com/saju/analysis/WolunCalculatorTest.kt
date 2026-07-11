package com.saju.analysis

import com.saju.domain.core.Gender
import com.saju.domain.core.JiJi
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WolunCalculatorTest {

    private val wolunCalc = WolunCalculator()

    private fun wonguk(): SajuResult = SajuCalculator().calculate(
        BirthInput(2024, 6, 15, 12, 0, gender = Gender.MALE)
    )

    // ── 월운 간지 ───────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0}년 {1}월 월운 = {2}")
    @CsvSource(
        "2024, 2,  丙寅",
        "2024, 6,  庚午",
        "2024, 12, 丙子",
        "2024, 1,  乙丑",  // 소한 이후 축월 (사주연도 2023 癸卯 기준)
        "2025, 1,  丁丑",  // 갑진년 축월
        "2025, 5,  辛巳",  // 을사년 사월
    )
    fun `연월별 월운 간지`(year: Int, month: Int, expected: String) {
        assertEquals(expected, wolunCalc.ganJiOf(year, month).hanja)
    }

    @Test
    fun `범위 밖 입력은 예외`() {
        assertThrows<IllegalArgumentException> { wolunCalc.ganJiOf(1899, 6) }
        assertThrows<IllegalArgumentException> { wolunCalc.ganJiOf(2024, 13) }
        assertThrows<IllegalArgumentException> { wolunCalc.ganJiOf(2024, 0) }
    }

    @Test
    fun `12개월 월운 월지는 12지지 전체`() {
        val branches = (1..12).map { wolunCalc.ganJiOf(2024, it).ji }.toSet()
        assertEquals(12, branches.size)
    }

    // ── 십성 ────────────────────────────────────────────────────────────

    @Test
    fun `2024년 6월 월운 庚午 - 庚 일간 기준 비견·정관`() {
        val result = wolunCalc.analyze(wonguk(), 2024, 6)
        assertEquals(SipSeong.BIGYEON, result.ganSipSeong)
        assertEquals(SipSeong.JEONGGWAN, result.jiSipSeong)
    }

    // ── 삼재 판정 ───────────────────────────────────────────────────────

    @ParameterizedTest(name = "辰생(신자진) {0}년 삼재 여부 = {1}")
    @CsvSource(
        "2022, true",   // 寅년 - 들삼재
        "2023, true",   // 卯년 - 눌삼재
        "2024, true",   // 辰년 - 날삼재
        "2025, false",  // 巳년
        "2021, false",  // 丑년
    )
    fun `신자진생 삼재 3년 판정`(year: Int, expected: Boolean) {
        assertEquals(expected, wolunCalc.isSamjae(JiJi.JIN, year))
    }

    @Test
    fun `4개 삼합군 삼재 시작년 검증`() {
        // 각 삼합군의 생지를 충하는 해부터 삼재 시작
        assertTrue(wolunCalc.isSamjae(JiJi.JA, 2022))   // 신자진생 → 寅년(2022)
        assertTrue(wolunCalc.isSamjae(JiJi.O, 2028))    // 인오술생 → 申년(2028)
        assertTrue(wolunCalc.isSamjae(JiJi.YU, 2031))   // 사유축생 → 亥년(2031)
        assertTrue(wolunCalc.isSamjae(JiJi.MYO, 2025))  // 해묘미생 → 巳년(2025)
    }

    @Test
    fun `원국 연지 辰 - 2024년 월운은 삼재 표시`() {
        val result = wolunCalc.analyze(wonguk(), 2024, 6)
        assertTrue(result.isSamjae)

        val notSamjae = wolunCalc.analyze(wonguk(), 2025, 6)
        assertFalse(notSamjae.isSamjae)
    }

    // ── 원국·세운·대운 관계 ─────────────────────────────────────────────

    @Test
    fun `2025년 5월 辛巳 월운 - 세운 乙巳와 乙辛충`() {
        val result = wolunCalc.analyze(wonguk(), 2025, 5)

        // 원국(甲庚庚壬 / 辰午戌午)과는 관계 없음
        assertTrue(result.relationsWithWonguk.isEmpty(), "원국 관계: ${result.relationsWithWonguk}")
        // 세운 乙巳와 천간충
        assertEquals(
            listOf(RelationType.GAN_CHUNG),
            result.relationsWithSeun.map { it.type },
        )
        assertEquals(WolunCalculator.GilHyung.BAD, result.gilHyung)
    }

    @Test
    fun `대운 지정 시 월운-대운 관계 포함`() {
        // 월운 丙子(2024-12) vs 대운 辛未: 丙辛합수, 子未해
        val result = wolunCalc.analyze(wonguk(), 2024, 12, currentDaeun = SixtyGapja.fromHanja("辛未"))
        val types = result.relationsWithDaeun.map { it.type }.toSet()
        assertTrue(RelationType.GAN_HAP in types, "丙辛합: $types")
        assertTrue(RelationType.HAE in types, "子未해: $types")
    }

    // ── 길흉 휴리스틱 일관성 ────────────────────────────────────────────

    @Test
    fun `길흉 판정은 관계 개수 집계와 일치 - 24개월 스윕`() {
        val saju = wonguk()
        val favorable = setOf(
            RelationType.GAN_HAP, RelationType.YUK_HAP,
            RelationType.SAM_HAP, RelationType.BAN_HAP, RelationType.BANG_HAP,
        )

        (1..24).forEach { i ->
            val year = 2024 + (i - 1) / 12
            val month = (i - 1) % 12 + 1
            val result = wolunCalc.analyze(saju, year, month)

            val all = result.relationsWithWonguk.map { it.type } +
                result.relationsWithSeun.map { it.type } +
                result.relationsWithDaeun.map { it.type }
            val fav = all.count { it in favorable }
            val unfav = all.size - fav
            val expected = when {
                fav > unfav -> WolunCalculator.GilHyung.GOOD
                unfav > fav -> WolunCalculator.GilHyung.BAD
                else -> WolunCalculator.GilHyung.NEUTRAL
            }
            assertEquals(expected, result.gilHyung, "$year-$month 길흉 불일치")
        }
    }
}
