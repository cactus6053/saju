package com.saju.domain.core

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GanRelationTest {

    @Test
    fun `천간합은 5쌍`() = assertEquals(5, GanRelation.HAP_LIST.size)

    @Test
    fun `천간합 - 갑기합토`() {
        val hap = GanRelation.hapOf(CheonGan.GAP, CheonGan.GI)
        assertEquals(Element.EARTH, hap?.resultElement)
    }

    @Test
    fun `천간합 - 순서 무관`() {
        assertEquals(
            GanRelation.hapOf(CheonGan.GAP, CheonGan.GI),
            GanRelation.hapOf(CheonGan.GI, CheonGan.GAP),
        )
    }

    @Test
    fun `천간합 - 5쌍 합화 오행 전체 검증`() {
        assertEquals(Element.EARTH, GanRelation.hapOf(CheonGan.GAP, CheonGan.GI)?.resultElement)
        assertEquals(Element.METAL, GanRelation.hapOf(CheonGan.EUL, CheonGan.GYEONG)?.resultElement)
        assertEquals(Element.WATER, GanRelation.hapOf(CheonGan.BYEONG, CheonGan.SIN)?.resultElement)
        assertEquals(Element.WOOD, GanRelation.hapOf(CheonGan.JEONG, CheonGan.IM)?.resultElement)
        assertEquals(Element.FIRE, GanRelation.hapOf(CheonGan.MU, CheonGan.GYE)?.resultElement)
    }

    @Test
    fun `천간합 - 합이 아닌 쌍은 null`() {
        assertNull(GanRelation.hapOf(CheonGan.GAP, CheonGan.EUL))
        assertNull(GanRelation.hapOf(CheonGan.GAP, CheonGan.GAP))
    }

    @Test
    fun `천간합 - 모든 천간은 정확히 하나의 합 상대를 가짐`() {
        CheonGan.entries.forEach { gan ->
            val partners = CheonGan.entries.filter { GanRelation.isHap(gan, it) }
            assertEquals(1, partners.size, "${gan.hangul}의 합 상대 수 불일치")
        }
    }

    @Test
    fun `천간합 - 합 상대는 5칸 차이 (양간+음간)`() {
        GanRelation.HAP_LIST.forEach { hap ->
            val (a, b) = hap.pair.toList()
            assertEquals(5, kotlin.math.abs(a.index - b.index), "${a.hangul}${b.hangul} 합")
            assertTrue(a.yinYang != b.yinYang, "합은 음양이 달라야 함")
        }
    }

    @Test
    fun `천간충은 4쌍 - 무기토는 충 없음`() {
        assertEquals(4, GanRelation.CHUNG_LIST.size)
        CheonGan.entries.forEach { other ->
            assertFalse(GanRelation.isChung(CheonGan.MU, other), "戊는 충이 없어야 함")
            assertFalse(GanRelation.isChung(CheonGan.GI, other), "己는 충이 없어야 함")
        }
    }

    @Test
    fun `천간충 - 갑경충, 을신충, 병임충, 정계충`() {
        assertTrue(GanRelation.isChung(CheonGan.GAP, CheonGan.GYEONG))
        assertTrue(GanRelation.isChung(CheonGan.EUL, CheonGan.SIN))
        assertTrue(GanRelation.isChung(CheonGan.BYEONG, CheonGan.IM))
        assertTrue(GanRelation.isChung(CheonGan.JEONG, CheonGan.GYE))
    }
}
