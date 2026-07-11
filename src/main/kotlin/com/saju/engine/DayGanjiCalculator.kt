package com.saju.engine

import com.saju.domain.core.GanJi
import com.saju.domain.core.SixtyGapja
import java.time.LocalDate

class DayGanjiCalculator {

    companion object {
        // epoch day 0 = 1970-01-01 = 辛巳일 (60갑자 index 17)
        // 앵커 검증: 2000-01-01=戊午(54), 2024-01-01=甲子(0), 1949-10-01=甲子(0)
        private const val EPOCH_DAY_GANJI_INDEX = 17L

        val MIN_DATE: LocalDate = LocalDate.of(1900, 1, 1)
        val MAX_DATE: LocalDate = LocalDate.of(2100, 12, 31)
    }

    fun calculate(date: LocalDate): GanJi {
        require(!date.isBefore(MIN_DATE) && !date.isAfter(MAX_DATE)) {
            "지원 범위(1900-01-01 ~ 2100-12-31)를 벗어난 날짜입니다: $date"
        }
        val index = (date.toEpochDay() + EPOCH_DAY_GANJI_INDEX) % 60
        return SixtyGapja.at(index.toInt())
    }
}
