package com.saju.domain.core

import com.saju.domain.core.JiJi.*

// 지지(地支) 간 합·충·형·파·해 관계 테이블
object JiRelation {

    // ── 육합(六合): 6쌍, 합화 오행 포함 ──────────────────────────────
    data class YukHap(val pair: Set<JiJi>, val resultElement: Element)

    val YUKHAP_LIST: List<YukHap> = listOf(
        YukHap(setOf(JA, CHUK), Element.EARTH),   // 子丑合土
        YukHap(setOf(IN, HAE), Element.WOOD),     // 寅亥合木
        YukHap(setOf(MYO, SUL), Element.FIRE),    // 卯戌合火
        YukHap(setOf(JIN, YU), Element.METAL),    // 辰酉合金
        YukHap(setOf(SA, SHIN), Element.WATER),   // 巳申合水
        YukHap(setOf(O, MI), Element.FIRE),       // 午未合火(태양)
    )

    fun yukHapOf(a: JiJi, b: JiJi): YukHap? =
        YUKHAP_LIST.firstOrNull { it.pair == setOf(a, b) }

    fun isYukHap(a: JiJi, b: JiJi): Boolean = yukHapOf(a, b) != null

    // ── 삼합(三合): 4국 ───────────────────────────────────────────────
    data class SamHap(val trio: Set<JiJi>, val resultElement: Element)

    val SAMHAP_LIST: List<SamHap> = listOf(
        SamHap(setOf(SHIN, JA, JIN), Element.WATER),  // 申子辰 水局
        SamHap(setOf(HAE, MYO, MI), Element.WOOD),    // 亥卯未 木局
        SamHap(setOf(IN, O, SUL), Element.FIRE),      // 寅午戌 火局
        SamHap(setOf(SA, YU, CHUK), Element.METAL),   // 巳酉丑 金局
    )

    fun samHapOf(a: JiJi, b: JiJi, c: JiJi): SamHap? =
        SAMHAP_LIST.firstOrNull { it.trio == setOf(a, b, c) }

    // 반합(半合): 삼합 중 왕지(子午卯酉)를 포함한 2지 조합
    fun banHapOf(a: JiJi, b: JiJi): SamHap? {
        val wangji = setOf(JA, O, MYO, YU)
        if (a !in wangji && b !in wangji) return null
        return SAMHAP_LIST.firstOrNull { setOf(a, b).all(it.trio::contains) && a != b }
    }

    // ── 방합(方合): 4국 (계절 방향) ──────────────────────────────────
    data class BangHap(val trio: Set<JiJi>, val resultElement: Element)

    val BANGHAP_LIST: List<BangHap> = listOf(
        BangHap(setOf(IN, MYO, JIN), Element.WOOD),   // 寅卯辰 東方木
        BangHap(setOf(SA, O, MI), Element.FIRE),      // 巳午未 南方火
        BangHap(setOf(SHIN, YU, SUL), Element.METAL), // 申酉戌 西方金
        BangHap(setOf(HAE, JA, CHUK), Element.WATER), // 亥子丑 北方水
    )

    fun bangHapOf(a: JiJi, b: JiJi, c: JiJi): BangHap? =
        BANGHAP_LIST.firstOrNull { it.trio == setOf(a, b, c) }

    // ── 육충(六沖): 6쌍 (지지 6칸 차이) ──────────────────────────────
    val CHUNG_LIST: List<Set<JiJi>> = listOf(
        setOf(JA, O),    // 子午沖
        setOf(CHUK, MI), // 丑未沖
        setOf(IN, SHIN), // 寅申沖
        setOf(MYO, YU),  // 卯酉沖
        setOf(JIN, SUL), // 辰戌沖
        setOf(SA, HAE),  // 巳亥沖
    )

    fun isChung(a: JiJi, b: JiJi): Boolean = CHUNG_LIST.any { it == setOf(a, b) }

    // ── 삼형(三刑) ────────────────────────────────────────────────────
    // 지세지형: 寅巳申 / 무은지형: 丑戌未 / 무례지형: 子卯 / 자형: 辰辰·午午·酉酉·亥亥
    val SAMHYEONG_TRIO_LIST: List<Set<JiJi>> = listOf(
        setOf(IN, SA, SHIN),   // 寅巳申
        setOf(CHUK, SUL, MI),  // 丑戌未
    )

    val SANGHYEONG_PAIR: Set<JiJi> = setOf(JA, MYO)  // 子卯刑

    val JAHYEONG_LIST: List<JiJi> = listOf(JIN, O, YU, HAE)  // 自刑

    fun isHyeong(a: JiJi, b: JiJi): Boolean = when {
        a == b -> a in JAHYEONG_LIST
        setOf(a, b) == SANGHYEONG_PAIR -> true
        else -> SAMHYEONG_TRIO_LIST.any { trio -> a in trio && b in trio }
    }

    // ── 육파(六破): 6쌍 ──────────────────────────────────────────────
    val PA_LIST: List<Set<JiJi>> = listOf(
        setOf(JA, YU),   // 子酉破
        setOf(CHUK, JIN),// 丑辰破
        setOf(IN, HAE),  // 寅亥破
        setOf(MYO, O),   // 卯午破
        setOf(SA, SHIN), // 巳申破
        setOf(MI, SUL),  // 未戌破
    )

    fun isPa(a: JiJi, b: JiJi): Boolean = PA_LIST.any { it == setOf(a, b) }

    // ── 육해(六害): 6쌍 ──────────────────────────────────────────────
    val HAE_LIST: List<Set<JiJi>> = listOf(
        setOf(JA, MI),   // 子未害
        setOf(CHUK, O),  // 丑午害
        setOf(IN, SA),   // 寅巳害
        setOf(MYO, JIN), // 卯辰害
        setOf(SHIN, HAE),// 申亥害
        setOf(YU, SUL),  // 酉戌害
    )

    fun isHae(a: JiJi, b: JiJi): Boolean = HAE_LIST.any { it == setOf(a, b) }
}
