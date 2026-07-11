package com.saju.domain.core

enum class JiJi(
    val hanja: Char,
    val hangul: String,
    val element: Element,
    val yinYang: YinYang,
    val janggan: List<CheonGan>,
) {
    JA('子', "자", Element.WATER, YinYang.YANG, listOf(CheonGan.GYE)),
    CHUK('丑', "축", Element.EARTH, YinYang.YIN, listOf(CheonGan.GI, CheonGan.GYE, CheonGan.SIN)),
    IN('寅', "인", Element.WOOD, YinYang.YANG, listOf(CheonGan.GAP, CheonGan.BYEONG, CheonGan.MU)),
    MYO('卯', "묘", Element.WOOD, YinYang.YIN, listOf(CheonGan.EUL)),
    JIN('辰', "진", Element.EARTH, YinYang.YANG, listOf(CheonGan.MU, CheonGan.EUL, CheonGan.GYE)),
    SA('巳', "사", Element.FIRE, YinYang.YIN, listOf(CheonGan.BYEONG, CheonGan.MU, CheonGan.GYEONG)),
    O('午', "오", Element.FIRE, YinYang.YANG, listOf(CheonGan.JEONG, CheonGan.GI)),
    MI('未', "미", Element.EARTH, YinYang.YIN, listOf(CheonGan.GI, CheonGan.JEONG, CheonGan.EUL)),
    SHIN('申', "신", Element.METAL, YinYang.YANG, listOf(CheonGan.GYEONG, CheonGan.IM, CheonGan.MU)),
    YU('酉', "유", Element.METAL, YinYang.YIN, listOf(CheonGan.SIN)),
    SUL('戌', "술", Element.EARTH, YinYang.YANG, listOf(CheonGan.MU, CheonGan.SIN, CheonGan.JEONG)),
    HAE('亥', "해", Element.WATER, YinYang.YIN, listOf(CheonGan.IM, CheonGan.GAP)),
    ;

    val index: Int get() = ordinal

    companion object {
        fun fromIndex(index: Int): JiJi = entries[index % 12]
        fun fromHanja(hanja: Char): JiJi = entries.first { it.hanja == hanja }
    }
}
