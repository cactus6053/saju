package com.saju.engine

import com.saju.domain.core.GanJi
import com.saju.domain.core.Jeolgi
import com.saju.domain.core.SixtyGapja
import java.time.LocalDateTime

class YearPillarCalculator(
    private val jeolgiCalculator: JeolgiCalculator = JeolgiCalculator(),
) {

    data class YearPillar(
        val ganJi: GanJi,
        val sajuYear: Int, // 입춘 기준으로 판정된 사주 연도
    )

    // 입춘 절입 시각 정각부터 새해로 판정
    fun calculate(kst: LocalDateTime): YearPillar {
        val ipchun = jeolgiCalculator.getMoment(kst.year, Jeolgi.IPCHUN)
        val sajuYear = if (kst.isBefore(ipchun)) kst.year - 1 else kst.year
        return YearPillar(SixtyGapja.fromYear(sajuYear), sajuYear)
    }
}
