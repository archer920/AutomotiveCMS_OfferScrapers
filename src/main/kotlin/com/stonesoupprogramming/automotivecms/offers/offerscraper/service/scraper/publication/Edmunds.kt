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
import java.util.concurrent.ExecutorService
import java.util.function.Supplier

private val scrapeJS = """
    const scrapedArticles = [];

    ${'$'}('.tips-item').each(function(){
        const image = "https:" + ${'$'}(this).find('img').attr('data-dsrc');
        const title = ${'$'}(this).find('a').attr('title');
        const summary = ${'$'}(this).find('p').text() + ${'$'}(this).find('p').next('a').text();
        const link = 'https://edmunds.com' + ${'$'}(this).find('a').attr('href');
        let rating = 0.0;

        ${'$'}(this)
            .find('.rating-stars')
            .find('.icon-star-full')
            .each(function(e){
                rating++;
            });

        ${'$'}(this)
            .find('.rating-stars')
            .find('.icon-star-half')
            .each(function(e){ rating += .5; });

        scrapedArticles.push({
            image: image,
            title: title,
            summary: summary,
            link: link,
            rating: rating,
        });
    });

    return scrapedArticles;
""".trimIndent()

private val bylineJs = """
    let byline = ${'$'}('.author-metadata:first').find('a').text();
    
    if(byline === ''){
        byline = ${'$'}('.author-image').next().text().replace('by ', '');
    }
    return [byline];
""".trimIndent()

@Service
@Qualifier("Edmunds")
class EdmundsScraper(private val publishedContentDao: PublishedContentDao,
                     private val appProperties: AppProperties,
                     private val executor: ExecutorService): ScrapeService {

    private val logger = LoggerFactory.getLogger(EdmundsScraper::class.java)
    private val url = "http://www.edmunds.com/new-car-ratings/"
    private val publication = "edmunds"
    private val displayName = "Edmunds"
    private val dtype = "RatedReview"
    private val logo = """<img src="https://static.ed.edmunds-media.com/unversioned/img/car-buying/ad/logo-horizontal-white.svg" alt="Edmunds logo" />"""

    @Async
    override fun scrape(): CompletableFuture<ScrapeResult> {
        return try {
            val publications = createChromeDriver().use { webDriver ->
                webDriver.navigate(url)
                webDriver.runJq()
                webDriver.executeScript(scrapeJS)

                @Suppress("UNCHECKED_CAST")
                val results = webDriver.executeScript(scrapeJS) as List<Map<String, String>>

                results.map { rs ->
                    PublishedContent(
                            publication = publication,
                            displayName = displayName,
                            logo = logo,
                            image = rs["image"],
                            title = rs["title"],
                            link = rs["link"],
                            summary = (rs["summary"] ?: error("Summary is missing") ).replace("\n", ""),
                            date = Date(),
                            dtype = dtype,
                            rating = parseRating((rs["summary"] ?: error("Summary is missing") ))
                    )
                }.filter {
                    publishedContentDao.exists(title = it.title!!, summary = it.summary!!, link = it.link!!)
                }.toList()
            }

            logger.info("Scraping the bylines")
            val futures = publications.map { pub ->
                CompletableFuture.supplyAsync(Supplier { scrapeByline(pub) }, executor)}.toTypedArray()

            CompletableFuture.allOf(*futures).get()

            logger.info("Deleting old content")
            publishedContentDao.deleteAllByDateBefore(appProperties.expirationDate())

            logger.info("Saving new content")
            val content = futures.map { it.get() }.toList()
            publishedContentDao.saveAll(content)

            CompletableFuture.completedFuture(ScrapeResult.DONE)
        } catch (e: Exception){
            logger.error("Failed to scrape car and driver", e)
            CompletableFuture.completedFuture(ScrapeResult.FAILED)
        }
    }

    private fun scrapeByline(publishedContent: PublishedContent): PublishedContent {
        return createChromeDriver().use { remoteWebDriver ->
            remoteWebDriver.navigate(publishedContent.link!!)
            remoteWebDriver.runJq()
            @Suppress("UNCHECKED_CAST")
            publishedContent.copy(byline = (remoteWebDriver.executeScript(bylineJs) as List<String>)[0])
        }
    }

    private fun parseRating(summary: String): Double? {
        val token = "Edmunds Rating:"

        return if (summary.contains(token)){
            summary.split(token)[1].split(" ")[1].toDouble()
        } else {
            return null
        }
    }
}
