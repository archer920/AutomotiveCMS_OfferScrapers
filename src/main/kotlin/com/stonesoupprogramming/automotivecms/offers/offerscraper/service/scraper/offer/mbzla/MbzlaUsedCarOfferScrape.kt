package com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.offer.mbzla

import com.stonesoupprogramming.automotivecms.offers.offerscraper.dao.OfferDao
import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.Offer
import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.OfferType
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.createChromeDriver
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.navigate
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.runScript
import com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.ScrapeResult
import com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.ScrapeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture

internal val usedCarJS = """
        let results = [];
	    ${'$'}('.ucs-container').each(function(){
		    let title = ${'$'}(this).find('.ucs-title').text();
            let image = ${'$'}(this).find('.ucs-image').find('img').attr('src');
            let price = ${'$'}(this).find('.ucs-price-block').find('.ucs-price').text();
            let priceTerm = ${'$'}(this).find('.ucs-price-term').text();
            let phoneNumber = ${'$'}(this).find('.ucs-phone').text().trim();
            let viewInventory = ${'$'}(this).find('.dt-inventory-btn').attr('href');
            let disclaimer = ${'$'}(this).find('.ncs-disclaimer').text();
            results.push({title: title, image: image, price: price, priceTerm: priceTerm, phoneNumber: phoneNumber, viewInventory: viewInventory, disclaimer: disclaimer});
	    });
        return results;
""".trimIndent()

@Service
@Qualifier("MBZLA Used Car")
class MbzlaUsedCarOfferScrape(private val offerDao: OfferDao,
                              applicationContext: AbstractApplicationContext): ScrapeService{

    private val source = "MBZLA"
    private val logger = LoggerFactory.getLogger(MbzlaNewCarOfferScrape::class.java)

    final var dealershipId: Long = -1

    init {
        try{
            dealershipId = System.getenv()["MBZLA_DEALERSHIP_ID"]!!.toLong()
        } catch (e: NumberFormatException){
            logger.error("DEALERSHIP_ID has to be numeric", e)
            SpringApplication.exit(applicationContext, ExitCodeGenerator { -1 })
        } catch (e: IllegalStateException){
            logger.error("DEALERSHIP_ID has not be set", e)
        }
    }

    @Async
    override fun scrape(): CompletableFuture<ScrapeResult>{
        val webDriver = createChromeDriver()
        return try {
            webDriver.navigate("https://www.mbzla.com/dtw-pre-owned-vehicle-offers-los-angeles-ca/")

            @Suppress("UNCHECKED_CAST")
            val resultSet = webDriver.runScript(usedCarJS) as List<Map<String, String>>

            val offers = resultSet.map { rs ->
                val vin = extractVinFromDisclaimer(rs["disclaimer"] ?: error("disclaimer is missing"))
                val title = extractTitle(rs["title"] ?: error("Title is missing"))
                val price = extractPrice(rs["price"] ?: error("Price is missing"))
                val disclaimer = extractDisclaimer(rs["disclaimer"] ?: error("disclaimer is missing"))
                val imageUrl = rs["image"] ?: error("Image is missing")
                val phoneNumber = rs["phoneNumber"] ?: error("Phone Number is missing")
                val inventoryLink = rs["viewInventory"] ?: error("View Inventory is missing")
                val priceTerm = rs["priceTerm"] ?: error("Price Term is missing")

                Offer(disclaimer = disclaimer.trim(),
                        image_url = imageUrl.trim(),
                        phoneNumber = phoneNumber.trim(),
                        price=price.trim(),
                        title=title.trim(),
                        priceTerm = priceTerm.trim(),
                        inventory_link = inventoryLink.trim(),
                        createdDate = Date(),
                        dealershipId = dealershipId,
                        source = source,
                        offerType = OfferType.USED_CAR,
                        vin = vin.trim())
            }
            logger.info("Deleting old offers...")
            offerDao.deleteAllByOfferType(OfferType.USED_CAR)

            logger.info("Saving new offers...")
            offerDao.saveAll(offers)

            logger.info("Offer Scrape Complete")
            CompletableFuture.completedFuture(ScrapeResult.DONE)
        } catch (e: Exception){
            logger.error("Failed to scrape used car offers", e)
            CompletableFuture.completedFuture(ScrapeResult.FAILED)
        } finally {
            webDriver.close()
        }
    }

    private fun extractDisclaimer(disclaimer: String): String {
        val monthIs = "mo.is"
        return if (monthIs in disclaimer){
            disclaimer.replace(monthIs, "mo. is")
        } else {
            disclaimer
        }
    }

    private fun extractPrice(price: String): String {
        val monthly = "mo.$"
        var p = price

        if (monthly in p){
            val parts = p.split(".")
            p = "Finance for ${parts[0]}. Buy for ${parts[1]}"
        }
        return p
    }

    private fun extractTitle(title: String): String {
        val text = "Used"
        val preOwned = "Pre-Owned"

        var t = title
        if (text in t){
            t = t.replace(text, "")
        }
        if (preOwned in t){
            t = t.replace(preOwned, "")
        }
        return t
    }

    private fun extractVinFromDisclaimer(disclaimer: String): String {
        val vin = "Vin: "
        val model = ". Model"
        val dotFor = ". FOR"

        var vinParts = disclaimer.split(vin)[1]
        if(model in vinParts) {
            vinParts = vinParts.split(model)[0]
        }
        if(dotFor in vinParts){
            vinParts = vinParts.split(dotFor)[0]
        }

        return vinParts
    }
}
