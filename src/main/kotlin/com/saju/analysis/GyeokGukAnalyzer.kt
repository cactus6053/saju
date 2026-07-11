package com.saju.analysis

import com.saju.domain.core.CheonGan
import com.saju.domain.core.Element
import com.saju.domain.core.JiJi
import com.saju.domain.core.SipSeong
import com.saju.domain.core.SipSeongGroup
import com.saju.domain.core.controls
import com.saju.domain.core.generates
import com.saju.engine.SajuResult

enum class GyeokGukCategory { INNER, OUTER }

enum class GyeokGuk(val hangul: String, val category: GyeokGukCategory) {
    // 내격 (월지 본기 십성 기준)
    GEONROK("건록격", GyeokGukCategory.INNER),
    YANGIN("양인격", GyeokGukCategory.INNER),
    SIKSIN_GYEOK("식신격", GyeokGukCategory.INNER),
    SANGGWAN_GYEOK("상관격", GyeokGukCategory.INNER),
    PYEONJAE_GYEOK("편재격", GyeokGukCategory.INNER),
    JEONGJAE_GYEOK("정재격", GyeokGukCategory.INNER),
    PYEONGWAN_GYEOK("편관격", GyeokGukCategory.INNER),
    JEONGGWAN_GYEOK("정관격", GyeokGukCategory.INNER),
    PYEONIN_GYEOK("편인격", GyeokGukCategory.INNER),
    JEONGIN_GYEOK("정인격", GyeokGukCategory.INNER),

    // 외격 - 종격
    JONGJAE("종재격", GyeokGukCategory.OUTER),
    JONGSAL("종살격", GyeokGukCategory.OUTER),
    JONGA("종아격", GyeokGukCategory.OUTER),

    // 외격 - 전왕격 (일행득기격)
    GOKJIK("곡직격", GyeokGukCategory.OUTER),   // 木
    YEOMSANG("염상격", GyeokGukCategory.OUTER), // 火
    GASAEK("가색격", GyeokGukCategory.OUTER),   // 土
    JONGHYEOK("종혁격", GyeokGukCategory.OUTER),// 金
    YUNHA("윤하격", GyeokGukCategory.OUTER),    // 水
}

data class GyeokGukResult(
    val gyeokGuk: GyeokGuk,
    val yongsin: Element,        // 억부(내격) 또는 순세(외격) 용신
    val johuYongsin: Element?,   // 조후 보조 용신 (겨울생→火, 여름생→水)
    val isSinGang: Boolean,
    val deukRyeong: Boolean,     // 득령: 월지 본기가 비겁·인성
    val deukJi: Boolean,         // 득지: 일지 본기가 비겁·인성
    val deukSe: Boolean,         // 득세: 나머지 5위치 중 비겁·인성 3개 이상
)

// 판정 기준(단순화된 결정적 규칙):
// - 위치 7곳: 연간·월간·시간 + 연지·월지·일지·시지 본기(장간 첫 글자)
// - 전왕격: 비겁 5곳 이상 & 관성 0곳
// - 종격: 비겁+인성 0곳 → 최다 세력(관성>재성>식상 우선순위)으로 종살/종재/종아
// - 내격: 월지 본기 십성 (비견→건록격, 겁재→양인격, 그 외→십성명+격)
// - 신강: 득령·득지·득세 중 2개 이상
// - 억부 용신: 신강(비겁 과다→관성, 인성 과다→재성 오행), 신약→인성 오행
class GyeokGukAnalyzer {

    private val supportGroups = setOf(SipSeongGroup.BIGEOP, SipSeongGroup.INSEONG)

