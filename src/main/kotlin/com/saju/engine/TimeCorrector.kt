package com.saju.engine

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin

enum class TimeCorrectionMode {
    STANDARD,            // 당시 공식 표준시 → 균일한 KST(UTC+9) 프레임으로 정규화
    LOCAL_MEAN_TIME,     // 지방평균시(LMT): 경도 × 4분 보정
    APPARENT_SOLAR_TIME, // 진태양시: 지방평균시 + 균시차(equation of time)
}

class TimeCorrector {

    companion object {
        const val SEOUL_LONGITUDE = 126.9780

        // IANA tzdata 사용: UTC+8:30 시기(1908~1911, 1954~1961)와
        // 서머타임(1948~1951, 1955~1960, 1987~1988) 이력이 모두 반영되어 있음
        private val KOREA_ZONE = ZoneId.of("Asia/Seoul")
    }

    // 출생 당시 벽시계 시각 → UTC (역사적 표준시·서머타임 자동 반영)
    fun toUtc(wallClock: LocalDateTime): LocalDateTime =
        wallClock.atZone(KOREA_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()

    fun correct(
        wallClock: LocalDateTime,
        longitude: Double = SEOUL_LONGITUDE,
        mode: TimeCorrectionMode = TimeCorrectionMode.STANDARD,
    ): LocalDateTime {
        val utc = toUtc(wallClock)
        return when (mode) {
            TimeCorrectionMode.STANDARD -> utc.plusHours(9)

            TimeCorrectionMode.LOCAL_MEAN_TIME -> applyLmt(utc, longitude)

            TimeCorrectionMode.APPARENT_SOLAR_TIME -> {
                val lmt = applyLmt(utc, longitude)
                lmt.plusSeconds((equationOfTimeMinutes(lmt.dayOfYear) * 60).roundToLong())
            }
        }
    }

    // LMT = UTC + 경도 × 4분 (경도 1도 = 4분)
    private fun applyLmt(utc: LocalDateTime, longitude: Double): LocalDateTime {
        require(longitude in -180.0..180.0) { "경도 범위(-180~180)를 벗어남: $longitude" }
        return utc.plusSeconds((longitude * 240).roundToLong())
    }

    // 균시차(분): NOAA 근사식, 연중 약 -14.6분 ~ +16.4분
    fun equationOfTimeMinutes(dayOfYear: Int): Double {
        val b = 2 * PI * (dayOfYear - 81) / 365.0
        return 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
    }
}
