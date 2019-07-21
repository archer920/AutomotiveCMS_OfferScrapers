package com.stonesoupprogramming.automotivecms.offers.offerscraper.cron

import com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.ScrapeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class Scheduler(
        @Qualifier("MBZLA New Car") private val mbzlaNewCarOfferScrape: ScrapeService,
        @Qualifier("MBZLA Used Car") private val mbzlaUsedCarOfferScrape: ScrapeService,
        @Qualifier("Car and Driver") private val carAndDriverScrape: ScrapeService) {

    private val logger = LoggerFactory.getLogger(Scheduler::class.java)

    @PostConstruct
    fun scrapeOnStartup(){
        val futures = listOf(
                mbzlaNewCarOfferScrape,
                mbzlaUsedCarOfferScrape, 
                carAndDriverScrape
        ).map { it.scrape() }.toMutableList()

        while(futures.isNotEmpty()){
            futures.filter { it.isDone }.forEach { logger.info("Result of task = ${it.get()}"); futures.remove(it) }
            Thread.sleep(1000)
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

    fun scrapeCarAndDriver(){
        logger.info("Starting Car and Driver Scrape")
        logger.info("Car and Driver Scrape Result = ${carAndDriverScrape.scrape().get()}")
    }
}
