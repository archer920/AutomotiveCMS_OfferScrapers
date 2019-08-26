package com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.offer.torrence

import com.stonesoupprogramming.automotivecms.offers.offerscraper.dao.OfferDao
import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.Offer
import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.OfferType
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.createChromeDriver
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.navigate
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.runScript
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.waitUntilClickable
import com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.ScrapeResult
import com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.ScrapeService
import org.openqa.selenium.By
import org.openqa.selenium.ElementClickInterceptedException
import org.openqa.selenium.WebElement
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

private val logger = LoggerFactory.getLogger(TorrenceNewCarOfferScrape::class.java)
private const val source = "TORRENCE"

@Service
@Qualifier("Torrence New Car")
class TorrenceNewCarOfferScrape(private val offerDao: OfferDao,
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
            val offers = scrapeOffers(dealershipId)

            logger.info("Deleting old offers")
            offerDao.deleteAllBySourceAndOfferType(source=source, offerType = OfferType.NEW_CAR)

            logger.info("Saving new offers")
            offerDao.saveAll(offers)

            logger.info("Offer Scrape Complete")
            CompletableFuture.completedFuture(ScrapeResult.DONE)
        } catch (e: Exception){
            logger.error("Failed to scrape new car offers", e)
            CompletableFuture.completedFuture(ScrapeResult.FAILED)
        }
    }
}

private fun scrapeOffers(dealershipId: Long): List<Offer> {
    //This has to stay headless because we are using WebElement.isVisible
    val driver = createChromeDriver(false)
    return try {
        driver.navigate("https://www.torrancetoyota.com/promotions/new/index.htm")

        val phoneNumber = driver.findElementByCssSelector("body > div.page-header.responsive-centered-nav.noShrink.sticky-header-nav > div.header-contact.clearfix > div.ddc-content.header-default.pull-right > div.vcard > ul > li.tel.phone1.collapsed-show > span.value.text-nowrap").text
        driver.findElementsByLinkText("Offer Details and Disclaimers").map { link ->
            val dialog = openDialog(driver, link)

            val title = dialog.findElement(By.className("promo-model")).text.replace("\n", " ")
            val disclaimer = dialog.findElement(By.className("promo-extra-details")).findElements(By.tagName("ul"))[1].text
            val imageUrl = dialog.findElement(By.className("promo-image")).getAttribute("src")
            val vins = if (disclaimer.contains(". 36")) {
                disclaimer.substring(0, disclaimer.indexOf(". 36")).trim()
            } else  {
                disclaimer.substring(0, disclaimer.indexOf(". MSRP")).trim()
            }
            val priceRaw = dialog.findElement(By.className("promo-short-description")).text.replace("\n", "  ")
            val inventoryLink = dialog.findElement(By.className("promo-cta-link")).getAttribute("href")

            val price = if (priceRaw.contains("for")){
                priceRaw.split("for")[1].trim()
            } else {
                priceRaw
            }
            val term = if (priceRaw.contains("for")){
                priceRaw.split("for")[0].trim()
            } else {
                ""
            }

            closeDialog(dialog, driver)

            Offer(
                    dealershipId = dealershipId,
                    title = title,
                    createdDate = Date(),
                    disclaimer = disclaimer,
                    image_url = imageUrl,
                    offerType = OfferType.NEW_CAR,
                    phoneNumber = phoneNumber,
                    vin = vins,
                    price = price,
                    priceTerm = term,
                    source = source,
                    inventory_link = inventoryLink
            )
        }
    } catch (e: Exception){
        logger.error("Failed to scrape TorrenceNewCar offers", e)
        throw e
    } finally {
        driver.close()
    }
}

val referenceMap: MutableMap<String, Int> = mutableMapOf()
private fun openDialog(driver: RemoteWebDriver, link: WebElement): WebElement{
    synchronized(referenceMap){
        val dataTitle = link.getAttribute("data-promo-tracking-label")
        val selector = "a[data-promo-tracking-label='$dataTitle']"

        println("Reference map has $dataTitle: ${referenceMap.containsKey(dataTitle)}")
        val index = if (referenceMap.containsKey(dataTitle)){
            val i = referenceMap[dataTitle]!!
            referenceMap[dataTitle] = i + 1
            referenceMap[dataTitle]
        } else {
            referenceMap[dataTitle] = 0
            0
        }

        driver.runScript("""
            var elems = document.querySelectorAll("$selector");
            elems[$index].click();
        """.trimIndent())

    }

    Thread.sleep(1500)
    return driver.findElementsByCssSelector("div[role=dialog]").first { it.isDisplayed }
}

private fun closeDialog(dialog: WebElement, driver: RemoteWebDriver){
    try {
        val closeButton = dialog.findElement(By.className("ui-dialog-titlebar-close"))
        waitUntilClickable(driver, closeButton)
        closeButton.click()
        Thread.sleep(1500)
    } catch (e: ElementClickInterceptedException){
        driver.runScript("""
            document.getElementsByClassName('ui-dialog-titlebar-close').each(function (elem) { elem.click(); });
        """.trimIndent())
        Thread.sleep(1000)
    }
}

fun main(args: Array<String>){
    scrapeOffers(1).forEach {
        println("${it.title} - ${it.price} - ${it.priceTerm}")
    }
}
