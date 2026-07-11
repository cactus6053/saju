package com.saju.analysis

import com.saju.domain.core.Element
import com.saju.domain.core.Gender
import com.saju.domain.core.PillarPosition.*
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
import kotlin.test.assertTrue

class RelationAnalyzerTest {

    private val analyzer = RelationAnalyzer()

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

    private fun List<PillarRelation>.of(type: RelationType) = filter { it.type == type }

    // ── 실제 사주 원국 매트릭스: 갑진 경오 경술 임오 ────────────────────

    @Test
    fun `갑진 경오 경술 임오 - 원국 관계 전체 검증`() {
        val saju = SajuCalculator().calculate(
            BirthInput(2024, 6, 15, 12, 0, gender = Gender.MALE)
        )
        val relations = analyzer.analyze(saju)

        // 천간충: 甲庚 (연-월), 甲庚 (연-일)
        assertEquals(
            setOf(setOf(YEAR, MONTH), setOf(YEAR, DAY)),
            relations.of(RelationType.GAN_CHUNG).map { it.positions }.toSet(),
        )

        // 육충: 辰戌 (연-일)
        assertEquals(
            listOf(setOf(YEAR, DAY)),
            relations.of(RelationType.YUK_CHUNG).map { it.positions },
        )

        // 반합: 午戌 (월-일), 戌午 (일-시) — 화국
        val banhap = relations.of(RelationType.BAN_HAP)
        assertEquals(
            setOf(setOf(MONTH, DAY), setOf(DAY, HOUR)),
            banhap.map { it.positions }.toSet(),
        )
        assertTrue(banhap.all { it.resultElement == Element.FIRE })

        // 자형: 午午 (월-시)
        assertEquals(
            listOf(setOf(MONTH, HOUR)),
            relations.of(RelationType.HYEONG).map { it.positions },
        )

        // 천간합·육합·삼합·방합·파·해 없음
        assertTrue(relations.of(RelationType.GAN_HAP).isEmpty())
        assertTrue(relations.of(RelationType.YUK_HAP).isEmpty())
        assertTrue(relations.of(RelationType.SAM_HAP).isEmpty())
        assertTrue(relations.of(RelationType.PA).isEmpty())
        assertTrue(relations.of(RelationType.HAE).isEmpty())
    }

    // ── 삼형살 케이스 ───────────────────────────────────────────────────

    @Test
    fun `지세지형 - 寅巳申 삼형 3쌍 모두 검출`() {
        val saju = syntheticSaju("丙寅", "乙巳", "甲申", "庚午")
        val hyeong = analyzer.analyze(saju).of(RelationType.HYEONG)

        assertEquals(
            setOf(setOf(YEAR, MONTH), setOf(MONTH, DAY), setOf(YEAR, DAY)),
            hyeong.map { it.positions }.toSet(),
        )
    }

    @Test
    fun `무은지형 - 丑戌未 삼형과 충·파 중첩 검출`() {
        val saju = syntheticSaju("丁丑", "甲戌", "乙未", "丙子")
        val relations = analyzer.analyze(saju)

        // 형 3쌍: 丑戌, 戌未, 丑未
        assertEquals(
            setOf(setOf(YEAR, MONTH), setOf(MONTH, DAY), setOf(YEAR, DAY)),
            relations.of(RelationType.HYEONG).map { it.positions }.toSet(),
        )
        // 丑未는 충이기도 함
        assertTrue(setOf(YEAR, DAY) in relations.of(RelationType.YUK_CHUNG).map { it.positions })
        // 戌未는 파이기도 함
        assertTrue(setOf(MONTH, DAY) in relations.of(RelationType.PA).map { it.positions })
    }

    @Test
    fun `자형 - 辰辰 검출`() {
        val saju = syntheticSaju("戊辰", "丙辰", "庚子", "丙子")
        val hyeong = analyzer.analyze(saju).of(RelationType.HYEONG)
        assertTrue(setOf(YEAR, MONTH) in hyeong.map { it.positions })
    }

    // ── 합 계열 ─────────────────────────────────────────────────────────

    @Test
    fun `천간합 - 甲己合土 검출`() {
        val saju = syntheticSaju("甲子", "己巳", "庚辰", "丙子")
        val hap = analyzer.analyze(saju).of(RelationType.GAN_HAP)
        assertEquals(1, hap.size)
        assertEquals(setOf(YEAR, MONTH), hap[0].positions)
        assertEquals(Element.EARTH, hap[0].resultElement)
    }

