package com.saju.reading

import com.saju.analysis.ElementStrengthAnalyzer
import com.saju.analysis.FortuneService
import com.saju.analysis.GyeokGukAnalyzer
import com.saju.analysis.RelationAnalyzer
import com.saju.analysis.SinSalAnalyzer
import com.saju.analysis.SipSeongAnalyzer
import com.saju.analysis.UnSeongAnalyzer
import com.saju.engine.BirthInput
import com.saju.engine.SajuCalculator
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

    data class ReadingResult(
        val reading: String,
        val model: String,
        val cached: Boolean,
        val cacheKey: String,
    )

    // DB에 있으면 그대로 반환, 없으면 LLM 1회 생성 후 영구 저장
    fun getReading(input: BirthInput, year: Int): ReadingResult {
        val prompt = buildPrompt(input, year)
        val cacheKey = sha256("${generator.model}\n$prompt")

        repository.findByCacheKey(cacheKey)?.let {
            return ReadingResult(it.content, it.model, cached = true, cacheKey = cacheKey)
        }

        val content = generator.generate(prompt)
        save(cacheKey, content)
        return ReadingResult(content, generator.model, cached = false, cacheKey = cacheKey)
    }

    private fun buildPrompt(input: BirthInput, year: Int): String {
        val saju = sajuCalculator.calculate(input)
        return ReadingPromptBuilder.build(
            saju = saju,
            sipSeong = sipSeongAnalyzer.analyze(saju),
            gyeokGuk = gyeokGukAnalyzer.analyze(saju),
            strength = elementStrengthAnalyzer.analyze(saju),
            sinSal = sinSalAnalyzer.analyze(saju),
            unSeong = unSeongAnalyzer.analyze(saju),
            wongukRelations = relationAnalyzer.analyze(saju),
            fortune = fortuneService.fortuneOfYear(saju, year),
        )
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
