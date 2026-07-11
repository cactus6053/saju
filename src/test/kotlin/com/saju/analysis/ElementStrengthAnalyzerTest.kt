package com.saju.analysis

import com.saju.domain.core.Element
import com.saju.domain.core.Gender
import com.saju.domain.core.SixtyGapja
import com.saju.engine.BirthInput
import com.saju.engine.NormalizedBirth
import com.saju.engine.SajuCalculator
import com.saju.engine.SajuResult
import com.saju.engine.ZasiMode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ElementStrengthAnalyzerTest {

    private val analyzer = ElementStrengthAnalyzer()
    private val calculator = SajuCalculator()

    private fun realSaju(year: Int, month: Int, day: Int, hour: Int): SajuResult =
        calculator.calculate(BirthInput(year, month, day, hour, 0, gender = Gender.MALE))

    private fun syntheticSaju(year: String, month: String, day: String, hour: String): SajuResult {
        val dummy = NormalizedBirth(
            LocalDate.of(2000, 1, 1), LocalDateTime.of(2000, 1, 1, 12, 0),
            LocalDateTime.of(2000, 1, 1, 12, 0), Gender.MALE, ZasiMode.YAJASI_JEONGJASI,
        )
        return SajuResult(
            SixtyGapja.fromHanja(year), SixtyGapja.fromHanja(month),
            SixtyGapja.fromHanja(day), SixtyGapja.fromHanja(hour),
            2000, dummy,
        )
    }

    // ── 수기 검산 점수 케이스 1: 癸卯 甲子 甲子 庚午 (甲 일간) ──────────

    @Test
    fun `수기 검산 - 계묘 갑자 갑자 경오 오행 점수`() {
        val result = analyzer.analyze(realSaju(2024, 1, 1, 12))

        // 木: 甲(월간)1.0 + 甲(일간)1.0 + 卯(乙)1.0 = 3.0
        // 水: 癸(연간)1.0 + 子월(癸)2.0 + 子일(癸)1.0 = 4.0
        // 金: 庚 1.0 / 火: 午(丁)0.7 / 土: 午(己)0.3
        assertEquals(3.0, result.scores.getValue(Element.WOOD), 0.001)
        assertEquals(4.0, result.scores.getValue(Element.WATER), 0.001)
        assertEquals(1.0, result.scores.getValue(Element.METAL), 0.001)
        assertEquals(0.7, result.scores.getValue(Element.FIRE), 0.001)
        assertEquals(0.3, result.scores.getValue(Element.EARTH), 0.001)
    }

    @Test
    fun `계묘 갑자 갑자 경오 - 신강, 수목 과다, 토 결핍`() {
        val result = analyzer.analyze(realSaju(2024, 1, 1, 12))

        // 지원 세력: 木3.0 + 水4.0 - 일간1.0 = 6.0 >= 4.0 → 신강
        assertEquals(6.0, result.supportScore, 0.001)
        assertEquals(2.0, result.opposeScore, 0.001)
        assertTrue(result.isSinGang)
        assertTrue(result.deukRyeong)
        assertTrue(result.deukJi)
        assertTrue(result.deukSe)

        assertEquals(setOf(Element.WOOD, Element.WATER), result.excessive.toSet())
        assertEquals(listOf(Element.EARTH), result.deficient)
    }

    // ── 수기 검산 점수 케이스 2: 甲辰 庚午 庚戌 壬午 (庚 일간) ──────────

    @Test
    fun `수기 검산 - 갑진 경오 경술 임오 오행 점수`() {
        val result = analyzer.analyze(realSaju(2024, 6, 15, 12))

        // 金: 庚+庚+戌중기辛0.25 = 2.25
        // 木: 甲1.0 + 辰중기乙0.25 = 1.25
        // 水: 壬1.0 + 辰여기癸0.15 = 1.15
        // 火: 午월(丁0.7×2=1.4) + 戌여기丁0.15 + 午시(丁0.7) = 2.25
        // 土: 辰본기戊0.6 + 午월(己0.3×2=0.6) + 戌본기戊0.6 + 午시(己0.3) = 2.1
        assertEquals(2.25, result.scores.getValue(Element.METAL), 0.001)
        assertEquals(1.25, result.scores.getValue(Element.WOOD), 0.001)
        assertEquals(1.15, result.scores.getValue(Element.WATER), 0.001)
        assertEquals(2.25, result.scores.getValue(Element.FIRE), 0.001)
        assertEquals(2.1, result.scores.getValue(Element.EARTH), 0.001)
    }

    @Test
    fun `갑진 경오 경술 임오 - 신약, 과다 결핍 없음`() {
        val result = analyzer.analyze(realSaju(2024, 6, 15, 12))

        // 지원: 金2.25 + 土2.1 - 1.0 = 3.35 < 4.0 → 신약
        assertEquals(3.35, result.supportScore, 0.001)
        assertFalse(result.isSinGang)
        assertFalse(result.deukRyeong)
        assertTrue(result.deukJi)
        assertFalse(result.deukSe)

        assertTrue(result.excessive.isEmpty())
        assertTrue(result.deficient.isEmpty())
    }

    // ── 구조 불변식 ─────────────────────────────────────────────────────

    @Test
    fun `총점은 항상 9 - 60개 사주 스윕`() {
        (0 until 60).forEach { i ->
            val saju = realSaju(1970 + (i % 50), 1 + (i % 12), 1 + (i % 28), i % 24)
            val result = analyzer.analyze(saju)
            assertEquals(9.0, result.scores.values.sum(), 0.001, "총점 이상: ${saju.paljaHanja}")
        }
    }

    @Test
    fun `지원과 대립 세력의 합은 8 (일간 제외)`() {
        (0 until 30).forEach { i ->
            val saju = realSaju(1980 + i, 1 + (i % 12), 5 + (i % 20), (i * 3) % 24)
            val result = analyzer.analyze(saju)
            assertEquals(
                8.0, result.supportScore + result.opposeScore, 0.001,
                "세력 합 이상: ${saju.paljaHanja}",
            )
        }
    }

    @Test
    fun `격국 분석과 신강 판정 방향 참조 - 극단 케이스 일치`() {
        // 극단적으로 지원 세력이 강한 원국: 점수 기반과 카운트 기반 판정이 일치해야 함
        val strong = syntheticSaju("甲寅", "乙卯", "甲寅", "乙卯")
        assertTrue(analyzer.analyze(strong).isSinGang)

        // 일간 무근 원국
        val weak = syntheticSaju("戊戌", "戊戌", "甲戌", "戊辰")
        assertFalse(analyzer.analyze(weak).isSinGang)
    }

    @Test
    fun `오행이 전부 존재하는 원국은 결핍 없음`() {
        val result = analyzer.analyze(realSaju(2024, 6, 15, 12))
        Element.entries.forEach { element ->
            assertTrue(result.scores.getValue(element) > 0.0, "${element.hangul} 점수 0")
        }
    }

    @Test
    fun `한 오행 일색 원국 - 과다와 결핍 동시 검출`() {
        val result = analyzer.analyze(syntheticSaju("甲寅", "乙卯", "甲寅", "乙卯"))

        // 木 = 천간 4.0 + 지지 전체(寅甲본기·卯乙 단일 위주)
        assertTrue(result.scores.getValue(Element.WOOD) >= EXCESSIVE, "목 과다")
        assertTrue(Element.WOOD in result.excessive)
        assertTrue(Element.METAL in result.deficient, "금 결핍")
        assertTrue(Element.WATER in result.deficient, "수 결핍")
    }

    companion object {
        private const val EXCESSIVE = 3.0
    }
}
