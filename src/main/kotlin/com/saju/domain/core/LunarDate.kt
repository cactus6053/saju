package com.saju.domain.core

data class LunarDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean = false,
) {
    override fun toString(): String =
        "음력 ${year}년 ${if (isLeapMonth) "윤" else ""}${month}월 ${day}일"
}
