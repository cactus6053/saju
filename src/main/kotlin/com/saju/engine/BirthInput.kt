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
    val timeZone: String = "Asia/Seoul", // 출생지 IANA 시간대 (해외 출생 지원)
    val longitude: Double = TimeCorrector.SEOUL_LONGITUDE,
    val timeCorrectionMode: TimeCorrectionMode = TimeCorrectionMode.STANDARD,
    val zasiMode: ZasiMode = ZasiMode.YAJASI_JEONGJASI,
)

// 이중 트랙:
// - corrected: 출생지 현지 프레임 보정 시각 → 일주·시주 판정 (현지 태양 기준)
// - instantKst: 절대 시점의 KST 표현 → 연주·월주 판정 (절기 시각과 비교)
// 한국 출생 STANDARD 모드에서는 두 값이 동일하다.
data class NormalizedBirth(
    val solarDate: LocalDate,     // 양력 생년월일 (현지 벽시계 기준)
    val wallClock: LocalDateTime, // 출생 당시 현지 벽시계 시각
    val corrected: LocalDateTime, // 현지 프레임 보정 시각 — 일주·시주 기준
    val gender: Gender,
    val zasiMode: ZasiMode,
    val instantKst: LocalDateTime = corrected, // 절대 시점(KST 프레임) — 연주·월주 기준
)
