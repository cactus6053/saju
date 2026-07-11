package com.saju.domain.core

// 천간합(天干合): 5쌍, 합화 오행 포함
object GanRelation {

    data class GanHap(val pair: Set<CheonGan>, val resultElement: Element)

    val HAP_LIST: List<GanHap> = listOf(
        GanHap(setOf(CheonGan.GAP, CheonGan.GI), Element.EARTH),      // 甲己合土
        GanHap(setOf(CheonGan.EUL, CheonGan.GYEONG), Element.METAL),  // 乙庚合金
        GanHap(setOf(CheonGan.BYEONG, CheonGan.SIN), Element.WATER),  // 丙辛合水
        GanHap(setOf(CheonGan.JEONG, CheonGan.IM), Element.WOOD),     // 丁壬合木
        GanHap(setOf(CheonGan.MU, CheonGan.GYE), Element.FIRE),       // 戊癸合火
    )

    fun hapOf(a: CheonGan, b: CheonGan): GanHap? =
        HAP_LIST.firstOrNull { it.pair == setOf(a, b) }

    fun isHap(a: CheonGan, b: CheonGan): Boolean = hapOf(a, b) != null

    // 천간충(天干沖): 4쌍 — 甲庚, 乙辛, 丙壬, 丁癸 (戊己는 충 없음)
    val CHUNG_LIST: List<Set<CheonGan>> = listOf(
        setOf(CheonGan.GAP, CheonGan.GYEONG),
        setOf(CheonGan.EUL, CheonGan.SIN),
        setOf(CheonGan.BYEONG, CheonGan.IM),
        setOf(CheonGan.JEONG, CheonGan.GYE),
    )

    fun isChung(a: CheonGan, b: CheonGan): Boolean =
        CHUNG_LIST.any { it == setOf(a, b) }
}
