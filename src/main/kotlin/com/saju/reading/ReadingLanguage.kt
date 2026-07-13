package com.saju.reading

// FE(SajuSays)가 지원하는 8개 언어와 동일한 화이트리스트
enum class ReadingLanguage(val code: String, val english: String) {
    KO("ko", "Korean"),
    EN("en", "English"),
    ES("es", "Spanish"),
    ZH("zh", "Simplified Chinese"),
    JA("ja", "Japanese"),
    TH("th", "Thai"),
    VI("vi", "Vietnamese"),
    MS("ms", "Malay"),
    ;

    companion object {
        fun of(code: String): ReadingLanguage =
            entries.find { it.code == code.lowercase() }
                ?: throw IllegalArgumentException(
                    "지원하지 않는 lang입니다: $code (사용 가능: ${entries.joinToString(", ") { it.code }})"
                )
    }
}
