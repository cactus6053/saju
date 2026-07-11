package com.saju.analysis

import com.saju.domain.core.Element
import com.saju.domain.core.Gender
import com.saju.domain.core.JiJi
import com.saju.domain.core.SixtyGapja
import com.saju.domain.core.controls
import com.saju.domain.core.generates
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GyeokGukAnalyzerTest {

    private val analyzer = GyeokGukAnalyzer()
    private val calculator = SajuCalculator()

    private fun realSaju(year: Int, month: Int, day: Int, hour: Int): SajuResult =
        calculator.calculate(BirthInput(year, month, day, hour, 0, gender = Gender.MALE))

    // 합성 원국 (종격·전왕격은 실제 날짜로 찾기 어려워 직접 구성)
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

    // ── 내격 판정 (실제 사주) ───────────────────────────────────────────

    @Test
    fun `정관격 - 갑진 경오 경술 임오 (庚 일간, 午월 丁 정관)`() {
        val result = analyzer.analyze(realSaju(2024, 6, 15, 12))
        assertEquals(GyeokGuk.JEONGGWAN_GYEOK, result.gyeokGuk)
        assertEquals(GyeokGukCategory.INNER, result.gyeokGuk.category)

        // 득지만 성립 → 신약 → 인성(土) 용신
        assertFalse(result.isSinGang)
        assertFalse(result.deukRyeong)
        assertTrue(result.deukJi)
        assertFalse(result.deukSe)
        assertEquals(Element.EARTH, result.yongsin)

        // 오월(여름) → 조후 수
        assertEquals(Element.WATER, result.johuYongsin)
    }

    @Test
    fun `정인격 신강 - 계묘 갑자 갑자 경오 (甲 일간, 子월 癸 정인)`() {
        val result = analyzer.analyze(realSaju(2024, 1, 1, 12))
        assertEquals(GyeokGuk.JEONGIN_GYEOK, result.gyeokGuk)

        // 득령·득지·득세 모두 성립 → 신강
        assertTrue(result.isSinGang)
        assertTrue(result.deukRyeong)
        assertTrue(result.deukJi)
        assertTrue(result.deukSe)

        // 인성(3) > 비겁(2) → 재성(土)으로 극인
        assertEquals(Element.EARTH, result.yongsin)

        // 자월(겨울) → 조후 화
        assertEquals(Element.FIRE, result.johuYongsin)
    }

    @Test
    fun `건록격 - 갑진 병인 갑진 경오 (甲 일간, 寅월 甲 비견)`() {
        val result = analyzer.analyze(realSaju(2024, 2, 10, 12))
        assertEquals(GyeokGuk.GEONROK, result.gyeokGuk)
        assertNull(result.johuYongsin, "봄생은 조후 용신 없음")
    }

    @Test
    fun `양인격 - 갑진 정묘 갑술 경오 (甲 일간, 卯월 乙 겁재)`() {
        val result = analyzer.analyze(realSaju(2024, 3, 11, 12))
        assertEquals(GyeokGuk.YANGIN, result.gyeokGuk)
    }

    // ── 외격: 종격 (합성 원국) ──────────────────────────────────────────

    @Test
    fun `종재격 - 甲 일간 무근, 재성(土) 일색`() {
        val result = analyzer.analyze(syntheticSaju("戊戌", "戊戌", "甲戌", "戊辰"))
        assertEquals(GyeokGuk.JONGJAE, result.gyeokGuk)
        assertEquals(GyeokGukCategory.OUTER, result.gyeokGuk.category)
        assertEquals(Element.EARTH, result.yongsin, "종재격은 재성 오행에 순응")
    }

    @Test
    fun `종살격 - 甲 일간 무근, 관성(金) 일색`() {
        val result = analyzer.analyze(syntheticSaju("庚申", "庚申", "甲申", "庚午"))
        assertEquals(GyeokGuk.JONGSAL, result.gyeokGuk)
        assertEquals(Element.METAL, result.yongsin)
    }

    @Test
    fun `종아격 - 甲 일간 무근, 식상(火) 일색`() {
        val result = analyzer.analyze(syntheticSaju("丙午", "丙午", "甲午", "丙午"))
        assertEquals(GyeokGuk.JONGA, result.gyeokGuk)
        assertEquals(Element.FIRE, result.yongsin)
    }

    // ── 외격: 전왕격 (합성 원국) ────────────────────────────────────────

    @Test
    fun `곡직격 - 甲 일간, 목 일색`() {
        val result = analyzer.analyze(syntheticSaju("甲寅", "乙卯", "甲寅", "乙卯"))
        assertEquals(GyeokGuk.GOKJIK, result.gyeokGuk)
        assertEquals(Element.WOOD, result.yongsin, "전왕격은 왕한 오행에 순응")
    }

    @Test
    fun `염상격 - 丙 일간, 화 일색`() {
        val result = analyzer.analyze(syntheticSaju("丙午", "丁巳", "丙午", "丁巳"))
        assertEquals(GyeokGuk.YEOMSANG, result.gyeokGuk)
        assertEquals(Element.FIRE, result.yongsin)
    }

    @Test
    fun `윤하격 - 壬 일간, 수 일색`() {
        val result = analyzer.analyze(syntheticSaju("壬子", "癸亥", "壬子", "癸亥"))
        assertEquals(GyeokGuk.YUNHA, result.gyeokGuk)
        assertEquals(Element.WATER, result.yongsin)
    }

    // ── 구조 불변식 (실제 사주 스윕) ────────────────────────────────────

    @Test
    fun `억부 용신 규칙 일관성 - 60개 실제 사주 스윕`() {
        (0 until 60).forEach { i ->
            val saju = realSaju(1970 + (i % 50), 1 + (i % 12), 1 + (i % 28), i % 24)
            val result = analyzer.analyze(saju)
            val dm = saju.dayMaster.element

            if (result.gyeokGuk.category == GyeokGukCategory.INNER) {
                if (result.isSinGang) {
                    assertTrue(
                        result.yongsin == Element.entries.first { it.controls == dm } ||
                            result.yongsin == dm.controls,
                        "신강 용신은 관성 또는 재성 오행: $saju → $result",
                    )
                } else {
                    assertEquals(
                        Element.entries.first { it.generates == dm }, result.yongsin,
                        "신약 용신은 인성 오행: ${saju.paljaHanja}",
                    )
                }
            }

            // 조후 규칙
            val expected = when (saju.monthPillar.ji) {
                JiJi.HAE, JiJi.JA, JiJi.CHUK -> Element.FIRE
                JiJi.SA, JiJi.O, JiJi.MI -> Element.WATER
                else -> null
            }
            assertEquals(expected, result.johuYongsin, "조후 불일치: ${saju.paljaHanja}")
        }
    }
}
