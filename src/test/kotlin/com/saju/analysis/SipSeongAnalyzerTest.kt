package com.saju.analysis

import com.saju.domain.core.CheonGan
import com.saju.domain.core.Gender
import com.saju.domain.core.SipSeong
import com.saju.domain.core.SipSeongGroup
import com.saju.engine.BirthInput
import com.saju.engine.SajuCalculator
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SipSeongAnalyzerTest {

    private val analyzer = SipSeongAnalyzer()

    // ── 십성 판정 규칙: 양간 일간 (甲) ──────────────────────────────────

    @ParameterizedTest(name = "甲 일간 vs {0} = {1}")
    @CsvSource(
        "GAP, BIGYEON",      // 甲: 같은 오행 같은 음양 → 비견
        "EUL, GEOPJAE",      // 乙: 같은 오행 다른 음양 → 겁재
        "BYEONG, SIKSIN",    // 丙: 내가 생함(木→火) 같은 음양 → 식신
        "JEONG, SANGGWAN",   // 丁: 내가 생함 다른 음양 → 상관
        "MU, PYEONJAE",      // 戊: 내가 극함(木克土) 같은 음양 → 편재
        "GI, JEONGJAE",      // 己: 내가 극함 다른 음양 → 정재
        "GYEONG, PYEONGWAN", // 庚: 나를 극함(金克木) 같은 음양 → 편관
        "SIN, JEONGGWAN",    // 辛: 나를 극함 다른 음양 → 정관
        "IM, PYEONIN",       // 壬: 나를 생함(水生木) 같은 음양 → 편인
        "GYE, JEONGIN",      // 癸: 나를 생함 다른 음양 → 정인
    )
    fun `갑 일간의 십성 전체 판정`(target: CheonGan, expected: SipSeong) {
        assertEquals(expected, SipSeong.of(CheonGan.GAP, target))
    }

    // ── 십성 판정 규칙: 음간 일간 (癸) ──────────────────────────────────

    @ParameterizedTest(name = "癸 일간 vs {0} = {1}")
    @CsvSource(
        "GYE, BIGYEON",     // 癸: 비견
        "IM, GEOPJAE",      // 壬: 겁재
        "EUL, SIKSIN",      // 乙: 水生木, 둘 다 음 → 식신
        "GAP, SANGGWAN",    // 甲: 다른 음양 → 상관
        "JEONG, PYEONJAE",  // 丁: 水克火, 같은 음양 → 편재
        "BYEONG, JEONGJAE", // 丙: 다른 음양 → 정재
        "GI, PYEONGWAN",    // 己: 土克水, 같은 음양 → 편관
        "MU, JEONGGWAN",    // 戊: 다른 음양 → 정관
        "SIN, PYEONIN",     // 辛: 金生水, 같은 음양 → 편인
        "GYEONG, JEONGIN",  // 庚: 다른 음양 → 정인
    )
    fun `계 일간의 십성 전체 판정`(target: CheonGan, expected: SipSeong) {
        assertEquals(expected, SipSeong.of(CheonGan.GYE, target))
    }

    // ── 구조 불변식 ─────────────────────────────────────────────────────

    @Test
    fun `100개 조합 전체 - 예외 없이 판정되고 그룹 분류가 정확함`() {
        CheonGan.entries.forEach { dayMaster ->
            CheonGan.entries.forEach { target ->
                val sipSeong = SipSeong.of(dayMaster, target)
                val expectedGroup = when (sipSeong) {
                    SipSeong.BIGYEON, SipSeong.GEOPJAE -> SipSeongGroup.BIGEOP
                    SipSeong.SIKSIN, SipSeong.SANGGWAN -> SipSeongGroup.SIKSANG
                    SipSeong.PYEONJAE, SipSeong.JEONGJAE -> SipSeongGroup.JAESEONG
                    SipSeong.PYEONGWAN, SipSeong.JEONGGWAN -> SipSeongGroup.GWANSEONG
                    SipSeong.PYEONIN, SipSeong.JEONGIN -> SipSeongGroup.INSEONG
                }
                assertEquals(expectedGroup, sipSeong.group)
            }
        }
    }

    @Test
    fun `일간별로 10개 십성이 모두 한 번씩 등장`() {
        CheonGan.entries.forEach { dayMaster ->
            val all = CheonGan.entries.map { SipSeong.of(dayMaster, it) }
            assertEquals(10, all.distinct().size, "${dayMaster.hangul} 일간의 십성 분포 이상")
        }
    }

    // ── 사주 원국 전체 분석 ─────────────────────────────────────────────

    @Test
    fun `갑진 경오 경술 임오 - 원국 십성 맵 검증`() {
        // 2024-06-15 12:00, 일간 庚
        val saju = SajuCalculator().calculate(
            BirthInput(2024, 6, 15, 12, 0, gender = Gender.MALE)
        )
        val result = analyzer.analyze(saju)

        // 천간: 甲=편재, 庚=비견, (일간 자신 null), 壬=식신
        assertEquals(SipSeong.PYEONJAE, result.year.gan)
        assertEquals(SipSeong.BIGYEON, result.month.gan)
        assertNull(result.day.gan, "일간 자신은 십성 없음")
        assertEquals(SipSeong.SIKSIN, result.hour.gan)

        // 지지 본기: 辰(戊)=편인, 午(丁)=정관, 戌(戊)=편인, 午(丁)=정관
        assertEquals(SipSeong.PYEONIN, result.year.jiPrincipal)
        assertEquals(SipSeong.JEONGGWAN, result.month.jiPrincipal)
        assertEquals(SipSeong.PYEONIN, result.day.jiPrincipal)
        assertEquals(SipSeong.JEONGGWAN, result.hour.jiPrincipal)
    }

    @Test
    fun `장간 전체 십성 - 오화의 장간 정관과 정인`() {
        // 일간 庚 기준 午(丁·己): 丁=정관, 己=정인
        val saju = SajuCalculator().calculate(
            BirthInput(2024, 6, 15, 12, 0, gender = Gender.MALE)
        )
        val result = analyzer.analyze(saju)
        assertEquals(
            listOf(SipSeong.JEONGGWAN, SipSeong.JEONGIN),
            result.month.jiJanggan,
        )
    }

    @Test
    fun `장간 수가 지지 장간 수와 일치`() {
        val saju = SajuCalculator().calculate(
            BirthInput(1995, 6, 15, 10, 30, gender = Gender.FEMALE)
        )
        val result = analyzer.analyze(saju)

        assertEquals(saju.yearPillar.ji.janggan.size, result.year.jiJanggan.size)
        assertEquals(saju.monthPillar.ji.janggan.size, result.month.jiJanggan.size)
        assertEquals(saju.dayPillar.ji.janggan.size, result.day.jiJanggan.size)
        assertEquals(saju.hourPillar.ji.janggan.size, result.hour.jiJanggan.size)
    }
}
