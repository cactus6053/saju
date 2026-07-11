package com.saju.domain.core

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class JiJiTest {

    @Test
    fun `지지는 12개`() {
        assertEquals(12, JiJi.entries.size)
    }

    @Test
    fun `자는 양 수`() {
        assertEquals(Element.WATER, JiJi.JA.element)
        assertEquals(YinYang.YANG, JiJi.JA.yinYang)
        assertEquals('子', JiJi.JA.hanja)
    }

    @Test
    fun `음양 분류 - 홀수 index가 음`() {
        JiJi.entries.forEach { ji ->
            val expected = if (ji.index % 2 == 0) YinYang.YANG else YinYang.YIN
            assertEquals(expected, ji.yinYang, "${ji.hangul} 음양 불일치")
        }
    }

    @Test
    fun `모든 지지는 장간을 1개 이상 포함`() {
        JiJi.entries.forEach { ji ->
            assertTrue(ji.janggan.isNotEmpty(), "${ji.hangul} 장간이 없음")
        }
    }

    @Test
    fun `장간 첫 번째는 주 기운 - 단기 지지 검증`() {
        assertEquals(CheonGan.GYE, JiJi.JA.janggan.first())   // 子 → 癸
        assertEquals(CheonGan.EUL, JiJi.MYO.janggan.first())  // 卯 → 乙
        assertEquals(CheonGan.SIN, JiJi.YU.janggan.first())   // 酉 → 辛
    }

    @Test
    fun `장간 수 - 단기(1개) 이기(2개) 삼기(3개) 지지 구분`() {
        // 단기: 子·卯·酉
        listOf(JiJi.JA, JiJi.MYO, JiJi.YU).forEach { ji ->
            assertEquals(1, ji.janggan.size, "${ji.hangul} 단기 장간 수 불일치")
        }
        // 이기: 午·亥
        listOf(JiJi.O, JiJi.HAE).forEach { ji ->
            assertEquals(2, ji.janggan.size, "${ji.hangul} 이기 장간 수 불일치")
        }
        // 삼기: 丑·寅·辰·巳·未·申·戌
        listOf(JiJi.CHUK, JiJi.IN, JiJi.JIN, JiJi.SA, JiJi.MI, JiJi.SHIN, JiJi.SUL).forEach { ji ->
            assertEquals(3, ji.janggan.size, "${ji.hangul} 삼기 장간 수 불일치")
        }
    }

    @Test
    fun `fromIndex - 12로 순환`() {
        assertSame(JiJi.JA, JiJi.fromIndex(0))
        assertSame(JiJi.JA, JiJi.fromIndex(12))
        assertSame(JiJi.JA, JiJi.fromIndex(60))
        assertSame(JiJi.HAE, JiJi.fromIndex(11))
        assertSame(JiJi.HAE, JiJi.fromIndex(59))
    }

    @Test
    fun `fromHanja - 한자로 조회`() {
        assertSame(JiJi.JA, JiJi.fromHanja('子'))
        assertSame(JiJi.HAE, JiJi.fromHanja('亥'))
        assertSame(JiJi.IN, JiJi.fromHanja('寅'))
    }
}
