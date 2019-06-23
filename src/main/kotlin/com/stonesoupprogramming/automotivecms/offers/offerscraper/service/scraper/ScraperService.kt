package com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper

import java.util.concurrent.CompletableFuture

interface ScraperService {

    fun scrapeOffers(): CompletableFuture<ScrapeResult>
}
