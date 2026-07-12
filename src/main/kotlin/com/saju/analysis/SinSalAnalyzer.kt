package com.saju.analysis

import com.saju.domain.core.CheonGan
import com.saju.domain.core.CheonGan.*
import com.saju.domain.core.JiJi
import com.saju.domain.core.JiJi.*
import com.saju.domain.core.PillarPosition
import com.saju.domain.core.SinSal
import com.saju.domain.core.SixtyGapja
import com.saju.engine.SajuResult

data class SinSalHit(
    val sinSal: SinSal,
    val position: PillarPosition,
)

class SinSalAnalyzer {

    companion object {
        // 천을귀인: 일간 기준 (갑무경 축미, 을기 자신, 병정 해유, 임계 사묘, 신 오인)
        private val CHEONEUL: Map<CheonGan, Set<JiJi>> = mapOf(
            GAP to setOf(CHUK, MI), MU to setOf(CHUK, MI), GYEONG to setOf(CHUK, MI),
            EUL to setOf(JA, SHIN), GI to setOf(JA, SHIN),
            BYEONG to setOf(HAE, YU), JEONG to setOf(HAE, YU),
            IM to setOf(SA, MYO), GYE to setOf(SA, MYO),
            SIN to setOf(O, IN),
        )

        // 문창귀인: 일간 기준
        private val MUNCHANG: Map<CheonGan, JiJi> = mapOf(
            GAP to SA, EUL to O, BYEONG to SHIN, JEONG to YU, MU to SHIN,
            GI to YU, GYEONG to HAE, SIN to JA, IM to IN, GYE to MYO,
        )

        // 학당귀인: 일간의 장생지
        private val HAKDANG: Map<CheonGan, JiJi> = mapOf(
            GAP to HAE, EUL to O, BYEONG to IN, JEONG to YU, MU to IN,
            GI to YU, GYEONG to SA, SIN to JA, IM to SHIN, GYE to MYO,
        )

        // 천덕귀인: 월지 기준 → 천간 또는 지지 (한자 문자로 대조)
        private val CHEONDEOK: Map<JiJi, Char> = mapOf(
            IN to '丁', MYO to '申', JIN to '壬', SA to '辛', O to '亥', MI to '甲',
            SHIN to '癸', YU to '寅', SUL to '丙', HAE to '乙', JA to '巳', CHUK to '庚',
        )

        // 월덕귀인: 월지 삼합 기준 → 천간
        private val WOLDEOK: Map<JiJi, CheonGan> = mapOf(
            IN to BYEONG, O to BYEONG, SUL to BYEONG,     // 寅午戌 화국 → 丙
            SHIN to IM, JA to IM, JIN to IM,              // 申子辰 수국 → 壬
            HAE to GAP, MYO to GAP, MI to GAP,            // 亥卯未 목국 → 甲
            SA to GYEONG, YU to GYEONG, CHUK to GYEONG,   // 巳酉丑 금국 → 庚
        )

        // 역마살: 기준 지지의 삼합군 → 충하는 생지
        private val YEOKMA: Map<JiJi, JiJi> = mapOf(
            SHIN to IN, JA to IN, JIN to IN,
            IN to SHIN, O to SHIN, SUL to SHIN,
            SA to HAE, YU to HAE, CHUK to HAE,
            HAE to SA, MYO to SA, MI to SA,
        )

        // 도화살: 기준 지지의 삼합군 → 왕지 다음 목욕지
        private val DOHWA: Map<JiJi, JiJi> = mapOf(
            SHIN to YU, JA to YU, JIN to YU,
            IN to MYO, O to MYO, SUL to MYO,
            SA to O, YU to O, CHUK to O,
            HAE to JA, MYO to JA, MI to JA,
        )

        // 백호대살: 해당 간지 자체
        private val BAEKHO: Set<String> = setOf("甲辰", "乙未", "丙戌", "丁丑", "戊辰", "壬戌", "癸丑")

        // 양인살: 양간 일간 기준
        private val YANGIN: Map<CheonGan, JiJi> = mapOf(
            GAP to MYO, BYEONG to O, MU to O, GYEONG to YU, IM to JA,
        )

        // 원진살: 6쌍 — 일지(배우자궁) 기준으로 판정
        private val WONJIN_PAIRS: List<Set<JiJi>> = listOf(
            setOf(JA, MI), setOf(CHUK, O), setOf(IN, YU),
            setOf(MYO, SHIN), setOf(JIN, HAE), setOf(SA, SUL),
        )

        // 고신·과숙살: 연지의 방합(계절) 그룹 기준
        // 다음 계절의 생지 = 고신, 이전 계절의 고지 = 과숙
        private val GOSIN: Map<JiJi, JiJi> = buildMap {
            listOf(HAE, JA, CHUK).forEach { put(it, IN) }   // 해자축생 → 고신 寅
            listOf(IN, MYO, JIN).forEach { put(it, SA) }    // 인묘진생 → 고신 巳
            listOf(SA, O, MI).forEach { put(it, SHIN) }     // 사오미생 → 고신 申
            listOf(SHIN, YU, SUL).forEach { put(it, HAE) }  // 신유술생 → 고신 亥
        }

        private val GWASUK: Map<JiJi, JiJi> = buildMap {
            listOf(HAE, JA, CHUK).forEach { put(it, SUL) }  // 해자축생 → 과숙 戌
            listOf(IN, MYO, JIN).forEach { put(it, CHUK) }  // 인묘진생 → 과숙 丑
            listOf(SA, O, MI).forEach { put(it, JIN) }      // 사오미생 → 과숙 辰
            listOf(SHIN, YU, SUL).forEach { put(it, MI) }   // 신유술생 → 과숙 未
        }
    }

