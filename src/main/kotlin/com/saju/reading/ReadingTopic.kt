package com.saju.reading

// 연도별 해석의 주제 — 프롬프트에 포함되므로 캐시 키가 주제별로 자동 분리된다
enum class ReadingTopic(val hangul: String) {
    GENERAL("종합"),
    MONEY("금전운"),
    CAREER("직장운"),
    HEALTH("건강운"),
}
