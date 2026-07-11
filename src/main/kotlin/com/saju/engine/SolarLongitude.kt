package com.saju.engine

import java.time.LocalDateTime
import kotlin.math.*

// Jean Meeus "Astronomical Algorithms" 2nd ed. Chapter 25 (Low Accuracy).
// 적경도(apparent ecliptic longitude) 정확도: 약 ±0.01° (≈ ±15분 시각 오차).
internal object SolarLongitude {

    private const val J2000 = 2451545.0

    fun at(jde: Double): Double {
        val T = (jde - J2000) / 36525.0

        val L0 = (280.46646 + T * (36000.76983 + T * 0.0003032)).mod360()
        val Mrad = (357.52911 + T * (35999.05029 - T * 0.0001537)).mod360().toRad()

        val C = (1.914602 - T * (0.004817 + T * 0.000014)) * sin(Mrad) +
                (0.019993 - T * 0.000101) * sin(2 * Mrad) +
                0.000289 * sin(3 * Mrad)

        val theta = L0 + C
        val omegaRad = (125.04 - 1934.136 * T).mod360().toRad()
        val lambda = theta - 0.00569 - 0.00478 * sin(omegaRad)

        return lambda.mod360()
    }

    // 그레고리력 날짜 → Julian Ephemeris Day (근사: ΔT 무시)
    fun toJde(year: Int, month: Int, day: Int, utcHour: Double = 12.0): Double {
        var y = year.toLong()
        var m = month.toLong()
        if (m <= 2) { y--; m += 12 }
        val A = y / 100
        val B = 2 - A + A / 4
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + B - 1524.5 + utcHour / 24.0
    }

    // Julian Ephemeris Day → LocalDateTime (UTC)
    fun toUtc(jde: Double): LocalDateTime {
        val jdAdj = jde + 0.5
        val Z = jdAdj.toLong()
        val F = jdAdj - Z

        val A: Long = if (Z < 2299161L) {
            Z
        } else {
            val alpha = ((Z - 1867216.25) / 36524.25).toLong()
            Z + 1 + alpha - alpha / 4
        }
        val B = A + 1524
        val C = ((B - 122.1) / 365.25).toLong()
        val D = floor(365.25 * C).toLong()
        val E = ((B - D) / 30.6001).toLong()

        val day = (B - D - floor(30.6001 * E).toLong()).toInt()
        val month = if (E < 14L) (E - 1).toInt() else (E - 13).toInt()
        val year = if (month > 2) (C - 4716).toInt() else (C - 4715).toInt()

        val totalSec = (F * 86400).toLong()
        val hour = (totalSec / 3600).toInt()
        val minute = ((totalSec % 3600) / 60).toInt()
        val second = (totalSec % 60).toInt()

        return LocalDateTime.of(year, month, day, hour, minute, second)
    }

    private fun Double.mod360() = ((this % 360) + 360) % 360
    private fun Double.toRad() = Math.toRadians(this)
}
