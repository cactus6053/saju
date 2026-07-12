package com.saju.api

import com.saju.analysis.ElementStrengthAnalyzer
import com.saju.analysis.FortuneService
import com.saju.analysis.GyeokGukAnalyzer
import com.saju.analysis.RelationAnalyzer
import com.saju.analysis.SinSalAnalyzer
import com.saju.analysis.SipSeongAnalyzer
import com.saju.engine.SajuCalculator
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

@Tag(name = "사주", description = "사주 팔자 계산 및 운세 조회")
@RestController
@RequestMapping("/api/v1/saju")
class SajuController(
    private val sajuCalculator: SajuCalculator,
    private val sipSeongAnalyzer: SipSeongAnalyzer,
    private val gyeokGukAnalyzer: GyeokGukAnalyzer,
    private val elementStrengthAnalyzer: ElementStrengthAnalyzer,
    private val sinSalAnalyzer: SinSalAnalyzer,
    private val relationAnalyzer: RelationAnalyzer,
    private val fortuneService: FortuneService,
) {

    @Operation(
        summary = "원국 계산 + 전체 분석",
        description = "생년월일시로 사주 팔자를 계산하고 십성·격국용신·오행강약·신살·합충형파해 분석을 반환합니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "계산 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 입력 (범위 밖 날짜, 존재하지 않는 날짜, 무효한 윤달 등)"),
    )
    @PostMapping
    fun calculate(@RequestBody request: BirthRequest): SajuResponse {
        val saju = sajuCalculator.calculate(request.toBirthInput())
        return SajuResponse(
            sajuYear = saju.sajuYear,
            yearPillar = saju.yearPillar.toDto(),
            monthPillar = saju.monthPillar.toDto(),
            dayPillar = saju.dayPillar.toDto(),
            hourPillar = saju.hourPillar.toDto(),
            dayMaster = saju.dayMaster.hanja.toString(),
            paljaHanja = saju.paljaHanja,
            paljaHangul = saju.paljaHangul,
            sipSeong = sipSeongAnalyzer.analyze(saju).toDto(),
            gyeokGuk = gyeokGukAnalyzer.analyze(saju).toDto(),
            elementStrength = elementStrengthAnalyzer.analyze(saju).toDto(),
            sinSal = sinSalAnalyzer.analyze(saju).map { it.toDto() },
            relations = relationAnalyzer.analyze(saju).map { it.toDto() },
        )
    }

    @Operation(
        summary = "연도별 통합 운세",
        description = "지정 연도의 현재 대운(기산 전이면 null) + 세운 + 월운 12개월과 원국·대운·세운 간 관계 분석을 반환합니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 입력 또는 출생 이전·범위 밖 연도"),
    )
    @PostMapping("/fortune/{year}")
    fun fortune(
        @RequestBody request: BirthRequest,
        @Parameter(description = "조회 연도 (출생 사주연도 ~ 2100)", example = "2031")
        @PathVariable year: Int,
    ): FortuneResponse {
        val saju = sajuCalculator.calculate(request.toBirthInput())
        return fortuneService.fortuneOfYear(saju, year).toDto()
    }

    @Operation(
        summary = "대운 타임라인",
        description = "10개 대운의 간지·나이 구간과 각 대운-원국 합충형파해 관계를 반환합니다.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 입력"),
    )
    @PostMapping("/daeun")
    fun daeunTimeline(@RequestBody request: BirthRequest): DaeunTimelineResponse {
        val saju = sajuCalculator.calculate(request.toBirthInput())
        return fortuneService.daeunTimeline(saju).toTimelineDto()
    }
}
