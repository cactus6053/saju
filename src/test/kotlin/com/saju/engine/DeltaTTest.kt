package com.saju.engine

import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertTrue

class DeltaTTest {

    @Test
    fun `관측값 대조 - 주요 연도 ΔT`() {
        // 실측 기록: 1900 ≈ -2.8s, 1950 ≈ 29.1s, 2000 ≈ 63.8s, 2020 ≈ 69.4s
        assertTrue(abs(DeltaT.seconds(1900) - (-2.8)) < 2.0, "1900: ${DeltaT.seconds(1900)}")
        assertTrue(abs(DeltaT.seconds(1950) - 29.1) < 2.0, "1950: ${DeltaT.seconds(1950)}")
        assertTrue(abs(DeltaT.seconds(2000) - 63.8) < 2.0, "2000: ${DeltaT.seconds(2000)}")
        assertTrue(abs(DeltaT.seconds(2020) - 69.4) < 5.0, "2020: ${DeltaT.seconds(2020)}")
    }

    @Test
    fun `구간 경계에서 불연속 없음`() {
        listOf(1920, 1941, 1961, 1986, 2005, 2050).forEach { boundary ->
            val before = DeltaT.seconds(boundary - 1)
            val after = DeltaT.seconds(boundary)
            assertTrue(
                abs(after - before) < 5.0,
                "${boundary}년 경계 불연속: $before → $after",
            )
        }
    }

    @Test
    fun `1950년 이후 단조 증가 경향`() {
        (1950..2099 step 10).forEach { year ->
            assertTrue(
                DeltaT.seconds(year + 10) > DeltaT.seconds(year) - 1.0,
                "${year}년대 ΔT 역전",
            )
        }
    }

    @Test
    fun `전체 범위에서 합리적 크기 - 1900~2100은 -10초에서 300초 사이`() {
        (1900..2100).forEach { year ->
            val dt = DeltaT.seconds(year)
            assertTrue(dt in -10.0..300.0, "${year}년 ΔT 범위 초과: $dt")
        }
    }
}
