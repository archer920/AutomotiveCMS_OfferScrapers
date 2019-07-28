package com.stonesoupprogramming.automotivecms.offers.offerscraper.functions

import com.stonesoupprogramming.automotivecms.offers.offerscraper.dao.PublishedContentDao
import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.PublishedContent

@Deprecated("TODO: Decouple from publishedContent Object")
fun PublishedContentDao.exists(publishedContent: PublishedContent): Boolean {
    return this.countByTitleAndSummaryAndLink(
            title = publishedContent.title!!,
            summary = publishedContent.summary!!,
            link = publishedContent.link!!) == 0
}

fun PublishedContentDao.exists(title: String, link: String): Boolean =
        this.countByTitleAndLink(title = title, link = link) == 0
