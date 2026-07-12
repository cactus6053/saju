package com.saju.reading

// LLM 해석문 생성 추상화 — 테스트에서 페이크로 대체 가능
interface ReadingGenerator {
    val model: String

    // 캐시 미스 시 1회 호출. 미구성 시 ReadingUnavailableException.
    fun generate(prompt: String): String
}

class ReadingUnavailableException(message: String) : RuntimeException(message)
