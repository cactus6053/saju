package com.saju.reading

import com.saju.analysis.ElementStrengthAnalyzer
import com.saju.analysis.FortuneService
import com.saju.analysis.GyeokGukResult
import com.saju.analysis.IlunCalculator
import com.saju.analysis.YearlySummaryCalculator
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
        appendLine("위 원국 데이터를 근거로 평생 사주 풀이를 작성하세요.")
        appendLine()
        append(sectionInstruction(ReadingSections.WONGUK))
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
        appendLine("위 데이터를 근거로 대운 풀이(10년 단위 인생 흐름)를 작성하세요.")
        appendLine("독자가 자기 인생 이야기로 읽을 수 있게, 의미가 겹치는 대운은 과감히 묶거나 생략합니다.")
        appendLine()
        append(sectionInstruction(ReadingSections.DAEUN))
    }

    // ── 결혼운 (향후 10년 세운 스캔) ────────────────────────────────────

    fun buildMarriage(data: WongukData, scanStartYear: Int): String = buildString {
        val saju = data.saju
        val dm = saju.dayMaster
        val isMale = saju.birth.gender == com.saju.domain.core.Gender.MALE
        val spouseStar = if (isMale) SipSeong.JEONGJAE else SipSeong.JEONGGWAN
        val spouseGroup = if (isMale) com.saju.domain.core.SipSeongGroup.JAESEONG
                          else com.saju.domain.core.SipSeongGroup.GWANSEONG
        val dayJi = saju.dayPillar.ji

        append(wongukBlock(data))
        appendLine()
        appendLine("[결혼운 데이터]")
        appendLine("배우자성: ${spouseStar.hangul} (${if (isMale) "남명은 정재" else "여명은 정관"} 기준, 편성은 보조)")

        // 원국 내 배우자성 위치
        val positions = listOf("연간", "월간", "시간").zip(
            listOf(saju.yearPillar.gan, saju.monthPillar.gan, saju.hourPillar.gan)
        ) + listOf("연지", "월지", "일지", "시지").zip(
            saju.pillars.map { it.ji.janggan.first() }
        )
        val spouseSpots = positions.filter { (_, gan) -> SipSeong.of(dm, gan).group == spouseGroup }
            .map { (pos, gan) -> "$pos(${SipSeong.of(dm, gan).hangul})" }
        appendLine("원국 배우자성: " + spouseSpots.joinToString(", ").ifEmpty { "없음 (세운으로 들어올 때가 중요)" })

        appendLine("배우자궁(일지): ${dayJi.hanja}${dayJi.hangul} — 12운성 ${data.unSeong.day.hangul}")
        val daySpotRelations = data.wongukRelations.filter {
            com.saju.domain.core.PillarPosition.DAY in it.positions
        }
        appendLine(
            "배우자궁 원국관계: " + daySpotRelations.joinToString(", ") {
                "${it.type.hangul}(${it.positions.joinToString("·") { p -> p.hangul }})"
            }.ifEmpty { "없음" }
        )

        appendLine()
        appendLine("[향후 10년 세운 스캔] (${scanStartYear}~${scanStartYear + 9})")
        for (year in scanStartYear until scanStartYear + 10) {
            val seun = com.saju.domain.core.SixtyGapja.fromYear(year)
            val ganSS = SipSeong.of(dm, seun.gan)
            val jiSS = SipSeong.of(dm, seun.ji.janggan.first())
            val marks = buildList {
                if (ganSS == spouseStar || jiSS == spouseStar) add("배우자성")
                else if (ganSS.group == spouseGroup || jiSS.group == spouseGroup) add("보조배우자성")
                com.saju.domain.core.JiRelation.yukHapOf(seun.ji, dayJi)?.let { add("일지육합") }
                com.saju.domain.core.JiRelation.banHapOf(seun.ji, dayJi)?.let { add("일지반합") }
                if (com.saju.domain.core.JiRelation.isChung(seun.ji, dayJi)) add("일지충")
                if (com.saju.domain.core.JiRelation.isHyeong(seun.ji, dayJi)) add("일지형")
            }
            appendLine(
                "  ${year}년 ${seun.hanja} ${ganSS.hangul}/${jiSS.hangul}" +
                    (if (marks.isEmpty()) "" else " ★" + marks.joinToString("·"))
            )
        }

        appendLine()
        appendLine(
            """
            위 데이터를 근거로 결혼운 풀이를 작성하세요.

            판단 기준: 배우자성이 세운에 드는 해와 배우자궁(일지)이 합으로 동(動)하는
            해가 고전적 결혼 적기이며, 두 신호가 겹치는 해가 가장 강합니다.
            일지충·형이 있는 해는 관계 변동에 주의가 필요한 해입니다.
            """.trimIndent()
        )
        appendLine()
        append(sectionInstruction(ReadingSections.MARRIAGE))
    }

    // ── 일일 운세 ───────────────────────────────────────────────────────

    // 점수·행운 요소는 엔진(IlunCalculator)이 결정 — LLM은 문구만 생성한다
    fun buildDaily(data: WongukData, ilun: IlunCalculator.IlunResult): String = buildString {
        append(wongukBlock(data))
        appendLine()
        appendLine("[일운 ${ilun.date}]")
        appendLine(
            "일진: ${ilun.ganJi.hanja} ${ilun.ganSipSeong.hangul}/${ilun.jiSipSeong.hangul} " +
                "12운성 ${ilun.unSeong.hangul}"
        )
        appendLine("원국관계: " + formatUnRelations(ilun.relationsWithWonguk))
        appendLine("오늘 점수: ${ilun.score}/5")
        appendLine()
        append(
            """
            위 데이터를 근거로 ${ilun.date} 하루 운세를 작성하세요.
            점수(${ilun.score}/5)의 기조를 따르고, 일진 십성과 합충을 근거로 삼되
            전문용어는 일상어로 풀어 씁니다.

            출력 형식 (정확히 지킬 것):
            - 첫 줄: 포토카드용 한 줄 운세 — 30자 이내, 따옴표와 마침표 없이
            - 빈 줄 하나
            - 이후: 오늘의 메시지 2~4문장 (200~350자), 완결된 문장으로 끝낼 것
            마크다운 헤더·목록 없이 순수 문장만 출력합니다.
            """.trimIndent()
        )
    }

    // ── 연간 운세 요약 (FE 카드 UI용 구조화 JSON) ──────────────────────

    // 점수는 엔진(YearlySummaryCalculator)이 결정 — LLM은 점수 기조에 맞는 요약문만 생성
    fun buildYearlySummary(
        data: WongukData,
        fortune: FortuneService.YearlyFortune,
        categoryScores: List<YearlySummaryCalculator.CategoryScore>,
        monthScores: List<YearlySummaryCalculator.MonthScore>,
    ): String = buildString {
        append(wongukBlock(data))
        appendLine()
        appendLine("[${fortune.year}년 세운] ${fortune.seun.ganJi.hanja} " +
            "${fortune.seun.ganSipSeong.hangul}/${fortune.seun.jiSipSeong.hangul} " +
            "원국관계: " + formatUnRelations(fortune.seun.relationsWithWonguk))
        appendLine()
        appendLine("[카테고리 점수(5점 만점)] " +
            categoryScores.joinToString(" ") { "${it.category.name}:${it.score}" })
        appendLine()
        appendLine("[월별] 간지 십성 점수 원국관계")
        fortune.wolunList.zip(monthScores).forEach { (w, s) ->
            appendLine(
                "  ${w.month}월 ${w.ganJi.hanja} ${w.ganSipSeong.hangul}/${w.jiSipSeong.hangul} " +
                    "${s.score}점 " + formatUnRelations(w.relationsWithWonguk)
            )
        }
        appendLine()
        append(
            """
            위 데이터를 근거로 ${fortune.year}년 운세 요약을 작성하세요.

            - categories: 5개 카테고리별 요약 1~2문장(40~90자). 주어진 점수의 기조와
              일치시킬 것 (2점은 조심, 5점은 순풍). LOVE는 성별(위 원국의 성별) 기준
              이성운, HEALTH는 의학적 단정 금지.
            - months: 1~12월 각각 요약 1문장(25~60자). 간지 십성과 합충을 근거로.

            아래 JSON 형식으로만 출력하세요. 코드펜스·설명문 없이 순수 JSON 하나만:
            {"categories":{"OVERALL":"...","MONEY":"...","LOVE":"...","HEALTH":"...","CAREER":"..."},"months":{"1":"...","2":"...","3":"...","4":"...","5":"...","6":"...","7":"...","8":"...","9":"...","10":"...","11":"...","12":"..."}}
            """.trimIndent()
        )
    }

    // ── 연도별 운세 ─────────────────────────────────────────────────────

    fun buildYearly(
        data: WongukData,
        fortune: FortuneService.YearlyFortune,
        topic: ReadingTopic = ReadingTopic.GENERAL,
    ): String = buildString {
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
        append(yearlyInstruction(topic, fortune.year))
    }

    private fun yearlyInstruction(topic: ReadingTopic, year: Int): String {
        val intro = when (topic) {
            ReadingTopic.GENERAL -> "위 데이터를 근거로 ${year}년 운세 해석을 작성하세요."

            ReadingTopic.MONEY ->
                "위 데이터를 근거로 ${year}년 금전운 해석을 작성하세요.\n" +
                    "재성(편재·정재)과 관련된 데이터를 중심으로 읽어냅니다."

            ReadingTopic.CAREER ->
                "위 데이터를 근거로 ${year}년 직장운(커리어) 해석을 작성하세요.\n" +
                    "관성(직장·직책)과 식상(이직·독립·표현), 역마살, 충(변동 신호)을 중심으로 읽어냅니다."

            ReadingTopic.HEALTH ->
                "위 데이터를 근거로 ${year}년 건강운 해석을 작성하세요.\n" +
                    "오행-장부 대응(전통 명리 관점): 木=간·담·눈·근육, 火=심장·혈관·소장,\n" +
                    "土=비장·위·소화기, 金=폐·대장·호흡기·피부, 水=신장·방광·생식기·뼈.\n" +
                    "특정 질병을 단정하지 않습니다."

            ReadingTopic.LOVE ->
                "위 데이터를 근거로 ${year}년 애정운(연애·관계) 해석을 작성하세요.\n" +
                    "이성성(남명은 재성=정재·편재, 여명은 관성=정관·편관 — 위 성별 참고)의\n" +
                    "흐름과 도화살·원진살, 배우자궁(일지)의 합충을 중심으로 읽어냅니다.\n" +
                    "결혼 시기 판단이 아니라 이 해의 연애·관계 기류 해석이 목적입니다."
        }
        return intro + "\n\n" + sectionInstruction(ReadingSections.yearly(topic))
    }

    // 섹션 스펙 → 내용 지시 + 고정 JSON 출력 형식 (검증·응답 구성과 같은 스펙 공유)
    private fun sectionInstruction(specs: List<SectionSpec>): String = buildString {
        appendLine("섹션별 내용:")
        specs.forEach { appendLine("- ${it.key} (${it.title}): ${it.guide}") }
        appendLine()
        appendLine("마크다운 없이 완결된 문장으로 쓰고, 아래 JSON 형식으로만 출력하세요.")
        appendLine("코드펜스·설명문 없이 순수 JSON 하나만:")
        append("{" + specs.joinToString(",") { "\"${it.key}\":\"...\"" } + "}")
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
