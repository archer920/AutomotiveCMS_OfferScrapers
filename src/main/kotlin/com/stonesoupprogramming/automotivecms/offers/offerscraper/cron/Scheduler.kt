package com.stonesoupprogramming.automotivecms.offers.offerscraper.cron

import com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.ScrapeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import javax.annotation.PostConstruct

@Component
class Scheduler(
        @Qualifier("MBZLA New Car") private val mbzlaNewCarOfferScrape: ScrapeService,
        @Qualifier("MBZLA Used Car") private val mbzlaUsedCarOfferScrape: ScrapeService,
        @Qualifier("Car and Driver") private val carAndDriverScrape: ScrapeService,
        @Qualifier("Cars.com") private val carsScraper: ScrapeService,
        @Qualifier("Edmunds") private val edmundsScraper: ScrapeService,
        @Qualifier("Jalopnik") private val jalopnikScraper: ScrapeService,
        @Qualifier("Left Lane News") private val leftLaneNewsScraper: ScrapeService,
        @Qualifier("Motor Trend") private val motorTrendScraper: ScrapeService) {

    private val logger = LoggerFactory.getLogger(Scheduler::class.java)

    @PostConstruct
    fun scrapeOnStartup(){
        try {
            val futures = listOf(
                    motorTrendScraper,
                    leftLaneNewsScraper,
                    jalopnikScraper,
                    edmundsScraper,
                    carsScraper,
                    mbzlaNewCarOfferScrape,
                    mbzlaUsedCarOfferScrape,
                    carAndDriverScrape
            ).map { it.scrape() }

            CompletableFuture.allOf(*futures.toTypedArray()).get()
            futures.forEach { logger.info("Result of task = ${it.get()}") }
        } catch (e: Exception){
            logger.error("Exception on bean startup. Will resume with scheduled scraping", e)
        }
    }

    @Scheduled(cron = "0 0,30 * * * *")
    fun scrapeMbzlaNew(){
        logger.info("Starting MBZLA New Car Scrape")
        val result = mbzlaNewCarOfferScrape.scrape()

        logger.info("MBZLA New Car Scrape Result = ${result.get()}")
    }

    @Scheduled(cron = "0 0,30 * * * *")
    fun scrapeMbzlaUsed(){
        logger.info("Starting MBZLA Used Car Scrape")
        val result = mbzlaUsedCarOfferScrape.scrape()

        logger.info("MBZLA Used Car Scrape Result = ${result.get()}")
    }

    @Scheduled(cron = "0 0 8 1/1 * ? *")
    fun scrapeCarAndDriver(){
        logger.info("Starting Car and Driver Scrape")
        logger.info("Car and Driver Scrape Result = ${carAndDriverScrape.scrape().get()}")
    }

    @Scheduled(cron = "0 0 8 ? * MON *")
    fun scrapeEdmunds() {
        logger.info("Starting Edmunds Scrape")
        logger.info("Edmunds Scrape Result = ${edmundsScraper.scrape().get()}")
    }

    @Scheduled(cron = "0 0 8 ? * MON *")
    fun scrapeCars() {
        logger.info("Starting Cars.com Scrape")
        logger.info("Cars.com Scrape Result = ${carsScraper.scrape().get()}")
    }

    @Scheduled(cron = "0 0 8 1/1 * ? *")
    fun scrapeJalopnik(){
        logger.info("Starting Jalopnik Scrape")
        logger.info("Jalopnik Scrape Result = ${carAndDriverScrape.scrape().get()}")
    }

    @Scheduled(cron = "0 0 8 1/1 * ? *")
    fun scrapeLeftLaneNews(){
        logger.info("Starting Left Lane News Scrape")
        logger.info("Left Lane News Scrape Result = ${carAndDriverScrape.scrape().get()}")
    }
}
