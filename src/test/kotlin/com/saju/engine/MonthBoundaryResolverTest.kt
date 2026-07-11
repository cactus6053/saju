package com.saju.engine

import com.saju.domain.core.Jeolgi
import com.saju.domain.core.JiJi
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonthBoundaryResolverTest {

    private val jeolgiCalc = JeolgiCalculator()
    private val resolver = MonthBoundaryResolver(jeolgiCalc)

    // ── 월 중간 날짜 판정 (절기 경계에서 충분히 먼 날짜) ────────────────

    @ParameterizedTest(name = "{0} → {1}월")
    @CsvSource(
        "2024-02-20T12:00, IN",   // 입춘~경칩: 인월
        "2024-03-20T12:00, MYO",  // 경칩~청명: 묘월
        "2024-04-20T12:00, JIN",  // 청명~입하: 진월
        "2024-05-20T12:00, SA",   // 입하~망종: 사월
        "2024-06-15T12:00, O",    // 망종~소서: 오월
        "2024-07-20T12:00, MI",   // 소서~입추: 미월
        "2024-08-20T12:00, SHIN", // 입추~백로: 신월
        "2024-09-20T12:00, YU",   // 백로~한로: 유월
        "2024-10-20T12:00, SUL",  // 한로~입동: 술월
        "2024-11-20T12:00, HAE",  // 입동~대설: 해월
        "2024-12-20T12:00, JA",   // 대설~소한: 자월
        "2024-01-20T12:00, CHUK", // 소한~입춘: 축월
    )
    fun `월 중간 날짜의 월지 판정`(dateTimeStr: String, expectedBranch: JiJi) {
        val result = resolver.resolve(LocalDateTime.parse(dateTimeStr))
        assertEquals(expectedBranch, result.monthBranch)
    }

    // ── 연 경계 케이스 ───────────────────────────────────────────────────

    @Test
    fun `1월 초는 전년도 대설이 지배하는 자월`() {
        val result = resolver.resolve(LocalDateTime.of(2024, 1, 3, 12, 0))
        assertEquals(JiJi.JA, result.monthBranch)
        assertEquals(Jeolgi.DAESEOL, result.governingJeol)
        assertEquals(2023, result.jeolMoment.year, "지배 절기는 전년도 12월의 대설")
    }

    @Test
    fun `12월 말은 당해 대설이 지배하는 자월`() {
        val result = resolver.resolve(LocalDateTime.of(2024, 12, 31, 23, 59))
        assertEquals(JiJi.JA, result.monthBranch)
        assertEquals(2024, result.jeolMoment.year)
    }

    // ── 절입 시각 경계 (분·초 단위) ─────────────────────────────────────

    @Test
    fun `절입 시각 정각은 새 달로 판정`() {
        val ipchun = jeolgiCalc.getMoment(2024, Jeolgi.IPCHUN)
        val result = resolver.resolve(ipchun)
        assertEquals(JiJi.IN, result.monthBranch)
        assertEquals(Jeolgi.IPCHUN, result.governingJeol)
    }

    @Test
    fun `절입 1초 전은 이전 달로 판정`() {
        val ipchun = jeolgiCalc.getMoment(2024, Jeolgi.IPCHUN)
        val result = resolver.resolve(ipchun.minusSeconds(1))
        assertEquals(JiJi.CHUK, result.monthBranch)
        assertEquals(Jeolgi.SOHAN, result.governingJeol)
    }

    @Test
    fun `절입 1초 후는 새 달로 판정`() {
        val gyeongchip = jeolgiCalc.getMoment(2024, Jeolgi.GYEONGCHIP)
        val result = resolver.resolve(gyeongchip.plusSeconds(1))
        assertEquals(JiJi.MYO, result.monthBranch)
    }

    @Test
    fun `12절 모든 경계에서 직전-직후 월지가 다름`() {
        Jeolgi.JEOL_LIST.forEach { jeol ->
            val moment = jeolgiCalc.getMoment(2024, jeol)
            val before = resolver.resolve(moment.minusSeconds(1))
            val after = resolver.resolve(moment)
            assertTrue(
                before.monthBranch != after.monthBranch,
                "${jeol.hangul} 경계에서 월지 변화 없음"
            )
            assertEquals(jeol.monthBranch, after.monthBranch, "${jeol.hangul} 직후 월지 불일치")
        }
    }

    // ── 결과 구조 일관성 ────────────────────────────────────────────────

    @Test
    fun `입력 시각은 항상 jeolMoment와 nextJeolMoment 사이`() {
        val samples = listOf(
            LocalDateTime.of(2024, 1, 1, 0, 0),
            LocalDateTime.of(2024, 6, 15, 12, 30),
            LocalDateTime.of(2024, 12, 31, 23, 59),
            LocalDateTime.of(1950, 3, 10, 8, 0),
            LocalDateTime.of(2099, 7, 7, 7, 7),
        )
        samples.forEach { kst ->
            val result = resolver.resolve(kst)
            assertTrue(!result.jeolMoment.isAfter(kst), "$kst: jeolMoment가 입력보다 늦음")
            assertTrue(result.nextJeolMoment.isAfter(kst), "$kst: nextJeolMoment가 입력보다 이름")
        }
    }

    @Test
    fun `1년 사이클에 12개 월지가 모두 등장`() {
        val branches = Jeolgi.JEOL_LIST.map { jeol ->
            val moment = jeolgiCalc.getMoment(2024, jeol)
            resolver.resolve(moment.plusHours(1)).monthBranch
        }.toSet()
        assertEquals(12, branches.size, "12개 월지가 모두 나와야 함: $branches")
    }

    @Test
    fun `과거와 미래 연도에서도 동작 - 1900, 2100`() {
        val old = resolver.resolve(LocalDateTime.of(1900, 6, 15, 12, 0))
        assertEquals(JiJi.O, old.monthBranch)

        val future = resolver.resolve(LocalDateTime.of(2100, 6, 15, 12, 0))
        assertEquals(JiJi.O, future.monthBranch)
    }
}
