package com.saju.domain.core

object SixtyGapja {
    // 갑자(甲子)년 = 1984년을 기준 앵커(index 0)로 사용
    private const val ANCHOR_YEAR = 1984

    val cycle: List<GanJi> = (0 until 60).map { i ->
        GanJi(CheonGan.fromIndex(i), JiJi.fromIndex(i))
    }

    fun at(index: Int): GanJi = cycle[((index % 60) + 60) % 60]

    fun indexOf(ganji: GanJi): Int = cycle.indexOfFirst { it == ganji }

    fun fromYear(year: Int): GanJi = at(year - ANCHOR_YEAR)

    fun fromHanja(hanja: String): GanJi {
        require(hanja.length == 2) { "간지는 2글자여야 합니다: $hanja" }
        val gan = CheonGan.fromHanja(hanja[0])
        val ji = JiJi.fromHanja(hanja[1])
        return GanJi(gan, ji)
    }
}
