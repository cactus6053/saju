package com.saju.domain.core

enum class CheonGan(
    val hanja: Char,
    val hangul: String,
    val element: Element,
    val yinYang: YinYang,
) {
    GAP('甲', "갑", Element.WOOD, YinYang.YANG),
    EUL('乙', "을", Element.WOOD, YinYang.YIN),
    BYEONG('丙', "병", Element.FIRE, YinYang.YANG),
    JEONG('丁', "정", Element.FIRE, YinYang.YIN),
    MU('戊', "무", Element.EARTH, YinYang.YANG),
    GI('己', "기", Element.EARTH, YinYang.YIN),
    GYEONG('庚', "경", Element.METAL, YinYang.YANG),
    SIN('辛', "신", Element.METAL, YinYang.YIN),
    IM('壬', "임", Element.WATER, YinYang.YANG),
    GYE('癸', "계", Element.WATER, YinYang.YIN),
    ;

    val index: Int get() = ordinal

    companion object {
        fun fromIndex(index: Int): CheonGan = entries[index % 10]
        fun fromHanja(hanja: Char): CheonGan = entries.first { it.hanja == hanja }
    }
}
