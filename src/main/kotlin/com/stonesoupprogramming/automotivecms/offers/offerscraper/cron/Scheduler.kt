package com.stonesoupprogramming.automotivecms.offers.offerscraper.cron

import com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.ScraperService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class Scheduler(
        @Qualifier("MBZLA New Car")
        private val mbzlaNewCarOfferScraper: ScraperService,

        @Qualifier("MBZLA Used Car")
        private val mbzlaUsedCarOfferScraper: ScraperService) {

    private val logger = LoggerFactory.getLogger(Scheduler::class.java)

    @Scheduled(cron = "0 0,30 * * * *")
    fun scrapeMbzlaNew(){
        logger.info("Starting MBZLA New Car Scrape")
        val result = mbzlaNewCarOfferScraper.scrapeOffers()

        logger.info("MBZLA New Car Scrape Result = ${result.get()}")
    }

    @Scheduled(cron = "0 0,30 * * * *")
    fun scrapeMbzlaUsed(){
        logger.info("Starting MBZLA Used Car Scrape")
        val result = mbzlaUsedCarOfferScraper.scrapeOffers()

        logger.info("MBZLA Used Car Scrape Result = ${result.get()}")
    }
}
