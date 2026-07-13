package com.saju.analysis

import com.saju.domain.core.GanJi
import com.saju.domain.core.Gender
import com.saju.domain.core.SipSeong
import com.saju.domain.core.SipSeongGroup
import com.saju.engine.SajuResult

// FE(SajuSays) FortuneCategory와 1:1
enum class FortuneCategory(val hangul: String) {
    OVERALL("종합"), MONEY("금전"), LOVE("애정"), HEALTH("건강"), CAREER("직업"),
}

// 점수(1~5) 공통 규칙 — 일운·연간 요약이 같은 매핑을 쓴다 (결정적, LLM 미사용)
object FortuneScore {
    val FAVORABLE = setOf(
        RelationType.GAN_HAP, RelationType.YUK_HAP,
        RelationType.SAM_HAP, RelationType.BAN_HAP, RelationType.BANG_HAP,
    )

    fun of(points: Int): Int = when {
        points <= -2 -> 1
        points == -1 -> 2
        points <= 1 -> 3
        points <= 3 -> 4
        else -> 5
    }

    // 용신 오행 일치 가점: 천간·지지 각 +2, 조후용신 각 +1
    fun yongsinPoints(ganJi: GanJi, gyeokGuk: GyeokGukResult): Int {
        var points = 0
        if (ganJi.gan.element == gyeokGuk.yongsin) points += 2
        if (ganJi.ji.element == gyeokGuk.yongsin) points += 2
        gyeokGuk.johuYongsin?.let { johu ->
            if (ganJi.gan.element == johu) points += 1
            if (ganJi.ji.element == johu) points += 1
        }
        return points
    }
}

// 연간 운세 요약의 카테고리 5종·월별 12개 점수 — 세운·월운과 원국의 관계로 결정적 산출
class YearlySummaryCalculator {

    data class CategoryScore(val category: FortuneCategory, val score: Int)
    data class MonthScore(val month: Int, val ganJi: GanJi, val score: Int)

    fun categoryScores(saju: SajuResult, gyeokGuk: GyeokGukResult, seun: SeunCalculator.SeunResult): List<CategoryScore> {
        val balance = relationBalance(seun.relationsWithWonguk.map { it.type })
        val yongsin = FortuneScore.yongsinPoints(seun.ganJi, gyeokGuk)
        val unfavorable = seun.relationsWithWonguk.count { it.type !in FortuneScore.FAVORABLE }

        val dm = saju.dayMaster
        val ganGroup = SipSeong.of(dm, seun.ganJi.gan).group
        val jiGroup = SipSeong.of(dm, seun.ganJi.ji.janggan.first()).group
        fun groupPoints(group: SipSeongGroup) = (if (ganGroup == group) 2 else 0) + (if (jiGroup == group) 1 else 0)
        val yongsinBonus = if (yongsin > 0) 1 else 0

        // 애정은 이성성 — 남명 재성, 여명 관성 (결혼운과 같은 기준)
        val loveGroup = if (saju.birth.gender == Gender.MALE) SipSeongGroup.JAESEONG else SipSeongGroup.GWANSEONG

        return listOf(
            CategoryScore(FortuneCategory.OVERALL, FortuneScore.of(balance + yongsin)),
            CategoryScore(FortuneCategory.MONEY, FortuneScore.of(balance + groupPoints(SipSeongGroup.JAESEONG) + yongsinBonus)),
            CategoryScore(FortuneCategory.LOVE, FortuneScore.of(balance + groupPoints(loveGroup) + yongsinBonus)),
            // 건강은 충·형 등 불리한 관계에 민감하고 십성 주제와는 무관
            CategoryScore(FortuneCategory.HEALTH, FortuneScore.of(yongsin - unfavorable)),
            CategoryScore(FortuneCategory.CAREER, FortuneScore.of(balance + groupPoints(SipSeongGroup.GWANSEONG) + yongsinBonus)),
        )
    }

    fun monthScores(gyeokGuk: GyeokGukResult, wolunList: List<WolunCalculator.WolunResult>): List<MonthScore> =
        wolunList.map { wolun ->
            val types = wolun.relationsWithWonguk.map { it.type } +
                wolun.relationsWithSeun.map { it.type } +
                wolun.relationsWithDaeun.map { it.type }
            val points = relationBalance(types) + FortuneScore.yongsinPoints(wolun.ganJi, gyeokGuk)
            MonthScore(wolun.month, wolun.ganJi, FortuneScore.of(points))
        }

    // 합 계열 +1, 충형파해 -1 의 합 = 2×합 - 전체
    private fun relationBalance(types: List<RelationType>): Int =
        types.count { it in FortuneScore.FAVORABLE } * 2 - types.size
}
