package com.saju.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimeCorrectorTest {

    private val corrector = TimeCorrector()

    // ── UTC 변환 ─────────────────────────────────────────────────────────

    @Test
    fun `현대 KST는 UTC+9`() {
        val utc = corrector.toUtc(LocalDateTime.of(2024, 6, 15, 12, 0))
        assertEquals(LocalDateTime.of(2024, 6, 15, 3, 0), utc)
    }

    @Test
    fun `1955년은 UTC+8_30 시기`() {
        // 1954-03-21 ~ 1961-08-09: 한국 표준시 UTC+8:30
        val utc = corrector.toUtc(LocalDateTime.of(1955, 1, 15, 12, 0))
        assertEquals(LocalDateTime.of(1955, 1, 15, 3, 30), utc)
    }

    @Test
    fun `1988년 여름은 서머타임 UTC+10`() {
        // 1988-05-08 ~ 10-09: 서머타임 실시
        val utc = corrector.toUtc(LocalDateTime.of(1988, 7, 15, 12, 0))
        assertEquals(LocalDateTime.of(1988, 7, 15, 2, 0), utc)
    }

    @Test
    fun `1988년 겨울은 서머타임 아님 UTC+9`() {
        val utc = corrector.toUtc(LocalDateTime.of(1988, 12, 15, 12, 0))
        assertEquals(LocalDateTime.of(1988, 12, 15, 3, 0), utc)
    }

    // ── STANDARD 모드: 균일 KST 프레임 정규화 ───────────────────────────

    @Test
    fun `STANDARD - 현대 시각은 변화 없음`() {
        val input = LocalDateTime.of(2024, 6, 15, 12, 0)
        assertEquals(input, corrector.correct(input, mode = TimeCorrectionMode.STANDARD))
    }

    @Test
    fun `STANDARD - 1955년 벽시계는 +30분 보정`() {
        // 당시 UTC+8:30 벽시계 12:00 = UTC 03:30 = KST 프레임 12:30
        val corrected = corrector.correct(
            LocalDateTime.of(1955, 1, 15, 12, 0),
            mode = TimeCorrectionMode.STANDARD,
        )
        assertEquals(LocalDateTime.of(1955, 1, 15, 12, 30), corrected)
    }

    @Test
    fun `STANDARD - 1988년 서머타임 벽시계는 -1시간 보정`() {
        val corrected = corrector.correct(
            LocalDateTime.of(1988, 7, 15, 12, 0),
            mode = TimeCorrectionMode.STANDARD,
        )
        assertEquals(LocalDateTime.of(1988, 7, 15, 11, 0), corrected)
    }

    // ── LOCAL_MEAN_TIME 모드 ─────────────────────────────────────────────

    @Test
    fun `LMT - 서울은 KST보다 약 32분 느림`() {
        val kst = LocalDateTime.of(2024, 6, 15, 12, 0)
        val lmt = corrector.correct(kst, TimeCorrector.SEOUL_LONGITUDE, TimeCorrectionMode.LOCAL_MEAN_TIME)

        // 서울 경도 126.978° → UTC+8:27:54 → KST 대비 -32분 6초
        val diffSeconds = Duration.between(lmt, kst).seconds
        assertTrue(diffSeconds in 1900..1940, "서울 LMT 보정량 이상: ${diffSeconds}초")
    }

    @Test
    fun `LMT - 동경 135도는 KST와 동일`() {
        val kst = LocalDateTime.of(2024, 6, 15, 12, 0)
        val lmt = corrector.correct(kst, 135.0, TimeCorrectionMode.LOCAL_MEAN_TIME)
        assertEquals(kst, lmt)
    }

    @Test
    fun `LMT - 경도가 동쪽일수록 시각이 빠름`() {
        val kst = LocalDateTime.of(2024, 6, 15, 12, 0)
        val busan = corrector.correct(kst, 129.0759, TimeCorrectionMode.LOCAL_MEAN_TIME)
        val seoul = corrector.correct(kst, 126.9780, TimeCorrectionMode.LOCAL_MEAN_TIME)
        assertTrue(busan.isAfter(seoul), "부산(동쪽) LMT가 서울보다 빨라야 함")
    }

    @Test
    fun `LMT - 경도 범위 초과 시 예외`() {
        assertThrows<IllegalArgumentException> {
            corrector.correct(
                LocalDateTime.of(2024, 1, 1, 0, 0), 181.0, TimeCorrectionMode.LOCAL_MEAN_TIME,
            )
        }
    }

    // ── 균시차 ───────────────────────────────────────────────────────────

    @Test
    fun `균시차 - 11월 초는 +14분 이상`() {
        // 11월 3일경 균시차 최대 (+16.4분)
        val eot = corrector.equationOfTimeMinutes(307)
        assertTrue(eot > 14.0, "11월 초 균시차: $eot")
    }

    @Test
    fun `균시차 - 2월 중순은 -13분 이하`() {
        // 2월 11일경 균시차 최소 (-14.2분)
        val eot = corrector.equationOfTimeMinutes(42)
        assertTrue(eot < -13.0, "2월 중순 균시차: $eot")
    }

    @Test
    fun `균시차 - 연중 범위는 -16 ~ +17분 이내`() {
        (1..365).forEach { day ->
            val eot = corrector.equationOfTimeMinutes(day)
            assertTrue(abs(eot) < 17.0, "day=$day 균시차 범위 초과: $eot")
        }
    }

    @Test
    fun `진태양시 - LMT에 균시차가 더해짐`() {
        val kst = LocalDateTime.of(2024, 11, 3, 12, 0)
        val lmt = corrector.correct(kst, TimeCorrector.SEOUL_LONGITUDE, TimeCorrectionMode.LOCAL_MEAN_TIME)
        val apparent = corrector.correct(kst, TimeCorrector.SEOUL_LONGITUDE, TimeCorrectionMode.APPARENT_SOLAR_TIME)

        // 11월 초: 진태양시가 LMT보다 약 16분 빠름
        val diffMinutes = Duration.between(lmt, apparent).toMinutes()
        assertTrue(diffMinutes in 14..17, "11월 진태양시 보정량: ${diffMinutes}분")
    }

    // ── 경계 케이스 ──────────────────────────────────────────────────────

    @Test
    fun `서머타임 전환 직후 존재하지 않는 시각도 예외 없이 처리`() {
        // 1987-05-10 02:00에 시계가 03:00으로 점프 → 02:30은 존재하지 않는 벽시계
        val corrected = corrector.correct(
            LocalDateTime.of(1987, 5, 10, 2, 30),
            mode = TimeCorrectionMode.STANDARD,
        )
        assertEquals(1987, corrected.year)
    }

    @Test
    fun `자정 부근 보정 시 날짜가 정확히 넘어감`() {
        // 1955년 벽시계 23:45 → KST 프레임 다음날 00:15
        val corrected = corrector.correct(
            LocalDateTime.of(1955, 1, 15, 23, 45),
            mode = TimeCorrectionMode.STANDARD,
        )
        assertEquals(LocalDateTime.of(1955, 1, 16, 0, 15), corrected)
    }
}
