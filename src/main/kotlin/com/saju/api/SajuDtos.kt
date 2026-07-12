package com.saju.api

import com.saju.analysis.ElementStrengthAnalyzer
import com.saju.analysis.FortuneService
import com.saju.analysis.GyeokGukResult
import com.saju.analysis.PillarRelation
import com.saju.analysis.RelationAnalyzer
import com.saju.analysis.SeunCalculator
import com.saju.analysis.SinSalHit
import com.saju.analysis.SipSeongAnalyzer
import com.saju.analysis.UnRelation
import com.saju.analysis.WolunCalculator
import com.saju.domain.core.GanJi
import com.saju.domain.core.Gender
import com.saju.engine.BirthInput
import com.saju.engine.CalendarType
import com.saju.engine.DaeunCalculator
import com.saju.engine.TimeCorrectionMode
import com.saju.engine.TimeCorrector
import com.saju.engine.ZasiMode
import com.saju.reading.SajuReadingService

// ── 요청 ────────────────────────────────────────────────────────────────

@io.swagger.v3.oas.annotations.media.Schema(description = "출생 정보")
data class BirthRequest(
    @field:io.swagger.v3.oas.annotations.media.Schema(description = "출생 연도 (1900~2100)", example = "2024")
    val year: Int,
    @field:io.swagger.v3.oas.annotations.media.Schema(description = "출생 월 (1~12)", example = "6")
    val month: Int,
    @field:io.swagger.v3.oas.annotations.media.Schema(description = "출생 일", example = "15")
    val day: Int,
    @field:io.swagger.v3.oas.annotations.media.Schema(description = "출생 시 (0~23)", example = "12")
    val hour: Int,
    @field:io.swagger.v3.oas.annotations.media.Schema(description = "출생 분 (0~59)", example = "0", defaultValue = "0")
    val minute: Int = 0,
    @field:io.swagger.v3.oas.annotations.media.Schema(description = "달력 종류 (SOLAR: 양력, LUNAR: 음력)", defaultValue = "SOLAR")
    val calendarType: CalendarType = CalendarType.SOLAR,
    @field:io.swagger.v3.oas.annotations.media.Schema(description = "음력 윤달 여부 (음력 입력 시에만 사용)", defaultValue = "false")
    val isLeapMonth: Boolean = false,
    @field:io.swagger.v3.oas.annotations.media.Schema(description = "성별 (대운 순행/역행 판정에 사용)", example = "MALE")
    val gender: Gender,
    @field:io.swagger.v3.oas.annotations.media.Schema(
        description = "출생지 IANA 시간대 — 해외 출생 시 현지 시간대 지정 (예: America/New_York). " +
            "연·월주는 절대 시점, 일·시주는 현지 시각 기준으로 계산됨",
        defaultValue = "Asia/Seoul",
        example = "Asia/Seoul",
    )
    val timeZone: String = "Asia/Seoul",
    @field:io.swagger.v3.oas.annotations.media.Schema(description = "출생지 경도 (지방평균시·진태양시 보정용)", defaultValue = "126.978")
    val longitude: Double = TimeCorrector.SEOUL_LONGITUDE,
    @field:io.swagger.v3.oas.annotations.media.Schema(
        description = "시간 보정 방식 (STANDARD: 표준시, LOCAL_MEAN_TIME: 지방평균시, APPARENT_SOLAR_TIME: 진태양시)",
        defaultValue = "STANDARD",
    )
    val timeCorrectionMode: TimeCorrectionMode = TimeCorrectionMode.STANDARD,
    @field:io.swagger.v3.oas.annotations.media.Schema(
        description = "자시 처리 방식 (YAJASI_JEONGJASI: 야자시/정자시 구분, SIMPLE: 23시부터 다음날)",
        defaultValue = "YAJASI_JEONGJASI",
    )
    val zasiMode: ZasiMode = ZasiMode.YAJASI_JEONGJASI,
) {
    fun toBirthInput() = BirthInput(
        year, month, day, hour, minute,
        calendarType, isLeapMonth, gender, timeZone, longitude, timeCorrectionMode, zasiMode,
    )
}

// ── 공통 ────────────────────────────────────────────────────────────────

data class GanJiDto(val hanja: String, val hangul: String)

fun GanJi.toDto() = GanJiDto(hanja, hangul)

data class RelationDto(
    val type: String,
    val positions: List<String>,
    val resultElement: String?,
)

fun PillarRelation.toDto() = RelationDto(
    type.hangul, positions.map { it.hangul }, resultElement?.hangul,
)

fun UnRelation.toDto() = RelationDto(
    type.hangul, positions.map { it.hangul }, resultElement?.hangul,
)

fun RelationAnalyzer.GanJiRelation.toDto() = RelationDto(
    type.hangul, emptyList(), resultElement?.hangul,
)

// ── 원국 + 분석 응답 ────────────────────────────────────────────────────

data class SajuResponse(
    val sajuYear: Int,
    val yearPillar: GanJiDto,
    val monthPillar: GanJiDto,
    val dayPillar: GanJiDto,
    val hourPillar: GanJiDto,
    val dayMaster: String,
    val paljaHanja: String,
    val paljaHangul: String,
    val sipSeong: SipSeongDto,
    val gyeokGuk: GyeokGukDto,
    val elementStrength: ElementStrengthDto,
    val sinSal: List<SinSalDto>,
    val relations: List<RelationDto>,
)

data class PillarSipSeongDto(
    val gan: String?,
    val jiPrincipal: String,
    val jiJanggan: List<String>,
)

