package com.saju.engine

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin

enum class TimeCorrectionMode {
    STANDARD,            // 당시 공식 표준시 → 해당 지역의 균일 표준 프레임으로 정규화 (DST·역사적 오프셋 제거)
    LOCAL_MEAN_TIME,     // 지방평균시(LMT): 경도 × 4분 보정
    APPARENT_SOLAR_TIME, // 진태양시: 지방평균시 + 균시차(equation of time)
}

class TimeCorrector {

    companion object {
        const val SEOUL_LONGITUDE = 126.9780

        val DEFAULT_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

        // 표준 오프셋 산정 기준 시점: 역사적 오프셋(한국 UTC+8:30 시기 등)과
        // DST에 흔들리지 않는 현대 표준 오프셋을 고정적으로 얻는다 (한국 +9, 뉴욕 -5)
        private val STANDARD_OFFSET_REFERENCE: Instant = Instant.parse("2000-01-01T00:00:00Z")
    }

    // 출생 당시 벽시계 시각 → UTC (해당 시간대의 역사적 표준시·서머타임 자동 반영)
    fun toUtc(wallClock: LocalDateTime, zone: ZoneId = DEFAULT_ZONE): LocalDateTime =
        wallClock.atZone(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime()

    fun correct(
        wallClock: LocalDateTime,
        longitude: Double = SEOUL_LONGITUDE,
        mode: TimeCorrectionMode = TimeCorrectionMode.STANDARD,
        zone: ZoneId = DEFAULT_ZONE,
    ): LocalDateTime {
        val utc = toUtc(wallClock, zone)
        return when (mode) {
            TimeCorrectionMode.STANDARD ->
                utc.plusSeconds(zone.rules.getStandardOffset(STANDARD_OFFSET_REFERENCE).totalSeconds.toLong())

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
