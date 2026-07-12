package com.saju.engine

class SajuCalculator(
    private val normalizer: BirthInputNormalizer = BirthInputNormalizer(),
    jeolgiCalculator: JeolgiCalculator = JeolgiCalculator(),
    dayGanjiCalculator: DayGanjiCalculator = DayGanjiCalculator(),
) {

    private val yearPillarCalculator = YearPillarCalculator(jeolgiCalculator)
    private val monthPillarCalculator = MonthPillarCalculator(
        MonthBoundaryResolver(jeolgiCalculator),
        yearPillarCalculator,
    )
    private val dayPillarCalculator = DayPillarCalculator(dayGanjiCalculator)
    private val hourPillarCalculator = HourPillarCalculator(dayGanjiCalculator)

    fun calculate(input: BirthInput): SajuResult {
        val birth = normalizer.normalize(input)

        // 연·월주는 절기(전 지구적 단일 순간) 비교 → 절대 시점(KST 프레임)
        // 일·시주는 출생지 태양 위치 기준 → 현지 프레임 보정 시각
        val year = yearPillarCalculator.calculate(birth.instantKst)
        val month = monthPillarCalculator.calculate(birth.instantKst)
        val day = dayPillarCalculator.calculate(birth.corrected, birth.zasiMode)
        val hour = hourPillarCalculator.calculate(birth.corrected)

        return SajuResult(
            yearPillar = year.ganJi,
            monthPillar = month.ganJi,
            dayPillar = day.ganJi,
            hourPillar = hour,
            sajuYear = year.sajuYear,
            birth = birth,
        )
    }
}
