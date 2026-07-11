package com.saju.analysis

import com.saju.domain.core.Gender
import com.saju.domain.core.JiJi
import com.saju.domain.core.PillarPosition
import com.saju.domain.core.SinSal
import com.saju.domain.core.SixtyGapja
import com.saju.engine.BirthInput
import com.saju.engine.NormalizedBirth
import com.saju.engine.SajuCalculator
import com.saju.engine.SajuResult
import com.saju.engine.ZasiMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SinSalAnalyzerTest {

    private val analyzer = SinSalAnalyzer()

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

    private fun hits(saju: SajuResult, sinSal: SinSal): List<SinSalHit> =
        analyzer.analyze(saju).filter { it.sinSal == sinSal }

    // ── 천을귀인 ────────────────────────────────────────────────────────

    @Test
    fun `천을귀인 - 甲 일간에 丑 연지`() {
        val saju = syntheticSaju("乙丑", "戊寅", "甲子", "庚午")
        val found = hits(saju, SinSal.CHEONEUL_GWIIN)
        assertEquals(listOf(PillarPosition.YEAR), found.map { it.position })
    }

    @Test
    fun `천을귀인 - 辛 일간은 午와 寅`() {
        // 특수 케이스: 辛 → 午·寅
        val saju = syntheticSaju("庚寅", "甲午", "辛卯", "戊子")
        val found = hits(saju, SinSal.CHEONEUL_GWIIN)
        assertEquals(
            setOf(PillarPosition.YEAR, PillarPosition.MONTH),
            found.map { it.position }.toSet(),
        )
    }

    // ── 문창·학당귀인 ───────────────────────────────────────────────────

    @Test
    fun `문창귀인 - 甲 일간에 巳 시지`() {
        val saju = syntheticSaju("戊辰", "戊寅", "甲子", "己巳")
        val found = hits(saju, SinSal.MUNCHANG_GWIIN)
        assertEquals(listOf(PillarPosition.HOUR), found.map { it.position })
    }

    @Test
    fun `학당귀인 - 甲 일간에 亥 월지`() {
        val saju = syntheticSaju("戊辰", "丁亥", "甲子", "庚午")
        val found = hits(saju, SinSal.HAKDANG_GWIIN)
        assertEquals(listOf(PillarPosition.MONTH), found.map { it.position })
    }

    // ── 천덕·월덕귀인 ───────────────────────────────────────────────────

    @Test
    fun `월덕귀인 - 午월(화국)에 丙 연간`() {
        val saju = syntheticSaju("丙辰", "甲午", "庚戌", "戊寅")
        val found = hits(saju, SinSal.WOLDEOK_GWIIN)
        assertEquals(listOf(PillarPosition.YEAR), found.map { it.position })
    }

    @Test
    fun `천덕귀인 - 寅월에 丁 시간`() {
        val saju = syntheticSaju("戊辰", "甲寅", "庚戌", "丁丑")
        val found = hits(saju, SinSal.CHEONDEOK_GWIIN)
        assertTrue(PillarPosition.HOUR in found.map { it.position })
    }

    // ── 역마·도화 ───────────────────────────────────────────────────────

    @Test
    fun `역마살 - 일지 申(신자진)에 寅 연지`() {
        val saju = syntheticSaju("庚寅", "戊子", "壬申", "庚子")
        val found = hits(saju, SinSal.YEOKMA)
        assertTrue(PillarPosition.YEAR in found.map { it.position })
    }

    @Test
    fun `도화살 - 일지 午(인오술)에 卯 시지`() {
        val saju = syntheticSaju("戊辰", "戊子", "甲午", "丁卯")
        val found = hits(saju, SinSal.DOHWA)
        assertTrue(PillarPosition.HOUR in found.map { it.position })
    }

    // ── 백호대살 ────────────────────────────────────────────────────────

    @Test
    fun `백호대살 - 7개 간지 전체 검증`() {
        listOf("甲辰", "乙未", "丙戌", "丁丑", "戊辰", "壬戌", "癸丑").forEach { ganji ->
            val saju = syntheticSaju(ganji, "庚午", "庚子", "丙子")
            val found = hits(saju, SinSal.BAEKHO)
            assertTrue(
                PillarPosition.YEAR in found.map { it.position },
                "$ganji 연주가 백호대살이어야 함",
            )
        }
    }

    // ── 양인살 ──────────────────────────────────────────────────────────

    @Test
    fun `양인살 - 甲 일간에 卯 월지`() {
        val saju = syntheticSaju("戊辰", "丁卯", "甲子", "庚午")
        val found = hits(saju, SinSal.YANGIN_SAL)
        assertEquals(listOf(PillarPosition.MONTH), found.map { it.position })
    }

    @Test
    fun `양인살 - 음간 일간은 해당 없음`() {
        val saju = syntheticSaju("戊辰", "丁卯", "乙丑", "庚午")
        assertEquals(emptyList(), hits(saju, SinSal.YANGIN_SAL))
    }

    // ── 공망 ────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0}일 공망 = {1}·{2}")
    @CsvSource(
        "甲子, SUL, HAE",  // 갑자순
        "癸酉, SUL, HAE",  // 갑자순 마지막
        "甲戌, SHIN, YU",  // 갑술순
        "庚寅, O, MI",     // 갑신순
        "甲午, JIN, SA",   // 갑오순
        "甲辰, IN, MYO",   // 갑진순
        "甲寅, JA, CHUK",  // 갑인순
    )
    fun `순중공망 공식 검증`(dayGanji: String, gm1: JiJi, gm2: JiJi) {
        val index = SixtyGapja.indexOf(SixtyGapja.fromHanja(dayGanji))
        assertEquals(setOf(gm1, gm2), analyzer.gongmangOf(index))
    }

    @Test
    fun `공망 - 甲子일에 戌 연지와 亥 시지`() {
        val saju = syntheticSaju("庚戌", "戊寅", "甲子", "乙亥")
        val found = hits(saju, SinSal.GONGMANG)
        assertEquals(
            setOf(PillarPosition.YEAR, PillarPosition.HOUR),
            found.map { it.position }.toSet(),
        )
    }

    @Test
    fun `공망 - 일지 자신은 공망 대상 아님`() {
        // 甲戌일: 공망은 申酉인데 일지 戌은 판정 대상에서 제외되어야 함
        val saju = syntheticSaju("庚辰", "戊寅", "甲戌", "庚午")
        assertEquals(emptyList(), hits(saju, SinSal.GONGMANG))
    }

    // ── 실제 사주 통합 ──────────────────────────────────────────────────

    @Test
    fun `실제 사주 - 갑진 경오 경술 임오는 백호대살만 해당`() {
        val saju = SajuCalculator().calculate(
            BirthInput(2024, 6, 15, 12, 0, gender = Gender.MALE)
        )
        val all = analyzer.analyze(saju)
        assertEquals(
            listOf(SinSalHit(SinSal.BAEKHO, PillarPosition.YEAR)),
            all,
            "甲辰 연주 백호대살 단 1건이어야 함: $all",
        )
    }
}
