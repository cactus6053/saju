package com.saju.engine

// ΔT = TT(지구시) - UT(세계시). 지구 자전 감속으로 인한 차이.
// Espenak & Meeus (2006) 다항식 — NASA 일월식 예보에 사용되는 표준 모델.
// 절기 계산은 TT 기준이므로 UTC 변환 시 ΔT를 빼야 한다.
internal object DeltaT {

    // 해당 연도의 ΔT (초)
    fun seconds(year: Int): Double {
        val y = year.toDouble()
        return when {
            year < 1920 -> {
                val t = y - 1900
                -2.79 + 1.494119 * t - 0.0598939 * t * t +
                    0.0061966 * t * t * t - 0.000197 * t * t * t * t
            }
            year < 1941 -> {
                val t = y - 1920
                21.20 + 0.84493 * t - 0.076100 * t * t + 0.0020936 * t * t * t
            }
            year < 1961 -> {
                val t = y - 1950
                29.07 + 0.407 * t - t * t / 233 + t * t * t / 2547
            }
            year < 1986 -> {
                val t = y - 1975
                45.45 + 1.067 * t - t * t / 260 - t * t * t / 718
            }
            year < 2005 -> {
                val t = y - 2000
                63.86 + 0.3345 * t - 0.060374 * t * t + 0.0017275 * t * t * t +
                    0.000651814 * t * t * t * t + 0.00002373599 * t * t * t * t * t
            }
            year < 2050 -> {
                val t = y - 2000
                62.92 + 0.32217 * t + 0.005589 * t * t
            }
            else -> {
                // 2050~2150 외삽 모델
                -20 + 32 * ((y - 1820) / 100) * ((y - 1820) / 100) - 0.5628 * (2150 - y)
            }
        }
    }

    // TT 기준 JDE → UT 기준 JD
    fun ttToUt(jde: Double, year: Int): Double = jde - seconds(year) / 86400.0
}
