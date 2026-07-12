package com.saju.api

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class SajuControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {

    private val baseBody = """
        {
          "year": 2024, "month": 6, "day": 15, "hour": 12,
          "gender": "MALE"
        }
    """.trimIndent()

    // ── POST /api/v1/saju ───────────────────────────────────────────────

    @Test
    fun `원국 계산 - 팔자와 분석 전체 반환`() {
        mockMvc.post("/api/v1/saju") {
            contentType = MediaType.APPLICATION_JSON
            content = baseBody
        }.andExpect {
            status { isOk() }
            jsonPath("$.paljaHanja") { value("甲辰 庚午 庚戌 壬午") }
            jsonPath("$.paljaHangul") { value("갑진 경오 경술 임오") }
            jsonPath("$.sajuYear") { value(2024) }
            jsonPath("$.dayMaster") { value("庚") }
            jsonPath("$.yearPillar.hanja") { value("甲辰") }
            jsonPath("$.gyeokGuk.name") { value("정관격") }
            jsonPath("$.gyeokGuk.yongsin") { value("토") }
            jsonPath("$.gyeokGuk.johuYongsin") { value("수") }
            jsonPath("$.gyeokGuk.isSinGang") { value(false) }
            jsonPath("$.elementStrength.scores.금") { value(2.25) }
            jsonPath("$.sinSal[0].name") { value("백호대살") }
            jsonPath("$.sinSal[0].position") { value("연주") }
            jsonPath("$.sipSeong.year.gan") { value("편재") }
            jsonPath("$.sipSeong.day.gan") { value(null) }
            jsonPath("$.relations.length()") { value(6) }
        }
    }

    @Test
    fun `음력 입력 - 설날 출생`() {
        mockMvc.post("/api/v1/saju") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "year": 2024, "month": 1, "day": 1, "hour": 12,
                  "calendarType": "LUNAR", "gender": "FEMALE"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.paljaHanja") { value("甲辰 丙寅 甲辰 庚午") }
        }
    }

    // ── 검증 실패 → 400 ────────────────────────────────────────────────

    @Test
    fun `범위 밖 연도 - 400과 입력값 포함 메시지`() {
        mockMvc.post("/api/v1/saju") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"year": 1899, "month": 6, "day": 15, "hour": 12, "gender": "MALE"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value(org.hamcrest.Matchers.containsString("1899")) }
        }
    }

    @Test
    fun `존재하지 않는 날짜 - 400`() {
        mockMvc.post("/api/v1/saju") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"year": 2024, "month": 2, "day": 30, "hour": 12, "gender": "MALE"}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `필수 필드 누락 - 400과 안내 메시지`() {
        mockMvc.post("/api/v1/saju") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"year": 2024}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value(org.hamcrest.Matchers.containsString("필수 필드")) }
        }
    }

    // ── POST /api/v1/saju/fortune/{year} ───────────────────────────────

    @Test
    fun `연도별 운세 - 대운·세운·월운 통합 반환`() {
        mockMvc.post("/api/v1/saju/fortune/2031") {
            contentType = MediaType.APPLICATION_JSON
            content = baseBody
        }.andExpect {
            status { isOk() }
            jsonPath("$.year") { value(2031) }
            jsonPath("$.age") { value(7) }
            jsonPath("$.currentDaeun.ganJi.hanja") { value("辛未") }
            jsonPath("$.currentDaeun.startAge") { value(7) }
            jsonPath("$.seun.ganJi.hanja") { value("辛亥") }
            jsonPath("$.wolunList.length()") { value(12) }
            jsonPath("$.wolunList[0].month") { value(1) }
        }
    }

    @Test
    fun `대운 기산 전 연도 - currentDaeun null`() {
        mockMvc.post("/api/v1/saju/fortune/2025") {
            contentType = MediaType.APPLICATION_JSON
            content = baseBody
        }.andExpect {
            status { isOk() }
            jsonPath("$.currentDaeun") { value(null) }
            jsonPath("$.daeunRelations.length()") { value(0) }
        }
    }

    @Test
    fun `출생 이전 연도 운세 - 400`() {
        mockMvc.post("/api/v1/saju/fortune/2023") {
            contentType = MediaType.APPLICATION_JSON
            content = baseBody
        }.andExpect {
            status { isBadRequest() }
        }
    }

    // ── POST /api/v1/saju/daeun ────────────────────────────────────────

    @Test
    fun `대운 타임라인 - 10개 대운과 원국 관계`() {
        mockMvc.post("/api/v1/saju/daeun") {
            contentType = MediaType.APPLICATION_JSON
            content = baseBody
        }.andExpect {
            status { isOk() }
            jsonPath("$.daeunList.length()") { value(10) }
            jsonPath("$.daeunList[0].daeun.ganJi.hanja") { value("辛未") }
            jsonPath("$.daeunList[0].daeun.startAge") { value(7) }
            jsonPath("$.daeunList[9].daeun.ganJi.hanja") { value("庚辰") }
            jsonPath("$.daeunList[9].daeun.endAge") { value(106) }
        }
    }
}
