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
        @Qualifier("Torrence New Car") private val torrenceNewCarOfferScrape: ScrapeService,
        @Qualifier("Torrence Used Car") private val torrenceUsedCarOfferScrape: ScrapeService) {

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
                    "MBZLA Used Cars Offers" to mbzlaUsedCarOfferScrape,
                    "Torrence Used Car Offers" to torrenceUsedCarOfferScrape,
                    "Torrence New Car Offers" to torrenceNewCarOfferScrape
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
    fun scrapeTorrenceNew() {
        logger.info("Starting Torrence New Car Scrape")
        val result = torrenceNewCarOfferScrape.scrape()

        logger.info("Torrence New Car Scrape Result = ${result.get()}")
    }

    @Scheduled(cron = EVERY_30_MINUTES)
    fun scrapeTorrenceUsed() {
        logger.info("Starting Torrence Used Car Scrape")
        val result = torrenceUsedCarOfferScrape.scrape()

        logger.info("Torrence Used Car Scrape Result = ${result.get()}")
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
}
