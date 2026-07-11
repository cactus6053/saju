package com.saju.engine

import com.saju.domain.core.LunarDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class BirthInputNormalizer(
    private val lunarConverter: LunarConverter = LunarConverter(),
    private val timeCorrector: TimeCorrector = TimeCorrector(),
) {

    companion object {
        private val MIN_CORRECTED: LocalDateTime = LocalDateTime.of(1900, 1, 1, 0, 0)
        private val MAX_CORRECTED: LocalDateTime = LocalDateTime.of(2100, 12, 31, 23, 59, 59)
    }

    fun normalize(input: BirthInput): NormalizedBirth {
        validateCommon(input)

        val solarDate = when (input.calendarType) {
            CalendarType.SOLAR -> toSolarDate(input)
            CalendarType.LUNAR -> lunarConverter.lunarToSolar(
                LunarDate(input.year, input.month, input.day, input.isLeapMonth)
            )
        }

        val wallClock = solarDate.atTime(input.hour, input.minute)
        val corrected = timeCorrector.correct(wallClock, input.longitude, input.timeCorrectionMode)

        require(corrected in MIN_CORRECTED..MAX_CORRECTED) {
            "보정된 시각($corrected)이 지원 범위(1900-01-01 ~ 2100-12-31)를 벗어났습니다. " +
                "경계 날짜 출생은 보정 방식에 따라 범위를 벗어날 수 있습니다."
        }

        return NormalizedBirth(
            solarDate = solarDate,
            wallClock = wallClock,
            corrected = corrected,
            gender = input.gender,
            zasiMode = input.zasiMode,
        )
    }

    private fun validateCommon(input: BirthInput) {
        require(input.year in 1900..2100) {
            "출생 연도는 1900~2100 범위여야 합니다: ${input.year}"
        }
        require(input.month in 1..12) {
            "월은 1~12 범위여야 합니다: ${input.month}"
        }
        require(input.hour in 0..23) {
            "시(hour)는 0~23 범위여야 합니다: ${input.hour}"
        }
        require(input.minute in 0..59) {
            "분(minute)은 0~59 범위여야 합니다: ${input.minute}"
        }
        require(!(input.calendarType == CalendarType.SOLAR && input.isLeapMonth)) {
            "양력 입력에는 윤달(isLeapMonth)을 지정할 수 없습니다"
        }
        require(input.longitude in -180.0..180.0) {
            "경도는 -180~180 범위여야 합니다: ${input.longitude}"
        }
    }

    private fun toSolarDate(input: BirthInput): LocalDate {
        val lengthOfMonth = YearMonth.of(input.year, input.month).lengthOfMonth()
        require(input.day in 1..lengthOfMonth) {
            "${input.year}년 ${input.month}월은 ${lengthOfMonth}일까지 있습니다: ${input.day}"
        }
        return LocalDate.of(input.year, input.month, input.day)
    }
}
