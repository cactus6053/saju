package com.saju.analysis

import com.saju.domain.core.Gender
import com.saju.engine.BirthInput
import com.saju.engine.DaeunCalculator
import com.saju.engine.SajuCalculator
import com.saju.engine.SajuResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FortuneServiceTest {

    private val service = FortuneService()
    private val daeunCalc = DaeunCalculator()

    // 2024-06-15 12:00 남 — 甲辰 庚午 庚戌 壬午, 순행, 대운수 7
    private fun wonguk(): SajuResult = SajuCalculator().calculate(
        BirthInput(2024, 6, 15, 12, 0, gender = Gender.MALE)
    )

    // ── 연도 기준 조회 ──────────────────────────────────────────────────

    @Test
    fun `출생 연도 조회 - 나이 0, 대운 기산 전`() {
        val fortune = service.fortuneOfYear(wonguk(), 2024)

        assertEquals(0, fortune.age)
        assertNull(fortune.currentDaeun, "대운수 7 이전은 대운 없음")
        assertTrue(fortune.daeunRelations.isEmpty())
        assertEquals("甲辰", fortune.seun.ganJi.hanja)
        assertEquals(12, fortune.wolunList.size)
    }

    @Test
    fun `대운 시작 연도 - 첫 대운 辛未 적용`() {
        // 대운수 7 → 2031년(나이 7)부터 辛未 대운
        val fortune = service.fortuneOfYear(wonguk(), 2031)

        assertEquals(7, fortune.age)
        assertEquals("辛未", fortune.currentDaeun?.ganJi?.hanja)
        assertTrue(fortune.daeunRelations.isNotEmpty(), "辛未 대운은 원국과 관계 존재(午未합 등)")
    }

    @Test
    fun `대운 경계 - 기산 전년은 null, 기산 연도부터 존재`() {
        assertNull(service.fortuneOfYear(wonguk(), 2030).currentDaeun)
        assertNotNull(service.fortuneOfYear(wonguk(), 2031).currentDaeun)
    }

    @Test
    fun `대운 전환 경계 - 17세에 두 번째 대운 壬申`() {
        assertEquals("辛未", service.fortuneOfYear(wonguk(), 2040).currentDaeun?.ganJi?.hanja)
        assertEquals("壬申", service.fortuneOfYear(wonguk(), 2041).currentDaeun?.ganJi?.hanja)
    }

    // ── 나이 기준 조회 ──────────────────────────────────────────────────

    @Test
    fun `나이 기준과 연도 기준 조회 결과 동일`() {
        val byAge = service.fortuneOfAge(wonguk(), 10)
        val byYear = service.fortuneOfYear(wonguk(), 2034)
        assertEquals(byYear, byAge)
    }

    // ── 구성 요소 정합성 ────────────────────────────────────────────────

    @Test
    fun `세운과 월운이 동일한 현재 대운을 참조`() {
        val fortune = service.fortuneOfYear(wonguk(), 2031)
        val daeunGanJi = fortune.currentDaeun!!.ganJi

        // 세운의 대운 관계 = relationsBetween(세운, 대운)과 일치
        val expected = RelationAnalyzer().relationsBetween(fortune.seun.ganJi, daeunGanJi)
        assertEquals(expected, fortune.seun.relationsWithDaeun)

        // 월운도 대운 관계 계산에 같은 대운 사용
        fortune.wolunList.forEach { wolun ->
            val expectedWolun = RelationAnalyzer().relationsBetween(wolun.ganJi, daeunGanJi)
            assertEquals(expectedWolun, wolun.relationsWithDaeun, "${wolun.month}월 대운 관계 불일치")
        }
    }

    @Test
    fun `월운 12개는 1~12월 순서`() {
        val fortune = service.fortuneOfYear(wonguk(), 2031)
        assertEquals((1..12).toList(), fortune.wolunList.map { it.month })
        assertTrue(fortune.wolunList.all { it.year == 2031 })
    }

    // ── 대운 타임라인 ───────────────────────────────────────────────────

    @Test
    fun `대운 타임라인 - 10개 대운과 원국 관계 요약`() {
        val timeline = service.daeunTimeline(wonguk())

        assertEquals(10, timeline.size)
        assertEquals(
            daeunCalc.calculate(wonguk()).daeunList,
            timeline.map { it.daeun },
            "타임라인 대운 배열은 DaeunCalculator와 일치",
        )

        // 甲戌 대운(4번째)은 원국 辰과 辰戌충 존재
        val gapsul = timeline.first { it.daeun.ganJi.hanja == "甲戌" }
        assertTrue(
            gapsul.relationsWithWonguk.any { it.type == RelationType.YUK_CHUNG },
            "甲戌 대운과 辰 연지의 충: ${gapsul.relationsWithWonguk}",
        )
    }

    // ── 범위 검증 ───────────────────────────────────────────────────────

    @Test
    fun `출생 이전 연도 조회는 예외`() {
        val ex = assertThrows<IllegalArgumentException> {
            service.fortuneOfYear(wonguk(), 2023)
        }
        assertTrue(ex.message!!.contains("2023"))
    }

    @Test
    fun `범위 밖 연도 조회는 예외`() {
        assertThrows<IllegalArgumentException> { service.fortuneOfYear(wonguk(), 2101) }
    }

    @Test
    fun `입춘 전 출생 - 사주 연도 기준 나이 계산`() {
        // 2024-01-15 출생: 사주 연도 2023 → 2033년 조회 시 나이 10
        val saju = SajuCalculator().calculate(
            BirthInput(2024, 1, 15, 12, 0, gender = Gender.MALE)
        )
        assertEquals(2023, saju.sajuYear)
        assertEquals(10, service.fortuneOfYear(saju, 2033).age)
    }
}
