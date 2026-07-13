package com.saju.reading

import com.saju.domain.core.Gender
import com.saju.engine.BirthInput
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@SpringBootTest
class SajuReadingServiceTest(
    @Autowired private val service: SajuReadingService,
    @Autowired private val repository: SajuReadingRepository,
) {

    @MockitoBean
    private lateinit var generator: ReadingGenerator

    private val input = BirthInput(1994, 10, 24, 12, 14, gender = Gender.FEMALE)

    // 모든 해석 종류의 섹션 키를 포함하는 범용 LLM 목 응답 — 어떤 스펙 검증도 통과
    private fun sectionsJson(): String {
        val keys = (ReadingSections.WONGUK + ReadingSections.DAEUN + ReadingSections.MARRIAGE +
            ReadingTopic.entries.flatMap { ReadingSections.yearly(it) }).map { it.key }.toSet()
        return "{" + keys.joinToString(",") { "\"$it\":\"$it 내용\"" } + "}"
    }

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
        given(generator.model).willReturn("claude-haiku-4-5")
    }

    @Test
    fun `캐시 미스 - LLM 1회 호출 후 DB 저장`() {
        given(generator.generate(anyString())).willReturn(sectionsJson())

        val result = service.getReading(input, 2026)

        // GENERAL 스펙 순서·제목·본문이 채워진다
        assertEquals(ReadingSections.yearly(ReadingTopic.GENERAL).map { it.key }, result.sections.map { it.key })
        assertEquals("원국 개관", result.sections.first().title)
        assertEquals("overview 내용", result.sections.first().body)
        assertFalse(result.cached)
        verify(generator, times(1)).generate(anyString())

        val saved = repository.findByCacheKey(result.cacheKey)
        assertEquals(sectionsJson(), saved?.content)
    }

    @Test
    fun `캐시 히트 - 두 번째 요청은 LLM 호출 없이 DB 반환`() {
        given(generator.generate(anyString())).willReturn(sectionsJson())

        val first = service.getReading(input, 2026)
        val second = service.getReading(input, 2026)

        assertFalse(first.cached)
        assertTrue(second.cached)
        assertEquals(first.sections, second.sections)
        assertEquals(first.cacheKey, second.cacheKey)
        // 총 1회만 호출
        verify(generator, times(1)).generate(anyString())
    }

    @Test
    fun `다른 연도는 다른 캐시 키`() {
        given(generator.generate(anyString())).willReturn(sectionsJson())

        val y2026 = service.getReading(input, 2026)
        val y2027 = service.getReading(input, 2027)

        assertNotEquals(y2026.cacheKey, y2027.cacheKey)
        verify(generator, times(2)).generate(anyString())
    }

    @Test
    fun `다른 출생 정보는 다른 캐시 키`() {
        given(generator.generate(anyString())).willReturn(sectionsJson())

        val a = service.getReading(input, 2026)
        val b = service.getReading(input.copy(hour = 6), 2026)

        assertNotEquals(a.cacheKey, b.cacheKey)
    }

    @Test
    fun `LLM 미구성 - 캐시 미스면 예외 전파, 저장 없음`() {
        given(generator.generate(anyString()))
            .willThrow(ReadingUnavailableException("LLM 미구성"))

        assertThrows<ReadingUnavailableException> {
            service.getReading(input, 2026)
        }
        assertEquals(0, repository.count())
    }

    @Test
    fun `LLM 미구성이어도 캐시가 있으면 정상 반환`() {
        given(generator.generate(anyString())).willReturn(sectionsJson())
        val first = service.getReading(input, 2026)

        // 이후 LLM이 죽어도 캐시는 동작
        given(generator.generate(anyString()))
            .willThrow(ReadingUnavailableException("LLM 미구성"))

        val second = service.getReading(input, 2026)
        assertTrue(second.cached)
        assertEquals(first.sections, second.sections)
    }

    @Test
    fun `3종 해석은 서로 다른 캐시 키`() {
        given(generator.generate(anyString())).willReturn(sectionsJson())

        val wonguk = service.getWongukReading(input)
        val daeun = service.getDaeunReading(input)
        val yearly = service.getReading(input, 2026)

        assertEquals(3, setOf(wonguk.cacheKey, daeun.cacheKey, yearly.cacheKey).size)
        verify(generator, times(3)).generate(anyString())
    }

    @Test
    fun `원국 풀이 캐싱 - 재요청 시 LLM 미호출`() {
        given(generator.generate(anyString())).willReturn(sectionsJson())

        val first = service.getWongukReading(input)
        val second = service.getWongukReading(input)

        assertFalse(first.cached)
        assertTrue(second.cached)
        verify(generator, times(1)).generate(anyString())
    }

    @Test
    fun `대운 풀이 캐싱 - 재요청 시 LLM 미호출`() {
        given(generator.generate(anyString())).willReturn(sectionsJson())

        val first = service.getDaeunReading(input)
        val second = service.getDaeunReading(input)

        assertFalse(first.cached)
        assertTrue(second.cached)
        verify(generator, times(1)).generate(anyString())
    }

    @Test
    fun `대운 풀이 - 성별이 다르면 다른 캐시 키 (순행 역행)`() {
        given(generator.generate(anyString())).willReturn(sectionsJson())

        val female = service.getDaeunReading(input)
        val male = service.getDaeunReading(input.copy(gender = Gender.MALE))

        assertNotEquals(female.cacheKey, male.cacheKey)
    }

    @Test
    fun `주제별 해석은 서로 다른 캐시 키 - 종합·금전·직장·건강·애정`() {
        given(generator.generate(anyString())).willReturn(sectionsJson())

        val keys = ReadingTopic.entries.map { topic ->
            service.getReading(input, 2026, topic).cacheKey
        }
        assertEquals(ReadingTopic.entries.size, keys.distinct().size)
    }

    @Test
    fun `주제별 해석도 캐싱 동작`() {
        given(generator.generate(anyString())).willReturn(sectionsJson())

        val first = service.getReading(input, 2026, ReadingTopic.MONEY)
        val second = service.getReading(input, 2026, ReadingTopic.MONEY)

        assertFalse(first.cached)
        assertTrue(second.cached)
        verify(generator, times(1)).generate(anyString())
    }

    @Test
    fun `결혼운 - 캐싱 동작 및 다른 해석과 키 분리`() {
        given(generator.generate(anyString())).willReturn(sectionsJson())

        val first = service.getMarriageReading(input)
        val second = service.getMarriageReading(input)

        assertFalse(first.cached)
        assertTrue(second.cached)
        verify(generator, times(1)).generate(anyString())

        given(generator.generate(anyString())).willReturn(sectionsJson())
        val yearly = service.getReading(input, 2026)
        assertNotEquals(first.cacheKey, yearly.cacheKey)
    }

    @Test
    fun `결혼운 - 성별에 따라 배우자성이 달라 키 분리`() {
        given(generator.generate(anyString())).willReturn(sectionsJson())

        val female = service.getMarriageReading(input)
        val male = service.getMarriageReading(input.copy(gender = Gender.MALE))

        assertNotEquals(female.cacheKey, male.cacheKey)
    }

    @Test
    fun `긴 해석 - 섹션 누락 응답은 재시도 후에도 실패하면 예외, 저장 안 됨`() {
        given(generator.generate(anyString())).willReturn("""{"dayMaster":"내용만 하나"}""")

        assertThrows<ReadingGenerationException> {
            service.getWongukReading(input)
        }
        verify(generator, times(2)).generate(anyString())
        assertEquals(0, repository.count())
    }

    @Test
    fun `긴 해석 - 잘못된 응답 후 재시도 성공 시 정상 반환`() {
        given(generator.generate(anyString()))
            .willReturn("JSON 아님")
            .willReturn(sectionsJson())

        val result = service.getWongukReading(input)

        assertEquals(ReadingSections.WONGUK.size, result.sections.size)
        verify(generator, times(2)).generate(anyString())
    }

    @Test
    fun `일일 운세 - 한 줄과 메시지 분리, 캐싱 동작`() {
        given(generator.generate(anyString()))
            .willReturn("오늘의 한 줄\n\n오늘의 메시지 첫 문장. 둘째 문장.")

        val today = java.time.LocalDate.now()
        val first = service.getDailyReading(input, today)
        val second = service.getDailyReading(input, today)

        assertEquals("오늘의 한 줄", first.oneLiner)
        assertEquals("오늘의 메시지 첫 문장. 둘째 문장.", first.message)
        assertEquals(today, first.ilun.date)
        assertFalse(first.cached)
        assertTrue(second.cached)
        verify(generator, times(1)).generate(anyString())
    }

    @Test
    fun `일일 운세 - LLM이 한 줄만 반환하면 메시지로 폴백`() {
        given(generator.generate(anyString())).willReturn("한 줄 운세만")

        val result = service.getDailyReading(input, java.time.LocalDate.now())

        assertEquals("한 줄 운세만", result.oneLiner)
        assertEquals("한 줄 운세만", result.message)
    }

    @Test
    fun `일일 운세 - 다른 날짜는 다른 캐시 키`() {
        given(generator.generate(anyString())).willReturn("한 줄\n\n메시지")

        val today = service.getDailyReading(input, java.time.LocalDate.now())
        val yesterday = service.getDailyReading(input, java.time.LocalDate.now().minusDays(1))

        assertNotEquals(today.cacheKey, yesterday.cacheKey)
    }

    @Test
    fun `일일 운세 - date 생략 시 오늘, 모레 이후는 거부`() {
        given(generator.generate(anyString())).willReturn("한 줄\n\n메시지")

        assertEquals(java.time.LocalDate.now(), service.getDailyReading(input, null).ilun.date)

        assertThrows<IllegalArgumentException> {
            service.getDailyReading(input, java.time.LocalDate.now().plusDays(2))
        }
        // 거부된 요청은 LLM을 호출하지 않음 (오늘 조회 1회만)
        verify(generator, times(1)).generate(anyString())
    }

    // ── 연간 운세 요약 ─────────────────────────────────────────────────

    private fun validSummaryJson(): String {
        val categories = com.saju.analysis.FortuneCategory.entries
            .joinToString(",") { "\"${it.name}\":\"${it.hangul} 요약\"" }
        val months = (1..12).joinToString(",") { "\"$it\":\"${it}월 요약\"" }
        return """{"categories":{$categories},"months":{$months}}"""
    }

    @Test
    fun `연간 요약 - 카테고리 5종과 월 12개 파싱, 캐싱 동작`() {
        given(generator.generate(anyString())).willReturn(validSummaryJson())

        val first = service.getYearlySummary(input, 2026)
        val second = service.getYearlySummary(input, 2026)

        assertEquals(5, first.categories.size)
        assertEquals(12, first.months.size)
        assertEquals("종합 요약", first.categories.first().summary)
        assertEquals("3월 요약", first.months[2].summary)
        first.categories.forEach { assertTrue(it.score in 1..5) }
        first.months.forEach { assertTrue(it.score in 1..5) }
        assertFalse(first.cached)
        assertTrue(second.cached)
        verify(generator, times(1)).generate(anyString())
    }

    @Test
    fun `연간 요약 - 코드펜스로 감싼 JSON도 파싱`() {
        given(generator.generate(anyString())).willReturn("```json\n${validSummaryJson()}\n```")

        val result = service.getYearlySummary(input, 2026)
        assertEquals(5, result.categories.size)
    }

    @Test
    fun `연간 요약 - 잘못된 응답은 1회 재시도 후 성공하면 정상 반환`() {
        given(generator.generate(anyString()))
            .willReturn("JSON이 아닌 응답")
            .willReturn(validSummaryJson())

        val result = service.getYearlySummary(input, 2026)

        assertEquals(12, result.months.size)
        verify(generator, times(2)).generate(anyString())
    }

    @Test
    fun `연간 요약 - 재시도도 실패하면 예외, 캐시에 저장 안 됨`() {
        given(generator.generate(anyString())).willReturn("""{"categories":{},"months":{}}""")

        assertThrows<ReadingGenerationException> {
            service.getYearlySummary(input, 2026)
        }
        verify(generator, times(2)).generate(anyString())
        assertEquals(0, repository.count())
    }

    // ── 다국어 ─────────────────────────────────────────────────────────

    @Test
    fun `lang별로 캐시 키가 분리된다`() {
        given(generator.generate(anyString())).willReturn(sectionsJson())

        val ko = service.getWongukReading(input, ReadingLanguage.KO)
        val en = service.getWongukReading(input, ReadingLanguage.EN)
        val ja = service.getWongukReading(input, ReadingLanguage.JA)

        assertEquals(3, setOf(ko.cacheKey, en.cacheKey, ja.cacheKey).size)
        verify(generator, times(3)).generate(anyString())
    }

    @Test
    fun `lang 기본값 ko는 명시적 ko와 같은 캐시 키 - 기존 캐시 호환`() {
        given(generator.generate(anyString())).willReturn(sectionsJson())

        val default = service.getWongukReading(input)
        val explicitKo = service.getWongukReading(input, ReadingLanguage.KO)

        assertEquals(default.cacheKey, explicitKo.cacheKey)
        verify(generator, times(1)).generate(anyString())
    }

    @Test
    fun `연간 요약도 lang별 캐시 분리, 검증은 언어 무관 동작`() {
        given(generator.generate(anyString())).willReturn(validSummaryJson())

        val ko = service.getYearlySummary(input, 2026)
        val en = service.getYearlySummary(input, 2026, ReadingLanguage.EN)

        assertNotEquals(ko.cacheKey, en.cacheKey)
        assertEquals(12, en.months.size)
    }

    @Test
    fun `저장 시 kind가 종류별로 기록된다`() {
        // 첫 호출(일일)은 텍스트 형식, 이후(원국·연도별)는 섹션 JSON
        given(generator.generate(anyString()))
            .willReturn("한 줄\n\n메시지", sectionsJson(), sectionsJson())

        val daily = service.getDailyReading(input, java.time.LocalDate.now())
        val wonguk = service.getWongukReading(input)
        val yearly = service.getReading(input, 2026)

        assertEquals(ReadingKind.DAILY, repository.findByCacheKey(daily.cacheKey)?.kind)
        assertEquals(ReadingKind.WONGUK, repository.findByCacheKey(wonguk.cacheKey)?.kind)
        assertEquals(ReadingKind.YEARLY, repository.findByCacheKey(yearly.cacheKey)?.kind)
    }

    @Test
    fun `프롬프트는 결정적 - 같은 입력이면 항상 같은 캐시 키`() {
        given(generator.generate(anyString())).willReturn(sectionsJson())

        val keys = (1..3).map {
            repository.deleteAll()
            service.getReading(input, 2026).cacheKey
        }
        assertEquals(1, keys.distinct().size)
    }
}
