package com.saju.analysis

import com.saju.domain.core.GanJi
import com.saju.domain.core.JiJi
import com.saju.domain.core.JiJi.*
import com.saju.domain.core.SipSeong
import com.saju.domain.core.SixtyGapja
import com.saju.engine.MonthPillarCalculator
import com.saju.engine.SajuResult
import java.time.LocalDateTime

class WolunCalculator(
    private val monthPillarCalculator: MonthPillarCalculator = MonthPillarCalculator(),
    private val relationAnalyzer: RelationAnalyzer = RelationAnalyzer(),
) {

    enum class GilHyung(val hangul: String) {
        GOOD("길"), NEUTRAL("보통"), BAD("흉"),
    }

    data class WolunResult(
        val year: Int,
        val month: Int,
        val ganJi: GanJi,
        val ganSipSeong: SipSeong,
        val jiSipSeong: SipSeong,
        val relationsWithWonguk: List<UnRelation>,
        val relationsWithSeun: List<RelationAnalyzer.GanJiRelation>,
        val relationsWithDaeun: List<RelationAnalyzer.GanJiRelation>,
        val isSamjae: Boolean,   // 해당 연도가 원국 연지 기준 삼재년인지
        val gilHyung: GilHyung,  // 합 계열 vs 충·형·파·해 개수 비교 휴리스틱
    )

    companion object {
        // 삼재: 원국 연지의 삼합군 → 삼재 3년 (생지를 충하는 해부터)
        private val SAMJAE: Map<JiJi, Set<JiJi>> = buildMap {
            listOf(SHIN, JA, JIN).forEach { put(it, setOf(IN, MYO, JIN)) }   // 신자진생 → 寅卯辰년
            listOf(IN, O, SUL).forEach { put(it, setOf(SHIN, YU, SUL)) }     // 인오술생 → 申酉戌년
            listOf(SA, YU, CHUK).forEach { put(it, setOf(HAE, JA, CHUK)) }   // 사유축생 → 亥子丑년
            listOf(HAE, MYO, MI).forEach { put(it, setOf(SA, O, MI)) }       // 해묘미생 → 巳午未년
        }

        private val FAVORABLE = setOf(
            RelationType.GAN_HAP, RelationType.YUK_HAP,
            RelationType.SAM_HAP, RelationType.BAN_HAP, RelationType.BANG_HAP,
        )
    }

    // 월운 간지: 해당 달력 월에 시작하는 사주 월의 월주.
    // 절입은 늦어도 매월 9일이므로 15일 정오는 항상 해당 사주 월에 속함.
    fun ganJiOf(year: Int, month: Int): GanJi {
        require(year in 1900..2100) { "지원 범위(1900~2100)를 벗어난 연도입니다: $year" }
        require(month in 1..12) { "월은 1~12 범위여야 합니다: $month" }
        return monthPillarCalculator.calculate(LocalDateTime.of(year, month, 15, 12, 0)).ganJi
    }

    fun isSamjae(wongukYearJi: JiJi, targetYear: Int): Boolean =
        SixtyGapja.fromYear(targetYear).ji in SAMJAE.getValue(wongukYearJi)

    fun analyze(
        saju: SajuResult,
        year: Int,
        month: Int,
        currentDaeun: GanJi? = null,
    ): WolunResult {
        val ganJi = ganJiOf(year, month)
        val seunGanJi = SixtyGapja.fromYear(year)

        val withWonguk = relationAnalyzer.analyzeWithUn(saju, ganJi)
        val withSeun = relationAnalyzer.relationsBetween(ganJi, seunGanJi)
        val withDaeun = currentDaeun
            ?.let { relationAnalyzer.relationsBetween(ganJi, it) }
            ?: emptyList()

        val allTypes = withWonguk.map { it.type } + withSeun.map { it.type } + withDaeun.map { it.type }
        val favorable = allTypes.count { it in FAVORABLE }
        val unfavorable = allTypes.size - favorable

        return WolunResult(
            year = year,
            month = month,
            ganJi = ganJi,
            ganSipSeong = SipSeong.of(saju.dayMaster, ganJi.gan),
            jiSipSeong = SipSeong.of(saju.dayMaster, ganJi.ji.janggan.first()),
            relationsWithWonguk = withWonguk,
            relationsWithSeun = withSeun,
            relationsWithDaeun = withDaeun,
            isSamjae = isSamjae(saju.yearPillar.ji, year),
            gilHyung = when {
                favorable > unfavorable -> GilHyung.GOOD
                unfavorable > favorable -> GilHyung.BAD
                else -> GilHyung.NEUTRAL
            },
        )
    }
}
