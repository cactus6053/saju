package com.saju.analysis

import com.saju.domain.core.Element
import com.saju.domain.core.GanJi
import com.saju.domain.core.GanRelation
import com.saju.domain.core.JiRelation
import com.saju.domain.core.PillarPosition
import com.saju.engine.SajuResult

enum class RelationType(val hangul: String) {
    GAN_HAP("천간합"),
    GAN_CHUNG("천간충"),
    YUK_HAP("육합"),
    SAM_HAP("삼합"),
    BAN_HAP("반합"),
    BANG_HAP("방합"),
    YUK_CHUNG("육충"),
    HYEONG("형"),
    PA("파"),
    HAE("해"),
}

// 원국 내부 관계: positions는 관련된 원국 기둥들 (쌍 2개, 삼합·방합 3개)
data class PillarRelation(
    val type: RelationType,
    val positions: Set<PillarPosition>,
    val resultElement: Element? = null, // 합화 오행 (합 계열만)
)

// 운(대운·세운)과 원국의 교차 관계: positions는 원국 쪽 기둥
data class UnRelation(
    val type: RelationType,
    val positions: Set<PillarPosition>,
    val resultElement: Element? = null,
)

class RelationAnalyzer {

    private val positions = PillarPosition.entries

    // 원국 전체 관계 매트릭스
    fun analyze(saju: SajuResult): List<PillarRelation> {
        val relations = mutableListOf<PillarRelation>()
        val pillars = saju.pillars

        // 쌍 관계: C(4,2) = 6쌍
        for (i in 0 until 4) {
            for (j in i + 1 until 4) {
                val posPair = setOf(positions[i], positions[j])
                relations += ganPairRelations(pillars[i], pillars[j], posPair)
                relations += jiPairRelations(pillars[i], pillars[j], posPair)
            }
        }

        // 트리오 관계: C(4,3) = 4조 (삼합·방합)
        for (i in 0 until 4) {
            for (j in i + 1 until 4) {
                for (k in j + 1 until 4) {
                    val posTrio = setOf(positions[i], positions[j], positions[k])
                    JiRelation.samHapOf(pillars[i].ji, pillars[j].ji, pillars[k].ji)?.let {
                        relations += PillarRelation(RelationType.SAM_HAP, posTrio, it.resultElement)
                    }
                    JiRelation.bangHapOf(pillars[i].ji, pillars[j].ji, pillars[k].ji)?.let {
                        relations += PillarRelation(RelationType.BANG_HAP, posTrio, it.resultElement)
                    }
                }
            }
        }

        return relations
    }

    // 운 간지와 원국의 교차 판정
    fun analyzeWithUn(saju: SajuResult, un: GanJi): List<UnRelation> {
        val relations = mutableListOf<UnRelation>()
        val pillars = saju.pillars

        pillars.forEachIndexed { i, pillar ->
            val pos = setOf(positions[i])

            // 천간 관계
            GanRelation.hapOf(un.gan, pillar.gan)?.let {
                relations += UnRelation(RelationType.GAN_HAP, pos, it.resultElement)
            }
            if (GanRelation.isChung(un.gan, pillar.gan)) {
                relations += UnRelation(RelationType.GAN_CHUNG, pos)
            }

            // 지지 쌍 관계
            JiRelation.yukHapOf(un.ji, pillar.ji)?.let {
                relations += UnRelation(RelationType.YUK_HAP, pos, it.resultElement)
            }
            JiRelation.banHapOf(un.ji, pillar.ji)?.let {
                relations += UnRelation(RelationType.BAN_HAP, pos, it.resultElement)
            }
            if (JiRelation.isChung(un.ji, pillar.ji)) relations += UnRelation(RelationType.YUK_CHUNG, pos)
            if (JiRelation.isHyeong(un.ji, pillar.ji)) relations += UnRelation(RelationType.HYEONG, pos)
            if (JiRelation.isPa(un.ji, pillar.ji)) relations += UnRelation(RelationType.PA, pos)
            if (JiRelation.isHae(un.ji, pillar.ji)) relations += UnRelation(RelationType.HAE, pos)
        }

        // 운지가 원국 지지 2개와 삼합·방합을 완성하는 경우
        for (i in 0 until 4) {
            for (j in i + 1 until 4) {
                val posPair = setOf(positions[i], positions[j])
                JiRelation.samHapOf(un.ji, pillars[i].ji, pillars[j].ji)?.let {
                    relations += UnRelation(RelationType.SAM_HAP, posPair, it.resultElement)
                }
                JiRelation.bangHapOf(un.ji, pillars[i].ji, pillars[j].ji)?.let {
                    relations += UnRelation(RelationType.BANG_HAP, posPair, it.resultElement)
                }
            }
        }

        return relations.distinct()
    }

    // 임의의 두 간지 사이 관계 (세운-대운 등 기둥 외 관계 분석용)
    data class GanJiRelation(
        val type: RelationType,
        val resultElement: Element? = null,
    )

    fun relationsBetween(a: GanJi, b: GanJi): List<GanJiRelation> {
        val result = mutableListOf<GanJiRelation>()
        GanRelation.hapOf(a.gan, b.gan)?.let {
            result += GanJiRelation(RelationType.GAN_HAP, it.resultElement)
        }
        if (GanRelation.isChung(a.gan, b.gan)) result += GanJiRelation(RelationType.GAN_CHUNG)
        JiRelation.yukHapOf(a.ji, b.ji)?.let {
            result += GanJiRelation(RelationType.YUK_HAP, it.resultElement)
        }
        JiRelation.banHapOf(a.ji, b.ji)?.let {
            result += GanJiRelation(RelationType.BAN_HAP, it.resultElement)
        }
        if (JiRelation.isChung(a.ji, b.ji)) result += GanJiRelation(RelationType.YUK_CHUNG)
        if (JiRelation.isHyeong(a.ji, b.ji)) result += GanJiRelation(RelationType.HYEONG)
        if (JiRelation.isPa(a.ji, b.ji)) result += GanJiRelation(RelationType.PA)
        if (JiRelation.isHae(a.ji, b.ji)) result += GanJiRelation(RelationType.HAE)
        return result
    }

    private fun ganPairRelations(a: GanJi, b: GanJi, pos: Set<PillarPosition>): List<PillarRelation> {
        val result = mutableListOf<PillarRelation>()
        GanRelation.hapOf(a.gan, b.gan)?.let {
            result += PillarRelation(RelationType.GAN_HAP, pos, it.resultElement)
        }
        if (GanRelation.isChung(a.gan, b.gan)) {
            result += PillarRelation(RelationType.GAN_CHUNG, pos)
        }
        return result
    }

    private fun jiPairRelations(a: GanJi, b: GanJi, pos: Set<PillarPosition>): List<PillarRelation> {
        val result = mutableListOf<PillarRelation>()
        JiRelation.yukHapOf(a.ji, b.ji)?.let {
            result += PillarRelation(RelationType.YUK_HAP, pos, it.resultElement)
        }
        JiRelation.banHapOf(a.ji, b.ji)?.let {
            result += PillarRelation(RelationType.BAN_HAP, pos, it.resultElement)
        }
        if (JiRelation.isChung(a.ji, b.ji)) result += PillarRelation(RelationType.YUK_CHUNG, pos)
        if (JiRelation.isHyeong(a.ji, b.ji)) result += PillarRelation(RelationType.HYEONG, pos)
        if (JiRelation.isPa(a.ji, b.ji)) result += PillarRelation(RelationType.PA, pos)
        if (JiRelation.isHae(a.ji, b.ji)) result += PillarRelation(RelationType.HAE, pos)
        return result
    }
}
