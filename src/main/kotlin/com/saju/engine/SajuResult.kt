package com.saju.engine

import com.saju.domain.core.CheonGan
import com.saju.domain.core.GanJi

data class SajuResult(
    val yearPillar: GanJi,
    val monthPillar: GanJi,
    val dayPillar: GanJi,
    val hourPillar: GanJi,
    val sajuYear: Int,
    val birth: NormalizedBirth,
) {
    val dayMaster: CheonGan get() = dayPillar.gan // 일간(日干) — 모든 파생 분석의 기준

    val pillars: List<GanJi> get() = listOf(yearPillar, monthPillar, dayPillar, hourPillar)

    val paljaHanja: String get() = pillars.joinToString(" ") { it.hanja }

    val paljaHangul: String get() = pillars.joinToString(" ") { it.hangul }
}
