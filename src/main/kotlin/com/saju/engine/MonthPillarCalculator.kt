package com.saju.engine

import com.saju.domain.core.CheonGan
import com.saju.domain.core.GanJi
import com.saju.domain.core.JiJi
import java.time.LocalDateTime

class MonthPillarCalculator(
    private val monthBoundaryResolver: MonthBoundaryResolver = MonthBoundaryResolver(),
    private val yearPillarCalculator: YearPillarCalculator = YearPillarCalculator(),
) {

    data class MonthPillar(
        val ganJi: GanJi,
        val sajuMonth: MonthBoundaryResolver.SajuMonth,
    )

    // 월간은 사주 연도(입춘 기준)의 연간으로부터 오호둔년법으로 결정되므로
    // 연주를 내부에서 함께 판정해 경계 불일치를 방지한다.
    fun calculate(kst: LocalDateTime): MonthPillar {
        val sajuMonth = monthBoundaryResolver.resolve(kst)
        val yearGan = yearPillarCalculator.calculate(kst).ganJi.gan

        // 오호둔년법(五虎遁年法): 인월의 월간 = 연간에 따라 결정
        // 甲己→丙寅, 乙庚→戊寅, 丙辛→庚寅, 丁壬→壬寅, 戊癸→甲寅
        val firstMonthGanIndex = (yearGan.index % 5) * 2 + 2
        val monthOffset = (sajuMonth.monthBranch.index - JiJi.IN.index + 12) % 12
        val monthGan = CheonGan.fromIndex(firstMonthGanIndex + monthOffset)

        return MonthPillar(GanJi(monthGan, sajuMonth.monthBranch), sajuMonth)
    }
}
