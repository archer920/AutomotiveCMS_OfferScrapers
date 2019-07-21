package com.stonesoupprogramming.automotivecms.offers.offerscraper.service.scraper

import java.util.concurrent.CompletableFuture

interface ScrapeService {

    fun scrape(): CompletableFuture<ScrapeResult>
}