    fun analyze(saju: SajuResult): List<SinSalHit> {
        val hits = mutableListOf<SinSalHit>()
        val dayGan = saju.dayMaster
        val positions = PillarPosition.entries
        val pillars = saju.pillars // 연월일시 순서 = PillarPosition 순서

        fun eachJi(action: (PillarPosition, JiJi) -> Unit) =
            pillars.forEachIndexed { i, p -> action(positions[i], p.ji) }

        // 일간 기준 신살: 천을·문창·학당·양인
        eachJi { pos, ji ->
            if (ji in CHEONEUL.getValue(dayGan)) hits += SinSalHit(SinSal.CHEONEUL_GWIIN, pos)
            if (ji == MUNCHANG.getValue(dayGan)) hits += SinSalHit(SinSal.MUNCHANG_GWIIN, pos)
            if (ji == HAKDANG.getValue(dayGan)) hits += SinSalHit(SinSal.HAKDANG_GWIIN, pos)
            if (ji == YANGIN[dayGan]) hits += SinSalHit(SinSal.YANGIN_SAL, pos)
        }

        // 월지 기준 신살: 천덕·월덕 (월주 자신 제외한 3주 대상)
        val monthJi = saju.monthPillar.ji
        val cheondeokTarget = CHEONDEOK.getValue(monthJi)
        val woldeokTarget = WOLDEOK.getValue(monthJi)
        pillars.forEachIndexed { i, p ->
            if (positions[i] == PillarPosition.MONTH) return@forEachIndexed
            if (p.gan.hanja == cheondeokTarget || p.ji.hanja == cheondeokTarget) {
                hits += SinSalHit(SinSal.CHEONDEOK_GWIIN, positions[i])
            }
            if (p.gan == woldeokTarget) hits += SinSalHit(SinSal.WOLDEOK_GWIIN, positions[i])
        }

        // 연지·일지 기준 신살: 역마·도화
        val anchors = listOf(saju.yearPillar.ji, saju.dayPillar.ji)
        anchors.forEach { anchor ->
            eachJi { pos, ji ->
                if (ji == YEOKMA.getValue(anchor)) hits += SinSalHit(SinSal.YEOKMA, pos)
                if (ji == DOHWA.getValue(anchor)) hits += SinSalHit(SinSal.DOHWA, pos)
            }
        }

        // 백호대살: 각 주 간지 자체
        pillars.forEachIndexed { i, p ->
            if (p.hanja in BAEKHO) hits += SinSalHit(SinSal.BAEKHO, positions[i])
        }

        // 공망: 일주 기준 순중공망 — 연·월·시지 대상
        val gongmang = gongmangOf(saju.dayPillar.let { SixtyGapja.indexOf(it) })
        pillars.forEachIndexed { i, p ->
            if (positions[i] == PillarPosition.DAY) return@forEachIndexed
            if (p.ji in gongmang) hits += SinSalHit(SinSal.GONGMANG, positions[i])
        }

        // 원진살: 일지(배우자궁) 기준 — 다른 지지와 원진 쌍이면 상대 위치에 표시
        val dayJi = saju.dayPillar.ji
        pillars.forEachIndexed { i, p ->
            if (positions[i] == PillarPosition.DAY) return@forEachIndexed
            if (WONJIN_PAIRS.any { it == setOf(dayJi, p.ji) }) {
                hits += SinSalHit(SinSal.WONJIN, positions[i])
            }
        }

        // 고신·과숙살: 연지 기준 — 다른 지지에서 해당 지지 검색
        val yearJi = saju.yearPillar.ji
        pillars.forEachIndexed { i, p ->
            if (positions[i] == PillarPosition.YEAR) return@forEachIndexed
            if (p.ji == GOSIN.getValue(yearJi)) hits += SinSalHit(SinSal.GOSIN, positions[i])
            if (p.ji == GWASUK.getValue(yearJi)) hits += SinSalHit(SinSal.GWASUK, positions[i])
        }

        return hits.distinct()
    }

    // 일주가 속한 순(旬)의 공망 지지 2개
    fun gongmangOf(dayGanjiIndex: Int): Set<JiJi> {
        val soonStart = dayGanjiIndex - dayGanjiIndex % 10
        return setOf(JiJi.fromIndex(soonStart + 10), JiJi.fromIndex(soonStart + 11))
    }
}
