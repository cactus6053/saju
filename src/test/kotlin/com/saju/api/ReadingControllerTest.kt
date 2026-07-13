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

    // 모든 해석 종류의 섹션 키를 포함하는 범용 LLM 목 응답 — 어떤 스펙 검증도 통과
    private val sectionsJson = run {
        val keys = (com.saju.reading.ReadingSections.WONGUK +
            com.saju.reading.ReadingSections.DAEUN +
            com.saju.reading.ReadingSections.MARRIAGE +
            com.saju.reading.ReadingTopic.entries.flatMap { com.saju.reading.ReadingSections.yearly(it) })
            .map { it.key }.toSet()
        "{" + keys.joinToString(",") { "\"$it\":\"$it 내용\"" } + "}"
    }

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
        given(generator.model).willReturn("claude-haiku-4-5")
    }

    @Test
    fun `해석 생성 - 섹션 구조 반환, 첫 요청은 cached false, 재요청은 true`() {
        given(generator.generate(anyString())).willReturn(sectionsJson)

        mockMvc.post("/api/v1/saju/reading/2026") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.sections.length()") { value(5) }
            jsonPath("$.sections[0].key") { value("overview") }
            jsonPath("$.sections[0].title") { value("원국 개관") }
            jsonPath("$.sections[0].body") { value(containsString("overview 내용")) }
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
        given(generator.generate(anyString())).willReturn(sectionsJson)

        mockMvc.post("/api/v1/saju/reading") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.sections[0].key") { value("dayMaster") }
            jsonPath("$.cached") { value(false) }
        }
    }

    @Test
    fun `대운 풀이 - daeun 경로가 year 변수에 삼켜지지 않음`() {
        given(generator.generate(anyString())).willReturn(sectionsJson)

        // /reading/daeun이 /reading/{year}로 매칭되면 Int 변환 실패로 400이 남 —
        // 200이면 라우팅이 정확함
        mockMvc.post("/api/v1/saju/reading/daeun") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.sections[0].key") { value("direction") }
        }
    }

    @Test
    fun `주제별 해석 - topic 파라미터`() {
        given(generator.generate(anyString())).willReturn(sectionsJson)

        mockMvc.post("/api/v1/saju/reading/2026?topic=money") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.sections[0].key") { value("capacity") }
            jsonPath("$.sections[0].title") { value("타고난 재물 그릇") }
        }
    }

    @Test
    fun `애정운 해석 - topic=love`() {
        given(generator.generate(anyString())).willReturn(sectionsJson)

        mockMvc.post("/api/v1/saju/reading/2026?topic=love") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.sections[0].key") { value("style") }
        }
    }

    @Test
    fun `lang 파라미터 - 지원 언어는 200`() {
        given(generator.generate(anyString())).willReturn(sectionsJson)

        mockMvc.post("/api/v1/saju/reading?lang=en") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.sections.length()") { value(5) }
        }
    }

    @Test
    fun `미지원 lang - 400과 사용 가능 값 안내`() {
        mockMvc.post("/api/v1/saju/reading?lang=fr") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value(containsString("fr")) }
            jsonPath("$.message") { value(containsString("ko")) }
        }
    }

    @Test
    fun `잘못된 topic - 400과 사용 가능 값 안내`() {
        mockMvc.post("/api/v1/saju/reading/2026?topic=luck") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value(containsString("luck")) }
            jsonPath("$.message") { value(containsString("general")) }
        }
    }

    @Test
    fun `결혼운 - marriage 경로가 year 변수에 삼켜지지 않음`() {
        given(generator.generate(anyString())).willReturn(sectionsJson)

        mockMvc.post("/api/v1/saju/reading/marriage") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.sections[0].key") { value("structure") }
            jsonPath("$.sections[1].key") { value("timing") }
        }
    }

    // ── 일일 운세 (SajuController /fortune/daily — LLM 목이 필요해 여기서 테스트) ──

    private val dailyLlmOutput = "오늘은 흐름이 좋은 날\n\n오늘의 메시지입니다. 차분히 진행하면 좋은 결과가 있어요."

    @Test
    fun `일일 운세 - date 생략 시 오늘 기준, 한 줄과 메시지 분리`() {
        given(generator.generate(anyString())).willReturn(dailyLlmOutput)

        mockMvc.post("/api/v1/saju/fortune/daily") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.date") { value(java.time.LocalDate.now().toString()) }
            jsonPath("$.oneLiner") { value("오늘은 흐름이 좋은 날") }
            jsonPath("$.message") { value(containsString("차분히")) }
            jsonPath("$.ilJin.hanja") { isNotEmpty() }
            jsonPath("$.score") { isNumber() }
            jsonPath("$.lucky.colorHangul") { isNotEmpty() }
            jsonPath("$.lucky.number") { isNumber() }
            jsonPath("$.lucky.itemHangul") { isNotEmpty() }
            jsonPath("$.cached") { value(false) }
        }
    }

    @Test
    fun `일일 운세 - 내일까지는 허용`() {
        given(generator.generate(anyString())).willReturn(dailyLlmOutput)

        val tomorrow = java.time.LocalDate.now().plusDays(1)
        mockMvc.post("/api/v1/saju/fortune/daily?date=$tomorrow") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.date") { value(tomorrow.toString()) }
        }
    }

    @Test
    fun `일일 운세 - 모레 이후는 400`() {
        val dayAfterTomorrow = java.time.LocalDate.now().plusDays(2)
        mockMvc.post("/api/v1/saju/fortune/daily?date=$dayAfterTomorrow") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value(containsString("내일까지만")) }
        }
    }

    @Test
    fun `연간 요약 - 카테고리와 월별 구조 반환`() {
        val categories = listOf("OVERALL", "MONEY", "LOVE", "HEALTH", "CAREER")
            .joinToString(",") { "\"$it\":\"$it 요약\"" }
        val months = (1..12).joinToString(",") { "\"$it\":\"${it}월 요약\"" }
        given(generator.generate(anyString()))
            .willReturn("""{"categories":{$categories},"months":{$months}}""")

        mockMvc.post("/api/v1/saju/fortune/2026/summary") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
            jsonPath("$.year") { value(2026) }
            jsonPath("$.categories.length()") { value(5) }
            jsonPath("$.categories[0].category") { value("OVERALL") }
            jsonPath("$.categories[0].categoryHangul") { value("종합") }
            jsonPath("$.categories[0].score") { isNumber() }
            jsonPath("$.months.length()") { value(12) }
            jsonPath("$.months[0].month") { value(1) }
            jsonPath("$.months[0].ganJi.hanja") { isNotEmpty() }
            jsonPath("$.cached") { value(false) }
        }
    }

    @Test
    fun `연간 요약 - 출생 이전 연도는 400`() {
        mockMvc.post("/api/v1/saju/fortune/1990/summary") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `일일 운세 - 잘못된 날짜 형식은 400`() {
        mockMvc.post("/api/v1/saju/fortune/daily?date=2026-13-99") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isBadRequest() }
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
