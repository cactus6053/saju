package com.saju.reading

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

// 일일 운세 캐시는 사주 수 × 날짜 수로 매일 쌓이고 지난 날짜는 다시 읽히지 않는다.
// DAILY만 기한 정리하고 나머지 kind(및 레거시 null)는 영구 보관.
@Component
class DailyReadingCleaner(
    private val repository: SajuReadingRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        val RETENTION: Duration = Duration.ofDays(7)
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    fun cleanUp() {
        val deleted = repository.deleteByKindAndCreatedAtBefore(
            ReadingKind.DAILY,
            Instant.now().minus(RETENTION),
        )
        if (deleted > 0) log.info("일일 운세 캐시 정리: {}건 삭제", deleted)
    }
}
