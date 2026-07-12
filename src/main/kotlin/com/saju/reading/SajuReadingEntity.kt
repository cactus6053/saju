package com.saju.reading

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

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

    @Lob
    @Column(nullable = false)
    val content: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
