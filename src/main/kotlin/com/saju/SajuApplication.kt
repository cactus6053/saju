package com.saju

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class SajuApplication

fun main(args: Array<String>) {
    runApplication<SajuApplication>(*args)
}
