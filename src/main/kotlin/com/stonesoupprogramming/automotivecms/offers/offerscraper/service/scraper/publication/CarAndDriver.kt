package com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.publication

import com.stonesoupprogramming.automotivecms.offers.offerscraper.AppProperties
import com.stonesoupprogramming.automotivecms.offers.offerscraper.dao.PublishedContentDao
import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.PublishedContent
import com.stonesoupprogramming.automotivecms.offers.offerscraper.functions.exists
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.createChromeDriver
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.navigate
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.runJq
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.use
import com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.ScrapeResult
import com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.ScrapeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture

private val animateJs = """
    ${'$'}('.full-item').each(function(){
        ${'$'}('html, body').animate({scrollTop: ${'$'}(this).offset().top}, 1000, 'linear')
    });
""".trimIndent()

private val scrapeJS = """
    const scrapedArticles = [];

    function toTitleCase(str){
        return str.replace(/\w\S*/g, function(txt){return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();});
    }

    const logo = "<img style='height:80px' src='{{host}}/images/car_driver_logo.png'></img>";

    ${'$'}('.full-item').each(function(){
        const title = ${'$'}(this).find(".full-item-title").text();
        const link = 'https://www.caranddriver.com' + ${'$'}(this).find(".full-item-title").attr("href");
        const byline = toTitleCase(${'$'}(this).find(".byline-name").text());
        let image = ${'$'}(this).find('.full-item-image').find('img').attr('data-src');
        if (image === undefined || image === ''){
            image = ${'$'}(this).find('.full-item-image').find('img').attr('src');
        }
        if (image === undefined || image === ''){
            image = ${'$'}(this).find('.full-item-video').find('video').attr('poster');
        }
        
        let summary = ${'$'}(this).find(".full-item-dek").find('p').text();
        if(summary === ''){
            summary = ${'$'}(this).find('.full-item-dek').text();
        }

        scrapedArticles.push({
            publication: "car_driver",
            logo: logo,
            image: image,
            title: title,
            summary: summary,
            link: link,
            byline: byline
        });
    });

    return scrapedArticles;
""".trimIndent()

@Service
@Qualifier("Car and Driver")
class ScrapeCarAndDriver(private val publishedContentDao: PublishedContentDao,
                         private val appProperties: AppProperties): ScrapeService {

    private val logger = LoggerFactory.getLogger(ScrapeCarAndDriver::class.java)

    private val url = "https://caranddriver.com/news/"
    private val publication = "car_driver"
    private val display_name = "Car and Driver"
    private val dtype = "Article"


    @Async
    override fun scrape(): CompletableFuture<ScrapeResult> {
        return createChromeDriver().use { webDriver ->
            try {
                webDriver.navigate(url)
                webDriver.runJq()

                for (i in 0 until 5){
                    webDriver.executeScript(animateJs)
                    Thread.sleep(30 * i.toLong() * 1000)
                }

                @Suppress("UNCHECKED_CAST")
                val resultSet = webDriver.executeScript(scrapeJS) as List<Map<String, String>>

                val publications = resultSet.mapNotNull { rs ->
                    if (rs.containsKey("image")) {
                        PublishedContent(
                                publication = publication,
                                displayName = display_name,
                                logo = (rs["logo"] ?: error("Logo is required")).replace("{{host}}", appProperties.host),
                                image = (rs["image"] ?: error("Image is required")).split("?")[0],
                                title = rs["title"],
                                link = rs["link"],
                                byline = (rs["byline"] ?: error("Byline is required")).replace("\n", "").trim(),
                                summary = (rs["summary"] ?: error("Summary is required")).replace("\n", "").trim(),
                                date = Date(),
                                dismiss = false,
                                dtype = dtype,
                                rating = null)
                    } else {
                        null
                    }
                }.filter {
                    publishedContentDao.exists(title = it.title!!, summary = it.summary!!, link = it.link!!)
                }.toList()

                logger.info("Deleting old content")
                publishedContentDao.deleteAllByDateBefore(appProperties.expirationDate())

                logger.info("Saving new content")
                publishedContentDao.saveAll(publications)
                CompletableFuture.completedFuture(ScrapeResult.DONE)
            } catch (e: Exception){
                logger.error("Failed to scrape car and driver", e)
                CompletableFuture.completedFuture(ScrapeResult.FAILED)
            }
        }
    }
}
