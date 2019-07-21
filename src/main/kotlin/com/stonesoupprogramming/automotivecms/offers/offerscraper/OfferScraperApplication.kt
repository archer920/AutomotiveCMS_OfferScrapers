package com.stonesoupprogramming.automotivecms.offers.offerscraper

import com.stonesoupprogramming.automotivecms.offers.offerscraper.cron.Scheduler
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor


@SpringBootApplication
@EnableAsync
@EnableScheduling
class OfferScraperApplication(private val scheduler: Scheduler): CommandLineRunner {

    override fun run(vararg args: String?) {

    }

    @Bean
    fun taskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 2
        executor.setQueueCapacity(500)
        executor.setThreadNamePrefix("OfferScrape-")
        executor.initialize()
        return executor
    }
}

fun main(args: Array<String>) {
    runApplication<OfferScraperApplication>(*args)
}
