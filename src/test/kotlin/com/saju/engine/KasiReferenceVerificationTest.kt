package com.saju.engine

import com.saju.domain.core.Jeolgi
import com.saju.domain.core.LunarDate
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// 레퍼런스 파일(kasi_reference.csv) 기반 자동 대조 검증.
// 새 레퍼런스 값은 코드 수정 없이 CSV에 행 추가로 반영된다.
class KasiReferenceVerificationTest {

    private data class ReferenceRow(
        val category: String,
        val key: String,
        val expected: String,
        val toleranceMinutes: Long,
        val source: String,
    )

    private val rows: List<ReferenceRow> = javaClass
        .getResourceAsStream("/reference/kasi_reference.csv")!!
        .bufferedReader()
        .readLines()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .map { line ->
            val parts = line.split(",")
            ReferenceRow(parts[0], parts[1], parts[2], parts[3].toLong(), parts[4])
        }

    private val jeolgiCalc = JeolgiCalculator()
    private val lunarConverter = LunarConverter()
    private val dayGanjiCalc = DayGanjiCalculator()

    @Test
    fun `절기 - 레퍼런스 시각과 허용 오차 이내`() {
        val deviations = rows.filter { it.category == "jeolgi" }.map { row ->
            val (yearStr, jeolgiName) = row.key.split(":")
            val jeolgi = Jeolgi.entries.first { it.hangul == jeolgiName }
            val computed = jeolgiCalc.getMoment(yearStr.toInt(), jeolgi)
            val expected = LocalDateTime.parse(row.expected)
            val diffMinutes = Duration.between(expected, computed).toMinutes()

            assertTrue(
                abs(diffMinutes) <= row.toleranceMinutes,
                "${row.key}: 계산=$computed, 기대=$expected, " +
                    "차이=${diffMinutes}분 (허용 ±${row.toleranceMinutes}, 출처 ${row.source})",
            )
            row.key to diffMinutes
        }

        val maxDev = deviations.maxBy { abs(it.second) }
        val meanDev = deviations.map { abs(it.second) }.average()
        println("절기 대조 ${deviations.size}건 — 평균 편차 ${"%.1f".format(meanDev)}분, " +
            "최대 편차 ${maxDev.second}분 (${maxDev.first})")
    }

    @Test
    fun `설날 - 음력 1월 1일 양력 날짜 정확 일치`() {
        rows.filter { it.category == "seollal" }.forEach { row ->
            val computed = lunarConverter.lunarToSolar(LunarDate(row.key.toInt(), 1, 1))
            assertEquals(
                LocalDate.parse(row.expected), computed,
                "${row.key}년 설날 불일치 (출처 ${row.source})",
            )
        }
    }

    @Test
    fun `추석 - 음력 8월 15일 양력 날짜 정확 일치`() {
        rows.filter { it.category == "chuseok" }.forEach { row ->
            val computed = lunarConverter.lunarToSolar(LunarDate(row.key.toInt(), 8, 15))
            assertEquals(
                LocalDate.parse(row.expected), computed,
                "${row.key}년 추석 불일치 (출처 ${row.source})",
            )
        }
    }

    @Test
    fun `일진 - 앵커 날짜 간지 정확 일치`() {
        rows.filter { it.category == "iljin" }.forEach { row ->
            val computed = dayGanjiCalc.calculate(LocalDate.parse(row.key))
            assertEquals(
                row.expected, computed.hanja,
                "${row.key} 일진 불일치 (출처 ${row.source})",
            )
        }
    }

    @Test
    fun `레퍼런스 파일 무결성 - 전 카테고리 존재`() {
        val categories = rows.map { it.category }.toSet()
        assertEquals(setOf("jeolgi", "seollal", "chuseok", "iljin"), categories)
        assertTrue(rows.size >= 20, "레퍼런스 데이터 ${rows.size}건 — 최소 20건 필요")
    }
}
