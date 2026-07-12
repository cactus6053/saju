package com.saju.domain.core

// 12운성(十二運星): 일간이 각 지지에서 갖는 생애 단계
enum class UnSeong(val hangul: String, val hanja: String) {
    JANGSAENG("장생", "長生"),
    MOKYOK("목욕", "沐浴"),
    GWANDAE("관대", "冠帶"),
    GEONROK("건록", "建祿"),
    JEWANG("제왕", "帝旺"),
    SOE("쇠", "衰"),
    BYEONG("병", "病"),
    SA("사", "死"),
    MYO("묘", "墓"),
    JEOL("절", "絶"),
    TAE("태", "胎"),
    YANG("양", "養"),
    ;

    companion object {
        // 일간별 장생지 (화토동법: 戊는 丙, 己는 丁을 따름)
        private val JANGSAENG_JI: Map<CheonGan, JiJi> = mapOf(
            CheonGan.GAP to JiJi.HAE,
            CheonGan.BYEONG to JiJi.IN,
            CheonGan.MU to JiJi.IN,
            CheonGan.GYEONG to JiJi.SA,
            CheonGan.IM to JiJi.SHIN,
            CheonGan.EUL to JiJi.O,
            CheonGan.JEONG to JiJi.YU,
            CheonGan.GI to JiJi.YU,
            CheonGan.SIN to JiJi.JA,
            CheonGan.GYE to JiJi.MYO,
        )

        // 양간은 장생지부터 순행, 음간은 역행
        fun of(dayGan: CheonGan, ji: JiJi): UnSeong {
            val start = JANGSAENG_JI.getValue(dayGan)
            val steps = when (dayGan.yinYang) {
                YinYang.YANG -> (ji.index - start.index + 12) % 12
                YinYang.YIN -> (start.index - ji.index + 12) % 12
            }
            return entries[steps]
        }
    }
}
