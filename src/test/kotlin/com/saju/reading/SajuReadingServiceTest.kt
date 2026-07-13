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

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
        given(generator.model).willReturn("claude-haiku-4-5")
    }

    @Test
    fun `캐시 미스 - LLM 1회 호출 후 DB 저장`() {
        given(generator.generate(anyString())).willReturn("생성된 해석문")

        val result = service.getReading(input, 2026)

        assertEquals("생성된 해석문", result.reading)
        assertFalse(result.cached)
        verify(generator, times(1)).generate(anyString())

        val saved = repository.findByCacheKey(result.cacheKey)
        assertEquals("생성된 해석문", saved?.content)
    }

    @Test
    fun `캐시 히트 - 두 번째 요청은 LLM 호출 없이 DB 반환`() {
        given(generator.generate(anyString())).willReturn("생성된 해석문")

        val first = service.getReading(input, 2026)
        val second = service.getReading(input, 2026)

        assertFalse(first.cached)
        assertTrue(second.cached)
        assertEquals(first.reading, second.reading)
        assertEquals(first.cacheKey, second.cacheKey)
        // 총 1회만 호출
        verify(generator, times(1)).generate(anyString())
    }

    @Test
    fun `다른 연도는 다른 캐시 키`() {
        given(generator.generate(anyString())).willReturn("해석문")

        val y2026 = service.getReading(input, 2026)
        val y2027 = service.getReading(input, 2027)

        assertNotEquals(y2026.cacheKey, y2027.cacheKey)
        verify(generator, times(2)).generate(anyString())
    }

    @Test
    fun `다른 출생 정보는 다른 캐시 키`() {
        given(generator.generate(anyString())).willReturn("해석문")

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
        given(generator.generate(anyString())).willReturn("해석문")
        val first = service.getReading(input, 2026)

        // 이후 LLM이 죽어도 캐시는 동작
        given(generator.generate(anyString()))
            .willThrow(ReadingUnavailableException("LLM 미구성"))

        val second = service.getReading(input, 2026)
        assertTrue(second.cached)
        assertEquals(first.reading, second.reading)
    }

    @Test
    fun `3종 해석은 서로 다른 캐시 키`() {
        given(generator.generate(anyString())).willReturn("해석문")

        val wonguk = service.getWongukReading(input)
        val daeun = service.getDaeunReading(input)
        val yearly = service.getReading(input, 2026)

        assertEquals(3, setOf(wonguk.cacheKey, daeun.cacheKey, yearly.cacheKey).size)
        verify(generator, times(3)).generate(anyString())
    }

    @Test
    fun `원국 풀이 캐싱 - 재요청 시 LLM 미호출`() {
        given(generator.generate(anyString())).willReturn("평생사주 해석")

        val first = service.getWongukReading(input)
        val second = service.getWongukReading(input)

        assertFalse(first.cached)
        assertTrue(second.cached)
        verify(generator, times(1)).generate(anyString())
    }

    @Test
    fun `대운 풀이 캐싱 - 재요청 시 LLM 미호출`() {
        given(generator.generate(anyString())).willReturn("대운 해석")

        val first = service.getDaeunReading(input)
        val second = service.getDaeunReading(input)

        assertFalse(first.cached)
        assertTrue(second.cached)
        verify(generator, times(1)).generate(anyString())
    }

    @Test
    fun `대운 풀이 - 성별이 다르면 다른 캐시 키 (순행 역행)`() {
        given(generator.generate(anyString())).willReturn("해석문")

        val female = service.getDaeunReading(input)
        val male = service.getDaeunReading(input.copy(gender = Gender.MALE))

        assertNotEquals(female.cacheKey, male.cacheKey)
    }

    @Test
    fun `주제별 해석은 서로 다른 캐시 키 - 종합·금전·직장·건강·애정`() {
        given(generator.generate(anyString())).willReturn("해석문")

        val keys = ReadingTopic.entries.map { topic ->
            service.getReading(input, 2026, topic).cacheKey
        }
        assertEquals(ReadingTopic.entries.size, keys.distinct().size)
    }

    @Test
    fun `주제별 해석도 캐싱 동작`() {
        given(generator.generate(anyString())).willReturn("금전운 해석")

        val first = service.getReading(input, 2026, ReadingTopic.MONEY)
        val second = service.getReading(input, 2026, ReadingTopic.MONEY)

        assertFalse(first.cached)
        assertTrue(second.cached)
        verify(generator, times(1)).generate(anyString())
    }

    @Test
    fun `결혼운 - 캐싱 동작 및 다른 해석과 키 분리`() {
        given(generator.generate(anyString())).willReturn("결혼운 해석")

        val first = service.getMarriageReading(input)
        val second = service.getMarriageReading(input)

        assertFalse(first.cached)
        assertTrue(second.cached)
        verify(generator, times(1)).generate(anyString())

        given(generator.generate(anyString())).willReturn("종합 해석")
        val yearly = service.getReading(input, 2026)
        assertNotEquals(first.cacheKey, yearly.cacheKey)
    }

    @Test
    fun `결혼운 - 성별에 따라 배우자성이 달라 키 분리`() {
        given(generator.generate(anyString())).willReturn("해석문")

        val female = service.getMarriageReading(input)
        val male = service.getMarriageReading(input.copy(gender = Gender.MALE))

        assertNotEquals(female.cacheKey, male.cacheKey)
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

    @Test
    fun `저장 시 kind가 종류별로 기록된다`() {
        given(generator.generate(anyString())).willReturn("한 줄\n\n메시지")

        val daily = service.getDailyReading(input, java.time.LocalDate.now())
        val wonguk = service.getWongukReading(input)
        val yearly = service.getReading(input, 2026)

        assertEquals(ReadingKind.DAILY, repository.findByCacheKey(daily.cacheKey)?.kind)
        assertEquals(ReadingKind.WONGUK, repository.findByCacheKey(wonguk.cacheKey)?.kind)
        assertEquals(ReadingKind.YEARLY, repository.findByCacheKey(yearly.cacheKey)?.kind)
    }

    @Test
    fun `프롬프트는 결정적 - 같은 입력이면 항상 같은 캐시 키`() {
        given(generator.generate(anyString())).willReturn("해석문")

        val keys = (1..3).map {
            repository.deleteAll()
            service.getReading(input, 2026).cacheKey
        }
        assertEquals(1, keys.distinct().size)
    }
}
