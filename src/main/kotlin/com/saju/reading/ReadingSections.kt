package com.saju.reading

// 해석 한 섹션의 스펙 — key는 FE l10n 매핑 키, title은 한글 참고용, guide는 LLM 내용 지시
data class SectionSpec(val key: String, val title: String, val guide: String)

// 해석 종류별 고정 섹션 — 프롬프트 지시·JSON 검증·응답 구성이 모두 이 스펙을 공유한다.
// 키를 바꾸면 FE 렌더링 매핑도 깨지므로 함부로 변경하지 말 것.
object ReadingSections {

    val WONGUK = listOf(
        SectionSpec("dayMaster", "일간과 기질", "일간 오행의 본질과 사주 전체가 만드는 성격 구조 (200~300자)"),
        SectionSpec("gyeokGuk", "격국과 용신", "이 사주가 택한 삶의 전략, 용신이 가리키는 보완 방향 (200~300자)"),
        SectionSpec("balance", "오행 균형", "과다·결핍이 만드는 강점과 약점, 생활 속 보완법 (150~250자)"),
        SectionSpec("sinSal", "신살과 특이점", "검출된 신살·12운성이 더하는 색채 (100~200자)"),
        SectionSpec("advice", "적성과 조언", "구조에 어울리는 방향 제안과 한 줄 요약 (150~250자)"),
    )

    val DAEUN = listOf(
        SectionSpec("direction", "대운의 방향", "순행/역행과 원국 구조가 만드는 큰 그림 (2~3문장)"),
        SectionSpec(
            "flow", "인생 흐름",
            "청년기~중년기(대체로 2~6대운)를 3개 구간으로 묶어 십성 주제와 원국 관계(합충형파해)를 " +
                "근거로 서술. 말년 대운은 한두 문장만, 80세 이후는 다루지 않고, 10개 대운을 " +
                "빠짐없이 나열하지 말 것 (600~800자)",
        ),
        SectionSpec("turningPoint", "전환점", "기운이 가장 크게 바뀌는 대운 하나와 그 이유 (150~250자)"),
        SectionSpec("yongsinDaeun", "용신 대운", "용신 오행이 들어오는 대운과 그 시기의 의미 (150~250자)"),
        SectionSpec("advice", "한 줄 요약", "인생 전체를 관통하는 조언 한 문장"),
    )

    val MARRIAGE = listOf(
        SectionSpec("structure", "배우자 인연의 구조", "원국 배우자성 상태와 배우자궁이 말하는 인연의 모양 (200~300자)"),
        SectionSpec(
            "timing", "결혼 기운이 들어오는 시기",
            "스캔에서 신호(★)가 있는 연도를 근거와 함께 짚고 신호가 겹치는 해를 강조. " +
                "신호 없는 해는 나열하지 않는다 (250~400자)",
        ),
        SectionSpec(
            "caution", "주의할 요소",
            "원진·고신·과숙·일지충 등 장애 신살이 있다면 그 의미와 보완 관점, " +
                "없다면 관계에서 일반적으로 주의할 포인트 (100~200자)",
        ),
        SectionSpec("advice", "조언", "시기에 매이지 않는 관계 조언과 한 줄 요약 (100~200자)"),
    )

    fun yearly(topic: ReadingTopic): List<SectionSpec> = when (topic) {
        ReadingTopic.GENERAL -> listOf(
            SectionSpec("overview", "원국 개관", "일간·격국·용신이 말하는 기질 구조 (간략히, 100~150자)"),
            SectionSpec("daeun", "현재 대운", "대운 간지의 십성 의미와 원국 관계가 만드는 10년 흐름 (150~250자)"),
            SectionSpec(
                "yearFlow", "올해 운세",
                "세운의 십성 주제, 원국·대운과의 합충형파해가 만드는 사건 방향. 삼재 여부 언급 (250~350자)",
            ),
            SectionSpec("monthly", "월별 흐름", "길한 달과 조심할 달을 근거와 함께 묶어 서술 (의미 있는 것만, 200~300자)"),
            SectionSpec("advice", "한 줄 요약", "이 해를 관통하는 조언 한 문장"),
        )

        ReadingTopic.MONEY -> listOf(
            SectionSpec(
                "capacity", "타고난 재물 그릇",
                "원국의 재성 상태와 신강/신약이 말하는 재물 체질 (신약한데 재성이 강하면 \"감당\" 이슈, " +
                    "재성이 용신이면 순풍 구조 등, 200~300자)",
            ),
            SectionSpec("yearFlow", "올해 재물 흐름", "세운·대운에서 재성이 어떻게 움직이는지, 합충이 만드는 수입·지출·투자 방향 (250~350자)"),
            SectionSpec("monthly", "월별 재물 포인트", "재성이 들어오거나 충이 걸리는 달만 골라 서술 (150~250자)"),
            SectionSpec("advice", "조언과 요약", "이 해의 돈 관리 원칙 한 문장"),
        )

        ReadingTopic.CAREER -> listOf(
            SectionSpec("structure", "타고난 직업 구조", "원국의 관성·식상 배치와 격국이 말하는 커리어 성향 (조직형인지 독립형인지, 200~300자)"),
            SectionSpec("yearFlow", "올해 직장 흐름", "세운의 관성/식상 기운, 원국·대운과의 관계가 만드는 승진·이동·이직 신호 (200~300자)"),
            SectionSpec("change", "이직·변동 판단", "충·역마 등 변동 신호가 있는지, 있다면 움직이기 좋은 시기인지 버틸 시기인지 (150~250자)"),
            SectionSpec("monthly", "월별 포인트", "커리어 관련 의미 있는 달만 골라 서술 (150~250자)"),
            SectionSpec("advice", "조언과 요약", "이 해의 커리어 전략 한 문장"),
        )

        ReadingTopic.HEALTH -> listOf(
            SectionSpec("constitution", "타고난 체질 경향", "원국의 과다/결핍 오행이 가리키는 취약 부위와 강한 부위 (오행-장부 대응표 근거, 200~300자)"),
            SectionSpec("yearFlow", "올해 건강 흐름", "세운이 강화하는 오행이 기신인지 희신인지, 충·형이 걸리는 부위 관점 (200~300자)"),
            SectionSpec("caution", "조심할 달", "충·형이 집중되거나 기신 오행이 강해지는 달 (100~200자)"),
            SectionSpec(
                "advice", "관리 조언",
                "생활 습관 관점의 실용 조언과 한 줄 요약. 마지막에 이 해석은 전통 명리 관점의 참고 " +
                    "정보이며 의학적 진단·치료 조언이 아님을 한 문장으로 명시 (150~250자)",
            ),
        )

        ReadingTopic.LOVE -> listOf(
            SectionSpec("style", "타고난 연애 스타일", "원국의 이성성 배치와 도화·원진 등 신살이 말하는 관계 기질 (200~300자)"),
            SectionSpec(
                "yearFlow", "올해 애정 흐름",
                "세운·대운에서 이성성이 어떻게 움직이는지, 일지 합(만남·진전)과 충·형(갈등·변동)이 만드는 관계 방향 (250~350자)",
            ),
            SectionSpec("monthly", "월별 포인트", "이성성이 들어오거나 일지에 합충이 걸리는 달만 골라 서술 (150~250자)"),
            SectionSpec("advice", "조언과 요약", "솔로/커플 각각의 관점 한 줄씩과 이 해의 관계 원칙 한 문장 (100~200자)"),
        )
    }
}
