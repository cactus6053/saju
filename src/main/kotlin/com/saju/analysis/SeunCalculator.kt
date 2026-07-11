package com.saju.analysis

import com.saju.domain.core.GanJi
import com.saju.domain.core.SipSeong
import com.saju.domain.core.SixtyGapja
import com.saju.engine.SajuResult

class SeunCalculator(
    private val relationAnalyzer: RelationAnalyzer = RelationAnalyzer(),
) {

    data class SeunResult(
        val year: Int,
        val ganJi: GanJi,
        val ganSipSeong: SipSeong,                    // 일간 기준 세운 천간의 십성
        val jiSipSeong: SipSeong,                     // 일간 기준 세운 지지 본기의 십성
        val relationsWithWonguk: List<UnRelation>,    // 원국과의 합충형파해
        val relationsWithDaeun: List<RelationAnalyzer.GanJiRelation>, // 현재 대운과의 관계
    )

    // 세운 간지 = 해당 연도의 연주 (입춘 기준 아님 — 연도 자체의 60갑자)
    fun ganJiOf(year: Int): GanJi {
        require(year in 1900..2100) { "지원 범위(1900~2100)를 벗어난 연도입니다: $year" }
        return SixtyGapja.fromYear(year)
    }

    fun analyze(saju: SajuResult, year: Int, currentDaeun: GanJi? = null): SeunResult {
        val ganJi = ganJiOf(year)
        return SeunResult(
            year = year,
            ganJi = ganJi,
            ganSipSeong = SipSeong.of(saju.dayMaster, ganJi.gan),
            jiSipSeong = SipSeong.of(saju.dayMaster, ganJi.ji.janggan.first()),
            relationsWithWonguk = relationAnalyzer.analyzeWithUn(saju, ganJi),
            relationsWithDaeun = currentDaeun
                ?.let { relationAnalyzer.relationsBetween(ganJi, it) }
                ?: emptyList(),
        )
    }
}
