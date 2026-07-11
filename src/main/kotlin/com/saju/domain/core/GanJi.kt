package com.saju.domain.core

data class GanJi(
    val gan: CheonGan,
    val ji: JiJi,
) {
    val hanja: String get() = "${gan.hanja}${ji.hanja}"
    val hangul: String get() = "${gan.hangul}${ji.hangul}"

    override fun toString(): String = hanja
}
