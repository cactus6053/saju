package com.saju.api

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI().info(
        Info()
            .title("사주 계산 엔진 API")
            .description(
                """
                생년월일시로 사주 팔자(연·월·일·시주)를 계산하고 파생 분석을 제공합니다.

                - 원국 계산: 절기 기준 4주 + 십성·격국용신·오행강약·신살·합충형파해
                - 운세 조회: 대운·세운·월운 및 원국과의 관계 분석
                - 지원 범위: 1900 ~ 2100년
                - 절기 정밀도: 평균 ±5분 (ΔT 보정 적용)
                """.trimIndent()
            )
            .version("v1")
    )
}
