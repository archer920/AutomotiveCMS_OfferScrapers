package com.stonesoupprogramming.automotivecms.offers.offerscraper

import com.stonesoupprogramming.automotivecms.offers.offerscraper.functions.toDate
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.time.LocalDate
import java.util.*
import java.util.concurrent.*

data class AppProperties(val host: String, val publicationExpirationDate: Int) {

    fun expirationDate(): Date {
        return LocalDate.now().minusDays(publicationExpirationDate.toLong()).toDate()
    }
}

@SpringBootApplication
@EnableAsync
@EnableScheduling
class OfferScraperApplication(private val applicationContext: AbstractApplicationContext): CommandLineRunner {

    private val logger = LoggerFactory.getLogger(OfferScraperApplication::class.java)

    override fun run(vararg args: String?) {

    }

    @Bean
    fun taskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = Runtime.getRuntime().availableProcessors()
        executor.maxPoolSize = Runtime.getRuntime().availableProcessors() * 2
        executor.setQueueCapacity(500)
        executor.setThreadNamePrefix("Scrape-")
        executor.initialize()
        return executor
    }

    @Bean
    fun executorService(): ExecutorService {
        var count = 0
        return ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors() * 4,
                1,
                TimeUnit.HOURS,
                LinkedBlockingQueue(),
                ThreadFactory { r ->
                    val t = Thread(r)
                    t.name = "Scrape-${count++}"
                    t
                }
        )
    }

    @Bean
    fun properties(): AppProperties {
        val host = host()!!
        val expirationDate = expirationDate()!!
        return AppProperties(host = host, publicationExpirationDate = expirationDate)
    }

    private fun expirationDate(): Int? {
        return try {
            if (System.getenv().containsKey("PUBLICATION_EXP_DAYS")){
                val v = System.getenv()["PUBLICATION_EXP_DAYS"]!!.toInt()
                if (v <= 0){
                    logger.error("PUBLICATION_EXP_DAYS has to be positive")
                    SpringApplication.exit(applicationContext, ExitCodeGenerator { -1 })
                    null
                } else {
                    v
                }
            } else {
                logger.error("Please set the property PUBLICATION_EXP_DAYS")
                SpringApplication.exit(applicationContext, ExitCodeGenerator { -1 })
                null
            }
        } catch (e: NumberFormatException){
            logger.error("Expiration days has to be numeric")
            SpringApplication.exit(applicationContext, ExitCodeGenerator { -1 })
            null
        }
    }

    private fun host(): String? {
        return if (System.getenv().containsKey("SYSTEM_HOST")){
            System.getenv()["SYSTEM_HOST"]!!
        } else {
            logger.error("Please set the property SYSTEM_HOST")
            SpringApplication.exit(applicationContext, ExitCodeGenerator { -1 })
            null
        }
    }
}

fun main(args: Array<String>) {
    runApplication<OfferScraperApplication>(*args)
}