data class SipSeongDto(
    val year: PillarSipSeongDto,
    val month: PillarSipSeongDto,
    val day: PillarSipSeongDto,
    val hour: PillarSipSeongDto,
)

fun SipSeongAnalyzer.SajuSipSeong.toDto(): SipSeongDto {
    fun SipSeongAnalyzer.PillarSipSeong.toDto() = PillarSipSeongDto(
        gan?.hangul, jiPrincipal.hangul, jiJanggan.map { it.hangul },
    )
    return SipSeongDto(year.toDto(), month.toDto(), day.toDto(), hour.toDto())
}

data class GyeokGukDto(
    val name: String,
    val category: String,
    val yongsin: String,
    val johuYongsin: String?,
    val isSinGang: Boolean,
)

fun GyeokGukResult.toDto() = GyeokGukDto(
    gyeokGuk.hangul, gyeokGuk.category.name, yongsin.hangul, johuYongsin?.hangul, isSinGang,
)

data class ElementStrengthDto(
    val scores: Map<String, Double>,
    val supportScore: Double,
    val opposeScore: Double,
    val isSinGang: Boolean,
    val excessive: List<String>,
    val deficient: List<String>,
)

fun ElementStrengthAnalyzer.ElementStrength.toDto() = ElementStrengthDto(
    scores = scores.entries.associate { it.key.hangul to it.value },
    supportScore = supportScore,
    opposeScore = opposeScore,
    isSinGang = isSinGang,
    excessive = excessive.map { it.hangul },
    deficient = deficient.map { it.hangul },
)

data class SinSalDto(
    val name: String,
    val position: String,
    val isGilsin: Boolean,
)

fun SinSalHit.toDto() = SinSalDto(sinSal.hangul, position.hangul, sinSal.isGilsin)

// ── 운세 응답 ───────────────────────────────────────────────────────────

data class FortuneResponse(
    val year: Int,
    val age: Int,
    val currentDaeun: DaeunDto?,
    val daeunRelations: List<RelationDto>,
    val seun: SeunDto,
    val wolunList: List<WolunDto>,
)

data class DaeunDto(
    val order: Int,
    val ganJi: GanJiDto,
    val startAge: Int,
    val endAge: Int,
)

fun DaeunCalculator.Daeun.toDto() = DaeunDto(order, ganJi.toDto(), startAge, endAge)

data class SeunDto(
    val ganJi: GanJiDto,
    val ganSipSeong: String,
    val jiSipSeong: String,
    val relationsWithWonguk: List<RelationDto>,
    val relationsWithDaeun: List<RelationDto>,
)

fun SeunCalculator.SeunResult.toDto() = SeunDto(
    ganJi.toDto(), ganSipSeong.hangul, jiSipSeong.hangul,
    relationsWithWonguk.map { it.toDto() },
    relationsWithDaeun.map { it.toDto() },
)

data class WolunDto(
    val month: Int,
    val ganJi: GanJiDto,
    val ganSipSeong: String,
    val jiSipSeong: String,
    val gilHyung: String,
    val isSamjae: Boolean,
    val relationsWithWonguk: List<RelationDto>,
    val relationsWithSeun: List<RelationDto>,
    val relationsWithDaeun: List<RelationDto>,
)

fun WolunCalculator.WolunResult.toDto() = WolunDto(
    month, ganJi.toDto(), ganSipSeong.hangul, jiSipSeong.hangul,
    gilHyung.hangul, isSamjae,
    relationsWithWonguk.map { it.toDto() },
    relationsWithSeun.map { it.toDto() },
    relationsWithDaeun.map { it.toDto() },
)

fun FortuneService.YearlyFortune.toDto() = FortuneResponse(
    year = year,
    age = age,
    currentDaeun = currentDaeun?.toDto(),
    daeunRelations = daeunRelations.map { it.toDto() },
    seun = seun.toDto(),
    wolunList = wolunList.map { it.toDto() },
)

data class DaeunTimelineResponse(
    val daeunList: List<DaeunTimelineEntryDto>,
)

data class DaeunTimelineEntryDto(
    val daeun: DaeunDto,
    val relationsWithWonguk: List<RelationDto>,
)

fun List<FortuneService.DaeunFortune>.toTimelineDto() = DaeunTimelineResponse(
    map { DaeunTimelineEntryDto(it.daeun.toDto(), it.relationsWithWonguk.map { r -> r.toDto() }) }
)

data class LuckyDto(
    val color: String,
    val colorHangul: String,
    val number: Int,
    val item: String,
    val itemHangul: String,
)

data class DailyFortuneResponse(
    val date: String,
    val ilJin: GanJiDto,
    val ganSipSeong: String,
    val jiSipSeong: String,
    val unSeong: String,
    val score: Int,
    val oneLiner: String,
    val message: String,
    val lucky: LuckyDto,
    val cached: Boolean,
)

fun SajuReadingService.DailyReadingResult.toDto() = DailyFortuneResponse(
    date = ilun.date.toString(),
    ilJin = ilun.ganJi.toDto(),
    ganSipSeong = ilun.ganSipSeong.hangul,
    jiSipSeong = ilun.jiSipSeong.hangul,
    unSeong = ilun.unSeong.hangul,
    score = ilun.score,
    oneLiner = oneLiner,
    message = message,
    lucky = LuckyDto(
        color = ilun.luckyColor.name,
        colorHangul = ilun.luckyColor.hangul,
        number = ilun.luckyNumber,
        item = ilun.luckyItem.name,
        itemHangul = ilun.luckyItem.hangul,
    ),
    cached = cached,
)
