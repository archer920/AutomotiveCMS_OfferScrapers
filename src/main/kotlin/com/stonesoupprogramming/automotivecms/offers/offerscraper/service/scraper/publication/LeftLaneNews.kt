package com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.publication

import com.stonesoupprogramming.automotivecms.offers.offerscraper.AppProperties
import com.stonesoupprogramming.automotivecms.offers.offerscraper.dao.PublishedContentDao
import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.PublishedContent
import com.stonesoupprogramming.automotivecms.offers.offerscraper.functions.exists
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.*
import com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.ScrapeResult
import com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.ScrapeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.function.Supplier

private val scrapeJs = """
    const scrapedArticles = [];
    const logo = "<img style='height:80px' src='{{host}}/images/left_lane_news_logo.png'></img>";

    ${'$'}('article').each(function(){
        const title = ${'$'}(this).find('.msh').text();
        const summary = ${'$'}(this).find('.ex').text();
        let link = 'https://' + ${'$'}(this).find("a").attr('href').replace('//www', 'www').replace('https:', '');
        link = link.replace("////", "//");

        scrapedArticles.push({
            logo: logo,
            title: title,
            summary: summary,
            link: link,
        });
    });
    return scrapedArticles;
""".trimIndent()

private val bylineImageJs = """
    const byline = ${'$'}('address').text().trim(' ').replace('by', '');

    let image = ${'$'}('.wimg > amp-img').attr('src');
    if (image === undefined){
        image = ${'$'}('amp-img > img').attr('src');
    }

    return {byline: byline, image: image};
""".trimIndent()

@Service
@Qualifier("Left Lane News")
class LeftLaneNews(private val publishedContentDao: PublishedContentDao,
                   private val appProperties: AppProperties,
                   private val executor: ExecutorService): ScrapeService {

    private val logger = LoggerFactory.getLogger(LeftLaneNews::class.java)
    private val url = "http://leftlanenews.com"
    private val publication = "left_name_news"
    private val displayName = "Left Lane News"
    private val dtype = "Article"

    override fun scrape(): CompletableFuture<ScrapeResult> {
         val publications =  createChromeDriver().use { driver ->
            try {
                driver.navigate(url)
                driver.runJq()

                @Suppress("UNCHECKED_CAST")
                val resultSet = driver.executeScript(scrapeJs) as List<Map<String, String>>

                resultSet.map { rs ->
                    PublishedContent(
                            publication = publication,
                            displayName = displayName,
                            logo = (rs["logo"] ?: error("Logo is required")).replace("{{host}}", appProperties.host),
                            title = (rs["title"] ?: error("Title is required")),
                            link = (rs["link"] ?: error("Link is required")),
                            date = Date(),
                            dismiss = false,
                            dtype = dtype
                    )
                }.filter { publishedContentDao.exists(it.title!!, it.link!!) }.toList()
            } catch (e: Exception){
                logger.error("Failed to scrape Left Lane News", e)
                null
            }
        }
        return if (publications == null){
            CompletableFuture.completedFuture(ScrapeResult.FAILED)
        } else {
            logger.info("Scraping image and byline")
            val futures = publications.map { pub ->
                CompletableFuture.supplyAsync(Supplier { scrapeImageByline(pub) }, executor) }.toTypedArray()
            CompletableFuture.allOf(*futures).get()

            logger.info("Deleting old content")
            publishedContentDao.deleteAllByDateBefore(appProperties.expirationDate())

            logger.info("Saving new content")
            val content = futures.map { it.get() }.toList()
            publishedContentDao.saveAll(content)

            CompletableFuture.completedFuture(ScrapeResult.DONE)
        }
    }

    private fun scrapeImageByline(pub: PublishedContent): PublishedContent? {
        return createChromeDriver().use { rwd ->
            rwd.navigate(pub.link!!)
            rwd.runJq()

            @Suppress("UNCHECKED_CAST")
            val rs = rwd.runScript(bylineImageJs) as Map<String, String>

            pub.copy(
                    byline = (rs["byline"] ?: error("Byline is required")).trim(),
                    image = (rs["image"] ?: error("Image is required")).trim()
            )
        }
    }

}
