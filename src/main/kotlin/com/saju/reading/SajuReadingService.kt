package com.saju.reading

import com.saju.analysis.ElementStrengthAnalyzer
import com.saju.analysis.FortuneService
import com.saju.analysis.GyeokGukAnalyzer
import com.saju.analysis.RelationAnalyzer
import com.saju.analysis.SinSalAnalyzer
import com.saju.analysis.SipSeongAnalyzer
import com.saju.analysis.UnSeongAnalyzer
import com.saju.engine.BirthInput
import com.saju.engine.DaeunStartCalculator
import com.saju.engine.SajuCalculator
import com.saju.engine.SajuResult
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
class SajuReadingService(
    private val repository: SajuReadingRepository,
    private val generator: ReadingGenerator,
    private val sajuCalculator: SajuCalculator,
    private val sipSeongAnalyzer: SipSeongAnalyzer,
    private val gyeokGukAnalyzer: GyeokGukAnalyzer,
    private val elementStrengthAnalyzer: ElementStrengthAnalyzer,
    private val sinSalAnalyzer: SinSalAnalyzer,
    private val relationAnalyzer: RelationAnalyzer,
    private val fortuneService: FortuneService,
) {

    private val unSeongAnalyzer = UnSeongAnalyzer()
    private val daeunStartCalculator = DaeunStartCalculator()

    data class ReadingResult(
        val reading: String,
        val model: String,
        val cached: Boolean,
        val cacheKey: String,
    )

    // 원국 풀이 (평생사주) — 연도 무관, 캐시 적중률 최고
    fun getWongukReading(input: BirthInput): ReadingResult {
        val saju = sajuCalculator.calculate(input)
        return cachedGenerate(ReadingPromptBuilder.buildWonguk(wongukData(saju)))
    }

    // 대운 풀이 (10년 단위 인생 흐름)
    fun getDaeunReading(input: BirthInput): ReadingResult {
        val saju = sajuCalculator.calculate(input)
        val prompt = ReadingPromptBuilder.buildDaeun(
            data = wongukData(saju),
            direction = daeunStartCalculator.directionOf(saju),
            timeline = fortuneService.daeunTimeline(saju),
        )
        return cachedGenerate(prompt)
    }

    // 연도별 운세 해석
    fun getReading(input: BirthInput, year: Int): ReadingResult {
        val saju = sajuCalculator.calculate(input)
        val prompt = ReadingPromptBuilder.buildYearly(
            data = wongukData(saju),
            fortune = fortuneService.fortuneOfYear(saju, year),
        )
        return cachedGenerate(prompt)
    }

    private fun wongukData(saju: SajuResult) = ReadingPromptBuilder.WongukData(
        saju = saju,
        sipSeong = sipSeongAnalyzer.analyze(saju),
        gyeokGuk = gyeokGukAnalyzer.analyze(saju),
        strength = elementStrengthAnalyzer.analyze(saju),
        sinSal = sinSalAnalyzer.analyze(saju),
        unSeong = unSeongAnalyzer.analyze(saju),
        wongukRelations = relationAnalyzer.analyze(saju),
    )

    // DB에 있으면 그대로 반환, 없으면 LLM 1회 생성 후 영구 저장
    private fun cachedGenerate(prompt: String): ReadingResult {
        val cacheKey = sha256("${generator.model}\n$prompt")

        repository.findByCacheKey(cacheKey)?.let {
            return ReadingResult(it.content, it.model, cached = true, cacheKey = cacheKey)
        }

        val content = generator.generate(prompt)
        save(cacheKey, content)
        return ReadingResult(content, generator.model, cached = false, cacheKey = cacheKey)
    }

    private fun save(cacheKey: String, content: String) {
        try {
            repository.save(SajuReadingEntity(cacheKey = cacheKey, model = generator.model, content = content))
        } catch (e: DataIntegrityViolationException) {
            // 동시 요청이 먼저 저장한 경우 — 결과는 동일하므로 무시
        }
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
