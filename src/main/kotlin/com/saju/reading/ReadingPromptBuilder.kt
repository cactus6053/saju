package com.saju.reading

import com.saju.analysis.ElementStrengthAnalyzer
import com.saju.analysis.FortuneService
import com.saju.analysis.GyeokGukResult
import com.saju.analysis.PillarRelation
import com.saju.analysis.SinSalHit
import com.saju.analysis.SipSeongAnalyzer
import com.saju.analysis.UnRelation
import com.saju.analysis.UnSeongAnalyzer
import com.saju.engine.SajuResult

// 엔진 계산 결과를 LLM 입력용 압축 텍스트로 직렬화.
// 결정적(deterministic)이어야 함 — 이 출력의 해시가 영구 캐시 키가 된다.
object ReadingPromptBuilder {

    fun build(
        saju: SajuResult,
        sipSeong: SipSeongAnalyzer.SajuSipSeong,
        gyeokGuk: GyeokGukResult,
        strength: ElementStrengthAnalyzer.ElementStrength,
        sinSal: List<SinSalHit>,
        unSeong: UnSeongAnalyzer.UnSeongResult,
        wongukRelations: List<PillarRelation>,
        fortune: FortuneService.YearlyFortune,
    ): String = buildString {
        appendLine("[원국]")
        appendLine("팔자: ${saju.paljaHanja} (일간 ${saju.dayMaster.hanja}${saju.dayMaster.hangul}, ${saju.dayMaster.element.hangul})")
        appendLine("성별: ${saju.birth.gender.hangul}, 사주연도: ${saju.sajuYear}")

        val ss = sipSeong.pillars
        appendLine(
            "십성(간/지본기): " +
                listOf("연", "월", "일", "시").zip(ss).joinToString(" ") { (name, p) ->
                    "$name:${p.gan?.hangul ?: "일간"}/${p.jiPrincipal.hangul}"
                }
        )

        appendLine(
            "격국: ${gyeokGuk.gyeokGuk.hangul}(${gyeokGuk.gyeokGuk.category.name}), " +
                "용신 ${gyeokGuk.yongsin.hangul}, 조후 ${gyeokGuk.johuYongsin?.hangul ?: "없음"}, " +
                (if (strength.isSinGang) "신강" else "신약")
        )

        appendLine(
            "오행점수: " + strength.scores.entries
                .sortedBy { it.key.ordinal }
                .joinToString(" ") { "${it.key.hangul}${"%.2f".format(it.value)}" } +
                " / 과다 ${strength.excessive.joinToString(",") { it.hangul }.ifEmpty { "없음" }}" +
                " / 결핍 ${strength.deficient.joinToString(",") { it.hangul }.ifEmpty { "없음" }}"
        )

        appendLine(
            "신살: " + sinSal.joinToString(", ") { "${it.sinSal.hangul}(${it.position.hangul})" }
                .ifEmpty { "없음" }
        )

        appendLine(
            "12운성: 연${unSeong.year.hangul} 월${unSeong.month.hangul} " +
                "일${unSeong.day.hangul} 시${unSeong.hour.hangul}"
        )

        appendLine("원국관계: " + formatPillarRelations(wongukRelations))

        appendLine()
        appendLine("[운세 ${fortune.year}년, 나이 ${fortune.age}]")

        val daeun = fortune.currentDaeun
        if (daeun == null) {
            appendLine("대운: 기산 전")
        } else {
            appendLine(
                "대운: ${daeun.ganJi.hanja}(${daeun.startAge}~${daeun.endAge}세) " +
                    "원국관계: " + formatUnRelations(fortune.daeunRelations)
            )
        }

        appendLine(
            "세운: ${fortune.seun.ganJi.hanja} ${fortune.seun.ganSipSeong.hangul}/${fortune.seun.jiSipSeong.hangul} " +
                "원국관계: " + formatUnRelations(fortune.seun.relationsWithWonguk) +
                " / 대운관계: " + fortune.seun.relationsWithDaeun.joinToString(",") { it.type.hangul }.ifEmpty { "없음" }
        )

        appendLine("삼재: " + if (fortune.wolunList.first().isSamjae) "해당" else "아님")

        appendLine("월운:")
        fortune.wolunList.forEach { w ->
            appendLine(
                "  ${w.month}월 ${w.ganJi.hanja} ${w.ganSipSeong.hangul}/${w.jiSipSeong.hangul} ${w.gilHyung.hangul}" +
                    " 원국:" + formatUnRelations(w.relationsWithWonguk) +
                    " 세운:" + w.relationsWithSeun.joinToString(",") { it.type.hangul }.ifEmpty { "없음" } +
                    " 대운:" + w.relationsWithDaeun.joinToString(",") { it.type.hangul }.ifEmpty { "없음" }
            )
        }

        appendLine()
        append("위 데이터를 근거로 사주 해석문을 작성하세요.")
    }

    private fun formatPillarRelations(relations: List<PillarRelation>): String =
        relations.joinToString(", ") { r ->
            "${r.type.hangul}(${r.positions.joinToString("·") { it.hangul }}" +
                (r.resultElement?.let { "→${it.hangul}" } ?: "") + ")"
        }.ifEmpty { "없음" }

    private fun formatUnRelations(relations: List<UnRelation>): String =
        relations.joinToString(",") { r ->
            "${r.type.hangul}(${r.positions.joinToString("·") { it.hangul }}" +
                (r.resultElement?.let { "→${it.hangul}" } ?: "") + ")"
        }.ifEmpty { "없음" }
}
