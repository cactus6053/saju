package com.saju.api

import com.saju.reading.ReadingLanguage
import com.saju.reading.ReadingTopic
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class ReadingSectionDto(
    val key: String,
    val title: String,
    val body: String,
)

data class ReadingResponse(
    val sections: List<ReadingSectionDto>,
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
        summary = "원국 풀이 (평생사주)",
        description = "성격·기질·격국·용신·오행 균형·신살·적성 해석. 연도 무관이라 같은 사주는 영구 재사용됩니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "해석 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 입력"),
        ApiResponse(responseCode = "503", description = "LLM 미구성 + 캐시 미스"),
    )
    @PostMapping
    fun wonguk(
        @RequestBody request: BirthRequest,
        @Parameter(description = LANG_DESC, example = "ko")
        @RequestParam(defaultValue = "ko") lang: String,
    ): ReadingResponse =
        readingService.getWongukReading(request.toBirthInput(), ReadingLanguage.of(lang)).toResponse()

    @Operation(
        summary = "대운 풀이 (10년 단위 인생 흐름)",
        description = "10개 대운의 십성·12운성·원국 관계를 근거로 인생 흐름을 해석합니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "해석 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 입력"),
        ApiResponse(responseCode = "503", description = "LLM 미구성 + 캐시 미스"),
    )
    @PostMapping("/daeun")
    fun daeun(
        @RequestBody request: BirthRequest,
        @Parameter(description = LANG_DESC, example = "ko")
        @RequestParam(defaultValue = "ko") lang: String,
    ): ReadingResponse =
        readingService.getDaeunReading(request.toBirthInput(), ReadingLanguage.of(lang)).toResponse()

    @Operation(
        summary = "결혼운 풀이",
        description = "배우자성·배우자궁(일지) 상태와 향후 10년 세운 스캔을 근거로 " +
            "결혼 기운이 들어오는 시기를 해석합니다. 스캔 기준 연도는 서버 현재 연도입니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "해석 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 입력"),
        ApiResponse(responseCode = "503", description = "LLM 미구성 + 캐시 미스"),
    )
    @PostMapping("/marriage")
    fun marriage(
        @RequestBody request: BirthRequest,
        @Parameter(description = LANG_DESC, example = "ko")
        @RequestParam(defaultValue = "ko") lang: String,
    ): ReadingResponse =
        readingService.getMarriageReading(request.toBirthInput(), ReadingLanguage.of(lang)).toResponse()

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
        @Parameter(description = "해석 주제 (general: 종합, money: 금전운, career: 직장운, health: 건강운, love: 애정운)", example = "general")
        @RequestParam(defaultValue = "general") topic: String,
        @Parameter(description = LANG_DESC, example = "ko")
        @RequestParam(defaultValue = "ko") lang: String,
    ): ReadingResponse {
        val parsedTopic = runCatching { ReadingTopic.valueOf(topic.uppercase()) }
            .getOrElse {
                throw IllegalArgumentException(
                    "지원하지 않는 topic입니다: $topic (사용 가능: general, money, career, health, love)"
                )
            }
        return readingService.getReading(request.toBirthInput(), year, parsedTopic, ReadingLanguage.of(lang))
            .toResponse()
    }

    private fun SajuReadingService.StructuredReadingResult.toResponse() = ReadingResponse(
        sections = sections.map { ReadingSectionDto(it.key, it.title, it.body) },
        model = model,
        cached = cached,
        cacheKey = cacheKey,
    )

    companion object {
        const val LANG_DESC = "출력 언어 (ko, en, es, zh, ja, th, vi, ms)"
    }
}
