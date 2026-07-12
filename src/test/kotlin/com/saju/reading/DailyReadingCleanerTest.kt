package com.saju.reading

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
class DailyReadingCleanerTest(
    @Autowired private val cleaner: DailyReadingCleaner,
    @Autowired private val repository: SajuReadingRepository,
) {

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    private fun entity(cacheKey: String, kind: ReadingKind?, createdAt: Instant) = SajuReadingEntity(
        cacheKey = cacheKey,
        model = "test-model",
        kind = kind,
        content = "내용",
        createdAt = createdAt,
    )

    @Test
    fun `보관 기한 지난 DAILY만 삭제 - 다른 kind와 레거시 null은 보존`() {
        val old = Instant.now().minus(8, ChronoUnit.DAYS)
        val recent = Instant.now().minus(1, ChronoUnit.DAYS)

        repository.saveAll(
            listOf(
                entity("daily-old", ReadingKind.DAILY, old),
                entity("daily-recent", ReadingKind.DAILY, recent),
                entity("wonguk-old", ReadingKind.WONGUK, old),
                entity("yearly-old", ReadingKind.YEARLY, old),
                entity("legacy-old", null, old),
            )
        )

        cleaner.cleanUp()

        assertNull(repository.findByCacheKey("daily-old"))
        assertNotNull(repository.findByCacheKey("daily-recent"))
        assertNotNull(repository.findByCacheKey("wonguk-old"))
        assertNotNull(repository.findByCacheKey("yearly-old"))
        assertNotNull(repository.findByCacheKey("legacy-old"))
        assertEquals(4, repository.count())
    }

    @Test
    fun `삭제 대상이 없으면 아무것도 지우지 않음`() {
        repository.save(entity("daily-recent", ReadingKind.DAILY, Instant.now()))

        cleaner.cleanUp()

        assertEquals(1, repository.count())
    }
}
