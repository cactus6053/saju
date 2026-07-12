package com.saju.reading

import com.saju.analysis.ElementStrengthAnalyzer
import com.saju.analysis.FortuneService
import com.saju.analysis.GyeokGukResult
import com.saju.analysis.PillarRelation
import com.saju.analysis.SinSalHit
import com.saju.analysis.SipSeongAnalyzer
import com.saju.analysis.UnRelation
import com.saju.analysis.UnSeongAnalyzer
import com.saju.domain.core.SipSeong
import com.saju.domain.core.UnSeong
import com.saju.engine.DaeunStartCalculator
import com.saju.engine.SajuResult

// 엔진 계산 결과를 LLM 입력용 압축 텍스트로 직렬화.
// 결정적(deterministic)이어야 함 — 이 출력의 해시가 영구 캐시 키가 된다.
object ReadingPromptBuilder {

    data class WongukData(
        val saju: SajuResult,
        val sipSeong: SipSeongAnalyzer.SajuSipSeong,
        val gyeokGuk: GyeokGukResult,
        val strength: ElementStrengthAnalyzer.ElementStrength,
        val sinSal: List<SinSalHit>,
        val unSeong: UnSeongAnalyzer.UnSeongResult,
        val wongukRelations: List<PillarRelation>,
    )

    // ── 원국 풀이 (평생사주) ─────────────────────────────────────────────

    fun buildWonguk(data: WongukData): String = buildString {
        append(wongukBlock(data))
        appendLine()
        append(
            """
            위 원국 데이터를 근거로 평생 사주 풀이를 작성하세요.

            출력 구조:
            1. **일간과 기질** — 일간 오행의 본질과 사주 전체가 만드는 성격 구조
            2. **격국과 용신** — 이 사주가 택한 삶의 전략, 용신이 가리키는 보완 방향
            3. **오행 균형** — 과다·결핍이 만드는 강점과 약점, 생활 속 보완법
            4. **신살과 특이점** — 검출된 신살·12운성이 더하는 색채
            5. **적성과 조언** — 구조에 어울리는 방향 제안과 한 줄 요약

            분량: 한국어 800~1200자.
            """.trimIndent()
        )
    }

    // ── 대운 풀이 (10년 단위 인생 흐름) ─────────────────────────────────

    fun buildDaeun(
        data: WongukData,
        direction: DaeunStartCalculator.Direction,
        timeline: List<FortuneService.DaeunFortune>,
    ): String = buildString {
        append(wongukBlock(data))
        appendLine()
        appendLine("[대운 타임라인] ${direction.hangul}")

        val dayMaster = data.saju.dayMaster
        timeline.forEach { entry ->
            val d = entry.daeun
            val ganSipSeong = SipSeong.of(dayMaster, d.ganJi.gan)
            val jiSipSeong = SipSeong.of(dayMaster, d.ganJi.ji.janggan.first())
            val unSeong = UnSeong.of(dayMaster, d.ganJi.ji)
            appendLine(
                "  ${d.order}대운 ${d.ganJi.hanja}(${d.startAge}~${d.endAge}세) " +
                    "${ganSipSeong.hangul}/${jiSipSeong.hangul} ${unSeong.hangul} " +
                    "원국관계: " + formatUnRelations(entry.relationsWithWonguk)
            )
        }

        appendLine()
        append(
            """
            위 데이터를 근거로 대운 풀이(10년 단위 인생 흐름)를 작성하세요.

            출력 구조:
            1. **대운의 방향** — 순행/역행과 원국 구조가 만드는 큰 그림 (2~3문장)
            2. **인생 흐름** — 청년기~중년기(대체로 2~6대운)를 중심으로 3개 구간으로
               묶어 서술. 각 구간은 십성 주제와 원국 관계(합충형파해)를 근거로.
               말년 대운은 한두 문장으로만 언급하고, 80세 이후 대운은 다루지 않는다
            3. **전환점** — 기운이 가장 크게 바뀌는 대운 하나와 그 이유
            4. **용신 대운** — 용신 오행이 들어오는 대운과 그 시기의 의미
            5. **한 줄 요약** — 인생 전체를 관통하는 조언 한 문장

            주의: 10개 대운을 빠짐없이 나열하지 말 것. 의미가 겹치는 대운은 과감히
            묶거나 생략하고, 독자가 자기 인생 이야기로 읽을 수 있게 쓸 것.

            분량: 한국어 1500~2000자. 반드시 이 분량 안에서 완결된 문장으로 끝낼 것.
            """.trimIndent()
        )
    }

    // ── 연도별 운세 ─────────────────────────────────────────────────────

    fun buildYearly(data: WongukData, fortune: FortuneService.YearlyFortune): String = buildString {
        append(wongukBlock(data))
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
        append(
            """
            위 데이터를 근거로 ${fortune.year}년 운세 해석을 작성하세요.

            출력 구조:
            1. **원국 개관** — 일간·격국·용신이 말하는 기질 구조 (간략히)
            2. **현재 대운** — 대운 간지의 십성 의미와 원국 관계가 만드는 10년 흐름
            3. **${fortune.year}년 운세** — 세운의 십성 주제, 원국·대운과의 합충형파해가
               만드는 사건 방향. 삼재 여부 언급
            4. **월별 흐름** — 길한 달과 조심할 달을 근거와 함께 묶어 서술 (의미 있는 것만)
            5. **한 줄 요약** — 이 해를 관통하는 조언 한 문장

            분량: 한국어 800~1200자.
            """.trimIndent()
        )
    }

    // ── 공통 원국 블록 ──────────────────────────────────────────────────

    private fun wongukBlock(data: WongukData): String = buildString {
        val saju = data.saju
        appendLine("[원국]")
        appendLine("팔자: ${saju.paljaHanja} (일간 ${saju.dayMaster.hanja}${saju.dayMaster.hangul}, ${saju.dayMaster.element.hangul})")
        appendLine("성별: ${saju.birth.gender.hangul}, 사주연도: ${saju.sajuYear}")

        val ss = data.sipSeong.pillars
        appendLine(
            "십성(간/지본기): " +
                listOf("연", "월", "일", "시").zip(ss).joinToString(" ") { (name, p) ->
                    "$name:${p.gan?.hangul ?: "일간"}/${p.jiPrincipal.hangul}"
                }
        )

        appendLine(
            "격국: ${data.gyeokGuk.gyeokGuk.hangul}(${data.gyeokGuk.gyeokGuk.category.name}), " +
                "용신 ${data.gyeokGuk.yongsin.hangul}, 조후 ${data.gyeokGuk.johuYongsin?.hangul ?: "없음"}, " +
                (if (data.strength.isSinGang) "신강" else "신약")
        )

        appendLine(
            "오행점수: " + data.strength.scores.entries
                .sortedBy { it.key.ordinal }
                .joinToString(" ") { "${it.key.hangul}${"%.2f".format(it.value)}" } +
                " / 과다 ${data.strength.excessive.joinToString(",") { it.hangul }.ifEmpty { "없음" }}" +
                " / 결핍 ${data.strength.deficient.joinToString(",") { it.hangul }.ifEmpty { "없음" }}"
        )

        appendLine(
            "신살: " + data.sinSal.joinToString(", ") { "${it.sinSal.hangul}(${it.position.hangul})" }
                .ifEmpty { "없음" }
        )

        appendLine(
            "12운성: 연${data.unSeong.year.hangul} 월${data.unSeong.month.hangul} " +
                "일${data.unSeong.day.hangul} 시${data.unSeong.hour.hangul}"
        )

        appendLine("원국관계: " + formatPillarRelations(data.wongukRelations))
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
