package com.saju.analysis

import com.saju.engine.DaeunCalculator
import com.saju.engine.SajuResult

class FortuneService(
    private val daeunCalculator: DaeunCalculator = DaeunCalculator(),
    private val seunCalculator: SeunCalculator = SeunCalculator(),
    private val wolunCalculator: WolunCalculator = WolunCalculator(),
    private val relationAnalyzer: RelationAnalyzer = RelationAnalyzer(),
) {

    // 나이 규약: 사주 연도 기준 경과 연수 (age = 대상연도 - 출생 사주연도)
    data class YearlyFortune(
        val year: Int,
        val age: Int,
        val currentDaeun: DaeunCalculator.Daeun?,        // 기산 전이면 null
        val daeunRelations: List<UnRelation>,            // 현재 대운과 원국의 관계
        val seun: SeunCalculator.SeunResult,             // 세운 (대운 관계 포함)
        val wolunList: List<WolunCalculator.WolunResult>,// 1~12월 월운
    )

    data class DaeunFortune(
        val daeun: DaeunCalculator.Daeun,
        val relationsWithWonguk: List<UnRelation>,
    )

    // 연도 기준 통합 조회: 현재 대운 + 세운 + 12개월 월운
    fun fortuneOfYear(saju: SajuResult, year: Int): YearlyFortune {
        require(year in 1900..2100) { "지원 범위(1900~2100)를 벗어난 연도입니다: $year" }
        val age = year - saju.sajuYear
        require(age >= 0) { "출생 연도(${saju.sajuYear}) 이전은 조회할 수 없습니다: $year" }

        val daeunResult = daeunCalculator.calculate(saju)
        val currentDaeun = daeunResult.at(age)
        val daeunGanJi = currentDaeun?.ganJi

        return YearlyFortune(
            year = year,
            age = age,
            currentDaeun = currentDaeun,
            daeunRelations = daeunGanJi
                ?.let { relationAnalyzer.analyzeWithUn(saju, it) }
                ?: emptyList(),
            seun = seunCalculator.analyze(saju, year, daeunGanJi),
            wolunList = (1..12).map { month ->
                wolunCalculator.analyze(saju, year, month, daeunGanJi)
            },
        )
    }

    // 나이 기준 통합 조회
    fun fortuneOfAge(saju: SajuResult, age: Int): YearlyFortune =
        fortuneOfYear(saju, saju.sajuYear + age)

    // 대운 타임라인 전체: 각 대운과 원국의 관계 요약 포함
    fun daeunTimeline(saju: SajuResult, count: Int = 10): List<DaeunFortune> =
        daeunCalculator.calculate(saju, count).daeunList.map { daeun ->
            DaeunFortune(
                daeun = daeun,
                relationsWithWonguk = relationAnalyzer.analyzeWithUn(saju, daeun.ganJi),
            )
        }
}
