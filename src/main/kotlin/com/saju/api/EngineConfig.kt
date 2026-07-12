package com.saju.api

import com.saju.analysis.ElementStrengthAnalyzer
import com.saju.analysis.FortuneService
import com.saju.analysis.GyeokGukAnalyzer
import com.saju.analysis.RelationAnalyzer
import com.saju.analysis.SinSalAnalyzer
import com.saju.analysis.SipSeongAnalyzer
import com.saju.engine.SajuCalculator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EngineConfig {

    @Bean
    fun sajuCalculator() = SajuCalculator()

    @Bean
    fun sipSeongAnalyzer() = SipSeongAnalyzer()

    @Bean
    fun gyeokGukAnalyzer() = GyeokGukAnalyzer()

    @Bean
    fun elementStrengthAnalyzer() = ElementStrengthAnalyzer()

    @Bean
    fun sinSalAnalyzer() = SinSalAnalyzer()

    @Bean
    fun relationAnalyzer() = RelationAnalyzer()

    @Bean
    fun fortuneService() = FortuneService()
}
