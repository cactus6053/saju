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
        val kst = birth.corrected

        val year = yearPillarCalculator.calculate(kst)
        val month = monthPillarCalculator.calculate(kst)
        val day = dayPillarCalculator.calculate(kst, birth.zasiMode)
        val hour = hourPillarCalculator.calculate(kst)

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
