package com.saju.engine

import com.saju.domain.core.CheonGan
import com.saju.domain.core.GanJi
import com.saju.domain.core.JiJi
import java.time.LocalDateTime

class HourPillarCalculator(
    private val dayGanjiCalculator: DayGanjiCalculator = DayGanjiCalculator(),
) {

    fun calculate(kst: LocalDateTime): GanJi {
        val hourBranch = branchOf(kst.hour)

        // 時干 기준 일간: 자시가 시작되는 23시부터는 다음날 일간 사용.
        // 야자시 학파에서도 야자시의 時干은 익일 자시 干을 쓰므로 두 자시 모드 공통.
        val basisDate = when (kst.hour) {
            23 -> kst.toLocalDate().plusDays(1)
            else -> kst.toLocalDate()
        }
        val dayGan = dayGanjiCalculator.calculate(basisDate).gan

        // 오서둔일법(五鼠遁日法): 자시의 시간 = 일간에 따라 결정
        // 甲己→甲子, 乙庚→丙子, 丙辛→戊子, 丁壬→庚子, 戊癸→壬子
        val firstHourGanIndex = (dayGan.index % 5) * 2
        val hourGan = CheonGan.fromIndex(firstHourGanIndex + hourBranch.index)

        return GanJi(hourGan, hourBranch)
    }

    // 시지: 23:00~00:59 子, 01:00~02:59 丑, ... 2시간 단위
    fun branchOf(hour: Int): JiJi {
        require(hour in 0..23) { "시(hour)는 0~23 범위여야 합니다: $hour" }
        return JiJi.fromIndex(((hour + 1) / 2) % 12)
    }
}
