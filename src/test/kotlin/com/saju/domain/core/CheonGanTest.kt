package com.saju.domain.core

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CheonGanTest {

    @Test
    fun `천간은 10개`() {
        assertEquals(10, CheonGan.entries.size)
    }

    @Test
    fun `갑은 양 목`() {
        assertEquals(Element.WOOD, CheonGan.GAP.element)
        assertEquals(YinYang.YANG, CheonGan.GAP.yinYang)
        assertEquals('甲', CheonGan.GAP.hanja)
    }

    @Test
    fun `음양 분류 - 홀수 index가 음`() {
        CheonGan.entries.forEach { gan ->
            val expected = if (gan.index % 2 == 0) YinYang.YANG else YinYang.YIN
            assertEquals(expected, gan.yinYang, "${gan.hangul} 음양 불일치")
        }
    }

    @Test
    fun `오행 분류 - 쌍으로 같은 오행`() {
        assertEquals(CheonGan.GAP.element, CheonGan.EUL.element)     // 목
        assertEquals(CheonGan.BYEONG.element, CheonGan.JEONG.element) // 화
        assertEquals(CheonGan.MU.element, CheonGan.GI.element)        // 토
        assertEquals(CheonGan.GYEONG.element, CheonGan.SIN.element)   // 금
        assertEquals(CheonGan.IM.element, CheonGan.GYE.element)       // 수
    }

    @Test
    fun `fromIndex - 10으로 순환`() {
        assertSame(CheonGan.GAP, CheonGan.fromIndex(0))
        assertSame(CheonGan.GAP, CheonGan.fromIndex(10))
        assertSame(CheonGan.GAP, CheonGan.fromIndex(60))
        assertSame(CheonGan.GYE, CheonGan.fromIndex(9))
        assertSame(CheonGan.GYE, CheonGan.fromIndex(59))
    }

    @Test
    fun `fromHanja - 한자로 조회`() {
        assertSame(CheonGan.GAP, CheonGan.fromHanja('甲'))
        assertSame(CheonGan.GYE, CheonGan.fromHanja('癸'))
        assertSame(CheonGan.BYEONG, CheonGan.fromHanja('丙'))
    }
}
