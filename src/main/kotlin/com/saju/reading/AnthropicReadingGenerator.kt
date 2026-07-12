package com.saju.reading

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.CacheControlEphemeral
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.TextBlockParam
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.stream.Collectors

@Component
class AnthropicReadingGenerator(
    @Value("\${anthropic.api-key}") private val apiKey: String,
    @Value("\${anthropic.model}") override val model: String,
    @Value("\${anthropic.max-tokens}") private val maxTokens: Long,
) : ReadingGenerator {

    // API 키가 있을 때만 지연 생성 — 미구성 환경(테스트·키 없는 개발)에서도 부팅 가능
    private val client: AnthropicClient? by lazy {
        if (apiKey.isBlank()) null
        else AnthropicOkHttpClient.builder().apiKey(apiKey).build()
    }

    override fun generate(prompt: String): String {
        val anthropic = client ?: throw ReadingUnavailableException(
            "LLM 해석이 구성되지 않았습니다: ANTHROPIC_API_KEY 환경변수를 설정하세요"
        )

        val params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(maxTokens)
            // 시스템 프롬프트는 전 요청 공통 — 프롬프트 캐싱 브레이크포인트.
            // (Haiku 4.5의 최소 캐시 프리픽스는 4096 토큰이라, 프롬프트가 그보다
            //  짧으면 조용히 캐시되지 않지만 요청 자체는 정상 동작한다.)
            .systemOfTextBlockParams(
                listOf(
                    TextBlockParam.builder()
                        .text(SYSTEM_PROMPT)
                        .cacheControl(CacheControlEphemeral.builder().build())
                        .build()
                )
            )
            .addUserMessage(prompt)
            .build()

        val response = anthropic.messages().create(params)

        // 토큰 한도로 잘린 미완성 해석문이 영구 캐시에 저장되는 것을 방지
        if (response.stopReason().map { it == com.anthropic.models.messages.StopReason.MAX_TOKENS }.orElse(false)) {
            throw ReadingGenerationException(
                "해석문 생성이 토큰 한도에서 잘렸습니다 (max-tokens=$maxTokens). 잠시 후 다시 시도해주세요."
            )
        }

        return response.content().stream()
            .flatMap { block -> block.text().stream() }
            .map { it.text() }
            .collect(Collectors.joining())
    }

    companion object {
        // 3종 해석(원국·대운·연도)이 공유하는 공통 규칙만 담는다 — 출력 구조 지시는
        // 요청별 프롬프트 끝에 붙는다. 시스템 프롬프트가 전 요청 동일해야
        // Anthropic 프롬프트 캐시가 해석 종류를 넘나들며 재사용된다.
        val SYSTEM_PROMPT = """
            당신은 명리학(사주팔자) 해석 전문가입니다. 사주 계산 엔진이 산출한 데이터를
            근거로 자연스러운 한국어 해석문을 작성합니다.

            ## 절대 규칙
            - 입력으로 주어진 엔진 계산 데이터(팔자, 십성, 격국, 용신, 오행 점수, 신살,
              12운성, 합충형파해, 대운/세운/월운, 삼재, 길흉)만 근거로 사용합니다.
            - 데이터에 없는 간지, 신살, 관계를 지어내지 않습니다.
            - 계산 값과 모순되는 서술을 하지 않습니다.
            - 출력 구조와 분량은 요청문 마지막의 지시를 따릅니다.

            ## 문체
            - 마크다운 사용, 섹션 제목은 굵은 글씨.
            - 단정적 예언이 아니라 "~하는 흐름", "~에 유리한 구조" 같은 경향 서술.
            - 전문용어는 사용하되 짧은 풀이를 곁들입니다. 예: "편관(나를 압박하는 책임의 별)".
            - 12운성의 사(死)·묘(墓)·절(絶) 등은 죽음의 직역이 아니라 에너지가
              고요해지는 단계의 은유입니다. "죽음에 가깝다" 같은 표현으로 겁주지 말고
              "숙성", "내면으로 향하는 힘" 같은 순화된 언어로 풀어냅니다.
            - 과도한 공포 조장이나 미신적 단정을 피하고, 실용적 조언으로 마무리합니다.
            - 지시된 분량을 넘기지 말고, 반드시 완결된 문장으로 마무리합니다.
        """.trimIndent()
    }
}
