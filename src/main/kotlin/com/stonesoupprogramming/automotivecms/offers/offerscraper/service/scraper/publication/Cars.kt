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

private val scrapeJS = """
    const scrapedArticles = [];

    let logo = ${'$'}(".global-nav__logo").attr("src");
    logo = '<img class="cars" src="https://cars.com/' + logo + '"></img>';

    ${'$'}('.article-preview').each(function(){
        const title = ${'$'}(this).find("a").attr("title");
        const link = "https://www.cars.com" + ${'$'}(this).find(".article-preview__text > a").attr("href");
        const summary = ${'$'}(this).find(".article-preview__text")
                                .text()
                                .trim()
                                .replace('Read More', '');

        const image = ${'$'}(this).find(".article-preview__image > a")
                            .attr("style")
                            .replace('background-image:url(', '')
                            .replace(');', '');

        let byline = ${'$'}(this).find("p.helper-text")
                            .text()
                            .replace('By ', '')
                            .split(' ');
        byline = byline[0] + ' ' +  byline[1]; //byline[0] is first name, byline[1] = lastName

        scrapedArticles.push({
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
@Qualifier("Cars.com")
class CarsScraper(private val publishedContentDao: PublishedContentDao,
                  private val appProperties: AppProperties): ScrapeService {

    private val logger = LoggerFactory.getLogger(CarsScraper::class.java)
    private val url = "https://www.cars.com/news/expert-reviews/?perPage=500"
    private val publication = "cars"
    private val display_name = "Cars.com"
    private val dtype = "Review"

    @Async
    override fun scrape(): CompletableFuture<ScrapeResult> {
        return createChromeDriver().use { webDriver ->
            try {
                webDriver.navigate(url)
                webDriver.runJq()

                @Suppress("UNCHECKED_CAST")
                val resultSet = webDriver.executeScript(scrapeJS) as List<Map<String, String>>

                val publications = resultSet.map { rs ->
                    PublishedContent(publication = publication,
                            displayName = display_name,
                            logo = rs["logo"] ?: error("Logo is required"),
                            image = rs["image"] ?: error("Image is required"),
                            title = rs["title"] ?: error("Title is required"),
                            link = rs["link"] ?: error("Link is required"),
                            byline = rs["byline"]  ?: error("Byline is required"),
                            summary = (rs["summary"] ?: error("Summary is required")).replace("\n", "").replace("\t", "").trim(),
                            date = Date(),
                            dismiss = false,
                            dtype = dtype
                    )
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
