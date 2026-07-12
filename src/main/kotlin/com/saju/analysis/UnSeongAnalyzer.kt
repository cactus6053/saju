package com.saju.analysis

import com.saju.domain.core.JiJi
import com.saju.domain.core.PillarPosition
import com.saju.domain.core.TwelveSinSal
import com.saju.domain.core.UnSeong
import com.saju.engine.SajuResult

class UnSeongAnalyzer {

    data class UnSeongResult(
        val year: UnSeong,
        val month: UnSeong,
        val day: UnSeong,
        val hour: UnSeong,
    ) {
        fun at(position: PillarPosition): UnSeong = when (position) {
            PillarPosition.YEAR -> year
            PillarPosition.MONTH -> month
            PillarPosition.DAY -> day
            PillarPosition.HOUR -> hour
        }
    }

    // 일간 기준 원국 4지지의 12운성
    fun analyze(saju: SajuResult): UnSeongResult {
        val dm = saju.dayMaster
        return UnSeongResult(
            year = UnSeong.of(dm, saju.yearPillar.ji),
            month = UnSeong.of(dm, saju.monthPillar.ji),
            day = UnSeong.of(dm, saju.dayPillar.ji),
            hour = UnSeong.of(dm, saju.hourPillar.ji),
        )
    }

    // 운(대운·세운) 지지의 12운성
    fun ofUn(saju: SajuResult, unJi: JiJi): UnSeong = UnSeong.of(saju.dayMaster, unJi)
}

class TwelveSinSalAnalyzer {

    enum class Base { YEAR_JI, DAY_JI }

    data class TwelveSinSalResult(
        val base: Base,
        val year: TwelveSinSal,
        val month: TwelveSinSal,
        val day: TwelveSinSal,
        val hour: TwelveSinSal,
    )

    // 기준 지지(기본: 연지)로 원국 4지지의 12신살 판정
    fun analyze(saju: SajuResult, base: Base = Base.YEAR_JI): TwelveSinSalResult {
        val baseJi = when (base) {
            Base.YEAR_JI -> saju.yearPillar.ji
            Base.DAY_JI -> saju.dayPillar.ji
        }
        return TwelveSinSalResult(
            base = base,
            year = TwelveSinSal.of(baseJi, saju.yearPillar.ji),
            month = TwelveSinSal.of(baseJi, saju.monthPillar.ji),
            day = TwelveSinSal.of(baseJi, saju.dayPillar.ji),
            hour = TwelveSinSal.of(baseJi, saju.hourPillar.ji),
        )
    }

    // 운(대운·세운) 지지의 12신살
    fun ofUn(saju: SajuResult, unJi: JiJi, base: Base = Base.YEAR_JI): TwelveSinSal {
        val baseJi = when (base) {
            Base.YEAR_JI -> saju.yearPillar.ji
            Base.DAY_JI -> saju.dayPillar.ji
        }
        return TwelveSinSal.of(baseJi, unJi)
    }
}
