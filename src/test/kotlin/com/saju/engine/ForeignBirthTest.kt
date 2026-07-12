package com.saju.engine

import com.saju.domain.core.Gender
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ForeignBirthTest {

    private val calculator = SajuCalculator()
    private val normalizer = BirthInputNormalizer()

    // ── 뉴욕 출생: 1998-02-05 06:00 EST ────────────────────────────────

    @Test
    fun `뉴욕 출생 - 연월주는 절대 시점, 일시주는 현지 시각 기준`() {
        // 현지 06:00 EST = UTC 11:00 = KST 20:00
        // 연·월주: KST 2/5 20:00은 입춘(2/4) 이후 → 戊寅년 甲寅월
        // 일주: 현지 날짜 2/5 → 癸未 / 시주: 현지 06:00 = 卯時, 癸일 → 乙卯
        val saju = calculator.calculate(
            BirthInput(1998, 2, 5, 6, 0, gender = Gender.MALE, timeZone = "America/New_York")
        )
        assertEquals("戊寅 甲寅 癸未 乙卯", saju.paljaHanja)
    }

    @Test
    fun `같은 벽시계 시각을 한국으로 입력하면 시주가 달라짐`() {
        // 뉴욕 현지 시각을 그대로 KST 20:00으로 입력한 경우 (잘못된 사용 예)
        val naive = calculator.calculate(
            BirthInput(1998, 2, 5, 20, 0, gender = Gender.MALE)
        )
        // 연·월·일주는 우연히 같지만 시주는 戌時로 완전히 다름
        assertEquals("壬戌", naive.hourPillar.hanja)

        val correct = calculator.calculate(
            BirthInput(1998, 2, 5, 6, 0, gender = Gender.MALE, timeZone = "America/New_York")
        )
        assertEquals("乙卯", correct.hourPillar.hanja)
    }

    // ── 절기 경계 교차: 연주는 바뀌고 일주는 현지 날짜 유지 ────────────

    @Test
    fun `절기 경계 교차 - 현지는 입춘 전날 밤이지만 절대 시점은 입춘 이후`() {
        // 뉴욕 1998-02-03 21:00 EST = KST 2/4 11:00 (입춘 09시경 이후)
        val saju = calculator.calculate(
            BirthInput(1998, 2, 3, 21, 0, gender = Gender.MALE, timeZone = "America/New_York")
        )

        // 연·월주: 절대 시점이 입춘 이후 → 새해 戊寅년 甲寅월
        assertEquals(1998, saju.sajuYear)
        assertEquals("戊寅", saju.yearPillar.hanja)
        assertEquals("甲寅", saju.monthPillar.hanja)

        // 일주: 현지 날짜 2/3 유지 → 辛巳
        assertEquals("辛巳", saju.dayPillar.hanja)
    }

    // ── DST(서머타임) 자동 처리 ─────────────────────────────────────────

    @Test
    fun `뉴욕 여름 출생 - EDT 서머타임이 표준 프레임으로 정규화`() {
        // 1998-07-15 12:00 EDT(UTC-4) → 표준 프레임 EST(UTC-5) 11:00
        val birth = normalizer.normalize(
            BirthInput(1998, 7, 15, 12, 0, gender = Gender.MALE, timeZone = "America/New_York")
        )
        assertEquals(LocalDateTime.of(1998, 7, 15, 11, 0), birth.corrected)
        // 절대 시점: UTC 16:00 → KST 다음날 01:00
        assertEquals(LocalDateTime.of(1998, 7, 16, 1, 0), birth.instantKst)
    }

    // ── 한국 출생 호환성 ────────────────────────────────────────────────

    @Test
    fun `한국 출생 기본값 - corrected와 instantKst 동일`() {
        val birth = normalizer.normalize(
            BirthInput(1995, 6, 15, 10, 30, gender = Gender.MALE)
        )
        assertEquals(birth.corrected, birth.instantKst)
    }

    @Test
    fun `기존 한국 사주 결과 무변경`() {
        val saju = calculator.calculate(
            BirthInput(2024, 6, 15, 12, 0, gender = Gender.MALE)
        )
        assertEquals("甲辰 庚午 庚戌 壬午", saju.paljaHanja)
    }

    // ── 다양한 시간대 ───────────────────────────────────────────────────

    @Test
    fun `런던·도쿄 출생 정규화`() {
        // 런던 1990-03-15 08:00 GMT → KST 17:00
        val london = normalizer.normalize(
            BirthInput(1990, 3, 15, 8, 0, gender = Gender.FEMALE, timeZone = "Europe/London")
        )
        assertEquals(LocalDateTime.of(1990, 3, 15, 17, 0), london.instantKst)
        assertEquals(LocalDateTime.of(1990, 3, 15, 8, 0), london.corrected)

        // 도쿄는 KST와 같은 UTC+9
        val tokyo = normalizer.normalize(
            BirthInput(1990, 3, 15, 8, 0, gender = Gender.FEMALE, timeZone = "Asia/Tokyo")
        )
        assertEquals(tokyo.corrected, tokyo.instantKst)
    }

    // ── 오류 처리 ───────────────────────────────────────────────────────

    @Test
    fun `유효하지 않은 시간대는 예외`() {
        val ex = assertThrows<IllegalArgumentException> {
            normalizer.normalize(
                BirthInput(1998, 2, 5, 6, 0, gender = Gender.MALE, timeZone = "Mars/Olympus")
            )
        }
        assertTrue(ex.message!!.contains("Mars/Olympus"))
        assertTrue(ex.message!!.contains("IANA"))
    }

    @Test
    fun `해외 출생 대운도 절대 시점 기준으로 계산`() {
        val saju = calculator.calculate(
            BirthInput(1998, 2, 3, 21, 0, gender = Gender.MALE, timeZone = "America/New_York")
        )
        val daeun = DaeunStartCalculator().calculate(saju)
        // 절대 시점(KST 2/4 11:00)이 입춘 직후 → 역행이면 일수가 매우 작아야 함
        // 戊寅년 양간 + 남자 = 순행 → 다음 절기(경칩)까지 약 29일
        assertEquals(DaeunStartCalculator.Direction.FORWARD, daeun.direction)
        assertTrue(daeun.daysToJeol in 27.0..30.0, "일수: ${daeun.daysToJeol}")
    }
}
