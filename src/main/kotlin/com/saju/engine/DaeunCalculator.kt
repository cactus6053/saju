package com.saju.engine

import com.saju.domain.core.GanJi
import com.saju.domain.core.SixtyGapja

class DaeunCalculator(
    private val daeunStartCalculator: DaeunStartCalculator = DaeunStartCalculator(),
) {

    data class Daeun(
        val order: Int,      // 1부터
        val ganJi: GanJi,
        val startAge: Int,
        val endAge: Int,
    )

    data class DaeunResult(
        val direction: DaeunStartCalculator.Direction,
        val daeunSu: Int,
        val daeunList: List<Daeun>,
    ) {
        // 특정 나이의 대운 (기산점 이전은 null)
        fun at(age: Int): Daeun? = daeunList.firstOrNull { age in it.startAge..it.endAge }
    }

    // 순행: 월주 다음 간지부터, 역행: 월주 이전 간지부터 10년 단위 나열
    fun calculate(saju: SajuResult, count: Int = 10): DaeunResult {
        require(count >= 1) { "대운 개수는 1 이상이어야 합니다: $count" }

        val start = daeunStartCalculator.calculate(saju)
        val monthIndex = SixtyGapja.indexOf(saju.monthPillar)

        val daeunList = (1..count).map { i ->
            val ganJi = when (start.direction) {
                DaeunStartCalculator.Direction.FORWARD -> SixtyGapja.at(monthIndex + i)
                DaeunStartCalculator.Direction.BACKWARD -> SixtyGapja.at(monthIndex - i)
            }
            val startAge = start.daeunSu + (i - 1) * 10
            Daeun(order = i, ganJi = ganJi, startAge = startAge, endAge = startAge + 9)
        }

        return DaeunResult(start.direction, start.daeunSu, daeunList)
    }
}
