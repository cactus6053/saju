package com.saju.engine

import com.saju.domain.core.Gender
import java.time.LocalDate
import java.time.LocalDateTime

enum class CalendarType { SOLAR, LUNAR }

// 자시(23:00~01:00) 처리 방식 — 시주 계산(#13)에서 적용
enum class ZasiMode {
    YAJASI_JEONGJASI, // 야자시/정자시 구분: 23시대는 당일 유지, 00시대는 다음날
    SIMPLE,           // 단순 자시: 23:00부터 다음날로 취급
}

data class BirthInput(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int = 0,
    val calendarType: CalendarType = CalendarType.SOLAR,
    val isLeapMonth: Boolean = false,
    val gender: Gender,
    val longitude: Double = TimeCorrector.SEOUL_LONGITUDE,
    val timeCorrectionMode: TimeCorrectionMode = TimeCorrectionMode.STANDARD,
    val zasiMode: ZasiMode = ZasiMode.YAJASI_JEONGJASI,
)

data class NormalizedBirth(
    val solarDate: LocalDate,     // 양력 생년월일 (벽시계 기준)
    val wallClock: LocalDateTime, // 출생 당시 벽시계 시각
    val corrected: LocalDateTime, // 보정된 KST 프레임 시각 — 사주 계산의 기준
    val gender: Gender,
    val zasiMode: ZasiMode,
)
