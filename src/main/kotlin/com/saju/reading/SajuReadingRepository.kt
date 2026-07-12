package com.saju.reading

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface SajuReadingRepository : JpaRepository<SajuReadingEntity, Long> {
    fun findByCacheKey(cacheKey: String): SajuReadingEntity?
    fun deleteByKindAndCreatedAtBefore(kind: ReadingKind, cutoff: Instant): Long
}
