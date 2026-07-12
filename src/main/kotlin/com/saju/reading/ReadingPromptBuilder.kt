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
        append(
            """
            위 데이터를 근거로 결혼운 풀이를 작성하세요.

            판단 기준: 배우자성이 세운에 드는 해와 배우자궁(일지)이 합으로 동(動)하는
            해가 고전적 결혼 적기이며, 두 신호가 겹치는 해가 가장 강합니다.
            일지충·형이 있는 해는 관계 변동에 주의가 필요한 해입니다.

            출력 구조:
            1. **배우자 인연의 구조** — 원국 배우자성 상태와 배우자궁이 말하는 인연의 모양
            2. **결혼 기운이 들어오는 시기** — 스캔에서 신호(★)가 있는 연도를 근거와 함께
               짚고, 신호가 겹치는 해를 강조. 신호 없는 해는 나열하지 않는다
            3. **주의할 요소** — 원진·고신·과숙·일지충 등 장애 신살이 있다면 그 의미와
               보완 관점 (없으면 이 섹션 생략)
            4. **조언** — 시기에 매이지 않는 관계 조언과 한 줄 요약

            분량: 한국어 1000~1400자. 반드시 완결된 문장으로 마무리할 것.
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

    private fun yearlyInstruction(topic: ReadingTopic, year: Int): String = when (topic) {
        ReadingTopic.GENERAL -> """
            위 데이터를 근거로 ${year}년 운세 해석을 작성하세요.

            출력 구조:
            1. **원국 개관** — 일간·격국·용신이 말하는 기질 구조 (간략히)
            2. **현재 대운** — 대운 간지의 십성 의미와 원국 관계가 만드는 10년 흐름
            3. **${year}년 운세** — 세운의 십성 주제, 원국·대운과의 합충형파해가
               만드는 사건 방향. 삼재 여부 언급
            4. **월별 흐름** — 길한 달과 조심할 달을 근거와 함께 묶어 서술 (의미 있는 것만)
            5. **한 줄 요약** — 이 해를 관통하는 조언 한 문장

            분량: 한국어 800~1200자. 반드시 완결된 문장으로 마무리할 것.
        """.trimIndent()

        ReadingTopic.MONEY -> """
            위 데이터를 근거로 ${year}년 금전운 해석을 작성하세요.
            재성(편재·정재)과 관련된 데이터를 중심으로 읽어냅니다.

            출력 구조:
            1. **타고난 재물 그릇** — 원국의 재성 상태와 신강/신약이 말하는 재물 체질
               (신약한데 재성이 강하면 "감당" 이슈, 재성이 용신이면 순풍 구조 등)
            2. **${year}년 재물 흐름** — 세운·대운에서 재성이 어떻게 움직이는지,
               합충이 만드는 수입·지출·투자 방향
            3. **월별 재물 포인트** — 재성이 들어오거나 충이 걸리는 달만 골라 서술
            4. **조언과 요약** — 이 해의 돈 관리 원칙 한 문장

            분량: 한국어 800~1200자. 반드시 완결된 문장으로 마무리할 것.
        """.trimIndent()

        ReadingTopic.CAREER -> """
            위 데이터를 근거로 ${year}년 직장운(커리어) 해석을 작성하세요.
            관성(직장·직책)과 식상(이직·독립·표현), 역마살, 충(변동 신호)을 중심으로
            읽어냅니다.

            출력 구조:
            1. **타고난 직업 구조** — 원국의 관성·식상 배치와 격국이 말하는 커리어 성향
               (조직형인지 독립형인지)
            2. **${year}년 직장 흐름** — 세운의 관성/식상 기운, 원국·대운과의 관계가
               만드는 승진·이동·이직 신호
            3. **이직·변동 판단** — 충·역마 등 변동 신호가 있는지, 있다면 움직이기
               좋은 시기인지 버틸 시기인지
            4. **월별 포인트** — 커리어 관련 의미 있는 달만 골라 서술
            5. **조언과 요약** — 이 해의 커리어 전략 한 문장

            분량: 한국어 800~1200자. 반드시 완결된 문장으로 마무리할 것.
        """.trimIndent()

        ReadingTopic.HEALTH -> """
            위 데이터를 근거로 ${year}년 건강운 해석을 작성하세요.

            오행-장부 대응(전통 명리 관점): 木=간·담·눈·근육, 火=심장·혈관·소장,
            土=비장·위·소화기, 金=폐·대장·호흡기·피부, 水=신장·방광·생식기·뼈.

            출력 구조:
            1. **타고난 체질 경향** — 원국의 과다/결핍 오행이 가리키는 취약 부위와
               강한 부위 (위 대응표 근거)
            2. **${year}년 건강 흐름** — 세운이 강화하는 오행이 기신인지 희신인지,
               충·형이 걸리는 부위 관점
            3. **조심할 달** — 충·형이 집중되거나 기신 오행이 강해지는 달
            4. **관리 조언** — 생활 습관 관점의 실용 조언과 한 줄 요약

            반드시 지킬 것: 이 해석은 전통 명리 관점의 참고 정보이며 의학적 진단이나
            치료 조언이 아님을 마지막에 한 문장으로 명시한다. 특정 질병을 단정하지
            않는다.

            분량: 한국어 800~1200자. 반드시 완결된 문장으로 마무리할 것.
        """.trimIndent()

        ReadingTopic.LOVE -> """
            위 데이터를 근거로 ${year}년 애정운(연애·관계) 해석을 작성하세요.
            이성성(남명은 재성=정재·편재, 여명은 관성=정관·편관 — 위 성별 참고)의
            흐름과 도화살·원진살, 배우자궁(일지)의 합충을 중심으로 읽어냅니다.
            결혼 시기 판단이 아니라 이 해의 연애·관계 기류 해석이 목적입니다.

            출력 구조:
            1. **타고난 연애 스타일** — 원국의 이성성 배치와 도화·원진 등 신살이
               말하는 관계 기질
            2. **${year}년 애정 흐름** — 세운·대운에서 이성성이 어떻게 움직이는지,
               일지 합(만남·진전)과 충·형(갈등·변동)이 만드는 관계 방향
            3. **월별 포인트** — 이성성이 들어오거나 일지에 합충이 걸리는 달만
               골라 서술
            4. **조언과 요약** — 솔로/커플 각각의 관점 한 줄씩과 이 해의 관계 원칙
               한 문장

            분량: 한국어 800~1200자. 반드시 완결된 문장으로 마무리할 것.
        """.trimIndent()
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
