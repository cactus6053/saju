package com.saju.domain.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SixtyGapjaTest {

    @Test
    fun `60갑자 사이클은 정확히 60개`() {
        assertEquals(60, SixtyGapja.cycle.size)
    }

    @Test
    fun `첫 번째는 갑자(甲子)`() {
        val gapja = SixtyGapja.cycle.first()
        assertEquals(CheonGan.GAP, gapja.gan)
        assertEquals(JiJi.JA, gapja.ji)
        assertEquals("甲子", gapja.hanja)
    }

    @Test
    fun `마지막은 계해(癸亥)`() {
        val gyehae = SixtyGapja.cycle.last()
        assertEquals(CheonGan.GYE, gyehae.gan)
        assertEquals(JiJi.HAE, gyehae.ji)
    }

    @Test
    fun `60갑자 내 모든 간지가 유일`() {
        val distinct = SixtyGapja.cycle.distinct()
        assertEquals(60, distinct.size)
    }

    @Test
    fun `at - 음수 index 처리`() {
        assertEquals(SixtyGapja.at(0), SixtyGapja.at(-60))
        assertEquals(SixtyGapja.at(59), SixtyGapja.at(-1))
    }

    @Test
    fun `at - 60 초과 index 순환`() {
        assertEquals(SixtyGapja.at(0), SixtyGapja.at(60))
        assertEquals(SixtyGapja.at(1), SixtyGapja.at(61))
    }

    @Test
    fun `indexOf - at의 역연산`() {
        SixtyGapja.cycle.forEachIndexed { i, ganji ->
            assertEquals(i, SixtyGapja.indexOf(ganji), "${ganji.hanja} indexOf 불일치")
        }
    }

    @ParameterizedTest(name = "{0}년 = {1}")
    @CsvSource(
        "1984, 甲子",
        "1985, 乙丑",
        "2000, 庚辰",
        "2024, 甲辰",
        "2025, 乙巳",
        "1924, 甲子",
        "1900, 庚子",
    )
    fun `fromYear - 연도별 연주 검증`(year: Int, expectedHanja: String) {
        val ganji = SixtyGapja.fromYear(year)
        assertEquals(expectedHanja, ganji.hanja, "${year}년 연주 불일치")
    }

    @Test
    fun `fromHanja - 한자 문자열로 간지 조회`() {
        val gapja = SixtyGapja.fromHanja("甲子")
        assertEquals(CheonGan.GAP, gapja.gan)
        assertEquals(JiJi.JA, gapja.ji)
    }

    @Test
    fun `천간과 지지 조합 - 양간은 양지, 음간은 음지`() {
        SixtyGapja.cycle.forEach { ganji ->
            assertEquals(
                ganji.gan.yinYang,
                ganji.ji.yinYang,
                "${ganji.hanja}: 천간·지지 음양 불일치"
            )
        }
    }

    @Test
    fun `60년 주기 - 1984년과 2044년은 같은 간지`() {
        assertEquals(SixtyGapja.fromYear(1984), SixtyGapja.fromYear(2044))
    }

    @Test
    fun `60갑자 순서 - 갑자·갑술·갑신·갑오·갑진·갑인 위치 검증`() {
        assertEquals(0, SixtyGapja.indexOf(SixtyGapja.fromHanja("甲子")))
        assertEquals(10, SixtyGapja.indexOf(SixtyGapja.fromHanja("甲戌")))
        assertEquals(20, SixtyGapja.indexOf(SixtyGapja.fromHanja("甲申")))
        assertEquals(30, SixtyGapja.indexOf(SixtyGapja.fromHanja("甲午")))
        assertEquals(40, SixtyGapja.indexOf(SixtyGapja.fromHanja("甲辰")))
        assertEquals(50, SixtyGapja.indexOf(SixtyGapja.fromHanja("甲寅")))
    }
}
