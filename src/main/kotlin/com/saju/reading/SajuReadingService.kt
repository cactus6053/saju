package com.saju.reading

import com.fasterxml.jackson.databind.ObjectMapper
import com.saju.analysis.ElementStrengthAnalyzer
import com.saju.analysis.FortuneCategory
import com.saju.analysis.FortuneService
import com.saju.analysis.GyeokGukAnalyzer
import com.saju.analysis.IlunCalculator
import com.saju.analysis.RelationAnalyzer
import com.saju.analysis.SinSalAnalyzer
import com.saju.analysis.SipSeongAnalyzer
import com.saju.analysis.UnSeongAnalyzer
import com.saju.analysis.YearlySummaryCalculator
import com.saju.domain.core.GanJi
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
    private val ilunCalculator: IlunCalculator,
    private val yearlySummaryCalculator: YearlySummaryCalculator,
    private val objectMapper: ObjectMapper,
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
        return cachedGenerate(ReadingPromptBuilder.buildWonguk(wongukData(saju)), ReadingKind.WONGUK)
    }

    // 대운 풀이 (10년 단위 인생 흐름)
    fun getDaeunReading(input: BirthInput): ReadingResult {
        val saju = sajuCalculator.calculate(input)
        val prompt = ReadingPromptBuilder.buildDaeun(
            data = wongukData(saju),
            direction = daeunStartCalculator.directionOf(saju),
            timeline = fortuneService.daeunTimeline(saju),
        )
        return cachedGenerate(prompt, ReadingKind.DAEUN)
    }

    // 연도별 운세 해석 (종합 또는 주제별: 금전·직장·건강)
    fun getReading(input: BirthInput, year: Int, topic: ReadingTopic = ReadingTopic.GENERAL): ReadingResult {
        val saju = sajuCalculator.calculate(input)
        val prompt = ReadingPromptBuilder.buildYearly(
            data = wongukData(saju),
            fortune = fortuneService.fortuneOfYear(saju, year),
            topic = topic,
        )
        return cachedGenerate(prompt, ReadingKind.YEARLY)
    }

    // 결혼운 — 서버 현재 연도부터 10년 세운 스캔 (해가 바뀌면 캐시 자연 갱신)
    fun getMarriageReading(input: BirthInput): ReadingResult {
        val saju = sajuCalculator.calculate(input)
        val prompt = ReadingPromptBuilder.buildMarriage(
            data = wongukData(saju),
            scanStartYear = java.time.Year.now().value,
        )
        return cachedGenerate(prompt, ReadingKind.MARRIAGE)
    }

    data class DailyReadingResult(
        val ilun: IlunCalculator.IlunResult,
        val oneLiner: String,
        val message: String,
        val cached: Boolean,
        val cacheKey: String,
    )

    // 일일 운세 — 점수·행운 요소는 엔진, 한 줄/메시지만 LLM. 미래는 내일까지만 허용
    fun getDailyReading(input: BirthInput, date: java.time.LocalDate?): DailyReadingResult {
        val target = date ?: java.time.LocalDate.now()
        require(!target.isAfter(java.time.LocalDate.now().plusDays(1))) {
            "일일 운세는 내일까지만 조회할 수 있습니다: $target"
        }

        val saju = sajuCalculator.calculate(input)
        val data = wongukData(saju)
        val ilun = ilunCalculator.analyze(saju, data.gyeokGuk, target)
        val result = cachedGenerate(ReadingPromptBuilder.buildDaily(data, ilun), ReadingKind.DAILY)

        // 첫 줄 = 포토카드 한 줄, 나머지 = 메시지 (프롬프트가 강제하는 형식)
        val lines = result.reading.trim().lines()
        val oneLiner = lines.first().trim()
        val message = lines.drop(1).joinToString("\n").trim().ifEmpty { oneLiner }

        return DailyReadingResult(ilun, oneLiner, message, result.cached, result.cacheKey)
    }

    data class YearlySummaryResult(
        val year: Int,
        val categories: List<CategorySummary>,
        val months: List<MonthSummary>,
        val cached: Boolean,
        val cacheKey: String,
    ) {
        data class CategorySummary(val category: FortuneCategory, val score: Int, val summary: String)
        data class MonthSummary(val month: Int, val ganJi: GanJi, val score: Int, val summary: String)
    }

    // 연간 운세 요약 — 점수는 엔진, 요약문(5카테고리+12개월)은 LLM 1회 호출로 일괄 생성
    fun getYearlySummary(input: BirthInput, year: Int): YearlySummaryResult {
        val saju = sajuCalculator.calculate(input)
        val data = wongukData(saju)
        val fortune = fortuneService.fortuneOfYear(saju, year)
        val categoryScores = yearlySummaryCalculator.categoryScores(saju, data.gyeokGuk, fortune.seun)
        val monthScores = yearlySummaryCalculator.monthScores(data.gyeokGuk, fortune.wolunList)

        val prompt = ReadingPromptBuilder.buildYearlySummary(data, fortune, categoryScores, monthScores)
        val result = cachedGenerate(prompt, ReadingKind.YEARLY, validate = ::parseSummaryJson)
        val (categoryTexts, monthTexts) = parseSummaryJson(result.reading)

        return YearlySummaryResult(
            year = year,
            categories = categoryScores.map {
                YearlySummaryResult.CategorySummary(it.category, it.score, categoryTexts.getValue(it.category))
            },
            months = monthScores.map {
                YearlySummaryResult.MonthSummary(it.month, it.ganJi, it.score, monthTexts.getValue(it.month))
            },
            cached = result.cached,
            cacheKey = result.cacheKey,
        )
    }

    // LLM JSON 파싱 + 완전성 검증 — 실패 시 예외 (검증 통과 전에는 캐시에 저장되지 않는다)
    private fun parseSummaryJson(content: String): Pair<Map<FortuneCategory, String>, Map<Int, String>> {
        val json = content.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

        val root = runCatching { objectMapper.readTree(json) }
            .getOrElse { throw ReadingGenerationException("연간 요약 JSON 파싱 실패: ${it.message}") }

        val categories = FortuneCategory.entries.associateWith { category ->
            root.path("categories").path(category.name).asText("").trim().ifEmpty {
                throw ReadingGenerationException("연간 요약에 ${category.name} 항목이 없습니다")
            }
        }
        val months = (1..12).associateWith { month ->
            root.path("months").path(month.toString()).asText("").trim().ifEmpty {
                throw ReadingGenerationException("연간 요약에 ${month}월 항목이 없습니다")
            }
        }
        return categories to months
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

    // DB에 있으면 그대로 반환, 없으면 LLM 1회 생성 후 영구 저장.
    // validate가 있으면 검증 통과분만 저장하며, 실패 시 1회 재생성 후 재검증한다.
    private fun cachedGenerate(
        prompt: String,
        kind: ReadingKind,
        validate: ((String) -> Unit)? = null,
    ): ReadingResult {
        val cacheKey = sha256("${generator.model}\n$prompt")

        repository.findByCacheKey(cacheKey)?.let {
            return ReadingResult(it.content, it.model, cached = true, cacheKey = cacheKey)
        }

        var content = generator.generate(prompt)
        if (validate != null) {
            try {
                validate(content)
            } catch (e: ReadingGenerationException) {
                content = generator.generate(prompt)
                validate(content) // 재시도도 실패하면 저장 없이 전파
            }
        }
        save(cacheKey, content, kind)
        return ReadingResult(content, generator.model, cached = false, cacheKey = cacheKey)
    }

    private fun save(cacheKey: String, content: String, kind: ReadingKind) {
        try {
            repository.save(
                SajuReadingEntity(cacheKey = cacheKey, model = generator.model, content = content, kind = kind)
            )
        } catch (e: DataIntegrityViolationException) {
            // 동시 요청이 먼저 저장한 경우 — 결과는 동일하므로 무시
        }
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
