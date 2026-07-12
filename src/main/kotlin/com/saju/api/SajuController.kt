package com.saju.api

import com.saju.analysis.ElementStrengthAnalyzer
import com.saju.analysis.FortuneService
import com.saju.analysis.GyeokGukAnalyzer
import com.saju.analysis.RelationAnalyzer
import com.saju.analysis.SinSalAnalyzer
import com.saju.analysis.SipSeongAnalyzer
import com.saju.engine.SajuCalculator
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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

    // 원국 계산 + 전체 분석
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

    // 연도별 통합 운세: 대운 + 세운 + 월운 12개월
    @PostMapping("/fortune/{year}")
    fun fortune(@RequestBody request: BirthRequest, @PathVariable year: Int): FortuneResponse {
        val saju = sajuCalculator.calculate(request.toBirthInput())
        return fortuneService.fortuneOfYear(saju, year).toDto()
    }

    // 대운 타임라인 전체
    @PostMapping("/daeun")
    fun daeunTimeline(@RequestBody request: BirthRequest): DaeunTimelineResponse {
        val saju = sajuCalculator.calculate(request.toBirthInput())
        return fortuneService.daeunTimeline(saju).toTimelineDto()
    }
}
