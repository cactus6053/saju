package com.saju.domain.core

enum class Jeolgi(
    val hanja: String,
    val hangul: String,
    val solarLongitude: Double,
    val isJeol: Boolean,
    val monthBranch: JiJi?,
) {
    // 소한(285°)부터 시작 — 한국 역서 순서
    SOHAN("小寒", "소한", 285.0, true, JiJi.CHUK),
    DAEHAN("大寒", "대한", 300.0, false, null),
    IPCHUN("立春", "입춘", 315.0, true, JiJi.IN),
    USU("雨水", "우수", 330.0, false, null),
    GYEONGCHIP("驚蟄", "경칩", 345.0, true, JiJi.MYO),
    CHUNBUN("春分", "춘분", 0.0, false, null),
    CHEONGMYEONG("清明", "청명", 15.0, true, JiJi.JIN),
    GOGU("穀雨", "곡우", 30.0, false, null),
    IPHA("立夏", "입하", 45.0, true, JiJi.SA),
    SOMAN("小滿", "소만", 60.0, false, null),
    MANGJONG("芒種", "망종", 75.0, true, JiJi.O),
    HAJI("夏至", "하지", 90.0, false, null),
    SOSEO("小暑", "소서", 105.0, true, JiJi.MI),
    DAESEO("大暑", "대서", 120.0, false, null),
    IPACHU("立秋", "입추", 135.0, true, JiJi.SHIN),
    CHEOSEO("處暑", "처서", 150.0, false, null),
    BAENGNO("白露", "백로", 165.0, true, JiJi.YU),
    CHUBUN("秋分", "추분", 180.0, false, null),
    HALLO("寒露", "한로", 195.0, true, JiJi.SUL),
    SANGGANG("霜降", "상강", 210.0, false, null),
    IPDONG("立冬", "입동", 225.0, true, JiJi.HAE),
    SOSEOL("小雪", "소설", 240.0, false, null),
    DAESEOL("大雪", "대설", 255.0, true, JiJi.JA),
    DONGJI("冬至", "동지", 270.0, false, null),
    ;

    companion object {
        val JEOL_LIST: List<Jeolgi> get() = entries.filter { it.isJeol }
        val JUNGGI_LIST: List<Jeolgi> get() = entries.filter { !it.isJeol }
    }
}
