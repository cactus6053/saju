package com.saju.api

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @Test
    fun `api-docs - 3개 엔드포인트와 메타 정보 노출`() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.info.title") { value("사주 계산 엔진 API") }
            jsonPath("$.info.version") { value("v1") }
            jsonPath("$.paths['/api/v1/saju'].post.summary") { value("원국 계산 + 전체 분석") }
            jsonPath("$.paths['/api/v1/saju/fortune/{year}'].post.summary") { value("연도별 통합 운세") }
            jsonPath("$.paths['/api/v1/saju/daeun'].post.summary") { value("대운 타임라인") }
        }
    }

    @Test
    fun `api-docs - BirthRequest 스키마에 필드 설명 포함`() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.components.schemas.BirthRequest.description") { value("출생 정보") }
            jsonPath("$.components.schemas.BirthRequest.properties.year.description") {
                value(containsString("1900~2100"))
            }
            jsonPath("$.components.schemas.BirthRequest.properties.zasiMode.description") {
                value(containsString("야자시"))
            }
        }
    }

    @Test
    fun `api-docs - 400 응답 문서화`() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.paths['/api/v1/saju'].post.responses['400'].description") {
                value(containsString("잘못된 입력"))
            }
        }
    }

    @Test
    fun `Swagger UI 접근 가능`() {
        mockMvc.get("/swagger-ui/index.html").andExpect {
            status { isOk() }
        }
    }
}
