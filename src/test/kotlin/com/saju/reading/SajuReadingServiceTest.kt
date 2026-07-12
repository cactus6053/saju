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
    fun `프롬프트는 결정적 - 같은 입력이면 항상 같은 캐시 키`() {
        given(generator.generate(anyString())).willReturn("해석문")

        val keys = (1..3).map {
            repository.deleteAll()
            service.getReading(input, 2026).cacheKey
        }
        assertEquals(1, keys.distinct().size)
    }
}