    fun analyze(saju: SajuResult): GyeokGukResult {
        val dm = saju.dayMaster

        val ganPositions = listOf(saju.yearPillar.gan, saju.monthPillar.gan, saju.hourPillar.gan)
        val jiPrincipals = saju.pillars.map { it.ji.janggan.first() }
        val allPositions: List<CheonGan> = ganPositions + jiPrincipals
        val groups = allPositions.map { SipSeong.of(dm, it).group }

        val bigeop = groups.count { it == SipSeongGroup.BIGEOP }
        val inseong = groups.count { it == SipSeongGroup.INSEONG }
        val gwan = groups.count { it == SipSeongGroup.GWANSEONG }

        val johu = johuYongsin(saju.monthPillar.ji)

        // ── 외격: 전왕격 ──
        if (bigeop >= 5 && gwan == 0) {
            val gyeok = when (dm.element) {
                Element.WOOD -> GyeokGuk.GOKJIK
                Element.FIRE -> GyeokGuk.YEOMSANG
                Element.EARTH -> GyeokGuk.GASAEK
                Element.METAL -> GyeokGuk.JONGHYEOK
                Element.WATER -> GyeokGuk.YUNHA
            }
            return result(gyeok, yongsin = dm.element, johu, groups, saju)
        }

        // ── 외격: 종격 ──
        if (bigeop + inseong == 0) {
            val dominant = listOf(
                SipSeongGroup.GWANSEONG to GyeokGuk.JONGSAL,
                SipSeongGroup.JAESEONG to GyeokGuk.JONGJAE,
                SipSeongGroup.SIKSANG to GyeokGuk.JONGA,
            ).maxBy { (group, _) -> groups.count { it == group } }

            val yongsin = when (dominant.second) {
                GyeokGuk.JONGSAL -> controllerOf(dm.element)
                GyeokGuk.JONGJAE -> dm.element.controls
                else -> dm.element.generates
            }
            return result(dominant.second, yongsin, johu, groups, saju)
        }

        // ── 내격: 월지 본기 십성 ──
        val monthPrincipal = SipSeong.of(dm, saju.monthPillar.ji.janggan.first())
        val gyeok = when (monthPrincipal) {
            SipSeong.BIGYEON -> GyeokGuk.GEONROK
            SipSeong.GEOPJAE -> GyeokGuk.YANGIN
            SipSeong.SIKSIN -> GyeokGuk.SIKSIN_GYEOK
            SipSeong.SANGGWAN -> GyeokGuk.SANGGWAN_GYEOK
            SipSeong.PYEONJAE -> GyeokGuk.PYEONJAE_GYEOK
            SipSeong.JEONGJAE -> GyeokGuk.JEONGJAE_GYEOK
            SipSeong.PYEONGWAN -> GyeokGuk.PYEONGWAN_GYEOK
            SipSeong.JEONGGWAN -> GyeokGuk.JEONGGWAN_GYEOK
            SipSeong.PYEONIN -> GyeokGuk.PYEONIN_GYEOK
            SipSeong.JEONGIN -> GyeokGuk.JEONGIN_GYEOK
        }

        val (sinGang, deukRyeong, deukJi, deukSe) = sinGangOf(saju)
        val yongsin = if (sinGang) {
            if (bigeop >= inseong) controllerOf(dm.element) // 비겁 과다 → 관성으로 억제
            else dm.element.controls                        // 인성 과다 → 재성으로 극인
        } else {
            generatorOf(dm.element)                         // 신약 → 인성으로 생조
        }

        return GyeokGukResult(gyeok, yongsin, johu, sinGang, deukRyeong, deukJi, deukSe)
    }

    private data class SinGang(
        val isSinGang: Boolean,
        val deukRyeong: Boolean,
        val deukJi: Boolean,
        val deukSe: Boolean,
    )

    private fun sinGangOf(saju: SajuResult): SinGang {
        val dm = saju.dayMaster
        fun groupOf(gan: CheonGan) = SipSeong.of(dm, gan).group

        val deukRyeong = groupOf(saju.monthPillar.ji.janggan.first()) in supportGroups
        val deukJi = groupOf(saju.dayPillar.ji.janggan.first()) in supportGroups

        val rest = listOf(
            saju.yearPillar.gan, saju.monthPillar.gan, saju.hourPillar.gan,
            saju.yearPillar.ji.janggan.first(), saju.hourPillar.ji.janggan.first(),
        )
        val deukSe = rest.count { groupOf(it) in supportGroups } >= 3

        val isSinGang = listOf(deukRyeong, deukJi, deukSe).count { it } >= 2
        return SinGang(isSinGang, deukRyeong, deukJi, deukSe)
    }

    // 외격은 신강/신약 무의미 — 세력 기준으로만 기록
    private fun result(
        gyeok: GyeokGuk, yongsin: Element, johu: Element?,
        groups: List<SipSeongGroup>, saju: SajuResult,
    ): GyeokGukResult {
        val (sinGang, deukRyeong, deukJi, deukSe) = sinGangOf(saju)
        return GyeokGukResult(gyeok, yongsin, johu, sinGang, deukRyeong, deukJi, deukSe)
    }

    private fun johuYongsin(monthBranch: JiJi): Element? = when (monthBranch) {
        JiJi.HAE, JiJi.JA, JiJi.CHUK -> Element.FIRE  // 겨울생: 화 필요
        JiJi.SA, JiJi.O, JiJi.MI -> Element.WATER     // 여름생: 수 필요
        else -> null
    }

    private fun controllerOf(element: Element): Element =
        Element.entries.first { it.controls == element }

    private fun generatorOf(element: Element): Element =
        Element.entries.first { it.generates == element }
}
