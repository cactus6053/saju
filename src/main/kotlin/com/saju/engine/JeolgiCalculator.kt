package com.saju.engine

import com.saju.domain.core.Jeolgi
import java.time.LocalDateTime
import kotlin.math.abs

class JeolgiCalculator {

    private val DEGREES_PER_DAY = 360.0 / 365.25

    // 주어진 연도의 절기 발생 시각을 KST로 반환 (ΔT 보정 적용)
    fun getMoment(year: Int, jeolgi: Jeolgi): LocalDateTime {
        val jde = findJde(year, jeolgi.solarLongitude)      // TT 기준
        val jdUt = DeltaT.ttToUt(jde, year)                 // UT 기준으로 보정
        return SolarLongitude.toUtc(jdUt).plusHours(9)      // KST = UTC+9
    }

    // 해당 연도 24절기 전체 반환
    fun getAll(year: Int): List<Pair<Jeolgi, LocalDateTime>> =
        Jeolgi.entries.map { it to getMoment(year, it) }

    // 해당 연도 12절(月 경계) 만 반환
    fun getJeolMoments(year: Int): List<Pair<Jeolgi, LocalDateTime>> =
        Jeolgi.JEOL_LIST.map { it to getMoment(year, it) }

    // 특정 JDE 직후에 오는 절기와 그 시각을 반환 (월주 판정에 사용)
    fun nextJeolAfter(jde: Double): Pair<Jeolgi, LocalDateTime> {
        val utc = SolarLongitude.toUtc(jde)
        val year = utc.year
        val kst = utc.plusHours(9)

        val candidates = sequenceOf(year - 1, year, year + 1)
            .flatMap { y -> Jeolgi.JEOL_LIST.map { j -> j to getMoment(y, j) } }
            .filter { (_, dt) -> dt.isAfter(kst) }
            .minByOrNull { (_, dt) -> dt }

        return checkNotNull(candidates) { "nextJeolAfter: 다음 절을 찾을 수 없음 (jde=$jde)" }
    }

    // Newton-Raphson으로 태양 황경 = targetLon 인 JDE 탐색
    private fun findJde(year: Int, targetLon: Double): Double {
        // 춘분(0°)은 초기 추정에서 360°로 처리해 wrap-around 방지
        val effectiveTarget = if (targetLon < 5.0) targetLon + 360.0 else targetLon

        val jan1Jde = SolarLongitude.toJde(year, 1, 1, 12.0)
        val lon0 = SolarLongitude.at(jan1Jde)

        val dayOffset = ((effectiveTarget - lon0 + 360.0) % 360.0) / DEGREES_PER_DAY
        var jde = jan1Jde + dayOffset

        repeat(50) {
            val current = SolarLongitude.at(jde)
            var diff = targetLon - current
            if (diff > 180) diff -= 360
            if (diff < -180) diff += 360
            if (abs(diff) < 0.00001) return jde
            jde += diff / DEGREES_PER_DAY
        }

        return jde
    }
}