    @Test
    fun `육합 - 子丑合土 검출`() {
        val saju = syntheticSaju("甲子", "丁丑", "庚辰", "丙戌")
        val yukhap = analyzer.analyze(saju).of(RelationType.YUK_HAP)
        assertEquals(setOf(YEAR, MONTH), yukhap[0].positions)
        assertEquals(Element.EARTH, yukhap[0].resultElement)
    }

    @Test
    fun `삼합 - 申子辰 수국 트리오 검출`() {
        val saju = syntheticSaju("甲申", "丙子", "庚辰", "丁丑")
        val samhap = analyzer.analyze(saju).of(RelationType.SAM_HAP)
        assertEquals(1, samhap.size)
        assertEquals(setOf(YEAR, MONTH, DAY), samhap[0].positions)
        assertEquals(Element.WATER, samhap[0].resultElement)
    }

    @Test
    fun `방합 - 寅卯辰 목국 트리오 검출`() {
        val saju = syntheticSaju("丙寅", "丁卯", "庚辰", "丁丑")
        val banghap = analyzer.analyze(saju).of(RelationType.BANG_HAP)
        assertEquals(setOf(YEAR, MONTH, DAY), banghap[0].positions)
        assertEquals(Element.WOOD, banghap[0].resultElement)
    }

    // ── 운(運)과의 교차 판정 ────────────────────────────────────────────

    @Test
    fun `운 교차 - 乙丑 운과 갑진 경오 경술 임오`() {
        val saju = SajuCalculator().calculate(
            BirthInput(2024, 6, 15, 12, 0, gender = Gender.MALE)
        )
        val unRelations = analyzer.analyzeWithUn(saju, SixtyGapja.fromHanja("乙丑"))

        fun of(type: RelationType) = unRelations.filter { it.type == type }

        // 乙庚합금: 월간·일간 庚 → 2건
        assertEquals(
            setOf(setOf(MONTH), setOf(DAY)),
            of(RelationType.GAN_HAP).map { it.positions }.toSet(),
        )
        assertTrue(of(RelationType.GAN_HAP).all { it.resultElement == Element.METAL })

        // 丑辰파(연), 丑午해(월·시), 丑戌형(일)
        assertEquals(listOf(setOf(YEAR)), of(RelationType.PA).map { it.positions })
        assertEquals(
            setOf(setOf(MONTH), setOf(HOUR)),
            of(RelationType.HAE).map { it.positions }.toSet(),
        )
        assertEquals(listOf(setOf(DAY)), of(RelationType.HYEONG).map { it.positions })
    }

    @Test
    fun `운이 삼합을 완성 - 원국 寅戌에 운 午`() {
        val saju = syntheticSaju("丙寅", "庚戌", "庚子", "丁丑")
        val unRelations = analyzer.analyzeWithUn(saju, SixtyGapja.fromHanja("甲午"))

        val samhap = unRelations.filter { it.type == RelationType.SAM_HAP }
        assertEquals(1, samhap.size)
        assertEquals(setOf(YEAR, MONTH), samhap[0].positions)
        assertEquals(Element.FIRE, samhap[0].resultElement)
    }

    @Test
    fun `운 충 - 甲午 운과 子 일지`() {
        val saju = syntheticSaju("丙寅", "庚戌", "庚子", "丁丑")
        val unRelations = analyzer.analyzeWithUn(saju, SixtyGapja.fromHanja("甲午"))
        assertTrue(
            setOf(DAY) in unRelations.filter { it.type == RelationType.YUK_CHUNG }.map { it.positions },
            "午운은 子 일지와 충",
        )
    }

    // ── 구조 검증 ───────────────────────────────────────────────────────

    @Test
    fun `원국 매트릭스 완전성 - 甲子 庚辰 戊寅 壬戌의 정확한 관계 집합`() {
        val saju = syntheticSaju("甲子", "庚辰", "戊寅", "壬戌")
        val relations = analyzer.analyze(saju)

        // 존재하는 관계: 甲庚충(연-월), 子辰반합 수국(연-월), 辰戌충(월-시) — 이게 전부
        assertEquals(3, relations.size, "예상 밖 관계 검출: $relations")
        assertEquals(
            listOf(setOf(YEAR, MONTH)),
            relations.of(RelationType.GAN_CHUNG).map { it.positions },
        )
        val banhap = relations.of(RelationType.BAN_HAP)
        assertEquals(listOf(setOf(YEAR, MONTH)), banhap.map { it.positions })
        assertEquals(Element.WATER, banhap[0].resultElement)
        assertEquals(
            listOf(setOf(MONTH, HOUR)),
            relations.of(RelationType.YUK_CHUNG).map { it.positions },
        )
    }
}
