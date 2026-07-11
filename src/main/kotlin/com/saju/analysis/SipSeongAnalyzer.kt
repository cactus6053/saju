package com.saju.analysis

import com.saju.domain.core.CheonGan
import com.saju.domain.core.GanJi
import com.saju.domain.core.SipSeong
import com.saju.engine.SajuResult

class SipSeongAnalyzer {

    data class PillarSipSeong(
        val gan: SipSeong?,             // 천간의 십성 (일간 자신은 null)
        val jiPrincipal: SipSeong,      // 지지 본기(장간 첫 번째) 기준 십성
        val jiJanggan: List<SipSeong>,  // 장간 전체의 십성
    )

    data class SajuSipSeong(
        val year: PillarSipSeong,
        val month: PillarSipSeong,
        val day: PillarSipSeong,
        val hour: PillarSipSeong,
    ) {
        val pillars: List<PillarSipSeong> get() = listOf(year, month, day, hour)
    }

    fun analyze(saju: SajuResult): SajuSipSeong {
        val dayMaster = saju.dayMaster
        return SajuSipSeong(
            year = pillarOf(dayMaster, saju.yearPillar, isDayPillar = false),
            month = pillarOf(dayMaster, saju.monthPillar, isDayPillar = false),
            day = pillarOf(dayMaster, saju.dayPillar, isDayPillar = true),
            hour = pillarOf(dayMaster, saju.hourPillar, isDayPillar = false),
        )
    }

    private fun pillarOf(dayMaster: CheonGan, pillar: GanJi, isDayPillar: Boolean): PillarSipSeong {
        val janggan = pillar.ji.janggan.map { SipSeong.of(dayMaster, it) }
        return PillarSipSeong(
            gan = if (isDayPillar) null else SipSeong.of(dayMaster, pillar.gan),
            jiPrincipal = janggan.first(),
            jiJanggan = janggan,
        )
    }
}
