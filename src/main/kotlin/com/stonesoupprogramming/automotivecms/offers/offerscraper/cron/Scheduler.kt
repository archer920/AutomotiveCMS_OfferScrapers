package com.stonesoupprogramming.automotivecms.offers.offerscraper.cron

import com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.ScrapeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.support.CronSequenceGenerator
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

private const val EVERY_30_MINUTES = "0 0,30 * * * *"
private const val DAILY = "0 0 8 ? * MON-FRI"
private const val WEEKLY = "0 0 8 ? * MON"

private fun String.isValidCron(): Boolean = CronSequenceGenerator.isValidExpression(this)

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
    fun scrapeOnStartup() {
        if (!EVERY_30_MINUTES.isValidCron()){
            throw IllegalArgumentException("$EVERY_30_MINUTES is not a valid cron expression")
        }
        if (!DAILY.isValidCron()){
            throw IllegalArgumentException("$DAILY is not a valid cron expression")
        }
        if (!WEEKLY.isValidCron()){
            throw IllegalArgumentException("$EVERY_30_MINUTES is not a valid cron expression")
        }

        try {
            mapOf(
                    "MBZLA New Car Offers" to mbzlaNewCarOfferScrape,
                    "MBZLA Used Cars Offers" to mbzlaUsedCarOfferScrape//,
//                    "Motor Trend" to motorTrendScraper,
//                    "Left Lane News" to leftLaneNewsScraper,
//                    "Jalopnik" to jalopnikScraper,
//                    "Edmunds" to edmundsScraper,
//                    "Cars.com" to carsScraper,
//                    "Car and Driver" to carAndDriverScrape
            ).forEach {
                logger.info("Starting scrape for ${it.key}")
                it.value.scrape().thenAccept { sr ->
                    logger.info("Result of ${it.key} = $sr")
                }
            }
        } catch (e: Exception){
            logger.error("Exception on bean startup. Will resume with scheduled scraping", e)
        }
    }

    @Scheduled(cron = EVERY_30_MINUTES)
    fun scrapeMbzlaNew(){
        logger.info("Starting MBZLA New Car Scrape")
        val result = mbzlaNewCarOfferScrape.scrape()

        logger.info("MBZLA New Car Scrape Result = ${result.get()}")
    }

    @Scheduled(cron = EVERY_30_MINUTES)
    fun scrapeMbzlaUsed(){
        logger.info("Starting MBZLA Used Car Scrape")
        val result = mbzlaUsedCarOfferScrape.scrape()

        logger.info("MBZLA Used Car Scrape Result = ${result.get()}")
    }

    @Scheduled(cron = DAILY)
    fun scrapeCarAndDriver(){
        logger.info("Starting Car and Driver Scrape")
        logger.info("Car and Driver Scrape Result = ${carAndDriverScrape.scrape().get()}")
    }

    @Scheduled(cron = WEEKLY)
    fun scrapeEdmunds() {
        logger.info("Starting Edmunds Scrape")
        logger.info("Edmunds Scrape Result = ${edmundsScraper.scrape().get()}")
    }

    @Scheduled(cron = DAILY)
    fun scrapeCars() {
        logger.info("Starting Cars.com Scrape")
        logger.info("Cars.com Scrape Result = ${carsScraper.scrape().get()}")
    }

    @Scheduled(cron = DAILY)
    fun scrapeJalopnik(){
        logger.info("Starting Jalopnik Scrape")
        logger.info("Jalopnik Scrape Result = ${carAndDriverScrape.scrape().get()}")
    }

    @Scheduled(cron = DAILY)
    fun scrapeLeftLaneNews(){
        logger.info("Starting Left Lane News Scrape")
        logger.info("Left Lane News Scrape Result = ${carAndDriverScrape.scrape().get()}")
    }
}
