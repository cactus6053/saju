package com.saju.reading

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

// DAILY만 보관 기한이 있고 나머지는 영구 — 정리 배치가 이 값으로 구분한다
enum class ReadingKind {
    WONGUK, DAEUN, MARRIAGE, YEARLY, DAILY,
}

// 해석문 영구 캐시. 엔진 출력이 결정적이므로 같은 cache_key의 해석문은 영원히 유효.
@Entity
@Table(
    name = "saju_reading",
    uniqueConstraints = [UniqueConstraint(name = "uk_saju_reading_cache_key", columnNames = ["cache_key"])],
)
class SajuReadingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "cache_key", nullable = false, length = 64)
    val cacheKey: String,

    @Column(nullable = false, length = 64)
    val model: String,

    // 컬럼 추가 이전의 레거시 행은 null — 전부 영구 보관 종류라 정리 대상 아님
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    val kind: ReadingKind? = null,

    // @Lob은 MariaDB에서 기본 길이 255로 TINYTEXT를 생성하는 함정이 있어
    // 명시적 길이 사용 — MariaDB는 MEDIUMTEXT, H2는 VARCHAR로 매핑됨
    @Column(nullable = false, length = 1_000_000)
    val content: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
