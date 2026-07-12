package com.saju.reading

import org.springframework.data.jpa.repository.JpaRepository

interface SajuReadingRepository : JpaRepository<SajuReadingEntity, Long> {
    fun findByCacheKey(cacheKey: String): SajuReadingEntity?
}
