package com.saju.analysis

import com.saju.domain.core.Element
import com.saju.domain.core.SipSeong
import com.saju.domain.core.SipSeongGroup
import com.saju.domain.core.generates
import com.saju.engine.SajuResult

// 점수 체계(합계 9.0):
// - 천간 4자: 각 1.0
// - 지지: 각 1.0을 장간에 분배 (1개: 1.0 / 2개: 0.7·0.3 / 3개: 0.6·0.25·0.15)
// - 월지: 계절 권력을 반영해 2배 가중
// 신강: 지원 세력(비겁+인성, 일간 자신 제외) >= 4.0 (= 나머지 8.0의 절반)
// 과다: 점수 >= 3.0 / 결핍: 점수 < 0.5
class ElementStrengthAnalyzer {

    companion object {
        private val JANGGAN_WEIGHTS = mapOf(
            1 to listOf(1.0),
            2 to listOf(0.7, 0.3),
            3 to listOf(0.6, 0.25, 0.15),
        )
        private const val MONTH_JI_WEIGHT = 2.0
        private const val SUPPORT_THRESHOLD = 4.0
        private const val EXCESSIVE_THRESHOLD = 3.0
        private const val DEFICIENT_THRESHOLD = 0.5
    }

    data class ElementStrength(
        val scores: Map<Element, Double>,  // 오행별 점수 (일간 포함 8자 전체, 합 9.0)
        val supportScore: Double,          // 일간 지원 세력: 비겁+인성 (일간 자신 1.0 제외)
        val opposeScore: Double,           // 대립 세력: 식상+재성+관성
        val isSinGang: Boolean,
        val deukRyeong: Boolean,
        val deukJi: Boolean,
        val deukSe: Boolean,
        val excessive: List<Element>,      // 과다 오행
        val deficient: List<Element>,      // 결핍 오행
    )

    fun analyze(saju: SajuResult): ElementStrength {
        val scores = Element.entries.associateWith { 0.0 }.toMutableMap()

        saju.pillars.forEach { pillar ->
            scores[pillar.gan.element] = scores.getValue(pillar.gan.element) + 1.0
        }

        saju.pillars.forEachIndexed { index, pillar ->
            val jiWeight = if (index == 1) MONTH_JI_WEIGHT else 1.0 // index 1 = 월주
            val weights = JANGGAN_WEIGHTS.getValue(pillar.ji.janggan.size)
            pillar.ji.janggan.forEachIndexed { i, gan ->
                scores[gan.element] = scores.getValue(gan.element) + weights[i] * jiWeight
            }
        }

        val dm = saju.dayMaster
        val supportElements = setOf(dm.element, generatorOf(dm.element))
        val supportScore = supportElements.sumOf { scores.getValue(it) } - 1.0 // 일간 자신 제외
        val opposeScore = Element.entries.filter { it !in supportElements }.sumOf { scores.getValue(it) }

        fun isSupport(gan: com.saju.domain.core.CheonGan) =
            SipSeong.of(dm, gan).group in setOf(SipSeongGroup.BIGEOP, SipSeongGroup.INSEONG)

        val deukRyeong = isSupport(saju.monthPillar.ji.janggan.first())
        val deukJi = isSupport(saju.dayPillar.ji.janggan.first())
        val deukSe = listOf(
            saju.yearPillar.gan, saju.monthPillar.gan, saju.hourPillar.gan,
            saju.yearPillar.ji.janggan.first(), saju.hourPillar.ji.janggan.first(),
        ).count { isSupport(it) } >= 3

        return ElementStrength(
            scores = scores,
            supportScore = supportScore,
            opposeScore = opposeScore,
            isSinGang = supportScore >= SUPPORT_THRESHOLD,
            deukRyeong = deukRyeong,
            deukJi = deukJi,
            deukSe = deukSe,
            excessive = Element.entries.filter { scores.getValue(it) >= EXCESSIVE_THRESHOLD },
            deficient = Element.entries.filter { scores.getValue(it) < DEFICIENT_THRESHOLD },
        )
    }

    private fun generatorOf(element: Element): Element =
        Element.entries.first { it.generates == element }
}
