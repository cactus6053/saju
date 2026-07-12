package com.saju.analysis

import com.saju.domain.core.CheonGan
import com.saju.domain.core.Gender
import com.saju.domain.core.JiJi
import com.saju.domain.core.TwelveSinSal
import com.saju.domain.core.UnSeong
import com.saju.domain.core.YinYang
import com.saju.engine.BirthInput
import com.saju.engine.SajuCalculator
import com.saju.engine.SajuResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class UnSeongAnalyzerTest {

    private val unSeongAnalyzer = UnSeongAnalyzer()
    private val sinSalAnalyzer = TwelveSinSalAnalyzer()

    private fun wonguk(): SajuResult = SajuCalculator().calculate(
        BirthInput(2024, 6, 15, 12, 0, gender = Gender.MALE)
    )

    // ── 12운성 규칙 검증 ────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} 일간의 건록지 = {1}")
    @CsvSource(
        "GAP, IN",     // 甲祿在寅
        "EUL, MYO",    // 乙祿在卯
        "BYEONG, SA",  // 丙祿在巳
        "JEONG, O",    // 丁祿在午
        "MU, SA",      // 戊祿在巳 (화토동법)
        "GI, O",       // 己祿在午
        "GYEONG, SHIN",// 庚祿在申
        "SIN, YU",     // 辛祿在酉
        "IM, HAE",     // 壬祿在亥
        "GYE, JA",     // 癸祿在子
    )
    fun `건록지는 십간 록지 조견표와 일치`(gan: CheonGan, expectedJi: JiJi) {
        assertEquals(UnSeong.GEONROK, UnSeong.of(gan, expectedJi))
    }

    @Test
    fun `양간의 제왕지는 양인살 지지와 일치`() {
        // 교차 검증: 제왕 = 양인 (甲卯 丙午 戊午 庚酉 壬子)
        val yanginTable = mapOf(
            CheonGan.GAP to JiJi.MYO, CheonGan.BYEONG to JiJi.O, CheonGan.MU to JiJi.O,
            CheonGan.GYEONG to JiJi.YU, CheonGan.IM to JiJi.JA,
        )
        yanginTable.forEach { (gan, ji) ->
            assertEquals(UnSeong.JEWANG, UnSeong.of(gan, ji), "${gan.hangul}의 제왕지")
        }
    }

    @Test
    fun `甲 일간 순행 - 12지지 전체 매핑`() {
        val expected = mapOf(
            JiJi.HAE to UnSeong.JANGSAENG, JiJi.JA to UnSeong.MOKYOK,
            JiJi.CHUK to UnSeong.GWANDAE, JiJi.IN to UnSeong.GEONROK,
            JiJi.MYO to UnSeong.JEWANG, JiJi.JIN to UnSeong.SOE,
            JiJi.SA to UnSeong.BYEONG, JiJi.O to UnSeong.SA,
            JiJi.MI to UnSeong.MYO, JiJi.SHIN to UnSeong.JEOL,
            JiJi.YU to UnSeong.TAE, JiJi.SUL to UnSeong.YANG,
        )
        expected.forEach { (ji, unSeong) ->
            assertEquals(unSeong, UnSeong.of(CheonGan.GAP, ji), "甲-${ji.hangul}")
        }
    }

    @Test
    fun `乙 일간 역행 - 장생 午부터 거꾸로`() {
        assertEquals(UnSeong.JANGSAENG, UnSeong.of(CheonGan.EUL, JiJi.O))
        assertEquals(UnSeong.MOKYOK, UnSeong.of(CheonGan.EUL, JiJi.SA))
        assertEquals(UnSeong.GWANDAE, UnSeong.of(CheonGan.EUL, JiJi.JIN))
        assertEquals(UnSeong.GEONROK, UnSeong.of(CheonGan.EUL, JiJi.MYO))
        assertEquals(UnSeong.JEWANG, UnSeong.of(CheonGan.EUL, JiJi.IN))
    }

    @Test
    fun `모든 일간은 12지지에 12운성이 한 번씩`() {
        CheonGan.entries.forEach { gan ->
            val all = JiJi.entries.map { UnSeong.of(gan, it) }
            assertEquals(12, all.distinct().size, "${gan.hangul} 일간 운성 분포 이상")
        }
    }

    @Test
    fun `원국 분석 - 庚 일간 갑진 경오 경술 임오`() {
        // 庚 장생 巳 순행: 辰=양(11), 午=목욕(1), 戌=쇠(5)
        val result = unSeongAnalyzer.analyze(wonguk())
        assertEquals(UnSeong.YANG, result.year)
        assertEquals(UnSeong.MOKYOK, result.month)
        assertEquals(UnSeong.SOE, result.day)
        assertEquals(UnSeong.MOKYOK, result.hour)
    }

    @Test
    fun `운 지지의 운성 조회`() {
        // 庚 일간 + 酉 운 → 제왕
        assertEquals(UnSeong.JEWANG, unSeongAnalyzer.ofUn(wonguk(), JiJi.YU))
    }

    // ── 12신살 규칙 검증 ────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} 기준 삼합 구조: 지살={1}, 장성={2}, 화개={3}")
    @CsvSource(
        "JA, SHIN, JA, JIN",    // 신자진: 생지 申, 왕지 子, 고지 辰
        "O, IN, O, SUL",        // 인오술
        "YU, SA, YU, CHUK",     // 사유축
        "MYO, HAE, MYO, MI",    // 해묘미
    )
    fun `삼합 구조와 12신살 대응 - 지살·장성살·화개살`(
        base: JiJi, saengji: JiJi, wangji: JiJi, goji: JiJi,
    ) {
        assertEquals(TwelveSinSal.JISAL, TwelveSinSal.of(base, saengji), "생지=지살")
        assertEquals(TwelveSinSal.JANGSEONG, TwelveSinSal.of(base, wangji), "왕지=장성살")
        assertEquals(TwelveSinSal.HWAGAE, TwelveSinSal.of(base, goji), "고지=화개살")
    }

    @Test
    fun `년살은 도화살, 역마살은 기존 역마 조견표와 일치`() {
        // 교차 검증: 신살 모듈(#17)의 도화·역마 테이블과 동일해야 함
        val dohwa = mapOf(
            JiJi.JA to JiJi.YU, JiJi.O to JiJi.MYO, JiJi.YU to JiJi.O, JiJi.MYO to JiJi.JA,
        )
        val yeokma = mapOf(
            JiJi.JA to JiJi.IN, JiJi.O to JiJi.SHIN, JiJi.YU to JiJi.HAE, JiJi.MYO to JiJi.SA,
        )
        dohwa.forEach { (base, target) ->
            assertEquals(TwelveSinSal.NYEONSAL, TwelveSinSal.of(base, target), "${base.hangul} 년살")
        }
        yeokma.forEach { (base, target) ->
            assertEquals(TwelveSinSal.YEOKMA, TwelveSinSal.of(base, target), "${base.hangul} 역마")
        }
    }

    @Test
    fun `寅午戌 기준 12신살 전체 순서`() {
        val expected = listOf(
            JiJi.HAE to TwelveSinSal.GEOPSAL, JiJi.JA to TwelveSinSal.JAESAL,
            JiJi.CHUK to TwelveSinSal.CHEONSAL, JiJi.IN to TwelveSinSal.JISAL,
            JiJi.MYO to TwelveSinSal.NYEONSAL, JiJi.JIN to TwelveSinSal.WOLSAL,
            JiJi.SA to TwelveSinSal.MANGSIN, JiJi.O to TwelveSinSal.JANGSEONG,
            JiJi.MI to TwelveSinSal.BANAN, JiJi.SHIN to TwelveSinSal.YEOKMA,
            JiJi.YU to TwelveSinSal.YUKHAE, JiJi.SUL to TwelveSinSal.HWAGAE,
        )
        expected.forEach { (ji, sinSal) ->
            assertEquals(sinSal, TwelveSinSal.of(JiJi.O, ji), "午 기준 ${ji.hangul}")
        }
    }

    @Test
    fun `원국 분석 - 연지 辰 기준`() {
        // 辰(신자진군, 겁살 巳): 辰=화개, 午=재살, 戌=월살
        val result = sinSalAnalyzer.analyze(wonguk())
        assertEquals(TwelveSinSalAnalyzer.Base.YEAR_JI, result.base)
        assertEquals(TwelveSinSal.HWAGAE, result.year)
        assertEquals(TwelveSinSal.JAESAL, result.month)
        assertEquals(TwelveSinSal.WOLSAL, result.day)
        assertEquals(TwelveSinSal.JAESAL, result.hour)
    }

    @Test
    fun `일지 기준 선택 가능`() {
        // 일지 戌(인오술군, 겁살 亥): 戌=화개
        val result = sinSalAnalyzer.analyze(wonguk(), TwelveSinSalAnalyzer.Base.DAY_JI)
        assertEquals(TwelveSinSal.HWAGAE, result.day)
        assertEquals(TwelveSinSal.JANGSEONG, result.month, "午는 인오술군 장성살")
    }

    // ── 구조 불변식 ─────────────────────────────────────────────────────

    @Test
    fun `음간과 양간의 순환 방향이 반대`() {
        // 양간: 장생 다음 지지가 목욕 / 음간: 장생 이전 지지가 목욕
        CheonGan.entries.forEach { gan ->
            val jangsaengJi = JiJi.entries.first { UnSeong.of(gan, it) == UnSeong.JANGSAENG }
            val nextJi = JiJi.fromIndex(jangsaengJi.index + 1)
            val expected = if (gan.yinYang == YinYang.YANG) UnSeong.MOKYOK else UnSeong.YANG
            assertEquals(expected, UnSeong.of(gan, nextJi), "${gan.hangul} 순환 방향")
        }
    }
}
