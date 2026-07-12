package com.saju.api

import com.saju.reading.SajuReadingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class ReadingResponse(
    val reading: String,
    val model: String,
    val cached: Boolean,
    val cacheKey: String,
)

@Tag(name = "사주 해석", description = "LLM 기반 해석문 생성 (DB 영구 캐싱)")
@RestController
@RequestMapping("/api/v1/saju/reading")
class ReadingController(
    private val readingService: SajuReadingService,
) {

    @Operation(
        summary = "연도별 사주 해석문",
        description = "원국 분석과 해당 연도 운세를 근거로 한국어 해석문을 생성합니다. " +
            "동일 입력의 해석문은 DB에 영구 캐싱되어 재요청 시 LLM 호출 없이 반환됩니다 (cached=true).",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "해석 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 입력"),
        ApiResponse(responseCode = "503", description = "LLM 미구성 (ANTHROPIC_API_KEY 없음) + 캐시 미스"),
    )
    @PostMapping("/{year}")
    fun reading(
        @RequestBody request: BirthRequest,
        @Parameter(description = "해석 대상 연도", example = "2026")
        @PathVariable year: Int,
    ): ReadingResponse {
        val result = readingService.getReading(request.toBirthInput(), year)
        return ReadingResponse(result.reading, result.model, result.cached, result.cacheKey)
    }
}
