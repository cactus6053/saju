package com.saju.engine

import com.saju.domain.core.LunarDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class LunarConverter {

    companion object {
        // 음력 1900년 1월 1일 = 양력 1900-01-31
        private val ANCHOR: LocalDate = LocalDate.of(1900, 1, 31)

        val MIN_DATE: LocalDate = ANCHOR
        val MAX_DATE: LocalDate = ANCHOR.plusDays(
            (LunarTable.MIN_YEAR..LunarTable.MAX_YEAR).sumOf { LunarTable.yearDays(it).toLong() } - 1
        )
    }

    fun solarToLunar(date: LocalDate): LunarDate {
        require(!date.isBefore(MIN_DATE) && !date.isAfter(MAX_DATE)) {
            "지원 범위($MIN_DATE ~ $MAX_DATE)를 벗어난 날짜입니다: $date"
        }

        var offset = ChronoUnit.DAYS.between(ANCHOR, date).toInt()

        var year = LunarTable.MIN_YEAR
        while (offset >= LunarTable.yearDays(year)) {
            offset -= LunarTable.yearDays(year)
            year++
        }

        val leap = LunarTable.leapMonth(year)
        var month = 1
        var isLeap = false
        while (true) {
            val len = if (isLeap) LunarTable.leapMonthDays(year) else LunarTable.monthDays(year, month)
            if (offset < len) break
            offset -= len
            if (!isLeap && month == leap) {
                isLeap = true
            } else {
                isLeap = false
                month++
            }
        }

        return LunarDate(year, month, offset + 1, isLeap)
    }

    fun lunarToSolar(lunar: LunarDate): LocalDate {
        require(lunar.year in LunarTable.MIN_YEAR..LunarTable.MAX_YEAR) {
            "지원 범위(1900~2100)를 벗어난 연도입니다: ${lunar.year}"
        }
        require(lunar.month in 1..12) { "월은 1~12 범위여야 합니다: ${lunar.month}" }

        val leap = LunarTable.leapMonth(lunar.year)
        if (lunar.isLeapMonth) {
            require(lunar.month == leap) {
                "${lunar.year}년의 윤달은 ${if (leap == 0) "없음" else "${leap}월"}입니다: 윤${lunar.month}월은 유효하지 않음"
            }
        }

        val monthLen = if (lunar.isLeapMonth) LunarTable.leapMonthDays(lunar.year)
                       else LunarTable.monthDays(lunar.year, lunar.month)
        require(lunar.day in 1..monthLen) {
            "${lunar.year}년 ${if (lunar.isLeapMonth) "윤" else ""}${lunar.month}월은 ${monthLen}일까지입니다: ${lunar.day}"
        }

        var days = 0L
        for (y in LunarTable.MIN_YEAR until lunar.year) {
            days += LunarTable.yearDays(y)
        }
        for (m in 1 until lunar.month) {
            days += LunarTable.monthDays(lunar.year, m)
            if (m == leap) days += LunarTable.leapMonthDays(lunar.year)
        }
        if (lunar.isLeapMonth) {
            days += LunarTable.monthDays(lunar.year, lunar.month)
        }
        days += lunar.day - 1

        return ANCHOR.plusDays(days)
    }

    fun leapMonthOf(year: Int): Int = LunarTable.leapMonth(year)
}
