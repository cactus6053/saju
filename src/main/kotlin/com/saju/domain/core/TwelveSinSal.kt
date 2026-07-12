package com.saju.domain.core

// 12신살(十二神殺): 기준 지지(연지 또는 일지)의 삼합군으로 판정
enum class TwelveSinSal(val hangul: String, val hanja: String) {
    GEOPSAL("겁살", "劫殺"),
    JAESAL("재살", "災殺"),
    CHEONSAL("천살", "天殺"),
    JISAL("지살", "地殺"),
    NYEONSAL("년살", "年殺"),      // = 도화살
    WOLSAL("월살", "月殺"),
    MANGSIN("망신살", "亡身殺"),
    JANGSEONG("장성살", "將星殺"),
    BANAN("반안살", "攀鞍殺"),
    YEOKMA("역마살", "驛馬殺"),
    YUKHAE("육해살", "六害殺"),
    HWAGAE("화개살", "華蓋殺"),
    ;

    companion object {
        // 삼합군별 겁살지 (삼합 오행의 절지)
        private val GEOPSAL_JI: Map<JiJi, JiJi> = buildMap {
            listOf(JiJi.SHIN, JiJi.JA, JiJi.JIN).forEach { put(it, JiJi.SA) }   // 수국 → 巳
            listOf(JiJi.HAE, JiJi.MYO, JiJi.MI).forEach { put(it, JiJi.SHIN) }  // 목국 → 申
            listOf(JiJi.IN, JiJi.O, JiJi.SUL).forEach { put(it, JiJi.HAE) }     // 화국 → 亥
            listOf(JiJi.SA, JiJi.YU, JiJi.CHUK).forEach { put(it, JiJi.IN) }    // 금국 → 寅
        }

        // 겁살지부터 지지 순행으로 12신살 배열
        fun of(baseJi: JiJi, targetJi: JiJi): TwelveSinSal {
            val start = GEOPSAL_JI.getValue(baseJi)
            return entries[(targetJi.index - start.index + 12) % 12]
        }
    }
}
