package com.saju.domain.core

enum class SinSal(val hangul: String, val hanja: String, val isGilsin: Boolean) {
    // 길신
    CHEONEUL_GWIIN("천을귀인", "天乙貴人", true),
    MUNCHANG_GWIIN("문창귀인", "文昌貴人", true),
    HAKDANG_GWIIN("학당귀인", "學堂貴人", true),
    CHEONDEOK_GWIIN("천덕귀인", "天德貴人", true),
    WOLDEOK_GWIIN("월덕귀인", "月德貴人", true),

    // 흉살
    YEOKMA("역마살", "驛馬殺", false),
    DOHWA("도화살", "桃花殺", false),
    BAEKHO("백호대살", "白虎大殺", false),
    YANGIN_SAL("양인살", "羊刃殺", false),
    GONGMANG("공망", "空亡", false),
    WONJIN("원진살", "怨嗔殺", false),   // 관계 갈등 — 일지 기준 판정
    GOSIN("고신살", "孤辰殺", false),    // 고독수 (남명 중심) — 연지 기준
    GWASUK("과숙살", "寡宿殺", false),   // 고독수 (여명 중심) — 연지 기준
}

enum class PillarPosition(val hangul: String) {
    YEAR("연주"), MONTH("월주"), DAY("일주"), HOUR("시주"),
}
