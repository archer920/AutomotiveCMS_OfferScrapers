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

internal val newCarJS = """
        let results = [];
	    ${'$'}('.ncs-container').each(function(){
		    let title = ${'$'}(this).find('.ncs-title').text();
            let image = ${'$'}(this).find('.ncs-image').find('img').attr('src');
            let price = ${'$'}(this).find('.ncs-price-label').first().text() + ' ' + ${'$'}(this).find('.ncs-price').first().text();
            let priceTerm = ${'$'}(this).find('.ncs-price-block').find('.ncs-price-term').text();
            let phoneNumber = ${'$'}(this).find('.ncs-phone').text().trim();
            let viewInventory = ${'$'}(this).find('.dt-inventory-btn').attr('href');
            let disclaimer = ${'$'}(this).find('.ncs-disclaimer').text();
            results.push({title: title, image: image, price: price, priceTerm: priceTerm, phoneNumber: phoneNumber, viewInventory: viewInventory, disclaimer: disclaimer});
	    });
        return results;
""".trimIndent()

@Service
@Qualifier("MBZLA New Car")
class MbzlaNewCarOfferScrape(private val offerDao: OfferDao,
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
            webDriver.navigate("https://www.mbzla.com/dtw-new-mercedes-benz-lease-incentives-finance-offers-los-angeles-ca/")

            @Suppress("UNCHECKED_CAST")
            val resultSet = webDriver.runScript(newCarJS) as List<Map<String, String>>

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
                        priceTerm = priceTerm.trim(),
                        price=price.trim(),
                        title=title.trim(),
                        inventory_link = inventoryLink.trim(),
                        createdDate = Date(),
                        dealershipId = dealershipId,
                        source = source,
                        offerType = OfferType.NEW_CAR,
                        vin = vin.trim())
            }
            logger.info("Deleting old offers...")
            offerDao.deleteAllBySourceAndOfferType(source = source, offerType = OfferType.NEW_CAR)

            logger.info("Saving new offers...")
            offerDao.saveAll(offers)

            logger.info("Offer Scrape Complete")
            CompletableFuture.completedFuture(ScrapeResult.DONE)
        } catch (e: Exception){
            logger.error("Failed to scrape new car offers", e)
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
        val forBuy = "forBuy"
        val monthly = "mo.$"
        var p = price

        if (forBuy in p){
            p = p.replace(forBuy, "")
        }
        if (monthly in p){
            p = p.replace(monthly, "")
        }

        return p
    }

    private fun extractTitle(title: String): String {
        val newText = "New"
        return if (newText in title){
            title.replace(newText, "")
        } else {
            title
        }
    }

    private fun extractVinFromDisclaimer(disclaimer: String): String {
        val vin = "Vin: "
        val model = ". Model"
        val dotFor = ". FOR"
        val msrp = ". MSRP"

        var vinParts = disclaimer.split(vin)[1]
        if(model in vinParts) {
            vinParts = vinParts.split(model)[0]
        }
        if(dotFor in vinParts){
            vinParts = vinParts.split(dotFor)[0]
        }
        if(msrp in vinParts){
            vinParts = vinParts.split(msrp)[0]
        }

        return vinParts
    }
}
