package com.saju.engine

import com.saju.domain.core.Jeolgi
import com.saju.domain.core.JiJi
import java.time.LocalDateTime

class MonthBoundaryResolver(
    private val jeolgiCalculator: JeolgiCalculator = JeolgiCalculator(),
) {

    data class SajuMonth(
        val monthBranch: JiJi,
        val governingJeol: Jeolgi,
        val jeolMoment: LocalDateTime,     // 이 달이 시작된 절입 시각 (KST)
        val nextJeolMoment: LocalDateTime, // 다음 절입 시각 (KST)
    )

    // 입력 시각(KST 프레임)이 속한 사주 월을 판정.
    // 절입 시각 정각부터 새 달로 판정 (moment <= kst).
    fun resolve(kst: LocalDateTime): SajuMonth {
        val moments = (kst.year - 1..kst.year + 1)
            .flatMap { year ->
                Jeolgi.JEOL_LIST.map { jeol -> jeol to jeolgiCalculator.getMoment(year, jeol) }
            }
            .sortedBy { it.second }

        val index = moments.indexOfLast { (_, moment) -> !moment.isAfter(kst) }
        check(index >= 0 && index < moments.lastIndex) { "절기 탐색 범위 초과: $kst" }

        val (jeol, moment) = moments[index]
        val (_, nextMoment) = moments[index + 1]

        return SajuMonth(
            monthBranch = checkNotNull(jeol.monthBranch),
            governingJeol = jeol,
            jeolMoment = moment,
            nextJeolMoment = nextMoment,
        )
    }
}
