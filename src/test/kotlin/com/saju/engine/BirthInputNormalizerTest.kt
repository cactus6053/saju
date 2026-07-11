package com.saju.engine

import com.saju.domain.core.Gender
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BirthInputNormalizerTest {

    private val normalizer = BirthInputNormalizer()

    private fun solarInput(
        year: Int = 1995, month: Int = 6, day: Int = 15,
        hour: Int = 10, minute: Int = 30,
    ) = BirthInput(year, month, day, hour, minute, gender = Gender.MALE)

    // ── 정상 케이스 ─────────────────────────────────────────────────────

    @Test
    fun `양력 입력 정규화 - 현대 날짜는 벽시계 그대로`() {
        val result = normalizer.normalize(solarInput())
        assertEquals(LocalDate.of(1995, 6, 15), result.solarDate)
        assertEquals(LocalDateTime.of(1995, 6, 15, 10, 30), result.wallClock)
        assertEquals(result.wallClock, result.corrected)
        assertEquals(Gender.MALE, result.gender)
    }

    @Test
    fun `음력 입력은 양력으로 변환 - 2024년 설날`() {
        val result = normalizer.normalize(
            BirthInput(2024, 1, 1, 12, 0, calendarType = CalendarType.LUNAR, gender = Gender.FEMALE)
        )
        assertEquals(LocalDate.of(2024, 2, 10), result.solarDate)
    }

    @Test
    fun `음력 윤달 입력 - 2020년 윤4월`() {
        val normal = normalizer.normalize(
            BirthInput(2020, 4, 15, 12, 0, calendarType = CalendarType.LUNAR, gender = Gender.MALE)
        )
        val leap = normalizer.normalize(
            BirthInput(2020, 4, 15, 12, 0, calendarType = CalendarType.LUNAR, isLeapMonth = true, gender = Gender.MALE)
        )
        assertTrue(leap.solarDate.isAfter(normal.solarDate), "윤4월은 평4월보다 뒤")
    }

    @Test
    fun `1955년 출생은 표준시 보정 적용 (+30분)`() {
        val result = normalizer.normalize(solarInput(year = 1955, month = 1, day = 15, hour = 12, minute = 0))
        assertEquals(LocalDateTime.of(1955, 1, 15, 12, 30), result.corrected)
    }

    @Test
    fun `LMT 모드 - 자시 경계에서 날짜가 넘어감`() {
        // 서울 LMT는 KST-32분: 00:10 출생 → 보정 후 전날 23:38경
        val result = normalizer.normalize(
            BirthInput(
                1995, 6, 15, 0, 10, gender = Gender.MALE,
                timeCorrectionMode = TimeCorrectionMode.LOCAL_MEAN_TIME,
            )
        )
        assertEquals(LocalDate.of(1995, 6, 14), result.corrected.toLocalDate(), "보정 후 전날로 넘어가야 함")
        assertEquals(23, result.corrected.hour)
    }

    @Test
    fun `옵션 기본값 - 서울 경도, STANDARD, 야자시 모드`() {
        val input = solarInput()
        assertEquals(TimeCorrector.SEOUL_LONGITUDE, input.longitude)
        assertEquals(TimeCorrectionMode.STANDARD, input.timeCorrectionMode)
        assertEquals(ZasiMode.YAJASI_JEONGJASI, input.zasiMode)
    }

    // ── 유효성 검사 실패 케이스 ─────────────────────────────────────────

    @Test
    fun `연도 범위 초과 - 에러 메시지에 입력값 포함`() {
        val ex = assertThrows<IllegalArgumentException> {
            normalizer.normalize(solarInput(year = 1899))
        }
        assertTrue(ex.message!!.contains("1899"), "메시지에 입력값 포함: ${ex.message}")

        assertThrows<IllegalArgumentException> { normalizer.normalize(solarInput(year = 2101)) }
    }

    @Test
    fun `월 범위 초과`() {
        val ex = assertThrows<IllegalArgumentException> {
            normalizer.normalize(solarInput(month = 13))
        }
        assertTrue(ex.message!!.contains("13"))
    }

    @Test
    fun `존재하지 않는 날짜 - 2월 30일`() {
        val ex = assertThrows<IllegalArgumentException> {
            normalizer.normalize(solarInput(month = 2, day = 30))
        }
        assertTrue(ex.message!!.contains("2월"), "메시지: ${ex.message}")
    }

    @Test
    fun `비윤년 2월 29일은 거부, 윤년은 허용`() {
        assertThrows<IllegalArgumentException> {
            normalizer.normalize(solarInput(year = 1995, month = 2, day = 29))
        }
        // 1996은 윤년
        normalizer.normalize(solarInput(year = 1996, month = 2, day = 29))
    }

    @Test
    fun `시각 범위 초과`() {
        assertThrows<IllegalArgumentException> { normalizer.normalize(solarInput(hour = 24)) }
        assertThrows<IllegalArgumentException> { normalizer.normalize(solarInput(minute = 60)) }
    }

    @Test
    fun `양력 입력에 윤달 지정 시 예외`() {
        val ex = assertThrows<IllegalArgumentException> {
            normalizer.normalize(
                BirthInput(1995, 6, 15, 10, 0, isLeapMonth = true, gender = Gender.MALE)
            )
        }
        assertTrue(ex.message!!.contains("양력"))
    }

    @Test
    fun `음력 - 윤달 없는 해에 윤달 입력 시 예외`() {
        assertThrows<IllegalArgumentException> {
            normalizer.normalize(
                BirthInput(2024, 4, 1, 12, 0, calendarType = CalendarType.LUNAR, isLeapMonth = true, gender = Gender.MALE)
            )
        }
    }

    @Test
    fun `음력 - 작은달 30일 입력 시 예외`() {
        // 2024년 음력 1월은 29일까지
        assertThrows<IllegalArgumentException> {
            normalizer.normalize(
                BirthInput(2024, 1, 30, 12, 0, calendarType = CalendarType.LUNAR, gender = Gender.MALE)
            )
        }
    }

    @Test
    fun `경도 범위 초과`() {
        assertThrows<IllegalArgumentException> {
            normalizer.normalize(
                BirthInput(1995, 6, 15, 10, 0, gender = Gender.MALE, longitude = 200.0)
            )
        }
    }

    @Test
    fun `보정 후 지원 범위 이탈 시 예외`() {
        // 1908년 이전 한국 벽시계는 tzdata상 이미 LMT(+8:27:52)라서 서울 경도
        // 보정으로는 범위를 벗어나지 않음 — 서쪽 경도(90°)로 이탈 케이스 검증
        val ex = assertThrows<IllegalArgumentException> {
            normalizer.normalize(
                BirthInput(
                    1900, 1, 1, 0, 10, gender = Gender.MALE,
                    longitude = 90.0,
                    timeCorrectionMode = TimeCorrectionMode.LOCAL_MEAN_TIME,
                )
            )
        }
        assertTrue(ex.message!!.contains("보정"), "메시지: ${ex.message}")
    }

    @Test
    fun `1908년 이전 서울 LMT 보정은 벽시계와 거의 동일 (tzdata가 이미 LMT)`() {
        val result = normalizer.normalize(
            BirthInput(
                1900, 1, 1, 0, 10, gender = Gender.MALE,
                timeCorrectionMode = TimeCorrectionMode.LOCAL_MEAN_TIME,
            )
        )
        assertEquals(LocalDate.of(1900, 1, 1), result.corrected.toLocalDate())
    }

    // ── 자시 경계 정확성 ────────────────────────────────────────────────

    @Test
    fun `23시 출생 - 벽시계 날짜 유지된 채 정규화`() {
        val result = normalizer.normalize(solarInput(hour = 23, minute = 30))
        assertEquals(LocalDate.of(1995, 6, 15), result.corrected.toLocalDate())
        assertEquals(23, result.corrected.hour)
    }

    @Test
    fun `00시 출생 - 벽시계 날짜 유지된 채 정규화`() {
        val result = normalizer.normalize(solarInput(hour = 0, minute = 30))
        assertEquals(LocalDate.of(1995, 6, 15), result.corrected.toLocalDate())
        assertEquals(0, result.corrected.hour)
    }
}
