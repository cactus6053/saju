package com.saju.api

import com.saju.reading.ReadingGenerator
import com.saju.reading.ReadingUnavailableException
import com.saju.reading.SajuReadingRepository
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class ReadingControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val repository: SajuReadingRepository,
) {

    @MockitoBean
    private lateinit var generator: ReadingGenerator

    private val body = """
        {
          "year": 1994, "month": 10, "day": 24, "hour": 12, "minute": 14,
          "gender": "FEMALE"
        }
    """.trimIndent()

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
        given(generator.model).willReturn("claude-haiku-4-5")
    }

    @Test
    fun `해석 생성 - 첫 요청은 cached false, 재요청은 true`() {
        given(generator.generate(anyString())).willReturn("2026년은 재성의 해입니다...")

        mockMvc.post("/api/v1/saju/reading/2026") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.reading") { value(containsString("재성의 해")) }
            jsonPath("$.model") { value("claude-haiku-4-5") }
            jsonPath("$.cached") { value(false) }
        }

        mockMvc.post("/api/v1/saju/reading/2026") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.cached") { value(true) }
        }
    }

    @Test
    fun `원국 풀이 - POST reading (연도 없음)`() {
        given(generator.generate(anyString())).willReturn("일간 계수의 평생사주는...")

        mockMvc.post("/api/v1/saju/reading") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.reading") { value(containsString("평생사주")) }
            jsonPath("$.cached") { value(false) }
        }
    }

    @Test
    fun `대운 풀이 - daeun 경로가 year 변수에 삼켜지지 않음`() {
        given(generator.generate(anyString())).willReturn("대운의 흐름은...")

        // /reading/daeun이 /reading/{year}로 매칭되면 Int 변환 실패로 400이 남 —
        // 200이면 라우팅이 정확함
        mockMvc.post("/api/v1/saju/reading/daeun") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.reading") { value(containsString("대운의 흐름")) }
        }
    }

    @Test
    fun `주제별 해석 - topic 파라미터`() {
        given(generator.generate(anyString())).willReturn("올해 재물의 흐름은...")

        mockMvc.post("/api/v1/saju/reading/2026?topic=money") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.reading") { value(containsString("재물의 흐름")) }
        }
    }

    @Test
    fun `잘못된 topic - 400과 사용 가능 값 안내`() {
        mockMvc.post("/api/v1/saju/reading/2026?topic=love") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value(containsString("love")) }
            jsonPath("$.message") { value(containsString("general")) }
        }
    }

    @Test
    fun `결혼운 - marriage 경로가 year 변수에 삼켜지지 않음`() {
        given(generator.generate(anyString())).willReturn("결혼 기운이 들어오는 시기는...")

        mockMvc.post("/api/v1/saju/reading/marriage") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.reading") { value(containsString("결혼 기운")) }
        }
    }

    @Test
    fun `LLM 미구성 + 캐시 미스 - 503과 안내 메시지`() {
        given(generator.generate(anyString()))
            .willThrow(ReadingUnavailableException("LLM 해석이 구성되지 않았습니다: ANTHROPIC_API_KEY 환경변수를 설정하세요"))

        mockMvc.post("/api/v1/saju/reading/2026") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isServiceUnavailable() }
            jsonPath("$.message") { value(containsString("ANTHROPIC_API_KEY")) }
        }
    }

    @Test
    fun `잘못된 출생 정보 - 400`() {
        mockMvc.post("/api/v1/saju/reading/2026") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"year": 1899, "month": 1, "day": 1, "hour": 0, "gender": "MALE"}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `출생 이전 연도 해석 - 400`() {
        mockMvc.post("/api/v1/saju/reading/1990") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
