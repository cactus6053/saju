package com.saju.domain.core

import com.saju.domain.core.JiJi.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JiRelationTest {

    // ── 육합 ─────────────────────────────────────────────────────────────

    @Test
    fun `육합은 6쌍, 모든 지지가 정확히 한 번씩 등장`() {
        assertEquals(6, JiRelation.YUKHAP_LIST.size)
        val all = JiRelation.YUKHAP_LIST.flatMap { it.pair }
        assertEquals(12, all.size)
        assertEquals(12, all.distinct().size)
    }

    @Test
    fun `육합 - 자축합토, 인해합목, 진유합금`() {
        assertEquals(Element.EARTH, JiRelation.yukHapOf(JA, CHUK)?.resultElement)
        assertEquals(Element.WOOD, JiRelation.yukHapOf(IN, HAE)?.resultElement)
        assertEquals(Element.METAL, JiRelation.yukHapOf(JIN, YU)?.resultElement)
    }

    @Test
    fun `육합 - 순서 무관, 비합 쌍은 null`() {
        assertEquals(JiRelation.yukHapOf(JA, CHUK), JiRelation.yukHapOf(CHUK, JA))
        assertNull(JiRelation.yukHapOf(JA, IN))
    }

    // ── 삼합 ─────────────────────────────────────────────────────────────

    @Test
    fun `삼합은 4국, 모든 지지가 정확히 한 번씩 등장`() {
        assertEquals(4, JiRelation.SAMHAP_LIST.size)
        val all = JiRelation.SAMHAP_LIST.flatMap { it.trio }
        assertEquals(12, all.distinct().size)
    }

    @Test
    fun `삼합 - 신자진 수국, 인오술 화국`() {
        assertEquals(Element.WATER, JiRelation.samHapOf(SHIN, JA, JIN)?.resultElement)
        assertEquals(Element.FIRE, JiRelation.samHapOf(IN, O, SUL)?.resultElement)
    }

    @Test
    fun `반합 - 왕지 포함 2지 조합만 인정`() {
        // 申子(왕지 子 포함) → 수국 반합
        assertEquals(Element.WATER, JiRelation.banHapOf(SHIN, JA)?.resultElement)
        // 申辰(왕지 없음) → 반합 아님
        assertNull(JiRelation.banHapOf(SHIN, JIN))
    }

    // ── 방합 ─────────────────────────────────────────────────────────────

    @Test
    fun `방합은 4국, 모든 지지가 정확히 한 번씩 등장`() {
        assertEquals(4, JiRelation.BANGHAP_LIST.size)
        val all = JiRelation.BANGHAP_LIST.flatMap { it.trio }
        assertEquals(12, all.distinct().size)
    }

    @Test
    fun `방합 - 인묘진 목국, 해자축 수국`() {
        assertEquals(Element.WOOD, JiRelation.bangHapOf(IN, MYO, JIN)?.resultElement)
        assertEquals(Element.WATER, JiRelation.bangHapOf(HAE, JA, CHUK)?.resultElement)
    }

    // ── 육충 ─────────────────────────────────────────────────────────────

    @Test
    fun `육충은 6쌍이며 모든 쌍이 지지 6칸 차이`() {
        assertEquals(6, JiRelation.CHUNG_LIST.size)
        JiRelation.CHUNG_LIST.forEach { pair ->
            val (a, b) = pair.toList()
            assertEquals(6, (b.index - a.index + 12) % 12, "${a.hangul}${b.hangul} 충 간격")
        }
    }

    @Test
    fun `육충 - 자오충, 인신충, 사해충`() {
        assertTrue(JiRelation.isChung(JA, O))
        assertTrue(JiRelation.isChung(IN, SHIN))
        assertTrue(JiRelation.isChung(SA, HAE))
        assertFalse(JiRelation.isChung(JA, CHUK))
    }

    // ── 형 ───────────────────────────────────────────────────────────────

    @Test
    fun `삼형 - 인사신, 축술미 상호 형`() {
        assertTrue(JiRelation.isHyeong(IN, SA))
        assertTrue(JiRelation.isHyeong(SA, SHIN))
        assertTrue(JiRelation.isHyeong(IN, SHIN))
        assertTrue(JiRelation.isHyeong(CHUK, SUL))
        assertTrue(JiRelation.isHyeong(SUL, MI))
        assertTrue(JiRelation.isHyeong(CHUK, MI))
    }

    @Test
    fun `상형 - 자묘형`() {
        assertTrue(JiRelation.isHyeong(JA, MYO))
        assertTrue(JiRelation.isHyeong(MYO, JA))
    }

    @Test
    fun `자형 - 진오유해는 자기 자신과 형`() {
        JiRelation.JAHYEONG_LIST.forEach { ji ->
            assertTrue(JiRelation.isHyeong(ji, ji), "${ji.hangul} 자형")
        }
        // 자형이 아닌 지지는 자기 자신과 형이 아님
        assertFalse(JiRelation.isHyeong(JA, JA))
        assertFalse(JiRelation.isHyeong(IN, IN))
    }

    @Test
    fun `형이 아닌 조합`() {
        assertFalse(JiRelation.isHyeong(JA, IN))
        assertFalse(JiRelation.isHyeong(O, YU))
    }

    // ── 파·해 ────────────────────────────────────────────────────────────

    @Test
    fun `육파는 6쌍, 모든 지지가 정확히 한 번씩 등장`() {
        assertEquals(6, JiRelation.PA_LIST.size)
        assertEquals(12, JiRelation.PA_LIST.flatten().distinct().size)
    }

    @Test
    fun `육파 - 자유파, 묘오파`() {
        assertTrue(JiRelation.isPa(JA, YU))
        assertTrue(JiRelation.isPa(MYO, O))
        assertFalse(JiRelation.isPa(JA, O))
    }

    @Test
    fun `육해는 6쌍, 모든 지지가 정확히 한 번씩 등장`() {
        assertEquals(6, JiRelation.HAE_LIST.size)
        assertEquals(12, JiRelation.HAE_LIST.flatten().distinct().size)
    }

    @Test
    fun `육해 - 자미해, 축오해`() {
        assertTrue(JiRelation.isHae(JA, MI))
        assertTrue(JiRelation.isHae(CHUK, O))
        assertFalse(JiRelation.isHae(JA, O))
    }

    // ── 교차 검증 ────────────────────────────────────────────────────────

    @Test
    fun `육합 쌍은 충 관계가 아님`() {
        JiRelation.YUKHAP_LIST.forEach { hap ->
            val (a, b) = hap.pair.toList()
            assertFalse(JiRelation.isChung(a, b), "${a.hangul}${b.hangul}: 합이면서 충일 수 없음")
        }
    }
}
