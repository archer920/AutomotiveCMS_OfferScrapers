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
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture

private val scrapeJs = """
    const scrapedArticles = [];

    const logoHtml = "<img class='moto-logo' src='https://st.motortrend.com/app/themes/motortrend/mantle/modules/assets/motor-trend-nav-logo.svg'></img>";

    ${'$'}('article[class="block-post"]').each(function(){
        const image = ${'$'}(this).find(".block-post-image > img").attr('data-src');
        const title = ${'$'}(this).find(".block-post-title > span").text();
        const link = ${'$'}(this).find(".block-post-title").attr("href");
        const summary = ${'$'}(this).find('.block-post-excerpt').text();
        const byline = ${'$'}(this).find(".byline > a").text().replace('Motor Trend Staff', '').replace('Manufacturer', '');
    
        scrapedArticles.push({
            logo: logoHtml,
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
@Qualifier("Motor Trend")
class MotorTrend(private val publishedContentDao: PublishedContentDao,
                 private val appProperties: AppProperties): ScrapeService {

    private val logger = LoggerFactory.getLogger(MotorTrend::class.java)
    private val url = "http://www.motortrend.com/car-reviews"
    private val publication = "motor_trend"
    private val displayName = "Motor Trend"
    private val dtype = "Article"

    override fun scrape(): CompletableFuture<ScrapeResult> {
        return createChromeDriver(false).use { rwd ->
            try {
                rwd.navigate(url)
                rwd.runJq()

                @Suppress("UNCHECKED_CAST")
                val resultSet = rwd.executeScript(scrapeJs) as List<Map<String, String>>

                val publications = resultSet.map { rs ->
                    PublishedContent(
                            publication = publication,
                            displayName = displayName,
                            logo = (rs["logo"] ?: error("Logo is required")),
                            title = (rs["title"] ?: error ("Title is required")),
                            link = (rs["link"] ?: error("Link is required")),
                            image = (rs["image"] ?: error("Image is required")),
                            byline = (rs["byline"] ?: error("Byline is required")).replace("\n", "").trim(),
                            summary = (rs["summary"] ?: error("Summary is required")).replace("\n", "").trim(),
                            date = Date(),
                            dismiss = false,
                            dtype = dtype
                    )
                }.filter { publishedContentDao.exists(title = it.title!!, summary = it.summary!!, link = it.link!!) }

                logger.info("Deleting old content")
                publishedContentDao.deleteAllByDateBefore(appProperties.expirationDate())

                logger.info("Saving new content")
                publishedContentDao.saveAll(publications)

                CompletableFuture.completedFuture(ScrapeResult.DONE)
            } catch (e: Exception){
                logger.error("Failed to scrape Motor Trend", e)
                CompletableFuture.completedFuture(ScrapeResult.FAILED)
            }
        }
    }

}
