package com.saju.engine

import com.saju.domain.core.GanJi
import java.time.LocalDate
import java.time.LocalDateTime

class DayPillarCalculator(
    private val dayGanjiCalculator: DayGanjiCalculator = DayGanjiCalculator(),
) {

    data class DayPillar(
        val ganJi: GanJi,
        val effectiveDate: LocalDate, // 자시 처리 후 일주 기준 날짜
    )

    // 자시 처리 규칙:
    // - YAJASI_JEONGJASI: 23시대는 야자시로 당일 유지, 00시대는 정자시로 당일 → 벽시계 날짜 그대로
    // - SIMPLE: 23:00부터 다음날 일주로 취급
    fun calculate(kst: LocalDateTime, zasiMode: ZasiMode = ZasiMode.YAJASI_JEONGJASI): DayPillar {
        val effectiveDate = when {
            zasiMode == ZasiMode.SIMPLE && kst.hour == 23 -> kst.toLocalDate().plusDays(1)
            else -> kst.toLocalDate()
        }
        return DayPillar(dayGanjiCalculator.calculate(effectiveDate), effectiveDate)
    }
}
