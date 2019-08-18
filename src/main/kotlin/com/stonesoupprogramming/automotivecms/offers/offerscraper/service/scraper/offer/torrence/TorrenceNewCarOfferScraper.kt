package com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.offer.torrence

import com.stonesoupprogramming.automotivecms.offers.offerscraper.dao.OfferDao
import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.Offer
import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.OfferType
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.createChromeDriver
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.navigate
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.runScript
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
import java.util.concurrent.*
import java.util.function.Supplier

private val logger = LoggerFactory.getLogger(TorrenceNewCarOfferScrape::class.java)
private const val source = "TORRENCE"

@Service
@Qualifier("Torrence New Car")
class TorrenceNewCarOfferScrape(private val offerDao: OfferDao,
                                private val executor: ExecutorService,
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
            val offers = scrapeOffers(executor = executor)

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

private fun getDisclaimer(driver: RemoteWebDriver): String {
    driver.findElementByCssSelector("#detailed-pricing1-app-root > div > dl.pricing-detail.line-height-condensed.mb-4.inv-type-new > dt.pointer.discount > button").click()
    Thread.sleep(5000)

    fun disclaimerFunc(attemptNum: Int = 0, maxAttempts: Int = 10): String {
        return try {
            driver.findElementByClassName("modal-body").findElement(By.cssSelector(".text-muted > p")).text
        } catch (e: org.openqa.selenium.NoSuchElementException){
            logger.debug("Waiting for the dialog", e)
            return if (attemptNum < maxAttempts){
                val sleepTime = (attemptNum + 1) * 1000L
                Thread.sleep(sleepTime)
                disclaimerFunc(attemptNum + 1, maxAttempts)
            } else {
                throw e
            }
        }
    }

    val disclaimer = disclaimerFunc()
    driver.findElementByClassName("modal-title").findElement(By.tagName("button")).click()
    Thread.sleep(1000)
    return disclaimer
}

fun getFinanceTerm(driver: RemoteWebDriver): Pair<String, String> {
    driver.switchTo().frame("mmd-frame-frame2")
    driver.runScript("""document.querySelector('#dr-deal-summary-link').click()""")
    val payment = driver.findElementByCssSelector("#dr-monthly-pmt").text
    val term = driver.findElementById("term").findElement(By.tagName("span")).text
    driver.switchTo().parentFrame()
    return Pair(payment, term)
}

fun getImageUrl(driver: RemoteWebDriver): String {
    return try {
        driver.findElementByCssSelector("#media1-app-root > div > div.photo-carousel.image-mode > div > div > ul > div > img").getAttribute("src")
    } catch (e: org.openqa.selenium.NoSuchElementException){
        driver.findElementByCssSelector("#media1-app-root > div > div.blurred-container > div.photo-carousel.image-mode.add-border > div > div.slider-frame > ul > li:nth-child(1) > img").getAttribute("src")
    }
}

private fun scrapeOffer(pageLink: String): Offer? {
    val driver = createChromeDriver(headless = false)
    return try {
        driver.navigate(pageLink)
        val title = driver.findElementByCssSelector("#vehicle-title1-app-root > h1").text.replace("\n", " ")
        val disclaimer = getDisclaimer(driver)
        val imageUrl = getImageUrl(driver)
        val phoneNumber = driver.findElementByCssSelector("body > div.page-header.responsive-centered-nav.noShrink.sticky-header-nav > div.header-contact.clearfix > div.ddc-content.header-default.pull-right > div > ul > li.tel.phone1.collapsed-show > span.value.text-nowrap").text
        val msrp = driver.findElementByCssSelector("#detailed-pricing1-app-root > div > dl.pricing-detail.line-height-condensed.mb-4.inv-type-new > dd.font-weight-bold.text-muted.ddc-font-size-large.final-price > span").text
        val (payment, term) = getFinanceTerm(driver)
        val price = "Finance for $payment. Buy for $msrp"
        val vin = driver.findElementByCssSelector("#vehicle-title1-app-root > ul > li:nth-child(1)").text.replace("Vin: ", "")

        Offer(
                title = title,
                disclaimer = disclaimer,
                image_url = imageUrl,
                inventory_link = pageLink,
                offerType = OfferType.NEW_CAR,
                phoneNumber = phoneNumber,
                price = price,
                priceTerm = term,
                vin = vin,
                source = source
        )
    } catch (e: Exception) {
        logger.error("Failed to scrape offer on page $pageLink", e)
        null
    } finally {
        driver.close()
    }
}

private fun findOfferCategoryLinks(): List<String> {
    val page = "https://www.torrancetoyota.com/promotions/new/index.htm"
    val driver = createChromeDriver(false)
    return try {
        driver.navigate(page)
        driver.findElementsByClassName("promo-cta-link").map { it.getAttribute("href")  }
    } catch (e: Exception){
        logger.error("Failed to find the offer category links on page $page", e)
        throw e
    } finally {
        driver.close()
    }
}

private fun findAllOfferPages(categoryPage: String): List<String> {
    val driver = createChromeDriver(false)
    return try {
        driver.navigate(categoryPage)
        val offerPages = driver.findElementsByClassName("url").map { it.getAttribute("href") }.toMutableList()
        offerPages.remove("https://www.torrancetoyota.com/")

        try {
            val nextPage = driver.findElementByCssSelector("#compareForm > div > div.ft > div > div > div.pull-right > ul > li:nth-child(3) > a").getAttribute("href")
            findAllOfferPages(nextPage).let { offerPages.addAll(it) }
        } catch (e: org.openqa.selenium.NoSuchElementException){
            logger.debug("At the last page")
        }
        offerPages.toList()
    } catch (e: Exception){
        logger.info("Unable to scrape offer pages for $categoryPage", e)
        throw e
    } finally {
        driver.close()
    }
}

fun scrapeOffers(executor: ExecutorService): List<Offer> {
    val offerCategoryLinks = findOfferCategoryLinks()
    val offerPagesFutures = offerCategoryLinks.map { CompletableFuture.supplyAsync(Supplier { findAllOfferPages(it) }, executor) }.toTypedArray()
    CompletableFuture.allOf(*offerPagesFutures)

    val offerPages = offerPagesFutures.map { it.get() }.flatMap { it -> it.asIterable() }
    val offerFutures = offerPages.map { CompletableFuture.supplyAsync(Supplier { scrapeOffer(it) }, executor) }.toTypedArray()
    CompletableFuture.allOf(*offerFutures)

    return offerFutures.mapNotNull { it.get() }
}

fun main(args: Array<String>){
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

    val ex = executorService()
    scrapeOffers(ex).forEach {
        println(it)
    }
}
