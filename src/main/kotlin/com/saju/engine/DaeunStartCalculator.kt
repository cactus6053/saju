package com.saju.engine

import com.saju.domain.core.Gender
import com.saju.domain.core.YinYang
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.roundToInt

class DaeunStartCalculator(
    private val monthBoundaryResolver: MonthBoundaryResolver = MonthBoundaryResolver(),
) {

    enum class Direction(val hangul: String) {
        FORWARD("순행"),
        BACKWARD("역행"),
    }

    data class DaeunStart(
        val direction: Direction,
        val daysToJeol: Double,           // 절입까지의 일수 (분 단위 정밀도)
        val daeunSu: Int,                 // 대운 시작 나이 (1~10)
        val targetJeolMoment: LocalDateTime, // 기준이 된 절입 시각
    )

    // 순행: 양간 연주 + 남자, 음간 연주 + 여자 / 역행: 그 반대
    // 대운수 = 절입까지 일수 ÷ 3 반올림 (3일 = 1년), 1~10 범위로 제한
    fun calculate(saju: SajuResult): DaeunStart {
        val direction = directionOf(saju)
        val kst = saju.birth.corrected
        val sajuMonth = monthBoundaryResolver.resolve(kst)

        val (minutes, target) = when (direction) {
            Direction.FORWARD ->
                Duration.between(kst, sajuMonth.nextJeolMoment).toMinutes() to sajuMonth.nextJeolMoment
            Direction.BACKWARD ->
                Duration.between(sajuMonth.jeolMoment, kst).toMinutes() to sajuMonth.jeolMoment
        }

        val days = minutes / 1440.0
        val daeunSu = (days / 3.0).roundToInt().coerceIn(1, 10)

        return DaeunStart(direction, days, daeunSu, target)
    }

    fun directionOf(saju: SajuResult): Direction {
        val isYangYear = saju.yearPillar.gan.yinYang == YinYang.YANG
        val isMale = saju.birth.gender == Gender.MALE
        return if (isYangYear == isMale) Direction.FORWARD else Direction.BACKWARD
    }
}
