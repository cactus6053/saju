package com.saju.analysis

import com.saju.domain.core.Element
import com.saju.domain.core.GanJi
import com.saju.domain.core.SipSeong
import com.saju.domain.core.SixtyGapja
import com.saju.domain.core.UnSeong
import com.saju.engine.DayGanjiCalculator
import com.saju.engine.SajuResult
import java.time.LocalDate

// FE(SajuSays) 행운의 색 인덱스 0~5와 정렬
enum class LuckyColor(val hangul: String) {
    RED("빨강"), BLUE("파랑"), GREEN("초록"), YELLOW("노랑"), PURPLE("보라"), GOLD("금색"),
}

// FE(SajuSays) 행운의 아이템 인덱스 0~7과 정렬
enum class LuckyItem(val hangul: String) {
    WALLET("지갑"), WATCH("손목시계"), BOOK("책"), COFFEE("커피"),
    UMBRELLA("우산"), SHOES("신발"), KEYRING("열쇠고리"), HANDKERCHIEF("손수건"),
}

// 일운(日運): 특정 날짜의 일진과 원국의 관계, 점수(1~5), 행운 요소.
// 모든 출력은 결정적 — 같은 (사주, 용신, 날짜)면 항상 같은 결과.
class IlunCalculator(
    private val dayGanjiCalculator: DayGanjiCalculator = DayGanjiCalculator(),
    private val relationAnalyzer: RelationAnalyzer = RelationAnalyzer(),
) {

    data class IlunResult(
        val date: LocalDate,
        val ganJi: GanJi,                             // 일진
        val ganSipSeong: SipSeong,                    // 일간 기준 일진 천간의 십성
        val jiSipSeong: SipSeong,                     // 일간 기준 일진 지지 본기의 십성
        val unSeong: UnSeong,                         // 일간 기준 일진 지지의 12운성
        val relationsWithWonguk: List<UnRelation>,    // 일진 vs 원국 합충형파해
        val score: Int,                               // 1~5
        val luckyColor: LuckyColor,
        val luckyNumber: Int,                         // 1~9
        val luckyItem: LuckyItem,
    )

    companion object {
        private val FAVORABLE = setOf(
            RelationType.GAN_HAP, RelationType.YUK_HAP,
            RelationType.SAM_HAP, RelationType.BAN_HAP, RelationType.BANG_HAP,
        )

        // 개운색은 용신 오행의 오방색 (보라는 FE 팔레트 호환용으로만 존재)
        private val ELEMENT_COLOR = mapOf(
            Element.WOOD to LuckyColor.GREEN,
            Element.FIRE to LuckyColor.RED,
            Element.EARTH to LuckyColor.YELLOW,
            Element.METAL to LuckyColor.GOLD,
            Element.WATER to LuckyColor.BLUE,
        )

        // 하도(河圖) 수리: 생수/성수 쌍. 土의 성수 10은 1~9 밖이라 생수 5로 고정
        private val ELEMENT_NUMBERS = mapOf(
            Element.WOOD to Pair(3, 8),
            Element.FIRE to Pair(2, 7),
            Element.EARTH to Pair(5, 5),
            Element.METAL to Pair(4, 9),
            Element.WATER to Pair(1, 6),
        )
    }

    fun ganJiOf(date: LocalDate): GanJi = dayGanjiCalculator.calculate(date)

    fun analyze(saju: SajuResult, gyeokGuk: GyeokGukResult, date: LocalDate): IlunResult {
        val ganJi = ganJiOf(date)
        val dm = saju.dayMaster
        val relations = relationAnalyzer.analyzeWithUn(saju, ganJi)

        return IlunResult(
            date = date,
            ganJi = ganJi,
            ganSipSeong = SipSeong.of(dm, ganJi.gan),
            jiSipSeong = SipSeong.of(dm, ganJi.ji.janggan.first()),
            unSeong = UnSeong.of(dm, ganJi.ji),
            relationsWithWonguk = relations,
            score = score(ganJi, gyeokGuk, relations),
            luckyColor = ELEMENT_COLOR.getValue(gyeokGuk.yongsin),
            luckyNumber = luckyNumber(ganJi, gyeokGuk.yongsin),
            luckyItem = luckyItem(ganJi, saju),
        )
    }

    // 용신 오행 일치(천간 +2, 지지 +2, 조후 각 +1)와 합충 개수(합 +1, 충형파해 -1)를
    // 합산해 1~5로 매핑. LLM에 맡기지 않는 결정적 규칙 — 재현성·캐시 안정성 목적.
    private fun score(ganJi: GanJi, gyeokGuk: GyeokGukResult, relations: List<UnRelation>): Int {
        var points = 0
        if (ganJi.gan.element == gyeokGuk.yongsin) points += 2
        if (ganJi.ji.element == gyeokGuk.yongsin) points += 2
        gyeokGuk.johuYongsin?.let { johu ->
            if (ganJi.gan.element == johu) points += 1
            if (ganJi.ji.element == johu) points += 1
        }
        relations.forEach { points += if (it.type in FAVORABLE) 1 else -1 }

        return when {
            points <= -2 -> 1
            points == -1 -> 2
            points <= 1 -> 3
            points <= 3 -> 4
            else -> 5
        }
    }

    // 용신 오행의 하도 수리 쌍에서 일진 순서(60갑자 index) 홀짝으로 택일 — 날마다 교대
    private fun luckyNumber(ganJi: GanJi, yongsin: Element): Int {
        val (a, b) = ELEMENT_NUMBERS.getValue(yongsin)
        return if (SixtyGapja.indexOf(ganJi) % 2 == 0) a else b
    }

    // 아이템은 명리 근거가 없는 재미 요소 — 일진과 일간으로 시드한 결정적 순환
    private fun luckyItem(ganJi: GanJi, saju: SajuResult): LuckyItem {
        val index = (SixtyGapja.indexOf(ganJi) + saju.dayMaster.ordinal) % LuckyItem.entries.size
        return LuckyItem.entries[index]
    }
}
