package com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.publication

import com.stonesoupprogramming.automotivecms.offers.offerscraper.AppProperties
import com.stonesoupprogramming.automotivecms.offers.offerscraper.dao.PublishedContentDao
import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.PublishedContent
import com.stonesoupprogramming.automotivecms.offers.offerscraper.functions.exists
import com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.*
import com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.ScrapeResult
import com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper.ScrapeService
import org.openqa.selenium.TimeoutException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.function.Supplier

private val scrapeJs = """
    const scrapedArticles = [];

    let logo = ${'$'}('#jalopnik-svgid').html();
    logo = '<svg width="100%" height="100%">' + logo + "</svg>";

    ${'$'}('.js_post_item').each(function(){

        const title = ${'$'}(this).find("a.js_link > h1").text();

        const link = ${'$'}(this).find("a.js_link > h1").parent().attr("href");

        let image = ${'$'}(this).find('figure').find('.js_lazy-image').find('img').attr('srcset');
        if (image === undefined){
            image = ${'$'}(this).find('figure').find('.js_lazy-image').find('video').attr('poster');
        }

        const byline = ${'$'}(this).find("a.fwjlmD").text();

        scrapedArticles.push({
            logo: logo,
            image: image,
            title: title,
            link: link,
            byline: byline
        });
    });
    return scrapedArticles;
""".trimIndent()

private val animateJs = """ 
    ${'$'}('.js_post_item').each(function(){
        ${'$'}('html, body').animate({
            scrollTop: ${'$'}(this).offset().top
        }, 1000, 'linear')
    });
""".trimIndent()

private val summaryJs = """
    return ${'$'}('.entry-content > p:nth-child(3)').text()
""".trimIndent()

@Service
@Qualifier("Jalopnik")
class Jalopnik(private val publishedContentDao: PublishedContentDao,
               private val appProperties: AppProperties,
               private val executor: ExecutorService): ScrapeService {

    private val logger = LoggerFactory.getLogger(Jalopnik::class.java)

    private val url = "https://jalopnik.com/c/news"
    private val publication = "jalopnik"
    private val displayName = "Jalopnik"
    private val dtype = "Article"

    override fun scrape(): CompletableFuture<ScrapeResult> {
        return createChromeDriver().use { webDriver ->
            try {
                webDriver.navigate(url)
                webDriver.runJq()

                webDriver.runScript(animateJs)
                Thread.sleep(60 * 1000)

                @Suppress("UNCHECKED_CAST")
                val resultSet = webDriver.executeScript(scrapeJs) as List<Map<String, String>>

                val publications = resultSet.map { rs ->
                    PublishedContent(
                            publication = publication,
                            displayName = displayName,
                            logo = (rs["logo"] ?: error("Logo is required")),
                            image = parseImage(rs["image"] ?: error("Image is required")),
                            title = (rs["title"] ?: error("Title is required")),
                            link = (rs["link"] ?: error("Link is required")),
                            byline = (rs["byline"] ?: error("Byline is required")),
                            date = Date(),
                            dismiss =  false,
                            dtype = dtype)
                }.filter { publishedContentDao.exists(title = it.title!!, link = it.link!!) }.toList()

                logger.info("Scraping summaries")
                val futures = publications.map { pub ->
                    CompletableFuture.supplyAsync(Supplier { scrapeSummary(pub) }, executor)}.toTypedArray()
                CompletableFuture.allOf(*futures).get()

                logger.info("Deleting old content")
                publishedContentDao.deleteAllByDateBefore(appProperties.expirationDate())

                logger.info("Saving new content")
                val content = futures.map { it.get() }.filter { publishedContentDao.exists(it) }.toList()
                publishedContentDao.saveAll(content)

                CompletableFuture.completedFuture(ScrapeResult.DONE)
            } catch (e: Exception){
                logger.error("Failed to scrape Jalopnik", e)
                CompletableFuture.completedFuture(ScrapeResult.FAILED)
            }
        }
    }

    private fun scrapeSummary(pub: PublishedContent): PublishedContent {
        return createChromeDriver().use { remoteWebDriver ->
            remoteWebDriver.navigate(pub.link ?: error("Link is required"))
            remoteWebDriver.runJq()

            pub.copy(summary = remoteWebDriver.runScript(summaryJs) as String)
        }
    }

    private fun parseImage(imageStr: String): String {
        val parts = imageStr.split(" ")
        return if(parts.size == 1){
            imageStr
        } else {
            parts[2]
        }
    }

}
