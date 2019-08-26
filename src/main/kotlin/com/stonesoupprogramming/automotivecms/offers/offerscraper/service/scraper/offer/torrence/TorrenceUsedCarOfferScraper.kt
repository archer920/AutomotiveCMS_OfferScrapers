package com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.offer.torrence

import com.stonesoupprogramming.automotivecms.offers.offerscraper.dao.OfferDao
import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.Offer
import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.OfferType
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.createChromeDriver
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.navigate
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.use
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.waitUntilAllPresent
import com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.ScrapeResult
import com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.ScrapeService
import org.openqa.selenium.By
import org.openqa.selenium.remote.RemoteWebDriver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

private const val startPage = "https://www.torrancetoyota.com/featured-vehicles/used.htm"
private const val source = "TORRENCE"

private val logger = LoggerFactory.getLogger("com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.offer.torrence.TorrenceUsedCarOfferScrapeKt")

private fun allOfferPages(): List<String> {
    val driver = createChromeDriver(false)
    try {
        driver.get(startPage)
        waitUntilAllPresent(driver, By.className("view-link"))
        return driver.findElementsByClassName("view-link").map { it.getAttribute("href") }
    } catch (e: Exception){
        logger.error("Failed to scrape offerPages from $startPage", e)
        throw e
    } finally {
        driver.close()
    }
}

private fun getDisclaimer(driver: RemoteWebDriver): String {
    try {
        driver.switchTo().frame("mmd-frame-frame2")
        driver.findElementByCssSelector("#dr-deal-summary-link").click()
        val disclaimer = driver.findElementByCssSelector("#ds-short-disclaimer").text
        driver.switchTo().parentFrame()
        return disclaimer
    } catch (e: org.openqa.selenium.NoSuchFrameException){
        logger.error("No frame found on page ${driver.currentUrl}", e)
        throw e
    }
}

private fun getMonthlyPayment(driver: RemoteWebDriver): String {
    driver.switchTo().frame("mmd-frame-frame2")
    val payment = driver.findElementByCssSelector("#dr-monthly-pmt").text
    driver.switchTo().parentFrame()
    return payment
}

private fun getPaymentTerms(driver: RemoteWebDriver): String {
    driver.switchTo().frame("mmd-frame-frame2")
    driver.findElementByCssSelector("#dr-deal-summary-link").click()
    val term = driver.findElementByCssSelector("#term > span").text
    driver.switchTo().parentFrame()
    return term
}

private fun scrapeOffer(inventoryPage: String, dealershipId: Long): Offer {
    return createChromeDriver(false).use { driver: RemoteWebDriver ->
        driver.navigate(inventoryPage)
        val title = driver.findElementByCssSelector("#vehicle-title1-app-root > h1").text.replace("\n", " ")
        val disclaimer = getDisclaimer(driver)
        val imageUrl = driver.findElementByCssSelector("#media1-app-root > div > div.photo-carousel.image-mode.add-border > div > div.slider-frame > ul > li:nth-child(1) > img").getAttribute("src")
        val phoneNumber = driver.findElementByCssSelector("body > div.page-header.responsive-centered-nav.noShrink.sticky-header-nav > div.header-contact.clearfix > div.ddc-content.header-default.pull-right > div.vcard > ul > li.tel.phone1.collapsed-show > span.value.text-nowrap").text
        val buy = driver.findElementByCssSelector("#detailed-pricing1-app-root > div > dl.pricing-detail.line-height-condensed.mb-4.inv-type-used > dd.font-weight-bold.text-muted.ddc-font-size-large.final-price > span").text
        val payment = getMonthlyPayment(driver)
        val term = getPaymentTerms(driver)
        val price = "Finance for $payment. Buy for $buy"
        val vin = driver.findElementByCssSelector("#vehicle-title1-app-root > ul > li:nth-child(1)").text.replace("Vin: ", "")

        Offer(
                dealershipId = dealershipId,
                title = title,
                disclaimer = disclaimer,
                image_url = imageUrl,
                offerType = OfferType.USED_CAR,
                phoneNumber = phoneNumber,
                price = price,
                priceTerm = term,
                source = source,
                vin = vin,
                inventory_link = inventoryPage,
                createdDate = Date()
        )
    }
}
@Service
@Qualifier("Torrence Used Car")
class TorrenceUsedCarOfferScrape(private val offerDao: OfferDao,
                                 private val executorService: ExecutorService,
                                applicationContext: AbstractApplicationContext): ScrapeService {

    final var dealershipId: Long = -1

    init {
        try{
            dealershipId = System.getenv()["TORRENCE_DEALERSHIP_ID"]!!.toLong()
        } catch (e: NumberFormatException){
            logger.error("TORRENCE_DEALERSHIP_ID has to be numeric", e)
            SpringApplication.exit(applicationContext, ExitCodeGenerator { -1 })
        } catch (e: IllegalStateException){
            logger.error("TORRENCE_DEALERSHIP_ID has not be set", e)
        }
    }

    @Async
    override fun scrape(): CompletableFuture<ScrapeResult> {
        return try {
            val offers = allOfferPages().map { scrapeOffer(it, dealershipId) }

            logger.info("Deleting old offers")
            offerDao.deleteAllBySourceAndOfferType(source=source, offerType = OfferType.USED_CAR)

            logger.info("Saving new offers")
            offerDao.saveAll(offers)

            logger.info("Offer Scrape Complete")
            CompletableFuture.completedFuture(ScrapeResult.DONE)
        } catch (e: Exception){
            logger.error("Failed to scrape used car offers", e)
            CompletableFuture.completedFuture(ScrapeResult.FAILED)
        }
    }
}


fun main(args: Array<String>){
    val offerPages = allOfferPages()
    offerPages.forEach { page ->
        try {
            println(scrapeOffer(page, 1))
        } catch (e: Exception){
            logger.error("Unable to scrape offer from $page", e)
        }
    }
}
